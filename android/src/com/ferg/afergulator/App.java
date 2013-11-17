package com.ferg.afergulator;

import android.app.Application;

import com.ferg.afergulator.tools.Audio;


public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Audio.init(this);

    }

}
