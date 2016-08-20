package com.hoanguyenhs.utility;

import android.util.Log;

import org.json.JSONObject;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

/**
 * Created by HoaNguyenHS on 10/8/2015.
 */
public class GeocodingProcessor {
    private static final String LOG_TAG = "Geocoding";
    private static final String API_URL = "https://maps.googleapis.com/maps/api/geocode/json";
    private static final String API_KEY = "AIzaSyCF-LfcMIWUMw2sJZRGVPy0c_6tBt53c48";

    public static String getLatLngFromAddress(String address) {
        String result = null;
        HttpURLConnection conn = null;
        StringBuilder jsonResults = new StringBuilder();
        try {
            StringBuilder sb = new StringBuilder(API_URL);
            sb.append("?address=" + URLEncoder.encode(address, "utf8"));
            sb.append("&bounds=-46.468226,168.423727|-46.352353,168.313006");
            sb.append("&components=country:nz");
            sb.append("&components=postal_code_prefix:981");
            sb.append("&key=" + API_KEY);

            URL url = new URL(sb.toString());
            conn = (HttpURLConnection) url.openConnection();
            InputStreamReader in = new InputStreamReader(conn.getInputStream());

            // Load the results into a StringBuilder
            int read;
            char[] buff = new char[1024];
            while ((read = in.read(buff)) != -1) {
                jsonResults.append(buff, 0, read);
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, LOG_TAG, e);
            return result;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        try {
            // Create a JSON object hierarchy from the results
            JSONObject jsonObj = new JSONObject(jsonResults.toString());
            JSONObject results = jsonObj.getJSONArray("results").getJSONObject(0);
            JSONObject geometry = results.getJSONObject("geometry");
            JSONObject location = geometry.getJSONObject("location");
            String lat = location.getString("lat");
            String lng = location.getString("lng");

            result = lat + "," + lng;
        } catch (Exception e) {
            Log.e(LOG_TAG, LOG_TAG, e);
            return result;
        }

        return result;
    }
}
