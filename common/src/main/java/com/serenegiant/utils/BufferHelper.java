package com.serenegiant.utils;
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

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.text.TextUtils;
import android.util.Log;

/**
 * Buffer/配列用のヘルパークラス
 */
public class BufferHelper {

	private BufferHelper() {
		// インスタンス化をエラーにするためにデフォルトコンストラクタをprivateに
	}

	private static final char[] HEX = {
		'0', '1', '2', '3',
		'4', '5', '6', '7',
		'8', '9', 'a', 'b',
		'c', 'd', 'e', 'f'};
	
	private static final int BUF_LEN = 256;

	/**
	 * ByteBufferの中身をlogCatへ出力する
	 * @param tag
	 * @param buffer
	 * @param offset
	 * @param size
	 */
	public static final void dump(@NonNull final String tag,
		final ByteBuffer buffer, final int offset, final int size) {

		dump(tag, null, buffer, offset, size, false);
	}

	/**
	 * ByteBufferの中身をlogCatへ出力する
	 * @param tag
	 * @param prefix
	 * @param buffer
	 * @param offset
	 * @param size
	 */
	public static final void dump(
		@NonNull final String tag, final String prefix,
		final ByteBuffer buffer, final int offset, final int size) {

		dump(tag, prefix, buffer, offset, size, false);
	}

	/**
	 * ByteBufferの中身をlogCatへ出力する
	 * @param tag
	 * @param buffer
	 * @param offset
	 * @param size
	 * @param findAnnexB
	 */
	public static final void dump(@NonNull final String tag,
		final ByteBuffer buffer, final int offset, final int size, final boolean findAnnexB) {

		dump(tag, null, buffer, offset, size, findAnnexB);
	}

	/**
	 * ByteBufferの中身をlogCatへ出力する
	 * @param tag
	 * @param _prefix
	 * @param _buffer
	 * @param offset
	 * @param _size
	 * @param findAnnexB
	 */
	public static final void dump(
		@NonNull final String tag, @Nullable final String _prefix,
		final ByteBuffer _buffer, final int offset, final int _size, final boolean findAnnexB) {

		@NonNull
		final String prefix = _prefix != null ? _prefix : "dump:";
	    final byte[] dump = new byte[BUF_LEN];
//		if (DEBUG) Log.i(TAG, "dump:" + buffer);
		if (_buffer == null) return;
		final ByteBuffer buffer = _buffer.asReadOnlyBuffer();
		final int n = buffer.limit();
		final int pos = buffer.position();
//		final int cap = buffer.capacity();
//		if (DEBUG) Log.i(TAG, "dump:limit=" + n + ",capacity=" + cap + ",position=" + buffer.position());
		int size = _size;
		if (size > n) size = n;
		buffer.position(offset);
		final StringBuilder sb = new StringBuilder();
		int sz;
		for (int i = offset; i < size; i += BUF_LEN) {
    		sz = i + BUF_LEN < size ? BUF_LEN : size - i;
			buffer.get(dump, 0, sz);
			for (int j = 0; j < sz; j++) {
				sb.append(String.format("%02x", dump[j]));
			}
			if (findAnnexB) {
				int index = -1;
				do {
					index = byteComp(dump, index+1, ANNEXB_START_MARK, ANNEXB_START_MARK.length);
					if (index >= 0) {
						Log.i(tag, prefix + " found ANNEXB: start index=" + index);
					}
				} while (index >= 0);
			}
		}
		Log.i(tag, prefix + sb);
	}

	/**
	 * ByteBufferの中身をlogCatへ出力する
	 * @param tag
	 * @param buffer
	 * @param offset
	 * @param size
	 * @param findAnnexB
	 */
	public static final void dump(final String tag,
		final byte[] buffer, final int offset, final int size, final boolean findAnnexB) {

		dump(tag, null, buffer, offset, size, findAnnexB);
	}

