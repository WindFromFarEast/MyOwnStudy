package com.xiangweixin.myownstudy.mediaallproj;

import android.graphics.SurfaceTexture;
import android.os.Build;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.RequiresApi;

import com.xiangweixin.myownstudy.util.LogUtil;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class XWXRecorder implements SurfaceHolder.Callback {

    private static final String TAG = XWXResult.class.getSimpleName();

    private Surface mSurface;
    private SurfaceView mSurfaceView;
    private int mSurfaceWidth;
    private int mSurfaceHeight;

    private XWXCamera mCamera;
    private XWXCameraSetting mCamSetting;
    private TextureHolder mTextureHolder = new TextureHolder();

    private long mHandler;

    public XWXRecorder(SurfaceView surfaceView, XWXCameraSetting setting) {
        surfaceView.getHolder().addCallback(this);
        mSurfaceView = surfaceView;
        mHandler = nativeCreate();
        mCamera = new XWXCamera();
        mCamSetting = setting;
    }

    public void startPreview() {
        mCamera.open(true);
        mCamera.config(mCamSetting);
        mCamera.startPreview(mTextureHolder.getSurfaceTexture());
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mSurface = holder.getSurface();
        mSurfaceWidth = mSurfaceView.getWidth();
        mSurfaceHeight = mSurfaceView.getHeight();

        //在底层创建GL线程用于显示Camera数据
        int ret = nativeInitOpenGL(mHandler, mSurface);
        if (ret != XWXResult.OK) {
            LogUtil.e(TAG, "init opengl failed. ret = " + ret);
            return;
        }

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    //---------------------------------------------Use for native or native callback------------------------------------------------------------//

    private Callback.OnOpenGLCallback mOpenGLCallback = new Callback.OnOpenGLCallback() {
        @Override
        public int onGLCreated() {
            mTextureHolder.onCreate();
            return mTextureHolder.getSurfaceTextureId();
        }

        @Override
        public void onGLRunning() {
            mTextureHolder.updateTexImage();
        }

        @Override
        public void onGLDestroyed() {

        }
    };

    public int onGLCreated() {
        return mOpenGLCallback.onGLCreated();
    }

    public void onGLRunning() {
        mOpenGLCallback.onGLRunning();
    }

    public void onGLDestroyed() {
        mOpenGLCallback.onGLDestroyed();
    }

    private native long nativeCreate();
    private native int nativeInitOpenGL(long handler, Surface surface);
}
