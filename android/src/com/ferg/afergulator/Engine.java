package com.ferg.afergulator;

import com.ferg.afergulator.widget.ButtonNES;

public class Engine {

    public interface KeyListener {
        void onKeyEvent(int key, int down);
    }

    public static native void init();
    public static native void resize(int w, int h);
    public static native void drawFrame();
    public static native void startProfile();
    public static native void stopProfile();

    public static native void initAudio(int minBufferSize);
    public static native void setFilePath(String path);
    public static native boolean loadRom(byte[] bytes, String name);
    public static native void pauseEmulator();
    public static native void enableAudio(boolean enabled);
    public static native void saveBatteryRam();
    public static native void saveState();
    public static native void loadState();
    private static native void keyEvent(int key, int down, int player);

	static {
		System.loadLibrary("Fergulator");
	}

    public static void buttonDown(ButtonNES.Key key) {
        buttonDown(key, 0);
    }

    public static void buttonDown(ButtonNES.Key key, int player) {
        if (key != null) {
            buttonDown(key.ordinal(), player);
        }
    }

    public static void buttonDown(int keyId, int player) {
        keyEvent(keyId, 1, player);
        if (player == 0)
            notifyListeners(keyId, 1);

    }

    public static void buttonUp(ButtonNES.Key key) {
        buttonUp(key, 0);
    }

    public static void buttonUp(ButtonNES.Key key, int player) {
        if (key != null) {
            buttonUp(key.ordinal(), player);
        }
    }

    public static void buttonUp(int keyId, int player) {
        keyEvent(keyId, 0, player);
        if (player == 0) notifyListeners(keyId, 0);
    }

    private static void notifyListeners(int keyId, int event) {
//        for (KeyListener listener : mKeyListeners) {
//            listener.onKeyEvent(key.ordinal(), event);
//        }
        if (mKeyListener != null) mKeyListener.onKeyEvent(keyId, event);
    }

    public static void setKeyListener(KeyListener aKeyListener) {
        mKeyListener = aKeyListener;
    }

    private static KeyListener mKeyListener;
//    private static List<KeyListener> mKeyListeners = new ArrayList<KeyListener>();

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

    static void playSamples(short[] samples) {
    }

}
