package com.serenegiant.glutils;
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
import android.opengl.GLES20;
import android.util.Log;
import android.view.Choreographer;

import com.serenegiant.gl.GLConst;
import com.serenegiant.gl.GLManager;
import com.serenegiant.gl.GLTexture;
import com.serenegiant.gl.GLUtils;
import com.serenegiant.math.Fraction;

import java.util.concurrent.locks.ReentrantLock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;
import androidx.annotation.WorkerThread;

/**
 * テクスチャへ読み込んだ静止画を映像ソースとして一定時間毎にGLFrameAvailableCallbackへ引き渡す
 * ヘルパークラス
 * 映像ソースとする静止画を設定していないときはGLFrameAvailableCallbackが呼び出されない
 */
public class GLTextureSource implements GLConst {
	private static final boolean DEBUG = false;	// 実働時はfalseにすること
	private static final String TAG = GLTextureSource.class.getSimpleName();

	private static final float DEFAULT_FPS = 30.0f;

	/**
	 * 排他制御用
	 */
	@NonNull
	private final ReentrantLock mLock = new ReentrantLock();
	@NonNull
	private final GLManager mManager;
	private final boolean mIsGLES3;

	private volatile boolean mReleased = false;
	@Nullable
	private GLFrameAvailableCallback mCallback;
	@Nullable
	private GLTexture mImageSource;
	private int mWidth, mHeight;
	private volatile long mFrameIntervalNs;
	private long mFirstTimeNs;
	private long mNumFrames;

	/**
	 * コンストラクタ
	 * @param manager
	 * @param bitmap nullのときは後で#setSourceを呼び出さないとGLFrameAvailableCallbackが呼び出されない
	 * @param fps nullの時は30fps相当, 30fpsよりも大きいと想定通りのフレームレートにならないことが多いので注意
	 */
	public GLTextureSource(
		@NonNull final GLManager manager,
		@Nullable final Bitmap bitmap, @Nullable final Fraction fps) {

		this(manager, bitmap, fps, null);
	}

	/**
	 * コンストラクタ
	 * @param manager
	 * @param bitmap nullのときは後で#setSourceを呼び出さないとGLFrameAvailableCallbackが呼び出されない
	 * @param fps nullの時は30fps相当, 30fpsよりも大きいと想定通りのフレームレートにならないことが多いので注意
	 * @param callback フレーム毎のコールバックリスナー
	 */
	public GLTextureSource(
		@NonNull final GLManager manager,
		@Nullable final Bitmap bitmap, @Nullable final Fraction fps,
		@Nullable GLFrameAvailableCallback callback) {
		super();
		if (DEBUG) Log.v(TAG, "コンストラクタ:" + bitmap);
		mManager = manager;
		mIsGLES3 = mManager.isGLES3();
		mCallback = callback;
		mWidth = 1;
		mHeight = 1;
		if (bitmap != null) {
			mManager.runOnGLThread(() -> {
				createImageSourceOnGL(bitmap, fps);
			});
		}
	}

	@Override
	protected void finalize() throws Throwable {
		try {
			release();
		} finally {
			super.finalize();
		}
	}

	public final void release() {
		if (!mReleased) {
			mReleased = true;
			internalRelease();
		}
	}

	protected void internalRelease() {
		if (isValid()) {
			mManager.removeFrameCallback(mFrameCallback);
			mManager.getGLHandler().postAtFrontOfQueue(() -> {
				releaseImageSourceOnGL();
			});
		}
	}

	@NonNull
	public GLManager getGLManager() throws IllegalStateException {
		return mManager;
	}

	public boolean isValid() {
		return !mReleased && mManager.isValid();
	}

	/**
	 * テクスチャ名を取得する
	 * すでに#releaseが呼ばれたか映像ソース用のBitmapがセットされていないときはIllegalStateExceptionを投げる
	 * @return
	 * @throws IllegalStateException
	 */
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
	@Size(value=16)
	@NonNull
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

	public int getWidth() {
		return mWidth;
	}

	public int getHeight() {
		return mHeight;
	}

