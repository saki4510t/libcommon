package com.serenegiant.widget;
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
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatImageButton;

public class CheckableImageButton extends AppCompatImageButton implements CheckableEx {
	private static final boolean DEBUG = false; // 実同時はfalseにすること
	private static final String TAG = CheckableImageButton.class.getSimpleName();

	private boolean mIsChecked;

	public CheckableImageButton(Context context) {
		this(context, null, 0);
	}

	public CheckableImageButton(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public CheckableImageButton(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@Override
	public void setChecked(boolean checked) {
		if (mIsChecked != checked) {
			mIsChecked = checked;
            refreshDrawableState();
        }
	}

	@Override
	public boolean isChecked() {
		return mIsChecked;
	}

	@Override
	public void toggle() {
		setChecked(!mIsChecked);
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
