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

import androidx.annotation.NonNull;
import androidx.databinding.BindingAdapter;
import androidx.databinding.BindingMethod;
import androidx.databinding.BindingMethods;
import androidx.databinding.InverseBindingAdapter;
import androidx.databinding.InverseBindingListener;
import androidx.databinding.InverseBindingMethod;
import androidx.databinding.InverseBindingMethods;

/*
 * FIXME CheckableImageButtonのandroid:checkedへの双方向データバインディングがビルド通らない
 *       片方向データバインディングならこのファイル要らない
 *
 * Cannot find a getter for <com.serenegiant.widget.CheckableImageButton android:checked> that accepts parameter type 'java.lang.Boolean'
 * If a binding adapter provides the getter, check that the adapter is annotated correctly and that the parameter type matches.
 *
 * FIXME CheckableImageButtonのandroid:onCheckChangedへOnCheckedChangeListenerを割り当てるとビルドが通らない
 * Cannot find a setter for <com.serenegiant.widget.CheckableImageButton android:onCheckedChanged> that accepts parameter type 'com.serenegiant.libcommon.CheckableExFragment.ViewModel'
 * If a binding adapter provides the setter, check that the adapter is annotated correctly and that the parameter type matches.
*/

@BindingMethods({
	@BindingMethod(type = CheckableEx.class, attribute = "android:onCheckedChanged", method = "setOnCheckedChangeListener"),
	@BindingMethod(type = CheckableImageButton.class, attribute = "android:onCheckedChanged", method = "setOnCheckedChangeListener"),
})
@InverseBindingMethods({
	@InverseBindingMethod(type = CheckableEx.class, attribute = "android:checked"),
	@InverseBindingMethod(type = CheckableImageButton.class, attribute = "android:checked"),
})

public class CheckableExBindingAdapter {
	@BindingAdapter("android:checked")
	public static void setChecked(@NonNull final CheckableEx checkable, final boolean checked) {
		if (checkable.isChecked() != checked) {
			checkable.setChecked(checked);
		}
	}

	@InverseBindingAdapter(attribute = "android:checked")
	public static Boolean getChecked(@NonNull final CheckableEx checkable) {
		return checkable.getChecked();
	}

	@BindingAdapter(value = {"android:onCheckedChanged", "android:checkedAttrChanged"},
		requireAll = false)
	public static void setListeners(
	    @NonNull final CheckableEx checkable,
	    final CheckableEx.OnCheckedChangeListener listener,
		final InverseBindingListener attrChange) {
		if (attrChange == null) {
			checkable.setOnCheckedChangeListener(listener);
		} else {
			checkable.setOnCheckedChangeListener(new CheckableEx.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(@NonNull final CheckableEx checkable, boolean isChecked) {
					if (listener != null) {
						listener.onCheckedChanged(checkable, isChecked);
					}
					attrChange.onChange();
				}
			});
		}
	}

//--------------------------------------------------------------------------------
	@BindingAdapter("android:checked")
	public static void setChecked(CheckableImageButton checkable, final boolean checked) {
		if (checkable.isChecked() != checked) {
			checkable.setChecked(checked);
		}
	}

	@InverseBindingAdapter(attribute = "android:checked")
	public static boolean getChecked(CheckableImageButton checkable) {
		return checkable.getChecked();
	}

	@BindingAdapter(value = {"android:onCheckedChanged", "android:checkedAttrChanged"},
		requireAll = false)
	public static void setListeners(
	    @NonNull CheckableImageButton checkable,
	    CheckableImageButton.OnCheckedChangeListener listener,
		InverseBindingListener attrChange) {
		if (attrChange == null) {
			checkable.setOnCheckedChangeListener(listener);
		} else {
			checkable.setOnCheckedChangeListener(new CheckableEx.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(@NonNull CheckableEx checkable, boolean isChecked) {
					if (listener != null) {
						listener.onCheckedChanged(checkable, isChecked);
					}
					attrChange.onChange();
				}
			});
		}
	}
}
