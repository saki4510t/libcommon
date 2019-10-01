package com.serenegiant.widget;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2019 saki t_saki@serenegiant.com
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
import android.graphics.drawable.Drawable;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;

public class ViewHelper {
	/**
	 * 指定したViewGroupとその配下のViewに背景色を指定
	 * TextInputEditTextは思った通りには透過してくれない
	 * @param vg
	 * @param color
	 */
	public static void setBackgroundAll(final ViewGroup vg, final int color) {
		for (int i = 0, count = vg.getChildCount(); i < count; i++) {
			final View child = vg.getChildAt(i);
			child.setBackgroundColor(color);
			if (child instanceof ViewGroup) {
				setBackgroundAll((ViewGroup) child, color);
			}
		}
	}

	/**
	 * 指定したViewGroupとその配下のViewに背景色を指定
	 * @param vg
	 * @param dr
	 */
	public static void setBackgroundAll(final ViewGroup vg, final Drawable dr) {
		for (int i = 0, count = vg.getChildCount(); i < count; i++) {
			final View child = vg.getChildAt(i);
			child.setBackground(dr);
			if (child instanceof ViewGroup) {
				setBackgroundAll((ViewGroup) child, dr);
			}
		}
	}

	/**
	 * 指定したテーマ用のLayoutInflaterを生成する
	 * @param context
	 * @param inflater
	 * @param themeRes
	 * @return
	 */
	@NonNull
	public static LayoutInflater createCustomLayoutInflater(
		@NonNull final Context context, @NonNull final LayoutInflater inflater,
		@StyleRes final int themeRes) {

		// フラグメントにテーマを割り当てる時は元のContext(Activity)を継承して
		// カスタムテーマを持つContextThemeWrapperを生成する
		final Context wrappedContext = new ContextThemeWrapper(context, themeRes);
		// ついでそのContextThemeWrapperを使ってinflaterを複製する
		return inflater.cloneInContext(wrappedContext);
	}
}
