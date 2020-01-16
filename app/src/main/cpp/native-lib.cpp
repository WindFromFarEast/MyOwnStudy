#include <jni.h>
#include <string>

extern "C" {
#include "libavcodec/avcodec.h" //编码
#include "libavformat/avformat.h" //封装格式处理
#include "libswscale/swscale.h" //像素处理
#include "android/native_window_jni.h"
#include "android/log.h"
#include "unistd.h"
#include <libavutil/frame.h>
}

#define LOGE(FORMAT, ...) __android_log_print(ANDROID_LOG_ERROR,"LC",FORMAT,##__VA_ARGS__);

/**
 * 一个完整的视频解封装->解码->显示到Surface上的流程
 */
extern "C"
JNIEXPORT void JNICALL
Java_com_xiangweixin_myownstudy_ffmpeg_onlyPlayVideo_OnlyPlayVideoView_render(JNIEnv *env, jobject instance,
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
    /**
     * 此处需要两个AVFrame 每个AVFrame内部都有一个数据缓冲区
     * 一个AVFrame用来保存从AVPacket中解码出的数据，这个Frame的缓冲区不需要手动分配，会通过avcodec_decode_video分配和填充
     * 另一个AVFrame用来存放将解码出来的原始数据变换为需要的数据格式（例如RGB，RGBA）的数据，这个AVFrame需要手动的分配数据缓存空间
     */
    AVFrame *frame = av_frame_alloc();//保存解码后的原始帧
    AVFrame *rgbFrame = av_frame_alloc();//保存转换成rgb后的帧

    //为AVFrame分配内部缓冲空间
    uint8_t *out_buffer = (uint8_t *)av_malloc(avpicture_get_size(AV_PIX_FMT_RGBA, avCodecContext->width, avCodecContext->height) *
                                                       sizeof(uint8_t));
    avpicture_fill((AVPicture *)rgbFrame, out_buffer, AV_PIX_FMT_RGBA, avCodecContext->width,avCodecContext->height);

    SwsContext* swsContext = sws_getContext(avCodecContext->width,avCodecContext->height,avCodecContext->pix_fmt,
                                            avCodecContext->width,avCodecContext->height,AV_PIX_FMT_RGBA,
                                            SWS_BICUBIC,NULL,NULL,NULL);

    //用Surface创建解码后要显示数据的Window
    ANativeWindow *nativeWindow = ANativeWindow_fromSurface(env, outputSurface);
    if (nativeWindow == nullptr) {
        LOGE("NativeWindow 获取失败");
        return;
    }
    //NativeWindow图像显示数据的缓冲区
    ANativeWindow_Buffer native_outBuffer;
    int hasFrameToDecode;//是否还有帧可以被解码
    //开始解码  av_read_frame 对应 av_free_packet
    while (av_read_frame(avFormatContext, packet) >= 0) {
        LOGE("解码 packect index: %d", packet->stream_index);
        LOGE("VIDEO INDEX: %d", video_index);
        if (packet->stream_index == video_index) {
            avcodec_decode_video2(avCodecContext, frame, &hasFrameToDecode, packet);//decode
            LOGE("解码了一帧");
            if (hasFrameToDecode) {
                LOGE("转换并绘制")
                //绘制之前先配置NativeWindow
                ANativeWindow_setBuffersGeometry(nativeWindow, avCodecContext->width, avCodecContext->height, WINDOW_FORMAT_RGBA_8888);
                //上锁
                ANativeWindow_lock(nativeWindow, &native_outBuffer, nullptr);
                //将解码后的数据转换为rgb格式
                sws_scale(swsContext, frame->data, frame->linesize, 0, frame->height, rgbFrame->data, rgbFrame->linesize);
                //获取NativeWindow目标图像显示缓冲区的首地址
                uint8_t *dst = (uint8_t *) native_outBuffer.bits;
                //获取一行有多少bytes rgba  ps:native_outBuffer.stride表示一行有多少pixels 而不是一行多少bytes
                int destStride = native_outBuffer.stride * 4;
                //rgb像素数据的首地址
                uint8_t *src = rgbFrame->data[0];
                //rgb像素数据的stride（表示一行多少bytes)
                int srcStride = rgbFrame->linesize[0];
                for (int i = 0; i < avCodecContext->height; ++i) {
                    //将rgbFrame中的每一行数据复制给native window的图像显示缓冲区
                    memcpy(dst + i * destStride, src + i * srcStride, srcStride);
                }

                ANativeWindow_unlockAndPost(nativeWindow);
                usleep(1000 * 16);
            }
        }
        av_free_packet(packet);
    }

    ANativeWindow_release(nativeWindow);
    av_frame_free(&frame);
    av_frame_free(&rgbFrame);
    avcodec_close(avCodecContext);
    avformat_close_input(&avFormatContext);
    env->ReleaseStringUTFChars(videoPath_, videoPath);
}