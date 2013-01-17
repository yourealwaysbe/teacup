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
        long length;
    }

    private boolean currentlyPlaying = false;
    private String currentArtist = null;
    private String currentTitle = null;
    private long currentLength = 0;


    BroadcastReceiver receiver = null;
   
	private static UpdateArtTask previousArt = null;
	
    
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
            }
        };
        registerReceiver(receiver, filter);
    }


    private void updateMeta(Config config,
                            Context context,
                            Intent intent) {
        MetaData meta = getMeta(config, context, intent);

        if (previousArt != null) 
        	previousArt.cancel(true);

        if (meta != null) {
            UpdateArtArgs artArgs = new UpdateArtArgs();
            artArgs.config = config;
            artArgs.context = context;
            artArgs.meta = meta;
            previousArt = new UpdateArtTask();
            previousArt.execute(artArgs);

            updateWidget(meta.artist, meta.title);

            UpdateLastFMArgs args = new UpdateLastFMArgs();
            args.config = config;
            args.context = context;
            args.artist = meta.artist;
            args.title = meta.title;
            args.currentlyPlaying = currentlyPlaying;
            args.length = meta.length;
            new UpdateLastFMTask().execute(args);

            currentArtist = meta.artist;
            currentTitle = meta.title;
            currentLength = meta.length;
        } else {
            String artist = context.getResources().getString(R.string.noartist);
            String title = context.getResources().getString(R.string.notitle);
            updateWidget(artist, title);

            Bitmap artBmp = getDefaultArt(context);
            updateWidgetArt(artBmp);

            currentArtist = null;
            currentTitle = null;
            currentLength = 0;
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
    		
    		if (currentArtist != null && 
    			currentTitle != null) {
    			UpdateLastFMArgs args = new UpdateLastFMArgs();
    			args.config = config;
    			args.context = context;
    			args.artist = currentArtist;
    			args.title = currentTitle;
    			args.currentlyPlaying = currentlyPlaying;
    			args.length = currentLength;
    			new UpdateLastFMTask().execute(args);
    		}
    	} catch (Exception e) {
    		Log.e("TeaCup", "Error updating playstate.", e);
    	}
    }
    
    private Bitmap getPlayButton(boolean playing) {
    	int imgId = playing ? R.drawable.ic_pause : R.drawable.ic_play;
    	return BitmapFactory.decodeResource(getResources(), imgId);
    }

    private void updateWidget(String artist,
                              String title) {
        AppWidgetManager appWidgetManager
            = AppWidgetManager.getInstance(this);
        RemoteViews views = new RemoteViews(getPackageName(),
                                            R.layout.teacup);

        views.setTextViewText(R.id.artistView, artist);
        views.setTextViewText(R.id.titleView,  title);

        ComponentName thisWidget = new ComponentName(this, TeaCup.class);
        
        appWidgetManager.updateAppWidget(thisWidget, views);
    }

    private void updateWidgetArt(Bitmap artBmp) {
        AppWidgetManager appWidgetManager
            = AppWidgetManager.getInstance(this);
        RemoteViews views = new RemoteViews(getPackageName(),
                                            R.layout.teacup);

        if (artBmp != null)
            views.setImageViewBitmap(R.id.albumArtButton, artBmp);

        ComponentName thisWidget = new ComponentName(this, TeaCup.class);
        
        appWidgetManager.updateAppWidget(thisWidget, views);
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
                meta.length = result.getLong(4);
            }
        }

        return meta;
    }


    
    private void updateWidgetPlay(Bitmap playButton) {
    	AppWidgetManager appWidgetManager
    		= AppWidgetManager.getInstance(this);
    	RemoteViews views = new RemoteViews(getPackageName(),
                                            R.layout.teacup);

    	views.setImageViewBitmap(R.id.playPauseButton, playButton);

    	ComponentName thisWidget = new ComponentName(this, TeaCup.class);
    	appWidgetManager.updateAppWidget(thisWidget, views);
    }

    private Bitmap getDefaultArt(Context context) {
        return BitmapFactory.decodeResource(context.getResources(),
                                            R.drawable.ic_blankalbum);
    }


    
    private class UpdateArtArgs {
    	Config config;
        Context context;
        MetaData meta;
    }


    private class UpdateArtTask extends AsyncTask<UpdateArtArgs, Void, Void> {

    	protected Void doInBackground(UpdateArtArgs... args) {
    		try {
            	updateArt(args[0].config,
                          args[0].context,
            			  args[0].meta);
            } catch (Exception e) {
            	Log.e("TeaCupReceiver", "Error updating meta.", e);
            }
            return null;
    	}

        private void updateArt(Config config,
                               Context context,
                               MetaData meta) {
            Bitmap artBmp = null;

            artBmp = getArtBmp(config, context, meta);

            if (!isCancelled()) {
            	updateWidgetArt(artBmp);
            }
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
    	String artist;
    	String title;
    	long length;
    	boolean currentlyPlaying;
    }

    private class UpdateLastFMTask extends AsyncTask<UpdateLastFMArgs, Void, Void> {

    	protected Void doInBackground(UpdateLastFMArgs... args) {
    		try {
    			LastFM.scrobbleUpdate(args[0].context,
                                      args[0].config,
                                      args[0].artist, 
                                      args[0].title, 
                                      args[0].length,
                                      args[0].currentlyPlaying);            	
            } catch (Exception e) {
            	Log.e("TeaCupReceiver", "Error updating meta.", e);
            }
            return null;
    	}
    }
}
