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

import java.nio.ByteBuffer;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

import static java.lang.Math.ceil;

/**
 * ByteBufferからビット単位で値を取得するためのヘルパークラス
 */
public class BitBufferReader implements IBitReader {
   @NonNull
   private final ByteBuffer mBuffer;
   private int mNumStartBits = 0;

   /**
    * コンストラクタ
    * bufferのlimit x 8ビットを読み込むことができる
    * @param buffer
    */
   public BitBufferReader(@NonNull final ByteBuffer buffer) {
      mBuffer = buffer.asReadOnlyBuffer();
   }

   /**
    * 次の1ビットを読み込んでbooleanとして返す
    * @return 読み込んだビット1ならtrue, 0ならfalseを返す
    * @throws IndexOutOfBoundsException
    * @throws IllegalArgumentException
    */
   @Override
   public boolean readBoolean() throws IndexOutOfBoundsException, IllegalArgumentException {
      return readBit() == 1L;
   }

   /**
    * 次の1ビットを読み込む
    * @return
    * @throws IndexOutOfBoundsException
    * @throws IllegalArgumentException
    */
   @Override
   public long readBit() throws IndexOutOfBoundsException, IllegalArgumentException {
      return readBits(1);
   }

   /**
    * 指定したビット数を読み込んでlongとして返す
    * @param bitCounts 読み込むビット数, 1-64
    * @return
    * @param bitCounts
    * @return
    * @throws IndexOutOfBoundsException
    * @throws IllegalArgumentException
    */
   @Override
   public long readBits(@IntRange(from = 1, to = 64) final int bitCounts)
      throws IndexOutOfBoundsException, IllegalArgumentException {

      if (bitCounts > 64) {
         throw new IllegalArgumentException("Can't read more than 64bits");
      }

      long result = 0;
      for (int i = 0; i < bitCounts; i++) {
         result <<= 1;
         final byte b = mBuffer.get(mNumStartBits / 8);
         if ((b & (0x80 >> (mNumStartBits % 8))) != 0) {
            result += 1;
         }
         mNumStartBits++;
      }

      return result;
   }

   /**
    * 符号無しexp-golomb符号を読み込む
    * @return
    */
   @Override
   public long ue() {
      int nZeroNum = 0;
      final int lastBits = mBuffer.limit() * 8;
      while (mNumStartBits < lastBits) {
         final byte b = mBuffer.get(mNumStartBits / 8);
         if ((b & (0x80 >> (mNumStartBits % 8))) != 0) {
            break;
         }
         nZeroNum++;
         mNumStartBits++;
      }
      mNumStartBits++;
      long dwRet = 0;
      for (int i = 0; i < nZeroNum; i++) {
         dwRet <<= 1;
         final byte b = mBuffer.get(mNumStartBits / 8);
         if ((b & (0x80 >> (mNumStartBits % 8))) != 0) {
            dwRet += 1;
         }
         mNumStartBits++;
      }

      return (1L << nZeroNum) - 1 + dwRet;
   }

   /**
    * 符号ありexp-golomb符号を読み込む
    * @return
    */
   @Override
   public long se() {
      final long ue = ue();
      double result = ceil(ue / 2.0);
      if ((ue % 2) == 0) {
         result = -result;
      }

      return (long)result;
   }

   /**
    * 読み込むビット位置をセットする
    * @param numBits 先頭からのビット数
    * @return
    * @throws IndexOutOfBoundsException
    * @throws IllegalArgumentException
    */
   @Override
   public BitBufferReader setBitPos(final int numBits)
      throws IndexOutOfBoundsException, IllegalArgumentException {

      mBuffer.get(numBits / 8);
      mNumStartBits = numBits;
      return this;
   }

   /**
    * 読み込み可能なビット数を取得する
    * 内包しているByteBufferのlimit x 8 - 現在のビット位置を返す
    * @return
    */
   @Override
   public int availableBits() {
      return mBuffer.limit() * 8 - mNumStartBits;
   }
}
