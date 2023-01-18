package com.serenegiant.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2023 saki t_saki@serenegiant.com
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
 * オブジェクトを再利用してオブジェクトの生成を抑制するためのプールオブジェクト
 * @param <T>
 */
public abstract class Pool<T> {

	@NonNull
	private final List<T> mPool = new ArrayList<T>();
	private final int mInitNum;
	private final int mMaxNumInPool;
	private final int mLimitNum;
	private int mCreatedObjects;
	
	/**
	 * コンストラクタ
	 * @param initNum
	 * @param maxNumInPool プール内に保持できる最大数==最大生成数
	 */
	public Pool(final int initNum, final int maxNumInPool) {
		this(initNum, maxNumInPool, maxNumInPool, (Object) null);
	}
	
	/**
	 * コンストラクタ
	 * @param initNum プール内のオブジェクトの初期数
	 * @param maxNumInPool プール内に保持できる最大数
	 * @param limitNum 最大生成数
	 * @param args initを呼ぶ際のオプション引数, Tの生成に必要な値を渡す, 省略可
	 */
	public Pool(final int initNum, final int maxNumInPool, final int limitNum, @Nullable final Object... args) {
		mInitNum = initNum;
		mMaxNumInPool = Math.min(maxNumInPool, limitNum);
		mLimitNum = limitNum;
		init(args);
	}
	
	/**
	 * プール内のオブジェクトを破棄して新たに初期数まで確保する
	 * @param args オプション引数, Tの生成に必要な値を渡す, #createObjectへ引き渡される, 省略可
	 */
	public void init(@Nullable final Object... args) {
		synchronized (mPool) {
			mPool.clear();
			mCreatedObjects = 0;
			for (int i = 0; (i < mInitNum) && (i < mMaxNumInPool); i++) {
				final T obj = createObject(args);
				mPool.add(obj);
				mCreatedObjects++;
			}
		}
	}

	/**
	 * プールからオブジェクトTを取得する。もしプールが空で最大生成数を超えている場合にはnullを返す
	 * XXX プールが空の時に一定時間待機するメソッドもあったほうがいいかもしれない
	 * @param args オプション引数, Tの生成に必要な値を渡す, #createObjectへ引き渡される, 省略可
	 * @return
	 */
	@Nullable
	public T obtain(@Nullable final Object... args) {
		T result = null;
		synchronized (mPool) {
			if (!mPool.isEmpty()) {
				result = mPool.remove(mPool.size() - 1);
			}
			if ((result == null) && (mCreatedObjects < mLimitNum)) {
				result = createObject(args);
				mCreatedObjects++;
			}
		}
		return result;
	}
	
	/**
	 * オブジェクトTを生成する
	 * @param args オプション引数, Tの生成に必要な値を渡す, 省略可
	 * @return
	 */
	@NonNull
	protected abstract T createObject(@Nullable final Object... args);
	
	/**
	 * 使用済みオブジェクトをプールに返却する
	 * @param obj
	 * @return true: プールに返却できた, false: プールに返却できなかった(最大保持数より多くなってしまった)
	 */
	public boolean recycle(@NonNull final T obj) {
		synchronized (mPool) {
			if (mPool.size() < mMaxNumInPool) {
				return mPool.add(obj);
			} else {
				mCreatedObjects--;
				return false;
			}
		}
	}
	
	/**
	 * 使用済みオブジェクトをプールに返却する
	 * @param objects
	 */
	public void recycle(@NonNull final Collection<T> objects) {
		for (final T obj: objects) {
			if (obj != null) {
				recycle(obj);
			}
		}
	}

	/**
	 * 使用済みオブジェクトをプールに返却する
	 * @param objects
	 */
	public void recycle(@NonNull final T[] objects) {
		for (final T obj: objects) {
			if (obj != null) {
				recycle(obj);
			}
		}
	}

	/**
	 * 使用済みオブジェクトを破棄する。
	 * オブジェクトが再利用できなくなったときなどに生成済みオブジェクト数を減らす
 	 * @param obj
	 */
	public void release(@NonNull final T obj) {
		synchronized (mPool) {
			if (mCreatedObjects > 0) {
				mCreatedObjects--;
			}
		}
	}

	/**
	 * プールを空にする
	 */
	public void clear() {
		synchronized (mPool) {
			mPool.clear();
			mCreatedObjects = 0;
		}
	}
}
