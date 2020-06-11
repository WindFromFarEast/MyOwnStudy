package com.xiangweixin.myownstudy.opengl;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import com.xiangweixin.myownstudy.util.LogUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL;
import javax.microedition.khronos.opengles.GL10;

/**
 * 用来显示一张图片，练习图片的缩放、裁剪、旋转、平移等
 */
public class ImageSurfaceViewRender implements GLSurfaceView.Renderer {

    private static final String TAG = ImageSurfaceViewRender.class.getSimpleName();

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

    private float[] vertexPosition = {
            -1f, -1f,
            -1f, 1f,
            1f, 1f,
            -1f, -1f,
            1f, 1f,
            1f, -1f
    };
    private float[] texturePosition = {
            0f, 1f, 0f, 0f, 1f, 0f, 0f, 1f, 1f, 0f, 1f, 1f
    };
    private FloatBuffer vertexData;
    private FloatBuffer textureData;

    private int texID = -1;
    private int programID = -1;

    private int imageWidth = 0;
    private int imageHeight = 0;
    private int viewWitdh = 0;
    private int viewHeight = 0;

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        initData();
        createTexture();
        int ret = bindImageToTexture(texID, "sdcard/wallpaper.png");
        programID = createProgram(vertexShaderCode, fragmentShaderCode);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        viewWitdh = width;
        viewHeight = height;
        GLES20.glViewport(0, 0, width, height);
        LogUtil.formatI(TAG, "glViewPort >>> x: %d, y: %d, width: %d, height: %d", 0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        float inputAspectRatio = imageHeight * 1.0f / imageWidth;
        float[] vsPos = {
                -1f, -inputAspectRatio,
                -1f, inputAspectRatio,
                1f, inputAspectRatio,
                -1f, -inputAspectRatio,
                1f, inputAspectRatio,
                1f, -inputAspectRatio
        };
        FloatBuffer vData = ByteBuffer.allocateDirect(vsPos.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(vsPos);
        vData.position(0);

        float[] orthoM = new float[16];
        float aspectRatio = viewHeight * 1.0f / viewWitdh;
        Matrix.setIdentityM(orthoM, 0);
        Matrix.orthoM(orthoM, 0, -1f, 1f, -aspectRatio, aspectRatio, -1f, 1f);

        float[] matrix = new float[16];
        Matrix.setIdentityM(matrix, 0);
        Matrix.rotateM(matrix, 0, 90f, 0, 0f, 1f);

        float[] finalMatrix = new float[16];
        Matrix.setIdentityM(finalMatrix, 0);
        Matrix.multiplyMM(finalMatrix, 0, orthoM, 0, matrix, 0);
        drawWithProgram(programID, vData, textureData, texID, finalMatrix);
    }

    private void initData() {
        vertexData = ByteBuffer.allocateDirect(vertexPosition.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(vertexPosition);
        vertexData.position(0);

        textureData = ByteBuffer.allocateDirect(texturePosition.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(texturePosition);
        textureData.position(0);
    }

    private void createTexture() {
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        texID = textures[0];
    }

    private int bindImageToTexture(int textureID, String path) {
        Bitmap bitmap = BitmapFactory.decodeFile(path);
        ByteBuffer buffer = ByteBuffer.allocate(bitmap.getWidth() * bitmap.getHeight() * 4);
        bitmap.copyPixelsToBuffer(buffer);
        buffer.position(0);
        imageWidth = bitmap.getWidth();
        imageHeight = bitmap.getHeight();

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureID);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, bitmap.getWidth(), bitmap.getHeight(), 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer);

        return 0;
    }

    private int createProgram(String vertex, String fragment) {
        int vertexShader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
        if (vertexShader > 0) {
            GLES20.glShaderSource(vertexShader, vertex);
            GLES20.glCompileShader(vertexShader);
            int[] compile = new int[1];
            GLES20.glGetShaderiv(vertexShader, GLES20.GL_COMPILE_STATUS, compile, 0);
            if (compile[0] != GLES20.GL_TRUE) {
                LogUtil.e(TAG, "Compile vertex shader failed.");
                GLES20.glDeleteShader(vertexShader);
                vertexShader = 0;
                return -1;
            }
        }

        int fragmentShader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
        if (fragmentShader > 0) {
            GLES20.glShaderSource(fragmentShader, fragment);
            GLES20.glCompileShader(fragmentShader);
            int[] compile = new int[1];
            GLES20.glGetShaderiv(fragmentShader, GLES20.GL_COMPILE_STATUS, compile, 0);
            if (compile[0] != GLES20.GL_TRUE) {
                LogUtil.e(TAG, "Compile fragment shader failed.");
                GLES20.glDeleteShader(fragmentShader);
                fragmentShader = 0;
                return -1;
            }
        }

        int program = GLES20.glCreateProgram();
        if (program == 0) {
            LogUtil.e(TAG, "Create program failed.");
            return -1;
        }
        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glLinkProgram(program);
        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] != GLES20.GL_TRUE) {
            LogUtil.e(TAG, "Link program failed.");
            GLES20.glDeleteProgram(program);
            program = 0;
            return -1;
        }

        return program;
    }

    private void drawWithProgram(int programID, FloatBuffer vertexPos, FloatBuffer fragPos, int texture, float[] matrix) {
        GLES20.glUseProgram(programID);

        int aPositionLocation = GLES20.glGetAttribLocation(programID, "a_position");
        GLES20.glEnableVertexAttribArray(aPositionLocation);
        GLES20.glVertexAttribPointer(aPositionLocation, 2, GLES20.GL_FLOAT, false, 0, vertexPos);

        int aTexturePosLocation = GLES20.glGetAttribLocation(programID, "a_texturePosition");
        GLES20.glEnableVertexAttribArray(aTexturePosLocation);
        GLES20.glVertexAttribPointer(aTexturePosLocation, 2, GLES20.GL_FLOAT, false, 0, fragPos);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture);
        int uTexturePos = GLES20.glGetUniformLocation(programID, "u_texture");
        GLES20.glUniform1f(uTexturePos, 0);//默认纹理单元为0

        int mvpLocation = GLES20.glGetUniformLocation(programID, "mvpMatrix");
        GLES20.glUniformMatrix4fv(mvpLocation, 1, false, matrix, 0);

        //draw.
        GLES20.glClearColor(0, 0, 0, 1);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);
    }

}
