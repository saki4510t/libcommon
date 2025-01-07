package com.serenegiant.collections;

import android.os.Build;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArraySet;

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

/**
 * 読み込みロックと書き込みロックを個別に制御できるようにするためのMap実装
 * @param <K>
 * @param <V>
 */
public class ReentrantReadWriteMap<K, V> implements Map<K, V> {
	@NonNull
	private final ReentrantReadWriteLock mSensorLock = new ReentrantReadWriteLock();
	@NonNull
	private final Lock mReadLock = mSensorLock.readLock();
	@NonNull
	private final Lock mWriteLock = mSensorLock.writeLock();
	/** hold key/value pairs */
	@NonNull
	private final Map<K, V> mMap = new HashMap<K, V>();

	/**
	 * デフォルトコンストラクタ
	 */
	public ReentrantReadWriteMap() {
		// 今は特に何もしない
	}

	@Nullable
	@Override
	public V get(final Object key) {
		mReadLock.lock();
		try {
			return getLocked(key, null);
		} finally {
			mReadLock.unlock();
		}
	}

	@Nullable
	public V tryGet(@NonNull final K key) {
		if (mReadLock.tryLock()) {
			try {
				return getLocked(key, null);
			} finally {
				mReadLock.unlock();
			}
		}
		return null;
	}

	/**
	 * put specific value into this map
	 * @param key
	 * @param value
	 * @return the previous value associated with key or null if no value mapped.
	 */
	@Override
	public V put(@NonNull final K key, @NonNull final V value) {
		V prev;
		mWriteLock.lock();
		try {
			prev = mMap.remove(key);
			mMap.put(key, value);
		} finally {
			mWriteLock.unlock();
		}
		return prev;
	}

	/**
	 * If the specified key is not already associated with a value (or is mapped to null)
	 * associates it with the given value and returns null, else returns the current value.
	 * @param key
	 * @param value
	 * @return
	 */
	@Override
	public V putIfAbsent(final K key, final V value) {
		V v;
		mWriteLock.lock();
		try {
			v = getLocked(key, null);
			if (v == null) {
				 v = mMap.put(key, value);
			}
		} finally {
			mWriteLock.unlock();
		}
		return v;
	}

	@Override
	public void putAll(@NonNull final Map<? extends K, ? extends V> map) {
		mWriteLock.lock();
		try {
			mMap.putAll(map);
		} finally {
			mWriteLock.unlock();
		}
	}

	@Override
	public V remove(final Object key) {
		mWriteLock.lock();
		try {
			return mMap.remove(key);
		} finally {
			mWriteLock.unlock();
		}
	}

	/**
	 * Removes the entry for the specified key only if it is currently mapped to the specified value.
	 * @param key
	 * @param value
	 * @return specific removed value or null if no mapping existed
	 */
	@Override
	public boolean remove(final Object key, final Object value) {
		mWriteLock.lock();
		try {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
				return mMap.remove(key, value);
			} else {
				// ここの実装はAPI>=24のMap#remove(Object,Object)と基本的には同じ
				final Object curValue = getLocked(key, null);
				if (!isEquals(curValue, value) ||
					((curValue == null) && !containsKeyLocked(key))) {
					return false;
				}
				mMap.remove(key);
				return true;
			}
		} finally {
			mWriteLock.unlock();
		}
	}

	public Collection<V> removeAll() {
		final Collection<V> result = new ArrayList<>();
		mWriteLock.lock();
		try {
			result.addAll(mMap.values());
			mMap.clear();
		} finally {
			mWriteLock.unlock();
		}
		return result;
	}

	@Override
	public void clear() {
		mWriteLock.lock();
		try {
			mMap.clear();
		} finally {
			mWriteLock.unlock();
		}
	}

	@Override
	public int size() {
		mReadLock.lock();
		try {
			return mMap.size();
		} finally {
			mReadLock.unlock();
		}
	}

	@Override
	public boolean containsKey(final Object key) {
		mReadLock.lock();
		try {
			return containsKeyLocked(key);
		} finally {
			mReadLock.unlock();
		}
	}

	@Override
	public boolean containsValue(final Object value) {
		mReadLock.lock();
		try {
			return mMap.containsValue(value);
		} finally {
			mReadLock.unlock();
		}
	}

	@Override
	public V getOrDefault(final Object key, @Nullable final V defaultValue) {
		mReadLock.lock();
		try {
			return getLocked(key, defaultValue);
		} finally {
			mReadLock.unlock();
		}
	}

	@Override
	public boolean isEmpty() {
		mReadLock.lock();
		try {
			return mMap.isEmpty();
		} finally {
			mReadLock.unlock();
		}
	}

	@NonNull
	@Override
	public Set<K> keySet() {
		final Set<K> result = new ArraySet<>();
		mReadLock.lock();
		try {
			result.addAll(mMap.keySet());
		} finally {
			mReadLock.unlock();
		}
		return result;
	}

	/**
	 * return copy of keys
	 * @return
	 */
	@NonNull
	public Collection<K> keys() {
		final Collection<K> result = new ArrayList<K>();
		mReadLock.lock();
		try {
			result.addAll(mMap.keySet());
		} finally {
			mReadLock.unlock();
		}
		return result;
	}

	/**
	 * return copy of mapped values
	 * @return
	 */
	@NonNull
	@Override
	public Collection<V> values() {
		final Collection<V> result = new ArrayList<V>();
		mReadLock.lock();
		try {
			if (!mMap.isEmpty()) {
				result.addAll(mMap.values());
			}
		} finally {
			mReadLock.unlock();
		}
		return result;
	}

	/**
	 * return copy of entries
	 * @return
	 */
	@NonNull
	@Override
	public Set<Map.Entry<K, V>> entrySet() {
		final Set<Map.Entry<K, V>> result = new HashSet<>();
		mReadLock.lock();
		try {
			result.addAll(mMap.entrySet());
		} finally {
			mReadLock.unlock();
		}
		return result;
	}

//================================================================================
	private V getLocked(final Object key, final V defaultValue) {
		return mMap.containsKey(key) ? mMap.get(key) : defaultValue;
	}

	private boolean containsKeyLocked(final Object key) {
		return mMap.containsKey(key);
	}

	private static final boolean isEquals(final Object a, final Object b) {
		// API>=19のObjects.equalsと同じ実装, このライブラリはAPI>=16なのでObjects.equalsは使えない
		return (a == b) || (a != null && a.equals(b));
	}

	/**
	 * lock for read access,
	 * never forget to call #readUnlock
	 */
	protected void readLock() {
		mReadLock.lock();
	}

	/**
	 * unlock read access
	 */
	protected void readUnlock() {
		mReadLock.unlock();
	}

	/**
	 * lock for write access
	 * never forget to call writeUnlock
	 */
	protected void writeLock() {
		mWriteLock.lock();
	}

	/**
	 * unlock write access
	 */
	protected void writeUnlock() {
		mWriteLock.unlock();
	}

	/**
	 * get underlying Collection of values
	 * call this between #readLock - #readUnlock or #writeLock - #writeUnlock
	 * @return
	 */
	protected Collection<V> valuesLocked() {
		return mMap.values();
	}

	/**
	 * get underlying Set of keys
	 * call this between #readLock - #readUnlock or #writeLock - #writeUnlock
	 * @return
	 */
	protected Set<K> keysLocked() {
		return mMap.keySet();
	}

	/**
	 * get underlying Map of key-value pairs
	 * call this between #readLock - #readUnlock or #writeLock - #writeUnlock
	 * @return
	 */
	protected Map<K, V> mapLocked() {
		return mMap;
	}
}
