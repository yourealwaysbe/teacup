package net.chilon.matt.teacup;

public class PlayerConfig {
    private int playerId;
    private String name;
    private String playerPackage;
    private String metaChangedAction;
    private String metaChangedId;
    private String playstateChangedAction;
    private String playstateChangedPlaying;
    private String jumpPreviousAction;
    private String jumpPreviousCommandField;
    private String jumpPreviousCommand;
    private String playPauseAction;
    private String playPauseCommandField;
    private String playPauseCommand;
    private String jumpNextAction;
    private String jumpNextCommandField;
    private String jumpNextCommand;
 
    PlayerConfig(int playerId,
                 String name,
                 String playerPackage,
                 String metaChangedAction,
                 String metaChangedId,
                 String playstateChangedAction,
                 String playstateChangedPlaying,
                 String jumpPreviousAction,
                 String jumpPreviousCommandField,
                 String jumpPreviousCommand,
                 String playPauseAction,
                 String playPauseCommandField,
                 String playPauseCommand,
                 String jumpNextAction,
                 String jumpNextCommandField,
                 String jumpNextCommand) {
        this.playerId = playerId;
        this.name = name;
        this.playerPackage = playerPackage;
        this.metaChangedAction = metaChangedAction;
        this.metaChangedId = metaChangedId;
        this.playstateChangedAction = playstateChangedAction;
        this.playstateChangedPlaying = playstateChangedPlaying;
        this.jumpPreviousAction = jumpPreviousAction;
        this.jumpPreviousCommandField = jumpPreviousCommandField;
        this.jumpPreviousCommand = jumpPreviousCommand;
        this.playPauseAction = playPauseAction;
        this.playPauseCommandField = playPauseCommandField;
        this.playPauseCommand = playPauseCommand;
        this.jumpNextAction = jumpNextAction;
        this.jumpNextCommandField = jumpNextCommandField;
        this.jumpNextCommand = jumpNextCommand;
    }

    PlayerConfig(PlayerConfig copy) {
        playerId = copy.playerId;
        name = copy.name;
        playerPackage = copy.playerPackage;
        metaChangedAction = copy.metaChangedAction;
        metaChangedId = copy.metaChangedId;
        playstateChangedAction = copy.playstateChangedAction;
        playstateChangedPlaying = copy.playstateChangedPlaying;
        jumpPreviousAction = copy.jumpPreviousAction;
        jumpPreviousCommandField = copy.jumpPreviousCommandField;
        jumpPreviousCommand = copy.jumpPreviousCommand;
        playPauseAction = copy.playPauseAction;
        playPauseCommandField = copy.playPauseCommandField;
        playPauseCommand = copy.playPauseCommand;
        jumpNextAction = copy.jumpNextAction;
        jumpNextCommandField = copy.jumpNextCommandField;
        jumpNextCommand = copy.jumpNextCommand;
    }

    public int getPlayerId() { return playerId; }
    public String getName() { return name; }
    public String getPlayerPackage() { return playerPackage; }
    public String getMetaChangedAction() { return metaChangedAction; }
    public String getMetaChangedId() { return metaChangedId; }
    public String getPlaystateChangedAction() { return playstateChangedAction; }
    public String getPlaystateChangedPlaying() { return playstateChangedPlaying; }
    public String getJumpPreviousAction() { return jumpPreviousAction; }
    public String getJumpPreviousCommandField() { return jumpPreviousCommandField; }
    public String getJumpPreviousCommand() { return jumpPreviousCommand; }
    public String getPlayPauseAction() { return playPauseAction; }
    public String getPlayPauseCommandField() { return playPauseCommandField; }
    public String getPlayPauseCommand() { return playPauseCommand; }
    public String getJumpNextAction() { return jumpNextAction; }
    public String getJumpNextCommandField() { return jumpNextCommandField; }
    public String getJumpNextCommand() { return jumpNextCommand; }

    public String toString() {
        return "(" +
               playerId + ", " +
               name + ", " +
               playerPackage + ", " +
               metaChangedAction + ", " +
               metaChangedId + ", " +
               playstateChangedAction + ", " +
               playstateChangedPlaying + ", " +
               jumpPreviousAction + ", " +
               jumpPreviousCommandField + ", " +
               jumpPreviousCommand + ", " +
               playPauseAction + ", " +
               playPauseCommandField + ", " +
               playPauseCommand + ", " +
               jumpNextAction + ", " +
               jumpNextCommandField + ", " +
               jumpNextCommand + ")";
    }
}

