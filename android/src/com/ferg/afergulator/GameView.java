package com.ferg.afergulator;

import android.content.Context;
import android.graphics.Point;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Environment;
import android.util.AttributeSet;

import java.io.*;
import java.util.Arrays;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import com.google.common.io.ByteStreams;
import timber.log.Timber;
import go.nesdroid.*;

/**
 * Created by Nick on 9/6/13.
 */
public class GameView extends GLSurfaceView implements GLSurfaceView.Renderer {

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
        setDebugFlags(DEBUG_CHECK_GL_ERROR | DEBUG_CHECK_GL_ERROR);

        setRenderer(this);

        if (!isInEditMode()) {
//            String cache = ctx.getExternalCacheDir().getAbsolutePath();
            File dl = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            Timber.d("Einitngine.setFilePath(): %s", dl.getAbsoluteFile());
            Nesdroid.SetFilePath(dl.getAbsolutePath());
        }
    }

    public boolean loadGame(InputStream is, String name) throws IOException {
        byte[] rom = ByteStreams.toByteArray(is);
        byte[] start = Arrays.copyOfRange(rom, 0, 3);
        Timber.d("%s ROM: %s (%dk)", new String(start), name, rom.length / 1024);
        boolean result = Nesdroid.LoadRom(rom, name);
        Timber.d("%s, loaded = %B", name, result);

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
        Timber.i("Engine.init()...");
        Nesdroid.Start();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Timber.i("Engine.resize(%d, %d)...", width, height);
        Nesdroid.Resize(width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        Nesdroid.DrawFrame();
    }
}
