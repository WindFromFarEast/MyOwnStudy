////
//// Created by xiangweixin on 2020-02-18.
////
//
//#ifndef MYOWNSTUDY_FFMPEGVIDEO_H
//#define MYOWNSTUDY_FFMPEGVIDEO_H
//
//#include <queue>
//#include<vector>
//#include <SLES/OpenSLES_Android.h>
//#include "FFmpegMusic.h"
//
//extern "C" {
//#include <unistd.h>
//#include <libavcodec/avcodec.h>
//#include <pthread.h>
//#include <libswresample/swresample.h>
//#include <libswscale/swscale.h>
//#include <libavformat/avformat.h>
//#include <libavutil/imgutils.h>
//#include <libavutil/time.h>
//}
//
//class FFmpegVideo {
//public:
//    FFmpegVideo();
//    ~FFmpegVideo();
//
//    void setAVCodecContext(AVCodecContext *avCodecContext);
//
//    int put(AVPacket *avPacket);
//    int get(AVPacket *avPacket);
//
//    void play();
//
//    double synchronized(AVFrame *frame, double play);
//
//    void setFFmpegMusic(FFmpegMusic *fFmpegMusic);
//
//    void setPlayCallback(void (*call)(AVFrame *frame));
//
//public:
//    int index;//流索引
//
//    pthread_t  playId;//处理线程
//    std::vector<AVPacket*> queue;
//
//    AVCodecContext *codecContext;
//
//    SwsContext *swsContext;
//
//    pthread_mutex_t mutex;
//
//    pthread_cond_t cond;
//
//    AVRational time_base;
//
//    double clock;
//
//};
//
//
//#endif //MYOWNSTUDY_FFMPEGVIDEO_H
