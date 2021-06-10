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

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Checkable;

import com.serenegiant.common.R;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

/**
 * アイテムのクリック・ロングクリックイベントのコールバック処理等を追加したRecyclerView.Adapter実装
 * @param <T>
 */
public abstract class ArrayListRecyclerViewAdapter<T>
	extends RecyclerView.Adapter<ArrayListRecyclerViewAdapter.ViewHolder<T>> {

	private static final boolean DEBUG = false;	// FIXME set false when production
	private static final String TAG = ArrayListRecyclerViewAdapter.class.getSimpleName();

	public interface ArrayListRecyclerViewListener<T> {
		public void onItemClick(
			@NonNull final RecyclerView.Adapter<?> parent,
			@NonNull final View view,
			final int position, @Nullable final T item);
		public boolean onItemLongClick(
			@NonNull final RecyclerView.Adapter<?> parent,
			@NonNull final View view,
			final int position, @Nullable final T item);

		/**
		 * タッチモード以外または対象となるRecyclerViewの
		 * isFocusableInTouchModeがtrueの場合に要素が
		 * 選択された時(フォーカスを受け取った時)に呼び出される
		 * @param parent
		 * @param view
		 * @param position
		 * @param item
		 */
		public void onItemSelected(
			@NonNull final RecyclerView.Adapter<?> parent,
			@NonNull final View view,
			final int position, @Nullable final T item);

		/**
		 * タッチモード以外または対象となるRecyclerViewの
		 * isFocusableInTouchModeがtrueの場合に選択されている要素が
		 * 無くなった時に呼び出される
		 * @param parent
		 */
		public void onNothingSelected(@NonNull final RecyclerView.Adapter<?> parent);
	}

	@LayoutRes
	private final int mItemViewId;
	@NonNull
	private final List<T> mItems;
	private LayoutInflater mLayoutInflater;
	private RecyclerView mRecycleView;
	private ArrayListRecyclerViewListener<T> mCustomRecycleViewListener;
	private int mSelectedPosition = -1;

    public ArrayListRecyclerViewAdapter(
    	@LayoutRes final int itemViewLayoutId,
		@NonNull final List<T> items) {

		mItemViewId = itemViewLayoutId;
		mItems = items;
		synchronized (mItems) {
			registerDataSetObserver(mItems);
		}
    }

	@Override
	protected void finalize() throws Throwable {
		synchronized (mItems) {
			unregisterDataSetObserver(mItems);
		}
		super.finalize();
	}

	@Override
	public void onAttachedToRecyclerView(@NonNull final RecyclerView recyclerView) {
		super.onAttachedToRecyclerView(recyclerView);
		mSelectedPosition = -1;
		mRecycleView = recyclerView;
	}

	@Override
	public void onDetachedFromRecyclerView(@NonNull final RecyclerView recyclerView) {
		mRecycleView = null;
		mSelectedPosition = -1;
		super.onDetachedFromRecyclerView(recyclerView);
	}

	@NonNull
	@Override
    public ViewHolder<T> onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
		final LayoutInflater inflater = getLayoutInflater(parent.getContext());
        final View view = onCreateItemView(inflater, parent, viewType);
		view.setOnClickListener(mOnClickListener);
		view.setOnLongClickListener(mOnLongClickListener);
		view.setOnFocusChangeListener(mOnFocusChangeListener);
        return onCreateViewHolder(view);
    }

	protected View onCreateItemView(final LayoutInflater inflater,
		final ViewGroup parent, final int viewType) {

		return inflater.inflate(mItemViewId, parent, false);
	}

	protected ViewHolder<T> onCreateViewHolder(final View item) {
		return new ViewHolder<T>(item);
	}

    @Override
    public int getItemCount() {
        return mItems.size();
    }

	/**
	 * 現在選択されている要素の位置を返す
	 * 選択していない場合は-1
	 * タッチモード以外またはRecyclerViewのisFocusableInTouchMode=trueのときのみ有効
	 * @return
	 */
	public int getSelectedPosition() {
		return mSelectedPosition;
	}

	/**
	 * 内部で保持している要素一覧を取得する
	 * 返り値のList<T>はunmodifiableListなので変更できない
	 * @return
	 */
	public List<T> getItems() {
		return Collections.unmodifiableList(mItems);
	}

	public T getItem(final int position) throws IndexOutOfBoundsException {
		if ((position >= 0) && (position < mItems.size())) {
			return mItems.get(position);
		} else {
			throw new IndexOutOfBoundsException();
		}
	}

	public void setOnItemClickListener(final ArrayListRecyclerViewListener<T> listener) {
		mCustomRecycleViewListener = listener;
	}

	@Nullable
	public RecyclerView getParent() {
		return mRecycleView;
	}

	public void clear() {
		synchronized (mItems) {
			unregisterDataSetObserver(mItems);
			mItems.clear();
		}
		notifyDataSetChanged();
	}

	/**
	 * 指定したコレクションを全て追加する
	 * @param collection
	 */
	public void addAll(final Collection<? extends T> collection) {
		final int n, m;
		synchronized (mItems) {
			n = mItems.size() - 1;
			unregisterDataSetObserver(mItems);
			mItems.addAll(collection);
			m = mItems.size() - 1;
			registerDataSetObserver(mItems);
		}
		if (m > n) {
			notifyItemRangeChanged(n, m - n);
		}
	}

	/**
	 * 指定したコレクションで置き換える
	 * @param collection
	 */
	public void replaceAll(final Collection<? extends T> collection) {
		synchronized (mItems) {
			unregisterDataSetObserver(mItems);
			mItems.clear();
			mItems.addAll(collection);
			registerDataSetObserver(mItems);
		}
		notifyDataSetChanged();
	}

	public void sort(final Comparator<? super T> comparator) {
		synchronized (mItems) {
			Collections.sort(mItems, comparator);
		}
		notifyDataSetChanged();
	}

	protected LayoutInflater getLayoutInflater(final Context context) {
		if (mLayoutInflater == null) {
			mLayoutInflater = LayoutInflater.from(context);
		}
		return mLayoutInflater;
	}

	protected abstract void registerDataSetObserver(@NonNull final List<T> items);

	protected abstract void unregisterDataSetObserver(@NonNull final List<T> items);

	protected final View.OnClickListener mOnClickListener = new View.OnClickListener() {
		@Override
		public void onClick(final View v) {
			if (mRecycleView != null) {
				if (v instanceof Checkable) {	// visual feedback
					((Checkable)v).setChecked(true);
					v.postDelayed(new Runnable() {
						@Override
						public void run() {
							((Checkable)v).setChecked(false);
						}
					}, 100);
				}
				if (mCustomRecycleViewListener != null) {
					final Object pos = v.getTag(R.id.position);
					if (pos instanceof Integer) {
						try {
							final int position = (Integer)pos;
							final T item = getItem(position);
							mCustomRecycleViewListener.onItemClick(
								ArrayListRecyclerViewAdapter.this, v, position, item);
							return;
						} catch (final Exception e) {
							Log.w(TAG, e);
						}
					}
					try {
						final int position = mRecycleView.getChildAdapterPosition(v);
						final T item = getItem(position);
						mCustomRecycleViewListener.onItemClick(
							ArrayListRecyclerViewAdapter.this, v, position, item);
					} catch (final Exception e) {
						Log.w(TAG, e);
					}
				}
			}
		}
	};

	protected final View.OnLongClickListener mOnLongClickListener
		= new View.OnLongClickListener() {
		@Override
		public boolean onLongClick(final View v) {
			if (DEBUG) Log.v(TAG, "onLongClick:" + v);
			if (mRecycleView != null) {
				try {
					if (mCustomRecycleViewListener != null) {
						final Object pos = v.getTag(R.id.position);
						if (pos instanceof Integer) {
							final int position = (Integer)pos;
							final T item = getItem(position);
							return mCustomRecycleViewListener.onItemLongClick(
								ArrayListRecyclerViewAdapter.this, v, position, item);
						} else {
							final int position = mRecycleView.getChildAdapterPosition(v);
							final T item = getItem(position);
							return mCustomRecycleViewListener.onItemLongClick(
								ArrayListRecyclerViewAdapter.this, v, position, item);
						}
					}
				} catch (final Exception e) {
					Log.w(TAG, e);
				}
			}
			return false;
		}
	};

	/**
	 * RecyclerViewとそのAdapterにはListView等のAdapterView系に存在した
	 * OnItemSelectedListenerに相当するものが存在しない。
	 * タッチモードの場合は基本的に選択している状態を視覚表示しないので問題ないが
	 * タッチモード以外(Android TVやD-padをつないでいる時)に選択している
	 * 要素を取得できないと困ることがあるので、ワークアラウンドとして
	 * 要素のルートViewのフォーカスの変化で選択状態を検知する
	 */
	protected final View.OnFocusChangeListener mOnFocusChangeListener
		= new View.OnFocusChangeListener() {
		@Override
		public void onFocusChange(final View v, final boolean hasFocus) {
			if (!hasFocus) {
				mSelectedPosition = -1;
			} else if (hasFocus && (mRecycleView != null)) {
				try {
					final Object pos = v.getTag(R.id.position);
					if (pos instanceof Integer) {
						final int position = mSelectedPosition = (Integer)pos;
						if (mCustomRecycleViewListener != null) {
							final T item = getItem(position);
							mCustomRecycleViewListener.onItemSelected(
								ArrayListRecyclerViewAdapter.this, v, position, item);
						}
						return;
					} else {
						final int position = mSelectedPosition
							= mRecycleView.getChildAdapterPosition(v);
						final T item = getItem(position);
						if (mCustomRecycleViewListener != null) {
							mCustomRecycleViewListener.onItemSelected(
								ArrayListRecyclerViewAdapter.this, v, position, item);
						}
						return;
					}
				} catch (final Exception e) {
					Log.w(TAG, e);
				}
			}
			if ((mSelectedPosition < 0) && (mCustomRecycleViewListener != null)) {
				// 何も選択されていない時(たぶんRecyclerView以外のViewへフォーカスが移った)
				mCustomRecycleViewListener.onNothingSelected(ArrayListRecyclerViewAdapter.this);
			}
 		}
	};

    public static class ViewHolder<T> extends RecyclerView.ViewHolder {
        public final View mView;
        public int position;
        public T mItem;

        public ViewHolder(final View view) {
            super(view);
            mView = view;
        }

		@NonNull
        @Override
        public String toString() {
            return super.toString() + " '" + mItem + "'";
        }

		public void setEnable(final boolean enable) {
			mView.setEnabled(enable);
		}

        public void hasDivider(final boolean hasDivider) {
        	if (mView instanceof Dividable) {
        		((Dividable)mView).hasDivider(hasDivider);
			} else {
        		mView.setTag(R.id.has_divider, hasDivider);
			}
        }

        public boolean hasDivider() {
			if (mView instanceof Dividable) {
				return ((Dividable)mView).hasDivider();
			} else {
				final Boolean b = (Boolean)mView.getTag(R.id.has_divider);
				return ((b != null) && b);
			}
        }
    }
}
