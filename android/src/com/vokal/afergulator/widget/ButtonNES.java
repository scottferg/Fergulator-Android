package com.vokal.afergulator.widget;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import com.vokal.afergulator.Engine;
import com.vokal.afergulator.R;

/**
 * Created by Nick on 9/13/13.
 */
public class ButtonNES extends Button {

    private static final String TAG = ButtonNES.class.getSimpleName();

    public static enum Key {
        A, B, SELECT, START, UP, DOWN, LEFT, RIGHT, RESET
    }

    private Key         mButton;
    private ButtonGroup mGroup;

    public ButtonNES(Context context, Key button) {
        super(context);
        mButton = button;
        init();
    }

    public ButtonNES(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ButtonNES(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.ButtonNES, 0, 0);
        try {
            mButton = Key.values()[a.getInt(R.styleable.ButtonNES_button, -1)];
        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            a.recycle();
        }

        init();
    }

    private void init() {
        if (mButton != null) {
            int o = mButton.ordinal();
            if (o >= 0 && o < Key.values().length) {
                setOnTouchListener(touchListener);
                setAlpha(0.5f);
            }
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (getParent() instanceof ButtonGroup) {
            mGroup = (ButtonGroup) getParent();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mGroup = null;
    }

    private OnTouchListener touchListener = new OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                setUp();
            } else if (event.getAction() == MotionEvent.ACTION_DOWN) {
                setDown();
            }
            if (mGroup != null) {
                mGroup.onTouch(v, event);
            }
            return true;
        }
    };

    private void setUp() {
        Engine.keyEvent(mButton.ordinal(), 0, 0);
        setAlpha(0.5f);
        invalidate();
    }

    private void setDown() {
        Engine.keyEvent(mButton.ordinal(), 1, 0);
        setAlpha(1.0f);
        invalidate();
        getParent().requestDisallowInterceptTouchEvent(false);
    }

    public void press() {
        Engine.keyEvent(mButton.ordinal(), 1, 0);
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Engine.keyEvent(mButton.ordinal(), 0, 0);
    }

}
