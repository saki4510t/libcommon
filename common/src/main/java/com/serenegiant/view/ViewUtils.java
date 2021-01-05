package com.serenegiant.view;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2021 saki t_saki@serenegiant.com
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
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.serenegiant.common.R;
import com.serenegiant.system.BuildCheck;
import com.serenegiant.system.ContextUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import androidx.annotation.IdRes;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
	 * @param view
	 * @return 0, 90, 180, 270[度]
	 */
	public static int getRotationDegrees(@NonNull final View view) {
		return rotation2Degrees(getRotation(view));
	}

	/**
	 * 画面の回転状態を角度として取得
	 * @param context
	 * @return 0, 90, 180, 270[度]
	 */
	public static int getRotationDegrees(@NonNull final Context context) {
		return rotation2Degrees(getRotation(context));
	}

	/**
	 * 画面の回転状態から回転角度を取得
	 * @param rotation
	 * @return
	 */
	private static int rotation2Degrees(@Rotation final int rotation) {
		final int degrees;
		switch (rotation) {
		case Surface.ROTATION_90:
			degrees = 90;
			break;
		case Surface.ROTATION_180:
			degrees = 180;
			break;
		case Surface.ROTATION_270:
			degrees = 270;
			break;
		case Surface.ROTATION_0:
		default:
			degrees = 0;
			break;
		}
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
//--------------------------------------------------------------------------------
	/**
	 * 振動しないようにするためのあそび
	 */
	public static final float EPS = 0.1f;
	/**
	 * ラジアンを度に変換するための係数(== (1.0f / Math.PI) * 180.0f;)
	 */
	public static final float TO_DEGREE = 57.2957795130823f;

	public static final float dotProduct(final float x0, final float y0, final float x1, final float y1) {
		return x0 * x1 + y0 * y1;
	}

	public static final float crossProduct(final float x0, final float y0, final float x1, final float y1) {
		return x0 * y1 - x1 * y0;
	}

	public static final float crossProduct(final Vector v1, final Vector v2) {
		return v1.x * v2.y - v2.x * v1.y;
	}

	private static final Vector sPtInPoly_v1 = new Vector();
	private static final Vector sPtInPoly_v2 = new Vector();
	/**
	 * check whether the point is in the clockwise 2D polygon
	 * @param x
	 * @param y
	 * @param poly: the array of polygon coordinates(x,y pairs)
	 * @return
	 */
	public static synchronized boolean ptInPoly(final float x, final float y, final float[] poly) {

		final int n = poly.length & 0x7fffffff;
		// minimum 3 points(3 pair of x/y coordinates) need to calculate >> length >= 6
		if (n < 6) return false;
		boolean result = true;
		for (int i = 0; i < n; i += 2) {
			sPtInPoly_v1.set(x, y).dec(poly[i], poly[i + 1]);
			if (i + 2 < n) sPtInPoly_v2.set(poly[i + 2], poly[i + 3]);
			else sPtInPoly_v2.set(poly[0], poly[1]);
			sPtInPoly_v2.dec(poly[i], poly[i + 1]);
			if (crossProduct(sPtInPoly_v1, sPtInPoly_v2) > 0) {
				if (DEBUG) Log.v(TAG, "pt is outside of a polygon:");
				result = false;
				break;
			}
		}
		return result;
	}

	/**
	 * helper for intersection check etc.
	 */
	public static final class Vector {

		public float x, y;

		public Vector() {
		}

/*		public Vector(Vector src) {
			set(src);
		} */

		public Vector(final float x, final float y) {
			set(x, y);
		}

		public Vector set(final float x, final float y) {
			this.x = x;
			this.y = y;
			return this;
		}

/*		public Vector set(final Vector other) {
			x = other.x;
			y = other.y;
			return this;
		} */

/*		public Vector add(final Vector other) {
			return new Vector(x + other.x, y + other.y);
		} */

/*		public Vector add(final float x, final float y) {
			return new Vector(this.x + x, this.y + y);
		} */

/*		public Vector inc(final Vector other) {
			x += other.x;
			y += other.y;
			return this;
		} */

/*		public Vector inc(final float x, final float y) {
			this.x += x;
			this.y += y;
			return this;
		} */

		public Vector sub(final Vector other) {
			return new Vector(x - other.x, y - other.y);
		}

/*		public Vector sub(final float x, final float y) {
			return new Vector(this.x - x, this.y - y);
		} */

/*		public Vector dec(final Vector other) {
			x -= other.x;
			y -= other.y;
			return this;
		} */

		public Vector dec(final float x, final float y) {
			this.x -= x;
			this.y -= y;
			return this;
		}
	}

	public static final class LineSegment {

		public final Vector p1;
		public final Vector p2;

		public LineSegment (final float x0, final float y0, final float x1, final float y1) {
			p1 = new Vector(x0, y0);
			p2 = new Vector(x1, y1);
		}

		public LineSegment set(final float x0, final float y0, final float x1, final float y1) {
			p1.set(x0, y0);
			p2.set(x1,  y1);
			return this;
		}

/*		@Override
		public String toString() {
			return String.format(Locale.US, "p1=(%f,%f),p2=(%f,%f)", p1.x, p1.y, p2.x, p2.y);
		} */
	}

	/**
	 * check whether line segment(seg) intersects with at least one of line segments in the array
	 * @param seg
	 * @param segs array of segment
	 * @return true if line segment intersects with at least one of other line segment.
	 */
	public static final boolean checkIntersect(final LineSegment seg, final LineSegment[] segs) {

		boolean result = false;
		final int n = segs != null ? segs.length : 0;

		final Vector a = seg.p2.sub(seg.p1);
		Vector b, c, d;
		for (int i= 0; i < n; i++) {
			c = segs[i].p1.sub(seg.p1);
			d = segs[i].p2.sub(seg.p1);
			result = crossProduct(a, c) * crossProduct(a, d) < EPS;
			if (result) {
				b = segs[i].p2.sub(segs[i].p1);
				c = seg.p1.sub(segs[i].p1);
				d = seg.p2.sub(segs[i].p1);
				result = crossProduct(b, c) * crossProduct(b, d) < EPS;
				if (result) {
					break;
				}
			}
		}
		return result;
	}


//--------------------------------------------------------------------------------
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
			if (id == View.NO_ID) continue;;
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
