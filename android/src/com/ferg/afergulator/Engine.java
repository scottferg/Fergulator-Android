package com.ferg.afergulator;

import com.ferg.afergulator.widget.ButtonNES;
import go.nesdroid.*;

public class Engine {

    public interface KeyListener {
        void onKeyEvent(int key, int down);
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
        Nesdroid.KeyEvent(keyId, 1, player);
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
        Nesdroid.KeyEvent(keyId, 0, player);
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
