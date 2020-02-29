package com.xiangweixin.myownstudy.mediaallproj;

import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class XWXRecorder implements SurfaceHolder.Callback {

    private Surface mSurface;
    private SurfaceView mSurfaceView;
    private int mSurfaceWidth;
    private int mSurfaceHeight;

    private long mHandler;

    public XWXRecorder(SurfaceView surfaceView) {
        surfaceView.getHolder().addCallback(this);
        mSurfaceView = surfaceView;
        mHandler = nativeCreate();
    }

    public void startPreview() {

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mSurface = holder.getSurface();
        mSurfaceWidth = mSurfaceView.getWidth();
        mSurfaceHeight = mSurfaceView.getHeight();

        //在底层创建GL线程用于显示Camera数据
        nativeInitOpenGL(mHandler, mSurface);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    private native long nativeCreate();
    private native int nativeInitOpenGL(long handler, Surface surface);
}
