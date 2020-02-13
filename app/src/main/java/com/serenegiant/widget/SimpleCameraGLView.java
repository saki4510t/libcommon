package com.serenegiant.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;

import com.serenegiant.glutils.GLDrawer2D;
import com.serenegiant.graphics.MatrixUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;

public class SimpleCameraGLView extends AspectScaledGLView
	implements CameraDelegator.ICameraView {

	private static final boolean DEBUG = true;	// set false on production
	private static final String TAG = SimpleCameraGLView.class.getSimpleName();

	private final Object mSync = new Object();
	private final CameraDelegator mCameraDelegator;
	private final float[] mTexMatrix = new float[16];

	private GLDrawer2D mDrawer;
	private int mTexId;
	private SurfaceTexture mSurfaceTexture;
	private Surface mSurface;
	private volatile boolean mRequestUpdateTex;

	/**
	 * コンストラクタ
	 * @param context
	 */
	public SimpleCameraGLView(@Nullable final Context context) {
		this(context, null, 0);
	}

	/**
	 * コンストラクタ
	 * @param context
	 * @param attrs
	 */
	public SimpleCameraGLView(@Nullable final Context context,
		@Nullable final AttributeSet attrs) {

		this(context, attrs, 0);
	}

	/**
	 * コンストラクタ
	 * @param context
	 * @param attrs
	 * @param defStyle
	 */
	public SimpleCameraGLView(@Nullable final Context context,
		@Nullable final AttributeSet attrs, final int defStyle) {

		super(context, attrs, defStyle);
		if (DEBUG) Log.v(TAG, "コンストラクタ:");
		mCameraDelegator = new CameraDelegator(this,
			CameraDelegator.DEFAULT_PREVIEW_WIDTH, CameraDelegator.DEFAULT_PREVIEW_HEIGHT,
			new CameraDelegator.ICameraRenderer() {
				@Override
				public boolean hasSurface() {
					if (DEBUG) Log.v(TAG, "hasSurface:" + mSurfaceTexture);
					synchronized (mSync) {
						return mSurfaceTexture != null;
					}
				}

				@Override
				public void updateViewport() {
					queueEvent(new Runnable() {
						@Override
						public void run() {
							SimpleCameraGLView.this.updateViewport();
						}
					});
				}

				@Override
				public void onPreviewSizeChanged(final int width, final int height) {
					if (DEBUG) Log.v(TAG, String.format("onPreviewSizeChanged:(%dx%dx)", width, height));
					setAspectRatio(width, height);
					synchronized (mSync) {
						if (mSurfaceTexture != null) {
							mSurfaceTexture.setDefaultBufferSize(width, height);
						}
					}
				}

				@NonNull
				@Override
				public SurfaceTexture getInputSurface() {
					if (DEBUG) Log.v(TAG, "getInputSurfaceTexture:" + mSurfaceTexture);
					synchronized (mSync) {
						return mSurfaceTexture;
					}
				}
			}
		);
		setRenderer(new GLRenderer() {
			private int cnt;
			@Override
			public void onSurfaceCreated() {
				if (DEBUG) Log.v(TAG, "onSurfaceCreated:");
				synchronized (mSync) {
					mDrawer = GLDrawer2D.create(isOES3(), true);
					mTexId = mDrawer.initTex();
					mSurfaceTexture = new SurfaceTexture(mTexId);
					mSurfaceTexture.setDefaultBufferSize(
						CameraDelegator.DEFAULT_PREVIEW_WIDTH, CameraDelegator.DEFAULT_PREVIEW_HEIGHT);
					mSurfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
		//				private int cnt;
						@Override
						public void onFrameAvailable(final SurfaceTexture surfaceTexture) {
		//					if (DEBUG && ((++cnt % 100) == 0)) Log.v(TAG, "onFrameAvailable:" + cnt);
							mRequestUpdateTex = true;
						}
					});
				}
			}

			@Override
			public void onSurfaceChanged(final int format, final int width, final int height) {
				if (DEBUG) Log.v(TAG, String.format("onSurfaceChanged:(%dx%d))", width, height));
				mCameraDelegator.startPreview(
					CameraDelegator.DEFAULT_PREVIEW_WIDTH, CameraDelegator.DEFAULT_PREVIEW_HEIGHT);
			}

			@Override
			public void drawFrame() {
				if (DEBUG && ((++cnt % 1800) == 0)) Log.v(TAG, "drawFrame:" + cnt);
				if (mRequestUpdateTex) {
					mRequestUpdateTex = false;
					mSurfaceTexture.updateTexImage();
					mSurfaceTexture.getTransformMatrix(mTexMatrix);
				}
				mDrawer.draw(mTexId, mTexMatrix, 0);
			}

			@SuppressLint("NewApi")
			@Override
			public void onSurfaceDestroyed() {
				if (DEBUG) Log.v(TAG, "onSurfaceDestroyed:");
				synchronized (mSync) {
					if (mSurfaceTexture != null) {
						mSurfaceTexture.release();
						mSurfaceTexture = null;
					}
					if (mTexId != 0) {
						if (isGLES3()) {
							com.serenegiant.glutils.es3.GLHelper.deleteTex(mTexId);
						} else {
							com.serenegiant.glutils.es2.GLHelper.deleteTex(mTexId);
						}
						mTexId = 0;
					}
					if (mDrawer != null) {
						mDrawer.release();
						mDrawer = null;
					}
				}
			}

			@Override
			public void applyTransformMatrix(@NonNull @Size(min=16) final float[] transform) {
				if (mDrawer != null) {
					if (DEBUG) Log.v(TAG, "applyTransformMatrix:"
						+ MatrixUtils.toGLMatrixString(transform));
					mDrawer.setMvpMatrix(transform, 0);
				}
			}
		});
	}

