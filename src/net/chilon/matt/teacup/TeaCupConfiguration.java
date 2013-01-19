package net.chilon.matt.teacup;

import java.util.Locale;
import java.util.Random;

import net.chilon.matt.teacup.R;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

public class TeaCupConfiguration extends Activity {

    private TeaCupConfiguration self = this;

    private int teaCupId;
    
    private PrefetchLastFMArtTask prefetcher = null;
    
    private long lastFMCacheSize = 0;

    private static String[] scrobblers = {
    	"evil geniuses",
    	"malevolent dictators",
    	"funky soulsters",
    	"randy terrorists",
    	"purple dinosaurs",
    	"blue whales",
    	"territorial armchairs",
    	"musical nerds",
    	"marvelous luvvies",
    	"skin and bones",
    	"fantastic foxes",
    	"hipsters and hackers",
    	"manx cats",
    	"heroic friendsters",
    	"teetotal auctioneers",
    	"ticklish sneezes"
    };
    
    private static final String LASTFM_URL = "http://www.lastfm.com";
    
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
        adjustScrobbleVisibility();
        setLegalText();
        
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
                
                stopPrefetchLastFMAlbumArt();

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
    	View prefetchOptions = (View) findViewById(R.id.prefetchOptions);
		    	
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
        	
        	if (styleId != R.id.lastFMCacheNone)
        		prefetchOptions.setVisibility(View.VISIBLE);
        	else
        		prefetchOptions.setVisibility(View.GONE);
    	} else {
    		cacheLine.setVisibility(View.GONE);
    		lastFMCacheDir.setVisibility(View.GONE);
    		prefetchOptions.setVisibility(View.GONE);
    		stopPrefetchLastFMAlbumArt();
    	}
    	
    	lastFMCacheSize = LastFM.getScrobbleCacheSize(this);
    	String buttonText = String.format(Locale.ENGLISH, 
    			                          getResources().getString(R.string.lastFMClearCache),
    			                          lastFMCacheSize);
    	Button button = (Button) findViewById(R.id.lastFMClearCache);
    	button.setText(buttonText);
    }


    public void onClickGetLastFMArtWifi(View view) {
    	onClickLastFMArt();
    }
    
    public void onClickGetLastFMArtNetwork(View view) {
    	onClickLastFMArt();
    }
    
    public void onClickPrefetchLastFMArt(View view) {
    	if (prefetcher == null || 
    		prefetcher.isDone()) {
    		startPrefetchLastFMAlbumArt();
    	} else {
    		stopPrefetchLastFMAlbumArt();
    	}
    }
    
    public void onClickLastFMClearCache(View view) {
    	LastFM.clearScrobbleCache(this);
    	adjustLastFMVisibility();
    }
    
    public void onClickLastFMButton(View view) {
    	Intent browserIntent = new Intent(Intent.ACTION_VIEW, 
    			                          Uri.parse(LASTFM_URL));
    	startActivity(browserIntent);
    }
    
    private void onClickLastFMArt() {
    	adjustLastFMVisibility();
    }

    private void startPrefetchLastFMAlbumArt() {
    	ProgressBar progress = (ProgressBar) findViewById(R.id.prefetchProgressBar);
    	Button prefetchButton = (Button) findViewById(R.id.prefetchLastFMArt);
    		
    	progress.setVisibility(View.VISIBLE);
    	prefetchButton.setText(R.string.cancel);
    	    	
    	if (prefetcher != null && 
    		!prefetcher.isDone()) {
    		stopPrefetchLastFMAlbumArt();
    	} else {
    		Config config = new Config(self);
    		prefetcher = new PrefetchLastFMArtTask();
    		prefetcher.execute(config);
    	}
    }
    
    
    public void onClickLastFMScrobbleOptions(View view) {
        adjustScrobbleVisibility();
    }

    private void adjustScrobbleVisibility() {
    	CheckBox wifi = (CheckBox) findViewById(R.id.lastFMScrobbleWifi);
    	CheckBox network = (CheckBox) findViewById(R.id.lastFMScrobbleNetwork);
    	CheckBox cache = (CheckBox) findViewById(R.id.lastFMScrobbleCache);
    	
    	View authOpts = findViewById(R.id.lastFMAuthentication);
    	
    	if (wifi.isChecked() || network.isChecked() || cache.isChecked())
    		authOpts.setVisibility(View.VISIBLE);
    	else
    		authOpts.setVisibility(View.GONE);
    }
    
    private void setLegalText() {
    	TextView text = (TextView) findViewById(R.id.lastFMLegalText);
    	String lastFMLegal = getResources().getString(R.string.lastFMLegalText);
    	Random rand = new Random();
    	rand.setSeed(System.currentTimeMillis());
    	String desc = scrobblers[rand.nextInt(scrobblers.length)];
    	text.setText(String.format(lastFMLegal, desc));
    }


    private void stopPrefetchLastFMAlbumArt() {
    	if (prefetcher != null) {
    		prefetcher.cancel(true);
    	}
    }

    

    private class PrefetchLastFMArtTask extends AsyncTask<Config, Integer, LastFM.PrefetchState>
                                        implements ProgressUpdater {
    	boolean done = false;
    	
    	protected LastFM.PrefetchState doInBackground(Config... configs) {
    		return LastFM.prefetchArt(self, configs[0], this);
    	}
    	
    	protected void onPostExecute(LastFM.PrefetchState result) {
    		
    		Button prefetchButton = (Button) findViewById(R.id.prefetchLastFMArt);
    		if (result == LastFM.PrefetchState.NOCONNECTION)
    			prefetchButton.setText(R.string.lastFMPrefetchAlbumArtNoConnection);
    		else
    			prefetchButton.setText(R.string.lastFMPrefetchAlbumArtAgain);
    		setProgressPercent(100);
    		done = true;
    	}
    	    	
    	protected void onProgressUpdate(Integer... percents) {
    		ProgressBar progress = (ProgressBar) findViewById(R.id.prefetchProgressBar);
    		progress.setProgress(percents[0]);    		
    	}
    	
    	protected void onCancelled() {
    		doCancel();
    	}
    	
    	protected void onCancelled(Integer result) {
    		doCancel();
    	}
  
    	private void doCancel() {
    		ProgressBar progress = (ProgressBar) findViewById(R.id.prefetchProgressBar);
    		Button prefetchButton = (Button) findViewById(R.id.prefetchLastFMArt);
    		
        	progress.setVisibility(View.GONE);
        	prefetchButton.setText(R.string.lastFMPrefetchAlbumArt);
        	
        	done = true;
    	}
    	
    	public void setProgressPercent(int percent) {
    		publishProgress(percent);
    	}
    	
    	public boolean getCancelled() {
    		return isCancelled();
    	}
    	
    	public boolean isDone() {
    		return done;
    	}
    }
}
