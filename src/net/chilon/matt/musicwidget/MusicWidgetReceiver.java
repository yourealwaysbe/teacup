package net.chilon.matt.musicwidget;

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
import android.widget.RemoteViews;


public class MusicWidgetReceiver extends BroadcastReceiver {
	
	private static final String INTENT_TRACK_ID = "id";
	private static final String INTENT_ARTIST = "artist";
	private static final String INTENT_TRACK = "track";
	
	@Override
	public void onReceive(Context context, Intent intent) {
		String artist = getArtist(context, intent);
		String track = getTrack(context, intent);
	    Bitmap artBmp = getArtBmp(context, intent);
	    
	    updateWidget(context, artist, track, artBmp);
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
	    	
	    long id = intent.getLongExtra(INTENT_TRACK_ID, -1);
	    	
	    if (id  >= 0) {
	    	String selectionArgs[] = {
	    		new Long(id).toString()
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
	    		String fileName = result.getString(0);
	    		MediaMetadataRetriever retriever = new MediaMetadataRetriever();
	    		retriever.setDataSource(fileName);
	    		byte[] artArray = retriever.getEmbeddedPicture();
	    		if (artArray != null) {
	    			artBmp = BitmapFactory.decodeByteArray(artArray, 
	    					                               0, 
	    					                               artArray.length);
	    		}
	    	}
	    }
	    	
	    if (artBmp == null) {
	    	artBmp = BitmapFactory.decodeResource(context.getResources(), 
	    				                          R.drawable.ic_blankalbum);
	    }

	    return artBmp;	
	}
	
	private void updateWidget(Context context,
			                  String artist, 
			                  String track, 
			                  Bitmap artBmp) {
	    Context appContext = context.getApplicationContext();
	    AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(appContext);
	    RemoteViews views = new RemoteViews(appContext.getPackageName(), 
	                                        R.layout.musicwidget);
	   	
	    views.setTextViewText(R.id.artistView, artist);
	    views.setTextViewText(R.id.trackView,  track);
	    views.setImageViewBitmap(R.id.albumArtView, artBmp);
	    	
	    ComponentName thisWidget = new ComponentName(context, MusicWidget.class);
	    appWidgetManager.updateAppWidget(thisWidget, views);
	}

}
