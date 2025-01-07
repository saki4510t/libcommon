package com.serenegiant.utils
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

import java.nio.ByteBuffer
import java.nio.ByteOrder

object ArrayUtilsKt {
	//--------------------------------------------------------------------------------
	// xxx.toByteArray
	//--------------------------------------------------------------------------------
	fun Short.toByteArray(byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN): ByteArray =
		ByteBuffer.allocate(Short.SIZE_BYTES).order(byteOrder).putShort(this).array()

	fun Int.toByteArray(byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN): ByteArray =
		ByteBuffer.allocate(Int.SIZE_BYTES).order(byteOrder).putInt(this).array()

	fun Long.toByteArray(byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN): ByteArray =
		ByteBuffer.allocate(Long.SIZE_BYTES).order(byteOrder).putLong(this).array()

	fun Float.toByteArray(byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN): ByteArray =
		ByteBuffer.allocate(Float.SIZE_BYTES).order(byteOrder).putFloat(this).array()

	fun Double.toByteArray(byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN): ByteArray =
		ByteBuffer.allocate(Double.SIZE_BYTES).order(byteOrder).putDouble(this).array()

	fun ShortArray.toByteArray(byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN): ByteArray {
		val buf = ByteBuffer.allocate(Short.SIZE_BYTES * size)
			.order(byteOrder)
		forEach { value ->
			buf.putShort(value)
		}
		return buf.array()
	}

	fun IntArray.toByteArray(byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN): ByteArray {
		val buf = ByteBuffer.allocate(Int.SIZE_BYTES * size)
			.order(byteOrder)
		forEach { value ->
			buf.putInt(value)
		}
		return buf.array()
	}

	fun LongArray.toByteArray(byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN): ByteArray {
		val buf = ByteBuffer.allocate(Long.SIZE_BYTES * size)
			.order(byteOrder)
		forEach { value ->
			buf.putLong(value)
		}
		return buf.array()
	}

	fun FloatArray.toByteArray(byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN): ByteArray {
		val buf = ByteBuffer.allocate(Float.SIZE_BYTES * size)
			.order(byteOrder)
		forEach { value ->
			buf.putFloat(value)
		}
		return buf.array()
	}

	fun DoubleArray.toByteArray(byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN): ByteArray {
		val buf = ByteBuffer.allocate(Double.SIZE_BYTES * size)
			.order(byteOrder)
		forEach { value ->
			buf.putDouble(value)
		}
		return buf.array()
	}

	//--------------------------------------------------------------------------------
	// ByteArray.toXXX
	//--------------------------------------------------------------------------------
	fun ByteArray.toShort(offset: Int = 0, byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN): Short =
		ByteBuffer.wrap(copyOfRange(offset, offset + Short.SIZE_BYTES)).order(byteOrder).short

	fun ByteArray.toInt(offset: Int = 0, byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN): Int =
		ByteBuffer.wrap(copyOfRange(offset, offset + Int.SIZE_BYTES)).order(byteOrder).int

	fun ByteArray.toLong(offset: Int = 0, byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN): Long =
		ByteBuffer.wrap(copyOfRange(offset, offset + Long.SIZE_BYTES)).order(byteOrder).long

	fun ByteArray.toFloat(offset: Int = 0, byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN): Float =
		ByteBuffer.wrap(copyOfRange(offset, offset + Float.SIZE_BYTES)).order(byteOrder).float

	fun ByteArray.toDouble(offset: Int = 0, byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN): Double =
		ByteBuffer.wrap(copyOfRange(offset, offset + Double.SIZE_BYTES)).order(byteOrder).double

	private val ZERO_PADDING = "0".repeat(32)
	/**
	 * Intを指定した文字数の2進数文字列に変換する
	 */
	fun Int.toBinString(numDigit: Int): String {
		return "$ZERO_PADDING${Integer.toBinaryString(this)}".takeLast(numDigit)
	}

	/**
	 * バイト配列を16進数文字列に変換する
	 */
	fun ByteArray.toHexString(): String {
		val builder = StringBuilder()
		for (byte in this) {
			builder.append("%02X".format(byte.toInt()))
		}
		return builder.toString()
	}

	/**
	 * 指定したパターンに一致する先頭位置を検索する
	 * @param pattern
	 * @param startFrom
	 * @param lastIndex
	 */
	fun ByteArray.findFirstBy(pattern: ByteArray, startFrom: Int = 0, lastIndex: Int = size - 1): Int {
		if (pattern.isEmpty()) throw IllegalArgumentException("non-empty byte sequence is required")
		if (startFrom < 0) throw IllegalArgumentException("startFrom must be non-negative")
		var matchOffset = 0
		var start = startFrom
		var offset = startFrom
		while (offset <= lastIndex) {
			if (this[offset] == pattern[matchOffset]) {
				if (matchOffset++ == 0) start = offset
				if (matchOffset == pattern.size) return start
			} else {
				matchOffset = 0
			}
			offset++
		}
		return -1
	}

	fun ByteArray.toInt2(offset: Int = 0): Int {
		if ((offset < 0) || (offset + 2 > this.size)) {
			throw IndexOutOfBoundsException()
		}
		return this[offset + 1].toUByte().toInt() +
			(this[offset].toUByte().toInt() shl 8)
	}

	fun ByteArray.toInt3(offset: Int = 0): Int {
		if ((offset < 0) || (offset + 4 > this.size)) {
			throw IndexOutOfBoundsException()
		}
		return this[offset + 2].toUByte().toInt() +
			(this[offset + 1].toUByte().toInt() shl 8) +
			(this[offset].toUByte().toInt() shl 16)
	}

	fun ByteArray.toInt(offset: Int = 0): Int {
		if ((offset < 0) || (offset + 4 > this.size)) {
			throw IndexOutOfBoundsException()
		}
		return this[offset + 3].toUByte().toInt() +
			(this[offset + 2].toUByte().toInt() shl 8) +
			(this[offset + 1].toUByte().toInt() shl 16) +
			(this[offset].toUByte().toInt() shl 24)
	}

	fun ByteArray.setInt2(value: Int, offset: Int = 0): ByteArray {
		if ((offset < 0) || (offset + 2 > this.size)) {
			throw IndexOutOfBoundsException()
		}
		this[offset    ] = (value shr 8).toByte()
		this[offset + 1] = value.toByte()
		return this
	}

	fun ByteArray.setInt3(value: Int, offset: Int = 0): ByteArray {
		if ((offset < 0) || (offset + 3 > this.size)) {
			throw IndexOutOfBoundsException()
		}
		this[offset    ] = (value shr 16).toByte()
		this[offset + 1] = (value shr 8).toByte()
		this[offset + 2] = value.toByte()
		return this
	}

	fun ByteArray.setInt(value: Int, offset: Int = 0): ByteArray {
		if ((offset < 0) || (offset + 4 > this.size)) {
			throw IndexOutOfBoundsException()
		}
		this[offset    ] = (value shr 24).toByte()
		this[offset + 1] = (value shr 16).toByte()
		this[offset + 2] = (value shr 8).toByte()
		this[offset + 3] = value.toByte()
		return this
	}

	fun ByteArray.calcBcc(): Int {
		return (this.fold(0) { v, value -> (v xor (value.toInt() and 0xff)) } and 0xff)
	}
}
