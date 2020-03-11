package com.xiangweixin.myownstudy.mediaallproj;

import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import com.xiangweixin.myownstudy.util.LogUtil;

public class TextureHolder {

    private int mSurfaceTextureId;
    private SurfaceTexture mSurfaceTexture;

    public void onCreate() {
        int[] texID = new int[1];
        GLES20.glGenTextures(1, texID, 0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texID[0]);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        mSurfaceTextureId = texID[0];
        mSurfaceTexture = new SurfaceTexture(mSurfaceTextureId);
        mSurfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
            @Override
            public void onFrameAvailable(SurfaceTexture surfaceTexture) {

            }
        });
    }

    public void updateTexImage() {
        if (mSurfaceTexture == null) {
            LogUtil.d("TextureHolder", "SurfaceTexture is null, ignore updateTexImage");
            return;
        }
        mSurfaceTexture.updateTexImage();
    }

    public SurfaceTexture getSurfaceTexture() {
        return mSurfaceTexture;
    }

    public int getSurfaceTextureId() {
        return mSurfaceTextureId;
    }

}
