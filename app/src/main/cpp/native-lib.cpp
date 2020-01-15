#include <jni.h>
#include <string>
#include <libavutil/frame.h>

extern "C" {
#include "libavcodec/avcodec.h" //编码
#include "libavformat/avformat.h" //封装格式处理
#include "libswscale/swscale.h" //像素处理
#include "android/native_window_jni.h"
#include "android/log.h"
#include "unistd.h"
}

#define LOGE(FORMAT, ...) __android_log_print(ANDROID_LOG_ERROR,"LC",FORMAT,##__VA_ARGS__);

extern "C"
JNIEXPORT void JNICALL
Java_com_xiangweixin_myownstudy_ffmpeg_VideoPlayerView_render(JNIEnv *env, jobject instance,
                                                              jstring videoPath_,
                                                              jobject outputSurface) {
    const char *videoPath = env->GetStringUTFChars(videoPath_, 0);

    //注册ffmpeg各个组件
    av_register_all();
    LOGE("注册成功");

    AVFormatContext *avFormatContext = nullptr;
    int error;
    char buf[1024];

    //解封装(格式处理) 获取信息
    if ((error = avformat_open_input(&avFormatContext, videoPath, nullptr, nullptr)) < 0) { //解封装
        av_strerror(error, buf, 1024);
        LOGE("Couldn't open file %s: %d(%s)", videoPath, error, buf);
        return;
    }
    if (avformat_find_stream_info(avFormatContext, nullptr) < 0) { //获取音频流/视频流的详细数据
        LOGE("获取文件内容失败");
        return;
    }

    //从各个流(视频流 音频流 字幕流)中找到视频流
    int video_index = -1;
    for (int i = 0; i < avFormatContext->nb_streams; i++) {
        if (avFormatContext->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_VIDEO) {
            video_index = i;
            break;
        }
    }
    if (video_index == -1) {
        LOGE("没有视频流");
        return;
    }

    //对视频流进行解码
    //获取解码器上下文
    AVCodecContext *avCodecContext = avFormatContext->streams[video_index]->codec;
    //获取解码器
    AVCodec *avCodec = avcodec_find_decoder(avCodecContext->codec_id);
    //打开解码器
    if (avcodec_open2(avCodecContext, avCodec, nullptr) < 0) {
        LOGE("解码器打开失败");
        return;
    }
    //申请AVPacket AVPacket的作用是保存解码之前的数据和一些附加信息，如显示时间戳（pts）、解码时间戳（dts）、数据时长，所在媒体流的索引等
    AVPacket *packet = (AVPacket *)av_malloc(sizeof(AVPacket));
    av_init_packet(packet);
    //申请AVFrame AVFrame的作用是保存解码后的数据
    AVFrame *frame = av_frame_alloc();//保存解码后的原始帧
    AVFrame *rgbFrame = av_frame_alloc();//保存转换成rgb后的帧

    uint8_t *out_buffer = (uint8_t *)av_malloc(avpicture_get_size(AV_PIX_FMT_RGBA, avCodecContext->width, avCodecContext->height));
    avpicture_fill((AVPicture *)rgbFrame, out_buffer, AV_PIX_FMT_RGBA, avCodecContext->width,avCodecContext->height);

    SwsContext* swsContext = sws_getContext(avCodecContext->width,avCodecContext->height,avCodecContext->pix_fmt,
                                            avCodecContext->width,avCodecContext->height,AV_PIX_FMT_RGBA,
                                            SWS_BICUBIC,NULL,NULL,NULL);

    ANativeWindow *nativeWindow = ANativeWindow_fromSurface(env, outputSurface);
    if (nativeWindow == nullptr) {
        LOGE("NativeWindow 获取失败");
        return;
    }
    //视频缓冲区
    ANativeWindow_Buffer native_outBuffer;

    //开始解码
    while (av_read_frame(avFormatContext, packet) >= 0) {
        LOGE("解码 %d", packet->stream_index);
        LOGE("VINDEX %d", video_index);
        avcodec_decode_video2(avCodecContext, frame, //todo);
    }

    env->ReleaseStringUTFChars(videoPath_, videoPath);
}