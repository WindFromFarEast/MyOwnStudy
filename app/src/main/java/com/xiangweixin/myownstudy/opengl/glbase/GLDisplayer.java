package com.xiangweixin.myownstudy.opengl.glbase;

import android.opengl.GLES20;
import android.opengl.Matrix;

import androidx.annotation.NonNull;

import com.xiangweixin.myownstudy.opengl.BufferUtil;
import com.xiangweixin.myownstudy.opengl.Frame;
import com.xiangweixin.myownstudy.opengl.GLProgram;
import com.xiangweixin.myownstudy.util.LogUtil;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

/**
 * 上屏
 */
public class GLDisplayer {

    private static final String TAG = GLDisplayer.class.getSimpleName();

    private static final String vertexShaderCode = "precision mediump float;\n" +
            "attribute vec4 a_position;\n" +
            "attribute vec2 a_texturePosition;\n" +
            "varying vec2 v_texturePosition;\n" +
            "uniform mat4 mvpMatrix;\n" +
            "void main() {\n" +
            "    v_texturePosition = a_texturePosition;\n" +
            "    gl_Position = mvpMatrix * a_position;\n" +
            "}";
    private static final String fragmentShaderCode = "precision mediump float;\n" +
            "varying vec2 v_texturePosition;\n" +
            "uniform sampler2D u_texture;\n" +
            "void main() {\n" +
            "    gl_FragColor = texture2D(u_texture, v_texturePosition);\n" +
            "}";

    private GLProgram program = new GLProgram();
    private int positionLoc = -1;
    private int texPositionLoc = -1;
    private int mvpLoc = -1;
    private int textureLoc = -1;

    private int surfaceWidth = 0;
    private int surfaceHeight = 0;

    private float scale = 1.0f;
    private GLDrawState drawState = new GLDrawState();

    private int scaleType = GLScaleType.CENTER_INSIDE;

    public void setSurfaceSize(int width, int height) {
        this.surfaceWidth = width;
        this.surfaceHeight = height;
    }

    public void setDrawState(@NonNull GLDrawState drawState) {
        this.drawState = drawState;
    }

    public void setScaleType(int scaleType) {
        this.scaleType = scaleType;
    }

    public int init() {
        int ret = program.init(vertexShaderCode, fragmentShaderCode);
        if (ret != 0) {
            LogUtil.e(TAG, "program init failed.");
            return ret;
        }
        program.use();
        positionLoc = GLES20.glGetAttribLocation(program.getProgramID(), "a_position");
        texPositionLoc = GLES20.glGetAttribLocation(program.getProgramID(), "a_texturePosition");
        mvpLoc = GLES20.glGetUniformLocation(program.getProgramID(), "mvpMatrix");
        textureLoc = GLES20.glGetUniformLocation(program.getProgramID(), "u_texture");
        program.unuse();
        return ret;
    }

    public void draw(Frame frame) {
        GLSize inputSize = new GLSize(frame.getWidth(), frame.getHeight());
        GLSize outputSize = new GLSize(surfaceWidth, surfaceHeight);

        float inputRatio = inputSize.width * 1f / inputSize.height;
        float inputWidth = inputRatio;
        float inputHeight = 1f;
        Rect vertexRect = new Rect();//输入纹理的顶点坐标(注意不是纹理坐标,可以超出[-1,1],最后会统一做缩放成[-1,1]的范围)
        vertexRect.left = -inputWidth / 2;
        vertexRect.right = inputWidth / 2;
        vertexRect.bottom = -inputHeight / 2;
        vertexRect.top = inputHeight / 2;

        float outputRatio = outputSize.width * 1f / outputSize.height;
        float outputWidth = outputRatio;
        float outputHeight = 1f;

        float realScale = 1.0f;
        if (scaleType == GLScaleType.CENTER_INSIDE) {
            realScale = Math.min(outputWidth / inputWidth, outputHeight / inputHeight);
        } else if (scaleType == GLScaleType.CENTER_CROP) {
            realScale = Math.max(outputWidth / inputWidth, outputHeight / inputHeight);
        }
        realScale *= scale;

        //MVP矩阵计算
        float[] mvpM = new float[16];
        Matrix.setIdentityM(mvpM, 0);

        float[] orthoM = new float[16];
        Matrix.setIdentityM(orthoM, 0);
        /**
         * float[] m：目标数组，数组至少有16个元素
         * int mOffset：结果矩阵起始的偏移值
         * float left：x轴的最小范围
         * float right: x轴的最大范围
         * float bottom：y轴的最小范围
         * float top: y轴的最大范围
         * float near：z轴的最小范围
         * float far: z轴的最大范围
         */
        Matrix.orthoM(orthoM, 0, -outputWidth / 2, outputWidth / 2, -outputHeight / 2, outputHeight / 2, -1, 1);

        float[] scaleM = new float[16];
        Matrix.setIdentityM(scaleM, 0);
        Matrix.scaleM(scaleM, 0, realScale, realScale, 0f);

        Matrix.multiplyMM(mvpM, 0, scaleM, 0, orthoM, 0);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLES20.glViewport(0, 0, surfaceWidth, surfaceHeight);
        GLES20.glClearColor(0, 0, 0, 0);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, frame.getTexture());
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        program.use();
        GLES20.glEnableVertexAttribArray(positionLoc);
        GLES20.glEnableVertexAttribArray(texPositionLoc);
        float[] vertexData = {
                vertexRect.left, vertexRect.top,
                vertexRect.right, vertexRect.top,
                vertexRect.left, vertexRect.bottom,
                vertexRect.right, vertexRect.bottom
        };
        FloatBuffer vertexBuffer = BufferUtil.getFloatBuffer(vertexData);
        GLES20.glVertexAttribPointer(positionLoc, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);
        float[] textureData = {
                0f, drawState.flipX ? 0f : 1f,
                1f, drawState.flipX ? 0f : 1f,
                0f, drawState.flipX ? 1f : 0f,
                1f, drawState.flipX ? 1f : 0f,
        };
        FloatBuffer textureBuffer = BufferUtil.getFloatBuffer(textureData);
        GLES20.glVertexAttribPointer(texPositionLoc, 2, GLES20.GL_FLOAT, false, 0, textureBuffer);

        GLES20.glUniformMatrix4fv(mvpLoc, 1, false, mvpM, 0);
        GLES20.glUniform1i(textureLoc, 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glFinish();

        GLES20.glDisableVertexAttribArray(positionLoc);
        GLES20.glDisableVertexAttribArray(texPositionLoc);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

    }

}
