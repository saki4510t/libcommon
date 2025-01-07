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
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import com.serenegiant.system.BuildCheck;
import com.serenegiant.system.Time;
import com.serenegiant.utils.ThreadUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;

public class AudioRecordCompat {
	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = AudioRecordCompat.class.getSimpleName();

	private AudioRecordCompat() {
		// インスタンス化をエラーとするためにデフォルトコンストラクタをprivateに
	}

	/**
	 * デフォルトの音声フォーマット
	 */
	@AudioFormats
	public static final int DEFAULT_AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
	/**
	 * デフォルトのサンプリングレート
	 */
	public static final int DEFAULT_SAMPLE_RATE = 44100;	// 44.1[KHz]	8-48[kHz] 全機種で保証されているのは44100だけ
	/**
	 * 音声データ1フレームあたりのサンプリング数
	 * この値に1サンプルあたりのバイト数(PCM_16BITなら2、PCM_8BITなら1)を掛けて
	 * チャネル数を掛けると1フレームあたりのデータバイト数になる
	 */
    public static final int SAMPLES_PER_FRAME = 1024;		// AAC, bytes/frame/channel
	/**
	 * バッファ辺りのフレーム数
	 */
	public static final int FRAMES_PER_BUFFER = 25; 		// AAC, frame/buffer/sec

	public static final int AUDIO_SOURCE_UAC = 100;
  	@IntDef({
  		MediaRecorder.AudioSource.DEFAULT,
  		MediaRecorder.AudioSource.MIC,
  		MediaRecorder.AudioSource.CAMCORDER,
  		MediaRecorder.AudioSource.VOICE_RECOGNITION,
  		MediaRecorder.AudioSource.VOICE_COMMUNICATION,
  		AUDIO_SOURCE_UAC,
  	})
  	@Retention(RetentionPolicy.SOURCE)
  	public @interface AudioSource {}

  	@IntDef({
		AudioFormat.CHANNEL_IN_MONO,
		AudioFormat.CHANNEL_IN_STEREO,
  	})
  	@Retention(RetentionPolicy.SOURCE)
  	public @interface AudioChannel {}

	@SuppressLint("InlinedApi")
	@IntDef({
		AudioFormat.ENCODING_PCM_16BIT,
		AudioFormat.ENCODING_PCM_8BIT,
		AudioFormat.ENCODING_PCM_FLOAT,
		AudioFormat.ENCODING_AAC_LC,
	})
	@Retention(RetentionPolicy.SOURCE)
	public @interface AudioFormats {}

	/**
	 * AudioRecorder初期化時に使用するバッファサイズを計算
	 * @param channelNum
	 * @param samplingRate
	 * @return
	 */
	public static int getAudioBufferSize(
		final int channelNum, final int format, final int samplingRate) {

		return getAudioBufferSize(channelNum, format, samplingRate,
			SAMPLES_PER_FRAME, FRAMES_PER_BUFFER);
	}

	/**
	 * AudioRecorder初期化時に使用するバッファサイズを計算
	 * @param channelNum
	 * @param samplingTate
	 * @param samplesPerFrame 1つの音声データ(1フレーム)辺りのサンプリング数
	 * @param framesPerBuffer
	 * @return
	 */
	public static int getAudioBufferSize(final int channelNum, final int format,
			final int samplingTate, final int samplesPerFrame, final int framesPerBuffer) {
		final int minBufferSize = AudioRecord.getMinBufferSize(samplingTate,
			(channelNum == 1 ? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO),
			format);
		int bufferSize = samplesPerFrame * framesPerBuffer;
		if (bufferSize < minBufferSize)
			bufferSize = ((minBufferSize / samplesPerFrame) + 1) * samplesPerFrame * 2 * channelNum;
		return bufferSize;
	}

	/**
	 * チャネル数またはAudioFormat.CHANNEL_IN_MONO/CHANNEL_IN_STEREOを
	 * CHANNEL_IN_MONO/CHANNEL_IN_STEREOに変換する
	 * @param channels 1, 2, CHANNEL_IN_MONO, CHANNEL_IN_STEREOのいずれか
	 * @return 1またはCHANNEL_IN_MONOであればCHANNEL_IN_MONO,
	 * 			2またはCHANNEL_IN_STEREOであればCHANNEL_IN_STEREO,
	 * 			それ以外はCHANNEL_IN_STEREO
	 */
	@AudioChannel
	public static int getAudioChannel(final int channels) {
		@AudioRecordCompat.AudioChannel
		final int audioChannel = switch (channels) {
			case AudioFormat.CHANNEL_IN_MONO -> AudioFormat.CHANNEL_IN_MONO;
			case AudioFormat.CHANNEL_IN_STEREO -> AudioFormat.CHANNEL_IN_STEREO;
			default ->
				(channels == 1 ? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO);
		};
		return audioChannel;
	}

