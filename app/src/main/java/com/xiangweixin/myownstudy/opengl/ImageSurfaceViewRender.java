package com.xiangweixin.myownstudy.opengl;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import com.xiangweixin.myownstudy.opengl.glbase.GLCropper;
import com.xiangweixin.myownstudy.opengl.glbase.GLDisplayer;
import com.xiangweixin.myownstudy.opengl.glbase.GLDrawState;
import com.xiangweixin.myownstudy.util.LogUtil;

import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * 用来显示一张图片，练习图片的缩放、裁剪、旋转、平移等
 */
public class ImageSurfaceViewRender implements GLSurfaceView.Renderer {

    private static final String TAG = ImageSurfaceViewRender.class.getSimpleName();

    private int texID = -1;

    private int imageWidth = 0;
    private int imageHeight = 0;
    private int viewWitdh = 0;
    private int viewHeight = 0;

    private GLCropper cropper = new GLCropper();
    private GLDisplayer displayer = new GLDisplayer();

    private TextureManager mManager = TextureManager.getManager();

    private Frame imageFrame;

    private GLDrawState drawState = new GLDrawState();

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        imageFrame = BitmapUtils.decodeFile("sdcard/360p.jpg");
        imageWidth = imageFrame.getWidth();
        imageHeight = imageFrame.getHeight();
        texID = mManager.createTexture(imageWidth, imageHeight, imageFrame.getData());
        imageFrame.setTexture(texID);
        displayer.init();
        drawState.flipX = true;
        displayer.setDrawState(drawState);
        cropper.init();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        viewWitdh = width;
        viewHeight = height;
        displayer.setSurfaceSize(width, height);
        LogUtil.formatI(TAG, "glViewPort >>> x: %d, y: %d, width: %d, height: %d", 0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        displayer.draw(imageFrame);
    }

}
