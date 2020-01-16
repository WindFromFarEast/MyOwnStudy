package com.xiangweixin.myownstudy.ffmpeg.onlyPlayVideo;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.xiangweixin.myownstudy.R;

public class OnlyPlayVideoActivity extends AppCompatActivity {

    private OnlyPlayVideoView onlyPlayVideoView;
    private Button btnPlay;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_only_play_video);
        btnPlay = findViewById(R.id.btn_play);
        onlyPlayVideoView = findViewById(R.id.only_play_video_view);
        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onlyPlayVideoView.play("sdcard/dance.mp4");
            }
        });
    }
}
