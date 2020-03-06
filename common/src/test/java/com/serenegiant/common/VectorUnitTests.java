package com.serenegiant.common;

import com.serenegiant.math.Vector;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * com.serenegiant.Vector用のローカルユニットテストクラス
 * FIXME +/- INFINITYやゼロ除算等の異常系のテストは不十分
 */
public class VectorUnitTests {

	private static final float EPS = (float)Math.pow(10., Float.MIN_EXPONENT + 2);

	@Test
	public void constructor_test() throws Exception {
		// デフォルトコンストラクタ
		final Vector v0 = new Vector();
		assertEquals(0.0f, v0.x, EPS);
		assertEquals(0.0f, v0.y, EPS);
		assertEquals(0.0f, v0.z, EPS);

		// Vector(x,y)
		final Vector v1 = new Vector(-123, 456);
		assertEquals(v1.x, -123, EPS);
		assertEquals(v1.y, 456, EPS);
		assertEquals(v1.z, 0.0f, EPS);

		// Vector(x,y,z)
		final Vector v2 = new Vector(-Float.MIN_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE);
		assertEquals(-Float.MIN_VALUE, v2.x, EPS);
		assertEquals(-Float.MAX_VALUE, v2.y, EPS);
		assertEquals(-Float.MAX_VALUE, v2.z, EPS);

		// コピーコンストラクタ
		final Vector v3 = new Vector(v1);
		assertEquals(v1.x, v3.x, EPS);
		assertEquals(v1.y, v3.y, EPS);
		assertEquals(v1.z, v3.z, EPS);

		// クローン
		final Vector v4 = v2.clone();
		assertEquals(v2.x, v4.x, EPS);
		assertEquals(v2.y, v4.y, EPS);
		assertEquals(v2.z, v4.z, EPS);

	}

	@Test
	public void setter_getter_test() throws Exception {
		final Vector v0 = new Vector();
		final Vector v1 = new Vector(-123, 456);

		// #clerar
		v0.clear(1.0f);
		assertEquals(1.0f, v0.x, EPS);
		assertEquals(1.0f, v0.y, EPS);
		assertEquals(1.0f, v0.z, EPS);

		// #set(x,y)
		v0.set(2.0f, 3.0f);
		assertEquals(2.0f, v0.x, EPS);
		assertEquals(3.0f, v0.y, EPS);
		assertEquals(0.0f, v0.z, EPS);

		// #set(x,y,z)
		v0.set(-Float.MIN_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE);
		assertEquals(-Float.MIN_VALUE, v0.x, EPS);
		assertEquals(-Float.MAX_VALUE, v0.y, EPS);
		assertEquals(-Float.MAX_VALUE, v0.z, EPS);

		// #set(src)
		v0.set(v1);
		assertEquals(v1.x, v0.x, EPS);
		assertEquals(v1.y, v0.y, EPS);
		assertEquals(v1.z, v0.z, EPS);

		// #set(src,scalar)
		v0.set(v1, 2);
		assertEquals(v1.x * 2, v0.x, EPS);
		assertEquals(v1.y * 2, v0.y, EPS);
		assertEquals(v1.z * 2, v0.z, EPS);
		// getter
		assertEquals(v0.x, v0.x(), EPS);
		assertEquals(v0.y, v0.y(), EPS);
		assertEquals(v0.z, v0.z(), EPS);
		// setter
		v0.x(5);
		assertEquals(5, v0.x, EPS);
		v0.y(6);
		assertEquals(6, v0.y, EPS);
		v0.z(7);
		assertEquals(7, v0.z, EPS);
	}

