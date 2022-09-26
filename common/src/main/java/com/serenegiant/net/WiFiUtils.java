package com.serenegiant.net;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.util.Log;

import com.serenegiant.system.ContextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;

/**
 * WiFi関係のユーティリティクラス
 */
public class WiFiUtils {
	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = WiFiUtils.class.getSimpleName();

	private WiFiUtils() {
		// インスタンス化をエラーとするためにデフォルトコンストラクタをprivateに
	}

   /**
  	 * WiFiが有効になっているかどうか
	 * @param context
  	 * @return true: WiFiが有効, false: WiFiが無効
  	 */
   @RequiresPermission(Manifest.permission.ACCESS_WIFI_STATE)
  	public static boolean isWiFiEnabled(@NonNull final Context context) {
		final WifiManager wifi
			= ContextUtils.requireSystemService(context, WifiManager.class);
		return wifi.isWifiEnabled();
  	}

  	/**
  	 * WiFiのON/OFFを切り替え
  	 * AndroidQ/Android10/API29以降ではデバイスオーナーアプリ/プロファイルオーナーアプリ
  	 *   システムアプリ以外では動作しないので端末のWiFi設定を開くようにしないといけない
  	 * @param context
  	 * @param enable
  	 * @return true: 切り替え成功, false: 切り替え失敗(端末のWiFi設定画面を開かないといけない)
  	 */
  	@RequiresPermission(Manifest.permission.CHANGE_WIFI_STATE)
  	public static boolean setEnableWiFi(
  		@NonNull final Context context,
  		final boolean enable) {

		final WifiManager wifi
			= ContextUtils.requireSystemService(context, WifiManager.class);
		try {
			if (wifi.isWifiEnabled() != enable) {
				if (DEBUG) Log.v(TAG, "setEnableWiFi:WiFi "
					+ (wifi.isWifiEnabled() ? "ON" : "OFF")
					+ "=>" + (enable ? "ON" : "OFF"));
				return wifi.setWifiEnabled(enable);
			} else {
				// 現在の状態と一致したときはtrue
				return true;
			}
		} catch (final  Exception e) {
			if (DEBUG) Log.w(TAG, e);
		}
		return false;
  	}

}
