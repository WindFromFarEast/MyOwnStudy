package com.xiangweixin.myownstudy.camera.camera2new;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Range;
import android.util.Rational;
import android.util.Size;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.xiangweixin.myownstudy.util.LogUtil;

import java.awt.font.NumericShaper;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static android.hardware.camera2.CameraMetadata.CONTROL_AE_MODE_ON;

/**
 * 正常预览流程: new->setSurfaceTextureListener->init->open->startPreview
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class Camera2 {

    private static final String TAG = "Camera2";

    private CameraManager mCameraManager;
    private CameraDevice mCameraDevice;
    private CameraCharacteristics mCharacteristics;
    private Context mContext;
    private Size mPreviewSize;
    private Size mPictureSize;
    private String[] mCameraIDList;

    private ImageReader mImageReader;

    private String mCurrentCameraID;

    private HandlerThread mHandlerThread = new HandlerThread("Camera2Thread");
    private Handler mCameraHandler;

    private TextureView mTextureView;
    private Camera2SurfaceTextureListener mCameraSurfaceTextureListener;

    private SurfaceTexture mSurfaceTexture;
    private Surface mPreviewSurface;
    private Surface mImageSurface;

    private CaptureRequest.Builder mCaptureRequestBuilder;
    private CameraCaptureSession mCaptureSession;

    private PictureCallback mPictureCallback;

    private OrientationEventListener mOrientationListener;
    private AtomicInteger mCurrentOrientation = new AtomicInteger(0);

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

    private ImageReader.OnImageAvailableListener mImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = reader.acquireNextImage();
            if (image == null) {
                LogUtil.formatE(TAG, "onImageAvailable >>> image is null.");
                return;
            }

            ByteBuffer imageBuffer = image.getPlanes()[0].getBuffer();
            byte[] imageBytes = new byte[imageBuffer.remaining()];
            imageBuffer.get(imageBytes);
            int width = image.getWidth();
            int height = image.getHeight();

            mPictureCallback.onSuccess(imageBytes, width, height);
            image.close();
        }
    };

    public Camera2(Context context, TextureView textureView) {
        mContext = context;
        mOrientationListener = new OrientationEventListener(mContext) {
            @Override
            public void onOrientationChanged(int orientation) {
                LogUtil.d(TAG, "Device orientation changed, now is " + orientation);
                mCurrentOrientation.getAndSet(orientation);
            }
        };
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

    public int init(Size previewSize, Size pictureSize, boolean useFront) {
        mHandlerThread.start();
        mCameraHandler = new Handler(mHandlerThread.getLooper());

        if (mOrientationListener.canDetectOrientation()) {
            mOrientationListener.enable();
        }

        mCameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        mPreviewSize = new Size(previewSize.getHeight(), previewSize.getWidth());
        mPictureSize = new Size(pictureSize.getHeight(), pictureSize.getWidth());

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
                    mCharacteristics = characteristics;
                    if (!isSupportedCamera2AllFeatures(mCharacteristics)) {
                        Log.e(TAG, "Don't support camera2 all features. Please use camera 1");
                        return -1;
                    }
                    break;
                } else if (!useFront && back) {
                    mCurrentCameraID = id;
                    mCharacteristics = characteristics;
                    if (!isSupportedCamera2AllFeatures(mCharacteristics)) {
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
        if (mCaptureSession != null) {
            try {
                mCaptureSession.stopRepeating();
                mCaptureSession.close();
                mCaptureSession = null;
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }

    public void startPreview() {
        try {
            StreamConfigurationMap streamConfigurationMap = mCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            Size[] previewSizes = streamConfigurationMap.getOutputSizes(SurfaceTexture.class);
            Size bestPreviewSize = getBestPreviewSize(previewSizes, mPreviewSize);
            mSurfaceTexture.setDefaultBufferSize(bestPreviewSize.getWidth(), bestPreviewSize.getHeight());
            mPreviewSurface = new Surface(mSurfaceTexture);

            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mCaptureRequestBuilder.addTarget(mPreviewSurface);

            Size[] pictureSizes = streamConfigurationMap.getOutputSizes(ImageReader.class);
            Size bestSize = getBestPictureSize(pictureSizes, mPictureSize);
            mImageReader = ImageReader.newInstance(bestSize.getWidth(), bestSize.getHeight(), ImageFormat.JPEG, 1);
            mImageReader.setOnImageAvailableListener(mImageAvailableListener, mCameraHandler);
            mImageSurface = mImageReader.getSurface();

            List<Surface> outputs = new ArrayList<>();
            outputs.add(mPreviewSurface);
            outputs.add(mImageSurface);
            mCameraDevice.createCaptureSession(outputs, mCaptureStateCallback, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * https://www.jianshu.com/p/2ae0a737c686
     */
    public void takePicture(final PictureCallback callback) {
        mPictureCallback = callback;

        try {
            CaptureRequest.Builder imageRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            imageRequestBuilder.addTarget(mImageSurface);
            imageRequestBuilder.addTarget(mPreviewSurface);
            int orientation = getPictureOrientation();
            imageRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, orientation);
            mCaptureSession.capture(imageRequestBuilder.build(), mSessionCaptureCallback, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void upExposureCompensation() {
        Range<Integer> compensationRange = mCharacteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);
        if (compensationRange == null || compensationRange.equals(Range.create(0, 0))) {
            return;
        }
        Rational compensationStep = mCharacteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP);
        if (compensationStep == null) {
            return;
        }
        //例如，stepF为0.2，那么每提高1EV的话，需要五个steF，也就是CONTROL_AE_EXPOSURE_COMPENSATION需要传入5才能提高1，传入1就只提高0.2EV，-1就降低0.2EV
        float stepF = compensationStep.getNumerator() * 1.0F / compensationStep.getDenominator();
        int stepsPerEv = Math.round(1 / stepF);
        int numSteps = (compensationRange.getUpper() - compensationRange.getLower()) / stepsPerEv;

        Integer currentCompensation = mCaptureRequestBuilder.get(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION);
        LogUtil.formatI(TAG, "Current exposure compensation: %d", currentCompensation.intValue());
        if (currentCompensation.equals(compensationRange.getUpper())) {
            return;
        }

        mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON);
        mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, currentCompensation + 1);

        try {
            mCaptureSession.setRepeatingRequest(mCaptureRequestBuilder.build(), mSessionCaptureCallback, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        LogUtil.formatI(TAG, "stepF: %f, stepsPerEv: %d, maxEv: %d, minEv: %d, numSteps: %d", stepF, stepsPerEv,compensationRange.getUpper(), compensationRange.getLower(), numSteps);
    }

    public void downExposureCompensation() {
        Range<Integer> compensationRange = mCharacteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);
        if (compensationRange == null || compensationRange.equals(Range.create(0, 0))) {
            return;
        }
        Rational compensationStep = mCharacteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP);
        if (compensationStep == null) {
            return;
        }
        //例如，stepF为0.2，那么每提高1EV的话，需要五个steF，也就是CONTROL_AE_EXPOSURE_COMPENSATION需要传入5才能提高1，传入1就只提高0.2EV，-1就降低0.2EV
        float stepF = compensationStep.getNumerator() * 1.0F / compensationStep.getDenominator();
        int stepsPerEv = Math.round(1 / stepF);
        int numSteps = (compensationRange.getUpper() - compensationRange.getLower()) / stepsPerEv;

        Integer currentCompensation = mCaptureRequestBuilder.get(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION);
        LogUtil.formatI(TAG, "Current exposure compensation: %d", currentCompensation.intValue());
        if (currentCompensation.equals(compensationRange.getLower())) {
            return;
        }

        mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON);
        mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, currentCompensation - 1);

        try {
            mCaptureSession.setRepeatingRequest(mCaptureRequestBuilder.build(), mSessionCaptureCallback, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        LogUtil.formatI(TAG, "stepF: %f, stepsPerEv: %d, maxEv: %d, minEv: %d, numSteps: %d", stepF, stepsPerEv,compensationRange.getUpper(), compensationRange.getLower(), numSteps);
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

    private Size getBestPictureSize(Size[] sizes, Size targetSize) {
        Size bestSize = sizes[0];
        float minDeltaRatio = Float.MAX_VALUE;
        for (Size size : sizes) {
            Log.i(TAG, "支持的拍照尺寸: (" + size.getWidth() + " x " + size.getHeight() + ")");
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

        Log.i(TAG, "目标拍照尺寸: (" + targetSize.getWidth() + " x " + targetSize.getHeight() + ")");
        Log.i(TAG, "最佳拍照尺寸: (" + bestSize.getWidth() + " x " + bestSize.getHeight() + ")");
        return bestSize;
    }

    /**
     * 预览的case下，Camera2已经自动为我们处理的旋转角度的问题。但是拍照case下没有，所以需要我们计算
     */
    private int getPictureOrientation() {
        //设备方向
        int deviceOrientation = mCurrentOrientation.getAndAdd(0);
        if (deviceOrientation == OrientationEventListener.ORIENTATION_UNKNOWN) {
            LogUtil.e(TAG, "Unknown orientation.");
            return -1;
        }
        //传感器方向
        Integer sensorOrientation = mCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);

        deviceOrientation = (deviceOrientation + 45) / 90 * 90;

        if (mCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
            deviceOrientation = -deviceOrientation;
        }

        return (sensorOrientation + deviceOrientation + 360) % 360;
    }

    public interface Camera2SurfaceTextureListener {
        void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height);

        void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height);

        boolean onSurfaceTextureDestroyed(SurfaceTexture surface);

        void onSurfaceTextureUpdated(SurfaceTexture surface);
    }

    public interface PictureCallback {
        void onSuccess(byte[] data, int width, int height);

        void onFail(Exception e);
    }

}
