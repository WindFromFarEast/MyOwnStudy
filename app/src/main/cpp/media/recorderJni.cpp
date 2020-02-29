//
// Created by xiangweixin on 2020-02-29.
//
#include <jni.h>
#include <android/native_window.h>
#include "RecorderOpenGLProxy.h"
#include "Result.h"
#include "android/native_window.h"
#include "android/native_window_jni.h"
#include "GLES2/gl2.h"

extern "C"
JNIEXPORT jlong JNICALL
Java_com_xiangweixin_myownstudy_mediaallproj_XWXRecorder_nativeCreate(JNIEnv *env,
                                                                      jobject instance) {

    RecorderOpenGLProxy *recorderOpenGLProxy = new RecorderOpenGLProxy;
    return reinterpret_cast<long>(recorderOpenGLProxy);

}

extern "C"
JNIEXPORT jint JNICALL
Java_com_xiangweixin_myownstudy_mediaallproj_XWXRecorder_nativeInitOpenGL(JNIEnv *env,
                                                                          jobject instance,
                                                                          jlong handler,
                                                                          jobject surface) {

    RecorderOpenGLProxy *recorderOpenGLProxy = reinterpret_cast<RecorderOpenGLProxy *>(handler);
    if (recorderOpenGLProxy == nullptr) {
        return RECORDER_INVALID_HANDLER;
    }
    if (surface == nullptr) {
        return RECORDER_INVALID_SURFACE;
    }
    ANativeWindow *window = ANativeWindow_fromSurface(env, surface);
    int ret = recorderOpenGLProxy->initEGL(window);
}