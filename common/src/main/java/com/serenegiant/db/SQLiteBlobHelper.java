package com.serenegiant.db;
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

import android.database.Cursor;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.serenegiant.graphics.BitmapHelper;
import com.serenegiant.utils.ArrayUtils;

public class SQLiteBlobHelper {
//	private static final boolean DEBUG = false;	// 実働時はfalseにすること
//	private static final String TAG = SQLiteBlobHelper.class.getSimpleName();
	
	/**
	 * SQLiteStatement#bindBlobのヘルパーメソッド。floatの配列をbyteの配列に変換して割り付ける
	 *
	 * @param stat
	 * @param index
	 * @param array
	 */
	public static void bindBlobFloatArray(@NonNull final SQLiteStatement stat,
		final int index, @NonNull final float[] array) {

		stat.bindBlob(index, ArrayUtils.floatArrayToByteArray(array, 0, array.length));
	}
	
	/**
	 * SQLiteStatement#bindBlobのヘルパーメソッド。floatの配列をbyteの配列に変換して割り付ける
	 *
	 * @param stat
	 * @param index
	 * @param array
	 * @param offset
	 * @param num
	 */
	public static void bindBlobFloatArray(@NonNull final SQLiteStatement stat,
		final int index, @NonNull final float[] array, final int offset, final int num) {

		stat.bindBlob(index, ArrayUtils.floatArrayToByteArray(array, offset, num));
	}
	
	/**
	 * SQLiteStatement#bindBlobのヘルパーメソッド。doubleの配列をbyteの配列に変換して割り付ける
	 *
	 * @param stat
	 * @param index
	 * @param array
	 */
	public static void bindBlobDoubleArray(@NonNull final SQLiteStatement stat,
		final int index, @NonNull final double[] array) {

		stat.bindBlob(index, ArrayUtils.doubleArrayToByteArray(array, 0, array.length));
	}
	
	/**
	 * SQLiteStatement#bindBlobのヘルパーメソッド。doubleの配列をbyteの配列に変換して割り付ける
	 *
	 * @param stat
	 * @param index
	 * @param array
	 * @param offset
	 * @param num
	 */
	public static void bindBlobDoubleArray(@NonNull final SQLiteStatement stat,
		final int index, @NonNull final double[] array, final int offset, final int num) {

		stat.bindBlob(index, ArrayUtils.doubleArrayToByteArray(array, offset, num));
	}
	
	/**
	 * SQLiteStatement#bindBlobのヘルパーメソッド。intの配列をbyteの配列に変換して割り付ける
	 *
	 * @param stat
	 * @param index
	 * @param array
	 */
	public static void bindBlobIntArray(@NonNull final SQLiteStatement stat,
		final int index, @NonNull final int[] array) {

		stat.bindBlob(index, ArrayUtils.intArrayToByteArray(array, 0, array.length));
	}
	
	/**
	 * SQLiteStatement#bindBlobのヘルパーメソッド。intの配列をbyteの配列に変換して割り付ける
	 *
	 * @param stat
	 * @param index
	 * @param array
	 */
	public static void bindBlobIntArray(@NonNull final SQLiteStatement stat,
		final int index, @NonNull final int[] array, final int offset, final int num) {

		stat.bindBlob(index, ArrayUtils.intArrayToByteArray(array, offset, num));
	}
	
	/**
	 * SQLiteStatement#bindBlobのヘルパーメソッド。shortの配列をbyteの配列に変換して割り付ける
	 *
	 * @param stat
	 * @param index
	 * @param array
	 */
	public static void bindBlobShortArray(@NonNull final SQLiteStatement stat,
		final int index, @NonNull final short[] array) {

		stat.bindBlob(index, ArrayUtils.shortArrayToByteArray(array, 0, array.length));
	}
	
	/**
	 * SQLiteStatement#bindBlobのヘルパーメソッド。shortの配列をbyteの配列に変換して割り付ける
	 *
	 * @param stat
	 * @param index
	 * @param array
	 * @param offset
	 * @param num
	 */
	public static void bindBlobShortArray(@NonNull final SQLiteStatement stat,
		final int index, @NonNull final short[] array, final int offset, final int num) {

		stat.bindBlob(index, ArrayUtils.shortArrayToByteArray(array, offset, num));
	}
	
	/**
	 * SQLiteStatement#bindBlobのヘルパーメソッド。longの配列をbyteの配列に変換して割り付ける
	 *
	 * @param stat
	 * @param index
	 * @param array
	 */
	public static void bindBlobLongArray(@NonNull final SQLiteStatement stat,
		final int index, @NonNull final long[] array) {

		stat.bindBlob(index, ArrayUtils.longArrayToByteArray(array, 0, array.length));
	}
	
