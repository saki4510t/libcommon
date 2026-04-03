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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.view.ContextThemeWrapper;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.serenegiant.system.BuildCheck;
import com.serenegiant.content.ContextUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;
import androidx.annotation.UiThread;

import static java.lang.annotation.RetentionPolicy.SOURCE;

public class ViewUtils {
	private static final boolean DEBUG = false;	// TODO for debugging
	private static final String TAG = ViewUtils.class.getSimpleName();

	private ViewUtils() {
		// インスタンス化をエラーにするためにデフォルトコンストラクタをprivateに
	}

	@IntDef({
		View.VISIBLE,
		View.INVISIBLE,
		View.GONE,
	})
	@Retention(SOURCE)
	public @interface Visibility {}

	/**
	 * Viewのリサイズ要求
	 * @param view
	 * @param width
	 * @param height
	 */
	@UiThread
	public static void requestResize(@NonNull final View view,
		final int width, final int height) {

		view.getLayoutParams().width = width;
		view.getLayoutParams().height = height;
		view.requestLayout();
	}

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
	 * @param themeRes
	 * @return
	 */
	public static LayoutInflater createCustomLayoutInflater(
		@NonNull final Context context,
		@StyleRes final int themeRes) {

		return createCustomLayoutInflater(context, LayoutInflater.from(context), themeRes);
	}

	/**
	 * 指定したテーマ用のLayoutInflaterを生成する
	 * @param inflater
	 * @param themeRes
	 * @return
	 */
	public static LayoutInflater createCustomLayoutInflater(
		@NonNull final LayoutInflater inflater,
		@StyleRes final int themeRes) {

		return createCustomLayoutInflater(inflater.getContext(), inflater, themeRes);
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

//--------------------------------------------------------------------------------
	@IntDef(value = {
		Surface.ROTATION_0,
		Surface.ROTATION_90,
		Surface.ROTATION_180,
		Surface.ROTATION_270
	})
	@Retention(RetentionPolicy.SOURCE)
	public @interface Rotation {}

	/**
	 * 画面の回転状態を取得
	 * 端末の物理的な向きではない。例えばスマホで縦画面固定にしていれば端末をどの方向に回転させても常にSurface.ROTATION_0
	 * @param view
	 * @return Surface.ROTATION_0, Surface.ROTATION_90, Surface.ROTATION_180, Surface.ROTATION_270のいずれか
	 */
	@SuppressLint("NewApi")
	@Rotation
	public static int getRotation(@NonNull final View view) {
		int rotation;
		if (BuildCheck.isAPI17()) {
			rotation = view.getDisplay().getRotation();
		} else {
			final Display display
				= ContextUtils.requireSystemService(view.getContext(), WindowManager.class)
					.getDefaultDisplay();
			rotation = display.getRotation();
		}
		return rotation;
	}

	/**
	 * 画面の回転状態を取得
	 * 端末の物理的な向きではない。例えばスマホで縦画面固定にしていれば端末をどの方向に回転させても常にSurface.ROTATION_0
	 * @param context
	 * @return Surface.ROTATION_0, Surface.ROTATION_90, Surface.ROTATION_180, Surface.ROTATION_270のいずれか
	 */
	@SuppressLint("NewApi")
	@Rotation
	public static int getRotation(@NonNull final Context context) {
		final Display display
			= ContextUtils.requireSystemService(context, WindowManager.class)
				.getDefaultDisplay();
		return display.getRotation();
	}

	/**
	 * 画面の回転状態を角度として取得
	 * 端末の物理的な向きではない。例えばスマホで縦画面固定にしていれば端末をどの方向に回転させても常に0[度]
	 * @param view
	 * @return 0, 90, 180, 270[度]
	 */
	public static int getRotationDegrees(@NonNull final View view) {
		return rotation2Degrees(getRotation(view));
	}

	/**
	 * 画面の回転状態を角度として取得
	 * 端末の物理的な向きではない。例えばスマホで縦画面固定にしていれば端末をどの方向に回転させても常に0[度]
	 * @param context
	 * @return 0, 90, 180, 270[度]
	 */
	public static int getRotationDegrees(@NonNull final Context context) {
		return rotation2Degrees(getRotation(context));
	}

	/**
	 * 画面の回転状態(Surface.ROTATION_XX)から回転角度を取得
	 * @param rotation
	 * @return 0, 90, 180, 270[度]
	 */
	public static int rotation2Degrees(@Rotation final int rotation) {
		final int degrees = switch (rotation) {
			case Surface.ROTATION_0 -> 0;
			case Surface.ROTATION_90 -> 90;
			case Surface.ROTATION_180 -> 180;
			case Surface.ROTATION_270 -> 270;
			default -> 0;
		};
		return degrees;
	}

	@IntDef({
		Configuration.ORIENTATION_PORTRAIT,
		Configuration.ORIENTATION_LANDSCAPE,
	})
	@Retention(SOURCE)
	public @interface Orientation {}

	/**
	 * 画面の向きを取得
	 * Resources#getConfiguration#orientationを返すだけ
	 * @param context
	 * @return
	 */
	@Orientation
	public static int getOrientation(@NonNull final Context context) {
		return context.getResources().getConfiguration().orientation;
	}

}
