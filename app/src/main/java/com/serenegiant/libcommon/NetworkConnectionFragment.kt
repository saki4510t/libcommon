package com.serenegiant.libcommon

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.serenegiant.net.ConnectivityHelper
import com.serenegiant.net.ConnectivityHelper.ConnectivityCallback
import com.serenegiant.net.NetworkUtils
import com.serenegiant.net.WifiApUtils
import com.serenegiant.system.BuildCheck

/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2022 saki t_saki@serenegiant.com
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
		Log.i(TAG, "dumpAll")
		NetworkUtils.dumpAll()
		Log.i(TAG, "dumpIpV4All")
		val ipv4s = NetworkUtils.getLocalIPv4Addresses()
		for (addr in ipv4s) {
			NetworkUtils.dump(addr)
		}
		for (addr in ipv4s) {
			Log.i(TAG, NetworkUtils.toString(addr))
		}
		Log.i(TAG, "dumpIpV6All")
		val ipv6s = NetworkUtils.getLocalIPv6Addresses()
		for (addr in ipv6s) {
			NetworkUtils.dump(addr)
		}
		for (addr in ipv6s) {
			Log.i(TAG, NetworkUtils.toString(addr))
		}
		Log.i(TAG, "dumpLocal")
		val locals = NetworkUtils.getLocalAddresses()
		for (addr in locals) {
			NetworkUtils.dump(addr)
		}
		val context = requireContext()
		if (BuildCheck.isAPI21()) {
			Log.i(TAG, "getNetworkAll")
			for (network in ConnectivityHelper.getNetworkAll(context)) {
				Log.i(TAG, "$network")
			}
			Log.i(TAG, "getLinkPropertiesAll")
			for (linkProperties in ConnectivityHelper.getLinkPropertiesAll(context)) {
				Log.i(TAG, "$linkProperties")
			}
			Log.i(TAG, "getNetworkCapabilitiesAll")
			for (caps in ConnectivityHelper.getNetworkCapabilitiesAll(context)) {
				Log.i(TAG, "$caps")
			}
		}
		try {
			Log.i(TAG, "SoftAp state=${WifiApUtils.getWifiApStateString(context)}")
			Log.i(TAG, "SoftAp config=${WifiApUtils.getWifiApConfiguration(context)}")
		} catch (e: Exception) {
			Log.w(TAG, e);
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

		override fun onNetworkChanged(activeNetworkType: Int, prevNetworkType: Int) {
			if (DEBUG) Log.v(TAG, "onNetworkChanged:"
				+ ConnectivityHelper.getNetworkTypeString(activeNetworkType))
			if (DEBUG) Log.v(TAG, "isWifiNetworkReachable:"
				+ ConnectivityHelper.isWifiNetworkReachable(requireContext()))
			if (DEBUG) Log.v(TAG, "isMobileNetworkReachable:"
				+ ConnectivityHelper.isMobileNetworkReachable(requireContext()))
			if (DEBUG) Log.v(TAG, "isNetworkReachable:"
				+ ConnectivityHelper.isNetworkReachable(requireContext()))
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
