package com.example.backup.carrt;

import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.PlaceDetectionClient;
import com.google.android.gms.location.places.Places;
import com.tbruyelle.rxpermissions.RxPermissions;

import net.gotev.speech.GoogleVoiceTypingDisabledException;
import net.gotev.speech.Speech;
import net.gotev.speech.SpeechDelegate;
import net.gotev.speech.SpeechRecognitionNotAvailable;
import net.gotev.speech.TextToSpeechCallback;
import net.gotev.speech.ui.SpeechProgressView;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import android.Manifest;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity implements SpeechDelegate {
    SpeechProgressView progressView;
    View main;
    TextView result;
    ImageButton button;
    boolean activeState = false;

    /* state number meaning
    0= initial no specific operations performed
    1= state used for searching directions
    2= start of reading results from google maps search
    3= state inside of reading results and waiting for reponse from user
     */
    int state = 0;

    int listPosition = 0;

    protected GeoDataClient mGeoDataClient;
    protected PlaceDetectionClient mPlaceDetectionClient;
    URL searchURL;
    String base = "https://maps.googleapis.com";
    LocationManager mLocationManager;
    Location mCurrentLocation;
    public ArrayList<MapsObject> results = new ArrayList<>();
    public ArrayList<ReviewObject> reviews_results = new ArrayList<>();
    TextView logger;

    int MY_LOCATION_REQUEST_CODE = 1;
    int count = 0;
    String logText = "start";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mGeoDataClient = Places.getGeoDataClient(this, null);

        mPlaceDetectionClient = Places.getPlaceDetectionClient(this, null);
        //mPlaceDetectionClient.getCurrentPlace(null);

        mLocationManager = (LocationManager) getSystemService(Service.LOCATION_SERVICE);
        logger = findViewById(R.id.log);
      if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
          // TODO: Consider calling
          //    ActivityCompat#requestPermissions
          // here to request the missing permissions, and then overriding
          //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
          //                                          int[] grantResults)
          // to handle the case where the user grants the permission. See the documentation
          // for ActivityCompat#requestPermissions for more details.

          ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_LOCATION_REQUEST_CODE );
      }else{
          mCurrentLocation =  mLocationManager.getLastKnownLocation(mLocationManager.NETWORK_PROVIDER);
          //handleGpsSearch("jimmy johns", "restaurants", "" + mCurrentLocation.getLatitude(), "" + mCurrentLocation.getLongitude());



      }
        getPermission();
        Speech.init(this, getPackageName());




        progressView = (SpeechProgressView) this.findViewById(R.id.progress);
        result = (TextView) this.findViewById(R.id.result);
        button = (ImageButton) this.findViewById(R.id.button);
        button.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        button.setOnClickListener(view -> StartListening());



    }
    @Override
    protected void onDestroy() {

        super.onDestroy();
        Speech.getInstance().shutdown();

    }
    @Override
    public void onStartOfSpeech() {
        if(state == 6){
            Log.v("voice was ", "initialized");
            Uri gmmIntent = Uri.parse("google.navigation:q="+results.get(listPosition).ADDRESS+"&mode=w");
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntent);
            mapIntent.setPackage("com.google.android.apps.maps");
            startActivity(mapIntent);
            state = 1;

        }
    }

    @Override
    public void onSpeechRmsChanged(float value) {

        //Log.d("speech", "rms is now: " + value);
    }

    @Override
    public void onSpeechPartialResults(List<String> results) {
        StringBuilder str = new StringBuilder();
        for (String res : results) {
            str.append(res).append(" ");
        }

       // Log.i("speech", "partial result: " + str.toString().trim());
    }

    @Override
    public void onSpeechResult(String result)
    {
        this.result.setText(result);
        parseInstructions(result);
    }

    public void parseInstructions(String result){

        if(mCurrentLocation == null){
            Log.v("current Location ", "not found");
        }
        if(state == 3){
            state = 4; //since we want the new state to be able to move onto the next
            if(result.contains("read reviews")){

            }else if(result.contains("next")){
                if(listPosition < results.size()-2) {
                    ++listPosition;
                    readResults();
                }
            }else if(result.contains("directions") || result.contains("take me ") ){
                state = 6;
                StartListening();




            }else if(result.contains("call")){

            }

        }
        if(state == 2 && mCurrentLocation != null){
            MapsTasks myTask = new MapsTasks(result, "restaurants","" + mCurrentLocation.getLatitude(), "" + mCurrentLocation.getLongitude());
            if (Build.VERSION.SDK_INT>= Build.VERSION_CODES.HONEYCOMB){
                myTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }else {
                myTask.execute();
            }

        }
        if(result.contains("directions")&& state <2){
            state = 1;

            Speech.getInstance().say("where would you like to go", new TextToSpeechCallback() {

                @Override
                public void onStart() {
                    Log.i("directions", "speech started");
                }

                @Override
                public void onCompleted() {
                    Log.i("speech", "speech completed");
                    StartListening();
                }

                @Override
                public void onError() {
                    Log.i("speech", "speech error");

                }
            });
        }else if(result.contains("list") || result.contains("search for")){

            state = 2;

            Speech.getInstance().say("what would you like to search ", new TextToSpeechCallback() {

                @Override
                public void onStart() {
                    Log.i("list search", "speech started");
                }

                @Override
                public void onCompleted() {
                    Log.i("speech", "speech completed");
                   StartListening();
                }

                @Override
                public void onError() {
                    Log.i("speech", "speech error");
                }
            });
        }else if(result.contains("terminate")){
            closePrograms();
        }
    }
    public void readResults(){

            switch (state) {
                case 3:
                    StartListening();
                break;
                case 4:
                    Speech.getInstance().say(" " + results.get(listPosition).NAME, new TextToSpeechCallback() {
                        @Override
                        public void onStart() {
                            Log.i("" + listPosition, "speech started");
                        }
                        @Override
                        public void onCompleted() {
                            if (listPosition < results.size() - 2) {
                                state = 3;
                                readResults();

                            }
                        }
                        @Override
                        public void onError() {
                            Log.i("speech", "speech error");
                        }
                    });
                break;
                case 5:
                    Speech.getInstance().say("Sorry I did not find any results matching that name", new TextToSpeechCallback() {
                        @Override
                        public void onStart() {
                            Log.i("" + listPosition, "speech started");
                        }
                        @Override
                        public void onCompleted() {
                            if (listPosition < results.size() - 2) {
                                state = 3;
                                readResults();
                            }
                        }
                        @Override
                        public void onError() {
                            Log.i("speech", "speech error");
                        }
                    });

            }


    }
    private void onRecordAudioPermissionGranted() {
        final Handler handler = new Handler();
//        handler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                //Do something after 100ms
//            }
//        }, 100);100
        try {
            if(Speech.getInstance().isListening()) {
            Speech.getInstance().stopListening();
            Speech.getInstance().stopTextToSpeech();
            Speech.getInstance().shutdown();
            }
            Speech.init(this, getPackageName());
            Speech.getInstance().startListening(progressView, MainActivity.this);


        }catch(SpeechRecognitionNotAvailable exc){
        } catch(GoogleVoiceTypingDisabledException exc) {

        }

    }
    //Calling this method results in a continous listening state
    private void StartListening(){

        onRecordAudioPermissionGranted();
    }
    private void getPermission(){

            RxPermissions.getInstance(getBaseContext())
                    .request(Manifest.permission.RECORD_AUDIO)
                    .subscribe(granted -> {
                        if (granted) {
                            //onRecordAudioPermissionGranted();
                        } else {
                            Toast.makeText(MainActivity.this, R.string.app_name, Toast.LENGTH_LONG);
                        }
                    });
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == MY_LOCATION_REQUEST_CODE) {
            if (permissions.length == 1 &&
                    permissions[0] == Manifest.permission.ACCESS_FINE_LOCATION &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            } else {
                // Permission was denied. Display an error message.
            }
        }
    }

    public void handleGpsSearch(String keyword, String searchType, String lat, String lon){
       MapsSyncAdapter.syncImmediately(this,keyword, searchType, mCurrentLocation.getLatitude()+"", mCurrentLocation.getLongitude()+"");
    }//Build search query

    private boolean handlePermissions(){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            return true;
        }else{

            return false;
        }
    }

    private class MapsTasks extends AsyncTask<String, Void, String>{
        String Key;
        String Type;
        String Latitude;
        String Longitude;

        public MapsTasks(String keyword, String searchType, String lat, String lon) {
            Key = keyword;
            Type = searchType;
            Latitude = lat;
            Longitude = lon;
        }

        @Override
        protected String doInBackground(String... urls) {


            if(isNetworkAvailable()){
                try {
                    getJsonDataMapResults(buildSearchURL(Key, Latitude, Longitude));
                    Log.v("json data search called", " ");
                } catch (IOException e) {
                    Log.v("IOException called", " ");
                    e.printStackTrace();
                } catch (JSONException e) {
                    Log.v("JSONException called", " ");
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            Log.v("async task was created", " check do in background ");
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(String aVoid) {
            if(results.size() >0) {
                state = 4;
                listPosition = 0;
                readResults();
            }else{
                state = 5;
                readResults();
            }
            Log.v("Post execute complete? ", "success or failure? ");
        }



        @Override
        protected void onCancelled() {
            super.onCancelled();
        }

        public void execute(String s, Object o, Object o1) {

        }
    }


    private boolean isNetworkAvailable(){
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public void getJsonDataMapResults(String url) throws JSONException {
        //logInfo("getJSON");
        final String get_RESULTS = "results";
        final String get_NAME = "name";
        final String get_OPEN = "open_now";
        final String get_OPENING_HOURS = "opening_hours";
        final String get_TYPES = "types";
        final String get_RATING = "rating";
        final String get_ADDRESS = "formatted_address";
        final String get_GEOMETRY = "geometry";
        final String get_LOCATION = "location";
        final String get_LATITUDE = "lat";
        final String get_LONGITUDE = "lng";
        final String get_ID = "place_id";

        JSONObject object = new JSONObject(url);
        Log.v("Json object string: ",object.toString());
        JSONArray search_result = object.getJSONArray(get_RESULTS);
        Log.v("Json array created: ",search_result.toString());
       // logInfo(search_result.length()+ " ");
        if(results.size()>0){
            results.clear();
        }

        for (int i = 0; i < search_result.length(); ++i) {
            JSONObject current = search_result.getJSONObject(i);
            JSONObject currentGeo = current.getJSONObject(get_GEOMETRY);
            JSONObject currentGeoLoc = currentGeo.getJSONObject(get_LOCATION);
            String latitude = currentGeoLoc.getString(get_LATITUDE);
            String longitude = currentGeoLoc.getString(get_LONGITUDE);
            results.add(new MapsObject(current.getString(get_NAME),
                 //   current.getJSONObject(get_OPENING_HOURS).getString(get_OPEN),
                    "",
                    current.getString(get_TYPES),
                    current.getString(get_RATING),
                    current.getString(get_ADDRESS), latitude, longitude,
                    current.getString(get_ID)));
        }

        count = results.size();
        Log.v("Count = ", search_result.length()+"");
    }
    public String buildSearchURL(String keyWord, String lat, String lon) throws IOException {
        //https://maps.googleapis.com/maps/api/place/textsearch/json?query=123+main+street&location=42.3675294,-71.186966&radius=10000&key=YOUR_API_KEY
        HttpURLConnection urlConnection = null;
        BufferedReader reader;

        String locationfull = lat + "," + lon;
        Log.v("Location ", locationfull);
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
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                return null;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {

                buffer.append(line + "\n");
            }

            if (buffer.length() == 0) {
                return null;
            }
            searchURLString = buffer.toString();


            return searchURLString;
        }catch (IOException e){

        }finally {
            if(urlConnection!=null){
                urlConnection.disconnect();

            }
        }
        return searchURLString;

    }
    public String buildSearchURLReviews(String id) throws IOException {
        //URL Example
        //https://maps.googleapis.com/maps/api/place/details/json?placeid=ChIJGYLm53XswogRXIKW2OqzD6E&key=APIKEY
        HttpURLConnection urlConnection = null;
        BufferedReader reader;

        String searchURLString = null;

        Uri mapsURL = Uri.parse(base).buildUpon()
                .appendPath("maps")
                .appendPath("api")
                .appendPath("place")
                .appendPath("details")
                .appendPath("json")
                .appendQueryParameter("placeid", id)
                .appendQueryParameter("key", "AIzaSyCEB1OEzUFhGesOQaTOfzXXAaM5A2yLgBM")
                .build();
        try{
            URL searchURL = new URL(mapsURL.toString() + "&location=");

            Log.v("URL", searchURL.toString());
            urlConnection = (HttpURLConnection) searchURL.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                return null;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {

                buffer.append(line + "\n");
            }

            if (buffer.length() == 0) {
                return null;
            }
            searchURLString = buffer.toString();


            return searchURLString;
        }catch (IOException e){

        }finally {
            if(urlConnection!=null){
                urlConnection.disconnect();

            }
        }
        return searchURLString;

    }
    public String getPhoneNumber(String url) throws JSONException{
        final String get_PHONE = "formatted_phone_number";
        JSONObject object = new JSONObject(url);

        return  object.getJSONObject(get_PHONE).toString();
    }
    public void createReviewsList(String url) throws JSONException {
        //logInfo("getJSON");
        final String get_REVIEWS = "reviews";
        final String get_PHONE = "formatted_phone_number";
        final String get_RATING = "rating"; //used twice once for getting the average and for each instance of a review
        final String get_REVIEWER_NAME = "author_name";
        final String get_REVIEWER_TEXT = "text";
        final String get_REVIEWER_TIME = "relative_time_description";



        JSONObject object = new JSONObject(url);
        Log.v("Json object string: ",object.toString());
        JSONArray search_reviews = object.getJSONArray(get_REVIEWS);

        // logInfo(search_result.length()+ " ");
        if(reviews_results.size()>0){
            reviews_results.clear();
        }

        for (int i = 0; i < search_reviews.length(); ++i) {
            JSONObject current = search_reviews.getJSONObject(i);
            //String Author, String rating, String text, String date
            reviews_results.add(new ReviewObject(current.getString(get_REVIEWER_NAME),
                    current.getString(get_RATING),
                    current.getString(get_REVIEWER_TEXT),
                    current.getString(get_REVIEWER_TIME)));
        }
        

    }
    private void closePrograms(){
        try
        {
            Process suProcess = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(suProcess.getOutputStream());

            os.writeBytes("adb shell" + "\n");
            os.flush();

            Context newContext=this;
            ActivityManager activityManager = (ActivityManager) newContext.getSystemService( Context.ACTIVITY_SERVICE );
            List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
            for(ActivityManager.RunningAppProcessInfo appProcess : appProcesses){
                if(appProcess.processName.equals("com.yourPackageName")){
                }
                else{
                    os.writeBytes("am force-stop "+appProcess.processName + "\n");
                }
            }

            os.flush();
            os.close();
            suProcess.waitFor();

        }

        catch (IOException ex)
        {
            ex.getMessage();
            Toast.makeText(getApplicationContext(), ex.getMessage(),Toast.LENGTH_LONG).show();
        }
        catch (SecurityException ex)
        {
            Toast.makeText(getApplicationContext(), "Can't get root access2",
                    Toast.LENGTH_LONG).show();
        }
        catch (Exception ex)
        {
            Toast.makeText(getApplicationContext(), "Can't get root access3",
                    Toast.LENGTH_LONG).show();
        }
    }



}
