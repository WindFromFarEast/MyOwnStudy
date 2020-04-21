package com.xiangweixin.myownstudy;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.xiangweixin.myownstudy.mediaallproj.XWXCamera;
import com.xiangweixin.myownstudy.mediaallproj.XWXCameraSetting;
import com.xiangweixin.myownstudy.util.LogUtil;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class FirstCameraActivity extends AppCompatActivity {

    private SurfaceView mSurfaceView;
    private XWXCamera mCamera;
    private Button mButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first_camera);
        mSurfaceView = findViewById(R.id.firstCamSurfaceView);
        mButton = findViewById(R.id.btn_leap);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(FirstCameraActivity.this, SecondCameraActivity.class));
            }
        });
        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                mCamera = new XWXCamera();
                mCamera.open(true);
                XWXCameraSetting.Builder builder = new XWXCameraSetting.Builder();
                mCamera.config(builder.setFocusMode(XWXCameraSetting.FocusMode.AUTO).setPreviewSize(1280, 720).setPictureSize(1280, 720).build());
                mCamera.startPreview(holder);
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
//                mCamera.onlyClose();
                LogUtil.e("FirstCamera", "camera close.");
            }
        });
    }
}
