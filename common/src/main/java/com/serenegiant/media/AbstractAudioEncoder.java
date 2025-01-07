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

import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;

import androidx.annotation.NonNull;

/**
 * 音声データをMediaCodecを使ってAACエンコードするための基本クラス
 */
public abstract class AbstractAudioEncoder extends AbstractEncoder
	implements IAudioEncoder {
	private static final boolean DEBUG = false;	// FIXME 実働時にはfalseにすること
	private static final String TAG = AbstractAudioEncoder.class.getSimpleName();

    protected int mAudioSource;
    protected int mChannelCount;
	protected int mSampleRate;
    protected int mBitRate;

	/**
	 * コンストラクタ
	 * @param recorder
	 * @param listener
	 * @param audioSource
	 * @param audioChannels
	 * @param sampleRate
	 * @param bitRate
	 */
	public AbstractAudioEncoder(
		@NonNull final IRecorder recorder,
		@NonNull final EncoderListener2 listener,
		final int audioSource, final int audioChannels, final int sampleRate, final int bitRate) {

		super(MediaCodecUtils.MIME_AUDIO_AAC, recorder, listener);
//		if (DEBUG) Log.v(TAG, "コンストラクタ:");
		mAudioSource = audioSource;
		mChannelCount = audioChannels;
		mSampleRate = sampleRate;
		mBitRate = bitRate;
	}

	@Override
	protected Encoder internalPrepare(@NonNull final MediaReaper.ReaperListener listener) throws Exception {
//		if (DEBUG) Log.v(TAG, "internalPrepare:");
        mTrackIndex = -1;
		final String mimeType = getMimeType();
// 音声を取り込んでAACにエンコードするためのMediaCodecの準備
        final MediaCodecInfo codecInfo = MediaCodecUtils.selectAudioEncoder(mimeType);
        if (codecInfo == null) {
			throw new IllegalArgumentException("Unable to find an appropriate codec for " + mimeType);
        }
//		if (DEBUG) Log.i(TAG, "selected codec: " + codecInfo.getName());

        final MediaFormat audioFormat = MediaFormat.createAudioFormat(mimeType, mSampleRate, mChannelCount);
		audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
		audioFormat.setInteger(MediaFormat.KEY_CHANNEL_MASK,
			mChannelCount == 1 ? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO);
		audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, mBitRate);
		audioFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, mChannelCount);
//		audioFormat.setLong(MediaFormat.KEY_MAX_INPUT_SIZE, inputFile.length());
//      audioFormat.setLong(MediaFormat.KEY_DURATION, (long)durationInMs );
//		if (DEBUG) Log.i(TAG, "format: " + audioFormat);

		final MediaCodec mediaCodec = MediaCodec.createEncoderByType(mimeType);
		mediaCodec.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
		mediaCodec.start();
        final MediaReaper reaper = new MediaReaper.AudioReaper(mediaCodec, listener, mSampleRate, mChannelCount);
//		if (DEBUG) Log.i(TAG, "internalPrepare:finished");
		return new Encoder(mediaCodec, reaper, false);
	}

}
