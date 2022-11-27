package com.serenegiant.media;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2022 saki t_saki@serenegiant.com
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

import android.Manifest;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;

/**
 * AudioSampleから音声データを受け取ってMediaCodecでエンコードするためのクラス
 */
public class AudioSamplerEncoder extends AbstractAudioEncoder {
	private static final boolean DEBUG = false;	// FIXME 実働時にはfalseにすること
	private static final String TAG = AudioSamplerEncoder.class.getSimpleName();

	@NonNull
	private final IAudioSampler mSampler;
	private int frame_count = 0;

	/**
	 * コンストラクタ
	 * @param recorder
	 * @param listener
	 * @param sampler
	 */
	public AudioSamplerEncoder(
		@NonNull final IRecorder recorder,
		@NonNull final EncoderListener listener,
		@Nullable IAudioSampler sampler) {

		super(recorder, listener, sampler.getAudioSource(),
			sampler.getChannels(), sampler.getSamplingFrequency(),
			DEFAULT_BIT_RATE);
//		if (DEBUG) Log.v(TAG, "コンストラクタ:");
		mSampler = sampler;
	}

	@RequiresPermission(Manifest.permission.RECORD_AUDIO)
	@Override
	public void start() {
		super.start();
		mSampler.addCallback(mSoundSamplerCallback);
		new Thread(mAudioTask, "AudioTask").start();
	}

	@Override
	public void stop() {
		mSampler.removeCallback(mSoundSamplerCallback);
		super.stop();
	}

	/**
	 * AudioSampleからのコールバックリスナー
	 */
	private final AudioSampler.SoundSamplerCallback mSoundSamplerCallback
		= new AudioSampler.SoundSamplerCallback() {

		@Override
		public void onData(@NonNull final ByteBuffer buffer, final int size, final long presentationTimeUs) {
    		synchronized (mSync) {
    			// 既に終了しているか終了指示が出てれば何もしない
        		if (!mIsCapturing || mRequestStop) return;
    		}
			if (size > 0) {
				// 音声データを受け取った時はエンコーダーへ書き込む
				frameAvailableSoon();
				encode(buffer, size, presentationTimeUs);
				frame_count++;
			}
		}

		@Override
		public void onError(final Exception e) {
		}
	};

	/**
	 * エンコード処理が終了するまで待機するためのRunnable
	 * MediaMuxerを使ってエンコード済データの出力を行う場合に、
	 * １フレームも書き込めなかった場合にはMediaMuxerがクラッシュするので
	 * その場合にダミーのデータを書き込むために別スレッドを生成して待機する
	 */
	private final Runnable mAudioTask = new Runnable() {
		@Override
		public void run() {
			for (; ;) {
        		synchronized (mSync) {
            		if (!mIsCapturing || mRequestStop) break;
            		try {
						mSync.wait();
					} catch (final InterruptedException e) {
						break;
					}
            	}
			}
			if (frame_count == 0) {
		    	// 1フレームも書き込めなかった時は動画出力時にMediaMuxerがクラッシュしないように
		    	// ダミーデータを書き込む
		    	final ByteBuffer buf = ByteBuffer.allocateDirect(AudioRecordCompat.SAMPLES_PER_FRAME).order(ByteOrder.nativeOrder());
		    	for (int i = 0; mIsCapturing && (i < 5); i++) {
		    		buf.clear();
					buf.position(AudioRecordCompat.SAMPLES_PER_FRAME);
					buf.flip();
					encode(buf, AudioRecordCompat.SAMPLES_PER_FRAME, getInputPTSUs());
					frameAvailableSoon();
					synchronized (this) {
						try {
							wait(50);
						} catch (final InterruptedException e) {
							break;
						}
					}
		    	}
			}
		}
	};
}
