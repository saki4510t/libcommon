package com.serenegiant.net;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2022 saki t_saki@serenegiant.com
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

import com.serenegiant.utils.ThreadPool;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * ネットワーク関係のヘルパーメソッドを定義するヘルパークラス
 */
public class NetworkUtils {
	private static final String TAG = NetworkUtils.class.getSimpleName();

	private NetworkUtils() {
		// インスタンス化をエラーとするためにデフォルトコンストラクタをprivateとする
	}

	/**
	 * 自分自身のIPv4アドレスを取得する
	 * @return
	 */
	@Nullable
	public static String getLocalIPv4Address() {
		try {
			for (final NetworkInterface intf: Collections.list(NetworkInterface.getNetworkInterfaces())) {
				for (final InetAddress addr: Collections.list(intf.getInetAddresses())) {
					if (!addr.isLoopbackAddress() && (addr instanceof Inet4Address)) {
						final String a = addr.getHostAddress();
						if ((a != null) && !a.contains("dummy")) {
							return addr.getHostAddress();
						}
					}
				}
			}
			// フォールバックする, 最初に見つかったInet4Address以外のアドレスを返す,
			// Bluetooth PANやWi-Fi Directでの接続時など
			for (final NetworkInterface intf: Collections.list(NetworkInterface.getNetworkInterfaces())) {
				for (final InterfaceAddress addr: intf.getInterfaceAddresses()) {
					final InetAddress ad = addr.getAddress();
					if (!ad.isLoopbackAddress() && !(ad instanceof Inet4Address)) {
						final String a = ad.getHostAddress();
						if ((a != null) && !a.contains("dummy")) {
							return ad.getHostAddress();
						}
					}
				}
			}
		} catch (final SocketException | NullPointerException e) {
			Log.e(TAG, "getLocalIPv4Address", e);
		}
		return null;
	}

	/**
	 * ループバック以外の自分自身のIPv4アドレスリストを取得する
	 * @return
	 */
	@NonNull
	public static List<InetAddress> getLocalIPv4Addresses() {
		final List<InetAddress> result = new ArrayList<>();
		try {
			for (final NetworkInterface intf: Collections.list(NetworkInterface.getNetworkInterfaces())) {
				for (final InetAddress addr: Collections.list(intf.getInetAddresses())) {
					if (!addr.isLoopbackAddress() && (addr instanceof Inet4Address)) {
						final String a = addr.getHostAddress();
						if ((a != null) && !a.contains("dummy")) {
							result.add(addr);
						}
					}
				}
			}
			// フォールバックする, 最初に見つかったInet4Address以外のアドレスを返す,
			// Bluetooth PANやWi-Fi Directでの接続時など
			for (final NetworkInterface intf: Collections.list(NetworkInterface.getNetworkInterfaces())) {
				for (final InterfaceAddress addr: intf.getInterfaceAddresses()) {
					final InetAddress ad = addr.getAddress();
					if (!ad.isLoopbackAddress() && (ad instanceof Inet4Address) && !result.contains(ad)) {
						final String a = ad.getHostAddress();
						if ((a != null) && !a.contains("dummy")) {
							result.add(ad);
						}
					}
				}
			}
		} catch (final SocketException | NullPointerException e) {
			Log.e(TAG, "getLocalIPv4Address", e);
		}
		return result;
	}

	/**
	 * 自分自身のIpV6アドレスを取得する
	 * @return
	 */
	@Nullable
	public static String getLocalIPv6Address() {
		try {
			for (final NetworkInterface intf: Collections.list(NetworkInterface.getNetworkInterfaces())) {
				for (final InetAddress addr: Collections.list(intf.getInetAddresses())) {
					if (!addr.isLoopbackAddress() && (addr instanceof Inet6Address)) {
						return addr.getHostAddress();
					}
				}
			}
		} catch (final SocketException | NullPointerException e) {
			Log.w(TAG, "getLocalIPv6Address", e);
		}
		return null;
	}

	/**
	 * ループバック以外の自分自身のIpV6アドレス一覧を取得する
	 * @return
	 */
	@NonNull
	public static List<InetAddress> getLocalIPv6Addresses() {
		final List<InetAddress> result = new ArrayList<>();
		try {
			for (final NetworkInterface intf: Collections.list(NetworkInterface.getNetworkInterfaces())) {
				for (final InetAddress addr: Collections.list(intf.getInetAddresses())) {
					if (!addr.isLoopbackAddress() && (addr instanceof Inet6Address)) {
						result.add(addr);
					}
				}
			}
		} catch (final SocketException | NullPointerException e) {
			Log.w(TAG, "getLocalIPv6Addresses", e);
		}
		return result;
	}

	/**
	 * 利用可能なネットワークインターフェースに存在する全てのネットワークアドレスをlogCatへ出力する
	 */
	public static void dumpAll() {
		try {
			for (final NetworkInterface intf: Collections.list(NetworkInterface.getNetworkInterfaces())) {
				for (final InetAddress addr: Collections.list(intf.getInetAddresses())) {
					dump(addr);
				}
			}
		} catch (final SocketException | NullPointerException e) {
			Log.w(TAG, "dumpAll", e);
		}
	}

