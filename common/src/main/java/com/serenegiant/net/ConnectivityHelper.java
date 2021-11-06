package com.serenegiant.net;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2021 saki t_saki@serenegiant.com
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

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;

import android.util.Log;

import com.serenegiant.system.BuildCheck;
import com.serenegiant.system.ContextUtils;
import com.serenegiant.utils.HandlerThreadHandler;
import com.serenegiant.utils.HandlerUtils;

import java.lang.ref.WeakReference;
import java.util.Locale;

/**
 * ネットワークの接続状態変化を取得するためのヘルパークラス
 * NetworkChangedReceiverの代替(ただし直接の互換性は無い)
 * ACCESS_NETWORK_STATEパーミッションが必要
 */
public class ConnectivityHelper {
	private static final boolean DEBUG = false; // FIXME 実働時はfalseにすること
	private static final String TAG = ConnectivityHelper.class.getSimpleName();
	
	public static final int NETWORK_TYPE_NON = 0;
	public static final int NETWORK_TYPE_MOBILE = 1;
	public static final int NETWORK_TYPE_WIFI = 1 << 1;
	public static final int NETWORK_TYPE_BLUETOOTH = 1 << 7;
	public static final int NETWORK_TYPE_ETHERNET = 1 << 9;

	/**
	 * ネットワークの接続状態変化を通知するためのコールバックリスナー
	 */
	public interface ConnectivityCallback {
		/**
		 * @param activeNetworkType
		 */
		@AnyThread
		public void onNetworkChanged(final int activeNetworkType, final int prevNetworkType);
		@AnyThread
		public void onError(final Throwable t);
	}

	/**
	 * 排他制御用オブジェクト
	 */
	@NonNull
	private final Object mSync = new Object();
	/**
	 * Contextの弱酸賞を保持
	 */
	@NonNull
	private final WeakReference<Context> mWeakContext;
	/**
	 * ネットワークの接続状態変化を通知するためのコールバックリスナーインスタンス
	 */
	@NonNull
	private final ConnectivityCallback mCallback;
	/**
	 * UI/メインスレッド上で処理を行うためのHandlerインスタンス
	 */
	@NonNull
	private final Handler mUIHandler;
	/**
	 * ワーカースレッド上で処理を行うためのHandlerインスタンス
	 */
	@NonNull
	private final Handler mAsyncHandler;
	/**
	 * API>=21でシステムのデフォルトのネットワーク接続がアクティブになったときの通知を受け取るためのOnNetworkActiveListenerインスタンス
	 */
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	@Nullable
	private ConnectivityManager.OnNetworkActiveListener mOnNetworkActiveListener;	// API>=21
	/**
	 * API>=21でネットワークの状態変更を受け取るためのNetworkCallbackインスタンス
	 */
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	@Nullable
	private ConnectivityManager.NetworkCallback mNetworkCallback;	// API>=21
	/**
	 * API21未満でブロードキャストレシーバーでネットワークの状態変化を受け取る時のBroadcastReceiverインスタンス, API<21
	 */
	@Nullable
	private BroadcastReceiver mNetworkChangedReceiver;
	/**
	 * 現在のアクティブになっているネットワーク接続の種類
	 */
	private int mActiveNetworkType = NETWORK_TYPE_NON;
	/**
	 * このConnectivityHelperインスタンスが破棄されたかどうか
	 */
	private volatile boolean mIsReleased = false;

	/**
	 * システムグローバルブロードキャスト用のインテントフィルター文字列
	 * API21未満用
	 */
	private static final String ACTION_GLOBAL_CONNECTIVITY_CHANGE
		= "android.net.conn.CONNECTIVITY_CHANGE";

