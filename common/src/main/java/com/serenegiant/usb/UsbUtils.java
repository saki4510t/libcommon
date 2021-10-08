package com.serenegiant.usb;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2021 saki t_saki@serenegiant.com
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

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.text.TextUtils;
import android.util.Log;

import com.serenegiant.nio.CharsetsUtils;
import com.serenegiant.system.BuildCheck;
import com.serenegiant.system.ContextUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

@SuppressWarnings("deprecation")
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

//--------------------------------------------------------------------------------
	/**
	 * シリアルナンバーを取得できる機器の場合にはシリアルナンバーを含めたデバイスキーを取得する
	 * シリアルナンバーを取得できなければgetDeviceKeyNameと同じ
	 * @param context
	 * @param device
	 * @return
	 */
	@Deprecated
	@NonNull
	public static String getDeviceKeyNameWithSerial(
		@NonNull final Context context,
		@Nullable final UsbDevice device) {

		return getDeviceKeyName(UsbDeviceInfo.getDeviceInfo(context, device));
	}

	/**
	 * シリアルナンバーを取得できる機器の場合にはシリアルナンバーを含めたデバイスキーを整数として取得
	 * getDeviceKeyNameWithSerialで得られる文字列のhasCodeを取得
	 * シリアルナンバーを取得できなければgetDeviceKeyと同じ
	 * @return
	 */
	@Deprecated
	public static int getDeviceKeyWithSerial(@NonNull final Context context,
		@Nullable final UsbDevice device) {

		return getDeviceKeyNameWithSerial(context, device).hashCode();
	}

	/**
	 * デバイスキー名を取得する
	 * @param ctrlBlock
	 * @return
	 */
	@NonNull
	public static String getDeviceKeyName(
		@NonNull final USBMonitor.UsbControlBlock ctrlBlock) {

		return getDeviceKeyName(ctrlBlock.getInfo());
	}

	/**
	 * デバイスキーを取得する
	 * @param ctrlBlock
	 * @return
	 */
	public static int getDeviceKey(
		@NonNull final USBMonitor.UsbControlBlock ctrlBlock) {

		return getDeviceKeyName(ctrlBlock).hashCode();
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

	/**
	 * デバイスキー文字列を生成
	 * @param device
	 * @param serial
	 * @param manufactureName
	 * @param configCount
	 * @param deviceVersion
	 * @return
	 */
	@Deprecated
	@NonNull
	public static final String getDeviceKeyName(@Nullable final UsbDevice device,
		final boolean useNewAPI,
		final String serial, final String manufactureName,
		final int configCount, final String deviceVersion) {

		if (device == null) return "";
		final StringBuilder sb = new StringBuilder();
		sb.append(device.getVendorId()).append("#")			// API >= 12
			.append(device.getProductId()).append("#")		// API >= 12
			.append(device.getDeviceClass()).append("#")	// API >= 12
			.append(device.getDeviceSubclass()).append("#")	// API >= 12
			.append(device.getDeviceProtocol());			// API >= 12
		if (!TextUtils.isEmpty(serial)) {
			sb.append("#");	sb.append(serial);
		}
		if (useNewAPI && BuildCheck.isAndroid5()) {
			if (!TextUtils.isEmpty(manufactureName)) {
				sb.append("#").append(manufactureName);
			}
			if (configCount >= 0) {
				sb.append("#").append(configCount);
			}
			if (!TextUtils.isEmpty(deviceVersion)) {
				sb.append("#").append(deviceVersion);
			}
		}
		return sb.toString();
	}

//--------------------------------------------------------------------------------
	/**
	 * デバイスキーを整数として取得
	 * getDeviceKeyNameで得られる文字列のhasCodeを取得
	 * ベンダーID, プロダクトID, デバイスクラス, デバイスサブクラス, デバイスプロトコルから生成
	 * 同種の製品だと同じデバイスキーになるので注意
	 * @param device nullなら0を返す
	 * @return
	 */
	@Deprecated
	public static final int getDeviceKey(@Nullable final UsbDevice device) {
		return device != null ? getDeviceKeyName(device, null, false).hashCode() : 0;
	}

	/**
	 * デバイスキーを整数として取得
	 * getDeviceKeyNameで得られる文字列のhasCodeを取得
	 * useNewAPI=falseで同種の製品だと同じデバイスキーになるので注意
	 * @param device
	 * @param useNewAPI
	 * @return
	 */
	@Deprecated
	public static final int getDeviceKey(@Nullable final UsbDevice device,
		final boolean useNewAPI) {

		return device != null ? getDeviceKeyName(device, null, useNewAPI).hashCode() : 0;
	}

	/**
	 * デバイスキーを整数として取得
	 * getDeviceKeyNameで得られる文字列のhasCodeを取得
	 * useNewAPI=falseで同種の製品だと同じデバイスキーになるので注意
	 * @param device
	 * @param useNewAPI
	 * @param useNonce
	 * @return
	 */
	@Deprecated
	public static final int getDeviceKey(@Nullable final UsbDevice device,
		final boolean useNewAPI, final boolean useNonce) {

		return device != null ? getDeviceKeyName(device, null, useNewAPI, useNonce).hashCode() : 0;
	}

	/**
	 * デバイスキーを整数として取得
	 * getDeviceKeyNameで得られる文字列のhasCodeを取得
	 * serialがnullでuseNewAPI=falseで同種の製品だと同じデバイスキーになるので注意
	 * @param device nullなら0を返す
	 * @param serial UsbDeviceConnection#getSerialで取得したシリアル番号を渡す, nullでuseNewAPI=trueでAPI>=21なら内部で取得
	 * @param useNewAPI API>=21またはAPI>=23のみで使用可能なメソッドも使用する(ただし機器によってはnullが返ってくるので有効かどうかは機器による)
	 * @return
	 */
	@Deprecated
	public static final int getDeviceKey(@Nullable final UsbDevice device,
		final String serial, final boolean useNewAPI) {

		return device != null ? getDeviceKeyName(device, serial, useNewAPI).hashCode() : 0;
	}

	/**
	 * デバイスキーを整数として取得
	 * getDeviceKeyNameで得られる文字列のhasCodeを取得
	 * 同じ機器でもつなぎ直すと違うデバイスキーになるので注意
	 * @param device
	 * @param serial
	 * @param useNewAPI
	 * @param useNonce
	 * @return
	 */
	@Deprecated
	public static final int getDeviceKey(@Nullable final UsbDevice device,
		final String serial, final boolean useNewAPI,
		final boolean useNonce) {

		return device != null ? getDeviceKeyName(device, serial, useNewAPI, useNonce).hashCode() : 0;
	}

	/**
	 * USB機器毎の設定保存用にデバイスキー名を生成する。
	 * ベンダーID, プロダクトID, デバイスクラス, デバイスサブクラス, デバイスプロトコルから生成
	 * 同種の製品だと同じキー名になるので注意
	 * @param device nullなら空文字列を返す
	 * @return
	 */
	@Deprecated
	@NonNull
	public static final String getDeviceKeyName(@Nullable final UsbDevice device) {
		return getDeviceKeyName(device, null, false);
	}

	/**
	 * USB機器毎の設定保存用にデバイスキー名を生成する。
	 * useNewAPI=falseで同種の製品だと同じデバイスキーになるので注意
	 * @param device
	 * @param useNewAPI
	 * @return
	 */
	@Deprecated
	@NonNull
	public static final String getDeviceKeyName(@Nullable final UsbDevice device,
		final boolean useNewAPI) {

		return getDeviceKeyName(device, null, useNewAPI);
	}
	/**
	 * USB機器毎の設定保存用にデバイスキー名を生成する。この機器名をHashMapのキーにする
	 * UsbDeviceがopenしている時のみ有効
	 * ベンダーID, プロダクトID, デバイスクラス, デバイスサブクラス, デバイスプロトコルから生成
	 * serialがnullや空文字でなければserialを含めたデバイスキー名を生成する
	 * useNewAPI=trueでAPIレベルを満たしていればマニュファクチャ名, バージョン, コンフィギュレーションカウントも使う
	 * @param device nullなら空文字列を返す
	 * @param serial	UsbDeviceConnection#getSerialで取得したシリアル番号を渡す, nullでuseNewAPI=trueでAPI>=21なら内部で取得
	 * @param useNewAPI API>=21またはAPI>=23のみで使用可能なメソッドも使用する(ただし機器によってはnullが返ってくるので有効かどうかは機器による)
	 * @return
	 */
	@Deprecated
	@NonNull
	public static final String getDeviceKeyName(@Nullable final UsbDevice device,
		final String serial, final boolean useNewAPI) {

		return getDeviceKeyName(device, serial, useNewAPI, false);
	}

	/**
	 * USB機器毎の設定保存用にデバイスキー名を生成する。この機器名をHashMapのキーにする
	 * UsbDeviceがopenしている時のみ有効
	 * ベンダーID, プロダクトID, デバイスクラス, デバイスサブクラス, デバイスプロトコルから生成
	 * serialがnullや空文字でなければserialを含めたデバイスキー名を生成する
	 * useNewAPI=trueでAPIレベルを満たしていればマニュファクチャ名, バージョン, コンフィギュレーションカウントも使う
	 * usuNonce=trueならUsbDevice#getDeviceNameも含める。ただし機器をつなぎ直すと異なるデバイスキー名になるので注意
	 * @param device nullなら空文字列を返す
	 * @param serial	UsbDeviceConnection#getSerialで取得したシリアル番号を渡す, nullでuseNewAPI=trueでAPI>=21なら内部で取得
	 * @param useNewAPI API>=21またはAPI>=23のみで使用可能なメソッドも使用する(ただし機器によってはnullが返ってくるので有効かどうかは機器による)
	 * @param usuNonce シリアルキーが無い同種の機器を区別するための追加の文字列として
	 * 					UsbDevice#getDeviceNameを使うかどうか。
	 * 					ただし機器をつなぎ直すと異なるデバイスキー名になるので注意
	 * @return
	 */
	@Deprecated
	@SuppressLint("NewApi")
	@NonNull
	public static final String getDeviceKeyName(@Nullable final UsbDevice device,
		final String serial, final boolean useNewAPI, final boolean usuNonce) {

		if (device == null) return "";
		final StringBuilder sb = new StringBuilder();
		sb.append(device.getVendorId()).append("#")			// API >= 12
			.append(device.getProductId()).append("#")		// API >= 12
			.append(device.getDeviceClass()).append("#")	// API >= 12
			.append(device.getDeviceSubclass()).append("#")	// API >= 12
			.append(device.getDeviceProtocol());			// API >= 12
		if (!TextUtils.isEmpty(serial)) {
			sb.append("#").append(serial);
		}
		if (usuNonce) {
			sb.append("#").append(device.getDeviceName());	// API >= 12
		}
		if (useNewAPI && BuildCheck.isAndroid5()) {
			sb.append("#");
			if (TextUtils.isEmpty(serial)) {
				sb.append(device.getSerialNumber()).append("#");		// API >= 21
			}
			sb.append(device.getManufacturerName()).append("#")			// API >= 21
				.append(device.getConfigurationCount()).append("#");	// API >= 21
			if (BuildCheck.isMarshmallow()) {
				sb.append(device.getVersion()).append("#");				// API >= 23　XXX ここで末尾に付く#が余分だった...
			}
		}
		return sb.toString();
		// FIXME 同じハッシュにならないので一時的に戻す
//		final boolean b = useNewAPI && BuildCheck.isAndroid5();
//		return getDeviceKeyName(device, useNewAPI,
//			!TextUtils.isEmpty(serial) ? serial : (b && TextUtils.isEmpty(serial) ? device.getSerialNumber() : null),
//			b ? device.getManufacturerName() : null,
//			b ? device.getConfigurationCount() : 0,
//			b && BuildCheck.isMarshmallow() ? device.getVersion() + "#" : null
//		);
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
			if (keys != null && keys.size() > 0) {
				final StringBuilder sb = new StringBuilder();
				for (final String key: keys) {
					final UsbDevice device = list.get(key);
					final int num_interface = device != null ? device.getInterfaceCount() : 0;
					sb.setLength(0);
					for (int i = 0; i < num_interface; i++) {
						sb.append(String.format(Locale.US, "interface%d:%s",
							i, device.getInterface(i).toString()));
					}
					Log.i(TAG, "key=" + key + ":" + device + ":" + sb.toString());
				}
			} else {
				Log.i(TAG, "no device");
			}
		} else {
			Log.i(TAG, "no device");
		}
	}

}
