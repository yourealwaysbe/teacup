
// Config object containing all user preferences and read/write methods
// We're keeping one shared config at the moment -- i'm not sure how to 
// use RemoteViews to update widgets individually...

package net.chilon.matt.teacup;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

public class Config {
	
	private static final String GET_EMBEDDED_ART = "getEmbeddedArt";
	private static final String GET_DIRECTORY_ART = "getDirectoryArt";
	private static final String SELECTED_PLAYER_ID = "selectedPlayerId";
	private static final String PLAYER_LAUNCH_ACTION = "playerLaunchAction";
	private static final String META_CHANGED_ACTION = "metaChangedAction";
	private static final String META_CHANGED_ID = "metaChangedId";
	private static final String PLAYSTATE_CHANGED_ACTION = "playstateChangedAction";
	private static final String PLAYSTATE_CHANGED_PLAYING = "playstateChangedPlaying";
	private static final String JUMP_PREVIOUS_ACTION = "jumpPreviousAction";
	private static final String JUMP_PREVIOUS_COMMAND_FIELD = "jumpPreviousCommandField";
	private static final String JUMP_PREVIOUS_COMMAND = "jumpPreviousCommand";
	private static final String PLAY_PAUSE_ACTION = "playPauseAction";
	private static final String PLAY_PAUSE_COMMAND_FIELD = "playPauseCommandField";
	private static final String PLAY_PAUSE_COMMAND = "playPauseCommand";
	private static final String JUMP_NEXT_ACTION = "jumpNextAction";
	private static final String JUMP_NEXT_COMMAND_FIELD = "jumpNextCommandField";
	private static final String JUMP_NEXT_COMMAND = "jumpNextCommand";
			
	private static final boolean DEFAULT_GET_EMBEDDED_ART = true;
	private static final boolean DEFAULT_GET_DIRECTORY_ART = true;
	private static final int DEFAULT_SELECTED_PLAYER_ID = R.id.androidPlayer;
	private static final String DEFAULT_PLAYER_LAUNCH_ACTION = "com.android.music";
	private static final String DEFAULT_META_CHANGED_ACTION = "com.android.music.metachanged";
	private static final String DEFAULT_META_CHANGED_ID = "id";
	private static final String DEFAULT_PLAYSTATE_CHANGED_ACTION = "com.android.music.playstatechanged";
	private static final String DEFAULT_PLAYSTATE_CHANGED_PLAYING = "playing";
	private static final String DEFAULT_JUMP_PREVIOUS_ACTION = "com.android.music.musicservicecommand";
	private static final String DEFAULT_JUMP_PREVIOUS_COMMAND_FIELD = "command";
	private static final String DEFAULT_JUMP_PREVIOUS_COMMAND = "previous";
	private static final String DEFAULT_PLAY_PAUSE_ACTION = "com.android.music.musicservicecommand";
	private static final String DEFAULT_PLAY_PAUSE_COMMAND_FIELD = "command";
	private static final String DEFAULT_PLAY_PAUSE_COMMAND = "togglepause";
	private static final String DEFAULT_JUMP_NEXT_ACTION = "com.android.music.musicservicecommand";
	private static final String DEFAULT_JUMP_NEXT_COMMAND_FIELD = "command";
	private static final String DEFAULT_JUMP_NEXT_COMMAND = "next";
		
	private boolean getEmbeddedArt = DEFAULT_GET_EMBEDDED_ART;
	private boolean getDirectoryArt = DEFAULT_GET_DIRECTORY_ART;
	private int selectedPlayerId = DEFAULT_SELECTED_PLAYER_ID;
	private String playerLaunchAction = DEFAULT_PLAYER_LAUNCH_ACTION;
	private String metaChangedAction = DEFAULT_META_CHANGED_ACTION;
	private String metaChangedId = DEFAULT_META_CHANGED_ID;
	private String playstateChangedAction = DEFAULT_PLAYSTATE_CHANGED_ACTION;
	private String playstateChangedPlaying = DEFAULT_PLAYSTATE_CHANGED_PLAYING;
	private String jumpPreviousAction = DEFAULT_JUMP_PREVIOUS_ACTION;
	private String jumpPreviousCommandField = DEFAULT_JUMP_PREVIOUS_COMMAND_FIELD;
	private String jumpPreviousCommand = DEFAULT_JUMP_PREVIOUS_COMMAND;
	private String playPauseAction = DEFAULT_PLAY_PAUSE_ACTION;
	private String playPauseCommandField = DEFAULT_PLAY_PAUSE_COMMAND_FIELD;
	private String playPauseCommand = DEFAULT_PLAY_PAUSE_COMMAND;
	private String jumpNextAction = DEFAULT_JUMP_NEXT_ACTION;
	private String jumpNextCommandField = DEFAULT_JUMP_NEXT_COMMAND_FIELD;
	private String jumpNextCommand = DEFAULT_JUMP_NEXT_COMMAND;
	
