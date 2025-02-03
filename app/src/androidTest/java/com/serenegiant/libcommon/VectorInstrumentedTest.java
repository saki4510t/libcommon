package com.serenegiant.libcommon;
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

import com.serenegiant.math.Vector;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * com.serenegiant.Vector用のインスツルメンテーションテスト用クラス
 * android.graphics.Matrixを使うメソッドはローカルユニットテストでは実行できないので
 */
@RunWith(AndroidJUnit4.class)
public class VectorInstrumentedTest {
	private static final float EPS = (float)Math.pow(10., Float.MIN_EXPONENT + 2);

	@Test
	public void rotateTest() throws Exception {
		final Vector v = new Vector(100, 0, 0);
		// rotate
		v.set(100, 0, 0).rotate(30, 0, 0, 1);
		assertEquals(30, v.angleXY(), EPS);
		assertEquals(0, v.angleXZ(), EPS);
		assertEquals(0, v.angleYZ(), EPS);
		v.set(0, 100, 0).rotate(30, 0, 0, 1);
		assertEquals(120, v.angleXY(), EPS);
		assertEquals(180, v.angleXZ(), EPS);
		assertEquals(0, v.angleYZ(), EPS);
		v.set(0, 0, 100).rotate(30, 0, 0, 1);
		assertEquals(0, v.angleXY(), EPS);
		assertEquals(90, v.angleXZ(), EPS);
		assertEquals(90, v.angleYZ(), EPS);
	}
}
