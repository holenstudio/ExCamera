package com.holenstudio.excamera.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.drawable.Drawable;
import android.graphics.RectF;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

import com.holenstudio.excamera.CameraHolder;
import com.holenstudio.excamera.R;
import com.holenstudio.excamera.util.CameraUtil;

public class FrontView extends ImageView {

	private static final String TAG = "FrontView";
	private Camera.Face[] mFaces;
	private Context mContext;
	private Paint mPaint;
	private RectF paintRect = new RectF();
	private Matrix mMatrix = new Matrix();
	private int mDisplayOrientation = 0;
	private Drawable mFaceDrawable = null;

	public FrontView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		initPaint();
		mFaceDrawable = getResources().getDrawable(R.drawable.ic_face_find_2);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		// canvas.drawPaint(mPaint);
		if (mFaces == null || mFaces.length < 1) {
			return;
		}
		boolean isMirror = false;
		if (CameraHolder.getCameraHolder().getCameraInfo().facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
			isMirror = true;
		}
		CameraUtil.prepareMatrix(mMatrix, isMirror, mDisplayOrientation, getWidth(), getHeight());
		canvas.save();
		mMatrix.postRotate(0); // Matrix.postRotate默认是顺时针
		canvas.rotate(0); // Canvas.rotate()默认是逆时针
		for (Camera.Face face : mFaces) {
			paintRect.set(face.rect);
			mMatrix.mapRect(paintRect);
			mFaceDrawable.setBounds(Math.round(paintRect.left), Math.round(paintRect.top), Math.round(paintRect.right),
					Math.round(paintRect.bottom));
			mFaceDrawable.draw(canvas);
		}
		canvas.restore();
		super.onDraw(canvas);
	}

	private void initPaint() {
		mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		int color = Color.rgb(98, 212, 68);
		mPaint.setColor(color);
		mPaint.setStyle(Style.STROKE);
		mPaint.setStrokeWidth(20f);
		mPaint.setAlpha(180);
	}

	public void setDisplayOrientation(int displayOrientation) {
		mDisplayOrientation = displayOrientation;
		invalidate();
	}

	public void setFaces(Camera.Face[] faces) {
		mFaces = faces;
		invalidate();
	}

	public void clearFaces() {
		mFaces = null;
		invalidate();
	}
}
