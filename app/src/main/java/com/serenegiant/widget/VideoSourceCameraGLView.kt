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
import android.opengl.GLES20
import android.opengl.Matrix
import android.os.Handler
import android.util.AttributeSet
import android.util.Log
import android.view.Choreographer
import android.view.Choreographer.FrameCallback
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.annotation.WorkerThread
import com.serenegiant.glpipeline.Distributor
import com.serenegiant.glpipeline.IPipelineSource
import com.serenegiant.glpipeline.IPipelineSource.PipelineSourceCallback
import com.serenegiant.glpipeline.VideoSource
import com.serenegiant.glutils.GLContext
import com.serenegiant.glutils.GLDrawer2D
import com.serenegiant.glutils.GLManager
import com.serenegiant.glutils.ISurface
import com.serenegiant.widget.CameraDelegator.ICameraRenderer
import com.serenegiant.widget.CameraDelegator.ICameraView

/**
 * カメラ映像をVideoSource経由で取得してプレビュー表示するためのICameraGLView実装
 * SurfaceViewを継承
 * XXX useSharedContext = trueで共有コンテキストを使ったマルチスレッド処理を有効にするとGPUのドライバー内でクラッシュする端末がある
 */
class VideoSourceCameraGLView @JvmOverloads constructor(
	context: Context?, attrs: AttributeSet? = null, defStyle: Int = 0)
		: SurfaceView(context, attrs), ICameraView {

	private val mCameraDelegator: CameraDelegator
	private val mGLManager: GLManager
	private val mGLContext: GLContext
	private val mGLHandler: Handler
	private val mCameraRenderer: CameraRenderer
	private var mVideoSource: VideoSource? = null
	private var mDistributor: Distributor? = null

	init {
		if (DEBUG) Log.v(TAG, "コンストラクタ:")
		mGLManager = GLManager()
		mGLContext = mGLManager.glContext
		mGLHandler = mGLManager.glHandler
		mCameraRenderer = CameraRenderer()
		mCameraDelegator = object : CameraDelegator(this@VideoSourceCameraGLView,
			DEFAULT_PREVIEW_WIDTH, DEFAULT_PREVIEW_HEIGHT) {
			override fun getInputSurfaceTexture(): SurfaceTexture {

				if (DEBUG) Log.v(TAG, "getInputSurfaceTexture:")
				checkNotNull(mVideoSource)
				return mVideoSource!!.inputSurfaceTexture
			}

			override fun createCameraRenderer(parent: CameraDelegator): ICameraRenderer {
				if (DEBUG) Log.v(TAG, "createCameraRenderer:")
				return mCameraRenderer
			}
		}
		val holder = holder
		holder.addCallback(object : SurfaceHolder.Callback {

			override fun surfaceCreated(holder: SurfaceHolder) {
				if (DEBUG) Log.v(TAG, "surfaceCreated:")
				// do nothing
				queueEvent(Runnable {
					mCameraRenderer.onSurfaceCreated(holder.surface)
				})
			}

			override fun surfaceChanged
				(holder: SurfaceHolder, format: Int,
				width: Int, height: Int) {

				if (DEBUG) Log.v(TAG, "surfaceChanged:")
				queueEvent(Runnable {
					mCameraRenderer.onSurfaceChanged(width, height)
				 })
			}

			override fun surfaceDestroyed(holder: SurfaceHolder) {
				if (DEBUG) Log.v(TAG, "surfaceDestroyed:")
				queueEvent(Runnable {
					mCameraRenderer.onSurfaceDestroyed()
				 })
			}
		})
	}

	@Synchronized
	override fun onResume() {
		if (DEBUG) Log.v(TAG, "onResume:")
		mVideoSource = createVideoSource(
			CameraDelegator.DEFAULT_PREVIEW_WIDTH, CameraDelegator.DEFAULT_PREVIEW_HEIGHT)
		mCameraDelegator.onResume()
	}

	@Synchronized
	override fun onPause() {
		if (DEBUG) Log.v(TAG, "onPause:")
		mCameraDelegator.onPause()
		if (mDistributor != null) {
			mDistributor!!.release()
			mDistributor = null
		}
		if (mVideoSource != null) {
			mVideoSource!!.release()
			mVideoSource = null
		}
	}

	override fun queueEvent(task: Runnable) {
		mGLHandler.post(task)
	}

	override fun addListener(listener: CameraDelegator.OnFrameAvailableListener) {
		mCameraDelegator.addListener(listener)
	}

	override fun removeListener(listener: CameraDelegator.OnFrameAvailableListener) {
		mCameraDelegator.removeListener(listener)
	}

	override fun setScaleMode(mode: Int) {
		mCameraDelegator.scaleMode = mode
	}

	override fun getScaleMode(): Int {
		return mCameraDelegator.scaleMode
	}

	override fun setVideoSize(width: Int, height: Int) {
		mCameraDelegator.setVideoSize(width, height)
	}

	override fun getVideoWidth(): Int {
		return mCameraDelegator.width
	}

	override fun getVideoHeight(): Int {
		return mCameraDelegator.height
	}

	/**
	 * プレビュー表示用Surfaceを追加
	 * @param id
	 * @param surface
	 * @param isRecordable
	 */
	@Synchronized
	override fun addSurface(
		id: Int, surface: Any,
		isRecordable: Boolean) {

		if (DEBUG) Log.v(TAG, "addSurface:$id")
		if (mDistributor == null) {
			mDistributor = Distributor(mVideoSource!!)
		}
		mDistributor!!.addSurface(id, surface, isRecordable)
	}

	/**
	 * プレビュー表示用Surfaceを除去
	 * @param id
	 */
	@Synchronized
	override fun removeSurface(id: Int) {
		if (DEBUG) Log.v(TAG, "removeSurface:$id")
		mDistributor?.removeSurface(id)
	}

	/**
	 * VideoSourceインスタンスを生成
	 * @param width
	 * @param height
	 * @return
	 */
	protected fun createVideoSource(
		width: Int, height: Int): VideoSource {

		return VideoSource(mGLManager, width, height,
			object : PipelineSourceCallback {

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
	 * ICameraRendererの実装
	 */
	private inner class CameraRenderer
		: ICameraRenderer, Runnable, FrameCallback,
			IPipelineSource.OnFrameAvailableListener {

		private var mTarget: ISurface? = null
		private var mDrawer: GLDrawer2D? = null
		private val mMvpMatrix = FloatArray(16)
		@Volatile
		private var mHasSurface = false

		/**
		 * コンストラクタ
		 */
		init {
			if (DEBUG) Log.v(TAG, "CameraRenderer:")
			Matrix.setIdentityM(mMvpMatrix, 0)
		}

		@WorkerThread
		fun onSurfaceCreated(surface: Surface) {
			if (DEBUG) Log.v(TAG, "CameraRenderer#onSurfaceCreated:$surface")
			GLES20.glClearColor(1.0f, 1.0f, 0.0f, 1.0f)
			// This renderer required OES_EGL_image_external extension
			val extensions = GLES20.glGetString(GLES20.GL_EXTENSIONS) // API >= 8
//			if (DEBUG) Log.i(TAG, "onSurfaceCreated:Gl extensions: " + extensions);
			if (!extensions.contains("OES_EGL_image_external")) {
				throw RuntimeException("This system does not support OES_EGL_image_external.")
			}
			mDrawer = GLDrawer2D.create(mGLContext.isGLES3, true)
			// clear screen with yellow color so that you can see rendering rectangle
			// create object for preview display
			mDrawer!!.setMvpMatrix(mMvpMatrix, 0)
			mTarget = mGLManager.egl.createFromSurface(surface)
			mHasSurface = true
			GLES20.glClearColor(1.0f, 1.0f, 0.0f, 1.0f)
			if (USE_CHOREOGRAPHER) {
				mGLManager.postFrameCallbackDelayed(this, 3)
			} else {
				mVideoSource?.add(this)
			}
		}

		@WorkerThread
		fun onSurfaceChanged(width: Int, height: Int) {
			if (DEBUG) Log.v(TAG, String.format("CameraRenderer#onSurfaceChanged:(%d,%d)",
				width, height))
			// if at least with or height is zero, initialization of this view is still progress.
			if ((width == 0) || (height == 0)) {
				return
			}
			mVideoSource!!.resize(width, height)
			updateViewport()
			mCameraDelegator.startPreview(width, height)
		}

		/**
		 * when GLSurface context is soon destroyed
		 */
		@WorkerThread
		override fun onSurfaceDestroyed() {
			if (DEBUG) Log.v(TAG, "CameraRenderer#onSurfaceDestroyed:")
			mHasSurface = false
			mGLManager.removeFrameCallback(this)
			if (mTarget != null) {
				mTarget!!.release()
				mTarget = null
			}
			if (mVideoSource != null) {
				mVideoSource!!.remove(this)
			}
			release()
		}

		override fun hasSurface(): Boolean {
			return false
		}

		override fun onPreviewSizeChanged(width: Int, height: Int) {
			mVideoSource!!.resize(width, height)
		}

		fun release() {
			if (DEBUG) Log.v(TAG, "CameraRenderer#release:")
			if (mDrawer != null) {
				mDrawer!!.release()
				mDrawer = null
			}
		}

		override fun updateViewport() {
			val viewWidth = width
			val viewHeight = height
			if ((viewWidth == 0) || (viewHeight == 0)) {
				if (DEBUG) Log.v(TAG, String.format("updateViewport:view is not ready(%dx%d)",
					viewWidth, viewHeight))
				return
			}
			if (!mHasSurface || (mTarget == null)) {
				if (DEBUG) Log.v(TAG, "updateViewport:has no surface")
				return
			}
			mTarget!!.makeCurrent()
			mTarget!!.setViewPort(0, 0, viewWidth, viewHeight)
			GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
			val videoWidth = mCameraDelegator.width.toDouble()
			val videoHeight = mCameraDelegator.height.toDouble()
			if ((videoWidth == 0.0) || (videoHeight == 0.0)) {
				if (DEBUG) Log.v(TAG, String.format("updateViewport:video is not ready(%dx%d)",
					viewWidth, viewHeight))
				return
			}
			val viewAspect = viewWidth / viewHeight.toDouble()
			Log.i(TAG, String.format("updateViewport:view(%d,%d)%f,video(%1.0f,%1.0f)",
				viewWidth, viewHeight, viewAspect, videoWidth, videoHeight))
			Matrix.setIdentityM(mMvpMatrix, 0)
			val scaleMode = mCameraDelegator.scaleMode
			when (scaleMode) {
				CameraDelegator.SCALE_STRETCH_FIT -> {
				}
				CameraDelegator.SCALE_KEEP_ASPECT_VIEWPORT -> {
					val req = videoWidth / videoHeight
					val x: Int
					val y: Int
					val width: Int
					val height: Int
					if (viewAspect > req) {
						// if view is wider than camera image, calc width of drawing area based on view height
 						y = 0
						height = viewHeight
						width = (req * viewHeight).toInt()
						x = (viewWidth - width) / 2
					} else {
						// if view is higher than camera image, calc height of drawing area based on view width
						x = 0
						width = viewWidth
						height = (viewWidth / req).toInt()
						y = (viewHeight - height) / 2
					}
					// set viewport to draw keeping aspect ration of camera image
					Log.i(TAG, String.format("updateViewport;xy(%d,%d),size(%d,%d)", x, y, width, height))
					mTarget!!.setViewPort(0, 0, width, height)
				}
				CameraDelegator.SCALE_KEEP_ASPECT, CameraDelegator.SCALE_CROP_CENTER -> {
					val scale_x = viewWidth / videoWidth
					val scale_y = viewHeight / videoHeight
					val scale
						= if (scaleMode == CameraDelegator.SCALE_CROP_CENTER)
							 Math.max(scale_x, scale_y) else Math.min(scale_x, scale_y)
					val width = scale * videoWidth
					val height = scale * videoHeight
					Log.i(TAG, String.format("updateViewport:size(%1.0f,%1.0f),scale(%f,%f),mat(%f,%f)",
						width, height, scale_x, scale_y, width / viewWidth, height / viewHeight))
					Matrix.scaleM(mMvpMatrix, 0,
						(width / viewWidth).toFloat(),
						(height / viewHeight).toFloat(),
						 1.0f)
				}
			}
			mDrawer!!.setMvpMatrix(mMvpMatrix, 0)
			mTarget!!.swap()
		}

//		private var cnt;
		private var cnt2 = 0

		/**
		 * IPipelineSource.OnFrameAvailableListenerの実装
		 * @param texId
		 * @param texMatrix
		 */
		override fun onFrameAvailable(texId: Int, texMatrix: FloatArray) {
//			if (DEBUG && ((++cnt % 100) == 0)) Log.v(TAG, "onFrameAvailable::" + cnt);
			if (!USE_CHOREOGRAPHER) {
				mGLHandler.post(this)
			}
		}

		/**
		 * Choreographer.FrameCallbackの実装
		 * @param frameTimeNanos
		 */
		override fun doFrame(frameTimeNanos: Long) {
//			if (DEBUG && ((++cnt % 100) == 0)) Log.v(TAG, "doFrame::" + cnt);
			run()
			Choreographer.getInstance().postFrameCallbackDelayed(this, 3)
		}

		/**
		 * このViewが保持しているレンダリングスレッド上で描画処理を実行するためのRunnableの実装
		 */
		@WorkerThread
		override fun run() {
			if (mHasSurface && (mVideoSource != null)) {
				handleDraw(mVideoSource!!.texId, mVideoSource!!.texMatrix)
			}
			mGLContext.makeDefault()
			GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
			GLES20.glFlush()
		}

		/**
		 * 描画処理の実体
		 * レンダリングスレッド上で実行
		 * @param texId
		 * @param texMatrix
		 */
		@WorkerThread
		private fun handleDraw(texId: Int, texMatrix: FloatArray) {
			if (mTarget != null) {
				if (DEBUG && ((++cnt2 % 100) == 0)) Log.v(TAG, "handleDraw:$cnt2")
				mTarget!!.makeCurrent()
				GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
				// draw to preview screen
				if ((mDrawer != null) && (mVideoSource != null)) {
					mDrawer!!.draw(texId, texMatrix, 0)
				}
				GLES20.glFlush()
				mTarget!!.swap()
			}
		}

	} // CameraRenderer

	companion object {
		private const val DEBUG = false // TODO set false on release
		private val TAG = VideoSourceCameraGLView::class.java.simpleName
		/**
		 * 共有GLコンテキストコンテキストを使ったマルチスレッド処理を行うかどうか
		 */
		private const val USE_SHARED_CONTEXT = false
		/**
		 * Choreographerを使ってテクスチャの描画をするかどうか
		 * XXX Choreographerを使うと数百フレームぐらいでlibGLESv2_adreno.so内でSIGSEGV投げてクラッシュする
		 */
		private const val USE_CHOREOGRAPHER = false
	}

}