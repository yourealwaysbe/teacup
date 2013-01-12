package net.chilon.matt.teacup;

import net.chilon.matt.teacup.R;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

public class TeaCupConfiguration extends Activity {

    private TeaCupConfiguration self = this;

    private int teaCupId;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent launchIntent = getIntent();
        Bundle extras = launchIntent.getExtras();
        if (extras != null) {
            teaCupId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                                     AppWidgetManager.INVALID_APPWIDGET_ID);
        } else {
            teaCupId = AppWidgetManager.INVALID_APPWIDGET_ID;
        }

        setContentView(R.layout.configuration);

        Config config = new Config((Context)this);
        config.writeConfigToActivity(this);

        showHideCustomOptions(config.getPlayer().getPlayerId());
        adjustLastFMVisibility();
        
        Button ok = (Button) findViewById(R.id.okbutton);
        ok.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                Config config = new Config(self);
                config.writeConfigToSharedPreferences(self);

                ServiceStarter.restartService(getApplicationContext());
                
                Intent resultValue = new Intent();
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                                     teaCupId);
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

    public void onClickSelectPlayer(View view) {
        RadioGroup playerSelect = (RadioGroup) findViewById(R.id.selectPlayerRadioGroup);
        if (playerSelect.getVisibility() == View.VISIBLE)
        	playerSelect.setVisibility(View.GONE);
        else
        	playerSelect.setVisibility(View.VISIBLE);
    }

    public void onClickPlayerSelectRadioGroup(View view) {
        RadioGroup playerSelect = (RadioGroup) findViewById(R.id.selectPlayerRadioGroup);
        playerSelect.setVisibility(View.GONE);

        int checkedId = playerSelect.getCheckedRadioButtonId();
        RadioButton checkedButton = (RadioButton) findViewById(checkedId);

        TextView playerSelected = (TextView) findViewById(R.id.playerSelected);
        playerSelected.setText(checkedButton.getText());

        showHideCustomOptions(checkedId);
    }
    
    public void onClickLastFMCacheRadioGroup(View view) {
    	View group = (View) findViewById(R.id.selectLastFMCacheRadioGroup);
    	group.setVisibility(View.GONE);
    	adjustLastFMVisibility();
    }
    
    public void onClickLastFMCacheLine(View view) {
    	View group = (View) findViewById(R.id.selectLastFMCacheRadioGroup);
    	if (group.getVisibility() == View.VISIBLE)
    		group.setVisibility(View.GONE);
    	else
    		group.setVisibility(View.VISIBLE);
    }




    private void showHideCustomOptions(int selectedId) {
        View customPlayerOptions = (View) findViewById(R.id.customPlayerOptions);
        if (selectedId == R.id.customPlayer) {
            customPlayerOptions.setVisibility(View.VISIBLE);
        } else {
            customPlayerOptions.setVisibility(View.GONE);
        }
    }
    
    private void adjustLastFMVisibility() {
    	CheckBox wifi = (CheckBox) findViewById(R.id.getLastFMArtWifi);
    	CheckBox network = (CheckBox) findViewById(R.id.getLastFMArtNetwork);

    	RadioGroup styleGroup = (RadioGroup) findViewById(R.id.selectLastFMCacheRadioGroup);    	
    	View lastFMCacheDir = (View) findViewById(R.id.lastFMDirectory);
    	View cacheLine = (View) findViewById(R.id.lastFMCacheLine);
		    	
    	if (wifi.isChecked() || network.isChecked()) {
    		int styleId = styleGroup.getCheckedRadioButtonId();
        	if (styleId == R.id.lastFMCacheInDir)
        		lastFMCacheDir.setVisibility(View.VISIBLE);
        	else
        		lastFMCacheDir.setVisibility(View.GONE);
        	
        	cacheLine.setVisibility(View.VISIBLE);
        	TextView cacheStyle = (TextView) findViewById(R.id.lastFMCacheStyle);
        	RadioButton styleButton = (RadioButton) findViewById(styleId);
        	cacheStyle.setText(styleButton.getText());
    	} else {
    		Log.d("TeaCup", "huh");
    		cacheLine.setVisibility(View.GONE);
    		lastFMCacheDir.setVisibility(View.GONE);
    	}
    }


    public void onClickGetLastFMArtWifi(View view) {
    	onClickLastFMArt();
    }
    
    public void onClickGetLastFMArtNetwork(View view) {
    	onClickLastFMArt();
    }
    
    private void onClickLastFMArt() {
    	adjustLastFMVisibility();
    }

}
