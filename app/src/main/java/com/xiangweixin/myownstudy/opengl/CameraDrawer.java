package com.xiangweixin.myownstudy.opengl;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import com.xiangweixin.myownstudy.mediaallproj.XWXResult;
import com.xiangweixin.myownstudy.util.LogUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

public class CameraDrawer {

    private static final String TAG = CameraDrawer.class.getSimpleName();

    private static final String vertexShaderCode = "" +
            "attribute vec4 av_Position;\n" +
            "attribute vec4 inputTextureCoordinate;\n" +
            "uniform mat4 uTextureMatrix;\n" +
            "varying vec2 textureCoordinate;\n" +
            "void main() {\n" +
            "textureCoordinate = (uTextureMatrix * inputTextureCoordinate).xy;\n" +
            "gl_Position = av_Position;\n" +
            "}";
    private static final String fragmentShaderCode = "" +
            "#extension GL_OES_EGL_image_external : require\n" +
            "precision mediump float;\n" +
            "varying vec2 textureCoordinate;\n" +
            "uniform samplerExternalOES s_texture;\n" +
            "void main() {\n" +
            "gl_FragColor = texture2D(s_texture, textureCoordinate);\n" +
            "}";

    private static final float[] vertexData = {
            -1f, -1f,
            1f, -1f,
            -1, 1f,
            1f, 1,
    };
    private static final float[] textureData = {
            0f, 1f,
            1f, 1f,
            0f, 0f,
            1f, 0f,
    };

    private static final int COORDS_PER_TEX = 2;
    private static final int VERTEX_COUNT = vertexData.length / COORDS_PER_TEX;
    private static final int VERTEX_STRIDE = COORDS_PER_TEX * 4;

    private int mTexture = -1;
    private FloatBuffer vertexBuffer;
    private FloatBuffer textureBuffer;

    private int vertexShaderObj;
    private int fragmentShaderObj;
    private int programObj;
    private int positionHandle;
    private int texturePositionHandle;
    private int textureMatrixHandle;

    public CameraDrawer() {
        vertexBuffer = ByteBuffer.allocateDirect(vertexData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(vertexData);
        vertexBuffer.position(0);
        textureBuffer = ByteBuffer.allocateDirect(textureData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(textureData);
        textureBuffer.position(0);
    }

    public int init() {
        vertexShaderObj = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        if (vertexShaderObj == 0) {
            LogUtil.e(TAG, "load shader failed.");
            return XWXResult.CREATE_SHADER_FAILED;
        }
        fragmentShaderObj = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);
        if (fragmentShaderObj == 0) {
            LogUtil.e(TAG, "load shader failed.");
            return XWXResult.CREATE_SHADER_FAILED;
        }

        programObj = GLES20.glCreateProgram();
        if (programObj == 0) {
            LogUtil.e(TAG, "create program failed.");
            return XWXResult.CREATE_PROGRAM_FAILED;
        }
        GLES20.glAttachShader(programObj, vertexShaderObj);
        GLES20.glAttachShader(programObj, fragmentShaderObj);
        GLES20.glLinkProgram(programObj);
        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(programObj, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] != GLES20.GL_TRUE) {
            LogUtil.e(TAG, "Link program failed.");
            GLES20.glDeleteProgram(programObj);
            programObj = 0;
            return XWXResult.LINK_PROGRAM_FAILED;
        }

        int[] texIds = new int[1];
        GLES20.glGenTextures(1, texIds, 0);
        mTexture = texIds[0];
        LogUtil.i(TAG, "Create texture.. Texture Id: " + mTexture);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTexture);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MIN_FILTER,GL10.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);

        return 0;
    }

    public void draw(float[] transformMatrix) {
        GLES20.glUseProgram(programObj);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTexture);

        positionHandle = GLES20.glGetAttribLocation(programObj, "av_Position");
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, COORDS_PER_TEX, GLES20.GL_FLOAT, false, COORDS_PER_TEX * 4, vertexBuffer);

        texturePositionHandle = GLES20.glGetAttribLocation(programObj, "inputTextureCoordinate");
        GLES20.glEnableVertexAttribArray(texturePositionHandle);
        GLES20.glVertexAttribPointer(texturePositionHandle, COORDS_PER_TEX, GLES20.GL_FLOAT, false, COORDS_PER_TEX * 4, textureBuffer);

        textureMatrixHandle = GLES20.glGetUniformLocation(programObj, "uTextureMatrix");
        GLES20.glUniformMatrix4fv(textureMatrixHandle, 1, false, transformMatrix, 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, VERTEX_COUNT);

        GLES20.glDisableVertexAttribArray(positionHandle);
        GLES20.glDisableVertexAttribArray(texturePositionHandle);
    }

    public void release() {
        int[] deleteTextures = { mTexture };
        GLES20.glDeleteTextures(1, deleteTextures, 0);
        GLES20.glDeleteShader(vertexShaderObj);
        GLES20.glDeleteShader(fragmentShaderObj);
        GLES20.glDeleteProgram(programObj);
    }

    public int getTexture() {
        return mTexture;
    }

    private int loadShader(int type, String code) {
        int shader = GLES20.glCreateShader(type);//error if return 0.
        if (shader != 0) {
            GLES20.glShaderSource(shader, code);
            GLES20.glCompileShader(shader);
            String info = GLES20.glGetShaderInfoLog(shader);
            LogUtil.e(TAG, "shader info: " + info);
            int[] compileStatus = new int[1];
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0);
            if (compileStatus[0] != GLES20.GL_TRUE) {
                LogUtil.e(TAG, "compile shader failed.");
                GLES20.glDeleteShader(shader);
                shader = 0;
            }
        }
        return shader;
    }

}
