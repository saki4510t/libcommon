package com.serenegiant.widget;
/*
 * Androusb
 * Copyright (c) 2014-2016 saki t_saki@serenegiant.com
 * Distributed under the terms of the GNU Lesser General Public License (LGPL v3.0) License.
 * License details are in the file license.txt, distributed as part of this software.
 */

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class ColorPickerDialogPreference extends DialogPreference {
//	private static final boolean DEBUG = false;	// FIXME 実働時はfalseにすること
	private static final String TAG = ColorPickerDialogPreference.class.getSimpleName();

	private int mColor = 0xffff0000;
	private boolean changed;

	public ColorPickerDialogPreference(final Context context) {
		this(context, null, 0);
	}

	public ColorPickerDialogPreference(final Context context, final AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public ColorPickerDialogPreference(final Context context, final AttributeSet attrs, final int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@Override
	protected void onBindView(final View view) {
		super.onBindView(view);
		mColor = getPersistedInt(mColor);
	}

	@Override
	protected View onCreateDialogView() {
		// ここでダイアログに表示するViewの生成と中身の設定をする
		final ColorPickerView view = new ColorPickerView(getContext());
		view.setColorPickerListener(mColorPickerListener);
		return view;
	}

	@Override
	protected void onBindDialogView(final View v) {
		super.onBindDialogView(v);
//		if (DEBUG) Log.v(TAG, "onBindDialogView:" + v);
		mColor = getPersistedInt(mColor);
		changed = false;
		if (v instanceof ColorPickerView) {
			((ColorPickerView)v).setColor(mColor);
		}
	}

	@Override
	protected void onDialogClosed(final boolean positiveResult) {
//		if (DEBUG) Log.v(TAG, "onDialogClosed:=" + positiveResult);
		if (positiveResult || changed) {
			setSummary(getSummary());
			if (callChangeListener(mColor)) {
				persistInt(mColor);
				notifyChanged();
			}
		}
		super.onDialogClosed(positiveResult || changed);
	}

	@Override
	protected void onSetInitialValue(final boolean restorePersistedValue, final Object defaultValue) {
		if (restorePersistedValue) {
			mColor = getPersistedInt(mColor);
		} else {
			mColor = (Integer)defaultValue;
			persistInt(mColor);
		}
	}

	private final ColorPickerView.ColorPickerListener
		mColorPickerListener = new ColorPickerView.ColorPickerListener() {
		@Override
		public void onColorChanged(final ColorPickerView colorPickerView, final int color) {
			if (mColor != color) {
				mColor = color;
				changed = true;
			}
		}
	};

	public int getValue() {
		return mColor;
	}
}
