package com.serenegiant.utils;
/*
 * Androusb
 * Copyright (c) 2014-2016 saki t_saki@serenegiant.com
 * Distributed under the terms of the GNU Lesser General Public License (LGPL v3.0) License.
 * License details are in the file license.txt, distributed as part of this software.
 */

import android.os.Build;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadPool {

    // for thread pool
    private static final int CORE_POOL_SIZE = 4;		// initial/minimum threads
    private static final int MAX_POOL_SIZE = 32;		// maximum threads
    private static final int KEEP_ALIVE_TIME = 10;		// time periods while keep the idle thread
    private static final ThreadPoolExecutor EXECUTOR
		= new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE_TIME,
			TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

	static {
		if (Build.VERSION.SDK_INT >= 9) {
			EXECUTOR.allowCoreThreadTimeOut(true);	// this makes core threads can terminate
		}
	}

	public static void preStartAllCoreThreads() {
		// in many case, calling createBitmapCache method means start the new query
		// and need to prepare to run asynchronous tasks
		EXECUTOR.prestartAllCoreThreads();
	}

	public static void queueEvent(final Runnable command) {
		EXECUTOR.execute(command);
	}
}
