package com.holenstudio.excamera;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
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
import com.holenstudio.excamera.view.CameraParasPopupWindow;
import com.holenstudio.excamera.view.FrontView;

public class MainActivity extends AppCompatActivity implements CameraHolder.OnCameraOpenedCallback{
    private static final String TAG = "MainActivity";
    private boolean mVideoStatus = false;
    private boolean mIsTakePicture = true;
    private boolean mIsStartFaceDetection = false;
    private boolean mIsShowPopupWindow = false;

    private CameraHolder mCameraHolder;
    private SurfaceView mCameraPreview;
    private MediaRecorder mRecorder;
    private SurfaceHolder mHolder;

    private Button mCaptureBtn;
    private Button mSwichModeBtn;
    private ImageView mPictureIv;
    private ImageView mSwitchIv;
    private ImageView mSettingIv;
    private View mFocusArea;
    private SeekBar mCameraZoomSeekBar;
    private TextView mCameraZoomValue;
    private FrontView mCameraFrontView;
    private CameraParasPopupWindow mPopupWindow;

    private OnSeekBarChangeListener mSeekBarChangeListener = new OnSeekBarChangeListener() {

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (mCameraHolder == null) {
                mCameraHolder = CameraHolder.getCameraHolder();
            }
            mCameraZoomValue.setText(String.valueOf(progress));
            mCameraHolder.getParameter().setZoom(progress);
            mCameraHolder.reloadParameters();
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

            String imagePath = FileUtil.savePhoto(data, System.currentTimeMillis() + ".jpg");
            BitmapFactory.Options option = new BitmapFactory.Options();
            option.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(data, 0, data.length, option);
            option.inSampleSize = CameraUtil.calculateInSampleSize(option, mPictureIv.getMeasuredWidth(),
                    mPictureIv.getMeasuredHeight());
            option.inJustDecodeBounds = false;
            mPictureIv.setImageBitmap(BitmapFactory.decodeByteArray(data, 0, data.length, option));
            try {
                MediaStore.Images.Media.insertImage(getContentResolver(), imagePath,
                        System.currentTimeMillis() + ".jpg", null);
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + imagePath));
            sendBroadcast(intent);

