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
import com.serenegiant.gl.GLDrawer2D;
import com.serenegiant.gl.GLUtils;
import com.serenegiant.gl.GLManager;
import com.serenegiant.gl.GLTexture;
import com.serenegiant.gl.RendererTarget;
import com.serenegiant.math.Fraction;
import com.serenegiant.media.OnFrameAvailableListener;

import java.util.concurrent.locks.ReentrantLock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;
import androidx.annotation.WorkerThread;

/**
 * MediaCodecのデコーダーでデコードした動画やカメラからの映像の代わりに、
 * 静止画をSurfaceへ出力するためのクラス
 * StaticTextureSourceと違って分配描画しないので出力先Surfaceが1つだけであればこちらの方が効率的
 * Surfaceをセットせずに、OnFrameAvailableListenerが呼ばれたタイミングで
 * 内包する映像ソースのテクスチャid/テクスチャ変換行列へアクセスすることも可能
 */
public class ImageTextureSource implements GLConst, IMirror {
	private static final boolean DEBUG = false;	// 実働時はfalseにすること
	private static final String TAG = ImageTextureSource.class.getSimpleName();

	private static final int DEFAULT_WIDTH = 640;
	private static final int DEFAULT_HEIGHT = 480;
	private static final float DEFAULT_FPS = 30.0f;

	/**
	 * 排他制御用
	 */
	@NonNull
	private final ReentrantLock mLock = new ReentrantLock();
	@NonNull
	private final GLManager mManager;
	@Nullable
	private OnFrameAvailableListener mListener;
	@Nullable
	private GLTexture mImageSource;
	private volatile long mFrameIntervalNs;
	private long mFirstTimeNs;
	private long mNumFrames;
	private volatile boolean mReleased = false;
	private int mWidth, mHeight;
	@Nullable
	private GLDrawer2D mDrawer;
	@Nullable
	private RendererTarget mRendererTarget;
	@MirrorMode
	private int mMirror = MIRROR_NORMAL;

	/**
	 * コンストラクタ
	 * @param manager
	 * @param bitmap nullのときは後で#setSourceを呼び出さないと#onFrameAvailableが呼び出されない
	 * @param fps nullの時は30fps相当, 30fpsよりも大きいと想定通りのフレームレートにならないことが多いので注意
	 */
	public ImageTextureSource(
		@NonNull final GLManager manager,
		@Nullable final Bitmap bitmap, @Nullable final Fraction fps) {

		this(manager, bitmap, fps, null);
	}

