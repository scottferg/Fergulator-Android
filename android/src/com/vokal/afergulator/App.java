package com.vokal.afergulator;

import android.app.Application;

import com.vokal.afergulator.tools.Audio;


public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Audio.init(this);

    }

}