	@Test
	public void arithmetic_test() throws Exception {
		final Vector v0 = new Vector(1.0f, 2.0f, 3.0f);
		final Vector v1 = new Vector(4.0f, 5.0f, 6.0f);
		final Vector v2 = Vector.add(null, v0, v1);
		assertEquals(5.0f, v2.x, EPS);
		assertEquals(7.0f, v2.y, EPS);
		assertEquals(9.0f, v2.z, EPS);
		v2.add(v0, 1.0f);
		assertEquals(6.0f, v2.x, EPS);
		assertEquals(9.0f, v2.y, EPS);
		assertEquals(12.0f, v2.z, EPS);

		v2.sub(v0, 1.0f);
		assertEquals(5.0f, v2.x, EPS);
		assertEquals(7.0f, v2.y, EPS);
		assertEquals( 9.0f, v2.z, EPS);

		v2.set(v1).mult(2.0f);
		assertEquals(v1.x * 2, v2.x, EPS);
		assertEquals(v1.y * 2, v2.y, EPS);
		assertEquals(v1.z * 2, v2.z, EPS);

		v2.div(2.0f);
		assertEquals(v1.x, v2.x, EPS);
		assertEquals(v1.y, v2.y, EPS);
		assertEquals(v1.z, v2.z, EPS);

		v2.mult(v0);
		assertEquals(v0.x * v1.x, v2.x, EPS);
		assertEquals(v0.y * v1.y, v2.y, EPS);
		assertEquals(v0.z * v1.z, v2.z, EPS);

		v2.mod(2.0f);
		assertEquals((v0.x * v1.x) % 2.0f, v2.x, EPS);
		assertEquals((v0.y * v1.y) % 2.0f, v2.y, EPS);
		assertEquals((v0.z * v1.z) % 2.0f, v2.z, EPS);
	}

	@Test
	public void limit_test() throws Exception {
		final Vector v0 = new Vector(90.0f, 275.0f, 365.0f);

		// #limit
		final Vector v1 = new Vector(v0).limit(180.0f);
		assertEquals(90.0f, v1.x, EPS);
		assertEquals(95.0f, v1.y, EPS);
		assertEquals(5.0f, v1.z, EPS);

		// limitでしきい値が0の時
		v1.set(v0).limit(0);
		assertEquals(0.0f, v1.x, EPS);
		assertEquals(0.0f, v1.y, EPS);
		assertEquals(0.0f, v1.z, EPS);

		// limitでしきい値が負の時
		v1.set(v0).limit(-180.f);
		assertEquals(90.0f, v1.x, EPS);
		assertEquals(95.0f, v1.y, EPS);
		assertEquals(5.0f, v1.z, EPS);

		// limitでしきい値が負の時
		v1.set(-195, 45, 195).limit(-180);
		assertEquals(-15.0f, v1.x, EPS);
		assertEquals(45.0f, v1.y, EPS);
		assertEquals(15.0f, v1.z, EPS);

		// limitで上下限値指定
		v1.set(v0).limit(-90, 90);
		assertEquals(0.0f, v1.x, EPS);
		assertEquals(5.0f, v1.y, EPS);
		assertEquals(5.0f, v1.z, EPS);

		// limitで上限と下限の大小が入れ替わっている時
		v1.set(v0).limit(90, -90);
		assertEquals(0.0f, v1.x, EPS);
		assertEquals(5.0f, v1.y, EPS);
		assertEquals(5.0f, v1.z, EPS);

		// 正の値の制限
		v1.set(45, 90, 195).limit(-90, 90);
		assertEquals(45.0f, v1.x, EPS);
		assertEquals(0.0f, v1.y, EPS);
		assertEquals(15.0f, v1.z, EPS);

		// 負の値の制限
		v1.set(-45, -90, -195).limit(-90, 90);
		assertEquals(-45.0f, v1.x, EPS);
		assertEquals(0.0f, v1.y, EPS);
		assertEquals(-15.0f, v1.z, EPS);

		// limitで上下限値が同じ値の時
		v1.set(-195, -90, 195).limit(90, 90);
		assertEquals(90.0f, v1.x, EPS);
		assertEquals(90.0f, v1.y, EPS);
		assertEquals(90.0f, v1.z, EPS);

		// limitで上下限値が同じ値の時
		v1.set(-195, -90, 195).limit(-90, -90);
		assertEquals(-90.0f, v1.x, EPS);
		assertEquals(-90.0f, v1.y, EPS);
		assertEquals(-90.0f, v1.z, EPS);

		// #saturate
		v1.set(v0).saturate(180.f);
		assertEquals(90.0f, v1.x, EPS);
		assertEquals(180.0f, v1.y, EPS);
		assertEquals(180.0f, v1.z, EPS);

		v1.set(v0).saturate(-180.f);
		assertEquals(90.0f, v1.x, EPS);
		assertEquals(180.0f, v1.y, EPS);
		assertEquals(180.0f, v1.z, EPS);

		v1.set(v0).saturate(0);
		assertEquals(0.0f, v1.x, EPS);
		assertEquals(0.0f, v1.y, EPS);
		assertEquals(0.0f, v1.z, EPS);

		// #saturate
		v1.set(-195, 90, 195).saturate(180);
		assertEquals(-180.0f, v1.x, EPS);
		assertEquals(90.0f, v1.y, EPS);
		assertEquals(180.0f, v1.z, EPS);

		// #saturate
		v1.set(-195, 90, 195).saturate(0, 180);
		assertEquals(0.0f, v1.x, EPS);
		assertEquals(90.0f, v1.y, EPS);
		assertEquals(180.0f, v1.z, EPS);

		// #saturate
		v1.set(-195, 90, 195).saturate(-180, 0);
		assertEquals(-180.0f, v1.x, EPS);
		assertEquals(0.0f, v1.y, EPS);
		assertEquals(0.0f, v1.z, EPS);
	}

