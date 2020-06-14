package com.xiangweixin.myownstudy.opengl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class BufferUtil {

    public static FloatBuffer getFloatBuffer(float[] data) {
        FloatBuffer floatBuffer = ByteBuffer.allocateDirect(data.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(data);
        floatBuffer.position(0);
        return floatBuffer;
    }

    public static ByteBuffer createByteBuffer(int size) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(size)
                .order(ByteOrder.nativeOrder());
        byteBuffer.position(0);
        return byteBuffer;
    }

}
