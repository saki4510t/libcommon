package com.serenegiant.utils;

/*
 * Androusb
 * Copyright (c) 2014-2016 saki t_saki@serenegiant.com
 * Distributed under the terms of the GNU Lesser General Public License (LGPL v3.0) License.
 * License details are in the file license.txt, distributed as part of this software.
 */

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.Log;

public final class UIThreadHelper {
	private static final String TAG = UIThreadHelper.class.getSimpleName();

	/** UI操作用のHandler */
	private static final Handler sUIHandler = new Handler(Looper.getMainLooper());
	/** UIスレッドの参照 */
	private static final Thread sUiThread = sUIHandler.getLooper().getThread();

	/**
	 * UIスレッドでRunnableを実行するためのヘルパーメソッド
	 * @param task
	 */
	public static final void runOnUiThread(@NonNull final Runnable task) {
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

	public static final void runOnUiThread(@NonNull final Runnable task, final long duration) {
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

	public static final void removeFromUiThread(@NonNull final Runnable task) {
		sUIHandler.removeCallbacks(task);
	}
}
