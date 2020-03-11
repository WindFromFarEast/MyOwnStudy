package com.xiangweixin.myownstudy;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.xiangweixin.myownstudy.ffmpeg.encode.FFmpeg;
import com.xiangweixin.myownstudy.opensl.OpenSLStudyOne;

public class UtilActivity extends AppCompatActivity {

    private EditText encodeEdit;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_util);
        encodeEdit = findViewById(R.id.et_encode);
        findViewById(R.id.btn_playMusic).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OpenSLStudyOne.playPCM("sdcard/music.mp3");
            }
        });
        findViewById(R.id.btn_encodeToH264).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String yuvPath = encodeEdit.getText().toString();
                if (TextUtils.isEmpty(yuvPath)) {
                    return;
                }
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final int ret = FFmpeg.encode(yuvPath, "sdcard/yuvToH264.h264", 4000000, 25, 720, 1280);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(UtilActivity.this, "Encode ret: " + ret, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }).start();
            }
        });
    }
}
