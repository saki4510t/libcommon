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

import android.Manifest;
import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import com.serenegiant.system.BuildCheck;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

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
	public static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
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
		final int audioChannel;
		switch (channels) {
		case AudioFormat.CHANNEL_IN_MONO:
			audioChannel = AudioFormat.CHANNEL_IN_MONO;
			break;
		case AudioFormat.CHANNEL_IN_STEREO:
			audioChannel = AudioFormat.CHANNEL_IN_STEREO;
			break;
		default:
			audioChannel = (channels == 1 ? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO);
			break;
		}
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
		case 2:	AUDIO_SOURCES[0] = MediaRecorder.AudioSource.CAMCORDER; break;	// 内蔵マイク
		case 3: AUDIO_SOURCES[0] = MediaRecorder.AudioSource.VOICE_COMMUNICATION; break;
		case 4: AUDIO_SOURCES[0] = MediaRecorder.AudioSource.MIC; break;
		default: AUDIO_SOURCES[0] = source; break;
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
}
