//
// Created by xiangweixin on 2020-03-01.
//

#include "jni.h"
#include <string>
#include <android/log.h>
#include "Result.h"

extern "C" {
#include "libavcodec/avcodec.h" //编解码
#include "libavformat/avformat.h" //封装格式处理
#include "libswscale/swscale.h" //像素处理
#include "android/native_window_jni.h"
#include "android/log.h"
#include "unistd.h"
#include <libavutil/frame.h>
#include "libavutil/opt.h"
#include "libavutil/imgutils.h"
}

#define TAG "FFmpegJni"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, __VA_ARGS__)

int flush_encoder(AVFormatContext *formatContext, unsigned int stream_index) {
    int ret;
    int got_frame;
    AVPacket enc_pkt;
    if (!(formatContext->streams[stream_index]->codec->codec->capabilities & CODEC_CAP_DELAY)) {
        return 0;
    }
    while (1) {
        enc_pkt.data = nullptr;
        enc_pkt.size = 0;
        av_init_packet(&enc_pkt);
        ret = avcodec_encode_video2(formatContext->streams[stream_index]->codec, &enc_pkt, nullptr, &got_frame);
        av_frame_free(nullptr);
        if (ret < 0) break;
        if (!got_frame) {
            ret = 0;
            break;
        }
        LOGD(TAG, "Flush Encoder: Succeed to encode 1 frame!\tsize:%5d\n",enc_pkt.size);
        ret = av_write_frame(formatContext, &enc_pkt);
        if (ret < 0) break;
    }
    return ret;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_xiangweixin_myownstudy_ffmpeg_encode_FFmpeg_nativeEncode(JNIEnv *env, jclass type,
                                                                  jstring yuv_, jstring h264_,
                                                                  jint bitrate, jint frameRate,
                                                                  jint width, jint height) {
    const char *yuv = env->GetStringUTFChars(yuv_, 0);
    const char *h264 = env->GetStringUTFChars(h264_, 0);

    LOGD(TAG, "nativeEncode start... yuvPath: %s, h264Path: %s", yuv, h264);

    FILE *in_file = fopen(yuv, "rb");
    int frameNum = 100;//编码100帧

    AVFormatContext *formatContext;
    AVOutputFormat *outputFormat;
    av_register_all();
    formatContext = avformat_alloc_context();
    outputFormat = av_guess_format(nullptr, h264, nullptr);
    formatContext->oformat = outputFormat;

    if (avio_open(&formatContext->pb, h264, AVIO_FLAG_READ_WRITE) < 0) {
        LOGE(TAG, "avio_open failed.");
        return FFMPEG_AVIO_OPEN_FAILED;
    }

    AVStream *video_st = nullptr;
    video_st = avformat_new_stream(formatContext, 0);
    if (video_st == nullptr) {
        LOGE(TAG, "avformat_new_stream failed.");
        return FFMPEG_NEW_STREAM_FAILED;
    }
    video_st->time_base.num = 1;//分子
    video_st->time_base.den = 25;//分母

    AVCodecContext *codecContext = nullptr;
    codecContext = video_st->codec;
    codecContext->codec_id = outputFormat->video_codec;
    codecContext->codec_type = AVMEDIA_TYPE_VIDEO;
    codecContext->pix_fmt = AV_PIX_FMT_YUV420P;
    codecContext->width = width;
    codecContext->height = height;
    codecContext->time_base.num = 1;
    codecContext->time_base.den = 25;
    codecContext->bit_rate = bitrate;
    codecContext->gop_size = 250;
    codecContext->qmin = 10;//todo 了解qmin和qmax的作用
    codecContext->qmax = 51;
    codecContext->max_b_frames = 3;

    AVDictionary *param = nullptr;
    if (codecContext->codec_id == AV_CODEC_ID_H264) {
        av_dict_set(&param, "preset", "slow", 0);
        av_dict_set(&param, "tune", "zerolatency", 0);
    }
    if (codecContext->codec_id == AV_CODEC_ID_H265) {
        av_dict_set(&param, "preset", "ultrafast", 0);
        av_dict_set(&param, "tune", "zero-latency", 0);
    }

    av_dump_format(formatContext, 0, h264, 1);

    AVCodec *codec = nullptr;
    codec = avcodec_find_encoder(codecContext->codec_id);
    if (!codec) {
        LOGE(TAG, "avcodec_find_encoder failed.");
        return FFMPEG_FIND_ENCODER_FAILED;
    }
    if (avcodec_open2(codecContext, codec, &param) < 0) {
        LOGE(TAG, "avcodec_open2 failed.");
        return FFMPEG_CODEC_OPEN_FAILED;
    }

    AVFrame *frame = nullptr;
    int picture_size;
    uint8_t *picture_buf;
    frame = av_frame_alloc();
    picture_size = avpicture_get_size(codecContext->pix_fmt, codecContext->width, codecContext->height);
    picture_buf = static_cast<uint8_t *>(av_malloc(picture_size));
    avpicture_fill((AVPicture *)frame, picture_buf, codecContext->pix_fmt, codecContext->width, codecContext->height);

    avformat_write_header(formatContext, nullptr);

    AVPacket packet;
    av_new_packet(&packet, picture_size);
    int y_size = codecContext->width * codecContext->height;

    int frameCount = 0;

    for (int i = 0; i < frameNum; ++i) {
        //Read raw yuv data..
        if (fread(picture_buf, 1, y_size * 3 / 2, in_file) <= 0) {
            LOGE(TAG, "fread file failed.");
            return FREAD_FAILED;
        } else if (feof(in_file)) {
            break;
        }
        frame->data[0] = picture_buf; //Y
        frame->data[1] = picture_buf + y_size; //U
        frame->data[2] = picture_buf + y_size * 5 / 4; //V

        //calculate pts
        frame->pts = i * (video_st->time_base.den) / ((video_st->time_base.num) * 25);
        int got_picture = 0;

        //encode..
        int ret = avcodec_encode_video2(codecContext, &packet, frame, &got_picture);
        if (ret < 0) {
            LOGE(TAG, "failed to encode.");
            return FFMPEG_ENCODE_VIDEO_FAILED;
        }
        if (got_picture == 1) {
            LOGD(TAG, "encode frame success, frameCount: %5d", frameCount);
            frameCount++;
            packet.stream_index = video_st->index;
            ret = av_write_frame(formatContext, &packet);//write packet to file.
            av_free_packet(&packet);
        }
    }

    //flush encode.. must do it
    int ret = flush_encoder(formatContext, 0);
    if (ret < 0) {
        LOGE(TAG, "flush encoder failed.");
        return FLUSH_ENCODER_FAILED;
    }

    av_write_trailer(formatContext);

    if (video_st) {
        avcodec_close(codecContext);
        av_free(frame);
        av_free(picture_buf);
    }
    avio_close(formatContext->pb);
    avformat_free_context(formatContext);

    env->ReleaseStringUTFChars(yuv_, yuv);
    env->ReleaseStringUTFChars(h264_, h264);
}



































