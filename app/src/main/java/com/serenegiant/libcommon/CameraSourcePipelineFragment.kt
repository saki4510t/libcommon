package com.serenegiant.libcommon
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
import androidx.fragment.app.viewModels
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.serenegiant.libcommon.databinding.FragmentCameraSourcePipelineBinding
import com.serenegiant.libcommon.viewmodel.CameraSourcePipelineViewModel
import com.serenegiant.view.ViewUtils
import com.serenegiant.widget.ResolutionAdapter
import kotlinx.coroutines.launch

class CameraSourcePipelineFragment : Fragment() {
	private val mViewModel: CameraSourcePipelineViewModel by viewModels()
	private lateinit var mBinding : FragmentCameraSourcePipelineBinding
	/**
	 * 解像度切り替え用
	 */
	private lateinit var mAdapter: ResolutionAdapter


	override fun onAttach(context: Context) {
		super.onAttach(context)
		if (DEBUG) Log.v(TAG, "onAttach:")
		requireActivity().title = getString(R.string.title_camera_source_pipeline)
		lifecycle.addObserver(mViewModel)
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		if (DEBUG) Log.v(TAG, "onCreate:")
	}

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		if (DEBUG) Log.v(TAG, "onCreateView:")
		val customInflater = ViewUtils.createCustomLayoutInflater(requireContext(), inflater, R.style.AppTheme_Camera)
		return DataBindingUtil.inflate<FragmentCameraSourcePipelineBinding>(
			customInflater,
			R.layout.fragment_camera_source_pipeline, container, false
		).apply {
			mBinding = this
			viewModel = mViewModel
			cameraView.apply {
				holder.addCallback(mViewModel)
			}
			mAdapter = ResolutionAdapter(requireActivity())
			spinner.adapter = mAdapter
			spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
				override fun onItemSelected(
					parent: AdapterView<*>?,
					view: View, position: Int, id: Long) {
					val item = mAdapter.getItem(position)
					val current = mViewModel.currentViewSize.value
					if (DEBUG) Log.v(TAG, "onItemSelected:${current}=>$item")
					if ((item != null) && (item != current)) {
						mViewModel.setVideoSize(item)
					}
				}

				override fun onNothingSelected(parent: AdapterView<*>?) {}
			}
		}.run {
			root
		}
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		if (DEBUG) Log.v(TAG, "onViewCreated:")
		mViewModel.apply {
			lifecycleScope.launch {
				viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
					supportedSizeList.collect { supported ->
						mAdapter.clear()
						if (supported.isNotEmpty()) {
							val current = mViewModel.currentViewSize.value
							var found = -1
							// 現在選択されている解像度のインデックスを探す
							for ((index, sz) in supported.withIndex()) {
								if (sz == current) {
									found = index
									break
								}
							}
							if (DEBUG) Log.v(TAG, "found=$found/$current")
							val f = found
							mAdapter.replaceAll(supported)
							mBinding.spinner.isEnabled = true
							if (f > 0) {
								mBinding.spinner.setSelection(f)
							}
						}
					}
				}
			}
			lifecycleScope.launch {
				viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
					currentViewSize.collect { sz ->
						if (DEBUG) Log.v(TAG, "currentViewSize=$sz")
						mBinding.cameraView.apply {
//							holder.setFixedSize(sz.width, sz.height)
//							aspectRatio = sz.width / sz.height.toDouble()
							setAspectRatio(sz.width, sz.height)
						}
					}
				}
			}
		}
	}

	companion object {
		private const val DEBUG = true // set false on release
		private val TAG = CameraSourcePipelineFragment::class.java.simpleName

		fun newInstance() = CameraSourcePipelineFragment()
	}
}