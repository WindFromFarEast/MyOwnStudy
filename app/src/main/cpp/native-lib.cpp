#include <jni.h>
#include <string>
#include <FFMpegMusic.h>
#include "header/Result.h"
#include "SLES/OpenSLES.h"
#include "SLES/OpenSLES_Android.h"
#include "pthread.h"

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

#define LOGE(FORMAT, ...) __android_log_print(ANDROID_LOG_ERROR,"LC",FORMAT,##__VA_ARGS__);

/**
 * 一个完整的视频解封装->解码->显示到Surface上的流程 (yuv420)
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
    uint8_t *out_buffer = (uint8_t *) av_malloc(avpicture_get_size(AV_PIX_FMT_RGBA, avCodecContext->width, avCodecContext->height) *
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

/**
 * h264编码流程 yuv420p->h264
 */
extern "C"
JNIEXPORT jint JNICALL
Java_com_xiangweixin_myownstudy_ffmpeg_encode_FFmpegEncode_nativeEncode(JNIEnv *env, jclass clazz,
                                                                        jstring yuv_path,
                                                                        jstring h264_out_path,
                                                                        jint bitrate, jint frameRate, jint width, jint height) {
    const char *yuvPath = env->GetStringUTFChars(yuv_path, nullptr);
    const char *h264OutPath = env->GetStringUTFChars(h264_out_path, nullptr);

    FILE *in_file = fopen(yuvPath, "rb");
    if (in_file == nullptr) {
        return FILE_NOT_EXIST;
    }

    av_register_all();

    //初始化AVFormatContext和AVOutputFormat
    AVFormatContext *formatContext;
    AVOutputFormat *outputFormat;
    formatContext = avformat_alloc_context();
    outputFormat = av_guess_format(nullptr, h264OutPath, nullptr); //AVOutputFormat从输出文件中guess出来
    formatContext->oformat = outputFormat;
    //初始化AVIOContext
    if (avio_open(&formatContext->pb, h264OutPath, AVIO_FLAG_READ_WRITE) < 0) {
        return FILE_NOT_EXIST;
    }
    //初始化AVStream
    AVStream *stream = avformat_new_stream(formatContext, nullptr);
    //获取AVCodecContext并设置编码器参数
    AVCodecContext *codecContext = stream->codec;
    codecContext->codec_id = outputFormat->video_codec;
    codecContext->codec_type = AVMEDIA_TYPE_VIDEO;
    codecContext->pix_fmt = AV_PIX_FMT_YUV420P;
    codecContext->width = width;
    codecContext->height = height;
    codecContext->bit_rate = bitrate;
    codecContext->gop_size = 10;
    codecContext->framerate = (AVRational) {frameRate, 1};
    codecContext->time_base = (AVRational) {1, frameRate}; //时间基: 一分钟frameRate帧
    codecContext->max_b_frames = 1;
    codecContext->qmin = 10;
    codecContext->qmax = 51;

    AVDictionary *dictionary = nullptr;
    if (codecContext->codec_id == AV_CODEC_ID_H264) {
        av_dict_set(&dictionary, "preset", "slow", 0);
        av_dict_set(&dictionary, "tune", "zerolatency", 0);
    }

    av_dump_format(formatContext, 0, h264OutPath, 1);

    //查找并打开编码器
    AVCodec *codec = nullptr;
    codec = avcodec_find_encoder(codecContext->codec_id);
    if (avcodec_open2(codecContext, codec, &dictionary) < 0) {
        return CODEC_OPEN_FAILED;
    }

    AVFrame *frame;
    uint8_t *picture_buf;
    int picture_size;
    frame = av_frame_alloc();
    picture_size = avpicture_get_size(codecContext->pix_fmt, codecContext->width, codecContext->height);
    picture_buf = static_cast<uint8_t *>(av_malloc(picture_size  * sizeof(uint8_t)));
    avpicture_fill((AVPicture *)frame, picture_buf, codecContext->pix_fmt, codecContext->width, codecContext->height);

    avformat_write_header(formatContext, nullptr);

    AVPacket *packet;
    av_new_packet(packet, picture_size);

    int y_size = codecContext->width * codecContext->height;
    int readCount = 0;
    int encodedFrameCount = 0;
    while (fread(picture_buf, 1, y_size * 3 / 2, in_file) > 0) {
        frame->data[0] = picture_buf; //y
        frame->data[1] = picture_buf + y_size; //u
        frame->data[2] = picture_buf + y_size * 5 / 4; //v
        //处理pts
        frame->pts = readCount * (1000 / codecContext->framerate.num);
        int got_picture = 0;
        //编码
        int ret = avcodec_encode_video2(codecContext, packet, frame, &got_picture);
        if (ret < 0) {
            return ENCODE_FAILED;
        }
        if (got_picture == 1) {
            encodedFrameCount++;
            packet->stream_index = stream->index;
            av_write_frame(formatContext, packet);
            av_free_packet(packet);
        }
    }

    //flush encode. 将编码器中剩余的packet输出
    AVPacket enc_pkt;
    if (!(formatContext->streams[0]->codec->codec->capabilities & CODEC_CAP_DELAY)) {
        return FLUSH_ENCODE_FAILED;
    }
    while (1) {
        enc_pkt.data = nullptr;
        enc_pkt.size = 0;
        av_init_packet(&enc_pkt);//因为av_init_packet不会初始化data和size 故data和size需要手动设置

        int got_frame;
        int ret = avcodec_encode_video2(codecContext, &enc_pkt, nullptr, &got_frame);
        av_frame_free(nullptr);
        if (ret < 0) break;
        if (!got_frame) {
            ret = 0;
            break;
        }
        ret = av_write_frame(formatContext, &enc_pkt);
        if (ret < 0) break;
    }

    av_write_trailer(formatContext);

    //clean
    if (stream) {
        avcodec_close(stream->codec);
        av_free(frame);
        av_free(picture_buf);
    }
    avio_close(formatContext->pb);
    avformat_free_context(formatContext);

    env->ReleaseStringUTFChars(yuv_path, yuvPath);
    env->ReleaseStringUTFChars(h264_out_path, h264OutPath);
    return SUCCESS;
}

