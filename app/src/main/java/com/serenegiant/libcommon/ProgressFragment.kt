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
import android.view.ViewGroup
import com.serenegiant.widget.ProgressView

class ProgressFragment : BaseFragment() {

	private var mProgress1: ProgressView? = null
	private var mProgress2: ProgressView? = null

	override fun onCreateView(inflater: LayoutInflater,
		container: ViewGroup?, savedInstanceState: Bundle?): View? {

		if (DEBUG) Log.v(TAG, "onCreateView:")
		return inflater.inflate(R.layout.fragment_progress_view, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		initView(view)
	}

	override fun onAttach(context: Context) {
		super.onAttach(context)
		requireActivity().title = getString(R.string.title_progress_view)
	}

	override fun onDestroy() {
		if (DEBUG) Log.v(TAG, "onDestroy:")
		super.onDestroy()
	}

//--------------------------------------------------------------------------------
	private fun initView(rootView: View) {
		if (DEBUG) Log.v(TAG, "initView:")
		mProgress1 = rootView.findViewById(R.id.progressView1)
		mProgress2 = rootView.findViewById(R.id.progressView2)
	}

//--------------------------------------------------------------------------------
	companion object {
		private const val DEBUG = true // TODO set false on release
		private val TAG = ProgressFragment::class.java.simpleName

	fun newInstance() = ProgressFragment().apply {
		arguments = Bundle().apply {
			// 今は何もない
		}
	}
	}
}
