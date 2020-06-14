package com.xiangweixin.myownstudy.opengl;

import java.nio.ByteBuffer;

public class Frame {

    private ByteBuffer mData;
    private int mWidth;
    private int mHeight;
    private int texture = -1;

    public ByteBuffer getData() {
        return mData;
    }

    public void setData(ByteBuffer mData) {
        this.mData = mData;
    }

    public int getWidth() {
        return mWidth;
    }

    public void setWidth(int mWidth) {
        this.mWidth = mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public void setHeight(int mHeight) {
        this.mHeight = mHeight;
    }

    public int getTexture() {
        return texture;
    }

    public void setTexture(int texture) {
        this.texture = texture;
    }
}
