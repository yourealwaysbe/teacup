package net.chilon.matt.teacup;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;


public class TeaCupService extends Service {

    public final static String TEACUP_SERVICE = "TeaCupService";

    TeaCupReceiver receiver = null;
    
    public void onCreate() {
    	Log.d("TeaCup", "creating service receiver: " + receiver);
    	makeReceiver();
    }
   

    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("TeaCup", "on start command service");
    	makeReceiver();
        return START_STICKY;
    }

    public void onDestroy() {
    	Log.d("TeaCup", "Destroying service");
        if (receiver != null)
            unregisterReceiver(receiver);
        receiver = null;
    }

    @Override
    public IBinder onBind(Intent arg0) {
        // TODO Auto-generated method stub
        return null;
    }
    
    private void makeReceiver() {
    	if (receiver != null)
    		unregisterReceiver(receiver);
    	
        Config config = new Config(getApplicationContext());
        IntentFilter filter = new IntentFilter();
        filter.addAction(config.getPlayer().getMetaChangedAction());
        filter.addAction(config.getPlayer().getPlaystateChangedAction());
        
        receiver = new TeaCupReceiver();
        registerReceiver(receiver, filter);	
    }
}
