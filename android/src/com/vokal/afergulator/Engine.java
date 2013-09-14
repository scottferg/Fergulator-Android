package com.vokal.afergulator;

public class Engine {

    public static native void init();
    public static native void resize(int w, int h);
    public static native void drawFrame();

    public static native void loadRom(byte[] bytes, String name);
    public static native void keyEvent(int key, int down, int player);

	static {
		System.loadLibrary("Fergulator");
	}

}
