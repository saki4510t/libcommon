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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * JSONから例外生成なし＆デフォルト値付きて値取得するためのヘルパークラス
 */
public class JSONHelper {
	private static final boolean DEBUG = false;	// FIXME set false on production
	private static final String TAG = JSONHelper.class.getSimpleName();

	private JSONHelper() {
		// インスタンス化をエラーにするためにデフォルトコンストラクタをprivateに
	}

	/**
	 * 例外生成せずにデフォルト値付きでbooleanとして値を取得する
	 * JSONObject#optBoolranと違ってtrue/false以外の場合でも
	 * ObjectHelper#asBooleanを使って値の変換を試みる
	 * (0に変換されるならfalse、0以外の数値に変換されるならtrue)
	 * @param payload
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	public static boolean optBoolean(@NonNull final JSONObject payload, final String key, final boolean defaultValue) {
		boolean result = defaultValue;
		if (payload.has(key)) {
			try {
				result = payload.getBoolean(key);
			} catch (final Exception e) {
				try {
					result = ObjectHelper.asBoolean(payload.get(key), defaultValue);
				} catch (final Exception e1) {
					// ignore #hasで確認しているので#getでは例外生成しないはず
				}
			}
		}

		return result;
	}

	/**
	 * 例外生成せずにデフォルト値付きでbooleanとして値を取得する
	 * JSONObject#optBoolranと違ってtrue/false以外の場合でも
	 * ObjectHelper#asBooleanを使って値の変換を試みる
	 * (0に変換されるならfalse、0以外の数値に変換されるならtrue)
	 * @param payload
	 * @param index
	 * @param defaultValue
	 * @return
	 */
	public static boolean optBoolean(@NonNull final JSONArray payload, final int index, final boolean defaultValue) {
		boolean result = defaultValue;
		if (payload.length() > index) {
			try {
				result = payload.getBoolean(index);
			} catch (final Exception e) {
				try {
					result = ObjectHelper.asBoolean(payload.get(index), defaultValue);
				} catch (final Exception e1) {
					// ignore #hasで確認しているので#getでは例外生成しないはず
				}
			}
		}

		return result;
	}

	/**
	 * 例外生成せずにデフォルト値付きでintとして値を取得する
	 * JSONObject#optIntと違って数値以外の場合でも
	 * ObjectHelper#asIntを使って値の変換を試みる
	 * @param payload
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	public static int optInt(@NonNull final JSONObject payload, final String key, final int defaultValue) {
		int result = defaultValue;
		if (payload.has(key)) {
			try {
				result = payload.getInt(key);
			} catch (final JSONException e) {
				try {
					result = ObjectHelper.asInt(payload.get(key), defaultValue);
				} catch (final Exception e1) {
					// ignore #hasで確認しているので#getでは例外生成しないはず
				}
			}
		}

		return result;
	}

	/**
	 * 例外生成せずにデフォルト値付きでintとして値を取得する
	 * JSONObject#optIntと違って数値以外の場合でも
	 * ObjectHelper#asIntを使って値の変換を試みる
	 * @param payload
	 * @param index
	 * @param defaultValue
	 * @return
	 */
	public static int optInt(@NonNull final JSONArray payload, final int index, final int defaultValue) {
		int result = defaultValue;
		if (payload.length() > index) {
			try {
				result = payload.getInt(index);
			} catch (final JSONException e) {
				try {
					result = ObjectHelper.asInt(payload.get(index), defaultValue);
				} catch (final Exception e1) {
					// ignore #hasで確認しているので#getでは例外生成しないはず
				}
			}
		}
		return result;
	}

	/**
	 * 例外生成せずにデフォルト値付きでlongとして値を取得する
	 * JSONObject#optLongと違って数値以外の場合でも
	 * ObjectHelper#asLongを使って値の変換を試みる
	 * @param payload
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	public static long optLong(@NonNull final JSONObject payload, final String key, final long defaultValue) {
		long result = defaultValue;
		if (payload.has(key)) {
			try {
				result = payload.getLong(key);
			} catch (final JSONException e) {
				try {
					result = ObjectHelper.asLong(payload.get(key), defaultValue);
				} catch (final Exception e1) {
					// ignore #hasで確認しているので#getでは例外生成しないはず
				}
			}
		}
		return result;
	}

	/**
	 * 例外生成せずにデフォルト値付きでlongとして値を取得する
	 * JSONObject#optLongと違って数値以外の場合でも
	 * ObjectHelper#asLongを使って値の変換を試みる
	 * @param payload
	 * @param index
	 * @param defaultValue
	 * @return
	 */
	public static long optLong(@NonNull final JSONArray payload, final int index, final long defaultValue) {
		long result = defaultValue;
		if (payload.length() > index) {
			try {
				result = payload.getLong(index);
			} catch (final JSONException e) {
				try {
					result = ObjectHelper.asLong(payload.get(index), defaultValue);
				} catch (final Exception e1) {
					// ignore #hasで確認しているので#getでは例外生成しないはず
				}
			}
		}

		return result;
	}

