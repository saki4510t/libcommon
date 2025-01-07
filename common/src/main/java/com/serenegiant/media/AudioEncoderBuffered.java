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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.TimeUnit;

import android.media.MediaRecorder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 *　FIFOキューによるバッファリング付きのAudioEncoder
 */
public class AudioEncoderBuffered extends AbstractAudioEncoder {
	private static final boolean DEBUG = false;	// FIXME 実働時にはfalseにすること
	private static final String TAG = AudioEncoderBuffered.class.getSimpleName();

	private static final int MAX_POOL_SIZE = 100;
	private static final int MAX_QUEUE_SIZE = 100;

	private AudioThread mAudioThread = null;
	private DequeueThread mDequeueThread = null;
	/**
	 * キューに入れる音声データのバッファサイズ
	 */
	protected final int mBufferSize = AudioRecordCompat.SAMPLES_PER_FRAME;
	@NonNull
	private final MemMediaQueue mAudioQueue;

	public AudioEncoderBuffered(
		@NonNull final IRecorder recorder,
		@NonNull final EncoderListener2 listener,
		final int audioSource, final int audioChannels) {

		super(recorder, listener,
			audioSource, audioChannels, AudioRecordCompat.DEFAULT_SAMPLE_RATE, DEFAULT_BIT_RATE);
//		if (DEBUG) Log.v(TAG, "コンストラクタ:");
		if (audioSource < MediaRecorder.AudioSource.DEFAULT
			|| audioSource > MediaRecorder.AudioSource.VOICE_COMMUNICATION)
			throw new IllegalArgumentException("invalid audio source:" + audioSource);
		mAudioQueue = new MemMediaQueue(MAX_POOL_SIZE, MAX_POOL_SIZE, MAX_QUEUE_SIZE);
	}

	@Override
	public void start() {
		super.start();
		if (mAudioThread == null) {
			// 内蔵マイクからの音声取り込みスレッド生成＆実行
	        mAudioThread = new AudioThread();
			mAudioThread.start();
			mDequeueThread = new DequeueThread();
			mDequeueThread.start();
		}
	}

	@Override
	public void stop() {
		mAudioThread = null;
		mDequeueThread = null;
		super.stop();
	}

	/**
	 * AudioRecordから無圧縮PCM16bitで内蔵マイクからの音を取得してキューへ追加するためのスレッド
	 */
    private final class AudioThread extends Thread {
    	public AudioThread() {
    		super(new AudioRecordCompat.AudioRecordTask(
				mAudioSource, mChannelCount, mSampleRate,
				AudioRecordCompat.SAMPLES_PER_FRAME, AudioRecordCompat.FRAMES_PER_BUFFER) {
				@Override
				public boolean isRunning() {
					return super.isRunning() && isReady();
				}

				@Nullable
				@Override
				protected MediaData obtain(final int bufferBytes) {
					return mAudioQueue.obtain(bufferBytes);
				}

				@Override
				protected void queueData(@NonNull final MediaData data) {
					mAudioQueue.queueFrame((RecycleMediaData) data);
				}

				@Override
				protected void onError(@NonNull final Throwable t) {
					if (DEBUG) Log.w(TAG, t);
				}
			},"AudioThread");
    	}
    }

    /**
     * キューから音声データを取り出してエンコーダーへ書き込むスレッド
     */
    private final class DequeueThread extends Thread {
    	public DequeueThread() {
    		super("DequeueThread");
    	}

    	@Override
    	public void run() {
			RecycleMediaData data;
			int frame_count = 0;
    		for (; ;) {
        		synchronized (mSync) {
            		if (!isEncoding() || isRequestStop()) break;
            	}
    			try {
					data = mAudioQueue.poll(30, TimeUnit.MILLISECONDS);
				} catch (final InterruptedException e1) {
					break;
				}
    			if (data != null) {
    				if (data.size() > 0) {
    					encode(data.get(), data.presentationTimeUs());
    					frameAvailableSoon();
    					frame_count++;
    				}
					data.recycle();
    			}
    		} // for
			if (frame_count == 0) {
		    	// 1フレームも書き込めなかった時は動画出力時にMediaMuxerがクラッシュしないように
		    	// ダミーデータを書き込む
		    	final ByteBuffer buf = ByteBuffer.allocateDirect(mBufferSize).order(ByteOrder.nativeOrder());
		    	for (int i = 0; isEncoding() && (i < 5); i++) {
					buf.clear();
					buf.position(AudioRecordCompat.SAMPLES_PER_FRAME);
					buf.flip();
					encode(buf, getInputPTSUs());
					frameAvailableSoon();
					synchronized (this) {
						try {
							wait(50);
						} catch (final InterruptedException e) {
							// ignore
						}
					}
		    	}
			}
    	}
    }

}
