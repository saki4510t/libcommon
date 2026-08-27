package com.serenegiant.libcommon
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

import android.util.Log
import android.util.Size
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.serenegiant.media.VideoConfig
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VideoConfigTest {
	/**
	 * VideoConfig#getBitrateで計算したビットレートが
	 * [VideoConfig.BITRATE_MIN, VideoConfig.BITRATE_MAX]に
	 * 収まることをテスト
	 */
	@Test
	fun calBitrateTest1() {
		val config = VideoConfig()
		RESOLUTIONS.forEach { sz ->
			FRAME_RATES.forEach { fps ->
				val bitrate = config.getBitrate(sz.width, sz.height, fps)
				Log.i(TAG, "calBitrateTest1:(${sz.width}x${sz.height}@$fps)bitrate=$bitrate")
				assertTrue(bitrate >= VideoConfig.BITRATE_MIN)
				assertTrue(bitrate <= VideoConfig.BITRATE_MAX)
			}
		}
	}

	/**
	 * 最大ビットレートをVideoConfig.BITRATE_MAX * 2に指定して
	 * VideoConfig#getBitrateで計算したビットレートが
	 * [VideoConfig.BITRATE_MIN, VideoConfig.BITRATE_MAX]に
	 * 収まることをテスト
	 */
	@Test
	fun calBitrateTest2() {
		val config = VideoConfig()
		RESOLUTIONS.forEach { sz ->
			FRAME_RATES.forEach { fps ->
				val bitrate = config.getBitrate(
					width = sz.width, height = sz.height,
					frameRate = fps,
					maxBitrate = VideoConfig.BITRATE_MAX * 2)
				Log.i(TAG, "calBitrateTest2:(${sz.width}x${sz.height}@$fps)bitrate=$bitrate")
				assertTrue(bitrate >= VideoConfig.BITRATE_MIN)
				assertTrue(bitrate <= VideoConfig.BITRATE_MAX * 2)
			}
		}
	}

	/**
	 * 分割録画用に最大ファイルサイズを計算する
	 * 解像度・フレームレート毎に計算
	 */
	@Test
	fun calcMaxRecordingSizeTest() {
		val config = VideoConfig()
		RESOLUTIONS.forEach { sz ->
			FRAME_RATES.forEach { fps ->
				var maxBytes = 0L
				for (duration in MAX_DURATIONS) {
					val bytes = config.getSizeRate(
						width = sz.width, height = sz.height,
						frameRate = fps
					) * duration
					if (bytes < MAX_RECORDING_BYTES) {
						maxBytes = bytes
						break
					}
				}
				Log.i(TAG, "calcMaxRecordingSizeTest:(${sz.width}x${sz.height}@$fps)maxBytes=$maxBytes(${maxBytes/1024f/1024f/1024f})")
				assertTrue(maxBytes > 0)
			}
		}
	}

	/**
	 * 分割録画用に最大ファイルサイズを計算する
	 * 解像度・フレームレート毎に最大録画時間と最大ファイルサイズを指定して計算
	 */
	@Test
	fun calcMaxSplitRecordingBytesTest() {
		val config = VideoConfig()
		RESOLUTIONS.forEach { sz ->
			FRAME_RATES.forEach { fps ->
				for (duration in MAX_DURATIONS) {
					val maxBytes = config.getSplitRecordingBytes(
						width = sz.width, height = sz.height,
						maxDurationMinutes = duration,
						maxBytes = MAX_RECORDING_BYTES,
						frameRate = fps
					)
					Log.i(TAG, "calcMaxSplitRecordingBytesTest:(${sz.width}x${sz.height}@$fps)d=$duration,maxBytes=$maxBytes(${maxBytes/1024f/1024f/1024f})")
					assertTrue(maxBytes > 0)
				}
				val maxBytes = config.getSplitRecordingBytes(
					width = sz.width, height = sz.height,
					maxDurationMinutes = -1,
					maxBytes = MAX_RECORDING_BYTES,
					frameRate = fps
				)
				Log.i(TAG, "calcMaxSplitRecordingBytesTest:(${sz.width}x${sz.height}@$fps)d=-1,maxBytes=$maxBytes(${maxBytes/1024f/1024f/1024f})")
				assertTrue(maxBytes > 0)
			}
		}
	}

	companion object {
		private val TAG = VideoConfigTest::class.java.simpleName

		private val RESOLUTIONS = arrayOf(
			Size(3840, 2160),
			Size(1920, 1080),
			Size(1280, 720),
			Size(640, 480),
			Size(320, 240),
		)
		private val FRAME_RATES = arrayOf(
			60,
			45,
			30,
			25,
			20,
			15,
			10,
			5,
		)
		private val MAX_DURATIONS = arrayOf(
			60,	// 60分
			45,	// 45分
			30,	// 30分
			20,	// 20分
			10,	// 10分
			8,	// 8分
			5,	// 5分
			3,	// 3分
			2,	// 2分
			1,	// 1分
		)

		/**
		 * FAT32で1ファイルの最大サイズは4GB未満なのでそれを上回らない切りのよいサイズ
		 */
		private const val MAX_RECORDING_BYTES = 4000000000L // 約3.73GB
	}
}