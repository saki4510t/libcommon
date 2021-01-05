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

import java.lang.ref.WeakReference;
import java.nio.ByteOrder;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

/**
 * IRecycleBufferを実装したMediaData
 */
public class RecycleMediaData extends MediaData implements IRecycleBuffer {
	private final WeakReference<IMediaQueue> mWeakParent;

	public RecycleMediaData(@NonNull final IMediaQueue parent) {
		super();
		mWeakParent = new WeakReference<IMediaQueue>(parent);
	}
	
	public RecycleMediaData(@NonNull final IMediaQueue parent,
		@IntRange(from = 1L) final int size) {

		super(size);
		mWeakParent = new WeakReference<IMediaQueue>(parent);
	}

	public RecycleMediaData(@NonNull final IMediaQueue parent,
		final int size, @NonNull final ByteOrder order) {

		super(size, order);
		mWeakParent = new WeakReference<IMediaQueue>(parent);
	}

	public RecycleMediaData(@NonNull final RecycleMediaData src) {
		super(src);
		mWeakParent = new WeakReference<IMediaQueue>(src.mWeakParent.get());
	}

	@Override
	public void recycle() {
		final IMediaQueue parent = mWeakParent.get();
		if (parent != null) {
			parent.recycle(this);
		}
	}
}
