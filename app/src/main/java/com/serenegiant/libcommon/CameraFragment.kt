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

import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.annotation.LayoutRes
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import com.serenegiant.media.MediaFileUtils
import com.serenegiant.service.ServiceRecorder
import com.serenegiant.utils.FileUtils
import java.io.IOException

class CameraFragment : AbstractCameraFragment() {
	private var mRecorder: ServiceRecorder? = null

	override fun isRecording(): Boolean {
		return mRecorder != null
	}

	override fun internalStartRecording() {
		if (DEBUG) Log.v(TAG, "internalStartRecording:mRecorder=$mRecorder")
		if (mRecorder == null) {
			if (DEBUG) Log.v(TAG, "internalStartRecording:get PostMuxRecorder")
			mRecorder = ServiceRecorder(requireContext(), mCallback)
		} else {
			Log.w(TAG, "internalStartRecording:recorder is not null, already start recording?")
		}
	}

	override fun internalStopRecording() {
		if (DEBUG) Log.v(TAG, "internalStopRecording:mRecorder=$mRecorder")
		if (mRecorder != null) {
			mRecorder!!.release()
			mRecorder = null
		}
	}

	override fun onFrameAvailable() {
		if (mRecorder != null) {
			mRecorder!!.frameAvailableSoon()
		}
	}

	private var mRecordingSurfaceId = 0
	private val mCallback = object: ServiceRecorder.Callback {
		override fun onConnected() {
			if (DEBUG) Log.v(TAG, "onConnected:")
			if (mRecordingSurfaceId != 0) {
				mCameraView!!.removeSurface(mRecordingSurfaceId)
				mRecordingSurfaceId = 0
			}
			if (mRecorder != null) {
				try {
					mRecorder!!.setVideoSettings(VIDEO_WIDTH, VIDEO_HEIGHT, 30, 0.25f)
					mRecorder!!.setAudioSettings(SAMPLE_RATE, CHANNEL_COUNT)
					mRecorder!!.prepare()
				} catch (e: Exception) {
					Log.w(TAG, e)
					stopRecording() // 非同期で呼ばないとデッドロックするかも
				}
			}
		}

		@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
		override fun onPrepared() {
			if (DEBUG) Log.v(TAG, "onPrepared:")
			if (mRecorder != null) {
				try {
					val surface = mRecorder!!.inputSurface // API>=18
					if (surface != null) {
						mRecordingSurfaceId = surface.hashCode()
						mCameraView!!.addSurface(mRecordingSurfaceId, surface, true)
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

		override fun onReady() {
			if (DEBUG) Log.v(TAG, "onReady:")
			if (mRecorder != null) {
				try {
					val dir = MediaFileUtils.getRecordingRoot(
						requireContext(), Environment.DIRECTORY_MOVIES, Const.REQUEST_ACCESS_SD)
					if (dir != null) {
						mRecorder!!.start(dir, FileUtils.getDateTimeString())
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
				mCameraView!!.removeSurface(mRecordingSurfaceId)
				mRecordingSurfaceId = 0
			}
			stopRecording()
		}
	}

	companion object {
		private const val DEBUG = true // TODO set false on release
		private val TAG = CameraFragment::class.java.simpleName

		fun newInstance(@LayoutRes layoutRes: Int, @StringRes titleRes: Int): CameraFragment {
			val fragment = CameraFragment()
			val args = Bundle()
			args.putInt(ARGS_KEY_LAYOUT_ID, layoutRes)
			args.putInt(ARGS_KEY_TITLE_ID, titleRes)
			fragment.arguments = args
			return fragment
		}
	}
}