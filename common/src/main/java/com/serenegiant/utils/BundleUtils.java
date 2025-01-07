package com.serenegiant.utils;
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

import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class BundleUtils {
	private static final boolean DEBUG = false;  // set false on production
	private static final String TAG = BundleUtils.class.getSimpleName();

	private BundleUtils() {
		// インスタンス化をエラーとするためにデフォルトコンストラクタをprivateに
	}

	@NonNull
	public static String toString(@Nullable final Bundle data) {
		if (data == null) {
			return "Bundle{null}";
		} else {
			final StringBuilder sb = new StringBuilder("Bundle{");
			final Set<String> keys = data.keySet();
			final int n = keys.size();
			int i = 0;
			for (final String key: keys) {
				final Object value = data.get(key);
				if (value.getClass().isArray()) {
					final Class<?> arrayClazz = ArrayUtils.getArrayClass(value);
					if (String.class.equals(arrayClazz)) {
						final String s = Arrays.toString((String[]) value).replace(" ", "");
						sb.append(key).append("=").append(s);
					} else if (CharSequence.class.equals(arrayClazz)) {
						final String s = Arrays.toString((CharSequence[]) value).replace(" ", "");
						sb.append(key).append("=").append(s);
					} else if (char.class.equals(arrayClazz)) {
						final String s = Arrays.toString((char[]) value).replace(" ", "");
						sb.append(key).append("=").append(s);
					} else if (Character.class.equals(arrayClazz)) {
						final String s = Arrays.toString((Character[]) value).replace(" ", "");
						sb.append(key).append("=").append(s);
					} else if (byte.class.equals(arrayClazz)) {
						final String s = Arrays.toString((byte[]) value).replace(" ", "");
						sb.append(key).append("=").append(s);
					} else if (Byte.class.equals(arrayClazz)) {
						final String s = Arrays.toString((Byte[]) value).replace(" ", "");
						sb.append(key).append("=").append(s);
					} else if (short.class.equals(arrayClazz)) {
						final String s = Arrays.toString((short[]) value).replace(" ", "");
						sb.append(key).append("=").append(s);
					} else if (Short.class.equals(arrayClazz)) {
						final String s = Arrays.toString((Short[]) value).replace(" ", "");
						sb.append(key).append("=").append(s);
					} else if (int.class.equals(arrayClazz)) {
						final String s = Arrays.toString((int[]) value).replace(" ", "");
						sb.append(key).append("=").append(s);
					} else if (Integer.class.equals(arrayClazz)) {
						final String s = Arrays.toString((Integer[]) value).replace(" ", "");
						sb.append(key).append("=").append(s);
					} else if (long.class.equals(arrayClazz)) {
						final String s = Arrays.toString((long[]) value).replace(" ", "");
						sb.append(key).append("=").append(s);
					} else if (Long.class.equals(arrayClazz)) {
						final String s = Arrays.toString((Long[]) value).replace(" ", "");
						sb.append(key).append("=").append(s);
					} else if (float.class.equals(arrayClazz)) {
						final String s = Arrays.toString((float[]) value).replace(" ", "");
						sb.append(key).append("=").append(s);
					} else if (Float.class.equals(arrayClazz)) {
						final String s = Arrays.toString((Float[]) value).replace(" ", "");
						sb.append(key).append("=").append(s);
					} else if (double.class.equals(arrayClazz)) {
						final String s = Arrays.toString((double[]) value).replace(" ", "");
						sb.append(key).append("=").append(s);
					} else if (Double.class.equals(arrayClazz)) {
						final String s = Arrays.toString((Double[]) value).replace(" ", "");
						sb.append(key).append("=").append(s);
					} else {
						@NonNull
						final Class<?>[] intfs = arrayClazz.getInterfaces();
						if (DEBUG) Log.v(TAG, "toString:arrayClazz=" + arrayClazz + ",intfs=" + Arrays.toString(intfs));
						for (final Class<?> intf: intfs) {
							if (intf == Parcelable.class) {
								final String s = Arrays.toString((Parcelable[]) value).replace(" ", "");
								sb.append(key).append("=").append(s);
								break;
							}
							if (intf == Serializable.class) {
								final String s = Arrays.toString((Serializable[]) value).replace(" ", "");
								sb.append(key).append("=").append(s);
								break;
							}
						}
					}
				} else {
					sb.append(key).append("=").append(value);
				}
				i++;
				if (i < n) {
					sb.append(",");
				}
			}
			sb.append("}");
			return sb.toString();
		}
	}

}