	/**
	 * コンストラクタ
	 * @param context
	 * @param callback
	 */
	@RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
	public ConnectivityHelper(@NonNull final Context context,
		@NonNull final ConnectivityCallback callback) {

		if (DEBUG) Log.v(TAG, "Constructor:");
		mWeakContext = new WeakReference<>(context);
		mCallback = callback;
		mUIHandler = new Handler(context.getMainLooper());
		mAsyncHandler = HandlerThreadHandler.createHandler(TAG);
		mUIHandler.post(new Runnable() {
			@RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
			@Override
			public void run() {
				initAsync();
			}
		});
	}
	
	@Override
	protected void finalize() throws Throwable {
		try {
			release();
		} finally {
			super.finalize();
		}
	}

	/**
	 * 関係するリソース等を破棄する、再利用はできない
	 */
	@SuppressLint("NewApi")
	public void release() {
		if (DEBUG) Log.v(TAG, "release:");
		mIsReleased = true;
		updateActiveNetwork(NETWORK_TYPE_NON);
		final Context context = mWeakContext.get();
		if (context != null) {
			mWeakContext.clear();
			if (BuildCheck.isAPI21()) {
				final ConnectivityManager manager
					= ContextUtils.requireSystemService(context, ConnectivityManager.class);
				if (mOnNetworkActiveListener != null) {
					try {
						manager
							.removeDefaultNetworkActiveListener(mOnNetworkActiveListener);	// API>=21
					} catch (final Exception e) {
						Log.w(TAG, e);
					}
					mOnNetworkActiveListener = null;
				}
				if (mNetworkCallback != null) {
					try {
						manager.unregisterNetworkCallback(mNetworkCallback);	// API>=21
					} catch (final Exception e) {
						Log.w(TAG, e);
					}
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
			mUIHandler.removeCallbacksAndMessages(null);
			try {
				mAsyncHandler.removeCallbacksAndMessages(null);
				mAsyncHandler.getLooper().quit();
			} catch (final Exception e) {
				Log.w(TAG, e);
			}
		}
	}

	/**
	 * このConnectivityManagerが破棄されていなくて利用可能かどうかを取得
	 * @return
	 */
	public boolean isValid() {
		try {
			requireConnectivityManager();
			return !mIsReleased;
		} catch (final IllegalStateException e) {
			if (DEBUG) Log.w(TAG, e);
		}
		return false;
	}

	/**
	 * ネットワーク接続の種類を取得
	 * @return
	 * @throws IllegalStateException
	 */
	public int getActiveNetworkType() throws IllegalStateException {
		synchronized (mSync) {
			if (mIsReleased) {
				throw new IllegalStateException("already released!");
			}
			return mActiveNetworkType;
		}
	}

	/**
	 * ネットワーク接続しているかどうかを取得
	 * @return
	 * @throws IllegalStateException
	 */
	public boolean isNetworkReachable() throws IllegalStateException {
		return getActiveNetworkType() != NETWORK_TYPE_NON;
	}

	/**
	 * Wi-Fi経由でネットワーク接続しているかどうかを取得
	 * @return
	 * @throws IllegalStateException
	 */
	public boolean isWifiNetworkReachable() throws IllegalStateException {
		final int active = getActiveNetworkType();
		return (active == NETWORK_TYPE_WIFI)
			|| (active == NETWORK_TYPE_ETHERNET);
	}

	/**
	 * モバイルデータ回線でネットワーク接続しているかどうかを取得
	 * @return
	 * @throws IllegalStateException
	 */
	public boolean isMobileNetworkReachable() throws IllegalStateException {
		return getActiveNetworkType() == NETWORK_TYPE_MOBILE;
	}

	/**
	 * Bluetooth経由でネットワーク接続しているかどうかを取得
	 * @return
	 */
	public boolean isBluetoothNetworkReachable() throws IllegalStateException  {
		return getActiveNetworkType() == NETWORK_TYPE_BLUETOOTH;
	}

	/**
	 * ネットワークの接続状態を確認してコールバックを呼び出す
	 * @throws IllegalStateException
	 */
	public void refresh() throws IllegalStateException {
		synchronized (mSync) {
			if (mIsReleased) {
				throw new IllegalStateException("already released!");
			}
			mActiveNetworkType = NETWORK_TYPE_NON;
			mUIHandler.post(new Runnable() {
				@RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
				@Override
				public void run() {
					refreshAsync();
				}
			});
		}
	}

//--------------------------------------------------------------------------------
	/**
	 * Contextを取得
	 * 取得できなければIllegalStateExceptionを投げる
	 * @return
	 * @throws IllegalStateException
	 */
	@NonNull
	private Context requireContext() throws IllegalStateException {
		final Context context = mWeakContext.get();
		if (context == null) {
			throw new IllegalStateException("context is already released");
		}
		return context;
	}

	/**
	 * ConnectivityManagerを取得
	 * 取得できなければIllegalStateExceptionを投げる
	 * @return
	 * @throws IllegalStateException
	 */
	@NonNull
	private ConnectivityManager requireConnectivityManager()
		throws IllegalStateException {
		
		return ContextUtils.requireSystemService(requireContext(), ConnectivityManager.class);
	}

//--------------------------------------------------------------------------------
	/**
	 * ネットワーク接続状態取得用のコールバック等のセットアップ処理
	 */
	@SuppressLint("NewApi")
	@RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
	private void initAsync() {
		if (DEBUG) Log.v(TAG, "initAsync:");
		final ConnectivityManager manager = requireConnectivityManager();
		if (BuildCheck.isAPI21()) {
			// API21以上の場合
			mOnNetworkActiveListener = new MyOnNetworkActiveListener();
			manager.addDefaultNetworkActiveListener(mOnNetworkActiveListener);	// API>=21
			mNetworkCallback = new MyNetworkCallback();
			// ネットワーク接続状態取得用コールバックを登録
			if (BuildCheck.isAPI26()) {
				manager.registerDefaultNetworkCallback(mNetworkCallback, mAsyncHandler); // API>=26
			} else if (BuildCheck.isAPI24()) {
				manager.registerDefaultNetworkCallback(mNetworkCallback);	// API>=24
			} else {
				// API>=21, API<23
				// ネットワーク接続状態取得用コールバックを登録
				manager.registerNetworkCallback(
					new NetworkRequest.Builder().build(),
					mNetworkCallback);	// API>=21
			}
		} else {
			// API21未満の場合
			mNetworkChangedReceiver = new NetworkChangedReceiver(this);
			final IntentFilter intentFilter = new IntentFilter();
			intentFilter.addAction(ACTION_GLOBAL_CONNECTIVITY_CHANGE);
			requireContext().registerReceiver(mNetworkChangedReceiver, intentFilter);
		}
		// 初期状態をコールバックで通知
		refreshAsync();
	}

	/**
	 * refreshの非同期実行部分
	 * 現在のアクティブなネットワーク接続を取得してコールバックする
	 */
	@SuppressLint("NewApi")
	@RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
	private void refreshAsync() {
		if (DEBUG) Log.v(TAG, "refreshAsync:");
		final ConnectivityManager manager = requireConnectivityManager();

		if (BuildCheck.isAPI23()) {
			// API>=23
			final Network network = manager.getActiveNetwork();
			updateActiveNetwork(network, null, null);	// API>=23
		} else if (BuildCheck.isAPI21()) {
			// API>=21, API<23
			@NonNull
			final Network[] allNetworks = manager.getAllNetworks();			// API>=21
			for (final Network network: allNetworks) {
				@Nullable
				final NetworkCapabilities capabilities = manager.getNetworkCapabilities(network);	// API>=21
				final LinkProperties linkProperties = manager.getLinkProperties(network);	// API>=21
				if ((capabilities != null) && (linkProperties != null)) {
					manager.getLinkProperties(network);
					updateActiveNetwork(network, capabilities, linkProperties);
					return;	// 最初に見つかったものを使う?
				}
			}
			updateActiveNetwork(NETWORK_TYPE_NON);
		} else {
			// API21未満
			updateActiveNetwork(manager.getActiveNetworkInfo());
		}
	}
//--------------------------------------------------------------------------------
	/**
	 * ConnectivityCallbackのコールバックメソッド呼び出しのためのヘルパーメソッド
	 * @param activeNetworkType
	 * @param prevNetworkType
	 */
	private void callOnNetworkChanged(
		final int activeNetworkType, final int prevNetworkType) {

		synchronized (mSync) {
			if (!HandlerUtils.isTerminated(mAsyncHandler)) {
				mAsyncHandler.post(() -> {
					try {
						mCallback.onNetworkChanged(activeNetworkType, prevNetworkType);
					} catch (final Exception e) {
						callOnError(e);
					}
				});
			} else if (DEBUG) Log.w(TAG, "callOnNetworkChanged:mAsyncHandler already terminated,");
		}
	}

	/**
	 * ConnectivityCallbackのコールバックメソッド呼び出しのためのヘルパーメソッド
	 * @param t
	 */
	private void callOnError(final Throwable t) {
		synchronized (mSync) {
			if (!HandlerUtils.isTerminated(mAsyncHandler)) {
				mAsyncHandler.post(() -> {
					try {
						mCallback.onError(t);
					} catch (final Exception e) {
						Log.w(TAG, e);
					}
				});
			} else if (DEBUG) Log.w(TAG, "callOnNetworkChanged:mAsyncHandler already terminated,");
		}
	}

//--------------------------------------------------------------------------------
	/**
	 * ネットワークの接続状態を更新して必要であればコールバックする
	 * API>=21での処理用
	 * @param network
	 * @param caps
	 */
	@RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	private void updateActiveNetwork(
		@Nullable final Network network,
		@Nullable final NetworkCapabilities caps,
		@Nullable final LinkProperties props) {

		if (DEBUG) Log.v(TAG, "updateActiveNetwork:" + network);

		if (network != null) {
			final ConnectivityManager manager = requireConnectivityManager();
			@Nullable
			final NetworkCapabilities capabilities = caps != null
				? caps : manager.getNetworkCapabilities(network);	// API>=21
			@Nullable
			final LinkProperties properties = props != null
				? props : manager.getLinkProperties(network);	// API>=21
			int activeNetworkType = NETWORK_TYPE_NON;
			if ((capabilities != null) && (properties != null)) {
				if (isWifiNetworkReachable(manager, network, capabilities, properties)) {
					activeNetworkType = NETWORK_TYPE_WIFI;
				} else if (isMobileNetworkReachable(manager, network, capabilities, properties)) {
					activeNetworkType = NETWORK_TYPE_MOBILE;
				} else if (isBluetoothNetworkReachable(manager, network, capabilities, properties)) {
					activeNetworkType = NETWORK_TYPE_BLUETOOTH;
				} else if (isNetworkReachable(manager, network, capabilities, properties)) {
					activeNetworkType = NETWORK_TYPE_ETHERNET;
				}
			}
			updateActiveNetwork(activeNetworkType);
		} else {
			updateActiveNetwork(NETWORK_TYPE_NON);
		}
	}

	/**
	 * ネットワークの接続状態を更新して必要であればコールバックする
	 * API21未満でブロードキャストレシーバーでネットワークの状態変化を受け取る時の処理用
	 * @param activeNetworkInfo
	 */
	private void updateActiveNetwork(@Nullable final NetworkInfo activeNetworkInfo) {
		final int type = (activeNetworkInfo != null)
			&& (activeNetworkInfo.isConnectedOrConnecting())
				? activeNetworkInfo.getType() : -1/*TYPE_NON*/;
		int activeNetworkType = NETWORK_TYPE_NON;
		switch (type) {
		case -1:
			break;
		case ConnectivityManager.TYPE_MOBILE:
			activeNetworkType = NETWORK_TYPE_MOBILE;
			break;
		case ConnectivityManager.TYPE_WIFI:
			activeNetworkType = NETWORK_TYPE_WIFI;
			break;
		case ConnectivityManager.TYPE_ETHERNET:
			activeNetworkType = NETWORK_TYPE_ETHERNET;
			break;
		}
		updateActiveNetwork(activeNetworkType);
	}

	/**
	 * ネットワークの接続状態を更新して変更があればコールバックする
	 * @param activeNetworkType
	 */
	private void updateActiveNetwork(final int activeNetworkType) {
		synchronized (mSync) {
			if (mActiveNetworkType != activeNetworkType) {
				final int prev = mActiveNetworkType;
				mActiveNetworkType = activeNetworkType;
				callOnNetworkChanged(activeNetworkType, prev);
			}
		}
	}

//--------------------------------------------------------------------------------
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	private class MyOnNetworkActiveListener
		implements ConnectivityManager.OnNetworkActiveListener {	// API>= 21

		private final String TAG = MyOnNetworkActiveListener.class.getSimpleName();

		public MyOnNetworkActiveListener() {
			if (DEBUG) Log.v(TAG, "Constructor:");
		}

		@SuppressLint({"MissingPermission", "NewApi"})
		@Override
		public void onNetworkActive() {
			if (DEBUG) Log.v(TAG, "onNetworkActive:");
			try {
				if (BuildCheck.isAPI23()) {
					updateActiveNetwork(requireConnectivityManager().getActiveNetwork(), null, null);	// API>=23
				} else {
					// API>=21, API<23の処理
					updateActiveNetwork(requireConnectivityManager().getActiveNetworkInfo());	// API>=21
				}
			} catch (final Exception e) {
				Log.w(TAG, e);
			}
		}
	}
	
//--------------------------------------------------------------------------------
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	private class MyNetworkCallback extends ConnectivityManager.NetworkCallback {	// API>=21
		private final String TAG = MyNetworkCallback.class.getSimpleName();
	
		public MyNetworkCallback() {
			super();
			if (DEBUG) Log.v(TAG, "Constructor:");
		}
		
		@SuppressLint("MissingPermission")
		@Override
		public void onAvailable(@NonNull final Network network) {
			super.onAvailable(network);
			// ネットワークの準備ができた時
			if (DEBUG) Log.v(TAG, String.format("onAvailable:Network(%s)", network));
			updateActiveNetwork(network, null, null);
		}
		
		@SuppressLint("MissingPermission")
		@Override
		public void onCapabilitiesChanged(@NonNull final Network network,
			@NonNull final NetworkCapabilities networkCapabilities) {

			super.onCapabilitiesChanged(network, networkCapabilities);
			// 接続が完了してネットワークの状態が変わった時
			if (DEBUG) Log.v(TAG,
			String.format("onCapabilitiesChanged:Network(%s)", network)
				+ networkCapabilities);
			updateActiveNetwork(network, networkCapabilities, null);
		}
		
		@SuppressLint("MissingPermission")
		@Override
		public void onLinkPropertiesChanged(@NonNull final Network network,
			@NonNull final LinkProperties linkProperties) {

			super.onLinkPropertiesChanged(network, linkProperties);
			// ネットワークのリンク状態が変わった時
			if (DEBUG) Log.v(TAG,
				String.format("onLinkPropertiesChanged:Network(%s),", network)
				+ linkProperties);
			updateActiveNetwork(network, null, linkProperties);
		}

		@SuppressLint("MissingPermission")
		@Override
		public void onLosing(@NonNull final Network network, final int maxMsToLive) {
			super.onLosing(network, maxMsToLive);
			// 接続を失いそうな時
			if (DEBUG) Log.v(TAG, String.format("onLosing:Network(%s)", network));
			updateActiveNetwork(network, null, null);
		}
		
		@SuppressLint("MissingPermission")
		@Override
		public void onLost(@NonNull final Network network) {
			super.onLost(network);
			// 接続を失った時
			if (DEBUG) Log.v(TAG, String.format("onLost:Network(%s)", network));
			updateActiveNetwork(network, null, null);
		}
		
		@Override
		public void onUnavailable() {
			super.onUnavailable();
			// ネットワークが見つからなかった時
			// なんだろ？来ない？
			if (DEBUG) Log.v(TAG, "onUnavailable:");
			updateActiveNetwork(NETWORK_TYPE_NON);
		}
	}

//--------------------------------------------------------------------------------
	/**
	 * API21未満でブロードキャストレシーバーを使ってネットワーク状態の変化を受け取るときのBroadcastReceiver実装
	 */
	@SuppressLint("MissingPermission")
	private static class NetworkChangedReceiver extends BroadcastReceiver {
		private static final String TAG = NetworkChangedReceiver.class.getSimpleName();

		@NonNull
		private final ConnectivityHelper mParent;

		/**
		 * コンストラクタ
		 * @param parent
		 */
		public NetworkChangedReceiver(@NonNull final ConnectivityHelper parent) {
			mParent = parent;
		}
	
		@Override
		public void onReceive(final Context context, final Intent intent) {
			if (DEBUG) Log.v(TAG, "onReceive:" + intent);
			final String action = intent != null ? intent.getAction() : null;
			if (ACTION_GLOBAL_CONNECTIVITY_CHANGE.equals(action)) {
				final ConnectivityManager manager
					= ContextUtils.requireSystemService(context, ConnectivityManager.class);

				// コールバックリスナーを呼び出す
				mParent.updateActiveNetwork(manager.getActiveNetworkInfo());
			}
		}

	}

//================================================================================
// ここ以下はポーリングでネットワーク状態をチェックするためのスタティックメソッド
//================================================================================
	/**
	 * Wi-Fiでネットワーク接続しているかどうかを取得
	 * このメソッドはブロードキャストレシーバーの登録の有無と関係なく使用可
	 * @param context
	 * @return
	 */
	@SuppressLint("NewApi")
	@RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
	public static boolean isWifiNetworkReachable(@NonNull final Context context) {
//		if (DEBUG) Log.v(TAG, "isWifiNetworkReachable:");
		final ConnectivityManager manager
			= ContextUtils.requireSystemService(context, ConnectivityManager.class);
		if (BuildCheck.isAPI23()) {
			// API>=23
			@Nullable
			final Network network = manager.getActiveNetwork();	// API>=23
			@Nullable
			final NetworkCapabilities capabilities = manager.getNetworkCapabilities(network);	// API>=21
			@Nullable
			final LinkProperties properties = manager.getLinkProperties(network);	// API>=21
			return (capabilities != null) && (properties != null)
				&& isWifiNetworkReachable(manager, network, capabilities, properties);
		} else if (BuildCheck.isAPI21()) {
			// API>=21
			@NonNull
			final Network[] allNetworks = manager.getAllNetworks();	// API>=21
			for (final Network network: allNetworks) {
				@Nullable
				final NetworkCapabilities capabilities = manager.getNetworkCapabilities(network);	// API>=21
				@Nullable
				final LinkProperties properties = manager.getLinkProperties(network);	// API>=21
				if ((capabilities != null) && (properties != null)
					&& isWifiNetworkReachable(manager, network, capabilities, properties)) {

					return true;
				}
			}
		} else {
			// API<21
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
	 * モバイルネットワーク接続しているかどうかを取得
	 * このメソッドはブロードキャストレシーバーの登録の有無と関係なく使用可
	 * @param context
	 * @return
	 */
	@SuppressLint("NewApi")
	@RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
	public static boolean isMobileNetworkReachable(@NonNull final Context context) {
//		if (DEBUG) Log.v(TAG, "isMobileNetworkReachable:");
		final ConnectivityManager manager
			= ContextUtils.requireSystemService(context, ConnectivityManager.class);
		if (BuildCheck.isAPI23()) {
			// API>=23
			@Nullable
			final Network network = manager.getActiveNetwork();	// API>=23
			@Nullable
			final NetworkCapabilities capabilities = manager.getNetworkCapabilities(network);	// API>=21
			@Nullable
			final LinkProperties properties = manager.getLinkProperties(network);	// API>=21
			return (capabilities != null) && (properties != null)
				&& isMobileNetworkReachable(manager, network, capabilities, properties);
		} else if (BuildCheck.isAPI21()) {
			// API>=21
			@NonNull
			final Network[] allNetworks = manager.getAllNetworks();	// API>=21
			for (final Network network: allNetworks) {
				@Nullable
				final NetworkCapabilities capabilities = manager.getNetworkCapabilities(network);	// API>=21
				@Nullable
				final LinkProperties properties = manager.getLinkProperties(network);	// API>=21
				if ((capabilities != null) && (properties != null)
					&& isMobileNetworkReachable(manager, network, capabilities, properties)) {

					return true;
				}
			}
		} else {
			// API<21
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
	 * @param context
	 * @return
	 */
	@SuppressLint("NewApi")
	@RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
	public static boolean isNetworkReachable(@NonNull final Context context) {
//		if (DEBUG) Log.v(TAG, "isNetworkReachable:");
		final ConnectivityManager manager
			= ContextUtils.requireSystemService(context, ConnectivityManager.class);

		if (BuildCheck.isAPI23()) {
			// API>23
			@Nullable
			final Network network = manager.getActiveNetwork();	// API>=23
			@Nullable
			final NetworkCapabilities capabilities = manager.getNetworkCapabilities(network);	// API>=21
			@Nullable
			final LinkProperties properties = manager.getLinkProperties(network);	// API>=21
			return (capabilities != null) && (properties != null)
				&& isNetworkReachable(manager, network, capabilities, properties);
		} if (BuildCheck.isAPI21()) {
			// API>=21
			@NonNull
			final Network[] allNetworks = manager.getAllNetworks();	// API>=21
			for (final Network network: allNetworks) {
				@Nullable
				final NetworkCapabilities capabilities = manager.getNetworkCapabilities(network);	// API>=21
				@Nullable
				final LinkProperties properties = manager.getLinkProperties(network);	// API>=21
				if ((capabilities != null) && (properties != null)
					&& isNetworkReachable(manager, network, capabilities, properties)) {
					return true;
				}
			}
		} else {
			// API<21
			final NetworkInfo activeNetworkInfo = manager.getActiveNetworkInfo();
			return (activeNetworkInfo != null) && (activeNetworkInfo.isConnectedOrConnecting());
		}
		return false;
	}

//--------------------------------------------------------------------------------
	/**
	 * Wi-Fiでネットワーク接続しているかどうかを取得
	 * API>=21
	 * @param manager
	 * @param network
	 * @param capabilities
	 * @return
	 */
	@SuppressLint("NewApi")
	@RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	private static boolean isWifiNetworkReachable(
		@NonNull final ConnectivityManager manager,
		@NonNull final Network network,
		@NonNull final NetworkCapabilities capabilities,
		@NonNull final LinkProperties linkProperties) {

		final boolean isWiFi;
		if (BuildCheck.isAPI26()) {
			isWiFi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)		// API>=21
				|| capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET);	// API>=21
//				|| capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI_AWARE);	// API>=26 これはWi-Fi端末間での近接情報の発見機能
		} else {
			isWiFi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)		// API>=21
				|| capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET);	// API>=21
		}
		return isWiFi && isNetworkReachable(manager, network, capabilities, linkProperties);
	}

	/**
	 * モバイルネットワーク接続しているかどうかを取得
	 * API>=21
	 * @param manager
	 * @param network
	 * @param capabilities
	 * @return
	 */
	@SuppressLint("NewApi")
	@RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	private static boolean isMobileNetworkReachable(
		@NonNull final ConnectivityManager manager,
		@NonNull final Network network,
		@NonNull final NetworkCapabilities capabilities,
		@NonNull final LinkProperties linkProperties) {

		final boolean isMobile;
		if (BuildCheck.isAPI27()) {
			isMobile = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)// API>=21
				|| capabilities.hasTransport(NetworkCapabilities.TRANSPORT_LOWPAN);	// API>=27
		} else {
			isMobile = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);// API>=21
		}
		return isMobile && isNetworkReachable(manager, network, capabilities, linkProperties);
	}

