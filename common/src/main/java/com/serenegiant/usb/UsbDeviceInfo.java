package com.serenegiant.usb;

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
	 * @param manager
	 * @param device
	 * @param out outがnullでなければoutへセットする、outがnullならば新しく生成して返す
	 * @return
	 */
	@SuppressLint("NewApi")
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
					if (TextUtils.isEmpty(result.usb_version)) {
						result.usb_version = String.format("%x.%02x", ((int)desc[3] & 0xff), ((int)desc[2] & 0xff));
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
						result.serial = connection.getSerial();
					}
					if (result.configCounts < 0) {
						// FIXME 未実装 デバイスディスクリプタをパースせんとなりゃん
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
							result.manufacturer = UsbUtils.getString(connection, desc[14], languageCount, languages);
						}
						if (TextUtils.isEmpty(result.product)) {
							result.product = UsbUtils.getString(connection, desc[15], languageCount, languages);
						}
						if (TextUtils.isEmpty(result.serial)) {
							result.serial = UsbUtils.getString(connection, desc[16], languageCount, languages);
						}
					}
				}
			}
			if (TextUtils.isEmpty(result.manufacturer)) {
				result.manufacturer = USBVendorId.vendorName(device.getVendorId());
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
	/** 設定してあるUsbDevice */
	@Nullable
	public UsbDevice device;
	/** 機器が対応しているUSB規格 */
	@Nullable
	public String usb_version;
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
		usb_version = in.readString();
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

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(final Parcel dest, final int flags) {
		dest.writeParcelable(device, flags);
		dest.writeString(usb_version);
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
