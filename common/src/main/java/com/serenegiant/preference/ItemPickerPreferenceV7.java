package com.serenegiant.preference;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2024 saki t_saki@serenegiant.com
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
import android.content.res.TypedArray;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.serenegiant.utils.TypedArrayUtils;
import com.serenegiant.widget.ItemPicker;
import com.serenegiant.widget.ItemPicker.OnChangedListener;

public final class ItemPickerPreferenceV7 extends Preference {
	private static final boolean DEBUG = false;	// 実働時はfalseにすること
	private static final String TAG = ItemPickerPreferenceV7.class.getSimpleName();

	private int preferenceValue;
	private int mMinValue = 1, mMaxValue = 100;
	private ItemPicker mItemPicker;

	public ItemPickerPreferenceV7(final Context context) {
		this(context, null);
	}

	public ItemPickerPreferenceV7(final Context context, final AttributeSet attrs) {
		this(context, attrs, TypedArrayUtils.getAttr(context, androidx.preference.R.attr.dialogPreferenceStyle,
			android.R.attr.dialogPreferenceStyle));
	}

	public ItemPickerPreferenceV7(final Context context, final AttributeSet attrs, final int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	public void onBindViewHolder(@NonNull final PreferenceViewHolder holder) {
		super.onBindViewHolder(holder);
		RelativeLayout parent = null;
		if (holder.itemView instanceof final ViewGroup group) {
			for (int i = group.getChildCount() - 1; i >= 0; i--) {
				final View v = group.getChildAt(i);
				if (v instanceof RelativeLayout) {
					parent = (RelativeLayout)v;
					break;
				}
			}
		}
		if (parent == null) {
			throw new RuntimeException("unexpected item view type");
		}
		// ItemPickerを生成
		mItemPicker = new ItemPicker(getContext());
		// summaryの下に挿入する
        final RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
        	RelativeLayout.LayoutParams.MATCH_PARENT,
        	RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.BELOW, android.R.id.summary);
        parent.addView(mItemPicker, params);

		mItemPicker.setRange(mMinValue, mMaxValue);
		mItemPicker.setValue(preferenceValue);
		preferenceValue = mItemPicker.getValue();
		persistInt(preferenceValue);
		mItemPicker.setOnChangeListener(mOnChangeListener);
	}

	@Override
	protected Object onGetDefaultValue(final TypedArray a, final int index) {
//		if (DEBUG) Log.v(TAG, "onGetDefaultValue:");
//		return super.onGetDefaultValue(a, index);
		return a.getInt(index, 0);
	}

//	@Override
//	protected void onSetInitialValue(final boolean restorePersistedValue, final Object defaultValue) {
////		if (DEBUG) Log.v(TAG, "onSetInitialValue:");
//		int def = preferenceValue;
//		if (defaultValue instanceof Integer) {
//			def = (Integer)defaultValue;
//		} else if (defaultValue instanceof String) {
//			try {
//				def = Integer.parseInt((String)defaultValue);
//			} catch (final Exception e) {
//				if (DEBUG) Log.w(TAG, e);
//			}
//		}
//		if (restorePersistedValue) {
//			preferenceValue = getPersistedInt(def);
//		} else {
//			preferenceValue = def;
//			persistInt(preferenceValue);
//		}
//	}

	@Override
	protected void onSetInitialValue(final Object defaultValue) {
//		if (DEBUG) Log.v(TAG, "onSetInitialValue:");
		int def = preferenceValue;
		if (defaultValue instanceof Integer) {
			def = (Integer)defaultValue;
		} else if (defaultValue instanceof String) {
			try {
				def = Integer.parseInt((String)defaultValue);
			} catch (final Exception e) {
				if (DEBUG) Log.w(TAG, e);
			}
		}
		preferenceValue = def;
		persistInt(preferenceValue);
	}

	private final OnChangedListener mOnChangeListener = new OnChangedListener() {
		@Override
		public void onChanged(final ItemPicker picker, final int oldVal, final int newVal) {
//			if (DEBUG) Log.v(TAG, "onChanged:");
			callChangeListener(newVal);
			preferenceValue = newVal;
    		persistInt(preferenceValue);
		}
	};

	public void setRange(int min, int max) {
//		if (DEBUG) Log.v(TAG, "setRange:");
		if (min > max) {
			final int w = min;
			min = max;
			max = w;
		}
		if ((mMinValue != min) || (mMaxValue != max) ) {
			mMaxValue = max;
			mMinValue = min;
			if (mItemPicker != null) {
				mItemPicker.setRange(mMinValue, mMaxValue);
				mItemPicker.setValue(preferenceValue);
				preferenceValue = mItemPicker.getValue();
				persistInt(preferenceValue);
			}
		}
	}
}
