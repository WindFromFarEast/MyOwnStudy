//
// Created by xwx on 2020/1/18.
//

#ifndef MYOWNSTUDY_FFMPEGMUSIC_H
#define MYOWNSTUDY_FFMPEGMUSIC_H

#include <jni.h>
#include <android/log.h>
#include <string>
#include <vector>
#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>
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

int createFFmpeg(const char* music, int *rate, int *channel);
int getPCM(void **pcm, size_t *pcm_size);
void releaseFFmpeg();

//class FFmpegMusic {
//public:
//    FFmpegMusic();
//    ~FFmpegMusic();
//    void setAvCodecContext(AVCodecContext *avCodecContext);
//
//    int put(AVPacket *avPacket);
//    int get(AVPacket *avPacket);
//
//    void play();
//    void stop();
//
//    int CreatePlayer();//创建openel播放器
//
//public:
//    int index;//音频流索引
//    int isPlay = -1;
//    pthread_t  playId;
//    std::vector<AVPacket*> queue;
//    AVCodecContext *codecContext;
//
//    SwrContext *swrContext;
//    uint8_t *out_buffer;
//    int out_channel_nb;
//
//    pthread_mutex_t mutex;
//    pthread_cond_t cond;
//
//    double clock;//从第一帧开始所需时间
//
//    AVRational time_base;
//
//    SLObjectItf engineObject;
//    SLEngineItf engineEngine;
//    SLEnvironmentalReverbItf outputMixEnvironmentalReverb;
//    SLObjectItf outputMixObject;
//    SLObjectItf bqPlayerObject;
//    SLEffectSendItf bqPlayerEffectSend;
//    SLVolumeItf  bqPlayerVolume;
//    SLPlayItf bqPlayerPlay;
//    SLAndroidSimpleBufferQueueItf bqPlayerBufferQueue;
//};

#endif //MYOWNSTUDY_FFMPEGMUSIC_H
