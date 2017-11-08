package com.example.backup.carrt;

/**
 * Created by backup on 10/26/17.
 */

public class ReviewObject {

    public String TIME = "time";
    public String RATING = "rating";
    public String AUTHOR = "NAME";
    public String REVIEW = "review";


    public ReviewObject(){

    }
    public ReviewObject(String Author, String rating, String text, String date){
        AUTHOR = Author;
        RATING = rating;
        REVIEW = text;
        TIME = date;
        //RATING = Rating;


    }
}
