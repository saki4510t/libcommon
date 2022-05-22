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
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Surface;

import com.serenegiant.gl.GLUtils;
import com.serenegiant.gl.GLManager;
import com.serenegiant.system.BuildCheck;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import androidx.annotation.CallSuper;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;
import androidx.annotation.WorkerThread;

import static com.serenegiant.gl.GLConst.GL_TEXTURE_EXTERNAL_OES;

/**
 * Surfaceを経由して映像をテクスチャとして受け取るためのクラスの基本部分を実装した抽象クラス
 * @param <T>
 */
public class GLImageReader<T> {
	private static final boolean DEBUG = false;
	private static final String TAG = GLImageReader.class.getSimpleName();

	private static final int REQUEST_UPDATE_TEXTURE = 1;
	private static final int REQUEST_UPDATE_SIZE = 2;
	private static final int REQUEST_RECREATE_MASTER_SURFACE = 5;

	/**
	 * 映像を取得可能になったときに呼ばれるコールバックリスナー
	 * @param <T>
	 */
	public interface OnImageAvailableListener<T> {
		public void onImageAvailable(@NonNull final GLImageReader<T> reader);
	}

	/**
	 * Surfaceを経由してテクスチャとして受け取った映像を処理するためのインターフェース
	 * WorkerThreadアノテーションの付いているインターフェースメソッドは全てGLコンテキストを
	 * 保持したスレッド上で実行される
	 * @param <T>
	 */
	public interface ImageHandler<T> {
		@WorkerThread
		public void onInitialize(@NonNull final GLImageReader<T> reader);
		/**
		 * 関係するリソースを破棄する
		 */
		@WorkerThread
		public void onRelease();
		/**
		 * 映像入力用Surfaceが生成されたときの処理
		 */
		@WorkerThread
		public void onCreateInputSurface(@NonNull final GLImageReader<T> reader);
		/**
		 * 映像入力用Surfaceが破棄されるときの処理
		 */
		@WorkerThread
		public void onReleaseInputSurface(@NonNull final GLImageReader<T> reader);
		/**
		 * 映像サイズ変更要求が来たときの処理
		 * @param width
		 * @param height
		 */
		@WorkerThread
		public void onResize(final int width, final int height);
		/**
		 * 映像をテクスチャとして受け取ったときの処理
		 * @param reader
		 * @param texId
		 * @param texMatrix
		 */
		@WorkerThread
		public boolean onFrameAvailable(
			@NonNull final GLImageReader<T> reader,
			final int texId, @Size(min=16) @NonNull final float[] texMatrix);

		/**
		 * 最新の映像を取得する。最新以外の古い映像は全てrecycleされる。
		 * コンストラクタで指定した同時取得可能な最大の映像数を超えて取得しようとするとIllegalStateExceptionを投げる
		 * 映像が準備できていなければnullを返す
		 * null以外が返ったときは#recycleで返却して再利用可能にすること
		 * @return
		 * @throws IllegalStateException
		 */
		@Nullable
		public T onAcquireLatestImage() throws IllegalStateException;
		/**
		 * 次の映像を取得する
		 * コンストラクタで指定した同時取得可能な最大の映像数を超えて取得しようとするとIllegalStateExceptionを投げる
		 * 映像がが準備できていなければnullを返す
		 * null以外が返ったときは#recycleで返却して再利用可能にすること
		 * @return
		 * @throws IllegalStateException
		 */
		@Nullable
		public T onAcquireNextImage() throws IllegalStateException;
		/**
		 * 使った映像を返却して再利用可能にする
		 * @param image
		 */
		public void onRecycle(@NonNull T image);
	}

	@NonNull
	private final Object mSync = new Object();
	@NonNull
	private final GLManager mManager;
	/**
	 * 自分用のGLManagerを保持しているかどうか
	 */
	private final boolean mOwnManager;
	@NonNull
	private final Handler mGLHandler;
	private int mWidth;
	private int mHeight;
	@NonNull
	private final ImageHandler<T> mImageHandler;
	private volatile boolean mReleased = false;
	private boolean mIsReaderValid = false;
	@Nullable
	private OnImageAvailableListener<T> mListener;
	@Nullable
	private Handler mListenerHandler;

	// 映像受け取り用テクスチャ/SurfaceTexture/Surface関係
	@Size(min=16)
	@NonNull
	final float[] mTexMatrix = new float[16];
	private int mTexId;
	private SurfaceTexture mInputTexture;
	private Surface mInputSurface;

	/**
	 * コンストラクタ
	 * @param width
	 * @param height
	 * @param imageHandler
	 */
	public GLImageReader(
		@IntRange(from=1) final int width, @IntRange(from=1) final int height,
		@NonNull final ImageHandler<T> imageHandler) {

		this(new GLManager(), false, width, height, imageHandler);
	}