//--------------------------------------------------------------------------------
// ICameraViewの実装
	/**
	 * ICameraViewの実装
	 */
	@Override
	public void onResume() {
		if (DEBUG) Log.v(TAG, "onResume:");
		mCameraDelegator.onResume();
	}

	/**
	 * ICameraViewの実装
	 */
	@Override
	public void onPause() {
		if (DEBUG) Log.v(TAG, "onPause:");
		mCameraDelegator.onPause();
	}

	/**
	 * ICameraViewの実装
	 * @param width
	 * @param height
	 */
	@Override
	public void setVideoSize(final int width, final int height) {
		if (DEBUG) Log.v(TAG, String.format("setVideoSize:(%dx%d)", width, height));
		mCameraDelegator.setVideoSize(width, height);
		setAspectRatio(width, height);
	}

	/**
	 * ICameraViewの実装
	 * @param listener
	 */
	@Override
	public void addListener(@NonNull final CameraDelegator.OnFrameAvailableListener listener) {
		if (DEBUG) Log.v(TAG, "addListener:");
	}

	/**
	 * ICameraViewの実装
	 * @param listener
	 */
	@Override
	public void removeListener(@NonNull final CameraDelegator.OnFrameAvailableListener listener) {
		if (DEBUG) Log.v(TAG, "removeListener:");
	}

	/**
	 * ICameraViewの実装
	 * @return
	 */
	@Override
	public int getVideoWidth() {
		if (DEBUG) Log.v(TAG, "getVideoWidth:");
		return mCameraDelegator.getWidth();
	}

	/**
	 * ICameraViewの実装
	 * @return
	 */
	@Override
	public int getVideoHeight() {
		if (DEBUG) Log.v(TAG, "getVideoHeight:");
		return mCameraDelegator.getHeight();
	}

	protected void updateViewport() {
//		final int viewWidth = getWidth();
//		final int viewHeight = getHeight();
//		if (viewWidth == 0 || viewHeight == 0) {
//			if (DEBUG) Log.v(TAG, String.format("updateViewport:view is not ready(%dx%d)", viewWidth, viewHeight));
//			return;
//		}
//		final double videoWidth = mCameraDelegator.getWidth();
//		final double videoHeight = mCameraDelegator.getHeight();
//		if (videoWidth == 0 || videoHeight == 0) {
//			if (DEBUG) Log.v(TAG, String.format("updateViewport:video is not ready(%dx%d)", viewWidth, viewHeight));
//			return;
//		}
//		final double viewAspect = viewWidth / (double)viewHeight;
//		Log.i(TAG, String.format("updateViewport:view(%d,%d)%f,video(%1.0f,%1.0f)",
//			viewWidth, viewHeight, viewAspect, videoWidth, videoHeight));
//
//		android.opengl.Matrix.setIdentityM(mMvpMatrix, 0);
//		final int scaleMode = mCameraDelegator.getScaleMode();
//		switch (scaleMode) {
//		case CameraDelegator.SCALE_STRETCH_FIT:
//			break;
//		case CameraDelegator.SCALE_KEEP_ASPECT_VIEWPORT:
//		{
//			final double req = videoWidth / videoHeight;
//			int x, y;
//			int width, height;
//			if (viewAspect > req) {
//				// if view is wider than camera image, calc width of drawing area based on view height
//				y = 0;
//				height = viewHeight;
//				width = (int)(req * viewHeight);
//				x = (viewWidth - width) / 2;
//			} else {
//				// if view is higher than camera image, calc height of drawing area based on view width
//				x = 0;
//				width = viewWidth;
//				height = (int)(viewWidth / req);
//				y = (viewHeight - height) / 2;
//			}
//			// set viewport to draw keeping aspect ration of camera image
//			Log.i(TAG, String.format("updateViewport;xy(%d,%d),size(%d,%d)", x, y, width, height));
////			mTarget.setViewPort(0, 0, width, height);
//			break;
//		}
//		case CameraDelegator.SCALE_KEEP_ASPECT:
//		case CameraDelegator.SCALE_CROP_CENTER:
//		{
//			final double scale_x = viewWidth / videoWidth;
//			final double scale_y = viewHeight / videoHeight;
//			final double scale = (scaleMode == CameraDelegator.SCALE_CROP_CENTER
//				? Math.max(scale_x,  scale_y) : Math.min(scale_x, scale_y));
//			final double width = scale * videoWidth;
//			final double height = scale * videoHeight;
//			Log.i(TAG, String.format("updateViewport:size(%1.0f,%1.0f),scale(%f,%f),mat(%f,%f)",
//				width, height, scale_x, scale_y, width / viewWidth, height / viewHeight));
//			android.opengl.Matrix.scaleM(mMvpMatrix, 0, (float)(width / viewWidth), (float)(height / viewHeight), 1.0f);
//			break;
//		}
//		}
	}


}
