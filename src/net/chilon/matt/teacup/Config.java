
// Config object containing all user preferences and read/write methods
// We're keeping one shared config at the moment -- i'm not sure how to 
// use RemoteViews to update widgets individually...

package net.chilon.matt.teacup;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.SparseArray;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

public class Config {
    
    private static final String CUSTOM_PLAYER_NAME = "Custom Player";
    private static final String ANDROID_PLAYER_NAME = "Android Player";
    
    private static final String GET_EMBEDDED_ART = "getEmbeddedArt";
    private static final String GET_DIRECTORY_ART = "getDirectoryArt";
    private static final String SELECTED_PLAYER_ID = "selectedPlayerId";
    private static final String PLAYER_LAUNCH_ACTION = "playerPackage";
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
    private static final String DEFAULT_PLAYER_NAME = ANDROID_PLAYER_NAME;
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

    private static final SparseArray<PlayerConfig> defaultPlayers = initDefaultPlayers();
    private static SparseArray<PlayerConfig> initDefaultPlayers() {
        SparseArray<PlayerConfig> map = new SparseArray<PlayerConfig>();
        map.put(R.id.androidPlayer, 
                new PlayerConfig(DEFAULT_SELECTED_PLAYER_ID,
                                 DEFAULT_PLAYER_NAME,
                                 DEFAULT_PLAYER_LAUNCH_ACTION,
                                 DEFAULT_META_CHANGED_ACTION,
                                 DEFAULT_META_CHANGED_ID,
                                 DEFAULT_PLAYSTATE_CHANGED_ACTION,
                                 DEFAULT_PLAYSTATE_CHANGED_PLAYING,
                                 DEFAULT_JUMP_PREVIOUS_ACTION,
                                 DEFAULT_JUMP_PREVIOUS_COMMAND_FIELD,
                                 DEFAULT_JUMP_PREVIOUS_COMMAND,
                                 DEFAULT_PLAY_PAUSE_ACTION,
                                 DEFAULT_PLAY_PAUSE_COMMAND_FIELD,
                                 DEFAULT_PLAY_PAUSE_COMMAND,
                                 DEFAULT_JUMP_NEXT_ACTION,
                                 DEFAULT_JUMP_NEXT_COMMAND_FIELD,
                                 DEFAULT_JUMP_NEXT_COMMAND));
        return map;
    }

    
    private boolean getEmbeddedArt = DEFAULT_GET_EMBEDDED_ART;
    private boolean getDirectoryArt = DEFAULT_GET_DIRECTORY_ART;
    private PlayerConfig customPlayer = defaultPlayers.get(R.id.androidPlayer);
    private int selectedPlayerId = DEFAULT_SELECTED_PLAYER_ID;
    
    public Config() {
        // do nothing
    }
    
    // reads config from the config activity
    public Config(Activity activity) {
        this.getEmbeddedArt = getCheckedValue(activity, R.id.getEmbeddedArt);
        this.getDirectoryArt = getCheckedValue(activity, R.id.getDirectoryArt);
        selectedPlayerId = getRadioGroupId(activity, R.id.selectPlayerRadioGroup);
        
        customPlayer = 
            new PlayerConfig(R.id.customPlayer,
                             CUSTOM_PLAYER_NAME,
                             getEditValue(activity, R.id.playerPackage),
                             getEditValue(activity, R.id.metaChangedAction),
                             getEditValue(activity, R.id.metaChangedId),
                             getEditValue(activity, R.id.playstateChangedAction),
                             getEditValue(activity, R.id.playstateChangedPlaying),
                             getEditValue(activity, R.id.jumpNextAction),
                             getEditValue(activity, R.id.jumpNextCommandField),
                             getEditValue(activity, R.id.jumpNextCommand),
                             getEditValue(activity, R.id.playPauseAction),
                             getEditValue(activity, R.id.playPauseCommandField),
                             getEditValue(activity, R.id.playPauseCommand),
                             getEditValue(activity, R.id.jumpNextAction),
                             getEditValue(activity, R.id.jumpNextCommandField),
                             getEditValue(activity, R.id.jumpNextCommand));        
    }
    
    public Config(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("prefs", 0);
        
        getEmbeddedArt = prefs.getBoolean(GET_EMBEDDED_ART, DEFAULT_GET_EMBEDDED_ART);
        getDirectoryArt = prefs.getBoolean(GET_DIRECTORY_ART, DEFAULT_GET_DIRECTORY_ART);
        selectedPlayerId = prefs.getInt(SELECTED_PLAYER_ID, DEFAULT_SELECTED_PLAYER_ID);

        customPlayer 
            = new PlayerConfig(R.id.customPlayer,
                               CUSTOM_PLAYER_NAME,
                               prefs.getString(PLAYER_LAUNCH_ACTION, DEFAULT_PLAYER_LAUNCH_ACTION),
                               prefs.getString(META_CHANGED_ACTION, DEFAULT_META_CHANGED_ACTION),
                               prefs.getString(META_CHANGED_ID, DEFAULT_META_CHANGED_ID),
                               prefs.getString(PLAYSTATE_CHANGED_ACTION, DEFAULT_PLAYSTATE_CHANGED_ACTION),
                               prefs.getString(PLAYSTATE_CHANGED_PLAYING, DEFAULT_PLAYSTATE_CHANGED_PLAYING),
                               prefs.getString(JUMP_PREVIOUS_ACTION, DEFAULT_JUMP_PREVIOUS_ACTION),
                               prefs.getString(JUMP_PREVIOUS_COMMAND_FIELD, DEFAULT_JUMP_PREVIOUS_COMMAND_FIELD),
                               prefs.getString(JUMP_PREVIOUS_COMMAND, DEFAULT_JUMP_PREVIOUS_COMMAND),
                               prefs.getString(PLAY_PAUSE_ACTION, DEFAULT_PLAY_PAUSE_ACTION),
                               prefs.getString(PLAY_PAUSE_COMMAND_FIELD, DEFAULT_PLAY_PAUSE_COMMAND_FIELD),
                               prefs.getString(PLAY_PAUSE_COMMAND, DEFAULT_PLAY_PAUSE_COMMAND),
                               prefs.getString(JUMP_NEXT_ACTION, DEFAULT_JUMP_NEXT_ACTION),
                               prefs.getString(JUMP_NEXT_COMMAND_FIELD, DEFAULT_JUMP_NEXT_COMMAND_FIELD),
                               prefs.getString(JUMP_NEXT_COMMAND, DEFAULT_JUMP_NEXT_COMMAND));
    }
        
