package com.serenegiant.media;
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

import android.Manifest;
import android.annotation.SuppressLint;
import android.util.Log;

import com.serenegiant.system.Time;

import java.nio.ByteBuffer;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;

public abstract class IAudioSampler {
	private static final boolean DEBUG = false;	// 実働時はfalseにすること
	private final String TAG = getClass().getSimpleName();

	/**
	 * 音声データ取得コールバックインターフェース
	 */
	public interface AudioSamplerCallback {
		/**
		 * 音声データが準備出来た時に呼び出される。
		 * presentationTimeUsは音声データを取得した時の時刻だが、他のコールバックでの処理によっては
		 * 遅延して呼び出される可能性がある。任意のスレッド上で実行される。
		 * 可能な限り早く処理を終えること。
		 * @param buffer
		 * @param presentationTimeUs
		 */
		public void onData(@NonNull final ByteBuffer buffer, final long presentationTimeUs);

		/**
		 * エラーが起こった時の処理(今は未使用)
		 * @param t
		 */
		public void onError(@NonNull  Throwable t);
	}

	@Deprecated
	public interface SoundSamplerCallback extends AudioSamplerCallback {
	}

	/**
	 * バッファリング用に生成する音声データレコードの最大生成する
	 */
	private static final int MAX_POOL_SIZE = 200;
	/**
	 * 音声データキューに存在できる最大音声データレコード数
	 * 25フレーム/秒のはずなので最大で約4秒分
	 */
	private static final int MAX_QUEUE_SIZE = 200;

	// 音声データキュー用
	private final MemMediaQueue mAudioQueue;

	// コールバック用
	private CallbackThread mCallbackThread;
	@NonNull
	private final Object mCallbackSync = new Object();
	@NonNull
	private final Set<AudioSamplerCallback> mCallbacks
		= new CopyOnWriteArraySet<>();
	private volatile boolean mIsCapturing;

	public IAudioSampler() {
		mAudioQueue = new MemMediaQueue(MAX_POOL_SIZE, MAX_POOL_SIZE, MAX_QUEUE_SIZE);
	}

	/**
	 * 音声データのサンプリングを停止して全てのコールバックを削除する
	 */
	public void release() {
		if (DEBUG) Log.v(TAG, "release:isStarted=" + isStarted());
		if (isStarted()) {
			stop();
		}
//		mIsCapturing = false;	// 念の為に
		mCallbacks.clear();
		if (DEBUG) Log.v(TAG, "release:finished");
	}

	/**
	 * 音声サンプリング開始
	 */
	@RequiresPermission(Manifest.permission.RECORD_AUDIO)
	public synchronized void start() {
		if (DEBUG) Log.v(TAG, "start:");
		// コールバック用スレッドを生成＆実行
		synchronized (mCallbackSync) {
			if (mCallbackThread == null) {
				mIsCapturing = true;
				mCallbackThread = new CallbackThread();
				mCallbackThread.start();
			}
		}
		if (DEBUG) Log.v(TAG, "start:finished");
	}

	/**
	 * 音声サンプリング終了
	 */
	public synchronized void stop() {
		if (DEBUG) Log.v(TAG, "stop:");
//		new Throwable().printStackTrace();
		synchronized (mCallbackSync) {
			final boolean capturing = mIsCapturing;
			mIsCapturing = false;
			mCallbackThread = null;
			if (capturing) {
				try {
					mCallbackSync.wait();
				} catch (InterruptedException e) {
					// ignore
				}
			}
		}
		if (DEBUG) Log.v(TAG, "stop:finished");
	}

	/**
	 * コールバックを追加する
	 * @param callback
	 */
	public void addCallback(final AudioSamplerCallback callback) {
		if (callback != null) {
			mCallbacks.add(callback);
		}
	}

	/**
	 * コールバックを削除する
	 * @param callback
	 */
	public void removeCallback(final AudioSamplerCallback callback) {
		if (callback != null) {
			mCallbacks.remove(callback);
		}
	}

	protected void setIsCapturing(final boolean isCapturing) {
		mIsCapturing = isCapturing;
	}

	/**
	 * 音声データのサンプリング中かどうかを返す
	 * @return
	 */
	public boolean isStarted() {
		return mIsCapturing;
	}

	/**
	 * コールバックが登録されているかどうか
	 * @return
	 */
	public boolean hasCallback() {
		return !mCallbacks.isEmpty();
	}

	/**
	 * コールバックリスナーを取得
	 * @return
	 */
	protected Set<AudioSamplerCallback> getCallbacks() {
		return mCallbacks;
	}

	@AudioRecordCompat.AudioFormats
	public abstract int getAudioFormat();

