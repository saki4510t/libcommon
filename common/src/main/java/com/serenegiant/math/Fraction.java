package com.serenegiant.math;
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

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * 分数クラス
 * 分子または分母にInteger.MIN_VALUEは使用できない(IllegalArgumentExceptionを生成する)
 * FIXME +/- INFINITY, MIN_VALUE/MAX_VALUE等の異常系境界系のテストは不十分
 * XXX 高速化のために約分なしの計算メソッドを追加したほうがいいかもあ？
 */
public class Fraction implements Parcelable {
	public static final Fraction ZERO			= unmodifiableFraction(0, 1);
	public static final Fraction ONE			= unmodifiableFraction(1, 1);
	public static final Fraction MINUS			= unmodifiableFraction(-1, 1);
	public static final Fraction TWO			= unmodifiableFraction(2, 1);
	public static final Fraction THREE			= unmodifiableFraction(3, 1);
	public static final Fraction FOUR			= unmodifiableFraction(4, 1);
	public static final Fraction FIVE			= unmodifiableFraction(5, 1);
	public static final Fraction TEN			= unmodifiableFraction(10, 1);
	public static final Fraction ONE_HALF		= unmodifiableFraction(1, 2);
	public static final Fraction ONE_THIRD		= unmodifiableFraction(1, 3);
	public static final Fraction ONE_QUARTER	= unmodifiableFraction(1, 4);
	public static final Fraction ONE_FOURTH		= ONE_QUARTER;
	public static final Fraction ONE_FIFTH		= unmodifiableFraction(1, 5);
	public static final Fraction TWO_THIRD		= unmodifiableFraction(2, 3);
	public static final Fraction THREE_QUARTER	= unmodifiableFraction(3, 4);
	public static final Fraction THREE_FOURTH	= THREE_QUARTER;

	private static final double DEFAULT_EPS = 1e-5;
//--------------------------------------------------------------------------------
	/**
	 * 分子
	 */
	private int mNumerator;
	/**
	 * 分母
	 * 基本的に分母は負にならないように計算する
	 */
	private int mDenominator;

	/**
	 * コンストラクタ
	 * 分子=0, 分母=1
	 */
	public Fraction() {
		mDenominator = 1;
	}

	/**
	 * コンストラクタ
	 * 分母は1
	 * @param numerator 分子
	 */
	public Fraction(final int numerator) {
		this.mNumerator = numerator;
		this.mDenominator = 1;
	}

	/**
	 * 指定したdoubleの値から近似値のインスタンスを生成するコンストラクタ
	 * @param value
	 */
	public Fraction(final double value) {
		this(value, DEFAULT_EPS, Integer.MAX_VALUE, 100);
	}

	/**
	 * 指定したdoubleの値から近似値のインスタンスを生成するコンストラクタ
	 * @param value
	 */
	public Fraction(final double value, final double eps) {
		this(value, eps, Integer.MAX_VALUE, 100);
	}

