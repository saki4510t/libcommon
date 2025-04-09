package com.serenegiant.gl;
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

import android.opengl.GLES20;
import android.os.Handler;
import android.os.Process;
import android.util.Log;
import android.view.Choreographer;

import com.serenegiant.egl.EGLBase;
import com.serenegiant.utils.HandlerThreadHandler;
import com.serenegiant.utils.HandlerUtils;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * GLコンテキストとそれを保持するワーカースレッドを扱うためのヘルパークラス
 */
public class GLManager {
	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = GLManager.class.getSimpleName();

	@NonNull
	private final GLContext mGLContext;
	@NonNull
	private final Handler mGLHandler;
	private final long mHandlerThreadId;
	private int mThreadPriority;
	/**
	 * 排他制御用
	 */
	@NonNull
	private final ReentrantLock mLock = new ReentrantLock();
	private boolean mInitialized;
	private boolean mReleased;

	/**
	 * コンストラクタ
	 * maxClientVersionは端末が対応しているGL|ESバージョンの一番大きいものを使う
	 */
	public GLManager() {
		this(GLUtils.getSupportedGLVersion(),
			null, 0,
			null, 0,0,
			Process.THREAD_PRIORITY_DISPLAY,
			null);
	}

	/**
	 * コンストラクタ
	 * maxClientVersionは端末が対応しているGL|ESバージョンの一番大きいものを使う
	 * @param callback
	 */
	public GLManager(@Nullable final Handler.Callback callback) {
		this(GLUtils.getSupportedGLVersion(),
			null, 0,
			null, 0, 0,
			Process.THREAD_PRIORITY_DISPLAY,
			callback);
	}

	/**
	 * コンストラクタ
	 * @param maxClientVersion
	 */
	public GLManager(final int maxClientVersion) {
		this(maxClientVersion, null, 0,
			null, 0, 0,
			Process.THREAD_PRIORITY_DISPLAY,
			null);
	}

	/**
	 * コンストラクタ
	 * @param maxClientVersion
	 * @param callback GLコンテキストを保持したワーカースレッド上での実行用Handlerのメッセージ処理用コールバック
	 */
	public GLManager(final int maxClientVersion,
		@Nullable final Handler.Callback callback) {

		this(maxClientVersion, null, 0,
			null, 0, 0,
			Process.THREAD_PRIORITY_DISPLAY, callback);
	}

	/**
	 * 他のGLManagerの共有コンテキストを保持するGLManagerを生成するためのコンストラクタ
	 * @param manager
	 * @param callback GLコンテキストを保持したワーカースレッド上での実行用Handlerのメッセージ処理用コールバック
	 */
	public GLManager(@NonNull final GLManager manager,
		@Nullable final Handler.Callback callback) {

		this(manager.getGLContext().getMaxClientVersion(),
			manager.getGLContext().getContext(),
			manager.getGLContext().getFlags(),
			null, 0, 0,
			Process.THREAD_PRIORITY_DISPLAY,
			callback);
	}

	/**
	 * 他のGLContextの共有コンテキストを保持するGLManagerを生成するためのコンストラクタ
	 * @param shared
	 * @param callback
	 */
	public GLManager(@NonNull GLContext shared,
		@Nullable final Handler.Callback callback) {

		this(shared.getMaxClientVersion(),
			shared.getContext(), shared.getFlags(),
			null, 0, 0,
			Process.THREAD_PRIORITY_DISPLAY,
			callback);
	}

