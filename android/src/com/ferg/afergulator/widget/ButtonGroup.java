package com.ferg.afergulator.widget;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.*;
import android.widget.LinearLayout;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Nick on 9/14/13.
 */
public class ButtonGroup extends LinearLayout implements View.OnTouchListener {

    private static final String TAG = ButtonGroup.class.getSimpleName();

    HashMap<ButtonNES, Rect>  bounds;
    HashMap<ButtonNES, Boolean> states;

    private int slop;

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

        slop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        bounds = new HashMap<ButtonNES, Rect>(2);
        states = new HashMap<ButtonNES, Boolean>(2);

        postDelayed(updateBounds, 50);
    }

    private Runnable updateBounds = new Runnable() {
        @Override
        public void run() {
            int numChildren = getChildCount();
            for (int i = 0; i < numChildren; i++) {
                if (getChildAt(i) instanceof ButtonNES) {
                    ButtonNES btn = (ButtonNES) getChildAt(i);
                    Rect b = new Rect(btn.getLeft(), btn.getTop(), btn.getRight(), btn.getBottom());
                    b.inset(slop, slop);
                    bounds.put(btn, b);
                    states.put(btn, false);
                }
            }
        }
    };

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        setOnTouchListener(null);
        bounds = null;
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) allUp();
        if (event.getAction() != MotionEvent.ACTION_MOVE) return true;
        if (!(view instanceof ButtonNES)) return true;

        ButtonNES button = (ButtonNES) view;
        Rect b = bounds.get(button);

        int x = (int) event.getX() + b.left;
        int y = (int) event.getY() + b.top;

        for (Map.Entry<ButtonNES, Rect> btn : bounds.entrySet()) {
            if (view != btn.getKey()) {
                if (btn.getValue().contains(x, y)) {
                    if (!states.get(btn.getKey())) {
                        btn.getKey().setDown();
                        states.put(btn.getKey(), true);
                    }
                } else if (states.get(btn.getKey())) {
                    btn.getKey().setUp();
                    states.put(btn.getKey(), false);
                }
            }
        }

        return true;
    }

    private void allUp() {
        for (Map.Entry<ButtonNES, Boolean> btn : states.entrySet()) {
            if (btn.getValue()) {
                btn.getKey().setUp();
            }
        }
    }

}
