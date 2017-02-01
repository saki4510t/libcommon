package com.serenegiant.media;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2017 saki t_saki@serenegiant.com
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

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

/**
 * AudioRecordを使って音声データを取得し、登録したコールバックへ分配するためのクラス
 * 同じ音声入力ソースに対して複数のAudioRecordを生成するとエラーになるのでシングルトン的にアクセス出来るようにするため
 */
public class AudioSampler extends IAudioSampler {
//	private static final boolean DEBUG = false;
//	private static final String TAG = "AudioSampler";

	private AudioThread mAudioThread;
    private final int AUDIO_SOURCE;
    private final int SAMPLING_RATE, CHANNEL_COUNT;
	private final int SAMPLES_PER_FRAME;
	private final int BUFFER_SIZE;
	private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

	public AudioSampler(final int audio_source, final int channel_num,
		final int sampling_rate, final int samples_per_frame, final int frames_per_buffer) {

//		if (DEBUG) Log.v(TAG, "コンストラクタ:");
		// パラメータを保存
		AUDIO_SOURCE = audio_source;
		CHANNEL_COUNT = channel_num;
		SAMPLING_RATE = sampling_rate;
		SAMPLES_PER_FRAME = samples_per_frame * channel_num;
		BUFFER_SIZE = getAudioBufferSize(channel_num, sampling_rate, samples_per_frame, frames_per_buffer);
	}

	/**
	 * 音声データ１つ当たりのバイト数を返す
	 * @return
	 */
	@Override
	public int getBufferSize() {
		return SAMPLES_PER_FRAME;
	}

	/**
	 * 音声データサンプリング開始
	 * 実際の処理は別スレッド上で実行される
	 */
	@Override
	public synchronized void start() {
//		if (DEBUG) Log.v(TAG, "start:mIsCapturing=" + mIsCapturing);
		super.start();
		if (mAudioThread == null) {
			init_pool(SAMPLES_PER_FRAME);
			// 内蔵マイクからの音声取り込みスレッド生成＆実行
	        mAudioThread = new AudioThread();
			mAudioThread.start();
		}
	}

	/**
	 * 音声データのサンプリングを停止させる
	 */
	@Override
	public synchronized void stop() {
//		if (DEBUG) Log.v(TAG, "stop:mIsCapturing=" + mIsCapturing);
		mIsCapturing = false;
		mAudioThread = null;
		super.stop();
	}

	@Override
	public int getAudioSource() {
		return AUDIO_SOURCE;
	}

	protected static final class AudioRecordRec {
		AudioRecord audioRecord;
		int bufferSize;
	}

	/**
	 * AudioRecorder初期化時に使用するバッファサイズを計算
	 * @param channel_num
	 * @param sampling_rate
	 * @param samples_per_frame
	 * @param frames_per_buffer
	 * @return
	 */
	public static int getAudioBufferSize(final int channel_num,
			final int sampling_rate, final int samples_per_frame, final int frames_per_buffer) {
		final int min_buffer_size = AudioRecord.getMinBufferSize(sampling_rate,
			(channel_num == 1 ? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO),
			AUDIO_FORMAT);
		int buffer_size = samples_per_frame * frames_per_buffer;
		if (buffer_size < min_buffer_size)
			buffer_size = ((min_buffer_size / samples_per_frame) + 1) * samples_per_frame * 2 * channel_num;
		return buffer_size;
	}

