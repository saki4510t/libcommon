package com.serenegiant.usb;
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
 *
 *  moved from aAndUsb
*/

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.text.TextUtils;
import android.util.Log;

import com.serenegiant.nio.CharsetsUtils;
import com.serenegiant.system.BuildCheck;
import com.serenegiant.system.ContextUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class UsbUtils implements Const {
	private static final String TAG = UsbUtils.class.getSimpleName();

	private UsbUtils() {
		// インスタンス化をエラーにするためにデフォルトコンストラクタをprivateに
	}

	/**
	 * 指定したIDのStringディスクリプタから文字列を取得する。取得できなければnull
	 * @param connection
	 * @param id
	 * @param languageCount
	 * @param languages
	 * @return
	 */
	public static String getString(@NonNull final UsbDeviceConnection connection,
		final int id, final int languageCount, final byte[] languages) {

		final byte[] work = new byte[256];
		String result = null;
		for (int i = 1; i <= languageCount; i++) {
			int ret = connection.controlTransfer(
				USB_REQ_STANDARD_DEVICE_GET, // USB_DIR_IN | USB_TYPE_STANDARD | USB_RECIP_DEVICE
				USB_REQ_GET_DESCRIPTOR,
				(USB_DT_STRING << 8) | id, languages[i], work, 256, 0);
			if ((ret > 2) && (work[0] == ret) && (work[1] == USB_DT_STRING)) {
				// skip first two bytes(bLength & bDescriptorType), and copy the rest to the string
				result = new String(work, 2, ret - 2, CharsetsUtils.UTF16LE/*"UTF-16LE"*/);
				if (!"Љ".equals(result)) {	// 変なゴミが返ってくる時がある
					break;
				} else {
					result = null;
				}
			}
		}
		return result;
	}

	/**
	 * 指定したUsbDeviceが指定したクラス・サブクラス・プロトコルに対応しているかどうかを取得
	 * @param device
	 * @param clazz 負数ならワイルドカード
	 * @param subClass 負数ならワイルドカード
	 * @param protocol 負数ならワイルドカード
	 * @return
	 */
	public static boolean isSupported(@NonNull final UsbDevice device,
		final int clazz, final int subClass, final int protocol) {

		// デバイスクラス・デバイスサブクラス・デバイスプロトコルが一致するかどうかをチェック
		if (((clazz < 0) || (clazz == device.getDeviceClass()))
			&& ((subClass < 0) || (subClass == device.getDeviceSubclass()))
			&& ((protocol < 0) || (protocol == device.getDeviceProtocol()))) {
			return true;
		}
		// インターフェースクラス・インターフェースサブクラス・インターフェースプロトコルが一致するかどうかをチェック
		final int n = device.getInterfaceCount();
		for (int i = 0; i < n; i++) {
			final UsbInterface intf = device.getInterface(i);
			if (((clazz < 0) || (clazz == intf.getInterfaceClass()))
				&& ((subClass < 0) || (subClass == intf.getInterfaceSubclass()))
				&& ((protocol < 0) || (protocol == intf.getInterfaceProtocol()))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * UVC機器かどうかを取得
	 * @param device
	 * @return
	 */
	public static boolean isUVC(@NonNull final UsbDevice device) {
		return isSupported(device, 14, 1, -1)	// UVC Video control interface
			|| isSupported(device, 14, 2, -1);	// UVC Video stream interface
	}

	/**
	 * UAC機器かどうかを取得
	 * FIXME UACブラックリスト機器(UAC対応のインターフェースを持ってるけど
	 *       使おうとすると動かない・ストールする・クラッシュする機器)をオミットする仕組みを作る
	 * @param device
	 * @return
	 */
	public static boolean isUAC(@NonNull final UsbDevice device) {
		return isSupported(device, 1, 1, -1)		// UAC Audio control interface
			|| isSupported(device, 1, 2, -1);	// UAC Audio stream interface
	}

	/**
	 * 指定したUsbDeviceが指定したクラス・サブクラス・プロトコルに対応したUsbInterfaceリストを取得する
	 * @param device
	 * @param clazz 負数ならワイルドカード
	 * @param subClass 負数ならワイルドカード
	 * @param protocol 負数ならワイルドカード
	 * @return
	 */
	@NonNull
	public static List<UsbInterface> findInterfaces(
		@NonNull final UsbDevice device,
		final int clazz, final int subClass, final int protocol) {

		final List<UsbInterface> result = new ArrayList<>();
		// インターフェースクラス・インターフェースサブクラス・インターフェースプロトコルが一致するかどうかをチェック
		final int n = device.getInterfaceCount();
		for (int i = 0; i < n; i++) {
			final UsbInterface intf = device.getInterface(i);
			if (((clazz < 0) || (clazz == intf.getInterfaceClass()))
				&& ((subClass < 0) || (subClass == intf.getInterfaceSubclass()))
				&& ((protocol < 0) || (protocol == intf.getInterfaceProtocol()))) {
				result.add(intf);
			}
		}

		return result;
	}

	/**
	 * UVCのインターフェースを取得
	 * @param device
	 * @return
	 */
	@Nullable
	public static List<UsbInterface> findUVCInterfaces(@NonNull final UsbDevice device) {
		// UVC Video control interface
		final List<UsbInterface> result
			= new ArrayList<>(findInterfaces(device, 14, 1, -1));
		result.addAll(findInterfaces(device, 14, 2, -1));

		return result;
	}

	/**
	 * UACのインターフェースを取得
	 * @param device
	 * @return
	 */
	@Nullable
	public static List<UsbInterface> findUACInterfaces(@NonNull final UsbDevice device) {
		// UAC Audio control interface
		final List<UsbInterface> result
			= new ArrayList<>(findInterfaces(device, 1, 1, -1));
		// UAC Audio stream interface
		result.addAll(findInterfaces(device, 1, 2, -1));

		return result;
	}

//--------------------------------------------------------------------------------
	/**
	 * デバイスキー名を取得する
	 * @param connector
	 * @return
	 */
	@NonNull
	public static String getDeviceKeyName(
		@NonNull final UsbConnector connector) {

		return getDeviceKeyName(connector.getInfo());
	}

	/**
	 * デバイスキーを取得する
	 * @param connector
	 * @return
	 */
	public static int getDeviceKey(
		@NonNull final UsbConnector connector) {

		return getDeviceKeyName(connector).hashCode();
	}

	/**
	 * デバイスキー名を取得する
	 * @param context
	 * @param device
	 * @return
	 */
	@NonNull
	public static String getDeviceKeyName(
		@NonNull final Context context,
		@Nullable final UsbDevice device) {

		return getDeviceKeyName(UsbDeviceInfo.getDeviceInfo(context, device));
	}

	/**
	 * デバイスキーを取得する
	 * @param context
	 * @param device
	 * @return
	 */
	public static int getDeviceKey(
		@NonNull final Context context,
		@Nullable final UsbDevice device) {

		return getDeviceKeyName(UsbDeviceInfo.getDeviceInfo(context, device)).hashCode();
	}

	/**
	 * デバイスキー名を取得する
	 * @param info
	 * @return
	 */
	@NonNull
	public static String getDeviceKeyName(@NonNull final UsbDeviceInfo info) {
		final UsbDevice device = info.device;
		if (device == null) return "";
		final StringBuilder sb = new StringBuilder();
		sb.append(device.getVendorId()).append("#")			// API >= 12
			.append(device.getProductId()).append("#")		// API >= 12
			.append(device.getDeviceClass()).append("#")	// API >= 12
			.append(device.getDeviceSubclass()).append("#")	// API >= 12
			.append(device.getDeviceProtocol());			// API >= 12
		if (!TextUtils.isEmpty(info.serial)) {
			sb.append("#");	sb.append(info.serial);
		}
		if (BuildCheck.isAndroid5()) {
			if (!TextUtils.isEmpty(info.manufacturer)) {
				sb.append("#").append(info.manufacturer);
			}
			if (info.configCounts >= 0) {
				sb.append("#").append(info.configCounts);
			}
			if (!TextUtils.isEmpty(info.version)) {
				sb.append("#").append(info.version);
			}
		}
		return sb.toString();
	}

	/**
	 * デバイスキーを取得する
	 * @param info
	 * @return
	 */
	public static int getDeviceKey(@NonNull final UsbDeviceInfo info) {
		return getDeviceKeyName(info).hashCode();
	}

//--------------------------------------------------------------------------------
	/**
	 * 指定したデバイス名に対応するUsbDeviceを取得する
	 * @param name　UsbDevice#getDeviceNameで取得できる値
	 * @return 見つからなければnull
	 */
	@Nullable
	public static UsbDevice findDevice(@NonNull final List<UsbDevice> devices, final String name) {
		UsbDevice result = null;
		for (final UsbDevice device: devices) {
			if (device.getDeviceName().equals(name)) {
				result = device;
				break;
			}
		}
		return result;
	}

//--------------------------------------------------------------------------------
	/**
	 * 接続されているUSBの機器リストをLogCatに出力
	 * @param context
	 */
	public static void dumpDevices(@NonNull final Context context) {
		final UsbManager usbManager = ContextUtils.requireSystemService(context, UsbManager.class);
		final HashMap<String, UsbDevice> list = usbManager.getDeviceList();
		if ((list != null) && !list.isEmpty()) {
			final Set<String> keys = list.keySet();
			if (keys != null && !keys.isEmpty()) {
				final StringBuilder sb = new StringBuilder();
				for (final String key: keys) {
					final UsbDevice device = list.get(key);
					final int num_interface = device != null ? device.getInterfaceCount() : 0;
					sb.setLength(0);
					for (int i = 0; i < num_interface; i++) {
						sb.append(String.format(Locale.US, "interface%d:%s",
							i, device.getInterface(i)));
					}
					Log.i(TAG, "key=" + key + ":" + device + ":" + sb);
				}
			} else {
				Log.i(TAG, "no device");
			}
		} else {
			Log.i(TAG, "no device");
		}
	}

}
