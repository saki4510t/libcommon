package com.serenegiant.utils;

import androidx.annotation.NonNull;

public class ArrayUtils {
	private ArrayUtils() {
		// インスタンス化をエラーにするためにコンストラクタをprivateに
	}

	/**
	 * 指定した値が配列に含まれているかどうかをチェック
	 * T#equalsで一致するかどうかをチェックする
	 * @param values
	 * @param value
	 * @param <T>
	 * @return
	 */
	public static <T> boolean contains(@NonNull final T[] values, final T value) {
		for (final T v: values) {
			if ((v != null) && v.equals(value)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 指定した値が配列に含まれているかどうかをチェック
	 * @param values
	 * @param value
	 * @return
	 */
	public static boolean contains(@NonNull final byte[] values, final byte value) {
		for (final byte v: values) {
			if (v == value) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 指定した値が配列に含まれているかどうかをチェック
	 * @param values
	 * @param value
	 * @return
	 */
	public static boolean contains(@NonNull final short[] values, final short value) {
		for (final short v: values) {
			if (v == value) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 指定した値が配列に含まれているかどうかをチェック
	 * @param values
	 * @param value
	 * @return
	 */
	public static boolean contains(@NonNull final int[] values, final int value) {
		for (final int v: values) {
			if (v == value) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 指定した値が配列に含まれているかどうかをチェック
	 * @param values
	 * @param value
	 * @return
	 */
	public static boolean contains(@NonNull final float[] values, final float value) {
		for (final float v: values) {
			if (Float.compare(v, value) == 0) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 指定した値が配列に含まれているかどうかをチェック
	 * @param values
	 * @param value
	 * @return
	 */
	public static boolean contains(@NonNull final double[] values, final double value) {
		for (final double v: values) {
			if (Double.compare(v, value) == 0) {
				return true;
			}
		}
		return false;
	}

	/**
	 * System.arraycopyのヘルパークラス
	 * srcの全部をdstへコピーする
	 * @param src
	 * @param dst
	 * @param <T>
	 */
	public static <T> void copy(@NonNull final T[] src, @NonNull final T[] dst) {
		System.arraycopy(src, 0, dst, 0, src.length);
	}
}
