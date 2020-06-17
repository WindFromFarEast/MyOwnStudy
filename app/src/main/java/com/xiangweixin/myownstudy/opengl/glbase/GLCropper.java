package com.xiangweixin.myownstudy.opengl.glbase;

import android.opengl.GLES20;

import com.xiangweixin.myownstudy.opengl.BufferUtil;
import com.xiangweixin.myownstudy.opengl.Frame;
import com.xiangweixin.myownstudy.opengl.GLProgram;
import com.xiangweixin.myownstudy.util.LogUtil;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;

/**
 * 用于裁剪
 */
public class GLCropper {

    private static final String TAG = GLCropper.class.getSimpleName();

    private static final String VERTEX_CODE = "" +
            "        attribute highp vec2 vPosition;\n" +
            "        attribute highp vec2 texCoordAttr;\n" +
            "        varying vec2 texCoord;\n" +
            "\n" +
            "        void main() {\n" +
            "            gl_Position = vec4(vPosition, 0.0, 1.0);\n" +
            "            texCoord = texCoordAttr;\n" +
            "        }";
    private static final String FRAG_CODE = "" +
            "        varying\n" +
            "        vec2 texCoord;\n" +
            "        uniform\n" +
            "        sampler2D inputTexture;\n" +
            "\n" +
            "        void main() {\n" +
            "            gl_FragColor = texture2D(inputTexture, texCoord);\n" +
            "        }";

    private int fbo = -1;
    private GLProgram program = new GLProgram();

    private int positionLoc = -1;
    private int texPositionLoc = -1;

    private int cropWidth = 0;
    private int cropHeight = 0;

    private boolean debug = true;

    private static float[] vertexData = {
        -1.0f, -1.0f, //(0.0, 0.0)
        1.0f, -1.0f,  //(1.0, 0.0)
        -1.0f, 1.0f,  //(0.0, 1.0)
        1.0f, 1.0f,   //(1.0, 1.0)
    };
    private static float[] texData = {
            0.0f, 0.0f,
            1.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f
    };

    public GLCropper() {

    }

    public int init() {
        int ret = program.init(VERTEX_CODE, FRAG_CODE);
        if (ret != 0) {
            LogUtil.e(TAG, "program init failed.");
            return ret;
        }
        program.use();
        positionLoc = GLES20.glGetAttribLocation(program.getProgramID(), "vPosition");
        texPositionLoc = GLES20.glGetAttribLocation(program.getProgramID(), "texCoordAttr");
        program.unuse();

        if (fbo == -1) {
            int[] fbos = new int[1];
            GLES20.glGenFramebuffers(1, fbos, 0);
            fbo = fbos[0];
        }
        return 0;
    }

    /**
     *
     * @param cropPosition 裁剪纹理坐标[左下、右下、左上、右上]
     * @param width 纹理原始宽
     * @param height 纹理原始高
     * @return [width, height], 裁剪后的宽高
     */
    public int[] setCropPosition(float[] cropPosition, int width, int height) {
        if (cropPosition.length == 8) {
            texData = cropPosition;
        }
        int[] cropSize = new int[2];
        cropWidth = (int) (Math.abs(cropPosition[2] - cropPosition[0]) * width);
        cropHeight = (int) (Math.abs(cropPosition[5] - cropPosition[1]) * height);
        cropSize[0] = cropWidth;
        cropSize[1] = cropHeight;
        return cropSize;
    }

    public Frame crop(int inTexture, int outTexture) {
        Frame frame = new Frame();
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, outTexture, 0);

        GLES20.glViewport(0, 0, cropWidth, cropHeight);

        program.use();

        GLES20.glClearColor(0, 0, 0, 0);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, inTexture);
        GLES20.glEnableVertexAttribArray(positionLoc);
        GLES20.glEnableVertexAttribArray(texPositionLoc);
        Buffer buffer = BufferUtil.getFloatBuffer(vertexData);
        Buffer buffer2 = BufferUtil.getFloatBuffer(texData);
        GLES20.glVertexAttribPointer(positionLoc, 2, GLES20.GL_FLOAT, false, 0, buffer);
        GLES20.glVertexAttribPointer(texPositionLoc, 2, GLES20.GL_FLOAT, false, 0, buffer2);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        if (debug) {
            ByteBuffer buffer3 = BufferUtil.createByteBuffer(cropWidth * cropHeight * 4);
            GLES20.glReadPixels(0, 0, cropWidth, cropHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer3);
            try {
                FileOutputStream outputStream = new FileOutputStream("sdcard/1.rgba");
                outputStream.write(buffer3.array());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            debug = false;
        }

        program.unuse();
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        GLES20.glFinish();

        return frame;
    }

}
