package com.serenegiant.camera
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

import android.content.Context
import android.hardware.Camera
import android.opengl.Matrix
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.annotation.Size
import androidx.annotation.WorkerThread
import com.serenegiant.gl.GLManager
import com.serenegiant.glpipeline.GLPipeline
import com.serenegiant.glpipeline.ProxyPipeline
import com.serenegiant.glutils.GLSurfaceReceiver
import com.serenegiant.view.ViewUtils
import java.lang.ref.WeakReference
import kotlin.concurrent.withLock

/**
 * Camera APIを映像ソースとするGLPipelineSource実装
 * useSharedContext=falseなら引数のGLManagerのスレッド上で動作する
 * useSharedContext=trueなら共有コンテキストを使って専用スレッド上で動作する
 * XXX useSharedContext = trueで共有コンテキストを使ったマルチスレッド処理を有効にするとGPUのドライバー内でクラッシュする端末がある
 * @param context
 * @param glManager
 * @param face
 * @param width
 * @param height
 * @param useSharedContext 共有コンテキストを使ってマルチスレッドで処理を行うかどうか
 */
@Suppress("DEPRECATION")
class Camera1SourcePipeline(
	context: Context,
	glManager: GLManager,
	@CameraConst.FaceType face: Int = CameraConst.FACING_BACK,
	width: Int = CameraConst.DEFAULT_WIDTH, height: Int = CameraConst.DEFAULT_HEIGHT,
	useSharedContext: Boolean = false
) : ProxyPipeline(width, height), CameraPipelineSource {
	private val mWeakContext = WeakReference(context)
	/**
	 * GLES環境保持用のGLManager
	 */
	private val mGLManager: GLManager
	/**
	 * 自分用のGLManagerを保持しているかどうか
	 */
	private val mOwnManager: Boolean
	/**
	 * 映像受け取り用のGLSurfaceReceiver
	 */
	private val mReceiver: GLSurfaceReceiver
	@Size(value = 16)
	private val mMvpMatrix: FloatArray = FloatArray(16)

	/**
	 * カメラ情報
	 */
	private val mCameraInfo = CameraInfo()
	/**
	 * 対応解像度一覧
	 */
	private val mSupportedSizeList = mutableListOf<CameraSize>()
	private var mCameraId = -1
	private var mRotation = 0
	private var mCamera: Camera? = null
	private var mIsPreviewing = false

	init {
		if (DEBUG) Log.v(TAG, "コンストラクタ:")
		mOwnManager = useSharedContext
		if (useSharedContext) {
			mGLManager = glManager.createShared(null)
		} else {
			mGLManager = glManager
		}
		mReceiver = object : GLSurfaceReceiver(
			mGLManager, width, height,
			object : DefaultCallback(this) {}
		) {}
		mCameraInfo.set(null, face, 0, width, height)
		Matrix.setIdentityM(mMvpMatrix, 0)
	}

	/**
	 * 映像ソースなので親パイプラインはセットできない
	 */
	override fun setParent(parent: GLPipeline?) {
		super.setParent(parent)
		throw UnsupportedOperationException("Can't set parent to CameraPipelineSource")
	}

	override fun internalRelease() {
		if (DEBUG) Log.v(TAG, "internalRelease:")
		releaseCamera()
		if (isValid) {
			mReceiver.release()
		}
		if (mOwnManager) {
			mGLManager.release()
		}
		mWeakContext.clear()
		super.internalRelease()
	}

	@WorkerThread
	@RequiresPermission(android.Manifest.permission.CAMERA)
	override fun connect() {
		if (DEBUG) Log.v(TAG, "connect:")
		checkValid()
		// カメラ映像取得開始処理
		startPreview()
	}

	@WorkerThread
	override fun disconnect() {
		if (isValid) {
			if (DEBUG) Log.v(TAG, "disconnect:")
			// カメラ映像取得終了処理
			releaseCamera()
		}
	}

	@WorkerThread
	override fun updateOrientation(orientation90: Int) {
		if (DEBUG) Log.v(TAG, "updateOrientation:$orientation90")
		checkValid()
		val camera = mLock.withLock { mCamera }
		if (camera != null) {
			val degrees = ViewUtils.getRotationDegrees(requireContext()) // getScreenRotation(requireContext());
			if (mRotation != degrees) {
				mRotation = degrees
				val isPreviewing: Boolean = mIsPreviewing
				if (isPreviewing) {
					stopPreview()
				}
				try {
					internalSetOrientation(camera, degrees)
				} finally {
					if (isPreviewing) {
						startPreview()
					}
				}
			}
		}
	}

	override fun getMvpMatrix(): FloatArray {
		return mMvpMatrix
	}

	@CameraConst.FaceType
	override val face: Int
		get() = mCameraInfo.face

	/**
	 * 対応解像度一覧
	 * カメラオープン中のみ有うこう
	 */
	override val supportedSizeList: List<CameraSize>
		get() = mSupportedSizeList.toList()

	@WorkerThread
	override fun resize(width: Int, height: Int) {
		if (DEBUG) Log.v(TAG, "resize:(${width}x$height)")
		if ((mCameraInfo.width == width) && (mCameraInfo.height == height)) {
			return
		}
		checkValid()
		super.resize(width, height)
		resizeCamera(width, height)
	}

	override fun isValid(): Boolean {
		return super.isValid() && mGLManager.isValid() && mReceiver.isValid
	}

	override fun isActive(): Boolean {
		return mLock.withLock {
			// 破棄されていない && 子と繋がっている && カメラ映像取得中
			isValid && (pipeline != null) && (mCamera != null)
		}
	}

	override fun getGLManager(): GLManager {
		return mGLManager
	}

	override fun getTexId(): Int {
		return mReceiver.texId
	}

	override fun getTexMatrix(): FloatArray {
		return mReceiver.texMatrix
	}

//--------------------------------------------------------------------------------
	/**
	 * Context取得用のヘルパーメソッド
	 * @return
	 * @throws IllegalStateException
	 */
	@Throws(IllegalStateException::class)
	private fun requireContext(): Context {
		val result = mWeakContext.get() ?: throw IllegalStateException()
		return result
	}

	@Throws(IllegalStateException::class)
	private fun checkValid() {
		check(mGLManager.isValid()) { "Already released" }
	}

	private fun swap(newCamera: Camera?): Camera? {
		return mLock.withLock {
			val c = mCamera
			mCamera = newCamera
			c
		}
	}

	@WorkerThread
	private fun openCamera(): Camera {
		val c = mLock.withLock { mCamera }
		if (DEBUG) Log.v(TAG, "openCamera:$c")
		if (c == null) {
			val cameraId = CameraUtils.findCamera(face)
			if (DEBUG) Log.v(TAG, "openCamera:open $cameraId")
			val camera = Camera.open(cameraId)
			if (camera != null) {
				val context = requireContext()
				mCameraId = cameraId
				mRotation = ViewUtils.getRotationDegrees(context)
				CameraUtils.setupCamera(context, cameraId, camera, width, height)
				updateSupportedSizeList(camera)
				onResizeCamera(camera)
				internalSetOrientation(camera, mRotation)
				swap(camera)
				return camera
			}
		} else {
			return c
		}
		throw IllegalArgumentException()
	}

	/**
	 * 対応解像度一覧を更新する
	 * @param camera
	 */
	@WorkerThread
	private fun updateSupportedSizeList(camera: Camera) {
		if (DEBUG) Log.v(TAG, "updateSupportedSizeList:")
		mSupportedSizeList.clear()
		val params = camera.parameters
		for (sz in params.supportedPreviewSizes) {
			mSupportedSizeList.add(sz.toCameraSize())
		}
	}

	/**
	 * カメラオープン中ならカメラ映像のサイズを変更する
	 * @param width
	 * @param height
	 */
	private fun resizeCamera(width: Int, height: Int) {
		if (DEBUG) Log.v(TAG, "resizeCamera:")
		val camera = mLock.withLock { mCamera }
		if (camera != null) {
			val isPreviewing = mIsPreviewing
			if (isPreviewing) {
				camera.stopPreview()
			}
			val params = camera.parameters
			CameraUtils.chooseVideoSize(params, width, height)
			CameraUtils.chooseFps(params, 1.0f, 120.0f)
			mRotation = CameraUtils.setupRotation(requireContext(), mCameraId, camera, params)
			camera.setParameters(params)
			onResizeCamera(camera)
			if (isPreviewing) {
				camera.startPreview()
			}
		} else {
			mReceiver.resize(width, height)
		}
	}

	/**
	 * カメラの映像サイズ変更後の処理
	 * カメラへ実際に設定された映像サイズに更新する
	 * @param camera
	 */
	private fun onResizeCamera(camera: Camera) {
		val info = Camera.CameraInfo()
		Camera.getCameraInfo(mCameraId, info)
		val params = camera.parameters
		val sz = params.previewSize
		val width = sz.width
		val height = sz.height
		mCameraInfo.id = mCameraId.toString()
		mCameraInfo.face = info.facing
		mCameraInfo.orientation = info.orientation
		mCameraInfo.width = width
		mCameraInfo.height = height
		mCameraInfo.canDisableShutterSound = info.canDisableShutterSound
		if (DEBUG) Log.v(TAG, "onResizeCamera:$mCameraInfo")
		mReceiver.resize(width, height)
		super.resize(width, height)
	}

	/**
	 * カメラからの映像取得を開始する
	 */
	@WorkerThread
	private fun startPreview() {
		if (DEBUG) Log.v(TAG, "startPreview:isPreviewing=$mIsPreviewing")
		checkValid()
		if (!mIsPreviewing) {
			try {
				val camera = openCamera()
				val st = mReceiver.surfaceTexture
				st.setDefaultBufferSize(width, height)	// 念の為にサイズをセット
				camera.setPreviewTexture(st)
				camera.startPreview()
				mIsPreviewing = true
			} catch (e: Exception) {
				val c = swap(null)
				c?.release()
				throw e
			}
		}
		if (DEBUG) Log.v(TAG, "startPreview:finished")
	}

	/**
	 * カメラからの映像取得を停止する
	 */
	@WorkerThread
	private fun stopPreview() {
		val camera = mLock.withLock { mCamera }
		if (DEBUG) Log.v(TAG, "stopPreview:$camera")
		camera?.stopPreview()
	}

	/**
	 * カメラからの映像取得を終了して関係するリソースを破棄する
	 */
	@WorkerThread
	private fun releaseCamera() {
		val camera = swap(null)
		if (camera != null) {
			if (DEBUG) Log.v(TAG, "releaseCamera:")
			mSupportedSizeList.clear()
			camera.stopPreview()
			camera.release()
		}
	}

	/**
	 * 画面の向きに合わせて映像を回転
	 * Camera#setDisplayOrientationは映像取得中には呼べないので
	 * このメソッドを呼び出すときはあらかじめ映像取得を停止させること
	 * @param orientation90
	 */
	private fun internalSetOrientation(camera: Camera, orientation90: Int) {
//		val params = camera.parameters
//		CameraUtils.setupRotation(requireContext(), mCameraId, camera, params)
		// カメラの方向を取得
		var d = orientation90
		val info = Camera.CameraInfo()
		Camera.getCameraInfo(mCameraId, info)
		val isFrontFace = (info.facing == CameraConst.FACING_FRONT)
		// カメラの方向に応じて回転角を補正
		if (isFrontFace) {    // フロントカメラの時
			d = (info.orientation + d) % 360
			d = (360 - d) % 360 // reverse
		} else {  // バックカメラの時
			d = (info.orientation - d + 360) % 360
		}
		if (DEBUG) Log.v(TAG, "internalSetOrientation:isFrontFace=$isFrontFace,$orientation90/$d/$info/$mCameraInfo")
		// プレビュー表示を回転させる
		camera.setDisplayOrientation(d)
		// Camera.Parameters#setRotationの呼び出しに失敗してカメラが動かなくなる機種がある
		// params.setRotation(degrees);
	}

	companion object {
		private const val DEBUG = true	// set false on production
		private val TAG = Camera1SourcePipeline::class.java.simpleName
		private const val IMAGE_BUFFER_SIZE = 3
	}
}