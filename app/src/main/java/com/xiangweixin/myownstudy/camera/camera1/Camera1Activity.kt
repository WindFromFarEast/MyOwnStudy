package com.xiangweixin.myownstudy.camera.camera1

import android.hardware.Camera
import android.os.Bundle
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.xiangweixin.myownstudy.R
import com.xiangweixin.myownstudy.util.LogUtil
import kotlinx.android.synthetic.main.activity_camera1.*

class Camera1Activity : AppCompatActivity() {

    companion object {
        const val TAG = "Camera1Activity"
    }

    private lateinit var mCameraHelper: CameraHelper

    private var mTorchOn = false
    private var mSurfaceWidth = 0
    private var mSurfaceHeight = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera1)
        init()
    }

    private fun init() {
        mCameraHelper = CameraHelper(this, surfaceView)
        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
                mSurfaceWidth = width
                mSurfaceHeight = height
            }

            override fun surfaceDestroyed(holder: SurfaceHolder?) {
            }

            override fun surfaceCreated(holder: SurfaceHolder?) {
            }
        })

        btn_takePic.setOnClickListener {
            mCameraHelper.takePicture()
        }
        btn_torch.setOnClickListener {
            if (mTorchOn) {
                mCameraHelper.switchFlashMode(Camera.Parameters.FLASH_MODE_OFF)
            } else {
                mCameraHelper.switchFlashMode(Camera.Parameters.FLASH_MODE_TORCH)
            }
            mTorchOn = !mTorchOn
        }

        btn_focus_mode.setOnClickListener {
            val camParameters = mCameraHelper.getParameters()
            val currentMode = camParameters.focusMode
            LogUtil.d("Camera1Activity", "current focus mode: $currentMode")
            val modes = camParameters.supportedFocusModes
            val index = modes.indexOf(currentMode)
            if (index < modes.size - 1) {
                mCameraHelper.switchFocusMode(modes[index + 1])
            } else if (index == modes.size - 1) {
                mCameraHelper.switchFocusMode(modes[0])
            }
            btn_focus_mode.text = mCameraHelper.getParameters().focusMode
        }

        btn_upExposure.setOnClickListener {
            mCameraHelper.upExposureCompensation()
        }
        btn_downExposure.setOnClickListener {
            mCameraHelper.downExposureCompensation()
        }

        surfaceView.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                LogUtil.d(TAG, "onTouch >>> (${event?.x}, ${event?.y})")
                return true
            }
        })
    }

}