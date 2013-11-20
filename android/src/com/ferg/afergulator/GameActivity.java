package com.ferg.afergulator;

import android.app.*;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Context;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.*;
import android.widget.*;

import com.google.android.glass.app.Card;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class GameActivity extends Activity {
    private static final String TAG = GameActivity.class.getSimpleName();

    private boolean audioEnabled = true;

    private GameView mGameView;

    protected PowerManager.WakeLock mWakeLock;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.game);

        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);

        mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "Fergulator");
        mWakeLock.acquire();

        mGameView = (GameView) findViewById(R.id.gameView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGameView.onResume();

        String rom = getIntent().getStringExtra("rom");
        android.util.Log.i(TAG, "Loading: " + rom);

        Engine.pauseEmulator();

        InputStream is = null;
        try {
            is = getAssets().open("roms/" + rom);
            mGameView.loadGame(is, rom);
        } catch (IOException e) {
            android.util.Log.i(TAG, e.toString());
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    android.util.Log.i(TAG, e.toString());
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        Engine.pauseEmulator();
        mGameView.onPause();
    }

    @Override
    public void onDestroy() {
        mWakeLock.release();
        super.onDestroy();
    }
}
