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

import com.serenegiant.math.Fraction;

import static com.serenegiant.math.Fraction.*;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * com.serenegiant.math.Fraction用のローカルユニットテストクラス
 * FIXME +/- INFINITY, MIN_VALUE/MAX_VALUE等の異常系境界系のテストは不十分
 */
public class FractionUnitTests {

	private static final float EPS = (float)Math.pow(10.0, Float.MIN_EXPONENT + 2);
	private static final int MIN_INTEGER = Integer.MIN_VALUE + 1;	// = -Integer.MAX_VALUE

	/**
	 * コンストラクタのテスト
	 */
	@Test
	public void constructor() {
		// コンストラクタをテスト
		assertEquals(ZERO, ZERO);
		assertEquals(ONE, ONE);
		assertNotEquals(ONE, ZERO);
		assertEquals(ZERO, new Fraction());	// デフォルトコンストラクタはZEROと等価
		assertEquals(ONE, new Fraction(1));	// 分子だけを指定(デフォルトの分母は1)
		assertEquals(new Fraction(10, 10), new Fraction(10, 10));
		assertNotEquals(ONE, new Fraction(10));
		assertNotEquals(ONE, new Fraction(10, 1));
		assertNotEquals(ONE, new Fraction(10, 10));	// 算数的には同じだけどフィールド値が違うのでfalseが正常
		assertEquals(10, new Fraction(10, 30).numerator());	// コンストラクタでは約分しない
		assertEquals(30, new Fraction(10, 30).denominator());	// コンストラクタでは約分しない
	}

	/**
	 *　分母に0を引き渡すとIllegalArgumentExceptionを生成することをテスト
	 */
	@Test(expected = IllegalArgumentException.class)
	public void zeroDenominator() {
		new Fraction(1, 0);
	}

	/**
	 * 分母にInteger.MIN_VALUEを引き渡すとIllegalArgumentExceptionを生成することをテスト
	 */
	@Test(expected = IllegalArgumentException.class)
	public void minValueDenominator() {
		new Fraction(1, Integer.MIN_VALUE);
	}

	/**
	 * 分子にInteger.MIN_VALUEを引き渡すとIllegalArgumentExceptionを生成することをテスト
	 */
	@Test(expected = IllegalArgumentException.class)
	public void minValueNumerator() {
		new Fraction(1, Integer.MIN_VALUE);
	}

	@Test
	public void asValues() {
		assertEquals(1/3.0f, ONE_THIRD.asFloat(), EPS);
		assertEquals(1/3.0, ONE_THIRD.asDouble(), EPS);
		assertEquals("1/3", ONE_THIRD.asString());
	}

	/**
	 * 演算関係のテスト
	 * ・新しいオブジェクトを生成
	 * ・演算結果は約分して返される
	 * ・演算結果が0になるならZEROと等価になる
	 */
	@Test
	public void arithmeticCreations() {
		assertEquals(1, new Fraction(10, 30).absFraction().numerator());	// 演算後は約分される
		assertEquals(3, new Fraction(10, 30).absFraction().denominator());// 演算後は約分される
		assertEquals(ONE_THIRD, ONE_THIRD.reducedFraction());			// 1/3の約分 = 1/3
		assertEquals(ONE_THIRD, new Fraction(6, 18).reducedFraction());	// 6/18の約分 = 1/3
		assertEquals(ONE_THIRD, new Fraction(10, 30).reducedFraction());	// 10/30の約分 = 1/3
		assertEquals(ONE, new Fraction(Integer.MAX_VALUE, Integer.MAX_VALUE).reducedFraction());
		assertEquals(ONE_THIRD, ONE_THIRD.absFraction());				// abs(1/3) = 1/3
		assertEquals(ONE_THIRD, ONE_THIRD.invertFraction().invertFraction());	// -(-1/3) = 1/3
		assertEquals(ONE, ONE_HALF.addFraction(ONE_HALF));				// 1/2 + 1/2 = 1
		assertEquals(ONE_HALF, ONE_QUARTER.addFraction(ONE_QUARTER));	// 1/4 + 1/4 = 1/2	演算後は約分される
		assertEquals(ONE_HALF, THREE_QUARTER.subFraction(ONE_QUARTER));	// 3/4 - 1/4 = 1/2	演算後は約分される
		assertEquals(ZERO, ONE_HALF.subFraction(ONE_HALF)); 			// 1/2 - 1/2 = 0
		assertEquals(ZERO, THREE_QUARTER.multiplyFraction(0)); 			// 3/4 * 0 = 0/1
		assertEquals(ZERO, ONE_HALF.multiplyFraction(ZERO)); 			// 1/2 * 0/1 = 0/1
		assertEquals(ONE_HALF, ONE_QUARTER.multiplyFraction(2)); 		// 1/4 * 2 = 1/2	演算後は約分される
		assertEquals(ONE_QUARTER, ONE_HALF.divFraction(2));				// 1/2 / 2 = 1/4
		assertEquals(ONE, ONE_HALF.divFraction(ONE_HALF));				// 1/2 / 1/2 = 1/1
		assertEquals(ONE, THREE_QUARTER.divFraction(THREE_QUARTER));	// 3/4 / 3/4 = 1/1
		assertEquals(ONE_HALF, new Fraction(7, 7).divFraction(2));	// 7/7 / 2 = 1/2	演算後は約分される
	}

