package com.serenegiant.glutils

import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.os.Handler
import android.util.Log
import android.view.Surface
import androidx.annotation.IntRange
import com.serenegiant.gl.GLManager

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
 * GLSurfaceReceiverを使ってSurfaceからテクスチャとして受け取った映像を
 * GLBitmapImageReaderを使ってビットマップとしてキャプチャするヘルパークラス
 */

class GLSurfaceCapture @JvmOverloads constructor(
	glManager: GLManager,
	@IntRange(from = 1) width: Int, @IntRange(from = 1) height: Int,
	listener: ImageReader.OnImageAvailableListener<Bitmap>,
	handler: Handler? = null,
	useOffscreenRendering: Boolean = true
) {

	private val mReader = GLBitmapImageReader(width, height, 4, useOffscreenRendering)
	private val mReceiver = GLSurfaceReceiver(glManager, width, height, mReader)

	init {
		if (DEBUG) Log.v(TAG, "init:")
		mReader.setOnImageAvailableListener(listener,
			handler ?: mReceiver.glManager.glHandler
		)
	}

	fun release() {
		if (DEBUG) Log.v(TAG, "release:")
		mReceiver.release()
	}

	/**
	 * 1回だけキャプチャ要求
	 */
	fun trigger() {
		if (DEBUG) Log.v(TAG, "trigger:")
		mReader.trigger()
	}

	/**
	 * 指定した条件でキャプチャ要求
	 * @param numCaptures キャプチャ回数, -1: 無制限, 0: 無効, 1以上: 指定回数
	 * @param intervalsMs 複数回キャプチャする場合の周期(ミリ秒)
	 */
	fun trigger(numCaptures: Int, intervalsMs: Long) {
		if (DEBUG) Log.v(TAG, "trigger:num=$numCaptures,intervals=$intervalsMs")
		mReader.trigger(numCaptures, intervalsMs)
	}

	/**
	 * キャプチャ中であればキャンセルする
	 */
	fun cancel() {
		if (DEBUG) Log.v(TAG, "cancel:")
		mReader.cancel()
	}

	/**
	 * GLManagerを取得する
	 * @return
	 */
	fun getGLManager(): GLManager {
		return mReceiver.glManager
	}

	/**
	 * 映像サイズ(幅)を取得
	 * @return
	 */
	fun getWidth(): Int {
		return mReceiver.width
	}

	/**
	 * 映像サイズ(高さ)を取得
	 * @return
	 */
	fun getHeight(): Int {
		return mReceiver.height
	}

	@Throws(IllegalStateException::class)
	fun getSurface(): Surface {
		return mReceiver.surface
	}

	@Throws(IllegalStateException::class)
	fun getSurfaceTexture(): SurfaceTexture {
		return mReceiver.surfaceTexture
	}

	companion object {
		private const val DEBUG = false	// set false on production
		private val TAG = GLSurfaceCapture::class.java.simpleName
	}
}