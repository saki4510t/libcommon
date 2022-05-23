package com.serenegiant.glutils;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2022 saki t_saki@serenegiant.com
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

import com.serenegiant.gl.GLSurface;
import com.serenegiant.gl.GLUtils;
import com.serenegiant.graphics.BitmapHelper;
import com.serenegiant.utils.Pool;

import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingDeque;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import static com.serenegiant.gl.GLConst.GL_TEXTURE_EXTERNAL_OES;

/**
 * GLImageReceiverを使ってBitmapとして映像を受け取るためのGLImageReader<Bitmap></>実装
 */
public class GLBitmapImageReader implements ImageReader<Bitmap>, GLImageReceiver.Callback {
	private static final boolean DEBUG = false;
	private static final String TAG = GLBitmapImageReader.class.getSimpleName();

	@NonNull
	private final Object mSync = new Object();
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
	/**
	 * SurfaceTextureへ割り当てたテクスチャをバックバッファとしてラップして
	 * 読み取り可能にするためのGLSurface
	 */
	private GLSurface mReadSurface;
	private volatile boolean mAllBitmapAcquired = false;

	@Nullable
	private OnImageAvailableListener<Bitmap> mListener;
	@Nullable
	private Handler mListenerHandler;
	private int mWidth;
	private int mHeight;

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
		mPool = new Pool<Bitmap>(1, maxImages) {
			@NonNull
			@Override
			protected Bitmap createObject(@Nullable final Object... args) {
				synchronized (mSync) {
					return Bitmap.createBitmap(mWidth, mHeight, config);
				}
			}
		};
	}

	/**
	 * GLImageReceiver.ImageReader<Bitmap>の実装
	 * @param reader
	 */
	@WorkerThread
	@Override
	public void onInitialize(@NonNull final GLImageReceiver reader) {
		if (DEBUG) Log.v(TAG, "onInitialize:");
		// do nothing now
	}

	/**
	 * GLImageReceiver.ImageReader<Bitmap>の実装
	 */
	@WorkerThread
	@Override
	public void onRelease() {
		if (DEBUG) Log.v(TAG, "release:");
		setOnImageAvailableListener(null, null);
		synchronized (mQueue) {
			mQueue.clear();
		}
		mWorkBuffer = null;
		mPool.clear();
	}

	/**
	 * GLImageReceiver.ImageReader<Bitmap>の実装
	 * @param reader
	 */
	@WorkerThread
	@Override
	public void onCreateInputSurface(@NonNull final GLImageReceiver reader) {
		if (DEBUG) Log.v(TAG, "onCreateInputSurface:");
		// do nothing now
	}

	/**
	 * GLImageReceiver.ImageReader<Bitmap>の実装
	 * @param reader
	 */
	@WorkerThread
	@Override
	public void onReleaseInputSurface(@NonNull final GLImageReceiver reader) {
		if (DEBUG) Log.v(TAG, "onReleaseInputSurface:");
		mWorkBuffer = null;
		if (mReadSurface != null) {
			mReadSurface.release();
			mReadSurface = null;
		}
	}

	/**
	 * GLImageReceiver.ImageReader<Bitmap>の実装
	 * @param width
	 * @param height
	 */
	@WorkerThread
	@Override
	public void onResize(final int width, final int height) {
		if (DEBUG) Log.v(TAG, String.format("onResize:(%dx%d)", width, height));
		synchronized (mSync) {
			mWidth = width;
			mHeight = height;
		}
	}

	/**
	 * GLImageReceiver.ImageReader<Bitmap>の実装
	 * @param reader
	 * @param isOES
	 * @param texId
	 * @param texMatrix
	 * @return true: #onImageAvailableコールバックメソッドを呼び出す, false: 呼び出さない
	 */
	@WorkerThread
	@Override
	public void onFrameAvailable(
		@NonNull final GLImageReceiver reader,
		final boolean isOES,
		final int texId, @NonNull final float[] texMatrix) {

//		if (DEBUG) Log.v(TAG, "onFrameAvailable:");
		final int width = reader.getWidth();
		final int height = reader.getHeight();
		final int bytes = width * height * BitmapHelper.getPixelBytes(mConfig);
		if ((mWorkBuffer == null) || (mWorkBuffer.capacity() != bytes)) {
			synchronized (mSync) {
				mWidth = width;
				mHeight = height;
			}
			mWorkBuffer = ByteBuffer.allocateDirect(bytes);
		}
		final Bitmap bitmap = obtainBitmap(width, height);
		if (bitmap != null) {
			mAllBitmapAcquired = false;
//			// OESテクスチャをオフスクリーン(マスターサーフェース)へ描画
			if (mReadSurface == null) {
				try {
					// テクスチャをバックバッファとしてアクセスできるようにGLSurfaceでラップする
					mReadSurface = GLSurface.wrap(reader.isGLES3(),
						GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE1, texId,
						width, height, false);
				} catch (final Exception e) {
					Log.w(TAG, e);
					return;
				}
			}
			mReadSurface.makeCurrent();
			// オフスクリーンから読み取り
			mWorkBuffer.clear();
			GLUtils.glReadPixels(mWorkBuffer, width, height);
			// Bitmapへ代入
			mWorkBuffer.clear();
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

	/**
	 * 読み取った映像データの準備ができたときのコールバックリスナーを登録
	 * @param listener
	 * @param handler
	 * @throws IllegalArgumentException
	 */
	public void setOnImageAvailableListener(
		@Nullable final OnImageAvailableListener<Bitmap> listener,
		@Nullable final Handler handler) throws IllegalArgumentException {

		synchronized (mSync) {
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
		}
	}

	/**
	 * GLImageReceiver.ImageReader<Bitmap>の実装
	 * @return
	 * @throws IllegalStateException
	 */
	@Nullable
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
	 * GLImageReceiver.ImageReader<Bitmap>の実装
	 * @return
	 * @throws IllegalStateException
	 */
	@Nullable
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
	 * GLImageReceiver.ImageReader<Bitmap>の実装
	 * @param image
	 */
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
		Bitmap result = mPool.obtain();
		if (result == null) {
			// フレームプールが空の時はキューの一番古い物を取得する
			synchronized (mQueue) {
				result = mQueue.pollFirst();
			}
		}
		if ((result != null) && ((result.getWidth() != width) || result.getHeight() != height)) {
			// 途中でサイズが変更されプール内に古いサイズのBitmapが残ってしまったときの処理
			result = Bitmap.createBitmap(mWidth, mHeight, mConfig);
		}
		return result;
	}

	/**
	 * OnImageAvailableListener#onImageAvailableを呼び出す
	 */
	private void callOnFrameAvailable() {
		synchronized (mSync) {
			if (mListenerHandler != null) {
				mListenerHandler.removeCallbacks(mOnImageAvailableTask);
				mListenerHandler.post(mOnImageAvailableTask);
			} else if (DEBUG) {
				Log.w(TAG, "handleDraw: Unexpectedly listener handler is null!");
			}
		}
	}

	/**
	 * OnImageAvailableListener#onImageAvailableを呼び出すためのRunnable実装
	 */
	private final Runnable mOnImageAvailableTask = new Runnable() {
		@Override
		public void run() {
			synchronized (mSync) {
				if (mListener != null) {
					mListener.onImageAvailable(GLBitmapImageReader.this);
				}
			}
		}
	};
}
