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
*/

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;

import com.serenegiant.system.BuildCheck;
import com.serenegiant.system.ContextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * 機器情報保持のためのヘルパークラス
 */
public class UsbDeviceInfo implements Const, Parcelable {
	private static final boolean DEBUG = false;
	private static final String TAG = UsbDeviceInfo.class.getSimpleName();

	/**
	 * ベンダー名・製品名・バージョン・シリアルを取得する
	 * 新しいUsbDeviceInfoを生成して返す
	 * #getDeviceInfo(final UsbManager, final UsbDevice, final UsbDeviceInfo)のラッパーメソッド
	 * XXX 指定したUsbDeviceへのアクセスパーミッションがない場合に読めない情報があるので注意
	 * @param context
	 * @param device
	 * @return
	 */
	@NonNull
	public static UsbDeviceInfo getDeviceInfo(
		@NonNull final Context context, @Nullable final UsbDevice device) {

		return getDeviceInfo(
			ContextUtils.requireSystemService(context, UsbManager.class),
			device, new UsbDeviceInfo());
	}

	/**
	 * USB機器情報(ベンダー名・製品名・バージョン・シリアル等)を取得する
	 * #getDeviceInfo(final UsbDeviceConnection, final UsbDevice, final UsbDeviceInfo)のラッパーメソッド
	 * XXX 指定したUsbDeviceへのアクセスパーミッションがない場合に読めない情報があるので注意
	 * @param manager
	 * @param device
	 * @param out outがnullでなければoutへセットする、outがnullならば新しく生成して返す
	 * @return
	 */
	@NonNull
	public static UsbDeviceInfo getDeviceInfo(
		@NonNull final UsbManager manager,
		@Nullable final UsbDevice device, @Nullable final UsbDeviceInfo out) {

		final UsbDeviceConnection connection
			= (device != null && manager.hasPermission(device))
				? manager.openDevice(device) : null;

		try {
			return getDeviceInfo(connection, device, out);
		} finally {
			if (connection != null) {
				connection.close();
			}
		}
	}

	/**
	 * USB機器情報(ベンダー名・製品名・バージョン・シリアル等)を取得する
	 * @param connector
	 * @param out
	 * @return
	 */
	@NonNull
	public static UsbDeviceInfo getDeviceInfo(
		@NonNull final UsbConnector connector, @Nullable final UsbDeviceInfo out) {

		return getDeviceInfo(connector.getConnection(), connector.getDevice(), out);
	}

	/**
	 * USB機器情報(ベンダー名・製品名・バージョン・シリアル等)を取得する
	 * @param connection
	 * @param device
	 * @param out	outがnullでなければoutへセットする、outがnullならば新しく生成して返す
	 * @return
	 */
	@SuppressLint("NewApi")
	@NonNull
	public static UsbDeviceInfo getDeviceInfo(
		@Nullable final UsbDeviceConnection connection,
		@Nullable final UsbDevice device, @Nullable final UsbDeviceInfo out) {

		@NonNull
		final UsbDeviceInfo result = out != null ? out : new UsbDeviceInfo();
		result.clear();

		result.device = device;
		if (device != null) {
			if (BuildCheck.isAPI29() && (connection != null)) {
				// API>=29でターゲットAPI>=29ならパーミッションがないとシリアル番号を読み込めない
				// connectionがnullでないならopenできているのでパーミッションがある
				result.manufacturer = device.getManufacturerName();
				result.product = device.getProductName();
				result.configCounts = device.getConfigurationCount();
			} else if (BuildCheck.isLollipop()) {	// API >= 21
				result.manufacturer = device.getManufacturerName();
				result.product = device.getProductName();
				result.serial = device.getSerialNumber();
				result.configCounts = device.getConfigurationCount();
			}
			if (BuildCheck.isMarshmallow()) {	// API >= 23
				result.version = device.getVersion();
			}
			if (connection != null) {
				final byte[] desc = connection.getRawDescriptors();
				if (desc != null) {
					if (TextUtils.isEmpty(result.bcdUsb)) {
						result.bcdUsb = String.format("%02x%02x", ((int)desc[3] & 0xff), ((int)desc[2] & 0xff));
					}
					if (TextUtils.isEmpty(result.version)) {
						result.version = String.format("%x.%02x", ((int)desc[13] & 0xff), ((int)desc[12] & 0xff));
					}
					if (BuildCheck.isAPI29()) {	// API >= 29
						// API>=29でターゲットAPI>=29ならパーミッションがないとシリアル番号を読み込めない
						try {
							result.serial = device.getSerialNumber();
						} catch (final Exception e) {
							if (DEBUG) Log.w(TAG, e);
						}
					}
					if (TextUtils.isEmpty(result.serial)) {
						result.serial = getSerialNumber(connection);
					}
					if (result.configCounts < 0) {
						// デバイスディスクリプタを読み込んでコンフィギュレーションディスクリプタの個数を取得する
						result.configCounts = getNumConfigurations(connection);
					}
					if (result.configCounts < 0) {
						// コンフィギュレーションディスクリプタが0個はUSB機器ではないのでとりあえず強制的に1にする
						result.configCounts = 1;
					}

					final byte[] languages = new byte[256];
					int languageCount = 0;
					// controlTransfer(int requestType, int request, int value, int index, byte[] buffer, int length, int timeout)
					int res = connection.controlTransfer(
						USB_REQ_STANDARD_DEVICE_GET, // USB_DIR_IN | USB_TYPE_STANDARD | USB_RECIP_DEVICE
						USB_REQ_GET_DESCRIPTOR,
						(USB_DT_STRING << 8)/* | 0*/, 0, languages, 256, 0);
					if (res > 0) {
						languageCount = (res - 2) / 2;
					}
					if (languageCount > 0) {
						if (TextUtils.isEmpty(result.manufacturer)) {
							result.manufacturer = UsbUtils.getString(connection, ((int)desc[14] & 0xff), languageCount, languages);
						}
						if (TextUtils.isEmpty(result.product)) {
							result.product = UsbUtils.getString(connection, ((int)desc[15] & 0xff), languageCount, languages);
						}
						if (TextUtils.isEmpty(result.serial)) {
							result.serial = UsbUtils.getString(connection, ((int)desc[16] & 0xff), languageCount, languages);
						}
					}
				}
			}
			if (TextUtils.isEmpty(result.manufacturer)) {
				result.manufacturer = UsbVendorId.vendorName(device.getVendorId());
			}
			if (TextUtils.isEmpty(result.manufacturer)) {
				result.manufacturer = String.format("%04x", device.getVendorId());
			}
			if (TextUtils.isEmpty(result.product)) {
				result.product = String.format("%04x", device.getProductId());
			}
		}

		return result;
	}

//--------------------------------------------------------------------------------
// デバイスディスクリプタ読み取り関係
	/**
	 * デバイスディスクリプタを読んでbcdUSBを取得する
	 * @param connection
	 * @return
	 */
	public static int getBcdUSB(@NonNull final UsbDeviceConnection connection) {
		return (getDeviceDescriptorByte(connection, 3) << 8)
			+ getDeviceDescriptorByte(connection, 2);
	}

