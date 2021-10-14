package com.serenegiant.glutils;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2021 saki t_saki@serenegiant.com
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
import android.os.Message;
import android.util.Log;
import android.view.Choreographer;

import com.serenegiant.utils.HandlerThreadHandler;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

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
	@Nullable
	private final Handler.Callback mCallback;
	private boolean mInitialized;
	private boolean mReleased;

	/**
	 * コンストラクタ
	 */
	public GLManager() {
		this(GLUtils.getSupportedGLVersion(), null, 0, null);
	}

	/**
	 * コンストラクタ
	 * @param callback
	 */
	public GLManager(@Nullable final Handler.Callback callback) {
		this(GLUtils.getSupportedGLVersion(), null, 0, callback);
	}

	/**
	 * コンストラクタ
	 * @param maxClientVersion
	 */
	public GLManager(final int maxClientVersion) {
		this(maxClientVersion, null, 0, null);
	}

	/**
	 * コンストラクタ
	 * @param maxClientVersion
	 * @param callback
	 */
	public GLManager(final int maxClientVersion,
		@Nullable final Handler.Callback callback) {

		this(maxClientVersion, null, 0, callback);
	}

	/**
	 * コンストラクタ
	 * @param maxClientVersion
	 * @param sharedContext
	 * @param flags
	 * @param callback
	 */
	public GLManager(final int maxClientVersion,
		@Nullable final EGLBase.IContext sharedContext, final int flags,
		@Nullable final Handler.Callback callback) throws RuntimeException {

		if (DEBUG) Log.v(TAG, "コンストラクタ:");
		mCallback = callback;
		mGLContext = new GLContext(maxClientVersion,
			sharedContext, flags);
		final Handler.Callback handlerCallback
			= new Handler.Callback() {
			@Override
			public boolean handleMessage(@NonNull final Message msg) {
				return GLManager.this.handleMessage(msg);
			}
		};
		final HandlerThreadHandler handler;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
			// API>=22ならHandlerを非同期仕様で初期化
			handler = HandlerThreadHandler.createHandler(TAG, handlerCallback, true);
		} else {
			// API<22ならHandlerをLooperによる同期バリアを受ける設定で初期化
			handler = HandlerThreadHandler.createHandler(TAG, handlerCallback);
		}
		mGLHandler = handler;
		mHandlerThreadId = handler.getId();
		final Semaphore sync = new Semaphore(0);
		mGLHandler.postAtFrontOfQueue(new Runnable() {
			@Override
			public void run() {
				try {
					mGLContext.initialize();
					mInitialized = true;
				} catch (final Exception e) {
					Log.w(TAG, e);
				}
				sync.release();
			}
		});
		try {
			sync.tryAcquire(3000, TimeUnit.MILLISECONDS);
		} catch (final InterruptedException e) {
			// do nothing
		}
		if (!mInitialized) {
			throw new RuntimeException("Failed to initialize GL context");
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
					mGLHandler.getLooper().quit();
				}
			});
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
	 * この共有GLコンテキストがコンテキストを持つGLTaskを生成して返す
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
			callback);
	}

	/**
	 * GLコンテキストを保持しているスレッド上での実行用のHandlerを返す
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
	 * GLコンテキストを保持しているスレッド上での実行要求
	 * @param task
	 */
	public synchronized void runOnGLThread(final Runnable task)
		throws IllegalStateException {

		if (DEBUG) Log.v(TAG, "runOnGLThread:");
		checkValid();
		mGLHandler.post(task);
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

	/**
	 * GLスレッド上での処理の実体
	 * @param msg
	 * @return
	 */
	@WorkerThread
	protected boolean handleMessage(@NonNull final Message msg) {
		if (mCallback != null) {
			return mCallback.handleMessage(msg);
		}
		return false;
	}

	/**
	 * 現在のスレッドがEGL/GLコンテキストを保持したスレッドかどうか
 	 * @return
	 */
	protected boolean isGLThread() {
		return mHandlerThreadId == Thread.currentThread().getId();
	}
}