            mCameraHolder.getCamera().stopPreview();
            mCameraHolder.getCamera().startPreview();
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
                        // mCamera.autoFocus(mAutoFocusCallback);
                        mCameraHolder.getCamera().takePicture(null, null, mPicture);

                    } else {
                        if (mVideoStatus) {
                            mRecorder.stop();
                            Bitmap bitmap = CameraUtil.getVideoThumbnail(CameraHolder.getRecorderPath(), mPictureIv.getWidth(), mPictureIv.getHeight(), MediaStore.Images.Thumbnails.MICRO_KIND);
                            mPictureIv.setImageBitmap(CameraUtil.rotatingImageFromCamera(mCameraHolder.getCameraInfo(), DisplayUtil.getActivityOrientation(MainActivity.this), bitmap));
                            releaseMediaRecorder();
                            mCameraHolder.getCamera().lock();
                            mVideoStatus = false;
                            mCaptureBtn.setBackgroundResource(R.drawable.btn_recorder_background);
                        } else {
                            if (mRecorder == null) {
                                mRecorder = new MediaRecorder();
                            }
                            if (CameraHolder.prepareVideoRecorder(mCameraHolder.getCamera(), mRecorder, mCameraPreview)) {
                                mRecorder.start();
                                mCaptureBtn.setBackgroundResource(R.drawable.btn_recorder_stop_background);
                                mVideoStatus = true;
                            } else {
                                releaseMediaRecorder();
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
                    mCameraHolder.getCamera().setPreviewCallback(null);
                    mCameraHolder.switchCamera();
                    try {
                        mCameraHolder.getCamera().setPreviewDisplay(mHolder);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    // 设置摄像头旋转的角度
                    CameraUtil.setCameraDisplayOrientation(MainActivity.this, mCameraHolder.getCameraInfo(), mCameraHolder.getCamera());
                    CameraUtil.setParameterOrientation(MainActivity.this, mCameraHolder.getCameraInfo(), mCameraHolder.getParameter());
                    mCameraHolder.getParameter().setPreviewSize(mCameraPreview.getMeasuredHeight(), mCameraPreview.getMeasuredWidth());
                    mCameraHolder.reloadParameters();
                    mPopupWindow.setCameraParams(mCameraHolder.getParameter());
                    mCameraHolder.getCamera().startPreview();
                    mCameraHolder.getCamera().autoFocus(mAutoFocusCallback);
                    mIsStartFaceDetection = false;
                    startFaceDetection();
                    break;
                case R.id.iv_camera_setting :
                    if (!mIsShowPopupWindow) {
                        mPopupWindow.showAsDropDown(v);
                    } else {
                        mPopupWindow.dismiss();
                    }
                    mIsShowPopupWindow = !mIsShowPopupWindow;
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
                    Rect focusRect = CameraUtil.calculateTapArea(x, y, mCameraHolder.getParameter(), 1);
                    Rect meterRect = CameraUtil.calculateTapArea(x, y, mCameraHolder.getParameter(), 1.5f);
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
                    if (mCameraHolder.getParameter().getMaxNumFocusAreas() > 0) {
                        List<Camera.Area> focusAreas = new ArrayList<Camera.Area>();
                        focusAreas.add(new Camera.Area(focusRect, 1000));
                        mCameraHolder.getParameter().setFocusAreas(focusAreas);
                    }
                    if (mCameraHolder.getParameter().getMaxNumMeteringAreas() > 0) {
                        List<Camera.Area> meteringAreas = new ArrayList<Camera.Area>();
                        meteringAreas.add(new Camera.Area(meterRect, 1000));
                        mCameraHolder.getParameter().setMeteringAreas(meteringAreas);
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
                    mCameraHolder.getCamera().cancelAutoFocus();
                    mCameraHolder.reloadParameters();
                    mCameraHolder.getCamera().autoFocus(mAutoFocusCallback);
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
                mCameraHolder.getCamera().stopPreview();
                mCameraHolder.getCamera().setPreviewDisplay(holder);

                // 设置摄像头旋转的角度
                CameraUtil.setCameraDisplayOrientation(MainActivity.this, mCameraHolder.getCameraInfo(), mCameraHolder.getCamera());
                mCameraHolder.getParameter().setPreviewSize(mCameraPreview.getMeasuredHeight(), mCameraPreview.getMeasuredWidth());
                mCameraHolder.reloadParameters();
                mPopupWindow.setCameraParams(mCameraHolder.getParameter());
//                mCamera.setPreviewCallback(mPreviewCallback);
                mCameraHolder.getCamera().startPreview();
                startFaceDetection();
                // mCamera.autoFocus(mAutoFocusCallback);
            } catch (IOException e) {
                Log.d(TAG, "Error setting camera preview:" + e.getMessage());
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            if (mCameraHolder == null) {
                mCameraHolder = CameraHolder.getCameraHolder();
            }
            if (mHolder.getSurface() == null) {
                return;
            }

            try {
                mCameraHolder.getCamera().stopPreview();
            } catch (Exception e) {
                Log.d(TAG, "Error stop camera preview:" + e.getMessage());
            }

            try {
                mCameraHolder.getCamera().setPreviewDisplay(mHolder);
                mCameraFrontView.setDisplayOrientation(90);
                CameraUtil.setCameraDisplayOrientation(MainActivity.this, mCameraHolder.getCameraInfo(), mCameraHolder.getCamera());
                mCameraHolder.getParameter().setPreviewSize(mCameraPreview.getMeasuredHeight(), mCameraPreview.getMeasuredWidth());
                mCameraHolder.getCamera().setDisplayOrientation(90);
                mCameraHolder.reloadParameters();
//                mCamera.setPreviewCallback(mPreviewCallback);
                mPopupWindow.setCameraParams(mCameraHolder.getParameter());
                mCameraHolder.getCamera().startPreview();
                startFaceDetection();
            } catch (Exception e) {
                Log.d(TAG, "Error starting camera preview:" + e.getMessage());
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            mIsStartFaceDetection = false;
//            mCamera.setPreviewCallback(null);
            // release camera
            // CameraUtil.releaseCamera();
        }
    };

    public CameraParasPopupWindow.OnParameterSelectedListener mSelectedListener = new CameraParasPopupWindow.OnParameterSelectedListener() {

        @Override
        public void selectedParameter(Parameters params) {
            mCameraHolder.setParameters(params);
        }
    };

//    private Camera.PreviewCallback mPreviewCallback = new Camera.PreviewCallback() {
//        @Override
//        public void onPreviewFrame(byte[] data, Camera camera) {
//            int imageType = mParams.getPreviewFormat();
//            int[] rgbArray;
//            int width = mParams.getPreviewSize().width;
//            int height = mParams.getPreviewSize().height;
//            rgbArray = CameraUtil.decodeYUV420SP(data, width, height);
//            Bitmap bmp = Bitmap.createBitmap(rgbArray, width, height, Bitmap.Config.ARGB_8888);
//            bmp = Bitmap.createScaledBitmap(bmp, mPictureIv.getWidth(), mPictureIv.getHeight(), true);
//            mPictureIv.setImageBitmap(bmp);
//        }
//    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mCameraHolder = CameraHolder.getCameraHolder();
        mCameraPreview = (SurfaceView) findViewById(R.id.camera_view);
        mHolder = mCameraPreview.getHolder();
        mHolder.setFormat(PixelFormat.TRANSPARENT);
        mRecorder = new MediaRecorder();
        // surfaceview不维护自己的缓冲区，等待屏幕渲染引擎将内容推送到用户面前
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
//        mHolder.addCallback(mSurfaceHolderCallBack);

        initView();
        initData();
    }


    private void initView() {
        mCaptureBtn = (Button) findViewById(R.id.btn_capture);
        mSwichModeBtn = (Button) findViewById(R.id.btn_switch_mode);
        mPictureIv = (ImageView) findViewById(R.id.iv_picture);
        mSwitchIv = (ImageView) findViewById(R.id.iv_switch_camera);
        mSettingIv = (ImageView) findViewById(R.id.iv_camera_setting);
        mFocusArea = findViewById(R.id.focus_area);
        mCameraZoomSeekBar = (SeekBar) findViewById(R.id.camera_zoom_seek_bar);
        mCameraZoomValue = (TextView) findViewById(R.id.camera_zoom_value);
        mCameraFrontView = (FrontView) findViewById(R.id.camera_front_view);

        mCaptureBtn.setOnClickListener(mClickListener);
        mSwichModeBtn.setOnClickListener(mClickListener);
        mPictureIv.setOnClickListener(mClickListener);
        mSettingIv.setOnClickListener(mClickListener);
        mSwitchIv.setOnClickListener(mClickListener);
        mCameraPreview.setOnTouchListener(mTouchListener);
        mCameraZoomSeekBar.setOnSeekBarChangeListener(mSeekBarChangeListener);
        mCameraZoomSeekBar.setMax(mCameraHolder.getParameter().getMaxZoom());
    }

    private void initData() {
        mPopupWindow = new CameraParasPopupWindow(MainActivity.this);
        mPopupWindow.setCameraParams(mCameraHolder.getParameter());
        mPopupWindow.setOnParameterSelectedListener(mSelectedListener);
        mPopupWindow.setOutsideTouchable(false);
        mCameraZoomValue.setText(String.valueOf(mCameraHolder.getParameter().getZoom()));
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
        mCameraHolder = CameraHolder.getCameraHolder();
        CameraUtil.setParameterOrientation(MainActivity.this, mCameraHolder.getCameraInfo(), mCameraHolder.getParameter());
        mCameraHolder.getCamera().stopPreview();
        mCameraHolder.setParameters(mCameraHolder.getParameter());
        mPopupWindow.setCameraParams(mCameraHolder.getParameter());
        try {
            mCameraHolder.getCamera().setPreviewDisplay(mHolder);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mCameraHolder.getCamera().startPreview();

    }

    private void startFaceDetection() {
        mCameraHolder.getCamera().setFaceDetectionListener(mFaceDatectionListener);
        if (!mIsStartFaceDetection) {
            mCameraHolder.getCamera().cancelAutoFocus();
            mCameraHolder.getCamera().startFaceDetection();
        } else {
            mCameraHolder.getCamera().autoFocus(mAutoFocusCallback);
            mCameraHolder.getCamera().stopFaceDetection();
            mCameraHolder.getCamera().startFaceDetection();
        }
        mIsStartFaceDetection = true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        releaseCamera();
        releaseMediaRecorder();
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

    private void releaseCamera() {
        if (mCameraHolder.getCamera() != null) {
            mCameraHolder.releaseCamera();
        }
    }

    private void releaseMediaRecorder() {
        if (mRecorder != null) {
            mRecorder.reset();
            mRecorder.release();
            mRecorder = null;
        }
    }

    @Override
    public void doCallback() {
        mHolder.addCallback(mSurfaceHolderCallBack);
        mCameraZoomSeekBar.setMax(mCameraHolder.getParameter().getMaxZoom());
        mPopupWindow.setCameraParams(mCameraHolder.getParameter());
        mCameraZoomValue.setText(String.valueOf(mCameraHolder.getParameter().getZoom()));
    }
}
