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
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.serenegiant.camera.CameraConst
import com.serenegiant.camera.CameraUtils
import com.serenegiant.gl.GLManager
import com.serenegiant.glpipeline.GLPipelineSurfaceSource
import com.serenegiant.glpipeline.MediaEffectPipeline
import com.serenegiant.glpipeline.SurfaceRendererPipeline
import com.serenegiant.glpipeline.SurfaceSourcePipeline
import com.serenegiant.mediaeffect.EffectsBuilder
import java.io.IOException
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

/**
 * カメラ映像にMediaEffectで映像効果を行って表示するだけのSurfaceView実装
 */
@Suppress("DEPRECATION")
class MediaEffectCameraSurfaceView @JvmOverloads constructor(
	context: Context?,
	attrs: AttributeSet? = null, defStyleAttr: Int = 0)
		: SurfaceView(context, attrs, defStyleAttr) {

	private var source: SurfaceSourcePipeline? = null
	private var pipeline: MediaEffectPipeline? = null
	private var preview: SurfaceRendererPipeline? = null
	private var mHasSurface = false
	private var mCamera: Camera? = null

	private var mEffectsBuilder: EffectsBuilder
		= object : EffectsBuilder{}

	/**
	 * コンストラクタ
	 */
	init {
		holder.addCallback(object : SurfaceHolder.Callback {
			override fun surfaceCreated(holder: SurfaceHolder) {
				if (DEBUG) Log.v(TAG, "surfaceCreated:")
				mHasSurface = true
			}

			override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
				if (DEBUG) Log.v(TAG, "surfaceChanged:")
				createPipeline()
				if (USE_PREVIEW) {
					preview?.setSurface(holder)
				} else {
					pipeline?.setSurface(holder)
				}
				startPreview()
			}

			override fun surfaceDestroyed(holder: SurfaceHolder) {
				if (DEBUG) Log.v(TAG, "surfaceDestroyed:")
				mHasSurface = false
				stopPreview()
			}
		})
	}

	fun onResume() {
		if (DEBUG) Log.v(TAG, "onResume:hasSurface=$mHasSurface")
		if (mHasSurface) {
			createPipeline()
			if (USE_PREVIEW) {
				preview?.setSurface(holder)
			} else {
				pipeline?.setSurface(holder)
			}
			startPreview()
		}
	}

	fun onPause() {
		if (DEBUG) Log.v(TAG, "onPause:")
		stopPreview()
	}

	fun changeEffect(effectsBuilder: EffectsBuilder) {
		mEffectsBuilder = effectsBuilder
		pipeline?.changeEffect(mEffectsBuilder)
	}

	private fun createPipeline() {
		if (DEBUG) Log.v(TAG, "createPipeline:pipeline=$pipeline")
		if (pipeline == null) {
			val manager = GLManager()
			pipeline = MediaEffectPipeline(manager, mEffectsBuilder)
			preview = SurfaceRendererPipeline(manager)
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
			if (USE_PREVIEW) {
				pipeline?.pipeline = preview
			}
			source!!.pipeline = pipeline
		}
	}

	private fun startPreview() {
		if (DEBUG) Log.v(TAG, "startPreview:camera=$mCamera")
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
			mCamera?.startPreview()
		}
	}

	private fun stopPreview() {
		if (DEBUG) Log.v(TAG, "stopPreview:")
		mCamera?.stopPreview()
		mCamera?.release()
		mCamera = null
		source?.release()
		source = null
		pipeline?.release()
		pipeline = null
		preview?.release()
		preview = null
	}

	companion object {
		private const val DEBUG = true // set false on production
		private val TAG = MediaEffectCameraSurfaceView::class.java.simpleName
		private const val CAMERA_ID = 0

		/**
		 * SurfaceRendererPipelineを使ってプレビュー表示するかどうか
		 */
		private const val USE_PREVIEW = true
	}
}
