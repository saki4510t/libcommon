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
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.serenegiant.libcommon.databinding.FragmentCpuMonitorBinding
import com.serenegiant.system.CpuMonitor
import com.serenegiant.view.ViewUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * CpuMonitorの出力を表示するだけ
 */
class CpuMonitorFragment : BaseFragment() {

	private val mCpuMonitor = CpuMonitor()
	private lateinit var mBinding: FragmentCpuMonitorBinding
	private var mCpuMonitorJob: Job? = null

	override fun onAttach(context: Context) {
		super.onAttach(context)
		requireActivity().title = getString(R.string.title_cpu_monitor)
	}

	override fun onCreateView(inflater: LayoutInflater,
		container: ViewGroup?, savedInstanceState: Bundle?): View? {

		if (DEBUG) Log.v(TAG, "onCreateView:")
		val customInflater
			= ViewUtils.createCustomLayoutInflater(requireContext(), inflater, R.style.AppTheme)
		return DataBindingUtil.inflate<FragmentCpuMonitorBinding?>(
			customInflater,
			R.layout.fragment_cpu_monitor, container, false
		).apply {
			mBinding = this
		}.run {
			root
		}
	}

	@SuppressLint("SetTextI18n")
	override fun internalOnResume() {
		super.internalOnResume()
		if (DEBUG) Log.v(TAG, "internalOnResume:")
		mCpuMonitorJob = lifecycleScope.launch {
			while (isActive) {
				val cpuInfoString = withContext(Dispatchers.Default) {
					if (mCpuMonitor.sampleCpuUtilization()) {
						String.format(
							Locale.US, "T=%4.1f\nCPU=%3d/%3d/%3d",
							mCpuMonitor.tempAve,
							mCpuMonitor.cpuCurrent,
							mCpuMonitor.cpuAvg3,
							mCpuMonitor.cpuAvgAll
						)
					} else {
						""
					}
				}
				if (cpuInfoString.isNotBlank()) {
					mBinding.message.text = cpuInfoString
					delay(1000)
				} else {
					mBinding.message.text = "Failed to get cpu information"
					mCpuMonitorJob?.cancel()
					break
				}
			}
		}
	}

	override fun internalOnPause() {
		if (DEBUG) Log.v(TAG, "internalOnPause:")
		mCpuMonitorJob?.cancel()
		super.internalOnPause()
	}

	companion object {
		private const val DEBUG = true // TODO set false on release
		private val TAG = CpuMonitorFragment::class.java.simpleName

		fun newInstance() = CpuMonitorFragment().apply {
			arguments = Bundle().apply {
				// 今は何もない
			}
		}
	}
}
