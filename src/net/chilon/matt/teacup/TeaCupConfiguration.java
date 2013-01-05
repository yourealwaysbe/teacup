package net.chilon.matt.teacup;

import net.chilon.matt.teacup.R;
import android.app.Activity;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;

public class TeaCupConfiguration extends Activity {
	
    private TeaCupConfiguration self = this;

	private int teaCupId;
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Intent launchIntent = getIntent();
		Bundle extras = launchIntent.getExtras();
		teaCupId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
		                         AppWidgetManager.INVALID_APPWIDGET_ID);
				
		setContentView(R.layout.configuration);
		
		Config config = new Config((Context)this);
		config.writeConfigToActivity(this);
		
		Button ok = (Button) findViewById(R.id.okbutton);
		ok.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				Intent resultValue = new Intent();
				resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
				                     teaCupId);

                Config config = new Config(self);
                config.writeConfigToSharedPreferences(self);

                restartService(getApplicationContext());
                
				setResult(RESULT_OK, resultValue);
				finish();
			}
		});
		
		Button cancel = (Button) findViewById(R.id.cancelbutton);
		cancel.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				Intent resultValue = new Intent();
				resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
				                     teaCupId);
				
				setResult(RESULT_CANCELED, resultValue);
				finish();
			}
		});
	}
	

    private void restartService(Context context) {
		Intent i = new Intent(TeaCupService.TEACUP_SERVICE);
	    i.setClass(context, TeaCupService.class);
	    context.stopService(i);
	    context.startService(i);	
    }
	
}