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

import android.widget.Checkable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface CheckableEx extends Checkable {
	static final int[] CHECKED_STATE_SET = { android.R.attr.state_checked };

	public static interface OnCheckedChangeListener {
		/**
		 * Called when the checked state of a compound button has changed.
		 *
		 * @param checkable The compound button view whose state has changed.
		 * @param isChecked  The new checked state of buttonView.
		 */
		void onCheckedChanged(@NonNull final CheckableEx checkable, final boolean isChecked);
	}

	public void setCheckable(final boolean checkable);
	public boolean isCheckable();

	/**
	 * CheckableにsetCheckedに対応するゲッターがないと双方向データバインディング時に
	 * 余分に手間がかかるので#getCheckedで#isCheckedを呼び出すように追加
	 * @return
	 */
	public boolean getChecked();

	public void setOnCheckedChangeListener(
		@Nullable final OnCheckedChangeListener listener);

}
