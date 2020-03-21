package com.xiangweixin.myownstudy.opengl;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;

import com.xiangweixin.myownstudy.mediaallproj.XWXCameraSetting;
import com.xiangweixin.myownstudy.mediaallproj.XWXResult;
import com.xiangweixin.myownstudy.util.LogUtil;

import java.io.IOException;
import java.util.List;

public class CameraUtil {

    //new->open->config->preview->close->open->...请按这种顺序调用,否则可能会有问题

    private static final String TAG = "XWXCamera";

    private Camera mCamera;
    private int mBackCamId = -1;
    private int mBackCamOrientation;
    private int mFrontCamId = -1;
    private int mFrontCamOrientation;
    private int mUsableCameraNum;

    public CameraUtil() {

    }

    public int open(boolean useBackCamera) {
        int prepareRet = prepare();
        if (prepareRet != XWXResult.OK) {
            return prepareRet;
        }
        if (mUsableCameraNum <= 0) {
            return XWXResult.NO_USABLE_CAMERA;
        }
        try {
            if (useBackCamera && mBackCamId >= 0) {
                mCamera = Camera.open(mBackCamId);
                return XWXResult.OK;
            } else if (!useBackCamera && mFrontCamId >= 0) {
                mCamera = Camera.open(mFrontCamId);
                return XWXResult.OK;
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
        LogUtil.e(TAG, "open camera failed. useBackCamera: " + useBackCamera + ", backCamId: " + mBackCamId + ", frontCamId: " + mFrontCamId);
        return XWXResult.NO_USABLE_CAMERA;
    }

    public void stopPreview() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    public int config(XWXCameraSetting setting) {
        if (mCamera == null) {
            return XWXResult.NO_USABLE_CAMERA;
        }

        Camera.Parameters parameters = mCamera.getParameters();
        Camera.Size bestPreviewSize = getBestSize(setting.getPreviewSize().getWidth(), setting.getPreviewSize().getHeight(), parameters.getSupportedPreviewSizes());
        LogUtil.i(TAG, "find best preview size: width->" + bestPreviewSize.width + ", height: " + bestPreviewSize.height);
        parameters.setPreviewSize(bestPreviewSize.width, bestPreviewSize.height);

//        Camera.Size bestPictureSize = getBestSize(setting.getPictureSize().getWidth(), setting.getPictureSize().getHeight(), parameters.getSupportedPictureSizes());
//        LogUtil.i(TAG, "find best picture size: width->" + bestPictureSize.width + ", height: " + bestPictureSize.height);
//        parameters.setPictureSize(bestPictureSize.width, bestPictureSize.height);

        //懒得写了，直接默认auto focus mode.
        if (supportFocusMode(Camera.Parameters.FOCUS_MODE_AUTO, parameters)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        }
        parameters.set("orientation", "potrait");
        mCamera.setDisplayOrientation(90);
        mCamera.setParameters(parameters);
        return XWXResult.OK;
    }

    public int close() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
        }
        return XWXResult.OK;
    }

    public void startPreview(SurfaceTexture surfaceTexture) {
        if (mCamera != null) {
            try {
                mCamera.setPreviewTexture(surfaceTexture);
                mCamera.startPreview();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private int prepare() {
        mUsableCameraNum = Camera.getNumberOfCameras();
        LogUtil.i(TAG, "Find usable camera num: " + mUsableCameraNum);
        if (mUsableCameraNum <= 0) {
            return XWXResult.NO_USABLE_CAMERA;
        }
        for (int i = 0; i < mUsableCameraNum; i++) {
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                mBackCamId = i;
                mBackCamOrientation = cameraInfo.orientation;
            } else if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                mFrontCamId = i;
                mFrontCamOrientation = cameraInfo.orientation;
            }
        }
        return XWXResult.OK;
    }

    private Camera.Size getBestSize(int targetWidth, int targetHeight, List<Camera.Size> sizeList) {
        Camera.Size bestSize = null;
        float targetRatio = targetWidth * 1f / targetHeight;
        float minDiff = targetRatio;

        for (Camera.Size size : sizeList) {
            float supportRatio = size.width * 1f / size.height;
            LogUtil.i(TAG, "System Camera Support Size: width->" + size.width + ", height->" + size.height + ", ratio->" + supportRatio);
            if (size.width == targetWidth && size.height == targetHeight) {
                bestSize = size;
                break;
            }
            if (Math.abs(supportRatio - targetRatio) < minDiff) {
                minDiff = Math.abs(supportRatio - targetRatio);
                bestSize = size;
            }
        }

        if (bestSize == null) {
            LogUtil.i(TAG, "No best size.");
            return null;
        }
        return bestSize;
    }

    private boolean supportFocusMode(String mode, Camera.Parameters parameters) {
        List<String> supportedFocusModes = parameters.getSupportedFocusModes();
        for (String focusMode : supportedFocusModes) {
            if (mode.equals(focusMode)) {
                return true;
            }
        }
        return false;
    }


}