//-----------------------------------------OpenSL ES-----------------------------------------//

SLObjectItf engineObject = nullptr;//引擎接口对象
SLEngineItf engineEngine = nullptr;//具体的引擎对象实例

SLObjectItf  outputMixObject = nullptr;
SLEnvironmentalReverbItf outputMixEnvironmentalReverb = nullptr;//混音器对象实例
SLEnvironmentalReverbSettings settings = SL_I3DL2_ENVIRONMENT_PRESET_DEFAULT;

SLObjectItf audioPlayer = nullptr;
SLPlayItf slPlayItf = nullptr;
SLAndroidSimpleBufferQueueItf slBufferQueueItf = nullptr;

//创建引擎
void createEngine() {
    //1、创建并实现SLObjectItf
    slCreateEngine(&engineObject, 0, nullptr, 0, nullptr, nullptr);
    (*engineObject)->Realize(engineObject, SL_BOOLEAN_FALSE);
    //2、通过SLObjectItf获取SLEngineItf
    (*engineObject)->GetInterface(engineObject, SL_IID_ENGINE, &engineEngine);
}

//创建混音器
void createMixVolume() {
    (*engineEngine)->CreateOutputMix(engineEngine, &outputMixObject, 0, 0, 0);
    (*outputMixObject)->Realize(outputMixObject, SL_BOOLEAN_FALSE);
    SLresult result = (*outputMixObject)->GetInterface(outputMixObject, SL_IID_ENVIRONMENTALREVERB, &outputMixEnvironmentalReverb);
    if (result == SL_RESULT_SUCCESS) {
        (*outputMixEnvironmentalReverb)->SetEnvironmentalReverbProperties(outputMixEnvironmentalReverb, &settings);
    }
}

size_t bufferSize = 0;
void *buffer;

//回调的函数
void getQueueCallback(SLAndroidSimpleBufferQueueItf slBufferQueueItf, void *context) {
    bufferSize = 0;
    int ret = getPCM(&buffer, &bufferSize);
    if (ret != 0) {
        LOGE("解码失败");
        return;
    }
    if (buffer != nullptr & bufferSize > 0) {
        //将得到的PCM数据加入队列中
        (*slBufferQueueItf)->Enqueue(slBufferQueueItf, buffer, bufferSize);
    }
}

