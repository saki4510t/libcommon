package com.serenegiant.bluetooth;
/*
 * Androusb
 * Copyright (c) 2014-2016 saki t_saki@serenegiant.com
 * Distributed under the terms of the GNU Lesser General Public License (LGPL v3.0) License.
 * License details are in the file license.txt, distributed as part of this software.
 */

import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by saki on 16/08/31.
 * ペアリング済みまたは検出したBluetooth機器情報保持のためのクラス
 */
public class BluetoothDeviceInfo implements Parcelable {
	public static final Creator<BluetoothDeviceInfo> CREATOR = new Creator<BluetoothDeviceInfo>() {
		@Override
		public BluetoothDeviceInfo createFromParcel(Parcel in) {
			return new BluetoothDeviceInfo(in);
		}

		@Override
		public BluetoothDeviceInfo[] newArray(int size) {
			return new BluetoothDeviceInfo[size];
		}
	};

	public final String name;
	public final String address;
	public final int type;
	public final int deviceClass;
	/**
	 * ペアリングの状態
	 * BluetoothDevice.BOND_NONE, BluetoothDevice.BOND_BONDING, BluetoothDevice.BOND_BONDEDのどれか
	 */
	public final int bondState;

	/*package*/BluetoothDeviceInfo(final BluetoothDevice device) {
		name = device.getName();
		address =  device.getAddress();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
			type = device.getType();
		} else {
			type = 0;
		}
		final BluetoothClass clazz = device.getBluetoothClass();
		deviceClass = clazz != null ? clazz.getDeviceClass() : 0;
		bondState = device.getBondState();
	}

	protected BluetoothDeviceInfo(Parcel in) {
		name = in.readString();
		address = in.readString();
		type = in.readInt();
		deviceClass = in.readInt();
		bondState = in.readInt();
	}

	/**
	 * ペアリングされているかどうか
	 * @return
	 */
	public boolean isPaired() {
		return bondState == BluetoothDevice.BOND_BONDED;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(final Parcel parcel, final int flags) {

		parcel.writeString(name);
		parcel.writeString(address);
		parcel.writeInt(type);
		parcel.writeInt(deviceClass);
		parcel.writeInt(bondState);
	}

	@Override
	public String toString() {
		return String.format("BluetoothDeviceInfo(%s/%s)", name, address);
	}
}