	/**
	 * Bluetoothを使ったネットワーク接続をしているかどうかを取得
	 * API>=21
	 * @param manager
	 * @param network
	 * @param capabilities
	 * @return
	 */
	@RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	private static boolean isBluetoothNetworkReachable(
		@NonNull final ConnectivityManager manager,
		@NonNull final Network network,
		@NonNull final NetworkCapabilities capabilities,
		@NonNull final LinkProperties linkProperties) {

		return
			capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH)// API>=21
				&& isNetworkReachable(manager, network, capabilities, linkProperties);
	}

	/**
	 * ネットワーク接続しているかどうかを取得
	 * API>=21
	 * @param manager
	 * @param network
	 * @param capabilities
	 * @return
	 */
	@SuppressLint("NewApi")
	@RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	private static boolean isNetworkReachable(
		@NonNull final ConnectivityManager manager,
		@NonNull final Network network,
		@NonNull final NetworkCapabilities capabilities,
		@NonNull final LinkProperties linkProperties) {

		if (DEBUG) Log.v(TAG, "isNetworkReachable:capabilities=" + capabilities);
		if (DEBUG) Log.v(TAG, "isNetworkReachable:linkProperties=" + linkProperties);
		boolean hasLinkAddress = !linkProperties.getLinkAddresses().isEmpty();
		final boolean hasCapability;
		if (BuildCheck.isAPI29()) {
			hasCapability = hasLinkAddress
				&& capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)			// API>=21
				&& capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)			// API>=23
				&& (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_SUSPENDED)	// API>=28
					|| capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_FOREGROUND));	// API>=28
		} else {
			// API>=21
			@Nullable
			final NetworkInfo info = manager.getNetworkInfo(network);	// API>=21, API<29
			if (DEBUG) Log.v(TAG, "isNetworkReachable:info=" + info);
			hasLinkAddress = hasLinkAddress && info.isConnectedOrConnecting();
			if (BuildCheck.isAPI28()) {
				// API>=28
				hasCapability = hasLinkAddress
					&& capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)			// API>=21
					&& capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)			// API>=23
					&& (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_SUSPENDED)	// API>=28
						|| capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_FOREGROUND));	// API>=28
			} else if (BuildCheck.isAPI23()) {
				// API>=23
				hasCapability = hasLinkAddress
					&& capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)			// API>=21
					&& capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);		// API>=23
			} else {
				// API>=21
				hasCapability = hasLinkAddress
					&& capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);			// API>=21
			}
		}
		if (DEBUG) {
			Log.v(TAG, "isNetworkReachable:hasCapability=" + hasCapability
				+ ",hasLinkAddress=" + hasLinkAddress
				+ ",NOT_SUSPENDED=" + capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_SUSPENDED)
				+ ",FOREGROUND=" + capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_FOREGROUND));
		}
		return hasCapability;
	}

	/**
	 * 指定した数値に対応するネットワークの種類を示す文字列を取得する
	 * @param networkType
	 * @return
	 */
	public static String getNetworkTypeString(final int networkType) {
		switch (networkType) {
		case NETWORK_TYPE_NON:
			return "NON";
		case NETWORK_TYPE_MOBILE:
			return "MOBILE";
		case NETWORK_TYPE_WIFI:
			return "WIFI";
		case NETWORK_TYPE_BLUETOOTH:
			return "BLUETOOTH";
		case NETWORK_TYPE_ETHERNET:
			return "ETHERNET";
		default:
			return String.format(Locale.US, "UNKNOWN(%d)", networkType);
		}
	}

}
