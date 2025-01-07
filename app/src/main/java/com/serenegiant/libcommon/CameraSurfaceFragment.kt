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
import com.serenegiant.widget.CameraSurfaceView

class CameraSurfaceFragment : BaseFragment() {

	private var mCameraView: CameraSurfaceView? = null

	override fun onAttach(context: Context) {
		super.onAttach(context)
		requireActivity().title = TAG
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?): View? {
		if (DEBUG) Log.v(TAG, "onCreateView:")
		val rootView = inflater.inflate(
			R.layout.fragment_camera_surfaceview,
			container, false)
		mCameraView = rootView.findViewById(R.id.cameraView)
		return rootView
	}

	override fun internalOnResume() {
		super.internalOnResume()
		if (DEBUG) Log.v(TAG, "internalOnResume:")
		mCameraView!!.onResume()
	}

	override fun internalOnPause() {
		if (DEBUG) Log.v(TAG, "internalOnPause:")
		mCameraView!!.onPause()
		super.internalOnPause()
	}

	companion object {
		private const val DEBUG = true // TODO set false on release
		private val TAG = CameraSurfaceFragment::class.java.simpleName

		fun newInstance() = CameraSurfaceFragment().apply {
			arguments = Bundle().apply {
				// 今は何もない
			}
		}
	}

}
