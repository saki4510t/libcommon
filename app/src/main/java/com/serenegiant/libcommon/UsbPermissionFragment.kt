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
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.serenegiant.app.ActivityUtils
import com.serenegiant.libcommon.databinding.FragmentUsbPermissionBinding
import com.serenegiant.usb.DeviceFilter
import com.serenegiant.usb.UsbConnector
import com.serenegiant.usb.UsbDetector
import com.serenegiant.usb.UsbDeviceInfo
import com.serenegiant.usb.UsbPermission
import com.serenegiant.usb.UsbUtils
import com.serenegiant.utils.BufferHelper
import com.serenegiant.view.ViewUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * UsbDetectorでUSB機器(UVC機器)の接続を検知
 * ＆ ローカルブロードキャストでMainActivityへUSB機器アクセスパーミッション要求
 * ＆ UsbPermissionでパーミッション結果取得する
 */
@SuppressLint("SetTextI18n")
class UsbPermissionFragment : BaseFragment() {

	private lateinit var mUsbDetector: UsbDetector
	private lateinit var mUsbPermission: UsbPermission
	private lateinit var mBinding: FragmentUsbPermissionBinding

	override fun onAttach(context: Context) {
		super.onAttach(context)
		if (DEBUG) Log.v(TAG, "onAttach:")
		requireActivity().title = getString(R.string.title_usb_permission)
		mUsbDetector = UsbDetector(context, mOnDeviceConnectListener)
		var filters = DeviceFilter.getDeviceFilters(context, R.xml.device_filter_uvc_exclude)
		mUsbDetector.setDeviceFilter(filters)
		if (DEBUG) Log.v(TAG, "onAttach:uvc_exclude=$filters")
		filters = DeviceFilter.getDeviceFilters(context, R.xml.device_filter_uvc)
		if (DEBUG) Log.v(TAG, "onAttach:uvc=$filters")
		filters = DeviceFilter.getDeviceFilters(context, R.xml.device_filter_uac)
		if (DEBUG) Log.v(TAG, "onAttach:uac=$filters")
		mUsbPermission = UsbPermission(context, mUsbPermissionCallback)
	}

	override fun onCreateView(inflater: LayoutInflater,
		container: ViewGroup?, savedInstanceState: Bundle?): View? {

		if (DEBUG) Log.v(TAG, "onCreateView:")
		val customInflater
			= ViewUtils.createCustomLayoutInflater(requireContext(), inflater, R.style.AppTheme_Usb)
		return DataBindingUtil.inflate<FragmentUsbPermissionBinding?>(
			customInflater,
			R.layout.fragment_usb_permission, container, false
		).apply {
			mBinding = this
		}.run {
			root
		}
	}

	override fun internalOnResume() {
		super.internalOnResume()
		if (DEBUG) Log.v(TAG, "internalOnResume:")
		if (!mUsbDetector.isRegistered) {
			if (DEBUG) Log.v(TAG, "onCreate:register UsbDetector")
			mUsbDetector.register()
			mBinding.message.text = "${mBinding.message.text}\nregister UsbDetector"
		}
		if (!mUsbPermission.isRegistered) {
			if (DEBUG) Log.v(TAG, "onCreate:register UsbPermission")
			mUsbPermission.register()
			mBinding.message.text = "${mBinding.message.text}\nregister UsbPermission"
		}
	}

	override fun internalOnPause() {
		if (DEBUG) Log.v(TAG, "internalOnPause:")
		if (mUsbDetector.isRegistered) {
			mBinding.message.text = "${mBinding.message.text}\nunregister UsbDetector"
			mUsbDetector.unregister()
		}
		if (mUsbPermission.isRegistered) {
			mBinding.message.text = "${mBinding.message.text}\nunregister UsbPermission"
			mUsbPermission.unregister()
		}
		super.internalOnPause()
	}

	override fun onDetach() {
		if (DEBUG) Log.v(TAG, "onDetach:")
		mUsbDetector.release()
		mUsbPermission.release()
		super.onDetach()
	}

	//--------------------------------------------------------------------------------
	private val mOnDeviceConnectListener: UsbDetector.Callback
		= object : UsbDetector.Callback {

		override fun onAttach(device: UsbDevice) {
			if (DEBUG) Log.v(TAG, "UsbDetector.Callback#onAttach:${device.deviceName}")
			// USB機器が接続された時
			// ここはActivityのコンテキスト内だから直接UsbPermission#requestPermissionを呼び出してもOK
			// サービスのコンテキストやアプリケーションコンテキストなどどのコンテキストかわからない場合を
			// 想定してローカルブロードキャストでActivityへIntentを送って処理して貰う
			// XXX Activityのコンテキスト以外からパーミッション要求等のシステムダイアログ/システムUIが
			//     表示される処理を要求すると、ダイアログ等がアプリの背面に回ってしまう場合がある
			LocalBroadcastManager
				.getInstance(requireActivity())
				.sendBroadcast(Intent(Const.ACTION_REQUEST_USB_PERMISSION).apply {
					putExtra(Const.EXTRA_REQUEST_USB_PERMISSION, device)
				})
			lifecycleScope.launch {
				mBinding.message.text = "${mBinding.message.text}\nonAttach(${device.deviceName})"
			}
		}

		override fun onDetach(device: UsbDevice) {
			if (DEBUG) Log.v(TAG, "UsbDetector.Callback#onDetach:${device.deviceName}")
			// USB機器が取り外された時
			lifecycleScope.launch {
				mBinding.message.text = "${mBinding.message.text}\nonDetach(${device.deviceName})"
			}
		}

		override fun onError(usbDevice: UsbDevice?, throwable: Throwable) {
			Log.w(TAG, throwable)
		}
	}

