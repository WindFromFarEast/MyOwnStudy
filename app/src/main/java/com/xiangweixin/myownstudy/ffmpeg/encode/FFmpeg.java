package com.xiangweixin.myownstudy.ffmpeg.encode;

public class FFmpeg {

    /**
     * 将yuv420编码为h264
     * https://blog.csdn.net/DaveBobo/article/details/79648900
     */
    public static int encode(String yuv, String h264, int bitrate, int frameRate, int width, int height) {
        return nativeEncode(yuv, h264, bitrate, frameRate, width, height);
    }

    private native static int nativeEncode(String yuv, String h264, int bitrate, int frameRate, int width, int height);

}
