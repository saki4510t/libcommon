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

import android.content.Context
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import com.serenegiant.gl.GLEffect
import com.serenegiant.media.MediaFileUtils
import com.serenegiant.mediastore.MediaStoreUtils
import com.serenegiant.service.ServiceRecorder
import com.serenegiant.system.BuildCheck
import com.serenegiant.utils.FileUtils
import com.serenegiant.widget.EffectCameraGLSurfaceView
import com.serenegiant.widget.GLPipelineView
import com.serenegiant.widget.SimpleVideoSourceCameraTextureView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * RecordingServiceを使った録画対応カメラ画面Fragment
 */
class CameraFragment : AbstractCameraFragment() {
	private var mRecorder: ServiceRecorder? = null

	override fun isRecording(): Boolean {
		return mRecorder != null
	}

	override fun internalStartRecording() {
		if (DEBUG) Log.v(TAG, "internalStartRecording:mRecorder=$mRecorder")
		if (mRecorder == null) {
			if (DEBUG) Log.v(TAG, "internalStartRecording:get ServiceRecorder")
			mRecorder = ServiceRecorder(requireContext(), mCallback)
		} else {
			Log.w(TAG, "internalStartRecording:recorder is not null, already start recording?")
		}
	}

	override fun internalStopRecording() {
		if (DEBUG) Log.v(TAG, "internalStopRecording:mRecorder=$mRecorder")
		val recorder = mRecorder
		mRecorder = null
		lifecycleScope.launch(Dispatchers.Default) {
			recorder?.stop()
			recorder?.release()
		}
	}

	override fun onFrameAvailable() {
		val recorder = mRecorder
		recorder?.frameAvailableSoon()
	}

	override fun onLongClick(view: View): Boolean {
		super.onLongClick(view)
		when (view.id) {
			R.id.cameraView -> {
				// カメラ映像表示Viewを長押ししたとき
				if (mCameraView is EffectCameraGLSurfaceView) {
					val v = view as EffectCameraGLSurfaceView
					v.effect = (v.effect + 1) % GLEffect.EFFECT_NUM
					return true
				} else if (mCameraView is SimpleVideoSourceCameraTextureView) {
					val v = view as SimpleVideoSourceCameraTextureView
					if (v.isEffectSupported()) {
						v.effect = (v.effect + 1) % GLEffect.EFFECT_NUM
						return true
					}
				}
			}
			R.id.record_button -> {
				if (triggerStillCapture()) {
					return true
				}
			}
		}

		return false
	}

	private var mRecordingSurfaceId = 0
	private val mCallback = object: ServiceRecorder.Callback {
		override fun onConnected() {
			if (DEBUG) Log.v(TAG, "onConnected:")
			if (mRecordingSurfaceId != 0) {
				mCameraView.removeSurface(mRecordingSurfaceId)
				mRecordingSurfaceId = 0
			}
			val recorder = mRecorder
			if (recorder != null) {
				lifecycleScope.launch(Dispatchers.Default) {
					try {
						recorder.setVideoSettings(VIDEO_WIDTH, VIDEO_HEIGHT, VIDEO_FPS, 0.25f)
						recorder.setAudioSettings(SAMPLE_RATE, CHANNEL_COUNT)
						recorder.prepare()
					} catch (e: Exception) {
						Log.w(TAG, e)
						stopRecording() // 非同期で呼ばないとデッドロックするかも
					}
				}
			}
		}

		override fun onPrepared() {
			if (DEBUG) Log.v(TAG, "onPrepared:")
			val recorder = mRecorder
			if (recorder != null) {
				lifecycleScope.launch(Dispatchers.Default) {
					try {
						val surface = recorder.getInputSurface() // API>=18
						if (surface != null) {
							mRecordingSurfaceId = surface.hashCode()
							mCameraView.addSurface(mRecordingSurfaceId, surface, true)
						} else {
							Log.w(TAG, "surface is null")
							stopRecording() // 非同期で呼ばないとデッドロックするかも
						}
					} catch (e: Exception) {
						Log.w(TAG, e)
						stopRecording() // 非同期で呼ばないとデッドロックするかも
					}
				}
			}
		}

		override fun onReady() {
			if (DEBUG) Log.v(TAG, "onReady:")
			val recorder = mRecorder
			if (recorder != null) {
				val context: Context = requireContext()
				try {
					val output: DocumentFile?
					= if (BuildCheck.isAPI29()) {
						// API29以降は対象範囲別ストレージ
						// DocumentFile.fromSingleUrはAPI>=19
						MediaStoreUtils.getContentDocument(
							context, "video/mp4",
							Environment.DIRECTORY_MOVIES + "/" + Const.APP_DIR,
							FileUtils.getDateTimeString() + ".mp4", null)
					} else {
						// ここの#getRecordingRoot呼び出しと#getRecordingFile呼び出しは等価
//						val dir = MediaFileUtils.getRecordingRoot(
//							context, Environment.DIRECTORY_MOVIES, Const.REQUEST_ACCESS_SD)
//						dir!!.createFile("video/mp4", FileUtils.getDateTimeString() + ".mp4")
						MediaFileUtils.getRecordingFile(
							context, Const.REQUEST_ACCESS_SD, Environment.DIRECTORY_MOVIES, "video/mp4", ".mp4")
					}
					if (DEBUG) Log.v(TAG, "onReady:output=$output," + output?.uri)
					if (output != null) {
						lifecycleScope.launch(Dispatchers.Default) {
							recorder.start(output)
						}
					} else {
						throw IOException()
					}
				} catch (e: Exception) {
					Log.w(TAG, e)
					stopRecording() // 非同期で呼ばないとデッドロックするかも
				}
			}
		}

		override fun onDisconnected() {
			if (DEBUG) Log.v(TAG, "onDisconnected:")
			if (mRecordingSurfaceId != 0) {
				mCameraView.removeSurface(mRecordingSurfaceId)
				mRecordingSurfaceId = 0
			}
			stopRecording()
		}
	}

	companion object {
		private const val DEBUG = true // TODO set false on release
		private val TAG = CameraFragment::class.java.simpleName

		fun newInstance(
			@LayoutRes layoutRes: Int, @StringRes titleRes: Int,
			pipelineMode: Int = GLPipelineView.PREVIEW_ONLY)
			= CameraFragment().apply {
				arguments = Bundle().apply {
					putInt(ARGS_KEY_LAYOUT_ID, layoutRes)
					putInt(ARGS_KEY_TITLE_ID, titleRes)
					putInt(ARGS_KEY_PIPELINE_MODE, pipelineMode)
				}
		}
	}
}
