package com.xiangweixin.myownstudy;

import android.os.Build;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.xiangweixin.myownstudy.mediaallproj.XWXCamera;
import com.xiangweixin.myownstudy.mediaallproj.XWXCameraSetting;
import com.xiangweixin.myownstudy.util.LogUtil;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class SecondCameraActivity extends AppCompatActivity {

    private SurfaceView mSurfaceView;
    private XWXCamera mCamera;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second_camera);
        mSurfaceView = findViewById(R.id.secondCamSurfaceView);
        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                mCamera = new XWXCamera();
                LogUtil.e("FirstCamera", "camera open.");
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

            }
        });
    }
}
