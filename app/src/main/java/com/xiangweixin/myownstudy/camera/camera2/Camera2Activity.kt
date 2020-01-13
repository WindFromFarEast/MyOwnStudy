package com.xiangweixin.myownstudy.camera.camera2

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.xiangweixin.myownstudy.R
import kotlinx.android.synthetic.main.activity_camera2.*

class Camera2Activity : AppCompatActivity() {

    private lateinit var mCamera2Helper: Camera2Helper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera2)
        initCamera()
        initView()
    }

    private fun initCamera() {
        mCamera2Helper = Camera2Helper(this, textureView)
    }

    private fun initView() {
        btn_exchange.setOnClickListener {
            mCamera2Helper.exchangeCamera()
        }
        btn_takePic.setOnClickListener {
            mCamera2Helper.takePicture()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mCamera2Helper.releaseCamera()
        mCamera2Helper.releaseThread()
    }

}