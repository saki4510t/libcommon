package com.serenegiant.libcommon
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

import com.serenegiant.utils.ArrayUtilsKt.toByteArray
import com.serenegiant.utils.ArrayUtilsKt.toDouble
import com.serenegiant.utils.ArrayUtilsKt.toFloat
import com.serenegiant.utils.ArrayUtilsKt.toInt
import com.serenegiant.utils.ArrayUtilsKt.toLong
import com.serenegiant.utils.ArrayUtilsKt.toShort
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.ByteOrder
import kotlin.math.pow

class ArrayUtilsKtTest {
	@Test
	fun primitiveToByteArrayBigEndian() {
		val shortBytes = 0x1234.toShort().toByteArray()
		assertEquals(Short.SIZE_BYTES, shortBytes.size)
		assertEquals(0x12.toByte(), shortBytes[0])
		assertEquals(0x34.toByte(), shortBytes[1])

		val intBytes = 0x12345678.toByteArray()
		assertEquals(Int.SIZE_BYTES, intBytes.size)
		assertEquals(0x12.toByte(), intBytes[0])
		assertEquals(0x34.toByte(), intBytes[1])
		assertEquals(0x56.toByte(), intBytes[2])
		assertEquals(0x78.toByte(), intBytes[3])

		val longBytes = 0x123456789abcdef0.toByteArray()
		assertEquals(Long.SIZE_BYTES, longBytes.size)
		assertEquals(0x12.toByte(), longBytes[0])
		assertEquals(0x34.toByte(), longBytes[1])
		assertEquals(0x56.toByte(), longBytes[2])
		assertEquals(0x78.toByte(), longBytes[3])
		assertEquals(0x9a.toByte(), longBytes[4])
		assertEquals(0xbc.toByte(), longBytes[5])
		assertEquals(0xde.toByte(), longBytes[6])
		assertEquals(0xf0.toByte(), longBytes[7])

		val floatBytes = 1234.5677f.toByteArray()
		assertEquals(Float.SIZE_BYTES, floatBytes.size)
		assertEquals(0x44.toByte(), floatBytes[0])
		assertEquals(0x9a.toByte(), floatBytes[1])
		assertEquals(0x52.toByte(), floatBytes[2])
		assertEquals(0x2b.toByte(), floatBytes[3])

		val doubleBytes = 1234.56789.toByteArray()
		assertEquals(Double.SIZE_BYTES, doubleBytes.size)
		assertEquals(0x40.toByte(), doubleBytes[0])
		assertEquals(0x93.toByte(), doubleBytes[1])
		assertEquals(0x4a.toByte(), doubleBytes[2])
		assertEquals(0x45.toByte(), doubleBytes[3])
		assertEquals(0x84.toByte(), doubleBytes[4])
		assertEquals(0xf4.toByte(), doubleBytes[5])
		assertEquals(0xc6.toByte(), doubleBytes[6])
		assertEquals(0xe7.toByte(), doubleBytes[7])
	}

	@Test
	fun primitiveToByteArrayLittleEndian() {
		val shortBytes = 0x1234.toShort().toByteArray(ByteOrder.LITTLE_ENDIAN)
		assertEquals(Short.SIZE_BYTES, shortBytes.size)
		assertEquals(0x12.toByte(), shortBytes[1])
		assertEquals(0x34.toByte(), shortBytes[0])

		val intBytes = 0x12345678.toByteArray(ByteOrder.LITTLE_ENDIAN)
		assertEquals(Int.SIZE_BYTES, intBytes.size)
		assertEquals(0x12.toByte(), intBytes[3])
		assertEquals(0x34.toByte(), intBytes[2])
		assertEquals(0x56.toByte(), intBytes[1])
		assertEquals(0x78.toByte(), intBytes[0])

		val longBytes = 0x123456789abcdef0.toByteArray(ByteOrder.LITTLE_ENDIAN)
		assertEquals(Long.SIZE_BYTES, longBytes.size)
		assertEquals(0x12.toByte(), longBytes[7])
		assertEquals(0x34.toByte(), longBytes[6])
		assertEquals(0x56.toByte(), longBytes[5])
		assertEquals(0x78.toByte(), longBytes[4])
		assertEquals(0x9a.toByte(), longBytes[3])
		assertEquals(0xbc.toByte(), longBytes[2])
		assertEquals(0xde.toByte(), longBytes[1])
		assertEquals(0xf0.toByte(), longBytes[0])

		val floatBytes = 1234.5677f.toByteArray(ByteOrder.LITTLE_ENDIAN)
		assertEquals(Float.SIZE_BYTES, floatBytes.size)
		assertEquals(0x44.toByte(), floatBytes[3])
		assertEquals(0x9a.toByte(), floatBytes[2])
		assertEquals(0x52.toByte(), floatBytes[1])
		assertEquals(0x2b.toByte(), floatBytes[0])

		val doubleBytes = 1234.56789.toByteArray(ByteOrder.LITTLE_ENDIAN)
		assertEquals(Double.SIZE_BYTES, doubleBytes.size)
		assertEquals(0x40.toByte(), doubleBytes[7])
		assertEquals(0x93.toByte(), doubleBytes[6])
		assertEquals(0x4a.toByte(), doubleBytes[5])
		assertEquals(0x45.toByte(), doubleBytes[4])
		assertEquals(0x84.toByte(), doubleBytes[3])
		assertEquals(0xf4.toByte(), doubleBytes[2])
		assertEquals(0xc6.toByte(), doubleBytes[1])
		assertEquals(0xe7.toByte(), doubleBytes[0])
	}

