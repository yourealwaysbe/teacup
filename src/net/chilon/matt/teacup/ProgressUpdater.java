/**
 * Copyright 2013 Matthew Hague
 * Released under the GNU General Public License v3 (see GPL.txt)
 */


package net.chilon.matt.teacup;

public interface ProgressUpdater {
	public void setProgressPercent(int percent);
	
	public boolean getCancelled();
}
