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
import android.graphics.Paint;
import android.opengl.GLES20;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;

import com.serenegiant.gl.GLConst;
import com.serenegiant.gl.GLSurface;
import com.serenegiant.gl.GLUtils;
import com.serenegiant.graphics.BitmapHelper;
import com.serenegiant.utils.Pool;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.locks.ReentrantLock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import static com.serenegiant.gl.GLConst.GL_TEXTURE_EXTERNAL_OES;

/**
 * GLImageReceiverを使ってBitmapとして映像を受け取るためのImageReader<Bitmap></>実装
 */
public class GLBitmapImageReader implements ImageReader<Bitmap>, GLSurfaceReceiver.Callback {
	private static final boolean DEBUG = false;
	private static final String TAG = GLBitmapImageReader.class.getSimpleName();

	/**
	 * 排他制御用
	 */
	@NonNull
	private final ReentrantLock mLock = new ReentrantLock();
	@NonNull
	private final Bitmap.Config mConfig;
	/**
	 * 最大同時にハンドリングできるBitmapの数
	 */
	private final int mMaxImages;
	/**
	 * ビットマップを再利用するためのプールオブジェクト
	 */
	@NonNull
	private final Pool<Bitmap> mPool;
	@NonNull
	private final LinkedBlockingDeque<Bitmap> mQueue = new LinkedBlockingDeque<>();
	@NonNull
	private final Paint mPaint = new Paint();
	@Nullable
	private ByteBuffer mWorkBuffer;
	private volatile boolean mAllBitmapAcquired = false;

	@Nullable
	private OnImageAvailableListener<Bitmap> mListener;
	@Nullable
	private Handler mListenerHandler;
	private int mWidth;
	private int mHeight;
	private volatile boolean mEnabled = true;

	/**
	 * コンストラクタ
	 * @param width
	 * @param height
	 * @param config
	 * @param maxImages
	 */
	public GLBitmapImageReader(
		final int width, final int height,
		@NonNull final Bitmap.Config config, final int maxImages) {

		if (DEBUG) Log.v(TAG, "コンストラクタ:");
		mWidth = width;
		mHeight = height;
		mConfig = config;
		mMaxImages = maxImages;
		mPool = new Pool<Bitmap>(1, maxImages, maxImages, mWidth, mHeight) {
			@NonNull
			@Override
			protected Bitmap createObject(@Nullable final Object... args) {
				final int w = (int)args[0];
				final int h = (int)args[1];
				return Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
			}

			@Override
			public void release(@NonNull final Bitmap bitmap) {
				if (!bitmap.isRecycled()) {
					bitmap.recycle();
				}
				super.release(bitmap);
			}

			@Nullable
			@Override
			public Bitmap obtain(@Nullable final Object... args) {
				final int w = (int)args[0];
				final int h = (int)args[1];
				Bitmap result = super.obtain(args);
				while ((result != null)
					&& (result.isRecycled()
					|| (result.getWidth() != w)
					|| (result.getHeight() != h))) {
					// サイズが違う・リサイクルされてしまっているBitmapは返さない
					release(result);
					result = super.obtain(args);
				}
				return result;
			}
		};
	}

//--------------------------------------------------------------------------------
// 	 GLImageReceiver.Callbackの実装
//--------------------------------------------------------------------------------
	/**
	 * GLImageReceiver.Callbackの実装
	 */
	@WorkerThread
	@Override
	public void onInitialize() {
		if (DEBUG) Log.v(TAG, "onInitialize:");
		// do nothing now
	}

	/**
	 * GLImageReceiver.Callbackの実装
	 */
	@WorkerThread
	@Override
	public void onRelease() {
		if (DEBUG) Log.v(TAG, "release:");
		mEnabled = false;
		setOnImageAvailableListener(null, null);
		synchronized (mQueue) {
			mQueue.clear();
		}
		mWorkBuffer = null;
		mPool.clear();
	}

