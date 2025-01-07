package com.serenegiant.media;
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

import com.serenegiant.utils.Pool;

import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * IMediaQueueのオンメモリー実装
 * LinkedBlockingQueueを使用
 */
public class MemMediaQueue implements IMediaQueue<RecycleMediaData> {
	@NonNull
	private final LinkedBlockingQueue<RecycleMediaData> mQueue;
	@NonNull
	private final IRecycleBuffer.Factory<RecycleMediaData> mFactory;
	@NonNull
	private final Pool<RecycleMediaData> mPool;
	
	/**
	 * MemMediaQueue用のデフォルトファクトリークラス
	 *　RecycleMediaDataを生成する
	 */
	public static class DefaultFactory implements IRecycleBuffer.Factory<RecycleMediaData> {
		@NonNull
		@Override
		public RecycleMediaData create(@NonNull final IRecycleParent<RecycleMediaData> parent,
			@Nullable final Object... args) {

			int sz = 0;
			ByteOrder order = null;
			if ((args != null) && args.length > 0) {
				for (final Object arg: args) {
					if (arg instanceof Integer) {
						sz = (int)arg;
					} else if (arg instanceof ByteOrder) {
						order = (ByteOrder)arg;
					}
				}
			}
			if ((sz > 0) && (order != null)) {
				return new RecycleMediaData(parent, sz, order);
			} else if (sz > 0) {
				return new RecycleMediaData(parent, sz);
			} else if (order != null) {
				return new RecycleMediaData(parent, order);
			} else {
				return new RecycleMediaData(parent);
			}
		}
	}

	/**
	 * コンストラクタ
	 * DefaultFactoryをファクトリーとして使う
 	 * @param initNum
	 * @param maxNumInPool
	 */
	public MemMediaQueue(final int initNum, final int maxNumInPool) {
		this(initNum, maxNumInPool, maxNumInPool, null);
	}

	/**
	 * コンストラクタ
	 * DefaultFactoryをファクトリーとして使う
 	 * @param initNum
	 * @param maxNumInPool
	 * @param maxQueueSz
	 */
	public MemMediaQueue(final int initNum, final int maxNumInPool, final int maxQueueSz) {
		this(initNum, maxNumInPool, maxQueueSz, null);
	}

	/**
	 * コンストラクタ
	 * @param initNum
	 * @param maxNumInPool
	 * @param maxQueueSz
	 * @param factory
	 */
	public MemMediaQueue(final int initNum, final int maxNumInPool, final int maxQueueSz,
		@Nullable final IRecycleBuffer.Factory<RecycleMediaData> factory) {

		mQueue = new LinkedBlockingQueue<RecycleMediaData>(maxQueueSz);
		mFactory = factory != null ? factory : new DefaultFactory();
		mPool = new Pool<RecycleMediaData>(initNum, maxNumInPool) {
			@NonNull
			@Override
			protected RecycleMediaData createObject(
				@Nullable final Object... args) {

				return mFactory.create(MemMediaQueue.this, args);
			}
		};
	}

	@Override
	public void init(@Nullable final Object... args) {
		clear();
		mPool.init(args);
	}

	@Override
	public void clear() {
		mQueue.clear();
		mPool.clear();
	}

	@Override
	public void drainAll() {
		final List<RecycleMediaData> list = new ArrayList<>();
		mQueue.drainTo(list);
		for (final RecycleMediaData data: list) {
			data.setRecycled(true);
		}
		mPool.recycle(list);
	}

	/**
	 * プールからデータ保持用オブジェクトを取得する
	 * @param args
	 * @return
	 */
	@Nullable
	@Override
	public RecycleMediaData obtain(@Nullable final Object... args) {
		final RecycleMediaData result = mPool.obtain(args);
		if (result != null) {
			result.setRecycled(false);
		}
		return result;
	}

	/**
	 * キューにデータを追加する
	 * @param buffer
	 * @return true: 正常にキューに追加できた
	 */
	@Override
	public boolean queueFrame(@NonNull final RecycleMediaData buffer) {
		buffer.setRecycled(false);
		return mQueue.offer(buffer);
	}
	
	@Override
	@Nullable
	public RecycleMediaData peek() {
		return mQueue.peek();
	}
	
	@Override
	@Nullable
	public RecycleMediaData poll() {
		return mQueue.poll();
	}
	
	@Override
	@Nullable
	public RecycleMediaData poll(final long timeout, final TimeUnit unit)
		throws InterruptedException {

		return mQueue.poll(timeout, unit);
	}
	
	@Override
	public int count() {
		return mQueue.size();
	}
	
	@Override
	public boolean recycle(@NonNull final RecycleMediaData buffer) {
		if (!buffer.isRecycled()) {
			buffer.setRecycled(true);
			return mPool.recycle(buffer);
		} else {
			return false;
		}
	}

}
