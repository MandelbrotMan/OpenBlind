package com.example.backup.carrt;

import android.location.Location;

/**
 * Created by backup on 10/20/17.
 */

public class MapsObject {
    public String NAME = "name";
    public String OPEN = "open_now";
    public String TYPES = "types";
    public String RATING = "rating";
    public String ADDRESS = "formatted_address";
    public String LATITUDE;
    public String LONGTITUDE;

    public MapsObject(){

    }
    public MapsObject(String name, String Open, String Types, String Rating, String Address, String lat, String lon){
        NAME = name;
        OPEN = Open;
        TYPES = Types;
        RATING = Rating;
        ADDRESS = Address;
        LATITUDE = lat;
        LONGTITUDE = lon;

    }
}
