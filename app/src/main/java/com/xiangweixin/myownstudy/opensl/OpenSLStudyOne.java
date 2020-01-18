package com.xiangweixin.myownstudy.opensl;

/**
 * 通过OpenSL ES播放一个mp3文件
 */
public class OpenSLStudyOne {

    public static void playPCM(String pcmPath) {
        nativePlayMusic(pcmPath);
    }

    public static native void nativePlayMusic(String pcmPath);

}