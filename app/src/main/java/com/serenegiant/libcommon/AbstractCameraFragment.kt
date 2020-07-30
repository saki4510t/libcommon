package com.serenegiant.libcommon
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

import android.app.Activity
import android.content.Context
import android.os.Bundle
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
import com.serenegiant.system.BuildCheck
import com.serenegiant.system.PermissionCheck
import com.serenegiant.system.SAFUtils
import com.serenegiant.utils.FileUtils
import com.serenegiant.view.ViewUtils
import com.serenegiant.widget.CameraDelegator
import com.serenegiant.widget.ICameraView
import com.serenegiant.widget.IScaledView
import java.io.IOException

/**
 * 内蔵カメラへアクセスして表示するための基本クラス
 * 録画の開始/停止の実際の処理以外を実装
 */
abstract class AbstractCameraFragment : BaseFragment() {

	/**
	 * for camera preview display
	 */
	protected var mCameraView: ICameraView? = null
	/**
	 * for scale mode display
	 */
	private var mScaleModeView: TextView? = null
	/**
	 * button for start/stop recording
	 */
	private var mRecordButton: ImageButton? = null

	override fun onAttach(context: Context) {
		super.onAttach(context)
		requireActivity().title = getString(titleRes)
		FileUtils.DIR_NAME = APP_DIR_NAME
	}

	override fun onCreateView(inflater: LayoutInflater,
		container: ViewGroup?, savedInstanceState: Bundle?): View? {

		val customInflater
			= ViewUtils.createCustomLayoutInflater(requireContext(), inflater, R.style.AppTheme_Camera)
		val rootView = customInflater.inflate(layoutXml, container, false)
		val cameraView: View = rootView.findViewById(R.id.cameraView)
		mCameraView = cameraView  as ICameraView
		mCameraView!!.getView().setOnClickListener(mOnClickListener)
		mCameraView!!.getView().setOnLongClickListener(mOnLongClickListener)
		mCameraView!!.setVideoSize(VIDEO_WIDTH, VIDEO_HEIGHT)
		mScaleModeView = rootView.findViewById(R.id.scalemode_textview)
		updateScaleModeText()
		mRecordButton = rootView.findViewById(R.id.record_button)
		mRecordButton!!.setOnClickListener(mOnClickListener)
		return rootView
	}

	public override fun internalOnResume() {
		super.internalOnResume()
		if (DEBUG) Log.v(TAG, "internalOnResume:")
		mCameraView!!.onResume()
		mCameraView!!.addListener(mOnFrameAvailableListener)
		if (!hasPermission()) {
			popBackStack()
		}
	}

	public override fun internalOnPause() {
		if (DEBUG) Log.v(TAG, "internalOnPause:")
		stopRecording()
		mCameraView!!.removeListener(mOnFrameAvailableListener)
		mCameraView!!.onPause()
		super.internalOnPause()
	}

	//================================================================================
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

	/**
	 * method when touch record button
	 */
	private val mOnClickListener = View.OnClickListener { view -> onClick(view) }
	private val mOnLongClickListener = OnLongClickListener { view -> onLongClick(view) }

	protected fun onClick(view: View) {
		when (view.id) {
			R.id.cameraView -> {
				if (mCameraView is IScaledView) {
					val scale_mode = (mCameraView!!.getScaleMode() + 1) % 3
					mCameraView!!.setScaleMode(scale_mode)
				} else {
					val scale_mode = (mCameraView!!.getScaleMode() + 1) % 4
					mCameraView!!.setScaleMode(scale_mode)
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
		return false
	}

	private fun updateScaleModeText() {
		val scale_mode = mCameraView!!.getScaleMode()
		if (mCameraView is IScaledView) {
			mScaleModeView!!.text =
				if (scale_mode == 0) "keep aspect"
					else if (scale_mode == 1) "scale to fit"
					else if (scale_mode == 2) "keep aspect(crop center)"
					else ""
		} else {
			mScaleModeView!!.text =
				if (scale_mode == 0) "scale to fit"
					else if (scale_mode == 1) "keep aspect(viewport)"
					else if (scale_mode == 2) "keep aspect(matrix)"
					else if (scale_mode == 3) "keep aspect(crop center)"
					else ""
		}
	}

	protected open fun addSurface(surface: Surface?) {
		val id = surface?.hashCode() ?: 0
		if (DEBUG) Log.d(TAG, "addSurface:id=$id")
		synchronized(mSync) {
			if (mCameraView != null) {
				mCameraView!!.addSurface(id, surface!!, true)
			}
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
			if (mCameraView != null) {
				mCameraView!!.removeSurface(id)
			}
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
	protected fun startRecording() {
		if (DEBUG) Log.v(TAG, "startRecording:")
		mRecordButton!!.setColorFilter(-0x10000) // turn red
		try {
			// FIXME 未実装 ちゃんとパーミッションのチェック＆要求をしないとだめ
			internalStartRecording()
		} catch (e: Exception) {
			mRecordButton!!.setColorFilter(0)
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
		mRecordButton!!.setColorFilter(0) // return to default color
		internalStopRecording()
	}

	protected abstract fun internalStopRecording()

	protected fun clearRecordingState() {
		mRecordButton!!.setColorFilter(0)
	}

	private val mOnFrameAvailableListener: CameraDelegator.OnFrameAvailableListener
		= object : CameraDelegator.OnFrameAvailableListener {

		override fun onFrameAvailable() {
			this@AbstractCameraFragment.onFrameAvailable()
		}
	}

	protected abstract fun onFrameAvailable()

	/**
	 * 録画に必要なパーミッションを持っているかどうか
	 * パーミッションの要求はしない
	 * @return
	 */
	protected fun hasPermission(): Boolean {
		val activity: Activity? = activity
		return if ((activity == null) || activity.isFinishing) {
			false
		} else (!BuildCheck.isAPI21()
			|| SAFUtils.hasPermission(activity, REQUEST_ACCESS_SD)
			|| PermissionCheck.hasWriteExternalStorage(activity))
			&& PermissionCheck.hasAudio(activity)
			&& PermissionCheck.hasCamera(activity)
	}

	companion object {
		private const val DEBUG = true // TODO set false on release
		private val TAG = AbstractCameraFragment::class.java.simpleName
		/**
		 * video resolution
		 */
		const val VIDEO_WIDTH = 1280
		const val VIDEO_HEIGHT = 720
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
	}
}