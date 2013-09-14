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
        implements GLSurfaceView.Renderer {

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
    }

    public void loadGame(InputStream is, String name) throws IOException {
        byte[] rom = IOUtils.toByteArray(is);
        byte[] start = Arrays.copyOfRange(rom, 0, 3);
        Log.d("GameView", "ROM NAME: " + name);
        Log.d("GameView", "ROM TYPE: " + new String(start));
        Log.d("GameView", "ROM SIZE: %d kb" + rom.length / 1024);
        Engine.loadRom(rom, name);
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

}
