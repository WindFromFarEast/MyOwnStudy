//
// Created by xiangweixin on 2020-02-29.
//

#ifndef MYOWNSTUDY_RECORDEROPENGLPROXY_H
#define MYOWNSTUDY_RECORDEROPENGLPROXY_H


#include <android/native_window.h>
#include <sys/types.h>
#include <pthread.h>
#include "EGL/egl.h"
#include "android/log.h"

void *render_thread(void *args);

class RecorderOpenGLProxy {
    friend void *render_thread(void *args);
public:
    RecorderOpenGLProxy();
    ~RecorderOpenGLProxy();

    int initEGL(ANativeWindow *window);
    int swapBuffers();
    void destroyEGL();

private:
    pthread_t m_threadId;//GL线程tid
    pthread_mutex_t m_mutex;
    pthread_cond_t m_cond;
    ANativeWindow *m_window = nullptr;
    EGLDisplay m_display = EGL_NO_DISPLAY;
    EGLContext m_context = EGL_NO_CONTEXT;
    EGLSurface m_surface = EGL_NO_SURFACE;

    bool stopRender = false;
};


#endif //MYOWNSTUDY_RECORDEROPENGLPROXY_H