	/**
	 * SQLiteStatement#bindBlobのヘルパーメソッド。longの配列をbyteの配列に変換して割り付ける
	 *
	 * @param stat
	 * @param index
	 * @param array
	 * @param offset
	 * @param num
	 */
	public static void bindBlobLongArray(@NonNull final SQLiteStatement stat,
		final int index, final long[] array, final int offset, final int num) {

		stat.bindBlob(index, ArrayUtils.longArrayToByteArray(array, offset, num));
	}
	
	/**
	 * SQLiteStatement#bindBlobのヘルパーメソッド。Bitmapをbyteの配列に変換して割り付ける
	 *
	 * @param stat
	 * @param index
	 * @param bitmap
	 */
	public static void bindBlobBitmap(@NonNull final SQLiteStatement stat,
		final int index, @NonNull final Bitmap bitmap) {

		stat.bindBlob(index, BitmapHelper.BitmapToByteArray(bitmap));
	}
	
	/**
	 * Cursor#getBlobのヘルパーメソッド。getBlobの返り値をfloatの配列として変換して返す
	 *
	 * @param cursor
	 * @param columnIndex
	 * @return float[]
	 */
	public static float[] getBlobFloatArray(@NonNull final Cursor cursor,
		final int columnIndex) {

		return ArrayUtils.byteArrayToFloatArray(cursor.getBlob(columnIndex));
	}
	
	/**
	 * Cursor#getBlobのヘルパーメソッド。getBlobの返り値をfloatの配列として変換して返す
	 *
	 * @param cursor
	 * @param columnName
	 * @param defaultValue
	 * @return float[]
	 */
	@Nullable
	public static float[] getBlobFloatArray(@NonNull final Cursor cursor,
		final String columnName, @Nullable final float[] defaultValue) {

		float[] result = ArrayUtils.byteArrayToFloatArray(getBlob(cursor, columnName, null));
		if (result == null) result = defaultValue;
		return result;
	}
	
	/**
	 * Cursor#getBlobのヘルパーメソッド。getBlobの返り値をdoubleの配列として変換して返す
	 *
	 * @param cursor
	 * @param columnIndex
	 * @return double[]
	 */
	public static double[] getBlobDoubleArray(@NonNull final Cursor cursor,
		final int columnIndex) {

		return ArrayUtils.byteArrayToDoubleArray(cursor.getBlob(columnIndex));
	}
	
	/**
	 * Cursor#getBlobのヘルパーメソッド。getBlobの返り値をdoubleの配列として変換して返す
	 *
	 * @param cursor
	 * @param columnName
	 * @param defaultValue
	 * @return double[]
	 */
	@Nullable
	public static double[] getBlobDoubleArray(@NonNull final Cursor cursor,
		final String columnName, @Nullable final double[] defaultValue) {

		double[] result = ArrayUtils.byteArrayToDoubleArray(getBlob(cursor, columnName, null));
		if (result == null) result = defaultValue;
		return result;
	}
	
	/**
	 * Cursor#getBlobのヘルパーメソッド。getBlobの返り値をbyteの配列として変換して返す
	 *
	 * @param cursor
	 * @param columnName
	 * @param defaultValue
	 * @return byte[]
	 */
	@Nullable
	public static byte[] getBlob(@NonNull final Cursor cursor,
		final String columnName, @Nullable final byte[] defaultValue) {

		byte[] result = defaultValue;
		try {
			result = cursor.getBlob(cursor.getColumnIndexOrThrow(columnName));
		} catch (final Exception e) {
//			if (DEBUG) Log.w(TAG, e);
		}
		return result;
	}
	
	/**
	 * Cursor#getBlobのヘルパーメソッド。getBlobの返り値をintの配列として変換して返す
	 *
	 * @param cursor
	 * @param columnIndex
	 * @return int[]
	 */
	public static int[] getBlobIntArray(@NonNull final Cursor cursor,
		final int columnIndex) {

		return ArrayUtils.byteArrayToIntArray(cursor.getBlob(columnIndex));
	}
	
	/**
	 * Cursor#getBlobのヘルパーメソッド。getBlobの返り値をintの配列として変換して返す
	 *
	 * @param cursor
	 * @param columnName
	 * @param defaultValue
	 * @return int[]
	 */
	@Nullable
	public static int[] getBlobIntArray(@NonNull final Cursor cursor,
		final String columnName, @Nullable final int[] defaultValue) {

		int[] result = ArrayUtils.byteArrayToIntArray(getBlob(cursor, columnName, null));
		if (result == null) result = defaultValue;
		return result;
	}
	
	/**
	 * Cursor#getBlobのヘルパーメソッド。getBlobの返り値をshortの配列として変換して返す
	 *
	 * @param cursor
	 * @param columnIndex
	 * @return short[]
	 */
	public static short[] getBlobShortArray(@NonNull final Cursor cursor,
		final int columnIndex) {

		return ArrayUtils.byteArrayToShortArray(cursor.getBlob(columnIndex));
	}
	
