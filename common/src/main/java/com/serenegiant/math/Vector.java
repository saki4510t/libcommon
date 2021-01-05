package com.serenegiant.math;
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

import android.opengl.Matrix;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;

/**
 * ベクトル計算用ヘルパークラス
 */
public class Vector implements Parcelable, Serializable, Cloneable {
	/**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = 1620440892067002860L;

	public static final double TO_RADIAN = (Math.PI / 180.0);
	public static final double TO_DEGREE = (180.0 / Math.PI);

	/**
	 * ゼロベクトル
	 */
	public static final Vector zeroVector = new Vector();
	/**
	 * (1,1,1)の単位方向ベクトル
	 */
	public static final Vector normVector = new Vector(1,1,1).normalize();

	private static final float[] matrix = new float[16];
	private static final float[] inVec = new float[4];
	private static final float[] outVec = new float[4];

	public float x, y, z;

	/**
	 * デフォルトコンストラクタ
	 */
	public Vector() {
	}

	/**
	 * コンストラクタ
	 * @param x
	 * @param y
	 */
	public Vector(final float x, final float y) {
		this(x, y, 0.0f);
	}

	/**
	 * コピーコンストラクタ
	 * @param v
	 */
	public Vector(@NonNull final Vector v) {
		this(v.x, v.y, v.z);
	}

