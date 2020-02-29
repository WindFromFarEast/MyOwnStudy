package com.xiangweixin.myownstudy.mediaallproj;

import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class XWXSurfaceView extends SurfaceView implements SurfaceHolder.Callback {

    public XWXSurfaceView(Context context) {
        this(context, null);
    }

    public XWXSurfaceView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public XWXSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

}
