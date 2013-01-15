package net.chilon.matt.teacup;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlPullParserException;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.provider.MediaStore;
import android.util.Log;
import android.content.SharedPreferences;

public class LastFM {
	
    public enum PrefetchState {
        OK, NOCONNECTION, CANCELLED
    }
	
	private static final int URL_TIMEOUT = 5000;
	
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

	private static final Bitmap.CompressFormat CACHE_TYPE = Bitmap.CompressFormat.PNG;
	private static final String CACHE_EXT = ".png";
	
	// we're limited to 5 requests per second, so will call "makeRequest" before each connection
	// and this will enforce the limit
	private static final int MAX_REQUESTS = 5;
	private static final int TIME_SLICE_LEN = 1000;
	private static final int TIME_SAFETY_BUFFER = 10;
	
	private static long timeSliceBegin = -1;
	private static int numRequests = 0;


    // Scrobbling
	private enum ScrobbleType {
		NOSCROBBLE, SCROBBLE, CACHE
	};
	
	
    private static final String PREFS_FILE = "lastfm";
    private static final String CUR_TRACK_ARTIST = "track-artist";
    private static final String CUR_TRACK_TITLE = "track-title";
    private static final String CUR_TRACK_BEGAN = "track-began";
    private static final String CUR_TRACK_TOTAL_TIME = "track-total-time";
    private static final long FOUR_MINUTES = 240000;
    private static final long THIRTY_SECONDS = 30000;

	public static Bitmap getArt(Context context,
			                    Config config, 
			                    String artist, 
			                    String album,
			                    String filename) {
		Bitmap artBmp = null;
		try {
			artBmp = getArtUnprotected(context, 
					                   config,
					                   artist,
					                   album,
					                   filename);
			if (config.getLastFMCacheStyle() != Config.LASTFM_NO_CACHE)
				artBmp = getCachedArt(config, artist, album, filename);
				
			if (artBmp == null) {
				artBmp = getWebArt(context, config, artist, album, filename);
				if (artBmp != null)
					cacheBitmap(config, artist, album, filename, artBmp);
			}
		} catch (InterruptedException e) {
			Log.w("TeaCup", "Last FM get art operation interrupted", e);
		}
			
		return artBmp;
	}
	
