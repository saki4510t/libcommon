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
import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import com.serenegiant.utils.ThreadPool;

import java.nio.ByteBuffer;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;

/**
 * AudioRecordを使って音声データを取得しMediaCodecでAACにエンコードしたデータを
 * 登録したコールバックへ分配するためのIAudioSampler実装
 * AudioSamplerのAACエンコード済みバージョン
 */
public class EncodedAudioSampler extends IAudioSampler {
	private static final boolean DEBUG = false; // set false on production
	private static final String TAG = EncodedAudioSampler.class.getSimpleName();

	/**
	 * MediaFormatが変更されたときのコールバックを追加したSoundSamplerCallback
	 */
	public interface Callback extends AudioSamplerCallback {
		public void onOutputFormatChanged(@NonNull final MediaFormat format);
	}

	@NonNull
	private final Object mSync = new Object();
    private final int AUDIO_SOURCE;
    private final int SAMPLING_RATE, CHANNEL_COUNT;
	/**
	 * AudioRecordから1度に読み込みを試みるサンプル数(バイト数とは限らない)
	 * 1フレーム1チャネルあたりのサンプリング数
	 */
	private final int SAMPLES_PER_FRAME;
	/**
	 * AudioRecordから1度に読み込みを試みるバイト数
	 * SAMPLES_PER_FRAME x 1サンプル辺りのバイト数
	 */
	private final int BYTES_PER_FRAME;
	private final boolean FORCE_SOURCE;

	@Nullable
	private AudioRecordCompat.AudioRecordTask mAudioTask;
	@Nullable
	private MediaEncoder.MediaAudioEncoder mEncoder;
	@NonNull
	private MediaFormat mMediaFormat = new MediaFormat();

