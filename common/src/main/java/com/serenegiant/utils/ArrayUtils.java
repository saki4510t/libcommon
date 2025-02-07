package com.serenegiant.utils;
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

import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * 配列用のヘルパークラス
 */
public class ArrayUtils {
	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = ArrayUtils.class.getSimpleName();

	private ArrayUtils() {
		// インスタンス化をエラーにするためにコンストラクタをprivateに
	}

	public static enum ArrayType {
		NOT_A_ARRAY,
		UNKNOWN_ARRAY,
		BUNDLE_ARRAY,
		CHAR_ARRAY,
		STRING_ARRAY,
		CHARSEQUENCE_ARRAY,
		BYTE_ARRAY,
		SHORT_ARRAY,
		INT_ARRAY,
		LONG_ARRAY,
		FLOAT_ARRAY,
		DOUBLE_ARRAY,
		PARCELABLE_ARRAY,
		SERIALIZABLE_ARRAY,
	}

	/**
	 * 引数が配列ならばその配列要素のクラスを返す
	 * 配列でなければnullを返す
	 * @param obj
	 * @return
	 */
	@Nullable
	public static Class<?> getArrayClass(@Nullable final Object obj) {
		final Class<?> clazz = obj != null ? obj.getClass() : null;
		if ((clazz != null) && clazz.isArray()) {
			return clazz.getComponentType();
		} else {
			return null;
		}
	}

	/**
	 * 引数が配列ならばその配列要素のクラスを返す
	 * 要素がプリミティブまたはプリミティブのボクシングクラスの場合はプリミティブの型を返す
	 * 配列でなければnullを返す
	 * XXX kotlin側でClassUtils.getPrimitiveClass(ArrayUtils.getArrayClass(obj))を呼ぶのと
	 *     Java側でClassUtils.getPrimitiveClass(ArrayUtils.getArrayClass(obj))を呼ぶのとは
	 *     結果が違う(kotlin側で呼ぶとjava.lang.Classになる)
	 * @param obj
	 * @return
	 */
	@Nullable
	public static Class<?> getPrimitiveArrayClass(@Nullable final Object obj) {
		return ClassUtils.getPrimitiveClass(getArrayClass(obj));
	}

