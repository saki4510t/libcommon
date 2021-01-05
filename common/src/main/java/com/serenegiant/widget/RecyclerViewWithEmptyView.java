package com.serenegiant.widget;
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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.serenegiant.common.R;
import com.serenegiant.view.ViewUtils;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * 表示内容が空のときに代わりに指定したViewを表示する機能を追加したRecyclerView実装
 */
public class RecyclerViewWithEmptyView extends RecyclerView {
	private static final boolean DEBUG = false;	// FIXME set false on production
	private static final String TAG = RecyclerViewWithEmptyView.class.getSimpleName();

	@IdRes
	private int mEmptyViewId;
   	@Nullable private View mEmptyView;

	/**
	 * コンストラクタ
	 * @param context
	 */
	public RecyclerViewWithEmptyView(
		@NonNull final Context context) {
		this(context, null, 0);
	}

	/**
	 * コンストラクタ
	 * @param context
	 * @param attrs
	 */
	public RecyclerViewWithEmptyView(
		@NonNull final Context context,
		@Nullable final AttributeSet attrs) {

		this(context, attrs, 0);
	}

	/**
	 * コンストラクタ
	 * @param context
	 * @param attrs
	 * @param defStyle
	 */
	@SuppressLint("WrongConstant")
	public RecyclerViewWithEmptyView(
		@NonNull final Context context,
		@Nullable final AttributeSet attrs, final int defStyle) {

		super(context, attrs, defStyle);
		Drawable divider = null;
		if (attrs != null) {
			int defStyleRes = 0;
			final TypedArray attribs = context.getTheme().obtainStyledAttributes(
				attrs, R.styleable.RecycleViewWithEmptyView, defStyle, defStyleRes);
			try {
				if (attribs.hasValue(R.styleable.RecycleViewWithEmptyView_listDivider)) {
					divider = attribs.getDrawable(R.styleable.RecycleViewWithEmptyView_listDivider);
				}
			} catch (final Exception e) {
				if (DEBUG) Log.w(TAG, e);
			}
			try {
				mEmptyViewId  = attribs.getResourceId(R.styleable.RecyclerViewWithEmptyView_emptyView, View.NO_ID);
			} catch (final Exception e) {
				if (DEBUG) Log.w(TAG, e);
			}
			attribs.recycle();
		}
		if (DEBUG) Log.v(TAG, "divider=" + divider);
		int orientation = LinearLayoutManager.VERTICAL;
		if (getLayoutManager() instanceof LinearLayoutManager) {
			orientation = ((LinearLayoutManager)getLayoutManager()).getOrientation();
		}
		final DividerItemDecoration deco = new DividerItemDecoration(context, divider);
		deco.setOrientation(orientation);
		addItemDecoration(deco);
	}

	@Override
	public void setAdapter(final Adapter adapter) {
		if (getAdapter() != adapter) {
			try {
				if (getAdapter() != null) {
					getAdapter().unregisterAdapterDataObserver(mAdapterDataObserver);
				}
			} catch (final Exception e) {
				// ignore
			}
			super.setAdapter(adapter);
			if (adapter != null) {
				adapter.registerAdapterDataObserver(mAdapterDataObserver);
			}
		}
		updateEmptyView();
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		// empty viewを探す
		final int[] ids = new int[] {mEmptyViewId, R.id.empty, android.R.id.empty};
		final View emptyView = ViewUtils.findViewInParent(this, ids, View.class);
		setEmptyView(emptyView);
	}

	public void setEmptyView(final View empty_view) {
		if (mEmptyView != empty_view) {
			mEmptyView = empty_view;
			updateEmptyView();
		}
	}

	protected void updateEmptyView() {
		if (mEmptyView != null) {
			if (!isInEditMode()) {
				// ASのレイアウトエディタで編集中ではないときのみ
				final Adapter adapter = getAdapter();
				post(new Runnable() {
					@Override
					public void run() {
						mEmptyView.setVisibility((adapter == null) || (adapter.getItemCount() == 0)
							? VISIBLE : GONE);
					}
				});
			}
		}
	}

	private final AdapterDataObserver mAdapterDataObserver = new AdapterDataObserver() {
		@Override
		public void onChanged() {
			super.onChanged();
			updateEmptyView();
		}

		@Override
		public void onItemRangeChanged(final int positionStart, final int itemCount) {
			super.onItemRangeChanged(positionStart, itemCount);
			updateEmptyView();
		}

//		@Override
//		public void onItemRangeChanged(final int positionStart,
// 				final int itemCount, Object payload) {
//
//			// fallback to onItemRangeChanged(positionStart, itemCount) if app
//			// does not override this method.
//			onItemRangeChanged(positionStart, itemCount);
//		}

//		@Override
//		public void onItemRangeInserted(final int positionStart, final int itemCount) {
//			updateEmptyView();
//		}

		@Override
		public void onItemRangeRemoved(final int positionStart, final int itemCount) {
			super.onItemRangeRemoved(positionStart, itemCount);
			updateEmptyView();
		}

//		@Override
//		public void onItemRangeMoved(final int fromPosition, final int toPosition,
// 			final int itemCount) {
//		}

	};

}
