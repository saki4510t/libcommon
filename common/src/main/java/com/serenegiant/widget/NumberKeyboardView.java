package com.serenegiant.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class NumberKeyboardView extends KeyboardView {
	public NumberKeyboardView(final Context context) {
		this(context, null, 0);
	}

	public NumberKeyboardView(final Context context, @Nullable final AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public NumberKeyboardView(final Context context, @Nullable final AttributeSet attrs, final int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@Override
	public void onClick(final View v) {

	}
}
