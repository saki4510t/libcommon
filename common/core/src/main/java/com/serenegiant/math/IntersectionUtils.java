package com.serenegiant.math;
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

import android.util.Log;

public class IntersectionUtils {
	private static final boolean DEBUG = false;	// TODO for debugging
	private static final String TAG = IntersectionUtils.class.getSimpleName();

	private IntersectionUtils() {
		// インスタンス化を防止するためデフォルトコンストラクタをprivateに
	}

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

	public static final float crossProduct(final Vector2d v1, final Vector2d v2) {
		return v1.x * v2.y - v2.x * v1.y;
	}

	private static final Vector2d sPtInPoly_v1 = new Vector2d();
	private static final Vector2d sPtInPoly_v2 = new Vector2d();
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
	public static final class Vector2d {

		public float x, y;

		public Vector2d() {
		}

/*		public Vector(Vector src) {
			set(src);
		} */

		public Vector2d(final float x, final float y) {
			set(x, y);
		}

		public Vector2d set(final float x, final float y) {
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

		public Vector2d sub(final Vector2d other) {
			return new Vector2d(x - other.x, y - other.y);
		}

/*		public Vector sub(final float x, final float y) {
			return new Vector(this.x - x, this.y - y);
		} */

/*		public Vector dec(final Vector other) {
			x -= other.x;
			y -= other.y;
			return this;
		} */

		public Vector2d dec(final float x, final float y) {
			this.x -= x;
			this.y -= y;
			return this;
		}
	}

	public static final class LineSegment {

		public final Vector2d p1;
		public final Vector2d p2;

		public LineSegment (final float x0, final float y0, final float x1, final float y1) {
			p1 = new Vector2d(x0, y0);
			p2 = new Vector2d(x1, y1);
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

		final Vector2d a = seg.p2.sub(seg.p1);
		Vector2d b, c, d;
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
