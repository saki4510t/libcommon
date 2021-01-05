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

package com.serenegiant.utils;

import android.app.Activity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.serenegiant.system.ContextUtils;

import androidx.annotation.NonNull;

/**
 * ソフトウエアキーボードの表示状態等を操作するためのヘルパークラス
 */
public class KeyboardUtils {
	private KeyboardUtils() {
		// インスタンス化をエラーとするためにデフォルトコンストラクタをprivateにする
	}

	/**
	 * ソフトウエアキーボードを非表示にする
	 * @param view
	 */
	public static void hide(@NonNull View view) {
		final InputMethodManager imm = ContextUtils.requireSystemService(view.getContext(), InputMethodManager.class);
		view.clearFocus();
	    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
	}

	/**
	 * ソフトウエアキーボードを非表示にする
	 * @param activity
	 */
	public static void hide(@NonNull final Activity activity) {
		final InputMethodManager imm = ContextUtils.requireSystemService(activity, InputMethodManager.class);
		//Find the currently focused view, so we can grab the correct window token from it.
		View view = activity.getCurrentFocus();
		   // If no view currently has focus, create a new one, just so we can grab a window token from it
		if (view == null) {
	        view = new View(activity);
	    }
	    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
	}
}
