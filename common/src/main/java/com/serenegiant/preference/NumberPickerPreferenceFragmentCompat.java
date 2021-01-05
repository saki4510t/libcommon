package com.serenegiant.preference;
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
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.NumberPicker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceDialogFragmentCompat;

public class NumberPickerPreferenceFragmentCompat extends PreferenceDialogFragmentCompat {
	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = NumberPickerPreferenceFragmentCompat.class.getSimpleName();

	private static final String SAVE_STATE_MIN_VALUE = "NumberPickerPreferenceFragment.minValue";
	private static final String SAVE_STATE_MAX_VALUE = "NumberPickerPreferenceFragment.maxValue";
	private static final String SAVE_STATE_VALUE = "NumberPickerPreferenceFragment.value";

	public static NumberPickerPreferenceFragmentCompat newInstance(final String key) {
		final NumberPickerPreferenceFragmentCompat fragment = new NumberPickerPreferenceFragmentCompat();
		final Bundle args = new Bundle(1);
		args.putString(ARG_KEY, key);
		fragment.setArguments(args);
		return fragment;
	}

	private int mMinValue;
	private int mMaxValue;
	private int mValue;
	private boolean changed;

	public NumberPickerPreferenceFragmentCompat() {
		super();
		// デフォルトコンストラクタが必要
		if (DEBUG) Log.v(TAG, "コンストラクタ:");
	}

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (DEBUG) Log.v(TAG, "onCreate:");
		if (savedInstanceState == null) {
			final NumberPickerPreferenceV7 preference = getNumberPickerPreference();

			mMinValue = preference.getMinValue();
			mMaxValue = preference.getMaxValue();
			mValue = preference.getValue();

		} else {
			mMinValue = savedInstanceState.getInt(SAVE_STATE_MIN_VALUE, 0);
			mMaxValue = savedInstanceState.getInt(SAVE_STATE_MAX_VALUE, 100);
			mValue = savedInstanceState.getInt(SAVE_STATE_VALUE, 0);
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		if (DEBUG) Log.v(TAG, "onSaveInstanceState:");
		outState.putInt(SAVE_STATE_MIN_VALUE, mMinValue);
		outState.putInt(SAVE_STATE_MAX_VALUE, mMaxValue);
		outState.putInt(SAVE_STATE_VALUE, mValue);
	}

	@Override
	public void onDialogClosed(final boolean positiveResult) {
		if (DEBUG) Log.v(TAG, "onDialogClosed:");
		final NumberPickerPreferenceV7 preference = getNumberPickerPreference();
		if (positiveResult || changed) {
			if (preference.callChangeListener(mValue)) {
				preference.setValue(mValue);
			}
		 }
	}

	@Override
	protected View onCreateDialogView(final Context context) {
		if (DEBUG) Log.v(TAG, "onCreateDialogView:");
		final NumberPicker picker = new NumberPicker(context);
		picker.setOnValueChangedListener(mOnValueChangeListener);
		picker.setMinValue(mMinValue);
		picker.setMaxValue(mMaxValue);
		picker.setValue(mValue);
		changed = false;
		return picker;
	}

	@Override
	protected void onPrepareDialogBuilder(final AlertDialog.Builder builder) {
		super.onPrepareDialogBuilder(builder);

		/*
		 * The typical interaction for list-based dialogs is to have
		 * click-on-an-item dismiss the dialog instead of the user having to
		 * press 'Ok'.
		 */
		builder.setPositiveButton(null, null);
	}

	protected NumberPickerPreferenceV7 getNumberPickerPreference() {
		return (NumberPickerPreferenceV7)getPreference();
	}

	private final NumberPicker.OnValueChangeListener mOnValueChangeListener
		= new NumberPicker.OnValueChangeListener() {
		@Override
		public void onValueChange(final NumberPicker picker, final int oldVal, final int newVal) {
			if (DEBUG) Log.v(TAG, "onValueChange:newVal=" + newVal);
			if (oldVal != newVal) {
				changed = true;
			}
			mValue = newVal;
		}
	};
}
