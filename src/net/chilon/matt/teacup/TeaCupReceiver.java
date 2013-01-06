package net.chilon.matt.teacup;

import java.io.File;
import java.io.FileFilter;

import net.chilon.matt.teacup.R;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.provider.MediaStore;
import android.support.v4.content.CursorLoader;
import android.widget.RemoteViews;


public class TeaCupReceiver extends BroadcastReceiver {

    private class MetaData {
        String artist;
        String title;
        String filename;
    }

    private static final int ART_WIDTH = 72;
    private static final int ART_HEIGHT = 72;

    @Override
    public void onReceive(Context context, Intent intent) {
        Config config = new Config(context);
        PlayerConfig player = config.getPlayer();
        String action = intent.getAction();
        if (player.getMetaChangedAction().equals(action)) {
            updateMeta(config, context, intent);
        }
        if (player.getPlaystateChangedAction().equals(action)) {
            updatePlaystate(config, context, intent);
        }
    }

    private void updateMeta(Config config,
                            Context context,
                            Intent intent) {
        MetaData meta = getMeta(config, context, intent);

        String artist;
        String title;
        Bitmap artBmp;

        if (meta != null) {
            artBmp = getArtBmp(meta.filename, config, context);
            artist = meta.artist;
            title = meta.title;
        } else {
            artist = context.getResources().getString(R.string.noartist);
            title = context.getResources().getString(R.string.notitle);
            artBmp = getDefaultArt(context);
        }

        updateWidget(context, artist, title, artBmp);
    }

    private void updatePlaystate(Config config,
                                 Context context,
                                 Intent intent) {
        Bitmap playButton = getPlayButton(config, context, intent);
        updateWidget(context, playButton);
    }

    private Bitmap getPlayButton(Config config,
                                 Context context,
                                 Intent intent) {
        String playingField = config.getPlayer().getPlaystateChangedPlaying();
        boolean playing = intent.getBooleanExtra(playingField, false);
        int imgId = playing ? R.drawable.ic_pause : R.drawable.ic_play;
        return BitmapFactory.decodeResource(context.getResources(),
                                            imgId);
    }

    private Bitmap getArtBmp(String filename,
                             Config config,
                             Context context) {
        Bitmap artBmp = null;

        boolean getEmbeddedArt = config.getEmbeddedArt();
        boolean getDirectoryArt = config.getDirectoryArt();

        if (getEmbeddedArt)
            artBmp = getFileEmbeddedArt(filename);
        if (artBmp == null && getDirectoryArt)
            artBmp = getImageFromDirectory(filename);

        if (artBmp == null) {
            artBmp = getDefaultArt(context);
        }

        return artBmp;
    }

    private Bitmap getImageFromDirectory(String filename) {
        Bitmap artBmp = null;

        File file = new File(filename);
        String directory = file.getParent();

        if (directory != null) {
            FileFilter imageFilter = new FileFilter() {
                public boolean accept(File file) {
                    String filename = file.getName();
                    return filename.endsWith(".jpg") ||
                           filename.endsWith(".jpeg") ||
                           filename.endsWith(".bmp") ||
                           filename.endsWith(".png") ||
                           filename.endsWith(".gif");
                }
            };

            File[] files = new File(directory).listFiles(imageFilter);
            for (int i = 0; i < files.length && artBmp == null; ++i) {
                artBmp = decodeSampledBitmap(files[i],
                                             ART_WIDTH,
                                             ART_HEIGHT);
            }
        }
        return artBmp;
    }


    private Bitmap getDefaultArt(Context context) {
        return BitmapFactory.decodeResource(context.getResources(),
                                            R.drawable.ic_blankalbum);
    }

    private Bitmap getFileEmbeddedArt(String filename) {
        Bitmap artBmp = null;

        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(filename);
        byte[] artArray = retriever.getEmbeddedPicture();
        if (artArray != null) {
            artBmp = decodeSampledBitmapFromBytes(artArray,
                                                  ART_WIDTH,
                                                  ART_HEIGHT);
        }

        return artBmp;
    }

    private MetaData getMeta(Config config,
                             Context context,
                             Intent intent) {
        PlayerConfig player = config.getPlayer();

        long id = intent.getLongExtra(player.getMetaChangedId(), -1);

        MetaData meta = null;

        if (id  >= 0) {
            String selectionArgs[] = {
                Long.toString(id)
            };
            String projection[] = {
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DATA
            };
            String selection = MediaStore.Audio.Media._ID + " = ?";
            CursorLoader q = new CursorLoader(context,
                                              MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                              projection,
                                              selection,
                                              selectionArgs,
                                              null);
            Cursor result = q.loadInBackground();
            if (result.getCount() > 0) {
                result.moveToFirst();
                meta = new MetaData();
                meta.artist = result.getString(0);
                meta.title = result.getString(1);
                meta.filename = result.getString(2);
            }
        }

        return meta;
    }

    private void updateWidget(Context context,
                              String artist,
                              String title,
                              Bitmap artBmp) {
        Context appContext = context.getApplicationContext();
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(appContext);
        RemoteViews views = new RemoteViews(appContext.getPackageName(),
                                            R.layout.teacup);

        views.setTextViewText(R.id.artistView, artist);
        views.setTextViewText(R.id.titleView,  title);
        views.setImageViewBitmap(R.id.albumArtButton, artBmp);

        ComponentName thisWidget = new ComponentName(context, TeaCup.class);
        appWidgetManager.updateAppWidget(thisWidget, views);
    }

    private void updateWidget(Context context,
                              Bitmap playButton) {
        Context appContext = context.getApplicationContext();
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(appContext);
        RemoteViews views = new RemoteViews(appContext.getPackageName(),
                                            R.layout.teacup);

        views.setImageViewBitmap(R.id.playPauseButton, playButton);

        ComponentName thisWidget = new ComponentName(context, TeaCup.class);
        appWidgetManager.updateAppWidget(thisWidget, views);
    }

    // code below from android tutorial:
    // https://developer.android.com/training/displaying-bitmaps/load-bitmap.html

    private static Bitmap decodeSampledBitmap(File file,
                                              int reqWidth,
                                              int reqHeight) {
        String path = file.getPath();
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;
        options.inPurgeable = true;
        return BitmapFactory.decodeFile(path, options);
    }

    private static Bitmap decodeSampledBitmapFromBytes(byte[] data,
                                                       int reqWidth,
                                                       int reqHeight) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length);
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;
        options.inPurgeable = true;
        return BitmapFactory.decodeByteArray(data, 0, data.length);
    }

    private static int calculateInSampleSize(BitmapFactory.Options options,
                                             int reqWidth,
                                             int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            if (width > height) {
                inSampleSize = Math.round((float)height / (float)reqHeight);
            } else {
                inSampleSize = Math.round((float)width / (float)reqWidth);
            }
        }

        return inSampleSize;
    }


}
