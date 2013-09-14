package com.vokal.afergulator.widget;

import android.content.Context;
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

    public static enum Button {
        A, B, SELECT, START, UP, DOWN, LEFT, RIGHT
    }

    private       Button  mButton;

    public ButtonNES(Context context, Button button) {
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
            mButton = Button.values()[a.getInt(R.styleable.ButtonNES_button, -1)];
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
            if (o >= 0 && o < Button.values().length) {
                setOnTouchListener(touchListener);
                setAlpha(0.5f);
            }
        }
    }

    private OnTouchListener touchListener = new OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                Engine.keyEvent(mButton.ordinal(), 0, 0);
                setAlpha(0.5f);
            } else if (event.getAction() == MotionEvent.ACTION_DOWN) {
                Engine.keyEvent(mButton.ordinal(), 1, 0);
                setAlpha(1.0f);
            }
            return true;
        }
    };

    public static void pressStart() {
        pressButton(Button.START);
    }

    public static void pressSelect() {
        pressButton(Button.SELECT);
    }

    public static void pressButton(Button button) {
        Engine.keyEvent(button.ordinal(), 1, 0);
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Engine.keyEvent(button.ordinal(), 0, 0);
    }
}
