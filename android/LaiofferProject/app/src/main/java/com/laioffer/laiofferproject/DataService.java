package com.laioffer.laiofferproject;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.util.LruCache;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.RequestFuture;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for retrieving data from backend.
 */
public class DataService {

    // Constants used to construct the connection to our backend.
    private static final String BASE_HOSTNAME = "laiproject.mooo.com";
    private static final String BASE_PORT = "8080";
    private static final String BASE_URL = "http://" + BASE_HOSTNAME + ":" + BASE_PORT + "/Rest/";

    private RequestQueue queue;
    private LruCache<String, Bitmap> bitmapCache;

    /**
     * Constructor.
     */
    public DataService(RequestQueue queue) {
        this.queue = queue;

        // Get max available VM memory, exceeding this amount will throw an
        // OutOfMemory exception.
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

        // Use 1/8th of the available memory for this memory cache.
        final int cacheSize = maxMemory / 8;
        Log.e("Cache size", Integer.toString(cacheSize));

        bitmapCache = new LruCache<String, Bitmap>(cacheSize) {

            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                // The cache size will be measured in kilobytes.
                return bitmap.getByteCount() / 1024;
            }

        };
    }

    /**
     * Get nearby restaurants.
     */
    public List<Restaurant> getNearbyRestaurants() {
        String[] request = {"GetRestaurantsNearby", "37.38", "-122.08"};
        return sendPostRequest(request);
    }

    /**
     * Download an Image from the given URL, then decodes and returns a Bitmap object.
     */
    public Bitmap getBitmapFromURL(String imageUrl) {
        Bitmap bitmap = bitmapCache.get(imageUrl);
        if (bitmap == null) {
            try {
                URL url = new URL(imageUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();
                bitmap = BitmapFactory.decodeStream(input);
                bitmapCache.put(imageUrl, bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return bitmap;
    }
    /**
     * Fire a SetVisitedRestaurants request.
     */
    public List<Restaurant> setVisitedRestaurants(String businessId) {
        String[] request = {"SetVisitedRestaurants", businessId};
        return sendPostRequest(request);
    }

    /**
     * Fire a RecommendRestaurants request.
     */
    public List<Restaurant> RecommendRestaurants() {
        String[] request = {"RecommendRestaurants"};
        return sendPostRequest(request);
    }

    /*
     * Send a post request to the backend.
     */
    private List<Restaurant> sendPostRequest(String[] request){
        try {
            String url = BASE_URL + request[0];

            // Construct payload.
            JSONObject payload = new JSONObject();
            payload.put("user_id", "1111");
            if (request[0].equals("GetRestaurantsNearby")) {
                payload.put("lat", Double.parseDouble(request[1]));
                payload.put("lon", Double.parseDouble(request[2]));
            } else if (request[0].equals("SetVisitedRestaurants")){
                payload.put("visited", new JSONArray().put(request[1]));
            } else if (request[0].equals("RecommendRestaurants")) {
            // Add payload here
            }

            RequestFuture<JSONArray> future = RequestFuture.newFuture();
            JsonArrayRequest jsonRequest = new JsonArrayRequest(
                    Request.Method.POST, url, payload, future, future);
            queue.add(jsonRequest);

            return parseResponse(future.get());

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Parse the JSON response.
     */
    private List<Restaurant> parseResponse(JSONArray response)  {
        List<Restaurant> restaurants = new ArrayList<>();
        if (response == null) {
            return restaurants;
        }
        try {
            for (int i = 0; i < response.length(); i++) {
                JSONObject business = response.getJSONObject(i);
                if (business != null) {
                    String businessId = business.getString("business_id");
                    String name = business.getString("name");
                    String type = business.getString("categories");
                    double lat = business.getDouble("latitude");
                    double lng = business.getDouble("longitude");
                    String address = business.getString("full_address")+", "+business.getString("city")+", "+business.getString("state");
                    // Download the image.
                    Bitmap thumbnail = getBitmapFromURL(business.getString("image_url"));

                    restaurants.add(
                            new Restaurant(businessId, name, address, type, lat, lng, thumbnail));
                }
            }
            return restaurants;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

}