	/**
	 * デバイスディスクリプタを読んでデバイスクラスを取得する
	 * @param connection
	 * @return
	 */
	public static int getDeviceClass(@NonNull final UsbDeviceConnection connection) {
		return getDeviceDescriptorByte(connection, 4);
	}

	/**
	 * デバイスディスクリプタを読んでデバイスサブクラスを取得する
	 * 読み取れなかった場合は0を返す
	 * @param connection
	 * @return
	 */
	public static int getDeviceSubClass(@NonNull final UsbDeviceConnection connection) {
		return getDeviceDescriptorByte(connection, 5);
	}

	/**
	 * デバイスディスクリプタを読んでデバイスプロトコルを取得する
	 * 読み取れなかった場合は0を返す
	 * @param connection
	 * @return
	 */
	public static int getDeviceProtocol(@NonNull final UsbDeviceConnection connection) {
		return getDeviceDescriptorByte(connection, 6);
	}

	/**
	 * デバイスディスクリプタを読んでベンダーIDを取得する
	 * 読み取れなかった場合は0を返す
	 * @param connection
	 * @return
	 */
	public static int getVendorId(@NonNull final UsbDeviceConnection connection) {
		return (getDeviceDescriptorByte(connection, 9) << 8)
			+ getDeviceDescriptorByte(connection, 8);
	}

	/**
	 * デバイスディスクリプタを読んでプロダクトIDを取得する
	 * 読み取れなかった場合は0を返す
	 * @param connection
	 * @return
	 */
	public static int getProductId(@NonNull final UsbDeviceConnection connection) {
		return (getDeviceDescriptorByte(connection, 11) << 8)
			+ getDeviceDescriptorByte(connection, 10);
	}

	/**
	 * デバイスディスクリプタを読んでプロダクトIDを取得する
	 * 読み取れなかった場合は0を返す
	 * @param connection
	 * @return
	 */
	public static int getBcdDevice(@NonNull final UsbDeviceConnection connection) {
		return (getDeviceDescriptorByte(connection, 13) << 8)
			+ getDeviceDescriptorByte(connection, 12);
	}

	/**
	 * USB機器からベンダー名文字列の取得を試みる
	 * @param connection
	 * @return
	 */
	@Nullable
	public static String getVendorName(@NonNull final UsbDeviceConnection connection) {
		return getDeviceString(connection, 14);
	}

	/**
	 * USB機器からプロダクト名文字列の取得を試みる
	 * @param connection
	 * @return
	 */
	@Nullable
	public static String getProductName(@NonNull final UsbDeviceConnection connection) {
		return getDeviceString(connection, 15);
	}