	/**
	 * 指定したdoubleの値から近似値のインスタンスを生成する
	 * この実装はApache Common MathのFractionから
	 * http://home.apache.org/~luc/commons-math-3.6-RC2-site/jacoco/org.apache.commons.math3.fraction/Fraction.java.html
	 * @param value
	 * @param eps
	 * @param maxDenominator
	 * @param maxIterations
	 */
	private Fraction(final double value, final double eps, final int maxDenominator, final int maxIterations) {
		long overflow = Integer.MAX_VALUE;
		double r0 = value;
		long a0 = (long) Math.floor(r0);
		if (Math.abs(a0) > overflow) {
			throw new IllegalArgumentException(String.format("Failed to create Fraction, v=%f,%d,%d)", value, a0, 1L));
		}

		// check for (almost) integer arguments, which should not go to iterations.
		if (Math.abs(a0 - value) < eps) {
			this.mNumerator = (int) a0;
			this.mDenominator = (int) 1;
		}

		long p0 = 1;
		long q0 = 0;
		long p1 = a0;
		long q1 = 1;

		long p2;
		long q2;

		int n = 0;
		boolean stop = false;
		do {
			++n;
			double r1 = 1.0 / (r0 - a0);
			long a1 = (long) Math.floor(r1);
			p2 = (a1 * p1) + p0;
			q2 = (a1 * q1) + q0;

			if ((Math.abs(p2) > overflow) || (Math.abs(q2) > overflow)) {
				// in maxDenominator mode, if the last fraction was very close to the actual value
				// q2 may overflow in the next iteration; in this case return the last one.
				if (eps == 0.0 && Math.abs(q1) < maxDenominator) {
					break;
				}
				throw new IllegalArgumentException(String.format("Failed to create Fraction, v=%f,%d,%d)", value, p2, q2));
			}

			double convergent = (double) p2 / (double) q2;
			if (n < maxIterations && Math.abs(convergent - value) > eps && q2 < maxDenominator) {
				p0 = p1;
				p1 = p2;
				q0 = q1;
				q1 = q2;
				a0 = a1;
				r0 = r1;
			} else {
				stop = true;
			}
		} while (!stop);

		if (n >= maxIterations) {
			throw new IllegalArgumentException(String.format("Failed to create Fraction, v=%f,%d,%d)", value, p2, q2));
		}

		if (q2 < maxDenominator) {
			this.mNumerator = (int) p2;
			this.mDenominator = (int) q2;
		} else {
			this.mNumerator = (int) p1;
			this.mDenominator = (int) q1;
		}
	}

	/**
	 * コンストラクタ
	 * @param numerator 分子
	 * @param denominator 分母, 0だとIllegalArgumentExceptionを投げる
	 * @throws IllegalArgumentException
	 */
	public Fraction(final int numerator, final int denominator) throws IllegalArgumentException {
		// 分母が負にならないようにしておく
		if ((numerator == Integer.MIN_VALUE)
			|| (denominator == Integer.MIN_VALUE)) {
			throw new IllegalArgumentException("numerator/denominator should not MIN_VALUE.");
		}
		if (denominator < 0) {
			this.mNumerator = -numerator;	// 値がInteger.MIN_VALUEなら符号反転しない
			this.mDenominator = -denominator;
		} else if (denominator > 0) {
			this.mNumerator = numerator;
			this.mDenominator = denominator;
		} else {
			throw new IllegalArgumentException("denominator should not zero/MIN_VALUE.");
		}
	}

	/**
	 * コピーコンストラクタ
	 * @param src
	 */
	public Fraction(@Nullable final Fraction src) {
		if (src == null) {
			mNumerator = 0;
			mDenominator = 1;
		} else {
			mNumerator = src.mNumerator;
			mDenominator = src.mDenominator;
		}
	}

	/**
	 * Parcelable実装用のコンストラクタ
	 * @param in
	 */
	protected Fraction(@NonNull final Parcel in) {
		mNumerator = in.readInt();
		mDenominator = in.readInt();
	}

	/**
	 * 分子を取得
	 * @return
	 */
	public int numerator() {
		return mNumerator;
	}

	/**
	 * 分母を取得
	 * @return
	 */
	public int denominator() {
		return mDenominator;
	}

	/**
	 * floatとして値を取得
	 * @return
	 */
	public float asFloat() {
		return mNumerator / (float)mDenominator;
	}

	/**
	 * doubleとして値を取得
	 * @return
	 */
	public double asDouble() {
		return mNumerator / (double)mDenominator;
	}

	/**
	 * Stringとして値を取得
	 * "{分子}/{分母}"
	 * @return
	 */
	public String asString() {
		return mNumerator + "/" + mDenominator;
	}

	/**
	 * 同じ値を持つ新しいインスタンスを生成して返す
	 * 約分はしないのでdup前インスタンス#equals(dup後インスタンス)==true
	 * @return
	 */
	public Fraction dup() {
		return new Fraction(this);
	}

