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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import androidx.annotation.NonNull;

public class ThreadPool {

	// for thread pool
	private static final int CORE_POOL_SIZE = 1;		// initial/minimum threads
	private static final int MAX_POOL_SIZE = 32;		// maximum threads
	private static final int KEEP_ALIVE_TIME_SECS = 10;	// time periods while keep the idle thread

	private static PausableThreadPoolExecutor EXECUTOR;

	static {
		getInstance();
	}

	private ThreadPool() {
		// インスタンス化をエラーにするためにデフォルトコンストラクタをprivateに
	}

	/**
	 * スレッドプールが存在しなければ新たに生成する
	 * @return
	 */
	@NonNull
	private static synchronized PausableThreadPoolExecutor getInstance() {
		if (EXECUTOR == null) {
			EXECUTOR = new PausableThreadPoolExecutor(
				CORE_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE_TIME_SECS,
				TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
		}
		return EXECUTOR;
	}

	/**
	 * シャットダウンしたかどうか
	 * @return
	 */
	public static synchronized boolean isShutdown() {
		return EXECUTOR == null || EXECUTOR.isShutdown();
	}

	/**
	 * スレッドプールのスレッド終了要求
	 * 新規のタスクをキューに入れることができなくなる
	 */
	public static synchronized void shutdown() {
		if (EXECUTOR != null) {
			EXECUTOR.resume();
			EXECUTOR.shutdown();
			EXECUTOR = null;
		}
	}

	/**
	 * スレッドプールのスレッド終了要求
	 * 未実行のタスクの一覧を返す
	 * @return
	 */
	@NonNull
	public static synchronized List<Runnable> shutdownNow() {
		final List<Runnable> result = new ArrayList<>();
		if (EXECUTOR != null) {
			final List<Runnable> list = EXECUTOR.shutdownNow();
			if (list != null) {
				result.addAll(list);
			}
			EXECUTOR = null;
		}
		return result;
	}

	/**
	 * スレッドプールで待機しているタスクの実行を再開する
	 */
	public void resume() {
		getInstance().resume();
	}

	/**
	 * スレッドプールで待機しているタスクの実行を一時中断する
	 */
	public void pause() {
		getInstance().pause();
	}

	/**
	 * コアスレッド数を設定する
	 * @param corePoolSize
	 */
	public static void setCorePoolSize(final int corePoolSize) {
		getInstance().setCorePoolSize(corePoolSize);
	}

	/**
	 * 最大スレッド数を設定する
	 * @param maximumPoolSize
	 */
	public static void setMaximumPoolSize(final int maximumPoolSize) {
		getInstance().setMaximumPoolSize(maximumPoolSize);
	}

	/**
	 * コアスレッド以外のアイドルスレッドを停止させるまでの時間を設定する
	 * @param time
	 * @param unit
	 */
	public static void setKeepAliveTime(final long time, final TimeUnit unit) {
		getInstance().setKeepAliveTime(time, unit);
	}

	/**
	 * 連続して実行されることがわかっているときなどにコアスレッドをあらかじめ起床しておく
	 */
	public static void preStartAllCoreThreads() {
		// in many case, calling createBitmapCache method means start the new query
		// and need to prepare to run asynchronous tasks
		getInstance().prestartAllCoreThreads();
	}

	/**
	 * スレッドプールのキューにタスクを追加する
	 * @param command
	 * @throws RejectedExecutionException
	 */
	public static void queueEvent(@NonNull final Runnable command)
		throws RejectedExecutionException {

		getInstance().execute(command);
	}

	/**
	 * 未実行のタスクをスレッドプールのキューから削除する
	 * @param command
	 * @return
	 */
	public static boolean removeEvent(@NonNull final Runnable command) {
		return getInstance().remove(command);
	}

	/**
	 * キューに入れたタスクの実行待ち/待ち解除を可能にするためのThreadPoolExecutor子クラス
	 */
	private static class PausableThreadPoolExecutor extends ThreadPoolExecutor {
		private boolean isPaused;
		private ReentrantLock pauseLock = new ReentrantLock();
		private Condition unpaused = pauseLock.newCondition();

		public PausableThreadPoolExecutor(
			final int corePoolSize, final int maximumPoolSize,
			final long keepAliveTime, final TimeUnit unit,
			final BlockingQueue<Runnable> workQueue) {

			super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
			isPaused = false;
			allowCoreThreadTimeOut(true);

		}

		protected void beforeExecute(final Thread t, final Runnable r) {
			super.beforeExecute(t, r);
			pauseLock.lock();
			try {
				while (isPaused) {
					// ポース中は実行待機する
					unpaused.await();
				}
			} catch (final InterruptedException ie) {
				t.interrupt();
			} finally {
				pauseLock.unlock();
			}
		}

		public void pause() {
			pauseLock.lock();
			try {
				isPaused = true;
			} finally {
				pauseLock.unlock();
			}
		}

		public void resume() {
			pauseLock.lock();
			try {
				isPaused = false;
				unpaused.signalAll();
			} finally {
				pauseLock.unlock();
			}
		}
	}
}
