package com.serenegiant.net;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.net.wifi.SoftApConfiguration;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;

import com.serenegiant.system.ContextUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;

/**
 * アンドロイドのWiFiアクセスポイント(Wi-Fiテザリング)機能用のユーティリティクラス
 */
public class WifiApUtils {
	private static final boolean DEBUG = false;    // set false on production
	private static final String TAG = WifiApUtils.class.getSimpleName();

	@IntDef({
		WIFI_AP_STATE_DISABLING,
		WIFI_AP_STATE_DISABLED,
		WIFI_AP_STATE_ENABLING,
		WIFI_AP_STATE_ENABLED,
		WIFI_AP_STATE_FAILED,
	})
	@Retention(RetentionPolicy.SOURCE)
	public @interface WifiApState {
	}

	/**
	 * Wi-Fi AP is currently being disabled. The state will change to
	 * {@link #WIFI_AP_STATE_DISABLED} if it finishes successfully.
	 */
	public static final int WIFI_AP_STATE_DISABLING = 10;
	/**
	 * Wi-Fi AP is disabled.
	 */
	public static final int WIFI_AP_STATE_DISABLED = 11;
	/**
	 * Wi-Fi AP is currently being enabled. The state will change to
	 * {@link #WIFI_AP_STATE_ENABLED} if it finishes successfully.
	 */
	public static final int WIFI_AP_STATE_ENABLING = 12;
	/**
	 * Wi-Fi AP is enabled.
	 */
	public static final int WIFI_AP_STATE_ENABLED = 13;
	/**
	 * Wi-Fi AP is in a failed state. This state will occur when an error occurs during
	 * enabling or disabling
	 */
	public static final int WIFI_AP_STATE_FAILED = 14;

	@IntDef({
		SAP_START_FAILURE_GENERAL,
		SAP_START_FAILURE_NO_CHANNEL,
		SAP_START_FAILURE_UNSUPPORTED_CONFIGURATION,
	})
	@Retention(RetentionPolicy.SOURCE)
	public @interface SapStartFailure {
	}

	/**
	 * All other reasons for AP start failure besides {@link #SAP_START_FAILURE_NO_CHANNEL} and
	 * {@link #SAP_START_FAILURE_UNSUPPORTED_CONFIGURATION}.
	 */
	public static final int SAP_START_FAILURE_GENERAL = 0;

	/**
	 * If Wi-Fi AP start failed, this reason code means that no legal channel exists on user
	 * selected band due to regulatory constraints.
	 */
	public static final int SAP_START_FAILURE_NO_CHANNEL = 1;

	/**
	 * If Wi-Fi AP start failed, this reason code means that the specified configuration
	 * is not supported by the current HAL version.
	 */
	public static final int SAP_START_FAILURE_UNSUPPORTED_CONFIGURATION = 2;

	private WifiApUtils() {
		// インスタンス化をエラーとするためにデフォルトコンストラクタをprivateに
	}

	/**
	 * リフレクションを使ってWiFiManager#getWifiApStateを呼び出すためのヘルパーメソッド
	 * @param context
	 * @return
	 * @throws UnsupportedOperationException
	 */
	@SuppressWarnings("all")
	@RequiresPermission(android.Manifest.permission.ACCESS_WIFI_STATE)
	@WifiApState
	public static int getWifiApState(@NonNull final Context context)
		throws UnsupportedOperationException {

		final WifiManager manager
			= ContextUtils.requireSystemService(context, WifiManager.class);
		final Class<WifiManager> clazz = WifiManager.class;
		try {
			final Method getWifiApState
				= clazz.getMethod("getWifiApState");
			getWifiApState.setAccessible(true);
			return (Integer) getWifiApState.invoke(manager);
		} catch (final SecurityException e) {
			if (DEBUG) Log.w(TAG, e);
		} catch (final NoSuchMethodException e) {
			if (DEBUG) Log.w(TAG, e);
		} catch (final Exception e) {
			if (DEBUG) Log.w(TAG, e);
		}
		throw new UnsupportedOperationException();
	}

