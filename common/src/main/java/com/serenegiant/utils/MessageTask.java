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

import android.util.Log;

import com.serenegiant.collections.ReentrantReadWriteList;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

/**
 * Looper/Handler経由での実装だと少なくともAPI22未満では
 * Looperによる同期バリアの影響を受ける(==vsync同期してしまうので
 * 60fpsの場合最大で16ミリ秒遅延する)のでそれを避けるために
 * Looper/Handlerを使わずに簡易的にメッセージ処理を行うための
 * ヘルパークラス
 * MessageTaskまたはその継承クラスをTreadへ引き渡して実行する
 */
public abstract class MessageTask implements Runnable {
	private static final boolean DEBUG = false;	// 実働時はfalseにすること
	private static final String TAG = MessageTask.class.getSimpleName();

	/**
	 * 中断指示を表すためのRuntimeException実装
	 */
	public static class TaskBreak extends RuntimeException {
	}

	/**
	 * offerAndWait呼び出し処理用のコールバックインターフェース
	 */
	private interface MessageCallback {
		/**
		 * Requestが実行されたときのコールバック
		 * @param req
		 * @param result
		 */
		@WorkerThread
		public void onResult(@NonNull final Request req, final Object result);
	}

	/**
	 * 実行要求ホルダー
	 */
	protected static final class Request {
		int request;
		int arg1;
		int arg2;
		Object obj;
		int request_for_result;
		Object result;
		MessageCallback callback;

		private Request() {
			request = request_for_result = REQUEST_TASK_NON;
		}

		/**
		 * @param _request minus value is reserved internal use
		 * @param _arg1
		 * @param _arg2
		 * @param _obj
		 */
		public Request(final int _request,
			final int _arg1, final int _arg2, final Object _obj) {

			request = _request;
			arg1 = _arg1;
			arg2 = _arg2;
			obj = _obj;
			request_for_result = REQUEST_TASK_NON;
		}

		public void setResult(@Nullable final Object result) {
			synchronized (this) {
				this.result = result;
				request = request_for_result = REQUEST_TASK_NON;
				if (callback != null) {
					callback.onResult(this, result);
				}
				callback = null;
				notifyAll();
			}
		}

		@Override
		public boolean equals(final Object o) {
			return (o instanceof Request)
				? (request == ((Request) o).request)
					&& (request_for_result == ((Request)o).request_for_result)
					&& (arg1 == ((Request) o).arg1)
					&& (arg2 == ((Request) o).arg2)
					&& (obj == ((Request) o).obj)
				: super.equals(o);
		}

		@NonNull
		@Override
		public String toString() {
			return "Request{" +
				"request=" + request +
				", arg1=" + arg1 +
				", arg2=" + arg2 +
				", obj=" + obj +
				", request_for_result=" + request_for_result +
				", result=" + result +
				", callback=" + callback +
				'}';
		}
	}

	// minus values and zero are reserved for internal use
	protected static final int REQUEST_TASK_NON = 0;
	protected static final int REQUEST_TASK_RUN = -1;
	protected static final int REQUEST_TASK_RUN_AND_WAIT = -2;
	protected static final int REQUEST_TASK_START = -8;
	protected static final int REQUEST_TASK_QUIT = -9;

	@NonNull
	private final Object mSync = new Object();
	/** プール/キューのサイズ, -1なら無制限 */
	private final int mMaxRequest;
	@NonNull
	private final ReentrantReadWriteList<Request> mRequestPool;
	@NonNull
	private final LinkedBlockingDeque<Request> mRequestQueue;
	private volatile boolean mIsRunning;
	private volatile boolean mFinished;
	private Thread mWorkerThread;
	private long mWorkerThreadId;

	/**
	 * コンストラクタ
	 * プール&キューのサイズは無制限
	 * プールは空で生成
	 */
	public MessageTask() {
		this(-1, 0);
	}

	/**
	 * コンストラクタ
	 * プール&キューのサイズは無制限
	 * @param init_num　プールするRequestの初期数を指定
	 */
	public MessageTask(final int init_num) {
		this(-1, init_num);
	}

	/**
	 * コンストラクタ
	 * プール及びキュー可能な最大サイズを指定して初期化
	 * @param max_request キューの最大サイズを指定
	 * @param init_num プールするRequestの初期数を指定, max_requestよりも大きければ切り捨てる
	 */
	public MessageTask(final int max_request, final int init_num) {
		mMaxRequest = max_request;
		if (max_request > 0) {
			mRequestPool = new ReentrantReadWriteList<Request>();
			mRequestQueue = new LinkedBlockingDeque<Request>(max_request);
		} else {
			mRequestPool = new ReentrantReadWriteList<Request>();
			mRequestQueue = new LinkedBlockingDeque<Request>();
		}
		for (int i = 0; i < init_num; i++) {
			if (!mRequestPool.add(new Request())) break;
		}
		mIsRunning = false;
		mFinished = false;
	}

