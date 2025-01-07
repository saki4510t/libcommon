package com.serenegiant.utils;
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
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * ワーカースレッド上で処理を行うためのHandler実装
 * インスタンス化のためのヘルパーメソッド$createHandler内でHandlerThreadを生成＆スタートする
 * XXX API>=28でasync=trueならHandler#createAsyncを使ってもよいかも
 */
public class HandlerThreadHandler extends Handler {
	private static final String TAG = "HandlerThreadHandler";

	/**
	 * インスタンス生成用ヘルパーメソッド
	 * @return
	 */
	@Deprecated
	@SuppressLint("NewApi")
	public static final HandlerThreadHandler createHandler() {
		return createHandler(TAG, null, Process.THREAD_PRIORITY_DEFAULT, false);
	}

	/**
	 * インスタンス生成用ヘルパーメソッド
	 * @param priority The priority to run the thread at. The value supplied must be from
	 * 			{@link android.os.Process} and not from java.lang.Thread.
	 * @return
	 */
	@Deprecated
	public static final HandlerThreadHandler createHandler(final int priority) {
		return createHandler(TAG, null, priority, false);
	}

	/**
	 * インスタンス生成用ヘルパーメソッド
	 * @param async Lopperの同期バリアの影響を受けずに非同期実行するかどうか，API<22なら無視される
	 * @return
	 */
	@Deprecated
	public static final HandlerThreadHandler createHandler(final boolean async) {
		return createHandler(TAG, null, Process.THREAD_PRIORITY_DEFAULT, async);
	}

	/**
	 * インスタンス生成用ヘルパーメソッド
	 * @param name
	 * @return
	 */
	@SuppressLint("NewApi")
	public static final HandlerThreadHandler createHandler(
		final String name) {

		return createHandler(name, null, Process.THREAD_PRIORITY_DEFAULT, false);
	}

	/**
	 * インスタンス生成用ヘルパーメソッド
	 * @param name
	 * @param priority The priority to run the thread at. The value supplied must be from
	 * 			{@link android.os.Process} and not from java.lang.Thread.
	 * @return
	 */
	@SuppressLint("NewApi")
	public static final HandlerThreadHandler createHandler(
		final String name, final int priority) {

		return createHandler(name, null, priority, false);
	}

	/**
	 * インスタンス生成用ヘルパーメソッド
	 * @param name
	 * @param priority The priority to run the thread at. The value supplied must be from
	 * 			{@link android.os.Process} and not from java.lang.Thread.
	 * @param async Lopperの同期バリアの影響を受けずに非同期実行するかどうか，API<22なら無視される
	 * @return
	 */
	public static final HandlerThreadHandler createHandler(
		final String name, final int priority, final boolean async) {

		return createHandler(TAG, null, priority, async);
	}

	/**
	 * インスタンス生成用ヘルパーメソッド
	 * @param callback
	 * @return
	 */
	@Deprecated
	public static final HandlerThreadHandler createHandler(
		@Nullable final Callback callback) {

		return createHandler(TAG, callback, Process.THREAD_PRIORITY_DEFAULT, false);
	}

	/**
	 * インスタンス生成用ヘルパーメソッド
	 * @param callback
	 * @param priority The priority to run the thread at. The value supplied must be from
	 * 			{@link android.os.Process} and not from java.lang.Thread.
	 * @return
	 */
	@Deprecated
	public static final HandlerThreadHandler createHandler(
		@Nullable final Callback callback, final int priority) {

		return createHandler(TAG, callback, priority, false);
	}

	/**
	 * インスタンス生成用ヘルパーメソッド
	 * @param callback
	 * @param async Lopperの同期バリアの影響を受けずに非同期実行するかどうか，API<22なら無視される
	 * @return
	 */
	@Deprecated
	public static final HandlerThreadHandler createHandler(
		@Nullable final Callback callback, final boolean async) {

		return createHandler(TAG, callback, Process.THREAD_PRIORITY_DEFAULT, async);
	}

	/**
	 * インスタンス生成用ヘルパーメソッド
	 * @param name
	 * @param callback
	 * @return
	 */
	public static final HandlerThreadHandler createHandler(
		final String name, @Nullable final Callback callback) {

		return createHandler(name, callback, Process.THREAD_PRIORITY_DEFAULT, false);
	}

	/**
	 * インスタンス生成用ヘルパーメソッド
	 * @param name
	 * @param callback
	 * @param async Lopperの同期バリアの影響を受けずに非同期実行するかどうか，API<22なら無視される
	 * @return
	 */
	public static final HandlerThreadHandler createHandler(
		final String name, @Nullable final Callback callback, final boolean async) {

		return createHandler(name, callback, Process.THREAD_PRIORITY_DEFAULT, async);
	}

	/**
	 * インスタンス生成用ヘルパーメソッド
	 * @param name
	 * @param callback
	 * @param priority The priority to run the thread at. The value supplied must be from
	 * 			{@link android.os.Process} and not from java.lang.Thread.
	 * @param async Lopperの同期バリアの影響を受けずに非同期実行するかどうか, API<22なら無視される
	 * @return
	 */
	public static final HandlerThreadHandler createHandler(
		final String name, @Nullable final Callback callback,
		final int priority, final boolean async) {

		final HandlerThread thread = new HandlerThread(name, priority);
		thread.start();
		return new HandlerThreadHandler(thread.getLooper(), callback, async);
	}

//--------------------------------------------------------------------------------
	private final long mId;
	private final boolean mAsynchronous;
	private volatile boolean mIsActive = true;

	/**
	 * コンストラクタ
	 * @param looper
	 * @param callback
	 * @param async Lopperの同期バリアの影響を受けずに非同期実行するかどうか, API<22なら無視される
	 */
	private HandlerThreadHandler(@NonNull final Looper looper,
		@Nullable final Callback callback, final boolean async) {

		super(looper, callback);
		mId = looper.getThread().getId();
		mAsynchronous = async;
	}

	/**
	 * mAsynchronous=trueでAPI>=22の場合にMessage#setAsynchronousで非同期設定フラグをつける。
	 * 今のHandlerの実装だと#sendMessageAtTimeと#sendMessageAtFrontOfQueueから
	 * #enqueueMessage(private)を呼び出していてその中でsetAsynchronousが呼び出されている。
	 * sendMessageAtFrontOfQueueもoverrideしたいけどfinalなのでoverrideできない
	 * @param msg
	 * @param uptimeMillis
	 * @return
	 */
	@SuppressLint("NewApi")
	@Override
	public boolean sendMessageAtTime(@NonNull Message msg, long uptimeMillis) {
		if (mAsynchronous
			&& (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1)) {

			msg.setAsynchronous(true);
		}
		if (mIsActive) {
			return super.sendMessageAtTime(msg, uptimeMillis);
		} else {
			return false;
		}
	}

	public long getId() {
		return mId;
	}
	
	public void quitSafely() throws IllegalStateException {
		mIsActive = false;
		getLooper().quitSafely();
	}
	
	public void quit() throws IllegalStateException {
		mIsActive = false;
		getLooper().quit();
	}
	
	public boolean isCurrentThread() throws IllegalStateException {
		return mId == Thread.currentThread().getId();
	}

	public boolean isActive() {
		return mIsActive;
	}
}