	/**
	 * 1サンプリングあたりのバイト数を取得する
	 * 未知の音声フォーマットに対しては1を返す
	 * @param format ENCODING_PCM_16BIT, ENCODING_PCM_8BIT, ENCODING_PCM_FLOAT
	 * @return
	 */
	public static int getBitResolution(@AudioFormats final int format) {
		switch (format) {
		case AudioFormat.ENCODING_PCM_16BIT:
			return 2;
		case AudioFormat.ENCODING_PCM_FLOAT:
			return 4;
		case AudioFormat.ENCODING_PCM_8BIT:
		case AudioFormat.ENCODING_AAC_LC:
		default:
			return 1;
		}
	}

   /**
  	 * AudioRecord生成用のヘルパーメソッド
  	 * @param source 音声ソース, MediaRecorder.AudioSource.DEFAULT, MIC,
  	 * 					CAMCORDER, VOICE_RECOGNITION, VOICE_COMMUNICATIONまたはAUDIO_SOURCE_UAC
  	 * @param samplingRate サンプリングレート
  	 * @param channels 音声チャネル AudioFormat.CHANNEL_IN_MONOかAudioFormat.CHANNEL_IN_STEREOのどちらか
  	 * @param format AudioFormat.ENCODING_PCM_16BIT
  	 * @param bufferSize バッファサイズ 基本はgetAudioBufferSizeで取得した値を使うこと
  	 * @return
  	 * @throws UnsupportedOperationException
  	 */
  	@SuppressLint("NewApi")
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
  	@NonNull
  	public static AudioRecord newInstance(
  		@AudioSource final int source,
  		final int samplingRate,
  		@AudioChannel final int channels,
  		final int format, final int bufferSize) throws UnsupportedOperationException {

  		final AudioRecord audioRecord;
  		if (BuildCheck.isAndroid6()) {
  			audioRecord = new AudioRecord.Builder()
  				.setAudioSource(source)
  				.setAudioFormat(new AudioFormat.Builder()
  					.setEncoding(format)
  					.setSampleRate(samplingRate)
  					.setChannelMask(channels)
  					.build())
  				.setBufferSizeInBytes(bufferSize)
  				.build();
  		} else {
  			audioRecord = new AudioRecord(source, samplingRate,
  				channels, format, bufferSize);
  		}
  		if (audioRecord == null) {
  			throw new UnsupportedOperationException ("Failed to create AudioRecord");
  		}
  		if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
  			audioRecord.release();
  			throw new UnsupportedOperationException("Failed to initialize AudioRecord");
  		}
  		return audioRecord;
  	}

	/**
	 * AudioRecordを生成する
	 * sourceで指定した映像ソースに対応するMediaRecorder.AudioSourceを選択するが
	 * 使用できないときは自動的に他のMediaRecorder.AudioSourceを選択する
	 * @param source
	 * @param samplingRate
	 * @param channels
	 * @param format
	 * @param bufferSize バッファサイズ 基本はgetAudioBufferSizeで取得した値を使うこと
	 * @return
	 */
	@RequiresPermission(Manifest.permission.RECORD_AUDIO)
	@Nullable
	public static AudioRecord createAudioRecord(
		final int source, final int samplingRate, final int channels, final int format, final int bufferSize) {

		@AudioSource
		final int[] AUDIO_SOURCES = new int[] {
			MediaRecorder.AudioSource.DEFAULT,		// ここ(1つ目)は引数で置き換えられる
			MediaRecorder.AudioSource.CAMCORDER,	// これにするとUSBオーディオルーティングが有効な場合でも内蔵マイクからの音になる
			MediaRecorder.AudioSource.MIC,
			MediaRecorder.AudioSource.DEFAULT,
			MediaRecorder.AudioSource.VOICE_COMMUNICATION,
			MediaRecorder.AudioSource.VOICE_RECOGNITION,
		};

		switch (source) {
		case 2 -> AUDIO_SOURCES[0] = MediaRecorder.AudioSource.CAMCORDER;	// 内蔵マイク
		case 3 -> AUDIO_SOURCES[0] = MediaRecorder.AudioSource.VOICE_COMMUNICATION;
		case 4 -> AUDIO_SOURCES[0] = MediaRecorder.AudioSource.MIC;
		default -> AUDIO_SOURCES[0] = source;
		}

		@AudioChannel
		final int audioChannel = getAudioChannel(channels);
		AudioRecord audioRecord = null;
		for (final int src: AUDIO_SOURCES) {
            try {
            	audioRecord = newInstance(src, samplingRate, audioChannel, format, bufferSize);
            } catch (final Exception e) {
            	audioRecord = null;
            }
            if (audioRecord != null) {
            	if (DEBUG) Log.v(IAudioSampler.class.getSimpleName(), "createAudioRecord:created, src=" + src);
            	break;
			}
    	}
		return audioRecord;
	}

	/**
	 * AudioRecordから無圧縮PCM16bitで内蔵マイクからの音声データを取得してキューへ書き込むためのスレッドの実行部
	 */
	public static abstract class AudioRecordTask implements Runnable {
		@NonNull
		private final Object mSync = new Object();
		@AudioFormats
		private final int mAudioSource;
		private final int mChannelCount;
  		private final int mSamplingRate;
		private final boolean mGenDummyFrameIfNoData;
  		private final boolean mForceSource;
		/**
		 * AudioRecordから1度に読み込みを試みるサンプル数(バイト数とは限らない)
		 * 1フレーム1チャネルあたりのサンプリング数
		 */
		private final int mSamplesPerFrame;
		/**
		 * AudioRecordから1度に読み込みを試みるバイト数
		 * SAMPLES_PER_FRAME x 1サンプル辺りのバイト数
		 */
		private final int mBytesPerFrame;
		/**
		 * AudioRecordで使う内部バッファサイズ
		 */
  		private final int mBufferSize;

		/**
		 * 実行中かどうか
		 */
		private volatile boolean mIsRunning;
		/**
		 * 前回MediaCodecへのエンコード時に使ったpresentationTimeUs
		 */
		private long prevInputPTSUs = -1;
		/**
		 * 音声セッションID
		 * onStart - onStop間でのみ有効
		 * 無効なときは0
		 */
		private volatile int mAudioSessionId;

		/**
		 * コンストラクタ
		 * @param audioSource 音声ソース, MediaRecorder.AudioSourceのどれか
		 *     ただし、一般アプリで利用できないVOICE_UPLINK(2)はCAMCORDER(5)へ
		 *     VOICE_DOWNLINK(3)はVOICE_COMMUNICATION(7)
		 *     VOICE_CALL(4)はMIC(1)へ置換する
		 * @param channelCount 音声チャネル数, 1 or 2
		 * @param samplingRate サンプリングレート
		 * @param samplesPerFrame 1フレーム辺りのサンプル数
		 * @param framesPerBuffer バッファ辺りのフレーム数
		 */
		public AudioRecordTask(
			@AudioFormats final int audioSource, final int channelCount, final int samplingRate,
			final int samplesPerFrame, final int framesPerBuffer) {
			this(audioSource, channelCount, samplingRate,
				samplesPerFrame, framesPerBuffer,
				false, false);
		}

		/**
		 * コンストラクタ
		 * @param audioSource 音声ソース, MediaRecorder.AudioSourceのどれか
		 *     ただし、一般アプリで利用できないVOICE_UPLINK(2)はCAMCORDER(5)へ
		 *     VOICE_DOWNLINK(3)はVOICE_COMMUNICATION(7)
		 *     VOICE_CALL(4)はMIC(1)へ置換する
		 * @param channelCount 音声チャネル数, 1 or 2
		 * @param samplingRate サンプリングレート
		 * @param samplesPerFrame 1フレーム辺りのサンプル数
		 * @param framesPerBuffer バッファ辺りのフレーム数
		 * @param forceSource 音声ソースを強制するかどうか, falseなら利用可能な音声ソースを順に試す
		 * @param genDummyFrameIfNoData 音声データを全く取得できなかったときにダミーデータを生成するかどうか
		 */
		public AudioRecordTask(
			@AudioFormats final int audioSource, final int channelCount, final int samplingRate,
			final int samplesPerFrame, final int framesPerBuffer,
			final boolean forceSource,
			final boolean genDummyFrameIfNoData) {
			mAudioSource = audioSource;
			mChannelCount = channelCount;
			mSamplingRate = samplingRate;
			mForceSource = forceSource;
			mGenDummyFrameIfNoData = genDummyFrameIfNoData;
			mSamplesPerFrame = samplesPerFrame;
			mBytesPerFrame = samplesPerFrame * channelCount
				* AudioRecordCompat.getBitResolution(AudioRecordCompat.DEFAULT_AUDIO_FORMAT);
			mBufferSize = getAudioBufferSize(
				channelCount, DEFAULT_AUDIO_FORMAT,
				samplingRate, samplesPerFrame, framesPerBuffer);
		}

		public int getAudioSource() {
			return mAudioSource;
		}

		@AudioFormats
		public int getAudioFormat() {
			return DEFAULT_AUDIO_FORMAT;
		}

		public int getChannels() {
			return mChannelCount;
		}

		public int getSamplingFrequency() {
			return mSamplingRate;
		}

		/**
		 * 音声データ１つ辺りのサンプリング数を返す
		 * 1フレーム辺りのサンプリング数xチャネル数
		 * @return
		 */
		public int getSampledPerFrame() {
			return mSamplesPerFrame * mChannelCount;
		}

		/**
		 * 音声データ１つ当たりのバイト数を返す
		 * 1フレーム辺りのサンプリング数xチャネル数x1サンプルあたりのバイト数
		 * (AudioRecordから1度に読み込みを試みる最大バイト数)
		 * @return
		 */
		public int getBufferSize() {
			return mBytesPerFrame;
		}

		public int getBitResolution() {
			return 16;	// AudioFormat.ENCODING_PCM_16BIT
		}

		/**
		 * 音声取得終了要求
		 */
		public void requestStop() {
			mIsRunning = false;
		}

		public boolean isRunning() {
			return mIsRunning;
		}

		/**
		 * 音声セッションIDを取得する
		 * onStart〜onStop間でのみ有効
		 * @return 0: 無効な値
		 */
		public int getAudioSessionId() {
			return mAudioSessionId;
		}

		@SuppressLint("MissingPermission")
		@Override
		public void run() {
    		if (DEBUG) Log.v(TAG, "AudioTask:start");
			mIsRunning = true;
    		android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO); // THREAD_PRIORITY_URGENT_AUDIO
			int retry = 3;
			int numFrames = 0;
RETRY_LOOP:	while (isRunning() && (retry > 0)) {
				@AudioChannel
				final int audioChannel = getAudioChannel(mChannelCount);
				AudioRecord audioRecord;
				if (mForceSource) {
					try {
						audioRecord = newInstance(
							mAudioSource, mSamplingRate, audioChannel, DEFAULT_AUDIO_FORMAT, mBufferSize);
					} catch (final Exception e) {
						Log.d(TAG, "AudioTask:", e);
						audioRecord = null;
					}
				} else {
					audioRecord = createAudioRecord(
						mAudioSource, mSamplingRate, audioChannel, DEFAULT_AUDIO_FORMAT, mBufferSize);
				}
				int errCount = 0;
				if (audioRecord != null) {
					mAudioSessionId = audioRecord.getAudioSessionId();
					onStart();
					try {
						if (isRunning()) {
		        			if (DEBUG) Log.v(TAG, "AudioTask:start audio recording");
							int readBytes;
							ByteBuffer buffer;
							audioRecord.startRecording();
							try {
								MediaData data;
LOOP:							while (isRunning()) {
									data = obtain(mBufferSize);
									if (data != null) {
										// check recording state
										final int recordingState = audioRecord.getRecordingState();
										if (recordingState != AudioRecord.RECORDSTATE_RECORDING) {
											if (errCount == 0) {
												Log.e(TAG, "not a recording state," + recordingState);
											}
											errCount++;
											recycle(data);
											if (errCount > 20) {
												retry--;
												break LOOP;
											} else {
												synchronized (mSync) {
													mSync.wait(100);
												}
												continue;
											}
										}
										// try to read audio data
										buffer = data.get();
										buffer.clear();
										// 1回に読み込むのはBYTES_PER_FRAMEバイト
										try {
											readBytes = audioRecord.read(buffer, mBytesPerFrame);
										} catch (final Exception e) {
											Log.e(TAG, "AudioRecord#read failed:" + e);
											errCount++;
											retry--;
											recycle(data);
											onError(e);
											break LOOP;
										}
										if (readBytes > 0) {
											// 正常に読み込めた時
											errCount = 0;
											numFrames++;
											data.presentationTimeUs(getInputPTSUs())
												.size(readBytes);
											buffer.position(readBytes);
											buffer.flip();
											// 音声データキューに追加する
											queueData(data);
											continue;
										} else if (readBytes == AudioRecord.SUCCESS) {	// == 0
											errCount = 0;
											recycle(data);
											continue;
										} else if (readBytes == AudioRecord.ERROR) {
											if (errCount == 0) {
												Log.e(TAG, "Read error ERROR");
											}
										} else if (readBytes == AudioRecord.ERROR_BAD_VALUE) {
											if (errCount == 0) {
												Log.e(TAG, "Read error ERROR_BAD_VALUE");
											}
										} else if (readBytes == AudioRecord.ERROR_INVALID_OPERATION) {
											if (errCount == 0) {
												Log.e(TAG, "Read error ERROR_INVALID_OPERATION");
											}
										} else if (readBytes == AudioRecord.ERROR_DEAD_OBJECT) {
											Log.e(TAG, "Read error ERROR_DEAD_OBJECT");
											errCount++;
											retry--;
											recycle(data);
											break LOOP;
										} else if (readBytes < 0) {
											if (errCount == 0) {
												Log.e(TAG, "Read returned unknown err " + readBytes);
											}
										}
										errCount++;
										recycle(data);
									} // end of if (data != null)
									if (errCount > 10) {
										retry--;
										break LOOP;
									}
								} // end of while (isRunning())
								if (DEBUG) Log.v(TAG, "AudioTask:stop audio recording");
							} finally {
								audioRecord.stop();
							}
						}	// if (isRunning())
						mAudioSessionId = 0;
					} catch (final Exception e) {
						retry--;
						onError(e);
					} finally {
						audioRecord.release();
					}
					onStop();
					if (isRunning() && (errCount > 0) && (retry > 0)) {
						// キャプチャリング中でエラーからのリカバリー処理が必要なときは0.5秒待機
						for (int i = 0; isRunning() && (i < 5); i++) {
							synchronized (mSync) {
								try {
									mSync.wait(100);
								} catch (final InterruptedException e) {
									break RETRY_LOOP;
								}
							}
						}
					}
				} else {
					onError(new RuntimeException("AudioRecord failed to initialize"));
					retry = 0;	// 初期化できんかったときはリトライしない
				}
			}	// end of for
			if (mGenDummyFrameIfNoData && (numFrames == 0)) {
				// 1フレームも書き込めなかった時は動画出力時にMediaMuxerがクラッシュしないように
				// ダミー音声データの書き込みを試みる
				final ByteBuffer buf = ByteBuffer.allocateDirect(mBufferSize).order(ByteOrder.nativeOrder());
				for (int i = 0; i < 5; i++) {
					final MediaData data = obtain(mBufferSize);
					if (data != null) {
						buf.clear();
						buf.position(mBufferSize);
						buf.flip();
						data.set(buf, mBufferSize, getInputPTSUs());
						ThreadUtils.NoThrowSleep(40);
					}
				}
			}
			onStop();
    		if (DEBUG) Log.v(TAG, "AudioTask:finished");
    	} // #run

		/**
		 * 音声データを読み込むためのMediaDataオブジェクトを取得
		 * @return
		 */
		@Nullable
		protected abstract MediaData obtain(final int bufferBytes);

		/**
		 * 取得した音声データをキューに入れる処理
		 * @param data
		 */
		protected abstract void queueData(@NonNull final MediaData data);

		/**
		 * #obtainで取得したMediaData(またはその継承オブジェクト)へ
		 * 音声データを取得できなかったときに再利用するための処理
		 * dataがRecycleMediaDataであればRecycleMediaData#recycleを呼び出す
		 * @param data
		 */
		protected void recycle(@NonNull final MediaData data) {
			if (data instanceof RecycleMediaData) {
				((RecycleMediaData) data).recycle();
			}
		}

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
		 * 音声サンプリング開始
		 * 複数回呼ばれる可能性がある
		 */
		protected void onStart() {}

		/**
		 * 音声サンプリング終了
		 * 複数回呼ばれる可能性がある
		 */
		protected void onStop() {}

		/**
		 * 音声サンプリングでエラー発生
		 * @param t
		 */
		protected abstract void onError(@NonNull final Throwable t);

	}
}
