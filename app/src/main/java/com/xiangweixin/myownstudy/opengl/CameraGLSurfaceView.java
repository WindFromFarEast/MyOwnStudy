package com.xiangweixin.myownstudy.opengl;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.xiangweixin.myownstudy.mediaallproj.XWXCameraSetting;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL;
import javax.microedition.khronos.opengles.GL10;

public class CameraGLSurfaceView extends GLSurfaceView implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {

    private SurfaceTexture surfaceTexture;
    int texId = -1;
    private CameraDrawer drawer;
    private CameraUtil cameraUtil;
    private float[] transformMatrix = new float[16];


    public CameraGLSurfaceView(Context context) {
        this(context, null);
    }

    public CameraGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setEGLContextClientVersion(2);
        cameraUtil = new CameraUtil();
        setRenderer(this);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        cameraUtil.open(true);
        XWXCameraSetting cameraSetting = new XWXCameraSetting.Builder()
                .setPreviewSize(1280, 720)
                .setFocusMode(XWXCameraSetting.FocusMode.AUTO)
                .build();
        cameraUtil.config(cameraSetting);

        drawer = new CameraDrawer();
        drawer.init();
        texId = drawer.getTexture();
        surfaceTexture = new SurfaceTexture(texId);
        surfaceTexture.setOnFrameAvailableListener(this);

        cameraUtil.startPreview(surfaceTexture);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        super.surfaceDestroyed(holder);
        cameraUtil.stopPreview();
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClearColor(1f, 1f, 1f, 1f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        surfaceTexture.updateTexImage();
        surfaceTexture.getTransformMatrix(transformMatrix);
        drawer.draw(transformMatrix);
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        this.requestRender();
    }

}
