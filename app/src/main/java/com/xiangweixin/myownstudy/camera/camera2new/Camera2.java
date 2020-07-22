package com.xiangweixin.myownstudy.camera.camera2new;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Build;
import android.os.ConditionVariable;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class Camera2 {

    private static final String TAG = "Camera2";

    private CameraManager mCameraManager;
    private CameraDevice mCameraDevice;
    private Context mContext;
    private Size mPreviewSize;
    private String[] mCameraIDList;

    private String mCurrentCameraID;

    private HandlerThread mHandlerThread = new HandlerThread("Camera2Thread");
    private Handler mCameraHandler;

    private TextureView mTextureView;
    private Camera2SurfaceTextureListener mCameraSurfaceTextureListener;

    private SurfaceTexture mSurfaceTexture;
    private Surface mPreviewSurface;

    private CaptureRequest.Builder mCaptureRequestBuilder;
    private CameraCaptureSession mCaptureSession;

    private ConditionVariable mCameraCondition = new ConditionVariable(false);

    private CameraDevice.StateCallback mOpenCameraStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            Log.i(TAG, "open camera success.");
            mCameraDevice = camera;
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            Log.e(TAG, "open disconnected.");
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            Log.e(TAG, "open error: error");
            mCameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onClosed(@NonNull CameraDevice camera) {
            Log.i(TAG, "camera close success.");
            super.onClosed(camera);
        }
    };

    private CameraCaptureSession.StateCallback mCaptureStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            Log.i(TAG, "onConfigured: ");
            mCaptureSession = session;

            updateCapture();
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {

        }

        @Override
        public void onClosed(@NonNull CameraCaptureSession session) {
            Log.i(TAG, "camera capture session closed.");
            super.onClosed(session);
        }
    };

    private CameraCaptureSession.CaptureCallback mSessionCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
            super.onCaptureStarted(session, request, timestamp, frameNumber);
        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
            super.onCaptureProgressed(session, request, partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
        }

        @Override
        public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
            super.onCaptureFailed(session, request, failure);
        }

        @Override
        public void onCaptureSequenceCompleted(@NonNull CameraCaptureSession session, int sequenceId, long frameNumber) {
            super.onCaptureSequenceCompleted(session, sequenceId, frameNumber);
        }

        @Override
        public void onCaptureSequenceAborted(@NonNull CameraCaptureSession session, int sequenceId) {
            super.onCaptureSequenceAborted(session, sequenceId);
        }

        @Override
        public void onCaptureBufferLost(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull Surface target, long frameNumber) {
            super.onCaptureBufferLost(session, request, target, frameNumber);
        }
    };

    public Camera2(Context context, TextureView textureView) {
        mContext = context;
        mTextureView = textureView;
        mTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                mSurfaceTexture = surface;
                if (mCameraSurfaceTextureListener != null) {
                    mCameraSurfaceTextureListener.onSurfaceTextureAvailable(surface, width, height);
                }
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                if (mCameraSurfaceTextureListener != null) {
                    mCameraSurfaceTextureListener.onSurfaceTextureSizeChanged(surface, width, height);
                }
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                if (mCameraSurfaceTextureListener != null) {
                    mCameraSurfaceTextureListener.onSurfaceTextureDestroyed(surface);
                }
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
                if (mCameraSurfaceTextureListener != null) {
                    mCameraSurfaceTextureListener.onSurfaceTextureUpdated(surface);
                }
            }
        });
    }

    public void setSurfaceTextureListener(Camera2SurfaceTextureListener listener) {
        mCameraSurfaceTextureListener = listener;
    }

    public int init(Size previewSize, boolean useFront) {
        mHandlerThread.start();
        mCameraHandler = new Handler(mHandlerThread.getLooper());

        mCameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        mPreviewSize = new Size(previewSize.getHeight(), previewSize.getWidth());
        try {
            mCameraIDList = mCameraManager.getCameraIdList();
            if (!hasUsableCamera(mCameraIDList)) {
                Log.e(TAG, "No usable camera.");
            }
            //检查对应摄像头是否支持Camera2
            for (String id : mCameraIDList) {
                CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(id);
                boolean front = characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT;
                boolean back = characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK;
                if (useFront && front) {
                    mCurrentCameraID = id;
                    if (!isSupportedCamera2AllFeatures(characteristics)) {
                        Log.e(TAG, "Don't support camera2 all features. Please use camera 1");
                        return -1;
                    }
                    break;
                } else if (!useFront && back) {
                    mCurrentCameraID = id;
                    if (!isSupportedCamera2AllFeatures(characteristics)) {
                        Log.e(TAG, "Don't support camera2 all features. Please use camera 1");
                        return -1;
                    }
                    break;
                }
            }

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public void open() {
        try {
            mCameraManager.openCamera(mCurrentCameraID, mOpenCameraStateCallback, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }

    public void startPreview() {
        try {
            CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(mCurrentCameraID);
            StreamConfigurationMap streamConfigurationMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            Size[] previewSizes = streamConfigurationMap.getOutputSizes(SurfaceTexture.class);
            Size bestPreviewSize = getBestPreviewSize(previewSizes, mPreviewSize);
            mSurfaceTexture.setDefaultBufferSize(bestPreviewSize.getWidth(), bestPreviewSize.getHeight());
            mPreviewSurface = new Surface(mSurfaceTexture);

            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mCaptureRequestBuilder.addTarget(mPreviewSurface);

            List<Surface> outputs = new ArrayList<>();
            outputs.add(mPreviewSurface);
            mCameraDevice.createCaptureSession(outputs, mCaptureStateCallback, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void stopPreview() {
        try {
            mCaptureSession.stopRepeating();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void destroy() {
        if (mHandlerThread != null) {
            mHandlerThread.quit();
            mHandlerThread = null;
        }
        mCameraHandler = null;
    }

    private void updateCapture() {
        CaptureRequest captureRequest = mCaptureRequestBuilder.build();
        try {
            mCaptureSession.setRepeatingRequest(captureRequest, mSessionCaptureCallback, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private boolean hasUsableCamera(String[] cameraIDList) {
        if (cameraIDList.length == 0) {
            return false;
        }
        return true;
    }

    private boolean isSupportedCamera2AllFeatures(CameraCharacteristics characteristics) {
        Integer level = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
        if (level != CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL && level != CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3) {
            return false;
        }
        return true;
    }

    private Size getBestPreviewSize(Size[] sizes, Size targetSize) {
        Size bestSize = sizes[0];
        float minDeltaRatio = Float.MAX_VALUE;
        for (Size size : sizes) {
            Log.i(TAG, "支持的预览尺寸: (" + size.getWidth() + " x " + size.getHeight() + ")");
            if (targetSize.equals(size)) {
                bestSize = size;
                break;
            }
            if (size.getHeight() > targetSize.getHeight() && size.getWidth() > targetSize.getWidth()) {
                continue;
            }
            float targetRatio = targetSize.getWidth() * 1.0f / targetSize.getHeight();
            float currentRatio = size.getWidth() * 1.0f / size.getHeight();
            float deltaRatio = Math.abs(targetRatio - currentRatio);
            if (deltaRatio < minDeltaRatio) {
                minDeltaRatio = deltaRatio;
                bestSize = size;
            }
        }

        Log.i(TAG, "目标预览尺寸: (" + targetSize.getWidth() + " x " + targetSize.getHeight() + ")");
        Log.i(TAG, "最佳预览尺寸: (" + bestSize.getWidth() + " x " + bestSize.getHeight() + ")");
        return bestSize;
    }

    public interface Camera2SurfaceTextureListener {
        void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height);
        void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height);
        boolean onSurfaceTextureDestroyed(SurfaceTexture surface);
        void onSurfaceTextureUpdated(SurfaceTexture surface);
    }

}
