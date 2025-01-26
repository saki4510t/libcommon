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

import android.annotation.SuppressLint;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Surface;

import com.serenegiant.gl.GLUtils;
import com.serenegiant.gl.GLManager;
import com.serenegiant.system.BuildCheck;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import androidx.annotation.CallSuper;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Size;
import androidx.annotation.WorkerThread;

import static com.serenegiant.gl.GLConst.GL_TEXTURE_EXTERNAL_OES;

/**
 * Surfaceを経由して映像をテクスチャとして受け取るためのクラスの基本部分を実装した抽象クラス
 */
public class GLImageReceiver {
	private static final boolean DEBUG = false;
	private static final String TAG = GLImageReceiver.class.getSimpleName();

	private static final int REQUEST_UPDATE_TEXTURE = 1;
	private static final int REQUEST_UPDATE_SIZE = 2;
	private static final int REQUEST_RECREATE_MASTER_SURFACE = 5;

	/**
	 * Surfaceを経由してテクスチャとして受け取った映像を処理するためのインターフェース
	 * WorkerThreadアノテーションの付いているインターフェースメソッドは全てGLコンテキストを
	 * 保持したスレッド上で実行される
	 */
	public interface Callback {
		@WorkerThread
		public void onInitialize(@NonNull final GLImageReceiver receiver);
		/**
		 * 関係するリソースを破棄する
		 */
		@WorkerThread
		public void onRelease();
		/**
		 * 映像入力用Surfaceが生成されたときの処理
		 */
		@WorkerThread
		public void onCreateInputSurface(@NonNull final GLImageReceiver receiver);
		/**
		 * 映像入力用Surfaceが破棄されるときの処理
		 */
		@WorkerThread
		public void onReleaseInputSurface(@NonNull final GLImageReceiver receiver);
		/**
		 * 映像サイズ変更要求が来たときの処理
		 * @param width
		 * @param height
		 */
		@WorkerThread
		public void onResize(final int width, final int height);
		/**
		 * 映像をテクスチャとして受け取ったときの処理
		 * @param receiver
		 * @param isOES
		 * @param texId
		 * @param texMatrix
		 * @return true: #onImageAvailableコールバックメソッドを呼び出す, false: 呼び出さない
		 */
		@WorkerThread
		public void onFrameAvailable(
			@NonNull final GLImageReceiver receiver,
			final boolean isOES,
			final int texId, @Size(min=16) @NonNull final float[] texMatrix);
	}

	/**
	 * 排他制御用
	 */
	@NonNull
	private final ReentrantLock mLock = new ReentrantLock();
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
	private final Callback mCallback;
	private volatile boolean mReleased = false;
	private boolean mIsReaderValid = false;

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
	 * @param callback
	 */
	public GLImageReceiver(
		@IntRange(from=1) final int width, @IntRange(from=1) final int height,
		@NonNull final Callback callback) {

		this(new GLManager(), false, width, height, callback);
	}

	/**
	 * コンストラクタ
	 * @param width
	 * @param height
	 * @param callback
	 */
	public GLImageReceiver(
		@NonNull final GLManager manager, final boolean useSharedContext,
		@IntRange(from=1) final int width, @IntRange(from=1) final int height,
		@NonNull final Callback callback) {

		mOwnManager = useSharedContext;
		final Handler.Callback handlerCallback
			= new Handler.Callback() {
			@Override
			public boolean handleMessage(@NonNull final Message msg) {
				return GLImageReceiver.this.handleMessage(msg);
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
		mCallback = callback;
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
			mLock.lock();
			try {
				surface = mInputSurface;
			} finally {
				mLock.unlock();
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
	 * GLManagerを取得する
	 * @return
	 */
	public GLManager getGLManager() {
		return mManager;
	}

	/**
	 * GLImageReaderが破棄されておらず利用可能かどうかを取得
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
		mLock.lock();
		try {
			return mWidth;
		} finally {
			mLock.unlock();
		}
	}

	/**
	 * 映像サイズ(高さ)を取得
	 * @return
	 */
	public int getHeight() {
		mLock.lock();
		try {
			return mHeight;
		} finally {
			mLock.unlock();
		}
	}

	/**
	 * テクスチャ名を取得
	 * @return
	 */
	public int getTexId() {
		mLock.lock();
		try {
			if (mInputSurface == null) {
				throw new IllegalStateException("surface not ready, already released?");
			}
			return mTexId;
		} finally {
			mLock.unlock();
		}
	}

	/**
	 * テクスチャ変換行列を取得
	 * @return
	 */
	@Size(min=16)
	@NonNull
	public float[] getTexMatrix() {
		mLock.lock();
		try {
			return mTexMatrix;
		} finally {
			mLock.unlock();
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
		mLock.lock();
		try {
			if (mInputSurface == null) {
				throw new IllegalStateException("surface not ready, already released?");
			}
			return mInputSurface;
		} finally {
			mLock.unlock();
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
		mLock.lock();
		try {
			if (mInputTexture == null) {
				throw new IllegalStateException("surface not ready, already released?");
			}
			return mInputTexture;
		} finally {
			mLock.unlock();
		}
	}

	protected boolean isGLES3() {
		return mManager.isGLES3();
	}

	public void resize(@IntRange(from=1) final int width, @IntRange(from=1) final int height) {
		final int _width = Math.max(width, 1);
		final int _height = Math.max(height, 1);
		mLock.lock();
		try {
			if ((mWidth != _width) || (mHeight != _height)) {
				mGLHandler.sendMessage(mGLHandler.obtainMessage(REQUEST_UPDATE_SIZE, _width, _height));
			}
		} finally {
			mLock.unlock();
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
		mCallback.onInitialize(this);
	}

	/**
	 * ワーカースレッド終了時の処理(ここはまだワーカースレッド上)
	 */
	@WorkerThread
	private void handleOnStop() {
		if (DEBUG) Log.v(TAG, "handleOnStop:");
		handleReleaseInputSurface();
		mCallback.onRelease();
	}

	@WorkerThread
	protected boolean handleMessage(@NonNull final Message msg) {
		switch (msg.what) {
		case REQUEST_UPDATE_TEXTURE -> {
			handleUpdateTexImage();
			return true;
		}
		case REQUEST_UPDATE_SIZE -> {
			handleResize(msg.arg1, msg.arg2);
			return true;
		}
		case REQUEST_RECREATE_MASTER_SURFACE -> {
			handleReCreateInputSurface();
			return true;
		}
		default -> {
			if (DEBUG) Log.v(TAG, "handleRequest:" + msg);
		}
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
		mCallback.onFrameAvailable(this, true, mTexId, mTexMatrix);
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
		mLock.lock();
		try {
			mWidth = width;
			mHeight = height;
		} finally {
			mLock.unlock();
		}
		if (BuildCheck.isAndroid4_1() && (mInputTexture != null)) {
			mInputTexture.setDefaultBufferSize(width, height);
		}
		mCallback.onResize(width, height);
	}

	/**
	 * 映像入力用SurfaceTexture/Surfaceを再生成する
	 */
	@SuppressLint("NewApi")
	@WorkerThread
	@CallSuper
	private void handleReCreateInputSurface() {
		if (DEBUG) Log.v(TAG, "handleReCreateInputSurface:");
		mLock.lock();
		try {
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
			mCallback.onCreateInputSurface(this);
			mInputTexture.setOnFrameAvailableListener(mOnFrameAvailableListener);
		} finally {
			mLock.unlock();
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
		mCallback.onReleaseInputSurface(this);
		mLock.lock();
		try {
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
		} finally {
			mLock.unlock();
		}
	}

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