	/**
	 * 映像ソース用のBitmapをセット
	 * @param bitmap nullのときは後で#setSourceを呼び出さないと#onFrameAvailableが呼び出されない
	 * @param fps
	 */
	public void setSource(@Nullable final Bitmap bitmap, @Nullable final Fraction fps) {
		mManager.runOnGLThread(() -> {
			mLock.lock();
			try {
				if (bitmap == null) {
					releaseImageSourceOnGL();
				} else {
					createImageSourceOnGL(bitmap, fps);
				}
			} finally {
				mLock.unlock();
			}
		});
	}

	/**
	 * OnFrameAvailableListenerをセット
	 * @param callback
	 */
	public void setFrameAvailableListener(@Nullable GLFrameAvailableCallback callback) {
		if (DEBUG) Log.v(TAG, "setOnFrameAvailableListener:" + callback);
		mLock.lock();
		try {
			mCallback = callback;
		} finally {
			mLock.unlock();
		}
	}

	/**
	 * 映像ソース用のGLTextureを破棄する
	 */
	@WorkerThread
	private void releaseImageSourceOnGL() {
		mManager.removeFrameCallback(mFrameCallback);
		final GLTexture source;
		mLock.lock();
		try {
			source = mImageSource;
			mImageSource = null;
		} finally {
			mLock.unlock();
		}
		if (source != null) {
			if (DEBUG) Log.v(TAG, "releaseImageSourceOnGL:");
			source.release();
		}
	}

	/**
	 * 映像ソース用のGLTextureを生成する
	 * @param bitmap
	 * @param fps
	 */
	@WorkerThread
	private void createImageSourceOnGL(@NonNull final Bitmap bitmap, @Nullable final Fraction fps) {
		if (DEBUG) Log.v(TAG, "createImageSourceOnGL:" + bitmap + ",fps=" + fps);
		mManager.removeFrameCallback(mFrameCallback);
		final int width = bitmap.getWidth();
		final int height = bitmap.getHeight();
		if ((width <= 0) || (height <= 0)) return;
		final boolean needResize = (mWidth != width) || (mHeight != height);
		float _fps = fps != null ? fps.asFloat() : DEFAULT_FPS;
		if (_fps <= 0.0f) {
			_fps = DEFAULT_FPS;
		}
		if (DEBUG) Log.v(TAG, "createImageSourceOnGL:fps=" + _fps);
		mLock.lock();
		try {
			if ((mImageSource == null) || needResize) {
				releaseImageSourceOnGL();
				mImageSource = GLTexture.newInstance(GLES20.GL_TEXTURE0, width, height, GLES20.GL_LINEAR);
				GLUtils.checkGlError("createImageSourceOnGL");
			}
			mImageSource.loadBitmap(bitmap);
			mFrameIntervalNs = Math.round(1000000000.0 / _fps);
			mFirstTimeNs = -1L;
			mNumFrames = 0;
			if (DEBUG) Log.v(TAG, "createImageSourceOnGL:mFrameIntervalNs=" + mFrameIntervalNs);
			mWidth = width;
			mHeight = height;
		} finally {
			mLock.unlock();
		}
		mManager.postFrameCallbackDelayed(mFrameCallback, 0);
	}

	/**
	 * 一定時間毎にGLFrameAvailableCallbackを呼び出すためのChoreographer.FrameCallback実装
	 */
	private final Choreographer.FrameCallback mFrameCallback
		= new Choreographer.FrameCallback() {
		@WorkerThread
		@Override
		public void doFrame(final long frameTimeNanos) {
			if (isValid()) {
				final long n = (++mNumFrames);
				if (mFirstTimeNs < 0) {
					mFirstTimeNs = frameTimeNanos;
				}
				long ms = (mFirstTimeNs + mFrameIntervalNs * (n + 1) - frameTimeNanos) / 1000000L;
				if (ms < 5L) {
					ms = 0L;
				}
				try {
					mManager.postFrameCallbackDelayed(this, ms);
					final GLFrameAvailableCallback callback;
					final GLTexture source;
					final int width, height;
					mLock.lock();
					try {
						callback = mCallback;
						source = mImageSource;
						width = mWidth;
						height = mHeight;
					} finally {
						mLock.unlock();
					}
					if ((callback != null) && (source != null) && (width > 0) && (height > 0)) {
						mManager.makeDefault(0xff000000);
						mManager.swap();
						callback.onFrameAvailable(mIsGLES3, false, width, height, source.getTexId(), source.getTexMatrix());
					}
				} catch (final Exception e) {
					if (DEBUG) Log.w(TAG, e);
				}
			}
		}
	};
}