	@Test
	fun primitiveArrayToByteArrayBigEndian() {
		val shortBytes = shortArrayOf(
			0x1234.toShort(),
			0x5678.toShort(),
		).toByteArray()
		assertEquals(Short.SIZE_BYTES * 2, shortBytes.size)
		assertEquals(0x12.toByte(), shortBytes[0])
		assertEquals(0x34.toByte(), shortBytes[1])
		assertEquals(0x56.toByte(), shortBytes[2])
		assertEquals(0x78.toByte(), shortBytes[3])

		val intBytes = intArrayOf(
			0x12345678.toInt(),
			0x9abcdef0.toInt(),
		).toByteArray()
		assertEquals(Int.SIZE_BYTES * 2, intBytes.size)
		assertEquals(0x12.toByte(), intBytes[0])
		assertEquals(0x34.toByte(), intBytes[1])
		assertEquals(0x56.toByte(), intBytes[2])
		assertEquals(0x78.toByte(), intBytes[3])
		assertEquals(0x9a.toByte(), intBytes[4])
		assertEquals(0xbc.toByte(), intBytes[5])
		assertEquals(0xde.toByte(), intBytes[6])
		assertEquals(0xf0.toByte(), intBytes[7])

		val longBytes = longArrayOf(
			0x123456789abcdef0,
			0x0fedcba987654321,
		).toByteArray()
		assertEquals(Long.SIZE_BYTES * 2, longBytes.size)
		assertEquals(0x12.toByte(), longBytes[0])
		assertEquals(0x34.toByte(), longBytes[1])
		assertEquals(0x56.toByte(), longBytes[2])
		assertEquals(0x78.toByte(), longBytes[3])
		assertEquals(0x9a.toByte(), longBytes[4])
		assertEquals(0xbc.toByte(), longBytes[5])
		assertEquals(0xde.toByte(), longBytes[6])
		assertEquals(0xf0.toByte(), longBytes[7])
		assertEquals(0x0f.toByte(), longBytes[8])
		assertEquals(0xed.toByte(), longBytes[9])
		assertEquals(0xcb.toByte(), longBytes[10])
		assertEquals(0xa9.toByte(), longBytes[11])
		assertEquals(0x87.toByte(), longBytes[12])
		assertEquals(0x65.toByte(), longBytes[13])
		assertEquals(0x43.toByte(), longBytes[14])
		assertEquals(0x21.toByte(), longBytes[15])

		val floatBytes = floatArrayOf(
			1234.5677f,
			1234.5677f,
		).toByteArray()
		assertEquals(Float.SIZE_BYTES * 2, floatBytes.size)
		assertEquals(0x44.toByte(), floatBytes[0])
		assertEquals(0x9a.toByte(), floatBytes[1])
		assertEquals(0x52.toByte(), floatBytes[2])
		assertEquals(0x2b.toByte(), floatBytes[3])
		assertEquals(0x44.toByte(), floatBytes[4])
		assertEquals(0x9a.toByte(), floatBytes[5])
		assertEquals(0x52.toByte(), floatBytes[6])
		assertEquals(0x2b.toByte(), floatBytes[7])

		val doubleBytes = doubleArrayOf(
			1234.56789,
			1234.56789,
		).toByteArray()
		assertEquals(Double.SIZE_BYTES * 2, doubleBytes.size)
		assertEquals(0x40.toByte(), doubleBytes[0])
		assertEquals(0x93.toByte(), doubleBytes[1])
		assertEquals(0x4a.toByte(), doubleBytes[2])
		assertEquals(0x45.toByte(), doubleBytes[3])
		assertEquals(0x84.toByte(), doubleBytes[4])
		assertEquals(0xf4.toByte(), doubleBytes[5])
		assertEquals(0xc6.toByte(), doubleBytes[6])
		assertEquals(0xe7.toByte(), doubleBytes[7])
		assertEquals(0x40.toByte(), doubleBytes[8])
		assertEquals(0x93.toByte(), doubleBytes[9])
		assertEquals(0x4a.toByte(), doubleBytes[10])
		assertEquals(0x45.toByte(), doubleBytes[11])
		assertEquals(0x84.toByte(), doubleBytes[12])
		assertEquals(0xf4.toByte(), doubleBytes[13])
		assertEquals(0xc6.toByte(), doubleBytes[14])
		assertEquals(0xe7.toByte(), doubleBytes[15])
	}

