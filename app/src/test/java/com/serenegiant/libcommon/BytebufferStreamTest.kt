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

import com.serenegiant.io.ByteBufferOutputStream
import com.serenegiant.utils.ArrayUtilsKt.toByteArray
import org.junit.Assert.*
import org.junit.Test
import java.nio.ByteBuffer

class BytebufferStreamTest {

	@Test
	fun byteBufferOutputStreamTest() {
		val outputStream = ByteBufferOutputStream(ByteBuffer.allocate(1000), true)

		outputStream.reset()
		for (i in 0.. 254) {
			outputStream.write(byteArrayOf(i.toByte()))
		}
		var buffer = outputStream.toByteBuffer()
		for (i in 0.. 254) {
			assertEquals(i.toByte(), buffer.get())
		}

		outputStream.reset()
		for (i in 0.. 254) {
			outputStream.write(i.toShort().toByteArray())
		}
		buffer = outputStream.toByteBuffer()
		for (i in 0.. 254) {
			assertEquals(i.toShort(), buffer.getShort())
		}

		outputStream.reset()
		for (i in 0.. 254) {
			outputStream.write(i.toByteArray())
		}
		buffer = outputStream.toByteBuffer()
		for (i in 0.. 254) {
			assertEquals(i, buffer.getInt())
		}

		outputStream.reset()
		for (i in 0.. 254L) {
			outputStream.write(i.toByteArray())
		}
		buffer = outputStream.toByteBuffer()
		for (i in 0.. 254L) {
			assertEquals(i, buffer.getLong())
		}

		outputStream.reset()
		val byteArray = ByteArray(255) { it.toByte() }
		outputStream.write(byteArray)
		buffer = outputStream.toByteBuffer()
		for (i in 0.. 254) {
			assertEquals(byteArray[i], buffer.get())
		}

		outputStream.reset()
		val intArray = IntArray(255) { it * it }
		outputStream.write(intArray.toByteArray())
		buffer = outputStream.toByteBuffer()
		for (i in 0.. 254) {
			assertEquals(intArray[i], buffer.getInt())
		}
	}
}