	/**
	 * ByteBufferの中身をlogCatへ出力する
	 * @param tag
	 * @param _prefix
	 * @param buffer
	 * @param offset
	 * @param _size
	 * @param findAnnexB
	 */
	public static final void dump(
		@NonNull final String tag, @Nullable final String _prefix,
		final byte[] buffer, final int offset, final int _size, final boolean findAnnexB) {

		@NonNull
		final String prefix = _prefix != null ? _prefix : "dump:";
		final int n = buffer != null ? buffer.length : 0;
		if (n == 0) return;
		int size = _size;
		if (size > n) size = n;
		final StringBuilder sb = new StringBuilder();
		int sz;
		for (int i = offset; i < size; i ++) {
			sb.append(String.format("%02x", buffer[i]));
		}
		if (findAnnexB) {
			int index = -1;
			do {
				index = byteComp(buffer, index+1, ANNEXB_START_MARK, ANNEXB_START_MARK.length);
				if (index >= 0) {
					Log.i(tag, prefix + " found ANNEXB: start index=" + index);
				}
			} while (index >= 0);
		}
		Log.i(tag, prefix + sb);
	}

    /**
     * codec specific dataのスタートマーカー
	 * AnnexBのスタートマーカーと同じ
	 * N[00] 00 00 01 (N ≧ 0)
     */
	public static final byte[] ANNEXB_START_MARK = { 0, 0, 0, 1, };
	/**
	 * byte[]を検索して一致する先頭インデックスを返す
	 * @param array 検索されるbyte[]
	 * @param search 検索するbyte[]
	 * @param len 検索するバイト数
	 * @return 一致した先頭位置、一致しなければ-1
	 */
	public static final int byteComp(@NonNull final byte[] array, final int offset, @NonNull final byte[] search, final int len) {
		int index = -1;
		final int n0 = array.length;
		final int ns = search.length;
		if ((n0 >= offset + len) && (ns >= len)) {
			for (int i = offset; i < n0 - len; i++) {
				int j = len - 1;
				while (j >= 0) {
					if (array[i + j] != search[j]) break;
					j--;
				}
				if (j < 0) {
					index = i;
					break;
				}
			}
		}
		return index;
	}

	/**
	 * AnnexBのスタートマーカー(N[00] 00 00 01 (N ≧ 0))を探して先頭インデックスを返す
	 * 返り値が0以上の場合は、返り値+3がpayloadの先頭位置(nalu headerのはず)
	 * @param data
	 * @param offset
	 * @return 見つからなければ負
	 */
	public static final int findAnnexB(final byte[] data, final int offset) {
		if (data != null) {
			final int len5 = data.length - 5;	// 本当はlength-3までだけどpayloadが無いのは無効とみなしてlength-4までとする
			for (int i = offset; i < len5; i++) {
				// 最低3つは連続して0x00
				if ((data[i] != 0x00) || (data[i+1] != 0x00) || (data[i+2] != 0x00)) {
					continue;
				}
				// 4つ目が0x01ならOK
				if (data[i+3] == 0x01) {
					return i;
				}
			}
			final int len4 = data.length - 4;	// 本当はlength-3までだけどpayloadが無いのは無効とみなしてlength-4までとする
			for (int i = offset; i < len4; i++) {
				// 最低2つは連続して0x00でないとだめ
				if ((data[i] != 0x00) || (data[i+1] != 0x00)) {
					continue;
				}
				// 3つ目が0x01ならOK
				if (data[i+2] == 0x01) {
					return i;
				}
			}
		}
		return -1;
	}

	/**
	 * float1つのサイズ[バイト]
	 */
	public static final int SIZEOF_FLOAT_BYTES = Float.SIZE / 8;

	/**
	 * 引数のfloat配列と同じ長さのFloatBufferを生成して引数の値をセットする
	 * @param coords
	 * @return
	 */
	public static FloatBuffer createFloatBuffer(@NonNull final float[] coords) {
		// Allocate a direct ByteBuffer, using 4 bytes per float, and copy coords into it.
		final FloatBuffer result
			= ByteBuffer.allocateDirect(coords.length * SIZEOF_FLOAT_BYTES)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		result.put(coords).flip();
		return result;
	}
	
