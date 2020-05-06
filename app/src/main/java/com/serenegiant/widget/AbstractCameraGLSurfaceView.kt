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
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.View
import com.serenegiant.glutils.GLDrawer2D
import com.serenegiant.glutils.GLUtils
import com.serenegiant.glutils.IRendererHolder
import com.serenegiant.glutils.IRendererHolder.RenderHolderCallback
import com.serenegiant.graphics.MatrixUtils
import com.serenegiant.utils.HandlerThreadHandler
import com.serenegiant.widget.CameraDelegator.ICameraRenderer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

 /**
 * カメラ映像をIRendererHolder経由で取得してプレビュー表示するためのGLSurfaceView実装
 */
abstract class AbstractCameraGLSurfaceView @JvmOverloads constructor(
	context: Context?, attrs: AttributeSet? = null, defStyle: Int = 0)
		: GLSurfaceView(context, attrs), ICameraView {

	protected val glVersion: Int

	private val mCameraDelegator: CameraDelegator
	/**
	 * 子クラスからIRendererHolderへアクセスできるように
	 * @return
	 */
	protected var rendererHolder: IRendererHolder? = null
		get() {
			return field
		}
		private set

	init {
		if (DEBUG) Log.v(TAG, "コンストラクタ:")
		glVersion = GLUtils.getSupportedGLVersion()
		mCameraDelegator = CameraDelegator(this@AbstractCameraGLSurfaceView,
			CameraDelegator.DEFAULT_PREVIEW_WIDTH, CameraDelegator.DEFAULT_PREVIEW_HEIGHT,
			CameraRenderer())
		@Suppress("LeakingThis")
		setEGLContextClientVersion(glVersion)
		@Suppress("LeakingThis")
		setRenderer(mCameraDelegator.cameraRenderer as CameraRenderer)
		val holder = holder
		holder.addCallback(object : SurfaceHolder.Callback {
			override fun surfaceCreated(holder: SurfaceHolder) {
				if (DEBUG) Log.v(TAG, "surfaceCreated:")
				// do nothing
			}

			override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) { // do nothing
				if (DEBUG) Log.v(TAG, "surfaceChanged:")
				mCameraDelegator.cameraRenderer.updateViewport()
			}

			override fun surfaceDestroyed(holder: SurfaceHolder) {
				if (DEBUG) Log.v(TAG, "surfaceDestroyed:")
				mCameraDelegator.cameraRenderer.onSurfaceDestroyed()
			}
		})
	}

	override fun getView() : View {
		return this
	}

	@Synchronized
	override fun onResume() {
		if (DEBUG) Log.v(TAG, "onResume:")
		super.onResume()
		rendererHolder = createRendererHolder(
			CameraDelegator.DEFAULT_PREVIEW_WIDTH, CameraDelegator.DEFAULT_PREVIEW_HEIGHT,
			mRenderHolderCallback)
		mCameraDelegator.onResume()
	}

	@Synchronized
	override fun onPause() {
		if (DEBUG) Log.v(TAG, "onPause:")
		mCameraDelegator.onPause()
		if (rendererHolder != null) {
			rendererHolder!!.release()
			rendererHolder = null
		}
		super.onPause()
	}

	override fun addListener(listener: CameraDelegator.OnFrameAvailableListener) {
		mCameraDelegator.addListener(listener)
	}

	override fun removeListener(listener: CameraDelegator.OnFrameAvailableListener) {
		mCameraDelegator.removeListener(listener)
	}

	override fun setScaleMode(mode: Int) {
		mCameraDelegator.scaleMode = mode
		(mCameraDelegator.cameraRenderer as CameraRenderer).updateViewport()
	}

	override fun getScaleMode(): Int {
		return mCameraDelegator.scaleMode
	}

	override fun setVideoSize(width: Int, height: Int) {
		mCameraDelegator.setVideoSize(width, height)
	}

	override fun getVideoWidth(): Int {
		return mCameraDelegator.previewWidth
	}

	override fun getVideoHeight(): Int {
		return mCameraDelegator.previewHeight
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
		rendererHolder?.addSurface(id, surface, isRecordable)
	}

	/**
	 * プレビュー表示用Surfaceを除去
	 * @param id
	 */
	@Synchronized
	override fun removeSurface(id: Int) {
		if (DEBUG) Log.v(TAG, "removeSurface:$id")
		rendererHolder?.removeSurface(id)
	}

	/**
	 * IRendererHolderを生成
	 * @param width
	 * @param height
	 * @param callback
	 * @return
	 */
	protected abstract fun createRendererHolder(
		width: Int, height: Int,
		callback: RenderHolderCallback?): IRendererHolder

	/**
	 * IRendererからのコールバックリスナーを実装
	 */
	private val mRenderHolderCallback: RenderHolderCallback
		= object : RenderHolderCallback {

		override fun onCreate(surface: Surface) {
			if (DEBUG) Log.v(TAG, "RenderHolderCallback#onCreate:")
		}

		override fun onFrameAvailable() {
//			if (DEBUG) Log.v(TAG, "RenderHolderCallback#onFrameAvailable:");
			mCameraDelegator.callOnFrameAvailable()
		}

		override fun onDestroy() {
			if (DEBUG) Log.v(TAG, "RenderHolderCallback#onDestroy:")
		}
	}

	/**
	 * GLSurfaceViewのRenderer
	 */
	private inner class CameraRenderer : ICameraRenderer, Renderer, SurfaceTexture.OnFrameAvailableListener {
		// API >= 11
		private var inputSurfaceTexture: SurfaceTexture? = null // API >= 11
		private var hTex = 0
		private var mDrawer: GLDrawer2D? = null
		private val mStMatrix = FloatArray(16)
		private val mMvpMatrix = FloatArray(16)
		private var mHasSurface = false
		@Volatile
		private var requestUpdateTex = false

		init {
			if (DEBUG) Log.v(TAG, "CameraRenderer:")
			Matrix.setIdentityM(mMvpMatrix, 0)
		}

		override fun onSurfaceCreated(unused: GL10, config: EGLConfig) {
			if (DEBUG) Log.v(TAG, "CameraRenderer#onSurfaceCreated:")
			// This renderer required OES_EGL_image_external extension
			val extensions = GLES20.glGetString(GLES20.GL_EXTENSIONS) // API >= 8
//			if (DEBUG) Log.i(TAG, "onSurfaceCreated:Gl extensions: " + extensions);
			if (!extensions.contains("OES_EGL_image_external")) {
				throw RuntimeException("This system does not support OES_EGL_image_external.")
			}
			val isOES3 = extensions.contains("GL_OES_EGL_image_external_essl3")
			mDrawer = GLDrawer2D.create(isOES3, true)
			// create texture ID
			hTex = mDrawer!!.initTex()
			// create SurfaceTexture with texture ID.
			inputSurfaceTexture = SurfaceTexture(hTex)
			inputSurfaceTexture!!.setDefaultBufferSize(
				mCameraDelegator.requestWidth, mCameraDelegator.requestHeight)
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				inputSurfaceTexture!!.setOnFrameAvailableListener(
					this, HandlerThreadHandler.createHandler(TAG))
			} else {
				inputSurfaceTexture!!.setOnFrameAvailableListener(this)
			}
			// clear screen with yellow color so that you can see rendering rectangle
			GLES20.glClearColor(1.0f, 1.0f, 0.0f, 1.0f)
			mHasSurface = true
			// create object for preview display
			mDrawer!!.setMvpMatrix(mMvpMatrix, 0)
			addSurface(1, Surface(inputSurfaceTexture), false)
		}

		override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
			if (DEBUG) Log.v(TAG, String.format("CameraRenderer#onSurfaceChanged:(%d,%d)",
				width, height))
			// if at least with or height is zero, initialization of this view is still progress.
			if ((width == 0) || (height == 0)) {
				return
			}
			updateViewport()
			mCameraDelegator.startPreview(
				CameraDelegator.DEFAULT_PREVIEW_WIDTH, CameraDelegator.DEFAULT_PREVIEW_HEIGHT)
		}

		/**
		 * when GLSurface context is soon destroyed
		 */
		fun onSurfaceDestroyed() {
			if (DEBUG) Log.v(TAG, "CameraRenderer#onSurfaceDestroyed:")
			mHasSurface = false
			removeSurface(1)
			release()
		}

		override fun hasSurface(): Boolean {
			return mHasSurface
		}

		override fun onPreviewSizeChanged(width: Int, height: Int) {
			inputSurfaceTexture?.setDefaultBufferSize(width, height)
		}

		override fun getInputSurface(): SurfaceTexture {
			if (DEBUG) Log.v(TAG, "getInputSurfaceTexture:")
			checkNotNull(rendererHolder)
			return rendererHolder!!.surfaceTexture
		}

		fun release() {
			if (DEBUG) Log.v(TAG, "CameraRenderer#release:")
			if (mDrawer != null) {
				mDrawer!!.deleteTex(hTex)
				mDrawer!!.release()
				mDrawer = null
			}
			if (inputSurfaceTexture != null) {
				inputSurfaceTexture!!.release()
				inputSurfaceTexture = null
			}
		}

		private var cnt = 0
		/**
		 * drawing to GLSurface
		 * we set renderMode to GLSurfaceView.RENDERMODE_WHEN_DIRTY,
		 * this method is only called when #requestRender is called(= when texture is required to update)
		 * if you don't set RENDERMODE_WHEN_DIRTY, this method is called at maximum 60fps
		 */
		override fun onDrawFrame(unused: GL10) {
			if (DEBUG && ++cnt % 1000 == 0) Log.v(TAG, "onDrawFrame::$cnt")
			GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
			if (requestUpdateTex && (inputSurfaceTexture != null)) {
				requestUpdateTex = false
				// update texture(came from camera)
				inputSurfaceTexture!!.updateTexImage()
				// get texture matrix
				inputSurfaceTexture!!.getTransformMatrix(mStMatrix)
			}
			// draw to preview screen
			mDrawer?.draw(hTex, mStMatrix, 0)
		}

		fun updateViewport() {
			queueEvent { updateViewportOnGLThread() }
		}

		fun updateViewportOnGLThread() {
			val viewWidth = width
			val viewHeight = height
			if ((viewWidth == 0) || (viewHeight == 0)) {
				if (DEBUG) Log.v(TAG, String.format("updateViewport:view is not ready(%dx%d)",
					viewWidth, viewHeight))
				return
			}
			GLES20.glViewport(0, 0, viewWidth, viewHeight)
			GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
			val videoWidth = mCameraDelegator.previewWidth.toDouble()
			val videoHeight = mCameraDelegator.previewHeight.toDouble()
			if ((videoWidth == 0.0) || (videoHeight == 0.0)) {
				if (DEBUG) Log.v(TAG, String.format("updateViewport:video is not ready(%dx%d)",
					viewWidth, viewHeight))
				return
			}
			val viewAspect = viewWidth / viewHeight.toDouble()
			val scaleMode = mCameraDelegator.scaleMode
			Log.i(TAG, String.format("updateViewport:view(%d,%d)%f,video(%1.0f,%1.0f),scaleMode=%d",
				viewWidth, viewHeight, viewAspect, videoWidth, videoHeight, scaleMode))
			Matrix.setIdentityM(mMvpMatrix, 0)
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
					Log.i(TAG, String.format("updateViewport:xy(%d,%d),size(%d,%d)",
						x, y, width, height))
					GLES20.glViewport(x, y, width, height)
				}
				CameraDelegator.SCALE_KEEP_ASPECT,
				CameraDelegator.SCALE_CROP_CENTER -> {
					val scaleX = viewWidth / videoWidth
					val scaleY = viewHeight / videoHeight
					val scale
						= if (scaleMode == CameraDelegator.SCALE_CROP_CENTER)
							Math.max(scaleX, scaleY) else Math.min(scaleX, scaleY)
					val width = scale * videoWidth
					val height = scale * videoHeight
					Log.i(TAG, String.format("updateViewport:size(%1.0f,%1.0f),scale(%f,%f),mat(%f,%f)",
						width, height, scaleX, scaleY, width / viewWidth, height / viewHeight))
					Matrix.scaleM(mMvpMatrix, 0,
						(width / viewWidth).toFloat(),
						(height / viewHeight).toFloat(),
						1.0f)
				}
			}
			Log.v(TAG, "updateViewport:" + MatrixUtils.toGLMatrixString(mMvpMatrix))
			mDrawer?.setMvpMatrix(mMvpMatrix, 0)
		}

		/**
		 * OnFrameAvailableListenerインターフェースの実装
		 * @param st
		 */
		override fun onFrameAvailable(st: SurfaceTexture) {
			requestUpdateTex = true
		}

	} // CameraRenderer

	 companion object {
		private const val DEBUG = true // TODO set false on release
		private val TAG = AbstractCameraGLSurfaceView::class.java.simpleName
	}
}