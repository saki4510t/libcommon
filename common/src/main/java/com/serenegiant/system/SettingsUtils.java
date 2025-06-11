package com.serenegiant.system;
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

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.provider.Settings;

import androidx.annotation.NonNull;

public class SettingsUtils {

	private SettingsUtils() {
		// インスタンス化をエラーにするためにデフォルトコンストラクタをprivateに
	}

	public static int getMinimumScreenBrightnessSetting(final Context context) {
		final Resources res = Resources.getSystem();
		int id = res.getIdentifier("config_screenBrightnessSettingMinimum", "integer", "android"); // API17+
		if (id == 0)
			id = res.getIdentifier("config_screenBrightnessDim", "integer", "android"); // lower API levels
		if (id != 0) {
			try {
				return res.getInteger(id);
			} catch (final Resources.NotFoundException e) {
				// ignore
			}
		}
		return 0;
	}

	public static int getMaximumScreenBrightnessSetting(final Context context) {
		final Resources res = Resources.getSystem();
		final int id = res.getIdentifier("config_screenBrightnessSettingMaximum", "integer", "android"); // API17+
		if (id != 0) {
			try {
				return res.getInteger(id);
			} catch (final Resources.NotFoundException e) {
				// ignore
			}
		}
		return 255;
	}

//--------------------------------------------------------------------------------
// 端末の設定画面関係
	/**
	 * 端末の設定画面
	 * @param context
	 */
	public static void openSettings(@NonNull final Context context) {
		final Intent intent = new Intent();
		intent.setAction(Settings.ACTION_SETTINGS);
		context.startActivity(intent);
	}

	/**
	 * 機内モード設定画面
	 * @param context
	 */
	public static void openSettingsAirplaneMode(@NonNull final Context context) {
		final Intent intent = new Intent();
		intent.setAction(Settings.ACTION_AIRPLANE_MODE_SETTINGS);
		context.startActivity(intent);
	}

	/**
	 * Bluetooth設定画面
	 * @param context
	 */
	public static void openSettingsBluetooth(@NonNull final Context context) {
		final Intent intent = new Intent();
		intent.setAction(Settings.ACTION_BLUETOOTH_SETTINGS);
		context.startActivity(intent);
	}

	/**
	 * Wi-Fi設定画面
	 * @param context
	 */
	public static void openSettingsWiFi(@NonNull final Context context) {
		final Intent intent = new Intent();
		intent.setAction(Settings.ACTION_WIFI_SETTINGS);
		context.startActivity(intent);
	}

	/**
	 * 無線とネットワーク設定画面
	 * @param context
	 */
	public static void openSettingsWireless(@NonNull final Context context) {
		final Intent intent = new Intent();
		intent.setAction(Settings.ACTION_WIRELESS_SETTINGS);
		context.startActivity(intent);
	}

	/**
	 * データローミング設定画面(モバイルネットワーク設定画面)
	 * @param context
	 */
	public static void openSettingsDataRoaming(@NonNull final Context context) {
		final Intent intent = new Intent();
		intent.setAction(Settings.ACTION_DATA_ROAMING_SETTINGS);
		context.startActivity(intent);
	}

	/**
	 * APN設定画面
	 * @param context
	 */
	public static void openSettingsAPN(@NonNull final Context context) {
		final Intent intent = new Intent();
		intent.setAction(Settings.ACTION_APN_SETTINGS);
		context.startActivity(intent);
	}

	private static final String ACTION_TETHER_SETTINGS = "android.settings.TETHER_SETTINGS";

	/**
	 * テザリング設定画面
	 * @param context
	 */
	public static void openSettingsTether(@NonNull final Context context) {
		final Intent intent = new Intent();
		intent.setAction(ACTION_TETHER_SETTINGS);
		context.startActivity(intent);
	}

//--------------------------------------------------------------------------------
// アプリ固有の設定画面関係
	/**
	 * アプリの詳細設定へ遷移させる(パーミッションを取得できなかった時など)
	 *
	 * @param context
	 */
	public static void openSettingsAppDetails(@NonNull final Context context) {
		final Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
		final Uri uri = Uri.fromParts("package", context.getPackageName(), null);
		intent.setData(uri);
		context.startActivity(intent);
	}

}
