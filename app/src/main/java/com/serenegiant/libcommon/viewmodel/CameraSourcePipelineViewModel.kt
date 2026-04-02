package com.serenegiant.libcommon.viewmodel
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2026 saki t_saki@serenegiant.com
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

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.opengl.Matrix
import android.os.Environment
import android.util.Log
import android.view.OrientationEventListener
import android.view.SurfaceHolder
import android.view.TextureView
import androidx.annotation.RequiresPermission
import androidx.annotation.Size
import androidx.annotation.WorkerThread
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import com.serenegiant.camera.Camera1SourcePipeline
import com.serenegiant.camera.Camera2SourcePipeline
import com.serenegiant.camera.CameraConst
import com.serenegiant.camera.CameraPipelineSource
import com.serenegiant.camera.CameraSize
import com.serenegiant.gl.GLManager
import com.serenegiant.glpipeline.CapturePipeline
import com.serenegiant.glpipeline.SurfaceRendererPipeline
import com.serenegiant.glpipeline.append
import com.serenegiant.mediastore.MediaStoreUtils
import com.serenegiant.system.BuildCheck
import com.serenegiant.utils.FileUtils
import com.serenegiant.view.ViewUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

/**
 * CameraSourcePipelineFragment用のViewModel
 * テスト用にTextureViewでもSurfaceViewでも使えるように
 * TextureView.SurfaceTextureListenerと
 * SurfaceHolder.Callbackの両方を実装
 */
class CameraSourcePipelineViewModel(app: Application)
	: AndroidViewModel(app),
	TextureView.SurfaceTextureListener,
	SurfaceHolder.Callback,
	DefaultLifecycleObserver {

	private val mGLManager = GLManager()
//	private val mRenderer = DrawerPipeline(mGLManager)	// パススルーモード以外のときはCapturePipelineへモデルビュー変換行列を適用してはいけない
	private val mRenderer = SurfaceRendererPipeline(mGLManager)
	private val mCapture: CapturePipeline by lazy { CapturePipeline(mGLManager, mCaptureCallback) }
	private var mSourcePipeline: CameraPipelineSource? = null
	private var mResumed = false
	private var mUpdateOrientationJob: Job? = null
	@Size(value = 16)
	private val mRotationMatrix = FloatArray(16)
	@Size(value = 16)
	private val mWorkMatrix = FloatArray(16)

	/**
	 * 対応解像度一覧
	 */
	private val _supportedSizeList = MutableStateFlow<List<CameraSize>>(emptyList())
	/**
	 * 対応解像度一覧のバインディング用
	 */
	val supportedSizeList = _supportedSizeList.asStateFlow()

	/**
	 * 現在選択されている解像度
 	 */
	private val _currentVideoSize = MutableStateFlow(CameraSize(CameraConst.DEFAULT_WIDTH, CameraConst.DEFAULT_HEIGHT))
	/**
	 * 現在選択されている解像度のバインディング用
	 */
	val currentViewSize = _currentVideoSize.asStateFlow()

	/**
	 * 端末の物理的な回転角度
	 */
	private val _deviceRotation90 = MutableStateFlow(0)
	/**
	 * 端末の物理的な回転角度のバインディング用
	 */
	private val deviceRotation90 = _deviceRotation90.asStateFlow()

	private val _thumbnailDocument = MutableStateFlow<DocumentFile?>(null)
	val thumbnailDocument = _thumbnailDocument.asStateFlow()

	/**
	 * 端末の物理的な向きが変化したときのイベントを取得する(画面の向きが変化したときだけではない)
	 */
	private val mOrientationEventListener = object : OrientationEventListener(app) {
		private var mOrientation90 = -1	// 初回は必ずヒットするように範囲外の値をセット
		override fun onOrientationChanged(orientation: Int) {
			val rotationDegree = when {
				orientation < 45 -> 0		// Surface.ROTATION_0
				orientation < 135 -> 90		// Surface.ROTATION_90
				orientation < 225 -> 180	// Surface.ROTATION_180
				orientation < 315 -> 270	// Surface.ROTATION_270
				else -> 0					// Surface.ROTATION_0
			}
			val orientation90 = ((orientation + 45) / 90 * 90) % 360
			if (mOrientation90 != orientation90) {
				mOrientation90 = orientation90
				_deviceRotation90.value = orientation90
				if (DEBUG) Log.v(TAG, "onOrientationChanged:$orientation=>$orientation90/$rotationDegree")
			}
		}
	}

	/**
	 * CapturePipelineでビットマップとしてキャプチャしたときのコールバック
	 */
	private val mCaptureCallback = object : CapturePipeline.Callback {
		@WorkerThread
		override fun onCapture(bitmap: Bitmap) {
			if (DEBUG) Log.v(TAG, "onCapture:bitmap=$bitmap")
			try {
				val context = getApplication<Application>()
				val ext = "png"
				val outputFile = MediaStoreUtils.getContentDocument(
					context, "image/$ext",
					"${Environment.DIRECTORY_DCIM}/${FileUtils.DIR_NAME}",
					"${FileUtils.getDateTimeString()}.$ext", null
				)
				if (DEBUG) Log.v(TAG, "takePicture: save to $outputFile")
				val output = context.contentResolver.openOutputStream(outputFile.uri)
				if (output != null) {
					try {
						bitmap.compress(Bitmap.CompressFormat.PNG, 80, output)
					} finally {
						output.close()
						MediaStoreUtils.updateContentUri(context, outputFile)
						_thumbnailDocument.value = outputFile
					}
				}
				if (DEBUG) Log.v(TAG, "onCapture:finished")
			} catch (e: Exception) {
				Log.w(TAG, e)
			}
		}

		@WorkerThread
		override fun onError(t: Throwable) {
			Log.w(TAG, t)
		}
	}

//--------------------------------------------------------------------------------
	/**
	 * TextureView.SurfaceTextureListenerの実装
	 */
	@SuppressLint("MissingPermission")
	override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
		if (DEBUG) Log.v(TAG, "onSurfaceTextureAvailable:(${width}x$height)")
		onSurfaceCreated(surface)
	}

	/**
	 * TextureView.SurfaceTextureListenerの実装
	 */
	override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
		if (DEBUG) Log.v(TAG, "onSurfaceTextureSizeChanged:(${width}x$height)")
	}

	/**
	 * TextureView.SurfaceTextureListenerの実装
	 */
	override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
		if (DEBUG) Log.v(TAG, "onSurfaceTextureDestroyed:")
		onSurfaceDestroyed()
		return false
	}

	/**
	 * TextureView.SurfaceTextureListenerの実装
	 */
	override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
