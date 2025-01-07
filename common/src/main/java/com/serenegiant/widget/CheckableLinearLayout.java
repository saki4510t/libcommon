package com.serenegiant.widget;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2025 saki t_saki@serenegiant.com
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
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Checkable;
import android.widget.LinearLayout;

import com.serenegiant.common.R;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class CheckableLinearLayout extends LinearLayout implements CheckableEx, Touchable {
	private static final boolean DEBUG = false; // 実同時はfalseにすること
	private static final String TAG = CheckableLinearLayout.class.getSimpleName();

	private boolean mIsChecked;
	private boolean mCheckable = true;
	@Nullable
	private OnCheckedChangeListener mListener;

	public CheckableLinearLayout(@NonNull final Context context) {
		this(context, null);
	}

	public CheckableLinearLayout(@NonNull final Context context, @Nullable final AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public CheckableLinearLayout(@NonNull final Context context, @Nullable final AttributeSet attrs, final int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		final TypedArray a = context.getTheme().obtainStyledAttributes(
			attrs, R.styleable.CheckableLinearLayout, defStyleAttr, 0);
		mCheckable = a.getBoolean(R.styleable.CheckableLinearLayout_android_checkable, mCheckable);
		final boolean clickable = a.getBoolean(R.styleable.CheckableLinearLayout_android_clickable, false);
		final boolean focusable = a.getBoolean(R.styleable.CheckableLinearLayout_android_focusable, false);
		a.recycle();
		setClickable(clickable);
		setFocusable(focusable);
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
//		if (DEBUG) Log.v(TAG, "setChecked:" + checked);
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

	protected void updateChildState(@NonNull final ViewGroup group, final boolean checked) {
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
	public boolean performClick() {
		if (DEBUG) Log.v(TAG, "performClick:isClickable=" + isClickable() + ",isCheckable=" + isCheckable());
		if (mCheckable) {
			toggle();
		}

		final boolean handled = super.performClick();
		if (!handled) {
			// View only makes a sound effect if the onClickListener was
			// called, so we'll need to make one here instead.
			playSoundEffect(SoundEffectConstants.CLICK);
		}

		return handled;
	}

	@Override
    public int[] onCreateDrawableState(final int extraSpace) {
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
	protected void onRestoreInstanceState(final Parcelable state) {
		if (!(state instanceof final SavedState savedState)) {
			super.onRestoreInstanceState(state);
			return;
		}
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
