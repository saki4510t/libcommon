package com.serenegiant.mediastore;
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

import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * MediaStore関係のRecyclerAdapterでアイテムを選択したときのコールバックリスナー
 */
public interface MediaStoreRecyclerAdapterListener {
	/**
	 * アイテムをクリックした
	 * @param parent
	 * @param view
	 * @param item
	 */
	public void onItemClick(
		@NonNull RecyclerView.Adapter<?> parent,
		@NonNull View view, @NonNull final MediaInfo item);

	/**
	 * アイテムを長押しした
	 * @param parent
	 * @param view
	 * @param item
	 * @return
	 */
	public boolean onItemLongClick(
		@NonNull RecyclerView.Adapter<?> parent,
		@NonNull View view, @NonNull final MediaInfo item);
}
