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
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;

import com.serenegiant.graphics.BitmapHelper;
import com.serenegiant.system.BuildCheck;
import com.serenegiant.utils.Pool;

import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

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
public class SurfaceImageReader {
	private static final boolean DEBUG = false;
	private static final String TAG = SurfaceImageReader.class.getSimpleName();

	public interface OnImageAvailableListener {
		public void onImageAvailable(@NonNull final SurfaceImageReader reader);
	}

	private static final int REQUEST_DRAW = 1;
	private static final int REQUEST_RECREATE_MASTER_SURFACE = 5;

	@NonNull
	private final Object mSync = new Object();
	@NonNull
	private final Object mReleaseLock = new Object();
	private final int mWidth;
	private final int mHeight;
	@NonNull
	private final Bitmap.Config mConfig;
	private final int mMaxImages;
	@NonNull
	private final Pool<Bitmap> mPool;
	@NonNull
	private final LinkedBlockingDeque<Bitmap> mQueue = new LinkedBlockingDeque<>();
	@NonNull
	private final EglTask mEglTask;
	@Nullable
	private OnImageAvailableListener mListener;
	@Nullable
	private Handler mListenerHandler;
	@NonNull
	private final Paint mPaint = new Paint();
	@Size(min=16)
	@NonNull
	final float[] mTexMatrix = new float[16];
	@NonNull
	private final ByteBuffer mWorkBuffer;
	private int mTexId;
	private SurfaceTexture mInputTexture;
	private Surface mInputSurface;
	/**
	 * SurfaceTextureへ割り当てたテクスチャをバックバッファとしてラップして
	 * 読み取り可能にするためのGLSurface
	 */
	private GLSurface mReadSurface;
	private boolean mIsReaderValid = false;
	private volatile boolean mAllBitmapAcquired = false;

