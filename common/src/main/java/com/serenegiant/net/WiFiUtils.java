package com.serenegiant.net;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2025 saki t_saki@serenegiant.com
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

import android.Manifest;
import android.content.Context;
import android.net.wifi.WifiManager;
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

//	/**
//	 * 接続中のWiFiインターフェースのMACアドレスを取得する
//	 * XXX ハードウエアIDの１つで一般的には利用を推奨されていないので利用時は注意
//	 *     Android6以降では通常のアプリから呼び出すとダミーのMACアドレス(02:00:00:00:00:00)が返る
//	 * @param context
//	 * @return
//	 */
//	@SuppressLint("HardwareIds")
//	@RequiresPermission(allOf = {Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.ACCESS_FINE_LOCATION})
//	@Nullable
//	public static String getMacAddress(@NonNull final Context context) {
//		final WifiManager wifi
//			= ContextUtils.requireSystemService(context, WifiManager.class);
//		final WifiInfo info = wifi.getConnectionInfo();
//		return info != null ? info.getMacAddress() : null;
//	}
}
