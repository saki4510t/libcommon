package com.serenegiant.widget
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

import android.content.Context
import android.hardware.Camera
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.serenegiant.camera.CameraConst
import com.serenegiant.camera.CameraUtils
import java.io.IOException

/**
 * カメラ映像を流し込んで表示するだけのSurfaceView実装
 */
@Suppress("DEPRECATION")
class CameraSurfaceView @JvmOverloads constructor(context: Context?,
	attrs: AttributeSet? = null, defStyleAttr: Int = 0)
		: SurfaceView(context, attrs, defStyleAttr) {

	private var mHasSurface = false
	private var mCamera: Camera? = null
	val cameraRotation = 0

	/**
	 * コンストラクタ
	 */
	init {
		holder.addCallback(object : SurfaceHolder.Callback {
			override fun surfaceCreated(holder: SurfaceHolder) {
				mHasSurface = true
			}

			override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
				startPreview()
			}

			override fun surfaceDestroyed(holder: SurfaceHolder) {
				mHasSurface = false
				stopPreview()
			}
		})
	}

	fun onResume() {
		if (mHasSurface) {
			startPreview()
		}
	}

	fun onPause() {
		stopPreview()
	}

	private fun startPreview() {
		if (mCamera == null) {
			try {
				mCamera = CameraUtils.setupCamera(context,
					CameraConst.FACING_BACK,
					CameraDelegator.DEFAULT_PREVIEW_WIDTH, CameraDelegator.DEFAULT_PREVIEW_HEIGHT)
				CameraUtils.setPreviewSurface(mCamera!!, this)
			} catch (e: IOException) {
				Log.w(TAG, e)
				mCamera = null
			}
			if (mCamera != null) {
				mCamera!!.startPreview()
			}
		}
	}

	private fun stopPreview() {
		if (mCamera != null) {
			mCamera!!.stopPreview()
			mCamera!!.release()
			mCamera = null
		}
	}

	companion object {
		private const val DEBUG = true // set false on production
		private val TAG = CameraSurfaceView::class.java.simpleName
		private const val CAMERA_ID = 0
	}
}
