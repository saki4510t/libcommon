package com.serenegiant.usb;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Fixed the issue when reading filter definition from xml file
 * that undefined null filter(that match all device) is generateed.
 * Copyright (c) 2014-2021 saki t_saki@serenegiant.com
 *
 * This class originally came from
 * com.android.server.usb.UsbSettingsManager.DeviceFilter
 * in UsbSettingsManager.java in Android SDK
 *
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *  moved from aAndUsb
 */

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbInterface;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;

import com.serenegiant.system.BuildCheck;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.XmlRes;

import static com.serenegiant.utils.XmlHelper.getAttribute;

public final class DeviceFilter implements Parcelable {

	private static final String TAG = "DeviceFilter";

	/**
	 * 指定したxmlリソースからDeviceFilterリストを生成する
	 * @param context
	 * @param deviceFilterXmlId
	 * @return
	 */
	public static List<DeviceFilter> getDeviceFilters(
		@NonNull final Context context,
		@XmlRes final int deviceFilterXmlId) {

		final XmlPullParser parser = context.getResources().getXml(deviceFilterXmlId);
		final List<DeviceFilter> deviceFilters = new ArrayList<DeviceFilter>();
		try {
			int eventType = parser.getEventType();
			while (eventType != XmlPullParser.END_DOCUMENT) {
	            if (eventType == XmlPullParser.START_TAG) {
					final DeviceFilter deviceFilter = readEntryOne(context, parser);
					if (deviceFilter != null) {
						deviceFilters.add(deviceFilter);
					}
	            }
				eventType = parser.next();
			}
		} catch (final XmlPullParserException e) {
			Log.d(TAG, "XmlPullParserException", e);
		} catch (final IOException e) {
			Log.d(TAG, "IOException", e);
		}

		return Collections.unmodifiableList(deviceFilters);
	}

	@Nullable
	public static DeviceFilter readEntryOne(
		@NonNull final Context context,
		@NonNull final XmlPullParser parser)
			throws XmlPullParserException, IOException {

		int vendorId = -1;
		int productId = -1;
		int deviceClass = -1;
		int deviceSubclass = -1;
		int deviceProtocol = -1;
		boolean exclude = false;
		String manufacturerName = null;
		String productName = null;
		String serialNumber = null;
		int[] intfClass = null, intfSubClass = null, intfProtocol = null;
		boolean hasValue = false;

		String tag;
        int eventType = parser.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
        	tag = parser.getName();
        	if ("usb-device".equalsIgnoreCase(tag)) {
        		if (eventType == XmlPullParser.START_TAG) {
        			hasValue = true;
					vendorId = getAttribute(context, parser, null, "vendor-id", -1);
        			if (vendorId == -1) {
        				vendorId = getAttribute(context, parser, null, "vendorId", -1);
        			}
					if (vendorId == -1) {
	         			vendorId = getAttribute(context, parser, null, "venderId", -1);
					}
    				productId = getAttribute(context, parser, null, "product-id", -1);
        			if (productId == -1) {
            			productId = getAttribute(context, parser, null, "productId", -1);
					}
        			deviceClass = getAttribute(context, parser, null, "class", -1);
        			deviceSubclass = getAttribute(context, parser, null, "subclass", -1);
        			deviceProtocol = getAttribute(context, parser, null, "protocol", -1);
        			manufacturerName = getAttribute(context, parser, null, "manufacturer-name", "");
        			if (TextUtils.isEmpty(manufacturerName)) {
        				// オリジナルの属性名が設定されていないときは末尾の"-name"がない場合をチェック
        				manufacturerName = getAttribute(context, parser, null, "manufacture", "");
					}
        			productName = getAttribute(context, parser, null, "product-name", "");
        			if (TextUtils.isEmpty(productName)) {
						// オリジナルの属性名が設定されていないときは末尾の"-name"がない場合をチェック
        				productName = getAttribute(context, parser, null, "product", "");
					}
        			serialNumber = getAttribute(context, parser, null, "serial-number", "");
        			if (TextUtils.isEmpty(serialNumber)) {
						// オリジナルの属性名が設定されていないときは末尾の"-number"がない場合をチェック
            			serialNumber = getAttribute(context, parser, null, "serial", "");
					}
					// ここから下はオリジナルには無い追加の設定
					exclude = getAttribute(context, parser, null, "exclude", false);
					// API>=28でclass, sub class, protocolが0になってしまうのでinterface class, interface sub class, interface protocolでチェックできるように定義を追加
					if (!TextUtils.isEmpty(parser.getAttributeValue(null, "interfaceClass"))) {
						intfClass = getAttribute(context, parser, null, "interfaceClass", new int[0]);
						if ((intfClass != null) && (intfClass.length == 0)) {
							intfClass = null;
						}
					}
					if (!TextUtils.isEmpty(parser.getAttributeValue(null, "interfaceSubClass"))) {
						intfSubClass = getAttribute(context, parser, null, "interfaceSubClass", new int[0]);
						if ((intfSubClass != null) && (intfSubClass.length == 0)) {
							intfSubClass = null;
						}
					}
					if (!TextUtils.isEmpty(parser.getAttributeValue(null, "interfaceProtocol"))) {
						intfProtocol = getAttribute(context, parser, null, "interfaceProtocol", new int[0]);
						if ((intfProtocol != null) && (intfProtocol.length == 0)) {
							intfProtocol = null;
						}
					}
        		} else if (eventType == XmlPullParser.END_TAG) {
        			if (hasValue) {

	        			return new DeviceFilter(vendorId, productId,
	        				deviceClass, deviceSubclass, deviceProtocol,
	        				manufacturerName, productName, serialNumber,
	        				intfClass, intfSubClass, intfProtocol,
	        				exclude);
        			}
        		}
        	}
        	eventType = parser.next();
        }
        return null;
	}

