package net.chilon.matt.musicwidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;

public class MusicWidget extends AppWidgetProvider {
	
	public static final String PLAY_PAUSE = "play-pause";

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
    }

    @Override
    public void onDisabled(Context context) {
    	super.onDisabled(context);
    }

    @Override
    public void onEnabled(Context context) {
    	super.onEnabled(context);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
    	if (intent.getAction().equals(PLAY_PAUSE)) {
    		onClickPlayPause(context, intent);
    	}
    	super.onReceive(context, intent);
    }

    @Override
    public void onUpdate(Context context, 
                         AppWidgetManager appWidgetManager,
                         int[] appWidgetIds) {
    	
    	RemoteViews remoteViews = new RemoteViews(context.getPackageName(), 
    			                                  R.layout.musicwidget);
    	
    	Intent active = new Intent(context, MusicWidget.class);
    	active.setAction(PLAY_PAUSE);
    	PendingIntent playPausePendingIntent 
    		= PendingIntent.getBroadcast(context, 0, active, 0);
    	
    	remoteViews.setOnClickPendingIntent(R.id.playPauseButton, playPausePendingIntent);
    	
    	appWidgetManager.updateAppWidget(appWidgetIds, remoteViews);
    	
   		super.onUpdate(context, appWidgetManager, appWidgetIds);
    }
    
    public void onClickPlayPause(Context context, Intent intent) {
    	System.out.println("Did click indeed");
    	Intent LaunchIntent = context.getPackageManager().getLaunchIntentForPackage("com.android.music");
    	context.startActivity(LaunchIntent);
    }
}
