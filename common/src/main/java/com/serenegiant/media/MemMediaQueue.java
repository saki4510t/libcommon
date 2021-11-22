package com.serenegiant.media;
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

import com.serenegiant.utils.Pool;

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
	private final LinkedBlockingQueue<RecycleMediaData> mQueue
		= new LinkedBlockingQueue<RecycleMediaData>();
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

			return new RecycleMediaData(parent);
		}
	}

	/**
	 * コンストラクタ
	 * DefaultFactoryをファクトリーとして使う
 	 * @param initNum
	 * @param maxNumInPool
	 */
	public MemMediaQueue(final int initNum, final int maxNumInPool) {
		this(initNum, maxNumInPool, null);
	}

	/**
	 * コンストラクタ
	 * @param initNum
	 * @param maxNumInPool
	 * @param factory
	 */
	public MemMediaQueue(final int initNum, final int maxNumInPool,
		@Nullable final IRecycleBuffer.Factory<RecycleMediaData> factory) {

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
	public RecycleMediaData obtain(@Nullable final Object... args) {
		return mPool.obtain(args);
	}
	
	@Override
	public boolean queueFrame(@NonNull final RecycleMediaData buffer) {
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
		return mPool.recycle(buffer);
	}

}
