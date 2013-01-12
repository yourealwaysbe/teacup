package net.chilon.matt.teacup;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

public class AlbumArtFactory {

    private static final int ART_WIDTH = 120;
    private static final int ART_HEIGHT = 120;
	
    // code below from android tutorial (more or less):
    // https://developer.android.com/training/displaying-bitmaps/load-bitmap.html

    public static Bitmap readFile(File file) {
        String path = file.getPath();
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        options.inSampleSize = calculateInSampleSize(options);
        options.inJustDecodeBounds = false;
        options.inPurgeable = true;
        return BitmapFactory.decodeFile(path, options);
    }

    public static Bitmap readBytes(byte[] data) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, options);
        options.inSampleSize = calculateInSampleSize(options);
        options.inJustDecodeBounds = false;
        options.inPurgeable = true;
        return BitmapFactory.decodeByteArray(data, 0, data.length, options);
    }
    

    public static Bitmap readUrl(String url) {
    	Bitmap artBmp = null;
    	
    	try {
    		Log.d("TeaCup", "Getting: '" + url + "'");
    		
    		HttpURLConnection ucon = (HttpURLConnection)new URL(url).openConnection();
    		
    		int response = ucon.getResponseCode();
    		
    		if(response == HttpURLConnection.HTTP_OK) {
    			// can't calculate for downsizing here... (unless we do the request twice)
    			InputStream is = ((URLConnection)ucon).getInputStream();
    			artBmp = BitmapFactory.decodeStream(is);
    		}
    	} catch (IOException e) {
    		// do nothing
    		Log.d("TeaCup", "art io exception: " + e);
    	}
    	
    	return artBmp;
    }
    
    

    private static int calculateInSampleSize(BitmapFactory.Options options) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > ART_HEIGHT || width > ART_WIDTH) {
            if (width > height) {
                inSampleSize = Math.round((float)height / (float)ART_WIDTH);
            } else {
                inSampleSize = Math.round((float)width / (float)ART_HEIGHT);
            }
        }

        return inSampleSize;
    }

}
