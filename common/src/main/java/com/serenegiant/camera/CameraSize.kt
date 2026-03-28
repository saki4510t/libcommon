package com.serenegiant.camera
/*
 * most code of this class originally came from android;util.Size
 *
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.os.Build
import android.util.Size
import androidx.annotation.Keep
import androidx.annotation.RequiresApi
import com.serenegiant.system.BuildCheck

/**
 * android.hardware.Camera.Sizeはdeprecated
 * android.util.SizeはAPI>=21なので自前で定義
 */
class CameraSize(val width: Int, val height: Int) {

	/**
	 * このCameraSizeインスタンスが保持している解像度を示す文字列を取得
	 * "{width}x{height}"
	 * @return
	 */
	fun toShortString(): String {
		return "${width}x$height"
	}

	override fun toString(): String {
		return "CameraSize(${width}x$height)"
	}

	@Suppress("DEPRECATION")
	override fun equals(other: Any?): Boolean {
		if (other == null) {
			return false
		}
		if (this === other) {
			return true
		}
		if (other is CameraSize) {
			return width == other.width && height == other.height
		}
		if (BuildCheck.isAPI21() && other is Size) {
			return width == other.width && height == other.height
		}
		if (other is android.hardware.Camera.Size) {
			return width == other.width && height == other.height
		}

		return false
	}

	override fun hashCode(): Int {
		// assuming most sizes are <2^16, doing a rotate will give us perfect hashing
		return height xor ((width shl (Integer.SIZE / 2)) or (width ushr (Integer.SIZE / 2)))
	}
}

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
fun Size.toCameraSize(): CameraSize {
	return CameraSize(width, height)
}

@Suppress("DEPRECATION")
fun android.hardware.Camera.Size.toCameraSize(): CameraSize {
	return CameraSize(width, height)
}

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
@Keep
fun CameraCharacteristics.getSupportedSizeList(
	klass: Class<*> = SurfaceTexture::class.java): List<CameraSize> {

	val result = mutableListOf<CameraSize>()
	val map = this.get(
		CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
	val fpsRanges = this.get(
		CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
	if ((map != null) && (fpsRanges != null)) {
		// 指定したクラスへの描画用の解像度を一覧を取得
		val sizes = map.getOutputSizes(klass)
		for (sz in sizes) {
			result.add(sz.toCameraSize())
		}
	}

	return result.toList()
}
