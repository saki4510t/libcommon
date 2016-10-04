package com.serenegiant.utils;
/*
 * Androusb
 * Copyright (c) 2014-2016 saki t_saki@serenegiant.com
 * Distributed under the terms of the GNU Lesser General Public License (LGPL v3.0) License.
 * License details are in the file license.txt, distributed as part of this software.
 */

import java.nio.ByteBuffer;

import android.util.Log;

public class BufferHelper {

	private static final int BUF_LEN = 256;
	public static final void dumpByteBuffer(String tag, ByteBuffer buffer, int offset, int size) {
	    final byte[] dump = new byte[BUF_LEN];
//		if (DEBUG) Log.i(TAG, "dumpByteBuffer:" + buffer);
		if (buffer == null) return;
		final int n = buffer.limit();
		final int pos = buffer.position();
//		final int cap = buffer.capacity();
//		if (DEBUG) Log.i(TAG, "dumpByteBuffer:limit=" + n + ",capacity=" + cap + ",position=" + buffer.position());
		if (size > n) size = n;
		buffer.position(offset);
		final StringBuilder sb = new StringBuilder();
		int sz;
		for (int i = offset; i < size; i += BUF_LEN) {
    		sz = i + BUF_LEN < size ? BUF_LEN : size - i;
			buffer.get(dump, 0, sz);
			sb.setLength(0);
			for (int j = 0; j < sz; j++) {
				sb.append(String.format("%02x", dump[j]));
			}
			int index = -1;
			do {
				index = byteComp(dump, index+1, ANNEXB_START_MARK, ANNEXB_START_MARK.length);
				if (index >= 0) {
					Log.i(tag, "found ANNEXB: start index=" + index);
				}
			} while (index >= 0);
		}
		Log.i(tag, "dumpByteBuffer:" + sb.toString());
		buffer.position(pos);
	}

    /**
     * codec specific dataの先頭マーカー
     */
	private static final byte[] ANNEXB_START_MARK = { 0, 0, 0, 1, };
	/**
	 * byte[]を検索して一致する先頭インデックスを返す
	 * @param array 検索されるbyte[]
	 * @param search 検索するbyte[]
	 * @param len 検索するバイト数
	 * @return 一致した先頭位置、一致しなければ-1
	 */
	public static final int byteComp(byte[] array, int offset, byte[] search, int len) {
		int index = -1;
		final int n0 = array != null ? array.length : 0;
		final int ns = search != null ? search.length : 0;
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
	 * AnnexBのスタートマーカーを探してインデックスを返す
	 * @param array
	 * @param offset
	 * @return 見つからなければ負
	 */
	public static final int findAnnexB(byte[] array, int offset) {
		return byteComp(array, offset, ANNEXB_START_MARK, ANNEXB_START_MARK.length);
	}

}
