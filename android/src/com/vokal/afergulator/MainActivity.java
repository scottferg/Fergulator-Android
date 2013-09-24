package com.vokal.afergulator;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

import java.io.IOException;
import java.io.InputStream;

import com.vokal.afergulator.widget.ButtonNES;

public class MainActivity extends Activity implements ActionBar.OnNavigationListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private RomAdapter romAdapter;
    private GameView gameView;
    private boolean audioEnabled = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        setContentView(R.layout.main);
        getActionBar().setDisplayShowTitleEnabled(false);

        findViewById(R.id.frameLayout).setOnClickListener(toggleActionBar);

        romAdapter = new RomAdapter();
        getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        getActionBar().setListNavigationCallbacks(romAdapter, this);

        gameView = (GameView) findViewById(R.id.gameView);
    }

    @Override
    protected void onResume() {
        super.onResume();

        gameView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();

        Engine.pauseEmulator();
        gameView.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_nes, menu);
        updateAudio(menu.findItem(R.id.menu_nes_audio));
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
            case R.id.menu_nes_audio:
                audioEnabled = !audioEnabled;
                Engine.enableAudio(audioEnabled);
                updateAudio(item);
                return true;
            case R.id.menu_nes_shutdown:
                Toast.makeText(this, "power down not implemented yet", Toast.LENGTH_SHORT).show();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateAudio(MenuItem menuItem) {
        if (audioEnabled) {
            menuItem.setIcon(R.drawable.ic_action_device_access_volume_on);
            menuItem.setTitle("Mute");
        } else {
            menuItem.setIcon(R.drawable.ic_action_device_access_volume_muted);
            menuItem.setTitle("Muted");
        }
    }

    private View.OnClickListener toggleActionBar = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (getActionBar().isShowing()) {
                getActionBar().hide();
                gameView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
            } else {
                gameView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
                getActionBar().show();
            }
        }
    };

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
        if (itemPosition == 0) return false;

        Engine.pauseEmulator();

        String rom = romAdapter.getItem(itemPosition);

        InputStream is = null;
        try {
            is = getAssets().open("roms/" + rom);
            if (gameView.loadGame(is, rom)) {
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