	public Config() {
		// do nothing
	}
	
	// reads config from the config activity
	public Config(Activity activity) {
		this.getEmbeddedArt = getCheckedValue(activity, R.id.getEmbeddedArt);
		this.getDirectoryArt = getCheckedValue(activity, R.id.getDirectoryArt);
        
        this.selectedPlayerId = getRadioGroupId(activity, R.id.selectPlayerRadioGroup);
        this.playerLaunchAction = getEditValue(activity, R.id.playerLaunchAction);
        this.metaChangedAction = getEditValue(activity, R.id.metaChangedAction);
        this.metaChangedId = getEditValue(activity, R.id.metaChangedId);
        this.playstateChangedAction = getEditValue(activity, R.id.playstateChangedAction);
        this.playstateChangedPlaying = getEditValue(activity, R.id.playstateChangedPlaying);
        this.jumpNextAction = getEditValue(activity, R.id.jumpNextAction);
        this.jumpNextCommandField = getEditValue(activity, R.id.jumpNextCommandField);
        this.jumpNextCommand = getEditValue(activity, R.id.jumpNextCommand);
        this.playPauseAction = getEditValue(activity, R.id.playPauseAction);
        this.playPauseCommandField = getEditValue(activity, R.id.playPauseCommandField);
        this.playPauseCommand = getEditValue(activity, R.id.playPauseCommand);
        this.jumpNextAction = getEditValue(activity, R.id.jumpNextAction);
        this.jumpNextCommandField = getEditValue(activity, R.id.jumpNextCommandField);
        this.jumpNextCommand = getEditValue(activity, R.id.jumpNextCommand);
        
        System.out.println("Read from activity" + this);
    }
	
	public Config(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("prefs", 0);
        
        getEmbeddedArt = prefs.getBoolean(GET_EMBEDDED_ART, 
        		                          DEFAULT_GET_EMBEDDED_ART);
		getDirectoryArt = prefs.getBoolean(GET_DIRECTORY_ART, 
				                           DEFAULT_GET_DIRECTORY_ART);
		selectedPlayerId = prefs.getInt(SELECTED_PLAYER_ID, 
				                        DEFAULT_SELECTED_PLAYER_ID);
		playerLaunchAction = prefs.getString(PLAYER_LAUNCH_ACTION,
				                             DEFAULT_PLAYER_LAUNCH_ACTION);
		metaChangedAction = prefs.getString(META_CHANGED_ACTION, 
				                            DEFAULT_META_CHANGED_ACTION);
		metaChangedId = prefs.getString(META_CHANGED_ID,
				                        DEFAULT_META_CHANGED_ID);
		playstateChangedAction = prefs.getString(PLAYSTATE_CHANGED_ACTION,
				                                 DEFAULT_PLAYSTATE_CHANGED_ACTION);
		playstateChangedPlaying = prefs.getString(PLAYSTATE_CHANGED_PLAYING,
				                                  DEFAULT_PLAYSTATE_CHANGED_PLAYING);
		jumpPreviousAction = prefs.getString(JUMP_PREVIOUS_ACTION,
				                             DEFAULT_JUMP_PREVIOUS_ACTION);
		jumpPreviousCommandField = prefs.getString(JUMP_PREVIOUS_COMMAND_FIELD,
				                                   DEFAULT_JUMP_PREVIOUS_COMMAND_FIELD);
		jumpPreviousCommand = prefs.getString(JUMP_PREVIOUS_COMMAND,
				                              DEFAULT_JUMP_PREVIOUS_COMMAND);
		playPauseAction = prefs.getString(PLAY_PAUSE_ACTION,
                                          DEFAULT_PLAY_PAUSE_ACTION);
		playPauseCommandField = prefs.getString(PLAY_PAUSE_COMMAND_FIELD,
                                                DEFAULT_PLAY_PAUSE_COMMAND_FIELD);
		playPauseCommand = prefs.getString(PLAY_PAUSE_COMMAND,
                                           DEFAULT_PLAY_PAUSE_COMMAND);
		jumpNextAction = prefs.getString(JUMP_NEXT_ACTION,
                                         DEFAULT_JUMP_NEXT_ACTION);
		jumpNextCommandField = prefs.getString(JUMP_NEXT_COMMAND_FIELD,
                                               DEFAULT_JUMP_NEXT_COMMAND_FIELD);
		jumpNextCommand = prefs.getString(JUMP_NEXT_COMMAND,
                                          DEFAULT_JUMP_NEXT_COMMAND);

		System.out.println("Read from shared" + this);
	}
		
