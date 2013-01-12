package net.chilon.matt.teacup;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ServiceStarter extends BroadcastReceiver {

    public void onReceive(Context context, Intent intext) {
            restartService(context);
    }

    static public void restartService(Context context) {
        Intent i = new Intent(TeaCupService.TEACUP_SERVICE);
        i.setClass(context, TeaCupService.class);
        context.stopService(i);

        Log.d("TeaCup", "Restart requested");
        
        // only start if there are widgets to receive
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName provider = new ComponentName(context, TeaCup.class);
        if (manager.getAppWidgetIds(provider).length > 0)
            context.startService(i);
        else
        	Log.d("TeaCup", "but there are no widgets...");
    }

    static public void stopService(Context context) {
    	Log.d("TeaCup", "stopping service");
        Intent i = new Intent(TeaCupService.TEACUP_SERVICE);
        i.setClass(context, TeaCupService.class);
        context.stopService(i);
    }
}
