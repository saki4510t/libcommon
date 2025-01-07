package com.serenegiant.collections;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Map holds multiple values for each key
 * This class use HashMap as Map and ArrayList as Collection as default.
 * You can override this by overriding #createContentsMap and #createCollection.
 * @param <K>
 * @param <V>
 */
public class CollectionMap<K, V> implements Map<K, Collection<V>> {
	@NonNull
	private final Map<K, Collection<V>> contents;

	/**
	 * コンストラクタ
	 */
	public CollectionMap() {
		contents = createContentsMap();
	}

	@Override
	public void clear() {
		contents.clear();
	}

	@Override
	public boolean containsKey(final Object key) {
		return contents.containsKey(key);
	}

	/**
	 * 指定したオブジェクトを値コレクションとしてい含んでいるかどうかを取得
	 * オブジェクトがCollection<V>でなければtrueにはならない
	 * オブジェクトが値コレクションのいずれかに含まれているかどうかを確認するには#containsInValueを使う
	 * @param value
	 * @return
	 */
	@Override
	public boolean containsValue(final Object value) {
		return contents.containsValue(value);
	}

	/**
	 * 指定したオブジェクトが値コレクションのいずれかに含まれているかどうかを取得
	 * @param value
	 * @return
	 */
	public boolean containsInValue(final V value) {
		for (final Collection<V> collection : contents.values()) {
			if (collection.contains(value)) {
				return true;
			}
		}
		return false;
	}

	@Override
	@NonNull
	public Set<Entry<K, Collection<V>>> entrySet() {
		return contents.entrySet();
	}

	@Nullable
	@Override
	public Collection<V> get(final Object key) {
		return internalGet(key);
	}

	@Override
	public boolean isEmpty() {
		return contents.isEmpty();
	}

	@Override
	@NonNull
	public Set<K> keySet() {
		return contents.keySet();
	}

	/**
	 * 指定したキーに対応する値コレクションが存在する場合には置き換える、
	 * 指定したキーに対応する値コレクションが存在しない場合には新規追加する
	 * @param key
	 * @param value
	 * @return
	 */
	@Override
	public Collection<V> put(final K key, final Collection<V> value) {
		return contents.put(key, value);
	}

	/**
	 * 指定したキーに対応する値コレクションに指定した値を追加する
	 * 指定したキーに対応する値コレクションが存在しない場合は#createCollectionで生成して追加する
	 * @param key
	 * @param value
	 * @return
	 */
	public boolean add(final K key, final V value) {
		Collection<V> collection = get(key);
		if (collection == null) {
			collection = createCollection();
			contents.put(key, collection);
		}
		return collection.add(value);
	}

	@Override
	public void putAll(@NonNull final Map<? extends K, ? extends Collection<V>> m) {
		contents.putAll(m);
	}

	/**
	 * #putAllと違ってキーに対応する値コレクションがすでに存在していても置換せず追加する
	 * @param m
	 */
	public void addAll(@NonNull final Map<? extends K, ? extends Collection<V>> m) {
		for (final Entry<? extends K, ? extends Collection<V>> entry : m.entrySet()) {
			addAll(entry.getKey(), entry.getValue());
		}
	}

	/**
	 * 指定したキーの値コレクションに指定した値コレクションの値をすべて追加する
	 * 指定したキーに対応する値コレクションが存在していない場合には#createCollectionで生成して追加する
	 * @param key
	 * @param values
	 * @return
	 */
	public boolean addAll(@NonNull final K key, @NonNull final Collection<? extends V> values) {
		Collection<V> collection = internalGet(key);
		if (collection == null) {
			collection = createCollection();
			contents.put(key, collection);
		}
		return collection.addAll(values);
	}

	@Override
	public Collection<V> remove(final Object key) {
		return contents.remove(key);
	}

	@Override
	public boolean remove(final Object key, final Object value) {
		final Collection<?> collection = internalGet(key);
		return collection != null && collection.remove(value);
	}

	@Override
	public int size() {
		return contents.size();
	}

	/**
	 * 指定したキーに対応する値コレクションのサイズを取得する
	 * 指定したキーに対応する値コレクションがなければ0
	 * @param key
	 * @return
	 */
	public int size(@NonNull final K key) {
		final Collection<V> collection = internalGet(key);
		return collection != null ? collection.size() : 0;
	}

	/**
	 * このMapに含まれる値コレクションを取得する
	 * @return
	 */
	@NonNull
	@Override
	public Collection<Collection<V>> values() {
		return contents.values();
	}

	/**
	 * このMapに含まれる値コレクション内のすべての値を取得する
	 * @return
	 */
	@NonNull
	public Collection<V> valuesAll() {
		final Collection<V> result = createCollection();
		for (final Collection<V> v: values()) {
			result.addAll(v);
		}
		return result;
	}

//--------------------------------------------------------------------------------
	/**
	 * Key-Valueペア保持用のMapオブジェクト生成メソッド
	 * デフォルトではHashMapを生成する
	 * @return
	 */
	@NonNull
	protected Map<K, Collection<V>> createContentsMap() {
		return new HashMap<>();
	}

	/**
	 * 値用のコレクション生成メソッド
	 * デフォルトではArrayListを使う
	 * @return
	 */
	@NonNull
	protected Collection<V> createCollection() {
		return new ArrayList<>();
	}

	@Nullable
	private Collection<V> internalGet(final Object key) {
		return contents.containsKey(key) ? contents.get(key) : null;
	}
}
