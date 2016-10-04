package com.serenegiant.net;
/*
 * Androusb
 * Copyright (c) 2014-2016 saki t_saki@serenegiant.com
 * Distributed under the terms of the GNU Lesser General Public License (LGPL v3.0) License.
 * License details are in the file license.txt, distributed as part of this software.
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
