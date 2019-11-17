package com.serenegiant.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;

import com.serenegiant.utils.BuildCheck;
import com.serenegiant.utils.HandlerThreadHandler;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

/**
 * カメラプレビュー処理の委譲クラス
 */
public abstract class CameraDelegator {
	private static final boolean DEBUG = true; // TODO set false on release
	private static final String TAG = CameraDelegator.class.getSimpleName();

	public static final int SCALE_STRETCH_FIT = 0;
	public static final int SCALE_KEEP_ASPECT_VIEWPORT = 1;
	public static final int SCALE_KEEP_ASPECT = 2;
	public static final int SCALE_CROP_CENTER = 3;

	public static final int DEFAULT_PREVIEW_WIDTH = 1280;
	public static final int DEFAULT_PREVIEW_HEIGHT = 720;

	private static final int TARGET_FPS_MS = 60 * 1000;
	private static final int CAMERA_ID = 0;

	public interface OnFrameAvailableListener {
		public void onFrameAvailable();
	}

	/**
	 * カメラ映像をGLSurfaceViewへ描画するためのGLSurfaceView.Rendererインターフェース
	 */
	public interface ICameraRenderer {
		public void onSurfaceDestroyed();
		public boolean hasSurface();
		public void updateViewport();
		public SurfaceTexture getInputSurfaceTexture();
		public void onPreviewSizeChanged(final int width, final int height);
	}

	@NonNull
	private final ICameraGLView mView;
	@NonNull
	private final Object mSync = new Object();
	@NonNull
	private final ICameraRenderer mRenderer;
	@NonNull
	private final Set<OnFrameAvailableListener> mListeners
		= new CopyOnWriteArraySet<>();
	@Nullable
	private Handler mCameraHandler;
	private int mVideoWidth, mVideoHeight;
	private int mRotation;
	private int mScaleMode = SCALE_STRETCH_FIT;
	@Nullable
	private Camera mCamera;
	private volatile boolean mResumed;

	/**
	 * コンストラクタ
	 * @param view
	 * @param width
	 * @param height
	 */
	public CameraDelegator(@NonNull final ICameraGLView view,
		final int width, final int height) {

		if (DEBUG) Log.v(TAG, String.format("コンストラクタ:(%dx%d)", width, height));
		mView = view;
		mRenderer = createCameraRenderer(this);
//		// XXX GLES30はAPI>=18以降なんだけどAPI=18でもGLコンテキスト生成に失敗する端末があるのでAP1>=21に変更
//		view.setEGLContextClientVersion((Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) ? 3 : 2);	// GLES20 API >= 8, GLES30 API>=18
//		view.setRenderer(mRenderer);
//		final SurfaceHolder holder = view.getHolder();
//		holder.addCallback(new SurfaceHolder.Callback() {
//			@Override
//			public void surfaceCreated(final SurfaceHolder holder) {
//				if (DEBUG) Log.v(TAG, "surfaceCreated:");
//				// do nothing
//			}
//
//			@Override
//			public void surfaceChanged(final SurfaceHolder holder, final int format, final int width, final int height) {
//				// do nothing
//				if (DEBUG) Log.v(TAG, "surfaceChanged:");
//				mView.queueEvent(new Runnable() {
//					@Override
//					public void run() {
//						mRenderer.updateViewport();
//					}
//				});
//			}
//
//			@Override
//			public void surfaceDestroyed(final SurfaceHolder holder) {
//				if (DEBUG) Log.v(TAG, "surfaceDestroyed:");
//				mRenderer.onSurfaceDestroyed();
//			}
//		});
		mVideoWidth = width;
		mVideoHeight = height;
	}

	@Override
	protected void finalize() throws Throwable {
		try {
			release();
		} finally {
			super.finalize();
		}
	}

	/**
	 * 関連するリソースを廃棄する
	 */
	public void release() {
		synchronized (mSync) {
			if (mCameraHandler != null) {
				if (DEBUG) Log.v(TAG, "release:");
				mCameraHandler.removeCallbacksAndMessages(null);
				mCameraHandler.getLooper().quit();
				mCameraHandler = null;
			}
		}
	}

	/**
	 * GLSurfaceView#onResumeが呼ばれたときの処理
	 */
	public void onResume() {
		if (DEBUG) Log.v(TAG, "onResume:");
		mResumed = true;
		if (mRenderer.hasSurface()) {
			if (mCameraHandler == null) {
				if (DEBUG) Log.v(TAG, "surface already exist");
				startPreview(mView.getWidth(),  mView.getHeight());
			}
		}
	}

	/**
	 * GLSurfaceView#onPauseが呼ばれたときの処理
	 */
	public void onPause() {
		if (DEBUG) Log.v(TAG, "onPause:");
		mResumed = false;
		// just request stop previewing
		stopPreview();
	}

	@NonNull
	public ICameraRenderer getCameraRenderer() {
		return mRenderer;
	}

	/**
	 * 映像が更新されたときのコールバックリスナーを登録
	 * @param listener
	 */
	public void addListener(final OnFrameAvailableListener listener) {
		if (DEBUG) Log.v(TAG, "addListener:" + listener);
		if (listener != null) {
			mListeners.add(listener);
		}
	}

