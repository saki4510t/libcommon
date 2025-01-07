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
import android.content.Context
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ToggleButton
import com.serenegiant.media.*
import com.serenegiant.widget.ProgressView

class AudioRecordFragment : BaseFragment() {

	private lateinit var mProgressView: ProgressView
	private var isRecording = false
	private var mAudioSampler: IAudioSampler? = null

	override fun onAttach(context: Context) {
		super.onAttach(context)
		requireActivity().title = getString(R.string.title_audio_record)
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		if (DEBUG) Log.v(TAG, "onCreate:")
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?): View {
		if (DEBUG) Log.v(TAG, "onCreateView:")
		return inflater.inflate(R.layout.fragment_audio_record, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		if (DEBUG) Log.v(TAG, "onViewCreated:")
		mProgressView = view.findViewById(R.id.progress)
		val toggleButton = view.findViewById<ToggleButton>(R.id.button)
		toggleButton?.setOnCheckedChangeListener {
			_, isChecked -> checkChanged(isChecked)
		}
	}

	override fun internalOnResume() {
		super.internalOnResume()
		if (DEBUG) Log.v(TAG, "internalOnResume:")
	}

	override fun internalOnPause() {
		if (DEBUG) Log.v(TAG, "internalOnPause:")
		super.internalOnPause()
	}

	override fun internalRelease() {
		if (DEBUG) Log.v(TAG, "internalRelease:")
		super.internalRelease()
	}

	private fun checkChanged(isChecked: Boolean) {
		if (DEBUG) Log.v(TAG, "checkChanged:$isChecked")
		if (isChecked && !isRecording) {
			startRecord()
		} else {
			stopRecord()
		}
	}

	@SuppressLint("MissingPermission")
	private fun startRecord() {
		if (DEBUG) Log.v(TAG, "startRecord:")
		isRecording = true
		val sampler = AudioSampler(MediaRecorder.AudioSource.CAMCORDER,
			1, AudioRecordCompat.DEFAULT_SAMPLE_RATE)
		SoundCheck.getInstance().setAudioSampler(sampler,
			object : SoundCheck.SoundCheckCallback {
			override fun onStart() {
				if (DEBUG) Log.v(TAG, "SoundCheck#onStart:")
			}

			override fun onStop() {
				if (DEBUG) Log.v(TAG, "SoundCheck#onStop:")
			}

			override fun onCheck(amplitude: Int) {
//				if (DEBUG) Log.v(TAG, "SoundCheck#onCheck:$amplitude")
				runOnUiThread({ mProgressView.progress = amplitude })
			}
		})
		sampler.start()
		mAudioSampler = sampler
	}

	private fun stopRecord() {
		if (DEBUG) Log.v(TAG, "stopRecord:")
		isRecording = false
		mAudioSampler?.stop()
		mAudioSampler?.release()
		mAudioSampler = null
	}

	companion object {
		private const val DEBUG = true // set false oon production
		private val TAG = AudioRecordFragment::class.java.simpleName

		fun newInstance() = AudioRecordFragment().apply {
			arguments = Bundle().apply {
				// 今は何もない
			}
		}
	}
}
