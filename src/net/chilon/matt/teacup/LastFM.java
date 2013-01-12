package net.chilon.matt.teacup;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

public class LastFM {
	
	private static final String API_KEY = "d6e802774ce70edfca5d501009377a53";
	private static final String API_ROOT = "http://ws.audioscrobbler.com/2.0/";
	private static final String GET_ALBUM_INFO = "method=album.getinfo";
	private static final String API_KEY_ARG = "api_key=" + API_KEY;
	private static final String ARTIST_ARG = "artist=%s";
	private static final String ALBUM_ARG = "album=%s";
	
	private static final String IMAGE_TAG = "image";
	private static final String IMAGE_SIZE = "large";
	private static final String SIZE_NAMESPACE = "";
	private static final String SIZE_ATTRIBUTE = "size";
	
	private static final String URL_TEMPLATE = API_ROOT + "?" +
	                                           GET_ALBUM_INFO + "&" +
	                                           API_KEY_ARG + "&" +
	                                           ARTIST_ARG + "&" +
	                                           ALBUM_ARG;

	
	public static Bitmap getArt(Context context,
			                    Config config, 
			                    String artist, 
			                    String album) {
		Bitmap artBmp = getCachedArt(config.getLastFMDirectory(), artist, album);
		
		if (artBmp == null)
			artBmp = getWebArt(context, config, artist, album);
		
		return artBmp;
	}
	
	private static Bitmap getCachedArt(String directory, 
			                           String artist, 
			                           String album) {
		return null;
	}
	
	private static Bitmap getWebArt(Context context,
			                        Config config,
			                        String artist, 
			                        String album) {
		Bitmap artBmp = null;
		
		if (shouldConnect(config, context)) {
			String artUrl = getArtUrl(artist, album);
			if (artUrl != null) {
				artBmp = AlbumArtFactory.readUrl(artUrl);
			}
		}
		
		return artBmp;
	}
	
	private static boolean shouldConnect(Config config, Context context) {
		boolean getLastFMArtWifi = config.getLastFMArtWifi();
        boolean getLastFMArtNetwork = config.getLastFMArtNetwork();
        
        ConnectivityManager cm = (ConnectivityManager)
        context.getSystemService(Context.CONNECTIVITY_SERVICE);

    	NetworkInfo wifiNetwork 
			= cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

    	if (getLastFMArtWifi &&
    		wifiNetwork != null &&  
        	wifiNetwork.isConnectedOrConnecting())
    		return true;

    	NetworkInfo mobileNetwork 
			= cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
    	
    	if (getLastFMArtNetwork &&
    	    mobileNetwork != null && 
    	    mobileNetwork.isConnectedOrConnecting())
    		return true;
    	

    	NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
    	
    	if (getLastFMArtNetwork &&
		    activeNetwork != null && 
		    activeNetwork.isConnectedOrConnecting())
    		return true;
    	
    	return false;
	}
	
	private static String getArtUrl(String artist, String album) {
		try {
			XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
			factory.setNamespaceAware(false);
			XmlPullParser xpp = factory.newPullParser();
			
			String webArtist = URLEncoder.encode(artist, "UTF-8");
			String webAlbum = URLEncoder.encode(album, "UTF-8");			
			
			URL url = new URL(String.format(URL_TEMPLATE, webArtist, webAlbum));
			URLConnection ucon = url.openConnection();			
			InputStream is = ucon.getInputStream();
			
			xpp.setInput(is, null);
        
			int eventType = xpp.getEventType();
        
			while (eventType != XmlPullParser.END_DOCUMENT) {
				
				if (eventType == XmlPullParser.START_TAG &&
						IMAGE_TAG.equals(xpp.getName())) {
        		
					int n = xpp.getAttributeCount();
					for (int i = 0; i < n; ++i) {
						String size = xpp.getAttributeValue(SIZE_NAMESPACE, SIZE_ATTRIBUTE);
						if (IMAGE_SIZE.equals(size)) {
							xpp.next();
							return xpp.getText();
						}
					}
				}
				xpp.next();
				eventType = xpp.getEventType();
			}
		} catch (XmlPullParserException e) {
			// do nothing
			Log.d("TeaCup", "xmlpullparserexception: " + e);
		} catch (IOException e) {
			// do nothing
			Log.d("TeaCup", "ioexception: " + e);
		}
        
        return null;
	}
}
