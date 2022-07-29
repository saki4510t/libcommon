package com.serenegiant.gl;
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

import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.Choreographer;

import com.serenegiant.egl.EGLBase;
import com.serenegiant.utils.HandlerThreadHandler;
import com.serenegiant.utils.HandlerUtils;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

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
			callback);
	}

	/**
	 * コンストラクタ
	 * @param maxClientVersion
	 */
	public GLManager(final int maxClientVersion) {
		this(maxClientVersion, null, 0,
			null, 0, 0,
			null);
	}

	/**
	 * コンストラクタ
	 * @param maxClientVersion
	 * @param callback GLコンテキストを保持したワーカースレッド上での実行用Handlerのメッセージ処理用コールバック
	 */
	public GLManager(final int maxClientVersion,
		@Nullable final Handler.Callback callback) {

		this(maxClientVersion, null, 0, null, 0, 0, callback);
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
		this(maxClientVersion, sharedContext, flags, null, 0, 0, callback);
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
	 * @param callback GLコンテキストを保持したワーカースレッド上での実行用Handlerのメッセージ処理用コールバック
	 */
	public GLManager(final int maxClientVersion,
		@Nullable final EGLBase.IContext<?> sharedContext, final int flags,
		@Nullable final Object masterSurface, final int masterWidth, final int masterHeight,
		@Nullable final Handler.Callback callback)
			throws IllegalArgumentException, IllegalStateException {

		if (DEBUG) Log.v(TAG, "コンストラクタ:");
		if ((masterSurface != null) && !GLUtils.isSupportedSurface(masterSurface)) {
			throw new IllegalArgumentException("wrong type of masterSurface");
		}
		mGLContext = new GLContext(maxClientVersion, sharedContext, flags);
		final HandlerThreadHandler handler;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
			// API>=22ならHandlerを非同期仕様で初期化
			handler = HandlerThreadHandler.createHandler(TAG, callback, true);
		} else {
			// API<22ならHandlerをLooperによる同期バリアを受ける設定で初期化
			handler = HandlerThreadHandler.createHandler(TAG, callback);
		}
		mGLHandler = handler;
		mHandlerThreadId = handler.getId();
		final Semaphore sync = new Semaphore(0);
		mGLHandler.postAtFrontOfQueue(new Runnable() {
			@Override
			public void run() {
				try {
					mGLContext.initialize(masterSurface, masterWidth, masterHeight);
					mInitialized = true;
				} catch (final Exception e) {
					Log.w(TAG, e);
				}
				sync.release();
			}
		});
		// ワーカースレッドの初期化待ち
		try {
			if (!sync.tryAcquire(3000, TimeUnit.MILLISECONDS)) {
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
	public synchronized void release() {
		if (DEBUG) Log.v(TAG, "release:");
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
			final Semaphore sync = new Semaphore(0);
			mGLHandler.postAtFrontOfQueue(new Runnable() {
				@Override
				public void run() {
					try {
						mGLContext.initialize(masterSurface, masterWidth, masterHeight);
						mInitialized = true;
					} catch (final Exception e) {
						Log.w(TAG, e);
					}
					sync.release();
				}
			});
			// ワーカースレッドの初期化待ち
			try {
				if (!sync.tryAcquire(3000, TimeUnit.MILLISECONDS)) {
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
	public synchronized boolean isValid() {
		return mInitialized && !mReleased;
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
	public synchronized GLManager createShared(
		@Nullable final Handler.Callback callback)	throws RuntimeException {

		if (DEBUG) Log.v(TAG, "createShared:");
		checkValid();
		return new GLManager(mGLContext.getMaxClientVersion(),
			mGLContext.getContext(), mGLContext.getFlags(),
			null, 0, 0,
			callback);
	}

	/**
	 * GLコンテキストを保持しているスレッド上での実行用のHandlerを返す
	 * こちらは内部で使っているHandlerをそのまま返す
	 * @return
	 * @throws IllegalStateException
	 */
	@NonNull
	public synchronized Handler getGLHandler() throws IllegalStateException {
		if (DEBUG) Log.v(TAG, "getGLHandler:");
		checkValid();
		return mGLHandler;
	}

	/**
	 * 同じGLコンテキスト保持しているスレッド上で実行するためのHandlerを生成して返す
	 * こちらは内部で使っているHandlerと同じLooperを使う新しいHandlerを生成して返す
	 * #getLooper#quitすると全部終了してしまうので注意
	 * @return
	 * @throws IllegalStateException
	 */
	public synchronized Handler createGLHandler(
		@Nullable final Handler.Callback callback) throws IllegalStateException {

		if (DEBUG) Log.v(TAG, "createGLHandler:");
		checkValid();
		return new Handler(mGLHandler.getLooper(), callback);
	}

	/**
	 * GLContextを取得する
	 * @return
	 * @throws IllegalStateException
	 */
	@NonNull
	public synchronized GLContext getGLContext() throws IllegalStateException {
		checkValid();
		return mGLContext;
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
	public synchronized void runOnGLThread(final Runnable task)
		throws IllegalStateException {

		if (DEBUG) Log.v(TAG, "runOnGLThread:");
		checkValid();
		if (isGLThread()) {
			// GLスレッド上で呼ばれたときはそのまま実行する
			task.run();
		} else {
			mGLHandler.post(task);
		}
	}

	/**
	 * GLコンテキストを保持しているスレッド上での実行要求
	 * @param task
	 * @param delayMs
	 * @throws IllegalStateException
	 */
	public synchronized void runOnGLThread(final Runnable task, final long delayMs)
		throws IllegalStateException{

		if (DEBUG) Log.v(TAG, "runOnGLThread:");
		checkValid();
		if (delayMs > 0) {
			mGLHandler.postDelayed(task, delayMs);
		} else if (isGLThread()) {
			// GLスレッド上で呼ばれたときはそのまま実行する
			task.run();
		} else {
			mGLHandler.post(task);
		}
	}

	/**
	 * GLコンテキスト上で実行されるChoreographer.FrameCallbackをpostする
	 * @param callback
	 * @param delayMs
	 * @throws IllegalStateException
	 */
	public synchronized void postFrameCallbackDelayed(
		@NonNull final Choreographer.FrameCallback callback,
		final long delayMs) throws IllegalStateException {

		if (DEBUG) Log.v(TAG, "postFrameCallbackDelayed:");
		checkValid();
		if (isGLThread()) {
			// すでにGLスレッド上であれば直接実行
			Choreographer.getInstance().postFrameCallbackDelayed(callback, delayMs);
		} else {
			// 別スレッド上にいるならGLスレッド上へ投げる
			mGLHandler.post(new Runnable() {
				@Override
				public void run() {
					Choreographer.getInstance().postFrameCallbackDelayed(callback, delayMs);
				}
			});
		}
	}

	/**
	 * 未実行のChoreographer.FrameCallbackがあれば取り除く
	 * @param callback
	 * @throws IllegalStateException
	 */
	public synchronized void removeFrameCallback(
		@NonNull final Choreographer.FrameCallback callback)
			throws IllegalStateException {

		if (DEBUG) Log.v(TAG, "removeFrameCallback:");
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
