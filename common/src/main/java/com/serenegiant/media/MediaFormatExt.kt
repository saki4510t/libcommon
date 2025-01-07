package com.serenegiant.media
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

import android.media.MediaFormat

/**
 * 指定した名前のCSDをByteArrayとして取り出す
 * 指定した名前のCSDが存在しないときは空のbyte配列を返す
 * @param name CSDの種類 "csd-0", "csd-1"
 * @param removeStartMarker スタートマーカー(00000001)を取り除くかどうか
 */
fun MediaFormat.getCsd(name: String, removeStartMarker: Boolean = false): ByteArray {
	val csd = getByteBuffer(name)
	return if (csd != null) {
		csd.position(csd.capacity()).flip()
		val buf = if (removeStartMarker) {
			csd.position(4)
			ByteArray(csd.capacity() - 4)
		} else {
			csd.position(0)
			ByteArray(csd.capacity())
		}
		csd.get(buf)
		buf
	} else {
		byteArrayOf()
	}
}

/**
 * MediaFormatのコピーコンストラクタがAPI>=29なので中身をコピーするためのヘルパー関数
 * MediaCodecUtils.duplicateを呼び出す
 * API>=29ならコピーコンストラクタを呼び出す,
 * API<29なら#asStringで文字列にしてから#asMediaFormatで新規生成する
 */
fun MediaFormat.duplicate(): MediaFormat {
	return MediaCodecUtils.duplicate(this)
}
