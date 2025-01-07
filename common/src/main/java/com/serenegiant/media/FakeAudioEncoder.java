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

import android.media.MediaFormat;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import androidx.annotation.NonNull;

/**
 * すでにAAC LCでエンコード済みの音声データを受け取ってバッファリングして引き渡すためのIVideoEncoder実装
 */
public class FakeAudioEncoder extends AbstractFakeEncoder
	implements IAudioEncoder {

	private final int sampleRate;
	private final int channelCount;

	/**
	 * コンストラクタ
	 * @param recorder
	 * @param listener
	 * @param sampleRate
	 * @param channelCount
	 */
	public FakeAudioEncoder(
		@NonNull final IRecorder recorder,
		@NonNull final EncoderListener2 listener,
		final int sampleRate, final int channelCount) {

		this(recorder, listener, sampleRate, channelCount,
			DEFAULT_FRAME_SZ, DEFAULT_MAX_POOL_SZ, DEFAULT_MAX_QUEUE_SZ);
	}

	/**
	 * コンストラクタ
	 * @param recorder
	 * @param listener
	 * @param sampleRate
	 * @param channelCount
	 * @param frameSz
	 * @param maxPoolSz
	 * @param maxQueueSz
	 */
	public FakeAudioEncoder(
		@NonNull final IRecorder recorder,
		@NonNull final EncoderListener2 listener,
		final int sampleRate, final int channelCount,
		final int frameSz, final int maxPoolSz, final int maxQueueSz) {

		super(MediaCodecUtils.MIME_AUDIO_AAC, recorder, listener, frameSz, maxPoolSz, maxQueueSz);
		this.sampleRate = sampleRate;
		this.channelCount = channelCount;
	}

	@Override
	protected MediaFormat createOutputFormat(final String mime, final byte[] csd, final int size,
		final int ix0, final int ix1, final int ix2) {

//		if (DEBUG) Log.v(TAG, "createOutputFormat:");
		final MediaFormat outFormat;
        if (ix0 >= 0) {
            outFormat = MediaFormat.createAudioFormat(mime, sampleRate, channelCount);
        	final ByteBuffer csd0 = ByteBuffer.allocateDirect(ix1 - ix0)
        		.order(ByteOrder.nativeOrder());
        	csd0.put(csd, ix0, ix1 - ix0);
        	csd0.flip();
            outFormat.setByteBuffer("csd-0", csd0);
//			if (DEBUG) BufferHelper.dump("sps", csd0, 0, csd0 != null ? csd0.capacity() : 0);
            if (ix1 > ix0) {
				final int sz = (ix2 > ix1) ? (ix2 - ix1) : (size - ix1);
            	final ByteBuffer csd1 = ByteBuffer.allocateDirect(sz)
            		.order(ByteOrder.nativeOrder());
            	csd1.put(csd, ix1, sz);
            	csd1.flip();
                outFormat.setByteBuffer("csd-1", csd1);
//				if (DEBUG) BufferHelper.dump("pps", csd1, 0, csd1 != null ? csd1.capacity() : 0);
            }
        } else {
        	throw new RuntimeException("unexpected csd data came.");
        }
//		if (DEBUG) Log.v(TAG, "createOutputFormat:result=" + outFormat);
        return outFormat;
	}
}
