package net.chilon.matt.teacup;

public interface ProgressUpdater {
	public void setProgressPercent(int percent);
	
	public boolean getCancelled();
}
