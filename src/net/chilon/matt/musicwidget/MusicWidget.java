package net.chilon.matt.musicwidget;

import java.util.HashSet;
import java.util.Set;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.TextView;

public class MusicWidget extends AppWidgetProvider {
	
	private static final Set<String> musicActions = initMusicActions();
	private static Set<String> initMusicActions() {
		Set<String> musicActions = new HashSet<String>(); 
        musicActions.add("com.android.music.metachanged");
        musicActions.add("com.android.music.playstatechanged");
        musicActions.add("com.android.music.playbackcomplete");
        musicActions.add("com.android.music.queuechanged");
        musicActions.add("com.htc.music.metachanged");
        musicActions.add("fm.last.android.metachanged");
        musicActions.add("com.sec.android.app.music.metachanged");
        musicActions.add("com.nullsoft.winamp.metachanged");
        musicActions.add("com.amazon.mp3.metachanged");     
        musicActions.add("com.miui.player.metachanged");        
        musicActions.add("com.real.IMP.metachanged");
        musicActions.add("com.sonyericsson.music.metachanged");
        musicActions.add("com.rdio.android.metachanged");
        musicActions.add("com.samsung.sec.android.MusicPlayer.metachanged");
        musicActions.add("com.andrew.apollo.metachanged");
		return musicActions;
	};

	private static Set<Integer> widgetIds =  new HashSet<Integer>();
	
    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
    	for (int i = 0; i < appWidgetIds.length; ++i) {
    		widgetIds.remove(appWidgetIds[i]);
    	}
        super.onDeleted(context, appWidgetIds);
    }

    @Override
    public void onDisabled(Context context) {
    	super.onDisabled(context);
    }

    @Override
    public void onEnabled(Context context) {
    	System.out.println("Why are you doing this now?");
            super.onEnabled(context);
            //runs when all of the first instance of the widget are placed
            //on the home screen
    }

    @Override
    public void onReceive(Context context, Intent intent) {
    	if (musicActions.contains(intent.getAction())) {
    		onUpdateMusicInfo(context, intent);
    	}
    	
    	super.onReceive(context, intent);
    }

    @Override
    public void onUpdate(Context context, 
                         AppWidgetManager appWidgetManager,
                         int[] appWidgetIds) {
    	for (int i = 0; i < appWidgetIds.length; ++i) {
    		widgetIds.add(appWidgetIds[i]);
    	}
   		super.onUpdate(context, appWidgetManager, appWidgetIds);
    }	
    
    private void onUpdateMusicInfo(Context context, Intent intent) {
    	String artist = intent.getStringExtra("artist");
    	String album = intent.getStringExtra("album");
    	String track = intent.getStringExtra("track");
    	
    	String info = track + " : " +
    			      artist + " / " +
    	              album;
    	
    	Context appContext = context.getApplicationContext();
    	AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(appContext);
    	RemoteViews views = new RemoteViews(appContext.getPackageName(), 
                                            R.layout.musicwidget);
    	views.setTextViewText(R.id.infoView, info);
    	
    	for (int id : widgetIds) {
    		appWidgetManager.updateAppWidget(id, views);
    	}
    }
}
