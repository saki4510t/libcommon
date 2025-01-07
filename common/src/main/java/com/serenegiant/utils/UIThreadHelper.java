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

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import android.util.Log;

public final class UIThreadHelper {
	private static final String TAG = UIThreadHelper.class.getSimpleName();

	private UIThreadHelper() {
		// インスタンス化をエラーにするためにデフォルトコンストラクタをprivateに
	}

	/** UI操作用のHandler */
	@NonNull
	private static final Handler sUIHandler = new Handler(Looper.getMainLooper());
	/** UIスレッドの参照 */
	@NonNull
	private static final Thread sUiThread = sUIHandler.getLooper().getThread();

	/**
	 * 内部で保持しているHandlerを取得
	 * @return
	 */
	public static Handler getHandler() {
		return sUIHandler;
	}

	/**
	 * UIスレッド上で実行しているかどうかを取得
	 * @return
	 */
	public static boolean isUiThread() {
		return Thread.currentThread().getId() == sUiThread.getId();
	}

	/**
	 * UIスレッドでRunnableを実行するためのヘルパーメソッド
	 * @param task
	 */
	public static void runOnUiThread(@NonNull final Runnable task) {
		if (Thread.currentThread() != sUiThread) {
			sUIHandler.post(task);
		} else {
			try {
				task.run();
			} catch (final Exception e) {
				Log.w(TAG, e);
			}
		}
	}

	/**
	 * UIスレッドでRunnableを実行するためのヘルパーメソッド
	 * @param task
	 * @param duration
	 */
	public static void runOnUiThread(@NonNull final Runnable task, final long duration) {
		if ((duration > 0) || Thread.currentThread() != sUiThread) {
			sUIHandler.postDelayed(task, duration);
		} else {
			try {
				task.run();
			} catch (final Exception e) {
				Log.w(TAG, e);
			}
		}
	}

	/**
	 * 指定したRunnableが未実行であれば実行待ちキューから取り除く
	 * @param task
	 */
	public static void removeFromUiThread(@NonNull final Runnable task) {
		sUIHandler.removeCallbacks(task);
	}

	/**
	 * 実行待ちキューから未実行のタスク・メッセージを全て取り除く
	 */
	public static void removeAllFromUiThread() {
		sUIHandler.removeCallbacksAndMessages(null);
	}

}
