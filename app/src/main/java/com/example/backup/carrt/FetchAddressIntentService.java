package com.example.backup.carrt;

import android.app.IntentService;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.support.annotation.Nullable;

import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Locale;

/**
 * Created by backup on 10/19/17.
 */

public class FetchAddressIntentService extends IntentService {
    protected ResultReceiver mReciever;
    String base = "https://maps.googleapis.com";

    public FetchAddressIntentService(String name) {
        super(name);
    }

    private void deliverResultToReceiver(int resultCode, String message ){
        Bundle bundle = new Bundle();
        bundle.putString(Constants.RESULT_DATA_KEY, message);
        mReciever.send(resultCode, bundle);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        Location location = intent.getParcelableExtra(Constants.LOCATION_DATA_EXTRA);

        List<Address> addresses = null;
        try{
            addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(),1);
        }catch (IOException e){

        }catch (IllegalArgumentException illegal){

        }

        if(addresses == null || addresses.size() == 0){

        }else {
            Address address = addresses.get(0);

        }
    }
    private void buildSearchURL(String searchType, String keyWord, Location myLocation) throws IOException {
        //https://maps.googleapis.com/maps/api/place/textsearch/json?query=123+main+street&location=42.3675294,-71.186966&radius=10000&key=YOUR_API_KEY
        HttpURLConnection urlConnection = null;

        String locationfull = myLocation.getLatitude() + "," + myLocation.getLongitude();
        Log.v("Location", locationfull);


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
        }catch (IOException e){

        }finally {
            if(urlConnection!=null){
                urlConnection.disconnect();
            }
        }


    }
}