//--------------------------------------------------------------------------------
	// USB Vendor ID (or -1 for unspecified)
	public final int mVendorId;
	// USB Product ID (or -1 for unspecified)
	public final int mProductId;
	// USB device or interface class (or -1 for unspecified)
	public final int mClass;
	// USB device or interface subclass (or -1 for unspecified)
	public final int mSubclass;
	// USB device protocol (or -1 for unspecified)
	public final int mProtocol;
	// USB device manufacturer name string (or null for unspecified)
	@Nullable
	public final String mManufacturerName;
	// USB device product name string (or null for unspecified)
	@Nullable
	public final String mProductName;
	// USB device serial number string (or null for unspecified)
	@Nullable
	public final String mSerialNumber;
	// USB interface class (or -1 for unspecified)
	@NonNull
	public final int[] mIntfClass;
	// USB interface subclass (or -1 for unspecified)
	@NonNull
	public final int[] mIntfSubClass;
	@NonNull
	public final int[] mIntfProtocol;
	// set true if specific device(s) should exclude
	public final boolean isExclude;

	public DeviceFilter(final int vid, final int pid,
		final int clasz, final int subclass, final int protocol,
		final String manufacturer, final String product, final String serialNum) {

		this(vid, pid, clasz, subclass, protocol,
			manufacturer, product, serialNum,
			null, null, null,
			false);
	}

	public DeviceFilter(final int vid, final int pid,
		final int clazz, final int subclass, final int protocol,
		final String manufacturer, final String product, final String serialNum,
		@Nullable final int[] intfClass,
		@Nullable final int[] intfSubClass,
		@Nullable final int[] intfProtocol,
		final boolean isExclude) {

		mVendorId = vid;
		mProductId = pid;
		mClass = clazz;
		mSubclass = subclass;
		mProtocol = protocol;
		mIntfClass = intfClass != null ? intfClass : new int[0];
		mIntfSubClass = intfSubClass != null ? intfSubClass : new int[0];
		mIntfProtocol = intfProtocol != null ? intfProtocol : new int[0];
		mManufacturerName = TextUtils.isEmpty(manufacturer) ? null : manufacturer;
		mProductName = TextUtils.isEmpty(product) ? null : product;
		mSerialNumber = TextUtils.isEmpty(serialNum) ? null : serialNum;
		this.isExclude = isExclude;
	}

	public DeviceFilter(@NonNull final UsbDevice device) {
		this(device, false);
	}

	@SuppressLint("NewApi")
	public DeviceFilter(@NonNull final UsbDevice device, final boolean isExclude) {
		mVendorId = device.getVendorId();
		mProductId = device.getProductId();
		mClass = device.getDeviceClass();
		mSubclass = device.getDeviceSubclass();
		mProtocol = device.getDeviceProtocol();
		// getInterfaceCountは内部配列のlengthを返すので負にはならないはずだけど年のために下限を0にする
		final int count = Math.max(device.getInterfaceCount(), 0);
		mIntfClass = new int[count];
		mIntfSubClass = new int[count];
		mIntfProtocol = new int[count];
		for (int i = 0; i < count; i++) {
			final UsbInterface intf = device.getInterface(i);
			mIntfClass[i] = intf.getInterfaceClass();
			mIntfSubClass[i] = intf.getInterfaceSubclass();
			mIntfProtocol[i] = intf.getInterfaceProtocol();
		}
		if (BuildCheck.isLollipop()) {
			mManufacturerName = device.getManufacturerName();
			mProductName = device.getProductName();
			mSerialNumber = device.getSerialNumber();
		} else {
			mManufacturerName = null;
			mProductName = null;
			mSerialNumber = null;
		}
		this.isExclude = isExclude;
	}

	protected DeviceFilter(@NonNull final Parcel in) {
		mVendorId = in.readInt();
		mProductId = in.readInt();
		mClass = in.readInt();
		mSubclass = in.readInt();
		mProtocol = in.readInt();
		mManufacturerName = in.readString();
		mProductName = in.readString();
		mSerialNumber = in.readString();
		mIntfClass = in.createIntArray();
		mIntfSubClass = in.createIntArray();
		mIntfProtocol = in.createIntArray();
		isExclude = in.readByte() != 0;
	}

