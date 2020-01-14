package com.xiangweixin.myownstudy.camera.camera1

import android.app.Activity
import android.graphics.ImageFormat
import android.hardware.Camera
import android.os.Environment
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.xiangweixin.myownstudy.util.logi
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs

private const val TAG = "CameraHelper"

/**
 * Camera1学习类
 */
class CameraHelper(private val mActivity: Activity, private val mSurfaceView: SurfaceView) : Camera.PreviewCallback {

    private var mCamera: Camera? = null
    private lateinit var mParameters: Camera.Parameters
    private var mSurfaceHolder: SurfaceHolder = mSurfaceView.holder

    private var mCameraFacing = Camera.CameraInfo.CAMERA_FACING_BACK //Camera方向
    private var mDisplayOrientation = 0 //预览旋转角度

    private var picWidth = 720
    private var picHeight = 1280

    init {
        init()
    }

    private fun init() {
        mSurfaceHolder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
            }

            override fun surfaceDestroyed(holder: SurfaceHolder?) {
                releaseCamera()
            }

            override fun surfaceCreated(holder: SurfaceHolder?) {
                if (mCamera == null) {
                    openCamera(mCameraFacing)
                }
                startPreview()
            }
        })
    }

    private fun openCamera(cameraFacing: Int = Camera.CameraInfo.CAMERA_FACING_BACK) {
        //判断手机是否支持前置/后置摄像头
        val supportCameraFacing = ifSupportCameraFacing(cameraFacing)
        if (supportCameraFacing) {
            mCamera = Camera.open(cameraFacing)
            initParameters(mCamera!!)
            mCamera?.setPreviewCallback(this)
        }
    }

    fun startPreview() {
        mCamera?.let {
            it.setPreviewDisplay(mSurfaceHolder)
            setCameraDisplayOrientation(mActivity)
            it.startPreview()
        }
    }

    fun takePicture() {
        mCamera?.let {
            it.takePicture(null, null, Camera.PictureCallback { data, _ ->
                it.startPreview()
                savePic(data)
            })
        }
    }

    private fun savePic(data: ByteArray) {
        val picFile = File("${Environment.getExternalStorageDirectory()}/DCIM/myPicture.jpg")

        val fos = FileOutputStream(picFile)
        fos.write(data, 0, data.size)
        fos.close()

        logi(TAG, "照片保存成功，路径: ${picFile.absolutePath}")
    }

    private fun releaseCamera() {
        mCamera?.let {
            it.stopPreview()
            it.setPreviewCallback(null)
            it.release()
        }
    }

    /**
     * 判断设备是否支持朝向为cameraFacing的Camera
     */
    private fun ifSupportCameraFacing(cameraFacing: Int): Boolean {
        val info = Camera.CameraInfo()
        for (i in 0 until Camera.getNumberOfCameras()) {
            Camera.getCameraInfo(i, info)
            if (info.facing == cameraFacing) {
                return true
            }
        }
        return false
    }

    private fun initParameters(camera: Camera) {
        mParameters = camera.parameters

        mParameters.previewFormat = ImageFormat.NV21 //预览格式

        //获取与指定宽高相等或最接近的尺寸 预览/拍照
        val bestPreviewSize = getCloselySize(mSurfaceView.width, mSurfaceView.height, mParameters.supportedPreviewSizes)
        bestPreviewSize?.let {
            mParameters.setPreviewSize(bestPreviewSize.width, bestPreviewSize.height)
        }
        val bestPicSize = getCloselySize(mSurfaceView.width, mSurfaceView.height, mParameters.supportedPreviewSizes)
        bestPicSize?.let {
            mParameters.setPictureSize(bestPicSize.width, bestPicSize.height)
        }

        //对焦模式
        if (isSupportFocus(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            mParameters.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
        }

        camera.parameters = mParameters
    }

    /**
     * 找到和目标宽高比(surface)最接近的预览尺寸
     * @param targetWidth 需要被进行对比的原宽
     * @param targetHeight 需要被进行对比的原高
     * @param previewSizeList 需要对比的预览尺寸列表
     */
    private fun getCloselySize(targetWidth: Int, targetHeight: Int, previewSizeList: List<Camera.Size>): Camera.Size? {
        //互换宽高
        val reqTmpWidth: Int = targetHeight
        val reqTmpHeight: Int = targetWidth
//        // 当屏幕为垂直的时候需要把宽高值进行调换，保证宽大于高
//        if (isPortrait) {
//            reqTmpWidth = targetHeight
//            reqTmpHeight = targetWidth
//        } else {
//            reqTmpWidth = targetWidth
//            reqTmpHeight = targetHeight
//        }

        //先查找previewList中是否存在与surfaceview相同宽高的尺寸
        for (size in previewSizeList) {
            logi(TAG, "系统支持的尺寸: ${size.width} * ${size.height}")
            if (size.width == reqTmpWidth && size.height == reqTmpHeight) {
                return size
            }
        }

        // 得到与传入的宽高比最接近的size
        val reqRatio = reqTmpWidth.toFloat() / reqTmpHeight
        var curRatio: Float
        var finalSize: Camera.Size? = null
        var minDeltaRatio = Float.MAX_VALUE
        for (size in previewSizeList) {
            curRatio = size.width.toFloat() / size.height
            val deltaRatio = abs(reqRatio - curRatio)
            if (deltaRatio < minDeltaRatio) {
                minDeltaRatio = deltaRatio
                finalSize = size
            }
        }

        logi(TAG, "目标尺寸: $targetWidth * $targetHeight")
        logi(TAG, "最终尺寸: ${finalSize?.width} * ${finalSize?.height}")

        return finalSize
    }

    /**
     * 判断是否支持某个对焦模式
     */
    private fun isSupportFocus(focusMode: String): Boolean {
        var ret = false
        val focusModeList = mParameters.supportedFocusModes
        for (mode in focusModeList) {
            logi(TAG, "相机支持的对焦模式: $mode")
            if (mode == focusMode) {
                ret = true
            }
        }
        return ret
    }

    private fun setCameraDisplayOrientation(activity: Activity) {
        val info = Camera.CameraInfo()
        Camera.getCameraInfo(mCameraFacing, info)

        val rotation = activity.windowManager.defaultDisplay.rotation

        var screenDegree = when(rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> 0
        }

        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            mDisplayOrientation = (info.orientation + screenDegree) % 360
            mDisplayOrientation = (360 - mDisplayOrientation) % 360          // compensate the mirror
        } else {
            mDisplayOrientation = (info.orientation - screenDegree + 360) % 360
        }
        mCamera?.setDisplayOrientation(mDisplayOrientation)

        logi(TAG, "屏幕的旋转角度 : $rotation")
        logi(TAG, "setDisplayOrientation(result) : $mDisplayOrientation")
    }

    override fun onPreviewFrame(data: ByteArray?, camera: Camera?) {

    }

}