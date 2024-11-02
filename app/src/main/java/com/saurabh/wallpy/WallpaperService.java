package com.saurabh.wallpy;

import android.app.Service;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import java.io.InputStream;

public class WallpaperService extends Service {
    private static final String TAG = "WallpaperService";
    public static final String ACTION_WALLPAPER_STATUS = "com.saurabh.wallpy.WALLPAPER_STATUS";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service started");

        if (intent == null || intent.getData() == null) {
            Log.e(TAG, "Intent or URI is null");
            sendStatusBroadcast(false, "No image selected");
            stopSelf();
            return START_NOT_STICKY;
        }

        Uri imageUri = intent.getData();
        Log.d(TAG, "Processing image: " + imageUri);

        processAndSetWallpaper(imageUri);
        return START_NOT_STICKY;
    }

    private void processAndSetWallpaper(Uri imageUri) {
        Thread thread = new Thread(() -> {
            try {
                WallpaperManager wallpaperManager = WallpaperManager.getInstance(this);
                Log.d(TAG, "Loading image from URI");

                try (InputStream inputStream = getContentResolver().openInputStream(imageUri)) {
                    if (inputStream == null) {
                        Log.e(TAG, "Failed to open input stream");
                        sendStatusBroadcast(false, "Failed to read image");
                        return;
                    }

                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    if (bitmap == null) {
                        Log.e(TAG, "Failed to decode bitmap");
                        sendStatusBroadcast(false, "Failed to process image");
                        return;
                    }

                    Log.d(TAG, "Setting wallpaper");
                    wallpaperManager.setBitmap(bitmap);
                    bitmap.recycle();

                    Log.d(TAG, "Wallpaper set successfully");
                    sendStatusBroadcast(true, "Wallpaper set successfully");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error setting wallpaper", e);
                sendStatusBroadcast(false, "Error: " + e.getMessage());
            } finally {
                stopSelf();
            }
        });

        thread.setUncaughtExceptionHandler((t, e) -> {
            Log.e(TAG, "Uncaught error in wallpaper thread", e);
            sendStatusBroadcast(false, "Unexpected error occurred");
            stopSelf();
        });

        thread.start();
    }

    private void sendStatusBroadcast(boolean success, String message) {
        try {
            Intent intent = new Intent(ACTION_WALLPAPER_STATUS);
            intent.putExtra("success", success);
            intent.putExtra("message", message);
            sendBroadcast(intent);
            Log.d(TAG, "Status broadcast sent: " + message);
        } catch (Exception e) {
            Log.e(TAG, "Error sending status broadcast", e);
        }
    }

    public static Intent createIntent(Context context, Uri imageUri) {
        Intent intent = new Intent(context, WallpaperService.class);
        intent.setData(imageUri);
        return intent;
    }
}