	/**
	 * 初期化要求。継承クラスのコンストラクタから呼び出すこと
	 * パラメータはonInitに引き渡される
	 * @param arg1
	 * @param arg2
	 * @param obj
	 */
	protected void init(final int arg1, final int arg2, @Nullable final Object obj) {
		if (DEBUG) Log.v(TAG, "init:");
		mFinished = false;
		mRequestQueue.offer(obtain(REQUEST_TASK_START, arg1, arg2, obj));
//		offer(REQUEST_TASK_START, arg1, arg2, obj);
	}

	/** 初期化処理 */
	@WorkerThread
	protected abstract void onInit(final int arg1, final int arg2, final Object obj);

	/** 要求処理ループ開始直前に呼ばれる */
	@WorkerThread
	protected abstract void onStart();

	/** onStopの直前に呼び出される, interruptされた時は呼び出されない */
	@WorkerThread
	protected void onBeforeStop() {}

	/** 停止処理, interruptされた時は呼び出されない */
	@WorkerThread
	protected abstract void onStop();

	/** onStop後に呼び出される。onStopで例外発生しても呼ばれる */
	@WorkerThread
	protected abstract void onRelease();

	/**
	 * メッセージ処理ループ中でのエラー発生時の処理
	 * デフフォルトはtrueを返しメッセージ処理ループを終了する
	 * @return trueを返すとメッセージ処理ループを終了する
	 */
	protected boolean onError(final Throwable e) {
//		if (DEBUG) Log.w(TAG, e);
		return true;
	}

	/** 要求メッセージの処理(内部メッセージは来ない)
	 * TaskBreakをthrowすると要求メッセージ処理ループを終了する */
	@WorkerThread
	protected abstract Object processRequest(final int request,
		final int arg1, final int arg2, final Object obj) throws TaskBreak;

	/** 要求メッセージを取り出す処理(要求メッセージがなければブロックされる) */
	protected Request takeRequest() throws InterruptedException {
		return mRequestQueue.take();
	}

	public boolean waitReady() {
		synchronized (mSync) {
			while (!mIsRunning && !mFinished) {
				try {
					mSync.wait(500);
				} catch (final InterruptedException e) {
					break;
				}
			} // end of while
			if (DEBUG) Log.v(TAG, "waitReady:" + mIsRunning);
			return mIsRunning;
		}
	}

	public boolean isRunning() {
		return mIsRunning;
	}

	public boolean isFinished() {
		return mFinished;
	}

	protected boolean isOnWorkerThread() {
		return mWorkerThreadId == Thread.currentThread().getId();
	}

	protected int getMaxRequest() {
		return mMaxRequest;
	}

	protected int getCurrentRequests() {
		return mRequestQueue.size();
	}

