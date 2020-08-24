package com.xiangweixin.myownstudy.camera.camera1

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.RectF
import android.hardware.Camera
import android.os.Environment
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.xiangweixin.myownstudy.camera.CameraCoordinateTransformer
import com.xiangweixin.myownstudy.util.LogUtil
import com.xiangweixin.myownstudy.util.logi
import com.xiangweixin.myownstudy.util.toast
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs

private const val TAG = "CameraHelper"

/**
 * Camera1学习类
 */
class CameraHelper(private val mActivity: Activity, private val mSurfaceView: SurfaceView) : Camera.PreviewCallback {

    companion object {
        const val TAG = "CameraHelper"
    }

    private var mCamera: Camera? = null
    private lateinit var mParameters: Camera.Parameters
    private var mSurfaceHolder: SurfaceHolder = mSurfaceView.holder

    private var mCameraFacing = Camera.CameraInfo.CAMERA_FACING_BACK//Camera方向
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
            try {
                mCamera = Camera.open(cameraFacing)
                initParameters(mCamera!!)
                mCamera?.setPreviewCallback(this)
                val result = PermissionChecker.checkSelfPermission(mActivity, Manifest.permission.CAMERA) == PermissionChecker.PERMISSION_GRANTED
                mActivity.toast("$result")
            } catch (e: Exception) {
                val result = PermissionChecker.checkSelfPermission(mActivity, Manifest.permission.CAMERA) == PermissionChecker.PERMISSION_GRANTED
                mActivity.toast("exception: $result")
            }
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

    fun switchFlashMode(value: String) {
        val supportFlashModes = mParameters.supportedFlashModes
        var isSupport = false
        for (mode in supportFlashModes) {
            if (value.equals(mode)) {
                isSupport = true
                break
            }
        }
        if (isSupport) {
            mParameters.flashMode = value
            mCamera?.parameters = mParameters
            LogUtil.d(TAG, "Switch flash mode: $value")
        } else {
            LogUtil.e(TAG, "Don't support flash mode: $value")
        }
    }

    fun switchFocusMode(value: String) {
        val supportFocusModes = mParameters.supportedFocusModes
        var isSupport = false
        for (mode in supportFocusModes) {
            if (value.equals(mode)) {
                isSupport = true
                break
            }
        }
        if (isSupport) {
            mCamera?.cancelAutoFocus()
            mParameters.focusMode = value
            mCamera?.parameters = mParameters
            //只有部分模式在设置了CameraParamter后还需要调用autoFocus来生效
            if (value.equals(Camera.Parameters.FOCUS_MODE_AUTO) || value.equals(Camera.Parameters.FOCUS_MODE_MACRO)) {
                mCamera?.autoFocus(object : Camera.AutoFocusCallback {
                    override fun onAutoFocus(success: Boolean, camera: Camera?) {
                        if (success) {
                            LogUtil.d(TAG, "onAutoFocus success.");
                        } else {
                            LogUtil.e(TAG, "onAutoFocus error.");
                        }
                    }

                })
            }
            LogUtil.d(TAG, "Switch focus mode: $value")
        } else {
            LogUtil.e(TAG, "Don't focus mode: $value")
        }
    }

    fun focusAt(x: Int, y: Int, lock: Boolean = false) {
        if (mParameters.focusMode != Camera.Parameters.FOCUS_MODE_AUTO
                && mParameters.focusMode != Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
                && mParameters.focusMode != Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO) {
            LogUtil.e(TAG, "Cannot focus at certain point in wrong focus mode: ${mParameters.focusMode}")
            return
        }
        if (mParameters.maxNumFocusAreas <= 0) {
            LogUtil.e(TAG, "Device doesn't support focus at certain point.")
            return
        }
        LogUtil.i(TAG, "Max focus areas num: ${mParameters.maxNumFocusAreas}")


        val focusArea = calculateFocusArea(x, y, mSurfaceView.width, mSurfaceView.height)
        mCamera?.cancelAutoFocus()
        mParameters.focusAreas = listOf(focusArea)
        mParameters.focusMode = Camera.Parameters.FOCUS_MODE_AUTO
        mCamera?.parameters = mParameters

        mCamera?.autoFocus(object : Camera.AutoFocusCallback {
            override fun onAutoFocus(success: Boolean, camera: Camera?) {
                logi(TAG, "focus result: $success")
                if (!lock) {
                    mParameters.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO
                    mCamera?.parameters = mParameters
                }
            }
        })
    }

