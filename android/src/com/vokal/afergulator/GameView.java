package com.vokal.afergulator;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import org.apache.commons.io.IOUtils;

/**
 * Created by Nick on 9/6/13.
 */
public class GameView extends GLSurfaceView
        implements GLSurfaceView.Renderer,
        View.OnTouchListener, View.OnLongClickListener {

    public GameView(Context context) {
        super(context);
        init();
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setupContextPreserve();
        setEGLContextClientVersion(2);
        setRenderer(this);
        setOnTouchListener(this);
    }

    public void loadGame(InputStream is, String s) throws IOException {
        byte[] rom = IOUtils.toByteArray(is);
        byte[] start = Arrays.copyOfRange(rom, 0, 3);
        Log.d("GameView", "ROM TYPE: " + new String(start));
        Log.d("ENGINE", String.format("ROM SIZE: %d kb", rom.length / 1024));
        Engine.loadRom(rom, rom.length);
    }

    private void setupContextPreserve() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            setPreserveEGLContextOnPause(true);
        }
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.d("ENGINE", "Engine.init()...");
        Engine.init();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.d("ENGINE", String.format("Engine.resize(%d, %d)...", width, height));
        Engine.resize(width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        Engine.drawFrame();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getActionIndex() == 0) {
            Engine.onTouch(event.getActionMasked(), event.getX(), event.getY());
            return true;
        }
        return false;
    }

    @Override
    public boolean onLongClick(View v) {
        Engine.keyEvent(Engine.ButtonSelect, 1, 0);
        Engine.keyEvent(Engine.ButtonSelect, 0, 0);
        return true;
    }
}
