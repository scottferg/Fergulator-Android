package com.ferg.afergulator.tools;

import com.ferg.afergulator.BuildConfig;

public class Log {

    public static final String TAG = "LOG";

    private static final boolean DEBUGGING = BuildConfig.DEBUG;

    public static void e(Object tag, Object msg) {
        String t = getTag(tag);
        String m = getMessage(msg);
        android.util.Log.e(t, m);
    }

    public static void e(Object tag, String format, Object... args) {
        e(tag, String.format(format, args));
    }

    public static void w(Object tag, Object msg) {
        String t = getTag(tag);
        String m = getMessage(msg);
        android.util.Log.w(t, m);
    }

    public static void w(Object tag, String format, Object... args) {
        w(tag, String.format(format, args));
    }

    public static void i(Object tag, Object msg) {
        if (DEBUGGING) {
            String t = getTag(tag);
            String m = getMessage(msg);
            android.util.Log.i(t, m);
        }
    }

    public static void i(Object tag, String format, Object... args) {
        if (DEBUGGING) {
            i(tag, String.format(format, args));
        }
    }

    public static void d(Object tag, Object msg) {
        if (DEBUGGING) {
            String t = getTag(tag);
            String m = getMessage(msg);
            android.util.Log.d(t, m);
        }
    }

    public static void d(Object tag, String format, Object... args) {
        if (DEBUGGING) {
            d(tag, String.format(format, args));
        }
    }

    public static void v(Object tag, Object msg) {
        if (DEBUGGING) {
            String t = getTag(tag);
            String m = getMessage(msg);
            android.util.Log.v(t, m);
        }
    }

    public static void v(Object tag, String format, Object... args) {
        if (DEBUGGING) {
            v(tag, String.format(format, args));
        }
    }

    private static String getTag(Object t) {
        if (t == null) {
            return TAG;
        }

        String tag = TAG;
        if (t instanceof String) {
            tag = (String) t;
        } else {
            tag = t.getClass().getSimpleName();
        }

        return tag;
    }

    private static String getMessage(Object m) {
        if (m == null) {
            return "null";
        }

        String msg = "";
        if (m instanceof String) {
            msg = (String) m;
        } else if (m instanceof Throwable) {
            msg = android.util.Log.getStackTraceString((Throwable) m);
        } else {
            msg = m.toString();
        }

        return msg;
    }

}