	@Override
	public void run() {
		if (DEBUG) Log.v(TAG, "run:");
		Request request = null;
		// ここでmIsRunning=trueにしてしまうと初期化・開始処理が終了する前に#waitReadyが抜けてしまうの
		mFinished = false;
		try {
			// #initが呼ばれて最初のリクエストがくるのを待機する
			// #initで送るのはREQUEST_TASK_STARTだけどそれ以外でも問題ない
			request = mRequestQueue.take();
		} catch (final InterruptedException e) {
			mIsRunning = false;
			mFinished = true;
		}
		if (!mFinished) {
			mWorkerThread = Thread.currentThread();
			mWorkerThreadId = mWorkerThread.getId();
			try {
				onInit(request.arg1, request.arg2, request.obj);
			} catch (final Exception e) {
				Log.w(TAG, e);
				mIsRunning = false;
				mFinished = true;
			}
		}
		if (!mFinished) {
			try {
				onStart();
				mIsRunning = true;
			} catch (final Exception e) {
				if (callOnError(e)) {
					mIsRunning = false;
					mFinished = true;
				}
			}
		}
		synchronized (mSync) {
			mSync.notify();
		}
LOOP:	while (mIsRunning) {
			try {
				request = takeRequest();
				switch (request.request) {
				case REQUEST_TASK_NON:
					break;
				case REQUEST_TASK_QUIT:
					break LOOP;
				case REQUEST_TASK_RUN:
					if (request.obj instanceof Runnable) {
						try {
							((Runnable)request.obj).run();
						} catch (final Exception e) {
							if (callOnError(e))
								break LOOP;
						}
					} else {
						if (DEBUG) Log.w(TAG, "Unknown task");
						// ここにくることはないはず
					}
					break;
				case REQUEST_TASK_RUN_AND_WAIT:
					try {
						request.setResult(processRequest(request.request_for_result, request.arg1, request.arg2, request.obj));
					} catch (final TaskBreak e) {
						request.setResult(null);
						break LOOP;
					} catch (final Exception e) {
						request.setResult(null);
						if (callOnError(e)) {
							break LOOP;
						}
					}
					break;
				default:
					try {
						processRequest(request.request, request.arg1, request.arg2, request.obj);
					} catch (final TaskBreak e) {
						break LOOP;
					} catch (final Exception e) {
						if (callOnError(e))
							break LOOP;
					}
					break;
				}
				request.request = request.request_for_result = REQUEST_TASK_NON;
				// プールへ返却する
				mRequestPool.add(request);
			} catch (final InterruptedException e) {
				break;
			}
		} // end of while
		final boolean interrupted = Thread.interrupted();
		synchronized (mSync) {
			mWorkerThread = null;
			mWorkerThreadId = 0;
			mIsRunning = false;
			mFinished = true;
		}
		if (!interrupted) {
			try {
				onBeforeStop();
				onStop();
			} catch (final Exception e) {
				callOnError(e);
			}
		}
		try {
			onRelease();
		} catch (final Exception e) {
			// callOnError(e);
		}
		synchronized (mSync) {
			mSync.notify();
		}
	}

	/**
	 * エラー処理。onErrorを呼び出す。
	 * trueを返すと要求メッセージ処理ループを終了する
	 * @param t
	 * @return
	 */
	protected boolean callOnError(final Throwable t) {
		try {
			return onError(t);
		} catch (final Exception e2) {
//			if (DEBUG) Log.e(TAG, "exception occurred in callOnError", e);
		}
		return true;
	}

	/**
	 * RequestプールからRequestを取得する
	 * プールが空の場合は新規に生成する
	 * @param request minus values and zero are reserved
	 * @param arg1
	 * @param arg2
	 * @param obj
	 * @return Request
	 */
	protected Request obtain(final int request, final int arg1, final int arg2, final Object obj) {
		Request req = mRequestPool.removeLast();
		if (req != null) {
			req.request = request;
			req.arg1 = arg1;
			req.arg2 = arg2;
			req.obj = obj;
			req.request_for_result = REQUEST_TASK_NON;
			req.result = null;
			req.callback = null;
		} else {
			req = new Request(request, arg1, arg2, obj);
		}
		return req;
	}

	/**
	 * offer request to run on worker thread
	 * @param request minus values and zero are reserved
	 * @param arg1
	 * @param arg2
	 * @param obj
	 * @return true if success offer
	 */
	public boolean offer(final int request,
		final int arg1, final int arg2, final Object obj) {

		return !mFinished && mRequestQueue.offer(obtain(request, arg1, arg2, obj));
	}

	/**
	 * offer request to run on worker thread
	 * @param request minus values and zero are reserved
	 * @param arg1
	 * @param obj
	 * @return true if success offer
	 */
	public boolean offer(final int request, final int arg1, final Object obj) {
		return offer(request, arg1, 0, obj);
	}

	/**
	 * offer request to run on worker thread
	 * @param request minus values and zero are reserved
	 * @param arg1
	 * @param arg2
	 * @return true if success offer
	 */
	public boolean offer(final int request, final int arg1, final int arg2) {
		return offer(request, arg1, arg2, null);
	}

	/**
	 * offer request to run on worker thread
	 * @param request minus values and zero are reserved
	 * @param arg1
	 * @return true if success offer
	 */
	public boolean offer(final int request, final int arg1) {
		return offer(request, arg1, 0, null);
	}

	/**
	 * offer request to run on worker thread
	 * @param request minus values and zero are reserved
	 * @return true if success offer
	 */
	public boolean offer(final int request) {
		return offer(request, 0, 0, null);
	}

	/**
	 * offer request to run on worker thread
	 * @param request minus values and zero are reserved
	 * @param obj
	 * @return true if success offer
	 */
	public boolean offer(final int request, final Object obj) {
		return offer(request, 0, 0, obj);
	}

