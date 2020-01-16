package com.xiangweixin.myownstudy.ffmpeg.onlyPlayVideo;

import android.content.Context;
import android.graphics.PixelFormat;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * 用于播放一段Video(不含Audio)的View
 */
public class OnlyPlayVideoView extends SurfaceView {

    private volatile boolean isPlay = false;

    public OnlyPlayVideoView(Context context) {
        this(context, null);
    }

    public OnlyPlayVideoView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public OnlyPlayVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        SurfaceHolder holder = getHolder();
        holder.setFormat(PixelFormat.RGBA_8888);
    }

    public void play(final String videoPath) {
        if (isPlay) {
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                render(videoPath, OnlyPlayVideoView.this.getHolder().getSurface());
            }
        }).start();
    }

    public native void render(String videoPath, Surface outputSurface);

}
