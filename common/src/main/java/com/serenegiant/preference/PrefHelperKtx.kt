package com.serenegiant.preference
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

import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit

private const val DEBUG = false
private const val TAG = "PrefHelperKtx"

//--------------------------------------------------------------------------------
fun SharedPreferences?.hasPref(
	key: String): Boolean {
	return this != null && this.contains(key)
}

fun SharedPreferences?.getPref(
	key: String, defaultValue: Boolean): Boolean {
	return if (this != null) PrefHelper.get(
		this,
		key,
		defaultValue
	) else defaultValue
}

fun SharedPreferences?.getPref(
	key: String, defaultValue: Int): Int {
	return if (this != null) PrefHelper.get(
		this,
		key,
		defaultValue
	) else defaultValue
}

fun SharedPreferences?.getPref(
	key: String, defaultValue: Long): Long {
	return if (this != null) PrefHelper.get(
		this,
		key,
		defaultValue
	) else defaultValue
}

fun SharedPreferences?.getPref(
	key: String, defaultValue: String?): String? {
	return if (this != null) this.getString(key, defaultValue) else defaultValue
}

fun SharedPreferences?.getPref(
	key: String, defaultValue: Float
): Float {
	return this?.getFloat(key, defaultValue) ?: defaultValue
}

fun SharedPreferences?.getPref(
	key: String, defaultValues: IntArray): IntArray {
	val n = defaultValues.size
	val result = IntArray(n)
	if (this != null) {
		for (i in 0 until n) {
			result[i] = PrefHelper.get(this, key + i, defaultValues[i])
		}
	}
	return result
}

fun SharedPreferences?.setPref(
	key: String, value: Boolean) {
	this?.edit { putBoolean(key, value) }
}

fun SharedPreferences?.setPref(
	key: String, value: Int) {
	this?.edit { putInt(key, value) }
}

fun SharedPreferences?.setPref(
	key: String, value: Long) {
	this?.edit { putLong(key, value) }
}

fun SharedPreferences?.setPref(
	key: String, value: String?) {
	this?.edit { putString(key, value) }
}

fun SharedPreferences?.setPref(
	key: String, value: Float) {
	this?.edit { putFloat(key, value) }
}

fun SharedPreferences?.clearPref(
	key: String) {
	this?.edit {remove(key) }
		?: Log.d(TAG, "can't get SharedPreferences")
}