	/**
	 * 指定したネットワークアドレスの値をlogCatへ出力する
	 * @param addr
	 */
	public static void dump(@Nullable final InetAddress addr) {
		final CountDownLatch latch = new CountDownLatch(1);
		if (addr == null) {
			Log.i(TAG, "dumpInetAddress: null");
		} else {
			Log.i(TAG, "addr=" + addr);	// InetAddress#toString
			Log.i(TAG, "  address=" + Arrays.toString(addr.getAddress()));
			Log.i(TAG, "  hostAddress=" + addr.getHostAddress());
			try {
				final String hostName = addr.getHostName();
				Log.i(TAG, "  hostName=" + hostName);
			} catch (final Exception e) {
				Log.i(TAG, "  hostName=");
			}
			Log.i(TAG, "  isLoopback=" + addr.isLoopbackAddress());
			Log.i(TAG, "  isAny=" + addr.isAnyLocalAddress());
			Log.i(TAG, "  isAnyLocal=" + addr.isAnyLocalAddress());
			Log.i(TAG, "  isLinkLocal=" + addr.isLinkLocalAddress());
			Log.i(TAG, "  isSiteLocal=" + addr.isSiteLocalAddress());
			Log.i(TAG, "  isMCLinkLocal=" + addr.isMCLinkLocal());
			Log.i(TAG, "  isMCSiteLocal=" + addr.isMCSiteLocal());
			Log.i(TAG, "  isMCNodeLocal=" + addr.isMCNodeLocal());
			Log.i(TAG, "  isMCOrgLocal=" + addr.isMCOrgLocal());
			Log.i(TAG, "  isMCGGlobal=" + addr.isMCGlobal());
			Log.i(TAG, "  isMulticast=" + addr.isMulticastAddress());
			if (addr instanceof Inet4Address) {
				Log.i(TAG, "  isIpv4=true");
			} else if (addr instanceof Inet6Address) {
				final Inet6Address _addr = (Inet6Address)addr;
				Log.i(TAG, "  isIpv6=true");
				Log.i(TAG, "  isIPv4CompatibleAddress=" + _addr.isIPv4CompatibleAddress());
			}
			ThreadPool.queueEvent(() -> {
				try {
					Log.i(TAG, "  isReachable=" + addr.isReachable(1000));
				} catch (final IOException e) {
					Log.w(TAG, e);
				}
				latch.countDown();
			});
			try {
				if (!latch.await(1000, TimeUnit.MILLISECONDS)) {
					Log.i(TAG, "  isReachable=timeout");
				}
			} catch (final InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 指定したネットワークアドレスの値を文字列化する
	 * @param addr
	 * @return
	 */
	public static String toString(@Nullable final InetAddress addr) {
		final CountDownLatch latch = new CountDownLatch(1);
		final StringBuilder sb = new StringBuilder();
		if (addr == null) {
			sb.append("null");
		} else {
			sb.append("addr=").append(addr).append(",");
			sb.append("address=").append(Arrays.toString(addr.getAddress())).append(",");
			sb.append("hostAddress=").append(addr.getHostAddress()).append(",");
			String hostName;
			try {
				hostName = addr.getHostName();
			} catch (final Exception e) {
				hostName = null;
			}
			sb.append("hostName=").append(hostName).append(",");
			sb.append("isLoopback=").append(addr.isLoopbackAddress()).append(",");
			sb.append("isAny=").append(addr.isAnyLocalAddress()).append(",");
			sb.append("isAnyLocal=").append(addr.isAnyLocalAddress()).append(",");
			sb.append("isLinkLocal=").append(addr.isLinkLocalAddress()).append(",");
			sb.append("isSiteLocal=").append(addr.isSiteLocalAddress()).append(",");
			sb.append("isMCLinkLocal=").append(addr.isMCLinkLocal()).append(",");
			sb.append("isMCSiteLocal=").append(addr.isMCSiteLocal()).append(",");
			sb.append("isMCNodeLocal=").append(addr.isMCNodeLocal()).append(",");
			sb.append("isMCOrgLocal=").append(addr.isMCOrgLocal()).append(",");
			sb.append("isMCGGlobal=").append(addr.isMCGlobal()).append(",");
			sb.append("isMulticast=").append(addr.isMulticastAddress()).append(",");
			if (addr instanceof Inet4Address) {
				sb.append("isIpv4=true").append(",");
			} else if (addr instanceof Inet6Address) {
				final Inet6Address _addr = (Inet6Address)addr;
				sb.append("isIpv6=true").append(",");
				sb.append("isIPv4CompatibleAddress=").append(_addr.isIPv4CompatibleAddress()).append(",");
			}
			ThreadPool.queueEvent(() -> {
				try {
					sb.append("isReachable=").append(addr.isReachable(1000)).append(",");
				} catch (final IOException e) {
					sb.append(e).append(",");
				}
				latch.countDown();
			});
			try {
				if (!latch.await(1000, TimeUnit.MILLISECONDS)) {
					sb.append("isReachable=").append("timeout").append(",");
				}
			} catch (final InterruptedException e) {
				e.printStackTrace();
			}
		}
		return sb.toString();
	}
}
