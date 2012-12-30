package net.chilon.matt.musicwidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public class MusicWidget extends AppWidgetProvider {
	
	public static final String PLAY_PAUSE = "play-pause";
	public static final String JUMP_NEXT = "next";
	public static final String JUMP_PREV = "prev";
	public static final String LAUNCH_PLAYER = "launch-player";
	
	private static final String MUSIC_SERVICE_CMD = "com.android.music.musicservicecommand";
	private static final String INTENT_COMMAND = "command";
	private static final String CMD_PLAY_PAUSE = "togglepause";
	private static final String CMD_JUMP_PREV = "previous";
	private static final String CMD_JUMP_NEXT = "next";
	
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
    	String action = intent.getAction();
    	if (action.equals(PLAY_PAUSE)) {
    		onClickPlayPause(context, intent);
    	} else if (action.equals(JUMP_NEXT)) {
    		onClickJumpNext(context, intent);
    	} else if (action.equals(JUMP_PREV)) {
    		onClickJumpPrev(context, intent);
    	} else if (action.equals(LAUNCH_PLAYER)) {
    		onClickLaunchPlayer(context, intent);
    	}
    	super.onReceive(context, intent);
    }

    @Override
    public void onUpdate(Context context, 
                         AppWidgetManager appWidgetManager,
                         int[] appWidgetIds) {
    	
    	RemoteViews remoteViews = new RemoteViews(context.getPackageName(), 
    			                                  R.layout.musicwidget);
    	

    	PendingIntent playPause = makePendingIntent(context, PLAY_PAUSE);
    	PendingIntent jumpNext = makePendingIntent(context, JUMP_NEXT);
    	PendingIntent jumpPrev = makePendingIntent(context, JUMP_PREV);
    	PendingIntent launchPlayer = makePendingIntent(context, LAUNCH_PLAYER);
    	
    	remoteViews.setOnClickPendingIntent(R.id.playPauseButton, playPause);
    	remoteViews.setOnClickPendingIntent(R.id.jumpNextButton, jumpNext);
    	remoteViews.setOnClickPendingIntent(R.id.jumpPrevButton, jumpPrev);
    	remoteViews.setOnClickPendingIntent(R.id.albumArtButton, launchPlayer);
    	
    	appWidgetManager.updateAppWidget(appWidgetIds, remoteViews);
    	
   		super.onUpdate(context, appWidgetManager, appWidgetIds);
    }
    
    public void onClickPlayPause(Context context, Intent intent) {
    	sendCommand(context, CMD_PLAY_PAUSE);
    }
    
    public void onClickJumpNext(Context context, Intent intent) {
    	sendCommand(context, CMD_JUMP_NEXT);
    }
    
    public void onClickJumpPrev(Context context, Intent intent) {
    	sendCommand(context, CMD_JUMP_PREV);
    }
    
    public void onClickLaunchPlayer(Context context, Intent intent) {
    	Intent LaunchIntent = context.getPackageManager().getLaunchIntentForPackage("com.android.music");
    	context.startActivity(LaunchIntent);
    }
    
    private void sendCommand(Context context, String command) {
    	Intent i = new Intent(MUSIC_SERVICE_CMD); 
    	i.putExtra(INTENT_COMMAND, command); 
    	context.sendBroadcast(i);    	
    }
    
    private PendingIntent makePendingIntent(Context context, 
    		                                String action) {
    	Intent i = new Intent(context, MusicWidget.class);
    	i.setAction(action);
    	return PendingIntent.getBroadcast(context, 0, i, 0);
    }
}
