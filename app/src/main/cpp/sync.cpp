////
//// Created by xiangweixin on 2020-02-18.
////
//#include <jni.h>
//#include <string>
//#include "header/Result.h"
//#include "SLES/OpenSLES.h"
//#include "SLES/OpenSLES_Android.h"
//#include "pthread.h"
//#include "FFmpegVideo.h"
//extern "C" {
//#include "libavcodec/avcodec.h" //编解码
//#include "libavformat/avformat.h" //封装格式处理
//#include "libswscale/swscale.h" //像素处理
//#include "android/native_window_jni.h"
//#include "android/log.h"
//#include "unistd.h"
//#include <libavutil/frame.h>
//#include "libavutil/opt.h"
//#include "libavutil/imgutils.h"
//}
//
//#define LOGE(FORMAT, ...) __android_log_print(ANDROID_LOG_ERROR,"LC",FORMAT,##__VA_ARGS__);
//
//const char *inputPath;
//AVFormatContext *pFormatCtx;
//AVPacket *packet;
//ANativeWindow *window = 0;
//pthread_t p_tid;
//int64_t *totalTime;
//int64_t duration;
//FFmpegVideo *ffmpegVideo;
//
//
//void init();
//
//extern "C"
//JNIEXPORT jint JNICALL
//Java_com_xiangweixin_myownstudy_ffmpeg_sycnplayvideo_SyncPlayVideoActivity_nativePlay(JNIEnv *env,
//                                                                                      jobject instance,
//                                                                                      jstring videoPath_) {
//    inputPath = env->GetStringUTFChars(videoPath_, 0);
//
//    // TODO
//    init();
//    ffmpegVideo = new FFmpegVideo;
//
//    env->ReleaseStringUTFChars(videoPath_, inputPath);
//}
//
//void init() {
//    av_register_all();
//
//    pFormatCtx = avformat_alloc_context();
//
//    if (avformat_open_input(&pFormatCtx, inputPath, nullptr, nullptr) != 0) {
//        LOGE("打开视频失败");
//    }
//
//    if (avformat_find_stream_info(pFormatCtx, nullptr) < 0) {
//        LOGE("获取视频信息失败");
//    }
//
//    //得到视频总时间
//    if (pFormatCtx->duration != AV_NOPTS_VALUE) {
//        duration = pFormatCtx->duration;
//    }
//}
//
//void *begin(void *agrs) {
//    LOGE("开启解码线程");
//    //找到视频流和音频流
//    for (int i = 0; i < pFormatCtx->nb_streams; ++i) {
//        //获取解码器
//        AVCodecContext *avCodecContext = pFormatCtx->streams[i]->codec;
//        AVCodec *avCodec = avcodec_find_decoder(avCodecContext->codec_id);
//
//        //copy一个解码器
//        AVCodecContext *codecContext = avcodec_alloc_context3(avCodec);
//        avcodec_copy_context(codecContext, avCodecContext);
//        //打开解码器
//        if (avcodec_open2(codecContext, avCodec, nullptr) < 0) {
//            LOGE("解码器打开失败")
//            continue;
//        }
//
//        if (pFormatCtx->streams[i]->codec->codec_type == AVMEDIA_TYPE_VIDEO) {
//            //视频流
//            ffmpegVideo->index = i;
//            ffmpegVideo->setAVCodecContext(codecContext);
//            ffmpegVideo->time_base = pFormatCtx->streams[i]->time_base;
//            if (window) {
//                ANativeWindow_setBuffersGeometry(window, ffmpegVideo->codecContext->width, ffmpegVideo->codecContext->height, WINDOW_FORMAT_RGBA_8888);
//            }
//        } else if (pFormatCtx->streams[i]->codec->codec_type == AVMEDIA_TYPE_AUDIO) {
//            //音频流
//
//        }
//    }
//}
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