	/**
	 * コンストラクタ
	 * @param width
	 * @param height
	 * @param imageHandler
	 */
	public GLImageReader(
		@NonNull final GLManager manager, final boolean useSharedContext,
		@IntRange(from=1) final int width, @IntRange(from=1) final int height,
		@NonNull final ImageHandler<T> imageHandler) {

		mOwnManager = useSharedContext;
		final Handler.Callback handlerCallback
			= new Handler.Callback() {
			@Override
			public boolean handleMessage(@NonNull final Message msg) {
				return GLImageReader.this.handleMessage(msg);
			}
		};
		if (useSharedContext) {
			mManager = manager.createShared(handlerCallback);
			mGLHandler = mManager.getGLHandler();
		} else {
			mManager = manager;
			mGLHandler = manager.createGLHandler(handlerCallback);
		}
		mWidth = width;
		mHeight = height;
		mImageHandler = imageHandler;
		final Semaphore sem = new Semaphore(0);
		mGLHandler.post(new Runnable() {
			@Override
			public void run() {
				handleOnStart();
				sem.release();
			}
		});
		// 初期化待ち
		try {
			if (!sem.tryAcquire(1000, TimeUnit.MILLISECONDS)) {
				throw new RuntimeException("failed to init");
			}
		} catch (final InterruptedException e) {
			throw new RuntimeException("failed to init", e);
		}
		// 映像受け取り用のテクスチャ/SurfaceTexture/Surfaceを生成
//		mEglTask.offer(REQUEST_RECREATE_MASTER_SURFACE);
		sem.release(sem.availablePermits());
		mGLHandler.post(new Runnable() {
			@Override
			public void run() {
				handleReCreateInputSurface();
				sem.release();
			}
		});
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
		} catch (final InterruptedException e) {
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
	public final void release() {
		if (!mReleased) {
			if (DEBUG) Log.v(TAG, "release:");
			mReleased = true;
			setOnImageAvailableListener(null, null);
			mIsReaderValid = false;
			internalRelease();
		}
	}

	protected void internalRelease() {
		if (mManager.isValid()) {
			mGLHandler.post(new Runnable() {
				@Override
				public void run() {
					handleOnStop();
					if (mOwnManager) {
						mManager.release();
					}
				}
			});
		}
	}

	/**
	 * GLSurfaceReaderが破棄されておらず利用可能かどうかを取得
	 * @return
	 */
	public boolean isValid() {
		return !mReleased && mIsReaderValid && mManager.isValid();
	}

	/**
	 * 映像サイズ(幅)を取得
	 * @return
	 */
	public int getWidth() {
		synchronized (mSync) {
			return mWidth;
		}
	}

	/**
	 * 映像サイズ(高さ)を取得
	 * @return
	 */
	public int getHeight() {
		synchronized (mSync) {
			return mHeight;
		}
	}

	/**
	 * 映像受け取り用のSurfaceを取得
	 * 既に破棄されているなどしてSurfaceが取得できないときはIllegalStateExceptionを投げる
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
	 * 映像受け取り用のSurfaceTextureを取得
	 * 既に破棄されているなどしてSurfaceTextureが取得できないときはIllegalStateExceptionを投げる
	 *
	 * @return
	 * @throws IllegalStateException
	 */
	@NonNull
	public SurfaceTexture getSurfaceTexture() throws IllegalStateException {
		synchronized (mSync) {
			if (mInputTexture == null) {
				throw new IllegalStateException("surface not ready, already released?");
			}
			return mInputTexture;
		}
	}

	/**
	 * 読み取った映像データの準備ができたときのコールバックリスナーを登録
	 * @param listener
	 * @param handler
	 * @throws IllegalArgumentException
	 */
	public void setOnImageAvailableListener(
		@Nullable final OnImageAvailableListener<T> listener,
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

	protected boolean isGLES3() {
		return mManager.isGLES3();
	}

	/**
	 * 最新の映像を取得する。最新以外の古い映像は全てrecycleされる。
	 * コンストラクタで指定した同時取得可能な最大の映像数を超えて取得しようとするとIllegalStateExceptionを投げる
	 * 映像が準備できていなければnullを返す
	 * null以外が返ったときは#recycleで返却して再利用可能にすること
	 * @return
	 * @throws IllegalStateException
	 */
	@Nullable
	public T acquireLatestImage() throws IllegalStateException {
		return mImageHandler.onAcquireLatestImage();
	}

	/**
	 * 次の映像を取得する
	 * コンストラクタで指定した同時取得可能な最大の映像数を超えて取得しようとするとIllegalStateExceptionを投げる
	 * 映像がが準備できていなければnullを返す
	 * null以外が返ったときは#recycleで返却して再利用可能にすること
	 * @return
	 * @throws IllegalStateException
	 */
	@Nullable
	public T acquireNextImage() throws IllegalStateException {
		return mImageHandler.onAcquireNextImage();
	}

	/**
	 * 使った映像を返却して再利用可能にする
	 * @param image
	 */
	public void recycle(@NonNull T image) {
		mImageHandler.onRecycle(image);
	}

	public void resize(@IntRange(from=1) final int width, @IntRange(from=1) final int height) {
		final int _width = Math.max(width, 1);
		final int _height = Math.max(height, 1);
		synchronized (mSync) {
			if ((mWidth != _width) || (mHeight != _height)) {
				mGLHandler.sendMessage(mGLHandler.obtainMessage(REQUEST_UPDATE_SIZE, _width, _height));
			}
		}
	}

//--------------------------------------------------------------------------------
// ワーカースレッド上での処理
	/**
	 * ワーカースレッド開始時の処理(ここはワーカースレッド上)
	 */
	@WorkerThread
	private void handleOnStart() {
		if (DEBUG) Log.v(TAG, "handleOnStart:");
		mImageHandler.onInitialize(this);
	}

	/**
	 * ワーカースレッド終了時の処理(ここはまだワーカースレッド上)
	 */
	@WorkerThread
	private void handleOnStop() {
		if (DEBUG) Log.v(TAG, "handleOnStop:");
		handleReleaseInputSurface();
		mImageHandler.onRelease();
	}

	@WorkerThread
	protected boolean handleMessage(@NonNull final Message msg) {
		switch (msg.what) {
		case REQUEST_UPDATE_TEXTURE:
			handleUpdateTexImage();
			return true;
		case REQUEST_UPDATE_SIZE:
			handleResize(msg.arg1, msg.arg2);
			return true;
		case REQUEST_RECREATE_MASTER_SURFACE:
			handleReCreateInputSurface();
			return true;
		default:
			if (DEBUG) Log.v(TAG, "handleRequest:" + msg);
			break;
		}
		return false;
	}

	/**
	 *
	 */
	private int drawCnt;
	@WorkerThread
	private void handleUpdateTexImage() {
		if (DEBUG && ((++drawCnt % 100) == 0)) Log.v(TAG, "handleDraw:" + drawCnt);
		try {
			mManager.makeDefault();
			// 何も描画しないとハングアップする端末があるので適当に塗りつぶす
			GLES20.glClearColor(0.5f, 0.5f, 0.5f, 0.5f);
			mManager.swap();
			mInputTexture.updateTexImage();
			mInputTexture.getTransformMatrix(mTexMatrix);
		} catch (final Exception e) {
			Log.e(TAG, "handleDraw:thread id =" + Thread.currentThread().getId(), e);
			return;
		}
		if (mImageHandler.onFrameAvailable(this, mTexId, mTexMatrix)) {
			callOnFrameAvailable();
		}
	}

	/**
	 * マスター映像サイズをリサイズ
	 * @param width
	 * @param height
	 */
	@SuppressLint("NewApi")
	@WorkerThread
	@CallSuper
	private void handleResize(final int width, final int height) {
		if (DEBUG) Log.v(TAG, String.format("handleResize:(%d,%d)", width, height));
		synchronized (mSync) {
			mWidth = width;
			mHeight = height;
		}
		if (BuildCheck.isAndroid4_1() && (mInputTexture != null)) {
			mInputTexture.setDefaultBufferSize(width, height);
		}
		mImageHandler.onResize(width, height);
	}

	/**
	 * 映像入力用SurfaceTexture/Surfaceを再生成する
	 */
	@SuppressLint("NewApi")
	@WorkerThread
	@CallSuper
	private void handleReCreateInputSurface() {
		if (DEBUG) Log.v(TAG, "handleReCreateInputSurface:");
		synchronized (mSync) {
			mManager.makeDefault();
			handleReleaseInputSurface();
			mManager.makeDefault();
			mTexId = GLUtils.initTex(
				GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE0, GLES20.GL_NEAREST);
			mInputTexture = new SurfaceTexture(mTexId);
			mInputSurface = new Surface(mInputTexture);
			// XXX この時点ではSurfaceTextureへ渡したテクスチャへメモリーが割り当てられておらずGLSurfaceを生成できない。
			//     少なくとも1回はSurfaceTexture#updateTexImageが呼ばれた後でGLSurfaceでラップする
			if (BuildCheck.isAndroid4_1()) {
				mInputTexture.setDefaultBufferSize(mWidth, mHeight);
			}
			mImageHandler.onCreateInputSurface(this);
			mInputTexture.setOnFrameAvailableListener(mOnFrameAvailableListener);
		}
	}

	/**
	 * 映像入力用Surfaceを破棄する
	 */
	@SuppressLint("NewApi")
	@WorkerThread
	@CallSuper
	private void handleReleaseInputSurface() {
		if (DEBUG) Log.v(TAG, "handleReleaseInputSurface:");
		mImageHandler.onReleaseInputSurface(this);
		synchronized (mSync) {
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
				GLUtils.deleteTex(mTexId);
				mTexId = 0;
			}
		}
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
					mListener.onImageAvailable(GLImageReader.this);
				}
			}
		}
	};

	/**
	 * 映像受け取り用のSurfaceTextureの映像が更新されたときのコールバックリスナー実装
	 */
	private final SurfaceTexture.OnFrameAvailableListener
		mOnFrameAvailableListener = new SurfaceTexture.OnFrameAvailableListener() {
		@Override
		public void onFrameAvailable(final SurfaceTexture surfaceTexture) {
//			if (DEBUG) Log.v(TAG, "onFrameAvailable:");
			mGLHandler.sendEmptyMessage(REQUEST_UPDATE_TEXTURE);
		}
	};
}