	/**
	 * コンストラクタ
	 * @param x
	 * @param y
	 * @param z
	 */
	public Vector(final float x, final float y, final float z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	/**
	 * Parcelable用コンストラクタ
	 * @param in
	 */
	protected Vector(@NonNull final Parcel in) {
		x = in.readFloat();
		y = in.readFloat();
		z = in.readFloat();
	}

	@NonNull
	public Vector clone() throws CloneNotSupportedException {
		final Vector result = (Vector)super.clone();
		return result;
	}

	/**
	 * ベクトルの各成分に指定したスカラ値をセット
	 * @param scalar
	 * @return
	 */
	public Vector clear(final float scalar) {
		x = y = z = scalar;
		return this;
	}

	/**
	 * ベクトルの各成分に指定したスカラ値をセット, zは0
	 * @param x
	 * @param y
	 * @return
	 */
	public Vector set(final float x, final float y) {
		return set(x, y, 0.0f);
	}

	/**
	 * ベクトルに指定したベクトルをセット v = v'
	 * @param v
	 * @return
	 */
	public Vector set(@NonNull final Vector v) {
		if (this != v) {
			set(v.x, v.y, v.z);
		}
		return this;
	}

	/**
	 * ベクトルに指定したベクトルをセット(スケール変換あり) v' = v * a
	 * @param v
	 * @param a
	 * @return
	 */
	public Vector set(@NonNull final Vector v, final float a) {
		return set(v.x, v.y, v.z, a);
	}

	/**
	 * ベクトルの各成分に指定したスカラ値をセット v = (x,y,z)
	 * @param x
	 * @param y
	 * @param z
	 * @return
	 */
	public Vector set(final float x, final float y, final float z) {
		this.x = x;
		this.y = y;
		this.z = z;
		return this;
	}

	/**
	 * ベクトルの各成分に指定したスカラ値をセット(スケール変換有り) v = (x,y,z) * a
	 * @param x
	 * @param y
	 * @param z
	 * @param a
	 * @return
	 */
	public Vector set(final float x, final float y, final float z, final float a) {
		this.x = x * a;
		this.y = y * a;
		this.z = z * a;
		return this;
	}

	/**
	 * x成分値を取得
	 * @return
	 */
	public float x() {
		return x;
	}

	/**
	 * x成分値をセット
	 * @param x
	 */
	public Vector x(final float x) {
		this.x = x;
		return this;
	}

	/**
	 * y成分値を取得
	 * @return
	 */
	public float y() {
		return y;
	}

	/**
	 * y成分値をセット
	 * @param y
	 */
	public Vector y(final float y) {
		this.y = y;
		return this;
	}

	/**
	 * z成分値を取得
	 * @return
	 */
	public float z() {
		return z;
	}

	/**
	 * z成分値をセット
	 * @param z
	 */
	public Vector z(final float z) {
		this.z = z;
		return this;
	}

	/**
	 * ベクトルに加算 v = v + (x,y,0)
	 * @param x
	 * @param y
	 * @return
	 */
	public Vector add(final float x, final float y) {
		return add(x, y, 0.0f);
	}

//	/**
//	 * ベクトルを加算 v = v + (x,y,z)
//	 * @param x
//	 * @param y
//	 * @param z
//	 * @return
//	 */
//	public Vector add(final float x, final float y, final float z) {
//		this.x += x;
//		this.y += y;
//		this.z += z;
//		return this;
//	}

	/**
	 * ベクトルを加算 v = v + (x,y,z)
	 * @param x
	 * @param y
	 * @param z
	 * @return
	 */
	public Vector add(final double x, final double y, final double z) {
		this.x = (float)(this.x + x);
		this.y = (float)(this.y + y);
		this.z = (float)(this.z + z);
		return this;
	}

//	/**
//	 * ベクトルを加算(スケール変換有り)v = v + (x,y,z)*a
//	 * @param x
//	 * @param y
//	 * @param z
//	 * @param a
//	 * @return
//	 */
//	public Vector add(final float x, final float y, final float z, final float a) {
//		this.x += x * a;
//		this.y += y * a;
//		this.z += z * a;
//		return this;
//	}

	/**
	 * ベクトルを加算(スケール変換有り)v = v + (x,y,z)*a
	 * @param x
	 * @param y
	 * @param z
	 * @param a
	 * @return
	 */
	public Vector add(final double x, final double y, final double z, final double a) {
		this.x = (float)(this.x + x * a);
		this.y = (float)(this.y + y * a);
		this.z = (float)(this.z + z * a);
		return this;
	}

	/**
	 * ベクトルを加算 v = v + v'
	 * @param v
	 * @return
	 */
	public Vector add(@NonNull final Vector v) {
		return add(v.x, v.y, v.z);
	}

	/**
	 * ベクトルを加算(スケール変換有り) v = v + v' * a
	 * @param v
	 * @param a
	 * @return
	 */
	public Vector add(@NonNull final Vector v, final float a) {
		return add(v.x, v.y, v.z, a);
	}

	/**
	 * ベクトルを加算
	 * result = v1 + v2
	 * @param result
	 * @param v1
	 * @param v2
	 * @return
	 */
	public static Vector add(@Nullable final Vector result,
		@NonNull final Vector v1, @NonNull final Vector v2) {

		return (result != null ? result.set(v1) : new Vector(v1)).add(v2);
	}

	/**
	 * ベクトルを減算 v = v - (x,y,0)
	 * @param x
	 * @param y
	 * @return
	 */
	public Vector sub(final float x, final float y) {
		return add(-x, -y, 0.0f);
	}

	/**
	 * ベクトルを減算 v = v - (x,y,0)
	 * @param x
	 * @param y
	 * @return
	 */
	public Vector sub(final double x, final double y) {
		return add(-x, -y, 0.0);
	}

	/**
	 * ベクトルを減算 v = v - v'
	 * @param v
	 * @return
	 */
	public Vector sub(@NonNull final Vector v) {
		return add(-v.x, -v.y, -v.z);
	}

	/**
	 * ベクトルを減算 v = v - v' * a
	 * @param v
	 * @param a
	 * @return
	 */
	public Vector sub(@NonNull final Vector v, final float a) {
		return add(-v.x, -v.y, -v.z, a);
	}

	/**
	 * ベクトルを減算 v = v - (x,y,z)
	 * @param x
	 * @param y
	 * @param z
	 * @return
	 */
	public Vector sub(final float x, final float y, final float z) {
		return add(-x, -y, -z);
	}

	/**
	 * ベクトルを減算 v = v - (x,y,z)
	 * @param x
	 * @param y
	 * @param z
	 * @return
	 */
	public Vector sub(final double x, final double y, final double z) {
		return add(-x, -y, -z);
	}

	/**
	 * ベクトルを減算(スケール変換有り)v = v - (x,y,z)*a
	 * @param x
	 * @param y
	 * @param z
	 * @param a
	 * @return
	 */
	public Vector sub(final float x, final float y, final float z, final float a) {
		return add(-x, -y, -z, a);
	}

	/**
	 * ベクトルを減算(スケール変換有り)v = v - (x,y,z)*a
	 * @param x
	 * @param y
	 * @param z
	 * @param a
	 * @return
	 */
	public Vector sub(final double x, final double y, final double z, final double a) {
		return add(-x, -y, -z, a);
	}

	/**
	 * ベクトルを減算
	 * result = v1 - v2
	 * @param result
	 * @param v1
	 * @param v2
	 * @return
	 */
	public static Vector sub(@Nullable final Vector result,
		@NonNull final Vector v1, @NonNull final Vector v2) {

		return (result != null ? result.set(v1) : new Vector(v1)).sub(v2);
	}

	/**
	 * ベクトルの各成分同士の掛け算(内積・外積じゃないよ)
	 * v = (v.x * v'.x, v.y * v'.y, v.z * v'.z)
	 * @param other
	 * @return
	 */
	public Vector mult(@NonNull final Vector other) {
		this.x *= other.x;
		this.y *= other.y;
		this.z *= other.z;
		return this;
	}

	/**
	 * ベクトルの各成分にスカラ値をかけ算
	 * v = (v.x * a, v.y * a, v.z * a)
	 * @param scale
	 * @return
	 */
	public Vector mult(final float scale) {
		this.x *= scale;
		this.y *= scale;
		this.z *= scale;
		return this;
	}

	/**
	 * ベクトルの各成分にスカラ値をかけ算
	 * v = (v.x * a, v.y * a, v.z * a)
	 * @param scale
	 * @return
	 */
	public Vector mult(final double scale) {
		this.x *= scale;
		this.y *= scale;
		this.z *= scale;
		return this;
	}

	/**
	 * ベクトルの各成分にスカラ値をかけ算
	 * v = (v.x * xScale, v.y * yScale, v.z)
	 * @param xScale
	 * @param yScale
	 * @return
	 */
	public Vector mult(final float xScale, final float yScale) {
		this.x *= xScale;
		this.y *= yScale;
		return this;
	}

	/**
	 * ベクトルの各成分にスカラ値をかけ算
	 * v = (v.x * xScale, v.y * yScale, v.z)
	 * @param xScale
	 * @param yScale
	 * @return
	 */
	public Vector mult(final double xScale, final double yScale) {
		this.x *= xScale;
		this.y *= yScale;
		return this;
	}

	/**
	 * ベクトルの各成分にスカラ値をかけ算
	 * v = (v.x * xScale, v.y * yScale, v.z * zScale)
	 * @param xScale
	 * @param yScale
	 * @param zScale
	 * @return
	 */
	public Vector mult(final float xScale, final float yScale, final float zScale) {
		this.x *= xScale;
		this.y *= yScale;
		this.z *= zScale;
		return this;
	}

	/**
	 * ベクトルの各成分にスカラ値をかけ算
	 * v = (v.x * xScale, v.y * yScale, v.z * zScale)
	 * @param xScale
	 * @param yScale
	 * @param zScale
	 * @return
	 */
	public Vector mult(final double xScale, final double yScale, final double zScale) {
		this.x *= xScale;
		this.y *= yScale;
		this.z *= zScale;
		return this;
	}

	/**
	 * ベクトルの各成分同士の割り算(内積・外積じゃないよ)
	 * v = (v.x / v'.x, v.y / v'.y, v.z / v'.z)
	 * @param other
	 * @return
	 */
	public Vector div(@NonNull final Vector other) {
		this.x /= other.x;
		this.y /= other.y;
		this.z /= other.z;
		return this;
	}

	/**
	 * ベクトルの各成分をスカラ値で割り算
	 * v = (v.x / a, v.y / a, v.z / a)
	 * @param scale
	 * @return
	 */
	public Vector div(final float scale) {
		this.x /= scale;
		this.y /= scale;
		this.z /= scale;
		return this;
	}

	/**
	 * ベクトルの各成分をスカラ値で割り算
	 * v = (v.x / a, v.y / a, v.z / a)
	 * @param scale
	 * @return
	 */
	public Vector div(final double scale) {
		this.x /= scale;
		this.y /= scale;
		this.z /= scale;
		return this;
	}

	/**
	 * ベクトルの各成分をスカラ値で割り算
	 * v = (v.x / xDiv, v.y / yDiv, v.z)
	 * @param xDiv
	 * @param yDiv
	 * @return
	 */
	public Vector div(final float xDiv, final float yDiv) {
		this.x /= xDiv;
		this.y /= yDiv;
		return this;
	}

	/**
	 * ベクトルの各成分をスカラ値で割り算
	 * v = (v.x / xDiv, v.y / yDiv, v.z)
	 * @param xDiv
	 * @param yDiv
	 * @return
	 */
	public Vector div(final double xDiv, final double yDiv) {
		this.x /= xDiv;
		this.y /= yDiv;
		return this;
	}

	/**
	 * ベクトルの各成分をスカラ値で割り算
	 * v = (v.x / xDiv, v.y / yDiv, v.z / zDiv)
	 * @param xDiv
	 * @param yDiv
	 * @param zDiv
	 * @return
	 */
	public Vector div(final float xDiv, final float yDiv, final float zDiv) {
		this.x /= xDiv;
		this.y /= yDiv;
		this.z /= zDiv;
		return this;
	}

	/**
	 * ベクトルの各成分をスカラ値で割り算
	 * v = (v.x / xDiv, v.y / yDiv, v.z / zDiv)
	 * @param xDiv
	 * @param yDiv
	 * @param zDiv
	 * @return
	 */
	public Vector div(final double xDiv, final double yDiv, final double zDiv) {
		this.x /= xDiv;
		this.y /= yDiv;
		this.z /= zDiv;
		return this;
	}

	/**
	 * ベクトルの各成分をスカラ値で剰余計算
	 * v = (v.x % a, v.y % a, v.z % a)
	 * @param scalar
	 * @return
	 */
	public Vector mod(final float scalar) {
		this.x %= scalar;
		this.y %= scalar;
		this.z %= scalar;
		return this;
	}

	/**
	 * x,y,zがそれぞれ角度(degree)であるとみなしてラジアンに変換する
	 * @return
	 */
	public Vector toRadian() {
		return mult(TO_RADIAN);
	}

	/**
	 * x,y,zがそれぞれラジアンであるとみなして角度に変換する
	 * @return
	 */
	public Vector toDegree() {
		return mult(TO_DEGREE);
	}

	/**
	 * x,y,z各成分を指定した値[-scalar, +scalar]に収まるように制限する
	 * 飽和処理じゃないので注意
	 * @param scalar
	 * @return
	 */
	public Vector limit(final float scalar) {
		final float limit = Math.abs(scalar);
		if (limit != 0) {
			while (x >= limit) x -= limit;
			while (x <= -limit) x += limit;
			while (y >= limit) y -= limit;
			while (y <= -limit) y += limit;
			while (z >= limit) z -= limit;
			while (z <= -limit) z += limit;
		} else {
			x = y = z = 0;
		}
		return this;
	}

	/**
	 * x,y,z各成分を指定した値[lower, upper]に収まるように制限する
	 * 飽和処理じゃないので注意
	 * @param lower
	 * @param upper
	 * @return
	 */
	public Vector limit(final float lower, final float upper) {
		final float min = Math.min(lower, upper);
		final float max = Math.max(lower, upper);
		if (max != min) {
			while (x >= max) x -= max;
			while (x <= min) x -= min;
			while (y >= max) y -= max;
			while (y <= min) y -= min;
			while (z >= max) z -= max;
			while (z <= min) z -= min;
		} else {
			x = y = z = min;
		}
		return this;
	}

	/**
	 * x,y,z各成分を指定した値[-scalar, +scalar]に収まるように飽和処理する
	 * @param scalar
	 * @return
	 */
	public Vector saturate(final float scalar) {
		final float limit = Math.abs(scalar);
		x = x >= limit ? limit : (Math.max(x, -limit));
		y = y >= limit ? limit : (Math.max(y, -limit));
		z = z >= limit ? limit : (Math.max(z, -limit));
		return this;
	}

	/**
	 * x,y,z各成分を指定した値[lower, upper]に収まるように飽和処理する
	 * @param lower
	 * @param upper
	 * @return
	 */
	public Vector saturate(final float lower, final float upper) {
		final float min = Math.min(lower, upper);
		final float max = Math.max(lower, upper);
		if (max != min) {
			x = x >= max ? max : (Math.max(x, min));
			y = y >= max ? max : (Math.max(y, min));
			z = z >= max ? max : (Math.max(z, min));
		} else {
			z = y = z = min;
		}
		return this;
	}

	/**
	 * (x,y)ベクトルの長さを取得
	 * @return
	 */
	public float len2D() {
		return (float)Math.hypot(x, y);
	}

	/**
	 * ベクトルの長さを取得
	 * @return
	 */
	public float len() {
		return (float) Math.sqrt(lenSquared());
	}

	/**
	 * ベクトルの長さを変更
	 * @param len
	 * @return
	 */
	public Vector len(final float len) {
		final double l = Math.sqrt(lenSquared());
		if ((l != 0) && (len != 0)) {
			return mult(len / l);
		} else {
			return clear(0);
		}
	}

	/**
	 * ベクトルの長さを変更
	 * @param len
	 * @return
	 */
	public Vector len(final double len) {
		final double l = Math.sqrt(lenSquared());
		if ((l != 0) && (len != 0)) {
			return mult(len / l);
		} else {
			return clear(0);
		}
	}

	/**
	 * ベクトルの長さの２乗を取得
	 * @return
	 */
	public float lenSquared() {
		return (float)(x * (double)x + y * (double)y + z * (double)z);
	}

	/**
	 * ベクトルを正規化(長さを1にする)
	 * #normalVectorとは違うよ
	 * @return
	 */
	public Vector normalize() {
		final double len = Math.sqrt(lenSquared());
		if (len != 0) {
			this.x /= len;
			this.y /= len;
			this.z /= len;
		}
		return this;
	}

	/**
	 * ベクトルの内積を取得(dotProduct2Dと同じ)
	 * xy平面で標準化ベクトルv2を含む直線にベクトルv1を真っ直ぐ下ろした（正射影した）時の長さ
	 * @param v
	 * @return
	 */
	public float dot2D(@NonNull final Vector v) {
		return (float)(x * (double)v.x + y * (double)v.y);
	}

	/**
	 * ベクトルの内積を取得(dot2Dと同じ)
	 * xy平面で標準化ベクトルv2を含む直線にベクトルv1を真っ直ぐ下ろした（正射影した）時の長さ
	 * @param v
	 * @return
	 */
	public float dotProduct2D(@NonNull final Vector v) {
		return (float)(x * (double)v.x + y * (double)v.y);
	}

	/**
	 * ベクトルの内積を取得(dotProduct2Dと同じ)
	 * @param x
	 * @param y
	 * @param z
	 * @return
	 */
	public float dot2D(final float x, final float y, final float z) {
		return (float)(this.x * (double)x + this.y * (double)y);
	}

	/**
	 * ベクトルの内積を取得(dot2Dと同じ)
	 * @param x
	 * @param y
	 * @param z
	 * @return
	 */
	public float dotProduct2D(final float x, final float y, final float z) {
		return (float)(this.x * (double)x + this.y * (double)y);
	}

	/**
	 * ベクトルの内積を取得(dotProductと同じ)
	 * 標準化ベクトルv2を含む直線にベクトルv1を真っ直ぐ下ろした（正射影した）時の長さ
	 * @param v
	 * @return
	 */
	public float dot(@NonNull final Vector v) {
		return dot(this, v);
	}

	/**
	 * ベクトルの内積を取得(dotと同じ)
	 * 標準化ベクトルv2を含む直線にベクトルv1を真っ直ぐ下ろした（正射影した）時の長さ
	 * @param v
	 * @return
	 */
	public float dotProduct(@NonNull final Vector v) {
		return dot(this, v);
	}

	/**
	 * ベクトルの内積を取得(dotProductと同じ)
	 * @param x
	 * @param y
	 * @param z
	 * @return
	 */
	public float dot(final float x, final float y, final float z) {
		return (float)(this.x * (double)x + this.y * (double)y + this.z * (double)z);
	}

	/**
	 * ベクトルの内積を取得(dotと同じ)
	 * @param x
	 * @param y
	 * @param z
	 * @return
	 */
	public float dotProduct(final float x, final float y, final float z) {
		return (float)(this.x * (double)x + this.y * (double)y + this.z * (double)z);
	}

	/**
	 * ベクトルの内積を計算
	 * @param v0
	 * @param v1
	 * @return
	 */
	public static float dot(@NonNull final Vector v0, @NonNull final Vector v1) {
		return (float)(v0.x * (double)v1.x + v0.y * (double)v1.y + v0.z * (double)v1.z);
	}

	/**
	 * ベクトルの内積を計算(doubleを返す)
	 * @param v0
	 * @param v1
	 * @return
	 */
	public static double dotDouble(@NonNull final Vector v0, @NonNull final Vector v1) {
		return v0.x * (double)v1.x + v0.y * (double)v1.y + v0.z * (double)v1.z;
	}

	/**
	 * ベクトルの外積を計算(2D, crossProduct2Dと同じ)
	 * v1×v2= x1*y2-x2*y1 = |v1||v2|sin(θ)
	 * @param v
	 * @return
	 */
	public float cross2D(@NonNull final Vector v) {
		return (float)((double)x * v.y - v.x * (double)y);
	}

	/**
	 * ベクトルの外積を計算(2D, cross2Dと同じ)
	 * v1×v2= x1*y2-x2*y1 = |v1||v2|sin(θ)
	 * @param v
	 * @return
	 */
	public float crossProduct2D(@NonNull final Vector v) {
		return (float)((double)x * v.y - v.x * (double)y);
	}

	/**
	 * ベクトルの外積を計算(3D, crossProductと同じ)
	 * v1×v2= (y1*z2-z1*y2, z1*x2-x1*z2, x1*y2-y1*x2) = (x3, y3, z3) =  v3
	 * 2つのベクトルに垂直な方向を向いた大きさが|v1||v2|sinθのベクトル
	 * @param v
	 * @return
	 */
	public Vector cross(@NonNull final Vector v) {
		return crossProduct(this, this, v);
	}

	/**
	 * ベクトルの外積を計算(3D, crossと同じ)
	 * v1×v2= (y1*z2-z1*y2, z1*x2-x1*z2, x1*y2-y1*x2) = (x3, y3, z3) =  v3
	 * 2つのベクトルに垂直な方向を向いた大きさが|v1||v2|sinθのベクトル
	 * @param v
	 * @return
	 */
	public Vector crossProduct(@NonNull final Vector v) {
		return crossProduct(this, this, v);
	}

	/**
	 * ベクトルの外積を計算(3D, crossProductと同じ)
	 * @param result nullなら新規に生成して返す
	 * @param v1
	 * @param v2
	 * @return result, result = v1 cross v2
	 */
	public static Vector cross(@Nullable final Vector result,
		@NonNull final Vector v1, @NonNull final Vector v2) {

		return crossProduct(result, v1, v2);
	}

	/**
	 * ベクトルの外積を計算(3D, crossと同じ)
	 * @param result nullなら新規に生成して返す
	 * @param v1
	 * @param v2
	 * @return result, result = v1 cross v2
	 */
	public static Vector crossProduct(@Nullable final Vector result,
		@NonNull final Vector v1, @NonNull final Vector v2) {

		final float x3 = (float)(v1.y * (double)v2.z - v1.z * (double)v2.y);
		final float y3 = (float)(v1.z * (double)v2.x - v1.x * (double)v2.z);
		final float z3 = (float)(v1.x * (double)v2.y - v1.y * (double)v2.x);
		return result != null ? result.set(x3, y3, z3) : new Vector(x3, y3, z3);
	}

	/**
	 * XY平面上でベクトルとX軸の角度を取得
	 */
	public float angleXY() {
		float angle = (float)(Math.atan2(y, x) * TO_DEGREE);
		if (angle < 0) {
			angle += 360;
		}
		return angle;
	}

	/**
	 * XZ平面上でベクトルとX軸の角度を取得
	 */
	public float angleXZ() {
		float angle = (float)(Math.atan2(z, x) * TO_DEGREE);
		if (angle < 0) {
			angle += 360;
		}
		return angle;
	}

	/**
	 * YZ平面上でベクトルとY軸の角度を取得
	 */
	public float angleYZ() {
		float angle = (float)(Math.atan2(z, y) * TO_DEGREE);
		if (angle < 0)
			angle += 360;
		return angle;
	}

	/**
	 * ベクトル間の角度を取得
	 * ベクトル１ Za=(X1,Y1,Z1)、ベクトル２ Zb=(X2,Y2,Z2)、求める角φとすると、
	 * cos φ ＝ Za・Zb / (|Za| |Zb|)
	 *  =(X1X2+Y1Y2+Z1Z2) / √{(X1^2 + Y1^2 + Z1^2)(X2^2 + Y2^2 + Z2^2)}
	 *  上式のアークコサイン(cos^-1)を取ればOK。
	 *  ※どちらかのベクトルの長さが0の場合には(ゼロ除算で)INFINITYになる
	 * @param v
	 * @return 角度[度]
	 */
	@Deprecated
	public float getAngle(@NonNull final Vector v) {
		return angle(v);
	}

	/**
	 * ベクトル間の角度を取得
	 * ベクトル１ Za=(X1,Y1,Z1)、ベクトル２ Zb=(X2,Y2,Z2)、求める角φとすると、
	 * cos φ ＝ Za・Zb / (|Za| |Zb|)
	 *  =(X1X2+Y1Y2+Z1Z2) / √{(X1^2 + Y1^2 + Z1^2)(X2^2 + Y2^2 + Z2^2)}
	 *  上式のアークコサイン(cos^-1)を取ればOK。
	 *  ※どちらかのベクトルの長さが0の場合には(ゼロ除算で)INFINITYになる
	 * @param v
	 * @return 角度[度]
	 */
	public float angle(@NonNull final Vector v) {
		final double cos = dotDouble(this, v)
			/ (Math.sqrt(lenSquared()) * Math.sqrt(v.lenSquared()));
		return (float) (Math.acos(cos) * TO_DEGREE);
	}

	/**
	 * Z軸周りに(XY平面上で)ベクトルを指定した角度[度]回転させる
	 * z値は変更しない
	 * @param angle
	 * @return
	 */
	public Vector rotateXY(final float angle) {
		final double rad = angle * TO_RADIAN;
		final double cos = Math.cos(rad);
		final double sin = Math.sin(rad);

		final double newX = x * cos - y * sin;
		final double newY = x * sin + y * cos;

		x = (float)newX;
		y = (float)newY;

		return this;
	}

	/**
	 * Y軸周りに(XZ平面上で)ベクトルを指定した角度[度]回転させる
	 * @param angle
	 * @return
	 */
	public Vector rotateXZ(final float angle) {
		final double rad = angle * TO_RADIAN;
		final double cos = Math.cos(rad);
		final double sin = Math.sin(rad);

		final double newX = x * cos - z * sin;
		final double newZ = x * sin + z * cos;

		x = (float)newX;
		z = (float)newZ;

		return this;
	}

	/**
	 * X軸周りに(YZ平面上で)ベクトルを指定した角度[度]回転させる
	 * @param angle
	 * @return
	 */
	public Vector rotateYZ(final float angle) {
		final double rad = angle * TO_RADIAN;
		final double cos = Math.cos(rad);
		final double sin = Math.sin(rad);

		final double newY = y * cos - z * sin;
		final double newZ = y * sin + z * cos;

		y = (float)newY;
		z = (float)newZ;

		return this;
	}

	/**
	 * ベクトルを回転(スレッドセーフではない)
	 * x軸：(1,0,0), y軸(0,1,0), z軸(0,0,1)
	 * @param angle [度]
	 * @param axisX
	 * @param axisY
	 * @param axisZ
	 * @return
	 */
	public Vector rotate(final float angle, final float axisX, final float axisY, final float axisZ) {
		inVec[0] = x;
		inVec[1] = y;
		inVec[2] = z;
		inVec[3] = 1;
		Matrix.setIdentityM(matrix, 0);
		Matrix.rotateM(matrix, 0, angle, axisX, axisY, axisZ);
		Matrix.multiplyMV(outVec, 0, matrix, 0, inVec, 0);
		x = outVec[0];
		y = outVec[1];
		z = outVec[2];
		return this;
	}

	/**
	 * ベクトルを回転(スレッドセーフではない)
	 * @param angleX [度]
	 * @param angleY [度]
	 * @param angleZ [度]
	 * @return
	 */
	public Vector rotate(final float angleX, final float angleY, final float angleZ) {
		return rotate(this, angleX, angleY, angleZ);
	}

	/**
	 * ベクトルを回転(スレッドセーフではない)
	 * @param result 回転させるベクトル
	 * @param angleX [度]
	 * @param angleY [度]
	 * @param angleZ [度]
	 * @return result
	 */
	public static Vector rotate(@NonNull final Vector result,
		final float angleX, final float angleY, final float angleZ) {

		inVec[0] = result.x;
		inVec[1] = result.y;
		inVec[2] = result.z;
		inVec[3] = 1;
		Matrix.setIdentityM(matrix, 0);
		if (angleX != 0) {
			Matrix.rotateM(matrix, 0, angleX, 1f, 0f, 0f);
		}
		if (angleY != 0) {
			Matrix.rotateM(matrix, 0, angleY, 0f, 1f, 0f);
		}
		if (angleZ != 0) {
			Matrix.rotateM(matrix, 0, angleZ, 0f, 0f, 1f);
		}
		Matrix.multiplyMV(outVec, 0, matrix, 0, inVec, 0);
		result.x = outVec[0];
		result.y = outVec[1];
		result.z = outVec[2];
		return result;
	}

//	/**
//	 * ベクトル配列内の各ベクトルを回転(スレッドセーフでは無い)
//	 * @param results ベクトル配列
//	 * @param angleX [度]
//	 * @param angleY [度]
//	 * @param angleZ [度]
//	 * @return results
//	 */
//	public static Vector[] rotate(@NonNull @Size(min=1) final Vector[] results,
//		final float angleX, final float angleY, final float angleZ) {
//
//		Matrix.setIdentityM(matrix, 0);
//		if (angleX != 0)
//			Matrix.rotateM(matrix, 0, angleX, 1f, 0f, 0f);
//		if (angleY != 0)
//			Matrix.rotateM(matrix, 0, angleY, 0f, 1f, 0f);
//		if (angleZ != 0)
//			Matrix.rotateM(matrix, 0, angleZ, 0f, 0f, 1f);
//		final int n = (results != null) ? results.length : 0;
//		for (int i = 0; i < n; i++) {
//			if (results[i] == null) continue;
//			inVec[0] = results[i].x;
//			inVec[1] = results[i].y;
//			inVec[2] = results[i].z;
//			inVec[3] = 1;
//			Matrix.multiplyMV(outVec, 0, matrix, 0, inVec, 0);
//			results[i].x = outVec[0];
//			results[i].y = outVec[1];
//			results[i].z = outVec[2];
//		}
//		return results;
//	}

	/**
	 * ベクトルを回転(スレッドセーフではない) v = v#rot(angle * a)
	 * @param angle 回転角, 各成分は [度]
	 * @param a　スケール変換
	 * @return
	 */
	public Vector rotate(@NonNull final Vector angle, final float a) {
		return rotate(this, angle.x * a, angle.y * a, angle.z * a);
	}

	/**
	 * ベクトルを回転(スレッドセーフではない) v = v#rot(angle)
	 * @param angle
	 * @return
	 */
	public Vector rotate(@NonNull final Vector angle) {
		return rotate(this, angle.x, angle.y, angle.z);
	}

	/**
	 * 逆回転(スレッドセーフではない)
	 * @param angleX
	 * @param angleY
	 * @param angleZ
	 * @return
	 */
	@Deprecated
	public Vector rotate_inv(final float angleX, final float angleY, final float angleZ) {
		inVec[0] = x;
		inVec[1] = y;
		inVec[2] = z;
		inVec[3] = 1;
		Matrix.setIdentityM(matrix, 0);
		if (angleZ != 0) {
			Matrix.rotateM(matrix, 0, angleZ, 0f, 0f, 1f);
		}
		if (angleY != 0) {
			Matrix.rotateM(matrix, 0, angleY, 0f, 1f, 0f);
		}
		if (angleX != 0) {
			Matrix.rotateM(matrix, 0, angleX, 1f, 0f, 0f);
		}
		Matrix.multiplyMV(outVec, 0, matrix, 0, inVec, 0);
		x = outVec[0];
		y = outVec[1];
		z = outVec[2];
		return this;
	}

	/**
	 * 逆回転(スレッドセーフではない)
	 * @param angle
	 * @param a
	 * @return
	 */
	@Deprecated
	public Vector rotate_inv(@NonNull final Vector angle, final float a) {
		rotate_inv(angle.x * a, angle.y * a, angle.z * a);
		return this;
	}

	/**
	 * 逆回転(スレッドセーフではない)
	 * @param angle
	 * @return
	 */
	@Deprecated
	public Vector rotate_inv(@NonNull final Vector angle) {
		rotate_inv(angle, -1f);
		return this;
	}

	/**
	 * クオータニオンとして取得(4番目の成分は1)
	 * @param result nullなら新規生成して返す, nullでないなら4要素以上必要
	 * @return
	 */
	public float[] getQuat(@Nullable @Size(min=4) final float[] result) {
		final float[] q = result != null ? result : new float[4];
		q[0] = x;
		q[1] = y;
		q[2] = z;
		q[3] = 1;
		return q;
	}

	/**
	 * クオータニオンをセット(4番目の成分は無視される)
	 * @param q 4要素以上必要
	 * @return
	 */
	public Vector setQuat(@NonNull @Size(min=4) final float[] q) {
		x = q[0];
		y = q[1];
		z = q[2];
		return this;
	}

	/**
	 * この位置ベクトルと引数ベクトルが示す2点間距離を取得する
	 * @param p
	 * @return
	 */
	public float distance(@NonNull final Vector p) {
		return distance(p.x, p.y, p.z);
	}

	/**
	 * この位置ベクトルと引数ベクトルが示す2点間距離を取得する
	 * @param px
	 * @param py
	 * @return
	 */
	public float distance(final float px, final float py) {
		return distance(px, py, this.z);
	}

	/**
	 * この位置ベクトルと引数が示す座標間の距離を取得する
	 * @param px
	 * @param py
	 * @param pz
	 * @return
	 */
	public float distance(final float px, final float py, final float pz) {
		return (float) Math.sqrt(distSquaredDouble(px, py, pz));
	}

	/**
	 * この位置ベクトルと引数ベクトルが示す2点間距離の2乗を取得する
	 * @param p
	 * @return
	 */
	public float distSquared(@NonNull final Vector p) {
		return distSquared(p.x, p.y, p.z);
	}

	/**
	 * この位置ベクトルと引数が示す座標間の距離の2乗を取得する
	 * @param px
	 * @param py
	 * @return
	 */
	public float distSquared(final float px, final float py) {
		return distSquared(px, py, this.z);
	}

	/**
	 * この位置ベクトルと引数が示す座標間の距離の2乗を取得する
	 * @param px
	 * @param py
	 * @param pz
	 * @return
	 */
	public float distSquared(final float px, final float py, final float pz) {
		final double dx = this.x - (double)px;
		final double dy = this.y - (double)py;
		final double dz = this.z - (double)pz;
		return (float)(dx * dx + dy * dy + dz * dz);
	}

	/**
	 * この位置ベクトルと引数が示す座標間の距離の2乗を取得する
	 * doubleで返す
	 * @param px
	 * @param py
	 * @param pz
	 * @return
	 */
	public double distSquaredDouble(final float px, final float py, final float pz) {
		final double dx = this.x - (double)px;
		final double dy = this.y - (double)py;
		final double dz = this.z - (double)pz;
		return dx * dx + dy * dy + dz * dz;
	}

	/**
	 * ベクトルの各成分を交換
	 */
	public Vector swap(@NonNull final Vector v) {
		float w = x; x = v.x; v.x = w;
		w = y; y = v.y; v.y = w;
		w = z; z = v.z; v.z = w;
		return this;
	}

	/**
	 * x成分とy成分を交換
	 */
	@SuppressWarnings("SuspiciousNameCombination")
	public Vector swapXY() {
		final float w = x; x = y; y = w;
		return this;
	}

	/**
	 * このベクトルと引数ベクトルで示す2点を通る直線の傾きを取得(2D)
	 * result if (x != v.x) (v.y - y) / (v.x - x) elif (v.y - y) > 0 MAX_VALUE else MIN_VALUE
	 * @param p
	 * @return
	 */
	public float slope2D(@NonNull final Vector p) {
		if (p.x != x) {
			return (float)((p.y - (double)y) / (p.x - (double)x));
		} else {
			return (p.y - y) >= 0 ? Float.MAX_VALUE : Float.MIN_VALUE;
		}
	}

	/**
	 * このベクトルと引数ベクトルが示す2点を通る直線のy切片を取得
	 * y軸に平行な場合はPOSITIVE_INFINITYを返す
	 * @param p
	 * @return
	 */
	public float interceptY2D(@NonNull final Vector p) {
		if (p.x != x) {
			final double slope = (p.y - (double)y) / (p.x - (double)x);
			return (float)(y - slope * x);
		} else {
			return Float.POSITIVE_INFINITY;
		}
	}

	/**
	 * 原点とこのベクトルが示す点を通る直線の傾きを取得(2D)
	 * result if (x != 0) y / x elif y > 0 MAX_VALUE else MIN_VALUE
	 * @return
	 */
	public float slope2D() {
		if (x != 0) {
			return (float)(y / (double)x);
		} else {
			return y >= 0 ? Float.MAX_VALUE : Float.MIN_VALUE;
		}
	}

	/**
	 * 各成分を負なら-1.0f, ゼロなら0.0f, 正なら1.0fにする
	 * @return
	 */
	public Vector sign() {
		x = Math.signum(x);
		y = Math.signum(y);
		z = Math.signum(z);
		return this;
	}

	/**
	 * この位置ベクトルと引数の位置ベクトルが示す2点を結ぶ線分の中点を示す位置ベクトルを取得
	 * @param p
	 * @return
	 */
	public Vector mid(@NonNull final Vector p) {
		return mid(null, this, p);
	}

	/**
	 * v1とv2が示す座標を結ぶ線分の中点を返す
	 * @param result
	 * @param p1
	 * @param p2
	 * @return
	 */
	public static Vector mid(@Nullable final Vector result,
		@NonNull final Vector p1, @NonNull final Vector p2) {

		return add(result != null ? result : new Vector(), p1, p2).div(2.0f);
	}

	/**
	 * p0, p1を通る線分に垂直でp2を通る線分の足(線分p0p1との交点座標)を求める
	 * 正規化#normalizeとは違うよ
	 * @param result
	 * @param p0
	 * @param p1
	 * @param p2
	 * @return
	 */
	public static Vector normalVector(@Nullable final Vector result,
		@NonNull final Vector p0, @NonNull final Vector p1,
		@NonNull final Vector p2) {

		final Vector v1 = sub(result, p1, p0);
		final Vector v2 = new Vector(p2).sub(p0);
		return v1.mult(v1.dot(v2) / v1.lenSquared()).add(p0);
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) return true;
		if (!(o instanceof Vector)) return false;
		final Vector v = (Vector) o;
		return Float.compare(v.x, x) == 0 &&
			Float.compare(v.y, y) == 0 &&
			Float.compare(v.z, z) == 0;
	}

	@Override
	public int hashCode() {
		return hasCode(x, y, z);
	}

	private static int hasCode(final Object... objects) {
		return Arrays.hashCode(objects);
	}

	@NonNull
	@Override
	public String toString() {
		return String.format(Locale.US, "(%f,%f,%f)", x, y, z);
	}

	public String toString(String fmt) {
		return String.format(Locale.US, fmt, x, y, z);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(@NonNull final Parcel dest, final int flags) {
		dest.writeFloat(x);
		dest.writeFloat(y);
		dest.writeFloat(z);
	}

	public static final Creator<Vector> CREATOR = new Creator<Vector>() {
		@Override
		public Vector createFromParcel(Parcel in) {
			return new Vector(in);
		}

		@Override
		public Vector[] newArray(int size) {
			return new Vector[size];
		}
	};

/*
 * Note:
 * 2D座標で線分p0p1を表すベクトルをv0、線分p0p2を表すベクトルをv1とした時に
 * 外積v0 x v1が負ならv0に対して右側に点p2が存在
 * 外積v0 x v1が0ならv0に対してv0またはその延長線上にp2が存在
 * 外積v0 x v1が正ならv0に対して左側に点p2が存在する
 */
}
