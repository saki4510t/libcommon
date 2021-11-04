package com.serenegiant.utils;
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

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

/**
 * ワーカースレッド上で処理を行うためのHandler実装
 * インスタンス化のためのヘルパーメソッド$createHandler内でHandlerThreadを生成＆スタートする
 */
public class HandlerThreadHandler extends Handler {
	private static final String TAG = "HandlerThreadHandler";

	/**
	 * インスタンス生成用メルパーメソッド
	 * @return
	 */
	@SuppressLint("NewApi")
	public static final HandlerThreadHandler createHandler() {
		return createHandler(TAG, false);
	}

	/**
	 * インスタンス生成用メルパーメソッド, API>=22
	 * @param async Lopperの同期バリアの影響を受けずに非同期実行するかどうか
	 * @return
	 */
	@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
	public static final HandlerThreadHandler createHandler(final boolean async) {
		return createHandler(TAG, async);
	}

	/**
	 * インスタンス生成用メルパーメソッド
	 * @param name
	 * @return
	 */
	@SuppressLint("NewApi")
	public static final HandlerThreadHandler createHandler(
		final String name) {

		return createHandler(name, false);
	}

	/**
	 * インスタンス生成用メルパーメソッド, API>=22
	 * @param name
	 * @param async Lopperの同期バリアの影響を受けずに非同期実行するかどうか
	 * @return
	 */
	@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
	public static final HandlerThreadHandler createHandler(
		final String name, final boolean async) {

		final HandlerThread thread = new HandlerThread(name);
		thread.start();
		return new HandlerThreadHandler(thread.getLooper(), async);
	}

	/**
	 * インスタンス生成用メルパーメソッド
	 * @param callback
	 * @return
	 */
	public static final HandlerThreadHandler createHandler(
		@Nullable final Callback callback) {

		return createHandler(TAG, callback);
	}

	/**
	 * インスタンス生成用メルパーメソッド, API>=22
	 * @param callback
	 * @param async Lopperの同期バリアの影響を受けずに非同期実行するかどうか
	 * @return
	 */
	@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
	public static final HandlerThreadHandler createHandler(
		@Nullable final Callback callback, final boolean async) {

		return createHandler(TAG, callback, async);
	}

	/**
	 * インスタンス生成用メルパーメソッド
	 * @param name
	 * @param callback
	 * @return
	 */
	public static final HandlerThreadHandler createHandler(
		final String name, @Nullable final Callback callback) {

		final HandlerThread thread = new HandlerThread(name);
		thread.start();
		return new HandlerThreadHandler(thread.getLooper(), callback, false);
	}

	/**
	 * インスタンス生成用メルパーメソッド, API>=22
	 * @param name
	 * @param callback
	 * @param async Lopperの同期バリアの影響を受けずに非同期実行するかどうか
	 * @return
	 */
	@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
	public static final HandlerThreadHandler createHandler(
		final String name, @Nullable final Callback callback, final boolean async) {

		final HandlerThread thread = new HandlerThread(name);
		thread.start();
		return new HandlerThreadHandler(thread.getLooper(), callback, async);
	}

//--------------------------------------------------------------------------------
	private final long mId;
	private final boolean mAsynchronous;

	/**
	 * コンストラクタ
	 * @param looper
	 */
	private HandlerThreadHandler(@NonNull final Looper looper, final boolean async) {
		super(looper);
		final Thread thread = looper.getThread();
		mId = thread != null ? thread.getId() : 0;
		mAsynchronous = async;
	}

	/**
	 * コンストラクタ
	 * @param looper
	 * @param callback
	 */
	private HandlerThreadHandler(@NonNull final Looper looper,
		@Nullable final Callback callback, final boolean async) {

		super(looper, callback);
		final Thread thread = looper.getThread();
		mId = thread != null ? thread.getId() : 0;
		mAsynchronous = async;
	}

	/**
	 * mAsynchronous=trueでAPI>=22の場合にMessage#setAsynchronousで非同期設定フラグをつける。
	 * 今のHandlerの実装だと#sendMessageAtTimeと#sendMessageAtFrontOfQueueから
	 * #enqueueMessage(private)を呼び出していてその中でsetAsynchronousが呼び出されている。
	 *
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
		return super.sendMessageAtTime(msg, uptimeMillis);
	}

	public long getId() {
		return mId;
	}
	
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
	public void quitSafely() throws IllegalStateException {
		final Looper looper = getLooper();
		if (looper != null) {
			looper.quitSafely();
		} else {
			throw new IllegalStateException("has no looper");
		}
	}
	
	public void quit() throws IllegalStateException {
		final Looper looper = getLooper();
		if (looper != null) {
			looper.quit();
		} else {
			throw new IllegalStateException("has no looper");
		}
	}
	
	public boolean isCurrentThread() throws IllegalStateException {
		final Looper looper = getLooper();
		if (looper != null) {
			return mId == Thread.currentThread().getId();
		} else {
			throw new IllegalStateException("has no looper");
		}
	}
}