	/**
	 * 音声入力ソースを返す
	 * 100以上ならUAC
	 * @return
	 */
	public abstract int getAudioSource();
	/**
	 * チャネル数を返す
	 * @return
	 */
	public abstract int getChannels();
	/**
	 * サンプリング周波数を返す
	 * @return
	 */
	public abstract int getSamplingFrequency();
	/**
	 *PCMエンコードの解像度(ビット数)を返す。8か16
	 * @return
	 */
	public abstract int getBitResolution();

	/**
	 * 音声セッションIDを取得
	 * @return 無効な場合は0を返す
	 */
	public abstract int getAudioSessionId();

	/**
	 * 音声データ１つ当たりのバイト数を返す
	 * (AudioRecordから1度に読み込みを試みる最大バイト数)
	 * @return
	 */
	public int getBufferSize() {
		return mDefaultBufferSize;
	}

	/**
	 * キュー内のデータを全てリサイクルして空にする
	 */
	public void drainAll() {
		mAudioQueue.drainAll();
	}

	/**
	 * 音声データ取得時のコールバックを呼び出す
	 * @param data
	 */
	private void callOnData(@NonNull final MediaData data) {
		@NonNull
		final ByteBuffer buf = data.get();
		final int size = data.size();
		final long pts = data.presentationTimeUs();
		for (final AudioSamplerCallback callback: mCallbacks) {
			try {
				buf.clear();
				buf.position(size);
				buf.flip();
				callback.onData(buf, pts);
			} catch (final Exception e) {
				mCallbacks.remove(callback);
				Log.w(TAG, "callOnData:", e);
			}
		}
    }

	/**
	 * エラー発生時のコールバックを呼び出す
	 * @param e
	 */
    protected void callOnError(final Throwable e) {
		for (final AudioSamplerCallback callback: mCallbacks) {
			try {
				callback.onError(e);
			} catch (final Exception e1) {
				mCallbacks.remove(callback);
				Log.w(TAG, "callOnError:", e1);
			}
		}
    }

	protected int mDefaultBufferSize = 1024;
	protected void init_pool(final int default_buffer_size) {
		mDefaultBufferSize = default_buffer_size;
		mAudioQueue.init(default_buffer_size);
	}

	/**
	 * 音声データバッファをプールから取得する。
	 * プールがからの場合には最大MAX_POOL_SIZE個までは新規生成する
	 * @param bufferBytes
	 * @return
	 */
	protected RecycleMediaData obtain(final int bufferBytes) {
//		if (DEBUG) Log.v(TAG, "obtain:" + mPool.size() + ",mBufferNum=" + mBufferNum);
		return mAudioQueue.obtain(bufferBytes);
	}

	protected boolean addMediaData(@NonNull final RecycleMediaData data) {
//		if (DEBUG) Log.v(TAG, "addMediaData:" + mAudioQueue.size());
		return mAudioQueue.queueFrame(data);
	}

	protected RecycleMediaData pollMediaData(final long timeout_msec) throws InterruptedException {
		return mAudioQueue.poll(timeout_msec, TimeUnit.MILLISECONDS);
	}

	/**
	 * 前回MediaCodecへのエンコード時に使ったpresentationTimeUs
	 */
	private long prevInputPTSUs = -1;

	/**
	 * 今回の書き込み用のpresentationTimeUs値を取得
	 * @return
	 */
    @SuppressLint("NewApi")
	protected long getInputPTSUs() {
		long result = Time.nanoTime() / 1000L;
		if (result <= prevInputPTSUs) {
			result = prevInputPTSUs + 9643;
		}
		prevInputPTSUs = result;
		return result;
    }

    /**
     * キューから音声データを取り出してコールバックを呼び出すためのスレッド
     */
    private final class CallbackThread extends Thread {
    	public CallbackThread() {
    		super("AudioSampler");
    	}

    	@Override
    	public void run() {
    		if (DEBUG) Log.i(TAG, "CallbackThread:start");
    		android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO); // THREAD_PRIORITY_URGENT_AUDIO
			RecycleMediaData data;
    		for (; mIsCapturing ;) {
    			try {
					data = pollMediaData(100);
				} catch (final InterruptedException e) {
					break;
				}
    			if (data != null) {
    				callOnData(data);
    				// 使用済みのバッファをプールに戻して再利用する
    				data.recycle();
    			}
    		} // for (; mIsCapturing ;)
    		synchronized (mCallbackSync) {
				mCallbackSync.notifyAll();
			}
    		if (DEBUG) Log.i(TAG, "CallbackThread:finished");
    	}
    }

}