	/**
	 * 映像が更新されたときのコールバックリスナーの登録を解除
	 * @param listener
	 */
	public void removeListener(final OnFrameAvailableListener listener) {
		if (DEBUG) Log.v(TAG, "removeListener:" + listener);
		mListeners.remove(listener);
	}

	/**
	 * 映像が更新されたときのコールバックを呼び出す
	 */
	public void callOnFrameAvailable() {
		for (final OnFrameAvailableListener listener: mListeners) {
			try {
				listener.onFrameAvailable();
			} catch (final Exception e) {
				mListeners.remove(listener);
			}
		}
	}

	/**
	 * スケールモードをセット
	 * @param mode
	 */
	public void setScaleMode(final int mode) {
		if (DEBUG) Log.v(TAG, "setScaleMode:" + mode);
		if (mScaleMode != mode) {
			mScaleMode = mode;
			mView.queueEvent(new Runnable() {
				@Override
				public void run() {
					mRenderer.updateViewport();
				}
			});
		}
	}

	/**
	 * 現在のスケールモードを取得
	 * @return
	 */
	public int getScaleMode() {
		if (DEBUG) Log.v(TAG, "getScaleMode:" + mScaleMode);
		return mScaleMode;
	}

	/**
	 * カメラ映像サイズを変更要求
	 * @param width
	 * @param height
	 */
	@SuppressWarnings("SuspiciousNameCombination")
	public void setVideoSize(final int width, final int height) {
		if (DEBUG) Log.v(TAG, String.format("setVideoSize:(%dx%d)", width, height));
		if ((mRotation % 180) == 0) {
			mVideoWidth = width;
			mVideoHeight = height;
		} else {
			mVideoWidth = height;
			mVideoHeight = width;
		}
		mView.queueEvent(new Runnable() {
			@Override
			public void run() {
				mRenderer.updateViewport();
			}
		});
	}

	/**
	 * カメラ映像幅を取得
	 * @return
	 */
	public int getWidth() {
		return mVideoWidth;
	}

	/**
	 * カメラ映像高さを取得
	 * @return
	 */
	public int getHeight() {
		return mVideoHeight;
	}

	/**
	 * プレビュー開始
	 * @param width
	 * @param height
	 */
	public void startPreview(final int width, final int height) {
		if (DEBUG) Log.v(TAG, String.format("startPreview:(%dx%d)", width, height));
		synchronized (mSync) {
			if (mCameraHandler == null) {
				mCameraHandler = HandlerThreadHandler.createHandler("CameraHandler");
			}
			mCameraHandler.post(new Runnable() {
				@Override
				public void run() {
					handleStartPreview(width, height);
				}
			});
		}
	}

	/**
	 * プレビュー終了
	 */
	public void stopPreview() {
		if (DEBUG) Log.v(TAG, "stopPreview:" + mCamera);
		synchronized (mSync) {
			if (mCamera != null) {
				mCamera.stopPreview();
				if (mCameraHandler != null) {
					mCameraHandler.post(new Runnable() {
						@Override
						public void run() {
							handleStopPreview();
							release();
						}
					});
				}
			}
		}
	}

//--------------------------------------------------------------------------------

	/**
	 * カメラ映像受け取り用のSurfaceTextureを取得
	 * @return
	 */
	@NonNull
	protected abstract SurfaceTexture getInputSurfaceTexture();

