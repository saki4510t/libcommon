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

import com.serenegiant.gl.GLManager;
import com.serenegiant.gl.GLUtils;
import com.serenegiant.system.BuildCheck;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import androidx.annotation.CallSuper;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;
import androidx.annotation.WorkerThread;

import static com.serenegiant.gl.GLConst.GL_TEXTURE_EXTERNAL_OES;

/**
 * Surfaceを経由して映像をテクスチャとして受け取るためのクラスの基本部分を実装
 * SurfaceTextureを使うので生成されるテクスチャはGL_TEXTURE_EXTERNAL_OES
 * FIXME RendererHolderだと入力ソースを変更時にSurfaceの再生成しなくても
 *       正常に動作するのにこっちはテクスチャが更新されなくなる
 */
public class GLSurfaceReceiver {
	private static final boolean DEBUG = false;
	private static final String TAG = GLSurfaceReceiver.class.getSimpleName();

	/**
	 * SurfaceTextureが映像を受け取ったときの更新要求
	 */
	private static final int REQUEST_UPDATE_TEXTURE = 1;
	/**
	 * 映像サイズ変更要求
	 */
	private static final int REQUEST_UPDATE_SIZE = 2;
	/**
	 * 映像受け取り用テクスチャ/SurfaceTexture/Surfaceの(再)生成要求
	 */
	private static final int REQUEST_RECREATE_INPUT_SURFACE = 5;
	/**
	 * Surfaceの生成待ち時間[ミリ秒]
	 */
	private static final long CREATE_SURFACE_WAIT_MS = 2500L;

	/**
	 * Surfaceを経由してテクスチャとして受け取った映像を処理するためのインターフェース
	 * WorkerThreadアノテーションの付いているインターフェースメソッドは全てGLコンテキストを
	 * 保持したスレッド上で実行される
	 */
	public interface Callback extends GLFrameAvailableCallback {
		@WorkerThread
		public default void onInitialize() {}
		/**
		 * 関係するリソースを破棄する
		 */
		@WorkerThread
		public default void onRelease() {}
		/**
		 * 映像入力用Surfaceが生成されたときの処理、生成直後に毎回呼ばれる
		 */
		@WorkerThread
		public default void onCreateInputSurface(@NonNull final Surface surface, final int width, final int height) {}
		/**
		 * 映像入力用Surfaceが破棄されるときの処理、これは実際に破棄される直前に毎回呼ばれる
		 */
		@WorkerThread
		public default void onReleaseInputSurface(@NonNull final Surface surface) {}
		/**
		 * 映像サイズ変更要求が来たときの処理
		 * @param width
		 * @param height
		 */
		@WorkerThread
		public default void onResize(final int width, final int height) {}
	}

	public static class DefaultCallback implements Callback {
		@NonNull
		private final GLFrameAvailableCallback mGLFrameAvailableCallback;
		public DefaultCallback(@NonNull final GLFrameAvailableCallback onGLFrameAvailableCallback) {
			mGLFrameAvailableCallback = onGLFrameAvailableCallback;
		}

		@Override
		public void onFrameAvailable(
			final boolean isGLES3, final boolean isOES,
			final int width, final int height,
			final int texId, @NonNull final float[] texMatrix) {
			mGLFrameAvailableCallback.onFrameAvailable(isGLES3, isOES, width, height, texId, texMatrix);
		}
	}
	/**
	 * 排他制御用
	 */
	@NonNull
	private final ReentrantLock mLock = new ReentrantLock();
	@NonNull
	private final GLManager mGLManager;
	@NonNull
	private final Handler mGLHandler;
	private int mWidth;
	private int mHeight;
	@NonNull
	private final Callback mCallback;
	@NonNull
	private GLFrameAvailableCallback mGLFrameAvailableCallback;
	private volatile boolean mReleased = false;
	private boolean mIsReceiverValid = false;

	/**
	 * 映像受け取り用SurfaceTextureの#getTransformMatrixで受け取った
	 * テクスチャ変換行列を保持する
	 */
	@Size(value=16)
	@NonNull
	final float[] mTexMatrix = new float[16];
	/**
	 * 映像受け取り用のSurfaceTextureでラップしたテクスチャID
	 */
	private int mTexId;
	/**
	 * 映像受け取り用SurfaceTexture
	 */
	@Nullable
	private SurfaceTexture mInputTexture;
	/**
	 * 映像受け取り用SurfaceTextureから生成したSurface
	 */
	@Nullable
	private Surface mInputSurface;

