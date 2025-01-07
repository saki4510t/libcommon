package com.serenegiant.system;
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
import android.app.Activity;
import android.os.PowerManager;
import android.util.Log;
import android.view.WindowManager;

import androidx.annotation.NonNull;

/**
 * スリープ状態から画面ON出来るようにするためのヘルパークラス
 * 実際に使うにはAndroidManifest.xmlに
 * <uses-permission android:name="android.permission.WAKE_LOCK"/>と
 * <uses-permission android:name="android.permission.DISABLE_KEYGUARD"/>
 * が必要
 */
public class PowerHelper {
	private static final String TAG = "PowerHelper";

	private PowerHelper() {
		// インスタンス化をエラーにするためにデフォルトコンストラクタをprivateに
	}

	/**
	 * スリープを解除して画面をONにする
	 * @param activity
	 * @param disableKeyguard これは無視される
	 * @param lockDelayed
	 */
	public static void wake(final Activity activity, final boolean disableKeyguard, final long lockDelayed) {
		wake(activity, true, true, lockDelayed);
	}

	/**
	 * スリープを解除して画面をONにする
	 * @param activity
	 * @param disableKeyguard
	 * @param keepScreenOn
	 * @param lockDelayed
	 */
	@SuppressLint({"MissingPermission", "WakelockTimeout"})
	public static void wake(
		@NonNull final Activity activity,
		final boolean disableKeyguard, final boolean keepScreenOn,
		final long lockDelayed) {

		try {
			// スリープ状態から起床(android.permission.WAKE_LOCKが必要)
			final PowerManager.WakeLock wakelock
				= ContextUtils.requireSystemService(activity, PowerManager.class)
					.newWakeLock(PowerManager.FULL_WAKE_LOCK
						| PowerManager.ACQUIRE_CAUSES_WAKEUP
						| PowerManager.ON_AFTER_RELEASE, "PowerHelper:disableLock");
			if (lockDelayed > 0) {
				wakelock.acquire(lockDelayed);
			} else {
				wakelock.acquire();
			}
			// キーガードを解除(android.permission.DISABLE_KEYGUARDが必要)
			try {
				if (disableKeyguard) {
					// キーガードを解除(android.permission.DISABLE_KEYGUARDが必要)
					activity.getWindow().addFlags(
						WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
						| WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
						| (keepScreenOn ? WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON : 0));
				} else if (keepScreenOn) {
					// 画面がOFFにならないようにする
					activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
				}
			} finally {
				wakelock.release();
			}
		} catch (final Exception e) {
			Log.w(TAG, e);
		}
	}

	/**
	 * wakeでスリープ解除・画面ONしたのをスリープできるようにする
	 * @param activity
	 */
	public static void releaseWakeup(@NonNull final Activity activity) {
		activity.getWindow().clearFlags(
			WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
			| WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
			| WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}
}