	/**
	 * offer request to run on worker thread on top of the request queue
	 * @param request minus values and zero are reserved
	 * @param arg1
	 * @param arg2
	 */
	public boolean offerFirst(final int request,
		final int arg1, final int arg2, final Object obj) {

		return !mFinished && mIsRunning
			&& mRequestQueue.offerFirst(obtain(request, arg1, arg2, obj));
	}

	/**
	 * offer request to run on worker thread and wait for result
	 * caller thread is blocked until the request finished running on worker thread
	 * 呼び出し元がMessageTaskのワーカースレッド上の場合にはデッドロックを避けるために直ちに要求が実行される。
	 * このためワーカースレッド上に実行待ちの要求が存在する場合にはそれよりも先に実行されることになるので注意。
	 * @param request
	 * @param arg1
	 * @param arg2
	 * @param obj
	 * @return
	 */
	public Object offerAndWait(final int request,
		final int arg1, final int arg2, final Object obj) {

		if (!mFinished && (request > REQUEST_TASK_NON)) {
			final Request req = obtain(REQUEST_TASK_RUN_AND_WAIT, arg1, arg2, obj);
			if (!isOnWorkerThread()) {
				// ワーカースレッド上でなければワーカースレッド上での実行要求＆結果を待機する
				final Semaphore sem = new Semaphore(0);
				final AtomicReference<Object> ret = new AtomicReference<>();
				req.request_for_result = request;
				req.result = null;
				req.callback = new MessageCallback() {
					@Override
					public void onResult(@NonNull final Request req, final Object result) {
						ret.set(result);
						sem.release();
					}
				};
				mRequestQueue.offer(req);
				while (mIsRunning && (req.request_for_result != REQUEST_TASK_NON)) {
					try {
						sem.acquire(100);
					} catch (final InterruptedException e) {
						break;
					}
				}
				return ret.get();
			} else {
				// ワーカースレッド上ならここで実行する
				try {
					req.setResult(processRequest(
						req.request_for_result,
						req.arg1, req.arg2, req.obj));
				} catch (final TaskBreak e) {
					req.setResult(null);
				} catch (final Exception e) {
					req.setResult(null);
					callOnError(e);
				}
			}
			return req.result;
		} else {
			return null;
		}
	}

	/**
	 * request to run on worker thread
	 * @param task
	 * @return true if success queue
	 */
	public boolean queueEvent(final Runnable task) {
		return !mFinished && (task != null) && offer(REQUEST_TASK_RUN, task);
	}

	/**
	 * Remove specific request from queue
	 * @param request
	 */
	public void removeRequest(final Request request) {
		for (final Request req: mRequestQueue) {
			if (!mIsRunning || mFinished || !mRequestQueue.contains(request)) break;
			if (req.equals(request)) {
				mRequestQueue.remove(req);
				mRequestPool.add(req);
			}
		}
	}

	/**
	 * Remove specific request from queue
	 * @param request
	 */
	public void removeRequest(final int request) {
		for (final Request req: mRequestQueue) {
			if (!mIsRunning || mFinished) break;
			if (req.request == request) {
				mRequestQueue.remove(req);
				mRequestPool.add(req);
			}
		}
	}

	/**
	 * request terminate worker thread and release all related resources
	 */
	public void release() {
		release(false);
	}

	/**
	 * request terminate worker thread and release all related resources
	 * @param interrupt trueなら実行中のタスクをinterruptする
	 */
	public void release(final boolean interrupt) {
		final boolean b = mIsRunning;
		mIsRunning = false;
		if (!mFinished) {
			mRequestQueue.clear();
			mRequestQueue.offerFirst(obtain(REQUEST_TASK_QUIT, 0, 0, null));
			synchronized (mSync) {
				if (b) {
					final long current = Thread.currentThread().getId();
					final long id = mWorkerThread != null ? mWorkerThread.getId() : current;
					if (id != current) {
						if (interrupt && (mWorkerThread != null)) {
							mWorkerThread.interrupt();
						}
						while (!mFinished) {
							try {
								mSync.wait(300);
							} catch (final InterruptedException e) {
								// ignore
							}
						}
					}
				}
			}
		}
	}

	/**
	 * 実行中のタスクが終了後開放する
	 */
	public void releaseSelf() {
		mIsRunning = false;
		if (!mFinished) {
			mRequestQueue.clear();
			mRequestQueue.offerFirst(obtain(REQUEST_TASK_QUIT, 0, 0, null));
		}
	}

	/**
	 * processRequest内でメッセージループを非常終了させるためのヘルパーメソッド
	 * 単にTaskBreakをthrowするだけ
	 * @throws TaskBreak
	 */
	public void userBreak() throws TaskBreak {
		throw new TaskBreak();
	}
}
