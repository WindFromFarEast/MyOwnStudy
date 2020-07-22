package com.xiangweixin.myownstudy.camera.camera2new;

import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Bundle;
import android.util.Size;
import android.view.TextureView;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.xiangweixin.myownstudy.R;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class NewCamera2Activity extends AppCompatActivity {

    private TextureView mTextureView;
    private Camera2 mCamera;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_camera2);

        initView();

        initCamera();
    }

    private void initView() {
        mTextureView = findViewById(R.id.textureView);
    }

    private void initCamera() {
        mCamera = new Camera2(getApplicationContext(), mTextureView);
        mCamera.setSurfaceTextureListener(new Camera2.Camera2SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                Size previewSize = new Size(width, height);
                mCamera.init(previewSize, false);
                mCamera.open();
                mCamera.startPreview();
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {

            }
        });
    }
}
