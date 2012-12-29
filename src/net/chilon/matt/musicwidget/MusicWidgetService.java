package net.chilon.matt.musicwidget;

import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.widget.RemoteViews;

public class MusicWidgetService extends Service {
	
    public static final String UPDATE = "update";

	public void onStart(Intent intent, int startId) {
		String command = intent.getAction();
		Bundle extras = intent.getExtras();
		int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID);
        RemoteViews remoteView 
        	= new RemoteViews(getApplicationContext().getPackageName(), 
        	                  R.layout.musicwidget);
        AppWidgetManager appWidgetManager 
        	= AppWidgetManager.getInstance(getApplicationContext());

        //SharedPreferences prefs = getApplicationContext().getSharedPreferences("prefs", 0);
        //long goal = prefs.getLong("goal" + appWidgetId, 0);

        remoteView.setTextViewText(R.id.TextView01, 
        		                   "Hello!" + SystemClock.elapsedRealtime());

        // apply changes to widget
        appWidgetManager.updateAppWidget(appWidgetId, remoteView);
		super.onStart(intent, startId);
	}
	
	public IBinder onBind(Intent intent) {
		return null;
	}
}
