package com.vokal.afergulator;

import android.app.Application;
import android.util.Log;

/**
 * Created by Nick on 9/12/13.
 */
public class App extends Application {

    public static boolean running;
    public static boolean playing;

    @Override
    public void onCreate() {
        super.onCreate();

        initAudio.start();
    }

    private Thread initAudio = new Thread() {
        @Override
        public void run() {
            Log.d("App.initAudio", "Engine.createAudioEngine()");
            Engine.createAudioEngine();
        }
    };
}
