package com.serenegiant.bluetooth;
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

import android.annotation.SuppressLint;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.serenegiant.common.R;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class BluetoothDeviceInfoRecyclerAdapter
	extends RecyclerView.Adapter<BluetoothDeviceInfoRecyclerAdapter.ViewHolder> {

	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = BluetoothDeviceInfoRecyclerAdapter.class.getSimpleName();

	public interface OnItemClickListener {
		public void onClick(final int position, final BluetoothDeviceInfo item);
	}
	
	/** 項目表示用レイアウトリソースID */
	@LayoutRes
	private final int mItemLayoutResId;
	@IdRes
	private final int mNameTvId;
	@IdRes
	private final int mAddressTvId;
	@IdRes
	private final int mIconIvId;
	@NonNull
	private final List<BluetoothDeviceInfo> mValues;
	@Nullable
	private final OnItemClickListener mListener;

	/**
	 * コンストラクタ
	 *
	 * @param itemLayoutResId
	 * @param listener
	 */
	public BluetoothDeviceInfoRecyclerAdapter
		(@LayoutRes final int itemLayoutResId,
		@Nullable final OnItemClickListener listener) {
		
		this(itemLayoutResId, null, listener);
	}
	
	public BluetoothDeviceInfoRecyclerAdapter(
		@LayoutRes final int itemLayoutResId,
		@Nullable final List<BluetoothDeviceInfo> list,
		@Nullable final OnItemClickListener listener) {

		this(itemLayoutResId, R.id.name, R.id.address, R.id.icon, list, listener);
	}

	/**
	 * コンストラクタ
	 * @param itemLayoutResId 項目表示用レイアウトリソースID
	 * @param nameTextViewId Bluetooth機器名表示用テキストビューID
	 * @param addressTextViewId Bluetoothアドレス表示用テキストビューID
	 * @param iconImageViewId アイコン表示用イメージビューID
	 * @param list
	 * @param listener
	 */
	public BluetoothDeviceInfoRecyclerAdapter(
		@LayoutRes final int itemLayoutResId,
		@IdRes final int nameTextViewId,
		@IdRes final int addressTextViewId,
		@IdRes final int iconImageViewId,
		@Nullable final List<BluetoothDeviceInfo> list,
		@Nullable final OnItemClickListener listener) {

		mItemLayoutResId = itemLayoutResId;
		mNameTvId = nameTextViewId;
		mAddressTvId = addressTextViewId;
		mIconIvId = iconImageViewId;
		mValues = list != null ? list : new ArrayList<>();
		mListener = listener;
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
		final View view = LayoutInflater.from(parent.getContext())
			.inflate(mItemLayoutResId, parent, false);
		return new ViewHolder(this, view);
	}
	
	@SuppressLint("RecyclerView")
	@Override
	public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
		holder.position = position;
		holder.mItem = mValues.get(position);
		if (holder.addressTv != null) {
			holder.addressTv.setText(mValues.get(position).address);
		}
		if (holder.nameTv != null) {
			holder.nameTv.setText(mValues.get(position).name);
		}
//		if (holder.icon != null) {
//			// FIXME 接続状態によるアイコンの変更は未実装
//			holder.icon.setImageResource(item.isPaired() ? R.mipmap.ic_paired : R.mipmap.ic_not_paired);
//		}
		
		holder.itemView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mListener != null) {
					// Notify the active callbacks interface (the activity, if the
					// fragment is attached to one) that an item has been selected.
					mListener.onClick(holder.position, holder.mItem);
				}
			}
		});
	}
	
	@Override
	public int getItemCount() {
		return mValues.size();
	}
	
	public BluetoothDeviceInfo getItem(final int index)
		throws IndexOutOfBoundsException {

		return mValues.get(index);
	}

	public void add(@NonNull BluetoothDeviceInfo info) {
		mValues.add(info);
		notifyItemInserted(mValues.size() - 1);
	}

	public void add(final int index, @NonNull BluetoothDeviceInfo info) {
		mValues.add(index, info);
		notifyItemInserted(index);
	}

	public void addAll(@NonNull Collection<? extends BluetoothDeviceInfo> collection) {
		if (!collection.isEmpty()) {
			final int index = mValues.size() - 1;
			mValues.addAll(collection);
			notifyItemRangeInserted(index + 1, index + collection.size());
		}
	}
	
	public void remove(final BluetoothDeviceInfo info) {
		final int index = mValues.indexOf(info);
		if (index >= 0) {
			mValues.remove(info);
			notifyItemRemoved(index);
		}
	}

	public void remove(final int index) {
		if (index >= 0) {
			mValues.remove(index);
			notifyItemRemoved(index);
		}
	}
	
	@SuppressLint("NotifyDataSetChanged")
	public void removeAll(@NonNull Collection<? extends BluetoothDeviceInfo> collection) {
		mValues.removeAll(collection);
		notifyDataSetChanged();
	}

	@SuppressLint("NotifyDataSetChanged")
	public void retainAll(@NonNull Collection<? extends BluetoothDeviceInfo> collection) {
		mValues.retainAll(collection);
		notifyDataSetChanged();
	}

	public void clear() {
		final int last = mValues.size() - 1;
		if (last >= 0) {
			mValues.clear();
			notifyItemRangeRemoved(0, last);
		}
	}

	@SuppressLint("NotifyDataSetChanged")
	public void sort(final Comparator<? super BluetoothDeviceInfo> comparator) {
		Collections.sort(mValues, comparator);
		notifyDataSetChanged();
	}

	public static class ViewHolder extends RecyclerView.ViewHolder {
		public final ImageView icon;
		public final TextView nameTv;
		public final TextView addressTv;
		public BluetoothDeviceInfo mItem;
		public int position;
		
		public ViewHolder(
			@NonNull final BluetoothDeviceInfoRecyclerAdapter parent,
			@NonNull final View view) {

			super(view);
			nameTv = view.findViewById(parent.mNameTvId);
			addressTv = view.findViewById(parent.mAddressTvId);
			icon = view.findViewById(parent.mIconIvId);
		}
		
	}
}
