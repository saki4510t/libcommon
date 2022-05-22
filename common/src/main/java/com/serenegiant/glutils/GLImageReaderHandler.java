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
 * GLSurfaceReaderを使ってBitmapとして映像を受け取るためのGLSurfaceReader.ImageHandler実装
 */
public class GLImageReaderHandler implements GLImageReader.ImageHandler<Bitmap> {
	private static final boolean DEBUG = false;
	private static final String TAG = GLImageReaderHandler.class.getSimpleName();

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
	private int mWidth;
	private int mHeight;

	/**
	 * コンストラクタ
	 * @param width
	 * @param height
	 * @param config
	 * @param maxImages
	 */
	public GLImageReaderHandler(
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
	 * GLImageReader.ImageHandler<Bitmap>の実装
	 * @param reader
	 */
	@WorkerThread
	@Override
	public void onInitialize(@NonNull final GLImageReader<Bitmap> reader) {
		if (DEBUG) Log.v(TAG, "onInitialize:");
		// do nothing now
	}

	/**
	 * GLImageReader.ImageHandler<Bitmap>の実装
	 */
	@WorkerThread
	@Override
	public void onRelease() {
		if (DEBUG) Log.v(TAG, "release:");
		synchronized (mQueue) {
			mQueue.clear();
		}
		mWorkBuffer = null;
		mPool.clear();
	}

	/**
	 * GLImageReader.ImageHandler<Bitmap>の実装
	 * @param reader
	 */
	@WorkerThread
	@Override
	public void onCreateInputSurface(@NonNull final GLImageReader<Bitmap> reader) {
		if (DEBUG) Log.v(TAG, "onCreateInputSurface:");
		// do nothing now
	}

	/**
	 * GLImageReader.ImageHandler<Bitmap>の実装
	 * @param reader
	 */
	@WorkerThread
	@Override
	public void onReleaseInputSurface(@NonNull final GLImageReader<Bitmap> reader) {
		if (DEBUG) Log.v(TAG, "onReleaseInputSurface:");
		mWorkBuffer = null;
		if (mReadSurface != null) {
			mReadSurface.release();
			mReadSurface = null;
		}
	}

	/**
	 * GLImageReader.ImageHandler<Bitmap>の実装
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
	 * GLImageReader.ImageHandler<Bitmap>の実装
	 * @param reader
	 * @param texId
	 * @param texMatrix
	 */
	@WorkerThread
	@Override
	public boolean onFrameAvailable(
		@NonNull final GLImageReader<Bitmap> reader,
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
					return false;
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
		return true;
	}

	/**
	 * GLImageReader.ImageHandler<Bitmap>の実装
	 * @return
	 * @throws IllegalStateException
	 */
	@Nullable
	@Override
	public Bitmap onAcquireLatestImage() throws IllegalStateException {
		synchronized (mQueue) {
			final Bitmap result = mQueue.pollLast();
			while (!mQueue.isEmpty()) {
				onRecycle(mQueue.pollFirst());
			}
			if (mAllBitmapAcquired && (result == null)) {
				throw new IllegalStateException("all bitmap is acquired!");
			}
			return result;
		}
	}

	/**
	 * GLImageReader.ImageHandler<Bitmap>の実装
	 * @return
	 * @throws IllegalStateException
	 */
	@Nullable
	@Override
	public Bitmap onAcquireNextImage() throws IllegalStateException {
		synchronized (mQueue) {
			final Bitmap result = mQueue.pollFirst();
			if (mAllBitmapAcquired && (result == null)) {
				throw new IllegalStateException("all bitmap is acquired!");
			}
			return result;
		}
	}

	/**
	 * GLImageReader.ImageHandler<Bitmap>の実装
	 * @param image
	 */
	@Override
	public void onRecycle(@NonNull final Bitmap image) {
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
}
