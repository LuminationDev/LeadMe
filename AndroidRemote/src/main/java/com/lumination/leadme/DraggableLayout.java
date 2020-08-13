package com.lumination.leadme;

import android.content.Context;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.LinearLayout;

public class DraggableLayout extends LinearLayout {
    private int padding = 85;

    private void init() {
        setFocusableInTouchMode(true);
        setClickable(true);
        requestDisallowInterceptTouchEvent(true);
    }

    public DraggableLayout(Context context) {
        super(context);
        init();
    }

    public DraggableLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DraggableLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public DraggableLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return true;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setX(padding);
        setY(padding);
    }
}
