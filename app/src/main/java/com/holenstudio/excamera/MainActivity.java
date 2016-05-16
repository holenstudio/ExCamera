package com.holenstudio.excamera;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Face;
import android.hardware.Camera.FaceDetectionListener;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.holenstudio.excamera.util.CameraUtil;
import com.holenstudio.excamera.util.DisplayUtil;
import com.holenstudio.excamera.util.FileUtil;
import com.holenstudio.excamera.view.FrontView;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private boolean mVideoStatus = false;
    private boolean mIsTakePicture = true;
    private boolean mIsStartFaceDetection = false;

    private CameraHolder mCameraHolder;
    private SurfaceView mCameraPreview;
    private MediaRecorder mRecorder;
    private Camera mCamera;
    private Camera.Parameters mParams;
    private Camera.CameraInfo mCameraInfo;
    private SurfaceHolder mHolder;

    private Button mCaptureBtn;
    private Button mSwichModeBtn;
    private ImageView mPictureIv;
    private ImageView mSwitchIv;
    private View mFocusArea;
    private SeekBar mCameraZoomSeekBar;
    private TextView mCameraZoomValue;
    private FrontView mCameraFrontView;

    private OnSeekBarChangeListener mSeekBarChangeListener = new OnSeekBarChangeListener() {

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            mCameraZoomValue.setText(String.valueOf(progress));
            mParams.setZoom(progress);
            mCamera.setParameters(mParams);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            // TODO Auto-generated method stub

        }

    };
    private PictureCallback mPicture = new PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            File pictureFile = FileUtil.photoDir();
            if (pictureFile == null) {
                Toast.makeText(MainActivity.this, "No permisson to access storage file", Toast.LENGTH_SHORT).show();
                return;
            }

            BitmapFactory.Options option = new BitmapFactory.Options();
            option.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(data, 0, data.length, option);
            option.inSampleSize = CameraUtil.calculateInSampleSize(option, mPictureIv.getMeasuredWidth(),
                    mPictureIv.getMeasuredHeight());
            option.inJustDecodeBounds = false;
            String imagePath = FileUtil.savePhoto(data, System.currentTimeMillis() + ".jpg");
            mPictureIv.setImageBitmap(BitmapFactory.decodeByteArray(data, 0, data.length, option));
            try {
                MediaStore.Images.Media.insertImage(getContentResolver(), imagePath,
                        System.currentTimeMillis() + ".jpg", null);
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + imagePath));

            mCamera.stopPreview();
            mCamera.startPreview();
        }

    };

    private FaceDetectionListener mFaceDatectionListener = new FaceDetectionListener() {

        @Override
        public void onFaceDetection(Face[] faces, Camera camera) {
            if (faces.length > 0) {
                mCameraFrontView.setFaces(faces);
            } else {
                mCameraFrontView.clearFaces();
            }
        }
    };

    private AutoFocusCallback mAutoFocusCallback = new AutoFocusCallback() {

        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            Log.d(TAG, "autoFocus start");
            mFocusArea.setVisibility(View.GONE);
            mCameraFrontView.clearFaces();
        }
    };

    private OnClickListener mClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            Intent intent;
            switch (v.getId()) {
                case R.id.btn_capture:
                    if (mIsTakePicture) {
                        Parameters p = mParams;
                        Log.d(TAG, p.flatten());
                        // mCamera.autoFocus(mAutoFocusCallback);
                        mCamera.takePicture(null, null, mPicture);

                    } else {
                        if (mVideoStatus) {
                            mRecorder.stop();
                            CameraUtil.releaseMediaRecorder(mRecorder);
                            mCameraHolder.getCameraInstance().lock();
                            mVideoStatus = false;
                        } else {
                            if (mCameraHolder.prepareVideoRecorder(mCamera, mRecorder, mCameraPreview)) {
                                mRecorder.start();
                                mVideoStatus = true;
                            } else {
                                CameraUtil.releaseMediaRecorder(mRecorder);
                            }
                        }
                    }
                    break;
                case R.id.btn_switch_mode:
                    if (mIsTakePicture) {
                        mIsTakePicture = false;
                        mSwichModeBtn.setBackgroundResource(R.drawable.btn_switch_to_camera);
                        mCaptureBtn.setBackgroundResource(R.drawable.btn_recorder_background);
                    } else {
                        mIsTakePicture = true;
                        mSwichModeBtn.setBackgroundResource(R.drawable.btn_switch_to_video);
                        mCaptureBtn.setBackgroundResource(R.drawable.btn_shutter_background);
                    }
                    break;
                case R.id.iv_picture:
                    intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("image/*");
                    startActivity(intent);
                    break;
                case R.id.iv_switch_camera:
                    mCameraFrontView.clearFaces();
                    try {
                        mCamera.setPreviewDisplay(mHolder);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    // 设置摄像头旋转的角度
                    CameraUtil.setCameraDisplayOrientation(MainActivity.this, mCameraInfo, mCamera);
                    mParams = mCamera.getParameters();
                    CameraUtil.setParameterOrientation(MainActivity.this, mCameraInfo, mParams);
                    mParams.setPreviewSize(mCameraPreview.getMeasuredHeight(), mCameraPreview.getMeasuredWidth());
                    mCamera.setParameters(mParams);
                    mCamera.startPreview();
                    mCamera.autoFocus(mAutoFocusCallback);
                    checkFaceDetection();
                    break;
                default:
                    break;
            }

        }
    };

    private OnTouchListener mTouchListener = new OnTouchListener() {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    int x = (int) event.getX();
                    int y = (int) event.getY();
                    int focusSize = 100;
                    Rect focusRect = CameraUtil.calculateTapArea(x, y, mParams, 1);
                    Rect meterRect = CameraUtil.calculateTapArea(x, y, mParams, 1.5f);
                    Log.d(TAG, "focusRect :" + focusRect.flattenToString());
                    Log.d(TAG, "meterRect :" + meterRect.flattenToString());
                    Rect displayRect = new Rect();
                    displayRect.left = x - focusSize;
                    displayRect.right = x + focusSize;
                    displayRect.bottom = y + focusSize;
                    displayRect.top = y - focusSize;
                    Log.d(TAG, "displayRect :" + displayRect.flattenToString());
                    if (displayRect.left < 0) {
                        displayRect.left = 0;
                        displayRect.right = displayRect.left + 2 * focusSize;
                    } else if (displayRect.right > v.getMeasuredWidth()) {
                        displayRect.right = v.getMeasuredWidth();
                        displayRect.left = displayRect.right - 2 * focusSize;
                    }
                    if (displayRect.top < 0) {
                        displayRect.top = 0;
                        displayRect.bottom = displayRect.top + 2 * focusSize;
                    } else if (displayRect.bottom > v.getMeasuredHeight()) {
                        displayRect.bottom = v.getMeasuredHeight();
                        displayRect.top = displayRect.bottom - 2 * focusSize;
                    }
                    if (mParams.getMaxNumFocusAreas() > 0) {
                        List<Camera.Area> focusAreas = new ArrayList<Camera.Area>();
                        focusAreas.add(new Camera.Area(focusRect, 1000));
                        mParams.setFocusAreas(focusAreas);
                    }
                    if (mParams.getMaxNumMeteringAreas() > 0) {
                        List<Camera.Area> meteringAreas = new ArrayList<Camera.Area>();
                        meteringAreas.add(new Camera.Area(meterRect, 1000));
                        mParams.setMeteringAreas(meteringAreas);
                    }
                    FrameLayout.LayoutParams focusLayoutParams = (android.widget.FrameLayout.LayoutParams) mFocusArea
                            .getLayoutParams();
                    focusLayoutParams.topMargin = displayRect.top;
                    focusLayoutParams.leftMargin = displayRect.left;
                    mFocusArea.setLayoutParams(focusLayoutParams);
                    mFocusArea.setVisibility(View.VISIBLE);
                    ScaleAnimation sa = new ScaleAnimation(3f, 1f, 3f, 1f, ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
                            ScaleAnimation.RELATIVE_TO_SELF, 0.5f);
                    sa.setDuration(800);
                    mFocusArea.startAnimation(sa);
                    mCamera.cancelAutoFocus();
                    mCamera.setParameters(mParams);
                    mCamera.autoFocus(mAutoFocusCallback);
                    break;

                default:
                    break;
            }
            return false;
        }
    };

    private SurfaceHolder.Callback mSurfaceHolderCallBack = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            try {
                mCamera.stopPreview();
                mCamera.setPreviewDisplay(holder);

                // 设置摄像头旋转的角度
                CameraUtil.setCameraDisplayOrientation(MainActivity.this, mCameraInfo, mCamera);
                mParams.setPreviewSize(mCameraPreview.getMeasuredHeight(), mCameraPreview.getMeasuredWidth());
                mCamera.setParameters(mParams);
                mCamera.startPreview();
                // mCamera.autoFocus(mAutoFocusCallback);
            } catch (IOException e) {
                Log.d(TAG, "Error setting camera preview:" + e.getMessage());
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            if (mCamera == null) {
                mCameraHolder = CameraHolder.getCameraHolder();
                mCamera = mCameraHolder.getCameraInstance();
                mCameraInfo = mCameraHolder.getCameraInfo();
            }
            if (mHolder.getSurface() == null) {
                return;
            }

            try {
                mCamera.stopPreview();
            } catch (Exception e) {
                Log.d(TAG, "Error stop camera preview:" + e.getMessage());
            }

            try {
                mCamera.setPreviewDisplay(mHolder);
                mCamera.startPreview();
            } catch (Exception e) {
                Log.d(TAG, "Error starting camera preview:" + e.getMessage());
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            // release camera
            // CameraUtil.releaseCamera();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mCameraHolder = CameraHolder.getCameraHolder();
        mCamera = mCameraHolder.getCamera();
        mCameraInfo = mCameraHolder.getCameraInfo();
        mParams = mCameraHolder.getParameter();
        mCameraPreview = (SurfaceView) findViewById(R.id.camera_view);
        mHolder = mCameraPreview.getHolder();
        mHolder.setFormat(PixelFormat.TRANSPARENT);
        mRecorder = new MediaRecorder();
        // surfaceview不维护自己的缓冲区，等待屏幕渲染引擎将内容推送到用户面前
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mHolder.addCallback(mSurfaceHolderCallBack);

        initView();
        initData();
    }

    private void initView() {
        mCaptureBtn = (Button) findViewById(R.id.btn_capture);
        mSwichModeBtn = (Button) findViewById(R.id.btn_switch_mode);
        mPictureIv = (ImageView) findViewById(R.id.iv_picture);
        mSwitchIv = (ImageView) findViewById(R.id.iv_switch_camera);
        mFocusArea = findViewById(R.id.focus_area);
        mCameraZoomSeekBar = (SeekBar) findViewById(R.id.camera_zoom_seek_bar);
        mCameraZoomValue = (TextView) findViewById(R.id.camera_zoom_value);
        mCameraFrontView = (FrontView) findViewById(R.id.camera_front_view);

        mCaptureBtn.setOnClickListener(mClickListener);
        mSwichModeBtn.setOnClickListener(mClickListener);
        mPictureIv.setOnClickListener(mClickListener);
        mSwitchIv.setOnClickListener(mClickListener);
        mCameraPreview.setOnTouchListener(mTouchListener);
        mCameraZoomSeekBar.setOnSeekBarChangeListener(mSeekBarChangeListener);
        mCameraZoomSeekBar.setMax(mParams.getMaxZoom());
    }

    private void initData() {
        mCameraZoomValue.setText(String.valueOf(mParams.getZoom()));
        mCameraFrontView.setDisplayOrientation(90);
        if (mIsTakePicture) {
            mSwichModeBtn.setBackgroundResource(R.drawable.btn_switch_to_video);
        } else {
            mSwichModeBtn.setBackgroundResource(R.drawable.btn_switch_to_camera);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        initViewParams();
        if (mCamera == null) {
            mCamera = mCameraHolder.getCameraInstance();
            mCameraInfo = mCameraHolder.getCameraInfo();
            mParams = mCamera.getParameters();
        }
        CameraUtil.setParameterOrientation(MainActivity.this, mCameraInfo, mParams);
        mCamera.stopPreview();
        mCamera.setParameters(mParams);
        try {
            mCamera.setPreviewDisplay(mHolder);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mCamera.startPreview();

        checkFaceDetection();
    }

    private void checkFaceDetection() {
        mCamera.setFaceDetectionListener(mFaceDatectionListener);
        if (mIsStartFaceDetection) {
            mCamera.cancelAutoFocus();
            mCamera.startFaceDetection();
        } else {
            mCamera.autoFocus(mAutoFocusCallback);
            mCamera.startFaceDetection();
            mCamera.stopFaceDetection();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        releaseCamera();
        CameraUtil.releaseMediaRecorder(mRecorder);
    }

    private void initViewParams() {
        LayoutParams params = mCameraPreview.getLayoutParams();
        Point p = DisplayUtil.getScreenMetrics(this);
        params.width = p.x;
        params.height = p.y;
        mCameraPreview.setLayoutParams(params);

        // set preview's width and height
        // mParams.setPreviewSize(p.x, p.y);

        LayoutParams p2 = mCaptureBtn.getLayoutParams();
        p2.width = DisplayUtil.dip2px(this, 80);
        p2.height = DisplayUtil.dip2px(this, 80);
        mCaptureBtn.setLayoutParams(p2);

    }

    public void releaseCamera() {
        if (mCamera != null) {
            try {
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
            } catch (Exception e) {
                Log.d(TAG, "Error release camera:" + e.getMessage());
            }
        }
    }


}
