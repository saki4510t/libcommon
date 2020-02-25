@file:Suppress("DEPRECATION")

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
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.os.Handler
import android.util.Log
import android.view.View
import androidx.annotation.WorkerThread
import com.serenegiant.camera.CameraConst
import com.serenegiant.camera.CameraUtils
import com.serenegiant.utils.HandlerThreadHandler
import java.io.IOException
import java.util.concurrent.CopyOnWriteArraySet

/**
 * カメラプレビュー処理の委譲クラス
 */
class CameraDelegator(
	view: View,
	width: Int, height: Int,
	cameraRenderer: ICameraRenderer) {

	/**
	 * 映像が更新されたときの通知用コールバックリスナー
	 */
	interface OnFrameAvailableListener {
		fun onFrameAvailable()
	}

	/**
	 * カメラ映像をGLSurfaceViewへ描画するためのGLSurfaceView.Rendererインターフェース
	 */
	interface ICameraRenderer {
		fun hasSurface(): Boolean
		fun onPreviewSizeChanged(width: Int, height: Int)
		/**
		 * カメラ映像受け取り用のSurface/SurfaceHolder/SurfaceTexture/SurfaceViewを取得
		 * @return
		 */
		fun getInputSurface(): Any
	}

//--------------------------------------------------------------------------------

	private val mView: View
	private val mSync = Any()
	val cameraRenderer: ICameraRenderer
	private val mListeners: MutableSet<OnFrameAvailableListener> = CopyOnWriteArraySet()
	private var mCameraHandler: Handler? = null
	/**
	 * カメラ映像幅を取得
	 * @return
	 */
	var requestWidth: Int
		private set
	/**
	 * カメラ映像高さを取得
	 * @return
	 */
	var requestHeight: Int
		private set

	var previewWidth: Int
		private set

	var previewHeight: Int
		private set

	var isPreviewing: Boolean
		private set

	private var mScaleMode = SCALE_STRETCH_FIT
	private var mCamera: Camera? = null
	@Volatile
	private var mResumed = false

	init {
		if (DEBUG) Log.v(TAG, String.format("コンストラクタ:(%dx%d)", width, height))
		mView = view
		@Suppress("LeakingThis")
		this.cameraRenderer = cameraRenderer
		this.requestWidth = width
		this.requestHeight = height
		this.previewWidth = width
		this.previewHeight = height
		isPreviewing = false
	}

	@Throws(Throwable::class)
	protected fun finalize() {
		release()
	}

	/**
	 * 関連するリソースを廃棄する
	 */
	fun release() {
		synchronized(mSync) {
			if (mCameraHandler != null) {
				if (DEBUG) Log.v(TAG, "release:")
				mCameraHandler!!.removeCallbacksAndMessages(null)
				mCameraHandler!!.looper.quit()
				mCameraHandler = null
			}
		}
	}

	/**
	 * GLSurfaceView#onResumeが呼ばれたときの処理
	 */
	fun onResume() {
		if (DEBUG) Log.v(TAG, "onResume:")
		mResumed = true
		if (cameraRenderer.hasSurface()) {
			if (mCameraHandler == null) {
				if (DEBUG) Log.v(TAG, "surface already exist")
				startPreview(requestWidth, requestHeight)
			}
		}
	}

	/**
	 * GLSurfaceView#onPauseが呼ばれたときの処理
	 */
	fun onPause() {
		if (DEBUG) Log.v(TAG, "onPause:")
		mResumed = false
		// just request stop previewing
		stopPreview()
	}

	/**
	 * 映像が更新されたときのコールバックリスナーを登録
	 * @param listener
	 */
	fun addListener(listener: OnFrameAvailableListener?) {
		if (DEBUG) Log.v(TAG, "addListener:$listener")
		if (listener != null) {
			mListeners.add(listener)
		}
	}

	/**
	 * 映像が更新されたときのコールバックリスナーの登録を解除
	 * @param listener
	 */
	fun removeListener(listener: OnFrameAvailableListener) {
		if (DEBUG) Log.v(TAG, "removeListener:$listener")
		mListeners.remove(listener)
	}

	/**
	 * 映像が更新されたときのコールバックを呼び出す
	 */
	fun callOnFrameAvailable() {
		for (listener in mListeners) {
			try {
				listener.onFrameAvailable()
			} catch (e: Exception) {
				mListeners.remove(listener)
			}
		}
	}

	var scaleMode: Int
		/**
		 * 現在のスケールモードを取得
		 * @return
		 */
		get() {
			if (DEBUG) Log.v(TAG, "getScaleMode:$mScaleMode")
			return mScaleMode
		}
		/**
		 * スケールモードをセット
		 * @param mode
		 */
		set(mode) {
			if (DEBUG) Log.v(TAG, "setScaleMode:$mode")
			if (mScaleMode != mode) {
				mScaleMode = mode
			}
		}

	/**
	 * カメラ映像サイズを変更要求
	 * @param width
	 * @param height
	 */
	fun setVideoSize(width: Int, height: Int) {
		if (DEBUG) Log.v(TAG, String.format("setVideoSize:(%dx%d)", width, height))
		if (requestWidth != width || (requestHeight != height)) {
			requestWidth = width
			requestHeight = height
			// FIXME 既にカメラから映像取得中ならカメラを再設定しないといけない
			if (isPreviewing) {
				stopPreview()
				startPreview(width, height)
			}
		}
	}

	/**
	 * プレビュー開始
	 * @param width
	 * @param height
	 */
	fun startPreview(width: Int, height: Int) {
		if (DEBUG) Log.v(TAG, String.format("startPreview:(%dx%d)", width, height))
		synchronized(mSync) {
			if (mCameraHandler == null) {
				mCameraHandler = HandlerThreadHandler.createHandler("CameraHandler")
			}
			isPreviewing = true
			mCameraHandler!!.post {
				handleStartPreview(width, height)
			}
		}
	}

	/**
	 * プレビュー終了
	 */
	fun stopPreview() {
		if (DEBUG) Log.v(TAG, "stopPreview:$mCamera")
		synchronized(mSync) {
			isPreviewing = false
			if (mCamera != null) {
				mCamera!!.stopPreview()
				if (mCameraHandler != null) {
					mCameraHandler!!.post {
						handleStopPreview()
						release()
					}
				}
			}
		}
	}

//--------------------------------------------------------------------------------
	/**
	 * カメラプレビュー開始の実体
	 * @param width
	 * @param height
	 */
	@SuppressLint("WrongThread")
	@WorkerThread
	private fun handleStartPreview(width: Int, height: Int) {
		if (DEBUG) Log.v(TAG, "CameraThread#handleStartPreview:(${width}x${height})")
		var camera: Camera?
		synchronized(mSync) {
			camera = mCamera
		}
		if (camera == null) {
			// This is a sample project so just use 0 as camera ID.
			// it is better to selecting camera is available
			try {
				val cameraId = CameraUtils.findCamera(CameraConst.FACING_BACK)
				camera = Camera.open(cameraId)
				val params = camera!!.getParameters()
				if (params != null) {
					val focusModes = params.supportedFocusModes
					if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
						params.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO
						if (DEBUG) Log.i(TAG, "handleStartPreview:FOCUS_MODE_CONTINUOUS_VIDEO")
					} else if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
						params.focusMode = Camera.Parameters.FOCUS_MODE_AUTO
						if (DEBUG) Log.i(TAG, "handleStartPreview:FOCUS_MODE_AUTO")
					} else {
						if (DEBUG) Log.i(TAG, "handleStartPreview:Camera does not support autofocus")
					}
					params.setRecordingHint(true)
					CameraUtils.chooseVideoSize(params, width, height)
					val fps = CameraUtils.chooseFps(params, 1.0f, 120.0f)
					// rotate camera preview according to the device orientation
					val degrees = CameraUtils.setupRotation(cameraId, mView, camera!!, params)
					camera!!.setParameters(params)
					// get the actual preview size
					val previewSize = camera!!.getParameters().previewSize
					// 画面の回転状態に合わせてプレビューの映像サイズの縦横を入れ替える
					if (degrees % 180 == 0) {
						previewWidth = previewSize.width
						previewHeight = previewSize.height
					} else {
						previewWidth = previewSize.height
						previewHeight = previewSize.width
					}
					Log.d(TAG, String.format("handleStartPreview:(%dx%d)→rot%d(%dx%d),fps(%d-%d)",
						previewSize.width, previewSize.height,
						degrees, previewWidth, previewHeight,
						fps?.get(0), fps?.get(1)))
					// adjust view size with keeping the aspect ration of camera preview.
					// here is not a UI thread and we should request parent view to execute.
					mView.post(Runnable {
						cameraRenderer.onPreviewSizeChanged(previewWidth, previewHeight)
					})
					// カメラ映像受け取り用Surfaceをセット
					val surface = cameraRenderer.getInputSurface()
					if (surface is SurfaceTexture) {
						surface.setDefaultBufferSize(previewWidth, previewHeight)
					}
					CameraUtils.setPreviewSurface(camera!!, surface)
				}
			} catch (e: IOException) {
				Log.e(TAG, "handleStartPreview:", e)
				if (camera != null) {
					camera!!.release()
					camera = null
				}
			} catch (e: RuntimeException) {
				Log.e(TAG, "handleStartPreview:", e)
				if (camera != null) {
					camera!!.release()
					camera = null
				}
			}
			// start camera preview display
			camera?.startPreview()
			synchronized(mSync) {
				mCamera = camera
			}
		}
	}

	/**
	 * カメラプレビュー終了の実体
	 */
	@WorkerThread
	private fun handleStopPreview() {
		if (DEBUG) Log.v(TAG, "CameraThread#handleStopPreview:")
		synchronized(mSync) {
			if (mCamera != null) {
				mCamera!!.stopPreview()
				mCamera!!.release()
				mCamera = null
			}
		}
	}

	companion object {
		private const val DEBUG = true // TODO set false on release
		private val TAG = CameraDelegator::class.java.simpleName
		const val SCALE_STRETCH_FIT = 0
		const val SCALE_KEEP_ASPECT_VIEWPORT = 1
		const val SCALE_KEEP_ASPECT = 2
		const val SCALE_CROP_CENTER = 3
		const val DEFAULT_PREVIEW_WIDTH = 1280
		const val DEFAULT_PREVIEW_HEIGHT = 720
		private const val TARGET_FPS_MS = 60 * 1000

	}

}