	private val mUsbPermissionCallback: UsbPermission.Callback
		= object : UsbPermission.Callback {

		override fun onPermission(device: UsbDevice) {
			if (DEBUG) Log.v(TAG, "UsbPermission.Callback#onPermission:${device.deviceName}")
			// XXX 永続的USBアクセスパーミッションを取得できるようにしている場合、
			//     アプリからのUSBアクセスパーミッション要求ダイアログに加えて
			//     OS側からもUSBアクセスパーミッション要求ダイアログが表示される。
			//     (ディスプレー下部からのスライドイン)
			//     アプリ側のUSBアクセスパーミッション要求ダイアログで許可/キャンセルしても
			//     OS側のUSBアクセスパーミッション要求ダイアログが表示されたままなので
			//     自Activityをフォアグラウンドへ移動させることでOS側のダイアログを
			//     非表示にする。
			ActivityUtils.bringToForeground(requireActivity())
			lifecycleScope.launch {
				mBinding.message.text = "${mBinding.message.text}\nonPermission(${device.deviceName})"
			}
			// パーミッションを取得できた時, openする
			val connector = UsbConnector(requireContext(), device)
			if (UsbUtils.isUVC(device)) {
				val intfs = UsbUtils.findUVCInterfaces(device)
				if (DEBUG) Log.v(TAG, "UsbPermission.Callback#onPermission:uvc=$intfs")
			}
			if (UsbUtils.isUAC(device)) {
				val intfs = UsbUtils.findUACInterfaces(device)
				if (DEBUG) Log.v(TAG, "UsbPermission.Callback#onPermission:uac=$intfs")
			}
			// テストのためにクローンする
			if (DEBUG) Log.v(TAG, "UsbPermission.Callback#nPermission:clone connector")
			val cloned = connector.clone()
			// 元のUsbConnectorはcloseする
			if (DEBUG) Log.v(TAG, "UsbPermission.Callback#onPermission:close connector")
			connector.close()
			// クローンしたUsbConnectorを使ってアクセスする
			val info = cloned.info
			if (DEBUG) {
				val connection = cloned.connection
				Log.v(TAG, "info=$info")
				Log.v(TAG, "device=$device")
				lifecycleScope.launch {
					mBinding.message.text = "${mBinding.message.text}\ninfo($info)"
				}
				if (connection != null) {
					Log.v(TAG, String.format("bcdUSB=0x%04x", UsbDeviceInfo.getBcdUSB(connection)))
					Log.v(TAG, String.format("class=0x%02x", UsbDeviceInfo.getDeviceClass(connection)))
					Log.v(TAG, String.format("subClass=0x%02x", UsbDeviceInfo.getDeviceSubClass(connection)))
					Log.v(TAG, String.format("protocol=0x%02x", UsbDeviceInfo.getDeviceProtocol(connection)))
					Log.v(TAG, String.format("vendorId=0x%04x", UsbDeviceInfo.getVendorId(connection)))
					Log.v(TAG, String.format("productId=0x%04x", UsbDeviceInfo.getProductId(connection)))
					Log.v(TAG, String.format("bcdDevice=0x%04x", UsbDeviceInfo.getBcdDevice(connection)))
					Log.v(TAG, "vendorName=${UsbDeviceInfo.getVendorName(connection)}")
					Log.v(TAG, "productName=${UsbDeviceInfo.getProductName(connection)}")
					Log.v(TAG, "serialNumber=${UsbDeviceInfo.getSerialNumber(connection)}")
					Log.v(TAG, "numConfigs=${UsbDeviceInfo.getNumConfigurations(connection)}")
					Log.v(TAG, "descAll=${BufferHelper.toHexString(connection.rawDescriptors)}")
				}
			}
			// USB機器がopenした時,1秒後にcloseする
			lifecycleScope.launch(Dispatchers.Default) {
				delay(1000L)
				if (DEBUG) Log.v(TAG, "UsbPermission.Callback#onPermission:close cloned")
				cloned.close()
			}
		}

		override fun onCancel(device: UsbDevice) {
			if (DEBUG) Log.v(TAG, "UsbPermission.Callback#onCancel:${device.deviceName}")
			// パーミッションを取得できなかった時
			lifecycleScope.launch {
				mBinding.message.text = "${mBinding.message.text}\nonCancel(${device.deviceName}}"
			}
		}

		override fun onError(usbDevice: UsbDevice?, throwable: Throwable) {
			Log.w(TAG, throwable)
			lifecycleScope.launch {
				mBinding.message.text = "${mBinding.message.text}\nonError:${usbDevice?.deviceName}"
			}
		}
	}

	companion object {
		private const val DEBUG = true // TODO set false on release
		private val TAG = UsbPermissionFragment::class.java.simpleName

		fun newInstance() = UsbPermissionFragment().apply {
			arguments = Bundle().apply {
				// 今は何もない
			}
		}
	}
}