    fun getMaxZoomIndex(): Int {
        return mParameters.maxZoom
    }

    fun smoothZoom(index: Int) {
        if (!mParameters.isSmoothZoomSupported) {
            return
        }
        if (index > mParameters.maxZoom) {
            return
        }
        mCamera?.startSmoothZoom(index)
        mCamera?.setZoomChangeListener(object : Camera.OnZoomChangeListener {
            override fun onZoomChange(zoomValue: Int, stopped: Boolean, camera: Camera?) {
                logi(TAG, "zoomValue: $zoomValue, stopped: $stopped")
            }
        })
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

    private fun setCameraDisplayOrientation(activity: Activity) {
        val info = Camera.CameraInfo()
        Camera.getCameraInfo(mCameraFacing, info)

        //Display.rotation可以理解为显示方向,若开启了屏幕锁定旋转则恒为0,显示方向的理解:https://www.jianshu.com/p/067889611ae7.即设备相对坐标系y轴到显示自然方向(竖直)的顺时针旋转角度
        val rotation = activity.windowManager.defaultDisplay.rotation

        var screenDegree = when(rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> 0
        }

        //info.orientation的理解:https://www.jianshu.com/p/7174754511df：手机自然竖直放置下，图像在自然显示方向正常显示需要顺时针旋转的角度，大部分手机后置为90
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

    fun getParameters(): Camera.Parameters {
        return mParameters
    }

    fun upExposureCompensation() {
        val minIndex = mParameters.minExposureCompensation
        val maxIndex = mParameters.maxExposureCompensation
        if (minIndex == 0 && maxIndex == 0) {
            logi(TAG, "不支持曝光补偿")
            return
        }
        val step = mParameters.exposureCompensationStep
        logi(TAG, "min exposure compensation index: $minIndex, max: $maxIndex, step: $step, current value: ${mParameters.exposureCompensation}")
        mParameters.exposureCompensation = if (mParameters.exposureCompensation < maxIndex) mParameters.exposureCompensation + 1 else mParameters.exposureCompensation
        mCamera!!.parameters = mParameters
    }

    fun downExposureCompensation() {
        val minIndex = mParameters.minExposureCompensation
        val maxIndex = mParameters.maxExposureCompensation
        if (minIndex == 0 && maxIndex == 0) {
            logi(TAG, "不支持曝光补偿")
            return
        }
        val step = mParameters.exposureCompensationStep
        logi(TAG, "min exposure compensation index: $minIndex, max: $maxIndex, step: $step")
        logi(TAG, "min exposure compensation index: $minIndex, max: $maxIndex, step: $step, current value: ${mParameters.exposureCompensation}")
        mParameters.exposureCompensation = if (mParameters.exposureCompensation > minIndex) mParameters.exposureCompensation - 1 else mParameters.exposureCompensation
        mCamera!!.parameters = mParameters
        mParameters.autoExposureLock
    }

    private fun calculateFocusArea(x: Int, y: Int, viewWidth: Int, viewHeight: Int, range: Int = 100): Camera.Area {
        val cameraCoordinateTransformer = CameraCoordinateTransformer(mCameraFacing == Camera.CameraInfo.CAMERA_FACING_FRONT,
                if (mCameraFacing == Camera.CameraInfo.CAMERA_FACING_FRONT) 270 else 90, RectF(0F, 0F, viewWidth.toFloat(), viewHeight.toFloat()))
        val touchRectF = RectF((x - range).toFloat(), (y + range).toFloat(), (x + range).toFloat(), (y - range).toFloat())
        val cameraRectF = cameraCoordinateTransformer.toCameraSpace(touchRectF)
        val cameraRect = Rect(Math.round(cameraRectF.left), Math.round(cameraRectF.top), Math.round(cameraRectF.right), Math.round(cameraRectF.bottom))

        logi(TAG, "Before convert: (${touchRectF.left}, ${touchRectF.top}. ${touchRectF.right}. ${touchRectF.bottom})")
        logi(TAG, "After convert: (${cameraRect.left}, ${cameraRect.top}. ${cameraRect.right}. ${cameraRect.bottom})")
        return Camera.Area(cameraRect, 1000)
    }

}