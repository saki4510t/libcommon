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

import java.io.IOException;
import java.nio.ByteBuffer;

import android.annotation.TargetApi;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class MediaMuxerWrapper implements IMuxer {

	private final MediaMuxer mMuxer;
	private volatile boolean mIsStarted;

	public MediaMuxerWrapper(final String output_path, final int format) throws IOException {
		mMuxer = new MediaMuxer(output_path, format);
	}

	@Override
	public int addTrack(final MediaFormat format) {
		return mMuxer.addTrack(format);
	}

	@Override
	public void writeSampleData(final int trackIndex, final ByteBuffer byteBuf, final BufferInfo bufferInfo) {
		mMuxer.writeSampleData(trackIndex, byteBuf, bufferInfo);
	}

	@Override
	public void start() {
		mMuxer.start();
		mIsStarted = true;
	}

	@Override
	public void stop() {
		mIsStarted = false;
		mMuxer.stop();
	}

	@Override
	public void release() {
		mIsStarted = false;
		mMuxer.release();
	}

	@Override
	public boolean isStarted() {
		return mIsStarted;
	}

}
