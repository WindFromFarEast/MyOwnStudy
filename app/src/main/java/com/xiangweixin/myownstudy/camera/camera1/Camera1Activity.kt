package com.xiangweixin.myownstudy.camera.camera1

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.xiangweixin.myownstudy.R
import kotlinx.android.synthetic.main.activity_camera1.*

class Camera1Activity : AppCompatActivity() {

    private lateinit var mCameraHelper: CameraHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera1)
        initCamera()
        btn_takePic.setOnClickListener {
            mCameraHelper.takePicture()
        }
    }

    private fun initCamera() {
        mCameraHelper = CameraHelper(this, surfaceView)
    }

}