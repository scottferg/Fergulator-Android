package com.ferg.afergulator.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;

import com.ferg.afergulator.Engine;
import com.ferg.afergulator.R;

/**
 * Created by Nick on 9/13/13.
 */
public class ButtonNES extends Button implements View.OnTouchListener {

    private static final String TAG = ButtonNES.class.getSimpleName();

    public static enum Key {
        A, B, SELECT, START, UP, DOWN, LEFT, RIGHT
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
                setOnTouchListener(this);
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

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = getDefaultSize(0, widthMeasureSpec);
        int h = resolveSize(0, heightMeasureSpec);
        if (h == 0) h = w;
        setMeasuredDimension(w, h);
    }

    public static Key keyFromKeyCode(int keyCode) {
        android.util.Log.i(TAG, "Code: " + Integer.toString(keyCode));
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
                return Key.LEFT;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                return Key.RIGHT;
            case KeyEvent.KEYCODE_DPAD_UP:
                return Key.UP;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                return Key.DOWN;
            case 108:
            case 105:
                return Key.START;
            case 104:
            case 4:
                return Key.SELECT;
            case 96:
                return Key.B;
            case 97:
                return Key.A;
            default:
                return null;
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                setDown();
                break;
            case MotionEvent.ACTION_UP:
                setUp();
                break;
        }

        if (mGroup != null && v != mGroup) {
            mGroup.onTouch(v, event);
        }

        return true;
    }

    public void setUp() {
        Engine.buttonUp(mButton);
        setAlpha(0.5f);
        invalidate();
    }

    public void setDown() {
        Engine.buttonDown(mButton);
        setAlpha(1.0f);
        invalidate();
    }

    @Override
    public String toString() {
        return mButton.name();
    }
}
