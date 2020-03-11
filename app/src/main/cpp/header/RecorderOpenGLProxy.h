//
// Created by xiangweixin on 2020-02-29.
//

#ifndef MYOWNSTUDY_RECORDEROPENGLPROXY_H
#define MYOWNSTUDY_RECORDEROPENGLPROXY_H


#include <android/native_window.h>
#include <sys/types.h>
#include <pthread.h>
#include <jni.h>
#include <string>
#include "EGL/egl.h"
#include "android/log.h"
#include "functional"
#include <GLES2/gl2.h>

void *render_thread(void *args);

typedef struct RecordEnv {
    jobject mObj;
} RecorderEnv;

class RecorderOpenGLProxy {
    friend void *render_thread(void *args);
public:
    RecorderOpenGLProxy();
    ~RecorderOpenGLProxy();

    int initEGL(ANativeWindow *window);
    int swapBuffers();
    void destroyEGL();

    int initRenderEnv();//初始化Shader、Program等
    void render();

    void setOnOpenGLCreateCallback(std::function<int(void*)> func) { m_onOpenGLCreateCallback = func; };
    void setOnOpenGLRunningCallback(std::function<void(void*)> func) { m_onOpenGLRunningCallback = func; };
    void setOnOpenGLDestroyCallback(std::function<void(void*)> func) { m_onOpenGLDestroyCallback = func; };

public:
    int srcTexId = -1;

private:
    std::function<int(void*)> m_onOpenGLCreateCallback;
    std::function<void(void*)> m_onOpenGLDestroyCallback;
    std::function<void(void*)> m_onOpenGLRunningCallback;

private:
    pthread_t m_threadId;//GL线程tid
    pthread_mutex_t m_mutex;
    pthread_cond_t m_cond;
    ANativeWindow *m_window = nullptr;
    EGLDisplay m_display = EGL_NO_DISPLAY;
    EGLContext m_context = EGL_NO_CONTEXT;
    EGLSurface m_surface = EGL_NO_SURFACE;
    RecorderEnv recorderEnv;

    bool stopRender = false;

    int m_program;
};


#endif //MYOWNSTUDY_RECORDEROPENGLPROXY_H
