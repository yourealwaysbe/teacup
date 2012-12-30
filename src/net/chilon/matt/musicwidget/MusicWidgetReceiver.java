package net.chilon.matt.musicwidget;

import java.io.FileNotFoundException;
import java.io.InputStream;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.content.CursorLoader;
import android.widget.RemoteViews;


public class MusicWidgetReceiver extends BroadcastReceiver {
	    @Override
	    public void onReceive(Context context, Intent intent) {
	    	String artist = intent.getStringExtra("artist");
	    	String track = intent.getStringExtra("track");
	    	String album = intent.getStringExtra("album");

	    	long id = intent.getLongExtra("id",  -1);

	    	Bitmap artBmp = getArtBmp(context, id);
	    	
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
	    
	    private Bitmap getArtBmp(Context context, long id) {
	    	Bitmap artBmp = null;
	    	
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

}
