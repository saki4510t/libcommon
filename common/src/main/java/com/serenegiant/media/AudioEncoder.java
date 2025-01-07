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

import android.media.MediaRecorder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * AudioRecordから音声データを取得してMediaCodecエンコーダーでエンコードするためのクラス
 */
public class AudioEncoder extends AbstractAudioEncoder {
	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = AudioEncoder.class.getSimpleName();

    private AudioThread mAudioThread = null;

	/**
	 * コンストラクタ
	 * @param recorder
	 * @param listener
	 * @param audioSource
	 * @param audioChannels
	 */
	public AudioEncoder(
		@NonNull final IRecorder recorder,
		@NonNull final EncoderListener2 listener,
		final int audioSource, final int audioChannels) {

		super(recorder, listener,
			audioSource, audioChannels,
			AudioRecordCompat.DEFAULT_SAMPLE_RATE, DEFAULT_BIT_RATE);
		if (DEBUG) Log.v(TAG, "コンストラクタ:");
		if (audioSource < MediaRecorder.AudioSource.DEFAULT
			|| audioSource > MediaRecorder.AudioSource.VOICE_COMMUNICATION)
			throw new IllegalArgumentException("invalid audio source:" + audioSource);
	}

	@Override
	public void start() {
		super.start();
		if (DEBUG) Log.v(TAG, "start:");
		if (mAudioThread == null) {
			// 内蔵マイクからの音声取り込みスレッド生成＆実行
	        mAudioThread = new AudioThread();
			mAudioThread.start();
		}
	}

	@Override
	public void stop() {
		if (DEBUG) Log.v(TAG, "stop:");
		mAudioThread = null;
		super.stop();
	}

	@Override
	public void release() {
		if (DEBUG) Log.v(TAG, "release:");
		mAudioThread = null;
		super.release();
	}

	/**
	 * AudioRecordから無圧縮PCM16bitで内蔵マイクからの音を取得してエンコーダーへ書き込むためのスレッド
	 */
    private final class AudioThread extends Thread {
    	public AudioThread() {
    		super(new AudioRecordCompat.AudioRecordTask(
				mAudioSource, mChannelCount, mSampleRate,
				AudioRecordCompat.SAMPLES_PER_FRAME, AudioRecordCompat.FRAMES_PER_BUFFER,
				false, true) {
				private final MediaData data = new MediaData();

				@Override
				public boolean isRunning() {
					return super.isRunning() && isReady();
				}

				@Nullable
				@Override
				protected MediaData obtain(final int bufferBytes) {
					data.resize(bufferBytes);
					return data;
				}

				@Override
				protected void recycle(@NonNull final MediaData data) {
					// 1つしか使っていないのでリサイクルは不用
				}

				@Override
				protected void queueData(@NonNull final MediaData data) {
					encode(data.get(), data.presentationTimeUs());
					frameAvailableSoon();
				}

				@Override
				protected void onError(@NonNull final Throwable t) {
					if (DEBUG) Log.w(TAG, t);
				}
			}, "AudioThread");
    	}
    }

}
