package com.xiangweixin.myownstudy.camera.camera2new;

import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Size;
import android.view.TextureView;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.xiangweixin.myownstudy.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

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

        findViewById(R.id.btn_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCamera.close();
            }
        });

        findViewById(R.id.btn_open).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCamera.open();
                mCamera.startPreview();
            }
        });

        findViewById(R.id.btn_capture).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCamera.takePicture(new Camera2.PictureCallback() {
                    @Override
                    public void onSuccess(byte[] data, int width, int height) {
                        if (data != null && data.length > 0) {
                            final File file = new File(Environment.getExternalStorageDirectory() + "/DCIM/"+ System.currentTimeMillis() +".jpg");
                            try {
                                FileOutputStream fos = new FileOutputStream(file);
                                fos.write(data, 0, data.length);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(NewCamera2Activity.this, "保存在" + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    @Override
                    public void onFail(Exception e) {

                    }
                });
            }
        });

        findViewById(R.id.btn_exposure_compensation_up).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCamera.upExposureCompensation();
            }
        });

        findViewById(R.id.btn_exposure_compensation_down).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCamera.downExposureCompensation();
            }
        });
    }

    private void initCamera() {
        mCamera = new Camera2(getApplicationContext(), mTextureView);
        mCamera.setSurfaceTextureListener(new Camera2.Camera2SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                Size previewSize = new Size(width, height);
                Size pictureSize = new Size(720, 1280);
                mCamera.init(previewSize, pictureSize, true);
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
