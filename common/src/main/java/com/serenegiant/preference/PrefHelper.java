package com.serenegiant.preference;

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
import android.content.SharedPreferences;
import android.util.Log;

import com.serenegiant.utils.ObjectHelper;

import java.util.Map;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Created by saki on 2016/11/05.
 * 例外生成なし＆デフォルト値付きで値を取得するためのヘルパークラス
 */
public class PrefHelper {
	private PrefHelper() {
		// インスタンス化をエラーにするためにデフォルトコンストラクタをprivateに
	}

	/**
	 * deprecatedになったPreferenceManagerとその#getDefaultSharedPreferencesと等価な
	 * 共有プレファレンス取得用ヘルパーメソッド
	 * 共有プレファレンス名がパッケージ名 + "_preferences"の共有プレファレンスが返す
	 * @param context
	 * @return
	 */
	@NonNull
	public static SharedPreferences getDefaultSharedPreferences(@NonNull final Context context) {
		return context.getSharedPreferences(getDefaultSharedPreferencesName(context),
			getDefaultSharedPreferencesMode());
	}

	public static String getDefaultSharedPreferencesName(Context context) {
		return context.getPackageName() + "_preferences";
	}

	private static int getDefaultSharedPreferencesMode() {
		return Context.MODE_PRIVATE;
	}

	public static short get(@Nullable final SharedPreferences pref,
		final String key, final short defaultValue) {

		short result = defaultValue;
		if ((pref != null) && pref.contains(key)) {
			try {
				result = (short)pref.getInt(key, defaultValue);
			} catch (final Exception e) {
				result = ObjectHelper.asShort(
					getObject(pref, key, defaultValue), defaultValue);
			}
		}
		return result;
	}

	public static int get(@Nullable final SharedPreferences pref,
		final String key, final int defaultValue) {

		int result = defaultValue;
		if ((pref != null) && pref.contains(key)) {
			try {
				result = pref.getInt(key, defaultValue);
			} catch (final Exception e) {
				result = ObjectHelper.asInt(
					getObject(pref, key, defaultValue), defaultValue);
			}
		}
		return result;
	}

	public static long get(@Nullable final SharedPreferences pref,
		final String key, final long defaultValue) {

		long result = defaultValue;
		if ((pref != null) && pref.contains(key)) {
			try {
				result = pref.getLong(key, defaultValue);
			} catch (final Exception e) {
				result = ObjectHelper.asLong(
					getObject(pref, key, defaultValue), defaultValue);
			}
		}
		return result;
	}

	public static float get(@Nullable final SharedPreferences pref,
		final String key, final float defaultValue) {

		float result = defaultValue;
		if ((pref != null) && pref.contains(key)) {
			try {
				result = pref.getFloat(key, defaultValue);
			} catch (final Exception e) {
				result = ObjectHelper.asFloat(
					getObject(pref, key, defaultValue), defaultValue);
			}
		}
		return result;
	}

	public static double get(@Nullable final SharedPreferences pref,
		final String key, final double defaultValue) {

		double result = defaultValue;
		if ((pref != null) && pref.contains(key)) {
			try {
				result = Double.parseDouble(pref.getString(key, Double.toString(defaultValue)));
			} catch (final Exception e) {
				result = ObjectHelper.asDouble(
					getObject(pref, key, defaultValue), defaultValue);
			}
		}
		return result;
	}