	/**
	 * コンストラクタ
	 * @param width
	 * @param height
	 * @param config 取得するビットマップのConfig
	 * @param maxImages 同時に取得できるビットマップの最大数
	 */
	public SurfaceImageReader(
		@IntRange(from = 1) final int width, @IntRange(from = 1) final int height,
		@NonNull final Bitmap.Config config, final int maxImages) {

		mWidth = width;
		mHeight = height;
		mConfig = config;
		mMaxImages = maxImages;
		mPool = new Pool<Bitmap>(1, mMaxImages) {
			@NonNull
			@Override
			protected Bitmap createObject(@Nullable final Object... args) {
				return Bitmap.createBitmap(width, height, config);
			}
		};
		final Semaphore sem = new Semaphore(0);
		// GLDrawer2Dでマスターサーフェースへ描画しなくなったのでEglTask内で保持する
		// マスターサーフェースは最小サイズ(1x1)でOK
		mEglTask = new EglTask(GLUtils.getSupportedGLVersion(), null, 0) {
			@Override
			protected void onStart() {
				handleOnStart();
			}

			@Override
			protected void onStop() {
				handleOnStop();
			}

			@Override
			protected Object processRequest(final int request,
				final int arg1, final int arg2, final Object obj)
					throws TaskBreak {
				if (DEBUG) Log.v(TAG, "processRequest:");
				final Object result =  handleRequest(request, arg1, arg2, obj);
				if ((request == REQUEST_RECREATE_MASTER_SURFACE)
					&& (sem.availablePermits() == 0)) {
					sem.release();
				}
				return result;
			}
		};
		mWorkBuffer = ByteBuffer.allocateDirect(width * height * BitmapHelper.getPixelBytes(config));
		new Thread(mEglTask, TAG).start();
		if (!mEglTask.waitReady()) {
			// 初期化に失敗した時
			throw new RuntimeException("failed to start renderer thread");
		}
		mEglTask.offer(REQUEST_RECREATE_MASTER_SURFACE);
		try {
			final Surface surface;
			synchronized (mSync) {
				surface = mInputSurface;
			}
			if (surface == null) {
				if (sem.tryAcquire(1000, TimeUnit.MILLISECONDS)) {
					mIsReaderValid = true;
				} else {
					throw new RuntimeException("failed to create surface");
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
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

	/**
	 * 関係するリソースを破棄する、再利用はできない
	 */
	public void release() {
		if (DEBUG) Log.v(TAG, "release:");
		setOnImageAvailableListener(null, null);
		synchronized (mReleaseLock) {
			mEglTask.release();
			mIsReaderValid = false;
		}
		synchronized (mQueue) {
			mQueue.clear();
		}
		mPool.clear();
	}

	/**
	 * 映像サイズ(幅)を取得
	 * @return
	 */
	public int getWidth() {
		return mWidth;
	}

	/**
	 * 映像サイズ(高さ)を取得
	 * @return
	 */
	public int getHeight() {
		return mHeight;
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
	 * 同時に取得できる最大のビットマップの数を取得
	 * @return
	 */
	public int getMaxImages() {
		return mMaxImages;
	}

	/**
	 * 映像受け取り用のSurfaceを取得
	 * 既に破棄されているなどしてsurfaceが取得できないときはIllegalStateExceptionを投げる
	 *
	 * @return
	 * @throws IllegalStateException
	 */
	@NonNull
	public Surface getSurface() throws IllegalStateException {
		synchronized (mSync) {
			if (mInputSurface == null) {
				throw new IllegalStateException("surface not ready, already released?");
			}
			return mInputSurface;
		}
	}

	/**
	 * 読み取った映像データの準備ができたときのコールバックリスナーを登録
	 * @param listener
	 * @param handler
	 * @throws IllegalArgumentException
	 */
	public void setOnImageAvailableListener(
		@Nullable final OnImageAvailableListener listener,
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
	 * Bitmapを再利用可能にする
	 * @param bitmap
	 */
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
	/**
	 * ワーカースレッド開始時の処理(ここはワーカースレッド上)
	 */
	@WorkerThread
	protected final void handleOnStart() {
		if (DEBUG) Log.v(TAG, "handleOnStart:");
	}

	/**
	 * ワーカースレッド終了時の処理(ここはまだワーカースレッド上)
	 */
	@WorkerThread
	protected final void handleOnStop() {
		if (DEBUG) Log.v(TAG, "handleOnStop:");
		handleReleaseInputSurface();
	}

	@WorkerThread
	protected Object handleRequest(final int request,
		final int arg1, final int arg2, final Object obj) {

		switch (request) {
		case REQUEST_DRAW:
			handleDraw();
			break;
		case REQUEST_RECREATE_MASTER_SURFACE:
			handleReCreateInputSurface();
			break;
		default:
			if (DEBUG) Log.v(TAG, "handleRequest:" + request);
			break;
		}
		return null;
	}

	private int drawCnt;

	@WorkerThread
	protected void handleDraw() {
		if (DEBUG && ((++drawCnt % 100) == 0)) Log.v(TAG, "handleDraw:" + drawCnt);
		mEglTask.removeRequest(REQUEST_DRAW);
		try {
			mEglTask.makeCurrent();
			// 何も描画しないとハングアップする端末があるので適当に塗りつぶす
			GLES20.glClearColor(0.5f, 0.5f, 0.5f, 0.5f);
			mEglTask.swap();
			mInputTexture.updateTexImage();
			mInputTexture.getTransformMatrix(mTexMatrix);
		} catch (final Exception e) {
			Log.e(TAG, "handleDraw:thread id =" + Thread.currentThread().getId(), e);
			return;
		}
		final Bitmap bitmap = obtainBitmap();
		if (bitmap != null) {
			mAllBitmapAcquired = false;
//			// OESテクスチャをオフスクリーン(マスターサーフェース)へ描画
			if (mReadSurface == null) {
				try {
					// テクスチャをバックバッファとしてアクセスできるようにGLSurfaceでラップする
					mReadSurface = GLSurface.wrap(mEglTask.isGLES3(),
						GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE1, mTexId,
						mWidth, mHeight, false);
				} catch (final Exception e) {
					Log.w(TAG, e);
					return;
				}
			}
			mReadSurface.makeCurrent();
			// オフスクリーンから読み取り
			mWorkBuffer.clear();
			GLUtils.glReadPixels(mWorkBuffer, mWidth, mHeight);
			// Bitmapへ代入
			mWorkBuffer.clear();
			bitmap.copyPixelsFromBuffer(mWorkBuffer);
			synchronized (mQueue) {
				mQueue.addLast(bitmap);
			}
			synchronized (mSync) {
				if (mListenerHandler != null) {
					mListenerHandler.removeCallbacks(mOnImageAvailableTask);
					mListenerHandler.post(mOnImageAvailableTask);
				} else if (DEBUG) {
					Log.w(TAG, "handleDraw: Unexpectedly listener handler is null!");
				}
			}
		} else {
			mAllBitmapAcquired = true;
			if (DEBUG) Log.w(TAG, "handleDraw: failed to obtain bitmap from pool!");
		}
	}

	private final Runnable mOnImageAvailableTask = new Runnable() {
		@Override
		public void run() {
			synchronized (mSync) {
				if (mListener != null) {
					mListener.onImageAvailable(SurfaceImageReader.this);
				}
			}
		}
	};

	/**
	 * 映像入力用SurfaceTexture/Surfaceを再生成する
	 */
	@SuppressLint("NewApi")
	@WorkerThread
	protected void handleReCreateInputSurface() {
		if (DEBUG) Log.v(TAG, "handleReCreateInputSurface:");
		synchronized (mSync) {
			mEglTask.makeCurrent();
			handleReleaseInputSurface();
			mEglTask.makeCurrent();
			mTexId = GLHelper.initTex(
				GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE0, GLES20.GL_NEAREST);
			mInputTexture = new SurfaceTexture(mTexId);
			mInputSurface = new Surface(mInputTexture);
			// XXX この時点ではSurfaceTextureへ渡したテクスチャへメモリーが割り当てられておらずGLSurfaceを生成できない。
			//     少なくとも1回はSurfaceTexture#updateTexImageが呼ばれた後でGLSurfaceでラップする
			if (BuildCheck.isAndroid4_1()) {
				mInputTexture.setDefaultBufferSize(mWidth, mHeight);
			}
			mInputTexture.setOnFrameAvailableListener(mOnFrameAvailableListener);
		}
	}

	/**
	 * 映像入力用Surfaceを破棄する
	 */
	@SuppressLint("NewApi")
	@WorkerThread
	protected void handleReleaseInputSurface() {
		if (DEBUG) Log.v(TAG, "handleReleaseInputSurface:");
		synchronized (mSync) {
			if (mReadSurface != null) {
				mReadSurface.release();
				mReadSurface = null;
			}
			if (mInputSurface != null) {
				try {
					mInputSurface.release();
				} catch (final Exception e) {
					Log.w(TAG, e);
				}
				mInputSurface = null;
			}
			if (mInputTexture != null) {
				try {
					mInputTexture.release();
				} catch (final Exception e) {
					Log.w(TAG, e);
				}
				mInputTexture = null;
			}
			if (mTexId != 0) {
				GLHelper.deleteTex(mTexId);
				mTexId = 0;
			}
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

	private final SurfaceTexture.OnFrameAvailableListener
		mOnFrameAvailableListener = new SurfaceTexture.OnFrameAvailableListener() {
		@Override
		public void onFrameAvailable(final SurfaceTexture surfaceTexture) {
//			if (DEBUG) Log.v(TAG, "onFrameAvailable:");
			mEglTask.offer(REQUEST_DRAW);
		}
	};
}