	/**
	 * USB機器からシリアル番号文字列の取得を試みる
	 * @param connection
	 * @return
	 */
	@Nullable
	public static String getSerialNumber(@NonNull final UsbDeviceConnection connection) {
		return getDeviceString(connection, 16);
	}

	/**
	 * デバイスディスクリプタを読んでコンフィギュレーションディスクリプタの個数を取得する
	 * 読み取れなかった場合は0を返す
	 * @param connection
	 * @return
	 */
	public static int getNumConfigurations(@NonNull final UsbDeviceConnection connection) {
		return getDeviceDescriptorByte(connection, 17);
	}


	/**
	 * デバイスディスクリプタ読み取り用のヘルパーメソッド
	 * 読み取れなかった場合は0を返す
	 * @param connection
	 * @param offset
	 * @return
	 */
	private static int getDeviceDescriptorByte(@NonNull final UsbDeviceConnection connection, final int offset) {
		final byte[] desc = connection.getRawDescriptors();
		if ((desc != null) && (desc.length >= 0x12)
			&& (desc[0] == 0x12) && (desc[1] == 0x01)) {
			// bLength = 0x12, bDescriptorType = 0x01 -> Device Descriptor
//			Log.i(TAG, String.format("%02x", ((int)desc[offset]) & 0xff));
			return ((int)desc[offset]) & 0xff;
		} else {
			return 0;
		}
	}

	@Nullable
	private static String getDeviceString(@NonNull final UsbDeviceConnection connection, final int offset) {
		String result = null;

		final byte[] languages = new byte[256];
		int languageCount = 0;
		// controlTransfer(int requestType, int request, int value, int index, byte[] buffer, int length, int timeout)
		int res = connection.controlTransfer(
			USB_REQ_STANDARD_DEVICE_GET, // USB_DIR_IN | USB_TYPE_STANDARD | USB_RECIP_DEVICE
			USB_REQ_GET_DESCRIPTOR,
			(USB_DT_STRING << 8)/* | 0*/, 0, languages, 256, 0);
		if (res > 0) {
			languageCount = (res - 2) / 2;
		}
		if (languageCount > 0) {
			result = UsbUtils.getString(connection, getDeviceDescriptorByte(connection, offset), languageCount, languages);
		}

		return result;
	}
//--------------------------------------------------------------------------------
	/** 設定してあるUsbDevice */
	@Nullable
	public UsbDevice device;
	/** 機器が対応しているUSB規格 */
	@Nullable
	public String bcdUsb;
	/** ベンダー名 */
	@Nullable
	public String manufacturer;
	/** プロダクト名 */
	@Nullable
	public String product;
	/** 機器のバージョン */
	@Nullable
	public String version;
	/** 機器のシリアル番号 */
	@Nullable
	public String serial;
	/** コンフィギュレーションの個数 */
	public int configCounts;

	/**
	 * コンストラクタ
	 */
	public UsbDeviceInfo() {
		configCounts = -1;
	}

	/**
	 * Parcelable用のコンストラクタ
	 * @param in
	 */
	public UsbDeviceInfo(@NonNull final Parcel in) {
		device = in.readParcelable(UsbDevice.class.getClassLoader());
		bcdUsb = in.readString();
		manufacturer = in.readString();
		product = in.readString();
		version = in.readString();
		serial = in.readString();
		configCounts = in.readInt();
	}

	/**
	 * 保持している情報をクリアする
	 */
	private void clear() {
		device = null;
		bcdUsb = manufacturer = product = version = serial = null;
		configCounts = -1;
	}

	@NonNull
	@Override
	public String toString() {
		return String.format("UsbDeviceInfo(bcdUsb=%s,"
		 	+ "manufacturer=%s,product=%s,version=%s,serial=%s,configCounts=%s)",
			bcdUsb != null ? bcdUsb : "",
			manufacturer != null ? manufacturer : "",
			product != null ? product : "",
			version != null ? version : "",
			serial != null ? serial : "",
			configCounts >= 0 ? Integer.toString(configCounts) : "");
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(final Parcel dest, final int flags) {
		dest.writeParcelable(device, flags);
		dest.writeString(bcdUsb);
		dest.writeString(manufacturer);
		dest.writeString(product);
		dest.writeString(version);
		dest.writeString(serial);
		dest.writeInt(configCounts);
	}

	public static final Creator<UsbDeviceInfo> CREATOR = new Creator<UsbDeviceInfo>() {
		@Override
		public UsbDeviceInfo createFromParcel(@NonNull final Parcel in) {
			return new UsbDeviceInfo(in);
		}

		@Override
		public UsbDeviceInfo[] newArray(final int size) {
			return new UsbDeviceInfo[size];
		}
	};
}
