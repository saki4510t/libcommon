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

import android.content.SharedPreferences;
import android.text.TextUtils;

import com.serenegiant.collections.ReentrantReadWriteMap;
import com.serenegiant.utils.UIThreadHelper;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Map/HashMapでメモリー上に値を保持するSharedPreferences実装
 */
public class OnMemorySharedPreferences implements SharedPreferences {
	@NonNull
	private final Map<String, Object> mMap = new ReentrantReadWriteMap<>();
	@NonNull
	private final CopyOnWriteArraySet<OnSharedPreferenceChangeListener> mListeners
		= new CopyOnWriteArraySet<>();

	@Override
	public Map<String, ?> getAll() {
		return Collections.unmodifiableMap(mMap);
	}

	@Nullable
	@Override
	public String getString(final String key, @Nullable final String defValue) {
		final Object value = mMap.containsKey(key) ? mMap.get(key) : defValue;
		if ((value == null) || (value instanceof String)) {
			return (String)value;
		} else {
			throw new ClassCastException();
		}
	}

	@SuppressWarnings("unchecked")
	@Nullable
	@Override
	public Set<String> getStringSet(final String key, @Nullable final Set<String> defValues) {
		final Object value = mMap.containsKey(key) ? mMap.get(key) : defValues;
		if ((value == null) || (value instanceof Set)) {
			return (Set<String>)value;
		} else {
			throw new ClassCastException();
		}
	}

	@Override
	public int getInt(final String key, final int defValue) {
		final Object value = mMap.containsKey(key) ? mMap.get(key) : defValue;
		if (value instanceof Integer) {
			return (int)value;
		} else {
			throw new ClassCastException();
		}
	}

	@Override
	public long getLong(final String key, final long defValue) {
		final Object value = mMap.containsKey(key) ? mMap.get(key) : defValue;
		if (value instanceof Long) {
			return (long)value;
		} else {
			throw new ClassCastException();
		}
	}

	@Override
	public float getFloat(final String key, final float defValue) {
		final Object value = mMap.containsKey(key) ? mMap.get(key) : defValue;
		if (value instanceof Float) {
			return (float)value;
		} else {
			throw new ClassCastException();
		}
	}

	@Override
	public boolean getBoolean(final String key, final boolean defValue) {
		final Object value = mMap.containsKey(key) ? mMap.get(key) : defValue;
		if (value instanceof Boolean) {
			return (boolean)value;
		} else {
			throw new ClassCastException();
		}
	}

	@Override
	public boolean contains(final String key) {
		return mMap.containsKey(key);
	}

	@Override
	public Editor edit() {
		return new OnMemoryEditor(this);
	}

	@Override
	public void registerOnSharedPreferenceChangeListener(final OnSharedPreferenceChangeListener listener) {
		mListeners.add(listener);
	}

	@Override
	public void unregisterOnSharedPreferenceChangeListener(final OnSharedPreferenceChangeListener listener) {
		mListeners.remove(listener);
	}

	/**
	 * 暗号化してSharedPreferencesへ保存するためのEditor実装
	 */
	private static class OnMemoryEditor implements Editor {
		@NonNull
		private final OnMemorySharedPreferences mParent;
		@NonNull
		private final Map<String, Object> mMap;
		@NonNull
		private final Set<OnSharedPreferenceChangeListener> mListeners;
		private final Set<String> mChanged = new HashSet<>();

		public OnMemoryEditor(
			@NonNull final OnMemorySharedPreferences parent) {

			mParent = parent;
			mMap = parent.mMap;
			mListeners = parent.mListeners;
		}

		@Override
		public Editor putString(final String key, @Nullable final String value) {
			mChanged.add(key);
			if (!TextUtils.isEmpty(value)) {
				mMap.put(key, value);
			} else {
				mMap.remove(key);
			}
			return this;
		}

		@Override
		public Editor putStringSet(final String key, @Nullable final Set<String> values) {
			mChanged.add(key);
			mMap.put(key, values);
			return this;
		}

		@Override
		public Editor putInt(final String key, final int value) {
			mChanged.add(key);
			mMap.put(key, value);
			return this;
		}

		@Override
		public Editor putLong(final String key, final long value) {
			mChanged.add(key);
			mMap.put(key, value);
			return this;
		}

		@Override
		public Editor putFloat(final String key, final float value) {
			mChanged.add(key);
			mMap.put(key, value);
			return this;
		}

		@Override
		public Editor putBoolean(final String key, final boolean value) {
			mChanged.add(key);
			mMap.put(key, value);
			return this;
		}

		@Override
		public Editor remove(final String key) {
			mChanged.add(key);
			mMap.remove(key);
			return this;
		}

		@Override
		public Editor clear() {
			mChanged.addAll(mMap.keySet());
			mMap.clear();
			return this;
		}

		@Override
		public boolean commit() {
			for (final String key: mChanged) {
				callOnChanged(key);
			}
			return true;
		}

		@Override
		public void apply() {
			UIThreadHelper.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					for (final String key: mChanged) {
						callOnChanged(key);
					}
				}
			});
		}

		private void callOnChanged(final String key) {
			for (final OnSharedPreferenceChangeListener listener: mListeners) {
				if (listener != null) {
					listener.onSharedPreferenceChanged(mParent, key);
				}
			}
		}
	}
}
