////
//// Created by xiangweixin on 2020-02-18.
////
//
//#include "header/FFmpegVideo.h"
//
//FFmpegVideo::FFmpegVideo() {
//    pthread_mutex_init(&mutex, nullptr);
//    pthread_cond_init(&cond, nullptr);
//}
//
//FFmpegVideo::~FFmpegVideo() {
//    pthread_cond_destroy(&cond);
//    pthread_mutex_destroy(&mutex);
//}
//
//void FFmpegVideo::setAVCodecContext(AVCodecContext *avCodecContext) {
//    codecContext = avCodecContext;
//}
//
//int FFmpegVideo::put(AVPacket *avPacket) {
//
//}