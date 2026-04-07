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

import android.content.Context;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by saki on 16/08/31.
 * Bluetooth機器を一覧表示するためのadapter
 */
public class BluetoothDeviceInfoAdapter extends ArrayAdapter<BluetoothDeviceInfo> {
	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = BluetoothDeviceInfoAdapter.class.getSimpleName();

	private final LayoutInflater mInflater;
	/** 項目表示用レイアウトリソースID */
	@LayoutRes
	private final int mItemLayoutResId;
	@IdRes
	private final int mNameTvId;
	@IdRes
	private final int mAddressTvId;
	@IdRes
	private final int mIconIvId;

	public BluetoothDeviceInfoAdapter(
		@NonNull final Context context, @LayoutRes final int itemLayoutResId) {
		this(context, itemLayoutResId, new ArrayList<>());
	}

	public BluetoothDeviceInfoAdapter(
		@NonNull final Context context,
		@LayoutRes final int itemLayoutResId,
		@NonNull final BluetoothDeviceInfo[] objects) {
		this(context, itemLayoutResId, Arrays.asList(objects));
	}

	public BluetoothDeviceInfoAdapter(
		@NonNull final Context context,
		@LayoutRes final int itemLayoutResId,
		@NonNull final List<BluetoothDeviceInfo> objects) {

		this(context, itemLayoutResId, R.id.name, R.id.address, R.id.icon, objects);
	}

	/**
	 * コンストラクタ
	 * @param context
	 * @param itemLayoutResId 項目表示用レイアウトリソースID
	 * @param nameTextViewId Bluetooth機器名表示用テキストビューID
	 * @param addressTextViewId Bluetoothアドレス表示用テキストビューID
	 * @param iconImageViewId アイコン表示用イメージビューID
	 * @param objects
	 */
	public BluetoothDeviceInfoAdapter(
		@NonNull final Context context,
		@LayoutRes final int itemLayoutResId,
		@IdRes final int nameTextViewId,
		@IdRes final int addressTextViewId,
		@IdRes final int iconImageViewId,
		@NonNull final List<BluetoothDeviceInfo> objects) {

		super(context, itemLayoutResId, objects);
		mInflater = LayoutInflater.from(context);
		mItemLayoutResId = itemLayoutResId;
		mNameTvId = nameTextViewId;
		mAddressTvId = addressTextViewId;
		mIconIvId = iconImageViewId;
	}

	@NonNull
	@Override
	public View getView(final int position, final View convertView, @NonNull final ViewGroup parent) {
		View rootView = convertView;
		if (rootView == null) {
			final TextView label;
			rootView = mInflater.inflate(mItemLayoutResId, parent, false);
			final ViewHolder holder = new ViewHolder();
			holder.nameTv = rootView.findViewById(mNameTvId);
			holder.addressTv = rootView.findViewById(mAddressTvId);
			holder.icon = rootView.findViewById(mIconIvId);
			rootView.setTag(holder);
		}
		final ViewHolder holder = (ViewHolder)rootView.getTag();
		// 指定行のデータを取得
		try {
			final BluetoothDeviceInfo item = getItem(position);
			if (item != null) {
				if (holder.nameTv != null) {
					holder.nameTv.setText(item.name);
				}
				if (holder.addressTv != null) {
					holder.addressTv.setText(item.address);
				}
//				if (holder.icon != null) {
//					// FIXME 接続状態によるアイコンの変更は未実装
//					holder.icon.setImageResource(item.isPaired() ? R.mipmap.ic_paired : R.mipmap.ic_not_paired);
//				}
			}
		} catch (final Exception e) {
			Log.w(TAG, e);
		}
     	return rootView;
	}

	/**
	 * Viewをリサイクルしやすくするためのヘルパークラス
	 */
	private static class ViewHolder {
		ImageView icon;
		TextView nameTv;
		TextView addressTv;
	}

}
