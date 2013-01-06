package net.chilon.matt.teacup;

import java.util.regex.Pattern;

import android.graphics.Bitmap;

public class LastFM {
	
	private static final String SEARCH_PREFIX = "http://www.last.fm/search?q=";
	private static final Pattern IMAGE_RE 
		= Pattern.compile(".*<img *width=\"[^\"]*\" *src=\"([^\"]*)\" *class=\"art\" *class=\"albumCover[^\"]*\" */>.*");
	private static final int IMAGE_GROUP = 1;
	private static final Pattern IMAGE_URL_RE 
		= Pattern.compile("(.*serve/)[0-9a-z]*(/.*)");
	private static final int IMAGE_URL_START_GROUP = 1;
	private static final int IMAGE_URL_REST_GROUP = 2;
	private static final String IMAGE_DIMENSION = "100";
	
	public static Bitmap getArt(Config config, 
			                    String artist, 
			                    String album) {
		return null;
	}
}