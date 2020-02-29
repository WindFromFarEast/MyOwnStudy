//
// Created by xiangweixin on 2020-02-29.
//
#include "RecorderOpenGLProxy.h"
#include <jni.h>
#include "Result.h"

RecorderOpenGLProxy::RecorderOpenGLProxy() {
    pthread_mutex_init(&m_mutex, nullptr);
    pthread_cond_init(&m_cond, nullptr);
}

RecorderOpenGLProxy::~RecorderOpenGLProxy() {
    pthread_mutex_destroy(&m_mutex);
    pthread_cond_destroy(&m_cond);
}

int RecorderOpenGLProxy::initEGL(ANativeWindow *window) {
    m_window = window;
    pthread_create(&m_threadId, nullptr, render_thread, this);
}

int RecorderOpenGLProxy::swapBuffers() {
    if (m_display != EGL_NO_DISPLAY && m_surface != EGL_NO_SURFACE && eglSwapBuffers(m_display, m_surface)) {
        return SUCCESS;
    }
    return RECORDER_SWAPBUFFERS_ERROR;
}

void RecorderOpenGLProxy::destroyEGL() {
    if (m_display != EGL_NO_DISPLAY) {
        eglMakeCurrent(m_display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
        if (m_surface != EGL_NO_SURFACE) {
            eglDestroySurface(m_display, m_surface);
            m_surface = EGL_NO_SURFACE;
        }
        if (m_context != EGL_NO_CONTEXT) {
            eglDestroyContext(m_display, m_context);
            m_context = EGL_NO_CONTEXT;
        }
        if (m_display != EGL_NO_DISPLAY) {
            eglTerminate(m_display);
            m_display = EGL_NO_DISPLAY;
        }
    }
}

void *render_thread(void *args) {
    RecorderOpenGLProxy *proxy = reinterpret_cast<RecorderOpenGLProxy *>(args);
    if (!proxy) {
        return nullptr;
    }

    //↓ init egl environment.
    proxy->m_display = eglGetDisplay(EGL_DEFAULT_DISPLAY);
    if (proxy->m_display == EGL_NO_DISPLAY) {
        return nullptr;
    }

    EGLint *version = new EGLint[2];
    if (!eglInitialize(proxy->m_display, &version[0], &version[1])) {
        return nullptr;
    }

    const EGLint attrib_config_list[] = {
            EGL_RED_SIZE, 8,
            EGL_GREEN_SIZE, 8,
            EGL_BLUE_SIZE, 8,
            EGL_ALPHA_SIZE, 8,
            EGL_DEPTH_SIZE, 8,
            EGL_STENCIL_SIZE, 8,
            EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
            EGL_SURFACE_TYPE, EGL_WINDOW_BIT,
            EGL_NONE
    };
    EGLint num_config;
    EGLConfig eglConfig;
    if (!eglChooseConfig(proxy->m_display, attrib_config_list, &eglConfig, 1, &num_config)) {
        return nullptr;
    }

    const EGLint attrib_ctx_list[] = {
            EGL_CONTEXT_CLIENT_VERSION, 2,
            EGL_NONE
    };
    proxy->m_context = eglCreateContext(proxy->m_display, eglConfig, nullptr, attrib_ctx_list);
    if (proxy->m_context == EGL_NO_CONTEXT) {
        return nullptr;
    }

    proxy->m_surface = eglCreateWindowSurface(proxy->m_display, eglConfig, proxy->m_window, nullptr);
    if (proxy->m_surface == EGL_NO_SURFACE) {
        return nullptr;
    }

    if (!eglMakeCurrent(proxy->m_display, proxy->m_surface, proxy->m_surface, proxy->m_context)) {
        return nullptr;
    }
    //↑ init egl done.

    //render loop
    while (!proxy->stopRender) {

    }

    proxy->destroyEGL();
}