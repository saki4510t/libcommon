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

/**
 * 別途H.264/AVCでエンコード済みの映像データを受け取ってバッファリングして引き渡すためのIVideoEncoder実装
 * webカメラがH.264エンコードに対応している場合などにデコード＆再エンコードをしないで直接録画できるようにするため
 */
public class FakeVideoEncoder extends AbstractFakeEncoder
	implements IVideoEncoder {
	
//	private static final boolean DEBUG = false;	// 実働時はfalseにすること
	private static final String TAG = FakeVideoEncoder.class.getSimpleName();

	protected int mWidth, mHeight;
	
	/**
	 * コンストラクタ
	 * H.264/AVC用
	 * @param recorder
	 * @param listener
	 * @param frameSz
	 */
	public FakeVideoEncoder(final IRecorder recorder,
		final EncoderListener2 listener, final int frameSz) {

		this(MediaCodecUtils.MIME_VIDEO_AVC, recorder, listener,
			frameSz, DEFAULT_MAX_POOL_SZ, DEFAULT_MAX_QUEUE_SZ);
	}

	/**
	 * コンストラクタ
	 * H.264/AVC用
	 * @param recorder
	 * @param listener
	 * @param frameSz
	 * @param maxPoolSz
	 * @param maxQueueSz
	 */
	public FakeVideoEncoder(final IRecorder recorder,
		final EncoderListener2 listener, final int frameSz,
		final int maxPoolSz, final int maxQueueSz) {
		
		this(MediaCodecUtils.MIME_VIDEO_AVC, recorder, listener,
			DEFAULT_FRAME_SZ, DEFAULT_MAX_POOL_SZ, DEFAULT_MAX_QUEUE_SZ);
	}

	/**
	 * コンストラクタ
	 * @param mimeType
	 * @param recorder
	 * @param listener
	 * @param defaultFrameSz
	 * @param maxPoolSz
	 * @param maxQueueSz
	 */
	public FakeVideoEncoder(final String mimeType,
		final IRecorder recorder, final EncoderListener2 listener,
		final int defaultFrameSz, final int maxPoolSz, final int maxQueueSz) {
		
		super(mimeType, recorder, listener, defaultFrameSz, maxPoolSz, maxQueueSz);
	}

	/**
	 * Muxer初期化用のMediaFormatを生成する
	 * @param csd
	 * @param size
	 * @param ix0
	 * @param ix1
	 * @param ix2
	 * @return
	 */
	@Override
	protected MediaFormat createOutputFormat(final String mime,
		final byte[] csd, final int size,
		final int ix0, final int ix1, final int ix2) {
		
//		if (DEBUG) Log.v(TAG, "createOutputFormat:");
		final MediaFormat outFormat;
        if (ix0 >= 0) {
            outFormat = MediaFormat.createVideoFormat(mime, mWidth, mHeight);
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
	
	@Override
	public void setVideoSize(final int width, final int height)
		throws IllegalArgumentException, IllegalStateException {
	
//		if (DEBUG) Log.v(TAG, "setVideoSize:");
		mWidth = width;
		mHeight = height;
	}

	@Override
	public int getWidth() {
		return mWidth;
	}
	
	@Override
	public int getHeight() {
		return mHeight;
	}

	@Override
	public void setVideoConfig(final int bitRate, final int frameRate, final int iFrameIntervals) {
		// ignore
	}
}
