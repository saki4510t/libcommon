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

import android.Manifest;
import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;

/**
 * IAudioSampleから音声データを受け取ってMediaCodecでエンコードするためのクラス
 * IAudioSamplerがすでにAACエンコードしている(EncodedAudioSampler)場合は
 * 実際のエンコード処理をスキップする
 */
public class AudioSamplerEncoder extends AbstractAudioEncoder {
	private static final boolean DEBUG = false;	// FIXME 実働時にはfalseにすること
	private static final String TAG = AudioSamplerEncoder.class.getSimpleName();

	@NonNull
	private final IAudioSampler mSampler;
	private final boolean mPreEncoded;
	private int frame_count = 0;

	/**
	 * コンストラクタ
	 * @param recorder
	 * @param listener
	 * @param sampler
	 */
	@SuppressLint("InlinedApi")
	public AudioSamplerEncoder(
		@NonNull final IRecorder recorder,
		@NonNull final EncoderListener2 listener,
		@Nullable IAudioSampler sampler) {

		super(recorder, listener, sampler.getAudioSource(),
			sampler.getChannels(), sampler.getSamplingFrequency(),
			DEFAULT_BIT_RATE);
//		if (DEBUG) Log.v(TAG, "コンストラクタ:");
		mSampler = sampler;
		mPreEncoded = (sampler.getAudioFormat() == AudioFormat.ENCODING_AAC_LC);
		if (DEBUG) Log.v(TAG, "AudioSamplerEncoder:mPreEncoded=" + mPreEncoded);
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
	 * IAudioSampleからのコールバックリスナー
	 */
	private final EncodedAudioSampler.Callback mSoundSamplerCallback
		= new EncodedAudioSampler.Callback() {

		private final MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
		@Override
		public void onData(@NonNull final ByteBuffer buffer, final long presentationTimeUs) {
    		synchronized (mSync) {
    			// 既に終了しているか終了指示が出てれば何もしない
        		if (!isEncoding() || isRequestStop()) return;
    		}
			if (buffer.remaining() > 0) {
				if (mPreEncoded) {
					// すでにエンコードされている場合は直接書き込む
					bufferInfo.set(0, buffer.remaining(), presentationTimeUs, 0);
					writeSampleData(buffer, bufferInfo);
				} else {
					// エンコードされていない音声データを受け取った時はエンコーダーへ書き込む
					frameAvailableSoon();
					encode(buffer, presentationTimeUs);
				}
				frame_count++;
			}
		}

		@Override
		public void onOutputFormatChanged(@NonNull final MediaFormat format) {
			if (DEBUG) Log.v(TAG, "onOutputFormatChanged:" + format);
			if (mPreEncoded) {
				AudioSamplerEncoder.this.onOutputFormatChanged(format);
			}
		}

		@Override
		public void onError(@NonNull final Throwable t) {
			if (DEBUG) Log.w(TAG, t);
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
            		if (!isEncoding() || isRequestStop()) break;
            		try {
						mSync.wait(300);
					} catch (final InterruptedException e) {
						break;
					}
            	}
			}
			if (frame_count == 0) {
		    	// 1フレームも書き込めなかった時は動画出力時にMediaMuxerがクラッシュしないように
		    	// ダミーデータを書き込む
		    	final ByteBuffer buf = ByteBuffer.allocateDirect(AudioRecordCompat.SAMPLES_PER_FRAME).order(ByteOrder.nativeOrder());
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
							break;
						}
					}
		    	}
			}
		}
	};
}
