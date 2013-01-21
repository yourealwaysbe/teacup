/**
 * Copyright 2013 Matthew Hague (matthewhague@zoho.com)
 * Released under the GNU General Public License v3 (see GPL.txt)
 */

package net.chilon.matt.teacup;

import java.io.File;
import java.io.FileFilter;
import java.util.concurrent.Semaphore;

import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.RemoteViews;


public class TeaCupService extends Service {

    public final static String TEACUP_SERVICE = "TeaCupService";

    private static final int INVALID_ID = -1;

    private class MetaData {
        String artist = null;
        String album = null;
        String title = null;
        String filename = null;
        long length = 0;
        Bitmap artBmp = null;
    }

    // ugh, concurrency...
    // meta data updated by asynctask because of database lookup
    // but as soon as updatemeta broadcast received we know currentMeta is out of
    // date, so we lock it so a change to playstate doesn't scrobble out of date
    // meta data: updatelastfm task has to wait for the current meta lock before
    // scrobble update.
    Semaphore currentMetaMutex = new Semaphore(1);
    MetaData currentMeta = null;
    private boolean currentlyPlaying = false;

    private static UpdateMetaTask previousMeta = null;

    BroadcastReceiver receiver = null;



    public void onCreate() {
        Log.d("TeaCup", "creating service receiver: " + receiver);
        makeReceiver();
    }


    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("TeaCup", "on start command service");
        makeReceiver();
        return START_STICKY;
    }

    public void onDestroy() {
        Log.d("TeaCup", "Destroying service");
        if (receiver != null)
            unregisterReceiver(receiver);
        receiver = null;
    }

    @Override
    public IBinder onBind(Intent arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    private void makeReceiver() {
        if (receiver != null)
            unregisterReceiver(receiver);

        Config config = new Config(getApplicationContext());
        IntentFilter filter = new IntentFilter();
        filter.addAction(config.getPlayer().getMetaChangedAction());
        filter.addAction(config.getPlayer().getPlaystateChangedAction());

        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Config config = new Config(context);
                PlayerConfig player = config.getPlayer();
                String action = intent.getAction();

                Log.d("TeaCup", "received " + action);

                if (player.getMetaChangedAction().equals(action)) {
                    updateMeta(config, context, intent);
                }

                if (player.getPlaystateChangedAction().equals(action)) {
                    updatePlaystate(config, context, intent);
                }

                if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
                    doConnectivityChange(config, context);
                }
            }
        };
        registerReceiver(receiver, filter);
    }

    private void doConnectivityChange(Config config, Context context) {
        UpdateLastFMArgs args = new UpdateLastFMArgs();
        args.config = config;
        args.context = context;

        new UpdateLastFMTask().execute(args);
        new LastFMScrobbleCacheTask().execute(args);
    }


    private void updateMeta(Config config,
                            Context context,
                            Intent intent) {
        if (previousMeta != null)
            previousMeta.cancel(true);

        UpdateMetaArgs args = new UpdateMetaArgs();
        args.config = config;
        args.context = context;
        args.intent = intent;

        Log.d("TeaCup", "updateMeta getting lock");
        try {
        	currentMetaMutex.acquire();
        	Log.d("TeaCup", "updateMeta gotten lock");
        	previousMeta = new UpdateMetaTask();
        	previousMeta.execute(args);
        } catch (InterruptedException e) {
        	Log.d("TeaCup", "updateMeta lock acquisition interrupted.");
        }
    }


    private void updatePlaystate(Config config,
                                 Context context,
                                 Intent intent) {
        try {
            String playState = config.getPlayer().getPlaystateChangedPlaying();
            currentlyPlaying = intent.getBooleanExtra(playState, false);
            Bitmap playButton = getPlayButton(currentlyPlaying);
            updateWidgetPlay(playButton);

            UpdateLastFMArgs args = new UpdateLastFMArgs();
            args.config = config;
            args.context = context;
            new UpdateLastFMTask().execute(args);
        } catch (Exception e) {
            Log.e("TeaCup", "Error updating playstate.", e);
        }
    }

    private Bitmap getPlayButton(boolean playing) {
        int imgId = playing ? R.drawable.ic_pause : R.drawable.ic_play;
        return BitmapFactory.decodeResource(getResources(), imgId);
    }

    private void updateWidget(String artist,
                              String title,
                              Bitmap artBmp) {
        AppWidgetManager appWidgetManager
            = AppWidgetManager.getInstance(this);
        RemoteViews views = new RemoteViews(getPackageName(),
                                            R.layout.teacup);

        views.setTextViewText(R.id.artistView, artist);
        views.setTextViewText(R.id.titleView,  title);
        
        TeaCup.addButtonsToRemoteViews(this, views);

        if (artBmp != null)
            views.setImageViewBitmap(R.id.albumArtButton, artBmp);

        ComponentName thisWidget = new ComponentName(this, TeaCup.class);

        appWidgetManager.updateAppWidget(thisWidget, views);
    }


    private void updateWidgetPlay(Bitmap playButton) {
        AppWidgetManager appWidgetManager
            = AppWidgetManager.getInstance(this);
        RemoteViews views = new RemoteViews(getPackageName(),
                                            R.layout.teacup);

        views.setImageViewBitmap(R.id.playPauseButton, playButton);

        TeaCup.addButtonsToRemoteViews(this, views);

        ComponentName thisWidget = new ComponentName(this, TeaCup.class);
        appWidgetManager.updateAppWidget(thisWidget, views);
    }


    private void resetWidget() {
        String artist = getResources().getString(R.string.noartist);
        String title = getResources().getString(R.string.notitle);
        Bitmap artBmp = getDefaultArt(this);

        updateWidget(artist, title, artBmp);
    }

    private Bitmap getDefaultArt(Context context) {
        return BitmapFactory.decodeResource(context.getResources(),
                                            R.drawable.ic_blankalbum);
    }



    private class UpdateMetaArgs {
        Config config;
        Context context;
        Intent intent;
    }


    private class UpdateMetaTask extends AsyncTask<UpdateMetaArgs, Boolean, Void> {

        // signals we've already released the updateMetaLock
        private boolean unlocked = false;

        protected Void doInBackground(UpdateMetaArgs... args) {
            try {
                updateMeta(args[0].config,
                           args[0].context,
                           args[0].intent);
            } catch (Exception e) {
                Log.e("TeaCupReceiver", "Error updating meta.", e);
            }
            return null;
        }

        protected void onPostExecute(Void args) {
            if (!unlocked) {
                Log.d("TeaCup", "releaseing meta lock");
                currentMetaMutex.release();
            }
        }

        protected void onProgressUpdate(Boolean... done) {
            Log.d("TeaCup", "MetaUpdater onProgressUpdate(" +
                            currentMeta.artist + ", " +
                            currentMeta.title + ", " +
                            currentMeta.artBmp);
            if (!isCancelled()) {
                if (currentMeta != null) {
                    updateWidget(currentMeta.artist,
                                 currentMeta.title,
                                 currentMeta.artBmp);
                } else {
                    resetWidget();
                }
            }

            if (!unlocked && (done[0] || isCancelled())) {
                unlocked = true;
                Log.d("TeaCup", "releasing meta lock");
                currentMetaMutex.release();
            }
        }


        private void updateMeta(Config config, Context context, Intent intent) {
            Log.d("TeaCup", "update meta called.");
            currentMeta = getMeta(config, context, intent);
            Log.d("TeaCup", "got meta.");
            if (currentMeta != null) {
                Log.d("TeaCup", "it's not null.");
                publishProgress(true);
                Log.d("TeaCup", "progress published.");
                if (!isCancelled()) {
                    Log.d("TeaCup", "getting art.");
                    Bitmap artBmp = getArtBmp(config, context, currentMeta);
                    Log.d("TeaCup", "gotten and locking.");
                    try {
                    	currentMetaMutex.acquire();
                    	Log.d("TeaCup", "publishing.");
                    	currentMeta.artBmp = artBmp;
                    	publishProgress(true);
                    	Log.d("TeaCup", "published, unlocking.");
                    } catch (InterruptedException e) {
                    	Log.d("TeaCup", "update meta task lock acquisition interrupted.");
                    } finally {
                    	currentMetaMutex.release();
                    	Log.d("TeaCup", "done.");
                    }
                }
            }

            UpdateLastFMArgs args = new UpdateLastFMArgs();
            args.config = config;
            args.context = context;
            new UpdateLastFMTask().execute(args);
        }

        private MetaData getMeta(Config config,
                                 Context context,
                                 Intent intent) {
            String idField = config.getPlayer().getMetaChangedId();
            long id = intent.getLongExtra(idField, INVALID_ID);

            MetaData meta = null;

            if (id  != INVALID_ID) {
                String selectionArgs[] = {
                    Long.toString(id)
                };
                String projection[] = {
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.ALBUM,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.DATA,
                    MediaStore.Audio.Media.DURATION
                };
                String selection = MediaStore.Audio.Media._ID + " = ?";
                ContentResolver resolver = context.getContentResolver();
                Cursor result = resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                               projection,
                                               selection,
                                               selectionArgs,
                                               null);
                if (result.getCount() > 0) {
                    result.moveToFirst();
                    meta = new MetaData();
                    meta.artist = result.getString(0);
                    meta.album = result.getString(1);
                    meta.title = result.getString(2);
                    meta.filename = result.getString(3);
                    meta.length = result.getLong(4);
                }
            }

            return meta;
        }

        private Bitmap getArtBmp(Config config,
                                 Context context,
                                 MetaData meta) {
            Bitmap artBmp = null;

            boolean getEmbeddedArt = config.getEmbeddedArt();
            boolean getDirectoryArt = config.getDirectoryArt();

            if (getEmbeddedArt)
                artBmp = getFileEmbeddedArt(meta.filename);
            if (artBmp == null && getDirectoryArt)
                artBmp = getImageFromDirectory(meta.filename);
            if (artBmp == null) {
                artBmp = LastFM.getArt(context,
                                       config,
                                       meta.artist,
                                       meta.album,
                                       meta.filename);
            }

            if (artBmp == null) {
                artBmp = getDefaultArt(context);
            }

            return artBmp;
        }

        private Bitmap getImageFromDirectory(String filename) {
            Bitmap artBmp = null;

            File file = new File(filename);
            String directory = file.getParent();

            if (directory != null) {
                FileFilter imageFilter = new FileFilter() {
                    boolean found = false;

                    public boolean accept(File file) {
                        if (found) {
                            return false;
                        } else {
                            String filename = file.getName();
                            found = filename.endsWith(".jpg") ||
                                    filename.endsWith(".jpeg") ||
                                    filename.endsWith(".bmp") ||
                                    filename.endsWith(".png") ||
                                    filename.endsWith(".gif");
                            return found;
                        }
                    }
                };

                File[] files = new File(directory).listFiles(imageFilter);
                for (int i = 0; i < files.length && artBmp == null; ++i) {
                    artBmp = AlbumArtFactory.readFile(files[i]);
                }
            }
            return artBmp;
        }


        private Bitmap getFileEmbeddedArt(String filename) {
            Bitmap artBmp = null;

            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(filename);
            byte[] artArray = retriever.getEmbeddedPicture();
            if (artArray != null) {
                artBmp = AlbumArtFactory.readBytes(artArray);
            }

            return artBmp;
        }


    }

    private class UpdateLastFMArgs {
        Config config;
        Context context;
    }

    private class UpdateLastFMTask extends AsyncTask<UpdateLastFMArgs, Void, Void> {

        protected Void doInBackground(UpdateLastFMArgs... args) {
            try {
                String artist = null;
                String title = null;
                long length = 0;

                currentMetaMutex.acquire();
                if (currentMeta != null) {
                    artist = currentMeta.artist;
                    title = currentMeta.title;
                    length = currentMeta.length;
                }
                currentMetaMutex.release();

                if (artist != null && title != null) {
                    LastFM.scrobbleUpdate(args[0].context,
                                          args[0].config,
                                          artist,
                                          title,
                                          length,
                                          currentlyPlaying);
                }
            } catch (Exception e) {
                Log.e("TeaCupReceiver", "Error updating meta.", e);
            }
            return null;
        }
    }

    private class LastFMScrobbleCacheTask extends AsyncTask<UpdateLastFMArgs, Void, Void> {

        protected Void doInBackground(UpdateLastFMArgs... args) {
            try {
                LastFM.scrobbleCache(args[0].config, args[0].context);
            } catch (Exception e) {
                Log.e("TeaCupReceiver", "Error updating meta.", e);
            }
            return null;
        }
    }
}
