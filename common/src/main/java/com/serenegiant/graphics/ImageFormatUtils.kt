package com.serenegiant.graphics
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2026 saki t_saki@serenegiant.com
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

import android.graphics.ImageFormat

object ImageFormatUtils {
	/**
	 * 映像フォーマット定数を文字列に変換する
	 * @param imageFormat
	 */
	@JvmStatic
	fun toImageFormatString(imageFormat: Int): String {
		when (imageFormat) {
			ImageFormat.DEPTH16 -> return "DEPTH16"
			ImageFormat.DEPTH_POINT_CLOUD -> return "DEPTH_POINT_CLOUD"
			ImageFormat.FLEX_RGBA_8888 -> return "FLEX_RGBA_8888"
			ImageFormat.FLEX_RGB_888 -> return "FLEX_RGB_888"
			ImageFormat.JPEG -> return "JPEG"
			ImageFormat.NV16 -> return "NV16"
			ImageFormat.NV21 -> return "NV21"
			ImageFormat.PRIVATE -> return "PRIVATE"
			ImageFormat.RAW10 -> return "RAW10"
			ImageFormat.RAW12 -> return "RAW12"
			ImageFormat.RAW_PRIVATE -> return "RAW_PRIVATE"
			ImageFormat.RAW_SENSOR -> return "RAW_SENSOR"
			ImageFormat.RGB_565 -> return "RGB_565"
			ImageFormat.UNKNOWN -> return "UNKNOWN"
			ImageFormat.YUV_420_888 -> return "YUV_420_888"
			ImageFormat.YUV_422_888 -> return "YUV_422_888"
			ImageFormat.YUV_444_888 -> return "YUV_444_888"
			ImageFormat.YUY2 -> return "YUY2"
			ImageFormat.YV12 -> return "YV12"
			else -> return String.format("unknown, %08x", imageFormat)
		}
	}
}