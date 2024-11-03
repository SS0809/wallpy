package com.saurabh.wallpy;

import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.GridView;
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

        // Set a long-click listener to set the wallpaper
        imageView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);
                try {
                    wallpaperManager.setBitmap(images.get(position)); // Pass the Bitmap directly
                    Toast.makeText(context, "Wallpaper set successfully!", Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(context, "Error setting wallpaper!", Toast.LENGTH_SHORT).show();
                }
                return true; // Return true to indicate that the long click was handled
            }
        });

        return imageView;
    }
}