	public static boolean get(@Nullable final SharedPreferences pref,
		final String key, final boolean defaultValue) {

		boolean result = defaultValue;
		if ((pref != null) && pref.contains(key)) {
			try {
				result = pref.getBoolean(key, defaultValue);
			} catch (final Exception e) {
				result = ObjectHelper.asBoolean(
					get(pref, key, defaultValue), defaultValue);
			}
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	public <T> T get(@Nullable final SharedPreferences pref,
		final String key, final T defaultValue) {

		final Class<?> clazz = defaultValue.getClass();
		final Object result = getObject(pref, key, defaultValue);
		if (clazz.isInstance(result)) {
			return (T)result;
		} else {
			return defaultValue;
		}
	}

	public static final Object getObject(@Nullable final SharedPreferences pref,
		final String key) {

		return getObject(pref, key, null);
	}

	public static final Object getObject(@Nullable final SharedPreferences pref,
		final String key, final Object defaultValue) {

		Object result = defaultValue;
		if ((pref != null) && pref.contains(key)) {
			final Map<String, ?> all = pref.getAll();
			result = all.get(key);
		}
		return result;
	}

//--------------------------------------------------------------------------------
	/**
	 * 共有プレファレンスの中身をコピー
	 * dstにsrcと同じキーの値があっても強制的に上書きする
	 * @param src
	 * @param dst
	 */
	@SuppressWarnings("unchecked")
	public static void copy(
		@NonNull final SharedPreferences src,
		@NonNull final SharedPreferences dst) {

		final Map<String, ?> values = src.getAll();
		if ((values != null) && !values.isEmpty()) {
			final SharedPreferences.Editor editor = dst.edit();
			try {
				for (final Map.Entry<String, ?> entry: values.entrySet()) {
					final String key = entry.getKey();
					final Object value = entry.getValue();
					if (value instanceof String) {
						editor.putString(key, (String)value);
					} else if (value instanceof Set) {
						// SharedPreferencesに入るSetはSet<String>のみ
						editor.putStringSet(key, (Set<String>)value);
					} else if (value instanceof Integer) {
						editor.putInt(key, (int)value);
					} else if (value instanceof Long) {
						editor.putLong(key, (long)value);
					} else if (value instanceof Float) {
						editor.putFloat(key, (Float)value);
					} else if (value instanceof Boolean) {
						editor.putBoolean(key, (Boolean)value);
					}
				}
			} finally {
				editor.apply();
			}
		}
	}

	/**
	 * 共有プレファレンスをマージする時にキー値ペアのコピーを行うかどうかを判定するための
	 * コールバックインターフェース
	 */
	public interface SharedPreferencesMergeCallback {
		/**
		 * コピーを行うかどうかを判定するコールバックメソッド
		 * @param key
		 * @param contains コピー先にキー含まれているかどうか
		 * @param srcValue コピー元の値
		 * @param dstValue コピー先の値(コピー先にキーが含まれていないときはnull)
		 * @return trueを返すとならコピー元の値を使う(=dstを上書きする)、
		 *         falseを返すとコピー先の値を使う(=dstを上書きしない)
		 */
		public boolean onMerge(
			@NonNull final String key, final boolean contains,
			final Object srcValue, final Object dstValue);
	}

	/**
	 * 共有プレファレンスの中身をマージ
	 * 同じキーが存在する場合には上書きしない
	 * @param src
	 * @param dst
	 */
	public static void merge(
		@NonNull final SharedPreferences src,
		@NonNull final SharedPreferences dst) {
		merge(src, dst, null);
	}

	/**
	 * 共有プレファレンスの中身をマージ
	 * @param src
	 * @param dst
	 * @param callback nullなら同じキーが存在するときに上書きしない
	 */
	@SuppressWarnings("unchecked")
	public static void merge(
		@NonNull final SharedPreferences src,
		@NonNull final SharedPreferences dst,
		@Nullable SharedPreferencesMergeCallback callback) {

		final Map<String, ?> srcValues = src.getAll();
		if ((srcValues != null) && !srcValues.isEmpty()) {
			final SharedPreferences.Editor editor = dst.edit();
			try {
				for (final Map.Entry<String, ?> srcEntry: srcValues.entrySet()) {
					final String key = srcEntry.getKey();
					final Object srcValue = srcEntry.getValue();
					final Object dstValue = getObject(dst, key, null);
					final boolean contains = dst.contains(key);
					// callbackがnull以外でtrueを返すと上書きする
					boolean needCopy = (!contains && (callback == null))
						|| ((callback != null) && callback.onMerge(key, contains, srcValue, dstValue));
					if (needCopy) {
						if (srcValue instanceof String) {
							editor.putString(key, (String)srcValue);
						} else if (srcValue instanceof Set) {
							// SharedPreferencesに入るSetはSet<String>のみ
							editor.putStringSet(key, (Set<String>)srcValue);
						} else if (srcValue instanceof Integer) {
							editor.putInt(key, (int)srcValue);
						} else if (srcValue instanceof Long) {
							editor.putLong(key, (long)srcValue);
						} else if (srcValue instanceof Float) {
							editor.putFloat(key, (Float)srcValue);
						} else if (srcValue instanceof Boolean) {
							editor.putBoolean(key, (Boolean)srcValue);
						}
					}
				}
			} finally {
				editor.apply();
			}
		}
	}

	/**
	 * fromで指定した共有プレファレンスからkeysSrcで指定した共有プレファレンスに含まれるキーを全て取り除く
	 * @param from
	 * @param keysSrc
	 * @return
	 */
	public static boolean removeAll(
		@NonNull final SharedPreferences from,
		@NonNull final SharedPreferences keysSrc) {
		final String[] keys = keysSrc.getAll().keySet().toArray(new String[0]);
		return removeAll(from, keys);
	}

	/**
	 * fromで指定した共有プレファレンスからkeysで指定したキーを全て取り除く
	 * @param from
	 * @param keys
	 * @return
	 */
	public static boolean removeAll(
		@NonNull final SharedPreferences from,
		final String[] keys) {

		boolean removed = false;
		if ((keys != null) && (keys.length > 0)) {
			final SharedPreferences.Editor editor = from.edit();
			try {
				for (final String key: keys) {
					if (from.contains(key)) {
						editor.remove(key);
						removed = true;
					}
				}
			} finally {
				editor.apply();
			}
		}

		return removed;
	}

	/**
	 * 共有プレファレンスのkey-valueペアをLog.dでlogCatへ出力
	 * @param tag
	 * @param preferences
	 */
	public static void dump(
		@NonNull final String tag,
		@NonNull final SharedPreferences preferences) {

		final Map<String, ?> values = preferences.getAll();
		if ((values != null) && !values.isEmpty()) {
			for (final Map.Entry<String, ?> entry: values.entrySet()) {
				Log.d(tag, "dump:" + entry.getKey() + "=" + entry.getValue());
			}
		} else {
			Log.d(tag, "dump:empty");
		}
	}
}
