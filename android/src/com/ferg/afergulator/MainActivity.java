package com.ferg.afergulator;

import android.annotation.SuppressLint;
import android.app.*;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.input.InputManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v4.app.DialogFragment;
import android.view.*;
import android.widget.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.ferg.afergulator.widget.ButtonNES;
import com.ferg.afergulator.widget.ButtonNES.Key;

public class MainActivity extends Activity implements ActionBar.OnNavigationListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private GameView   mGameView;
    private RomAdapter romAdapter;

    protected PowerManager.WakeLock mWakeLock;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);

        setContentView(R.layout.main);

        getActionBar().setDisplayShowTitleEnabled(false);

        findViewById(R.id.frameLayout).setOnClickListener(onLayoutClick);

        romAdapter = new RomAdapter();
        getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        getActionBar().setListNavigationCallbacks(romAdapter, this);

        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);

        mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "Fergulator");
        mWakeLock.acquire();

        mGameView = (GameView) findViewById(R.id.gameView);
    }

    @Override
    protected void onResume() {
        super.onResume();

        mGameView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();

        Engine.pauseEmulator();
        Engine.saveBatteryRam();
        mGameView.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // TODO: Leave this out until save states work
        // getMenuInflater().inflate(R.menu.main_nes, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_nes_load:
                Engine.loadState();
                return true;
            case R.id.menu_nes_save:
                Engine.saveState();
                return true;
            case R.id.menu_nes_shutdown:
                Toast.makeText(this, "power down not implemented yet", Toast.LENGTH_SHORT).show();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private View.OnClickListener onLayoutClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            toggleActionBar();
        }
    };

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

    private void toggleActionBar() {
        if (getActionBar().isShowing()) {
            getActionBar().hide();
            mGameView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
        } else {
            mGameView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            getActionBar().show();
        }
    }

    private class RomAdapter extends ArrayAdapter<String> {

        public static final String SELECT_ROM = "Select ROM...";

        public RomAdapter() {
            super(MainActivity.this, R.layout.rom_spinner_item);
            try {
                String[] roms = getRoms();
                add(SELECT_ROM);
                addAll(roms);
            } catch (IOException e) {
                e.printStackTrace();
                add("NO ROMS FOUND!");
            }
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            TextView v = (TextView) super.getDropDownView(position, convertView, parent);

            if (position == getActionBar().getSelectedNavigationIndex()) {
                v.setTextColor(Integer.MAX_VALUE);
            } else {
                v.setTextColor(getResources().getColor(android.R.color.holo_blue_light));
            }

            v.setText(displayRomName(v.getText().toString()));

            return v;
        }

        private String[] getRoms() throws IOException {
            return getAssets().list("roms");
        }
    }

    private String displayRomName(String rom) {
        if (rom.endsWith(".nes")) {
            return rom.substring(0, rom.length() - 4);
        }
        return rom;
    }

    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        if (itemPosition == 0)
            return false;

        Engine.pauseEmulator();

        String rom = romAdapter.getItem(itemPosition);

        InputStream is = null;
        try {
            is = getAssets().open("roms/" + rom);
            if (mGameView.loadGame(is, rom)) {
                toggleActionBar();
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }
}
