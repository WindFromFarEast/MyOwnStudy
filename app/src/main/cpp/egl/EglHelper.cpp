//
// Created by xiangweixin on 2020-01-14.
//

#include "EglHelper.h"

int EglHelper::initEgl(EGLNativeWindowType window) {
    //获取默认显示设备
    mEglDisplay = eglGetDisplay(EGL_DEFAULT_DISPLAY);
    if (mEglDisplay == EGL_NO_DISPLAY) {
        return EGL_NO_DISPLAY_ERROR;
    }
    return SUCCESS;
}
