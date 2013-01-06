package net.chilon.matt.teacup;

import net.chilon.matt.teacup.R;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public class TeaCup extends AppWidgetProvider {
	
	public static final String BTN_JUMP_NEXT = "jump-next";
	public static final String BTN_JUMP_PREV = "jump-prev";
	public static final String BTN_PLAY_PAUSE = "play-pause";
	
    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
    }

    @Override
    public void onDisabled(Context context) {
    	Intent i = new Intent(TeaCupService.TEACUP_SERVICE);
    	i.setClass(context, TeaCupService.class);
    	context.stopService(i);
    	super.onDisabled(context);
    }

    @Override
    public void onEnabled(Context context) {
    	super.onEnabled(context);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
    	String action = intent.getAction();
    	if (action.equals(BTN_JUMP_NEXT)) {
    		onClickJumpNext(context, intent);
    	} else if (action.equals(BTN_JUMP_PREV)) {
    		onClickJumpPrev(context, intent);
    	} else if (action.equals(BTN_PLAY_PAUSE)) {
    		onClickPlayPause(context, intent);
    	}
    	super.onReceive(context, intent);
    }

    @Override
    public void onUpdate(Context context, 
                         AppWidgetManager appWidgetManager,
                         int[] appWidgetIds) {
    	
    	Config config = new Config(context);
    	
    	makeButtons(context, appWidgetManager, appWidgetIds, config);
    	
   		super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    
    private void makeButtons(Context context, 
    		                 AppWidgetManager appWidgetManager,
    		                 int[] appWidgetIds,
    		                 Config config) {
    	RemoteViews remoteViews = new RemoteViews(context.getPackageName(), 
    			                                  R.layout.teacup);
    	
    	PendingIntent playPause = makePendingIntent(context, BTN_PLAY_PAUSE);
    	PendingIntent jumpNext = makePendingIntent(context, BTN_JUMP_NEXT);
    	PendingIntent jumpPrev = makePendingIntent(context, BTN_JUMP_PREV);
    	PendingIntent launchPlayer = makeLaunchPendingIntent(context, config);
    	
    	remoteViews.setOnClickPendingIntent(R.id.playPauseButton, playPause);
    	remoteViews.setOnClickPendingIntent(R.id.jumpNextButton, jumpNext);
    	remoteViews.setOnClickPendingIntent(R.id.jumpPrevButton, jumpPrev);
    	remoteViews.setOnClickPendingIntent(R.id.albumArtButton, launchPlayer);
    	
    	appWidgetManager.updateAppWidget(appWidgetIds, remoteViews);	
    }
        
    private PendingIntent makePendingIntent(Context context, String command) {
    	Intent i = new Intent(context, TeaCup.class);
    	i.setAction(command);
    	return PendingIntent.getBroadcast(context, 0, i, 0);
    }
    
    private PendingIntent makeLaunchPendingIntent(Context context, Config config) {
    	String musicPackage = config.getPlayer().getPlayerPackage();
    	Intent li = context.getPackageManager().getLaunchIntentForPackage(musicPackage);
    	return PendingIntent.getActivity(context, 0, li, 0);
    }
    
    private void onClickJumpNext(Context context, Intent intent) {
		PlayerConfig player = new Config(context).getPlayer();
    	if (!isMusicRunning(context, player)) {
    		startMusic(context, player);
    	} else {
    		sendCommand(context,
    				    player.getJumpNextAction(),
    				    player.getJumpNextCommandField(),
    				    player.getJumpNextCommand());
    	}
    }
    
    private void onClickJumpPrev(Context context, Intent intent) {
		PlayerConfig player = new Config(context).getPlayer();
    	if (!isMusicRunning(context, player)) {
    		startMusic(context, player);
    	} else {
    		sendCommand(context,
    				    player.getJumpPreviousAction(),
    				    player.getJumpPreviousCommandField(),
    				    player.getJumpPreviousCommand());
    	}
    }
    
    private void onClickPlayPause(Context context, Intent intent) {
		PlayerConfig player = new Config(context).getPlayer();
    	if (!isMusicRunning(context, player)) {
    		startMusic(context, player);
    	} else {
    		sendCommand(context,
    				    player.getPlayPauseAction(),
    				    player.getPlayPauseCommandField(),
    				    player.getPlayPauseCommand());
    	}
    }
    
    private void sendCommand(Context context, 
    		                 String action, 
    		                 String field, 
    		                 String cmd) {
		Intent i = new Intent(action);
		i.putExtra(field, cmd);
		context.sendBroadcast(i);
    }
        
    private void startMusic(Context context, PlayerConfig player) {
    	Intent li = context.getPackageManager().getLaunchIntentForPackage(player.getPlayerPackage());
    	context.startActivity(li);
    }
    
    
    private boolean isMusicRunning(Context context, PlayerConfig player) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        String playerPackage = player.getPlayerPackage();
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (playerPackage.equals(service.service.getPackageName())) {
                return true;
            }
        }
        return false;
    }
    

}