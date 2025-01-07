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
