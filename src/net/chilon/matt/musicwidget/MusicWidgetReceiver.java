package net.chilon.matt.musicwidget;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;


public class MusicWidgetReceiver extends BroadcastReceiver {
	    @Override
	    public void onReceive(Context context, Intent intent) {
	    	String artist = intent.getStringExtra("artist");
	    	String track = intent.getStringExtra("track");
	    	String info = artist + " / " + track;
	    	
	    	Context appContext = context.getApplicationContext();
	    	AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(appContext);
	    	RemoteViews views = new RemoteViews(appContext.getPackageName(), 
	                                            R.layout.musicwidget);
	    	views.setTextViewText(R.id.infoView, info);
	    	
	    	ComponentName thisWidget = new ComponentName(context, MusicWidget.class);
	    	appWidgetManager.updateAppWidget(thisWidget, views);
	    }

}
