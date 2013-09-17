package com.vokal.afergulator.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.*;
import android.widget.RelativeLayout;

import com.vokal.afergulator.Engine;

/**
 * Created by Nick on 9/14/13.
 */
public class ButtonGroup extends RelativeLayout implements View.OnTouchListener {

    private static final String TAG = ButtonGroup.class.getSimpleName();

    private GestureDetector detector;

    public ButtonGroup(Context context) {
        super(context);
    }

    public ButtonGroup(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ButtonGroup(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        setBackgroundColor(0);
        setOnTouchListener(this);

        detector = new GestureDetector(getContext(), gestureListener);

//        GestureOverlayView gov = new GestureOverlayView(getContext());
//        addView(gov);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
//        dump(event);
        detector.onTouchEvent(event);
        return true;
    }

    private void dump(MotionEvent ev) {
        Log.v("BTN_GRP", String.format("(%.0f, %.0f) sz=%.3f, ps=%.3f, tch=%.3f, tool%.3f",
                                       ev.getX(), ev.getY(),
                                       ev.getSize(), ev.getPressure(),
                                       ev.getTouchMajor(),
                                       ev.getToolMajor()));
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        super.requestDisallowInterceptTouchEvent(disallowIntercept);
//        Log.v(TAG, "requestDisallowIntercept");
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
//        Log.v("BTN_GRP", "onIntercept");
        return super.onInterceptTouchEvent(ev);

    }

    private GestureDetector.OnGestureListener gestureListener = new GestureDetector.SimpleOnGestureListener() {
        private static final String TAG = "GESTURE";

        private boolean multiMode;

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (e2.getSize() > .9f && e2.getPressure() > 1f) {
                if (!multiMode) {
                    Log.i(TAG, "MULTI START");
                    Engine.buttonDown(ButtonNES.Key.A);
                    multiMode = true;
                }
            } else {
                if (multiMode) {
                    Log.v(TAG, "multi stop");
                    Engine.buttonUp(ButtonNES.Key.A);
                    multiMode = false;
                }
            }

            return false;
        }

    };
}
