package com.xiangweixin.myownstudy.opengl;

import android.opengl.GLES20;

import com.xiangweixin.myownstudy.util.LogUtil;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 纹理池
 */
public class TextureManager {

    private static final String TAG = TextureManager.class.getSimpleName();

    private static TextureManager mInstance;
    private static Lock mLock = new ReentrantLock();

    private TextureManager() {

    }

    public static TextureManager getManager() {
        mLock.lock();
        try {
            if (mInstance == null) {
                mInstance = new TextureManager();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            mLock.unlock();
        }
        return mInstance;
    }

    public int createTexture(int width, int height, Buffer pixels) {
        LogUtil.formatI(TAG, "Create texture >>> width: %d, height: %d", width, height);
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, pixels);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        return textures[0];
    }

}
