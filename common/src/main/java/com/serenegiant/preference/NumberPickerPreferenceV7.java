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
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;

import com.serenegiant.common.R;
import com.serenegiant.utils.TypedArrayUtils;

import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.DialogPreference;

public class NumberPickerPreferenceV7 extends DialogPreference {
	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = NumberPickerPreferenceV7.class.getSimpleName();

	private int mDefaultValue;
	private final int mMinValue;
	private final int mMaxValue;
	private int mValue;

	public NumberPickerPreferenceV7(@NonNull final Context context) {
		this(context, null);
	}

	public NumberPickerPreferenceV7(@NonNull final Context context,
		@Nullable final AttributeSet attrs) {

		this(context, attrs, TypedArrayUtils.getAttr(context, R.attr.dialogPreferenceStyle,
			android.R.attr.dialogPreferenceStyle));
	}

	public NumberPickerPreferenceV7(@NonNull final Context context,
		@Nullable final AttributeSet attrs, final int defStyle) {

		super(context, attrs, defStyle);
		if (DEBUG) Log.v(TAG, "コンストラクタ:");

		final TypedArray a = context.getTheme().obtainStyledAttributes(
			attrs, R.styleable.NumberPicker, defStyle, 0);
		mDefaultValue = a.getInt(R.styleable.NumberPicker_DefaultValue, 0);
		final int min = a.getInt(R.styleable.NumberPicker_MinValue, 0);
		final int max = a.getInt(R.styleable.NumberPicker_MaxValue, 100);
        a.recycle();

		mMinValue = Math.min(min, max);
		mMaxValue = Math.max(min, max);
        setPositiveButtonText(android.R.string.ok);
		setNegativeButtonText(android.R.string.cancel);
	}

	@Override
	protected Object onGetDefaultValue(final TypedArray a, final int index) {
		return (a.getString(index));
	}

	@Override
	protected void onSetInitialValue(@Nullable final Object defaultValue) {

		if (defaultValue != null) {
			mDefaultValue = Integer.parseInt((String) defaultValue);
		}
		mValue = getPersistedInt(mDefaultValue);

		setSummary(getSummary());

		if (DEBUG) Log.v(TAG, "onSetInitialValue:" + mValue);
	}

	@Override
	public CharSequence getSummary() {
		return String.format(Locale.US, "%d", mValue);
	}

//--------------------------------------------------------------------------------

	public int getMinValue() {
		return mMinValue;
	}

	public int getMaxValue() {
		return mMaxValue;
	}

	public int getValue() {
		return mValue;
	}

	public void setValue(final int value) {
		final boolean changed = getValue() != value;
		if (changed) {
			mValue = value;
			persistInt(value);
			if (changed) {
				notifyChanged();
			}
		}
	}

}
