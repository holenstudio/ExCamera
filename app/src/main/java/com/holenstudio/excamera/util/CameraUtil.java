package com.holenstudio.excamera.util;

import java.io.IOException;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.media.MediaRecorder;
import android.util.Log;
import android.view.Surface;

import com.holenstudio.excamera.CameraHolder;

public class CameraUtil {
    private final static String TAG = "CameraUtil";
    public final static String FACE_DETECTION = "face_detection"; //1 start,0 stop

    public static Camera switchCamera(boolean isFrontCamera) {
        Camera camera = null;
        if (isFrontCamera) {
            // switch front camera to back camera
            camera = CameraHolder.getCameraHolder().getCameraInstance(false);
        } else {
            // switch back camera to front
            camera = CameraHolder.getCameraHolder().getCameraInstance(true);
        }
        return camera;
    }

    public static boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_CAMERA)) {
            return true;
        }
        return false;
    }

    public static void releaseCamera(Camera camera) {
        if (camera != null) {
            try {
                camera.stopPreview();
                camera.release();
                camera = null;
            } catch (Exception e) {
                Log.d(TAG, "Error release camera:" + e.getMessage());
            }
        }
    }

    public static void releaseMediaRecorder(MediaRecorder recorder) {
        if (recorder != null) {
            recorder.reset();
            recorder.release();
            recorder = null;
        }
    }

    public static void setCameraDisplayOrientation(Activity activity,
                                                   CameraInfo info, Camera camera) {
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;

        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360; // compensate the mirror
        } else { // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    public static void setParameterOrientation(Activity activity, CameraInfo info, Parameters params) {
        int orientation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (orientation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        orientation = (degrees + 45) / 90 * 90;
        int rotation = 0;
        if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
            rotation = (info.orientation - orientation + 360) % 360;
        } else {  // back-facing camera
            rotation = (info.orientation + orientation) % 360;
        }
        params.setRotation(rotation);
    }


    public static int calculateInSampleSize(BitmapFactory.Options opt, int width, int height) {
        int inSampleSize = 1;
        int optWidth = opt.outWidth;
        int optHeight = opt.outHeight;
        optWidth /= 2;
        optHeight /= 2;
        while (optWidth > width || optHeight > height) {
            optWidth /= 2;
            optHeight /= 2;
            inSampleSize *= 2;
        }
        return inSampleSize;
    }

    /**
     * Convert touch position x:y to {@link Camera.Area} position -1000:-1000 to 1000:1000.
     */
    public static Rect calculateTapArea(float x, float y, Parameters params, float coefficient) {
        float focusAreaSize = 50;
        Camera.Size size = params.getPreviewSize();
        int areaSize = Float.valueOf(focusAreaSize * coefficient).intValue();

        int centerX = (int) (x / size.height * 2000 - 1000);
        int centerY = (int) (y / size.width * 2000 - 1000);

        int left = clamp(centerX - areaSize / 2, -1000, 1000);
        int right = clamp(left + areaSize, -1000, 1000);
        int top = clamp(centerY - areaSize / 2, -1000, 1000);
        int bottom = clamp(top + areaSize, -1000, 1000);

        Log.d(TAG, "Rect size:x = " + x + ",y=" + y + ",rect:" + new Rect(left, top, right, bottom).flattenToString());
        return new Rect(left, top, right, bottom);
    }

    private static int clamp(int x, int min, int max) {
        if (x > max) {
            return max;
        }
        if (x < min) {
            return min;
        }
        return x;
    }

    public static void prepareMatrix(Matrix matrix, boolean mirror, int displayOrientation,
                                     int viewWidth, int viewHeight) {
        // Need mirror for front camera.
        matrix.setScale(mirror ? -1 : 1, 1);
        // This is the value for android.hardware.Camera.setDisplayOrientation.
        matrix.postRotate(displayOrientation);
        // Camera driver coordinates range from (-1000, -1000) to (1000, 1000).
        // UI coordinates range from (0, 0) to (width, height).
        matrix.postScale(viewWidth / 2000f, viewHeight / 2000f);
        matrix.postTranslate(viewWidth / 2f, viewHeight / 2f);
    }
}
