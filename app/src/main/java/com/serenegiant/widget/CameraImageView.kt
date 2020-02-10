package com.serenegiant.widget
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2020 saki t_saki@serenegiant.com
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

import android.content.Context
import android.graphics.SurfaceTexture
import android.util.AttributeSet
import android.util.Log
import android.view.Surface
import androidx.appcompat.widget.AppCompatImageView
import com.serenegiant.glutils.GLUtils
import com.serenegiant.graphics.SurfaceDrawable
import com.serenegiant.widget.CameraDelegator.ICameraView

/**
 * SurfaceDrawableを使ってカメラ映像を表示するImageView実装
 */
class CameraImageView @JvmOverloads constructor(
	context: Context?, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
		: AppCompatImageView(context, attrs, defStyleAttr), ICameraView {

	private val mCameraDelegator: CameraDelegator
	private val mDrawable: SurfaceDrawable

	init {
		if (DEBUG) Log.v(TAG, "コンストラクタ:")

		mCameraDelegator = object : CameraDelegator(this@CameraImageView,
			DEFAULT_PREVIEW_WIDTH, DEFAULT_PREVIEW_HEIGHT) {

			override fun getInputSurfaceTexture(): SurfaceTexture {
				val st = this@CameraImageView.getInputSurfaceTexture()
				if (DEBUG) Log.v(TAG, "getInputSurfaceTexture:$st")
				return st
			}

			override fun createCameraRenderer(parent: CameraDelegator): ICameraRenderer {
				return object : ICameraRenderer {
					override fun onSurfaceDestroyed() {
						if (DEBUG) Log.v(TAG, "onSurfaceDestroyed:")
					}

					override fun hasSurface(): Boolean {
						if (DEBUG) Log.v(TAG, "hasSurface:")
						return this@CameraImageView.hasSurface()
					}

					override fun updateViewport() {
						if (DEBUG) Log.v(TAG, "updateViewport:")
					}

					override fun onPreviewSizeChanged(width: Int, height: Int) {
						if (DEBUG) Log.v(TAG, String.format("onPreviewSizeChanged:(%dx%d)", width, height))
					}
				}
			}
		}

		mDrawable = SurfaceDrawable(
			CameraDelegator.DEFAULT_PREVIEW_WIDTH,
			CameraDelegator.DEFAULT_PREVIEW_HEIGHT,
			GLUtils.getSupportedGLVersion(),
			object : SurfaceDrawable.Callback {

			override fun onCreateSurface(surface: Surface) {
				if (DEBUG) Log.v(TAG, "onCreateSurface:$surface")
				mCameraDelegator.startPreview(
					CameraDelegator.DEFAULT_PREVIEW_WIDTH, CameraDelegator.DEFAULT_PREVIEW_HEIGHT)
			}

			override fun onDestroySurface() {
				if (DEBUG) Log.v(TAG, "onDestroySurface:")
				mCameraDelegator.stopPreview()
			}
		})
		setImageDrawable(mDrawable)
		scaleType = ScaleType.CENTER_CROP
	}

	override fun onResume() {
		if (DEBUG) Log.v(TAG, "onResume:")
		mCameraDelegator.onResume()
	}

	override fun onPause() {
		if (DEBUG) Log.v(TAG, "onPause:")
		mCameraDelegator.onPause()
	}

	override fun queueEvent(task: Runnable) {
		if (DEBUG) Log.v(TAG, "queueEvent:$task")
	}

	override fun setVideoSize(width: Int, height: Int) {
		if (DEBUG) Log.v(TAG, String.format("setVideoSize:(%dx%d)", width, height))
		mCameraDelegator.setVideoSize(width, height)
	}

	override fun addListener(listener: CameraDelegator.OnFrameAvailableListener) {
		if (DEBUG) Log.v(TAG, "addListener:")
	}

	override fun removeListener(listener: CameraDelegator.OnFrameAvailableListener) {
		if (DEBUG) Log.v(TAG, "removeListener:")
	}

	override fun setScaleMode(mode: Int) {
		if (DEBUG) Log.v(TAG, "setScaleMode:")
		mCameraDelegator.scaleMode = mode
	}

	override fun getScaleMode(): Int {
		if (DEBUG) Log.v(TAG, "getScaleMode:")
		return mCameraDelegator.scaleMode
	}

	override fun getVideoWidth(): Int {
		if (DEBUG) Log.v(TAG, "getVideoWidth:")
		return mCameraDelegator.width
	}

	override fun getVideoHeight(): Int {
		if (DEBUG) Log.v(TAG, "getVideoHeight:")
		return mCameraDelegator.height
	}

	override fun addSurface(id: Int, surface: Any, isRecordable: Boolean) {
		if (DEBUG) Log.v(TAG, "addSurface:")
	}

	override fun removeSurface(id: Int) {
		if (DEBUG) Log.v(TAG, "removeSurface:")
	}

	fun getInputSurfaceTexture(): SurfaceTexture {
		return mDrawable.surfaceTexture
	}

	fun hasSurface(): Boolean {
		return mDrawable.isSurfaceValid
	}

	companion object {
		private const val DEBUG = false // set false on production
		private val TAG = CameraImageView::class.java.simpleName
	}
}