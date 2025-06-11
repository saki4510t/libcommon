package com.serenegiant.system
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

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

fun Context?.getMinimumScreenBrightnessSetting(): Int {
	return SettingsUtils.getMinimumScreenBrightnessSetting(this)
}

fun Context?.getMaximumScreenBrightnessSetting(): Int {
	return SettingsUtils.getMinimumScreenBrightnessSetting(this)
}


//--------------------------------------------------------------------------------
// 端末の設定画面関係

/**
 * 端末の設定画面
 */
fun Context.openSettings() {
	val intent = Intent()
	intent.setAction(Settings.ACTION_SETTINGS)
	startActivity(intent)
}

/**
 * 機内モード設定画面
 */
fun Context.openSettingsAirplaneMode() {
	val intent = Intent()
	intent.setAction(Settings.ACTION_AIRPLANE_MODE_SETTINGS)
	startActivity(intent)
}

/**
 * Bluetooth設定画面
 */
fun Context.openSettingsBluetooth() {
	val intent = Intent()
	intent.setAction(Settings.ACTION_BLUETOOTH_SETTINGS)
	startActivity(intent)
}

/**
 * Wi-Fi設定画面
 */
fun Context.openSettingsWiFi() {
	val intent = Intent()
	intent.setAction(Settings.ACTION_WIFI_SETTINGS)
	startActivity(intent)
}

/**
 * 無線とネットワーク設定画面
 */
fun Context.openSettingsWireless() {
	val intent = Intent()
	intent.setAction(Settings.ACTION_WIRELESS_SETTINGS)
	startActivity(intent)
}

/**
 * データローミング設定画面(モバイルネットワーク設定画面)
 */
fun Context.openSettingsDataRoaming() {
	val intent = Intent()
	intent.setAction(Settings.ACTION_DATA_ROAMING_SETTINGS)
	startActivity(intent)
}

/**
 * APN設定画面
 */
fun Context.openSettingsAPN() {
	val intent = Intent()
	intent.setAction(Settings.ACTION_APN_SETTINGS)
	startActivity(intent)
}

private const val ACTION_TETHER_SETTINGS = "android.settings.TETHER_SETTINGS"

/**
 * テザリング設定画面
 */
fun Context.openSettingsTether() {
	val intent = Intent()
	intent.setAction(ACTION_TETHER_SETTINGS)
	startActivity(intent)
}

//--------------------------------------------------------------------------------
// アプリ固有の設定画面関係
/**
 * アプリの詳細設定へ遷移させる(パーミッションを取得できなかった時など)
 */
fun Context.openSettingsAppDetails() {
	val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
	val uri = Uri.fromParts("package", packageName, null)
	intent.setData(uri)
	startActivity(intent)
}
