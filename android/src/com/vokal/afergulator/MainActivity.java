package com.vokal.afergulator;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.*;

import java.io.IOException;
import java.io.InputStream;

import com.vokal.afergulator.widget.ButtonNES;

public class MainActivity extends Activity implements View.OnTouchListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private GameView gameView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        gameView = (GameView) findViewById(R.id.gameView);
        gameView.setOnTouchListener(this);

        if (!App.running) {
            try {
                final String[] assetList = getAssets().list("roms");
                AlertDialog.Builder bldr = new AlertDialog.Builder(this);
                bldr.setSingleChoiceItems(assetList, -1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            Log.d("MainActivity", "loading rom: " + assetList[which]);
                            InputStream is = getAssets().open("roms/" + assetList[which]);
                            gameView.loadGame(is, assetList[which]);
                            App.running = true;
                            dialog.dismiss();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
                bldr.setTitle("Choose ROM:").create().show();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        gameView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();

        gameView.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_nes, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_nes_select:
                ButtonNES.pressSelect();
                return true;

            case R.id.menu_nes_start:
                ButtonNES.pressStart();
                App.playing = !App.playing;
                invalidateOptionsMenu();
                if (App.playing) {
                    getActionBar().hide();
                    gameView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LOW_PROFILE);
                }
                return true;

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (v.getId()) {
            case R.id.buttonAxisBkg:
            case R.id.buttonCtrlBkg:
                return true;

            default:
                if (MotionEvent.ACTION_DOWN == event.getAction())
                    toggleActionBar();
        }

        return true;
    }

    private void toggleActionBar() {
        if (getActionBar().isShowing()) {
            getActionBar().hide();
            gameView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LOW_PROFILE);
        } else {
            gameView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            getActionBar().show();
        }
    }
}
