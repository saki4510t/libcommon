package com.serenegiant.glpipeline;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2025 saki t_saki@serenegiant.com
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

import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.util.Log;
import android.view.Choreographer;
import android.view.Surface;

import com.serenegiant.gl.GLManager;
import com.serenegiant.gl.GLUtils;
import com.serenegiant.gl.GLTexture;
import com.serenegiant.math.Fraction;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;
import androidx.annotation.WorkerThread;

/**
 * 静止画(Bitmap)を映像ソースとするためのGLPipelineSource実装
 */
public class ImageSourcePipeline extends ProxyPipeline implements GLPipelineSource {
	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = ImageSourcePipeline.class.getSimpleName();

	private static final float DEFAULT_FPS = 30.0f;

	@NonNull
	private final GLManager mManager;
	@Nullable
	private GLTexture mImageSource;
	private volatile long mFrameIntervalNs;
	private volatile long mFrameIntervalMs;
	private long prevFrameTimeNs;

	/**
	 * コンストラクタ
	 * @param manager
	 * @param bitmap nullのときは後で#setSourceを呼び出さないと#onFrameAvailableが呼び出されない
	 * @param fps nullの時は30fps相当
	 */
	public ImageSourcePipeline(
		@NonNull final GLManager manager,
		@Nullable final Bitmap bitmap, @Nullable final Fraction fps) {
		super();
		if (DEBUG) Log.v(TAG, "コンストラクタ:" + bitmap);
		mManager = manager;
		if (bitmap != null) {
			mManager.runOnGLThread(new Runnable() {
				@WorkerThread
				@Override
				public void run() {
					createImageSource(bitmap, fps);
				}
			});
		}
	}

	/**
	 * GLPipelineの実装
	 * パイプラインチェーンに組み込まれているかどうかを取得
	 * @return
	 */
	@Override
	public boolean isActive() {
		mLock.lock();
		try {
			// 破棄されていない && 子と繋がっている
			return isValid() && (getPipeline() != null);
		} finally {
			mLock.unlock();
		}
	}

	@Override
	public void setParent(@Nullable final GLPipeline parent) {
		super.setParent(parent);
		throw new UnsupportedOperationException("Can't set parent to GLPipelineSource");
	}

	@Override
	protected void internalRelease() {
		if (isValid()) {
			mManager.runOnGLThread(new Runnable() {
				@Override
				public void run() {
					releaseImageSource();
				}
			});
		}
		super.internalRelease();
	}

	@NonNull
	@Override
	public GLManager getGLManager() throws IllegalStateException {
		return mManager;
	}

	/**
	 * ImageSourceでは対応していないのでUnsupportedOperationExceptionを投げる
	 * @return
	 * @throws UnsupportedOperationException
	 */
	@NonNull
	@Override
	public SurfaceTexture getInputSurfaceTexture() throws UnsupportedOperationException {
		throw new UnsupportedOperationException("");
	}

	/**
	 * ImageSourceでは対応していないのでUnsupportedOperationExceptionを投げる
	 * @return
	 * @throws UnsupportedOperationException
	 */
	@NonNull
	@Override
	public Surface getInputSurface() throws UnsupportedOperationException {
		throw new UnsupportedOperationException("");
	}

	/**
	 * テクスチャ名を取得する
	 * すでに#releaseが呼ばれたか映像ソース用のBitmapがセットされていないときはIllegalStateExceptionを投げる
	 * @return
	 * @throws IllegalStateException
	 */
	@Override
	public int getTexId() throws IllegalStateException {
		mLock.lock();
		try {
			if (!isValid() || (mImageSource == null)) {
				throw new IllegalStateException("already released or image not set yet.");
			}
			return mImageSource != null ? mImageSource.getTexId() : 0;
		} finally {
			mLock.unlock();
		}
	}

	/**
	*  テキスチャ変換行列を取得する
	 * すでに#releaseが呼ばれたか映像ソース用のBitmapがセットされていないときはIllegalStateExceptionを投げる
	 * @return
	 * @throws IllegalStateException
	 */
	@Size(min=16)
	@NonNull
	@Override
	public float[] getTexMatrix() throws IllegalStateException {
		mLock.lock();
		try {
			if (!isValid() || (mImageSource == null)) {
				throw new IllegalStateException("already released or image not set yet.");
			}
			return mImageSource.getTexMatrix();
		} finally {
			mLock.unlock();
		}
	}

