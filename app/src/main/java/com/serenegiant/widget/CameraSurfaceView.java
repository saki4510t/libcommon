package com.serenegiant.widget;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.serenegiant.camera.CameraConst;
import com.serenegiant.camera.CameraUtils;

import java.io.IOException;

/**
 * カメラ映像を流し込んで表示するだけのSurfaceView実装
 */
public class CameraSurfaceView extends SurfaceView {
	private static final boolean DEBUG = true;	// set false on production
	private static final String TAG = CameraSurfaceView.class.getSimpleName();

	private static final int CAMERA_ID = 0;

	private boolean mHasSurface;

	private Camera mCamera;
	private int mRotation;

	/**
	 * コンストラクタ
	 * @param context
	 */
	public CameraSurfaceView(final Context context) {
		this(context, null, 0);
	}

	/**
	 * コンストラクタ
	 * @param context
	 * @param attrs
	 */
	public CameraSurfaceView(final Context context, final AttributeSet attrs) {
		this(context, attrs, 0);
	}

	/**
	 * コンストラクタ
	 * @param context
	 * @param attrs
	 * @param defStyleAttr
	 */
	public CameraSurfaceView(final Context context, final AttributeSet attrs, final int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		getHolder().addCallback(new SurfaceHolder.Callback() {
			@Override
			public void surfaceCreated(final SurfaceHolder holder) {
				mHasSurface = true;
			}

			@Override
			public void surfaceChanged(final SurfaceHolder holder, final int format, final int width, final int height) {
				startPreview();
			}

			@Override
			public void surfaceDestroyed(final SurfaceHolder holder) {
				mHasSurface = false;
				stopPreview();
			}
		});
	}

	public void onResume() {
		if (mHasSurface) {
			startPreview();
		}
	}

	public void onPause() {
		stopPreview();
	}

	public int getCameraRotation() {
		return mRotation;
	}

	private void startPreview() {
		if (mCamera == null) {
			try {
				mCamera = CameraUtils.setupCamera(getContext(),
					CameraConst.FACING_BACK,
					CameraDelegator.DEFAULT_PREVIEW_WIDTH, CameraDelegator.DEFAULT_PREVIEW_HEIGHT);
				CameraUtils.setPreviewSurface(mCamera, this);
			} catch (final IOException e) {
				Log.w(TAG, e);
				mCamera = null;
			}
			if (mCamera != null) {
				mCamera.startPreview();
			}
		}
	}

	private void stopPreview() {
		if (mCamera != null) {
			mCamera.stopPreview();
			mCamera.release();
			mCamera = null;
		}
	}

}