	/**
	 * 例外生成せずにデフォルト値付きでdoubleとして値を取得する
	 * JSONObject#optDoubleと違って数値以外の場合でも
	 * ObjectHelper#asDoubleを使って値の変換を試みる
	 * @param payload
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	public static double optDouble(@NonNull final JSONObject payload, final String key, final double defaultValue) {
		double result = defaultValue;
		if (payload.has(key)) {
			try {
				result = payload.getDouble(key);
			} catch (final JSONException e) {
				try {
					result = ObjectHelper.asDouble(payload.get(key), defaultValue);
				} catch (final Exception e1) {
					// ignore #hasで確認しているので#getでは例外生成しないはず
				}
			}
		}

		return result;
	}

	/**
	 * 例外生成せずにデフォルト値付きでdoubleとして値を取得する
	 * JSONObject#optDoubleと違って数値以外の場合でも
	 * ObjectHelper#asDoubleを使って値の変換を試みる
	 * @param payload
	 * @param index
	 * @param defaultValue
	 * @return
	 */
	public static double optDouble(@NonNull final JSONArray payload, final int index, final double defaultValue) {
		double result = defaultValue;
		if (payload.length() > index) {
			try {
				result = payload.getDouble(index);
			} catch (final JSONException e) {
				try {
					result = ObjectHelper.asDouble(payload.get(index), defaultValue);
				} catch (final Exception e1) {
					// ignore #hasで確認しているので#getでは例外生成しないはず
				}
			}
		}

		return result;
	}

	/**
	 * 指定したJSONObjectの指定したキーに対応する値をboolean配列として読み取る。
	 * 指定したキーに対応する値が存在しない場合は長さ0の配列を返す。
	 * 指定したキーに対応する値がJSONArrayの場合、含まれる各要素の値をObjectHelper#asBooleanでbooleanに変換して配列として返す。
	 * 指定したキーに対応する値がJSONObjectの場合、含まれる各要素の値をObjectHelper#asBooleanでbooleanに変換して配列として返す。
	 * 指定したキーに対応する値がそれ以外の型(intなNumber、Stringなど)の場合、ObjectHelper#asBooleanでbooleanに変換して要素数1の配列として返す。
	 * @param payload
	 * @param key
	 * @return
	 * @throws JSONException
	 */
	@NonNull
	public static boolean[] getBooleanArray(@NonNull final JSONObject payload, final String key) throws JSONException {
		boolean[] result;

		if (payload.has(key)) {
			final Object value = payload.opt(key);
			if (value instanceof JSONArray) {
				final JSONArray array = (JSONArray) value;
				final int n = array.length();
				result = new boolean[n];
				for (int i = 0; i < n; i++) {
					result[i] = ObjectHelper.asBoolean(array.get(i), false);
				}
			} else if (value instanceof JSONObject) {
				final JSONObject v = (JSONObject) value;
				final int n = v.length();
				result = new boolean[n];
				int i = 0;
				for (final Iterator<String> it = v.keys(); it.hasNext(); ) {
					final String k = it.next();
					result[i] = ObjectHelper.asBoolean(v.get(k), false);
				}
			} else {
				result = new boolean[1];
				result[0] = ObjectHelper.asBoolean(value, false);
			}
		} else {
			result = new boolean[0];
		}

		return result;
	}