	/**
	 * コンストラクタ
	 * @param maxClientVersion
	 * @param sharedContext 共有コンテキストを使わない場合はnull
	 * @param flags GLContext生成時のフラグ
	 * @param callback GLコンテキストを保持したワーカースレッド上での実行用Handlerのメッセージ処理用コールバック
	 */
	public GLManager(final int maxClientVersion,
		@Nullable final EGLBase.IContext<?> sharedContext, final int flags,
		@Nullable final Handler.Callback callback)
			throws IllegalArgumentException, IllegalStateException {
		this(maxClientVersion, sharedContext, flags,
			null, 0, 0,
			Process.THREAD_PRIORITY_DISPLAY,
			callback);
	}
	/**
	 * コンストラクタ
	 * @param maxClientVersion
	 * @param sharedContext 共有コンテキストを使わない場合はnull
	 * @param flags GLContext生成時のフラグ
	 * @param masterSurface nullまたはSurface/SurfaceHolder/SurfaceTexture/SurfaceView
	 * @param masterWidth GLコンテキスト保持用のEglSurfaceのサイズ(幅)、0以下の場合は1になる
	 * 						masterSurfaceにnull以外を指定した場合は有効な値をセットすること
	 * @param masterHeight GLコンテキスト保持用のEglSurfaceのサイズ(高さ)、0以下の場合は1になる
	 * 						masterSurfaceにnull以外を指定した場合は有効な値をセットすること
	 * @param threadPriority スレッドの優先度、Process.THREAD_PRIORITY_XXXで指定, デフォルトはProcess.THREAD_PRIORITY_DISPLAY
	 * @param callback GLコンテキストを保持したワーカースレッド上での実行用Handlerのメッセージ処理用コールバック
	 */
	public GLManager(final int maxClientVersion,
		@Nullable final EGLBase.IContext<?> sharedContext, final int flags,
		@Nullable final Object masterSurface, final int masterWidth, final int masterHeight,
		final int threadPriority,
		@Nullable final Handler.Callback callback)
			throws IllegalArgumentException, IllegalStateException {

		if (DEBUG) Log.v(TAG, "コンストラクタ:");
		if ((masterSurface != null) && !GLUtils.isSupportedSurface(masterSurface)) {
			throw new IllegalArgumentException("wrong type of masterSurface");
		}
		mThreadPriority = threadPriority;
		mGLContext = new GLContext(maxClientVersion, sharedContext, flags);
		final HandlerThreadHandler handler = HandlerThreadHandler.createHandler(TAG, callback, threadPriority, true);
		mGLHandler = handler;
		mHandlerThreadId = handler.getId();
		final Semaphore sem = new Semaphore(0);
		mGLHandler.postAtFrontOfQueue(new Runnable() {
			@Override
			public void run() {
				try {
					mGLContext.initialize(masterSurface, masterWidth, masterHeight, threadPriority);
					mInitialized = true;
				} catch (final Exception e) {
					Log.w(TAG, e);
				}
				sem.release();
			}
		});
		// ワーカースレッドの初期化待ち
		try {
			if (!sem.tryAcquire(3000, TimeUnit.MILLISECONDS)) {
				// タイムアウトしたとき
				mInitialized = false;
			}
		} catch (final InterruptedException e) {
			// do nothing
		}
		if (!mInitialized) {
			throw new IllegalStateException("Failed to initialize GL context");
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
	 * 関連するリソースを廃棄する
	 * 再利用はできない
	 */
	public void release() {
		if (DEBUG) Log.v(TAG, "release:");
		mLock.lock();
		try {
			if (!mReleased) {
				mReleased = true;
				mGLHandler.postAtFrontOfQueue(new Runnable() {
					@Override
					public void run() {
						mGLContext.release();
						mGLHandler.removeCallbacksAndMessages(null);
						HandlerUtils.NoThrowQuit(mGLHandler);
					}
				});
			}
		} finally {
			mLock.unlock();
		}
	}

	/**
	 * GLコンテキストを再初期化する
	 * @param masterSurface
	 * @param masterWidth
	 * @param masterHeight
	 */
	public void reInitialize(
		@Nullable final Object masterSurface,
		final int masterWidth, final int masterHeight) {

		if (mInitialized) {
			mInitialized = false;
			final Semaphore sem = new Semaphore(0);
			mGLHandler.postAtFrontOfQueue(new Runnable() {
				@Override
				public void run() {
					try {
						mGLContext.initialize(masterSurface, masterWidth, masterHeight, mThreadPriority);
						mInitialized = true;
					} catch (final Exception e) {
						Log.w(TAG, e);
					}
					sem.release();
				}
			});
			// ワーカースレッドの初期化待ち
			try {
				if (!sem.tryAcquire(3000, TimeUnit.MILLISECONDS)) {
					// タイムアウトしたとき
					mInitialized = false;
				}
			} catch (final InterruptedException e) {
				// do nothing
			}
			if (!mInitialized) {
				throw new IllegalStateException("Failed to initialize GL context");
			}
		} else {
			throw new IllegalStateException("Not initialized!");
		}
	}

	/**
	 * GLコンテキストが有効かどうか
	 * @return
	 */
	public boolean isValid() {
		mLock.lock();
		try {
			return mInitialized && !mReleased;
		} finally {
			mLock.unlock();
		}
	}

	/**
	 * GLコンテキストがGL|ES3に対応しているかどうかを取得
	 * @return
	 */
	public boolean isGLES3() {
		return mGLContext.isGLES3();
	}

	/**
	 * 現在のスレッドがEGL/GLコンテキストを保持したスレッドかどうか
 	 * @return
	 */
	public boolean isGLThread() {
		return mHandlerThreadId == Thread.currentThread().getId();
	}

	/**
	 * EGLを取得
	 * @return
	 * @throws IllegalStateException
	 */
	public EGLBase getEgl() throws IllegalStateException {
		if (DEBUG) Log.v(TAG, "getEgl:");
		checkValid();
		return mGLContext.getEgl();
	}

	/**
	 * マスターサーフェースの幅を取得
	 * GLコンテキストが初期化されていなければ0
	 * @return
	 */
	public int getMasterWidth() {
		return mGLContext.getMasterWidth();
	}

	/**
	 * マスターサーフェースの高さを取得
	 * GLコンテキストが初期化されていなければ0
	 * @return
	 */
	public int getMasterHeight() {
		return mGLContext.getMasterHeight();
	}

	/**
	 * このGLManagerとGLコンテキストを共有する新しいGLManagerを生成して返す
	 * @param callback
	 * @return
	 * @throws RuntimeException
	 */
	public GLManager createShared(
		@Nullable final Handler.Callback callback)	throws RuntimeException {

		if (DEBUG) Log.v(TAG, "createShared:");
		mLock.lock();
		try {
			checkValid();
			return new GLManager(mGLContext.getMaxClientVersion(),
				mGLContext.getContext(), mGLContext.getFlags(),
				null, 0, 0,
				mThreadPriority,
				callback);
		} finally {
			mLock.unlock();
		}
	}

	/**
	 * GLコンテキストを保持しているスレッド上での実行用のHandlerを返す
	 * こちらは内部で使っているHandlerをそのまま返す
	 * @return
	 * @throws IllegalStateException
	 */
	@NonNull
	public Handler getGLHandler() throws IllegalStateException {
		if (DEBUG) Log.v(TAG, "getGLHandler:");
		mLock.lock();
		try {
			checkValid();
			return mGLHandler;
		} finally {
			mLock.unlock();
		}
	}

	/**
	 * 同じGLコンテキスト保持しているスレッド上で実行するためのHandlerを生成して返す
	 * こちらは内部で使っているHandlerと同じLooperを使う新しいHandlerを生成して返す
	 * #getLooper#quitすると全部終了してしまうので注意
	 * @return
	 * @throws IllegalStateException
	 */
	public Handler createGLHandler(
		@Nullable final Handler.Callback callback) throws IllegalStateException {

		if (DEBUG) Log.v(TAG, "createGLHandler:");
		mLock.lock();
		try {
			checkValid();
			return new Handler(mGLHandler.getLooper(), callback);
		} finally {
			mLock.unlock();
		}
	}

	/**
	 * GLContextを取得する
	 * @return
	 * @throws IllegalStateException
	 */
	@NonNull
	public GLContext getGLContext() throws IllegalStateException {
		mLock.lock();
		try {
			checkValid();
			return mGLContext;
		} finally {
			mLock.unlock();
		}
	}

	/**
	 * GLContext#makeDefaultを呼び出すためのヘルパーメソッド
	 * @throws IllegalStateException
	 */
	public void makeDefault() throws IllegalStateException {
		checkValid();
		mGLContext.makeDefault();
	}

	/**
	 * GLContext#makeDefaultを呼び出すためのヘルパーメソッド
	 * 何も描画しないとハングアップする端末があるので指定色に塗りつぶす処理を追加
	 * @param color
	 * @throws IllegalStateException
	 */
	public void makeDefault(final int color) throws IllegalStateException {
		makeDefault();
		// 何も描画しないとハングアップする端末があるので適当に塗りつぶす
		GLES20.glClearColor(
			((color & 0x00ff0000) >>> 16) / 255.0f,	// R
			((color & 0x0000ff00) >>>  8) / 255.0f,	// G
			((color & 0x000000ff)) / 255.0f,		// B
			((color & 0xff000000) >>> 24) / 255.0f	// A
		);
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
	}

	/**
	 * GLContext#swapを呼び出すためのヘルパーメソッド
	 * @throws IllegalStateException
	 */
	public void swap() throws IllegalStateException {
		checkValid();
		mGLContext.swap();
	}

	/**
	 * GLコンテキストを保持しているスレッド上での実行要求
	 * @param task
	 */
	public void runOnGLThread(@NonNull final Runnable task)
		throws IllegalStateException {

		if (DEBUG) Log.v(TAG, "runOnGLThread:");
		mLock.lock();
		try {
			checkValid();
			if (isGLThread()) {
				// GLスレッド上で呼ばれたときはそのまま実行する
				task.run();
			} else {
				mGLHandler.post(task);
			}
		} finally {
			mLock.unlock();
		}
	}

	/**
	 * GLコンテキストを保持しているスレッド上での実行要求
	 * @param task
	 * @param delayMs
	 * @throws IllegalStateException
	 */
	public void runOnGLThread(@NonNull final Runnable task, final long delayMs)
		throws IllegalStateException{

		if (DEBUG) Log.v(TAG, "runOnGLThread:");
		mLock.lock();
		try {
			checkValid();
			if (delayMs > 0) {
				mGLHandler.postDelayed(task, delayMs);
			} else if (isGLThread()) {
				// GLスレッド上で呼ばれたときはそのまま実行する
				task.run();
			} else {
				mGLHandler.post(task);
			}
		} finally {
			mLock.unlock();
		}
	}

	/**
	 * GLコンテキスト上で実行されるChoreographer.FrameCallbackをpostする
	 * @param callback
	 * @param delayMs
	 * @return true: GLワーカースレッドへpostした, false: 既にGLワーカースレッド上なので現在のスレッド上で実行要求した
	 * @throws IllegalStateException
	 */
	public boolean postFrameCallbackDelayed(
		@NonNull final Choreographer.FrameCallback callback,
		final long delayMs) throws IllegalStateException {

		if (DEBUG) Log.v(TAG, "postFrameCallbackDelayed:");
		boolean result = false;
		mLock.lock();
		try {
			checkValid();
			if (isGLThread()) {
				// すでにGLスレッド上であれば直接実行
				Choreographer.getInstance().postFrameCallbackDelayed(callback, delayMs);
			} else {
				// 別スレッド上にいるならGLスレッド上へ投げる
				result = true;
				mGLHandler.post(new Runnable() {
					@Override
					public void run() {
						Choreographer.getInstance().postFrameCallbackDelayed(callback, delayMs);
					}
				});
			}
		} finally {
			mLock.unlock();
		}

		return result;
	}

	/**
	 * 未実行のChoreographer.FrameCallbackがあれば取り除く
	 * @param callback
	 * @throws IllegalStateException
	 */
	public void removeFrameCallback(
		@NonNull final Choreographer.FrameCallback callback)
			throws IllegalStateException {

		if (DEBUG) Log.v(TAG, "removeFrameCallback:");
		mLock.lock();
		try {
			checkValid();
			if (isGLThread()) {
				// すでにGLスレッド上であれば直接実行
				Choreographer.getInstance().removeFrameCallback(callback);
			} else {
				// 別スレッド上にいるならGLスレッド上へ投げる
				mGLHandler.post(new Runnable() {
					@Override
					public void run() {
						Choreographer.getInstance().removeFrameCallback(callback);
					}
				});
			}
		} finally {
			mLock.unlock();
		}
	}

	/**
	 * スレッドの優先度を変更
	 * @param priority Process.THREAD_PRIORITY_XXXで指定する
	 */
	public void setPriority(final int priority) {
		checkValid();
		if (priority != mThreadPriority) {
			runOnGLThread(() -> {
				try {
					Process.setThreadPriority(priority);
					mThreadPriority = priority;
				} catch (final Exception e) {
					if (DEBUG) Log.w(TAG, e);
				}
			});
		}
	}

//--------------------------------------------------------------------------------
	/**
	 * GLコンテキストが破棄されていないかどうかを確認
	 * @throws IllegalStateException
	 */
	private void checkValid() throws IllegalStateException {
		if (!isValid()) {
			throw new IllegalStateException("already released");
		}
	}

}
