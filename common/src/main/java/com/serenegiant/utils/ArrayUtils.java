package com.serenegiant.utils;

import androidx.annotation.NonNull;

public class ArrayUtils {
	private ArrayUtils() {
		// インスタンス化をエラーにするためにコンストラクタをprivateに
	}

	/**
	 * 指定した値が配列に含まれているかどうかをチェック
	 * T#equalsで一致するかどうかをチェックする
	 * @param values
	 * @param value
	 * @param <T>
	 * @return
	 */
	public static <T> boolean contains(@NonNull final T[] values, final T value) {
		for (final T v: values) {
			if ((v != null) && v.equals(value)) {
				return true;
			}
		}
		return false;
	}
}
