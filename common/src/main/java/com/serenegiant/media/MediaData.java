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

import android.media.MediaCodec;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MediaData {

	private static final int DEFAULT_BUFFER_SIZE = 1024;

	@NonNull
	private ByteOrder mByteOrder = ByteOrder.nativeOrder();
	private int mTrackIx;
	@Nullable
	private ByteBuffer mBuffer;
	private int mFlags;
	private int mSize = DEFAULT_BUFFER_SIZE;
	private long mPresentationTimeUs;
	
	/**
	 * コンストラクタ
	 * orderはByteOrder.nativeOrderになる
	 * 内部バッファは未生成(使用時に生成)
	 */
	public MediaData() {
	}

	/**
	 * コンストラクタ
	 * 内部バッファは未生成(使用時に生成)
	 * @param order データ保持用の内部バッファのエンディアン
	 */
	public MediaData(@NonNull final ByteOrder order) {
		mByteOrder = order;
	}

	/**
	 * コンストラクタ
	 * orderはByteOrder.nativeOrderになる
	 * @param size データ保持用の内部バッファのデフォルトサイズ
	 */
	public MediaData(@IntRange(from=0)final int size) {
		resize(size);
	}
	
	/**
	 * コンストラクタ
	 * @param size データ保持用の内部バッファのデフォルトサイズ
	 * @param order データ保持用の内部バッファのエンディアン
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
			buffer.clear();	// limit==positionになってる変なByteBufferが来る端末があるのでclearする
			buffer.position(offset + size);
			buffer.flip();
			buffer.position(offset);
			mBuffer.put(buffer);
			mBuffer.flip();
		}
	}

	/**
	 * データをセット(byte配列からコピー)
	 * @param buffer
	 * @param presentationTimeUs
	 */
	public void set(
		@Nullable final byte[] buffer,
		final long presentationTimeUs) {

		set(0, buffer, 0, buffer != null ? buffer.length : 0, presentationTimeUs, 0);
	}

	/**
	 * データをセット(byte配列からコピー)
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
		@Nullable final byte[] buffer,
		@IntRange(from=0) final int offset,
		@IntRange(from=0)final int size,
		final long presentationTimeUs, final int flags) {

		final int _size = Math.min(buffer != null ? buffer.length : 0, size);
		mTrackIx = trackIx;
		mPresentationTimeUs = presentationTimeUs;
		mSize = _size;
		mFlags = flags;
		resize(_size);
		if ((buffer != null) && (_size > offset)) {
			mBuffer.put(buffer, offset, _size);
			mBuffer.flip();
		}
	}

	/**
	 * データをセット
	 * @param buffer
	 * @param info
	 */
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
			buffer.clear();	// limit==positionになってる変なByteBufferが来る端末があるのでclearする
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
		if (mBuffer != null) {
			mBuffer.clear();
		}
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
	 * 内部バッファから読み取る
	 * read((buffer, 0, buffer.length))と等価
	 * 前回の#setまたは#read呼び出しでセットされたposition位置から続けて読み取る,
	 * #getはclear → position(size) → flipするので必ず先頭から読み取ることになるので注意
	 * @param buffer
	 * @return
	 */
	public int read(@NonNull final byte[] buffer) {
		return read(buffer, 0, buffer.length);
	}

	/**
	 * 内部バッファから読み取る
	 * 前回の#setまたは#read呼び出しでセットされたposition位置から続けて読み取る,
	 * #getはclear → position(size) → flipするので必ず先頭から読み取ることになるので注意
	 * @param buffer
	 * @param offset
	 * @param length
	 * @return
	 */
	public int read(@NonNull final byte[] buffer, final int offset, final int length) {
		final ByteBuffer buf = mBuffer;
		final int min = (buf != null) ? Math.min(length, size() - buf.position()) : 0;
		if (min > 0) {
			buf.get(buffer, offset, min);
		}
		return min;
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
	public void get(@NonNull final MediaCodec.BufferInfo info) {
		info.set(0, mSize, mPresentationTimeUs, mFlags);
	}
	
	/**
	 * 内部で保持しているByteBufferを返す
	 * FIXME ByteBuffer#asReadOnlyBufferを返すように修正する
 	 * @return
	 */
	@NonNull
	public ByteBuffer get() {
		if (mBuffer != null) {
			mBuffer.clear();
			mBuffer.position(mSize);
			mBuffer.flip();
		} else {
			resize(mSize > 0 ? mSize : DEFAULT_BUFFER_SIZE);
		}
		return mBuffer;
	}

	/**
	 * 内部で保持しているByteBufferを返す
	 * @param size
	 * @return
	 */
	@NonNull
	public ByteBuffer get(final int size) {
		resize(size);
		return get();
	}

	/**
	 * 内部で保持しているByteBufferをそのまま返す
	 * 他の#getはclear → position(size) → flipするので
	 * 必ず先頭から読み取ることになる
	 * @return
	 */
	@Nullable
	public ByteBuffer getRaw() {
		return mBuffer;
	}

	/**
	 * 内部バッファの容量を取得
	 * @return
	 */
	public int capacity() {
		return mBuffer != null ? mBuffer.capacity() : 0;
	}

	/**
	 * データの残量を取得
	 * 内部バッファのremainingを呼び出す
	 * @return
	 */
	public int remaining() {
		return mBuffer != null ? mBuffer.remaining() : 0;
	}

	/**
	 * データのposition取得用
	 * 内部バッファのpositionを呼び出す
	 * @return
	 */
	public int position() {
		return mBuffer != null ? mBuffer.position() : 0;
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
