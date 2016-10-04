package com.serenegiant.widget;
/*
 * Androusb
 * Copyright (c) 2014-2016 saki t_saki@serenegiant.com
 * Distributed under the terms of the GNU Lesser General Public License (LGPL v3.0) License.
 * License details are in the file license.txt, distributed as part of this software.
 */

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Checkable;
import android.widget.LinearLayout;

public class CheckableLinearLayout extends LinearLayout implements Checkable {

//	private static final boolean DEBUG = false;	// FIXME 実働時にはfalseにすること
//	private static final String TAG = "CheckableLinearLayout";

	private boolean mChecked;

    private static final int[] CHECKED_STATE_SET = { android.R.attr.state_checked };

	public CheckableLinearLayout(final Context context) {
		this(context, null);
	}

	public CheckableLinearLayout(final Context context, final AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public CheckableLinearLayout(final Context context, final AttributeSet attrs, final int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@Override
	public boolean isChecked() {
		return mChecked;
	}

	@Override
	public void setChecked(final boolean checked) {
//		if (DEBUG) Log.v(TAG, "setChecked:" + checked);
		if (mChecked != checked) {
			mChecked = checked;
            final int n = this.getChildCount();
            View v;
            for (int i = 0; i < n; i++) {
            	v = this.getChildAt(i);
            	if (v instanceof Checkable)
            		((Checkable)v).setChecked(checked);
            }
            refreshDrawableState();
        }
	}

	@Override
	public void toggle() {
		setChecked(!mChecked);
	}

	@Override
    public int[] onCreateDrawableState(final int extraSpace) {
        final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
        if (isChecked()) {
            mergeDrawableStates(drawableState, CHECKED_STATE_SET);
        }
        return drawableState;
    }

}