	/**
	 * 16進文字列をパースしてByteBufferとして返す
	 * @param hexString
	 * @return
	 * @throws NumberFormatException
	 */
	public static ByteBuffer from(final String hexString) throws NumberFormatException {
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		final int n = !TextUtils.isEmpty(hexString) ? hexString.length() : 0;
		for (int i = 0; i < n; i += 2) {
			final int b = Integer.parseInt(hexString.substring(i, i + 2), 16);
			out.write(b);
		}
		return ByteBuffer.wrap(out.toByteArray());
	}
	
	/**
	 * byte配列を16進文字列に変換する
	 * @param bytes
	 * @return
	 */
	public static String toHexString(@NonNull final byte[] bytes) {
		return toHexString(bytes, 0, bytes.length);
	}

	/**
	 * byte配列を16進文字列に変換する
	 * @param bytes
	 * @param offset
	 * @param len 出力する最大バイト数
	 * @return
	 */
	public static String toHexString(final byte[] bytes,
		final int offset, final int len) {

		final int n = (bytes != null) ? bytes.length : 0;
		final int m = Math.min(n, offset + len);
		final StringBuilder sb = new StringBuilder(n * 2 + 2);
		for (int i = offset; i < m; i++) {
			final byte b = bytes[i];
			sb.append(HEX[(0xf0 & b) >>> 4]);
			sb.append(HEX[0x0f & b]);
		}
		return sb.toString();
	}
	
	/**
	 * ByteBufferを16進文字列に変換する
	 * @param buffer
	 * @return
	 */
	public static String toHexString(final ByteBuffer buffer) {
		if (buffer == null) return null;
		final ByteBuffer _buffer = buffer.duplicate();
		final int n = _buffer.remaining();
		final StringBuilder sb = new StringBuilder(n * 2 + 2);
		for (int i = 0; i < n; i++) {
			final byte b = _buffer.get();
			sb.append(HEX[(0xf0 & b) >>> 4]);
			sb.append(HEX[0x0f & b]);
		}
		return sb.toString();
	}

	/**
	 * 指定したbyte配列が指定したサイズ以上になるようにする
	 * 引数のbyte配列がnullまたは指定したサイズよりも小さい場合には新しいbyte配列を生成して返す
	 * @param buffer
	 * @param newSize
	 * @return
	 */
	@NonNull
	public static byte[] resize(@Nullable final byte[] buffer, final int newSize) {
		byte[] result = buffer;
		if ((result == null) || (result.length < newSize)) {
			result = new byte[newSize];
		}
		return result;
	}

	/**
	 * 指定したByteBufferが指定したサイズ以上になるようにする
	 * 引数のByteBufferがnullまたは指定したサイズよりも小さい場合には新しいByteBufferを生成して返す
	 * @param buffer
	 * @param newSize
	 * @return
	 */
	@NonNull
	public static ByteBuffer resize(@Nullable final ByteBuffer buffer, final int newSize) {
		ByteBuffer result = buffer;
		if ((result == null) || (result.capacity() < newSize)) {
			result = ByteBuffer.allocate(newSize);
		}
		result.clear();
		return result;
	}

	/**
	 * 指定したByteBufferが指定したサイズ以上になるようにする
	 * 引数のByteBufferがnullまたは指定したサイズよりも小さい場合には新しいダイレクトByteBufferを生成して返す
	 * @param buffer
	 * @param newSize
	 * @return
	 */
	@NonNull
	public static ByteBuffer resizeDirect(@Nullable final ByteBuffer buffer, final int newSize) {
		ByteBuffer result = buffer;
		if ((result == null) || (result.capacity() < newSize)) {
			result = ByteBuffer.allocateDirect(newSize).order(ByteOrder.nativeOrder());
		}
		result.clear();
		return result;
	}
}
