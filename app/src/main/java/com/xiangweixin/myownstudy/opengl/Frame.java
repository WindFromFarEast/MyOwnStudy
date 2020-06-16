package com.xiangweixin.myownstudy.opengl;

import java.nio.ByteBuffer;

public class Frame {

    public interface FrameType {
        int DEFAULT = 0;
        int RGBA8 = 1;
        int OPENGL_RGBA8 = 1 << 2;
    }

    private int type = FrameType.DEFAULT;
    private ByteBuffer data;
    private int width;
    private int height;
    private int texture = -1;

    public ByteBuffer getData() {
        return data;
    }

    public void setData(ByteBuffer mData) {
        this.data = mData;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int mWidth) {
        this.width = mWidth;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int mHeight) {
        this.height = mHeight;
    }

    public int getTexture() {
        return texture;
    }

    public void setTexture(int texture) {
        this.texture = texture;
    }
}
