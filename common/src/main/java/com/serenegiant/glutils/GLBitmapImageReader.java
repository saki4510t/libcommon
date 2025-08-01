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

import com.serenegiant.gl.GLDrawer2D;
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

/**
 * GLSurfaceReceiverを使ってBitmapとして映像を受け取るためのImageReader<Bitmap></>実装
 */
public class GLBitmapImageReader implements ImageReader<Bitmap>, GLSurfaceReceiver.Callback {
	private static final boolean DEBUG = false;
	private static final String TAG = GLBitmapImageReader.class.getSimpleName();

	/**
	 * テクスチャをオフスクリーン描画してオフスクリーンからビットマップへ読み込むかどうか
	 * XXX GLSurface#wrapでテクスチャをバックバッファへ割り当てるときに#assignTexture内で
	 *     フレームバッファオブジェクト関係でエラーになる端末がある。
	 * 		ex. NEC PC-TE507FAW(ANDROID6)
	 * 		どういう条件で起こるか不明だけどこの場合も、
	 * 		独立したGLSurfaceでオフスクリーンを生成
	 * 		    → オフスクリーンのGLSurfaceへGLDrawer2Dでレンダリング
	 * 		    → GLSurface#makeCurrentでGLSurfaceのオフスクリーンのバックバッファへ切り替え
	 * 		    → glReadPixelsで読み取る
	 * 		のであれば正常に読み取ることができる。
	 */
	private final boolean mUseOffscreenRendering;
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
	/**
	 * 受け取ったテクスチャからBitmapの生成をするかどうか
	 * #setOnImageAvailableListenerでリスナー/Handlerとしてnullを渡すと無効になる
	 * #setOnImageAvailableListenerへ有効なリスナー/Handlerを渡すと有効になる
	 */
	private volatile boolean mEnabled = false;
	/**
	 * キャプチャ回数
	 * -1: 無制限, 0: 無効, 1以上: 指定回数
	 */
	private int mNumCaptures;
	/**
	 * キャプチャ周期[ミリ秒]
	 */
	private long mIntervalsMs;
	/**
	 * キャプチャした回数
	 */
	private int mCaptureCnt;
	/**
	 * キャプチャしたシステム時刻[ミリ秒]
	 */
	private long mLastCaptureMs;
	/**
	 * キャプチャに使うオフスクリーン
	 */
	@Nullable
	private GLSurface mOffscreen;
	/**
	 * オフスクリーン描画用GLDrawer2D
	 */
	@Nullable
	private GLDrawer2D mDrawer;

	/**
	 * コンストラクタ
	 * @param width
	 * @param height
	 * @param maxImages
	 */
	public GLBitmapImageReader(
		final int width, final int height, final int maxImages) {

		this(width, height, Bitmap.Config.ARGB_8888, maxImages, false);
	}

	/**
	 * コンストラクタ
	 * @param width
	 * @param height
	 * @param maxImages
	 * @param useOffscreenRendering テクスチャを一旦オフスクリーンへ描画してオフスクリーンからビットマップへ読み込むかどうか
	 *                              一部機種でGLSurface#wrapが正常に動作しないことへのワークアラウンド
	 */
	public GLBitmapImageReader(
		final int width, final int height, final int maxImages,
		final boolean useOffscreenRendering) {

		this(width, height, Bitmap.Config.ARGB_8888, maxImages, useOffscreenRendering);
	}

