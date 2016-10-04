package com.serenegiant.widget;
/*
 * Androusb
 * Copyright (c) 2014-2016 saki t_saki@serenegiant.com
 * Distributed under the terms of the GNU Lesser General Public License (LGPL v3.0) License.
 * License details are in the file license.txt, distributed as part of this software.
 */

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Checkable;
import android.widget.RelativeLayout;

public class CheckableRelativeLayout extends RelativeLayout implements Checkable {

	private boolean mIsChecked;
	private static final int[] CHECKED_STATE_SET = { android.R.attr.state_checked };

	public CheckableRelativeLayout(final Context context) {
		this(context, null);
	}

	public CheckableRelativeLayout(final Context context, final AttributeSet attrs) {
		super(context, attrs);
	}


	@Override
	public boolean isChecked() {
		return mIsChecked;
	}

	@Override
	public void setChecked(final boolean checked) {
		if (mIsChecked != checked) {
			mIsChecked = checked;
			updateChildState(this, checked);
            refreshDrawableState();
        }
	}

	protected void updateChildState(final ViewGroup group, final boolean checked) {
		final int n = group.getChildCount();
		for (int i = 0; i < n; i++) {
			final View child = group.getChildAt(i);
			if (child instanceof Checkable) {
				((Checkable)child).setChecked(checked);
			}
		}
	}

	@Override
	public void toggle() {
		setChecked(!mIsChecked);
	}

	@Override
    protected int[] onCreateDrawableState(final int extraSpace) {
        final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
        if (isChecked()) {
            mergeDrawableStates(drawableState, CHECKED_STATE_SET);
        }
        return drawableState;
    }

}
