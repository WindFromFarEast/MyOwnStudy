//
// Created by xwx on 2020/1/18.
//

#ifndef MYOWNSTUDY_FFMPEGMUSIC_H
#define MYOWNSTUDY_FFMPEGMUSIC_H

#include <jni.h>
#include <android/log.h>
#include <string>
#include "Result.h"

extern "C" {
//编码
#include "libavcodec/avcodec.h"
//封装格式处理
#include "libavformat/avformat.h"
#include "libswresample/swresample.h"
//像素处理
#include "libswscale/swscale.h"
#include <android/native_window_jni.h>
#include <unistd.h>
}
#define LOGE(FORMAT,...) __android_log_print(ANDROID_LOG_ERROR,"LC",FORMAT,##__VA_ARGS__);

int createFFmpeg(const char* music, int *rate, int *channel);
int getPCM(void **pcm, size_t *pcm_size);
void releaseFFmpeg();

#endif //MYOWNSTUDY_FFMPEGMUSIC_H
