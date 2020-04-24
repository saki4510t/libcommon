package com.serenegiant.libcommon
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
*/

import android.content.Context
import android.hardware.usb.UsbDevice
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.serenegiant.usb.DeviceFilter
import com.serenegiant.usb.USBMonitor
import com.serenegiant.usb.USBMonitor.OnDeviceConnectListener
import com.serenegiant.usb.USBMonitor.UsbControlBlock
import com.serenegiant.view.ViewUtils

/**
 * A simple [BaseFragment] subclass.
 */
class UsbFragment : BaseFragment() {

	private var mUSBMonitor: USBMonitor? = null

	override fun onAttach(context: Context) {
		super.onAttach(context)
		if (DEBUG) Log.v(TAG, "onAttach:$mUSBMonitor")
		requireActivity().title = getString(R.string.title_usB_monitor)
		if (mUSBMonitor == null) {
			mUSBMonitor = USBMonitor(context, mOnDeviceConnectListener)
			var filters
				 = DeviceFilter.getDeviceFilters(context, R.xml.device_filter_uvc_exclude)
			mUSBMonitor!!.setDeviceFilter(filters)
			if (DEBUG) Log.v(TAG, "onAttach:uvc_exclude=$filters")
			filters = DeviceFilter.getDeviceFilters(context, R.xml.device_filter_uvc)
			if (DEBUG) Log.v(TAG, "onAttach:uvc=$filters")
			filters = DeviceFilter.getDeviceFilters(context, R.xml.device_filter_uac)
			if (DEBUG) Log.v(TAG, "onAttach:uac=$filters")
			// API>=28以上でclass,subclassが0になってのでinterfaceClass等を追加した定義の読み込みテスト
			filters  = DeviceFilter.getDeviceFilters(context, R.xml.device_filter_uvc_exclude_intf)
			mUSBMonitor!!.setDeviceFilter(filters)
			if (DEBUG) Log.v(TAG, "onAttach:uvc_exclude_sub=$filters")
			filters = DeviceFilter.getDeviceFilters(context, R.xml.device_filter_uvc_intf)
			if (DEBUG) Log.v(TAG, "onAttach:uvc_sub=$filters")
			filters = DeviceFilter.getDeviceFilters(context, R.xml.device_filter_uac_intf)
			if (DEBUG) Log.v(TAG, "onAttach:uac_sub=$filters")
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		if (DEBUG) Log.v(TAG, "onCreate:")
		if ((mUSBMonitor != null) && !mUSBMonitor!!.isRegistered) {
			if (DEBUG) Log.v(TAG, "onCreate:register USBMonitor")
			mUSBMonitor!!.register()
		}
	}

	override fun onCreateView(inflater: LayoutInflater,
		container: ViewGroup?, savedInstanceState: Bundle?): View? {

		if (DEBUG) Log.v(TAG, "onCreateView:")
		val customInflater
			= ViewUtils.createCustomLayoutInflater(requireContext(), inflater, R.style.AppTheme_Usb)
		return customInflater.inflate(R.layout.fragment_usb, container, false)
	}

	override fun internalOnResume() {
		super.internalOnResume()
		if (DEBUG) Log.v(TAG, "internalOnResume:")
	}

	override fun internalOnPause() {
		if (DEBUG) Log.v(TAG, "internalOnPause:")
		super.internalOnPause()
	}

	override fun onDestroy() {
		if (DEBUG) Log.v(TAG, "onDestroy:")
		try {
			if ((mUSBMonitor != null) && mUSBMonitor!!.isRegistered) {
				if (DEBUG) Log.v(TAG, "onDestroy:unregister USBMonitor")
				mUSBMonitor!!.unregister()
			}
		} catch (e: Exception) {
			Log.w(TAG, e)
		}
		super.onDestroy()
	}

	override fun onDetach() {
		if (DEBUG) Log.v(TAG, "onDetach:")
		if (mUSBMonitor != null) {
			mUSBMonitor!!.destroy()
			mUSBMonitor = null
		}
		super.onDetach()
	}

	//--------------------------------------------------------------------------------
	private val mOnDeviceConnectListener: OnDeviceConnectListener
		= object : OnDeviceConnectListener {

		override fun onAttach(device: UsbDevice) {
			if (DEBUG) Log.v(TAG, "OnDeviceConnectListener:onAttach:${device}")
			// USB機器が接続された時
		}

		override fun onPermission(device: UsbDevice) {
			if (DEBUG) Log.v(TAG, "OnDeviceConnectListener:onAttach:${device}")
			// パーミッションを取得できた時
		}

		override fun onConnected(device: UsbDevice, ctrlBlock: UsbControlBlock) {
			if (DEBUG) Log.v(TAG, "OnDeviceConnectListener:onConnected:${device}")
			// USB機器がopenした時
		}

		override fun onDisconnect(device: UsbDevice) {
			if (DEBUG) Log.v(TAG, "OnDeviceConnectListener:onDisconnect:${device}")
			// USB機器がcloseした時
		}

		override fun onDetach(device: UsbDevice) {
			if (DEBUG) Log.v(TAG, "OnDeviceConnectListener:onDetach:${device}")
			// USB機器が取り外された時
		}

		override fun onCancel(device: UsbDevice) {
			if (DEBUG) Log.v(TAG, "onCancel:${device}")
			// パーミッションを取得できなかった時
		}

		override fun onError(usbDevice: UsbDevice, throwable: Throwable) {
			Log.w(TAG, throwable)
		}
	}

	companion object {
		private const val DEBUG = true // TODO set false on release
		private val TAG = UsbFragment::class.java.simpleName

		fun newInstance(): UsbFragment {
			val fragment = UsbFragment()
			val args = Bundle()
			fragment.arguments = args
			return fragment
		}
	}
}