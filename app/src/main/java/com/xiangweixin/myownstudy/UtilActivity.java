package com.xiangweixin.myownstudy;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.xiangweixin.myownstudy.opensl.OpenSLStudyOne;

public class UtilActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_util);
        findViewById(R.id.btn_playMusic).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OpenSLStudyOne.playPCM("sdcard/music.mp3");
            }
        });
    }
}
