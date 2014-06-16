package com.ferg.afergulator;

import android.app.ActionBar;
import android.app.Activity;
import android.content.*;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

import java.io.*;
import java.util.Set;
import java.util.prefs.Preferences;

import butterknife.*;
import timber.log.Timber;

public class MainActivity extends Activity implements ActionBar.OnNavigationListener {

    private static final int FILE_SELECT_CODE = 0xc001;

    static {
        if (BuildConfig.DEBUG)
            Timber.plant(new Timber.DebugTree());
    }

    @InjectView(R.id.gameView) GameView mGameView;

    private RomAdapter        romAdapter;
    private SharedPreferences mRecentPrefs;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);

        setContentView(R.layout.main);
        ButterKnife.inject(this);

        mRecentPrefs = getSharedPreferences("recent", MODE_PRIVATE);

        getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        getActionBar().setDisplayShowTitleEnabled(false);
        setSpinnerAdapter();
    }

    public void setSpinnerAdapter() {
        romAdapter = new RomAdapter();
        getActionBar().setListNavigationCallbacks(romAdapter, this);
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

    @OnClick(R.id.frameLayout)
    public void toggleActionBar() {
        if (getActionBar().isShowing()) {
            getActionBar().hide();
            mGameView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
        } else {
            mGameView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            getActionBar().show();
        }
    }

    private class RomAdapter extends ArrayAdapter<String> {

        int mHighlightColor;

        public RomAdapter() {
            super(MainActivity.this, R.layout.rom_spinner_item);

            mHighlightColor = getResources().getColor(android.R.color.holo_blue_light);

            add("Select ROM:");
            add("Browse...");

            Set<String> recent = (mRecentPrefs.getAll() == null) ?
                                 null : mRecentPrefs.getAll().keySet();

            if (recent != null) {
                addAll(recent);
            }
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            TextView v = (TextView) super.getDropDownView(position, convertView, parent);

            int i = getActionBar().getSelectedNavigationIndex();
            v.setTextColor(position == i ? Color.WHITE : mHighlightColor);

            return v;
        }
    }

    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        if (itemPosition == 0) return false;

        if (itemPosition == 1) {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*; application/zip");
            intent.addCategory(Intent.CATEGORY_OPENABLE);

            try {
                startActivityForResult(Intent.createChooser(intent, "Select a NES or ZIP file:"), FILE_SELECT_CODE);
            } catch (ActivityNotFoundException ex) {
                // Potentially direct the user to the Market with a Dialog
                Toast.makeText(this, "Please install a File Manager.", Toast.LENGTH_SHORT).show();
            }

            getActionBar().setSelectedNavigationItem(0);
            return false;
        }

        Engine.pauseEmulator();

        String rom = romAdapter.getItem(itemPosition);
        String romUriString = mRecentPrefs.getString(rom, null);
        if (romUriString != null) {
            loadRom(Uri.parse(romUriString));
        }

        return false;
    }

    private void loadRom(Uri uri) {
        Timber.d("Loading ROM: %s", uri);

        String name = uri.getLastPathSegment();
        if (name == null) return;

        if (name.endsWith(".nes")) {
            InputStream is = null;
            try {
                is = getContentResolver().openInputStream(uri);
                if (mGameView.loadGame(is, name)) {
                    toggleActionBar();
                }
            } catch (IOException e) {
                Timber.e(e, "%s", e.getMessage());
            } finally {
                closeSilently(is);
            }
        } else if (name.endsWith(".zip")) {
            Timber.d("ZIP File: TODO");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FILE_SELECT_CODE && resultCode == RESULT_OK) {

            Uri uri = data.getData();
            if (uri != null) {

                String name = uri.getLastPathSegment();

                if (name.endsWith(".zip")) {
                    Toast.makeText(this, "Zip files not implemented yet.", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!name.endsWith(".nes")) {
                    Toast.makeText(this, "Only NES roms are accepted right now (*.nes)", Toast.LENGTH_LONG).show();
                    return;
                }

                name = displayRomName(name);

                mRecentPrefs.edit().putString(name, uri.toString()).apply();

                romAdapter.remove(name);
                romAdapter.insert(name, 2);
                getActionBar().setSelectedNavigationItem(2);
            }
        }
    }

    private String displayRomName(String rom) {
        if (rom.endsWith(".nes")) {
            return rom.substring(0, rom.length() - 4);
        }
        return rom;
    }

    static void closeSilently(Closeable aCloseable) {
        if (aCloseable != null) {
            try {
                aCloseable.close();
            } catch (IOException ignored) { }
        }
    }

}
