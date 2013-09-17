package com.vokal.afergulator.widget;

import android.content.Context;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import java.util.HashMap;
import java.util.Map;

import com.vokal.afergulator.Engine;

import static com.vokal.afergulator.widget.ButtonNES.Key.DOWN;
import static com.vokal.afergulator.widget.ButtonNES.Key.LEFT;
import static com.vokal.afergulator.widget.ButtonNES.Key.RIGHT;
import static com.vokal.afergulator.widget.ButtonNES.Key.UP;

/**
 * Created by Nick on 9/13/13.
 */
public class DPadNES extends Button {

    public static final String TAG = "DPadNES";

    private static final Map<ButtonNES.Key, Boolean> states = new HashMap<ButtonNES.Key, Boolean>(4);

    private float mMaxPressure;
    private float mMinPressure;

    private PointF mCenter;

    public DPadNES(Context context) {
        super(context);
        init();
    }

    public DPadNES(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DPadNES(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        setOnTouchListener(touchListener);
        resetStates();
    }

    private OnTouchListener touchListener = new OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mCenter = new PointF(v.getWidth() / 2f, v.getHeight() / 2f);
                    handleTouch(v, event);
                    getBackground().setLevel(1);
                    return true;

                case MotionEvent.ACTION_MOVE:
                    handleTouch(v, event);
                    return true;

                case MotionEvent.ACTION_UP:
                    resetStates();
                    getBackground().setLevel(0);
                    return true;
            }
            return true;
        }
    };

    void handleTouch(View v, MotionEvent ev) {

        PointF pt = new PointF(ev.getX(), ev.getY());

        if (pt.y < 0 || pt.x > v.getRight())
            return;

        double distCenter = lineLen(pt, mCenter);

        float proj = (pt.x - mCenter.x) * mCenter.x;
        double rad = Math.acos(proj / (distCenter * mCenter.x));

        if (rad < Math.PI / 8.0) {
            setPressed(true, RIGHT);
            setPressed(false, UP, DOWN, LEFT);
        } else if (rad > Math.PI * 7.0 / 8.0) {
            setPressed(true, LEFT);
            setPressed(false, UP, DOWN, RIGHT);
        } else if (rad > Math.PI * 5.0 / 8.0) {
            if (pt.y < mCenter.y) {
                setPressed(true, UP, LEFT);
                setPressed(false, DOWN, RIGHT);
            } else {
                setPressed(true, DOWN, LEFT);
                setPressed(false, UP, RIGHT);
            }
        } else if (rad < Math.PI * 3.0 / 8.0) {
            if (pt.y < mCenter.y) {
                setPressed(true, UP, RIGHT);
                setPressed(false, DOWN, LEFT);
            } else {
                setPressed(true, DOWN, RIGHT);
                setPressed(false, UP, LEFT);
            }
        } else {
            if (pt.y < mCenter.y) {
                setPressed(true, UP);
                setPressed(false, DOWN, LEFT, RIGHT);
            } else {
                setPressed(true, DOWN);
                setPressed(false, UP, LEFT, RIGHT);
            }
        }

//        StringBuilder bldr = new StringBuilder();
//        if (states[0]) bldr.append("UP ");
//        if (states[1]) bldr.append("DOWN ");
//        if (states[2]) bldr.append("LEFT");
//        if (states[3]) bldr.append("RIGHT");
//        Log.v(TAG, bldr.toString());

//        checkPressure(ev.getPressure());
    }

    private void setPressed(boolean state, ButtonNES.Key... buttons) {
        for (ButtonNES.Key b : buttons) {
            if (state != states.get(b)) {
                if (state) {
                    Engine.keyEvent(b.ordinal(), 1, 0);
                } else {
                    Engine.keyEvent(b.ordinal(), 0, 0);
                }
                states.put(b, state);
            }
        }
    }

    private void resetStates() {
        states.put(UP, false);
        states.put(DOWN, false);
        states.put(LEFT, false);
        states.put(RIGHT, false);

        applyStates();
    }

    private void applyStates() {
        for (Map.Entry<ButtonNES.Key, Boolean> s : states.entrySet()) {
            if (s.getValue()) {
                Engine.keyEvent(s.getKey().ordinal(), 1, 0);
            } else {
                Engine.keyEvent(s.getKey().ordinal(), 0, 0);
            }
        }
    }

    private double lineLen(PointF aP1, PointF aP2) {
        return Math.sqrt(Math.pow((aP1.x - aP2.x), 2) + Math.pow(aP1.y - aP2.y, 2));
    }

    void checkPressure(float pressure) {
        mMaxPressure = Math.max(mMaxPressure, pressure);
        mMinPressure = Math.min(mMinPressure, pressure);
        float pct = pressure / (mMaxPressure - mMinPressure) * 100;
//        Log.v(TAG, String.format("pressure %% %.2f", pct));
    }

    // TODO: has physical keyboard
    private OnTouchListener dPadListener = new OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
//                case MotionEvent.ACTION:  // ???
            }

            return false;
        }
    };

}
