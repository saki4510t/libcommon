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
import java.nio.ByteOrder;

public class AudioData {
//	private static final boolean DEBUG = false;
//	private static final String TAG = "AudioData";

//	private static int count = 0;

	ByteBuffer mBuffer;
	int size;
	long presentationTimeUs;

	public AudioData(final int size) {
		mBuffer = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
//		if (DEBUG) Log.i(TAG, "create new AudioData:" + ++count);
	}

	public void set(final ByteBuffer buffer, final int _size, final long _presentationTimeUs) {
		presentationTimeUs = _presentationTimeUs;
		size = _size;
		if (mBuffer == null || mBuffer.capacity() < _size) {
			mBuffer = ByteBuffer.allocateDirect(_size).order(ByteOrder.nativeOrder());
		}
		mBuffer.clear();
		mBuffer.put(buffer);
		mBuffer.position(_size);
		mBuffer.flip();
	}

	public void clear() {
		size = 0;
		mBuffer.clear();
	}

	public int size() {
		return size;
	}

	public long presentationTimeUs() {
		return presentationTimeUs;
	}

	public void get(final byte[] buffer) {
		if ((buffer == null) || (buffer.length < size)) {
			throw new ArrayIndexOutOfBoundsException("");
		}
		mBuffer.clear();
		mBuffer.get(buffer);
	}

	public void get(final ByteBuffer buffer) {
		if ((buffer == null) || (buffer.remaining() < size)) {
			throw new ArrayIndexOutOfBoundsException("");
		}
		mBuffer.clear();
		buffer.put(mBuffer);
	}
}