	/**
	 * Cursor#getBlobのヘルパーメソッド。getBlobの返り値をshortの配列として変換して返す
	 *
	 * @param cursor
	 * @param columnName
	 * @param defaultValue
	 * @return short[]
	 */
	@Nullable
	public static short[] getBlobShortArray(@NonNull final Cursor cursor,
		final String columnName, @Nullable final short[] defaultValue) {

		short[] result = ArrayUtils.byteArrayToShortArray(getBlob(cursor, columnName, null));
		if (result == null) result = defaultValue;
		return result;
	}
	
	/**
	 * Cursor#getBlobのヘルパーメソッド。getBlobの返り値をlongの配列として変換して返す
	 *
	 * @param cursor
	 * @param columnIndex
	 * @return long[]
	 */
	public static long[] getBlobLongArray(@NonNull final Cursor cursor,
		final int columnIndex) {

		return ArrayUtils.byteArrayToLongArray(cursor.getBlob(columnIndex));
	}
	
	/**
	 * Cursor#getBlobのヘルパーメソッド。getBlobの返り値をlongの配列として変換して返す
	 *
	 * @param cursor
	 * @param columnName
	 * @param defaultValue
	 * @return long[]
	 */
	@Nullable
	public static long[] getBlobLongArray(@NonNull final Cursor cursor,
		final String columnName, @Nullable final long[] defaultValue) {

		long[] result = ArrayUtils.byteArrayToLongArray(getBlob(cursor, columnName, null));
		if (result == null) result = defaultValue;
		return result;
	}
	
	/**
	 * Cursor#getBlobのヘルパーメソッド。getBlobの返り値をBitmapとして変換して返す
	 *
	 * @param cursor
	 * @param columnIndex
	 * @return Bitmap
	 */
	public static Bitmap getBlobBitmap(@NonNull final Cursor cursor,
		final int columnIndex) {

		return BitmapHelper.asBitmap(cursor.getBlob(columnIndex));
	}
	
	/**
	 * Cursor#getBlobのヘルパーメソッド。getBlobの返り値をBitmapとして変換して返す
	 *
	 * @param cursor
	 * @param columnName
	 * @return Bitmap
	 */
	public static Bitmap getBlobBitmap(@NonNull final Cursor cursor,
		final String columnName) {

		return BitmapHelper.asBitmap(getBlob(cursor, columnName, null));
	}
	
	/**
	 * Cursor#getBlobのヘルパーメソッド。getBlobの返り値を指定した大きさに最も近いBitmapに変換して返す
	 *
	 * @param cursor
	 * @param columnIndex
	 * @param requestWidth
	 * @param requestHeight
	 * @return Bitmap
	 */
	@Nullable
	public static Bitmap getBlobBitmap(@NonNull final Cursor cursor,
		final int columnIndex, final int requestWidth, final int requestHeight) {

		return BitmapHelper.asBitmap(cursor.getBlob(columnIndex), requestWidth, requestHeight);
	}
	
	/**
	 * Cursor#getBlobのヘルパーメソッド。getBlobの返り値を指定した大きさに最も近いBitmapに変換して返す
	 *
	 * @param cursor
	 * @param columnName
	 * @param requestWidth
	 * @param requestHeight
	 * @return Bitmap
	 */
	@Nullable
	public static Bitmap getBlobBitmap(@NonNull final Cursor cursor,
		final String columnName, final int requestWidth, final int requestHeight) {

		return BitmapHelper.asBitmap(getBlob(cursor, columnName, null), requestWidth, requestHeight);
	}
	
	/**
	 * Cursor#getBlobのヘルパーメソッド。getBlobの返り値を指定した大きさのBitmapに変換して返す
	 *
	 * @param cursor
	 * @param columnIndex
	 * @param requestWidth
	 * @param requestHeight
	 * @return Bitmap
	 */
	@Nullable
	public static Bitmap getBlobBitmapStrictSize(@NonNull final Cursor cursor,
		final int columnIndex, final int requestWidth, final int requestHeight) {

		return BitmapHelper.asBitmapStrictSize(
			cursor.getBlob(columnIndex), requestWidth, requestHeight);
	}
	
	/**
	 * Cursor#getBlobのヘルパーメソッド。getBlobの返り値を指定した大きさのBitmapに変換して返す
	 *
	 * @param cursor
	 * @param columnName
	 * @param requestWidth
	 * @param requestHeight
	 * @return Bitmap
	 */
	@Nullable
	public static Bitmap getBlobBitmapStrictSize(@NonNull final Cursor cursor,
		final String columnName, final int requestWidth, final int requestHeight) {

		return BitmapHelper.asBitmapStrictSize(
			getBlob(cursor, columnName, null), requestWidth, requestHeight);
	}
}