	/**
	 * GLImageReceiver.Callbackの実装
	 * @param surface
	 * @param width
	 * @param height
	 */
	@WorkerThread
	@Override
	public void onCreateInputSurface(@NonNull final Surface surface, final int width, final int height) {
		if (DEBUG) Log.v(TAG, "onCreateInputSurface:");
		// do nothing now
	}

	/**
	 * GLImageReceiver.Callbackの実装
	 * @param surface
	 */
	@WorkerThread
	@Override
	public void onReleaseInputSurface(@NonNull final Surface surface) {
		if (DEBUG) Log.v(TAG, "onReleaseInputSurface:");
		mWorkBuffer = null;
	}

	/**
	 * GLImageReceiver.Callbackの実装
	 * @param width
	 * @param height
	 */
	@WorkerThread
	@Override
	public void onResize(final int width, final int height) {
		if (DEBUG) Log.v(TAG, String.format("onResize:(%dx%d)", width, height));
		mLock.lock();
		try {
			mWidth = width;
			mHeight = height;
		} finally {
			mLock.unlock();
		}
	}

//--------------------------------------------------------------------------------
// 	 GLImageReceiver.FrameAvailableCallbackの実装
//--------------------------------------------------------------------------------
	/**
	 * GLImageReceiver.FrameAvailableCallbackの実装
	 * @param isGLES3
	 * @param isOES
	 * @param width
	 * @param height
	 * @param texId
	 * @param texMatrix
	 * @return true: #onImageAvailableコールバックメソッドを呼び出す, false: 呼び出さない
	 */
	@WorkerThread
	@Override
	public void onFrameAvailable(
		final boolean isGLES3, final boolean isOES,
		final int width, final int height,
		final int texId, @NonNull final float[] texMatrix) {

		if (!mEnabled) return;
//		if (DEBUG) Log.v(TAG, "onFrameAvailable:");
		final int bytes = width * height * BitmapHelper.getPixelBytes(mConfig);
		if ((mWorkBuffer == null) || (mWorkBuffer.capacity() != bytes)) {
			mLock.lock();
			try {
				mWidth = width;
				mHeight = height;
			} finally {
				mLock.unlock();
			}
			mWorkBuffer = ByteBuffer.allocateDirect(bytes).order(ByteOrder.LITTLE_ENDIAN);
		}
		final Bitmap bitmap = obtainBitmap(width, height);
		if (bitmap != null) {
			mAllBitmapAcquired = false;
			// テクスチャをバックバッファとしてアクセスできるようにGLSurfaceでラップする
			// FIXME テクスチャ変換行列が適用されていない！
			final GLSurface readSurface = GLSurface.wrap(isGLES3,
				isOES ? GL_TEXTURE_EXTERNAL_OES : GLConst.GL_TEXTURE_2D,
				GLES20.GL_TEXTURE4, texId, width, height, false);
			try {
				readSurface.makeCurrent();
				// テクスチャをバックバッファとしたオフスクリーンから読み取り
				mWorkBuffer.clear();
				GLUtils.glReadPixels(mWorkBuffer, width, height);
			} finally {
				readSurface.release();
			}
			// Bitmapへ代入
			bitmap.copyPixelsFromBuffer(mWorkBuffer);
			synchronized (mQueue) {
				mQueue.addLast(bitmap);
			}
		} else {
			mAllBitmapAcquired = true;
			if (DEBUG) Log.w(TAG, "handleDraw: failed to obtain bitmap from pool!");
		}
		callOnFrameAvailable();
	}

//--------------------------------------------------------------------------------
// 	 ImageReader<Bitmap>の実装
//--------------------------------------------------------------------------------
	/**
	 * ImageReader<Bitmap>の実装
	 * 読み取った映像データの準備ができたときのコールバックリスナーを登録
	 * @param listener
	 * @param handler
	 * @throws IllegalArgumentException
	 */
	@Override
	public void setOnImageAvailableListener(
		@Nullable final OnImageAvailableListener<Bitmap> listener,
		@Nullable final Handler handler) throws IllegalArgumentException {

		mLock.lock();
		try {
			if (listener != null) {
				Looper looper = handler != null ? handler.getLooper() : Looper.myLooper();
				if (looper == null) {
					throw new IllegalArgumentException(
						"handler is null but the current thread is not a looper");
				}
				if (mListenerHandler == null || mListenerHandler.getLooper() != looper) {
					mListenerHandler = new Handler(looper);
				}
				mListener = listener;
			} else {
				mListener = null;
				mListenerHandler = null;
			}
		} finally {
			mLock.unlock();
		}
	}

