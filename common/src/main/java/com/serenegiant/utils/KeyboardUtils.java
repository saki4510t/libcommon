package com.serenegiant.utils;
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

import android.app.Activity;
import android.graphics.Rect;
import android.os.Build;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;

import com.serenegiant.system.ContextUtils;

import androidx.annotation.NonNull;

/**
 * ソフトウエアキーボードの表示状態等を操作するためのヘルパークラス
 */
public class KeyboardUtils {
	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = KeyboardUtils.class.getSimpleName();

	/**
	 * キーボードの表示/非表示イベントリスナー
	 */
	public interface OnKeyboardVisibilityChangedListener {
		public void onKeyboardVisibilityChanged(final boolean visible);
	}

	private KeyboardUtils() {
		// インスタンス化をエラーとするためにデフォルトコンストラクタをprivateにする
	}

	/**
	 * ソフトウエアキーボードを非表示にする
	 * @param view
	 */
	public static void hide(@NonNull View view) {
		final InputMethodManager imm = ContextUtils.requireSystemService(view.getContext(), InputMethodManager.class);
		view.clearFocus();
	    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
	}

	/**
	 * ソフトウエアキーボードを非表示にする
	 * @param activity
	 */
	public static void hide(@NonNull final Activity activity) {
		final InputMethodManager imm = ContextUtils.requireSystemService(activity, InputMethodManager.class);
		//Find the currently focused view, so we can grab the correct window token from it.
		View view = activity.getCurrentFocus();
		   // If no view currently has focus, create a new one, just so we can grab a window token from it
		if (view == null) {
	        view = new View(activity);
	    }
	    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
	}

	/**
	 * 指定したViewへキーボードの表示/非表示イベントリスナーをセットする
	 * https://stackoverflow.com/questions/4312319/how-to-capture-the-virtual-keyboard-show-hide-event-in-android
	 * @param view
	 * @param listener
	 * @return view
	 */
	public static View setOnKeyboardVisibilityChangedListener(
		@NonNull final View view,
		@NonNull final OnKeyboardVisibilityChangedListener listener) {

		view.getViewTreeObserver().addOnGlobalLayoutListener(
			new ViewTreeObserver.OnGlobalLayoutListener() {
				private boolean alreadyOpen;
				private final int defaultKeyboardHeightDP = 100;
				private final int EstimatedKeyboardDP = defaultKeyboardHeightDP + (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ? 48 : 0);
				private final Rect rect = new Rect();

				@Override
				public void onGlobalLayout() {
					final int estimatedKeyboardHeight
						= (int) TypedValue.applyDimension(
							TypedValue.COMPLEX_UNIT_DIP, EstimatedKeyboardDP,
							view.getResources().getDisplayMetrics());
					view.getWindowVisibleDisplayFrame(rect);
					final int heightDiff = view.getRootView().getHeight() - (rect.bottom - rect.top);
					final boolean isShown = heightDiff >= estimatedKeyboardHeight;

					if (isShown != alreadyOpen) {
						alreadyOpen = isShown;
						listener.onKeyboardVisibilityChanged(isShown);
					}
				}
			});
		return view;
	}
}
