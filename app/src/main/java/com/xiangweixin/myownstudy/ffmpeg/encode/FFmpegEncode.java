package com.xiangweixin.myownstudy.ffmpeg.encode;

public class FFmpegEncode {

    public static int encode(String yuvPath, String h264OutPath,int bitrate, int frameRate, int width, int height) {
        return nativeEncode(yuvPath, h264OutPath, bitrate, frameRate, width, height);
    }

    private native static int nativeEncode(String yuvPath, String h264OutPath, int bitrate, int frameRate, int width, int height);

}
