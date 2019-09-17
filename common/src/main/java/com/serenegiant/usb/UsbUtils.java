package com.serenegiant.usb;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2019 saki t_saki@serenegiant.com
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
import android.text.TextUtils;

import com.serenegiant.utils.BuildCheck;

public class UsbUtils {
	/**
	 * USB機器毎の設定保存用にデバイスキー名を生成する。
	 * ベンダーID, プロダクトID, デバイスクラス, デバイスサブクラス, デバイスプロトコルから生成
	 * 同種の製品だと同じキー名になるので注意
	 * @param device nullなら空文字列を返す
	 * @return
	 */
	public static final String getDeviceKeyName(final UsbDevice device) {
		return getDeviceKeyName(device, null, false);
	}

	/**
	 * USB機器毎の設定保存用にデバイスキー名を生成する。
	 * useNewAPI=falseで同種の製品だと同じデバイスキーになるので注意
	 * @param device
	 * @param useNewAPI
	 * @return
	 */
	public static final String getDeviceKeyName(final UsbDevice device, final boolean useNewAPI) {
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
	public static final String getDeviceKeyName(final UsbDevice device, final String serial, final boolean useNewAPI) {
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
	@SuppressLint("NewApi")
	public static final String getDeviceKeyName(final UsbDevice device, final String serial, final boolean useNewAPI, final boolean usuNonce) {
		if (device == null) return "";
		final StringBuilder sb = new StringBuilder();
		sb.append(device.getVendorId()).append("#");		// API >= 12
		sb.append(device.getProductId()).append("#");		// API >= 12
		sb.append(device.getDeviceClass()).append("#");		// API >= 12
		sb.append(device.getDeviceSubclass()).append("#");	// API >= 12
		sb.append(device.getDeviceProtocol());				// API >= 12
		if (!TextUtils.isEmpty(serial)) {
			sb.append("#").append(serial);
		}
		if (usuNonce) {
			sb.append("#").append(device.getDeviceName());	// API >= 12
		}
		if (useNewAPI && BuildCheck.isAndroid5()) {
			sb.append("#");
			if (TextUtils.isEmpty(serial)) {
				sb.append(device.getSerialNumber()).append("#");	// API >= 21
			}
			sb.append(device.getManufacturerName()).append("#");	// API >= 21
			sb.append(device.getConfigurationCount()).append("#");	// API >= 21
			if (BuildCheck.isMarshmallow()) {
				sb.append(device.getVersion()).append("#");			// API >= 23　XXX ここで末尾に付く#が余分だった...
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

	/**
	 * デバイスキー文字列を生成
	 * @param device
	 * @param serial
	 * @param manufactureName
	 * @param configCount
	 * @param deviceVersion
	 * @return
	 */
	public static final String getDeviceKeyName(final UsbDevice device, final boolean useNewAPI,
		final String serial, final String manufactureName, final int configCount, final String deviceVersion) {
		if (device == null) return "";
		final StringBuilder sb = new StringBuilder();
		sb.append(device.getVendorId());			sb.append("#");	// API >= 12
		sb.append(device.getProductId());			sb.append("#");	// API >= 12
		sb.append(device.getDeviceClass());			sb.append("#");	// API >= 12
		sb.append(device.getDeviceSubclass());		sb.append("#");	// API >= 12
		sb.append(device.getDeviceProtocol());						// API >= 12
		if (!TextUtils.isEmpty(serial)) {
			sb.append("#");	sb.append(serial);
		}
		if (useNewAPI && BuildCheck.isAndroid5()) {
			if (!TextUtils.isEmpty(manufactureName)) {
				sb.append("#");	sb.append(manufactureName);
			}
			if (configCount >= 0) {
				sb.append("#");	sb.append(configCount);
			}
			if (!TextUtils.isEmpty(deviceVersion)) {
				sb.append("#");	sb.append(deviceVersion);
			}
		}
		return sb.toString();
	}

	/**
	 * デバイスキーを整数として取得
	 * getDeviceKeyNameで得られる文字列のhasCodeを取得
	 * ベンダーID, プロダクトID, デバイスクラス, デバイスサブクラス, デバイスプロトコルから生成
	 * 同種の製品だと同じデバイスキーになるので注意
	 * @param device nullなら0を返す
	 * @return
	 */
	public static final int getDeviceKey(final UsbDevice device) {
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
	public static final int getDeviceKey(final UsbDevice device, final boolean useNewAPI) {
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
	public static final int getDeviceKey(final UsbDevice device, final boolean useNewAPI, final boolean useNonce) {
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
	public static final int getDeviceKey(final UsbDevice device, final String serial,
		final boolean useNewAPI) {

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
	public static final int getDeviceKey(final UsbDevice device, final String serial,
		final boolean useNewAPI, final boolean useNonce) {

		return device != null ? getDeviceKeyName(device, serial, useNewAPI, useNonce).hashCode() : 0;
	}

}