	@Test
	public void length_test() throws Exception {
		final Vector v0 = new Vector(1.0f, 2.0f, 3.0f);
		assertEquals(1.0f * 1.0f + 2.0f * 2.0f + 3.0f * 3.0f, v0.lenSquared(), EPS);
		assertEquals(Math.hypot(1.0f, 2.0f), Math.sqrt(1.0f * 1.0f + 2.0f * 2.0f), EPS);
		assertEquals((float)Math.hypot(1.0f, 2.0f), v0.len2D(), EPS);
		assertEquals((float)Math.sqrt(1.0f * 1.0f + 2.0f * 2.0f), v0.len2D(), EPS);
		assertEquals((float)Math.sqrt(1.0f * 1.0f + 2.0f * 2.0f + 3.0f * 3.0f), v0.len(), EPS);

		// ベクトルを正規化
		v0.normalize();
		assertEquals(1.0f, v0.len(), EPS);
	}

	@Test
	public void dot_cross_test() throws Exception {
		// 2次元
		final Vector v0 = new Vector(1.0f, 2.0f);
		final Vector v1 = new Vector(4.0f, 5.0f);
		// 内積
		assertEquals(v0.x * v1.x + v0.y * v1.y, v0.dot(v1), EPS);
		assertEquals(v0.x * v1.x + v0.y * v1.y, v0.dot2D(v1), EPS);
		assertEquals(v0.x * v1.x + v0.y * v1.y, v0.dotProduct(v1), EPS);
		assertEquals(v0.x * v1.x + v0.y * v1.y, v0.dotProduct2D(v1), EPS);

		// 外積
		assertEquals(v0.x * v1.y - v1.x * v0.y, v0.cross2D(v1), EPS);
		assertEquals(v0.x * v1.y - v1.x * v0.y, v0.crossProduct2D(v1), EPS);

		// 3次元
		v0.set(1.0f, 2.0f, 3.0f);
		v1.set(4.0f, 5.0f, 6.0f);
		// 内積
		assertEquals(v0.x * v1.x + v0.y * v1.y + v0.z * v1.z, v0.dot(v1), EPS);
		assertEquals(v0.x * v1.x + v0.y * v1.y + v0.z * v1.z, v0.dotProduct(v1), EPS);
		assertEquals(v0.x * v1.x + v0.y * v1.y, v0.dot2D(v1), EPS);
		assertEquals(v0.x * v1.x + v0.y * v1.y, v0.dotProduct2D(v1), EPS);
		// 外積
		assertEquals(v0.x * v1.y - v1.x * v0.y, v0.cross2D(v1), EPS);
		assertEquals(v0.x * v1.y - v1.x * v0.y, v0.crossProduct2D(v1), EPS);

		final Vector v2 = Vector.crossProduct(null, v0, v1);
		assertEquals(v0.y * v1.z - v0.z * v1.y, v2.x, EPS);
		assertEquals(v0.z * v1.x - v0.x * v1.z, v2.y, EPS);
		assertEquals(v0.x * v1.y - v0.y * v1.x, v2.z, EPS);
	}

