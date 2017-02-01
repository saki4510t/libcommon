package com.serenegiant.net;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2017 saki t_saki@serenegiant.com
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

import android.util.Log;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class NetworkHelper {
	private static final String TAG = NetworkHelper.class.getSimpleName();

	public static String getLocalIPv4Address() {
		try {
			for (final Enumeration<NetworkInterface> networkInterfaceEnum = NetworkInterface.getNetworkInterfaces();
				networkInterfaceEnum.hasMoreElements(); ) {

				final NetworkInterface networkInterface =
					networkInterfaceEnum.nextElement();
				for (final Enumeration<InetAddress> ipAddressEnum = networkInterface.getInetAddresses();
					ipAddressEnum.hasMoreElements(); ) {

					final InetAddress addr = ipAddressEnum.nextElement();
					if (!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
						return addr.getHostAddress();
					}
				}
			}
		} catch (final SocketException e) {
			Log.e(TAG, "getLocalIPv4Address", e);
		}
		return null;
	}

	public static String getLocalIPv6Address() {
		try {
			for (final Enumeration<NetworkInterface> networkInterfaceEnum = NetworkInterface.getNetworkInterfaces();
				networkInterfaceEnum.hasMoreElements(); ) {

				final NetworkInterface networkInterface =
					networkInterfaceEnum.nextElement();
				for (final Enumeration<InetAddress> ipAddressEnum = networkInterface.getInetAddresses();
					 ipAddressEnum.hasMoreElements(); ) {

					final InetAddress addr = ipAddressEnum.nextElement();
					if (!addr.isLoopbackAddress() && addr instanceof Inet6Address) {
						return addr.getHostAddress();
					}
				}
			}
		} catch (final SocketException e) {
			Log.w(TAG, "getLocalIPv6Address", e);
		}
		return null;
	}
}
