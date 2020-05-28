package com.serenegiant.widget;

import android.view.View;
import android.widget.TextView;

import com.serenegiant.common.R;

import java.util.List;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;

/**
 * 文字列リストを表示するためのCustomRecyclerViewAdapter実装
 */
public class StringsRecyclerViewAdapter extends CustomRecyclerViewAdapter<String> {
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
	protected void registerDataSetObserver(@NonNull final List items) {

	}

	@Override
	protected void unregisterDataSetObserver(@NonNull final List items) {

	}

	/**
	 * 文字列表示用のTextViewのidとして対応しているid一覧
	 */
	private static final int[] TV_ID_LIST = new int[] {
		R.id.title,
		R.id.content,
		android.R.id.title,
		android.R.id.text1,
		android.R.id.text2,
	};

	@Override
	public void onBindViewHolder(
		@NonNull final ViewHolder<String> holder,
		final int position) {

		holder.mItem = getItem(position);
		TextView tv = null;
		if (holder.mView instanceof TextView) {
			tv = (TextView)holder.mView;
		} else {
			for (final int id: TV_ID_LIST) {
				final View v = holder.mView.findViewById(id);
				if (v instanceof TextView) {
					tv = (TextView)v;
					break;
				}
			}
		}
		if (tv != null) {
			tv.setText(holder.mItem);
		} else {
			throw new RuntimeException("TextView not found!");
		}
	}
}
