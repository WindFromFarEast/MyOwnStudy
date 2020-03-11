package com.xiangweixin.myownstudy.mediaallproj;

public interface Callback {

    interface OnOpenGLCallback {
        int onGLCreated();
        void onGLRunning();
        void onGLDestroyed();
    }

}
