package com.serenegiant.system;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

/**
 * Context関係のラッパークラス
 */
public class ContextUtils {
	private ContextUtils() {
		// インスタンス化をエラーにするためにデフォルトコンストラクタをprivateに
	}

	/**
	 * ContextCompat.getSystemServiceを呼び出すだけ
	 * (たぶんAPIレベルをチェックしていないときにnullになる)
	 * @param context
	 * @param serviceClass
	 * @param <T>
	 * @return
	 */
	@Nullable
	public static <T> T getSystemService(@NonNull Context context, @NonNull Class<T> serviceClass) {
		return ContextCompat.getSystemService(context, serviceClass);
	}

	/**
	 * ContextCompat.getSystemServiceを呼び出して取得した結果がnullならIllegalArgumentExceptionを投げる
	 * (たぶんAPIレベルをチェックしていないときにnullになる)
	 * @param context
	 * @param serviceClass
	 * @param <T>
	 * @return
	 * @throws IllegalArgumentException
	 */
	@NonNull
	public static <T> T requireSystemService(@NonNull Context context, @NonNull Class<T> serviceClass)
		throws IllegalArgumentException {

		final T result = ContextCompat.getSystemService(context, serviceClass);
		if (result != null) {
			return result;
		} else {
			throw new IllegalArgumentException();
		}
	}

}