	public static AudioRecord createAudioRecord(
		final int source, final int sampling_rate, final int channels, final int format, final int buffer_size) {

		final int[] AUDIO_SOURCES = new int[] {
			MediaRecorder.AudioSource.DEFAULT,		// ここ(1つ目)は引数で置き換えられる
			MediaRecorder.AudioSource.CAMCORDER,	// これにするとUSBオーディオルーティングが有効な場合でも内蔵マイクからの音になる
			MediaRecorder.AudioSource.MIC,
			MediaRecorder.AudioSource.DEFAULT,
			MediaRecorder.AudioSource.VOICE_COMMUNICATION,
			MediaRecorder.AudioSource.VOICE_RECOGNITION,
		};

		switch (source) {
		case 1:	AUDIO_SOURCES[0] = MediaRecorder.AudioSource.MIC; break;		// 自動
		case 2:	AUDIO_SOURCES[0] = MediaRecorder.AudioSource.CAMCORDER; break;	// 内蔵マイク
		default:AUDIO_SOURCES[0] = MediaRecorder.AudioSource.MIC; break;		// 自動(UACのopenに失敗した時など)
		}
		AudioRecord audioRecord = null;
		for (final int src: AUDIO_SOURCES) {
            try {
	            audioRecord = new AudioRecord(src, sampling_rate,
	            	(channels == 1 ? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO),
	            	format, buffer_size);
	            if (audioRecord != null) {
    	            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
    	            	audioRecord.release();
    	            	audioRecord = null;
    	            }
	            }
            } catch (final Exception e) {
            	audioRecord = null;
            }
            if (audioRecord != null)
            	break;
    	}
		return audioRecord;
	}

	/**
	 * AudioRecordから無圧縮PCM16bitで内蔵マイクからの音声データを取得してキューへ書き込むためのスレッド
	 */
    private final class AudioThread extends Thread {

    	public AudioThread() {
    		super("AudioThread");
    	}

    	@Override
    	public final void run() {
//    		if (DEBUG) Log.v(TAG, "AudioThread:start");
    		android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO); // THREAD_PRIORITY_URGENT_AUDIO
//    		if (DEBUG) Log.v(TAG, getName() + " started");
/*			final Class audioSystemClass = Class.forName("android.media.AudioSystem");
			// will disable the headphone
			setDeviceConnectionState.Invoke(audioSystemClass, (Integer)DEVICE_OUT_WIRED_HEADPHONE, (Integer)DEVICE_STATE_UNAVAILABLE, new String(""));
			// will enable the headphone
			setDeviceConnectionState.Invoke(audioSystemClass, (Integer)DEVICE_OUT_WIRED_HEADPHONE, (Integer)DEVICE_STATE_AVAILABLE, new Lang.String(""));
*/
    		final AudioRecord audioRecord = createAudioRecord(
    			AUDIO_SOURCE, SAMPLING_RATE, CHANNEL_COUNT, AUDIO_FORMAT, BUFFER_SIZE);
            int err_count = 0;
            if (audioRecord != null) {
		        try {
		        	if (mIsCapturing) {
//		        		if (DEBUG) Log.v(TAG, "AudioThread:start audio recording");
		                int readBytes;
		                ByteBuffer buffer;
		                audioRecord.startRecording();
		                try {
		                	AudioData data;
		                	for ( ; mIsCapturing ;) {
		                		data = obtain();
		                		if (data != null) {
			                		buffer = data.mBuffer;
			                		buffer.clear();
			                		// 1回に読み込むのはSAMPLES_PER_FRAMEバイト
			                		try {
			                			readBytes = audioRecord.read(buffer, SAMPLES_PER_FRAME);
			                		} catch (final Exception e) {
//			    		        		Log.w(TAG, "AudioRecord#read failed:", e);
			    		        		callOnError(e);
			                			break;
			                		}
					    			if (readBytes == AudioRecord.ERROR_BAD_VALUE) {
//					    				Log.e(TAG, "Read error ERROR_BAD_VALUE");
					    				err_count++;
					    				recycle(data);
					    			} else if (readBytes == AudioRecord.ERROR_INVALID_OPERATION) {
//					    				Log.e(TAG, "Read error ERROR_INVALID_OPERATION");
					    				err_count++;
					    				recycle(data);
			                		} else if (readBytes > 0) {
			                			err_count = 0;
			                			data.presentationTimeUs = getInputPTSUs();
			                			data.size = readBytes;
			                			buffer.position(readBytes);
			                			buffer.flip();
			                			// 音声データキューに追加する
			                			addAudioData(data);
					    			}
		                		}
				    			if (err_count > 10) break;
		                	}
		                } finally {
		                	audioRecord.stop();
		                }
		        	}
		        } catch (final Exception e) {
//	        		Log.w(TAG, "exception on AudioRecord:", e);
	        		callOnError(e);
		        } finally {
		        	audioRecord.release();
		        }
            } else {
//        		Log.w(TAG, "AudioRecord failed to initialize");
        		callOnError(new RuntimeException("AudioRecord failed to initialize"));
            }
        	AudioSampler.this.stop();
//    		if (DEBUG) Log.v(TAG, "AudioThread:finished");
    	} // #run
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

}