	/**
	 * 符号を取得
	 * @return 正なら1, 負なら-1, 0なら0を返す
	 */
	public int sign() throws IllegalArgumentException {
		return Integer.signum(mNumerator) * Integer.signum(mDenominator);	// API>=1
	}

//--------------------------------------------------------------------------------
// インスタンス自身を変更するメソッド
	/**
	 * デフォルト値(分子=0, 分母=1)をセットする
	 * @return
	 */
	@NonNull
	public Fraction reset() {
		mNumerator = 0;
		mDenominator = 1;
		return this;
	}

	/**
	 * 約分する
	 * このインスタンス自体を変更する
	 * @return
	 */
	@NonNull
	public Fraction reduced() {
		final int gcd = gcd(mNumerator, mDenominator);
		mNumerator /= gcd;
		mDenominator /= gcd;
		// 分母が負にならないようにする
		if (mDenominator < 0) {
			mNumerator = -mNumerator;
			mDenominator = -mDenominator;
		}
		return this;
	}

	/**
	 * 絶対値にする
	 * このインスタンス自体を変更する
	 * 演算結果は約分して返す
	 * @return
	 */
	public Fraction abs() {
		mNumerator = Math.abs(mNumerator);
		mDenominator = Math.abs(mDenominator);
		return reduced();
	}

	/**
	 * 絶対値にする
	 * このインスタンス自体を変更する
	 * 演算結果は約分して返す
	 * @return
	 */
	public Fraction invert() {
		final long v = mNumerator * (long)mDenominator;
		if (v == -0) {
			mNumerator = 0;
		} else if (v == 0) {
			mNumerator = -0;
		} else if (v > 0) {
			mNumerator = -Math.abs(mNumerator);
		} else {
			mNumerator = Math.abs(mNumerator);
		}
		mDenominator = Math.abs(mDenominator);
		return reduced();
	}

	/**
	 * 加算する
	 * このインスタンス自体を変更する
	 * 演算結果は約分して返す
	 * @param value
	 * @return
	 */
	@NonNull
	public Fraction add(@NonNull final Fraction value) {
		mNumerator = value.mDenominator * mNumerator + value.mNumerator * mDenominator;
		mDenominator *= value.mDenominator;
		return reduced();
	}

	/**
	 * 減算する
	 * このインスタンス自体を変更する
	 * 演算結果は約分して返す
	 * @param value
	 * @return
	 */
	@NonNull
	public Fraction sub(@NonNull final Fraction value) {
		mNumerator = value.mDenominator * mNumerator - value.mNumerator * mDenominator;
		mDenominator *= value.mDenominator;
		return reduced();
	}

	/**
	 * 掛け算する
	 * このインスタンス自体を変更する
	 * 演算結果は約分して返す
	 * @param value
	 * @return
	 */
	@NonNull
	public Fraction multiply(@NonNull final Fraction value) {
		mNumerator *= value.mNumerator;
		mDenominator *= value.mDenominator;
		return reduced();
	}

	/**
	 * 掛け算する
	 * このインスタンス自体を変更する
	 * 演算結果は約分して返す
	 * @param value
	 * @return
	 */
	@NonNull
	public Fraction multiply(final int value) {
		mNumerator *= value;
		return reduced();
	}

	/**
	 * 割り算する
	 * このインスタンス自体を変更する
	 * 演算結果は約分して返す
	 * @param value
	 * @return
	 */
	@NonNull
	public Fraction div(@NonNull final Fraction value) {
		// 分子分母を入れ替えて掛ける
		mNumerator *= value.mDenominator;
		mDenominator *= value.mNumerator;
		return reduced();
	}

	/**
	 * 割り算する
	 * このインスタンス自体を変更する
	 * 演算結果は約分して返す
	 * @param value
	 * @return
	 */
	@NonNull
	public Fraction div(final int value) {
		mDenominator *= value;
		return reduced();
	}

//--------------------------------------------------------------------------------
// インスタンス自身へ変更せずに新しいFractionを生成して返すメソッド
	/**
	 * 絶対値にする(もとの値は変更されない)
	 * 演算結果は約分して返す
	 * @return
	 */
	public Fraction absFraction() {
		return reducedFraction(Math.abs(mNumerator), Math.abs(mDenominator));
	}

