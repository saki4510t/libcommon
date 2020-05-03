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

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.GLES20
import android.opengl.Matrix
import android.util.AttributeSet
import android.util.Log
import android.view.Surface
import androidx.annotation.Size
import androidx.annotation.WorkerThread
import com.serenegiant.glpipeline.Distributor
import com.serenegiant.glpipeline.IPipelineSource
import com.serenegiant.glpipeline.IPipelineSource.PipelineSourceCallback
import com.serenegiant.glpipeline.VideoSource
import com.serenegiant.glutils.GLDrawer2D
import com.serenegiant.widget.CameraDelegator.ICameraRenderer

/**
 * カメラ映像をVideoSource経由で取得してプレビュー表示するためのICameraView実装
 * GLViewを継承
 * XXX useSharedContext = trueで共有コンテキストを使ったマルチスレッド処理を有効にするとGPUのドライバー内でクラッシュする端末がある
 */
class VideoSourceCameraGLView @JvmOverloads constructor(
	context: Context?, attrs: AttributeSet? = null, defStyle: Int = 0)
		: AspectScaledGLView(context, attrs, defStyle), ICameraView {

	private val mCameraDelegator: CameraDelegator
	private val mCameraRenderer: CameraRenderer
	private var mVideoSource: VideoSource? = null
	private var mDistributor: Distributor? = null
	private val mMvpMatrix = FloatArray(16)
	private val mWork = FloatArray(9)

	init {
		if (DEBUG) Log.v(TAG, "コンストラクタ:")
		mCameraRenderer = CameraRenderer()
		mCameraDelegator = CameraDelegator(this@VideoSourceCameraGLView,
			CameraDelegator.DEFAULT_PREVIEW_WIDTH, CameraDelegator.DEFAULT_PREVIEW_HEIGHT,
			mCameraRenderer)
		setRenderer(object : GLRenderer {
			@SuppressLint("WrongThread")
			@WorkerThread
			override fun onSurfaceCreated() {
				if (DEBUG) Log.v(TAG, "onSurfaceCreated:")
				mDrawer = GLDrawer2D.create(isOES3(), true)
				mDrawer!!.setMvpMatrix(mMvpMatrix, 0)
				GLES20.glClearColor(1.0f, 1.0f, 0.0f, 1.0f)
				mVideoSource?.add(mCameraRenderer)
			}

			@WorkerThread
			override fun onSurfaceChanged(format: Int, width: Int, height: Int) {
				mVideoSource!!.resize(width, height)
				mCameraDelegator.startPreview(
					CameraDelegator.DEFAULT_PREVIEW_WIDTH, CameraDelegator.DEFAULT_PREVIEW_HEIGHT)
			}

			@SuppressLint("WrongThread")
			@WorkerThread
			override fun drawFrame() {
				if (mVideoSource != null) {
					handleDraw(mVideoSource!!.texId, mVideoSource!!.texMatrix)
				}
			}

			@WorkerThread
			override fun onSurfaceDestroyed() {
				if (mVideoSource != null) {
					mVideoSource!!.remove(mCameraRenderer)
				}
				if (mDrawer != null) {
					mDrawer!!.release()
					mDrawer = null
				}
			}

			override fun applyTransformMatrix(@Size(min=16) transform: FloatArray) {
				if (mDrawer != null) {
					System.arraycopy(transform, 0, mMvpMatrix, 0, 16)
					mDrawer!!.setMvpMatrix(mMvpMatrix, 0)
				}
			}
		})
		Matrix.setIdentityM(mMvpMatrix, 0)
	}

	/**
	 * ICameraViewの実装
	 */
	@Synchronized
	override fun onResume() {
		if (DEBUG) Log.v(TAG, "onResume:")
		mVideoSource = createVideoSource(
			CameraDelegator.DEFAULT_PREVIEW_WIDTH, CameraDelegator.DEFAULT_PREVIEW_HEIGHT)
		mCameraDelegator.onResume()
	}

	/**
	 * ICameraViewの実装
	 */
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

	/**
	 * ICameraViewの実装
	 */
	override fun addListener(listener: CameraDelegator.OnFrameAvailableListener) {
		mCameraDelegator.addListener(listener)
	}

	/**
	 * ICameraViewの実装
	 */
	override fun removeListener(listener: CameraDelegator.OnFrameAvailableListener) {
		mCameraDelegator.removeListener(listener)
	}

	/**
	 * ICameraViewの実装
	 */
	override fun setScaleMode(mode: Int) {
		mCameraDelegator.scaleMode = mode
		(mCameraDelegator.cameraRenderer as CameraRenderer).updateViewport()
	}

	/**
	 * ICameraViewの実装
	 */
	override fun getScaleMode(): Int {
		return mCameraDelegator.scaleMode
	}

	/**
	 * ICameraViewの実装
	 */
	override fun setVideoSize(width: Int, height: Int) {
		mCameraDelegator.setVideoSize(width, height)
	}

	/**
	 * ICameraViewの実装
	 */
	override fun getVideoWidth(): Int {
		return mCameraDelegator.previewWidth
	}

	/**
	 * ICameraViewの実装
	 */
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

		return VideoSource(getGLManager(), width, height,
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

	private var mDrawer: GLDrawer2D? = null
	private var cnt2 = 0
	/**
	 * 描画処理の実体
	 * レンダリングスレッド上で実行
	 * @param texId
	 * @param texMatrix
	 */
	@WorkerThread
	private fun handleDraw(texId: Int, texMatrix: FloatArray) {
		if (DEBUG && ((++cnt2 % 100) == 0)) Log.v(TAG, "handleDraw:$cnt2")
		// draw to preview screen
		if ((mDrawer != null) && (mVideoSource != null)) {
			mDrawer!!.draw(texId, texMatrix, 0)
		}
		GLES20.glFlush()
	}

	/**
	 * ICameraRendererの実装
	 */
	@SuppressLint("WrongThread")
	private inner class CameraRenderer
		: ICameraRenderer,
			IPipelineSource.OnFrameAvailableListener {

		override fun hasSurface(): Boolean {
			return mVideoSource != null
		}

		override fun onPreviewSizeChanged(width: Int, height: Int) {
			mVideoSource!!.resize(width, height)
		}

		override fun getInputSurface(): SurfaceTexture {

			if (DEBUG) Log.v(TAG, "getInputSurfaceTexture:")
			checkNotNull(mVideoSource)
			return mVideoSource!!.inputSurfaceTexture
		}

		fun updateViewport() {
//			val viewWidth = width
//			val viewHeight = height
//			if ((viewWidth == 0) || (viewHeight == 0)) {
//				if (DEBUG) Log.v(TAG, String.format("updateViewport:view is not ready(%dx%d)",
//					viewWidth, viewHeight))
//				return
//			}
//			if (!mHasSurface || (mTarget == null)) {
//				if (DEBUG) Log.v(TAG, "updateViewport:has no surface")
//				return
//			}
//			mTarget!!.makeCurrent()
//			mTarget!!.setViewPort(0, 0, viewWidth, viewHeight)
//			GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
//			val videoWidth = mCameraDelegator.width.toDouble()
//			val videoHeight = mCameraDelegator.height.toDouble()
//			if ((videoWidth == 0.0) || (videoHeight == 0.0)) {
//				if (DEBUG) Log.v(TAG, String.format("updateViewport:video is not ready(%dx%d)",
//					viewWidth, viewHeight))
//				return
//			}
//			val viewAspect = viewWidth / viewHeight.toDouble()
//			Log.i(TAG, String.format("updateViewport:view(%d,%d)%f,video(%1.0f,%1.0f)",
//				viewWidth, viewHeight, viewAspect, videoWidth, videoHeight))
//			Matrix.setIdentityM(mMvpMatrix, 0)
//			val scaleMode = mCameraDelegator.scaleMode
//			when (scaleMode) {
//				CameraDelegator.SCALE_STRETCH_FIT -> {
//				}
//				CameraDelegator.SCALE_KEEP_ASPECT_VIEWPORT -> {
//					val req = videoWidth / videoHeight
//					val x: Int
//					val y: Int
//					val width: Int
//					val height: Int
//					if (viewAspect > req) {
//						// if view is wider than camera image, calc width of drawing area based on view height
// 						y = 0
//						height = viewHeight
//						width = (req * viewHeight).toInt()
//						x = (viewWidth - width) / 2
//					} else {
//						// if view is higher than camera image, calc height of drawing area based on view width
//						x = 0
//						width = viewWidth
//						height = (viewWidth / req).toInt()
//						y = (viewHeight - height) / 2
//					}
//					// set viewport to draw keeping aspect ration of camera image
//					Log.i(TAG, String.format("updateViewport;xy(%d,%d),size(%d,%d)", x, y, width, height))
//					mTarget!!.setViewPort(0, 0, width, height)
//				}
//				CameraDelegator.SCALE_KEEP_ASPECT, CameraDelegator.SCALE_CROP_CENTER -> {
//					val scale_x = viewWidth / videoWidth
//					val scale_y = viewHeight / videoHeight
//					val scale
//						= if (scaleMode == CameraDelegator.SCALE_CROP_CENTER)
//							 Math.max(scale_x, scale_y) else Math.min(scale_x, scale_y)
//					val width = scale * videoWidth
//					val height = scale * videoHeight
//					Log.i(TAG, String.format("updateViewport:size(%1.0f,%1.0f),scale(%f,%f),mat(%f,%f)",
//						width, height, scale_x, scale_y, width / viewWidth, height / viewHeight))
//					Matrix.scaleM(mMvpMatrix, 0,
//						(width / viewWidth).toFloat(),
//						(height / viewHeight).toFloat(),
//						 1.0f)
//				}
//			}
//			mDrawer!!.setMvpMatrix(mMvpMatrix, 0)
//			mTarget!!.swap()
		}

		/**
		 * IPipelineSource.OnFrameAvailableListenerの実装
		 * @param texId
		 * @param texMatrix
		 */
		override fun onFrameAvailable(texId: Int, texMatrix: FloatArray) {
		}

	} // CameraRenderer

	companion object {
		private const val DEBUG = true // TODO set false on release
		private val TAG = VideoSourceCameraGLView::class.java.simpleName
		/**
		 * 共有GLコンテキストコンテキストを使ったマルチスレッド処理を行うかどうか
		 */
		private const val USE_SHARED_CONTEXT = false
	}

}