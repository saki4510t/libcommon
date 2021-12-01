package com.serenegiant.widget
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2021 saki t_saki@serenegiant.com
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
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.os.Handler
import android.util.AttributeSet
import android.util.Log
import android.view.Surface
import android.view.View
import com.serenegiant.glpipeline.*
import com.serenegiant.glutils.GLContext
import com.serenegiant.glutils.GLEffect
import com.serenegiant.glutils.GLManager
import com.serenegiant.view.ViewTransformDelegater

/**
 * VideoSourceを使ってカメラ映像を受け取りSurfacePipelineで描画処理を行うZoomAspectScaledTextureView/ICameraView実装
 */
class SimpleVideoSourceCameraTextureView @JvmOverloads constructor(
	context: Context?, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
		: ZoomAspectScaledTextureView(context, attrs, defStyleAttr), ICameraView {

	private val mGLManager: GLManager
	private val mGLContext: GLContext
	private val mGLHandler: Handler
	private val mCameraDelegator: CameraDelegator
	private var mVideoSource: VideoSource? = null
	private var mPipeline: IPipeline? = null

	init {
		if (DEBUG) Log.v(TAG, "コンストラクタ:")

		mGLManager = GLManager()
		mGLContext = mGLManager.glContext
		mGLHandler = mGLManager.glHandler
		setEnableHandleTouchEvent(ViewTransformDelegater.TOUCH_DISABLED)
		mCameraDelegator = CameraDelegator(this@SimpleVideoSourceCameraTextureView,
			CameraDelegator.DEFAULT_PREVIEW_WIDTH, CameraDelegator.DEFAULT_PREVIEW_HEIGHT,
			object : CameraDelegator.ICameraRenderer {
				override fun hasSurface(): Boolean {
					if (DEBUG) Log.v(TAG, "hasSurface:")
					return mVideoSource != null
				}

				override fun getInputSurface(): SurfaceTexture {
					if (DEBUG) Log.v(TAG, "getInputSurfaceTexture:")
					checkNotNull(mVideoSource)
					return mVideoSource!!.inputSurfaceTexture
				}

				override fun onPreviewSizeChanged(width: Int, height: Int) {
					if (DEBUG) Log.v(TAG, String.format("onPreviewSizeChanged:(%dx%d)", width, height))
					mVideoSource!!.resize(width, height)
					setAspectRatio(width, height)
				}
			}
		)

		surfaceTextureListener = object : SurfaceTextureListener {
			override fun onSurfaceTextureAvailable(
				surface: SurfaceTexture, width: Int, height: Int) {

				if (DEBUG) Log.v(TAG, String.format("onSurfaceTextureAvailable:(%dx%d)",
					width, height))
				when (mPipeline) {
					null -> {
						mPipeline = createPipeline(surface)
						mVideoSource!!.setPipeline(mPipeline)
					}
					is SurfacePipeline -> {
						(mPipeline as SurfacePipeline).setSurface(surface, null)
					}
					is EffectPipeline -> {
						(mPipeline as EffectPipeline).setSurface(surface, null)
					}
				}
				mVideoSource!!.resize(width, height)
				mCameraDelegator.startPreview(
					CameraDelegator.DEFAULT_PREVIEW_WIDTH, CameraDelegator.DEFAULT_PREVIEW_HEIGHT)
			}

			override fun onSurfaceTextureSizeChanged(
				surface: SurfaceTexture,
				width: Int, height: Int) {

				if (DEBUG) Log.v(TAG, String.format("onSurfaceTextureSizeChanged:(%dx%d)",
					width, height))
			}

			override fun onSurfaceTextureDestroyed(
				surface: SurfaceTexture): Boolean {

				if (DEBUG) Log.v(TAG, "onSurfaceTextureDestroyed:")
				if (mPipeline is SurfacePipeline) {
					(mPipeline as SurfacePipeline).setSurface(null)
				} else if (mPipeline is EffectPipeline) {
					(mPipeline as EffectPipeline).setSurface(null)
				}
				return true
			}

			override fun onSurfaceTextureUpdated(
				surface: SurfaceTexture) {

//				if (DEBUG) Log.v(TAG, "onSurfaceTextureUpdated:")
			}

		}
	}

	override fun getView() : View {
		return this
	}

	override fun onResume() {
		if (DEBUG) Log.v(TAG, "onResume:")
		mVideoSource = createVideoSource(
			CameraDelegator.DEFAULT_PREVIEW_WIDTH, CameraDelegator.DEFAULT_PREVIEW_HEIGHT)
		mCameraDelegator.onResume()
	}

	override fun onPause() {
		if (DEBUG) Log.v(TAG, "onPause:")
		mCameraDelegator.onPause()
		if (mVideoSource != null) {
			mVideoSource!!.setPipeline(null)
		}
		if (mPipeline != null) {
			mPipeline!!.release()
			mPipeline = null
		}
		if (mVideoSource != null) {
			mVideoSource!!.release()
			mVideoSource = null
		}
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

	override fun getScaleMode(): Int {
		if (DEBUG) Log.v(TAG, "getScaleMode:")
		return mCameraDelegator.scaleMode
	}

	override fun getVideoWidth(): Int {
		if (DEBUG) Log.v(TAG, "getVideoWidth:${mCameraDelegator.previewWidth}")
		return mCameraDelegator.previewWidth
	}

	override fun getVideoHeight(): Int {
		if (DEBUG) Log.v(TAG, "getVideoHeight:${mCameraDelegator.previewHeight}")
		return mCameraDelegator.previewHeight
	}

	override fun addSurface(id: Int, surface: Any, isRecordable: Boolean) {
		when (mPipeline) {
			null -> {
				mPipeline = createPipeline(surface)
				mVideoSource!!.setPipeline(mPipeline)
			}
			is SurfacePipeline -> {
				(mPipeline as SurfacePipeline).setSurface(surface, null)
			}
			is EffectPipeline -> {
				(mPipeline as EffectPipeline).setSurface(surface, null)
			}
		}
	}

	override fun removeSurface(id: Int) {
		when (mPipeline) {
			is SurfacePipeline -> {
				(mPipeline as SurfacePipeline).setSurface(surfaceTexture, null)
			}
			is EffectPipeline -> {
				(mPipeline as EffectPipeline).setSurface(surfaceTexture, null)
			}
		}
	}

	override fun isRecordingSupported(): Boolean {
		return false
	}

	fun isEffectSupported(): Boolean {
		return USE_EFFECT_PIPELINE
	}

	var effect: Int
	get() {
		val pipeline = mPipeline
		if (DEBUG) Log.v(TAG, "getEffect:$pipeline")
		return if (pipeline is EffectPipeline) pipeline.currentEffect else 0
	}

	set(effect) {
		if (DEBUG) Log.v(TAG, "setEffect:$effect")
		if ((effect >= 0) && (effect < GLEffect.EFFECT_NUM)) {
			post {
				val pipeline = mPipeline
				if (pipeline is EffectPipeline) {
					pipeline.setEffect(effect)
				}
			}
		}
	}


	override fun getContentBounds(): RectF {
		if (DEBUG) Log.v(TAG, "getContentBounds:")
		return RectF(0.0f, 0.0f, getVideoWidth().toFloat(), getVideoHeight().toFloat())
	}

	/**
	 * VideoSourceインスタンスを生成
	 * @param width
	 * @param height
	 * @return
	 */
	private fun createVideoSource(
		width: Int, height: Int): VideoSource {

		return VideoSource(mGLManager, width, height,
			object : IPipelineSource.PipelineSourceCallback {

				override fun onCreate(surface: Surface) {
					if (DEBUG) Log.v(TAG, "PipelineSourceCallback#onCreate:$surface")
				}

				override fun onDestroy() {
					if (DEBUG) Log.v(TAG, "PipelineSourceCallback#onDestroy:")
				}
			}
			, USE_SHARED_CONTEXT)
	}

	/**
	 * IPipelineインスタンスを生成
	 * @param surface
	 */
	private fun createPipeline(surface: Any?): IPipeline {
		return if (USE_EFFECT_PIPELINE) {
			EffectPipeline(mGLManager, surface, null)
		} else {
			SurfacePipeline(mGLManager, surface, null)
		}
	}

	companion object {
		private const val DEBUG = true // set false on production
		private val TAG = SimpleVideoSourceCameraTextureView::class.java.simpleName
		/**
		 * 共有GLコンテキストコンテキストを使ったマルチスレッド処理を行うかどうか
		 */
		private const val USE_SHARED_CONTEXT = false

		/**
		 * EffectPipelineを使うかどうか
		 */
		private const val USE_EFFECT_PIPELINE = true

	}
}