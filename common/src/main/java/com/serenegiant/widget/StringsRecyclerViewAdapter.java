package com.serenegiant.widget;

import android.widget.TextView;

import com.serenegiant.view.ViewUtils;

import java.util.List;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;

/**
 * 文字列リストを表示するためのCustomRecyclerViewAdapter実装
 */
public class StringsRecyclerViewAdapter extends ArrayListRecyclerViewAdapter<String> {
	private static final boolean DEBUG = false;	// FIXME set false when production
	private static final String TAG = StringsRecyclerViewAdapter.class.getSimpleName();

	/**
	 * コンストラクタ
	 * @param itemViewLayoutId
	 * @param items
	 */
	public StringsRecyclerViewAdapter(
		@LayoutRes final int itemViewLayoutId,
		@NonNull final List<String> items) {

		super(itemViewLayoutId, items);
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
		final TextView tv = ViewUtils.findTitleView(holder.mView);
		if (tv != null) {
			tv.setText(holder.mItem);
		} else {
			throw new RuntimeException("TextView not found!");
		}
	}
}
