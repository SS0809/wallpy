package com.saurabh.wallpy;

import static com.saurabh.wallpy.MainActivity.cachedImages;
import static com.saurabh.wallpy.MainActivity.imageAdapter;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.GridView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;



public class FetchImagesTask extends AsyncTask<String, Void, ArrayList<Bitmap>> {
    private static final String TAG = "FetchImagesTask";
    private MainActivity mainActivity;
    private int selectedNumber;

    public FetchImagesTask(MainActivity activity, int selectedNumber) {
        this.mainActivity = activity;
        this.selectedNumber = selectedNumber;
    }

    @Override
    protected ArrayList<Bitmap> doInBackground(String... urls) {
        ArrayList<Bitmap> bitmaps = new ArrayList<>();
        String jsonResponse = "";

        try {
            // Use selectedNumber to construct the URL
            URL url = new URL(urls[0] + "&maxKeys=" + selectedNumber);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

            try {
                int responseCode = urlConnection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream in = urlConnection.getInputStream();
                    jsonResponse = readStream(in);
                } else {
                    InputStream errorStream = urlConnection.getErrorStream();
                    String errorResponse = readStream(errorStream);
                    Log.e(TAG, "Error response code: " + responseCode + ", response: " + errorResponse);
                    return null; // Exit if there's an error
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching images", e);
            }


            Log.d(TAG, "Server response: " + jsonResponse); // Log server response

            // Parse the JSON response
            JSONObject jsonObject = new JSONObject(jsonResponse);
            JSONArray jsonArray = jsonObject.getJSONArray("files");

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject imageJson = jsonArray.getJSONObject(i);
                String imageUrl = imageJson.getString("key");

                Bitmap bitmap = getBitmapFromURL("url/fetch-file?bucket=&key=" + imageUrl);
                if (bitmap != null) {
                    bitmaps.add(bitmap);
                } else {
                    Log.e(TAG, "Bitmap is null for URL: " + imageUrl);
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "JSON parsing error", e);
        } catch (Exception e) {
            Log.e(TAG, "Error fetching images", e);
        }

        return bitmaps;
    }

    // Helper method to read the input stream
    private String readStream(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }


    @Override
    protected void onPostExecute(ArrayList<Bitmap> bitmaps) {
        if (bitmaps != null && !bitmaps.isEmpty()) {
            for (int i =0 ;i<bitmaps.size();i++)
            {
                mainActivity.saveImageToCache(bitmaps.get(i));
            }
            cachedImages.addAll(bitmaps);
            imageAdapter.notifyDataSetChanged(); // Notify adapter to refresh the GridView
            mainActivity.updateCacheSize(); // Update cache size display
        }
    }



    private Bitmap getBitmapFromURL(String strURL) {
        try {
            URL url = new URL(strURL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            return BitmapFactory.decodeStream(input);
        } catch (Exception e) {
            Log.e(TAG, "Error fetching image from URL", e);
            return null;
        }
    }


}
