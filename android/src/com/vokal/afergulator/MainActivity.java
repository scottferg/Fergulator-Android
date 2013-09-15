package com.vokal.afergulator;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.view.*;
import android.widget.ArrayAdapter;

import java.io.IOException;
import java.io.InputStream;

import com.vokal.afergulator.widget.ButtonNES;

public class MainActivity extends Activity
        implements View.OnTouchListener, ActionBar.OnNavigationListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private RomAdapter romAdapter;
    private GameView gameView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        setContentView(R.layout.main);

        getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

        romAdapter = new RomAdapter();
        getActionBar().setListNavigationCallbacks(romAdapter, this);

        gameView = (GameView) findViewById(R.id.gameView);
        gameView.setOnTouchListener(this);
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
            case R.id.menu_nes_restart:
                ButtonNES.pressReset();
                return true;

            case R.id.menu_nes_select:
                ButtonNES.pressSelect();
                return true;

            case R.id.menu_nes_start:
                ButtonNES.pressStart();
                return true;

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (v.getId()) {
            case R.id.buttonAxisBkg:
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
            gameView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
        } else {
            gameView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            getActionBar().show();
        }
    }

    private class RomAdapter extends ArrayAdapter<String> {

        public static final String SELECT_ROM = "Select ROM...";

        public RomAdapter() {
            super(MainActivity.this, android.R.layout.simple_list_item_1);
            try {
                String[] roms = getRoms();
                add(SELECT_ROM);
                addAll(roms);
            } catch (IOException e) {
                e.printStackTrace();
                add("NO ROMS FOUND!");
            }
        }

        private String[] getRoms() throws IOException {
            return getAssets().list("roms");
        }
    }

    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        String rom = romAdapter.getItem(itemPosition);
        if (RomAdapter.SELECT_ROM.equals(rom) && itemPosition == 0) {
            return false;
        } else {
            String zero = romAdapter.getItem(0);
            if (RomAdapter.SELECT_ROM.equals(zero)) {
                romAdapter.remove(zero);
            }
        }

        InputStream is = null;
        try {
            is = getAssets().open("roms/" + rom);
            gameView.loadGame(is, rom);
            return true;
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
