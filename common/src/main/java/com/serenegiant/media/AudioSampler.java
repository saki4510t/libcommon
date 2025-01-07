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
import android.util.Log;

import java.util.concurrent.locks.ReentrantLock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;

/**
 * AudioRecordを使って音声データを取得し、登録したコールバックへ分配するためのクラス
 * 同じ音声入力ソースに対して複数のAudioRecordを生成するとエラーになるのでシングルトン的にアクセス出来るようにするため
 */
public class AudioSampler extends IAudioSampler {
	private static final boolean DEBUG = false; // set false on production
	private static final String TAG = AudioSampler.class.getSimpleName();

	/**
	 * 排他制御用
	 */
	@NonNull
	protected final ReentrantLock mLock = new ReentrantLock();
	@Nullable
	private AudioRecordCompat.AudioRecordTask mAudioTask;
    private final int AUDIO_SOURCE;
    private final int SAMPLING_RATE, CHANNEL_COUNT;
	/**
	 * AudioRecordから1度に読み込みを試みるサンプル数(バイト数とは限らない)
	 * 1フレーム1チャネルあたりのサンプリング数
	 */
	private final int SAMPLES_PER_FRAME;
	/**
	 * AudioRecordから1℃に読み込みを試みるバイト数
	 * SAMPLES_PER_FRAME x 1サンプル辺りのバイト数
	 */
	private final int BYTES_PER_FRAME;
	private final boolean FORCE_SOURCE;

	/**
	 * コンストラクタ
	 * @param audioSource 音声ソース, MediaRecorder.AudioSourceのどれか
	 *     ただし、一般アプリで利用できないVOICE_UPLINK(2)はCAMCORDER(5)へ
	 *     VOICE_DOWNLINK(3)はVOICE_COMMUNICATION(7)
	 *     VOICE_CALL(4)はMIC(1)へ置換する
	 * @param channelNum
	 * @param samplingRate
	 */
	public AudioSampler(final int audioSource,
		final int channelNum, final int samplingRate) {

		this(audioSource, channelNum, samplingRate,
			AudioRecordCompat.SAMPLES_PER_FRAME, AudioRecordCompat.FRAMES_PER_BUFFER, false);
	}

	/**
	 * コンストラクタ
	 * @param audioSource 音声ソース, MediaRecorder.AudioSourceのどれか
	 *     ただし、一般アプリで利用できないVOICE_UPLINK(2)はCAMCORDER(5)へ
	 *     VOICE_DOWNLINK(3)はVOICE_COMMUNICATION(7)
	 *     VOICE_CALL(4)はMIC(1)へ置換する
	 * @param channelNum
	 * @param samplingRate
	 * @param samplesPerFrame
	 * @param framesPerBuffer
	 */
	public AudioSampler(final int audioSource, final int channelNum,
		final int samplingRate, final int samplesPerFrame, final int framesPerBuffer) {

		this(audioSource, channelNum, samplingRate, samplesPerFrame, framesPerBuffer, false);
	}

	/**
	 * コンストラクタ
	 * @param audioSource 音声ソース, MediaRecorder.AudioSourceのどれか
	 *     ただし、一般アプリで利用できないVOICE_UPLINK(2)はCAMCORDER(5)へ
	 *     VOICE_DOWNLINK(3)はVOICE_COMMUNICATION(7)
	 *     VOICE_CALL(4)はMIC(1)へ置換する
	 * @param channelNum 音声チャネル数, 1 or 2
	 * @param samplingRate サンプリングレート
	 * @param samplesPerFrame 1フレーム辺りのサンプル数
	 * @param framesPerBuffer バッファ辺りのフレーム数
	 * @param forceSource 音声ソースを強制するかどうか, falseなら利用可能な音声ソースを順に試す
	 */
	public AudioSampler(final int audioSource, final int channelNum,
		final int samplingRate, final int samplesPerFrame, final int framesPerBuffer,
		final boolean forceSource) {

		super();
//		if (DEBUG) Log.v(TAG, "コンストラクタ:");
		// パラメータを保存
		AUDIO_SOURCE = audioSource;
		CHANNEL_COUNT = channelNum;
		SAMPLING_RATE = samplingRate;
		SAMPLES_PER_FRAME = samplesPerFrame;
		BYTES_PER_FRAME = samplesPerFrame * channelNum
			* AudioRecordCompat.getBitResolution(AudioRecordCompat.DEFAULT_AUDIO_FORMAT);
		FORCE_SOURCE = forceSource;
	}

	/**
	 * 音声データ１つ辺りのサンプリング数を返す
	 * 1フレーム辺りのサンプリング数xチャネル数
	 * @return
	 */
	public int getSampledPerFrame() {
		return SAMPLES_PER_FRAME * CHANNEL_COUNT;
	}

	/**
	 * 音声データ１つ当たりのバイト数を返す
	 * 1フレーム辺りのサンプリング数xチャネル数x1サンプルあたりのバイト数
	 * (AudioRecordから1度に読み込みを試みる最大バイト数)
	 * @return
	 */
	@Override
	public int getBufferSize() {
		return BYTES_PER_FRAME;
	}

	@AudioRecordCompat.AudioFormats
	@Override
	public int getAudioFormat() {
		return AudioRecordCompat.DEFAULT_AUDIO_FORMAT;
	}

	/**
	 * 音声データサンプリング開始
	 * 実際の処理は別スレッド上で実行される
	 */
	@RequiresPermission(Manifest.permission.RECORD_AUDIO)
	@Override
	public synchronized void start() {
		if (DEBUG) Log.v(TAG, "start:isStarted=" + isStarted());
		super.start();
		mLock.lock();
		try {
			if (mAudioTask == null) {
				init_pool(BYTES_PER_FRAME);
				// 内蔵マイクからの音声取り込みスレッド生成＆実行
				mAudioTask = new AudioRecordCompat.AudioRecordTask(
					AUDIO_SOURCE, CHANNEL_COUNT, SAMPLING_RATE,
					SAMPLES_PER_FRAME, BYTES_PER_FRAME, FORCE_SOURCE, false) {

					@Override
					public boolean isRunning() {
						return super.isRunning()
							&& AudioSampler.this.isStarted();
					}

					@Nullable
					@Override
					protected MediaData obtain(final int bufferBytes) {
						return AudioSampler.this.obtain(bufferBytes);
					}

					@Override
					protected void queueData(@NonNull final MediaData data) {
						addMediaData((RecycleMediaData) data);
					}

					@Override
					protected void onError(@NonNull final Throwable t) {
						callOnError(t);
					}
				};
				new Thread(mAudioTask, "AudioTread").start();
			}
		} finally {
			mLock.unlock();
		}
	}

	/**
	 * 音声データのサンプリングを停止させる
	 */
	@Override
	public synchronized void stop() {
		if (DEBUG) Log.v(TAG, "stop:isStarted=" + isStarted());
		setIsCapturing(false);
		mLock.lock();
		try {
			mAudioTask = null;
		} finally {
			mLock.unlock();
		}
		super.stop();
	}

	@Override
	public int getAudioSource() {
		return AUDIO_SOURCE;
	}

	@Override
	public int getChannels() {
		return CHANNEL_COUNT;
	}

	@Override
	public int getSamplingFrequency() {
		return SAMPLING_RATE;
	}

	@Override
	public int getBitResolution() {
		return 16;	// AudioFormat.ENCODING_PCM_16BIT
	}

	@Override
	public int getAudioSessionId() {
		mLock.lock();
		try {
			return mAudioTask != null ? mAudioTask.getAudioSessionId() : 0;
		} finally {
			mLock.unlock();
		}
	}

}
