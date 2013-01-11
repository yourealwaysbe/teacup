package net.chilon.matt.teacup;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import android.util.Log;


public class TeaCupReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
    	System.out.println("received: " + intent.getAction());
    	try {
    		Config config = new Config(context);
    		PlayerConfig player = config.getPlayer();
    		String action = intent.getAction();
    		if (player.getMetaChangedAction().equals(action)) {
    			updateMeta(config, context, intent);
    		}
    		if (player.getPlaystateChangedAction().equals(action)) {
    			updatePlaystate(config, context, intent);
    		}
    	} catch (Exception e) {
    		System.out.println("Lofi error reporting: " + e);
    		Log.e("TeaCupReceiver", e.toString());
    	}
    }
    
    private void updateMeta(Config config, Context context, Intent intent) {
    	String idField = config.getPlayer().getMetaChangedId();
    	long id = intent.getLongExtra(idField, TeaCupUpdater.INVALID_ID);
    	
    	Intent tcIntent = new Intent(context.getApplicationContext(),  TeaCupUpdater.class);
    	tcIntent.setAction(TeaCupUpdater.UPDATE_META);
    	tcIntent.putExtra(TeaCupUpdater.ID, id);
    	
    	context.startService(tcIntent);
    }

    private void updatePlaystate(Config config, 
    		                     Context context,
    		                     Intent intent) {
    	String playstateField = config.getPlayer().getPlaystateChangedPlaying();
    	boolean playing = intent.getBooleanExtra(playstateField, false);
    	
    	Context appContext = context.getApplicationContext();
    	
    	Intent tcIntent = new Intent(appContext,  TeaCupUpdater.class);
    	tcIntent.setAction(TeaCupUpdater.UPDATE_PLAYSTATE);
    	tcIntent.putExtra(TeaCupUpdater.PLAYSTATE, playing);
    	
    	appContext.startService(tcIntent);   
    }
}
