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

import androidx.annotation.NonNull;
import androidx.databinding.BindingAdapter;
import androidx.databinding.BindingMethod;
import androidx.databinding.BindingMethods;
import androidx.databinding.InverseBindingListener;
import androidx.databinding.InverseBindingMethod;
import androidx.databinding.InverseBindingMethods;

/**
 * CheckableExインターフェースを継承したViewの双方向データバインディング用バインディングアダプター実装
 */
@BindingMethods({
	@BindingMethod(type = CheckableImageButton.class, attribute = "android:onCheckedChanged", method = "setOnCheckedChangeListener"),
	@BindingMethod(type = CheckableImageView.class, attribute = "android:onCheckedChanged", method = "setOnCheckedChangeListener"),
	@BindingMethod(type = CheckableButton.class, attribute = "android:onCheckedChanged", method = "setOnCheckedChangeListener"),
	@BindingMethod(type = CheckableLinearLayout.class, attribute = "android:onCheckedChanged", method = "setOnCheckedChangeListener"),
	@BindingMethod(type = CheckableRelativeLayout.class, attribute = "android:onCheckedChanged", method = "setOnCheckedChangeListener"),
})
@InverseBindingMethods({
	@InverseBindingMethod(type = CheckableImageButton.class, attribute = "android:checked"),
	@InverseBindingMethod(type = CheckableImageView.class, attribute = "android:checked"),
	@InverseBindingMethod(type = CheckableButton.class, attribute = "android:checked"),
	@InverseBindingMethod(type = CheckableLinearLayout.class, attribute = "android:checked"),
	@InverseBindingMethod(type = CheckableRelativeLayout.class, attribute = "android:checked"),

	@InverseBindingMethod(type = CheckableImageButton.class, attribute = "android:checkable"),
	@InverseBindingMethod(type = CheckableImageView.class, attribute = "android:checkable"),
	@InverseBindingMethod(type = CheckableButton.class, attribute = "android:checkable"),
	@InverseBindingMethod(type = CheckableLinearLayout.class, attribute = "android:checkable"),
	@InverseBindingMethod(type = CheckableRelativeLayout.class, attribute = "android:checkable"),
})

public class CheckableExBindingAdapter {
	@BindingAdapter("android:checked")
	public static void setChecked(CheckableImageButton checkable, final boolean checked) {
		if (checkable.isChecked() != checked) {
			checkable.setChecked(checked);
		}
	}

	@BindingAdapter("android:checkable")
	public static void setCheckable(CheckableImageButton checkable, final boolean checked) {
		if (checkable.isCheckable() != checked) {
			checkable.setCheckable(checked);
		}
	}

	@BindingAdapter(value = {"android:onCheckedChanged", "android:checkedAttrChanged"},
		requireAll = false)
	public static void setListeners(
		@NonNull CheckableImageButton checkable,
		CheckableEx.OnCheckedChangeListener listener,
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

//--------------------------------------------------------------------------------
	@BindingAdapter("android:checked")
	public static void setChecked(CheckableImageView checkable, final boolean checked) {
		if (checkable.isChecked() != checked) {
			checkable.setChecked(checked);
		}
	}

	@BindingAdapter("android:checkable")
	public static void setCheckable(CheckableImageView checkable, final boolean checked) {
		if (checkable.isCheckable() != checked) {
			checkable.setCheckable(checked);
		}
	}

	@BindingAdapter(value = {"android:onCheckedChanged", "android:checkedAttrChanged"},
		requireAll = false)
	public static void setListeners(
		@NonNull CheckableImageView checkable,
		CheckableEx.OnCheckedChangeListener listener,
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

//--------------------------------------------------------------------------------
	@BindingAdapter("android:checked")
	public static void setChecked(CheckableButton checkable, final boolean checked) {
		if (checkable.isChecked() != checked) {
			checkable.setChecked(checked);
		}
	}

	@BindingAdapter("android:checkable")
	public static void setCheckable(CheckableButton checkable, final boolean checked) {
		if (checkable.isCheckable() != checked) {
			checkable.setCheckable(checked);
		}
	}

	@BindingAdapter(value = {"android:onCheckedChanged", "android:checkedAttrChanged"},
		requireAll = false)
	public static void setListeners(
		@NonNull CheckableButton checkable,
		CheckableEx.OnCheckedChangeListener listener,
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

	//--------------------------------------------------------------------------------

	@BindingAdapter("android:checked")
	public static void setChecked(CheckableLinearLayout checkable, final boolean checked) {
		if (checkable.isChecked() != checked) {
			checkable.setChecked(checked);
		}
	}

	@BindingAdapter("android:checkable")
	public static void setCheckable(CheckableLinearLayout checkable, final boolean checked) {
		if (checkable.isCheckable() != checked) {
			checkable.setCheckable(checked);
		}
	}

	@BindingAdapter(value = {"android:onCheckedChanged", "android:checkedAttrChanged"},
		requireAll = false)
	public static void setListeners(
		@NonNull CheckableLinearLayout checkable,
		CheckableEx.OnCheckedChangeListener listener,
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

//--------------------------------------------------------------------------------
	@BindingAdapter("android:checked")
	public static void setChecked(CheckableRelativeLayout checkable, final boolean checked) {
		if (checkable.isChecked() != checked) {
			checkable.setChecked(checked);
		}
	}

	@BindingAdapter("android:checkable")
	public static void setCheckable(CheckableRelativeLayout checkable, final boolean checked) {
		if (checkable.isCheckable() != checked) {
			checkable.setCheckable(checked);
		}
	}

	@BindingAdapter(value = {"android:onCheckedChanged", "android:checkedAttrChanged"},
		requireAll = false)
	public static void setListeners(
		@NonNull CheckableRelativeLayout checkable,
		CheckableEx.OnCheckedChangeListener listener,
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
