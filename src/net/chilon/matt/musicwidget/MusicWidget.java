package net.chilon.matt.musicwidget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.SystemClock;
import android.widget.RemoteViews;

public class MusicWidget extends AppWidgetProvider {
	
	private int UPDATE_RATE = 1000;
	
    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
    	for (int appWidgetId : appWidgetIds) {      
            setAlarm(context, appWidgetId, -1);
        }
        super.onDeleted(context, appWidgetIds);
    }

    @Override
    public void onDisabled(Context context) {
    	context.stopService(new Intent(context,MusicWidgetService.class));
    	super.onDisabled(context);
    }

    @Override
    public void onEnabled(Context context) {
            super.onEnabled(context);
            //runs when all of the first instance of the widget are placed
            //on the home screen
    }

    @Override
    public void onReceive(Context context, Intent intent) {
            //all the intents get handled by this method
            //mainly used to handle self created intents, which are not
            //handled by any other method
           
           
            //the super call delegates the action to the other methods
           
            //for example the APPWIDGET_UPDATE intent arrives here first
            //and the super call executes the onUpdate in this case
            //so it is even possible to handle the functionality of the
            //other methods here
            //or if you don't call super you can overwrite the standard
            //flow of intent handling
            super.onReceive(context, intent);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
                    int[] appWidgetIds) {
    	//runs on APPWIDGET_UPDATE
        //here is the widget content set, and updated
        //it is called once when the widget created
        //and periodically as set in the metadata xml
           
        //the layout modifications can be done using the AppWidgetManager
        //passed in the parameter, we will discuss it later
           
        //the appWidgetIds contains the Ids of all the widget instances
        //so here you want likely update all of them in an iteration
           
        //we will use only the first creation run
    	for (int appWidgetId : appWidgetIds) {
    		RemoteViews remoteView = new RemoteViews(context.getPackageName(),
    		                                         R.layout.musicwidget);
    		setAlarm(context, appWidgetId, UPDATE_RATE);
    	}
   		super.onUpdate(context, appWidgetManager, appWidgetIds);
    }
    
    
	
	public static PendingIntent makeControlPendingIntent(Context context, 
			                                             String command, 
			                                             int appWidgetId) {
        Intent active = new Intent(context, MusicWidgetService.class);
        active.setAction(command);
        active.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        //this Uri data is to make the PendingIntent unique, so it wont be updated by FLAG_UPDATE_CURRENT
        //so if there are multiple widget instances they wont override each other
        String unique = "musicwidget://widget/id/#" + command + appWidgetId;
        Uri data = Uri.withAppendedPath(Uri.parse(unique),
        		                        String.valueOf(appWidgetId));
        active.setData(data);
        return(PendingIntent.getService(context, 
        		                        0, 
        		                        active, 
        		                        PendingIntent.FLAG_UPDATE_CURRENT));
    }

	
	public static void setAlarm(Context context, int appWidgetId, int updateRate) {
    	PendingIntent newPending 
    		= makeControlPendingIntent(context,
    			                       MusicWidgetService.UPDATE,
    			                       appWidgetId);
    	AlarmManager alarms 
    		= (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        if (updateRate >= 0) {
            alarms.setRepeating(AlarmManager.ELAPSED_REALTIME, 
            		            SystemClock.elapsedRealtime(), 
            		            updateRate, 
            		            newPending);
        } else {
            // on a negative updateRate stop the refreshing
            alarms.cancel(newPending);
        }
    }

	
}