	/**
	 * 引数の配列要素の種類を取得する
	 * 配列でなければNOT_A_ARRAY, 未対応配列であればUNKNOWN_ARRAYを返す
	 * プリミティブとそれに対応する型の配列(int/Integerなど)はプリミティブかどうかを区別せず判定する
	 * @param obj
	 * @return
	 */
	@NonNull
	public static ArrayType getArrayType(@Nullable final Object obj) {
		final Class<?> clazz = obj != null ? obj.getClass() : null;
		if ((clazz != null) && clazz.isArray()) {
			final Class<?> arrayClazz = clazz.getComponentType();
			if (arrayClazz == Bundle.class) {
				return ArrayType.BUNDLE_ARRAY;
			} else if ((arrayClazz == char.class) || (arrayClazz == Character.class)) {
				return ArrayType.CHAR_ARRAY;
			} else if (arrayClazz == String.class) {
				return ArrayType.STRING_ARRAY;
			} else if (arrayClazz == CharSequence.class) {
				return ArrayType.CHARSEQUENCE_ARRAY;
			} else if ((arrayClazz == byte.class) || (arrayClazz == Byte.class)) {
				return ArrayType.BYTE_ARRAY;
			} else if ((arrayClazz == short.class) || (arrayClazz == Short.class)) {
				return ArrayType.SHORT_ARRAY;
			} else if ((arrayClazz == int.class) || (arrayClazz == Integer.class)) {
				return ArrayType.INT_ARRAY;
			} else if ((arrayClazz == long.class) || (arrayClazz == Long.class)) {
				return ArrayType.LONG_ARRAY;
			} else if ((arrayClazz == float.class) || (arrayClazz == Float.class)) {
				return ArrayType.FLOAT_ARRAY;
			} else if ((arrayClazz == double.class) || (arrayClazz == Double.class)) {
				return ArrayType.DOUBLE_ARRAY;
			} else {
				@NonNull
				final Class<?>[] intfs = arrayClazz.getInterfaces();
				if (DEBUG) Log.v(TAG, "getArrayType:arrayClazz=" + arrayClazz + ",intfs=" + Arrays.toString(intfs));
				for (final Class<?> intf: intfs) {
					if (intf == Parcelable.class) {
						return ArrayType.PARCELABLE_ARRAY;
					}
					if (intf == Serializable.class) {
						return ArrayType.SERIALIZABLE_ARRAY;
					}
				}
			}
			return ArrayType.UNKNOWN_ARRAY;
		} else {
			return ArrayType.NOT_A_ARRAY;
		}
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
			if (((v != null) && v.equals(value))
				|| ((v == null) && (value == null))) {
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

	/**
	 * 配列をArrayListに変換する
	 * Arrays.asListは実際にはArrayListだけど返り値はListで単純にキャストできないので
	 * ArrayListのコンストラクタへ引き渡してArrayListとして返す
	 * @param array
	 * @param <T>
	 * @return
	 */
	public static <T> ArrayList<T> asList(@NonNull final T[] array) {
		return new ArrayList<>(Arrays.asList(array));
	}

//--------------------------------------------------------------------------------
	/**
	 * float[]をbyte[]に変換して返す
	 *
	 * @param array
	 * @param offset
	 * @param num
	 * @return
	 */
	public static byte[] floatArrayToByteArray(
		@NonNull final float[] array, final int offset, final int num) {

		final ByteBuffer buf = ByteBuffer.allocate(num * Float.SIZE / 8);
		buf.order(ByteOrder.nativeOrder());
		final int n8 = num % 8 + offset;
		final int n = offset + num;
		for (int i = offset; i < n8; i++) buf.putFloat(array[i]);
		for (int i = n8; i < n; i += 8) {
			buf.putFloat(array[i]);
			buf.putFloat(array[i + 1]);
			buf.putFloat(array[i + 2]);
			buf.putFloat(array[i + 3]);
			buf.putFloat(array[i + 4]);
			buf.putFloat(array[i + 5]);
			buf.putFloat(array[i + 6]);
			buf.putFloat(array[i + 7]);
		}
		buf.flip();
		return buf.array();
	}

	/**
	 * byte[]をfloat[]に変換して返す
	 *
	 * @param bytes
	 * @return
	 */
	@Nullable
	public static float[] byteArrayToFloatArray(
		@Nullable final byte[] bytes) {

		if ((bytes == null) || (bytes.length < Float.SIZE / 8)) return null;
		final ByteBuffer tmp = ByteBuffer.wrap(bytes);
		tmp.order(ByteOrder.nativeOrder());
		final int n = tmp.limit() / (Float.SIZE / 8);    // nはfloatの配列とみなした時の要素数
		final float[] array = new float[n];
		final int n8 = n % 8;
		for (int i = 0; i < n8; i++) array[i] = tmp.getFloat();
		for (int i = n8; i < n; i += 8) {
			array[i] = tmp.getFloat();
			array[i + 1] = tmp.getFloat();
			array[i + 2] = tmp.getFloat();
			array[i + 3] = tmp.getFloat();
			array[i + 4] = tmp.getFloat();
			array[i + 5] = tmp.getFloat();
			array[i + 6] = tmp.getFloat();
			array[i + 7] = tmp.getFloat();
		}
		return array;
	}

	/**
	 * double[]をbyte[]に変換して返す
	 *
	 * @param array
	 * @param offset
	 * @param num
	 * @return
	 */
	public static byte[] doubleArrayToByteArray(
		@NonNull final double[] array, final int offset, final int num) {

		final ByteBuffer buf = ByteBuffer.allocate(num * Double.SIZE / 8);
		buf.order(ByteOrder.nativeOrder());
		final int n8 = num % 8 + offset;
		final int n = offset + num;
		for (int i = offset; i < n8; i++) buf.putDouble(array[i]);
		for (int i = n8; i < n; i += 8) {
			buf.putDouble(array[i]);
			buf.putDouble(array[i + 1]);
			buf.putDouble(array[i + 2]);
			buf.putDouble(array[i + 3]);
			buf.putDouble(array[i + 4]);
			buf.putDouble(array[i + 5]);
			buf.putDouble(array[i + 6]);
			buf.putDouble(array[i + 7]);
		}
		buf.flip();
		return buf.array();
	}

	/**
	 * byte[]をdouble[]に変換して返す
	 *
	 * @param bytes
	 * @return
	 */
	@Nullable
	public static double[] byteArrayToDoubleArray(
		@Nullable final byte[] bytes) {

		if ((bytes == null) || (bytes.length < Double.SIZE / 8)) return null;
		final ByteBuffer tmp = ByteBuffer.wrap(bytes);
		tmp.order(ByteOrder.nativeOrder());
		final int n = tmp.limit() / (Double.SIZE / 8);    // nはdoubleの配列とみなした時の要素数
		final double[] array = new double[n];
		final int n8 = n % 8;
		for (int i = 0; i < n8; i++) array[i] = tmp.getDouble();
		for (int i = n8; i < n; i += 8) {
			array[i] = tmp.getDouble();
			array[i + 1] = tmp.getDouble();
			array[i + 2] = tmp.getDouble();
			array[i + 3] = tmp.getDouble();
			array[i + 4] = tmp.getDouble();
			array[i + 5] = tmp.getDouble();
			array[i + 6] = tmp.getDouble();
			array[i + 7] = tmp.getDouble();
		}
		return array;
	}

	/**
	 * double[]をbyte[]に変換して返す
	 *
	 * @param array
	 * @param offset
	 * @param num
	 * @return
	 */
	public static byte[] intArrayToByteArray(
		@NonNull final int[] array, final int offset, final int num) {

		final ByteBuffer buf = ByteBuffer.allocate(num * Integer.SIZE / 8);
		buf.order(ByteOrder.nativeOrder());
		final int n8 = num % 8 + offset;
		final int n = offset + num;
		for (int i = offset; i < n8; i++) buf.putInt(array[i]);
		for (int i = n8; i < n; i += 8) {
			buf.putInt(array[i]);
			buf.putInt(array[i + 1]);
			buf.putInt(array[i + 2]);
			buf.putInt(array[i + 3]);
			buf.putInt(array[i + 4]);
			buf.putInt(array[i + 5]);
			buf.putInt(array[i + 6]);
			buf.putInt(array[i + 7]);
		}
		buf.flip();
		return buf.array();
	}

	/**
	 * byte[]をint[]に変換して返す
	 *
	 * @param bytes
	 * @return
	 */
	@Nullable
	public static int[] byteArrayToIntArray(
		@Nullable final byte[] bytes) {

		if ((bytes == null) || (bytes.length < Integer.SIZE / 8)) return null;
		final ByteBuffer tmp = ByteBuffer.wrap(bytes);
		tmp.order(ByteOrder.nativeOrder());
		final int n = tmp.limit() / (Integer.SIZE / 8);    // nはintの配列とみなした時の要素数
		final int[] array = new int[n];
		final int n8 = n % 8;
		for (int i = 0; i < n8; i++) array[i] = tmp.getInt();
		for (int i = n8; i < n; i += 8) {
			array[i] = tmp.getInt();
			array[i + 1] = tmp.getInt();
			array[i + 2] = tmp.getInt();
			array[i + 3] = tmp.getInt();
			array[i + 4] = tmp.getInt();
			array[i + 5] = tmp.getInt();
			array[i + 6] = tmp.getInt();
			array[i + 7] = tmp.getInt();
		}
		return array;
	}

	/**
	 * short[]をbyte[]に変換して返す
	 *
	 * @param array
	 * @param offset
	 * @param num
	 * @return
	 */
	public static byte[] shortArrayToByteArray(
		@NonNull final short[] array, final int offset, final int num) {

		final ByteBuffer buf = ByteBuffer.allocate(num * Short.SIZE / 8);
		buf.order(ByteOrder.nativeOrder());
		final int n8 = num % 8 + offset;
		final int n = offset + num;
		for (int i = offset; i < n8; i++) buf.putShort(array[i]);
		for (int i = n8; i < n; i += 8) {
			buf.putShort(array[i]);
			buf.putShort(array[i + 1]);
			buf.putShort(array[i + 2]);
			buf.putShort(array[i + 3]);
			buf.putShort(array[i + 4]);
			buf.putShort(array[i + 5]);
			buf.putShort(array[i + 6]);
			buf.putShort(array[i + 7]);
		}
		buf.flip();
		return buf.array();
	}

	/**
	 * byte[]をshort[]に変換して返す
	 *
	 * @param bytes
	 * @return
	 */
	@Nullable
	public static short[] byteArrayToShortArray(
		@Nullable final byte[] bytes) {

		if ((bytes == null) || (bytes.length < Short.SIZE / 8)) return null;
		final ByteBuffer tmp = ByteBuffer.wrap(bytes);
		tmp.order(ByteOrder.nativeOrder());
		final int n = tmp.limit() / (Short.SIZE / 8);    // nはshortの配列とみなした時の要素数
		final short[] buf = new short[n];
		final int n8 = n % 8;
		for (int i = 0; i < n8; i++) buf[i] = tmp.getShort();
		for (int i = n8; i < n; i += 8) {
			buf[i] = tmp.getShort();
			buf[i + 1] = tmp.getShort();
			buf[i + 2] = tmp.getShort();
			buf[i + 3] = tmp.getShort();
			buf[i + 4] = tmp.getShort();
			buf[i + 5] = tmp.getShort();
			buf[i + 6] = tmp.getShort();
			buf[i + 7] = tmp.getShort();
		}
		return buf;
	}

	/**
	 * long[]をbyte[]に変換して返す
	 *
	 * @param array
	 * @param offset
	 * @param num
	 * @return
	 */
	public static byte[] longArrayToByteArray(
		@NonNull final long[] array, final int offset, final int num) {

		final ByteBuffer buf = ByteBuffer.allocate(num * Long.SIZE / 8);
		buf.order(ByteOrder.nativeOrder());
		final int n8 = num % 8 + offset;
		final int n = offset + num;
		for (int i = offset; i < n8; i++) buf.putLong(array[i]);
		for (int i = n8; i < n; i += 8) {
			buf.putLong(array[i]);
			buf.putLong(array[i + 1]);
			buf.putLong(array[i + 2]);
			buf.putLong(array[i + 3]);
			buf.putLong(array[i + 4]);
			buf.putLong(array[i + 5]);
			buf.putLong(array[i + 6]);
			buf.putLong(array[i + 7]);
		}
		buf.flip();
		return buf.array();
	}

	/**
	 * byte[]をlong[]に変換して返す
	 *
	 * @param bytes
	 * @return
	 */
	@Nullable
	public static long[] byteArrayToLongArray(
		@Nullable final byte[] bytes) {

		if ((bytes == null) || (bytes.length < Long.SIZE / 8)) return null;
		final ByteBuffer tmp = ByteBuffer.wrap(bytes);
		tmp.order(ByteOrder.nativeOrder());
		final int n = tmp.limit() / (Long.SIZE / 8);    // nはlongの配列とみなした時の要素数
		final long[] array = new long[n];
		final int n8 = n % 8;
		for (int i = 0; i < n8; i++) array[i] = tmp.getLong();
		for (int i = n8; i < n; i += 8) {
			array[i] = tmp.getLong();
			array[i + 1] = tmp.getLong();
			array[i + 2] = tmp.getLong();
			array[i + 3] = tmp.getLong();
			array[i + 4] = tmp.getLong();
			array[i + 5] = tmp.getLong();
			array[i + 6] = tmp.getLong();
			array[i + 7] = tmp.getLong();
		}
		return array;
	}

//--------------------------------------------------------------------------------
	/**
	 * Javaだとプリミティブの配列とプリミティブのボクシングクラスの配列で互換性がないので
	 * スマートじゃ無いけどアンボクシングしてプリミティブの配列に変換する
	 * arrayの要素がnullの時はfalseとする
	 * @param array
	 * @return
	 */
	@NonNull
	public static boolean[] toPrimitiveArray(@NonNull final Boolean[] array) {
		return toPrimitiveArray(array, false);
	}

	/**
	 * Javaだとプリミティブの配列とプリミティブのボクシングクラスの配列で互換性がないので
	 * スマートじゃ無いけどアンボクシングしてプリミティブの配列に変換する
	 * XXX floatArrayToByteArrayとかと同じでコピーの高速化をした方がよいかも
	 * arrayの要素がnullの時はnullValueとする
	 * @param array
	 * @param nullValue arrayの要素がnullの時の値
	 * @return
	 */
	@NonNull
	public static boolean[] toPrimitiveArray(@NonNull final Boolean[] array, final boolean nullValue) {
		final int n = array.length;;
		final boolean[] result = new boolean[n];
		for (int i = 0; i < n; i++) {
			result[i] = (array[i] != null) ? array[i] : nullValue;
		}

		return result;
	}

	/**
	 * Javaだとプリミティブの配列とプリミティブのボクシングクラスの配列で互換性がないので
	 * スマートじゃ無いけどアンボクシングしてプリミティブの配列に変換する
	 * arrayの要素がnullの時は0とする
	 * @param array
	 * @return
	 */
	@NonNull
	public static byte[] toPrimitiveArray(@NonNull final Byte[] array) {
		return toPrimitiveArray(array, (byte)0);
	}

	/**
	 * Javaだとプリミティブの配列とプリミティブのボクシングクラスの配列で互換性がないので
	 * スマートじゃ無いけどアンボクシングしてプリミティブの配列に変換する
	 * XXX floatArrayToByteArrayとかと同じでコピーの高速化をした方がよいかも
	 * arrayの要素がnullの時はnullValueとする
	 * @param array
	 * @param nullValue arrayの要素がnullの時の値
	 * @return
	 */
	@NonNull
	public static byte[] toPrimitiveArray(@NonNull final Byte[] array, final byte nullValue) {
		final int n = array.length;;
		final byte[] result = new byte[n];
		for (int i = 0; i < n; i++) {
			result[i] = array[i] != null ? array[i] : nullValue;
		}

		return result;
	}

	/**
	 * Javaだとプリミティブの配列とプリミティブのボクシングクラスの配列で互換性がないので
	 * スマートじゃ無いけどアンボクシングしてプリミティブの配列に変換する
	 * arrayの要素がnullの時は0とする
	 * @param array
	 * @return
	 */
	@NonNull
	public static short[] toPrimitiveArray(@NonNull final Short[] array) {
		return toPrimitiveArray(array, (short)0);
	}

	/**
	 * Javaだとプリミティブの配列とプリミティブのボクシングクラスの配列で互換性がないので
	 * スマートじゃ無いけどアンボクシングしてプリミティブの配列に変換する
	 * XXX floatArrayToByteArrayとかと同じでコピーの高速化をした方がよいかも
	 * arrayの要素がnullの時はnullValueとする
	 * @param array
	 * @param nullValue arrayの要素がnullの時の値
	 * @return
	 */
	@NonNull
	public static short[] toPrimitiveArray(@NonNull final Short[] array, final short nullValue) {
		final int n = array.length;;
		final short[] result = new short[n];
		for (int i = 0; i < n; i++) {
			result[i] = array[i] != null ? array[i] : nullValue;
		}

		return result;
	}

	/**
	 * Javaだとプリミティブの配列とプリミティブのボクシングクラスの配列で互換性がないので
	 * スマートじゃ無いけどアンボクシングしてプリミティブの配列に変換する
	 * arrayの要素がnullの時は0とする
	 * @param array
	 * @return
	 */
	@NonNull
	public static int[] toPrimitiveArray(@NonNull final Integer[] array) {
		return toPrimitiveArray(array, 0);
	}

	/**
	 * Javaだとプリミティブの配列とプリミティブのボクシングクラスの配列で互換性がないので
	 * スマートじゃ無いけどアンボクシングしてプリミティブの配列に変換する
	 * XXX floatArrayToByteArrayとかと同じでコピーの高速化をした方がよいかも
	 * arrayの要素がnullの時はnullValueとする
	 * @param array
	 * @param nullValue arrayの要素がnullの時の値
	 * @return
	 */
	@NonNull
	public static int[] toPrimitiveArray(@NonNull final Integer[] array, final int nullValue) {
		final int n = array.length;;
		final int[] result = new int[n];
		for (int i = 0; i < n; i++) {
			result[i] = array[i] != null ? array[i] : nullValue;
		}

		return result;
	}

	/**
	 * Javaだとプリミティブの配列とプリミティブのボクシングクラスの配列で互換性がないので
	 * スマートじゃ無いけどアンボクシングしてプリミティブの配列に変換する
	 * arrayの要素がnullの時は0とする
	 * @param array
	 * @return
	 */
	@NonNull
	public static long[] toPrimitiveArray(@NonNull final Long[] array) {
		return toPrimitiveArray(array, 0);
	}

	/**
	 * Javaだとプリミティブの配列とプリミティブのボクシングクラスの配列で互換性がないので
	 * スマートじゃ無いけどアンボクシングしてプリミティブの配列に変換する
	 * XXX floatArrayToByteArrayとかと同じでコピーの高速化をした方がよいかも
	 * arrayの要素がnullの時はnullValueとする
	 * @param array
	 * @param nullValue arrayの要素がnullの時の値
	 * @return
	 */
	@NonNull
	public static long[] toPrimitiveArray(@NonNull final Long[] array, final long nullValue) {
		final int n = array.length;;
		final long[] result = new long[n];
		for (int i = 0; i < n; i++) {
			result[i] = array[i] != null ? array[i] : nullValue;
		}

		return result;
	}

	/**
	 * Javaだとプリミティブの配列とプリミティブのボクシングクラスの配列で互換性がないので
	 * スマートじゃ無いけどアンボクシングしてプリミティブの配列に変換する
	 * arrayの要素がnullの時はFloat.NaNとする
	 * @param array
	 * @return
	 */
	@NonNull
	public static float[] toPrimitiveArray(@NonNull final Float[] array) {
		return toPrimitiveArray(array, Float.NaN);
	}

	/**
	 * Javaだとプリミティブの配列とプリミティブのボクシングクラスの配列で互換性がないので
	 * スマートじゃ無いけどアンボクシングしてプリミティブの配列に変換する
	 * XXX floatArrayToByteArrayとかと同じでコピーの高速化をした方がよいかも
	 * arrayの要素がnullの時はnullValueとする
	 * @param array
	 * @param nullValue arrayの要素がnullの時の値
	 * @return
	 */
	@NonNull
	public static float[] toPrimitiveArray(@NonNull final Float[] array, final float nullValue) {
		final int n = array.length;;
		final float[] result = new float[n];
		for (int i = 0; i < n; i++) {
			result[i] = array[i] != null ? array[i] : nullValue;
		}

		return result;
	}

	/**
	 * Javaだとプリミティブの配列とプリミティブのボクシングクラスの配列で互換性がないので
	 * スマートじゃ無いけどアンボクシングしてプリミティブの配列に変換する
	 * arrayの要素がnullの時はDouble.NaNとする
	 * @param array
	 * @return
	 */
	@NonNull
	public static double[] toPrimitiveArray(@NonNull final Double[] array) {
		return toPrimitiveArray(array, Double.NaN);
	}

	/**
	 * Javaだとプリミティブの配列とプリミティブのボクシングクラスの配列で互換性がないので
	 * スマートじゃ無いけどアンボクシングしてプリミティブの配列に変換する
	 * XXX floatArrayToByteArrayとかと同じでコピーの高速化をした方がよいかも
	 * arrayの要素がnullの時はnullValueとする
	 * @param array
	 * @param nullValue arrayの要素がnullの時の値
	 * @return
	 */
	@NonNull
	public static double[] toPrimitiveArray(@NonNull final Double[] array, final double nullValue) {
		final int n = array.length;;
		final double[] result = new double[n];
		for (int i = 0; i < n; i++) {
			result[i] = array[i] != null ? array[i] : nullValue;
		}

		return result;
	}
}
