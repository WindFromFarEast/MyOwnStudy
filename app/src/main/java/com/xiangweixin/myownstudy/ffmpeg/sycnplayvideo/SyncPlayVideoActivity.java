package com.xiangweixin.myownstudy.ffmpeg.sycnplayvideo;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.xiangweixin.myownstudy.R;
import com.xiangweixin.myownstudy.util.LogUtil;

/**
 * 音视频同步播放
 */
public class SyncPlayVideoActivity extends AppCompatActivity {

    private EditText editText;

    private static final String TAG = "SyncPlay";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sync_play);
        editText = findViewById(R.id.et_video_path);
//        findViewById(R.id.btn_play).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                String path = editText.getText().toString();
//                int result = play(path);
//                if (result != 0) {
//                    LogUtil.e(TAG, "play failed.");
//                }
//            }
//        });
    }

//    private int play(String videoPath) {
//        if (TextUtils.isEmpty(videoPath)) {
//            return -1;
//        }
//        return nativePlay(videoPath);
//    }

//    public native int nativePlay(String videoPath);

}