	/**
	 * 指定したJSONObjectの指定したキーに対応する値をbyte配列として読み取る。
	 * 指定したキーに対応する値が存在しない場合は長さ0の配列を返す。
	 * 指定したキーに対応する値がJSONArrayの場合、含まれる各要素の値をObjectHelper#asByteでbyteに変換して配列として返す。
	 * 指定したキーに対応する値がJSONObjectの場合、含まれる各要素の値をObjectHelper#asByteでbyteに変換して配列として返す。
	 * 指定したキーに対応する値がそれ以外の型(intなNumber、Stringなど)の場合、ObjectHelper#asByteでbyteに変換して要素数1の配列として返す。
	 * @param payload
	 * @param key
	 * @return
	 * @throws JSONException
	 */
	@NonNull
	public static byte[] getByteArray(@NonNull final JSONObject payload, final String key) throws JSONException {
		byte[] result;

		if (payload.has(key)) {
			final Object value = payload.opt(key);
			if (value instanceof JSONArray) {
				final JSONArray array = (JSONArray) value;
				final int n = array.length();
				result = new byte[n];
				for (int i = 0; i < n; i++) {
					result[i] = ObjectHelper.asByte(array.get(i), (byte) 0);
				}
			} else if (value instanceof JSONObject) {
				final JSONObject v = (JSONObject) value;
				final int n = v.length();
				result = new byte[n];
				int i = 0;
				for (final Iterator<String> it = v.keys(); it.hasNext(); ) {
					final String k = it.next();
					result[i] = ObjectHelper.asByte(v.get(k), (byte) 0);
				}
			} else {
				result = new byte[1];
				result[0] = ObjectHelper.asByte(value, (byte) 0);
			}
		} else {
			result = new byte[0];
		}

		return result;
	}

	/**
	 * 指定したJSONObjectの指定したキーに対応する値をshort配列として読み取る。
	 * 指定したキーに対応する値が存在しない場合は長さ0の配列を返す。
	 * 指定したキーに対応する値がJSONArrayの場合、含まれる各要素の値をObjectHelper#asShortでshortに変換して配列として返す。
	 * 指定したキーに対応する値がJSONObjectの場合、含まれる各要素の値をObjectHelper#asShortでshortに変換して配列として返す。
	 * 指定したキーに対応する値がそれ以外の型(intなNumber、Stringなど)の場合、ObjectHelper#asShortでshortに変換して要素数1の配列として返す。
	 * @param payload
	 * @param key
	 * @return
	 * @throws JSONException
	 */
	@NonNull
	public static short[] getShortArray(@NonNull final JSONObject payload, final String key) throws JSONException {
		short[] result;

		if (payload.has(key)) {
			final Object value = payload.opt(key);
			if (value instanceof JSONArray) {
				final JSONArray array = (JSONArray) value;
				final int n = array.length();
				result = new short[n];
				for (int i = 0; i < n; i++) {
					result[i] = ObjectHelper.asShort(array.get(i), (short) 0);
				}
			} else if (value instanceof JSONObject) {
				final JSONObject v = (JSONObject) value;
				final int n = v.length();
				result = new short[n];
				int i = 0;
				for (final Iterator<String> it = v.keys(); it.hasNext(); ) {
					final String k = it.next();
					result[i] = ObjectHelper.asShort(v.get(k), (short) 0);
				}
			} else {
				result = new short[1];
				result[0] = ObjectHelper.asShort(value, (short) 0);
			}
		} else {
			result = new short[0];
		}

		return result;
	}

	/**
	 * 指定したJSONObjectの指定したキーに対応する値をint配列として読み取る。
	 * 指定したキーに対応する値が存在しない場合は長さ0の配列を返す。
	 * 指定したキーに対応する値がJSONArrayの場合、含まれる各要素の値をObjectHelper#asIntでintに変換して配列として返す。
	 * 指定したキーに対応する値がJSONObjectの場合、含まれる各要素の値をObjectHelper#asIntでintに変換して配列として返す。
	 * 指定したキーに対応する値がそれ以外の型(intなNumber、Stringなど)の場合、ObjectHelper#asIntでintに変換して要素数1の配列として返す。
	 * @param payload
	 * @param key
	 * @return
	 * @throws JSONException
	 */
	@NonNull
	public static int[] getIntArray(@NonNull final JSONObject payload, final String key) throws JSONException {
		int[] result;

		if (payload.has(key)) {
			final Object value = payload.opt(key);
			if (value instanceof JSONArray) {
				final JSONArray array = (JSONArray) value;
				final int n = array.length();
				result = new int[n];
				for (int i = 0; i < n; i++) {
					result[i] = ObjectHelper.asInt(array.get(i), 0);
				}
			} else if (value instanceof JSONObject) {
				final JSONObject v = (JSONObject) value;
				final int n = v.length();
				result = new int[n];
				int i = 0;
				for (final Iterator<String> it = v.keys(); it.hasNext(); ) {
					final String k = it.next();
					result[i] = ObjectHelper.asInt(v.get(k), 0);
				}
			} else {
				result = new int[1];
				result[0] = ObjectHelper.asInt(value, 0);
			}
		} else {
			result = new int[0];
		}

		return result;
	}