	@Override
	public boolean isValid() {
		return super.isValid() && mManager.isValid();
	}

	private int cnt;
	@WorkerThread
	@Override
	public void onFrameAvailable(
		final boolean isOES, final int texId,
		@NonNull @Size(min=16) final float[] texMatrix) {

		mLock.lock();
		try {
			// 映像ソースが準備できていなければスキップする
			if (!isValid() || (mImageSource == null)) return;
		} finally {
			mLock.unlock();
		}
		if (DEBUG && (++cnt % 100) == 0) {
			Log.v(TAG, "onFrameAvailable:" + cnt);
		}
		super.onFrameAvailable(isOES, texId, texMatrix);
	}

	/**
	 * 映像ソース用のBitmapをセット
	 * @param bitmap nullのときは後で#setSourceを呼び出さないと#onFrameAvailableが呼び出されない
	 * @param fps
	 */
	public void setSource(@Nullable final Bitmap bitmap, @Nullable final Fraction fps) {
		mManager.runOnGLThread(new Runnable() {
			@Override
			public void run() {
				mLock.lock();
				try {
					if (bitmap == null) {
						releaseImageSource();
					} else {
						createImageSource(bitmap, fps);
					}
				} finally {
					mLock.unlock();
				}
			}
		});
	}

	@WorkerThread
	private void releaseImageSource() {
		mManager.removeFrameCallback(mFrameCallback);
		mLock.lock();
		try {
			if (mImageSource != null) {
				if (DEBUG) Log.v(TAG, "releaseImageSource:");
				mImageSource.release();
				mImageSource = null;
			}
		} finally {
			mLock.unlock();
		}
	}

	@WorkerThread
	private void createImageSource(@NonNull final Bitmap bitmap, @Nullable final Fraction fps) {
		if (DEBUG) Log.v(TAG, "createImageSource:" + bitmap);
		mManager.removeFrameCallback(mFrameCallback);
		final int width = bitmap.getWidth();
		final int height = bitmap.getHeight();
		final boolean needResize = (getWidth() != width) || (getHeight() != height);
		final float _fps = fps != null ? fps.asFloat() : DEFAULT_FPS;
		mLock.lock();
		try {
			if ((mImageSource == null) || needResize) {
				releaseImageSource();
				mImageSource = GLTexture.newInstance(GLES20.GL_TEXTURE0, width, height, GLES20.GL_LINEAR);
				GLUtils.checkGlError("createImageSource");
			}
			mImageSource.loadBitmap(bitmap);
			mFrameIntervalNs = Math.round(1000000000.0 / _fps);
			mFrameIntervalMs = mFrameIntervalNs / 1000000L - 5;
			if (DEBUG) Log.v(TAG, "createImageSource:mFrameIntervalNs=" + mFrameIntervalNs);
		} finally {
			mLock.unlock();
		}
		if (needResize) {
			resize(width, height);
		}
		prevFrameTimeNs = -1L;
		mManager.postFrameCallbackDelayed(mFrameCallback, 0);
	}

	/**
	 * 一定時間毎にonFrameAvailableを呼び出すためのChoreographer.FrameCallback実装
	 */
	private final Choreographer.FrameCallback mFrameCallback
		= new Choreographer.FrameCallback() {
		@WorkerThread
		@Override
		public void doFrame(final long frameTimeNanos) {
			if (isValid()) {
				if (prevFrameTimeNs < 0) {
					prevFrameTimeNs = frameTimeNanos - mFrameIntervalNs;
				}
				final long delta = (mFrameIntervalNs - (frameTimeNanos - prevFrameTimeNs)) / 1000000L;
				prevFrameTimeNs = frameTimeNanos;
				if (delta < 0) {
					// フレームレートから想定されるより呼び出しが遅かった場合
					mManager.postFrameCallbackDelayed(this, mFrameIntervalMs + delta);
				} else {
					mManager.postFrameCallbackDelayed(this, mFrameIntervalMs);
				}
				if (DEBUG && (delta != 0)) Log.v(TAG, "delta=" + delta);
				mLock.lock();
				try {
					if (mImageSource != null) {
						onFrameAvailable(false, mImageSource.getTexId(), mImageSource.getTexMatrix());
					}
				} finally {
					mLock.unlock();
				}
			}
		}
	};

}
