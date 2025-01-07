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

import android.os.Parcel;
import android.os.Parcelable;
import android.widget.Checkable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.customview.view.AbsSavedState;

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

	public void setOnCheckedChangeListener(
		@Nullable final OnCheckedChangeListener listener);

	/**
	 * 状態を保存復帰するためのAbsSavedState実装
	 */
	static class SavedState extends AbsSavedState {
		boolean checked;
		boolean checkable;

		public SavedState(@NonNull final Parcelable superState) {
			super(superState);
		}

		public SavedState(@NonNull final Parcel src, final ClassLoader loader) {
			super(src, loader);
			readFromParcel(src);
		}

		@Override
		public void writeToParcel(@NonNull final Parcel dst, final int flags) {
			super.writeToParcel(dst, flags);
			dst.writeInt(checked ? 1 : 0);
			dst.writeInt(checkable ? 1 : 0);
		}

		private void readFromParcel(@NonNull final Parcel src) {
			checked = src.readInt() == 1;
			checkable = src.readInt() == 1;
		}

		public static final Creator<SavedState> CREATOR =
			new ClassLoaderCreator<SavedState>() {
				@NonNull
				@Override
				public SavedState createFromParcel(@NonNull final Parcel src, final ClassLoader loader) {
					return new SavedState(src, loader);
				}

				@NonNull
				@Override
				public SavedState createFromParcel(@NonNull final Parcel src) {
					return new SavedState(src, null);
				}

				@NonNull
				@Override
				public SavedState[] newArray(final int size) {
					return new SavedState[size];
				}
			};
	}

}
