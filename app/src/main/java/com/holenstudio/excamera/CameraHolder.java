package com.holenstudio.excamera;

import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.util.Log;
import android.view.SurfaceView;

import com.holenstudio.excamera.util.CameraUtil;
import com.holenstudio.excamera.util.FileUtil;

import java.io.IOException;
import java.util.List;

/**
 * Camera类
 */
public class CameraHolder {
    private final static String TAG = "CameraHolder";

    private static CameraHolder mHolder = null;
    private Camera mCamera;
    private CameraInfo mCameraInfo;
    private Camera.Parameters mParameters;
    private boolean mIsFrontCamera;
    private int cameraCount;
    private int cameraId = CameraInfo.CAMERA_FACING_BACK;
    private static String mRecorderPath;
    private boolean mIsStartFaceDetection;
    private OnCameraOpenedCallback mOpenedCallback;

    public static CameraHolder getCameraHolder() {
        if (mHolder == null) {
            try {
                mHolder = new CameraHolder();
            } catch (Exception e) {
                Log.d(TAG, "Error open camera:" + e.getMessage());
            }
        }
        return mHolder;
    }

    public CameraHolder() {
        mCamera = getBackCameraInstance();
        if (mOpenedCallback != null) {
            mOpenedCallback.doCallback();
        }
        initCameraParams();
    }

    private void initCameraParams() {
        mParameters.setAntibanding(Camera.Parameters.ANTIBANDING_AUTO);
        mParameters.setColorEffect(Camera.Parameters.EFFECT_NONE);
        mParameters.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
        mParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        mParameters.setJpegQuality(100);
        mParameters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
        List<Camera.Size> supporteSize = mParameters.getSupportedPictureSizes();
        for (Camera.Size size:supporteSize) {
            Log.d(TAG, "width=" + size.width + ", height=" + size.height);
            if (size.width == 1280) {
                mParameters.setPictureSize(size.width, size.height);
            }
            if (size.height == 720) {
                mParameters.setPictureSize(size.width, size.height);
            }
        }
        setParameters(mParameters);
    }

    private Camera getCameraInstance(boolean isFrontCamera) {
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
        cameraCount = Camera.getNumberOfCameras();
        Camera.CameraInfo info = new CameraInfo();

        for (int i = 0; i < cameraCount; i++) {
            Camera.getCameraInfo(i, info);
            if (isFrontCamera) {
                if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    cameraId = i;
                    mCamera = Camera.open(cameraId);
                    mIsFrontCamera = true;
                    mParameters = mCamera.getParameters();
                    mCameraInfo = new CameraInfo();
                    mCameraInfo = info;
                    return mCamera;
//                    Camera.getCameraInfo(i, mCameraInfo);
                }
            } else {
                if (info.facing == CameraInfo.CAMERA_FACING_BACK) {
                    cameraId = i;
                    mCamera = Camera.open(cameraId);
                    mIsFrontCamera = false;
                    mParameters = mCamera.getParameters();
                    mCameraInfo = info;
                    return mCamera;
//                    Camera.getCameraInfo(i, mCameraInfo);
                }
            }
        }
        return mCamera;
    }

    private Camera getFrontCameraInstance() {
        if (mCamera != null && mIsFrontCamera) {
            return mCamera;
        }
        return getCameraInstance(true);
    }

    private Camera getBackCameraInstance() {
        if (mCamera != null && !mIsFrontCamera) {
            return mCamera;
        }
        return getCameraInstance(false);
    }

    public Camera switchCamera() {
        if (mIsFrontCamera) {
            // switch front camera to back camera
            mCamera = getCameraInstance(false);
        } else {
            // switch back camera to front
            mCamera = getCameraInstance(true);
        }
        return mCamera;
    }

    public CameraInfo getCameraInfo() {
        return mCameraInfo;
    }

    public int getCameraId() {
        return cameraId;
    }

    public void setParameters (Camera.Parameters params) {
        mParameters = params;
        mCamera.setParameters(mParameters);
    }

    public Camera.Parameters getParameter () {
        if (mParameters == null) {
            mParameters = mCamera.getParameters();
        }
        return mParameters;
    }

    public Camera getCamera () {
        if (mCamera == null) {
            mCamera = getBackCameraInstance();
        }
        return mCamera;
    }

    public void setCamera (Camera camera) {
        mCamera = camera;
    }

    public static boolean prepareVideoRecorder(Camera camera, MediaRecorder recorder,
                                               SurfaceView preview) {
        // Step1:Unlock and set camera to MediaRecorder
        camera.unlock();
        recorder.setCamera(camera);

        // Step2:Set sources
        recorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        recorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        // Step3:Set a CamcorderProfile
        recorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_720P));

        // Step4:Set output file
        mRecorderPath = FileUtil.videoDir().getAbsolutePath() + "/" +  System.currentTimeMillis() + ".mp4";
        recorder.setOutputFile(mRecorderPath);

        // Step5:Set the preview output
        recorder.setPreviewDisplay(preview.getHolder().getSurface());

        // Step6:Prepare configured MediaRecorder
        try {
            recorder.prepare();
        } catch (IllegalStateException e) {
            Log.d(TAG,
                    "IllegalStateException preparing MediaRecorder: "
                            + e.getMessage());
            CameraUtil.releaseMediaRecorder(recorder);
            CameraUtil.releaseCamera(camera);
            return false;
        } catch (IOException e) {
            Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
            CameraUtil.releaseMediaRecorder(recorder);
            CameraUtil.releaseCamera(camera);
            return false;
        }

        return true;
    }

    public static String getRecorderPath() {
        return mRecorderPath;
    }
    
    public void releaseCamera () {
        mCamera.setPreviewCallback(null);
        mIsStartFaceDetection = false;
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
    }

    public void reloadParameters () {
        if (mParameters != null) {
            mCamera.setParameters(mParameters);
        }
    }

    public void setOnCameraOpenedCallback(OnCameraOpenedCallback callback) {
        mOpenedCallback = callback;
    }
    public interface OnCameraOpenedCallback {
        public void doCallback();
    }
}