/*	public void write(XmlSerializer serializer) throws IOException {
		serializer.startTag(null, "usb-device");
		if (mVendorId != -1) {
			serializer
					.attribute(null, "vendor-id", Integer.toString(mVendorId));
		}
		if (mProductId != -1) {
			serializer.attribute(null, "product-id",
					Integer.toString(mProductId));
		}
		if (mClass != -1) {
			serializer.attribute(null, "class", Integer.toString(mClass));
		}
		if (mSubclass != -1) {
			serializer.attribute(null, "subclass", Integer.toString(mSubclass));
		}
		if (mProtocol != -1) {
			serializer.attribute(null, "protocol", Integer.toString(mProtocol));
		}
		if (mManufacturerName != null) {
			serializer.attribute(null, "manufacturer-name", mManufacturerName);
		}
		if (mProductName != null) {
			serializer.attribute(null, "product-name", mProductName);
		}
		if (mSerialNumber != null) {
			serializer.attribute(null, "serial-number", mSerialNumber);
		}
		serializer.attribute(null, "serial-number", Boolean.toString(isExclude));
		serializer.endTag(null, "usb-device");
	} */

	/**
	 * 指定したUsbDeviceがこのDeviceFilterにマッチするかどうかを返す
	 * isExcludeフラグは別途#isExcludeか自前でチェックすること
	 * @param device
	 * @return
	 */
	public boolean matches(@NonNull final UsbDevice device) {
		if (mVendorId != -1 && device.getVendorId() != mVendorId) {
			return false;
		}
		if (mProductId != -1 && device.getProductId() != mProductId) {
			return false;
		}

		// check device class/subclass/protocol
		if (matches(
			device.getDeviceClass(),
			device.getDeviceSubclass(),
			device.getDeviceProtocol())) {

			return true;
		}

		// check device interface class/interface subclass/interface protocol
		return interfaceMatches(device);
	}

	/**
	 * 指定したUsbDeviceInfoがこのDeviceFilterにマッチするかどうかを返す
	 * isExcludeフラグは別途#isExcludeか自前でチェックすること
	 * @param info
	 * @return
	 */
	public boolean matches(
		@Nullable final UsbDeviceInfo info) {

		if ((info == null) || (info.device == null)) {
			return false;
		}

		if ((mManufacturerName != null) && (info.manufacturer != null)
				&& !mManufacturerName.equals(info.manufacturer)) {
			// マニファクチャ名が指定されていてデバイスにもマニファクチャ名が指定されているが一致しない
			return false;
		}
		if ((mProductName != null) && (info.product != null)
				&& !mProductName.equals(info.product)) {
			// プロダクト名が指定されていてデバイスにもプロダクト名が指定されているが一致しない
			return false;
		}
		if ((mSerialNumber != null) && (info.serial != null)
				&& !mSerialNumber.equals(info.serial)) {
			// シリアル名が指定されていてデバイスにもシリアル名が指定されているが一致しない
			return false;
		}

		return matches(info.device);
	}

	/**
	 * このDeviceFilterに一致してかつmExcludeがtrueならtrueを返す
	 * @param device
	 * @return
	 */
	public boolean isExclude(@NonNull final UsbDevice device) {
		return isExclude && matches(device);
	}

	@Override
	public boolean equals(final Object obj) {
		// can't compare if we have wildcard strings
		if (mVendorId == -1 || mProductId == -1 || mClass == -1
				|| mSubclass == -1 || mProtocol == -1) {
			return false;
		}
		if (obj instanceof DeviceFilter) {
			final DeviceFilter filter = (DeviceFilter) obj;

			if (filter.mVendorId != mVendorId
					|| filter.mProductId != mProductId
					|| filter.mClass != mClass || filter.mSubclass != mSubclass
					|| filter.mProtocol != mProtocol) {
				return false;
			}
			if ((filter.mManufacturerName != null && mManufacturerName == null)
					|| (filter.mManufacturerName == null && mManufacturerName != null)
					|| (filter.mProductName != null && mProductName == null)
					|| (filter.mProductName == null && mProductName != null)
					|| (filter.mSerialNumber != null && mSerialNumber == null)
					|| (filter.mSerialNumber == null && mSerialNumber != null)) {
				return false;
			}
			if ((filter.mManufacturerName != null && mManufacturerName != null && !mManufacturerName
					.equals(filter.mManufacturerName))
					|| (filter.mProductName != null && mProductName != null && !mProductName
							.equals(filter.mProductName))
					|| (filter.mSerialNumber != null && mSerialNumber != null && !mSerialNumber
							.equals(filter.mSerialNumber))) {
				return false;
			}
			return (filter.isExclude != isExclude);
		}
		if (obj instanceof UsbDevice) {
			final UsbDevice device = (UsbDevice) obj;
			if (isExclude
					|| (device.getVendorId() != mVendorId)
					|| (device.getProductId() != mProductId)
					|| (device.getDeviceClass() != mClass)
					|| (device.getDeviceSubclass() != mSubclass)
					|| (device.getDeviceProtocol() != mProtocol) ) {
				return false;
			}
/*			if ((mManufacturerName != null && device.getManufacturerName() == null)
					|| (mManufacturerName == null && device
							.getManufacturerName() != null)
					|| (mProductName != null && device.getProductName() == null)
					|| (mProductName == null && device.getProductName() != null)
					|| (mSerialNumber != null && device.getSerialNumber() == null)
					|| (mSerialNumber == null && device.getSerialNumber() != null)) {
				return (false);
			} */
/*			if ((device.getManufacturerName() != null && !mManufacturerName
					.equals(device.getManufacturerName()))
					|| (device.getProductName() != null && !mProductName
							.equals(device.getProductName()))
					|| (device.getSerialNumber() != null && !mSerialNumber
							.equals(device.getSerialNumber()))) {
				return (false);
			} */
			return true;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return (((mVendorId << 16) | mProductId) ^ ((mClass << 16)
				| (mSubclass << 8) | mProtocol));
	}

	@NonNull
	@Override
	public String toString() {
		return "DeviceFilter{" +
			"mVendorId=" + mVendorId +
			", mProductId=" + mProductId +
			", mClass=" + mClass +
			", mSubclass=" + mSubclass +
			", mProtocol=" + mProtocol +
			", mManufacturerName='" + mManufacturerName + '\'' +
			", mProductName='" + mProductName + '\'' +
			", mSerialNumber='" + mSerialNumber + '\'' +
			", mIntfClass=" + Arrays.toString(mIntfClass) +
			", mIntfSubClass=" + Arrays.toString(mIntfSubClass) +
			", mIntfProtocol=" + Arrays.toString(mIntfProtocol) +
			", isExclude=" + isExclude +
			'}';
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(final Parcel dest, final int flags) {
		dest.writeInt(mVendorId);
		dest.writeInt(mProductId);
		dest.writeInt(mClass);
		dest.writeInt(mSubclass);
		dest.writeInt(mProtocol);
		dest.writeString(mManufacturerName);
		dest.writeString(mProductName);
		dest.writeString(mSerialNumber);
		dest.writeIntArray(mIntfClass);
		dest.writeIntArray(mIntfSubClass);
		dest.writeIntArray(mIntfProtocol);
		dest.writeByte((byte) (isExclude ? 1 : 0));
	}

//--------------------------------------------------------------------------------
	/**
	 * 指定したクラス・サブクラス・プロトコルがこのDeviceFilterとマッチするかどうかを返す
	 * mExcludeフラグは別途#isExcludeか自前でチェックすること
	 * @param clazz
	 * @param subclass
	 * @param protocol
	 * @return
	 */
	private boolean matches(final int clazz, final int subclass, final int protocol) {
		return ((mClass == -1 || clazz == mClass)
			&& (mSubclass == -1 || subclass == mSubclass)
			&& (mProtocol == -1 || protocol == mProtocol));
	}

	/**
	 * インターフェースクラスに一致するものがあるかどうか
	 * フィルター用インターフェースクラス配列の長さが0なら常にtrue
	 * @param clazz
	 * @return true: 一致したものがある
	 */
	private boolean matchesIntfClass(final int clazz) {
		if (mIntfClass.length > 0) {
			for (int intfClass: mIntfClass) {
				if (intfClass == clazz) {
					return true;
				}
			}
			return false;
		} else {
			return true;
		}
	}

	/**
	 * インターフェースサブクラスに一致するものがあるかどうか
	 * フィルター用インターフェースサブクラス配列の長さが0なら常にtrue
	 * @param subClazz
	 * @return true: 一致したものがある
	 */
	private boolean matchesIntfSubClass(final int subClazz) {
		if (mIntfSubClass.length > 0) {
			for (int value: mIntfSubClass) {
				if (value == subClazz) {
					return true;
				}
			}
			return false;
		} else {
			return true;
		}
	}

	/**
	 * インターフェースプロトコルに一致するものがあるかどうか
	 * フィルター用インターフェースプロトコル配列の長さが0なら常にtrue
	 * @param protocol
	 * @return true: 一致したものがある
	 */
	private boolean matchesIntfProtocol(final int protocol) {
		if (mIntfProtocol.length > 0) {
			for (int value: mIntfProtocol) {
				if (value == protocol) {
					return true;
				}
			}
			return false;
		} else {
			return true;
		}
	}

	/**
	 * インターフェース専用のフィルタが定義されてあって一致する場合true
	 * @param clazz
	 * @param subclass
	 * @param protocol
	 * @return
	 */
	private boolean interfaceMatches(final int clazz, final int subclass, final int protocol) {
		return (mIntfClass.length > 0)
			&& (mIntfSubClass.length > 0)
			&& (mIntfProtocol.length > 0)
			&& matchesIntfClass(clazz)
			&& matchesIntfSubClass(subclass)
			&& matchesIntfProtocol(protocol);
	}

	private boolean interfaceMatches(@NonNull final UsbDevice device) {
		// if device doesn't match, check the interfaces
		final int count = device.getInterfaceCount();
		for (int i = 0; i < count; i++) {
			final UsbInterface intf = device.getInterface(i);
			if (matches(
				intf.getInterfaceClass(),
				intf.getInterfaceSubclass(),
				intf.getInterfaceProtocol())) {

				return true;
			}
			if (interfaceMatches(
				intf.getInterfaceClass(),
				intf.getInterfaceSubclass(),
				intf.getInterfaceProtocol())) {

				return true;
			}
		}
		return false;
	}

//--------------------------------------------------------------------------------
	public static final Creator<DeviceFilter> CREATOR
		= new Creator<DeviceFilter>() {

		@Override
		public DeviceFilter createFromParcel(Parcel in) {
			return new DeviceFilter(in);
		}

		@Override
		public DeviceFilter[] newArray(int size) {
			return new DeviceFilter[size];
		}
	};

}
