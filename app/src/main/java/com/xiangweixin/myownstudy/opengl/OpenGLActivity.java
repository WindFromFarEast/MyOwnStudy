package com.xiangweixin.myownstudy.opengl;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.xiangweixin.myownstudy.R;

public class OpenGLActivity extends AppCompatActivity {

    private CameraGLSurfaceView cameraGLSurfaceView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_opengl);
        cameraGLSurfaceView = findViewById(R.id.camera_surfaceview);
    }
}