	@Test
	fun primitiveArrayToByteArrayLittleEndian() {
		val shortBytes = shortArrayOf(
			0x1234.toShort(),
			0x5678.toShort(),
		).toByteArray(ByteOrder.LITTLE_ENDIAN)
		assertEquals(Short.SIZE_BYTES * 2, shortBytes.size)
		assertEquals(0x12.toByte(), shortBytes[1])
		assertEquals(0x34.toByte(), shortBytes[0])
		assertEquals(0x56.toByte(), shortBytes[3])
		assertEquals(0x78.toByte(), shortBytes[2])

		val intBytes = intArrayOf(
			0x12345678.toInt(),
			0x9abcdef0.toInt(),
		).toByteArray(ByteOrder.LITTLE_ENDIAN)
		assertEquals(Int.SIZE_BYTES * 2, intBytes.size)
		assertEquals(0x12.toByte(), intBytes[3])
		assertEquals(0x34.toByte(), intBytes[2])
		assertEquals(0x56.toByte(), intBytes[1])
		assertEquals(0x78.toByte(), intBytes[0])
		assertEquals(0x9a.toByte(), intBytes[7])
		assertEquals(0xbc.toByte(), intBytes[6])
		assertEquals(0xde.toByte(), intBytes[5])
		assertEquals(0xf0.toByte(), intBytes[4])

		val longBytes = longArrayOf(
			0x123456789abcdef0,
			0x0fedcba987654321,
		).toByteArray(ByteOrder.LITTLE_ENDIAN)
		assertEquals(Long.SIZE_BYTES * 2, longBytes.size)
		assertEquals(0x12.toByte(), longBytes[7])
		assertEquals(0x34.toByte(), longBytes[6])
		assertEquals(0x56.toByte(), longBytes[5])
		assertEquals(0x78.toByte(), longBytes[4])
		assertEquals(0x9a.toByte(), longBytes[3])
		assertEquals(0xbc.toByte(), longBytes[2])
		assertEquals(0xde.toByte(), longBytes[1])
		assertEquals(0xf0.toByte(), longBytes[0])
		assertEquals(0x0f.toByte(), longBytes[15])
		assertEquals(0xed.toByte(), longBytes[14])
		assertEquals(0xcb.toByte(), longBytes[13])
		assertEquals(0xa9.toByte(), longBytes[12])
		assertEquals(0x87.toByte(), longBytes[11])
		assertEquals(0x65.toByte(), longBytes[10])
		assertEquals(0x43.toByte(), longBytes[9])
		assertEquals(0x21.toByte(), longBytes[8])

		val floatBytes = floatArrayOf(
			1234.5677f,
			1234.5677f,
		).toByteArray(ByteOrder.LITTLE_ENDIAN)
		assertEquals(Float.SIZE_BYTES * 2, floatBytes.size)
		assertEquals(0x44.toByte(), floatBytes[3])
		assertEquals(0x9a.toByte(), floatBytes[2])
		assertEquals(0x52.toByte(), floatBytes[1])
		assertEquals(0x2b.toByte(), floatBytes[0])
		assertEquals(0x44.toByte(), floatBytes[7])
		assertEquals(0x9a.toByte(), floatBytes[6])
		assertEquals(0x52.toByte(), floatBytes[5])
		assertEquals(0x2b.toByte(), floatBytes[4])

		val doubleBytes = doubleArrayOf(
			1234.56789,
			1234.56789,
		).toByteArray(ByteOrder.LITTLE_ENDIAN)
		assertEquals(Double.SIZE_BYTES * 2, doubleBytes.size)
		assertEquals(0x40.toByte(), doubleBytes[7])
		assertEquals(0x93.toByte(), doubleBytes[6])
		assertEquals(0x4a.toByte(), doubleBytes[5])
		assertEquals(0x45.toByte(), doubleBytes[4])
		assertEquals(0x84.toByte(), doubleBytes[3])
		assertEquals(0xf4.toByte(), doubleBytes[2])
		assertEquals(0xc6.toByte(), doubleBytes[1])
		assertEquals(0xe7.toByte(), doubleBytes[0])
		assertEquals(0x40.toByte(), doubleBytes[15])
		assertEquals(0x93.toByte(), doubleBytes[14])
		assertEquals(0x4a.toByte(), doubleBytes[13])
		assertEquals(0x45.toByte(), doubleBytes[12])
		assertEquals(0x84.toByte(), doubleBytes[11])
		assertEquals(0xf4.toByte(), doubleBytes[10])
		assertEquals(0xc6.toByte(), doubleBytes[9])
		assertEquals(0xe7.toByte(), doubleBytes[8])
	}