//		if (DEBUG) Log.v(TAG, "onSurfaceTextureUpdated:")
	}

//--------------------------------------------------------------------------------
	/**
	 * SurfaceHolder.Callbackの実装
	 */
	override fun surfaceCreated(holder: SurfaceHolder) {
		if (DEBUG) Log.v(TAG, "surfaceCreated:")
		onSurfaceCreated(holder.surface)
	}

	/**
	 * SurfaceHolder.Callbackの実装
	 */
	override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
		if (DEBUG) Log.v(TAG, "surfaceChanged:")
	}

	/**
	 * SurfaceHolder.Callbackの実装
	 */
	override fun surfaceDestroyed(holder: SurfaceHolder) {
		if (DEBUG) Log.v(TAG, "surfaceDestroyed:")
		onSurfaceDestroyed()
	}

//--------------------------------------------------------------------------------
	override fun onCleared() {
		if (DEBUG) Log.v(TAG, "onCleared:")
		mUpdateOrientationJob?.cancel()
		val pipeline = mSourcePipeline
		mSourcePipeline = null
		if (pipeline != null) {
			runBlocking(Dispatchers.IO) {
				pipeline.release()
			}
		}
		mCapture.release()
		mRenderer.release()
		mGLManager.release()
		super.onCleared()
	}

	/**
	 * DefaultLifecycleObserverの実装
	 * @param owner
	 */
	@RequiresPermission(Manifest.permission.CAMERA)
	override fun onResume(owner: LifecycleOwner) {
		if (DEBUG) Log.v(TAG, "onResume:")
		mResumed = true
		mOrientationEventListener.enable()
		if (mRenderer.hasSurface()) {
			val source = mSourcePipeline
			if (source != null) {
				connect(source)
			}
		}
	}

	/**
	 * DefaultLifecycleObserverの実装
	 * @param owner
	 */
	override fun onPause(owner: LifecycleOwner) {
		mResumed = false
		mOrientationEventListener.disable()
		mUpdateOrientationJob?.cancel()
		mRenderer.remove()
		val source = mSourcePipeline
		if (source != null) {
			viewModelScope.launch(Dispatchers.IO) {
				source.disconnect()
			}
		}
		if (DEBUG) Log.v(TAG, "onPause:")
	}

	/**
	 * DefaultLifecycleObserverの実装
	 * @param owner
	 */
	override fun onDestroy(owner: LifecycleOwner) {
		if (DEBUG) Log.v(TAG, "onDestroy:")
	}

	/**
	 * 映像サイズ変更要求
	 * @param sz
	 */
	fun setVideoSize(sz: CameraSize) {
		if (DEBUG) Log.v(TAG, "setVideoSize:$sz")
		val source = mSourcePipeline
		if (source != null) {
			viewModelScope.launch {
				withContext(Dispatchers.IO) {
					source.resize(sz.width, sz.height)
				}
				_currentVideoSize.value = CameraSize(source.width, source.height)
			}
		}
	}

	/**
	 * 静止画撮影要求
 	 */
	fun triggerStillCapture() {
		if (DEBUG) Log.v(TAG, "triggerStillCapture:")
		viewModelScope.launch {
			mCapture.trigger()
		}
	}

	/**
	 * 映像表示用のSurface/SurfaceTextureが生成されたときの処理
	 * @param surface Surface/SurfaceTexture等
	 */
	private fun onSurfaceCreated(surface: Any) {
		if (DEBUG) Log.v(TAG, "onSurfaceCreated:$surface")
		mRenderer.setSurface(surface)
		val source = if (USE_CAMERA2 && BuildCheck.isAPI21()) {
			Camera2SourcePipeline(getApplication(), mGLManager)
		} else {
			Camera1SourcePipeline(getApplication(), mGLManager)
		}
		source.append(mRenderer)
		source.append(mCapture)
		mSourcePipeline = source
		connect(source)
	}

	/**
	 * カメラへ接続開始
	 */
	@SuppressLint("MissingPermission")
	private fun connect(source: CameraPipelineSource) {
		if (DEBUG) Log.v(TAG, "connect:$source")
		if (mResumed) {
			viewModelScope.launch {
				withContext(Dispatchers.IO) {
					source.connect()
				}
				updateOrientation(source, ViewUtils.getRotationDegrees(getApplication<Application>()))
				_supportedSizeList.value = source.supportedSizeList.toList()
			}
			mUpdateOrientationJob = viewModelScope.launch {
				deviceRotation90.collect {
					viewModelScope.launch {
						if (source.isActive) {
							val screenRotationDegree = ViewUtils.getRotationDegrees(getApplication<Application>())
							withContext(Dispatchers.IO) {
								source.updateOrientation(screenRotationDegree)
							}
							updateOrientation(source, screenRotationDegree)
						}
					}
				}
			}
		}
	}

	private fun updateOrientation(source: CameraPipelineSource, screenRotationDegree: Int) {
		val mvpMatrix = source.getMvpMatrix()
		mRenderer.setMvpMatrix(mvpMatrix, 0)
		// XXX SurfaceRendererPipelineとパススルーモードのDrawerPipelineを使うときは
		//     元映像がそのまま引き渡されるのでCapturePipelineへモデルビュー変換行列を
		//     適用しないといけない
		Matrix.setIdentityM(mRotationMatrix, 0)
		if ((screenRotationDegree % 180 != 0) && (source is Camera2SourcePipeline)) {
			// Camera2 APIで画面がランドスケープのときは180度回転してしまうので補正
			Matrix.setRotateM(mRotationMatrix, 0, 180.0f, 0.0f, 0.0f, 1.0f)
		}
		Matrix.multiplyMM(mWorkMatrix, 0, mvpMatrix, 0, mRotationMatrix, 0)
		mCapture.setMvpMatrix(mWorkMatrix, 0)
	}

	/**
	 * 映像表示用のSurfaceが破棄されたときの処理
	 */
	private fun onSurfaceDestroyed() {
		if (DEBUG) Log.v(TAG, "onSurfaceDestroyed:")
		mUpdateOrientationJob?.cancel()
		mRenderer.remove()
		mCapture.remove()
		mRenderer.setSurface(null)
		val pipeline = mSourcePipeline
		mSourcePipeline = null
		if (pipeline != null) {
			runBlocking(Dispatchers.IO) {
				pipeline.disconnect()
				pipeline.release()
			}
		}
	}

	companion object {
		private const val DEBUG = true
		private val TAG = CameraSourcePipelineViewModel::class.java.simpleName

		private const val USE_CAMERA2 = true
	}
}
