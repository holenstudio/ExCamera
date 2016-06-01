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

/**
 * Camera类
 */
public class CameraHolder {
    private final static String TAG = "CameraHolder";

    private static CameraHolder mHolder = null;
    private Camera mCamera;
    private CameraInfo mCameraInfo;
    private Camera.Parameters mParameter;
    private boolean mIsFrontCamera;
    private int cameraCount;
    private int cameraId = CameraInfo.CAMERA_FACING_BACK;
    private static String mRecorderPath;

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
    }

    public Camera getCameraInstance() {
        Camera camera = null;
            try {
                camera = Camera.open();
                mIsFrontCamera = false;
                mCameraInfo = new CameraInfo();
            } catch (Exception e) {
                Log.d(TAG, "Error open camera:" + e.getMessage());
            }
        return camera;
    }

    public Camera getCameraInstance(boolean isFrontCamera) {
        if (mCamera == null) {
            mCamera = getCameraInstance();
        }
        cameraCount = mCamera.getNumberOfCameras();
        Camera.CameraInfo info = new CameraInfo();

        for (int i = 0; i < cameraCount; i++) {
            mCamera.getCameraInfo(i, info);
            if (isFrontCamera) {
                if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    mCamera.release();
                    mCamera = null;
                    cameraId = info.facing;
                    mCamera = Camera.open(i);
                    mIsFrontCamera = true;
                    mParameter = mCamera.getParameters();
                    mCamera.getCameraInfo(i, mCameraInfo);
                }
            } else {
                if (info.facing == CameraInfo.CAMERA_FACING_BACK) {
                    mCamera.release();
                    mCamera = null;
                    cameraId = info.facing;
                    mCamera = Camera.open(i);
                    mIsFrontCamera = false;
                    mParameter = mCamera.getParameters();
                    mCamera.getCameraInfo(i, mCameraInfo);
                }
            }
        }
        return mCamera;
    }

    public Camera getFrontCameraInstance() {
        if (mCamera != null && mIsFrontCamera) {
            return mCamera;
        }
        return getCameraInstance(true);
    }

    public Camera getBackCameraInstance() {
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
        mParameter = params;
        mCamera.setParameters(mParameter);
    }

    public Camera.Parameters getParameter () {
        if (mParameter == null) {
            mParameter = mCamera.getParameters();
        }
        return mParameter;
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
}
