package com.ferg.afergulator;

import android.app.*;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Context;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.*;
import android.widget.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.ferg.afergulator.widget.ButtonNES;
import com.ferg.afergulator.widget.ButtonNES.Key;

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

        mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "Fergulator");
        mWakeLock.acquire();

        mGameView = (GameView) findViewById(R.id.gameView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGameView.onResume();

        String rom = getIntent().getStringExtra("rom");
        android.util.Log.i(TAG, "Loading: " + rom);

        // Nesdroid.PauseEmulator();

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

        // Nesdroid.PauseEmulator();
        mGameView.onPause();
    }

    @Override
    public void onDestroy() {
        mWakeLock.release();
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Key nesKey = ButtonNES.keyFromKeyCode(keyCode);
        if (nesKey != null) {
            Engine.buttonDown(nesKey);
            return true;
        }

        return false;
    }
     
     @Override
     public boolean onKeyUp(int keyCode, KeyEvent event) {
         Key nesKey = ButtonNES.keyFromKeyCode(keyCode);
         if (nesKey != null) {
             Engine.buttonUp(nesKey);
             return true;
         }

         return false;
     }
}