	/**
	 * コンストラクタ
	 * @param width
	 * @param height
	 * @param config テクスチャからの読み取り処理の都合上現在は実質的にBitmap.Config.ARGB_8888のみ
	 * @param maxImages
	 * @param useOffscreenRendering テクスチャを一旦オフスクリーンへ描画してオフスクリーンからビットマップへ読み込むかどうか
	 *                              一部機種でGLSurface#wrapが正常に動作しないことへのワークアラウンド
	 */
	protected GLBitmapImageReader(
		final int width, final int height,
		@NonNull final Bitmap.Config config, final int maxImages,
		final boolean useOffscreenRendering) {

		if (DEBUG) Log.v(TAG, "コンストラクタ:");
		mUseOffscreenRendering = useOffscreenRendering;
		mConfig = config;
		mMaxImages = maxImages;
		mWidth = width;
		mHeight = height;
		mCaptureCnt = 0;
		mNumCaptures = -1;	// 無制限
		mLastCaptureMs = mIntervalsMs = 0L;
		mPool = new Pool<Bitmap>(1, maxImages, maxImages, mWidth, mHeight) {
			@NonNull
			@Override
			protected Bitmap createObject(@Nullable final Object... args) {
				final int w = (int)args[0];
				final int h = (int)args[1];
				return Bitmap.createBitmap(w, h, mConfig);
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

	/**
	 * 1回だけキャプチャ要求
	 */
	public void trigger() {
		trigger(1, 0L);
	}

	/**
	 * 指定した条件でキャプチャ要求
	 * @param numCaptures キャプチャ回数, -1: 無制限, 0: 無効, 1以上: 指定回数
	 * @param intervalsMs 複数回キャプチャする場合の周期[ミリ秒]
	 */
	public void trigger(final int numCaptures, final long intervalsMs) {
		if (DEBUG) Log.v(TAG, "trigger:num=" + numCaptures + ",intervalsMs=" + intervalsMs);
		mLock.lock();
		try {
			mCaptureCnt = 0;
			mNumCaptures = numCaptures;
			mIntervalsMs = intervalsMs;
			mLastCaptureMs = 0L;
		} finally {
			mLock.unlock();
		}
	}

	/**
	 * キャプチャ中であればキャンセルする
	 */
	public void cancel() {
		if (DEBUG) Log.v(TAG, "cancel");
		mLock.lock();
		try {
			mCaptureCnt = mNumCaptures = 0;
			mLastCaptureMs = mIntervalsMs = 0L;
		} finally {
			mLock.unlock();
		}
	}

//--------------------------------------------------------------------------------
// 	 GLSurfaceReceiver.Callbackの実装
//--------------------------------------------------------------------------------
	/**
	 * GLSurfaceReceiver.Callbackの実装
	 */
	@WorkerThread
	@Override
	public void onInitialize() {
		if (DEBUG) Log.v(TAG, "onInitialize:");
		// do nothing now
	}

	/**
	 * GLSurfaceReceiver.Callbackの実装
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
	 * GLSurfaceReceiver.Callbackの実装
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
	 * GLSurfaceReceiver.Callbackの実装
	 * @param surface
	 */
	@WorkerThread
	@Override
	public void onReleaseInputSurface(@NonNull final Surface surface) {
		if (DEBUG) Log.v(TAG, "onReleaseInputSurface:");
		mWorkBuffer = null;
		if (mOffscreen != null) {
			mOffscreen.release();
			mOffscreen = null;
		}
		if (mDrawer != null) {
			mDrawer.release();
			mDrawer = null;
		}
	}

	/**
	 * GLSurfaceReceiver.Callbackの実装
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
// 	 GLSurfaceReceiver.FrameAvailableCallbackの実装
//--------------------------------------------------------------------------------
	/**
	 * GLSurfaceReceiver.FrameAvailableCallbackの実装
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

//		if (DEBUG) Log.v(TAG, "onFrameAvailable:");
		// キャプチャするかどうかを判定
		final long current = System.currentTimeMillis();
		final boolean needCapture;
		mLock.lock();
		try {
			needCapture = mEnabled && (mNumCaptures != 0)
				&& ((mNumCaptures < 0) || (mCaptureCnt < mNumCaptures))
				&& (current - mLastCaptureMs > mIntervalsMs);
			if (needCapture) {
				mLastCaptureMs = current;
				mCaptureCnt++;
			}
		} finally {
			mLock.unlock();
		}
		if (!needCapture) return;

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
		if (mUseOffscreenRendering) {
			doCaptureOffscreen(isGLES3, isOES, width, height, texId, texMatrix);
		} else {
			doCapture(isGLES3, isOES, width, height, texId, texMatrix);
		}
		callOnFrameAvailable();
	}

	/**
	 * テクスチャをGLSurfaceでラップしてオフスクリーンとして読み取る
	 * @param isGLES3
	 * @param isOES
	 * @param width
	 * @param height
	 * @param texId
	 * @param texMatrix
	 */
	@WorkerThread
	private void doCapture(
		final boolean isGLES3, final boolean isOES,
		final int width, final int height,
		final int texId, @NonNull final float[] texMatrix) {

		final Bitmap bitmap = obtainBitmap(width, height);
		if (bitmap != null) {
			mAllBitmapAcquired = false;
			final Bitmap readBitmap = GLUtils.glCopyTextureToBitmap(isOES, width, height, texId, texMatrix, mWorkBuffer);
			mWorkBuffer.clear();
			readBitmap.copyPixelsToBuffer(mWorkBuffer);
			mWorkBuffer.flip();
			bitmap.copyPixelsFromBuffer(mWorkBuffer);
			synchronized (mQueue) {
				mQueue.addLast(bitmap);
			}
		} else {
			mAllBitmapAcquired = true;
			if (DEBUG) Log.w(TAG, "doCapture: failed to obtain bitmap from pool!");
		}
	}

	@WorkerThread
	private void doCaptureOffscreen(
		final boolean isGLES3, final boolean isOES,
		final int width, final int height,
		final int texId, @NonNull final float[] texMatrix) {

		if ((mOffscreen == null)
			|| (mOffscreen.getWidth() != width)
			|| (mOffscreen.getHeight() != height)
			|| (mOffscreen.isGLES3 != isGLES3)) {
			if (mOffscreen != null) {
				mOffscreen.release();
			}
			mOffscreen = GLSurface.newInstance(isGLES3, GLES20.GL_TEXTURE0, width, height);
		}
		if ((mDrawer == null) || (mDrawer.isGLES3 != isGLES3) || (mDrawer.isOES() != isOES)) {
			if (mDrawer != null) {
				mDrawer.release();
			}
			mDrawer = GLDrawer2D.create(isGLES3, isOES);
		}
		final Bitmap bitmap = obtainBitmap(width, height);
		if ((mOffscreen != null) && (mDrawer != null) && (bitmap != null)) {
			// オフスクリーンSurfaceへ描画
			mOffscreen.makeCurrent();
			mOffscreen.setViewPort(0, 0, width, height);
			// 本来は映像が全面に描画されるので#glClearでクリアする必要はないけど
			// ハングアップする機種があるのでクリアしとく
			GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
			// GLDrawer2Dでオフスクリーンへ描画
			mDrawer.draw(GLES20.GL_TEXTURE0, texId, texMatrix, 0);
			// glReadPixelsでBitmapへ読み込む
			final ByteBuffer buffer = GLUtils.glReadPixels(mWorkBuffer, width, height);
			mOffscreen.swap();
			bitmap.copyPixelsFromBuffer(buffer);
			synchronized (mQueue) {
				mQueue.addLast(bitmap);
			}
		} else {
			mAllBitmapAcquired = true;
			if (DEBUG) Log.w(TAG, "doCaptureOffscreen: failed to obtain bitmap from pool!");
		}
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
						"handler is null but the current thread does not have a looper");
				}
				if (mListenerHandler == null || mListenerHandler.getLooper() != looper) {
					mListenerHandler = new Handler(looper);
				}
				mListener = listener;
			} else {
				mListener = null;
				mListenerHandler = null;
			}
			mEnabled = (mListener != null) && (mListenerHandler != null);
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
	 * テクスチャからBitmapを生成するかどうかをセット
	 * 有効なOnImageAvailableListenerとHandlerが設定されていなければ常にfalseがセットされる
	 * @param enabled
	 */
	@Override
	public void setEnabled(final Boolean enabled) {
		mEnabled = enabled && (mListener != null) && (mListenerHandler != null);
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
