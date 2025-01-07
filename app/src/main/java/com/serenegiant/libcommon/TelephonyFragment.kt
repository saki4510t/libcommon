package com.serenegiant.libcommon

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.serenegiant.telephony.TelephonyUtils

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
*/ /**
 * A simple [BaseFragment] subclass.
 * Use the [TelephonyFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class TelephonyFragment : BaseFragment() {

	override fun onAttach(context: Context) {
		super.onAttach(context)
		if (DEBUG) Log.v(TAG, "onAttach:")
		requireActivity().title = getString(R.string.title_telephony)
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		if (DEBUG) Log.v(TAG, "onCreate:")
	}

	override fun onCreateView(inflater: LayoutInflater,
		container: ViewGroup?, savedInstanceState: Bundle?): View? {

		if (DEBUG) Log.v(TAG, "onCreateView:")
		return inflater.inflate(R.layout.fragment_telephony, container, false)
	}

	@SuppressLint("MissingPermission")
	override fun internalOnResume() {
		super.internalOnResume()
		if (DEBUG) Log.v(TAG, "internalOnResume:")
		val context = requireContext()
		Log.i(TAG, "phone=" + TelephonyUtils.getPhoneNumber(context))
	}

	override fun internalOnPause() {
		if (DEBUG) Log.v(TAG, "internalOnPause:")
		super.internalOnPause()
	}

	override fun internalRelease() {
		if (DEBUG) Log.v(TAG, "internalRelease:")
		super.internalRelease()
	}

	override fun onDetach() {
		if (DEBUG) Log.v(TAG, "onDetach:")
		super.onDetach()
	}

	companion object {
		private const val DEBUG = true // set false on production
		private val TAG = TelephonyFragment::class.java.simpleName

		/**
		 * Use this factory method to create a new instance of
		 * this fragment using the provided parameters.
		 *
		 * @return A new instance of fragment NetworkConnectionFragment.
		 */
		fun newInstance() = TelephonyFragment().apply {
			arguments = Bundle().apply {
				// 今は何もない
			}
		}
	}
}
