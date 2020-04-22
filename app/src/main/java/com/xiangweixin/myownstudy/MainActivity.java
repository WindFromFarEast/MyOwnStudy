package com.xiangweixin.myownstudy;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.xiangweixin.myownstudy.audiorecord.AudioRecordActivity;
import com.xiangweixin.myownstudy.camera.camera1.Camera1Activity;
import com.xiangweixin.myownstudy.camera.camera2.Camera2Activity;
import com.xiangweixin.myownstudy.ffmpeg.onlyPlayVideo.OnlyPlayVideoActivity;
import com.xiangweixin.myownstudy.opengl.BlendActivity;
import com.xiangweixin.myownstudy.opengl.GLSurfaceViewActivity;
import com.xiangweixin.myownstudy.opengl.OpenGLActivity;

public class MainActivity extends AppCompatActivity {

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
        System.loadLibrary("avcodec-57");
        System.loadLibrary("avfilter-6");
        System.loadLibrary("avformat-57");
        System.loadLibrary("avutil-55");
        System.loadLibrary("swresample-2");
        System.loadLibrary("swscale-4");
        System.loadLibrary("postproc-54");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestAllPermission();
        findViewById(R.id.btn_toCamera2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, Camera2Activity.class));
            }
        });
        findViewById(R.id.btn_toCamera1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, Camera1Activity.class));
            }
        });
        findViewById(R.id.btn_toOnlyPlayVideo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, OnlyPlayVideoActivity.class));
            }
        });
        findViewById(R.id.btn_toUtil).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, UtilActivity.class));
            }
        });
        findViewById(R.id.btn_toAudioRecord).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, AudioRecordActivity.class));
            }
        });
        findViewById(R.id.btn_toOpenGL).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, OpenGLActivity.class));
            }
        });
        findViewById(R.id.btn_toFBO).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, GLSurfaceViewActivity.class));
            }
        });
        findViewById(R.id.btn_toFirstCamera).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, FirstCameraActivity.class));
            }
        });
        findViewById(R.id.btn_toBlend).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, BlendActivity.class));
            }
        });
    }

    private void requestAllPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                finish();
            }
        }
    }
}
