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
   
	private static UpdateMetaTask previousMeta = null;
	
    
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
                    UpdateMetaArgs args = new UpdateMetaArgs();
                    args.config = config;
                    args.context = context;
                    args.intent = intent;
                    args.currentlyPlaying = currentlyPlaying;
                	if (previousMeta != null) 
                		previousMeta.cancel(true);
                	previousMeta = new UpdateMetaTask();
                	previousMeta.execute(args);
                }
                	
                if (player.getPlaystateChangedAction().equals(action)) {
                	updatePlaystate(config, context, intent);
                }                
            }
        };
        registerReceiver(receiver, filter);
    }


    private void updatePlaystate(Config config,
                                 Context context,
                                 Intent intent) {
    	try {
            String playState = config.getPlayer().getPlaystateChangedPlaying();
            boolean playing = intent.getBooleanExtra(playState, false);
    		Bitmap playButton = getPlayButton(currentlyPlaying);
    		updateWidget(playButton);
    		
    		if (currentArtist != null && 
    			currentTitle != null &&
    			currentlyPlaying != playing) {
    			UpdateLastFMArgs args = new UpdateLastFMArgs();
    			args.config = config;
    			args.context = context;
    			args.artist = currentArtist;
    			args.title = currentTitle;
    			args.currentlyPlaying = playing;
    			args.length = currentLength;
    			new UpdateLastFMTask().execute(args);
    		}
    		
    		currentlyPlaying = playing;
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
        if (artBmp != null)
            views.setImageViewBitmap(R.id.albumArtButton, artBmp);

        ComponentName thisWidget = new ComponentName(this, TeaCup.class);
        
        appWidgetManager.updateAppWidget(thisWidget, views);
    }

    
    private void updateWidget(Bitmap playButton) {
    	AppWidgetManager appWidgetManager
    		= AppWidgetManager.getInstance(this);
    	RemoteViews views = new RemoteViews(getPackageName(),
                                            R.layout.teacup);

    	views.setImageViewBitmap(R.id.playPauseButton, playButton);

    	ComponentName thisWidget = new ComponentName(this, TeaCup.class);
    	appWidgetManager.updateAppWidget(thisWidget, views);
    }

    
    private class UpdateMetaArgs {
    	Config config;
    	Context context;
    	Intent intent;
    	boolean currentlyPlaying;
    }


    private class UpdateInfo {
        String artist = null;
        String title = null;
        long length = 0;
        Bitmap art = null;
    }

    private class UpdateMetaTask extends AsyncTask<UpdateMetaArgs, UpdateInfo, Void> {

    	protected Void doInBackground(UpdateMetaArgs... args) {
    		try {
            	updateMeta(args[0].config, 
            			   args[0].context, 
            			   args[0].intent,
            			   args[0].currentlyPlaying);
            } catch (Exception e) {
            	Log.e("TeaCupReceiver", "Error updating meta.", e);
            }
            return null;
    	}

    	protected void onProgressUpdate(UpdateInfo... info) {
            // better if we used a later API where we can guarantee serial in order 
            // execution of the async task, but just this check should be good enough
            if (!isCancelled()) {
            	currentArtist = info[0].artist;
            	currentTitle = info[0].title;
            	currentLength = info[0].length;
            	updateWidget(info[0].artist, 
            			     info[0].title,
            			     info[0].art);
            }
        }


        private void updateMeta(Config config,
                                Context context,
                                Intent intent, 
                                boolean currentlyPlaying) {
            MetaData meta = getMeta(config, context, intent);
        	UpdateInfo info = new UpdateInfo();

            if (meta != null) {
            	info.artist = meta.artist;
                info.title = meta.title;
                info.length = meta.length;
            } else {
                info.artist = context.getResources().getString(R.string.noartist);
                info.title = context.getResources().getString(R.string.notitle);
                info.length = 0;
            }
            
            publishProgress(info);
            
            if (meta != null) {
                info.art = getArtBmp(meta, config, context);
            } else {
            	info.art = getDefaultArt(context);
            }
            
            publishProgress(info);

            if (meta != null) {
                LastFM.scrobbleUpdate(context,
                                      config,
                                      meta.artist, 
                                      meta.title, 
                                      meta.length,
                                      currentlyPlaying);
            }
            
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
