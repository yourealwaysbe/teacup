package net.chilon.matt.musicwidget;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class MusicWidgetConfiguration extends Activity {
    private Context self = this;

	private int musicWidgetId;
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Intent launchIntent = getIntent();
		Bundle extras = launchIntent.getExtras();
		musicWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
		                              AppWidgetManager.INVALID_APPWIDGET_ID);
		
		Intent cancelResultValue = new Intent();
		cancelResultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 
		                           musicWidgetId);
		setResult(RESULT_CANCELED, cancelResultValue);
		
		setContentView(R.layout.configuration);
		
		Button ok = (Button) findViewById(R.id.okbutton);
		ok.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				Intent resultValue = new Intent();
				resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
				                     musicWidgetId);
				setResult(RESULT_OK, resultValue);
				finish();
				
                // SharedPreferences prefs = self.getSharedPreferences("prefs", 0);
                // SharedPreferences.Editor edit = prefs.edit();
                // edit.putLong("goal" + appWidgetId, date.getTime());
                // edit.commit();
			}
		});
	}

}
