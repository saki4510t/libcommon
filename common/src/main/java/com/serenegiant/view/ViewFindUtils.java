package com.serenegiant.view;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2026 saki t_saki@serenegiant.com
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

import android.view.View;
import android.view.ViewParent;
import android.widget.ImageView;
import android.widget.TextView;

import com.serenegiant.common.R;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ViewFindUtils {
	private ViewFindUtils() {
		// インスタンス化を防止するためデフォルトコンストラクタをprivateに
	}

	@IdRes
	private static final int[] ICON_IDS = {
		R.id.thumbnail,
		android.R.id.icon,
		R.id.icon,
		R.id.image,
	};

	@IdRes
	private static final int[] TITLE_IDS = {
		R.id.title,
		R.id.content,
		android.R.id.title,
		android.R.id.text1,
		android.R.id.text2,
	};

	/**
	 * サムネイル・アイコン表示用にImageViewを探す
	 * id = R.id.thumbnail, android.R.id.icon, R.id.icon, R.id.image
	 * @param view
	 * @return
	 */
	@Deprecated
	@Nullable
	public static ImageView findIconView(@NonNull final View view) {
		return findView(view, ICON_IDS, ImageView.class);
	}

	/**
	 * サムネイル・アイコン表示用にImageViewを探す
	 * @param view
	 * @param ids
	 * @return
	 */
	@Nullable
	public static ImageView findIconView(
		@NonNull final View view,
		@NonNull @IdRes final int[] ids) {

		return findView(view, ids, ImageView.class);
	}

	/**
	 * タイトル表示用にTextViewを探す
	 * id = android.R.id.title, R.id.title
	 * @param view
	 * @return
	 */
	@Deprecated
	@Nullable
	public static TextView findTitleView(@NonNull final View view) {
		return findView(view, TITLE_IDS, TextView.class);
	}

	/**
	 * タイトル表示用にTextViewを探す
	 * @param view
	 * @param ids
	 * @return
	 */
	@Nullable
	public static TextView findTitleView(
		@NonNull final View view,
		@NonNull @IdRes final int[] ids) {

		return findView(view, ids, TextView.class);
	}

	/**
	 * 指定したViewから指定したidで指定した型のViewを探す
	 * @param view
	 * @param ids
	 * @param clazz
	 * @param <T>
	 * @return
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	public static <T extends View> T findView(
		@NonNull final View view,
		@NonNull @IdRes final int[] ids,
		@NonNull final Class<T> clazz) {

		T result = null;
		if (clazz.isInstance(view)) {
			result = (T) view;
		} else {
			for (final int id: ids) {
				final View v = view.findViewById(id);
				if (clazz.isInstance(v)) {
					result = (T)v;
					break;
				}
			}
		}

		return result;
	}

	/**
	 * Viewまたは親Viewから指定したidで指定した型のViewを探す
	 * @param view
	 * @param ids
	 * @param clazz
	 * @param <T>
	 * @return
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	public static <T extends View> T findViewInParent(
		@NonNull final View view,
		@NonNull @IdRes final int[] ids,
		@NonNull final Class<T> clazz) {

		T result = null;
LOOP:	for (final int id: ids) {
			if (id == View.NO_ID) continue;
			final View v = view.findViewById(id);
			if (clazz.isInstance(v)) {
				result = (T)v;
				break LOOP;
			}
			if (result == null) {
				ViewParent parent = view.getParent();
				for (; (parent != null) && (result == null); parent = parent.getParent()) {
					if (parent instanceof View) {
						final View vv = ((View)parent).findViewById(id);
						if (clazz.isInstance(vv)) {
							result = (T)vv;
							break LOOP;
						}
					}
				}
			}
		}

		return result;
	}
}
