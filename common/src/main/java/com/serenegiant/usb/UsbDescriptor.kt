package com.serenegiant.usb
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

/**
 * USB機器のデバイスディスクリプタ解析用ヘルパークラス
 */
class UsbDescriptor(private val rawDescriptor: ByteArray) {
	/**
	 * このディスクリプタのサイズを取得
	 * bLength
	 * @return 有効な場合はディスクリプタのサイズ、無効な場合は0を返す
	 */
	val length: Int
		get() {
			return if (rawDescriptor.size >= DESCRIPTOR_HEADER_BYTES) {
				rawDescriptor[0].toInt().coerceAtMost(rawDescriptor.size)
			} else {
				0
			}
		}

	/**
	 * このディスクリプタの種類を取得
	 * bDescriptorType
	 * @return 有効な場合はディスクリプタの種類、無効な場合は0を返す
	 */
	val type: Int
		get() {
			return if (rawDescriptor.size >= DESCRIPTOR_HEADER_BYTES) {
				rawDescriptor[1].toInt()
			} else {
				0
			}
		}

	/**
	 * 有効なディスクリプタを保持しているかどうかを取得
	 * @return true: 有効なディスクリプタを保持している、false: 有効なディスクリプタを保持していない
	 */
	val isValid: Boolean
		get() = (length >= DESCRIPTOR_HEADER_BYTES) && (rawDescriptor.size >= length)

	/**
	 * 次のディスクリプタが存在するかどうかを取得
	 * @return true: 次のディスクリプタのデータが存在する, false: 次のディスクリプタのデータが存在しない
	 */
	val hasNext: Boolean
		get() = (length >= DESCRIPTOR_HEADER_BYTES) && (rawDescriptor.size >= length + DESCRIPTOR_HEADER_BYTES)

	/**
	 * 次のディスクリプタを取得
	 * 次のディスクリプタ以降のデータを全て含む
	 * @return 次のディスクリプタを示すDescriptorを返す
	 */
	fun next(): UsbDescriptor {
		return UsbDescriptor(if (rawDescriptor.size > length) {
			rawDescriptor.sliceArray(length..< rawDescriptor.size)
		} else {
			ByteArray(0)
		})
	}

	/**
	 * このディスクリプタをバイト配列として取得する
	 * hasNext=trueでもこのディスクリプタより後ろのディスクリプタのデータは含まない
	 * @return isValid=trueなら有効なバイト配列が返る, isValid=falseなら長さ0のバイト配列が返る
	 */
	fun byteArray(): ByteArray {
		return rawDescriptor.copyOf(length)
	}

	/**
	 * このディスクリプタのデータだけを含むDescriptorを返す
	 * このディスクリプタがhasNext=trueの場合でも
	 * 返値のDescriptorはhasNext=falseになる
	 * @return このディスクリプタよりも後ろのデータを切り捨てたDescriptor
	 */
	fun shrink(): UsbDescriptor {
		return UsbDescriptor(byteArray())
	}

	override fun toString(): String {
		return "Descriptor(len=$length(${rawDescriptor.size}),type=0x%02x,hasNext=$hasNext)".format(type)
	}

	companion object {
		private const val DEBUG = false	// set false on production
		private val TAG = UsbDescriptor::class.java.simpleName
		/**
		 * ディスクリプタヘッダーのバイト数
		 * bLengthとbDescriptorTypeの最低2バイト必要
		 */
		private const val DESCRIPTOR_HEADER_BYTES = 2
	}
}

/**
 * Byte配列からDescriptorを生成する拡張関数
 */
fun ByteArray.toDescriptor(): UsbDescriptor {
	return UsbDescriptor(this)
}
