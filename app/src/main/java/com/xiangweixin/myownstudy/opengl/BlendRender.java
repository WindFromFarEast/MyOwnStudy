package com.xiangweixin.myownstudy.opengl;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import com.xiangweixin.myownstudy.util.BitmapUtil;
import com.xiangweixin.myownstudy.util.LogUtil;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL;
import javax.microedition.khronos.opengles.GL10;

/**
 * 将两个图片纹理Blend后，使用FBO的输出纹理把两个纹理合成为一个纹理
 */
public class BlendRender implements GLSurfaceView.Renderer {

    private static final String vertexShaderCode = "" +
            "precision mediump float;\n" +
            "attribute vec4 a_Position;\n" +
            "attribute vec2 a_textureCoordinate;\n" +
            "varying vec2 v_textureCoordinate;\n" +
            "void main() {\n" +
            "v_textureCoordinate = a_textureCoordinate;\n" +
            "gl_Position = a_Position;\n" +
            "}";
    private static final String fragmentShaderCode = "" +
            "precision mediump float;\n" +
            "varying vec2 v_textureCoordinate;\n" +
            "uniform sampler2D u_texture;\n" +
            "void main(){\n" +
            "gl_FragColor = texture2D(u_texture, v_textureCoordinate);\n" +
            "}";

    private float[] vertexData1 = {//第一个图片的顶点坐标
            -1f, -1f,
            -1f, 1f,
            1f, 1f,
            -1f, -1f,
            1f, 1f,
            1f, -1,
    };
    private float[] vertexData2 = {//第二个图片的顶点坐标
            -0.5f, -0.3f,
            -0.5f, 0.3f,
            0.5f, 0.3f,
            -0.5f, -0.3f,
            0.5f, 0.3f,
            0.5f, -0.3f
    };
    private float[] textureData = {
            0f, 1f,
            0f, 0f,
            1f, 0f,
            0f, 1f,
            1f, 0f,
            1f, 1f,
    };

    private FloatBuffer vertex1Buffer;
    private FloatBuffer vertex2Buffer;
    private FloatBuffer textureBuffer;

    //GL Attribute.
    private int programId;
    private int texture1;
    private int texture2;
    private int fboTexture;
    private int fbo;

    private int mSurfaceWidth;
    private int mSurfaceHeight;

    private boolean readPixles = true;

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        initData();
        programId = createGLProgram(vertexShaderCode, fragmentShaderCode);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        mSurfaceWidth = width;
        mSurfaceHeight = height;
        initFBO(width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClearColor(0.9f, 0.9f, 0.9f, 1f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo);

        bindGLProgram(programId, texture1, vertex1Buffer, textureBuffer, GLES20.GL_TEXTURE0);
        render();

        bindGLProgram(programId, texture2, vertex2Buffer, textureBuffer, GLES20.GL_TEXTURE1);
        render();

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        bindGLProgram(programId, fboTexture, vertex1Buffer, textureBuffer, GLES20.GL_TEXTURE1);
        render();
        GLES20.glFinish();

        if (readPixles) {
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(mSurfaceWidth * mSurfaceHeight * 4)
                    .order(ByteOrder.nativeOrder());
            byteBuffer.position(0);
            GLES20.glReadPixels(0, 0, mSurfaceWidth, mSurfaceHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, byteBuffer);
            LogUtil.i("BlendRender", "surfaceWidth: " + mSurfaceWidth + ", surfaceHeight: " + mSurfaceHeight);
            readPixles = false;

            try {
                FileOutputStream fos = new FileOutputStream("sdcard/pixels.rgba");
                fos.write(byteBuffer.array());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void initData() {
        vertex1Buffer = ByteBuffer.allocateDirect(vertexData1.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(vertexData1);
        vertex1Buffer.position(0);
        vertex2Buffer = ByteBuffer.allocateDirect(vertexData2.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(vertexData2);
        vertex2Buffer.position(0);
        textureBuffer = ByteBuffer.allocateDirect(textureData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(textureData);
        textureBuffer.position(0);

        int[] textures = new int[2];
        GLES20.glGenTextures(2, textures, 0);
        texture1 = textures[0];
        texture2 = textures[1];

        loadImageToTexture("sdcard/sample.jpg", texture1, GLES20.GL_TEXTURE0);
        loadImageToTexture("sdcard/test1.jpg", texture2, GLES20.GL_TEXTURE0);

    }

    /**
     * 将图片加载进纹理
     */
    private int loadImageToTexture(String imgPath, int texId, int textureUnit) {
        GLES20.glActiveTexture(textureUnit);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texId);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        Bitmap bitmap = BitmapUtil.getBitmapFromFile(imgPath);
        if (bitmap == null) {
            return -1;
        }
        ByteBuffer bitmapBuffer = ByteBuffer.allocate(bitmap.getWidth() * bitmap.getHeight() * 4);
        bitmap.copyPixelsToBuffer(bitmapBuffer);
        bitmapBuffer.position(0);

        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, bitmap.getWidth(), bitmap.getHeight(), 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, bitmapBuffer);

        return 0;
    }

    private int createGLProgram(String vertexCode, String fragmentCode) {
        int program = GLES20.glCreateProgram();

        int vertexShader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
        int fragmentShader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);

        GLES20.glShaderSource(vertexShader, vertexCode);
        GLES20.glShaderSource(fragmentShader, fragmentCode);
        GLES20.glCompileShader(vertexShader);
        GLES20.glCompileShader(fragmentShader);

        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);

        GLES20.glLinkProgram(program);

        return program;
    }

    private void bindGLProgram(int program, int texture, FloatBuffer vertexBuffer, FloatBuffer textureBuffer, int textureUnit) {
        GLES20.glUseProgram(program);

        int aPositionLocation = GLES20.glGetAttribLocation(program, "a_Position");
        GLES20.glEnableVertexAttribArray(aPositionLocation);
        GLES20.glVertexAttribPointer(aPositionLocation, 2, GLES20.GL_FLOAT, false, 2 * 4, vertexBuffer);

        int aTextureCoordinateLocation = GLES20.glGetAttribLocation(program, "a_textureCoordinate");
        GLES20.glEnableVertexAttribArray(aTextureCoordinateLocation);
        GLES20.glVertexAttribPointer(aTextureCoordinateLocation, 2, GLES20.GL_FLOAT, false, 2 * 4, textureBuffer);

        GLES20.glActiveTexture(textureUnit);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture);

        int uTextureLocation = GLES20.glGetUniformLocation(program, "u_texture");
        if (textureUnit == GLES20.GL_TEXTURE0) {
            GLES20.glUniform1i(uTextureLocation, 0);
        } else  {
            GLES20.glUniform1i(uTextureLocation, 1);
        }
    }

    private void initFBO(int width, int height) {
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        fboTexture = textures[0];

        int[] frameBuffers = new int[1];
        GLES20.glGenFramebuffers(1, frameBuffers, 0);
        fbo = frameBuffers[0];

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, fboTexture);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, fboTexture, 0);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    private void render() {
        GLES20.glViewport(0, 0, mSurfaceWidth, mSurfaceHeight);
//        GLES20.glEnable(GLES20.GL_BLEND);
//        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);
    }

}
