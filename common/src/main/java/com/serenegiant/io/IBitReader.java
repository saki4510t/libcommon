package com.serenegiant.io;
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

import androidx.annotation.IntRange;

/**
 * ビット単位で値を取得するためのヘルパークラスのメソッドを定義するインターフェース
 */
public interface IBitReader {
	/**
	 * 次の1ビットを読み込んでbooleanとして返す
	 *
	 * @return 読み込んだビット1ならtrue, 0ならfalseを返す
	 * @return
	 * @throws IndexOutOfBoundsException
	 * @throws IllegalArgumentException
	 */
	public boolean readBoolean() throws IndexOutOfBoundsException, IllegalArgumentException;

	/**
	 * 次の1ビットを読み込む
	 *
	 * @return
	 * @throws IndexOutOfBoundsException
	 * @throws IllegalArgumentException
	 */
	public long readBit() throws IndexOutOfBoundsException, IllegalArgumentException;

	/**
	 * 指定したビット数を読み込んでlongとして返す
	 *
	 * @param bitCounts 読み込むビット数, 1-64
	 * @param bitCounts
	 * @return
	 * @return
	 * @throws IndexOutOfBoundsException
	 * @throws IllegalArgumentException
	 */
	public long readBits(@IntRange(from = 1, to = 64) final int bitCounts)
		throws IndexOutOfBoundsException, IllegalArgumentException;

	/**
	 * 符号無しexp-golomb符号を読み込む
	 * @return
	 */
	public long ue();

	/**
	 * 符号ありexp-golomb符号を読み込む
	 * @return
	 */
	public long se();

	/**
	 * 読み込むビット位置をセットする
	 *
	 * @param numBits 先頭からのビット数
	 * @return
	 * @throws IndexOutOfBoundsException
	 * @throws IllegalArgumentException
	 */
	public BitBufferReader setBitPos(final int numBits)
		throws IndexOutOfBoundsException, IllegalArgumentException;

	/**
	 * 読み込み可能なビット数を取得する
	 * 内包しているByteBufferのlimit x 8 - 現在のビット位置を返す
	 *
	 * @return
	 */
	public int availableBits();
}
