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

import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ClassUtils {
	private static final boolean DEBUG = false;	// XXX set false on production
	private static final String TAG = ClassUtils.class.getSimpleName();

	private ClassUtils() {
		// インスタンス化をエラーとするためにデフォルトコンストラクタをprivateに
	}

	/**
	 * プリミティブのボクシングクラスとプリミティブクラスのマッピング
	 */
	private static final Map<Class<?>, Class<?>> sBoxingClassToPrimitiveClassMap = new HashMap<>();
	static {
		sBoxingClassToPrimitiveClassMap.put(Boolean.class, Boolean.TYPE);
		sBoxingClassToPrimitiveClassMap.put(Character.class, Character.TYPE);
		sBoxingClassToPrimitiveClassMap.put(Byte.class, Byte.TYPE);
		sBoxingClassToPrimitiveClassMap.put(Short.class, Short.TYPE);
		sBoxingClassToPrimitiveClassMap.put(Integer.class, Integer.TYPE);
		sBoxingClassToPrimitiveClassMap.put(Long.class, Long.TYPE);
		sBoxingClassToPrimitiveClassMap.put(Float.class, Float.TYPE);
		sBoxingClassToPrimitiveClassMap.put(Double.class, Double.TYPE);
		sBoxingClassToPrimitiveClassMap.put(Void.class, Void.TYPE);
		sBoxingClassToPrimitiveClassMap.put(Boolean.TYPE, Boolean.TYPE);
		sBoxingClassToPrimitiveClassMap.put(Character.TYPE, Character.TYPE);
		sBoxingClassToPrimitiveClassMap.put(Byte.TYPE, Byte.TYPE);
		sBoxingClassToPrimitiveClassMap.put(Short.TYPE, Short.TYPE);
		sBoxingClassToPrimitiveClassMap.put(Integer.TYPE, Integer.TYPE);
		sBoxingClassToPrimitiveClassMap.put(Long.TYPE, Long.TYPE);
		sBoxingClassToPrimitiveClassMap.put(Float.TYPE, Float.TYPE);
		sBoxingClassToPrimitiveClassMap.put(Double.TYPE, Double.TYPE);
		sBoxingClassToPrimitiveClassMap.put(Void.TYPE, Void.TYPE);
	}

	/**
	 * プリミティブのボクシングクラスとプリミティブクラスのマッピング
	 */
	private static final Map<Class<?>, Class<?>> sPrimitiveClassToBoxingClassMap = new HashMap<>();
	static {
		sPrimitiveClassToBoxingClassMap.put(Boolean.class, Boolean.class);
		sPrimitiveClassToBoxingClassMap.put(Character.class, Character.class);
		sPrimitiveClassToBoxingClassMap.put(Byte.class, Byte.class);
		sPrimitiveClassToBoxingClassMap.put(Short.class, Short.class);
		sPrimitiveClassToBoxingClassMap.put(Integer.class, Integer.class);
		sPrimitiveClassToBoxingClassMap.put(Long.class, Long.class);
		sPrimitiveClassToBoxingClassMap.put(Float.class, Float.class);
		sPrimitiveClassToBoxingClassMap.put(Double.class, Double.class);
		sPrimitiveClassToBoxingClassMap.put(Void.class, Void.class);
		sPrimitiveClassToBoxingClassMap.put(Boolean.TYPE, Boolean.class);
		sPrimitiveClassToBoxingClassMap.put(Character.TYPE, Character.class);
		sPrimitiveClassToBoxingClassMap.put(Byte.TYPE, Byte.class);
		sPrimitiveClassToBoxingClassMap.put(Short.TYPE, Short.class);
		sPrimitiveClassToBoxingClassMap.put(Integer.TYPE, Integer.class);
		sPrimitiveClassToBoxingClassMap.put(Long.TYPE, Long.class);
		sPrimitiveClassToBoxingClassMap.put(Float.TYPE, Float.class);
		sPrimitiveClassToBoxingClassMap.put(Double.TYPE, Double.class);
		sPrimitiveClassToBoxingClassMap.put(Void.TYPE, Void.class);
	}

	/**
	 * 指定したオブジェクトのクラスを返す
	 * @param object
	 * @return
	 */
	@Nullable
	public static Class<?> getClass(@Nullable final Object object) {
		if (DEBUG) Log.v(TAG, "getClass:" + object);
		if (object != null) {
			return object.getClass();
		} else {
			return null;
		}
	}

	/**
	 * 指定したクラスに対応するプリミティブクラスがあればそれを返す、
	 * 対応するプリミティブクラスがなければそのまま返す
	 * @param clazz
	 * @return
	 */
	@NonNull
	public static Class<?> getPrimitiveClass(@NonNull final Class<?> clazz) {
		Class<?> result = clazz;
		if (sBoxingClassToPrimitiveClassMap.containsKey(result)) {
			result = sBoxingClassToPrimitiveClassMap.get(result);
		}
		if (DEBUG) Log.v(TAG, "getPrimitiveClass:" + clazz + "=>" + result);

		return result;
	}

	/**
	 * 指定したオブジェクトがプリミティブのボクシングクラスなら対応するプリミティブクラスを返す
	 * プリミティブのボクシングクラスとでなければそのままのクラスを返す
	 * @param object
	 * @return
	 */
	@Nullable
	public static Class<?> getPrimitiveClass(@Nullable final Object object) {
		Class<?> result = object != null ? object.getClass() : null;
		if (sBoxingClassToPrimitiveClassMap.containsKey(result)) {
			result = sBoxingClassToPrimitiveClassMap.get(result);
		}
		if (DEBUG) Log.v(TAG, "getPrimitiveClass:" + object + "=>" + result);

		return result;
	}

	/**
	 * 指定したクラスに対応するプリミティブクラスがあればそれを返す、
	 * 対応するプリミティブクラスがなければそのまま返す
	 * @param clazz
	 * @return
	 */
	@NonNull
	public static Class<?> getBoxingClass(@NonNull final Class<?> clazz) {
		Class<?> result = clazz;
		if (sPrimitiveClassToBoxingClassMap.containsKey(result)) {
			result = sPrimitiveClassToBoxingClassMap.get(result);
		}
		if (DEBUG) Log.v(TAG, "getBoxingClass:" + clazz + "=>" + result);

		return result;
	}

	/**
	 * 指定したオブジェクトがプリミティブのボクシングクラスまたはボクシングクラスなら対応するボクシングクラスを返す
	 * プリミティブでもボクシングクラスでも無ければそのままのクラスを返す
	 * @param object
	 * @return
	 */
	@Nullable
	public static Class<?> getBoxingClass(@Nullable final Object object) {
		Class<?> result = object != null ? object.getClass() : null;
		if (sPrimitiveClassToBoxingClassMap.containsKey(result)) {
			result = sPrimitiveClassToBoxingClassMap.get(result);
		}
		if (DEBUG) Log.v(TAG, "getBoxingClass:" + object + "=>" + result);

		return result;
	}

}