	/**
	 * コンストラクタ
	 * 映像入力用Surfacetexture/Surfaceが生成されるまで実行がブロックされる
	 * @param glManager
	 * @param width
	 * @param height
	 * @param callback
	 */
	public GLSurfaceReceiver(
		@NonNull final GLManager glManager,
		@IntRange(from=1) final int width, @IntRange(from=1) final int height,
		@NonNull final Callback callback) {

		final Handler.Callback handlerCallback
			= new Handler.Callback() {
			@Override
			public boolean handleMessage(@NonNull final Message msg) {
				return GLSurfaceReceiver.this.handleMessage(msg);
			}
		};
		mGLManager = glManager;
		mGLHandler = glManager.createGLHandler(handlerCallback);
		mWidth = Math.max(width, 1);
		mHeight = Math.max(height, 1);
		mCallback = callback;
		mGLFrameAvailableCallback = mCallback;
		final Semaphore sem = new Semaphore(0);	// CountdownLatchの方が良いかも?
		mGLHandler.postAtFrontOfQueue(() -> {
			try {
				handleOnStartOnGL();
			} catch (final Exception e) {
				if (DEBUG) Log.w(TAG, e);
			}
			try {
				handleReCreateInputSurfaceOnGL();
			} catch (final Exception e) {
				if (DEBUG) Log.w(TAG, e);
			}
			sem.release();
		});
		try {
			if (sem.tryAcquire(CREATE_SURFACE_WAIT_MS, TimeUnit.MILLISECONDS)) {
				final Surface surface;
				mLock.lock();
				try {
					surface = mInputSurface;
				} finally {
					mLock.unlock();
				}
				mIsReceiverValid = (surface != null) && surface.isValid();
			} else {
				mIsReceiverValid = false;
			}
			if (!mIsReceiverValid) {
				throw new RuntimeException("failed to create surface");
			}
		} catch (final InterruptedException e) {
			// ignore
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
			mReleased = true;
			if (DEBUG) Log.v(TAG, "release:");
			mIsReceiverValid = false;
			internalRelease();
		}
	}

	/**
	 * FrameAvailableCallbackを差し替える
	 * @param callback callbackがnull以外のFrameAvailableCallbackを指定した場合にはコンストラクタで
	 *                 引き渡したコールバックの#onFrameAvailableは呼ばれない。
	 *                 callbackがnullの場合はコンストラクタで引き渡したコールバックに戻る。
	 */
	public void setFrameAvailableCallback(@Nullable final GLFrameAvailableCallback callback) {
		if (mGLManager.isValid()) {
			mGLHandler.post(() -> {
				if (callback != null) {
					mGLFrameAvailableCallback = callback;
				} else {
					mGLFrameAvailableCallback = mCallback;
				}
			});
		}
	}
	/**
	 * 関連するリソースを破棄する
	 */
	protected void internalRelease() {
		if (mGLManager.isValid()) {
			mGLHandler.postAtFrontOfQueue(() -> {
				handleOnStopOnGL();
			});
		}
	}

	/**
	 * GLManagerを取得する
	 * @return
	 */
	public GLManager getGLManager() {
		return mGLManager;
	}