    public boolean getEmbeddedArt() {
        return getEmbeddedArt;
    }
    
    public boolean getDirectoryArt() {
        return getDirectoryArt;
    }
    
    public PlayerConfig getPlayer() {
        return getPlayer(selectedPlayerId);
    }
    
    public void writeConfigToSharedPreferences(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("prefs", 0);
        SharedPreferences.Editor edit = prefs.edit();

        edit.putBoolean(GET_EMBEDDED_ART, getEmbeddedArt);
        edit.putBoolean(GET_DIRECTORY_ART, getDirectoryArt);
        edit.putInt(SELECTED_PLAYER_ID, selectedPlayerId);
        edit.putString(PLAYER_LAUNCH_ACTION, customPlayer.getPlayerPackage());
        edit.putString(META_CHANGED_ACTION, customPlayer.getMetaChangedAction());
        edit.putString(META_CHANGED_ID, customPlayer.getMetaChangedId());
        edit.putString(PLAYSTATE_CHANGED_ACTION, customPlayer.getPlaystateChangedAction());
        edit.putString(PLAYSTATE_CHANGED_PLAYING, customPlayer.getPlaystateChangedPlaying());
        edit.putString(JUMP_PREVIOUS_ACTION, customPlayer.getJumpPreviousAction());
        edit.putString(JUMP_PREVIOUS_COMMAND_FIELD, customPlayer.getJumpPreviousCommandField());
        edit.putString(JUMP_PREVIOUS_COMMAND, customPlayer.getJumpPreviousCommand());
        edit.putString(PLAY_PAUSE_ACTION, customPlayer.getPlayPauseAction());
        edit.putString(PLAY_PAUSE_COMMAND_FIELD, customPlayer.getPlayPauseCommandField());
        edit.putString(PLAY_PAUSE_COMMAND, customPlayer.getPlayPauseCommand());
        edit.putString(JUMP_NEXT_ACTION, customPlayer.getJumpNextAction());
        edit.putString(JUMP_NEXT_COMMAND_FIELD, customPlayer.getJumpNextCommandField());
        edit.putString(JUMP_NEXT_COMMAND, customPlayer.getJumpNextCommand());
        
        edit.commit();
    }
    
    
    public void writeConfigToActivity(Activity activity) {
        setCheckedValue(activity, R.id.getEmbeddedArt, getEmbeddedArt);
        setCheckedValue(activity, R.id.getDirectoryArt, getDirectoryArt);
        setRadioGroupId(activity, selectedPlayerId);
        setTextValue(activity, R.id.playerSelected, getPlayer(selectedPlayerId).getName());
        setEditValue(activity, R.id.playerPackage, customPlayer.getPlayerPackage());
        setEditValue(activity, R.id.metaChangedAction, customPlayer.getMetaChangedAction());
        setEditValue(activity, R.id.metaChangedId, customPlayer.getMetaChangedId());
        setEditValue(activity, R.id.playstateChangedAction, customPlayer.getPlaystateChangedAction());
        setEditValue(activity, R.id.playstateChangedPlaying, customPlayer.getPlaystateChangedPlaying());
        setEditValue(activity, R.id.jumpPreviousAction, customPlayer.getJumpPreviousAction());
        setEditValue(activity, R.id.jumpPreviousCommandField, customPlayer.getJumpPreviousCommandField());
        setEditValue(activity, R.id.jumpPreviousCommand, customPlayer.getJumpPreviousCommand());
        setEditValue(activity, R.id.playPauseAction, customPlayer.getPlayPauseAction());
        setEditValue(activity, R.id.playPauseCommandField, customPlayer.getPlayPauseCommandField());
        setEditValue(activity, R.id.playPauseCommand, customPlayer.getPlayPauseCommand());
        setEditValue(activity, R.id.jumpNextAction, customPlayer.getJumpNextAction());
        setEditValue(activity, R.id.jumpNextCommandField, customPlayer.getJumpNextCommandField());
        setEditValue(activity, R.id.jumpNextCommand, customPlayer.getJumpNextCommand());
    }
    
    
    public String toString() {
        return "(" +
               getEmbeddedArt + ", " +
               getDirectoryArt + ", " +
               selectedPlayerId + ", " +
               customPlayer + ")";
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

    private void setTextValue(Activity activity, 
                              int textId, 
                              String value) {
        TextView text = (TextView) activity.findViewById(textId);
        text.setText(value);
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
    
    private PlayerConfig getPlayer(int id) {
        if (id == R.id.customPlayer)
            return customPlayer;
        else
            return defaultPlayers.get(id);
    }

    
}