	@Test
	fun byteArrayToXXXTest() {
		val shortValue = byteArrayOf(
			0x12.toByte(),
			0x34.toByte(),
		).toShort()
		assertEquals(0x1234.toShort(), shortValue)

		val intValue = byteArrayOf(
			0x12.toByte(),
			0x34.toByte(),
			0x56.toByte(),
			0x78.toByte(),
		).toInt()
		assertEquals(0x12345678, intValue)

		val longValue = byteArrayOf(
			0x12.toByte(),
			0x34.toByte(),
			0x56.toByte(),
			0x78.toByte(),
			0x9a.toByte(),
			0xbc.toByte(),
			0xde.toByte(),
			0xf0.toByte(),
		).toLong()
		assertEquals(0x123456789abcdef0, longValue)

		val floatValue = byteArrayOf(
			0x44.toByte(),
			0x9a.toByte(),
			0x52.toByte(),
			0x2b.toByte(),
		).toFloat()
		assertEquals(1234.5677f, floatValue)

		val doubleValue = byteArrayOf(
			0x40.toByte(),
			0x93.toByte(),
			0x4a.toByte(),
			0x45.toByte(),
			0x84.toByte(),
			0xf4.toByte(),
			0xc6.toByte(),
			0xe7.toByte(),
		).toDouble()
		assertEquals(1234.56789, doubleValue, EPS)
	}

	@Test
	fun byteArrayToXXXWithOffsetTest() {
		val shortValue = byteArrayOf(
			0x00, 0x00, 0x00,
			0x12.toByte(),
			0x34.toByte(),
		).toShort(3)
		assertEquals(0x1234.toShort(), shortValue)

		val intValue = byteArrayOf(
			0x00, 0x00, 0x00,
			0x00, 0x00, 0x00,
			0x12.toByte(),
			0x34.toByte(),
			0x56.toByte(),
			0x78.toByte(),
		).toInt(6)
		assertEquals(0x12345678, intValue)

		val longValue = byteArrayOf(
			0x00, 0x00, 0x00,
			0x00, 0x00, 0x00,
			0x00, 0x00, 0x00,
			0x12.toByte(),
			0x34.toByte(),
			0x56.toByte(),
			0x78.toByte(),
			0x9a.toByte(),
			0xbc.toByte(),
			0xde.toByte(),
			0xf0.toByte(),
		).toLong(9)
		assertEquals(0x123456789abcdef0, longValue)

		val floatValue = byteArrayOf(
			0x12, 0x34, 0x56, 0x78, 0x00,
			0x44.toByte(),
			0x9a.toByte(),
			0x52.toByte(),
			0x2b.toByte(),
		).toFloat(5)
		assertEquals(1234.5677f, floatValue)

		val doubleValue = byteArrayOf(
			0x12, 0x34, 0x56, 0x78, 0x00,
			0x12, 0x34, 0x56, 0x78, 0x00,
			0x40.toByte(),
			0x93.toByte(),
			0x4a.toByte(),
			0x45.toByte(),
			0x84.toByte(),
			0xf4.toByte(),
			0xc6.toByte(),
			0xe7.toByte(),
		).toDouble(10)
		assertEquals(1234.56789, doubleValue, EPS)
	}

	companion object {
		private val EPS = 10.0.pow((java.lang.Float.MIN_EXPONENT + 2).toDouble())
	}
}
