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
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MediaData {

	@NonNull
	private ByteOrder mByteOrder = ByteOrder.nativeOrder();
	/*package*/ByteBuffer mBuffer;
	/*package*/int flags;
	/*package*/int size;
	/*package*/long presentationTimeUs;
	
	/**
	 * コンストラクタ
	 */
	public MediaData() {
	}

	/**
	 * コンストラクタ
	 */
	public MediaData(@NonNull final ByteOrder order) {
		mByteOrder = order;
	}

	/**
	 * コンストラクタ
	 * @param size データ保持用の内部バッファのデフォルトサイズ
	 */
	public MediaData(@IntRange(from=0)final int size) {
		resize(size);
	}
	
	/**
	 * コンストラクタ
	 * @param size データ保持用の内部バッファのデフォルトサイズ
	 * @param order
	 */
	public MediaData(@IntRange(from=0)final int size, @NonNull final ByteOrder order) {
		mByteOrder = order;
		resize(size);
	}

	/**
	 * コピーコンストラクタ(ディープコピー)
	 * @param src
	 */
	public MediaData(@NonNull MediaData src) {
		mByteOrder = src.mByteOrder;
		set(src.mBuffer, 0, src.size, src.presentationTimeUs, src.flags);
	}

	/**
	 * データをセット
	 * このオブジェクトのエンディアンは変更されないので注意
	 * @param src
	 */
	public void set(@NonNull MediaData src) {
		set(src.mBuffer, 0, src.size, src.presentationTimeUs, src.flags);
	}

	/**
	 * データをセット
	 * @param buffer
	 * @param size
	 * @param presentationTimeUs
	 */
	public void set(@NonNull final ByteBuffer buffer,
		@IntRange(from=0)final int size, final long presentationTimeUs) {
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
	public void set(@Nullable final ByteBuffer buffer,
		@IntRange(from=0) final int _offset,
		@IntRange(from=0)final int _size,
		final long _presentationTimeUs, final int _flags) {

		presentationTimeUs = _presentationTimeUs;
		size = _size;
		flags = _flags;
		resize(_size);
		if ((buffer != null) && (_size > _offset)) {
			buffer.position(_offset + _size);
			buffer.flip();
			buffer.position(_offset);
			mBuffer.put(buffer);
			mBuffer.flip();
		}
	}
	
	/**
	 * データをセット
	 * @param buffer
	 * @param info
	 */
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	public void set(@Nullable ByteBuffer buffer,
		@NonNull final MediaCodec.BufferInfo info) {
	
		presentationTimeUs = info.presentationTimeUs;
		size = buffer != null ? info.size : 0;
		flags = info.flags;
		final int offset = info.offset;
		resize(size);
		if ((buffer != null) && (size > offset)) {
			buffer.position(offset + size);
			buffer.flip();
			buffer.position(offset);
			mBuffer.put(buffer);
			mBuffer.flip();
		}
	}
	
	/**
	 * 必要に応じて内部のByteBufferの容量を変更する、
	 * 保持しているデータサイズ(sizeフィールド)は変更しない
	 * @param newSize
	 * @return
	 */
	public MediaData resize(@IntRange(from=0)final int newSize) {
		if ((mBuffer == null) || (mBuffer.capacity() < newSize)) {
			mBuffer = ByteBuffer.allocateDirect(newSize)
				.order(mByteOrder);
		}
		mBuffer.clear();
		return this;
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
		if ((mBuffer == null) || (size <= 0)|| (buffer == null) || (buffer.length < size)) {
			throw new ArrayIndexOutOfBoundsException("");
		}
		mBuffer.clear();
		mBuffer.position(size);
		mBuffer.flip();
		mBuffer.get(buffer);
	}
	
	/**
	 * データの内容を取得する
	 * @param buffer
	 * @throws ArrayIndexOutOfBoundsException
	 */
	public void get(final ByteBuffer buffer) throws ArrayIndexOutOfBoundsException {
		if ((mBuffer == null) || (size <= 0) || (buffer == null) || (buffer.remaining() < size)) {
			throw new ArrayIndexOutOfBoundsException("");
		}
		mBuffer.clear();
		mBuffer.position(size);
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
	
	/**
	 * 内部で保持しているByteBufferを返す
	 * FIXME ByteBuffer#asReadOnlyBufferを返すように修正する
 	 * @return
	 */
	public ByteBuffer get() {
		if (mBuffer != null) {
			mBuffer.clear();
			mBuffer.position(size);
			mBuffer.flip();
		}
		return mBuffer;
	}

	/**
	 * 内部で保持しているByteBufferのバイトオーダーを返す
	 * @return
	 */
	@NonNull
	public ByteOrder order() {
		return mByteOrder;
	}

	/**
	 * 内部で保持しているByteBufferのバイトオーダーを変更する
	 * @param order
	 */
	public void order(@NonNull final ByteOrder order) {
		mByteOrder = order;
		if (mBuffer != null) {
			mBuffer.order(order);
		}
	}
}
