package net.chilon.matt.teacup;

import java.io.File;
import java.io.FileFilter;

import net.chilon.matt.teacup.R;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.provider.MediaStore;
import android.support.v4.content.CursorLoader;
import android.view.LayoutInflater;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.view.View;


public class TeaCupReceiver extends BroadcastReceiver {
	
	private static final String INTENT_TRACK_ID = "id";
	private static final String INTENT_ARTIST = "artist";
	private static final String INTENT_TRACK = "track";
	private static final String INTENT_PLAYING = "playing";
	
	private static final int ART_WIDTH = 72;
	private static final int ART_HEIGHT = 72;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		String artist = getArtist(context, intent);
		String track = getTrack(context, intent);
	    Bitmap artBmp = getArtBmp(context, intent);
	    Bitmap playButton = getPlayButton(context, intent);
	    
	    updateWidget(context, artist, track, playButton, artBmp);
	}
	
	private Bitmap getPlayButton(Context context, Intent intent) {
		boolean playing = intent.getBooleanExtra(INTENT_PLAYING, false);
		int imgId = playing ? R.drawable.ic_pause : R.drawable.ic_play;
		return BitmapFactory.decodeResource(context.getResources(), 
                                            imgId);
	}
	
	private String getArtist(Context context, Intent intent) {
		String artist = intent.getStringExtra(INTENT_ARTIST);
		if (artist == null) {
			artist = context.getResources().getString(R.string.noartist);
		}
		return artist;
	}

	private String getTrack(Context context, Intent intent) {
		String track = intent.getStringExtra(INTENT_TRACK);
		if (track == null) {
			track = context.getResources().getString(R.string.notrack);
		}
		return track;
	}
	
	private Bitmap getArtBmp(Context context, Intent intent) {
		Bitmap artBmp = null;
		
		String playingFilename = getPlayingFilename(context, intent);
	    	
	    if (playingFilename != null) {
	    	artBmp = getFileEmbeddedArt(playingFilename);
	    	if (artBmp == null) {
	    		artBmp = getImageFromDirectory(playingFilename);
	    	}
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
				public boolean accept(File file) {
					String filename = file.getName();
					return filename.endsWith(".jpg") ||
						   filename.endsWith(".jpeg") ||
						   filename.endsWith(".bmp") ||
						   filename.endsWith(".png") ||
						   filename.endsWith(".gif");
				}
			};
	    
			File[] files = new File(directory).listFiles(imageFilter);
			for (int i = 0; i < files.length && artBmp == null; ++i) {
				artBmp = decodeSampledBitmap(files[i],  
						                     ART_WIDTH, 
						                     ART_HEIGHT);
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
			artBmp = decodeSampledBitmapFromBytes(artArray,
					                              ART_WIDTH,
					                              ART_HEIGHT);
		}
		
		return artBmp;
	}
	
	private String getPlayingFilename(Context context, Intent intent) {
	    long id = intent.getLongExtra(INTENT_TRACK_ID, -1);
    	
	    String filename = null;
	    
	    if (id  >= 0) {
	    	String selectionArgs[] = {
	    		Long.toString(id)
	    	};
	    	String projection[] = {
	    		MediaStore.Audio.Media.DATA
	    	};
	    	String selection = MediaStore.Audio.Media._ID + " = ?";
	    	CursorLoader q = new CursorLoader(context,
	    				                      MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
	    				                      projection,
	    				                      selection,
	    				                      selectionArgs,
	    				                      null);
	    	Cursor result = q.loadInBackground();
	    	if (result.getCount() > 0) {
	    		result.moveToFirst();
	    		filename = result.getString(0);	
	    	}
	    }
	    
	    return filename;
	}
	
	private void updateWidget(Context context,
			                  String artist, 
			                  String track,
			                  Bitmap playButton,
			                  Bitmap artBmp) {
	    Context appContext = context.getApplicationContext();
	    AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(appContext);
	    RemoteViews views = new RemoteViews(appContext.getPackageName(), 
	                                        R.layout.teacup);
	   	
	    views.setTextViewText(R.id.artistView, artist);
	    views.setTextViewText(R.id.trackView,  track);
	    views.setImageViewBitmap(R.id.albumArtButton, artBmp);
	    views.setImageViewBitmap(R.id.playPauseButton, playButton);
	    
	    ComponentName thisWidget = new ComponentName(context, TeaCup.class);
	    appWidgetManager.updateAppWidget(thisWidget, views);
	}
	
	// code below from android tutorial:
	// https://developer.android.com/training/displaying-bitmaps/load-bitmap.html

	private static Bitmap decodeSampledBitmap(File file,
	                                          int reqWidth, 
	                                          int reqHeight) {
		String path = file.getPath();
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(path, options);

		options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
		options.inJustDecodeBounds = false;
		options.inPurgeable = true;
		return BitmapFactory.decodeFile(path, options);
	}
	
	private static Bitmap decodeSampledBitmapFromBytes(byte[] data,
                                                       int reqWidth, 
                                                       int reqHeight) {
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeByteArray(data, 0, data.length);
		options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
		options.inJustDecodeBounds = false;
		options.inPurgeable = true;
		return BitmapFactory.decodeByteArray(data, 0, data.length);
	}

	public static int calculateInSampleSize(BitmapFactory.Options options, 
                                            int reqWidth, 
                                            int reqHeight) {
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth) {
			if (width > height) {
				inSampleSize = Math.round((float)height / (float)reqHeight);
			} else {
				inSampleSize = Math.round((float)width / (float)reqWidth);
			}
		}
		
		return inSampleSize;
	}

	
}
