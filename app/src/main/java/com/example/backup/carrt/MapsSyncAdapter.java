package com.example.backup.carrt;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncResult;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by backup on 10/19/17.
 */

public class MapsSyncAdapter extends AbstractThreadedSyncAdapter {

    String base = "https://maps.googleapis.com";
    public ArrayList<MapsObject> results = new ArrayList<>();


    public MapsSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    @Override
    public void onPerformSync(Account account, Bundle bundle, String s, ContentProviderClient contentProviderClient, SyncResult syncResult) {
        String keyword = bundle.getString(Constants.KEYWORDS);
        String searchType = bundle.getString(Constants.TYPE);
        String lat = bundle.getString(Constants.LATITUDE);
        String lon = bundle.getString(Constants.LONGITUDE);
        Log.v("Start of sync", "hello");
        String searchURL = null;


        try {
             searchURL = buildSearchURL(searchType, keyword, lat, lon);
        } catch (IOException e) {
             e.printStackTrace();
        }
        if(isNetworkAvailable() && searchURL!= null){
            try {
                getJsonData(searchURL, lat, lon);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }



    }
    public String buildSearchURL(String searchType, String keyWord, String lat, String lon) throws IOException {
        //https://maps.googleapis.com/maps/api/place/textsearch/json?query=123+main+street&location=42.3675294,-71.186966&radius=10000&key=YOUR_API_KEY
        HttpURLConnection urlConnection = null;

        String locationfull = lat + "," + lon;
        Log.v("Location", locationfull);
        String searchURLString = null;

        Uri mapsURL = Uri.parse(base).buildUpon()
                .appendPath("maps")
                .appendPath("api")
                .appendPath("place")
                .appendPath("textsearch")
                .appendQueryParameter("query",  keyWord)
                .appendQueryParameter("radius", "500")
                .appendQueryParameter("key", "AIzaSyCEB1OEzUFhGesOQaTOfzXXAaM5A2yLgBM")
                .appendPath("json").build();
        try{
            URL searchURL = new URL(mapsURL.toString() + "&location="+locationfull);

            Log.v("URL", searchURL.toString());
            urlConnection = (HttpURLConnection) searchURL.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();
            searchURLString = searchURL.toString();
            return searchURLString;
        }catch (IOException e){

        }finally {
            if(urlConnection!=null){
                urlConnection.disconnect();

            }
        }
        return searchURLString;

    }
    public void getJsonData(String url, String lat, String lon) throws JSONException {
        final String get_RESULTS = "results";
        final String get_NAME = "name";
        final String get_OPEN = "open_now";
        final String get_TYPES = "types";
        final String get_RATING = "rating";
        final String get_ADDRESS = "formatted_address";
        final String get_GEOMETRY = "geometry";
        final String get_LATITUDE = "latitude";
        final String get_LONGITUDE = "longitude";

        JSONObject object = new JSONObject(url);
        JSONArray search_result = object.getJSONArray(get_RESULTS);
        if(results.size()>0){
            results.clear();
        }

        for (int i = 0; i < search_result.length(); ++i) {
            JSONObject current = search_result.getJSONObject(i);
            JSONObject currentGeo = current.getJSONObject(get_GEOMETRY);
            String latitude = currentGeo.getString(get_LATITUDE);
            String longitude = currentGeo.getString(get_LONGITUDE);
//            results.add(new MapsObject(current.getString(get_NAME),
//                    current.getString(get_OPEN),
//                    current.getString(get_TYPES),
//                    current.getString(get_RATING),
//                    current.getString(get_ADDRESS), lat, lon));
        }
        Log.v("Size of search: ", results.size()+"");
    }

   public static void syncImmediately(Context context, String lat, String lon, String keywords, String searchType){
        Log.v("Sync called", "called");
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        bundle.putString(Constants.LATITUDE,lat);
        bundle.putString(Constants.LONGITUDE,lon);
        bundle.putString(Constants.KEYWORDS, keywords);
        bundle.putString(Constants.TYPE, searchType);
        ContentResolver.requestSync(null, context.getString(R.string.content_authority), bundle);
    }

    private boolean isNetworkAvailable(){
        ConnectivityManager connectivityManager = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}
