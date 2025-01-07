package com.serenegiant.view;
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

import android.graphics.Rect;
import android.os.Build;
import android.view.MotionEvent;
import android.view.TouchDelegate;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

/**
 * 親ViewにTouchDelegateがセットされている場合に保存しておいて
 * 自分の処理のあとに元々のTouchDelegateを呼び出す処理を追加したTouchDelegate
 */
public class ChainedTouchDelegate extends TouchDelegate {

	/**
	 * コンストラクタ呼び出し時に親ViewにセットされていたTouchDelegate
	 */
	@Nullable
	private final TouchDelegate mParentTouchDelegate;

	/**
	 * コンストラクタ
	 * @param parent 親View
	 * @param target TouchDelegateの対象となるView
	 * @param bounds 処理するタッチ領域
	 */
	public ChainedTouchDelegate(@NonNull final View parent,
		final @NonNull View target, @NonNull  final Rect bounds) {
		super(bounds, target);

		mParentTouchDelegate = parent.getTouchDelegate();
		parent.setTouchDelegate(this);
	}

	@Override
	public boolean onTouchEvent(@NonNull final MotionEvent event) {
		boolean result = super.onTouchEvent(event);
		if (!result && (mParentTouchDelegate != null)) {
			result = mParentTouchDelegate.onTouchEvent(event);
		}
		return result;
	}

	@RequiresApi(api = Build.VERSION_CODES.Q)
	@Override
	public boolean onTouchExplorationHoverEvent(@NonNull final MotionEvent event) {
		boolean result = super.onTouchExplorationHoverEvent(event);
		if (!result && (mParentTouchDelegate != null)) {
			result = mParentTouchDelegate.onTouchExplorationHoverEvent(event);
		}
		return result;
	}
}
