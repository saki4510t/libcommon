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
import java.util.List;

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
			final int cameraId = CameraUtils.findCamera(CameraConst.FACING_BACK);
			mCamera = Camera.open(cameraId);
			final Camera.Parameters params = mCamera.getParameters();
			if (params != null) {
				try {
					final List<String> focusModes = params.getSupportedFocusModes();
					if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
						params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
					} else if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
						params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
					} else {
						if (DEBUG) Log.i(TAG, "handleStartPreview:Camera does not support autofocus");
					}
					params.setRecordingHint(true);
					CameraUtils.chooseVideoSize(params, CameraConst.DEFAULT_WIDTH, CameraConst.DEFAULT_HEIGHT);
					final int[] fps = CameraUtils.chooseFps(params, 1.0f, 120.0f);
					mRotation = CameraUtils.setupRotation(cameraId, this, mCamera, params);
					mCamera.setParameters(params);
					// get the actual preview size
					final Camera.Size previewSize = mCamera.getParameters().getPreviewSize();
					Log.d(TAG, String.format("handleStartPreview(%d, %d),fps(%d-%d)",
						previewSize.width, previewSize.height, fps[0], fps[1]));
					CameraUtils.setPreviewSurface(mCamera, this);
				} catch (final IOException e) {
					Log.w(TAG, e);
					mCamera = null;
				}
			}
			if (mCamera != null) {
				mCamera.startPreview();
			}
		}
	}

	private void stopPreview() {
		if (mCamera != null) {
			mCamera.stopPreview();
			mCamera = null;
		}
	}

}