	@Test
	public void angle_test() throws Exception {
		final Vector v0 = new Vector(10, 20, 30);
		// XY平面上でベクトルとX軸の角度を取得
		assertEquals((float)Math.atan2(v0.y, v0.x) * Vector.TO_DEGREE, v0.angleXY(), EPS);
		// XZ平面上でベクトルとX軸の角度を取得
		assertEquals((float)Math.atan2(v0.z, v0.x) * Vector.TO_DEGREE, v0.angleXZ(), EPS);
		// YZ平面上でベクトルとY軸の角度を取得
		assertEquals((float)Math.atan2(v0.z, v0.y) * Vector.TO_DEGREE, v0.angleYZ(), EPS);

		final Vector v1 = new Vector(100, 200, 300);
		// 長さは違うけど方向は同じベクトルとの角度
		assertEquals(0.0f, v1.getAngle(v0), EPS);

		// 2次元(xy平面)
		v0.set(10, 20, 0);
		v1.set(40, 50, 0);
		double cos = (v0.x * v1.x + v0.y * v1.y)
			/ (Math.sqrt(v0.x * v0.x + v0.y * v0.y)
				* Math.sqrt(v1.x * v1.x + v1.y * v1.y));
		assertEquals((float)Math.acos(cos) * Vector.TO_DEGREE, v1.getAngle(v0), EPS);

		v0.set(10, 20, 0);
		v1.set(40, 50, 0);
		cos = (v0.x * v1.x + v0.y * v1.y)
			/ (Math.sqrt(v0.x * v0.x + v0.y * v0.y)
				* Math.sqrt(v1.x * v1.x + v1.y * v1.y));
		assertEquals((float)Math.acos(cos) * Vector.TO_DEGREE, v1.getAngle(v0), EPS);

		// 3次元
		v0.set(10, 20, 30);
		v1.set(40, 50, 60);
		cos = (v0.x * v1.x + v0.y * v1.y + v0.z * v1.z)
			/ (Math.sqrt(v0.x * v0.x + v0.y * v0.y + v0.z * v0.z)
				* Math.sqrt(v1.x * v1.x + v1.y * v1.y + v1.z * v1.z));
		assertEquals((float)Math.acos(cos) * Vector.TO_DEGREE, v1.getAngle(v0), EPS);
	}

	@Test
	public void rotate_test() throws Exception {
		final Vector v = new Vector(100, 0, 0);
		// rotate無し
		assertEquals(0, v.angleXY(), EPS);
		assertEquals(0, v.angleXZ(), EPS);
		assertEquals(0, v.angleYZ(), EPS);
		v.set(0, 100, 0);
		assertEquals(90, v.angleXY(), EPS);
		assertEquals(0, v.angleXZ(), EPS);
		assertEquals(0, v.angleYZ(), EPS);
		v.set(0, 0, 100);
		assertEquals(0, v.angleXY(), EPS);
		assertEquals(90, v.angleXZ(), EPS);
		assertEquals(90, v.angleYZ(), EPS);

		// rotateXY
		v.set(100, 0, 0).rotateXY(30);
		assertEquals(30, v.angleXY(), EPS);
		assertEquals(0, v.angleXZ(), EPS);
		assertEquals(0, v.angleYZ(), EPS);
		v.set(0, 100, 0).rotateXY(30);
		assertEquals(120, v.angleXY(), EPS);
		assertEquals(180, v.angleXZ(), EPS);
		assertEquals(0, v.angleYZ(), EPS);
		v.set(0, 0, 100).rotateXY(30);
		assertEquals(0, v.angleXY(), EPS);
		assertEquals(90, v.angleXZ(), EPS);
		assertEquals(90, v.angleYZ(), EPS);

		// rotateXZ
		v.set(100, 0, 0).rotateXZ(30);
		assertEquals(0, v.angleXY(), EPS);
		assertEquals(30, v.angleXZ(), EPS);
		assertEquals(90, v.angleYZ(), EPS);
		v.set(0, 100, 0).rotateXZ(30);
		assertEquals(90, v.angleXY(), EPS);
		assertEquals(0, v.angleXZ(), EPS);
		assertEquals(0, v.angleYZ(), EPS);
		v.set(0, 0, 100).rotateXZ(30);
		assertEquals(180, v.angleXY(), EPS);
		assertEquals(120, v.angleXZ(), EPS);
		assertEquals(90, v.angleYZ(), EPS);

		// rotateYZ
		v.set(100, 0, 0).rotateYZ(30);
		assertEquals(0, v.angleXY(), EPS);
		assertEquals(0, v.angleXZ(), EPS);
		assertEquals(0, v.angleYZ(), EPS);
		v.set(0, 100, 0).rotateYZ(30);
		assertEquals(90, v.angleXY(), EPS);
		assertEquals(90, v.angleXZ(), EPS);
		assertEquals(30, v.angleYZ(), EPS);
		v.set(0, 0, 100).rotateYZ(30);
		assertEquals(270, v.angleXY(), EPS);
		assertEquals(90, v.angleXZ(), EPS);
		assertEquals(120, v.angleYZ(), EPS);

	}

