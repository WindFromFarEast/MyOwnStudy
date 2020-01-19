//
// Created by xwx on 2020/1/18.
//

#include "header/FFMpegMusic.h"

AVFormatContext *pFormatCtx;
AVCodecContext *pCodecCtx;
AVCodec *pCodec;
AVPacket *packet;
AVFrame *frame;
SwrContext *swrContext;
uint8_t *out_buffer;
int out_channel_nb;
int audio_stream_idx = -1;

int createFFmpeg(const char *music, int *rate, int *channel) {
    av_register_all();
    pFormatCtx = avformat_alloc_context();
    LOGE("music path: %s", music);
    int error;
    char buf[] = "";
    //解封装
    if ((error = avformat_open_input(&pFormatCtx, music, nullptr, nullptr)) < 0) {
        av_strerror(error, buf, 1024);
        LOGE("Cannot open file: %s", music)
    }
    //获取文件的流信息
    if (avformat_find_stream_info(pFormatCtx, nullptr) < 0) {
        return FIND_INFO_FAILED;
    }
    //找到音频流
    for (int i = 0; i < pFormatCtx->nb_streams; ++i) {
        if (pFormatCtx->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_AUDIO) {
            audio_stream_idx = i;
            //获取音频解码器 new way
            pCodec = avcodec_find_decoder(pFormatCtx->streams[audio_stream_idx]->codecpar->codec_id);
            pCodecCtx = avcodec_alloc_context3(pCodec);
            avcodec_parameters_to_context(pCodecCtx, pFormatCtx->streams[audio_stream_idx]->codecpar);//AVCodecParameter包含了大量的解码器信息，这里直接复制到AVCodecContext
            break;
        } else if (i == pFormatCtx->nb_streams - 1) {
            return NO_MUSIC_STREAM;
        }
    }
    //获取音频解码器 old way
//    pCodecCtx = pFormatCtx->streams[audio_stream_idx]->codecpar;
//    pCodec = avcodec_find_decoder(pCodecCtx->codec_id);
    //打开音频解码器
    if (avcodec_open2(pCodecCtx, pCodec, nullptr) < 0) {
        return CODEC_OPEN_FAILED;
    }

    packet = (AVPacket *) av_malloc(sizeof(AVPacket));
    frame = av_frame_alloc();
//    av_init_packet(packet);
    //重采样
    swrContext = swr_alloc();
    int length = 0;
    int got_frame;
    out_buffer = (uint8_t *) av_malloc(44100 * 2);
    uint64_t out_ch_layout = AV_CH_LAYOUT_STEREO;//输出的声道布局(立体声)
    AVSampleFormat out_format = AV_SAMPLE_FMT_S16;//输出采样位数 16位
    int out_sample_rate = pCodecCtx->sample_rate;//输出的采样率一定要和输入相同
    swr_alloc_set_opts(swrContext, out_ch_layout, out_format, out_sample_rate, pCodecCtx->channel_layout, pCodecCtx->sample_fmt, pCodecCtx->sample_rate, 0,
                       nullptr);//将PCM源文件的采样格式转换为自己希望的采样格式
    swr_init(swrContext);
    //获取输出声道数
    out_channel_nb = av_get_channel_layout_nb_channels(AV_CH_LAYOUT_STEREO);
    *rate = pCodecCtx->sample_rate;
    *channel = pCodecCtx->channels;
    return SUCCESS;
}

int getPCM(void **pcm, size_t *pcm_size) {
    int got_frame;
    while (av_read_frame(pFormatCtx, packet) >= 0) {//从文件中获取编码数据到Packet
        if (packet->stream_index == audio_stream_idx) {
            //解码
//            avcodec_decode_audio4(pCodecCtx, frame, &got_frame, packet);//old way
            int codecRet = avcodec_send_packet(pCodecCtx, packet);
            if (codecRet < 0 && codecRet != AVERROR(EAGAIN) && codecRet != AVERROR_EOF) {
                return DECODE_FAILED;
            }
            codecRet = avcodec_receive_frame(pCodecCtx, frame);
            if (codecRet < 0 && codecRet != AVERROR_EOF) {
                return DECODE_FAILED;
            }
            LOGE("decode one frame");
            if (codecRet == 0) {
                swr_convert(swrContext, &out_buffer, 44100 * 2, (const uint8_t **) frame->data, frame->nb_samples);
                int size = av_samples_get_buffer_size(nullptr, out_channel_nb, frame->nb_samples, AV_SAMPLE_FMT_S16, 1);
                *pcm = out_buffer;
                *pcm_size = size;
                break;
            }
        }
    }
    return 0;
}

void releaseFFmpeg() {
    av_free(out_buffer);
    av_frame_free(&frame);
    swr_free(&swrContext);
//    avcodec_close(pCodecCtx); old way
    avcodec_free_context(&pCodecCtx);//new way
    avformat_close_input(&pFormatCtx);
}