	/**
	 * コンストラクタ
	 * @param audioSource 音声ソース, MediaRecorder.AudioSourceのどれか
	 *     ただし、一般アプリで利用できないVOICE_UPLINK(2)はCAMCORDER(5)へ
	 *     VOICE_DOWNLINK(3)はVOICE_COMMUNICATION(7)
	 *     VOICE_CALL(4)はMIC(1)へ置換する
	 * @param channelNum
	 * @param samplingRate
	 */
	public EncodedAudioSampler(final int audioSource,
		final int channelNum, final int samplingRate) {

		this(audioSource, channelNum, samplingRate,
			AudioRecordCompat.SAMPLES_PER_FRAME, AudioRecordCompat.FRAMES_PER_BUFFER,
			false);
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
	public EncodedAudioSampler(final int audioSource, final int channelNum,
		final int samplingRate, final int samplesPerFrame, final int framesPerBuffer) {

		this(audioSource, channelNum, samplingRate,
			samplesPerFrame, framesPerBuffer, false);
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
	public EncodedAudioSampler(final int audioSource, final int channelNum,
		final int samplingRate, final int samplesPerFrame, final int framesPerBuffer,
		final boolean forceSource) {

		super();
		if (DEBUG) Log.v(TAG, "コンストラクタ:");
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

	@SuppressLint("InlinedApi")
	@AudioRecordCompat.AudioFormats
	@Override
	public int getAudioFormat() {
		return AudioFormat.ENCODING_AAC_LC;
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
		synchronized (mSync) {
			return mAudioTask != null ? mAudioTask.getAudioSessionId() : 0;
		}
	}

	@NonNull
	public MediaFormat getMediaFormat() {
		return mMediaFormat;
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
		synchronized (mSync) {
			if (mAudioTask == null) {
				init_pool(BYTES_PER_FRAME);
				try {
					// エンコーダーを生成
					mEncoder = createEncoder();
					mEncoder.prepare();
					mEncoder.start();
					// 内蔵マイクからの音声取り込みスレッド生成＆実行
					mAudioTask = new AudioRecordCompat.AudioRecordTask(
						AUDIO_SOURCE, CHANNEL_COUNT, SAMPLING_RATE,
						SAMPLES_PER_FRAME, BYTES_PER_FRAME,
						FORCE_SOURCE, false) {

						/**
						 * 音声データ受け取り用のMediaDataオブジェクト
						 * こっちはすぐにエンコーダーへ入れてしまうのでバッファリング不要
						 */
						@NonNull
						private final MediaData data = new MediaData();

						@Override
						protected void onStart() {
							super.onStart();
							data.resize(getBufferSize());
						}

						@Override
						public boolean isRunning() {
							return super.isRunning() && isStarted();
						}

						@Nullable
						@Override
						protected MediaData obtain(final int bufferBytes) {
							return data;
						}

						@Override
						protected void queueData(@NonNull final MediaData data) {
							// キューへ入れる代わりにエンコーダーへ入れる
							final MediaEncoder encoder;
							synchronized (mSync) {
								encoder = mEncoder;
							}
							if (encoder != null) {
								try {
									encoder.encode(data.get(), data.presentationTimeUs());
								} catch (final Exception e) {
									callOnError(e);
								}
							}
						}

						@Override
						protected void onError(@NonNull final Throwable t) {
							callOnError(t);
						}
					};
					new Thread(mAudioTask, "AudioThread").start();
				} catch (Exception e) {
					stop();
				}
			}
		}
	}

	/**
	 * 音声データのサンプリングを停止させる
	 */
	@Override
	public synchronized void stop() {
		if (DEBUG) Log.v(TAG, "stop:isStarted=" + isStarted());
		setIsCapturing(false);
		synchronized (mSync) {
			if (mEncoder != null) {
				mEncoder.stop();
				mEncoder.release();
				mEncoder = null;
			}
			mAudioTask = null;
			mSync.notify();
		}
		super.stop();
	}

	/**
	 * AudioRecordから無圧縮PCM16bitで内蔵マイクからの音声データを取得してキューへ書き込むためのスレッド
	 */
    private final class AudioThread extends Thread {
    	public AudioThread() {
    		super(new AudioRecordCompat.AudioRecordTask(
    			AUDIO_SOURCE, CHANNEL_COUNT, SAMPLING_RATE,
    			SAMPLES_PER_FRAME, BYTES_PER_FRAME,
    			FORCE_SOURCE, false) {

				/**
				 * 音声データ受け取り用のMediaDataオブジェクト
				 * こっちはすぐにエンコーダーへ入れてしまうのでバッファリング不要
				 */
				@NonNull
				private final MediaData data = new MediaData();

				@Override
				protected void onStart() {
					super.onStart();
					data.resize(getBufferSize());
				}

				@Override
				public boolean isRunning() {
					return super.isRunning() && isStarted();
				}

				@Nullable
				@Override
				protected MediaData obtain(final int bufferBytes) {
					return data;
				}

				@Override
				protected void queueData(@NonNull final MediaData data) {
					// キューへ入れる代わりにエンコーダーへ入れる
					final MediaEncoder encoder;
					synchronized (mSync) {
						encoder = mEncoder;
					}
					if (encoder != null) {
						try {
							encoder.encode(data.get(), data.presentationTimeUs());
						} catch (final Exception e) {
							callOnError(e);
						}
					}
				}

				@Override
				protected void onError(@NonNull final Throwable t) {
					callOnError(t);
				}
			}, "AudioThread");
    	}
    }

	/**
	 * コールバックを追加する
	 * @param callback
	 */
	public void addCallback(final AudioSamplerCallback callback) {
		super.addCallback(callback);
		if (DEBUG) Log.v(TAG, "addCallback:");
		if (callback instanceof Callback) {
			final MediaFormat format = getMediaFormat();
			// callbackがonOutputFormatChangedを含んでいてすでにMediaFormatを
			// 取得している時はすぐに#onOutputFormatChangedを呼び出す
			if (format.containsKey(MediaFormat.KEY_MIME)) {
				ThreadPool.queueEvent(() -> {
					((Callback) callback).onOutputFormatChanged(format);
				});
			}
		}
	}

	/**
	 * MediaFormatをクリアする
	 */
	private void clearMediaFormat() {
		if (DEBUG) Log.v(TAG, "clearMediaFormat:");
		synchronized (mSync) {
			mMediaFormat = new MediaFormat();
		}
	}

	/**
	 * 登録している全てのコールバックをスキャンしてCallbackかその継承クラスであれば
	 * #onOutputFormatChangedを呼び出す
	 * @param format
	 */
	private void callOnMediaFormatChanged(@NonNull MediaFormat format) {
		if (DEBUG) Log.v(TAG, "callOnMediaFormatChanged:" + format);
		ThreadPool.queueEvent(() -> {
			final Set<AudioSamplerCallback> callbacks = getCallbacks();
			for (final AudioSamplerCallback callback: callbacks) {
				if (callback instanceof Callback) {
					((Callback) callback).onOutputFormatChanged(format);
				}
			}
		});
	}

	/**
	 * 音声エンコーダーを生成
	 * @return
	 */
	@NonNull
	private MediaEncoder.MediaAudioEncoder createEncoder() {
		if (DEBUG) Log.v(TAG, "createEncoder:");
		return new MediaEncoder.MediaAudioEncoder(
			MediaCodecUtils.MIME_AUDIO_AAC, mEncodeListener,
			getChannels(), getSamplingFrequency(),
			IAudioEncoder.DEFAULT_BIT_RATE) {
			@NonNull
			@Override
			protected MediaReaper.ReaperListener getReaperListener() {
				if (DEBUG) Log.v(TAG, "getReaperListener:");
				return mReaperListener;
			}
		};
	}

	/**
	 * エンコーダーからのコールバックリスナー実装
	 */
	private final EncoderListener2 mEncodeListener
		= new EncoderListener2() {
		@Override
		public void onStartEncode(
			@NonNull final Encoder encoder,
			@Nullable final Surface source, final boolean mayFail) {
			if (DEBUG) Log.v(TAG, "onStartEncode:");
		}

		@Override
		public void onStopEncode(@NonNull final Encoder encoder) {
			if (DEBUG) Log.v(TAG, "onStopEncode:");
		}

		@Override
		public void onDestroy(@NonNull final Encoder encoder) {
			if (DEBUG) Log.v(TAG, "onDestroy:");
		}

		@Override
		public void onError(@NonNull final Throwable t) {
			if (DEBUG) Log.v(TAG, "onError:");
			clearMediaFormat();
			setIsCapturing(false);
			callOnError(t);
		}
	};

	/**
	 * エンコーダーのMediaReaperからのコールバックリスナー実装
	 */
	private final MediaReaper.ReaperListener mReaperListener
		= new MediaReaper.ReaperListener() {
		@Override
		public void writeSampleData(
			@NonNull final MediaReaper reaper,
			@NonNull final ByteBuffer buffer,
			@NonNull final MediaCodec.BufferInfo bufferInfo) {

			final RecycleMediaData data = obtain(bufferInfo.size);
			if (data != null) {
				data.set(buffer, bufferInfo.size, bufferInfo.presentationTimeUs);
				// エンコード済み音声データをキューに追加する
				addMediaData(data);
			}
		}

		@Override
		public void onOutputFormatChanged(
			@NonNull final MediaReaper reaper, @NonNull final MediaFormat format) {
			if (DEBUG) Log.v(TAG, "onOutputFormatChanged:");
			synchronized (mSync) {
				mMediaFormat = MediaCodecUtils.duplicate(format);
			}
			callOnMediaFormatChanged(format);
		}

		@Override
		public void onStop(@NonNull final MediaReaper reaper) {
			if (DEBUG) Log.v(TAG, "onStop:");
			clearMediaFormat();
			setIsCapturing(false);
			final MediaEncoder encoder = mEncoder;
			if (encoder != null) {
				encoder.requestStop();
			}
		}

		@Override
		public void onError(@NonNull final MediaReaper reaper, final Throwable t) {
			if (DEBUG) Log.w(TAG, t);
			clearMediaFormat();
			setIsCapturing(false);
			final MediaEncoder encoder = mEncoder;
			if (encoder != null) {
				encoder.requestStop();
			}
		}
	};
}
