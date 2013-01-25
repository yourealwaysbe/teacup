/**
 * Copyright 2013 Matthew Hague (matthewhague@zoho.com)
 * Released under the GNU General Public License v3 (see GPL.txt)
 */

package net.chilon.matt.teacup;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
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
    public enum AuthResponse {
        OK, NOCONNECTION, BADREPLY, LASTFMERROR
    }

    static public class AuthResult {
        private AuthResponse response;
        private String value;

        public AuthResult(AuthResponse response, String value) {
            this.response = response;
            this.value = value;
        }

        public AuthResponse getResponse() { return response; }
        public String getValue() { return value; }
    }

    public enum PrefetchState {
        OK, NOCONNECTION, CANCELLED
    }

    private static final int URL_TIMEOUT = 5000;

    private static final String SCROBBLE_CACHE_FILENAME = "scrobble-cache";
    private static final String SCROBBLE_TEMP_CACHE_FILENAME = "temp-scrobble-cache";

    private static final String API_KEY = "d6e802774ce70edfca5d501009377a53";
    private static final String API_SECRET = "9320d44c69440dfe648bca72140fecb2";
    private static final String API_ROOT = "http://ws.audioscrobbler.com/2.0/";
    private static final String SECURE_API_ROOT = "https://ws.audioscrobbler.com/2.0/";

    private static final String GET_ALBUM_INFO = "album.getinfo";
    private static final String GET_ARTIST_INFO = "artist.getinfo";
    private static final String GET_MOBILE_SESSION = "auth.getMobileSession";
    private static final String UPDATE_NOW_PLAYING = "track.updateNowPlaying";
    private static final String SCROBBLE = "track.scrobble";

    private static final String API_KEY_ARG = "api_key";
    private static final String API_SIG_ARG = "api_sig";
    private static final String METHOD_ARG = "method";
    private static final String SESSION_KEY_ARG = "sk";
    private static final String USERNAME_ARG = "username";
    private static final String PASSWORD_ARG = "password";
    private static final String ARTIST_ARG = "artist";
    private static final String ALBUM_ARG = "album";
    private static final String TRACK_ARG = "track";
    private static final String TIMESTAMP_ARG = "timestamp";
    private static final String DURATION_ARG = "duration";

    private static final String SIMILAR_TAG = "similar";
    private static final String IMAGE_TAG = "image";
    private static final String IMAGE_SIZE = "large";
    private static final String SIZE_ATTR = "size";
    private static final String LFM_NAMESPACE = "";
    private static final String LFM_TAG = "lfm";
    private static final String LFM_STATUS_ATTR = "status";
    private static final String LFM_STATUS_OK = "ok";
    private static final String KEY_TAG = "key";
    private static final String ERROR_TAG = "error";

    private static final Set<String> SIMILAR_TAG_SET
        = new HashSet<String>() {{
            add(SIMILAR_TAG);
        }};


    private static final String ALBUM_ART_TEMPLATE
        = API_ROOT + "?" +
          API_KEY_ARG + "=" + API_KEY + "&" +
          METHOD_ARG + "=" + GET_ALBUM_INFO + "&" +
          ARTIST_ARG + "=%s&" +
          ALBUM_ARG + "=%s";

    private static final String ARTIST_ART_TEMPLATE
        = API_ROOT + "?" +
          API_KEY_ARG + "=" + API_KEY + "&" +
          METHOD_ARG + "=" + GET_ARTIST_INFO + "&" +
          ARTIST_ARG + "=%s";

    private static final int MAX_SCROBBLE_SIZE = 50;
    private static final int NUM_SCROBBLE_FIELDS = 3;
    private static final String ITH_TIMESTAMP = "timestamp[%d]";
    private static final String ITH_ARTIST = "artist[%d]";
    private static final String ITH_TITLE = "track[%d]";

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
    private static final String CUR_TRACK_LEN = "track-length";
    private static final String CUR_TRACK_TOTAL_TIME = "track-total-time";
    private static final String SESSION_KEY = "session-key";
    private static final String SESSION_KEY_USERNAME = "session-key-username";
    private static final String SESSION_KEY_PASSWORD = "session-key-password";
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
                artBmp = getWebArt(context, config, artist, album);
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
        Log.d("TeaCup", "updateScrobble(" + artist + ", " + title + ", " + playing);
        try {
            boolean scrobble = false;
            String scrobbleArtist = null;
            String scrobbleTitle = null;
            long scrobbleTime = 0;

            long now = System.currentTimeMillis();

            // Update stored data
            synchronized (PREFS_FILE) {
                SharedPreferences prefs = context.getSharedPreferences(PREFS_FILE, 0);

                String curArtist = prefs.getString(CUR_TRACK_ARTIST, "");
                String curTitle = prefs.getString(CUR_TRACK_TITLE, "");
                long timeBegan = prefs.getLong(CUR_TRACK_BEGAN, -1);
                long curTrackLen = prefs.getLong(CUR_TRACK_LEN, 0);

                Log.d("TeaCup", "from prefs " + curArtist + ", " + curTitle + ", " + curTrackLen);

                if (!curArtist.equals(artist) ||
                     !curTitle.equals(title) ||
                     !playing) {
                    Log.d("TeaCup", "passed");
                    Log.d("TeaCup", "played for " + (now - timeBegan) + " from " + timeBegan + " vs " + (curTrackLen / 2));
                    long playedFor = now - timeBegan;
                    if (trackLen >= THIRTY_SECONDS &&
                        (playedFor >= FOUR_MINUTES ||
                         playedFor >= (curTrackLen / 2))) {

                        Log.d("TeaCup", "played for " + playedFor);

                        scrobble = !"".equals(curArtist) && !"".equals(curTitle);
                        Log.d("TeaCup", "decide scrobble = " + scrobble);
                        scrobbleArtist = curArtist;
                        scrobbleTitle = curTitle;
                        scrobbleTime = timeBegan;
                    }

                    SharedPreferences.Editor edit = prefs.edit();
                    if (playing) {
                        Log.d("TeaCup", "putting args");
                        edit.putString(CUR_TRACK_ARTIST, artist);
                        edit.putString(CUR_TRACK_TITLE, title);
                        edit.putLong(CUR_TRACK_BEGAN, now);
                        edit.putLong(CUR_TRACK_LEN, trackLen);
                    } else {
                        Log.d("TeaCup", "putting null");
                        edit.putString(CUR_TRACK_ARTIST, "");
                        edit.putString(CUR_TRACK_TITLE, "");
                        edit.putLong(CUR_TRACK_BEGAN, -1);
                        edit.putLong(CUR_TRACK_LEN, 0);
                    }
                    edit.commit();
                }
            }

            switch (getScrobbleType(context, config)) {
                case NOSCROBBLE:
                    break;
                case SCROBBLE:
                    if (scrobble)
                        scrobbleTrack(context,
                                      config,
                                      scrobbleArtist,
                                      scrobbleTitle,
                                      scrobbleTime);
                    if (playing)
                        sendNowPlaying(context, config, artist, title, trackLen);
                    break;
                case CACHE:
                    if (scrobble)
                        cacheScrobble(context,
                                      scrobbleArtist,
                                      scrobbleTitle,
                                      scrobbleTime);
                    break;
            }
        } catch (InterruptedException e) {
            Log.w("TeaCup", "updateScrobble interrupted", e);
        }
    }

    public static void scrobbleCache(Config config, Context context) {
        if(getScrobbleType(context, config) != ScrobbleType.SCROBBLE)
            return;

        synchronized (SCROBBLE_CACHE_FILENAME) {
            FileInputStream fis = null;
            try {
                fis = context.openFileInput(SCROBBLE_CACHE_FILENAME);
            } catch (FileNotFoundException e) {
                fis = null;
            }

            boolean didFile = false;
            int numBatches = 0;
            if (fis != null) {
                try {
                    InputStreamReader ir = new InputStreamReader(fis);
                    BufferedReader buf = new BufferedReader(ir);
                    String artist = buf.readLine();
                    while (artist != null) {
                        artist = scrobbleCacheChunk(config,
                                                    context,
                                                    artist,
                                                    buf);
                        ++numBatches;
                    }

                    didFile = true;
                } catch (IOException e) {
                    Log.w("TeaCup", "cacheScrobble ioexception", e);
                } catch (InterruptedException e) {
                    Log.w("TeaCup", "cacheScrobble interruptedexception", e);
                }
            }


            try {
                if (fis != null) {
                    fis.close();
                    if (!didFile) {
                        // we did MAX_SCROBBLE_SIZE * numBatches of the cache
                        // contents, but not the rest.  Need to update the
                        // cache file to reflect that
                        cutDownCacheFile(context,
                                         numBatches * MAX_SCROBBLE_SIZE);

                    } else {
                        context.deleteFile(SCROBBLE_CACHE_FILENAME);
                    }
                }
            } catch (IOException e) {
                Log.w("TeaCup", "scrobbleCache ioexception", e);
            }
        }
    }

    public static long getScrobbleCacheSize(Context context) {
        long length = 0;
        synchronized (SCROBBLE_CACHE_FILENAME) {
            File cache = context.getFileStreamPath(SCROBBLE_CACHE_FILENAME);
            length = cache.length();
        }
        return length;
    }

    public static void clearScrobbleCache(Context context) {
        synchronized (SCROBBLE_CACHE_FILENAME) {
            context.deleteFile(SCROBBLE_CACHE_FILENAME);
        }
    }

    public static AuthResult testLastFMAuthentication(Context context, 
                                                      Config config) {
        boolean wifi = config.getLastFMScrobbleWifi();
        boolean network = config.getLastFMScrobbleNetwork();

        boolean connect = shouldConnect(context, wifi, network);
        if (!connect) {
            return new AuthResult(AuthResponse.NOCONNECTION, null);
        }

        try {
            AuthResult result = getSessionKey(context, config, false);
            if (result.getResponse() == AuthResponse.OK)
                return new AuthResult(AuthResponse.OK, "<key redacted>");
            else
                return result;
        } catch (Exception e) {
            return new AuthResult(AuthResponse.BADREPLY, e.getMessage());
        }
    }

    private static void cutDownCacheFile(Context context, int num) {
        FileInputStream fis = null;
        FileOutputStream fos = null;

        boolean copied = false;

        try {
            fis = context.openFileInput(SCROBBLE_CACHE_FILENAME);
            fos = context.openFileOutput(SCROBBLE_TEMP_CACHE_FILENAME,
                                         Context.MODE_PRIVATE);

            InputStreamReader ir = new InputStreamReader(fis);
            BufferedReader buf = new BufferedReader(ir);
            BufferedOutputStream bufOut = new BufferedOutputStream(fos);

            for (int i = 0; i < num * NUM_SCROBBLE_FIELDS; ++i) {
                buf.readLine();
            }

            String out = buf.readLine();
            while (out != null) {
                bufOut.write(out.getBytes());
                bufOut.write("\n".getBytes());
                out = buf.readLine();
            }
            bufOut.flush();

            copied = true;
        } catch (FileNotFoundException e) {
            Log.w("TeaCup", "cutDownCacheFile filenotfoundexception", e);
        } catch (IOException e) {
            Log.w("TeaCup", "cutDownCacheFile ioexception", e);
        } finally {
            try {
                if (fis != null)
                    fis.close();
            } catch (IOException e) {
                Log.d("TeaCup", "cutDownCacheFile ioexception", e);
            } finally {
                try {
                    if (fos != null)
                        fos.close();
                } catch (IOException e) {
                    Log.d("TeaCup", "cutDownCacheFile ioexception", e);
                }
            }
        }

        if (copied) {
            context.deleteFile(SCROBBLE_CACHE_FILENAME);
            File tmp = context.getFileStreamPath(SCROBBLE_TEMP_CACHE_FILENAME);
            File cache = context.getFileStreamPath(SCROBBLE_CACHE_FILENAME);
            tmp.renameTo(cache);
        }
        context.deleteFile(SCROBBLE_TEMP_CACHE_FILENAME);
    }

    private static String scrobbleCacheChunk(Config config,
                                             Context context,
                                             String artist,
                                             BufferedReader buf)
                throws IOException, InterruptedException {
        String sessionKey = pureGetSessionKey(context,  config,  true);

        ArrayList<NameValuePair> vals = new ArrayList<NameValuePair>(4);
        vals.add(new BasicNameValuePair(SESSION_KEY_ARG, sessionKey));
        vals.add(new BasicNameValuePair(API_KEY_ARG, API_KEY));
        vals.add(new BasicNameValuePair(METHOD_ARG, SCROBBLE));

        int i = 0;
        while (artist != null && i < MAX_SCROBBLE_SIZE) {
            String title = buf.readLine();
            String timestamp = buf.readLine();

            Log.d("TeaCup", "cache adding " + artist + ", " + title + ", " + timestamp);

            String artist_arg = String.format(Locale.ENGLISH, ITH_ARTIST, i);
            String title_arg = String.format(Locale.ENGLISH, ITH_TITLE, i);
            String timestamp_arg = String.format(Locale.ENGLISH, ITH_TIMESTAMP, i);

            vals.add(new BasicNameValuePair(artist_arg, artist));
            vals.add(new BasicNameValuePair(title_arg, title));
            vals.add(new BasicNameValuePair(timestamp_arg, timestamp));

            ++i;
            artist = buf.readLine();
        }

        Log.d("TeaCup", "sending batch!");

        String apiSig = makeApiSig(vals);
        vals.add(new BasicNameValuePair(API_SIG_ARG, apiSig));

        boolean ok = lfmIsOK(postRequest(SECURE_API_ROOT, vals));
        if (!ok) {
            Log.d("TeaCup", "retrying batch scrobble");
            // retry once with fresh key
            sessionKey = pureGetSessionKey(context, config, false);
            vals.remove(vals.size() - 1);
            resetNameValue(SESSION_KEY_ARG, sessionKey, vals);
            apiSig = makeApiSig(vals);
            vals.add(new BasicNameValuePair(API_SIG_ARG, apiSig));
            ok = lfmIsOK(postRequest(SECURE_API_ROOT, vals));
            if (!ok)
                throw new IOException("Failed to connect to lastfm!");
        }

        return artist;
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



    private static void scrobbleTrack(Context context,
                                      Config config,
                                      String artist,
                                      String title,
                                      long time)
            throws InterruptedException {
        Log.d("TeaCup", "scrobble " + artist + ", " + title + ".");
        try {
            String sessionKey = pureGetSessionKey(context, config, true);
            String timestamp = Long.toString(time / 1000);

            ArrayList<NameValuePair> vals = new ArrayList<NameValuePair>(4);
            vals.add(new BasicNameValuePair(SESSION_KEY_ARG, sessionKey));
            vals.add(new BasicNameValuePair(API_KEY_ARG, API_KEY));
            vals.add(new BasicNameValuePair(METHOD_ARG, SCROBBLE));
            vals.add(new BasicNameValuePair(ARTIST_ARG, artist));
            vals.add(new BasicNameValuePair(TRACK_ARG, title));
            vals.add(new BasicNameValuePair(TIMESTAMP_ARG, timestamp));

            String apiSig = makeApiSig(vals);
            vals.add(new BasicNameValuePair(API_SIG_ARG, apiSig));

            boolean ok = lfmIsOK(postRequest(SECURE_API_ROOT, vals));
            if (!ok) {
                Log.d("TeaCup", "retrying scrobble");
                // retry once with fresh key
                sessionKey = pureGetSessionKey(context, config, false);
                vals.remove(vals.size() - 1);
                resetNameValue(SESSION_KEY_ARG, sessionKey, vals);
                apiSig = makeApiSig(vals);
                vals.add(new BasicNameValuePair(API_SIG_ARG, apiSig));
                ok = lfmIsOK(postRequest(SECURE_API_ROOT, vals));
                if (!ok && config.getLastFMScrobbleCache()) {
                    cacheScrobble(context, artist, title, time);
                }
            }

            Log.d("TeaCup", "done scrobble " + artist + ", " + title + ".");
        } catch (IOException e) {
            Log.w("TeaCup", "scrobble ioexception", e);
        }
    }

    private static void cacheScrobble(Context context,
                                      String artist,
                                      String title,
                                      long time) {
        synchronized (SCROBBLE_CACHE_FILENAME) {
            try {
                FileOutputStream fos;
                try {
                    fos = context.openFileOutput(SCROBBLE_CACHE_FILENAME,
                                                 Context.MODE_APPEND);
                } catch (FileNotFoundException e) {
                    fos = context.openFileOutput(SCROBBLE_CACHE_FILENAME,
                                                 Context.MODE_PRIVATE);
                }
                BufferedOutputStream buf = new BufferedOutputStream(fos);
                buf.write(artist.getBytes());
                buf.write("\n".getBytes());
                buf.write(title.getBytes());
                buf.write("\n".getBytes());
                buf.write(Long.toString(time / 1000).getBytes());
                buf.write("\n".getBytes());
                buf.flush();
                fos.close();
            } catch (FileNotFoundException e) {
                Log.e("TeaCup", "cacheScrobble filenotfoundexception", e);
            } catch (IOException e) {
                Log.e("TeaCup", "cacheScrobble ioexception", e);
            }
            Log.d("TeaCup", "cache " + artist + ", " + title + ", " + time);
        }
    }

    private static void sendNowPlaying(Context context,
                                       Config config,
                                       String artist,
                                       String title,
                                       long length)
            throws InterruptedException {
        Log.d("TeaCup", "now playing " + artist + ", " + title + ".");
        try {
            String sessionKey = pureGetSessionKey(context, config, true);
            String strLength = Long.toString(length / 1000);

            ArrayList<NameValuePair> vals = new ArrayList<NameValuePair>(6);
            vals.add(new BasicNameValuePair(SESSION_KEY_ARG, sessionKey));
            vals.add(new BasicNameValuePair(API_KEY_ARG, API_KEY));
            vals.add(new BasicNameValuePair(METHOD_ARG, UPDATE_NOW_PLAYING));
            vals.add(new BasicNameValuePair(ARTIST_ARG, artist));
            vals.add(new BasicNameValuePair(TRACK_ARG, title));
            vals.add(new BasicNameValuePair(DURATION_ARG, strLength));

            String apiSig = makeApiSig(vals);
            vals.add(new BasicNameValuePair(API_SIG_ARG, apiSig));

            boolean ok = lfmIsOK(postRequest(SECURE_API_ROOT, vals));
            if (!ok) {
                Log.d("TeaCup", "retrying now playing");
                // retry once with fresh key
                sessionKey = pureGetSessionKey(context, config, false);
                vals.remove(vals.size() - 1);
                resetNameValue(SESSION_KEY_ARG, sessionKey, vals);
                apiSig = makeApiSig(vals);
                vals.add(new BasicNameValuePair(API_SIG_ARG, apiSig));
                ok = lfmIsOK(postRequest(SECURE_API_ROOT, vals));
                if (!ok) {
                    Log.w("TeaCup", "Failed to send now playing.");
                }
            }

            Log.d("TeaCup", "done now playing " + artist + ", " + title + ".");
        } catch (IOException e) {
            Log.v("TeaCup", "now playing ioexception", e);
        }
    }

    
    private static String pureGetSessionKey(Context context,
                                            Config config,
                                            boolean tryStoredKey)
                throws IOException, InterruptedException {
        AuthResult result = getSessionKey(context,  config,  true);
        if (result.getResponse() != AuthResponse.OK) 
            throw new IOException(result.getValue());
        return result.getValue();
    }


    private static AuthResult getSessionKey(Context context,
                                            Config config,
                                            boolean tryStoredKey)
                throws IOException, InterruptedException {
        AuthResult result = null;

        synchronized (PREFS_FILE) {
            String username = config.getLastFMUserName();
            String password = config.getLastFMPassword();

            SharedPreferences prefs = context.getSharedPreferences(PREFS_FILE, 0);

            String key = "";

            if (tryStoredKey) {
                String sessionUsername = prefs.getString(SESSION_KEY_USERNAME, "");
                String sessionPassword = prefs.getString(SESSION_KEY_PASSWORD, "");
                if (username.equals(sessionUsername) &&
                    password.equals(sessionPassword))
                    key = prefs.getString(SESSION_KEY, "");
            }

            // request key
            if (!"".equals(key)) {
                result = new AuthResult(AuthResponse.OK, key);
            } else {
                ArrayList<NameValuePair> vals = new ArrayList<NameValuePair>(5);
                vals.add(new BasicNameValuePair(API_KEY_ARG, API_KEY));
                vals.add(new BasicNameValuePair(METHOD_ARG, GET_MOBILE_SESSION));
                vals.add(new BasicNameValuePair(USERNAME_ARG, username));
                vals.add(new BasicNameValuePair(PASSWORD_ARG, password));

                String apiSig = makeApiSig(vals);
                vals.add(new BasicNameValuePair(API_SIG_ARG, apiSig));

                HttpResponse response = postRequest(SECURE_API_ROOT, vals);
                HttpEntity entity = response.getEntity();
                result = parseLastFMSessionResponse(entity.getContent());
                if (result.response == AuthResponse.OK) {
                    SharedPreferences.Editor edit = prefs.edit();
                    edit.putString(SESSION_KEY_USERNAME, username);
                    edit.putString(SESSION_KEY_PASSWORD, password);
                    edit.putString(SESSION_KEY, result.getValue());
                    edit.commit();
                }
            }
        }

        return result;
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
            artBmp = getWebArt(context, config, artist, album);
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
                                    String album)
                throws InterruptedException {
        Bitmap artBmp = null;

        if (shouldConnectArt(config, context)) {
            String artUrl = getAlbumArtUrl(artist, album);
            if (artUrl == null)
                artUrl = getArtistArtUrl(artist);
            if (artUrl != null) {
                try {
                    URLConnection url = makeRequest(new URL(artUrl));
                    InputStream is = url.getInputStream();
                    artBmp = AlbumArtFactory.readStream(is);
                } catch (MalformedURLException e) {
                    Log.w("TeaCup", "LastFM produced a malformed url!" + artUrl);
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

    private static String getAlbumArtUrl(String artist, String album)
                throws InterruptedException {
        try {
            String webArtist = URLEncoder.encode(artist, "UTF-8");
            String webAlbum = URLEncoder.encode(album, "UTF-8");

            URL url = new URL(String.format(ALBUM_ART_TEMPLATE,
                                            webArtist,
                                            webAlbum));
            URLConnection ucon = makeRequest(url);
            InputStream is = ucon.getInputStream();

            return grabXmlTag(is,
                              IMAGE_TAG,
                              SIZE_ATTR,
                              IMAGE_SIZE);

        } catch (IOException e) {
            // do nothing
            Log.d("TeaCup", "ioexception: " + e);
        }

        return null;
    }

    private static String getArtistArtUrl(String artist)
                throws InterruptedException {
        try {
            String webArtist = URLEncoder.encode(artist, "UTF-8");

            URL url = new URL(String.format(ARTIST_ART_TEMPLATE,
                                            webArtist));
            URLConnection ucon = makeRequest(url);
            InputStream is = ucon.getInputStream();

            return grabXmlTag(is,
                              IMAGE_TAG,
                              SIZE_ATTR,
                              IMAGE_SIZE,
                              SIMILAR_TAG_SET);

        } catch (IOException e) {
            // do nothing
            Log.d("TeaCup", "ioexception: " + e);
        }

        return null;
    }



    private static String grabXmlTag(InputStream is,
                                     String tagName) {
        return grabXmlTag(is, tagName,  null, null, null);
    }

    private static String grabXmlTag(InputStream is,
                                     String tagName,
                                     String attrName,
                                     String attrVal) {
        return grabXmlTag(is, tagName, attrName, attrVal, null);
    }

    private static String grabXmlTag(InputStream is,
                                     String tagName,
                                     String attrName,
                                     String attrVal,
                                     Set<String> ignoreTags) {
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(false);
            XmlPullParser xpp = factory.newPullParser();

            xpp.setInput(is, null);

            int eventType = xpp.getEventType();

            int ignoreDepth = 0;

            while (eventType != XmlPullParser.END_DOCUMENT) {

                if (ignoreDepth > 0 && ignoreTags != null) {
                    if (eventType == XmlPullParser.START_TAG &&
                        ignoreTags.contains(xpp.getName())) {
                        ++ignoreDepth;
                    } else if (eventType == XmlPullParser.END_TAG &&
                               ignoreTags.contains(xpp.getName())) {
                        --ignoreDepth;
                    }
                } else {
                    if (eventType == XmlPullParser.START_TAG &&
                        tagName.equals(xpp.getName())) {
                        if (attrName != null) {
                            String val = xpp.getAttributeValue(LFM_NAMESPACE, attrName);
                            if (attrVal.equals(val)) {
                                xpp.next();
                                return xpp.getText();
                            }
                        } else {
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
            Log.d("TeaCup", "grabXmlTag xmlpullparserexception", e);
        } catch (IOException e) {
            // do nothing
            Log.d("TeaCup", "grabXmlTag ioexception", e);
        }

        return null;
    }


    private static AuthResult parseLastFMSessionResponse(InputStream is) {
        AuthResponse response = null;
        String value = null;

        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(false);
            XmlPullParser xpp = factory.newPullParser();

            xpp.setInput(is, null);

            int eventType = xpp.getEventType();

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    if (LFM_TAG.equals(xpp.getName())) {
                        String val = xpp.getAttributeValue(LFM_NAMESPACE, LFM_STATUS_ATTR);
                        if (LFM_STATUS_OK.equals(val)) {
                            response = AuthResponse.OK;
                        } else {
                            response = AuthResponse.LASTFMERROR;
                        }
                    } else if (KEY_TAG.equals(xpp.getName()) ||
                               ERROR_TAG.equals(xpp.getName())) {
                        xpp.next();
                        value = xpp.getText();
                    }
                }
                
                xpp.next();
                eventType = xpp.getEventType();
            }
        } catch (XmlPullParserException e) {
            // do nothing
            Log.d("TeaCup", "grabXmlTag xmlpullparserexception", e);
        } catch (IOException e) {
            // do nothing
            Log.d("TeaCup", "grabXmlTag ioexception", e);
        }

        if (response != null && value != null) {
            return new AuthResult(response, value);
        } else {
            return new AuthResult(AuthResponse.BADREPLY, null);
        }
    }


    private static boolean lfmIsOK(HttpResponse response) {
        try {
            HttpEntity entity = response.getEntity();
            if (entity == null)
                return false;

            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(false);
            XmlPullParser xpp = factory.newPullParser();

            xpp.setInput(entity.getContent(), null);

            int eventType = xpp.getEventType();

            while (eventType != XmlPullParser.END_DOCUMENT) {

                if (eventType == XmlPullParser.START_TAG &&
                        LFM_TAG.equals(xpp.getName())) {
                    String val = xpp.getAttributeValue(LFM_NAMESPACE, LFM_STATUS_ATTR);
                    Log.d("TeaCup", "status: " + val);
                    return LFM_STATUS_OK.equals(val);
                }
                xpp.next();
                eventType = xpp.getEventType();
            }
        } catch (XmlPullParserException e) {
            // do nothing
            Log.d("TeaCup", "grabXmlTag xmlpullparserexception", e);
        } catch (IOException e) {
            // do nothing
            Log.d("TeaCup", "grabXmlTag ioexception", e);
        }

        Log.d("TeaCup", "status: bad");

        return false;
    }


    private static URLConnection makeRequest(URL url)
                throws IOException, InterruptedException {
        waitForRequestPermission();

        URLConnection ucon = url.openConnection();
        ucon.setConnectTimeout(URL_TIMEOUT);
        ucon.setReadTimeout(URL_TIMEOUT);
        return ucon;
    }

    private static HttpResponse postRequest(String url,
                                            List<NameValuePair> vals)
                throws IOException, InterruptedException {
        waitForRequestPermission();

        try {
            HttpPost post = new HttpPost(url);
            post.setEntity(new UrlEncodedFormEntity(vals, HTTP.UTF_8));
            HttpClient client = new DefaultHttpClient();
            return client.execute(post);
        } catch (ClientProtocolException e) {
            Log.e("TeaCup", "ClientProtocolException -- i guess we did something wrong.");
            throw new IOException("Failed to create client protocol: " + e.toString());
        }
    }

    private synchronized static void waitForRequestPermission()
                throws InterruptedException {

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
    }

    // Following method stolen from lastfm-java: https://code.google.com/p/lastfm-java/
    // Which is something i wish i'd found before starting this class file :)
    private static String md5Hash(String s) {
        String hash = "";
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] bytes = md5.digest(s.getBytes("UTF-8"));
            StringBuilder b = new StringBuilder(32);
            for (byte aByte : bytes) {
                    String hex = Integer.toHexString((int) aByte & 0xFF);
                    if (hex.length() == 1)
                            b.append('0');
                    b.append(hex);
            }
            return b.toString();
        } catch (NoSuchAlgorithmException e) {
            Log.e("TeaCup", "md5Hash nosuchalgorithmexception", e);
        } catch (UnsupportedEncodingException e) {
            Log.e("TeaCup", "md5Hash unsupportedencodingexception", e);
        }
        return hash;
    }

    private static void logIS(InputStream is) {
        try {
            InputStreamReader ir = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(ir);
            String read = br.readLine();
            while (read != null) {
                Log.d("TeaCup", read);
                read = br.readLine();
            }
        } catch (IOException e) {
            Log.d("TeaCup", "error logging input stream", e);
        }
    }


    private static final Comparator<NameValuePair> comparator
        = new Comparator<NameValuePair>() {
            public int compare(NameValuePair p1, NameValuePair p2) {
                return p1.getName().compareTo(p2.getName());
            }
        };

    private static String makeApiSig(ArrayList<NameValuePair> vals) {
        Collections.sort(vals, comparator);
        StringBuilder sb = new StringBuilder();
        for (NameValuePair p : vals) {
            sb.append(p.getName());
            sb.append(p.getValue());
        }
        sb.append(API_SECRET);
        Log.d("TeaCup", "apisig: " + sb.toString());
        return md5Hash(sb.toString());
    }

    private static void resetNameValue(String name,
                                       String value,
                                       ArrayList<NameValuePair> vals) {
        for (int i = 0; i < vals.size(); ++i) {
            if (vals.get(i).getName().equals(name)) {
                vals.set(i, new BasicNameValuePair(name, value));
                return;
            }
        }
    }

}
