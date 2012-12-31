package net.chilon.matt.teacup;

import net.chilon.matt.teacup.R;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class TeaCupConfiguration extends Activity {
    private Context self = this;

	private int teaCupId;
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Intent launchIntent = getIntent();
		Bundle extras = launchIntent.getExtras();
		teaCupId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
		                              AppWidgetManager.INVALID_APPWIDGET_ID);
		
		Intent cancelResultValue = new Intent();
		cancelResultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 
		                           teaCupId);
		setResult(RESULT_CANCELED, cancelResultValue);
		
		setContentView(R.layout.configuration);
		
		Button ok = (Button) findViewById(R.id.okbutton);
		ok.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				Intent resultValue = new Intent();
				resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
				                     teaCupId);
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
