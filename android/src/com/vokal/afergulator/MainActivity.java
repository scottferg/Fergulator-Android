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
    public boolean onTouch(View v, MotionEvent event) {
        return toggleActionBar();
    }

    private boolean toggleActionBar() {
        // TODO
        return false;
    }
}
