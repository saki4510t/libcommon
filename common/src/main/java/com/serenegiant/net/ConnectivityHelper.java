package com.serenegiant.net;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2018 saki t_saki@serenegiant.com
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
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.serenegiant.utils.BuildCheck;
import com.serenegiant.utils.HandlerThreadHandler;

import java.lang.ref.WeakReference;

@SuppressLint("MissingPermission")
public class ConnectivityHelper {
	private static final boolean DEBUG = false; // FIXME 実働時はfalseにすること
	private static final String TAG = ConnectivityHelper.class.getSimpleName();
	
	private final Object mSync = new Object();
	private final WeakReference<Context> mWeakContext;
	private Handler mAsyncHandler;
	private ConnectivityManager.OnNetworkActiveListener mOnNetworkActiveListener;
	private ConnectivityManager.NetworkCallback mNetworkCallback;
	private BroadcastReceiver mNetworkChangedReceiver;

	/** システムグローバルブロードキャスト用のインテントフィルター文字列 */
	private static final String ACTION_GLOBAL_CONNECTIVITY_CHANGE = "android.net.conn.CONNECTIVITY_CHANGE";

	public ConnectivityHelper(@NonNull final Context context) {
		if (DEBUG) Log.v(TAG, "Constructor:");
		mWeakContext = new WeakReference<>(context);
		mAsyncHandler = HandlerThreadHandler.createHandler(TAG);
		init();
	}
	
	@Override
	protected void finalize() throws Throwable {
		try {
			release();
		} finally {
			super.finalize();
		}
	}
	
	@SuppressLint("NewApi")
	public void release() {
		if (DEBUG) Log.v(TAG, "release:");
		final Context context = getContext();
		if (context != null) {
			if (BuildCheck.isLollipop()) {
				final ConnectivityManager manager = requireConnectivityManager();
				if (mOnNetworkActiveListener != null) {
					try {
						manager
							.removeDefaultNetworkActiveListener(mOnNetworkActiveListener);
					} catch (final Exception e) {
						Log.w(TAG, e);
					}
					mOnNetworkActiveListener = null;
				}
				if (mNetworkCallback != null) {
					manager.unregisterNetworkCallback(mNetworkCallback);
					mNetworkCallback = null;
				}
			}
			if (mNetworkChangedReceiver != null) {
				try {
					context.unregisterReceiver(mNetworkChangedReceiver);
				} catch (final Exception e) {
					Log.w(TAG, e);
				}
				mNetworkChangedReceiver = null;
			}
		}
		synchronized (mSync) {
			if (mAsyncHandler != null) {
				try {
					mAsyncHandler.getLooper().quit();
				} catch (final Exception e) {
					Log.w(TAG, e);
				}
				mAsyncHandler = null;
			}
		}
	}
	
	public boolean isValid() {
		try {
			requireConnectivityManager();
			return true;
		} catch (final IllegalStateException e) {
			if (DEBUG) Log.w(TAG, e);
		}
		return false;
	}
	
	@Nullable
	private Context getContext() {
		final Context context = mWeakContext.get();
		if (context == null) {
			throw new IllegalStateException("context is already released");
		}
		return context;
	}

	@NonNull
	private Context requireContext() throws IllegalStateException {
		final Context context = mWeakContext.get();
		if (context == null) {
			throw new IllegalStateException("context is already released");
		}
		return context;
	}
	
