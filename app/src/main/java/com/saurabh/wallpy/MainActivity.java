// MainActivity.java
package com.saurabh.wallpy;

import android.Manifest;
import android.app.Activity;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import android.graphics.Bitmap;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.io.IOException;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;



public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_CODE = 100;
    private ImageView[] imageViews;
    boolean imageSet = false;
    private boolean isImageViewFull = false;
    int current_image_index = 0;

    private static final int PICK_IMAGE_REQUEST = 1;
    private ProgressBar progressBar;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "onCreate called");


        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.INVISIBLE);
        // Initialize ImageView array
        imageViews = new ImageView[] {
                findViewById(R.id.selected_image_view),
                findViewById(R.id.selected_image_view2),
                findViewById(R.id.selected_image_view3),
                findViewById(R.id.selected_image_view4),
                findViewById(R.id.selected_image_view5),
                findViewById(R.id.selected_image_view6),
                findViewById(R.id.selected_image_view7),
                findViewById(R.id.selected_image_view8),
                findViewById(R.id.selected_image_view9)

        };

        // Load cached images into the corresponding ImageView
        for (int i = 0; i < imageViews.length; i++) {
            if (imageViews[i] != null) { // Ensure the ImageView is not null
                Bitmap cachedImage = loadImageFromCache(this, i + ".png"); // Use a consistent naming pattern
                if (cachedImage != null) {
                    imageViews[i].setImageBitmap(cachedImage); // Set the cached image to the ImageView
                } else {
                    Log.d(TAG, "No cached image found for index " + i);
                }
            } else {
                Log.e(TAG, "ImageView at index " + i + " is null");
            }
        }
        for (int i = 0; i < imageViews.length; i++) {
            final int index = i;  // Create a final variable to capture the current index
            imageViews[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    WallpaperManager wallpaperManager = WallpaperManager.getInstance(getApplicationContext());
                    try {
                        wallpaperManager.setBitmap(((BitmapDrawable) imageViews[index].getDrawable()).getBitmap());
                        Toast.makeText(getApplicationContext(), "Wallpaper set successfully!", Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(getApplicationContext(), "Error setting wallpaper!", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
        setupImagePicker();
    }


    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();

                        // Check if multiple images are selected
                        if (data.getClipData() != null) {
                            int count = Math.min(data.getClipData().getItemCount(), imageViews.length); // Limit to ImageView array size
                            for (int i = 0; i < count; i++) {
                                Uri imageUri = data.getClipData().getItemAt(i).getUri();
                                processAndDisplayImage(imageUri, i);
                            }
                        } else if (data.getData() != null) {
                            // Single image selected
                            Uri imageUri = data.getData();
                            processAndDisplayImage(imageUri, 0); // Display in the first ImageView
                        }
                    } else {
                        Log.d(TAG, "No image selected or operation cancelled");
                    }
                }
        );
    }

    private void processAndDisplayImage(Uri imageUri, int index) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
            saveImageToCache(this, bitmap, index + ".png");

            // Display the image in the respective ImageView
            if (imageViews[index].getDrawable() == null) {
                imageViews[index].setImageBitmap(bitmap);
            }

            displayImage(bitmap); // Additional display if needed
            Log.d(TAG, "Image selected and displayed at index " + index + ": " + imageUri);
        } catch (IOException e) {
            Log.e(TAG, "Failed to load image", e);
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
        }
    }

    private void displayImage(Bitmap bitmap) {
        if (bitmap == null) {
            Log.e(TAG, "Error: Bitmap is null");
            return;
        }


        // Hide other ImageViews
        if (imageViews[current_image_index] != null) {
            if (imageViews[current_image_index].getDrawable() != null) {
                imageViews[current_image_index].setImageBitmap(bitmap);
                imageViews[current_image_index].setVisibility(View.VISIBLE);
                //break;
                current_image_index++;
            }
        } else {
            Log.e(TAG, "Error: imageViews[" + current_image_index + "] is null");
        }

    }



    public void chooseWallpaper(View view) {
        Log.d(TAG, "Choose wallpaper button clicked");
        try {
            if (checkPermissions()) {
                openImagePicker();
            } else {
                requestPermissions();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in chooseWallpaper", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public static void saveImageToCache(Context context, Bitmap bitmap, String fileName) {
        // Get the cache directory
        File cacheDir = context.getCacheDir();
        File imageFile = new File(cacheDir, fileName);

        // Write the bitmap to a file in cache
        try (FileOutputStream fos = new FileOutputStream(imageFile)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            System.out.println("Image saved to cache: " + imageFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Bitmap loadImageFromCache(Context context, String fileName) {
        File cacheDir = context.getCacheDir();
        File imageFile = new File(cacheDir, fileName);

        if (imageFile.exists()) {
            return BitmapFactory.decodeFile(imageFile.getAbsolutePath());
        }
        return null;
    }
    private boolean checkPermissions() {
        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        Log.d(TAG, "Requesting permissions");
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                PERMISSION_REQUEST_CODE);
    }

    private void openImagePicker() {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            imagePickerLauncher.launch(intent);

        } catch (Exception e) {
            Log.e(TAG, "Error opening image picker", e);
            Toast.makeText(this, "Error opening image picker", Toast.LENGTH_SHORT).show();
        }
    }

    private void setWallpaper(Uri imageUri) {
        try {
            Log.d(TAG, "Setting wallpaper with URI: " + imageUri);
            progressBar.setVisibility(View.VISIBLE);
            Intent serviceIntent = WallpaperService.createIntent(this, imageUri);
            startService(serviceIntent);
        } catch (Exception e) {
            Log.e(TAG, "Error starting wallpaper service", e);
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private final BroadcastReceiver wallpaperReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                progressBar.setVisibility(View.GONE);
                boolean success = intent.getBooleanExtra("success", false);
                String message = intent.getStringExtra("message");
                Log.d(TAG, "Received wallpaper status - Success: " + success + ", Message: " + message);
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e(TAG, "Error in broadcast receiver", e);
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        try {
            registerReceiver(wallpaperReceiver,
                    new IntentFilter(WallpaperService.ACTION_WALLPAPER_STATUS));
            Log.d(TAG, "Registered wallpaper receiver");
        } catch (Exception e) {
            Log.e(TAG, "Error registering receiver", e);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(wallpaperReceiver);
            Log.d(TAG, "Unregistered wallpaper receiver");
        } catch (Exception e) {
            Log.e(TAG, "Error unregistering receiver", e);
        }
    }
}
