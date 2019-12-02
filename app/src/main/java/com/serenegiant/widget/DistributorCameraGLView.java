package com.serenegiant.widget;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2019 saki t_saki@serenegiant.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
*/

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.serenegiant.glpipeline.Distributor;
import com.serenegiant.glpipeline.IPipelineSource;
import com.serenegiant.glpipeline.VideoSource;
import com.serenegiant.glutils.GLDrawer2D;
import com.serenegiant.glutils.GLManager;
import com.serenegiant.utils.HandlerThreadHandler;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static com.serenegiant.widget.CameraDelegator.*;

/**
 * カメラ映像をVideoSourceとDistributor経由で取得してプレビュー表示するためのGLSurfaceView実装
 * XXX useSharedContext = trueで共有コンテキストを使ったマルチスレッド処理を有効にするとGPUのドライバー内でクラッシュする端末がある
 */
public class DistributorCameraGLView
	extends GLSurfaceView implements ICameraGLView {

	private static final boolean DEBUG = true; // TODO set false on release
	private static final String TAG = DistributorCameraGLView.class.getSimpleName();

	/**
	 * 共有GLコンテキストコンテキストを使ったマルチスレッド処理を行うかどうか
	 */
	private static final boolean USE_SHARED_CONTEXT = false;

	private final int mGLVersion;
	@NonNull
	private final CameraDelegator mCameraDelegator;
	@NonNull
	private final GLManager mGLManager;

	@Nullable
	private VideoSource mVideoSource;
	@Nullable
	private Distributor mDistributor;

	/**
	 * コンストラクタ
	 * @param context
	 */
	public DistributorCameraGLView(final Context context) {
		this(context, null, 0);
	}

	/**
	 * コンストラクタ
	 * @param context
	 * @param attrs
	 */
	public DistributorCameraGLView(final Context context, final AttributeSet attrs) {
		this(context, attrs, 0);
	}

	/**
	 * コンストラクタ
	 * @param context
	 * @param attrs
	 * @param defStyle
	 */
	public DistributorCameraGLView(final Context context, final AttributeSet attrs, final int defStyle) {
		super(context, attrs);
		if (DEBUG) Log.v(TAG, "コンストラクタ:");
		// XXX GLES30はAPI>=18以降なんだけどAPI=18でもGLコンテキスト生成に失敗する端末があるのでAP1>=21に変更
		mGLVersion = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) ? 3 : 2;	// GLES20 API >= 8, GLES30 API>=18
		mGLManager = new GLManager(mGLVersion);
		mCameraDelegator = new CameraDelegator(this,
			DEFAULT_PREVIEW_WIDTH, DEFAULT_PREVIEW_HEIGHT) {

			@NonNull
			@Override
			protected SurfaceTexture getInputSurfaceTexture() {
				if (DEBUG) Log.v(TAG, "getInputSurfaceTexture:");
				if (mVideoSource == null) {
					throw new IllegalStateException();
				}
				return mVideoSource.getInputSurfaceTexture();
			}

			@NonNull
			@Override
			protected ICameraRenderer createCameraRenderer(@NonNull final CameraDelegator parent) {
				if (DEBUG) Log.v(TAG, "createCameraRenderer:");
				return new CameraRenderer();
			}
		};

		setEGLContextClientVersion(mGLVersion);
		setRenderer((CameraRenderer)mCameraDelegator.getCameraRenderer());
		final SurfaceHolder holder = getHolder();
		holder.addCallback(new SurfaceHolder.Callback() {
			@Override
			public void surfaceCreated(final SurfaceHolder holder) {
				if (DEBUG) Log.v(TAG, "surfaceCreated:");
				// do nothing
			}

			@Override
			public void surfaceChanged(final SurfaceHolder holder, final int format, final int width, final int height) {
				// do nothing
				if (DEBUG) Log.v(TAG, "surfaceChanged:");
				queueEvent(new Runnable() {
					@Override
					public void run() {
						mCameraDelegator.getCameraRenderer().updateViewport();
					}
				});
			}

			@Override
			public void surfaceDestroyed(final SurfaceHolder holder) {
				if (DEBUG) Log.v(TAG, "surfaceDestroyed:");
				mCameraDelegator.getCameraRenderer().onSurfaceDestroyed();
			}
		});
	}

	@Override
	public synchronized void onResume() {
		if (DEBUG) Log.v(TAG, "onResume:");
		super.onResume();
		mVideoSource = createVideoSource(DEFAULT_PREVIEW_WIDTH, DEFAULT_PREVIEW_HEIGHT);
		mCameraDelegator.onResume();
	}

	@Override
	public synchronized void onPause() {
		if (DEBUG) Log.v(TAG, "onPause:");
		mCameraDelegator.onPause();
		if (mDistributor != null) {
			mDistributor.release();
			mDistributor = null;
		}
		if (mVideoSource != null) {
			mVideoSource.release();
			mVideoSource = null;
		}
		super.onPause();
	}

	@Override
	public void addListener(final OnFrameAvailableListener listener) {
		mCameraDelegator.addListener(listener);
	}

	@Override
	public void removeListener(final OnFrameAvailableListener listener) {
		mCameraDelegator.removeListener(listener);
	}

	@Override
	public void setScaleMode(final int mode) {
		mCameraDelegator.setScaleMode(mode);
	}

	@Override
	public int getScaleMode() {
		return mCameraDelegator.getScaleMode();
	}

	@Override
	public void setVideoSize(final int width, final int height) {
		mCameraDelegator.setVideoSize(width, height);
	}

	@Override
	public int getVideoWidth() {
		return mCameraDelegator.getWidth();
	}

	@Override
	public int getVideoHeight() {
		return mCameraDelegator.getHeight();
	}

	/**
	 * プレビュー表示用Surfaceを追加
	 * @param id
	 * @param surface
	 * @param isRecordable
	 */
	@Override
	public synchronized void addSurface(final int id, final Object surface,
		final boolean isRecordable) {

		if (DEBUG) Log.v(TAG, "addSurface:" + id);
		if (mDistributor == null) {
			mDistributor = new Distributor(mVideoSource);
		}
		mDistributor.addSurface(id, surface, isRecordable);
	}

	/**
	 * プレビュー表示用Surfaceを除去
	 * @param id
	 */
	@Override
	public synchronized void removeSurface(final int id) {
		if (DEBUG) Log.v(TAG, "removeSurface:" + id);
		if (mDistributor != null) {
			mDistributor.removeSurface(id);
		}
	}

	/**
	 * VideoSourceインスタンスを生成
	 * @param width
	 * @param height
	 * @return
	 */
	@NonNull
	protected VideoSource createVideoSource(
		final int width, final int height) {

		return new VideoSource(mGLManager, width, height,
			new IPipelineSource.PipelineSourceCallback() {

				@Override
				public void onCreate(@NonNull final Surface surface) {
					if (DEBUG) Log.v(TAG, "PipelineSourceCallback#onCreate:" + surface);
				}

				@Override
				public void onDestroy() {
					if (DEBUG) Log.v(TAG, "PipelineSourceCallback#onDestroy:");
				}
			}
		, USE_SHARED_CONTEXT);
	}

	/**
	 * GLSurfaceViewのRenderer
	 */
	private final class CameraRenderer
		implements ICameraRenderer, Renderer,
			SurfaceTexture.OnFrameAvailableListener {	// API >= 11

		private SurfaceTexture mSTexture;	// API >= 11
		private int hTex;
		private GLDrawer2D mDrawer;
		private final float[] mStMatrix = new float[16];
		private final float[] mMvpMatrix = new float[16];
		private boolean mHasSurface;
		private volatile boolean requestUpdateTex = false;

		public CameraRenderer() {
			if (DEBUG) Log.v(TAG, "CameraRenderer:");
			Matrix.setIdentityM(mMvpMatrix, 0);
		}

		@Override
		public void onSurfaceCreated(final GL10 unused, final EGLConfig config) {
			if (DEBUG) Log.v(TAG, "CameraRenderer#onSurfaceCreated:");
			// This renderer required OES_EGL_image_external extension
			final String extensions = GLES20.glGetString(GLES20.GL_EXTENSIONS);	// API >= 8
//			if (DEBUG) Log.i(TAG, "onSurfaceCreated:Gl extensions: " + extensions);
			if (!extensions.contains("OES_EGL_image_external"))
				throw new RuntimeException("This system does not support OES_EGL_image_external.");
			mDrawer = GLDrawer2D.create(mGLVersion == 3, true);
			// create texture ID
			hTex = mDrawer.initTex();
			// create SurfaceTexture with texture ID.
			mSTexture = new SurfaceTexture(hTex);
			mSTexture.setDefaultBufferSize(mCameraDelegator.getWidth(), mCameraDelegator.getHeight());
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				mSTexture.setOnFrameAvailableListener(this, HandlerThreadHandler.createHandler(TAG));
			} else {
				mSTexture.setOnFrameAvailableListener(this);
			}
			// clear screen with yellow color so that you can see rendering rectangle
			GLES20.glClearColor(1.0f, 1.0f, 0.0f, 1.0f);
			mHasSurface = true;
			// create object for preview display
			mDrawer.setMvpMatrix(mMvpMatrix, 0);
			addSurface(1, new Surface(mSTexture), false);
		}

		@Override
		public void onSurfaceChanged(final GL10 unused, final int width, final int height) {
			if (DEBUG) Log.v(TAG, String.format("CameraRenderer#onSurfaceChanged:(%d,%d)", width, height));
			// if at least with or height is zero, initialization of this view is still progress.
			if ((width == 0) || (height == 0)) return;
			updateViewport();
			mCameraDelegator.startPreview(width, height);
		}

		/**
		 * when GLSurface context is soon destroyed
		 */
		@Override
		public void onSurfaceDestroyed() {
			if (DEBUG) Log.v(TAG, "CameraRenderer#onSurfaceDestroyed:");
			mHasSurface = false;
			removeSurface(1);
			release();
		}

		@Override
		public boolean hasSurface() {
			return mHasSurface;
		}

		public SurfaceTexture getInputSurfaceTexture() {
			return mSTexture;
		}

		@Override
		public void onPreviewSizeChanged(final int width, final int height) {
			mVideoSource.resize(width, height);
			mDistributor.resize(width, height);
			if (mSTexture != null) {
				mSTexture.setDefaultBufferSize(width, height);
			}
		}

		public void release() {
			if (DEBUG) Log.v(TAG, "CameraRenderer#release:");
			if (mDrawer != null) {
				mDrawer.deleteTex(hTex);
				mDrawer.release();
				mDrawer = null;
			}
			if (mSTexture != null) {
				mSTexture.release();
				mSTexture = null;
			}
		}

		private int cnt;
		/**
		 * drawing to GLSurface
		 * we set renderMode to GLSurfaceView.RENDERMODE_WHEN_DIRTY,
		 * this method is only called when #requestRender is called(= when texture is required to update)
		 * if you don't set RENDERMODE_WHEN_DIRTY, this method is called at maximum 60fps
		 */
		@Override
		public void onDrawFrame(final GL10 unused) {
			if (DEBUG && ((++cnt % 100) == 0)) Log.v(TAG, "onDrawFrame::" + cnt);

			GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
			if (requestUpdateTex && (mSTexture != null)) {
				requestUpdateTex = false;
				// update texture(came from camera)
				mSTexture.updateTexImage();
				// get texture matrix
				mSTexture.getTransformMatrix(mStMatrix);
			}
			// draw to preview screen
			if (mDrawer != null) {
				mDrawer.draw(hTex, mStMatrix, 0);
			}
		}

		@Override
		public final void updateViewport() {
			final int viewWidth = getWidth();
			final int viewHeight = getHeight();
			if (viewWidth == 0 || viewHeight == 0) {
				if (DEBUG) Log.v(TAG, String.format("updateViewport:view is not ready(%dx%d)", viewWidth, viewHeight));
				return;
			}
			GLES20.glViewport(0, 0, viewWidth, viewHeight);
			GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
			final double videoWidth = mCameraDelegator.getWidth();
			final double videoHeight = mCameraDelegator.getHeight();
			if (videoWidth == 0 || videoHeight == 0) {
				if (DEBUG) Log.v(TAG, String.format("updateViewport:video is not ready(%dx%d)", viewWidth, viewHeight));
				return;
			}
			final double viewAspect = viewWidth / (double)viewHeight;
			Log.i(TAG, String.format("updateViewport:view(%d,%d)%f,video(%1.0f,%1.0f)",
				viewWidth, viewHeight, viewAspect, videoWidth, videoHeight));

			Matrix.setIdentityM(mMvpMatrix, 0);
			final int scaleMode = mCameraDelegator.getScaleMode();
			switch (scaleMode) {
			case SCALE_STRETCH_FIT:
				break;
			case SCALE_KEEP_ASPECT_VIEWPORT:
			{
				final double req = videoWidth / videoHeight;
				int x, y;
				int width, height;
				if (viewAspect > req) {
					// if view is wider than camera image, calc width of drawing area based on view height
					y = 0;
					height = viewHeight;
					width = (int)(req * viewHeight);
					x = (viewWidth - width) / 2;
				} else {
					// if view is higher than camera image, calc height of drawing area based on view width
					x = 0;
					width = viewWidth;
					height = (int)(viewWidth / req);
					y = (viewHeight - height) / 2;
				}
				// set viewport to draw keeping aspect ration of camera image
				Log.i(TAG, String.format("updateViewport:xy(%d,%d),size(%d,%d)", x, y, width, height));
				GLES20.glViewport(x, y, width, height);
				break;
			}
			case SCALE_KEEP_ASPECT:
			case SCALE_CROP_CENTER:
			{
				final double scale_x = viewWidth / videoWidth;
				final double scale_y = viewHeight / videoHeight;
				final double scale = (scaleMode == SCALE_CROP_CENTER
					? Math.max(scale_x,  scale_y) : Math.min(scale_x, scale_y));
				final double width = scale * videoWidth;
				final double height = scale * videoHeight;
				Log.i(TAG, String.format("updateViewport:size(%1.0f,%1.0f),scale(%f,%f),mat(%f,%f)",
					width, height, scale_x, scale_y, width / viewWidth, height / viewHeight));
				Matrix.scaleM(mMvpMatrix, 0, (float)(width / viewWidth), (float)(height / viewHeight), 1.0f);
				break;
			}
			}
			if (mDrawer != null) {
				mDrawer.setMvpMatrix(mMvpMatrix, 0);
			}
		}

		/**
		 * OnFrameAvailableListenerインターフェースの実装
		 * @param st
		 */
		@Override
		public void onFrameAvailable(final SurfaceTexture st) {
			requestUpdateTex = true;
		}
	}	// CameraRenderer

}
