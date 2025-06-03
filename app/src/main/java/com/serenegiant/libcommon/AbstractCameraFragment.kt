package com.serenegiant.libcommon
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

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.Surface
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.annotation.WorkerThread
import com.serenegiant.glpipeline.CapturePipeline
import com.serenegiant.math.Fraction
import com.serenegiant.media.OnFrameAvailableListener
import com.serenegiant.mediastore.MediaStoreUtils
import com.serenegiant.system.BuildCheck
import com.serenegiant.system.PermissionUtils
import com.serenegiant.system.SAFUtils
import com.serenegiant.utils.FileUtils
import com.serenegiant.view.ViewUtils
import com.serenegiant.widget.*
import java.io.IOException

/**
 * 内蔵カメラへアクセスして表示するための基本クラス
 * 録画の開始/停止の実際の処理以外を実装
 */
abstract class AbstractCameraFragment : BaseFragment() {

	/**
	 * for camera preview display
	 */
	protected lateinit var mCameraView: ICameraView
	/**
	 * for scale mode display
	 */
	private lateinit var mScaleModeView: TextView
	/**
	 * button for start/stop recording
	 */
	protected lateinit var mRecordButton: ImageButton

	private var mCapture: CapturePipeline? = null

	override fun onAttach(context: Context) {
		super.onAttach(context)
		requireActivity().title = getString(titleRes)
		FileUtils.DIR_NAME = APP_DIR_NAME
	}