	public static PrefetchState prefetchArt(Context context, 
			                                Config config, 
			                                ProgressUpdater progress) {
		try {
			if (shouldConnectArt(config,  context)) {
				String projection[] = {
					MediaStore.Audio.Media.ARTIST,
					MediaStore.Audio.Media.ALBUM,
					MediaStore.Audio.Media.DATA
				};
				String selection = "";
				ContentResolver resolver = context.getContentResolver();
				Cursor result = resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
						                       projection,
	                                           selection,
	                                           null,
	                                           null);
	        
				int count = result.getCount();
				int done = 0;
				result.moveToFirst();
				while (!result.isAfterLast() && !progress.getCancelled()) {
					String artist = result.getString(0);
					String album = result.getString(1);
					String filename = result.getString(2);
	    	
					getArtUnprotected(context, 
							          config, 
							          artist, 
							          album, 
							          filename);
	    	
					++done;
					progress.setProgressPercent((100*done)/count);
	    	
					result.moveToNext();
				}
			
				if (progress.getCancelled())
					return PrefetchState.CANCELLED;
				else 
					return PrefetchState.OK;
			} else {
				return PrefetchState.NOCONNECTION;
			}
		} catch (InterruptedException e) {
			Log.w("TeaCup", "Last FM prefetch interrupted.", e);
			return PrefetchState.CANCELLED;
		}
	}

	
	public static void scrobbleUpdate(Context context,
			                          Config config,
                                      String artist, 
                                      String title, 
                                      long trackLen,
                                      boolean playing) {
    
        boolean scrobble = false;
        String scrobbleArtist = null;
        String scrobbleTitle = null;
        long scrobbleTime = 0;

        long now = System.currentTimeMillis();
        
		// Update stored data
		synchronized (LastFM.class) {
	        SharedPreferences prefs = context.getSharedPreferences(PREFS_FILE, 0);

            String curArtist = prefs.getString(CUR_TRACK_ARTIST, "");
            String curTitle = prefs.getString(CUR_TRACK_TITLE, "");
            long timeBegan = prefs.getLong(CUR_TRACK_BEGAN, -1);

            if (curArtist != artist || curTitle != title) {
                long playedFor = now - timeBegan;
                if (trackLen >= THIRTY_SECONDS && 
                    (playedFor >= FOUR_MINUTES || 
                     playedFor >= (trackLen / 2))) {

                    Log.d("TeaCup", "played for " + playedFor);

                    scrobble = true;
                    scrobbleArtist = curArtist;
                    scrobbleTitle = curTitle;
                    scrobbleTime = timeBegan / 1000;
                }

                SharedPreferences.Editor edit = prefs.edit(); 
                edit.putString(CUR_TRACK_ARTIST, artist);
                edit.putString(CUR_TRACK_TITLE, title);
                if (playing)
                    edit.putLong(CUR_TRACK_BEGAN, now);
                edit.commit();
            } 			
		}


		ScrobbleType scrobbleType = getScrobbleType(context, config);

        switch (scrobbleType) {
            case NOSCROBBLE: 
                break;
            case SCROBBLE:
                if (scrobble)
                    scrobbleTrack(scrobbleArtist, 
                                  scrobbleTitle, 
                                  scrobbleTime);
                sendNowPlaying(artist, title);
            case CACHE:
                if (scrobble)
                    cacheScrobble(scrobbleArtist, 
                                  scrobbleTitle, 
                                  scrobbleTime);
        }
		
	}


    private static ScrobbleType getScrobbleType(Context context, Config config) {
        boolean wifi = config.getLastFMScrobbleWifi();
        boolean network = config.getLastFMScrobbleNetwork();
        boolean cache = config.getLastFMScrobbleCache();

        boolean connect = shouldConnect(context, wifi, network);
        if (connect)
            return ScrobbleType.SCROBBLE;
        else if (cache)
            return ScrobbleType.CACHE;
        else
            return ScrobbleType.NOSCROBBLE;
    }



    private static void scrobbleTrack(String artist,
                                      String title,
                                      long time) {
        Log.d("TeaCup", "scrobble " + artist + ", " + title + ", " + time);
    }

    private static void cacheScrobble(String artist,
                                      String title,
                                      long time) {
        Log.d("TeaCup", "cache " + artist + ", " + title + ", " + time);
    }

    private static void sendNowPlaying(String artist, String title) {
        Log.d("TeaCup", "now playing " + artist + ", " + title + ".");
    }


	
	private static Bitmap getArtUnprotected(Context context,
                                            Config config, 
                                            String artist, 
                                            String album,
                                            String filename) throws InterruptedException {
		Bitmap artBmp = null;

		if (config.getLastFMCacheStyle() != Config.LASTFM_NO_CACHE)
			artBmp = getCachedArt(config, artist, album, filename);

		if (artBmp == null) {
			artBmp = getWebArt(context, config, artist, album, filename);
			if (artBmp != null)
				cacheBitmap(config, artist, album, filename, artBmp);
		}
		
		return artBmp;
	}
		
	private synchronized static void cacheBitmap(Config config,
			                                     String artist,
			                                     String album,
			                                     String filename,
			                                     Bitmap artBmp) {
		try {
			String directory = getCacheDir(config, filename);
			File dir = new File(directory);
			if (!dir.exists())
				dir.mkdirs();
			String imageName = directory +
					           File.separator +
				               getCacheFileName(artist, album);
			File image = new File(imageName);
			if (!image.exists()) {
				FileOutputStream os = new FileOutputStream(image);

				artBmp.compress(CACHE_TYPE, 85, os);
				os.flush();
				os.close();
			}
		} catch (FileNotFoundException e) {
			Log.e("TeaCup", "filenotfoundexception: " + e.toString());
		} catch (IOException e) {
			Log.e("TeaCup", "ioexception: " + e.toString());
		}
	}
	
	private synchronized static Bitmap getCachedArt(Config config, 
			                                        String artist, 
			                                        String album,
			                                        String filename) {
		String imageName = getCacheDir(config,  filename) +
				           File.separator +
				           getCacheFileName(artist, album);
		File image = new File(imageName);
		if (image.exists())
			return AlbumArtFactory.readFile(image);
		else
			return null;
	}
	
	private static String getCacheDir(Config config, String filename) {
		switch (config.getLastFMCacheStyle()) {
		case Config.LASTFM_CACHE_INDIR: return config.getLastFMDirectory();
		case Config.LASTFM_CACHE_WITHMUSIC: return new File(filename).getParent();
		default: return null;
		}
	}
	
	private static String getCacheFileName(String artist, String album) {
		String cleanArtist = artist.replaceAll("\\W+", "");
		String cleanAlbum = album.replaceAll("\\W+", "");
		
		return cleanArtist + cleanAlbum + CACHE_EXT;
	}
	
	private static Bitmap getWebArt(Context context,
			                        Config config,
			                        String artist, 
			                        String album,
			                        String filename) throws InterruptedException {
		Bitmap artBmp = null;
		
		if (shouldConnectArt(config, context)) {
			String artUrl = getArtUrl(artist, album);
			if (artUrl != null) {
				try {
					URLConnection url = makeRequest(new URL(artUrl));
					InputStream is = url.getInputStream();
					artBmp = AlbumArtFactory.readStream(is);
				} catch (MalformedURLException e) {
					Log.e("TeaCup", "LastFM produced a malformed url!" + artUrl);
				} catch (IOException e) {
					Log.d("TeaCup", "ioexception:", e);
				}
			}
		}
		
		return artBmp;
	}

    private static boolean shouldConnectArt(Config config, Context context) {
		boolean getLastFMArtWifi = config.getLastFMArtWifi();
        boolean getLastFMArtNetwork = config.getLastFMArtNetwork();
        
        return shouldConnect(context, 
                             getLastFMArtWifi, 
                             getLastFMArtNetwork);
    }
	
	private static boolean shouldConnect(Context context,
                                         boolean wifiOk,
                                         boolean networkOK) {
        ConnectivityManager cm = (ConnectivityManager)
        context.getSystemService(Context.CONNECTIVITY_SERVICE);

    	NetworkInfo wifiNetwork 
			= cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

    	if (wifiOk &&
    		wifiNetwork != null &&  
        	wifiNetwork.isConnectedOrConnecting())
    		return true;

    	NetworkInfo mobileNetwork 
			= cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
    	
    	if (networkOK &&
    	    mobileNetwork != null && 
    	    mobileNetwork.isConnectedOrConnecting())
    		return true;
    	

    	NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
    	
    	if (networkOK &&
		    activeNetwork != null && 
		    activeNetwork.isConnectedOrConnecting())
    		return true;
    	
    	return false;
	}
	
	private static String getArtUrl(String artist, String album) throws InterruptedException {
		try {
			XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
			factory.setNamespaceAware(false);
			XmlPullParser xpp = factory.newPullParser();
			
			String webArtist = URLEncoder.encode(artist, "UTF-8");
			String webAlbum = URLEncoder.encode(album, "UTF-8");			
			
			URL url = new URL(String.format(URL_TEMPLATE, webArtist, webAlbum));
			URLConnection ucon = makeRequest(url);
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
	
	private synchronized static URLConnection makeRequest(URL url) 
				throws IOException, InterruptedException {
		
		if (timeSliceBegin < 0)
			timeSliceBegin = System.currentTimeMillis();
		
		long time = System.currentTimeMillis();
		long sliceEnd = timeSliceBegin + TIME_SLICE_LEN;
				
		if (++numRequests > MAX_REQUESTS && time < sliceEnd) {
			Log.d("TeaCup", "sleep!");
			Thread.sleep(sliceEnd - time + TIME_SAFETY_BUFFER);
			numRequests = 1;
			timeSliceBegin = System.currentTimeMillis();
		} else if (time > sliceEnd) {
			numRequests = 0;
			timeSliceBegin = System.currentTimeMillis();
		}
		
		URLConnection ucon = url.openConnection();
		ucon.setConnectTimeout(URL_TIMEOUT);
		ucon.setReadTimeout(URL_TIMEOUT);
		return ucon;
	}

}
