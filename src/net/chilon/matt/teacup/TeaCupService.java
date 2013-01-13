package net.chilon.matt.teacup;

import java.io.File;
import java.io.FileFilter;

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
import android.os.AsyncTask;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.RemoteViews;


public class TeaCupService extends Service {

    public final static String TEACUP_SERVICE = "TeaCupService";
    
    private static final int INVALID_ID = -1;
    
    private class MetaData {
        String artist;
        String album;
        String title;
        String filename;
    }


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
        
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                ReceiveArgs args = new ReceiveArgs();
                args.context = context;
                args.intent = intent;
                new ReceiveTask().execute(args);
            }	
        };
        registerReceiver(receiver, filter);	
    }
    
    private class ReceiveArgs {
    	Context context;
    	Intent intent;
    }
    
    private class ReceiveTask extends AsyncTask<ReceiveArgs, Void, Void> {

    	protected Void doInBackground(ReceiveArgs... args) {
            try {
                Context context = args[0].context;
                Intent intent = args[0].intent;
            	Log.d("TeaCup", "received: " + intent.getAction());
            	Config config = new Config(context);
            	PlayerConfig player = config.getPlayer();
            	String action = intent.getAction();
            	if (player.getMetaChangedAction().equals(action)) {
            		updateMeta(config, context, intent);
            	}
            	if (player.getPlaystateChangedAction().equals(action)) {
            		updatePlaystate(config, context, intent);
            	}
            	Log.d("TeaCup", "receiver done");
            } catch (Exception e) {
            	Log.e("TeaCupReceiver", e.toString());
            }
            return null;
    	}

        private void updateMeta(Config config,
                                Context context,
                                Intent intent) {
            MetaData meta = getMeta(config, context, intent);
        	
            String artist;
            String title;
            Bitmap artBmp;

            if (meta != null) {
                artBmp = getArtBmp(meta, config, context);
                artist = meta.artist;
                title = meta.title;
            } else {
                artist = context.getResources().getString(R.string.noartist);
                title = context.getResources().getString(R.string.notitle);
                artBmp = getDefaultArt(context);
            }

            updateWidget(context, artist, title, artBmp);
        }

        private void updatePlaystate(Config config,
                                     Context context,
                                     Intent intent) {
            Bitmap playButton = getPlayButton(config, context, intent);
            updateWidget(context, playButton);
        }

        private Bitmap getPlayButton(Config config,
                                     Context context,
                                     Intent intent) {
        	String playState = config.getPlayer().getPlaystateChangedPlaying();
            boolean playing = intent.getBooleanExtra(playState, false);
            int imgId = playing ? R.drawable.ic_pause : R.drawable.ic_play;
            return BitmapFactory.decodeResource(context.getResources(), imgId);
        }

        private Bitmap getArtBmp(MetaData meta,
                                 Config config,
                                 Context context) {
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


        private Bitmap getDefaultArt(Context context) {
            return BitmapFactory.decodeResource(context.getResources(),
                                                R.drawable.ic_blankalbum);
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
                    MediaStore.Audio.Media.DATA
                };
                String selection = MediaStore.Audio.Media._ID + " = ?";
                ContentResolver resolver = getContentResolver();
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
                }
            }

            return meta;
        }

        private void updateWidget(Context context,
                                  String artist,
                                  String title,
                                  Bitmap artBmp) {
            AppWidgetManager appWidgetManager
                = AppWidgetManager.getInstance(context);
            RemoteViews views = new RemoteViews(context.getPackageName(),
                                                R.layout.teacup);

            views.setTextViewText(R.id.artistView, artist);
            views.setTextViewText(R.id.titleView,  title);
            views.setImageViewBitmap(R.id.albumArtButton, artBmp);

            ComponentName thisWidget = new ComponentName(context, TeaCup.class);
            appWidgetManager.updateAppWidget(thisWidget, views);
        }

        private void updateWidget(Context context,
                                  Bitmap playButton) {
            AppWidgetManager appWidgetManager
                = AppWidgetManager.getInstance(context);
            RemoteViews views = new RemoteViews(context.getPackageName(),
                                                R.layout.teacup);

            views.setImageViewBitmap(R.id.playPauseButton, playButton);

            ComponentName thisWidget = new ComponentName(context, TeaCup.class);
            appWidgetManager.updateAppWidget(thisWidget, views);
        }
    }    
}
