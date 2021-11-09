package com.serenegiant.utils;
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

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;

import androidx.annotation.ArrayRes;
import androidx.annotation.NonNull;

/**
 * TypedArray用のヘルパークラス
 */
public class TypedArrayUtils {
	private TypedArrayUtils() {
		// インスタンス化をエラーとするためにデフォルトコンストラクタをprivateに
	}

	/**
	 * xmlで定義したfloat arrayを読み込む
	 * @param res
	 * @param arrayId
	 * @param defaultValue
	 * @return
	 */
	@NonNull
	public static float[] readArray(@NonNull final Resources res,
		@ArrayRes final int arrayId, final float defaultValue) {

		float[] result;
		final TypedArray a = res.obtainTypedArray(arrayId);
		try {
			final int n = a.length();
			result = new float[n];
			for (int i = 0; i < n; i++) {
				result[i] = a.getFloat(i, defaultValue);
			}
		} finally {
			a.recycle();
		}
		return result;
	}

	/**
	 * xmlで定義したboolean arrayを読み込む
	 * @param res
	 * @param arrayId
	 * @param defaultValue
	 * @return
	 */
	@NonNull
	public static boolean[] readArray(@NonNull final Resources res,
		@ArrayRes final int arrayId, final boolean defaultValue) {

		boolean[] result;
		final TypedArray a = res.obtainTypedArray(arrayId);
		try {
			final int n = a.length();
			result = new boolean[n];
			for (int i = 0; i < n; i++) {
				result[i] = a.getBoolean(i, defaultValue);
			}
		} finally {
			a.recycle();
		}
		return result;
	}

	/**
	 * xmlで定義したstring arrayを読み込む
	 * @param res
	 * @param arrayId
	 * @param defaultValue
	 * @return
	 */
	@NonNull
	public static String[] readArray(@NonNull final Resources res,
		@ArrayRes final int arrayId, final String defaultValue) {

		String[] result;
		final TypedArray a = res.obtainTypedArray(arrayId);
		try {
			final int n = a.length();
			result = new String[n];
			for (int i = 0; i < n; i++) {
				result[i] = a.getString(i);
				if (result[i] == null) {
					result[i] = defaultValue;
				}
			}
		} finally {
			a.recycle();
		}
		return result;
	}

	/**
	 * xmlで定義したstring arrayを読み込む
	 * @param res
	 * @param arrayId
	 * @param defaultValue
	 * @return
	 */
	@NonNull
	public static CharSequence[] readArray(@NonNull final Resources res,
		@ArrayRes final int arrayId, final CharSequence defaultValue) {

		CharSequence[] result;
		final TypedArray a = res.obtainTypedArray(arrayId);
		try {
			final int n = a.length();
			result = new CharSequence[n];
			for (int i = 0; i < n; i++) {
				result[i] = a.getText(i);
				if (result[i] == null) {
					result[i] = defaultValue;
				}
			}
		} finally {
			a.recycle();
		}
		return result;
	}

	/**
	 * xmlで定義したint arrayを読み込む
	 * @param res
	 * @param arrayId
	 * @param defaultValue
	 * @return
	 */
	@NonNull
	public static int[] readArray(@NonNull final Resources res,
		@ArrayRes final int arrayId, final int defaultValue) {

		int[] result;
		final TypedArray a = res.obtainTypedArray(arrayId);
		try {
			final int n = a.length();
			result = new int[n];
			for (int i = 0; i < n; i++) {
				result[i] = a.getInt(i, defaultValue);
			}
		} finally {
			a.recycle();
		}
		return result;
	}

	/**
	 * xmlで定義したint arrayを読み込む
	 * xmlで定義している値がintとして扱えないときはUnsupportedOperationExceptionを投げる
	 * @param res
	 * @param arrayId
	 * @param defaultValue
	 * @return
	 * @throws UnsupportedOperationException
	 */
	@NonNull
	public static int[] readArrayWithException(@NonNull final Resources res,
		@ArrayRes final int arrayId, final int defaultValue)
			throws UnsupportedOperationException {

		int[] result;
		final TypedArray a = res.obtainTypedArray(arrayId);
		try {
			final int n = a.length();
			result = new int[n];
			for (int i = 0; i < n; i++) {
				result[i] = a.getInteger(i, defaultValue);
			}
		} finally {
			a.recycle();
		}
		return result;
	}

	/**
	 * xmlで定義したcolor arrayをint配列へ読み込む
	 * @param res
	 * @param arrayId
	 * @param defaultValue
	 * @return
	 */
	@NonNull
	public static int[] readColorArray(@NonNull final Resources res,
		@ArrayRes final int arrayId, final int defaultValue) {

		int[] result;
		final TypedArray a = res.obtainTypedArray(arrayId);
		try {
			final int n = a.length();
			result = new int[n];
			for (int i = 0; i < n; i++) {
				result[i] = a.getColor(i, defaultValue);
			}
		} finally {
			a.recycle();
		}
		return result;
	}
	/**
	 * xmlで定義したDimension arrayをfloat配列へ読み込む
	 * @param res
	 * @param arrayId
	 * @param defaultValue
	 * @return
	 */
	@NonNull
	public static float[] readDimensionArray(@NonNull final Resources res,
		@ArrayRes final int arrayId, final float defaultValue) {

		float[] result;
		final TypedArray a = res.obtainTypedArray(arrayId);
		try {
			final int n = a.length();
			result = new float[n];
			for (int i = 0; i < n; i++) {
				result[i] = a.getDimension(i, defaultValue);
			}
		} finally {
			a.recycle();
		}
		return result;
	}

	/**
	 * xmlで定義したdrawable arrayを読み込む
	 * @param res
	 * @param arrayId
	 * @return
	 */
	@NonNull
	public static Drawable[] readDrawableArray(@NonNull final Resources res,
		@ArrayRes final int arrayId) {

		Drawable[] result;
		final TypedArray a = res.obtainTypedArray(arrayId);
		try {
			final int n = a.length();
			result = new Drawable[n];
			for (int i = 0; i < n; i++) {
				result[i] = a.getDrawable(i);
			}
		} finally {
			a.recycle();
		}
		return result;
	}

	/**
	 * @return The resource ID value in the {@code context} specified by {@code attr}. If it does
	 * not exist, {@code fallbackAttr}.
	 */
	public static int getAttr(@NonNull final Context context, final int attr, final int fallbackAttr) {
		final TypedValue value = new TypedValue();
		context.getTheme().resolveAttribute(attr, value, true);
		if (value.resourceId != 0) {
			return attr;
		}
		return fallbackAttr;
	}

}
