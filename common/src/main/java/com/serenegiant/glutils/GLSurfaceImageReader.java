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

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.opengl.GLES20;
import android.util.Log;

import com.serenegiant.graphics.BitmapHelper;
import com.serenegiant.utils.Pool;

import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingDeque;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;
import androidx.annotation.WorkerThread;

import static com.serenegiant.glutils.GLConst.GL_TEXTURE_EXTERNAL_OES;

/**
 * Surface/SurfaceTextureに描画された内容をビットマップとして取得するためのヘルパークラス
 * ImageReaderをイメージして似た使い方ができるようにしてみた
 */
public class GLSurfaceImageReader extends GLSurfaceReader<Bitmap> {
	private static final boolean DEBUG = false;
	private static final String TAG = GLSurfaceImageReader.class.getSimpleName();

	@NonNull
	private final Bitmap.Config mConfig;
	@NonNull
	private final Pool<Bitmap> mPool;
	@NonNull
	private final LinkedBlockingDeque<Bitmap> mQueue = new LinkedBlockingDeque<>();
	@NonNull
	private final Paint mPaint = new Paint();
	@NonNull
	private final ByteBuffer mWorkBuffer;
	/**
	 * SurfaceTextureへ割り当てたテクスチャをバックバッファとしてラップして
	 * 読み取り可能にするためのGLSurface
	 */
	private GLSurface mReadSurface;
	private volatile boolean mAllBitmapAcquired = false;

	/**
	 * コンストラクタ
	 * @param width
	 * @param height
	 * @param config 取得するビットマップのConfig
	 * @param maxImages 同時に取得できるビットマップの最大数
	 */
	public GLSurfaceImageReader(
		@IntRange(from = 1) final int width, @IntRange(from = 1) final int height,
		@NonNull final Bitmap.Config config, final int maxImages) {

		super(width, height, maxImages);
		mConfig = config;
		mPool = new Pool<Bitmap>(1, maxImages) {
			@NonNull
			@Override
			protected Bitmap createObject(@Nullable final Object... args) {
				return Bitmap.createBitmap(width, height, config);
			}
		};
		mWorkBuffer = ByteBuffer.allocateDirect(width * height * BitmapHelper.getPixelBytes(config));
	}

	/**
	 * 関係するリソースを破棄する、再利用はできない
	 */
	protected void internalRelease() {
		if (DEBUG) Log.v(TAG, "release:");
		synchronized (mQueue) {
			mQueue.clear();
		}
		mPool.clear();
	}

	/**
	 * 取得するBitmapのConfigを取得
	 * @return
	 */
	@NonNull
	public Bitmap.Config getConfig() {
		return mConfig;
	}

	/**
	 * Bitmapを再利用可能にする
	 * @param bitmap
	 */
	@Override
	public void recycle(@NonNull final Bitmap bitmap) {
		mAllBitmapAcquired = false;
		mPool.recycle(bitmap);
	}

	/**
	 * 最新のビットマップを取得する
	 * コンストラクタで指定した同時取得可能な最大のビットマップ数を超えて取得しようとするとIllegalStateExceptionを投げる
	 * ビットマップが準備できていなければnullを返す
	 * null以外が返ったときは#recycleでビットマップを返却して再利用可能にすること
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
	 * 次のビットマップを取得する
	 * コンストラクタで指定した同時取得可能な最大のビットマップ数を超えて取得しようとするとIllegalStateExceptionを投げる
	 * ビットマップが準備できていなければnullを返す
	 * null以外が返ったときは#recycleでビットマップを返却して再利用可能にすること
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

//--------------------------------------------------------------------------------
// ワーカースレッド上での処理
	@WorkerThread
	protected void onFrameAvailable(final int texId, @Size(min=16) @NonNull final float[] texMatrix) {
		final Bitmap bitmap = obtainBitmap();
		if (bitmap != null) {
			mAllBitmapAcquired = false;
			final int width = getWidth();
			final int height = getHeight();
//			// OESテクスチャをオフスクリーン(マスターサーフェース)へ描画
			if (mReadSurface == null) {
				try {
					// テクスチャをバックバッファとしてアクセスできるようにGLSurfaceでラップする
					mReadSurface = GLSurface.wrap(isGLES3(),
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
			callOnFrameAvailable();
		} else {
			mAllBitmapAcquired = true;
			if (DEBUG) Log.w(TAG, "handleDraw: failed to obtain bitmap from pool!");
		}
	}


	/**
	 * 映像入力用Surfaceを破棄する
	 */
	@SuppressLint("NewApi")
	@WorkerThread
	@Override
	protected void handleReleaseInputSurface() {
		if (DEBUG) Log.v(TAG, "handleReleaseInputSurface:");
		super.handleReleaseInputSurface();
		if (mReadSurface != null) {
			mReadSurface.release();
			mReadSurface = null;
		}
	}

	@Nullable
	private Bitmap obtainBitmap() {
		Bitmap result = mPool.obtain();
		if (result == null) {
			// フレームプールが空の時はキューの一番古い物を取得する
			synchronized (mQueue) {
				result = mQueue.pollFirst();
			}
		}
		return result;
	}

}