	/**
	 * GLImageReaderが破棄されておらず利用可能かどうかを取得
	 * @return
	 */
	public boolean isValid() {
		return !mReleased && mIsReceiverValid && mGLManager.isValid();
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
	@Size(value=16)
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
		checkInputSurface();
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
		checkInputSurface();
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

	/**
	 * GL|ES3を使っているかどうか
	 * @return
	 */
	protected boolean isGLES3() {
		return mGLManager.isGLES3();
	}

	/**
	 * 映像サイズを変更要求
	 * @param width
	 * @param height
	 */
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

	/**
	 * 映像入力用Surface/Surfacetextureを(再)生成する
	 * 呼び出し元スレッドをブロックする
	 */
	public void reCreateInputSurface() {
		final Semaphore sem = new Semaphore(0);	// CountdownLatchの方が良いかも?
		mGLHandler.post(() -> {
			try {
				handleReCreateInputSurfaceOnGL();
			} catch (final Exception e) {
				if (DEBUG) Log.w(TAG, e);
			}
			sem.release();
		});
		try {
			if (sem.tryAcquire(CREATE_SURFACE_WAIT_MS, TimeUnit.MILLISECONDS)) {
				final Surface surface;
				mLock.lock();
				try {
					surface = mInputSurface;
				} finally {
					mLock.unlock();
				}
				mIsReceiverValid = (surface != null) && surface.isValid();
			} else {
				mIsReceiverValid = false;
			}
			if (!mIsReceiverValid) {
				throw new RuntimeException("failed to create surface");
			}
		} catch (final InterruptedException e) {
			// ignore
		}
	}

	/**
	 * 分配描画用のマスターSurfaceが有効かどうかをチェックして無効なら再生成する
	 * @throws IllegalStateException
	 */
	private void checkInputSurface() throws IllegalStateException {
		if (!isValid()) throw new IllegalStateException("Already released?");
		if ((mInputSurface == null) || (!mInputSurface.isValid())) {
			if (DEBUG) Log.d(TAG, "checkInputSurface:invalid master surface");
			reCreateInputSurface();
		}
	}

//--------------------------------------------------------------------------------
// ワーカースレッド上での処理
	/**
	 * ワーカースレッド開始時の処理(ここはワーカースレッド上)
	 */
	@WorkerThread
	private void handleOnStartOnGL() {
		if (DEBUG) Log.v(TAG, "handleOnStart:");
		mCallback.onInitialize();
	}

	/**
	 * ワーカースレッド終了時の処理(ここはまだワーカースレッド上)
	 */
	@WorkerThread
	private void handleOnStopOnGL() {
		if (DEBUG) Log.v(TAG, "handleOnStop:");
		handleReleaseInputSurfaceOnGL();
		mCallback.onRelease();
	}

	@WorkerThread
	protected boolean handleMessage(@NonNull final Message msg) {
		switch (msg.what) {
		case REQUEST_UPDATE_TEXTURE -> {
			handleUpdateTexImageOnGL();
			return true;
		}
		case REQUEST_UPDATE_SIZE -> {
			handleResizeOnGL(msg.arg1, msg.arg2);
			return true;
		}
		case REQUEST_RECREATE_INPUT_SURFACE -> {
			handleReCreateInputSurfaceOnGL();
			return true;
		}
		default -> {
			if (DEBUG) Log.v(TAG, "handleRequest:" + msg);
		}
		}
		return false;
	}

	/**
	 * テクスチャ更新要求の処理
	 */
	private int updateTextImageCnt;
	@WorkerThread
	private void handleUpdateTexImageOnGL() {
		if (!isValid() || (mWidth <= 0) || (mHeight <= 0)) return;
		if (DEBUG && ((++updateTextImageCnt % 100) == 0)) Log.v(TAG, "handleUpdateTexImageOnGL:" + updateTextImageCnt);
		try {
			mGLManager.makeDefault(0xff000000);
			mGLManager.swap();
			mInputTexture.updateTexImage();
			mInputTexture.getTransformMatrix(mTexMatrix);
			mGLFrameAvailableCallback.onFrameAvailable(isGLES3(), true, mWidth, mHeight, mTexId, mTexMatrix);
		} catch (final Exception e) {
			Log.e(TAG, "handleUpdateTexImageOnGL:thread id =" + Thread.currentThread().getId(), e);
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
	private void handleResizeOnGL(final int width, final int height) {
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
	private void handleReCreateInputSurfaceOnGL() {
		if (DEBUG) Log.v(TAG, "handleReCreateInputSurface:");
		mLock.lock();
		try {
			mGLManager.makeDefault();
			handleReleaseInputSurfaceOnGL();
			mGLManager.makeDefault();
			mTexId = GLUtils.initTex(
				GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE0, GLES20.GL_NEAREST);
			mInputTexture = new SurfaceTexture(mTexId);
			mInputSurface = new Surface(mInputTexture);
			// XXX この時点ではSurfaceTextureへ渡したテクスチャへメモリーが割り当てられていない
			if (BuildCheck.isAndroid4_1()) {
				mInputTexture.setDefaultBufferSize(mWidth, mHeight);
			}
			mCallback.onCreateInputSurface(mInputSurface, mWidth, mHeight);
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
	private void handleReleaseInputSurfaceOnGL() {
		if (DEBUG) Log.v(TAG, "handleReleaseInputSurface:");
		final Surface surface;
		final SurfaceTexture surfaceTexture;
		final int texId;
		mLock.lock();
		try {
			surface = mInputSurface;
			mInputSurface = null;
			surfaceTexture = mInputTexture;
			mInputTexture = null;
			texId = mTexId;
			mTexId = 0;
		} finally {
			mLock.unlock();
		}
		if (surface != null) {
			mCallback.onReleaseInputSurface(surface);
			try {
				surface.release();
			} catch (final Exception e) {
				Log.w(TAG, e);
			}
		}
		if (surfaceTexture != null) {
			try {
				surfaceTexture.release();
			} catch (final Exception e) {
				Log.w(TAG, e);
			}
		}
		if (texId != 0) {
			GLUtils.deleteTex(texId);
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
