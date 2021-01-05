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

import java.util.concurrent.TimeUnit;

import androidx.annotation.Nullable;

/**
 * バッファリング用のFIFOバッファインターフェース
 */
public interface IMediaQueue {
	/**
	 * キューをクリア
	 */
	public void clear();

	/**
	 * バッファプールからIRecycleBufferを取得
	 * @return
	 */
	public IRecycleBuffer obtain(@Nullable final Object... args);

	/**
	 * キューに追加
	 * @param buffer
	 * @return
	 */
	public boolean queueFrame(final IRecycleBuffer buffer);
	
	/**
	 * キューの先頭を取得
	 * @return
	 */
	@Nullable
	public IRecycleBuffer peek();
	
	/**
	 * キューの先頭を除去して返す
	 * キューが空の時はnullを返す
	 * @return
	 */
	@Nullable
	public IRecycleBuffer poll();
	
	/**
	 * キューの先頭を除去して返す
	 * キューが空の時は指定時間待機する
	 * @param timeout
	 * @param unit
	 * @return
	 * @throws InterruptedException
	 */
	@Nullable
	public IRecycleBuffer poll(final long timeout, final TimeUnit unit)
		throws InterruptedException;

	/**
	 * キュー内の個数を取得
	 */
	public int count();
	
	/**
	 * バッファを再利用可能にする
	 * @param buffer
	 * @return
	 */
	public boolean recycle(final IRecycleBuffer buffer);
}
