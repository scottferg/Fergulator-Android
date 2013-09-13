package com.vokal.afergulator;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends Activity implements View.OnTouchListener {

    private GameView gameView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        findViewById(R.id.buttonA).setOnTouchListener(this);
        findViewById(R.id.buttonB).setOnTouchListener(this);
        findViewById(R.id.buttonUp).setOnTouchListener(this);
        findViewById(R.id.buttonDown).setOnTouchListener(this);
        findViewById(R.id.buttonLeft).setOnTouchListener(this);
        findViewById(R.id.buttonRight).setOnTouchListener(this);

        findViewById(R.id.buttonAxisBkg).setOnTouchListener(this);
        findViewById(R.id.buttonCtrlBkg).setOnTouchListener(this);

        gameView = (GameView) findViewById(R.id.gameView);

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
    public boolean onTouch(View v, MotionEvent event) {
        if (v.getTag() != null) {
            try {
                int key = Integer.parseInt(v.getTag().toString());
                if (key >= 0 && key < 8) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            Engine.keyEvent(key, 1, 0);
                            break;

                        case MotionEvent.ACTION_UP:
                            Engine.keyEvent(key, 0, 0);
                            break;
                    }
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
        return true;
    }
}