	/**
	 * コンストラクタ
	 * @param manager
	 * @param bitmap nullのときは後で#setSourceを呼び出さないと#onFrameAvailableが呼び出されない
	 * @param fps nullの時は30fps相当, 30fpsよりも大きいと想定通りのフレームレートにならないことが多いので注意
	 * @param listener フレーム毎のコールバックリスナー
	 */
	public ImageTextureSource(
		@NonNull final GLManager manager,
		@Nullable final Bitmap bitmap, @Nullable final Fraction fps,
		@Nullable OnFrameAvailableListener listener) {
		super();
		if (DEBUG) Log.v(TAG, "コンストラクタ:" + bitmap);
		mManager = manager;
		mListener = listener;
		mWidth = DEFAULT_WIDTH;
		mHeight = DEFAULT_HEIGHT;
		if (bitmap != null) {
			mManager.runOnGLThread(() -> {
				createImageSource(bitmap, fps);
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
			mManager.runOnGLThread(() -> {
				releaseImageSource();
				releaseTarget();
			});
		}
	}

	@NonNull
	public GLManager getGLManager() throws IllegalStateException {
		return mManager;
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
	@Size(min=16)
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

	public boolean isValid() {
		return !mReleased && mManager.isValid();
	}

	public int getWidth() {
		return mWidth;
	}

	public int getHeight() {
		return mHeight;
	}

	public boolean hasSurface() {
		mLock.lock();
		try {
			return mRendererTarget != null;
		} finally {
			mLock.unlock();
		}
	}

	@Override
	public void setMirror(final int mirror) {
		mLock.lock();
		try {
			if (mMirror != mirror) {
				mMirror = mirror;
				mManager.runOnGLThread(() -> {
					if (mRendererTarget != null) {
						mRendererTarget.setMirror(mirror);
					}
				});
			}
		} finally {
			mLock.unlock();
		}
	}

	@MirrorMode
	@Override
	public int getMirror() {
		mLock.lock();
		try {
			return mMirror;
		} finally {
			mLock.unlock();
		}
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
					releaseImageSource();
				} else {
					createImageSource(bitmap, fps);
				}
			} finally {
				mLock.unlock();
			}
		});
	}

	/**
	 * ISurfacePipelineの実装
	 * 描画先のSurfaceを差し替え
	 * 対応していないSurface形式の場合はIllegalArgumentExceptionを投げる
	 * @param surface nullまたはSurface/SurfaceHolder/SurfaceTexture/SurfaceView
	 * @throws IllegalStateException
	 * @throws IllegalArgumentException
	 */
	public void setSurface(
		@Nullable final Object surface) throws IllegalStateException, IllegalArgumentException {

		if (DEBUG) Log.v(TAG, "setSurface:" + surface);
		if (!isValid()) {
			throw new IllegalStateException("already released?");
		}
		if ((surface != null) && !GLUtils.isSupportedSurface(surface)) {
			throw new IllegalArgumentException("Unsupported surface type!," + surface);
		}
		mManager.runOnGLThread(() -> {
			createTargetOnGL(surface);
		});
	}

	/**
	 * OnFrameAvailableListenerをセット
	 * @param listener
	 */
	public void setOnFrameAvailableListener(@Nullable OnFrameAvailableListener listener) {
		if (DEBUG) Log.v(TAG, "setOnFrameAvailableListener:" + listener);
		mLock.lock();
		try {
			mListener = listener;
		} finally {
			mLock.unlock();
		}
	}

	/**
	 * 描画処理
	 * Choreographer.FrameCallbackからmImageSource != nullのときだけ呼ばれる
	 */
	@WorkerThread
	private void onFrameAvailable(final int texId, final float[] texMatrix) {
		if (isValid()) {
			@NonNull
			final GLDrawer2D drawer;
			@Nullable
			final RendererTarget target;
			mLock.lock();
			try {
				drawer = mDrawer;
				target = mRendererTarget;
			} finally {
				mLock.unlock();
			}
			if ((drawer != null)
				&& (target != null)
				&& target.canDraw()) {
				target.draw(drawer, GLES20.GL_TEXTURE0, texId, texMatrix);
			}
		}
	}

	/**
	 * 描画処理(黒で塗りつぶすだけ)
	 * Choreographer.FrameCallbackからmImageSource == nullのときだけ呼ばれる
	 */
	@WorkerThread
	private void onFrameAvailable() {
		if (isValid()) {
			@Nullable
			final RendererTarget target;
			mLock.lock();
			try {
				target = mRendererTarget;
			} finally {
				mLock.unlock();
			}
			if ((target != null)
				&& target.canDraw()) {
				target.clear(0);
			}
		}
	}

	/**
	 * 映像ソース用のGLTextureを破棄する
	 */
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

	/**
	 * 映像ソース用のGLTextureを生成する
	 * @param bitmap
	 * @param fps
	 */
	@WorkerThread
	private void createImageSource(@NonNull final Bitmap bitmap, @Nullable final Fraction fps) {
		if (DEBUG) Log.v(TAG, "createImageSource:" + bitmap + ",fps=" + fps);
		mManager.removeFrameCallback(mFrameCallback);
		final int width = bitmap.getWidth();
		final int height = bitmap.getHeight();
		final boolean needResize = (getWidth() != width) || (getHeight() != height);
		final float _fps = fps != null ? fps.asFloat() : DEFAULT_FPS;
		if (DEBUG) Log.v(TAG, "createImageSource:fps=" + _fps);
		mLock.lock();
		try {
			if ((mImageSource == null) || needResize) {
				releaseImageSource();
				mImageSource = GLTexture.newInstance(GLES20.GL_TEXTURE0, width, height, GLES20.GL_LINEAR);
				GLUtils.checkGlError("createImageSource");
			}
			mImageSource.loadBitmap(bitmap);
			mFrameIntervalNs = Math.round(1000000000.0 / _fps);
			if (DEBUG) Log.v(TAG, "createImageSource:mFrameIntervalNs=" + mFrameIntervalNs);
			mWidth = width;
			mHeight = height;
		} finally {
			mLock.unlock();
		}
		mFirstTimeNs = -1L;
		mNumFrames = 0;
		mManager.postFrameCallbackDelayed(mFrameCallback, 0);
	}

	/**
	 * 描画先のSurfaceとGLDrawer2Dを生成
	 * @param surface
	 */
	@WorkerThread
	private void createTargetOnGL(@Nullable final Object surface) {
		if (DEBUG) Log.v(TAG, "createTarget:" + surface);
		mLock.lock();
		try {
			if ((mRendererTarget != null) && (mRendererTarget.getSurface() != surface)) {
				// すでにRendererTargetが生成されていて描画先surfaceが変更された時
				mRendererTarget.release();
				mRendererTarget = null;
			}
			if ((mRendererTarget == null) && (surface != null)) {
				mRendererTarget = RendererTarget.newInstance(
					mManager.getEgl(), surface, 0);
				mRendererTarget.setMirror(mMirror);
				if (mDrawer == null) {
					mDrawer = GLDrawer2D.create(mManager.isGLES3(), false);
				}
			}
		} finally {
			mLock.unlock();
		}
	}

	@WorkerThread
	private void releaseTarget() {
		final GLDrawer2D drawer;
		final RendererTarget target;
		mLock.lock();
		try {
			drawer = mDrawer;
			mDrawer = null;
			target = mRendererTarget;
			mRendererTarget = null;
		} finally {
			mLock.unlock();
		}
		if ((drawer != null) || (target != null)) {
			if (DEBUG) Log.v(TAG, "releaseTarget:");
			if (mManager.isValid()) {
				try {
					mManager.runOnGLThread(() -> {
						if (drawer != null) {
							if (DEBUG) Log.v(TAG, "releaseTarget:release drawer");
							drawer.release();
						}
						if (target != null) {
							if (DEBUG) Log.v(TAG, "releaseTarget:release target");
							target.release();
						}
					});
				} catch (final Exception e) {
					if (DEBUG) Log.w(TAG, e);
				}
			} else if (DEBUG) {
				Log.w(TAG, "releaseTarget:unexpectedly GLManager is already released!");
			}
		}
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
				final long n = (++mNumFrames);
				if (mFirstTimeNs < 0) {
					mFirstTimeNs = frameTimeNanos;
				}
				long ms = (mFirstTimeNs + mFrameIntervalNs * (n + 1) - frameTimeNanos) / 1000000L;
				if (ms < 5L) {
					ms = 0L;
				}
				mManager.postFrameCallbackDelayed(this, ms);
				final OnFrameAvailableListener listener;
				mLock.lock();
				try {
					listener = mListener;
					if (mImageSource != null) {
						onFrameAvailable(mImageSource.getTexId(), mImageSource.getTexMatrix());
					} else {
						onFrameAvailable();
					}
				} finally {
					mLock.unlock();
				}
				if (listener != null) {
					listener.onFrameAvailable();
				}
			}
		}
	};
}