	/**
	 * 指定したJSONObjectの指定したキーに対応する値をlong配列として読み取る。
	 * 指定したキーに対応する値が存在しない場合は長さ0の配列を返す。
	 * 指定したキーに対応する値がJSONArrayの場合、含まれる各要素の値をObjectHelper#asLongでlongに変換して配列として返す。
	 * 指定したキーに対応する値がJSONObjectの場合、含まれる各要素の値をObjectHelper#asLongでlongに変換して配列として返す。
	 * 指定したキーに対応する値がそれ以外の型(intなNumber、Stringなど)の場合、ObjectHelper#asLongでlongに変換して要素数1の配列として返す。
	 * @param payload
	 * @param key
	 * @return
	 * @throws JSONException
	 */
	@NonNull
	public static long[] getLongArray(@NonNull final JSONObject payload, final String key) throws JSONException {
		long[] result;

		if (payload.has(key)) {
			final Object value = payload.opt(key);
			if (value instanceof JSONArray) {
				final JSONArray array = (JSONArray) value;
				final int n = array.length();
				result = new long[n];
				for (int i = 0; i < n; i++) {
					result[i] = ObjectHelper.asLong(array.get(i), 0);
				}
			} else if (value instanceof JSONObject) {
				final JSONObject v = (JSONObject) value;
				final int n = v.length();
				result = new long[n];
				int i = 0;
				for (final Iterator<String> it = v.keys(); it.hasNext(); ) {
					final String k = it.next();
					result[i] = ObjectHelper.asLong(v.get(k), 0);
				}
			} else {
				result = new long[1];
				result[0] = ObjectHelper.asLong(value, 0);
			}
		} else {
			result = new long[0];
		}

		return result;
	}

	/**
	 * 指定したJSONObjectの指定したキーに対応する値をfloat配列として読み取る。
	 * 指定したキーに対応する値が存在しない場合は長さ0の配列を返す。
	 * 指定したキーに対応する値がJSONArrayの場合、含まれる各要素の値をObjectHelper#asFloatでfloatに変換して配列として返す。
	 * 指定したキーに対応する値がJSONObjectの場合、含まれる各要素の値をObjectHelper#asFloatでfloatに変換して配列として返す。
	 * 指定したキーに対応する値がそれ以外の型(intなNumber、Stringなど)の場合、ObjectHelper#asFloatでfloatに変換して要素数1の配列として返す。
	 * @param payload
	 * @param key
	 * @return
	 * @throws JSONException
	 */
	@NonNull
	public static float[] getFloatArray(@NonNull final JSONObject payload, final String key) throws JSONException {
		float[] result;

		if (payload.has(key)) {
			final Object value = payload.opt(key);
			if (value instanceof JSONArray) {
				final JSONArray array = (JSONArray) value;
				final int n = array.length();
				result = new float[n];
				for (int i = 0; i < n; i++) {
					result[i] = ObjectHelper.asFloat(array.get(i), 0);
				}
			} else if (value instanceof JSONObject) {
				final JSONObject v = (JSONObject) value;
				final int n = v.length();
				result = new float[n];
				int i = 0;
				for (final Iterator<String> it = v.keys(); it.hasNext(); ) {
					final String k = it.next();
					result[i] = ObjectHelper.asFloat(v.get(k), 0);
				}
			} else {
				result = new float[1];
				result[0] = ObjectHelper.asFloat(value, 0);
			}
		} else {
			result = new float[0];
		}

		return result;
	}

	/**
	 * 指定したJSONObjectの指定したキーに対応する値をdouble配列として読み取る。
	 * 指定したキーに対応する値が存在しない場合は長さ0の配列を返す。
	 * 指定したキーに対応する値がJSONArrayの場合、含まれる各要素の値をObjectHelper#asDoubleでdoubleに変換して配列として返す。
	 * 指定したキーに対応する値がJSONObjectの場合、含まれる各要素の値をObjectHelper#asDoubleでdoubleに変換して配列として返す。
	 * 指定したキーに対応する値がそれ以外の型(intなNumber、Stringなど)の場合、ObjectHelper#asDoubleでdoubleに変換して要素数1の配列として返す。
	 * @param payload
	 * @param key
	 * @return
	 * @throws JSONException
	 */
	@NonNull
	public static double[] getDoubleArray(@NonNull final JSONObject payload, final String key) throws JSONException {
		double[] result;

		if (payload.has(key)) {
			final Object value = payload.opt(key);
			if (value instanceof JSONArray) {
				final JSONArray array = (JSONArray) value;
				final int n = array.length();
				result = new double[n];
				for (int i = 0; i < n; i++) {
					result[i] = ObjectHelper.asDouble(array.get(i), 0);
				}
			} else if (value instanceof JSONObject) {
				final JSONObject v = (JSONObject) value;
				final int n = v.length();
				result = new double[n];
				int i = 0;
				for (final Iterator<String> it = v.keys(); it.hasNext(); ) {
					final String k = it.next();
					result[i] = ObjectHelper.asDouble(v.get(k), 0);
				}
			} else {
				result = new double[1];
				result[0] = ObjectHelper.asDouble(value, 0);
			}
		} else {
			result = new double[0];
		}

		return result;
	}

