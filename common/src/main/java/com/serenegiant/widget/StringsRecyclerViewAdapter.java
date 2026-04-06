package com.serenegiant.widget;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2026 saki t_saki@serenegiant.com
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

import android.widget.TextView;

import com.serenegiant.common.R;
import com.serenegiant.view.ViewFindUtils;

import java.util.List;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;

/**
 * 文字列リストを表示するためのArrayListRecyclerViewAdapter実装
 */
public class StringsRecyclerViewAdapter extends ArrayListRecyclerViewAdapter<String> {
	private static final boolean DEBUG = false;	// FIXME set false when production
	private static final String TAG = StringsRecyclerViewAdapter.class.getSimpleName();

	@IdRes
	private static final int[] TITLE_IDS = {
		R.id.title,
		R.id.content,
		android.R.id.title,
		android.R.id.text1,
		android.R.id.text2,
	};

	/**
	 * コンストラクタ
	 * @param itemViewLayoutId
	 * @param items
	 */
	public StringsRecyclerViewAdapter(
		@LayoutRes final int itemViewLayoutId,
		@NonNull final List<String> items) {

		super(itemViewLayoutId, items, new DiffCallback<String>());
	}

	@Override
	protected void registerDataSetObserver(@NonNull final List<String> items) {

	}

	@Override
	protected void unregisterDataSetObserver(@NonNull final List<String> items) {

	}

	@Override
	public void onBindViewHolder(
		@NonNull final ViewHolder<String> holder,
		final int position) {

		holder.mItem = getItem(position);
		final TextView tv = ViewFindUtils.findTitleView(holder.mView, TITLE_IDS);
		if (tv != null) {
			tv.setText(holder.mItem);
		} else {
			throw new RuntimeException("TextView not found!");
		}
	}
}
