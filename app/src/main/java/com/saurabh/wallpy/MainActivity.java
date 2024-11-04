package com.saurabh.wallpy;

import android.Manifest;
import android.app.Activity;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    public static final String ACTION_WALLPAPER_STATUS = "com.saurabh.wallpy.WALLPAPER_STATUS";
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int PICK_IMAGE_REQUEST = 1;
    private static boolean stone = false;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ArrayList<Bitmap> cachedImages = new ArrayList<>();
    private GridView coursesGV;
    private ImageAdapter imageAdapter;
    private TextView editTextNumber2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "onCreate called");
        editTextNumber2 = findViewById(R.id.editTextNumber2);
        coursesGV = findViewById(R.id.idGVcourses);
        imageAdapter = new ImageAdapter(this, cachedImages);
        coursesGV.setAdapter(imageAdapter);
        updateCacheSize(); // Update cache size display
        loadCachedImages();
        setupImagePicker();
    }


    private void loadCachedImages() {
        File cacheDir = getCacheDir();
        File[] files = cacheDir.listFiles();
        if (files != null) {
            for (File file : files) {
                Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                if (bitmap != null) {
                    cachedImages.add(bitmap);
                }
            }
            imageAdapter.notifyDataSetChanged(); // Notify the adapter to refresh the grid
        }
    }

    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        if (data.getClipData() != null) {
                            int count = data.getClipData().getItemCount();
                            for (int i = 0; i < count; i++) {
                                Uri imageUri = data.getClipData().getItemAt(i).getUri();
                                processAndDisplayImage(imageUri);
                            }
                        } else if (data.getData() != null) {
                            Uri imageUri = data.getData();
                            processAndDisplayImage(imageUri);
                        }
                    } else {
                        Log.d(TAG, "No image selected or operation cancelled");
                    }
                }
        );
    }
    private void updateCacheSize() {
        File cacheDir = getCacheDir();
        long totalSize = getDirectorySize(cacheDir);
        editTextNumber2.setText(String.valueOf(totalSize/1024/1024+ "MB")); // update the EditText
    }

    private long getDirectorySize(File dir) {
        long size = 0;
        if (dir != null && dir.isDirectory()) {
            for (File file : dir.listFiles()) {
                if (file.isFile()) {
                    size += file.length();
                }
            }
        }
        return size;
    }
    private void processAndDisplayImage(Uri imageUri) {
        if (imageUri == null) {
            Toast.makeText(this, "Invalid image URI", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
            saveImageToCache(bitmap);
            cachedImages.add(bitmap);
            imageAdapter.notifyDataSetChanged();
            updateCacheSize(); // Update cache size display
            Log.d(TAG, "Image selected and displayed: " + imageUri);
        } catch (IOException e) {
            Log.e(TAG, "Failed to load image", e);
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveImageToCache(Bitmap bitmap) {
        File cacheDir = getCacheDir();
        String fileName = System.currentTimeMillis() + ".png"; // Unique filename

        File imageFile = new File(cacheDir, fileName);
        try (FileOutputStream fos = new FileOutputStream(imageFile)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            Log.d(TAG, "Image saved to cache: " + imageFile.getAbsolutePath());
        } catch (IOException e) {
            Log.e(TAG, "Error saving image to cache", e);
        }
    }

    public void setWallpaper(Bitmap bitmap) {
        WallpaperManager wallpaperManager = WallpaperManager.getInstance(getApplicationContext());
        try {
            wallpaperManager.setBitmap(bitmap);
            Toast.makeText(getApplicationContext(), "Wallpaper2 set successfully!", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "Error setting wallpaper!", Toast.LENGTH_SHORT).show();
        }
    }
    public void clearCache(View view) {
        File cacheDir = getCacheDir();
        File[] files = cacheDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.delete()) {
                    Log.d(TAG, "Deleted file: " + file.getName());
                    updateCacheSize(); // Update cache size display
                } else {
                    Log.e(TAG, "Failed to delete file: " + file.getName());
                }
            }
        }
        cachedImages.clear(); // Clear the cached images list
        imageAdapter.notifyDataSetChanged(); // Notify the adapter to refresh the grid
        Toast.makeText(this, "Cache cleared", Toast.LENGTH_SHORT).show();
    }

    public void transparent(View view) {
        Button buttonChooseWallpaper = findViewById(R.id.buttonChooseWallpaper);
        Button buttonClearCache = findViewById(R.id.buttonClearCache);
        if(stone){
            getSupportActionBar().hide();
            buttonChooseWallpaper.setVisibility(View.GONE);
            buttonClearCache.setVisibility(View.GONE);
            stone = false;
        }else {
            getSupportActionBar().show();
            buttonChooseWallpaper.setVisibility(View.VISIBLE);
            buttonClearCache.setVisibility(View.VISIBLE);
            stone = true;
        }
    }


    public void chooseWallpaper(View view) {
        if (checkPermissions()) {
            openImagePicker();
        } else {
            requestPermissions();
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        imagePickerLauncher.launch(intent);
    }

    private boolean checkPermissions() {
        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(wallpaperReceiver, new IntentFilter(ACTION_WALLPAPER_STATUS), Context.RECEIVER_NOT_EXPORTED);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(wallpaperReceiver);
    }

    private final BroadcastReceiver wallpaperReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean success = intent.getBooleanExtra("success", false);
            String message = intent.getStringExtra("message");
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
    };
}
