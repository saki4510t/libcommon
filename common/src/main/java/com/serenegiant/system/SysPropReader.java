package com.serenegiant.system;
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

import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import androidx.annotation.NonNull;

public class SysPropReader {
	private static final boolean DEBUG = false;    // set false on production
	private static final String TAG = SysPropReader.class.getSimpleName();
	private static final String GETPROP_EXECUTABLE_PATH = "/system/bin/getprop";

	private SysPropReader() {
		// インスタンス化をエラーにするためにデフォルトコンストラクタをprivateに
	}

	/**
	 * システムプロパティを読み込み文字列として返す
	 * システムプロパティの値が1行で無い場合は最初の行のみ読み込む
	 * @param propName
	 * @return
	 */
	@NonNull
	public static String read(@NonNull final  String propName) {
		Process process = null;
		BufferedReader bufferedReader = null;
		String result = null;

		try {
			process = new ProcessBuilder()
				.command(GETPROP_EXECUTABLE_PATH, propName)
				.redirectErrorStream(true)
				.start();
			bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			result = bufferedReader.readLine();
			if (DEBUG) Log.v(TAG, "read System Property: " + propName + "=" + result);
		} catch (final Exception e) {
			if (DEBUG) Log.d(TAG, "Failed to read System Property " + propName, e);
		} finally {
			if (bufferedReader != null) {
				try {
					bufferedReader.close();
				} catch (final IOException e) {
					if (DEBUG) Log.w(TAG, e);
				}
			}
			if (process != null) {
				process.destroy();
			}
		}
		if (TextUtils.isEmpty(result)) {
			// 指定したプロパティがセットされていないか読み込み時にエラーが発生した
			result = "";
		}
		return result;
	}

	/**
	 * システムプロパティを読み込み文字列として返す
	 * システムプロパティの値が1行で無い場合もすべて読み込む
	 * @param propName
	 * @return
	 */
	@NonNull
	public static String readAll(@NonNull final  String propName) {
		final StringBuilder sb = new StringBuilder();

		Process process = null;
		BufferedReader bufferedReader = null;

		try {
			process = new ProcessBuilder()
				.command(GETPROP_EXECUTABLE_PATH, propName)
				.redirectErrorStream(true)
				.start();
			bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line;
			do {
				line = bufferedReader.readLine();
				if (!TextUtils.isEmpty(line)) {
					if (sb.length() > 0) {
						sb.append("\n");
					}
					sb.append(line);
				}
			} while (!TextUtils.isEmpty(line));
		} catch (final Exception e) {
			if (DEBUG) Log.d(TAG, "Failed to read System Property " + propName, e);
		} finally {
			if (bufferedReader != null) {
				try {
					bufferedReader.close();
				} catch (final IOException e) {
					if (DEBUG) Log.w(TAG, e);
				}
			}
			if (process != null) {
				process.destroy();
			}
		}

		return sb.toString();
	}

}