	@Test
	public void distance_test() throws Exception {
		final Vector v0 = new Vector(1, 2, 3);
		final Vector v1 = new Vector(4, 5, 6);
		final Vector v2 = new Vector(v1).sub(v0);
		assertEquals(v2.x * v2.x + v2.y * v2.y + v2.z * v2.z, v0.distSquared(v1), EPS);
		assertEquals((float)Math.sqrt(v2.x * v2.x + v2.y * v2.y + v2.z * v2.z), v0.distance(v1), EPS);
		assertEquals(v2.x * v2.x + v2.y * v2.y + v2.z * v2.z, v0.distSquared(v1.x, v1.y, v1.z), EPS);
		assertEquals((float)Math.sqrt(v2.x * v2.x + v2.y * v2.y + v2.z * v2.z), v0.distance(v1.x, v1.y, v1.z), EPS);
	}

	@Test
	public void swap_test() throws Exception {
		final Vector v0 = new Vector(1, 2, 3);
		final Vector v1 = new Vector(4, 5, 6);
		v0.swap(v1);
		assertEquals(4, v0.x, EPS);
		assertEquals(5, v0.y, EPS);
		assertEquals(6, v0.z, EPS);
		assertEquals(1, v1.x, EPS);
		assertEquals(2, v1.y, EPS);
		assertEquals(3, v1.z, EPS);

		v0.swapXY();
		assertEquals(5, v0.x, EPS);
		assertEquals(4, v0.y, EPS);
	}

	@Test
	public void slope_test() throws Exception {
		final Vector v0 = new Vector(1, 2, 0);
		final Vector v1 = new Vector(4, 7, 0);	// should be v0.x ≠ v1.x
		assertEquals(v0.y / v0.x, v0.slope2D(), EPS);
		assertEquals((v0.y - v1.y) / (v0.x - v1.x), v0.slope2D(v1), EPS);
	}

	@Test
	public void sign_test() throws Exception {
		final Vector v = new Vector(-123, 0, +456).sign();
		assertEquals(-1, v.x, EPS);
		assertEquals(0, v.y, EPS);
		assertEquals(+1, v.z, EPS);
		v.set(0, +465, -123).sign();
		assertEquals(0, v.x, EPS);
		assertEquals(+1, v.y, EPS);
		assertEquals(-1, v.z, EPS);
		v.set(+465, -123, 0).sign();
		assertEquals(+1, v.x, EPS);
		assertEquals(-1, v.y, EPS);
		assertEquals(0, v.z, EPS);
	}

	@Test
	public void mid_test() throws Exception {
		final Vector p0 = new Vector();
		final Vector p1 = p0.mid(new Vector(100, 200, 300));
		assertEquals(50, p1.x, EPS);
		assertEquals(100, p1.y, EPS);
		assertEquals(150, p1.z, EPS);
		p0.set(-50, 30, 1000);
		final Vector v2 = p0.mid(p1);
		assertEquals((p0.x + p1.x) / 2, v2.x, EPS);
		assertEquals((p0.y + p1.y) / 2, v2.y, EPS);
		assertEquals((p0.z + p1.z) / 2, v2.z, EPS);
	}


	@Test
	public void normal_test() throws Exception {
		// 2次元 (0,0)-(100,200)に垂直で(150,50)を通る垂線の足
		final Vector p0 = new Vector();
		final Vector p1 = new Vector(100, 200, 0);
		final Vector p2 = new Vector(150, 50, 0);
		final Vector p3 = Vector.normalVector(null, p0, p1, p2);
		assertEquals(50, p3.x, EPS);
		assertEquals(100, p3.y, EPS);
		assertEquals(0, p3.z, EPS);

		p0.set(50, 100);
		Vector.normalVector(p3, p0, p1, p2);
		assertEquals(50, p3.x, EPS);
		assertEquals(100, p3.y, EPS);
		assertEquals(0, p3.z, EPS);

		// 3次元
		p0.set(0, 0, 200);
		p1.set(100, 200, 200);
		p2.set(150, 50, 200);
		Vector.normalVector(p3, p0, p1, p2);
		assertEquals(50, p3.x, EPS);
		assertEquals(100, p3.y, EPS);
		assertEquals(200, p3.z, EPS);

		p0.set(0, 0, 0);
		p1.set(100, 200, 200);
		p2.set(150, 50, 100);
		Vector.normalVector(p3, p0, p1, p2);
		assertEquals(50, p3.x, EPS);
		assertEquals(100, p3.y, EPS);
		assertEquals(100, p3.z, EPS);

	}
}