//创建播放器并播放
void createPlayer(const char *music) {
    int rate;
    int channels;
    createFFmpeg(music, &rate, &channels);
    /**
     * struct SLDataLocator_AndroidBufferQueue_ {
     *   SLuint32    locatorType;//缓冲区队列类型
     *   SLuint32    numBuffers;//buffer位数
     * }
     */
    SLDataLocator_AndroidBufferQueue android_queue = {SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE, 2};
    /**
    typedef struct SLDataFormat_PCM_ {
        SLuint32        formatType;  pcm
        SLuint32        numChannels;  通道数
        SLuint32        samplesPerSec;  采样率
        SLuint32        bitsPerSample;  采样位数
        SLuint32        containerSize;  包含位数
        SLuint32        channelMask;     立体声
        SLuint32        endianness;    end标志位
    } SLDataFormat_PCM;
    */
    SLDataFormat_PCM pcm = {
            SL_DATAFORMAT_PCM, static_cast<SLuint32>(channels), static_cast<SLuint32>(rate * 1000),
            SL_PCMSAMPLEFORMAT_FIXED_16,
            SL_PCMSAMPLEFORMAT_FIXED_16,
            SL_SPEAKER_FRONT_LEFT | SL_SPEAKER_FRONT_RIGHT, SL_BYTEORDER_LITTLEENDIAN
    };
    /**
    * typedef struct SLDataSource_ {
        void *pLocator;//缓冲区队列
        void *pFormat;//数据样式,配置信息
    } SLDataSource;
    * */
    SLDataSource dataSource = {&android_queue, &pcm};

    SLDataLocator_OutputMix slDataLocator_outputMix = {
            SL_DATALOCATOR_OUTPUTMIX, outputMixObject
    };

    SLDataSink slDataSink = {
            &slDataLocator_outputMix, NULL
    };

    const SLInterfaceID ids[3]={
            SL_IID_BUFFERQUEUE, SL_IID_EFFECTSEND,SL_IID_VOLUME
    };
    const SLboolean req[3]={
            SL_BOOLEAN_FALSE, SL_BOOLEAN_FALSE, SL_BOOLEAN_FALSE
    };

    (*engineEngine)->CreateAudioPlayer(engineEngine, &audioPlayer, &dataSource, &slDataSink, 3, ids, req);
    LOGE("CreateAudioPlayer done")
    (*audioPlayer)->Realize(audioPlayer, SL_BOOLEAN_FALSE);
    LOGE("audio player Realize done")
    (*audioPlayer)->GetInterface(audioPlayer, SL_IID_PLAY, &slPlayItf);
    LOGE("GetInterface(audioPlayer, SL_IID_PLAY, &slPlayItf) done")
    (*audioPlayer)->GetInterface(audioPlayer, SL_IID_BUFFERQUEUE, &slBufferQueueItf);//注册缓冲区 通过缓冲区里面的数据进行播放
    LOGE("GetInterface(audioPlayer, SL_IID_BUFFERQUEUE, &slBufferQueueItf) done")
    (*slBufferQueueItf)->RegisterCallback(slBufferQueueItf, getQueueCallback, nullptr);
    LOGE("RegisterCallback done")
    //播放
    (*slPlayItf)->SetPlayState(slPlayItf, SL_PLAYSTATE_PLAYING);
    LOGE("SetPlayState done")
    //开始播放
    getQueueCallback(slBufferQueueItf, nullptr);
    LOGE("getQueueCallback done")
}

/**
 * 播放PCM
 */
extern "C"
JNIEXPORT void JNICALL
Java_com_xiangweixin_myownstudy_opensl_OpenSLStudyOne_nativePlayMusic(JNIEnv *env, jclass clazz,
                                                                    jstring path) {
    const char *music = env->GetStringUTFChars(path, nullptr);
    createEngine();
    createMixVolume();
    createPlayer(music);
    env->ReleaseStringUTFChars(path, music);

}

//-----------------------------------------OpenSL ES-----------------------------------------//









