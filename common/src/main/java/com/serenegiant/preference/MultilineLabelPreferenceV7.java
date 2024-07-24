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

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.TextView;

import com.serenegiant.utils.TypedArrayUtils;

public class MultilineLabelPreferenceV7 extends Preference {
	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = MultilineLabelPreferenceV7.class.getSimpleName();

	public MultilineLabelPreferenceV7(final Context context) {
		this(context, null);
	}

	public MultilineLabelPreferenceV7(final Context context, final AttributeSet attrs) {
		this(context, attrs, TypedArrayUtils.getAttr(context, androidx.preference.R.attr.preferenceStyle,
			android.R.attr.preferenceStyle));
	}

	public MultilineLabelPreferenceV7(final Context context, final AttributeSet attrs, final int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@Override
	public void onBindViewHolder(@NonNull final PreferenceViewHolder holder) {
		super.onBindViewHolder(holder);
//		if (DEBUG) Log.w(TAG, "onBindViewHolder:");
		try {
			final TextView summary = (TextView)holder.findViewById(android.R.id.summary);
			summary.setSingleLine(false);
		} catch (final Exception e) {
			if (DEBUG) Log.w(TAG, e);
		}
	}
}
