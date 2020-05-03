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
import android.view.View
import com.serenegiant.glutils.GLUtils
import com.serenegiant.graphics.SurfaceDrawable
import java.lang.UnsupportedOperationException

/**
 * SurfaceDrawableを使ってカメラ映像を表示するImageView実装
 */
class CameraImageView @JvmOverloads constructor(
	context: Context?, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
		: ZoomImageView(context, attrs, defStyleAttr), ICameraView {

	private val mCameraDelegator: CameraDelegator
	private val mDrawable: SurfaceDrawable

	init {
		if (DEBUG) Log.v(TAG, "コンストラクタ:")

		mCameraDelegator = CameraDelegator(this@CameraImageView,
			CameraDelegator.DEFAULT_PREVIEW_WIDTH, CameraDelegator.DEFAULT_PREVIEW_HEIGHT,
			object : CameraDelegator.ICameraRenderer {
				override fun hasSurface(): Boolean {
					if (DEBUG) Log.v(TAG, "hasSurface:")
					return this@CameraImageView.hasSurface()
				}

				override fun getInputSurface(): SurfaceTexture {
					if (DEBUG) Log.v(TAG, "updateViewport:")
					return this@CameraImageView.getInputSurfaceTexture()
				}

				override fun onPreviewSizeChanged(width: Int, height: Int) {
					if (DEBUG) Log.v(TAG, String.format("onPreviewSizeChanged:(%dx%d)", width, height))
					this@CameraImageView.onPreviewSizeChanged(width, height)
				}
			}
		)

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

	override fun getView() : View {
		return this
	}

	override fun onResume() {
		if (DEBUG) Log.v(TAG, "onResume:")
		mCameraDelegator.onResume()
	}

	override fun onPause() {
		if (DEBUG) Log.v(TAG, "onPause:")
		mCameraDelegator.onPause()
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
		super.setScaleMode(mode)
		mCameraDelegator.scaleMode = mode
	}

	override fun getVideoWidth(): Int {
		if (DEBUG) Log.v(TAG, "getVideoWidth:")
		return mCameraDelegator.previewWidth
	}

	override fun getVideoHeight(): Int {
		if (DEBUG) Log.v(TAG, "getVideoHeight:")
		return mCameraDelegator.previewHeight
	}

	override fun addSurface(id: Int, surface: Any, isRecordable: Boolean) {
		throw UnsupportedOperationException()
	}

	override fun removeSurface(id: Int) {
		throw UnsupportedOperationException()
	}

	fun getInputSurfaceTexture(): SurfaceTexture {
		return mDrawable.surfaceTexture
	}

	fun hasSurface(): Boolean {
		return mDrawable.isSurfaceValid
	}

	fun onPreviewSizeChanged(width: Int, height: Int) {
		mDrawable.setBounds(0, 0, width, height)
		setAspectRatio(width, height)
	}

	companion object {
		private const val DEBUG = true // set false on production
		private val TAG = CameraImageView::class.java.simpleName
	}
}