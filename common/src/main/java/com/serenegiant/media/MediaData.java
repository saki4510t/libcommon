package com.serenegiant.media;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2018 saki t_saki@serenegiant.com
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

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.os.Build;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MediaData {

	ByteBuffer mBuffer;
	int flags;
	int size;
	long presentationTimeUs;
	
	/**
	 * コンストラクタ
	 */
	public MediaData() {
	}

	/**
	 * コンストラクタ
	 * @param size データ保持用の内部バッファのデフォルトサイズ
	 */
	public MediaData(@IntRange(from=1)final int size) {
		mBuffer = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
	}
	
	/**
	 * データをセット
	 * @param buffer
	 * @param _size
	 * @param _presentationTimeUs
	 */
	public void set(@NonNull final ByteBuffer buffer,
		@IntRange(from=0)final int _size, final long _presentationTimeUs) {
		set(buffer, 0, size, presentationTimeUs, 0);
	}
	
	/**
	 * データをセット
	 * bufferのoffsetからoffset+size分をコピーする。
	 * ここで指定したオフセット値は保持されず#getでバッファの内容を取得する際には
	 * オフセットは必ず0になる
	 * @param buffer
	 * @param _offset
	 * @param _size
	 * @param _presentationTimeUs
	 * @param _flags
	 */
	public void set(@NonNull final ByteBuffer buffer,
		@IntRange(from=0) final int _offset,
		@IntRange(from=0)final int _size,
		final long _presentationTimeUs, final int _flags) {

		presentationTimeUs = _presentationTimeUs;
		size = _size;
		flags = _flags;
		if (mBuffer == null || mBuffer.capacity() < _size) {
			mBuffer = ByteBuffer.allocateDirect(_size).order(ByteOrder.nativeOrder());
		}
		buffer.limit(_offset + _size);
		buffer.flip();
		buffer.position(_offset);
		mBuffer.clear();
		mBuffer.put(buffer);
		mBuffer.position(_size);
		mBuffer.flip();
	}
	
	/**
	 * データをクリア
	 */
	public void clear() {
		size = flags = 0;
		mBuffer.clear();
	}
	
	/**
	 * 保持しているデータサイズを取得
	 * @return
	 */
	public int size() {
		return size;
	}
	
	/**
	 * presentationTimeUsを取得
	 * @return
	 */
	public long presentationTimeUs() {
		return presentationTimeUs;
	}
	
	/**
	 * データの内容を取得する
	 * @param buffer
	 * @throws ArrayIndexOutOfBoundsException
	 */
	public void get(final byte[] buffer) throws ArrayIndexOutOfBoundsException {
		if ((buffer == null) || (buffer.length < size)) {
			throw new ArrayIndexOutOfBoundsException("");
		}
		mBuffer.clear();
		mBuffer.limit(size);
		mBuffer.flip();
		mBuffer.get(buffer);
	}
	
	/**
	 * データの内容を取得する
	 * @param buffer
	 * @throws ArrayIndexOutOfBoundsException
	 */
	public void get(final ByteBuffer buffer) throws ArrayIndexOutOfBoundsException {
		if ((buffer == null) || (buffer.remaining() < size)) {
			throw new ArrayIndexOutOfBoundsException("");
		}
		mBuffer.clear();
		mBuffer.limit(size);
		mBuffer.flip();
		buffer.put(mBuffer);
	}
	
	/**
	 * MediaCodec.BufferInfoとしてメタデータを取得する
	 * @param info
	 */
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	public void get(@NonNull final MediaCodec.BufferInfo info) {
		info.set(0, size, presentationTimeUs, flags);
	}
}
