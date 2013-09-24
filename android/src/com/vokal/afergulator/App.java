package com.vokal.afergulator;

import android.app.Application;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;
import android.util.Log;

import static android.content.pm.PackageManager.FEATURE_AUDIO_LOW_LATENCY;
import static android.media.AudioFormat.CHANNEL_OUT_MONO;
import static android.media.AudioFormat.ENCODING_PCM_16BIT;


public class App extends Application {

    private static final int AUDIO_SAMPLE_RATE = 44100;

    @Override
    public void onCreate() {
        super.onCreate();

        boolean claimsFeature = getPackageManager().hasSystemFeature(FEATURE_AUDIO_LOW_LATENCY);
        if (claimsFeature) Log.i("App", "System Features: AUDIO_LOW_LATENCY");

        initAudio.start();
    }

    private Thread initAudio = new Thread() {
        @Override
        public void run() {
            int sr = AUDIO_SAMPLE_RATE;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
                sr = Integer.parseInt(am.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE));
                String fpb = am.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER);
                Log.i("App", String.format("JB audio props: sampleRate=%s, framesPerBuffer=%s", sr, fpb));
            }

            int minBuffer = AudioTrack.getMinBufferSize(sr, CHANNEL_OUT_MONO, ENCODING_PCM_16BIT);

            Engine.initAudio(minBuffer);

            audioTrack = new AudioTrack(AudioManager.STREAM_SYSTEM, sr,
                                        CHANNEL_OUT_MONO, ENCODING_PCM_16BIT,
                                        minBuffer, AudioTrack.MODE_STREAM);

            audioTrack.play();
        }
    };

}
