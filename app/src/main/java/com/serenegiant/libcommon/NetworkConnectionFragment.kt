package com.serenegiant.libcommon

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.serenegiant.net.ConnectivityHelper
import com.serenegiant.net.ConnectivityHelper.ConnectivityCallback

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
*/ /**
 * A simple [BaseFragment] subclass.
 * Use the [NetworkConnectionFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
@Suppress("DEPRECATION")
class NetworkConnectionFragment : BaseFragment() {

	private var mHelper: ConnectivityHelper? = null

	override fun onAttach(context: Context) {
		super.onAttach(context)
		requireActivity().title = getString(R.string.title_network_connection)
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		if (mHelper == null) {
			mHelper = ConnectivityHelper(requireContext(), mConnectivityCallback)
		}
	}

	override fun onCreateView(inflater: LayoutInflater,
		container: ViewGroup?, savedInstanceState: Bundle?): View? {

		return inflater.inflate(R.layout.fragment_network_connection, container, false)
	}

	override fun internalOnResume() {
		super.internalOnResume()
		if (DEBUG) Log.v(TAG, "isWifiNetworkReachable:"
			+ ConnectivityHelper.isWifiNetworkReachable(requireContext()))
		if (DEBUG) Log.v(TAG, "isMobileNetworkReachable:"
			+ ConnectivityHelper.isMobileNetworkReachable(requireContext()))
		if (DEBUG) Log.v(TAG, "isNetworkReachable:"
			+ ConnectivityHelper.isNetworkReachable(requireContext()))
	}

	override fun internalOnPause() {
		super.internalOnPause()
	}

	override fun internalRelease() {
		if (mHelper != null) {
			mHelper!!.release()
			mHelper = null
		}
		super.internalRelease()
	}

	private val mConnectivityCallback: ConnectivityCallback
		= object : ConnectivityCallback {

		override fun onNetworkChanged(activeNetworkType: Int) {
			if (DEBUG) Log.v(TAG, "onNetworkChanged:"
				+ ConnectivityHelper.getNetworkTypeString(activeNetworkType))
		}

		override fun onError(t: Throwable) {
			Log.w(TAG, t)
		}
	}

	companion object {
		private const val DEBUG = true // set false on production
		private val TAG = NetworkConnectionFragment::class.java.simpleName

		/**
		 * Use this factory method to create a new instance of
		 * this fragment using the provided parameters.
		 *
		 * @return A new instance of fragment NetworkConnectionFragment.
		 */
		fun newInstance(): NetworkConnectionFragment {
			val fragment = NetworkConnectionFragment()
			val args = Bundle()
			fragment.arguments = args
			return fragment
		}
	}
}