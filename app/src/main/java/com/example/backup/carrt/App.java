package com.example.backup.carrt;

import android.app.Application;

import net.gotev.speech.Logger;
import net.gotev.speech.Speech;


public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        Speech.init(this, getPackageName());
        Logger.setLogLevel(Logger.LogLevel.DEBUG);
    }
}
