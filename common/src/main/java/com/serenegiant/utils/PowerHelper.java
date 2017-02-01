package com.serenegiant.utils;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2017 saki t_saki@serenegiant.com
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

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.os.PowerManager;
import android.util.Log;
import android.view.WindowManager;

/**
 * スリープ状態から画面ON出来るようにするためのヘルパークラス
 * 実際に使うにはAndroidManifest.xmlに
 * <uses-permission android:name="android.permission.WAKE_LOCK"/>と
 * <uses-permission android:name="android.permission.DISABLE_KEYGUARD"/>
 * が必要
 */
public class PowerHelper {
	private static final String TAG = "PowerHelper";

	public static void wake(final Activity activity, final boolean disable_keyguard, final long lock_delayed) {
		try {
			// スリープ状態から起床(android.permission.WAKE_LOCKが必要)
			final PowerManager.WakeLock wakelock = ((PowerManager)activity.getSystemService(Context.POWER_SERVICE))
				.newWakeLock(PowerManager.FULL_WAKE_LOCK
							| PowerManager.ACQUIRE_CAUSES_WAKEUP
							| PowerManager.ON_AFTER_RELEASE, "disableLock");
			if (lock_delayed > 0) {
				wakelock.acquire(lock_delayed);
			} else {
				wakelock.acquire();
			}
			// キーガードを解除(android.permission.DISABLE_KEYGUARDが必要)
			try {
				final KeyguardManager keyguard = (KeyguardManager)activity.getSystemService(Context.KEYGUARD_SERVICE);
				final KeyguardManager.KeyguardLock keylock = keyguard.newKeyguardLock(TAG);
				keylock.disableKeyguard();
			} finally {
				wakelock.release();
			}
			// 画面がOFFにならないようにする
			activity.getWindow().addFlags(
				WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		} catch (final Exception e) {
			Log.w(TAG, e);
		}
	}

}
