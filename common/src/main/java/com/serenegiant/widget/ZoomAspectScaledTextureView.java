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
import android.util.AttributeSet;

/**
 * @deprecated TouchTransformTextureViewを使うこと
 */
@Deprecated
public class ZoomAspectScaledTextureView extends TouchTransformTextureView {
	public ZoomAspectScaledTextureView(final Context context) {
		super(context);
	}

	public ZoomAspectScaledTextureView(final Context context, final AttributeSet attrs) {
		super(context, attrs);
	}

	public ZoomAspectScaledTextureView(final Context context, final AttributeSet attrs, final int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}
}
