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
import android.hardware.usb.UsbDevice
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.serenegiant.app.ActivityUtils
import com.serenegiant.libcommon.databinding.FragmentUsbMonitorBinding
import com.serenegiant.usb.DeviceFilter
import com.serenegiant.usb.USBMonitor
import com.serenegiant.usb.UsbConnector
import com.serenegiant.usb.UsbDeviceInfo
import com.serenegiant.usb.UsbPermission
import com.serenegiant.usb.UsbUtils
import com.serenegiant.utils.BufferHelper
import com.serenegiant.view.ViewUtils
import kotlinx.coroutines.launch

/**
 * A simple [BaseFragment] subclass.
 */
@SuppressLint("SetTextI18n")
class UsbMonitorFragment : BaseFragment() {

	private lateinit var mUSBMonitor: USBMonitor
	private lateinit var mBinding: FragmentUsbMonitorBinding

	override fun onAttach(context: Context) {
		super.onAttach(context)
		if (DEBUG) Log.v(TAG, "onAttach:")
		requireActivity().title = getString(R.string.title_usb_monitor)
		mUSBMonitor = USBMonitor(requireActivity(), mOnDeviceConnectListener)
		var filters
			 = DeviceFilter.getDeviceFilters(context, R.xml.device_filter_uvc_exclude)
		mUSBMonitor.setDeviceFilter(filters)
		if (DEBUG) Log.v(TAG, "onAttach:uvc_exclude=$filters")
		filters = DeviceFilter.getDeviceFilters(context, R.xml.device_filter_uvc)
		if (DEBUG) Log.v(TAG, "onAttach:uvc=$filters")
		filters = DeviceFilter.getDeviceFilters(context, R.xml.device_filter_uac)
		if (DEBUG) Log.v(TAG, "onAttach:uac=$filters")
	}

	override fun onCreateView(inflater: LayoutInflater,
		container: ViewGroup?, savedInstanceState: Bundle?): View? {

		if (DEBUG) Log.v(TAG, "onCreateView:")
		val customInflater
			= ViewUtils.createCustomLayoutInflater(requireContext(), inflater, R.style.AppTheme_Usb)
		return DataBindingUtil.inflate<FragmentUsbMonitorBinding?>(
			customInflater,
			R.layout.fragment_usb_monitor, container, false
		).apply {
			mBinding = this
		}.run {
			root
		}
	}

	override fun internalOnResume() {
		super.internalOnResume()
		if (DEBUG) Log.v(TAG, "internalOnResume:")
		if (!mUSBMonitor.isRegistered) {
			if (DEBUG) Log.v(TAG, "onCreate:register USBMonitor")
			mBinding.message.text = "${mBinding.message.text}\nregister USBMonitor"
			mUSBMonitor.register()
		}
	}

	override fun internalOnPause() {
		if (DEBUG) Log.v(TAG, "internalOnPause:")
		if (mUSBMonitor.isRegistered) {
			if (DEBUG) Log.v(TAG, "onDestroy:unregister USBMonitor")
			mBinding.message.text = "${mBinding.message.text}\nunregister USBMonitor"
			mUSBMonitor.unregister()
		}
		super.internalOnPause()
	}

	override fun onDetach() {
		if (DEBUG) Log.v(TAG, "onDetach:")
		mBinding.message.text = "${mBinding.message.text}\ndestroy USBMonitor"
		mUSBMonitor.destroy()
		super.onDetach()
	}

	//--------------------------------------------------------------------------------
	private val mOnDeviceConnectListener: USBMonitor.Callback
		= object : USBMonitor.Callback {

		override fun onAttach(device: UsbDevice) {
			if (DEBUG) Log.v(TAG, "OnDeviceConnectListener:onAttach:${device.deviceName}")
			lifecycleScope.launch {
				mBinding.message.text = "${mBinding.message.text}\nonAttach:${device.deviceName}"
			}
			// USB機器が接続された時
			if (false) {
				// こっちは通常の使い方
				mUSBMonitor.requestPermission(device)
			} else {
				// staticメソッド版のrequestPermissionを使って違うContextからパーミッション要求する場合
				// こっちでパーミッション要求した場合でもUSBMonitor#registerで登録したBroadcastReceiverで
				// 結果を受け取れる
				UsbPermission.requestPermission(requireActivity(), device, mOnDeviceConnectListener2, 5000)
			}
		}

		override fun onPermission(device: UsbDevice) {
			if (DEBUG) Log.v(TAG, "OnDeviceConnectListener#onPermission:${device.deviceName}")
			if (DEBUG) Log.v(TAG, "OnDeviceConnectListener#onPermission:パーミッションを取得できた時, openする")
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
				mBinding.message.text = "${mBinding.message.text}\nonPermission:${device.deviceName}"
			}
			// XXX USBMonitor#openDeviceを呼ぶと#onConnected/#onDisconnectedの呼び出し処理が含まれる特別な
			//     UsbConnectorオブジェクトが生成される
			//     一方UsbConnector(Context,UsbDevice)コンストラクタで生成すると#onConnected/#onDisconnectedが
			//     呼び出されない素のUsbConnectorを生成できるがこの場合は自前でライフサイクルの管理が必要になる。
			val connector = mUSBMonitor.openDevice(device)
			if (DEBUG) Log.v(TAG, "OnDeviceConnectListener#onPermission:${connector}")
			if (UsbUtils.isUVC(device)) {
				val intfs = UsbUtils.findUVCInterfaces(device)
				if (DEBUG) Log.v(TAG, "OnDeviceConnectListener#onPermission:uvc=$intfs")
			}
			if (UsbUtils.isUAC(device)) {
				val intfs = UsbUtils.findUACInterfaces(device)
				if (DEBUG) Log.v(TAG, "OnDeviceConnectListener#onPermission:uac=$intfs")
			}
		}