	/**
	 * 絶対値にする(もとの値は変更されない)
	 * 演算結果は約分して返す
	 * @return
	 */
	public Fraction invertFraction() {
		final long v = mNumerator * (long)mDenominator;
		final int numerator;
		if (v == -0) {
			numerator = 0;
		} else if (v == 0) {
			numerator = -0;
		} else if (v > 0) {
			numerator = -Math.abs(mNumerator);
		} else {
			numerator = Math.abs(mNumerator);
		}
		return reducedFraction(numerator, Math.abs(mDenominator));
	}

	/**
	 * 約分して新しいインスタンスを返す(もとの値は変更されない)
	 * 演算結果は約分して返す
	 * @return
	 */
	@NonNull
	public Fraction reducedFraction() {
		return reducedFraction(mNumerator, mDenominator);
	}

	/**
	 * 加算して新しいインスタンスを返す(もとの値は変更されない)
	 * 演算結果は約分して返す
	 * @param value
	 * @return
	 */
	@NonNull
	public Fraction addFraction(@NonNull final Fraction value) {
		final int numerator = value.mDenominator * mNumerator + value.mNumerator * mDenominator;
		final int denominator = mDenominator * value.mDenominator;
		return reducedFraction(numerator, denominator);
	}

	/**
	 * 減算して新しいインスタンスを返す(もとの値は変更されない)
	 * 演算結果は約分して返す
	 * @param value
	 * @return
	 */
	@NonNull
	public Fraction subFraction(@NonNull final Fraction value) {
		final int numerator = value.mDenominator * mNumerator - value.mNumerator * mDenominator;
		final int denominator = mDenominator * value.mDenominator;
		return reducedFraction(numerator, denominator);
	}

	/**
	 * 掛け算して新しいインスタンスを返す(もとの値は変更されない)
	 * 演算結果は約分して返す
	 * @param value
	 * @return
	 */
	@NonNull
	public Fraction multiplyFraction(@NonNull final Fraction value) {
		return reducedFraction(mNumerator * value.mNumerator, mDenominator * value.mDenominator);
	}

	/**
	 * 掛け算して新しいインスタンスを返す(もとの値は変更されない)
	 * 演算結果は約分して返す
	 * @param value
	 * @return
	 */
	@NonNull
	public Fraction multiplyFraction(final int value) {
		return reducedFraction(mNumerator * value, mDenominator);
	}

	/**
	 * 割り算して新しいインスタンスを返す(もとの値は変更されない)
	 * 演算結果は約分して返す
	 * @param value
	 * @return
	 */
	@NonNull
	public Fraction divFraction(@NonNull final Fraction value) {
		return reducedFraction(mNumerator * value.mDenominator, mDenominator * value.mNumerator);
	}

	/**
	 * 割り算して新しいインスタンスを返す(もとの値は変更されない)
	 * 演算結果は約分して返す
	 * @param value
	 * @return
	 */
	@NonNull
	public Fraction divFraction(final int value) {
		return reducedFraction(mNumerator, mDenominator * value);
	}

//--------------------------------------------------------------------------------
	@Override
	public boolean equals(final Object o) {
		if (this == o) return true;
		if (!(o instanceof Fraction)) return false;
		final Fraction fraction = (Fraction) o;
		return ((mNumerator == fraction.mNumerator)
				&& (mDenominator == fraction.mDenominator));
	}

	@Override
	public int hashCode() {
		return Objects.hash(mNumerator, mDenominator);
	}

