package com.ferg.afergulator.tools;

import android.content.Context;
import android.media.AudioManager;
import android.media.AudioTrack;

import com.ferg.afergulator.Engine;

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
    }

    public static void play(short[] samples) {
    }
}