	/**
	 * ImageReader<Bitmap>の実装
	 * @return
	 * @throws IllegalStateException
	 */
	@Nullable
	@Override
	public Bitmap acquireLatestImage() throws IllegalStateException {
		synchronized (mQueue) {
			final Bitmap result = mQueue.pollLast();
			while (!mQueue.isEmpty()) {
				recycle(mQueue.pollFirst());
			}
			if (mAllBitmapAcquired && (result == null)) {
				throw new IllegalStateException("all bitmap is acquired!");
			}
			return result;
		}
	}

	/**
	 * ImageReader<Bitmap>の実装
	 * @return
	 * @throws IllegalStateException
	 */
	@Nullable
	@Override
	public Bitmap acquireNextImage() throws IllegalStateException {
		synchronized (mQueue) {
			final Bitmap result = mQueue.pollFirst();
			if (mAllBitmapAcquired && (result == null)) {
				throw new IllegalStateException("all bitmap is acquired!");
			}
			return result;
		}
	}

	/**
	 * ImageReader<Bitmap>の実装
	 * @param image
	 */
	@Override
	public void recycle(@NonNull final Bitmap image) {
		mAllBitmapAcquired = false;
		// Bitmap#recycleが呼ばれてしまっていると再利用できないのでプールに戻せない。
		// そのままだとプールが空になってしまうのでプールへ廃棄したことを通知する
		// (プールの生成済みオブジェクト数を減らして新しいBitmapをアロケーションできるようにする)
		if (!image.isRecycled()) {
			mPool.recycle(image);
		} else {
			mPool.release(image);
		}
	}

	/**
	 * ImageReader<Bitmap>の実装
	 * @param enabled
	 */
	@Override
	public void setEnabled(final Boolean enabled) {
		mEnabled = enabled;
	}

//--------------------------------------------------------------------------------
	/**
	 * 同時に取得できる最大の映像の数を取得
	 * @return
	 */
	public int getMaxImages() {
		return mMaxImages;
	}

	/**
	 * 取得するBitmapのConfigを取得
	 * @return
	 */
	@NonNull
	public Bitmap.Config getConfig() {
		return mConfig;
	}

	@Nullable
	private Bitmap obtainBitmap(final int width, final int height) {
		Bitmap result = mPool.obtain(width, height);
		if (result == null) {
			// フレームプールが空の時はキューの一番古い物を取得する
			synchronized (mQueue) {
				result = mQueue.pollFirst();
			}
		}
		if ((result != null) && ((result.getWidth() != width) || result.getHeight() != height)) {
			// 途中でサイズが変更されプール内に古いサイズのBitmapが残ってしまったときの処理
			result = Bitmap.createBitmap(width, height, mConfig);
		}
		return result;
	}

	/**
	 * OnImageAvailableListener#onImageAvailableを呼び出す
	 */
	private void callOnFrameAvailable() {
		mLock.lock();
		try {
			if (mListenerHandler != null) {
				mListenerHandler.removeCallbacks(mOnImageAvailableTask);
				mListenerHandler.post(mOnImageAvailableTask);
			} else if (DEBUG) {
				Log.w(TAG, "handleDraw: Unexpectedly listener handler is null!");
			}
		} finally {
			mLock.unlock();
		}
	}

	/**
	 * OnImageAvailableListener#onImageAvailableを呼び出すためのRunnable実装
	 */
	private final Runnable mOnImageAvailableTask = new Runnable() {
		@Override
		public void run() {
			mLock.lock();
			try {
				if (mListener != null) {
					mListener.onImageAvailable(GLBitmapImageReader.this);
				}
			} finally {
				mLock.unlock();
			}
		}
	};
}
