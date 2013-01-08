package net.chilon.matt.teacup;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;


public class TeaCupService extends Service {

    public final static String TEACUP_SERVICE = "TeaCupService";

    TeaCupReceiver receiver = null;

    public void onStart(Intent intent, int startId) {
        Config config = new Config(getApplicationContext());
        IntentFilter filter = new IntentFilter();
        filter.addAction(config.getPlayer().getMetaChangedAction());
        filter.addAction(config.getPlayer().getPlaystateChangedAction());
        receiver = new TeaCupReceiver();
        getApplicationContext().registerReceiver(receiver, filter);
    }

    public void onDestroy() {
        if (receiver != null)
            getApplicationContext().unregisterReceiver(receiver);
    }

    @Override
    public IBinder onBind(Intent arg0) {
        // TODO Auto-generated method stub
        return null;
    }
}
