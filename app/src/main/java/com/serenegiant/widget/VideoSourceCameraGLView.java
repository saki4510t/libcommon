package com.serenegiant.widget;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2020 saki t_saki@serenegiant.com
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
import android.opengl.Matrix;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Choreographer;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.serenegiant.glpipeline.Distributor;
import com.serenegiant.glpipeline.IPipelineSource;
import com.serenegiant.glpipeline.VideoSource;
import com.serenegiant.glutils.GLContext;
import com.serenegiant.glutils.GLDrawer2D;
import com.serenegiant.glutils.GLManager;
import com.serenegiant.glutils.ISurface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import static com.serenegiant.widget.CameraDelegator.*;

/**
 * カメラ映像をVideoSource経由で取得してプレビュー表示するためのICameraGLView実装
 * SurfaceViewを継承
 * XXX useSharedContext = trueで共有コンテキストを使ったマルチスレッド処理を有効にするとGPUのドライバー内でクラッシュする端末がある
 */
public class VideoSourceCameraGLView
	extends SurfaceView implements ICameraGLView {

	private static final boolean DEBUG = true; // TODO set false on release
	private static final String TAG = VideoSourceCameraGLView.class.getSimpleName();

	/**
	 * 共有GLコンテキストコンテキストを使ったマルチスレッド処理を行うかどうか
	 */
	private static final boolean USE_SHARED_CONTEXT = false;

	@NonNull
	private final CameraDelegator mCameraDelegator;
	@NonNull
	private final GLManager mGLManager;
	@NonNull
	private final GLContext mGLContext;
	@NonNull
	private final Handler mGLHandler;
	@NonNull
	private final CameraRenderer mCameraRenderer;

	@Nullable
	private VideoSource mVideoSource;
	@Nullable
	private Distributor mDistributor;

	/**
	 * コンストラクタ
	 * @param context
	 */
	public VideoSourceCameraGLView(final Context context) {
		this(context, null, 0);
	}

	/**
	 * コンストラクタ
	 * @param context
	 * @param attrs
	 */
	public VideoSourceCameraGLView(final Context context, final AttributeSet attrs) {
		this(context, attrs, 0);
	}

	/**
	 * コンストラクタ
	 * @param context
	 * @param attrs
	 * @param defStyle
	 */
	public VideoSourceCameraGLView(final Context context, final AttributeSet attrs, final int defStyle) {
		super(context, attrs);
		if (DEBUG) Log.v(TAG, "コンストラクタ:");
		mGLManager = new GLManager();
		mGLContext = mGLManager.getGLContext();
		mGLHandler = mGLManager.getGLHandler();
		mCameraRenderer = new CameraRenderer();
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
				return mCameraRenderer;
			}
		};
		final SurfaceHolder holder = getHolder();
		holder.addCallback(new SurfaceHolder.Callback() {
			@Override
			public void surfaceCreated(final SurfaceHolder holder) {
				if (DEBUG) Log.v(TAG, "surfaceCreated:");
				// do nothing
				queueEvent(new Runnable() {
					@Override
					public void run() {
						mCameraRenderer.onSurfaceCreated(holder.getSurface());
					}
				});
			}

			@Override
			public void surfaceChanged(final SurfaceHolder holder, final int format, final int width, final int height) {
				// do nothing
				if (DEBUG) Log.v(TAG, "surfaceChanged:");
				queueEvent(new Runnable() {
					@Override
					public void run() {
						mCameraRenderer.onSurfaceChanged(width, height);
					}
				});
			}

			@Override
			public void surfaceDestroyed(final SurfaceHolder holder) {
				if (DEBUG) Log.v(TAG, "surfaceDestroyed:");
				queueEvent(new Runnable() {
					@Override
					public void run() {
						mCameraRenderer.onSurfaceDestroyed();
					}
				});
			}
		});
	}

	@Override
	public synchronized void onResume() {
		if (DEBUG) Log.v(TAG, "onResume:");
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
	}

	@Override
	public void queueEvent(final Runnable task) {
		mGLHandler.post(task);
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
	 * Choreographerを使ってテクスチャの描画をするかどうか
	 * XXX Choreographerを使うと数百フレームぐらいでlibGLESv2_adreno.so内でSIGSEGV投げてクラッシュする
	 */
	private static final boolean USE_CHOREOGRAPHER = false;

	/**
	 * ICameraRendererの実装
	 */
	private class CameraRenderer implements ICameraRenderer, Runnable,
		Choreographer.FrameCallback, IPipelineSource.OnFrameAvailableListener {

		private ISurface mTarget;
		private GLDrawer2D mDrawer;
		private final float[] mMvpMatrix = new float[16];
		private volatile boolean mHasSurface;

		/**
		 * コンストラクタ
		 */
		public CameraRenderer() {
			if (DEBUG) Log.v(TAG, "CameraRenderer:");
			Matrix.setIdentityM(mMvpMatrix, 0);
		}

		@WorkerThread
		private void onSurfaceCreated(@NonNull final Surface surface) {
			if (DEBUG) Log.v(TAG, "CameraRenderer#onSurfaceCreated:" + surface);
			GLES20.glClearColor(1.0f, 1.0f, 0.0f, 1.0f);
			// This renderer required OES_EGL_image_external extension
			final String extensions = GLES20.glGetString(GLES20.GL_EXTENSIONS);	// API >= 8
//			if (DEBUG) Log.i(TAG, "onSurfaceCreated:Gl extensions: " + extensions);
			if (!extensions.contains("OES_EGL_image_external"))
				throw new RuntimeException("This system does not support OES_EGL_image_external.");
			mDrawer = GLDrawer2D.create(mGLContext.isGLES3(), true);
			// clear screen with yellow color so that you can see rendering rectangle
			// create object for preview display
			mDrawer.setMvpMatrix(mMvpMatrix, 0);
			mTarget = mGLManager.getEgl().createFromSurface(surface);
			mHasSurface = true;
			GLES20.glClearColor(1.0f, 1.0f, 0.0f, 1.0f);
			if (USE_CHOREOGRAPHER) {
				mGLManager.postFrameCallbackDelayed(this, 3);
			} else {
				mVideoSource.add(this);
			}
		}

		@WorkerThread
		private void onSurfaceChanged(final int width, final int height) {
			if (DEBUG) Log.v(TAG, String.format("CameraRenderer#onSurfaceChanged:(%d,%d)", width, height));
			// if at least with or height is zero, initialization of this view is still progress.
			if ((width == 0) || (height == 0)) return;
			mVideoSource.resize(width, height);
			updateViewport();
			mCameraDelegator.startPreview(width, height);
		}

		/**
		 * when GLSurface context is soon destroyed
		 */
		@WorkerThread
		@Override
		public void onSurfaceDestroyed() {
			if (DEBUG) Log.v(TAG, "CameraRenderer#onSurfaceDestroyed:");
			mHasSurface = false;
			mGLManager.removeFrameCallback(this);
			if (mTarget != null) {
				mTarget.release();
				mTarget = null;
			}
			if (mVideoSource != null) {
				mVideoSource.remove(this);
			}
			release();
		}

		@Override
		public boolean hasSurface() {
			return false;
		}

		@Override
		public SurfaceTexture getInputSurfaceTexture() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void onPreviewSizeChanged(final int width, final int height) {
			mVideoSource.resize(width, height);
		}

		public void release() {
			if (DEBUG) Log.v(TAG, "CameraRenderer#release:");
			if (mDrawer != null) {
				mDrawer.release();
				mDrawer = null;
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
			if (!mHasSurface || (mTarget == null)) {
				if (DEBUG) Log.v(TAG, "updateViewport:has no surface");
				return;
			}
			mTarget.makeCurrent();
			mTarget.setViewPort(0, 0, viewWidth, viewHeight);
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
				Log.i(TAG, String.format("updateViewport;xy(%d,%d),size(%d,%d)", x, y, width, height));
				mTarget.setViewPort(0, 0, width, height);
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

			mTarget.swap();
		}

//		private int cnt;
		private int cnt2;

		/**
		 * IPipelineSource.OnFrameAvailableListenerの実装
		 * @param texId
		 * @param texMatrix
		 */
		@Override
		public void onFrameAvailable(final int texId, @NonNull final float[] texMatrix) {
//			if (DEBUG && ((++cnt % 100) == 0)) Log.v(TAG, "onFrameAvailable::" + cnt);
			if (!USE_CHOREOGRAPHER) {
				mGLHandler.post(this);
			}
		}

		/**
		 * Choreographer.FrameCallbackの実装
		 * @param frameTimeNanos
		 */
		@Override
		public void doFrame(final long frameTimeNanos) {
//			if (DEBUG && ((++cnt % 100) == 0)) Log.v(TAG, "doFrame::" + cnt);
			run();
			Choreographer.getInstance().postFrameCallbackDelayed(this, 3);
		}

		/**
		 * このViewが保持しているレンダリングスレッド上で描画処理を実行するためのRunnableの実装
		 */
		@WorkerThread
		@Override
		public void run() {
			if (mHasSurface && (mVideoSource != null)) {
				handleDraw(mVideoSource.getTexId(), mVideoSource.getTexMatrix());
			}
			mGLContext.makeDefault();
			GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
			GLES20.glFlush();
		}

		/**
		 * 描画処理の実体
		 * レンダリングスレッド上で実行
		 * @param texId
		 * @param texMatrix
		 */
		@WorkerThread
		private void handleDraw(final int texId, @NonNull final float[] texMatrix) {
			if (mTarget != null) {
				if (DEBUG && ((++cnt2 % 100) == 0)) Log.v(TAG, "handleDraw:" + cnt2);
				mTarget.makeCurrent();
				GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
				// draw to preview screen
				if ((mDrawer != null) && (mVideoSource != null)) {
					mDrawer.draw(texId, texMatrix, 0);
				}
				GLES20.glFlush();
				mTarget.swap();
			}
		}

	}	// CameraRenderer
}
