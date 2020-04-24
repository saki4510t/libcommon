package com.serenegiant.usb;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.text.TextUtils;

import com.serenegiant.system.BuildCheck;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * 機器情報保持のためのヘルパークラス
 */
public class UsbDeviceInfo implements Const {

	/**
	 * ベンダー名・製品名・バージョン・シリアルを取得する
	 * #updateDeviceInfo(final UsbManager, final UsbDevice, final UsbDeviceInfo)のヘルパーメソッド
	 * @param context
	 * @param device
	 * @return
	 */
	public static UsbDeviceInfo getDeviceInfo(
		@NonNull final Context context, @Nullable final UsbDevice device) {

		return getDeviceInfo(
			(UsbManager)context.getSystemService(Context.USB_SERVICE),
			device, new UsbDeviceInfo());
	}

	/**
	 * ベンダー名・製品名・バージョン・シリアルを取得する
	 * @param manager
	 * @param device
	 * @param _info
	 * @return
	 */
	@SuppressLint("NewApi")
	public static UsbDeviceInfo getDeviceInfo(
		@NonNull final UsbManager manager,
		@Nullable final UsbDevice device, @Nullable final UsbDeviceInfo _info) {

		final UsbDeviceInfo info = _info != null ? _info : new UsbDeviceInfo();
		info.clear();

		if (device != null) {
			if (BuildCheck.isLollipop()) {	// API >= 21
				info.manufacturer = device.getManufacturerName();
				info.product = device.getProductName();
				info.serial = device.getSerialNumber();
				info.configCounts = device.getConfigurationCount();
			}
			if (BuildCheck.isMarshmallow()) {	// API >= 23
				info.version = device.getVersion();
			}
			if (manager.hasPermission(device)) {
				final UsbDeviceConnection connection = manager.openDevice(device);
				if (connection != null) {
					try {
						final byte[] desc = connection.getRawDescriptors();
						if (desc != null) {
							if (TextUtils.isEmpty(info.usb_version)) {
								info.usb_version = String.format("%x.%02x", ((int)desc[3] & 0xff), ((int)desc[2] & 0xff));
							}
							if (TextUtils.isEmpty(info.version)) {
								info.version = String.format("%x.%02x", ((int)desc[13] & 0xff), ((int)desc[12] & 0xff));
							}
							if (TextUtils.isEmpty(info.serial)) {
								info.serial = connection.getSerial();
							}
							if (info.configCounts < 0) {
								// FIXME 未実装 デバイスディスクリプタをパースせんとなりゃん
								info.configCounts = 1;
							}

							final byte[] languages = new byte[256];
							int languageCount = 0;
							// controlTransfer(int requestType, int request, int value, int index, byte[] buffer, int length, int timeout)
							int result = connection.controlTransfer(
								USB_REQ_STANDARD_DEVICE_GET, // USB_DIR_IN | USB_TYPE_STANDARD | USB_RECIP_DEVICE
								USB_REQ_GET_DESCRIPTOR,
								(USB_DT_STRING << 8) | 0, 0, languages, 256, 0);
							if (result > 0) {
								languageCount = (result - 2) / 2;
							}
							if (languageCount > 0) {
								if (TextUtils.isEmpty(info.manufacturer)) {
									info.manufacturer = UsbUtils.getString(connection, desc[14], languageCount, languages);
								}
								if (TextUtils.isEmpty(info.product)) {
									info.product = UsbUtils.getString(connection, desc[15], languageCount, languages);
								}
								if (TextUtils.isEmpty(info.serial)) {
									info.serial = UsbUtils.getString(connection, desc[16], languageCount, languages);
								}
							}
						}
					} finally {
						connection.close();
					}
				}
			}
			if (TextUtils.isEmpty(info.manufacturer)) {
				info.manufacturer = USBVendorId.vendorName(device.getVendorId());
			}
			if (TextUtils.isEmpty(info.manufacturer)) {
				info.manufacturer = String.format("%04x", device.getVendorId());
			}
			if (TextUtils.isEmpty(info.product)) {
				info.product = String.format("%04x", device.getProductId());
			}
		}
		return info;
	}

//--------------------------------------------------------------------------------
	/** 機器が対応しているUSB規格 */
	public String usb_version;
	/** ベンダー名 */
	public String manufacturer;
	/** プロダクト名 */
	public String product;
	/** 機器のバージョン */
	public String version;
	/** 機器のシリアル番号 */
	public String serial;
	/** コンフィギュレーションの個数 */
	public int configCounts;

	public UsbDeviceInfo() {
		configCounts = -1;
	}

	private void clear() {
		usb_version = manufacturer = product = version = serial = null;
		configCounts = -1;
	}

	@NonNull
	@Override
	public String toString() {
		return String.format("UsbDeviceInfo:usb_version=%s,"
		 	+ "manufacturer=%s,product=%s,version=%s,serial=%s,configCounts=%s",
			usb_version != null ? usb_version : "",
			manufacturer != null ? manufacturer : "",
			product != null ? product : "",
			version != null ? version : "",
			serial != null ? serial : "",
			configCounts >= 0 ? Integer.toString(configCounts) : "");
	}
}
