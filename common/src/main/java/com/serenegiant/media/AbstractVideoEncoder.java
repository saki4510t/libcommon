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

import androidx.annotation.NonNull;

/**
 * 映像をMediaCodecでエンコードするためのクラス
 */
public abstract class AbstractVideoEncoder extends AbstractEncoder
	implements IVideoEncoder {

	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = AbstractVideoEncoder.class.getSimpleName();

	protected int mWidth, mHeight;
    protected int mBitRate = -1;
	protected int mFramerate = -1;
    protected int mIFrameIntervals = -1;

    public AbstractVideoEncoder(
    	@NonNull final String mime,
    	@NonNull final IRecorder recorder,
    	@NonNull final EncoderListener2 listener) {

		super(mime, recorder, listener);
    }

	/**
	 * 動画サイズをセット
	 * ビットレートもサイズとVideoConfigの設定値に合わせて変更される
	 * @param width
	 * @param height
	 */
	@Override
	public void setVideoSize(final int width, final int height)
		throws IllegalArgumentException, IllegalStateException {
//    	Log.d(TAG, String.format("setVideoSize(%d,%d)", width, height));
    	mWidth = width;
    	mHeight = height;
		mBitRate = getConfig().getBitrate(width, height);
    }

	@Override
	public void setVideoConfig(final int bitRate, final int frameRate, final int iFrameIntervals) {
		mBitRate = bitRate;
		mFramerate = frameRate;
		mIFrameIntervals = iFrameIntervals;
	}

	@Override
    public int getWidth() {
    	return mWidth;
    }

	@Override
    public int getHeight() {
    	return mHeight;
    }

}
