package com.ferg.afergulator;

import android.content.Context;
import android.graphics.Point;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import org.apache.commons.io.IOUtils;

/**
 * Created by Nick on 9/6/13.
 */
public class GameView extends GLSurfaceView implements GLSurfaceView.Renderer {

    private static final String TAG = GameView.class.getSimpleName();

    private Point mSize;

    public GameView(Context context) {
        super(context);
        init(context);
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context ctx) {
        setupContextPreserve();
        setEGLContextClientVersion(2);
        setRenderer(this);
        setDebugFlags(DEBUG_CHECK_GL_ERROR | DEBUG_LOG_GL_CALLS);

        if (!isInEditMode()) {
            Engine.setFilePath(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath());
        }
    }

    public boolean loadGame(InputStream is, String name) throws IOException {
        byte[] rom = IOUtils.toByteArray(is);
        byte[] start = Arrays.copyOfRange(rom, 0, 3);
        Log.d("GameView", String.format("%s ROM: %s (%dk)", new String(start), name, rom.length / 1024));
        boolean result = Engine.loadRom(rom, name);
        Log.d(TAG, String.format("%s, loaded = %B", name, result));
        return result;
    }

    private void setupContextPreserve() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            setPreserveEGLContextOnPause(true);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = resolveSize(0, widthMeasureSpec);
        int h = getDefaultSize(0, heightMeasureSpec);
        if (w == 0) w = Math.round(h * 240f / 224f);
        setMeasuredDimension(w, h);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.d("GameView", "Engine.init()...");
        Engine.init();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.d("GameView", String.format("Engine.resize(%d, %d)...", width, height));
        Engine.resize(width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        Engine.drawFrame();
    }
}
