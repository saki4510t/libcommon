package com.serenegiant.view;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2020 saki t_saki@serenegiant.com
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
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.lang.annotation.Retention;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;

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

	private static final ThreadLocal<Vector> sPtInPoly_v1 = new ThreadLocal<>();
	private static final ThreadLocal<Vector> sPtInPoly_v2 = new ThreadLocal<>();
	/**
	 * check whether the point is in the clockwise 2D polygon
	 * @param x
	 * @param y
	 * @param poly: the array of polygon coordinates(x,y pairs)
	 * @return
	 */
	public static boolean ptInPoly(final float x, final float y, @NonNull final float[] poly) {

		final int n = poly.length & 0x7fffffff;
		// minimum 3 points(3 pair of x/y coordinates) need to calculate >> length >= 6
		if (n < 6) {
			return false;
		}

		boolean result = true;
		final Vector v1 = sPtInPoly_v1.get();
		final Vector v2 = sPtInPoly_v2.get();
		for (int i = 0; i < n; i += 2) {
			v1.set(x, y).dec(poly[i], poly[i + 1]);
			if (i + 2 < n) {
				v2.set(poly[i + 2], poly[i + 3]);
			} else {
				v2.set(poly[0], poly[1]);
			}
			v2.dec(poly[i], poly[i + 1]);
			if (crossProduct(v1, v2) > 0) {
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
	public static class Vector {

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
}