	override fun onCreateView(inflater: LayoutInflater,
		container: ViewGroup?, savedInstanceState: Bundle?): View? {

		val customInflater
			= ViewUtils.createCustomLayoutInflater(requireContext(), inflater, R.style.AppTheme_Camera)
		return customInflater.inflate(layoutXml, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		if (DEBUG) Log.v(TAG, "onViewCreated:")
		val cameraView: View = view.findViewById(R.id.cameraView)
		mCameraView = cameraView  as ICameraView
		mCameraView.getView().setOnClickListener(mOnClickListener)
		mCameraView.getView().setOnLongClickListener(mOnLongClickListener)
		mCameraView.setVideoSize(VIDEO_WIDTH, VIDEO_HEIGHT)
		if (mCameraView !is GLPipelineView) {
			Log.d(TAG, "onViewCreated:camera view is not a GLPipelineView,$cameraView")
		}
		mScaleModeView = view.findViewById(R.id.scalemode_textview)
		updateScaleModeText()
		mRecordButton = view.findViewById(R.id.record_button)
		mRecordButton.setOnClickListener(mOnClickListener)
		mRecordButton.setOnLongClickListener(mOnLongClickListener)
		mRecordButton.visibility = if (isRecordingSupported()) View.VISIBLE else View.GONE
		if (mCameraView is SimpleVideoSourceCameraTextureView) {
			val v = mCameraView as SimpleVideoSourceCameraTextureView
			v.pipelineMode = pipelineMode
			v.enableFaceDetect = enableFaceDetect
		}
		mCapture = CapturePipeline(mCaptureCallback)
	}

	public override fun internalOnResume() {
		super.internalOnResume()
		if (DEBUG) Log.v(TAG, "internalOnResume:")
		mCameraView.onResume()
		mCameraView.addListener(mOnFrameAvailableListener)
		if (mCameraView is GLPipelineView) {
			val v = mCameraView as GLPipelineView
			if (DEBUG) Log.v(TAG, "internalOnResume:add capture pipeline")
			v.addPipeline(mCapture!!)
		} else {
			if (DEBUG) Log.d(TAG, "internalOnResume:camera view is not a GLPipelineView")
		}
		// カメラパーミッションが無いか、
		// 録画に対応していててストレージアクセス/録音のパーミッションが無いなら
		// 終了して前画面へ戻る
		if (!PermissionUtils.hasCamera(activity)
			|| (isRecordingSupported() && !hasPermission())) {
			popBackStack()
		}
	}

	public override fun internalOnPause() {
		if (DEBUG) Log.v(TAG, "internalOnPause:")
		stopRecording()
		mCapture!!.remove()
		mCameraView.removeListener(mOnFrameAvailableListener)
		mCameraView.onPause()
		super.internalOnPause()
	}

	override fun internalRelease() {
		mCapture?.release()
		mCapture = null
		super.internalRelease()
	}

	//================================================================================
	protected open fun isRecordingSupported(): Boolean {
		return mCameraView.isRecordingSupported()
	}

	@get:LayoutRes
	protected val layoutXml: Int
		get() {
			val args = arguments
			return args?.getInt(ARGS_KEY_LAYOUT_ID, R.layout.fragment_camera)
				?: R.layout.fragment_camera
		}
	@get:StringRes
	protected val titleRes: Int
		get() {
			val args = arguments
			return args?.getInt(ARGS_KEY_TITLE_ID, R.string.title_camera)
				?: R.string.title_camera
		}
	private val pipelineMode: Int
		get() {
			val args = arguments
			return args?.getInt(ARGS_KEY_PIPELINE_MODE, GLPipelineView.PREVIEW_ONLY)
				?: GLPipelineView.PREVIEW_ONLY
		}
	protected val enablePipelineEncode: Boolean
		get() {
			val args = arguments
			return args?.getBoolean(ARGS_KEY_ENABLE_PIPELINE_RECORD, false)
				?: false
		}
	protected val enableFaceDetect: Boolean
		get() {
			val args = arguments
			return args?.getBoolean(ARGS_KEY_ENABLE_FACE_DETECT, false)
				?: false
		}

	/**
	 * method when touch record button
	 */
	private val mOnClickListener = View.OnClickListener { view -> onClick(view) }
	private val mOnLongClickListener = OnLongClickListener { view -> onLongClick(view) }

	protected fun onClick(view: View) {
		when (view.id) {
			R.id.cameraView -> {
				if (mCameraView is IScaledView) {
					val scaleMode = (mCameraView.getScaleMode() + 1) % 3
					mCameraView.setScaleMode(scaleMode)
				} else {
					val scaleMode = (mCameraView.getScaleMode() + 1) % 4
					mCameraView.setScaleMode(scaleMode)
				}
				updateScaleModeText()
			}
			R.id.record_button -> if (!isRecording()) {
				startRecording()
			} else {
				stopRecording()
			}
		}
	}

	protected open fun onLongClick(view: View): Boolean {
		if (DEBUG) Log.v(TAG, "onLongClick:${view}")
		return false
	}

	protected fun triggerStillCapture(): Boolean {
		val capture = mCapture
		if (DEBUG) Log.v(TAG, "triggerStillCapture:$capture")
		return if (capture != null) {
			capture.trigger()
			true
		} else {
			if (DEBUG) Log.v(TAG, "triggerStillCapture:CapturePipeline is null")
			false
		}
	}

	private fun updateScaleModeText() {
		val scaleMode = mCameraView.getScaleMode()
		if (mCameraView is IScaledView) {
			mScaleModeView.text =
				when (scaleMode) {
					0 -> "keep aspect"
					1 -> "scale to fit"
					2 -> "keep aspect(crop center)"
					else -> ""
				}
		} else {
			mScaleModeView.text =
				when (scaleMode) {
					0 -> "scale to fit"
					1 -> "keep aspect(viewport)"
					2 -> "keep aspect(matrix)"
					3 -> "keep aspect(crop center)"
					else -> ""
				}
		}
	}

	protected open fun addSurface(surface: Surface?, maxFps: Fraction? = null) {
		val id = surface?.hashCode() ?: 0
		if (DEBUG) Log.d(TAG, "addSurface:id=$id")
		synchronized(mSync) {
			mCameraView.addSurface(id, surface!!, true, maxFps)
		}
		if (DEBUG) Log.v(TAG, "addSurface:finished")
	}

	/**
	 * request remove Surface
	 * @param surface // id usually use Surface#hashCode
	 */
	open fun removeSurface(surface: Surface?) {
		val id = surface?.hashCode() ?: 0
		if (DEBUG) Log.d(TAG, "removeSurface:id=$id")
		synchronized(mSync) {
			mCameraView.removeSurface(id)
		}
		if (DEBUG) Log.v(TAG, "removeSurface:finished")
	}


	protected abstract fun isRecording(): Boolean

	/**
	 * start recording
	 * This is a sample project and call this on UI thread to avoid being complicated
	 * but basically this should be called on private thread because prepareing
	 * of encoder is heavy work
	 */
	private fun startRecording() {
		if (DEBUG) Log.v(TAG, "startRecording:")
		mRecordButton.setColorFilter(-0x10000) // turn red
		try {
			if (hasPermission()) {
				internalStartRecording()
			} else {
				mRecordButton.setColorFilter(0)
				Log.e(TAG, "startCapture:has something missing permission(s)")
			}
		} catch (e: Exception) {
			mRecordButton.setColorFilter(0)
			Log.e(TAG, "startCapture:", e)
		}
	}

	@Throws(IOException::class)
	protected abstract fun internalStartRecording()

	/**
	 * request stop recording
	 */
	protected fun stopRecording() {
		if (DEBUG) Log.v(TAG, "stopRecording:")
		mRecordButton.setColorFilter(0) // return to default color
		internalStopRecording()
	}

	protected abstract fun internalStopRecording()

	protected fun clearRecordingState() {
		mRecordButton.setColorFilter(0)
	}

	private val mOnFrameAvailableListener = OnFrameAvailableListener {
		this@AbstractCameraFragment.onFrameAvailable()
	}

	protected abstract fun onFrameAvailable()

	/**
	 * 録画に必要なパーミッションを持っているかどうか
	 * パーミッションの要求はしない
	 * @return
	 */
	@SuppressLint("NewApi")
	protected fun hasPermission(): Boolean {
		// API29以降は対象範囲別ストレージ＆MediaStoreを使うのでWRITE_EXTERNAL_STORAGEパーミッションは不要
		val activity: Activity? = activity
		return if ((activity == null) || activity.isFinishing) {
			false
		} else (!BuildCheck.isAPI21()
			|| SAFUtils.hasPermission(activity, REQUEST_ACCESS_SD)	// API>=19
			|| (BuildCheck.isAPI28() || PermissionUtils.hasWriteExternalStorage(activity)))
			&& PermissionUtils.hasAudio(activity)
			&& PermissionUtils.hasCamera(activity)
	}

	private val mCaptureCallback = object : CapturePipeline.Callback {
		@WorkerThread
		override fun onCapture(bitmap: Bitmap) {
			if (DEBUG) Log.v(TAG, "onCapture:bitmap=$bitmap")
			try {
				val ctx = requireContext()
				val ext = "png"
				val outputFile = MediaStoreUtils.getContentDocument(
					ctx, "image/$ext",
					"${Environment.DIRECTORY_DCIM}/${FileUtils.DIR_NAME}",
					"${FileUtils.getDateTimeString()}.$ext", null
				)
				if (DEBUG) Log.v(TAG, "takePicture: save to $outputFile")
				val output = ctx.contentResolver.openOutputStream(outputFile.uri)
				if (output != null) {
					try {
						bitmap.compress(Bitmap.CompressFormat.PNG, 80, output)
					} finally {
						output.close()
						MediaStoreUtils.updateContentUri(ctx, outputFile)
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

	companion object {
		private const val DEBUG = false // TODO set false on release
		private val TAG = AbstractCameraFragment::class.java.simpleName
		/**
		 * video resolution
		 */
		const val VIDEO_WIDTH = 1280
		const val VIDEO_HEIGHT = 720

		/**
		 * video frame rate
		 */
		const val VIDEO_FPS = 30
		/**
		 * Audio recording settings
		 */
		const val SAMPLE_RATE = 44100
		const val CHANNEL_COUNT = 1
		const val APP_DIR_NAME = "libcommon"
		/** access code for secondary storage etc.  */
		const val REQUEST_ACCESS_SD = 12345
		const val ARGS_KEY_LAYOUT_ID = "LAYOUT_ID"
		const val ARGS_KEY_TITLE_ID = "TITLE_ID"
		const val ARGS_KEY_PIPELINE_MODE = "PIPELINE_MODE"
		const val ARGS_KEY_ENABLE_PIPELINE_RECORD = "ENABLE_PIPELINE_RECORD"
		const val ARGS_KEY_ENABLE_FACE_DETECT = "ENABLE_FACE_DETECT"
	}
}
