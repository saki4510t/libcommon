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
import android.content.Intent
import android.media.projection.MediaProjection
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.viewModels
import com.serenegiant.libcommon.databinding.FragmentScreenCaptureBinding
import com.serenegiant.libcommon.viewmodel.ScreenCaptureViewModel
import com.serenegiant.media.ScreenCaptureUtils
import com.serenegiant.media.ScreenCaptureUtils.ScreenCaptureCallback

/**
 * MediaProjectionManager/MediaProjectionを使った画面録画サンプル
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class ScreenCaptureFragment : BaseFragment() {
	private val mViewModel: ScreenCaptureViewModel by viewModels()
	/**
	 * スクリーンキャプチャー要求用のヘルパーオブジェクト
	 */
	private lateinit var mScreenCapture: ScreenCaptureUtils

	override fun onAttach(context: Context) {
		super.onAttach(context)
		if (DEBUG) Log.v(TAG, "onAttach:")
		mScreenCapture = ScreenCaptureUtils(this, object : ScreenCaptureCallback {
			override fun onResult(data: Intent) {
				if (DEBUG) Log.v(TAG, "onResult:$data")
				// スクリーンキャプチャーサービス開始要求
				mViewModel.startScreenCapture(data)
			}

			override fun onResult(projection: MediaProjection) {
				if (DEBUG) Log.v(TAG, "onResult:$projection")
				// ScreenCaptureFragmentではScreenCaptureUtils#requestMediaProjectioを
				// 呼び出さないのでこのコールバックが呼び出されることはない
			}

			override fun onFailed() {
				if (DEBUG) Log.v(TAG, "onFailed:")
				mViewModel.isChecked.value = false
				showToast(Toast.LENGTH_LONG, "User denied to start screen capture!")
			}
		})
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		if (DEBUG) Log.v(TAG, "onCreateView:")
		return FragmentScreenCaptureBinding.inflate(
			inflater, container, false)
			.apply {
				viewModel = mViewModel
				lifecycleOwner = this@ScreenCaptureFragment
			}
			.run {
				root
			}
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		if (DEBUG) Log.v(TAG, "onViewCreated:")
		mViewModel.run {
			isChecked.observe(viewLifecycleOwner) {
				if (DEBUG) Log.v(TAG, "check changed:isCheck=${it}/${isChecked.value}," +
					"isReceived=${isReceived},isRecording=${isRecording}")
				// 最初のステータス更新要求が終了するまではなにもしない
				if (isReceived) {
					if (it && (it != isRecording)) {
						mScreenCapture.requestScreenCapture()
					} else if (!it) {
						stopScreenCapture()
					}
				}
			}
		}
	}

	override fun onDestroy() {
		if (DEBUG) Log.v(TAG, "onDestroy:")
		super.onDestroy()
	}

	override fun internalOnResume() {
		if (DEBUG) Log.v(TAG, "internalOnResume:")
		super.internalOnResume()
		mViewModel.queryRecordingStatus()
	}

	companion object {
		private const val DEBUG = true // set false on production
		private val TAG = ScreenCaptureFragment::class.java.simpleName

		fun newInstance() = ScreenCaptureFragment().apply {
			arguments = Bundle().apply {
				// 今は何もない
			}
		}
	}
}
