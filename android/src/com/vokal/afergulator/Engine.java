package com.vokal.afergulator;

public class Engine {

    public static final int ButtonA      = 0;
    public static final int ButtonB      = 1;
    public static final int ButtonSelect = 2;
    public static final int ButtonStart  = 3;
    public static final int AxisUp       = 4;
    public static final int AxisDown     = 5;
    public static final int AxisLeft     = 6;
    public static final int AxisRight    = 7;

    static native void resize(int w, int h);
    static native void init();
    static native void drawFrame();
    static native void onTouch(int action, float x, float y);
    static native void loadRom(byte[] bytes, int length);
    static native void keyEvent(int key, int down, int player);

	static {
		System.loadLibrary("Fergulator");
	}

}
