package com.serenegiant.widget;
/*
 * Androusb
 * Copyright (c) 2014-2016 saki t_saki@serenegiant.com
 * Distributed under the terms of the GNU Lesser General Public License (LGPL v3.0) License.
 * License details are in the file license.txt, distributed as part of this software.
 */

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

public class MultilineLabelPreference extends Preference {
	public MultilineLabelPreference(final Context context) {
		super(context);
	}

	public MultilineLabelPreference(final Context context, final AttributeSet attrs) {
		super(context, attrs);
	}

	public MultilineLabelPreference(final Context context, final AttributeSet attrs, final int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@Override
	protected void onBindView(final View view) {
		super.onBindView(view);
//		if (DEBUG) Log.w(TAG, "onBindView:");
/*		RelativeLayout parent = null;
		final ViewGroup group = (ViewGroup)view;
		for (int i = group.getChildCount() - 1; i >= 0; i--) {
			final View v = group.getChildAt(i);
			if (v instanceof RelativeLayout) {
				parent = (RelativeLayout)v;
				break;
			}
		} */
		try {
			final TextView summary = (TextView)view.findViewById(android.R.id.summary);
			summary.setSingleLine(false);
		} catch (final Exception e) {
		}
	}
}