		override fun onConnected(device: UsbDevice, connector: UsbConnector) {
			if (DEBUG) Log.v(TAG, "OnDeviceConnectListener#onConnected:${device.deviceName}")
			lifecycleScope.launch {
				mBinding.message.text = "${mBinding.message.text}\nonConnected:${device.deviceName}"
			}
			// XXX このコールバック関数に引き渡されるUsbConnectorはUSBMonitor#UsbControlBlockで
			//     #onConnected/#onDisconnectを呼び出すためにUsbConnectorを拡張したオブジェクトである。
			//     なのでこの関数内でconnector#cloneやUSBMonitor#openを呼んで再度UsbConnectorを
			//     生成すると連鎖的に#onConnectedコールバックが呼ばれてしまうことになる。
			//     ・この関数内でconnector#cloneやUSBMonitor#openを呼ばないようにする
			//     ・連鎖してopenを繰り返していないか自前でチェックする
			//     ・#onConnected/#onDisconnectが呼び出されないようにUsbConnector(Context,UsbDevice)でUsbConnectorを生成する
			//     などで意図せずに連鎖的にopen(onConnected)が繰り返されないようにする必要がある
			if (DEBUG) Log.v(TAG, "OnDeviceConnectListener#nPermission:close connector")
			// 元のUsbConnectorはcloseする
			connector.close()
			if (DEBUG) Log.v(TAG, "OnDeviceConnectListener#nPermission:create connector")
			val newConnector = UsbConnector(requireContext(), device)
			// 新規作成したUsbConnectorを使ってアクセスする
			val connection = newConnector.connection
			val info = UsbDeviceInfo.getDeviceInfo(newConnector, null)
			if (DEBUG) {
				Log.v(TAG, "info=$info")
				Log.v(TAG, "device=$device")
				lifecycleScope.launch {
					mBinding.message.text = "${mBinding.message.text}\ninfo($info)"
				}
				if (connection != null) {
					try {
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
					} catch (e: Exception) {
						Log.w(TAG, e)
					}
				}
			}
			// USB機器がopenした時,1秒後にcloseする
			queueEvent({
				if (DEBUG) Log.v(TAG, "OnDeviceConnectListener#onConnected:close newConnector")
				newConnector.close()
			}, 1000)
		}

		override fun onDisconnect(device: UsbDevice) {
			if (DEBUG) Log.v(TAG, "OnDeviceConnectListener#onDisconnect:${device.deviceName}")
			// USB機器がcloseした時
			lifecycleScope.launch {
				mBinding.message.text = "${mBinding.message.text}\nonDisconnect:${device.deviceName}"
			}
		}

		override fun onDetach(device: UsbDevice) {
			if (DEBUG) Log.v(TAG, "OnDeviceConnectListener#onDetach:${device.deviceName}")
			// USB機器が取り外された時
			lifecycleScope.launch {
				mBinding.message.text = "${mBinding.message.text}\nonDetach:${device.deviceName}"
			}
		}

		override fun onCancel(device: UsbDevice) {
			if (DEBUG) Log.v(TAG, "OnDeviceConnectListener#onCancel:${device.deviceName}")
			// パーミッションを取得できなかった時
			lifecycleScope.launch {
				mBinding.message.text = "${mBinding.message.text}\nonCancel:${device.deviceName}"
			}
		}

		override fun onError(usbDevice: UsbDevice?, throwable: Throwable) {
			Log.w(TAG, throwable)
			lifecycleScope.launch {
				mBinding.message.text = "${mBinding.message.text}\nonError:${usbDevice?.deviceName}"
			}
		}
	}

	private val mOnDeviceConnectListener2 = object : USBMonitor.PermissionCallback() {
		override fun onPermission(device: UsbDevice) {
			if (DEBUG) Log.v(TAG, "mOnDeviceConnectListener2#onPermission:${device.deviceName}")
		}

		override fun onCancel(device: UsbDevice) {
			if (DEBUG) Log.v(TAG, "mOnDeviceConnectListener2#onCancel:${device.deviceName}")
			// パーミッションを取得できなかった時
		}

		override fun onError(device: UsbDevice?, t: Throwable) {
			Log.w(TAG, t)
		}
	}

	companion object {
		private const val DEBUG = true // TODO set false on release
		private val TAG = UsbMonitorFragment::class.java.simpleName

		fun newInstance() = UsbMonitorFragment().apply {
			arguments = Bundle().apply {
				// 今は何もない
			}
		}
	}
}