	public boolean getEmbeddedArt() {
		return getEmbeddedArt;
	}
	
	public boolean getDirectoryArt() {
		return getDirectoryArt;
	}
	
	public String metaChangedAction() {
		return metaChangedAction;
	}
	
	public String metaChangedId() {
		return metaChangedId;
	}
	
	public void writeConfigToSharedPreferences(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("prefs", 0);
        SharedPreferences.Editor edit = prefs.edit();

		edit.putBoolean(GET_EMBEDDED_ART, getEmbeddedArt);
		edit.putBoolean(GET_DIRECTORY_ART, getDirectoryArt);		
		edit.putString(META_CHANGED_ACTION, metaChangedAction);
		edit.putString(META_CHANGED_ID, metaChangedId);
		
        edit.commit();
        
        System.out.println("Write to shared" + this);
	}
	
	
	public void writeConfigToActivity(Activity activity) {
		CheckBox cbEmbeddedArt = (CheckBox) activity.findViewById(R.id.getEmbeddedArt);
		cbEmbeddedArt.setChecked(getEmbeddedArt);
		
		CheckBox cbDirectoryArt = (CheckBox) activity.findViewById(R.id.getDirectoryArt);
		cbDirectoryArt.setChecked(getDirectoryArt);
		
		EditText ebmetaChangedAction = (EditText) activity.findViewById(R.id.metaChangedAction);
		ebmetaChangedAction.setText(metaChangedAction);
		
		EditText ebmetaChangedId = (EditText) activity.findViewById(R.id.metaChangedId);
		ebmetaChangedId.setText(metaChangedId);
		
		System.out.println("Write to activity" + this);
	}
	
	
	public String toString() {
		return "(" +
	           getEmbeddedArt + ", " +
			   getDirectoryArt + ", " +
	           metaChangedAction + ", " +
			   metaChangedId + ")";
	}
	
	private String getEditValue(Activity activity, int editId) {
		EditText edit = (EditText) activity.findViewById(editId);
		return edit.getText().toString();
	}
	
	private boolean getCheckedValue(Activity activity, int checkId) {
        CheckBox check = (CheckBox) activity.findViewById(checkId);
        return check.isChecked();
	}
	
	private int getRadioGroupId(Activity activity, int groupId) {
		RadioGroup group = (RadioGroup) activity.findViewById(groupId);
		return group.getCheckedRadioButtonId();
	}
	
	
	private void setEditValue(Activity activity, 
			                    int editId, 
			                    String value) {
		EditText edit = (EditText) activity.findViewById(editId);
		edit.setText(value);
	}
	
	private void setCheckedValue(Activity activity, 
			                     int checkId,
			                     boolean value) {
        CheckBox check = (CheckBox) activity.findViewById(checkId);
        check.setChecked(value);
	}
	
	private void setRadioGroupId(Activity activity,
			                     int valueId) {
		RadioButton button = (RadioButton) activity.findViewById(valueId);
		button.setChecked(true);
	}
}