	/**
	 * 演算関係のテスト
	 * ・演算結果は約分して返される
	 * ・演算結果が0になるならZEROと等価になる
	 */
	@Test
	public void arithmetics() {
		assertEquals(1, new Fraction(10, 30).abs().numerator());	// 演算後は約分される
		assertEquals(3, new Fraction(10, 30).abs().denominator());// 演算後は約分される
		assertEquals(ONE_THIRD, ONE_THIRD.dup().reduced());				// 1/3の約分 = 1/3
		assertEquals(ONE_THIRD, new Fraction(6, 18).reduced());	// 6/18の約分 = 1/3
		assertEquals(ONE_THIRD, new Fraction(10, 30).reduced());	// 10/30の約分 = 1/3
		assertEquals(ONE, new Fraction(Integer.MAX_VALUE, Integer.MAX_VALUE).reduced());
		assertEquals(ONE_THIRD, ONE_THIRD.dup().abs());					// abs(1/3) = 1/3
		assertEquals(ONE_THIRD, ONE_THIRD.dup().invert().invert());		// -(-1/3) = 1/3
		assertEquals(ONE, ONE_HALF.dup().add(ONE_HALF));				// 1/2 + 1/2 = 1
		assertEquals(ONE_HALF, ONE_QUARTER.dup().add(ONE_QUARTER));		// 1/4 + 1/4 = 1/2	演算後は約分される
		assertEquals(ONE_HALF, THREE_QUARTER.dup().sub(ONE_QUARTER));	// 3/4 - 1/4 = 1/2	演算後は約分される
		assertEquals(ZERO, ONE_HALF.dup().sub(ONE_HALF)); 				// 1/2 - 1/2 = 0
		assertEquals(ZERO, THREE_QUARTER.dup().multiply(0)); 			// 3/4 * 0 = 0/1
		assertEquals(ZERO, ONE_HALF.dup().multiply(ZERO)); 				// 1/2 * 0/1 = 0/1
		assertEquals(ONE_HALF, ONE_QUARTER.dup().multiply(2)); 			// 1/4 * 2 = 1/2	演算後は約分される
		assertEquals(ONE_QUARTER, ONE_HALF.dup().div(2));				// 1/2 / 2 = 1/4
		assertEquals(ONE, ONE_HALF.dup().div(ONE_HALF));				// 1/2 / 1/2 = 1/1
		assertEquals(ONE, THREE_QUARTER.dup().div(THREE_QUARTER));		// 3/4 / 3/4 = 1/1
		assertEquals(ONE_HALF, new Fraction(7, 7).div(2));	// 7/7 / 2 = 1/2	演算後は約分される
	}

	/**
	 * 符号取得のテスト
	 */
	@Test
	public void sign() {
		assertEquals(0, ZERO.sign());
		assertEquals(1, ONE.sign());
		assertEquals(-1, ONE.dup().invert().sign());
		assertEquals(-1, new Fraction(-3, 7).sign());
		assertEquals(-1, new Fraction(3, -7).sign());
		assertEquals(1, new Fraction(-3, -7).sign());
		assertEquals(-1, Integer.signum(Integer.MIN_VALUE));
		assertEquals(-1, Integer.signum(MIN_INTEGER));
		assertEquals(1, Integer.signum(Integer.MAX_VALUE));
		assertEquals(1, new Fraction(Integer.MAX_VALUE, Integer.MAX_VALUE).sign());	// 桁あふれしないことを確認
		assertEquals(-1, new Fraction(MIN_INTEGER, Integer.MAX_VALUE).sign());	// 桁あふれしないことを確認
		assertEquals(-1, new Fraction(Integer.MAX_VALUE, MIN_INTEGER).sign());	// 桁あふれしないことを確認
		assertEquals(1, new Fraction(MIN_INTEGER, MIN_INTEGER).sign());	// 桁あふれしないことを確認
		assertEquals(1, new Fraction(MIN_INTEGER, MIN_INTEGER).sign());	// 桁あふれしないことを確認
	}

	/**
	 * 分母が負にならないことを確認
	 * 分母が負のときは分子の符号が反転することを確認
	 */
	@Test
	public void others() {
		assertEquals(7, new Fraction(5, 7).denominator());
		assertEquals(7, new Fraction(5, -7).denominator());
		assertEquals(-5, new Fraction(5, -7).numerator());
		assertEquals(-5, new Fraction(-5, 7).numerator());
		assertEquals(5, new Fraction(-5, -7).numerator());
	}

//--------------------------------------------------------------------------------
// 定数の値が変化する演算はUnsupportedOperationExceptionを投げることを確認
	@Test(expected = UnsupportedOperationException.class)
	public void unmodifiableReset() {
		ONE.reset();
	}

	@Test(expected = UnsupportedOperationException.class)
	public void unmodifiableInvert() {
		ONE.invert();
	}

	@Test(expected = UnsupportedOperationException.class)
	public void unmodifiableAbs() {
		ONE.abs();
	}

	@Test(expected = UnsupportedOperationException.class)
	public void unmodifiableReduces() {
		ONE.reduced();
	}

	@Test(expected = UnsupportedOperationException.class)
	public void unmodifiableAdd() {
		ONE.add(TWO);
	}

	@Test(expected = UnsupportedOperationException.class)
	public void unmodifiableSub() {
		ONE.sub(TWO);
	}

	@Test(expected = UnsupportedOperationException.class)
	public void unmodifiableMultiply1() {
		ONE.multiply(TWO);
	}

	@Test(expected = UnsupportedOperationException.class)
	public void unmodifiableMultiply2() {
		ONE.multiply(2);
	}

	@Test(expected = UnsupportedOperationException.class)
	public void unmodifiableDiv1() {
		ONE.div(TWO);
	}

	@Test(expected = UnsupportedOperationException.class)
	public void unmodifiableDiv2() {
		ONE.div(2);
	}
}
