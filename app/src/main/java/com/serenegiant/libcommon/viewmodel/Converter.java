package com.serenegiant.libcommon.viewmodel;
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

import android.text.TextUtils;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.databinding.InverseMethod;

/**
 * Converter#requestCodeToString/stringToRequestCodeを使うと
 * kotlin関係の処理でエラーになってコード生成できないのでConverterは使わずに
 * 文字列用の双方向バインディングをViewModel側で処理する
 */
public class Converter {

	/**
	 * リクエストコードをEditTextで表示するために文字列に変換
	 * @param view
	 * @param oldValue
	 * @param value
	 * @return
	 */
	@InverseMethod("stringToRequestCode")
	public static String requestCodeToString(
		@NonNull final EditText view,
		final int oldValue, final int value) {

		return Integer.toString(value);
	}

	/**
	 * EditTextで編集したリクエストコード文字列をリクエストコードに変換
	 * @param view
	 * @param oldValue
	 * @param value
	 * @return
	 */
	public static int stringToRequestCode(
		@NonNull final EditText view,
		final int oldValue, final String value) {

		if (!TextUtils.isEmpty(value)) {
			try {
				return Integer.parseInt(value);
			} catch (final NumberFormatException e) {
				// ignore
			}
		}
		return oldValue;
	}

}