	@NonNull
	private ConnectivityManager requireConnectivityManager()
		throws IllegalStateException {
		
		final Context context = requireContext();
		final ConnectivityManager connManager
			= (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (connManager == null) {
			throw new IllegalStateException("failed to get ConnectivityManager");
		}
		return connManager;
	}

//================================================================================
	@SuppressLint("NewApi")
	private void init() {
		if (DEBUG) Log.v(TAG, "init:");
		final ConnectivityManager manager = requireConnectivityManager();
		if (BuildCheck.isLollipop()) {
			mOnNetworkActiveListener = new MyOnNetworkActiveListener();
			manager.addDefaultNetworkActiveListener(mOnNetworkActiveListener);	// API>=21
			mNetworkCallback = new MyNetworkCallback();
			// ACCESS_NETWORK_STATEパーミッションが必要
			if (BuildCheck.isNougat()) {
				manager.registerDefaultNetworkCallback(mNetworkCallback);	// API>=24
			} else if (BuildCheck.isOreo()) {
				manager.registerDefaultNetworkCallback(mNetworkCallback, mAsyncHandler); // API>=26
			} else {
				manager.registerNetworkCallback(new NetworkRequest.Builder()
					.build(),
					mNetworkCallback);	// API>=21
			}
		} else {
			mNetworkChangedReceiver = new NetworkChangedReceiver(
				new OnNetworkChangedListener() {
					@Override
					public void onNetworkChanged(final int isConnectedOrConnecting,
						final int isConnected, final int activeNetworkMask) {
		
						ConnectivityHelper.this.onNetworkChanged(
							isConnectedOrConnecting, isConnected, activeNetworkMask);
					}
				}
			);
			final IntentFilter intentFilter = new IntentFilter();
			intentFilter.addAction(ACTION_GLOBAL_CONNECTIVITY_CHANGE);
				requireContext().registerReceiver(mNetworkChangedReceiver, intentFilter);
		}
	}
	
	private void onNetworkChanged(final int isConnectedOrConnecting,
		final int isConnected, final int activeNetworkMask) {
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	private class MyOnNetworkActiveListener
		implements ConnectivityManager.OnNetworkActiveListener {

		private final String TAG = MyOnNetworkActiveListener.class.getSimpleName();

		public MyOnNetworkActiveListener() {
			if (DEBUG) Log.v(TAG, "Constructor:");
		}

		@Override
		public void onNetworkActive() {
			if (DEBUG) Log.v(TAG, "onNetworkActive:");
		}
	}
	
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	private class MyNetworkCallback extends ConnectivityManager.NetworkCallback {
		private final String TAG = MyNetworkCallback.class.getSimpleName();
	
		public MyNetworkCallback() {
			super();
			if (DEBUG) Log.v(TAG, "Constructor:");
		}
		
		@Override
		public void onAvailable(final Network network) {
			super.onAvailable(network);
			// ネットワークの準備ができた時
			if (DEBUG) Log.v(TAG, String.format("onAvailable:Network(%s)", network));
		}
		
		@Override
		public void onCapabilitiesChanged(final Network network,
			final NetworkCapabilities networkCapabilities) {

			super.onCapabilitiesChanged(network, networkCapabilities);
			// 接続が完了してネットワークの状態が変わった時
			if (DEBUG) Log.v(TAG,
			String.format("onCapabilitiesChanged:Network(%s),", network)
				+ networkCapabilities);
		}
		
		@Override
		public void onLinkPropertiesChanged(final Network network,
			final LinkProperties linkProperties) {

			super.onLinkPropertiesChanged(network, linkProperties);
			// ネットワークのリンク状態が変わった時
			if (DEBUG) Log.v(TAG,
				String.format("onLinkPropertiesChanged:Network(%s),", network)
				+ linkProperties);
		}

		@Override
		public void onLosing(final Network network, final int maxMsToLive) {
			super.onLosing(network, maxMsToLive);
			// 接続を失いそうな時
			if (DEBUG) Log.v(TAG, String.format("onLosing:Network(%s),", network));
		}
		
		@Override
		public void onLost(final Network network) {
			super.onLost(network);
			// 接続を失った時
			if (DEBUG) Log.v(TAG, String.format("onLost:Network(%s),", network));
		}
		
		@Override
		public void onUnavailable() {
			super.onUnavailable();
			// ネットワークが見つからなかった時
			// なんだろ？来ない？
			if (DEBUG) Log.v(TAG, "onUnavailable:");
		}
	}
	
	private interface OnNetworkChangedListener {
		/**
		 * @param isConnectedOrConnecting 接続中かread/write可能
		 * @param isConnected read/write可能
		 * @param activeNetworkMask アクティブなネットワークの選択マスク 接続しているネットワークがなければ0
		 */
		public void onNetworkChanged(
			final int isConnectedOrConnecting,
			final int isConnected,
			final int activeNetworkMask);
	}
	
	@SuppressWarnings("deprecation")
	private static class NetworkChangedReceiver extends BroadcastReceiver {
		private final String TAG = NetworkChangedReceiver.class.getSimpleName();
		/**
		 * The Mobile data connection.  When active, all data traffic
		 * will use this network type's interface by default
		 * (it has a default route)
		 */
		public static final int NETWORK_TYPE_MOBILE
			= 1 << ConnectivityManager.TYPE_MOBILE;	// 1 << 0
		/**
		 * The WIFI data connection.  When active, all data traffic
		 * will use this network type's interface by default
		 * (it has a default route).
		 */
		public static final int NETWORK_TYPE_WIFI
			= 1 << ConnectivityManager.TYPE_WIFI;	// 1 << 1
	
		/**
		 * An MMS-specific Mobile data connection.  This network type may use the
		 * same network interface as TYPE_MOBILE or it may use a different
		 * one.  This is used by applications needing to talk to the carrier's
		 * Multimedia Messaging Service servers.
		 */
		public static final int NETWORK_TYPE_MOBILE_MMS
			= 1 << ConnectivityManager.TYPE_MOBILE_MMS;	// 1 << 2
	
		/**
		 * A SUPL-specific Mobile data connection.  This network type may use the
		 * same network interface as TYPE_MOBILE or it may use a different
		 * one.  This is used by applications needing to talk to the carrier's
		 * Secure User Plane Location servers for help locating the device.
		 */
		public static final int NETWORK_TYPE_MOBILE_SUPL
			= 1 << ConnectivityManager.TYPE_MOBILE_SUPL;	// 1 << 3
	
		/**
		 * A DUN-specific Mobile data connection.  This network type may use the
		 * same network interface as TYPE_MOBILE or it may use a different
		 * one.  This is sometimes by the system when setting up an upstream connection
		 * for tethering so that the carrier is aware of DUN traffic.
		 */
		public static final int NETWORK_TYPE_MOBILE_DUN
			= 1 << ConnectivityManager.TYPE_MOBILE_DUN;	// 1 << 4
	
		/**
		 * A High Priority Mobile data connection.  This network type uses the
		 * same network interface as TYPE_MOBILE but the routing setup
		 * is different.  Only requesting processes will have access to the
		 * Mobile DNS servers and only IP's explicitly requested via requestRouteToHost
		 * will route over this interface if no default route exists.
		 */
		public static final int NETWORK_TYPE_MOBILE_HIPRI
			= 1 << ConnectivityManager.TYPE_MOBILE_HIPRI;	// 1 << 5
	
		/**
		 * The WiMAX data connection.  When active, all data traffic
		 * will use this network type's interface by default
		 * (it has a default route).
		 */
		public static final int NETWORK_TYPE_WIMAX
			= 1 << ConnectivityManager.TYPE_WIMAX;	// 1 << 6
	
		/**
		 * The Bluetooth data connection.  When active, all data traffic
		 * will use this network type's interface by default
		 * (it has a default route).
		 * XXX 単にBluetooth機器を検出しただけじゃこの値は来ない, Bluetooth経由のネットワークに接続しないとダメみたい
		 */
		public static final int NETWORK_TYPE_BLUETOOTH
			= 1 << ConnectivityManager.TYPE_BLUETOOTH;	// 1 << 7
	
		/**
		 * The Ethernet data connection.  When active, all data traffic
		 * will use this network type's interface by default
		 * (it has a default route).
		 */
		public static final int NETWORK_TYPE_ETHERNET
			= 1 << ConnectivityManager.TYPE_ETHERNET;	// 1 << 9

		/** ネットワーク種とそのビットマスク対の配列 */
		private static final int[] NETWORKS;
		static {
			NETWORKS = new int[] {
				ConnectivityManager.TYPE_MOBILE, NETWORK_TYPE_MOBILE,
				ConnectivityManager.TYPE_WIFI, NETWORK_TYPE_WIFI,
				ConnectivityManager.TYPE_MOBILE_MMS, NETWORK_TYPE_MOBILE_MMS,
				ConnectivityManager.TYPE_MOBILE_SUPL, NETWORK_TYPE_MOBILE_SUPL,
				ConnectivityManager.TYPE_MOBILE_DUN, NETWORK_TYPE_MOBILE_DUN,
				ConnectivityManager.TYPE_MOBILE_HIPRI, NETWORK_TYPE_MOBILE_HIPRI,
				ConnectivityManager.TYPE_WIMAX, NETWORK_TYPE_WIMAX,
				ConnectivityManager.TYPE_BLUETOOTH, NETWORK_TYPE_BLUETOOTH,
				ConnectivityManager.TYPE_ETHERNET, NETWORK_TYPE_ETHERNET,
//				ConnectivityManager.TYPE_VPN, NETWORK_TYPE_VPN,
			};
		}

		@NonNull
		private final OnNetworkChangedListener mListener;
		public NetworkChangedReceiver(@NonNull final OnNetworkChangedListener listener) {
			mListener = listener;
		}
	
		@Override
		public void onReceive(final Context context, final Intent intent) {
			if (DEBUG) Log.v(TAG, "onReceive:" + intent);
			final String action = intent != null ? intent.getAction() : null;
			if (ACTION_GLOBAL_CONNECTIVITY_CHANGE.equals(action)) {
				onReceiveGlobal(context, intent);
			}
		}

		/**
		 * システムグローバルブロードキャスト受信時の処理
		 * @param context
		 * @param intent
		 */
		@SuppressLint("NewApi")
		private void onReceiveGlobal(final Context context, final Intent intent) {
			final ConnectivityManager connMgr
				= (ConnectivityManager) context
					.getSystemService(Context.CONNECTIVITY_SERVICE);
			final LocalBroadcastManager broadcastManager
				= LocalBroadcastManager.getInstance(context.getApplicationContext());
	
			int isConnectedOrConnecting = 0;
			int isConnected = 0;
	
			if (BuildCheck.isAndroid5()) {	// API>=21
				final Network[] networks = connMgr.getAllNetworks();
				if (networks != null) {
					for (final Network network: networks) {
						final NetworkInfo info = connMgr.getNetworkInfo(network);
						if (info != null) {
							isConnectedOrConnecting |=
								info.isConnectedOrConnecting() ? (1 << info.getType()) : 0;
							isConnected |= info.isConnected() ? (1 << info.getType()) : 0;
						}
					}
				}
			} else {
				final int n = NETWORKS.length;
				for (int i = 0; i < n; i += 2) {
					final NetworkInfo info = connMgr.getNetworkInfo(NETWORKS[i]);
					if (info != null) {
						isConnectedOrConnecting |= info.isConnectedOrConnecting() ? NETWORKS[i + 1] : 0;
						isConnected |= info.isConnected() ? NETWORKS[i + 1] : 0;
					}
				}
			}
			final NetworkInfo activeNetworkInfo = connMgr.getActiveNetworkInfo();
			final int activeNetworkMask = (activeNetworkInfo != null ? 1 << activeNetworkInfo.getType() : 0);
			if (DEBUG) Log.v(TAG, String.format(
				"onNetworkChanged:isConnectedOrConnecting=%08x,isConnected=%08x,activeNetworkMask=%08x",
				isConnectedOrConnecting, isConnected, activeNetworkMask));
			// コールバックリスナーを呼び出す
			callOnNetworkChanged(isConnectedOrConnecting, isConnected, activeNetworkMask);
		}

		private void callOnNetworkChanged(final int isConnectedOrConnecting,
			final int isConnected, final int activeNetworkInfo) {

			if (mListener != null) {
				try {
					mListener.onNetworkChanged(isConnectedOrConnecting, isConnected, activeNetworkInfo);
				} catch (final Exception e) {
					Log.w(TAG, e);
				}
			}
		}
	}

//================================================================================
// ここ以下はポーリングでネットワーク状態をチェックするためのスタティックメソッド
//================================================================================
	/**
	 * WiFiネットワークが使用可能かどうかを返す
	 * このメソッドはブロードキャストレシーバーの登録の有無と関係なく使用可
	 * @param context
	 * @return
	 */
	@SuppressLint("NewApi")
	public static boolean isWifiNetworkReachable(@NonNull final Context context) {
		if (DEBUG) Log.v(TAG, "isWifiNetworkReachable:");
		final ConnectivityManager manager
			= (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (BuildCheck.isLollipop()) {
			if (BuildCheck.isMarshmallow()) {
				final Network network = manager.getActiveNetwork();	// API>=23
				final NetworkCapabilities capabilities = manager.getNetworkCapabilities(network);	// API>=21
				final NetworkInfo info = manager.getNetworkInfo(network);	// API>=21
				if (DEBUG) Log.v(TAG, "isWifiNetworkReachable:capabilities=" + capabilities);
				if (DEBUG) Log.v(TAG, "isWifiNetworkReachable:info=" + info);
				return isWifiNetworkReachable(capabilities, info);
			} else {
				final Network[] allNetworks = manager.getAllNetworks();	// API>=21
				for (final Network network: allNetworks) {
					final NetworkCapabilities capabilities = manager.getNetworkCapabilities(network);	// API>=21
					final NetworkInfo info = manager.getNetworkInfo(network);	// API>=21
					if (DEBUG) Log.v(TAG, "isWifiNetworkReachable:capabilities=" + capabilities);
					if (DEBUG) Log.v(TAG, "isWifiNetworkReachable:info=" + info);
					if (isWifiNetworkReachable(capabilities, info)) {
						return true;
					}
				}
			}
		} else {
			final NetworkInfo activeNetworkInfo = manager.getActiveNetworkInfo();
			if ((activeNetworkInfo != null) && (activeNetworkInfo.isConnectedOrConnecting())) {
				final int type = activeNetworkInfo.getType();
				return (type == ConnectivityManager.TYPE_WIFI)
					|| (type == ConnectivityManager.TYPE_WIMAX)
					|| (type == ConnectivityManager.TYPE_BLUETOOTH)
					|| (type == ConnectivityManager.TYPE_ETHERNET);
			}
		}
		return false;
	}

	/**
	 * モバイルネットワークが使用可能かどうかを返す
	 * このメソッドはブロードキャストレシーバーの登録の有無と関係なく使用可
	 * @param context
	 * @return
	 */
	@SuppressLint("NewApi")
	public static boolean isMobileNetworkReachable(@NonNull final Context context) {
		if (DEBUG) Log.v(TAG, "isMobileNetworkReachable:");
		final ConnectivityManager manager
			= (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (BuildCheck.isLollipop()) {
			if (BuildCheck.isMarshmallow()) {
				final Network network = manager.getActiveNetwork();	// API>=23
				final NetworkCapabilities capabilities = manager.getNetworkCapabilities(network);	// API>=21
				final NetworkInfo info = manager.getNetworkInfo(network);	// API>=21
				if (DEBUG) Log.v(TAG, "isWifiNetworkReachable:capabilities=" + capabilities);
				if (DEBUG) Log.v(TAG, "isWifiNetworkReachable:info=" + info);
				return isMobileNetworkReachable(capabilities, info);
			} else {
				final Network[] allNetworks = manager.getAllNetworks();	// API>=21
				for (final Network network: allNetworks) {
					final NetworkCapabilities capabilities = manager.getNetworkCapabilities(network);	// API>=21
					final NetworkInfo info = manager.getNetworkInfo(network);	// API>=21
					if (DEBUG) Log.v(TAG, "isWifiNetworkReachable:capabilities=" + capabilities);
					if (DEBUG) Log.v(TAG, "isWifiNetworkReachable:info=" + info);
					if (isMobileNetworkReachable(capabilities, info)) {
						return true;
					}
				}
			}
		} else {
			final NetworkInfo activeNetworkInfo = manager.getActiveNetworkInfo();
			if ((activeNetworkInfo != null) && (activeNetworkInfo.isConnectedOrConnecting())) {
				final int type = activeNetworkInfo.getType();
				return (type == ConnectivityManager.TYPE_MOBILE);
			}
		}
		return false;
	}

	/**
	 * ネットワークが使用可能かどうかをチェック
	 * このメソッドはブロードキャストレシーバーの登録の有無と関係なく使用可
	 */
	@SuppressLint("NewApi")
	public static boolean isNetworkReachable(@NonNull final Context context) {
		if (DEBUG) Log.v(TAG, "isNetworkReachable:");
		final ConnectivityManager manager
			= (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);

		if (BuildCheck.isLollipop()) {
			if (BuildCheck.isMarshmallow()) {
				final Network network = manager.getActiveNetwork();	// API>=23
				final NetworkCapabilities capabilities = manager.getNetworkCapabilities(network);	// API>=21
				final NetworkInfo info = manager.getNetworkInfo(network);	// API>=21
				return isNetworkReachable(capabilities, info);
			} else {
				final Network[] allNetworks = manager.getAllNetworks();	// API>=21
				for (final Network network: allNetworks) {
					final NetworkCapabilities capabilities = manager.getNetworkCapabilities(network);	// API>=21
					final NetworkInfo info = manager.getNetworkInfo(network);	// API>=21
					if (isNetworkReachable(capabilities, info)) {
						return true;
					}
				}
			}
			return false;
		} else {
			final NetworkInfo activeNetworkInfo = manager.getActiveNetworkInfo();
			return (activeNetworkInfo != null) && (activeNetworkInfo.isConnectedOrConnecting());
		}
	}

	@SuppressLint("NewApi")
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	private static boolean isWifiNetworkReachable(
		@NonNull final NetworkCapabilities capabilities,
		@NonNull final NetworkInfo info) {

		final boolean isWiFi;
		if (BuildCheck.isOreo()) {
			isWiFi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)		// API>=21
				|| capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)	// API>=21
				|| capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI_AWARE);	// API>=26
		} else {
			isWiFi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)		// API>=21
				|| capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET);	// API>=21
		}
		return isWiFi && isNetworkReachable(capabilities, info);
	}
	
	@SuppressLint("NewApi")
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	private static boolean isMobileNetworkReachable(
		@NonNull final NetworkCapabilities capabilities,
		@NonNull final NetworkInfo info) {

		final boolean isMobile;
		if (BuildCheck.isOreoMR1()) {
			isMobile = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)// API>=21
				|| capabilities.hasTransport(NetworkCapabilities.	TRANSPORT_LOWPAN);	// API>=27
		} else {
			isMobile = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);// API>=21
		}
		return isMobile && isNetworkReachable(capabilities, info);
	}

	@SuppressLint("NewApi")
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	private static boolean isNetworkReachable(
		@NonNull final NetworkCapabilities capabilities,
		@NonNull final NetworkInfo info) {

		if (DEBUG) Log.v(TAG, "isNetworkReachable:capabilities=" + capabilities);
		if (DEBUG) Log.v(TAG, "isNetworkReachable:info=" + info);
		final NetworkInfo.DetailedState state = info.getDetailedState();
		final boolean isConnectedOrConnecting
			= (state == NetworkInfo.DetailedState.CONNECTED)
				|| (state == NetworkInfo.DetailedState.CONNECTING);
		final boolean hasCapability;
		if (BuildCheck.isPie()) {
			hasCapability = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)	// API>=21
				&& capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)			// API>=23
				&& (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_SUSPENDED)	// API>=28
					|| capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_FOREGROUND));	// API>=28
		} else if (BuildCheck.isMarshmallow()) {
			hasCapability = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)	// API>=21
				&& capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);		// API>=23
		} else {
			hasCapability = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);// API>=21
		}
		if (DEBUG) Log.v(TAG, "isNetworkReachable:isConnectedOrConnecting="
			+ isConnectedOrConnecting + ",hasCapability=" + hasCapability
			+ ",NOT_SUSPENDED=" + capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_SUSPENDED)
			+ ",FOREGROUND=" + capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_FOREGROUND));
		return isConnectedOrConnecting && hasCapability;
	}
}
