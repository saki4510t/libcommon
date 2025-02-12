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
import android.graphics.Color
import android.hardware.Camera
import android.util.AttributeSet
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.serenegiant.camera.CameraConst
import com.serenegiant.camera.CameraUtils
import com.serenegiant.glpipeline.MaskPipeline
import com.serenegiant.gl.GLManager
import com.serenegiant.glpipeline.GLPipelineSurfaceSource
import com.serenegiant.glpipeline.SurfaceSourcePipeline
import com.serenegiant.graphics.BitmapHelper
import java.io.IOException
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

/**
 * カメラ映像にマスク処理をして表示するだけのSurfaceView実装
 */
@Suppress("DEPRECATION")
class MaskCameraSurfaceView @JvmOverloads constructor(context: Context?,
		attrs: AttributeSet? = null, defStyleAttr: Int = 0)
		: SurfaceView(context, attrs, defStyleAttr) {

	private var source: SurfaceSourcePipeline? = null
	private var pipeline: MaskPipeline? = null
	private var mHasSurface = false
	private var mCamera: Camera? = null

	/**
	 * コンストラクタ
	 */
	init {
		holder.addCallback(object : SurfaceHolder.Callback {
			override fun surfaceCreated(holder: SurfaceHolder) {
				mHasSurface = true
			}

			override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
				createPipeline();
				pipeline!!.setSurface(holder)
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
			createPipeline();
			startPreview()
		}
	}

	fun onPause() {
		stopPreview()
	}

	private fun createPipeline() {
		if (pipeline == null) {
			val manager = GLManager()
			pipeline = MaskPipeline(manager)
			pipeline!!.setMask(BitmapHelper.genMaskImage(0,
				CameraDelegator.DEFAULT_PREVIEW_WIDTH, CameraDelegator.DEFAULT_PREVIEW_HEIGHT,
				60, Color.BLUE,127, 255))
			val sem = Semaphore(0)
			source = SurfaceSourcePipeline(manager,
				CameraDelegator.DEFAULT_PREVIEW_WIDTH, CameraDelegator.DEFAULT_PREVIEW_HEIGHT,
				object : GLPipelineSurfaceSource.PipelineSourceCallback {

					override fun onCreate(surface: Surface) {
						if (DEBUG) Log.v(TAG, "PipelineSourceCallback#onCreate:$surface")
						sem.release()
					}

					override fun onDestroy() {
						if (DEBUG) Log.v(TAG, "PipelineSourceCallback#onDestroy:")
					}
				})
			sem.tryAcquire(1000, TimeUnit.MILLISECONDS)
			source!!.pipeline = pipeline
		}
	}

	private fun startPreview() {
		if (mCamera == null) {
			try {
				mCamera = CameraUtils.setupCamera(context,
					CameraConst.FACING_BACK,
					CameraDelegator.DEFAULT_PREVIEW_WIDTH, CameraDelegator.DEFAULT_PREVIEW_HEIGHT)
				CameraUtils.setPreviewSurface(mCamera!!, source!!.inputSurface)
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
		if (source != null) {
			source!!.release()
			source = null
		}
		if (pipeline != null) {
			pipeline!!.release()
			pipeline = null
		}
	}

	companion object {
		private const val DEBUG = true // set false on production
		private val TAG = MaskCameraSurfaceView::class.java.simpleName
		private const val CAMERA_ID = 0
	}
}
