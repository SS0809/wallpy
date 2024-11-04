package com.saurabh.wallpy;

import android.app.AlertDialog;
import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;

public class ImageAdapter extends BaseAdapter {
    private Context context;
    private ArrayList<Bitmap> images;

    public ImageAdapter(Context context, ArrayList<Bitmap> images) {
        this.context = context;
        this.images = images;
    }

    @Override
    public int getCount() {
        return images.size();
    }

    @Override
    public Object getItem(int position) {
        return images.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView imageView;
        if (convertView == null) {
            // Create a new ImageView and set layout parameters
            imageView = new ImageView(context);
            imageView.setLayoutParams(new GridView.LayoutParams(720, 1080)); // Set the size for the image
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP); // Crop the image to fit
            imageView.setPadding(0, 0, 0, 0); // Set padding if needed
        } else {
            imageView = (ImageView) convertView;
        }

        // Load the image into imageView
        imageView.setImageBitmap(images.get(position));



        Bitmap selectedImage = images.get(position);
        int byteCount = selectedImage.getByteCount();
        double sizeInMB = byteCount / 1024.0 / 1024.0;
        String sizeMessage = String.format("-- %.2f MB", sizeInMB);

        TextView title = new TextView(context);
        title.setText("Set Wallpaper "+sizeMessage);
        title.setTextSize(20);
        title.setPadding(50, 50, 10, 10);
        title.setTextColor(Color.parseColor("#6BFFFFFF"));
        imageView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                new AlertDialog.Builder(context)
                        .setCustomTitle(title)
                        .setIcon(R.mipmap.ic_launcher)
                        .setItems(new String[]{"Home Screen", "Lock Screen"}, (dialog, which) -> {
                            WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);
                            if (which == 0) {
                                try {
                                    wallpaperManager.setBitmap(images.get(position), null, true, WallpaperManager.FLAG_SYSTEM);
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                                Toast.makeText(context, "Home Screen wallpaper set successfully!", Toast.LENGTH_SHORT).show();
                            } else if (which == 1) { // Lock Screen
                                try {
                                    wallpaperManager.setBitmap(images.get(position), null, true, WallpaperManager.FLAG_LOCK);
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                                Toast.makeText(context, "Lock Screen wallpaper set successfully!", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .show();
                return true; // Return true to indicate that the long click was handled
            }
        });



        return imageView;
    }
}
