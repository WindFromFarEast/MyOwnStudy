//
// Created by xiangweixin on 2020-01-14.
//

#ifndef MYOWNSTUDY_EGLHELPER_H
#define MYOWNSTUDY_EGLHELPER_H

#include <EGL/egl.h>
#include <jni.h>
#include "../header/Result.h"

class EglHelper {

public:
    EGLDisplay mEglDisplay;
    EGLContext mEglContext;
    EGLSurface mEglSurface;

public:
    EglHelper();
    ~EglHelper();

    int initEgl(EGLNativeWindowType surface);
    int swapBuffers();

    void destroyEgl();

};


#endif //MYOWNSTUDY_EGLHELPER_H
