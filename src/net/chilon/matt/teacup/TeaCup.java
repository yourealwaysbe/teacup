/**
 * Copyright 2013 Matthew Hague (matthewhague@zoho.com)
 * Released under the GNU General Public License v3 (see GPL.txt)
 */

package net.chilon.matt.teacup;

import net.chilon.matt.teacup.R;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

public class TeaCup extends AppWidgetProvider {

    public static final String BTN_JUMP_NEXT = "jump-next";
    public static final String BTN_JUMP_PREV = "jump-prev";
    public static final String BTN_PLAY_PAUSE = "play-pause";
    public static final String BTN_ALBUM_ART = "album-art";

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        Log.d("TeaCup", "deleted widget");
        super.onDeleted(context, appWidgetIds);
    }

    @Override
    public void onDisabled(Context context) {
        Log.d("TeaCup", "disabled widget, stopping service");
        ServiceStarter.stopService(context);
        super.onDisabled(context);
    }

    @Override
    public void onEnabled(Context context) {
        Log.d("TeaCup", "enabled widget");
        super.onEnabled(context);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("TeaCup", "widget received");
        String action = intent.getAction();
        Log.d("TeaCup", "got " + action);
        if (action.equals(BTN_JUMP_NEXT)) {
            onClickJumpNext(context, intent);
        } else if (action.equals(BTN_JUMP_PREV)) {
            onClickJumpPrev(context, intent);
        } else if (action.equals(BTN_PLAY_PAUSE)) {
            onClickPlayPause(context, intent);
        } else if (action.equals(BTN_ALBUM_ART)) {
            onClickAlbumArt(context, intent);
        }
        super.onReceive(context, intent);
    }

    @Override
    public void onUpdate(Context context,
                         AppWidgetManager appWidgetManager,
                         int[] appWidgetIds) {
        Log.d("TeaCup", "update called with " + appWidgetIds.length + " ids");
        Config config = new Config(context);
        makeButtons(context, appWidgetManager, appWidgetIds, config);
        ServiceStarter.restartService(context);
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
        PendingIntent albumArt = makePendingIntent(context, BTN_ALBUM_ART);

        remoteViews.setOnClickPendingIntent(R.id.playPauseButton, playPause);
        remoteViews.setOnClickPendingIntent(R.id.jumpNextButton, jumpNext);
        remoteViews.setOnClickPendingIntent(R.id.jumpPrevButton, jumpPrev);
        remoteViews.setOnClickPendingIntent(R.id.albumArtButton, albumArt);

        appWidgetManager.updateAppWidget(appWidgetIds, remoteViews);
    }

    private PendingIntent makePendingIntent(Context context, String command) {
        Intent i = new Intent(context, TeaCup.class);
        i.setAction(command);
        return PendingIntent.getBroadcast(context, 0, i, 0);
    }

    private void onClickAlbumArt(Context context, Intent intent) {
        PlayerConfig player = new Config(context).getPlayer();
        startMusic(context, player);
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
        String playerPackage = player.getPlayerPackage();
        Intent li = context.getPackageManager().getLaunchIntentForPackage(playerPackage);
        if (li != null)
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