	@NonNull
	@Override
	public String toString() {
		return "Fraction{" +
			"numerator=" + mNumerator +
			", denominator=" + mDenominator +
			'}';
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(final Parcel dest, final int flags) {
		dest.writeInt(mNumerator);
		dest.writeInt(mDenominator);
	}

//--------------------------------------------------------------------------------
	/**
	 * 変更できないFractionを生成するヘルパーメソッド
	 * (インスタンス自体の値を変更するメソッドを呼ぶとUnsupportedOperationExceptionを投げる)
	 * @param value
	 * @return
	 */
	public static Fraction unmodifiableFraction(@NonNull  final Fraction value)  {
		return unmodifiableFraction(value.mNumerator, value.mDenominator);
	}

	/**
	 * 変更できないFractionを生成するヘルパーメソッド
	 * (インスタンス自体の値を変更するメソッドを呼ぶとUnsupportedOperationExceptionを投げる)
	 * @param numerator
	 * @param denominator
	 * @return
	 */
	public static Fraction unmodifiableFraction(final int numerator, final int denominator)  {
		return new Fraction(numerator, denominator) {
			@NonNull
			@Override
			public Fraction reset() {
				throw new UnsupportedOperationException("Can't modify this Fraction instance.");
			}
			@Override
			public Fraction abs() {
				throw new UnsupportedOperationException("Can't modify this Fraction instance, use #absFraction instead.");
			}
			@Override
			public Fraction invert() {
				throw new UnsupportedOperationException("Can't modify this Fraction instance, use #invertFraction instead.");
			}
			@NonNull
			@Override
			public Fraction reduced() {
				throw new UnsupportedOperationException("Can't modify this Fraction instance, use #reducedFraction instead.");
			}
			@NonNull
			@Override
			public Fraction add(@NonNull final Fraction value) {
				throw new UnsupportedOperationException("Can't modify this Fraction instance, use #addFraction instead.");
			}
			@NonNull
			@Override
			public Fraction sub(@NonNull final Fraction value) {
				throw new UnsupportedOperationException("Can't modify this Fraction instance, use #subFraction instead.");
			}
			@NonNull
			@Override
			public Fraction multiply(@NonNull final Fraction value) {
				throw new UnsupportedOperationException("Can't modify this Fraction instance, use #multiplyFraction instead.");
			}
			@NonNull
			@Override
			public Fraction multiply(final int value) {
				throw new UnsupportedOperationException("Can't modify this Fraction instance, use #multiplyFraction instead.");
			}
			@NonNull
			@Override
			public Fraction div(@NonNull final Fraction value) {
				throw new UnsupportedOperationException("Can't modify this Fraction instance, use #divFraction instead.");
			}
			@NonNull
			@Override
			public Fraction div(final int value) {
				throw new UnsupportedOperationException("Can't modify this Fraction instance, use #divFraction instead.");
			}
		};
	}

	/**
	 * 約分して新しいFractionインスタンスを生成して返す
	 * ・分母が0ならIllegalArgumentExceptionを生成
	 * ・分子が0ならZEROをコピーして返す
	 * ・それ以外なら約分して新しいインスタンスを生成して返す
	 * @param numerator
	 * @param denominator
	 * @return
	 */
	@NonNull
	private static Fraction reducedFraction(
		final int numerator, final int denominator) throws IllegalArgumentException {
		if (denominator == 0) {
			throw new IllegalArgumentException("denominator should not zero");
		}
		if (numerator == 0) {
			return new Fraction(ZERO);	// ZEROを直接返すと変更できないインスタンスになってしまう
		}
		final int gcd = gcd(numerator, denominator);
		return new Fraction(numerator / gcd, denominator / gcd);
	}

	/**
	 * 最大公約数を計算
	 * @param numerator
	 * @param denominator
	 * @return
	 */
	private static int gcd(final int numerator, final int denominator) {
		int a = Math.abs(numerator), b = Math.abs(denominator);
		while (b > 0) {
			final int c = a;
			a = b;
			b = c % b;
		}
		return a;
	}

	/**
	 * Parcelable実装用ヘルパーオブジェクト
	 */
	public static final Creator<Fraction> CREATOR = new Creator<Fraction>() {
		@Override
		public Fraction createFromParcel(Parcel in) {
			return new Fraction(in);
		}

		@Override
		public Fraction[] newArray(int size) {
			return new Fraction[size];
		}
	};
}
