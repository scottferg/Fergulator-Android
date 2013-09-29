package com.vokal.afergulator.tools;

import android.content.Context;
import android.media.AudioManager;
import android.media.AudioTrack;

import com.vokal.afergulator.Engine;

import static android.content.pm.PackageManager.FEATURE_AUDIO_LOW_LATENCY;
import static android.media.AudioFormat.CHANNEL_OUT_MONO;
import static android.media.AudioFormat.ENCODING_PCM_16BIT;

/**
 * Created by Nick on 9/28/13.
 */
public class Audio {


    private static final int AUDIO_SAMPLE_RATE = 44100;
    public static AudioTrack sAudioTrack;

    public static void init(Context aContext) {

        boolean claimsFeature = aContext.getPackageManager().hasSystemFeature(FEATURE_AUDIO_LOW_LATENCY);
        if (claimsFeature)
            Log.i("App", "System Features: AUDIO_LOW_LATENCY");

        initAudio.start();
    }

    public static void play(short[] samples) {
        sAudioTrack.write(samples, 0, samples.length);
    }

    private static Thread initAudio = new Thread() {
        @Override
        public void run() {
            int sampleRate = AUDIO_SAMPLE_RATE;

//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
//                AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
//                sampleRate = Integer.parseInt(am.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE));
//                String fpb = am.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER);
//                Log.i("App", String.format("JB audio props: sampleRate=%s, framesPerBuffer=%s", sampleRate, fpb));
//            }

            int minBuffer = AudioTrack.getMinBufferSize(sampleRate, CHANNEL_OUT_MONO, ENCODING_PCM_16BIT);

            Engine.initAudio(minBuffer);

            sAudioTrack = new AudioTrack(AudioManager.STREAM_SYSTEM, sampleRate,
                                         CHANNEL_OUT_MONO, ENCODING_PCM_16BIT,
                                         minBuffer, AudioTrack.MODE_STREAM);

            sAudioTrack.play();
        }
    };

}