	/**
	 * 指定したJSONObjectの指定したキーに対応する値をString配列として読み取る。
	 * 指定したキーに対応する値が存在しない場合は長さ0の配列を返す。
	 * 指定したキーに対応する値がJSONArrayの場合、含まれる各要素の値を#toStringでStringへ変換して配列として返す。
	 * 指定したキーに対応する値がJSONObjectの場合、含まれる各要素の値を#toStringでStringへ変換して配列として返す。
	 * 指定したキーに対応する値がそれ以外の型(intなNumber、Stringなど)の場合、#toStringでStringへ変換して要素数1の配列として返す。
	 * @param payload
	 * @param key
	 * @return
	 * @throws JSONException
	 */
	@NonNull
	public static String[] getStringArray(@NonNull final JSONObject payload, final String key) throws JSONException {
		String[] result;

		if (payload.has(key)) {
			final Object value = payload.opt(key);
			if (value instanceof JSONArray) {
				final JSONArray array = (JSONArray) value;
				final int n = array.length();
				result = new String[n];
				for (int i = 0; i < n; i++) {
					result[i] = array.get(i).toString();
				}
			} else if (value instanceof JSONObject) {
				final JSONObject v = (JSONObject) value;
				final int n = v.length();
				result = new String[n];
				int i = 0;
				for (final Iterator<String> it = v.keys(); it.hasNext(); ) {
					final String k = it.next();
					result[i] = v.get(k).toString();
				}
			} else {
				result = new String[1];
				result[0] = value.toString();
			}
		} else {
			result = new String[0];
		}

		return result;
	}

	/**
	 * JSONArrayをObject[]へ変換する。
	 * 指定したJSONArrayがnullまたは値を含まない場合は長さ0の配列を返す
	 * @param jsonArray
	 * @return
	 * @throws JSONException
	 */
	@NonNull
	public static Object[] toArray(@Nullable final JSONArray jsonArray) throws JSONException {

		Object[] result;

		final int sz = jsonArray != null  ?jsonArray.length() : 0;
		if (sz > 0) {
			result = new Object[sz];
			for (int i = 0; i < sz; i++) {
				result[i] = jsonArray.get(i);
			}
		} else {
			result = new Object[0];
		}

		return result;
	}

	/**
	 * JSONArrayを指定した型の配列へ代入する
	 * 内部で型変換はするもののtypedArrayで指定した型と互換性のない値を
	 * 含む場合には意図しない値になることがあるので注意！
	 * @param jsonArray
	 * @param typedArray
	 * @return
	 * @param <T>
	 * @throws JSONException
	 */
	@SuppressWarnings("unchecked")
	@NonNull
	public static <T> T[] toArray(
		@Nullable final JSONArray jsonArray,
		@NonNull final T[] typedArray) throws JSONException {

		T[] result;

		final Class<?> arrayClass = ArrayUtils.getArrayClass(typedArray);
		final int sz = jsonArray != null  ?jsonArray.length() : 0;
		if (sz > 0) {
			// new T[sz]は文法エラーになるのでArrays.copyOfを使って指定した長さのT[]を生成
			result = (T[]) Arrays.copyOf(typedArray, sz, typedArray.getClass());
			for (int i = 0; i < sz; i++) {
				// XXX JSONArrayへlong/Long/double/Doubleを入れてもJSON文字列を経由してしまうと
				//     型が混在してしまってJSONArray$getの結果を直接配列へ代入できず例外生成することが
				//     あるのでObjectHelper#asで強制的に型変換
				result[i] = (T)ObjectHelper.as(jsonArray.get(i), arrayClass);
			}
		} else {
			// new T[0]は文法エラーになるのでCollections.emptyList().toArrayでT[0]を生成
			result = Collections.emptyList().toArray(typedArray);
		}

		return result;
	}
}
