package com.xiangweixin.myownstudy.opengl;

import android.opengl.GLSurfaceView;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.xiangweixin.myownstudy.R;

public class ImageActivity extends AppCompatActivity {

    private GLSurfaceView mImage;
    private ImageSurfaceViewRender mRender;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);
        mImage = findViewById(R.id.image);
        mImage.setEGLContextClientVersion(2);
        mRender = new ImageSurfaceViewRender();
        mImage.setRenderer(mRender);
    }
}
