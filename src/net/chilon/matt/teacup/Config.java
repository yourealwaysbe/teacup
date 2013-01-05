
// Config object containing all user preferences and read/write methods
// We're keeping one shared config at the moment -- i'm not sure how to 
// use RemoteViews to update widgets individually...

package net.chilon.matt.teacup;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.widget.CheckBox;
import android.widget.EditText;

public class Config {

	private static final String GET_EMBEDDED_ART = "getEmbeddedArt";
	private static final String GET_DIRECTORY_ART = "getDirectoryArt";
	private static final String STATUS_CHANGED_ACTION = "statusChangedAction";
	private static final String STATUS_CHANGED_ID = "statusChangedId";
	
	private static final boolean DEFAULT_GET_EMBEDDED_ART = true;
	private static final boolean DEFAULT_GET_DIRECTORY_ART = true;
	private static final String DEFAULT_STATUS_CHANGED_ACTION = "com.android.music.metachanged";
	private static final String DEFAULT_STATUS_CHANGED_ID = "id";
		
	private boolean getEmbeddedArt = DEFAULT_GET_EMBEDDED_ART;
	private boolean getDirectoryArt = DEFAULT_GET_DIRECTORY_ART;
	private String statusChangedAction = DEFAULT_STATUS_CHANGED_ACTION;
	private String statusChangedId = DEFAULT_STATUS_CHANGED_ID;
	
	public Config() {
		// do nothing
	}
	
	// reads config from the config activity
	public Config(Activity activity) {
		CheckBox getEmbeddedArt = (CheckBox) activity.findViewById(R.id.getEmbeddedArt);                
		this.getEmbeddedArt = getEmbeddedArt.isChecked();
        CheckBox getDirectoryArt = (CheckBox) activity.findViewById(R.id.getDirectoryArt);
        this.getDirectoryArt = getDirectoryArt.isChecked();
        EditText statusChangedIntent = (EditText) activity.findViewById(R.id.statusChangedAction);
        this.statusChangedAction = statusChangedIntent.getText().toString();
        EditText statusChangedId = (EditText) activity.findViewById(R.id.statusChangedId);
        this.statusChangedId = statusChangedId.getText().toString();
        
        System.out.println("Read from activity" + this);
    }
	
	public Config(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("prefs", 0);
        
        getEmbeddedArt = prefs.getBoolean(GET_EMBEDDED_ART, 
        		                          DEFAULT_GET_EMBEDDED_ART);
		getDirectoryArt = prefs.getBoolean(GET_DIRECTORY_ART, 
				                           DEFAULT_GET_DIRECTORY_ART);
		statusChangedAction = prefs.getString(STATUS_CHANGED_ACTION, 
				                              DEFAULT_STATUS_CHANGED_ACTION);
		statusChangedId = prefs.getString(STATUS_CHANGED_ID,
				                          DEFAULT_STATUS_CHANGED_ID);
		
		System.out.println("Read from shared" + this);
	}
		
	public boolean getEmbeddedArt() {
		return getEmbeddedArt;
	}
	
	public boolean getDirectoryArt() {
		return getDirectoryArt;
	}
	
	public String statusChangedAction() {
		return statusChangedAction;
	}
	
	public String statusChangedId() {
		return statusChangedId;
	}
	
	public void writeConfigToSharedPreferences(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("prefs", 0);
        SharedPreferences.Editor edit = prefs.edit();

		edit.putBoolean(GET_EMBEDDED_ART, getEmbeddedArt);
		edit.putBoolean(GET_DIRECTORY_ART, getDirectoryArt);		
		edit.putString(STATUS_CHANGED_ACTION, statusChangedAction);
		edit.putString(STATUS_CHANGED_ID, statusChangedId);
		
        edit.commit();
        
        System.out.println("Write to shared" + this);
	}
	
	
	public void writeConfigToActivity(Activity activity) {
		CheckBox cbEmbeddedArt = (CheckBox) activity.findViewById(R.id.getEmbeddedArt);
		cbEmbeddedArt.setChecked(getEmbeddedArt);
		
		CheckBox cbDirectoryArt = (CheckBox) activity.findViewById(R.id.getDirectoryArt);
		cbDirectoryArt.setChecked(getDirectoryArt);
		
		EditText ebStatusChangedAction = (EditText) activity.findViewById(R.id.statusChangedAction);
		ebStatusChangedAction.setText(statusChangedAction);
		
		EditText ebStatusChangedId = (EditText) activity.findViewById(R.id.statusChangedId);
		ebStatusChangedId.setText(statusChangedId);
		
		System.out.println("Write to activity" + this);
	}
	
	
	public String toString() {
		return "(" +
	           getEmbeddedArt + ", " +
			   getDirectoryArt + ", " +
	           statusChangedAction + ", " +
			   statusChangedId + ")";
	}
}
