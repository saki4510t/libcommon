package com.serenegiant.utils;
/*
 * Androusb
 * Copyright (c) 2014-2016 saki t_saki@serenegiant.com
 * Distributed under the terms of the GNU Lesser General Public License (LGPL v3.0) License.
 * License details are in the file license.txt, distributed as part of this software.
 */

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

public class HandlerThreadHandler extends Handler {
	private static final String TAG = "HandlerThreadHandler";

	public static final HandlerThreadHandler createHandler() {
		return createHandler(TAG);
	}

	public static final HandlerThreadHandler createHandler(final String name) {
		final HandlerThread thread = new HandlerThread(name);
		thread.start();
		return new HandlerThreadHandler(thread.getLooper());
	}

	public static final HandlerThreadHandler createHandler(final Callback callback) {
		return createHandler(TAG, callback);
	}

	public static final HandlerThreadHandler createHandler(final String name, final Callback callback) {
		final HandlerThread thread = new HandlerThread(name);
		thread.start();
		return new HandlerThreadHandler(thread.getLooper(), callback);
	}

	private HandlerThreadHandler(final Looper looper) {
		super(looper);
	}

	private HandlerThreadHandler(final Looper looper, final Callback callback) {
		super(looper, callback);
	}

}
