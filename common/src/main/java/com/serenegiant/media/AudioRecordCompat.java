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

import com.serenegiant.system.BuildCheck;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;

public class AudioRecordCompat {
   private AudioRecordCompat() {
      // インスタンス化をエラーとするためにデフォルトコンストラクタをprivateに
   }

	public static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

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

	/**
	 * AudioRecorder初期化時に使用するバッファサイズを計算
	 * @param channelNum
	 * @param samplingTate
	 * @param samplesPerFrame
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
  	 * AudioRecord生成用のヘルパーメソッド
  	 * @param source
  	 * @param samplingRate 音声ソース, MediaRecorder.AudioSource.DEFAULT, MIC,
  	 * 			CAMCORDER, VOICE_RECOGNITION, VOICE_COMMUNICATIONまたはAUDIO_SOURCE_UAC
  	 * @param channels 音声チャネル AudioFormat.CHANNEL_IN_MONOかAudioFormat.CHANNEL_IN_STEREOのどちらか
  	 * @param format AudioFormat.ENCODING_PCM_16BIT
  	 * @param bufferSize バッファサイズ
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
}
