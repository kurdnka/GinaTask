/*
 * Copyright (c) 2015, GINA Software s.r.o. All rights reserved.
 * http://www.ginasystem.com/
 *
 * ginatask
 */
package com.tomi.ginatask;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.TouchDelegate;

import com.nutiteq.core.MapPos;
import com.nutiteq.core.ScreenPos;
import com.nutiteq.ui.MapView;

/**
 * <p>
 * <p/>
 * </p>
 *
 * @author Roman Beránek {@iteral <beranek@ginasystem.com>}
 * @version 08/08/15
 */
public class EditableMapView extends MapView {

    private static final String LOG_TAG = "EditableMapView";
    private int startMappingCounter;
    private Object mapListener;
    private boolean mJustVisiting = false;

    public void setJustVisiting(boolean justVisiting) {
        mJustVisiting = justVisiting;
    }

    public EditableMapView(Context context) {
        super(context);
    }

    public EditableMapView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mJustVisiting) return super.onTouchEvent(event);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (startDrag(event.getX(), event.getY()))
                return true;
                break;
            case MotionEvent.ACTION_MOVE:
                if (dragTo(event.getX(), event.getY()))
                    return true;
                break;
            case MotionEvent.ACTION_CANCEL:
                if (endDrag(event.getX(), event.getY(), false))
                    return true;
                break;
            case MotionEvent.ACTION_UP:
                if (endDrag(event.getX(), event.getY(), true))
                    return true;
                break;
        }
        return super.onTouchEvent(event);
    }

    private boolean dragTo(float x, float y) {
        MapPos pos = this.screenToMap(new ScreenPos(x, y));
        Log.v(LOG_TAG, "Drawing to:" + pos);
        return true;
    }

    private boolean endDrag(float x, float y, boolean b) {
        Log.v(LOG_TAG, "Drawing ended");
        return false;
    }

    private boolean startDrag(float x, float y) {
        MapPos pos = this.screenToMap(new ScreenPos(x, y));
        Log.v(LOG_TAG, "Drawing started:" + pos);
        return false;
    }
}
