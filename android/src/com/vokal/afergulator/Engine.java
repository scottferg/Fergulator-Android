package com.vokal.afergulator;

import com.vokal.afergulator.widget.ButtonNES;

public class Engine {


    public static native void init();
    public static native void resize(int w, int h);
    public static native void drawFrame();

    public static native boolean loadRom(byte[] bytes, String name);
    public static native void setFilePath(String path);
    public static native void pauseEmulator(boolean running);
    private static native void keyEvent(int key, int down, int player);

	static {
		System.loadLibrary("Fergulator");
	}

    public static void resume() {
        pauseEmulator(false);
    }

    public static void pause() {
        pauseEmulator(true);
    }

    public static void buttonDown(ButtonNES.Key key) {
        buttonDown(key, 0);
    }

    public static void buttonDown(ButtonNES.Key key, int player) {
        if (key != null) keyEvent(key.ordinal(), 1, player);
    }

    public static void buttonUp(ButtonNES.Key key) {
        buttonUp(key, 0);
    }

    public static void buttonUp(ButtonNES.Key key, int player) {
        if (key != null) keyEvent(key.ordinal(), 0, player);
    }

    public static void simulatePress(final ButtonNES.Key key, final int player) {
        new Thread() {
            @Override
            public void run() {
                buttonDown(key, player);
                try {
                    Thread.sleep(50);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                buttonUp(key, player);
            }
        }.start();
    }

}