	/**
	 * カメラ映像をGLSurfaceViewへ描画するためのICameraRenderer(GLSurfaceView.Renderer)を生成
	 * @param parent
	 * @return
	 */
	@NonNull
	protected abstract ICameraRenderer createCameraRenderer(@NonNull final CameraDelegator parent);

//--------------------------------------------------------------------------------
	/**
	 * カメラプレビュー開始の実体
	 * @param width
	 * @param height
	 */
	@WorkerThread
	private final void handleStartPreview(final int width, final int height) {
		if (DEBUG) Log.v(TAG, "CameraThread#handleStartPreview:");
		Camera camera;
		synchronized (mSync) {
			camera = mCamera;
		}
		if (camera == null) {
			// This is a sample project so just use 0 as camera ID.
			// it is better to selecting camera is available
			try {
				camera = Camera.open(CAMERA_ID);
				final Camera.Parameters params = camera.getParameters();
				final List<String> focusModes = params.getSupportedFocusModes();
				if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
					params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
				} else if(focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
					params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
				} else {
					if (DEBUG) Log.i(TAG, "handleStartPreview:Camera does not support autofocus");
				}
				// let's try fastest frame rate. You will get near 60fps, but your device become hot.
				final List<int[]> supportedFpsRange = params.getSupportedPreviewFpsRange();
				final int n = supportedFpsRange != null ? supportedFpsRange.size() : 0;
				int[] max_fps = null;
				for (int i = n - 1; i >= 0; i--) {
					final int[] range = supportedFpsRange.get(i);
					Log.i(TAG, String.format("handleStartPreview:supportedFpsRange(%d)=(%d,%d)", i, range[0], range[1]));
					if ((range[0] <= TARGET_FPS_MS) && (TARGET_FPS_MS <= range[1])) {
						max_fps = range;
						break;
					}
				}
				if (max_fps == null) {
					// 見つからなかったときは一番早いフレームレートを選択
					max_fps = supportedFpsRange.get(supportedFpsRange.size() - 1);
				}
				Log.i(TAG, String.format("handleStartPreview:found fps:%d-%d", max_fps[0], max_fps[1]));
				params.setPreviewFpsRange(max_fps[0], max_fps[1]);
				params.setRecordingHint(true);
				// request closest supported preview size
				final Camera.Size closestSize = getClosestSupportedSize(
					params.getSupportedPreviewSizes(), width, height);
				params.setPreviewSize(closestSize.width, closestSize.height);
				// request closest picture size for an aspect ratio issue on Nexus7
				final Camera.Size pictureSize = getClosestSupportedSize(
					params.getSupportedPictureSizes(), width, height);
				params.setPictureSize(pictureSize.width, pictureSize.height);
				// rotate camera preview according to the device orientation
				setRotation(camera, params);
				camera.setParameters(params);
				// get the actual preview size
				final Camera.Size previewSize = camera.getParameters().getPreviewSize();
				Log.i(TAG, String.format("handleStartPreview(%d, %d)", previewSize.width, previewSize.height));
				// adjust view size with keeping the aspect ration of camera preview.
				// here is not a UI thread and we should request parent view to execute.
				mView.post(new Runnable() {
					@Override
					public void run() {
						setVideoSize(previewSize.width, previewSize.height);
						mRenderer.onPreviewSizeChanged(previewSize.width, previewSize.height);
					}
				});
				// カメラ映像受け取り用SurfaceTextureをセット
				final SurfaceTexture st = getInputSurfaceTexture();
				st.setDefaultBufferSize(previewSize.width, previewSize.height);
				camera.setPreviewTexture(st);
			} catch (final IOException e) {
				Log.e(TAG, "handleStartPreview:", e);
				if (camera != null) {
					camera.release();
					camera = null;
				}
			} catch (final RuntimeException e) {
				Log.e(TAG, "handleStartPreview:", e);
				if (camera != null) {
					camera.release();
					camera = null;
				}
			}
			if (camera != null) {
				// start camera preview display
				camera.startPreview();
			}
			synchronized (mSync) {
				mCamera = camera;
			}
		}
	}

	/**
	 * カメラが対応する解像度一覧から指定した解像度順に一番近いものを選んで返す
	 * @param supportedSizes
	 * @param requestedWidth
	 * @param requestedHeight
	 * @return
	 */
	private static Camera.Size getClosestSupportedSize(
		@NonNull final List<Camera.Size> supportedSizes,
		final int requestedWidth, final int requestedHeight) {

		return Collections.min(supportedSizes, new Comparator<Camera.Size>() {

			private int diff(final Camera.Size size) {
				return Math.abs(requestedWidth - size.width)
					+ Math.abs(requestedHeight - size.height);
			}

			@Override
			public int compare(final Camera.Size lhs, final Camera.Size rhs) {
				return diff(lhs) - diff(rhs);
			}
		});

	}

	/**
	 * カメラプレビュー終了の実体
	 */
	@WorkerThread
	private void handleStopPreview() {
		if (DEBUG) Log.v(TAG, "CameraThread#handleStopPreview:");
		synchronized (mSync) {
			if (mCamera != null) {
				mCamera.stopPreview();
				mCamera.release();
				mCamera = null;
			}
		}
	}

	/**
	 * 端末の画面の向きに合わせてプレビュー画面を回転させる
	 * @param params
	 */
	@SuppressLint("NewApi")
	private final void setRotation(@NonNull final Camera camera,
		@NonNull final Camera.Parameters params) {

		if (DEBUG) Log.v(TAG, "CameraThread#setRotation:");

		final View view = (View)mView;
		final int rotation;
		if (BuildCheck.isAPI17()) {
			rotation = view.getDisplay().getRotation();
		} else {
			final Display display = ((WindowManager)view.getContext()
				.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
			rotation = display.getRotation();
		}
		int degrees;
		switch (rotation) {
		case Surface.ROTATION_90:
			degrees = 90;
			break;
		case Surface.ROTATION_180:
			degrees = 180;
			break;
		case Surface.ROTATION_270:
			degrees = 270;
			break;
		case Surface.ROTATION_0:
		default:
			degrees = 0;
			break;
		}
		// get whether the camera is front camera or back camera
		final Camera.CameraInfo info = new Camera.CameraInfo();
		Camera.getCameraInfo(CAMERA_ID, info);
		if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {	// front camera
			degrees = (info.orientation + degrees) % 360;
			degrees = (360 - degrees) % 360;  // reverse
		} else {  // back camera
			degrees = (info.orientation - degrees + 360) % 360;
		}
		// apply rotation setting
		camera.setDisplayOrientation(degrees);
		mRotation = degrees;
		// XXX This method fails to call and camera stops working on some devices.
//		params.setRotation(degrees);
	}

}
