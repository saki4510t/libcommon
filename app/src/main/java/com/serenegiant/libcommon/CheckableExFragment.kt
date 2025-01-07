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
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import com.serenegiant.libcommon.databinding.FragmentCheckableexBinding
import com.serenegiant.widget.CheckableEx
import com.serenegiant.widget.CheckableEx.OnCheckedChangeListener

class CheckableExFragment : BaseFragment() {

	class ViewModel : androidx.lifecycle.ViewModel(), OnClickListener, OnCheckedChangeListener {
		val isChecked = MutableLiveData(false)
		val isGroupChecked = MutableLiveData(false)
		override fun onClick(v: View) {
			if (DEBUG) Log.v(TAG, "onClick:$v")
		}

		override fun onCheckedChanged(checkable: CheckableEx, isChecked: Boolean) {
			if (DEBUG) Log.v(TAG, "onCheckedChanged:$checkable.isChecked=$isChecked")
		}
	}

	private val mViewModel: ViewModel by viewModels()
	private lateinit var checkableGroup : CheckableEx

	override fun onCreateView(inflater: LayoutInflater,
		container: ViewGroup?, savedInstanceState: Bundle?): View {

		if (DEBUG) Log.v(TAG, "onCreateView:")
		return FragmentCheckableexBinding.inflate(inflater, container, false)
		.apply {
			viewModel = mViewModel
			lifecycleOwner = viewLifecycleOwner
			this@CheckableExFragment.checkableGroup = checkableGroup
		}
		.run {
			root
		}
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		mViewModel.apply {
			isChecked.observe(viewLifecycleOwner) {
				isGroupChecked.value = it
			}
		}
	}

	override fun onAttach(context: Context) {
		super.onAttach(context)
		requireActivity().title = getString(R.string.title_checkable_ex)
	}

	override fun onDestroy() {
		if (DEBUG) Log.v(TAG, "onDestroy:")
		super.onDestroy()
	}

//--------------------------------------------------------------------------------
	companion object {
		private const val DEBUG = true // TODO set false on release
		private val TAG = CheckableExFragment::class.java.simpleName

		fun newInstance() = CheckableExFragment().apply {
			arguments = Bundle().apply {
				// 今は何もない
			}
		}
	}
}
