package com.xiangweixin.myownstudy.opengl;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.nio.Buffer;
import java.nio.ByteBuffer;

public class BitmapUtils {

    public static Frame decodeFile(String path) {
        Bitmap bitmap = BitmapFactory.decodeFile(path);
        ByteBuffer buffer = ByteBuffer.allocate(bitmap.getWidth() * bitmap.getHeight() * 4);
        bitmap.copyPixelsToBuffer(buffer);
        buffer.position(0);

        Frame frame = new Frame();
        frame.setData(buffer);
        frame.setWidth(bitmap.getWidth());
        frame.setHeight(bitmap.getHeight());
        return frame;
    }

}
