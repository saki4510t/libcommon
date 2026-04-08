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

import android.Manifest
import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCaptureSession.CaptureCallback
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.opengl.Matrix
import android.os.Build
import android.os.Handler
import android.util.Log
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.annotation.Size
import androidx.annotation.WorkerThread
import com.serenegiant.camera.Camera2Utils.RequestTemplate
import com.serenegiant.gl.GLManager
import com.serenegiant.glpipeline.GLPipeline
import com.serenegiant.glpipeline.ProxyPipeline
import com.serenegiant.glutils.GLSurfaceReceiver
import com.serenegiant.system.BuildCheck
import com.serenegiant.content.ContextUtils
import com.serenegiant.system.HandlerThreadHandler
import com.serenegiant.view.ViewUtils
import java.lang.ref.WeakReference
import java.util.concurrent.Executor
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
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
 * @param templateType
 * @param burstNum
 * @param afMode
 * @param needCaptureCallback
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class Camera2SourcePipeline(
	context: Context,
	glManager: GLManager,
	@CameraConst.FaceType face: Int = CameraConst.FACING_BACK,
	width: Int = CameraConst.DEFAULT_WIDTH, height: Int = CameraConst.DEFAULT_HEIGHT,
	useSharedContext: Boolean = false,
	@RequestTemplate
	private val templateType: Int = CameraDevice.TEMPLATE_RECORD,
	private val burstNum: Int = 1,
	private val afMode: Int = CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO,
	private val needCaptureCallback: Boolean = false
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
	/**
	 * カメラアクセスを非同期で行うためのHandler
	 */
	private val mCameraHandler = HandlerThreadHandler.createHandler(TAG)
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
	private val mCameraOpenCloseLock = Semaphore(1)
	private var mCamera: CameraDevice? = null
	private var mOrientationDegree = 0
	@Volatile
	private var mIsCapturing = false
	@Volatile
	private var mRequestPreview = false
	private var mCaptureSession: CameraCaptureSession? = null
	private var mPreviewRequestBuilder: CaptureRequest.Builder? = null
	private val mPreviewRequest = mutableListOf<CaptureRequest>()

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
		mCameraInfo.face = face
		mCameraInfo.width = width
		mCameraInfo.height = height
		// モデルビュー変換行列を初期化
		internalSetOrientation(ViewUtils.getRotationDegrees(context))
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
		internalDisconnect()
		if (isValid) {
			mReceiver.release()
		}
		if (mOwnManager) {
			mGLManager.release()
		}
		mCameraHandler.removeCallbacksAndMessages(null)
		mCameraHandler.quitSafely()
		mWeakContext.clear()
		super.internalRelease()
	}

	@WorkerThread
	@RequiresPermission(Manifest.permission.CAMERA)
	override fun connect() {
		if (DEBUG) Log.v(TAG, "connect:")
		checkValid()
		if (!mIsCapturing) {
			val camera = mLock.withLock { mCamera }
			if (camera != null) {
				// 既にカメラがオープンしているとき
				internalStartCapture(camera)
			} else {
				// カメラガオープンしていないとき
				mRequestPreview = true
				openCamera()
			}
		}
	}

	@WorkerThread
	override fun disconnect() {
		if (isValid) {
			if (DEBUG) Log.v(TAG, "disconnect:")
			// カメラ映像取得終了処理
			internalStopCapture()
			internalDisconnect()
		}
	}

	@WorkerThread
	override fun updateOrientation(orientation90: Int) {
		if (DEBUG) Log.v(TAG, "updateOrientation:$orientation90")
		checkValid()
		if (mOrientationDegree != orientation90) {
			internalSetOrientation(orientation90)
			val camera = mLock.withLock { mCamera }
			if (camera != null) {
				onResizeCamera(mCameraInfo)
			}
		}
	}

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

	override fun isValid(): Boolean {
		return super.isValid() && mGLManager.isValid() && mReceiver.isValid
	}

	override fun isActive(): Boolean {
		mLock.lock()
		try {
			// 破棄されていない && 子と繋がっている && カメラ映像取得中
			return isValid && (pipeline != null)
		} finally {
			mLock.unlock()
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

	/**
	 * CameraManager取得用のヘルパーメソッド
	 */
	private fun requireCameraManager(): CameraManager {
		return ContextUtils.requireSystemService(requireContext(), CameraManager::class.java)
	}

	@Throws(IllegalStateException::class)
	private fun checkValid() {
		check(mGLManager.isValid()) { "Already released" }
	}

	private fun swap(newCamera: CameraDevice?): CameraDevice? {
		return mLock.withLock {
			val c = mCamera
			mCamera = newCamera
			c
		}
	}

	/**
	 * 画面の回転角・カメラの向き(FRONT/BACK)を考慮したカメラ映像の向きを取得
	 * アスペクト比の計算とonResizeコールバック時の映像サイズ計算に使う
	 * @return
	 */
	private fun getRotation(): Int {
		// このメソッドをで取得出来るのは旧Camera API相当のカメラ回転角度
		var degrees = mOrientationDegree
		if (mCameraInfo.face == CameraConst.FACING_FRONT) {
			// front camera
			degrees = (mCameraInfo.orientation + degrees) % 360
			degrees = (360 - degrees) % 360 // reverse
		} else {
			// back camera
			degrees = (mCameraInfo.orientation - degrees + 360) % 360
		}
		if (DEBUG) Log.v(TAG, "getRotation:$degrees")
		return degrees
	}

	/**
	 * 画面の回転角・カメラの向き(FRONT/BACK)を考慮したカメラ映像の回転行列を設定
	 * @param orientation90 画面の回転角, 0, 90, 180, 270のいずれか
	 */
	private fun internalSetOrientation(orientation90: Int) {
		mOrientationDegree = orientation90
		Matrix.setIdentityM(mMvpMatrix, 0)
		Matrix.rotateM(mMvpMatrix, 0, orientation90.toFloat(), 0.0f, 0.0f, 1.0f)
	}

	@RequiresPermission(Manifest.permission.CAMERA)
	@WorkerThread
	private fun openCamera() {
		if (DEBUG) Log.v(TAG, "openCamera:")
		val camera = mLock.withLock { mCamera }
		if (camera == null) {
			val context = requireContext()
			val info = try {
				Camera2Utils.findCamera(
					requireCameraManager(),
					mCameraInfo.face,
					width, height,
					ViewUtils.getRotationDegrees(context)
				)
			} catch (e: CameraAccessException) {
				null
			}
			if (DEBUG) Log.v(TAG, "openCamera:$info")
			if (info?.isValid == true) {
				val manager = requireCameraManager()
				// 対応解像度一覧を更新する
				val characteristics = manager.getCameraCharacteristics(info.id)
				mSupportedSizeList.clear()
				mSupportedSizeList.addAll(characteristics.getSupportedSizeList())
				onResizeCamera(info)
				internalSetOrientation(mOrientationDegree)
				// カメラのロックを試みる(最大2500ミリ秒待機)
				if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
					throw RuntimeException("Time out waiting to lock camera opening.")
				}
				// カメラをロックできたらopen要求する。mCameraHandlerに関連づいているスレッド上で実行される。
				// 実際にopenされた後の処理はmCameraStateCallbackに記述。
				manager.openCamera(info.id, mCameraStateCallback, mCameraHandler)
			}
		} else if (DEBUG) Log.v(TAG, "openCamera:already camera opened")
	}

	/**
	 * カメラオープン中ならカメラ映像のサイズを変更する
	 * @param width
	 * @param height
	 */
	private fun resizeCamera(width: Int, height: Int) {
		if (DEBUG) Log.v(TAG, "resizeCamera:(${width}x$height)")
		val camera = mLock.withLock { mCamera }
		if (camera != null) {
			val isCapturing = mIsCapturing
			if (isCapturing) {
				// 映像取得中なら一旦停止させる
				internalStopCapture()
			}
			val info = try {
				Camera2Utils.chooseOptimalSize(
					requireCameraManager(),
					mCameraInfo.id, mCameraInfo.face,
					width, height,
					ViewUtils.getRotationDegrees(requireContext())
				)
			} catch (e: CameraAccessException) {
				null
			}
			if (DEBUG) Log.v(TAG, "resizeCamera:new size $info")
			if (info?.isValid == true) {
				onResizeCamera(info)
			}
			if (isCapturing) {
				// 映像取得中だったときは再開する
				internalStartCapture(camera)
			}
		} else {
			mReceiver.resize(width, height)
		}
	}

	/**
	 * カメラの映像サイズ変更後の処理
	 * カメラへ実際に設定された映像サイズに更新する
	 * @param info
	 */
	private fun onResizeCamera(info: CameraInfo) {
		if (DEBUG) Log.v(TAG, "onResizeCamera:$info")
		mCameraInfo.set(info)
		mReceiver.resize(info.width, info.height)
		super.resize(info.width, info.height)
	}

	/**
	 * 映像取得開始
	 * @param camera
	 */
	private fun internalStartCapture(camera: CameraDevice) {
		if (DEBUG) Log.v(TAG, "internalStartCapture:isCapturing=$mIsCapturing")
		if (!mIsCapturing) {
			mIsCapturing = true
			try {
				mCameraOpenCloseLock.acquire()
				try {
					onResizeCamera(mCameraInfo)
					// プレビュー表示用にCameraCaptureSessionを生成する
					createCaptureSessionLocked(camera)
				} finally {
					mCameraOpenCloseLock.release()
				}
			} catch (e: CameraAccessException) {
				mIsCapturing = false
				Log.w(TAG, e)
				// FIXME パーミッションが無いか他でカメラを使用中なのでパーミッション要求等をする
			} catch (e: InterruptedException) {
				mIsCapturing = false
				if (DEBUG) Log.w(TAG, e)
			}
		}
	}

	/**
	 * 映像取得終了
	 */
	@WorkerThread
	private fun internalStopCapture() {
		if (mCaptureSession != null) {
			if (DEBUG) Log.v(TAG, "internalStopCapture:")
			mIsCapturing = false
			try {
				mCameraOpenCloseLock.acquire()
				try {
					internalStopCaptureLocked()
				} finally {
					mCameraOpenCloseLock.release()
				}
			} catch (e: InterruptedException) {
				if (DEBUG) Log.w(TAG, e)
			}
			if (DEBUG) Log.v(TAG, "internalStopCapture:finished")
		}
	}

	/**
	 * 実際の映像取得終了処理
	 * mCameraOpenCloseLockを取得した状態で呼び出す
	 */
	@WorkerThread
	private fun internalStopCaptureLocked() {
		mRequestPreview = false
		mPreviewRequest.clear()
		val captureSession = mCaptureSession
		mCaptureSession = null
		if (captureSession != null) {
			if (DEBUG) Log.v(TAG, "internalStopCaptureLocked:" )
			try {
				captureSession.abortCaptures()
			} catch (e: CameraAccessException) {
				Log.d(TAG, "internalStopCaptureLocked:", e)
			} catch (e: IllegalStateException) {
				Log.d(TAG, "internalStopCaptureLocked:", e)
			}
			captureSession.close()
			if (DEBUG) Log.v(TAG, "internalStopCaptureLocked:finished" )
		}
		mPreviewRequestBuilder = null
	}

	@WorkerThread
	private fun internalDisconnect() {
		val camera = swap(null)
		if (camera != null) {
			if (DEBUG) Log.v(TAG, "internalDisconnect:")
			try {
				mCameraOpenCloseLock.acquire()
				internalStopCaptureLocked()
				camera.close()
				// CameraDevice.StateCallback#onClosedが呼ばれるのを待機する
				if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
					mCameraOpenCloseLock.release()
					throw RuntimeException("Time out waiting to lock camera closing.")
				}
			} catch (e: InterruptedException) {
				mCameraOpenCloseLock.release()
				throw RuntimeException("Interrupted while trying to lock camera closing.", e)
			}
			if (DEBUG) Log.v(TAG, "internalDisconnect:finished")
		}
	}

	/**
	 * CameraCaptureSessionを生成する
	 * mCameraOpenCloseLockを取得した状態で呼び出す
	 * @param camera
	 */
	@Suppress("DEPRECATION")
	private fun createCaptureSessionLocked(camera: CameraDevice) {
		val st = mReceiver.surfaceTexture
		st.setDefaultBufferSize(width, height)
		val surface = Surface(st)
		if (DEBUG) Log.v(TAG, "createCaptureSession:size(${width}x$height),surface=$surface")
		val previewRequestBuilder = createCaptureRequestBuilder(camera, templateType, surface)
		if (DEBUG) Log.v(TAG, "createCaptureSession:mPreviewRequestBuilder=$mPreviewRequestBuilder")
		val stateCallback = object : CameraCaptureSession.StateCallback() {
			override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
				if (DEBUG) Log.v(TAG, "onConfigured:session=$cameraCaptureSession")
				mLock.withLock {
					if (mCamera == null) {
						// config中にカメラがcloseされてしまった
						Log.w(TAG, "onConfigured: camera is null")
						return
					}
				}
				mCaptureSession = cameraCaptureSession
				mPreviewRequestBuilder = previewRequestBuilder
				try {
					internalConfigSettingsLocked(cameraCaptureSession, previewRequestBuilder)
					// プレビュー表示を開始する
					internalApplyConfigsAndStartPreviewLocked(cameraCaptureSession, previewRequestBuilder)
					mIsCapturing = true
				} catch (e: CameraAccessException) {
					Log.e(TAG, "onConfigured:$cameraCaptureSession", e)
					mIsCapturing = false
				} catch (e: IllegalStateException) {
					Log.e(TAG, "onConfigured:$cameraCaptureSession", e)
					mIsCapturing = false
				}
			}

			override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
				Log.e(TAG, "onConfigureFailed:$cameraCaptureSession")
				mIsCapturing = false
				// FIXME カメラ停止処理
			}
		}
		if (BuildCheck.isAPI28()) {
			val sessionConfig = SessionConfiguration(
				SessionConfiguration.SESSION_REGULAR,
				createOutputConfiguration(arrayOf(surface)),
				HandlerExecutor(mCameraHandler),
				stateCallback
			)
			camera.createCaptureSession(sessionConfig)
		} else {
			camera.createCaptureSession(
				createCaptureSurfaceList(arrayOf(surface)),
				stateCallback, mCameraHandler
			)
		}
	}

	/**
	 * 映像取得用Surfaceリストを生成する
	 * @param surfaces
	 */
	private fun createCaptureSurfaceList(surfaces: Array<Surface>): List<Surface> {
		if (DEBUG) Log.v(TAG, "createCaptureSurfaceList:")
		return surfaces.toList()
	}

	/**
	 * 映像取得用のOutputConfigurationを生成する
	 * @param surfaces
	 */
	@RequiresApi(Build.VERSION_CODES.N)
	private fun createOutputConfiguration(surfaces: Array<Surface>): List<OutputConfiguration> {
		if (DEBUG) Log.v(TAG, "createOutputConfiguration:")
		return surfaces.map { surface ->
			OutputConfiguration(surface).apply {
			}
		}
	}

	@Throws(CameraAccessException::class)
	private fun createCaptureRequestBuilder(
		camera: CameraDevice, @RequestTemplate templateType: Int,
		previewSurface: Surface
	): CaptureRequest.Builder {
		if (DEBUG) Log.v(TAG, "createCaptureRequestBuilder:")
		// CaptureRequest.Builderに出力用のSurfaceをセットする
		// 用途に応じてテンプレートを指定する。TEMPLATE_RECORD, TEMPLATE_STILL_CAPTURE, TEMPLATE_PREVIEW
		return camera.createCaptureRequest(templateType).apply {
			addTarget(previewSurface)
		}
	}

	private fun onCaptureProgressed(
		session: CameraCaptureSession,
		request: CaptureRequest,
		partialResult: CaptureResult
	) {
		if (DEBUG) Log.v(TAG, "onCaptureProgressed:")
		// 今の実装ではプレビュー映像を取得するだけのなので特にすることなし
		// 静止画キャプチャをカメラ機能を使ってするなら実装しないとダメ
	}

	private fun onCaptureCompleted(
		session: CameraCaptureSession,
		request: CaptureRequest,
		result: TotalCaptureResult
	) {
		if (DEBUG) Log.v(TAG, "onCaptureCompleted:")
		// 今の実装ではプレビュー映像を取得するだけのなので特にすることなし
		// 静止画キャプチャをカメラ機能を使ってするなら実装しないとダメ
	}

	/**
	 * カメラの設定
	 * mCameraOpenCloseLockを取得した状態で呼び出す
	 * @param session
	 * @param builder
	 */
	private fun internalConfigSettingsLocked(
		session: CameraCaptureSession,
		builder: CaptureRequest.Builder
	) {
		if (DEBUG) Log.v(TAG, "internalConfigSettingsLocked:")
		// 連続オートフォーカスを指定する(でも機種によっては未対応)
		builder.set(CaptureRequest.CONTROL_AF_MODE, afMode)
		// フラッシュも自動設定にする(でも機種によっては未対応)
//		builder.set(CaptureRequest.CONTROL_AE_MODE,
//			CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
	}

	/**
	 * カメラ設定を適用して新しい繰り返しキャプチャーリクエストを発行する
	 * mCameraOpenCloseLockを取得した状態で呼び出す
	 * @param session
	 * @param builder
	 * @throws CameraAccessException
	 */
	@Throws(CameraAccessException::class)
	private fun internalApplyConfigsAndStartPreviewLocked(
		session: CameraCaptureSession,
		builder: CaptureRequest.Builder
	) {
		if (DEBUG) Log.v(TAG, "internalApplyConfigsAndStartPreviewLocked:session=$session,builder=$builder,burstNum=$burstNum")
		mPreviewRequest.clear()
		if (burstNum <= 1) {
			val req = builder.build()
			mPreviewRequest.add(req)
			session.setRepeatingRequest(req,
				if (needCaptureCallback) mCaptureCallback else null,
				mCameraHandler)
		} else {
			for (i in 0 until burstNum) {
				val req = builder.build()
				mPreviewRequest.add(req)
			}
			session.setRepeatingBurst(
				mPreviewRequest,
				if (needCaptureCallback) mCaptureCallback else null,
				mCameraHandler)
		}
	}

	/**
	 * カメラの状態が変更された時に呼び出されるコールバックリスナー
	 */
	private val mCameraStateCallback = object : CameraDevice.StateCallback() {
		override fun onOpened(camera: CameraDevice) {
			if (DEBUG) Log.v(TAG, "onOpened:mRequestPreview=$mRequestPreview,camera=$camera")
			swap(camera)
			mCameraOpenCloseLock.release()
			if (mRequestPreview) {
				mRequestPreview = false
				if (DEBUG) Log.v(TAG, "onOpened:request start preview")
				internalStartCapture(camera)
			}
			if (DEBUG) Log.v(TAG, "onOpened:finished")
		}

		override fun onClosed(camera: CameraDevice) {
			super.onClosed(camera)
			if (DEBUG) Log.v(TAG, "onClosed:camera=$camera")
			mCameraOpenCloseLock.release()
		}

		override fun onDisconnected(camera: CameraDevice) {
			if (DEBUG) Log.v(TAG, "onDisconnected:camera=$camera")
			mRequestPreview = false
			swap(null)
			mCameraOpenCloseLock.release()
			try {
				camera.close()
			} catch (e: Exception) {
				if (DEBUG) Log.w(TAG, e)
			}
		}

		override fun onError(camera: CameraDevice, error: Int) {
			if (DEBUG) Log.v(TAG, "onError:camera=$camera,err=$error")
			mRequestPreview = false
			swap(null)
			mCameraOpenCloseLock.release()
			try {
				camera.close()
			} catch (e: Exception) {
				if (DEBUG) Log.w(TAG, e)
			}
		}
	}

	/**
	 * キャプチャ要求のコールバックリスナー
	 */
	private val mCaptureCallback = object : CaptureCallback() {
		override fun onCaptureProgressed(
			session: CameraCaptureSession,
			request: CaptureRequest,
			partialResult: CaptureResult
		) {
			this@Camera2SourcePipeline.onCaptureProgressed(session, request, partialResult)
		}

		override fun onCaptureCompleted(
			session: CameraCaptureSession,
			request: CaptureRequest,
			result: TotalCaptureResult
		) {
			this@Camera2SourcePipeline.onCaptureCompleted(session, request, result)
		}
	}

	class HandlerExecutor(private val handler: Handler) : Executor {
		override fun execute(r: Runnable) {
			handler.post(r)
		}
	}

	companion object {
		private const val DEBUG = true	// set false on production
		private val TAG = Camera2SourcePipeline::class.java.simpleName
	}
}