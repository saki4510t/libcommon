package com.serenegiant.media;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2021 saki t_saki@serenegiant.com
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
	private int mTrackIx;
	private ByteBuffer mBuffer;
	private int mFlags;
	private int mSize;
	private long mPresentationTimeUs;
	
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
		set(src.mTrackIx, src.mBuffer, 0, src.mSize, src.mPresentationTimeUs, src.mFlags);
	}

//--------------------------------------------------------------------------------
	/**
	 * データをセット
	 * このオブジェクトのエンディアンは変更されないので注意
	 * @param src
	 */
	public void set(@NonNull MediaData src) {
		set(0, src.mBuffer, 0, src.mSize, src.mPresentationTimeUs, src.mFlags);
	}

	/**
	 * データをセット
	 * @param buffer
	 * @param size
	 * @param presentationTimeUs
	 */
	public void set(@NonNull final ByteBuffer buffer,
		@IntRange(from=0)final int size, final long presentationTimeUs) {
		set(0, buffer, 0, size, presentationTimeUs, 0);
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
	public void set(
		@Nullable final ByteBuffer buffer,
		@IntRange(from=0) final int _offset,
		@IntRange(from=0)final int _size,
		final long _presentationTimeUs, final int _flags) {

		set(0, buffer, _offset, _size, _presentationTimeUs, _flags);
	}

	/**
	 * データをセット
	 * bufferのoffsetからoffset+size分をコピーする。
	 * ここで指定したオフセット値は保持されず#getでバッファの内容を取得する際には
	 * オフセットは必ず0になる
	 * @param trackIx
	 * @param buffer
	 * @param offset
	 * @param size
	 * @param presentationTimeUs
	 * @param flags
	 */
	public void set(
		final int trackIx,
		@Nullable final ByteBuffer buffer,
		@IntRange(from=0) final int offset,
		@IntRange(from=0)final int size,
		final long presentationTimeUs, final int flags) {

		mTrackIx = trackIx;
		mPresentationTimeUs = presentationTimeUs;
		mSize = size;
		mFlags = flags;
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
	 * データをセット
	 * @param buffer
	 * @param info
	 */
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	public void set(
		@Nullable ByteBuffer buffer,
		@NonNull final MediaCodec.BufferInfo info) {

		set(0, buffer, info);
	}

	/**
	 * データをセット
	 * @param buffer
	 * @param info
	 */
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	public void set(
		final int trackIx,
		@Nullable ByteBuffer buffer,
		@NonNull final MediaCodec.BufferInfo info) {

		mTrackIx = trackIx;
		mPresentationTimeUs = info.presentationTimeUs;
		mSize = buffer != null ? info.size : 0;
		mFlags = info.flags;
		final int offset = info.offset;
		resize(mSize);
		if ((buffer != null) && (mSize > offset)) {
			buffer.position(offset + mSize);
			buffer.flip();
			buffer.position(offset);
			mBuffer.put(buffer);
			mBuffer.flip();
		}
	}

//--------------------------------------------------------------------------------
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
		mSize = mFlags = 0;
		mBuffer.clear();
	}
	
	/**
	 * 保持しているデータサイズを取得
	 * @return
	 */
	public int size() {
		return mSize;
	}

	public MediaData size(final int size) {
		mSize = size;
		return this;
	}

	public int trackIx() {
		return mTrackIx;
	}

	public int flags() {
		return mFlags;
	}

	/**
	 * presentationTimeUsを取得
	 * @return
	 */
	public long presentationTimeUs() {
		return mPresentationTimeUs;
	}

	public MediaData presentationTimeUs(final long pts) {
		mPresentationTimeUs = pts;
		return this;
	}

	/**
	 * データの内容を取得する
	 * @param buffer
	 * @throws ArrayIndexOutOfBoundsException
	 */
	public void get(final byte[] buffer) throws ArrayIndexOutOfBoundsException {
		if ((mBuffer == null) || (mSize <= 0)|| (buffer == null) || (buffer.length < mSize)) {
			throw new ArrayIndexOutOfBoundsException("");
		}
		mBuffer.clear();
		mBuffer.position(mSize);
		mBuffer.flip();
		mBuffer.get(buffer);
	}
	
	/**
	 * データの内容を取得する
	 * @param buffer
	 * @throws ArrayIndexOutOfBoundsException
	 */
	public void get(final ByteBuffer buffer) throws ArrayIndexOutOfBoundsException {
		if ((mBuffer == null) || (mSize <= 0) || (buffer == null) || (buffer.remaining() < mSize)) {
			throw new ArrayIndexOutOfBoundsException("");
		}
		mBuffer.clear();
		mBuffer.position(mSize);
		mBuffer.flip();
		buffer.put(mBuffer);
	}
	
	/**
	 * MediaCodec.BufferInfoとしてメタデータを取得する
	 * @param info
	 */
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	public void get(@NonNull final MediaCodec.BufferInfo info) {
		info.set(0, mSize, mPresentationTimeUs, mFlags);
	}
	
	/**
	 * 内部で保持しているByteBufferを返す
	 * FIXME ByteBuffer#asReadOnlyBufferを返すように修正する
 	 * @return
	 */
	public ByteBuffer get() {
		if (mBuffer != null) {
			mBuffer.clear();
			mBuffer.position(mSize);
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
	public MediaData order(@NonNull final ByteOrder order) {
		mByteOrder = order;
		if (mBuffer != null) {
			mBuffer.order(order);
		}
		return this;
	}
}
