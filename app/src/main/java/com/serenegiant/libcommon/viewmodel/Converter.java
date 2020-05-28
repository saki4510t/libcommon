package com.serenegiant.libcommon.viewmodel;

import android.text.TextUtils;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.databinding.InverseMethod;

/**
 * Converter#requestCodeToString/stringToRequestCodeを使うと
 * kotline関係の処理でエラーになってコード生成できないのでConverterは使わずに
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
