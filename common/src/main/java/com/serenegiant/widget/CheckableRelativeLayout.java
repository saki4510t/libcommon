package com.serenegiant.widget;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2022 saki t_saki@serenegiant.com
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
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Checkable;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class CheckableRelativeLayout extends RelativeLayout implements CheckableEx, Touchable {

	private boolean mIsChecked;
	private boolean mCheckable = true;
	@Nullable
	private OnCheckedChangeListener mListener;

	public CheckableRelativeLayout(final Context context) {
		this(context, null);
	}

	public CheckableRelativeLayout(final Context context, final AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	public void setOnCheckedChangeListener(@Nullable final OnCheckedChangeListener listener) {
		synchronized (this) {
			mListener = listener;
		}
	}

	@Override
	public void setCheckable(final boolean checkable) {
		this.mCheckable = checkable;
	}

	@Override
	public boolean isCheckable() {
		return mCheckable;
	}

	@Override
	public boolean isChecked() {
		return mIsChecked;
	}

	@Override
	public void setChecked(final boolean checked) {
		if (mCheckable && (mIsChecked != checked)) {
			mIsChecked = checked;
			updateChildState(this, checked);
            refreshDrawableState();
			final OnCheckedChangeListener listener;
			synchronized (this) {
				listener = mListener;
			}
			if (listener != null) {
				listener.onCheckedChanged(this, checked);
			}
        }
	}

	@Override
	public boolean getChecked() {
		return isChecked();
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

	@NonNull
	@Override
	protected Parcelable onSaveInstanceState() {
		Parcelable superState = super.onSaveInstanceState();
		SavedState savedState = new SavedState(superState);
		savedState.checked = mIsChecked;
		savedState.checkable = mCheckable;
		return savedState;
	}

	@Override
	protected void onRestoreInstanceState(Parcelable state) {
		if (!(state instanceof SavedState)) {
			super.onRestoreInstanceState(state);
			return;
		}
		SavedState savedState = (SavedState) state;
		super.onRestoreInstanceState(savedState.getSuperState());
		mCheckable = savedState.checkable;
		setChecked(savedState.checked);
	}

	private float mTouchX, mTouchY;
	@Override
	public boolean onInterceptTouchEvent(final MotionEvent ev) {
		mTouchX = ev.getX();
		mTouchY = ev.getY();
		return super.onInterceptTouchEvent(ev);
	}

	@Override
	public float touchX() { return mTouchX; }
	@Override
	public float touchY() { return mTouchY; }
}