	/**
	 * #getWifiApStateのラッパー
	 * こっちはACCESS_WIFI_STATEがなくても例外生成しない(falseを返す)
	 * @param context
	 * @return
	 */
	@SuppressLint("MissingPermission")
	public static boolean isWifiApEnabled(@NonNull final Context context) {
		try {
			return getWifiApState(context) == WIFI_AP_STATE_ENABLED;
		} catch (final Exception e) {
			if (DEBUG) Log.w(TAG, e);
		}
		return false;
	}

	/**
	 * リフレクションを使ってWiFiManager#getWifiApStateを呼び出すためのヘルパーメソッド
	 * こっちは文字列として返す
	 * @param context
	 * @return
	 */
	@RequiresPermission(android.Manifest.permission.ACCESS_WIFI_STATE)
	@NonNull
	public static String getWifiApStateString(@NonNull final Context context) {
		return getWifiApStateString(getWifiApState(context));
	}

	@NonNull
	public static String getWifiApStateString(@WifiApState final int state) {
		switch (state) {
		case WIFI_AP_STATE_DISABLING:
			return "disabling";
		case WIFI_AP_STATE_DISABLED:
			return "disabled";
		case WIFI_AP_STATE_ENABLING:
			return "enabling";
		case WIFI_AP_STATE_ENABLED:
			return "enabled";
		case WIFI_AP_STATE_FAILED:
			return "failed";
		default:
			return "";
		}
	}

	/**
	 * WiFiアクセスポイント/テザリングの設定を取得する
	 * XXX WifiManager#getWifiApConfigurationのソースコードを見ると
	 *     ACCESS_WIFI_STATEパーミッションがあればアクセスできそうだけど
	 *     SecurityExceptionになって実際には実行できない(少なくともAndroid8以降はだめ)
	 *     (NETWORK_SETTINGSパーミッションが必要な気がする)
	 * @param context
	 * @return
	 * @throws UnsupportedOperationException
	 */
	@SuppressWarnings("all")
	@RequiresPermission(android.Manifest.permission.ACCESS_WIFI_STATE)
	public static WifiConfiguration getWifiApConfiguration(@NonNull final Context context)
		throws UnsupportedOperationException {

		final WifiManager manager
			= ContextUtils.requireSystemService(context, WifiManager.class);
		final Class<WifiManager> clazz = WifiManager.class;
		try {
			final Method getWifiApConfiguration
				= clazz.getMethod("getWifiApConfiguration");
			getWifiApConfiguration.setAccessible(true);
			return (WifiConfiguration) getWifiApConfiguration.invoke(manager);
		} catch (final Exception e) {
			if (DEBUG) Log.w(TAG, e);
			throw new UnsupportedOperationException(e);
		}
	}

	/**
	 * WiFiアクセスポイント/テザリングの設定を取得する
	 * XXX NETWORK_SETTINGSパーミッションはシステムアプリにしか付与できないので実行できない
	 * API>=30
	 * @param context
	 * @return
	 * @throws UnsupportedOperationException
	 */
	@SuppressWarnings("all")
	@TargetApi(Build.VERSION_CODES.R)
	@RequiresPermission("android.Manifest.permission.NETWORK_SETTINGS")
	public static SoftApConfiguration getSoftApConfiguration(@NonNull final Context context)
		throws UnsupportedOperationException {

		final WifiManager manager
			= ContextUtils.requireSystemService(context, WifiManager.class);
		final Class<WifiManager> clazz = WifiManager.class;
		try {
			final Method getSoftApConfiguration
				= clazz.getMethod("getSoftApConfiguration");
			getSoftApConfiguration.setAccessible(true);
			return (SoftApConfiguration) getSoftApConfiguration.invoke(manager);
		} catch (final Exception e) {
			if (DEBUG) Log.w(TAG, e);
			throw new UnsupportedOperationException(e);
		}
	}
}
