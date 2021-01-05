package com.serenegiant.db;
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

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class CursorHelper {
	private static final boolean DEBUG = false;	// FIXME 実働時にはfalseにすること
	private static final String TAG = CursorHelper.class.getSimpleName();

	public static String get(@Nullable final Cursor cursor,
		@NonNull final String columnName, @Nullable final String defaultValue) {

		String result = defaultValue;
		if ((cursor != null) && !cursor.isClosed()) {
			try {
				result = cursor.getString(cursor.getColumnIndexOrThrow(columnName));
			} catch (final Exception e) {
				if (DEBUG) Log.w(TAG, e);
			}
		}
		return result;
	}

	public static CharSequence get(@Nullable final Cursor cursor,
		@NonNull final String columnName, @Nullable final CharSequence defaultValue) {

		CharSequence result = defaultValue;
		if ((cursor != null) && !cursor.isClosed()) {
			try {
				result = cursor.getString(cursor.getColumnIndexOrThrow(columnName));
			} catch (final Exception e) {
				if (DEBUG) Log.w(TAG, e);
			}
		}
		return result;
	}

	public static String getString(@Nullable final Cursor cursor,
		@NonNull final String columnName, @Nullable final CharSequence defaultValue) {

		final CharSequence result = get(cursor, columnName, defaultValue);
		return result != null ? result.toString() : null;
	}

	public static int get(@Nullable final Cursor cursor,
		@NonNull final String columnName, final int defaultValue) {

		int result = defaultValue;
		if ((cursor != null) && !cursor.isClosed()) {
			try {
				result = cursor.getInt(cursor.getColumnIndexOrThrow(columnName));
			} catch (final Exception e) {
				if (DEBUG) Log.w(TAG, e);
			}
		}
		return result;
	}
	
	public static short get(@Nullable final Cursor cursor,
		@NonNull final String columnName, final short defaultValue) {

		short result = defaultValue;
		if ((cursor != null) && !cursor.isClosed()) {
			try {
				result = cursor.getShort(cursor.getColumnIndexOrThrow(columnName));
			} catch (final Exception e) {
				if (DEBUG) Log.w(TAG, e);
			}
		}
		return result;
	}
	
	public static long get(@Nullable final Cursor cursor,
		@NonNull final String columnName, final long defaultValue) {

		long result = defaultValue;
		if ((cursor != null) && !cursor.isClosed()) {
			try {
				result = cursor.getLong(cursor.getColumnIndexOrThrow(columnName));
			} catch (final Exception e) {
				if (DEBUG) Log.w(TAG, e);
			}
		}
		return result;
	}
	
	public static float get(@Nullable final Cursor cursor,
		@NonNull final String columnName, final float defaultValue) {

		float result = defaultValue;
		if ((cursor != null) && !cursor.isClosed()) {
			try {
				result = cursor.getFloat(cursor.getColumnIndexOrThrow(columnName));
			} catch (final Exception e) {
				if (DEBUG) Log.w(TAG, e);
			}
		}
		return result;
	}
	
	public static double get(@Nullable final Cursor cursor,
		@NonNull final String columnName, final double defaultValue) {

		double result = defaultValue;
		if ((cursor != null) && !cursor.isClosed()) {
			try {
				result = cursor.getDouble(cursor.getColumnIndexOrThrow(columnName));
			} catch (final Exception e) {
				if (DEBUG) Log.w(TAG, e);
			}
		}
		return result;
	}

//--------------------------------------------------------------------------------
	@Deprecated
	public static String getString(@Nullable final Cursor cursor,
		@NonNull final String columnName, @Nullable final String defaultValue) {

		return get(cursor, columnName, defaultValue);
	}

	@Deprecated
	public static CharSequence getCharSequence(@Nullable final Cursor cursor,
		@NonNull final String columnName, @Nullable final CharSequence defaultValue) {

		return get(cursor, columnName, defaultValue);
	}

	@Deprecated
	public static int getInt(@Nullable final Cursor cursor,
		@NonNull final String columnName, final int defaultValue) {

		return get(cursor, columnName, defaultValue);
	}

	@Deprecated
	public static short getShort(@Nullable final Cursor cursor,
		@NonNull final String columnName, final short defaultValue) {

		return get(cursor, columnName, defaultValue);
	}

	@Deprecated
	public static long getLong(@Nullable final Cursor cursor,
		@NonNull final String columnName, final long defaultValue) {

		return get(cursor, columnName, defaultValue);
	}

	@Deprecated
	public static float getFloat(@Nullable final Cursor cursor,
		@NonNull final String columnName, final float defaultValue) {

		return get(cursor, columnName, defaultValue);
	}

	@Deprecated
	public static double getDouble(@Nullable final Cursor cursor,
		@NonNull final String columnName, final double defaultValue) {

		return get(cursor, columnName, defaultValue);
	}

//--------------------------------------------------------------------------------
	/**
	 * カラム名"_id"から値を読み取り指定したidと一致するpositionを探す。見つからなければ-1を返す
	 *
	 * @param cursor
	 * @param requestID
	 * @return
	 */
	public static int findPositionFromId(@Nullable final Cursor cursor, final long requestID) {
		int savedPosition, position = -1;
		if ((cursor != null) && !cursor.isClosed()) {
			savedPosition = cursor.getPosition();
			try {
				if (cursor.moveToFirst()) {
					long rowId;
					do {
						rowId = CursorHelper.getLong(cursor, "_id", 0);
						if (rowId == requestID) {
							position = cursor.getPosition();
							break;
						}
					} while (cursor.moveToNext());
				}
			} finally {
				cursor.moveToPosition(savedPosition);
			}
		}
		return position;
	}
	
	@SuppressLint("NewApi")
	public static void dumpCursor(@Nullable final Cursor cursor) {
		if ((cursor != null) && !cursor.isClosed() && cursor.moveToFirst()) {
			final StringBuilder sb = new StringBuilder();
			final int n = cursor.getColumnCount();
			final String[] columnNames = cursor.getColumnNames();
			int row = 0;
			do {
				sb.setLength(0);
				sb.append("row=").append(row).append(", ");
				for (int i = 0; i < n; i++) {
					switch (cursor.getType(i)) {
					case Cursor.FIELD_TYPE_FLOAT:
						sb.append(columnNames[i]).append("=").append(cursor.getDouble(i));
						break;
					case Cursor.FIELD_TYPE_INTEGER:
						sb.append(columnNames[i]).append("=").append(cursor.getLong(i));
						break;
					case Cursor.FIELD_TYPE_STRING:
						sb.append(columnNames[i]).append("=").append(cursor.getString(i));
						break;
					case Cursor.FIELD_TYPE_BLOB:
						sb.append(columnNames[i]).append("=").append("BLOB");
						break;
					case Cursor.FIELD_TYPE_NULL:
						sb.append(columnNames[i]).append("=").append("NULL");
						break;
					default:
						sb.append(columnNames[i]).append("=").append("UNKNOWN");
						break;
					}
					sb.append(", ");
				}
				Log.v(TAG, "dumpCursor:" + sb.toString());
				row++;
			} while (cursor.moveToNext());
		}
	}

	/**
	 * 指定したCursorの現在のレコードを文字列に変換
	 * @param cursor
	 */
	public static String toString(@Nullable final Cursor cursor) {
		if (cursor == null) {
			return "{null}";
		} else if (cursor.isClosed()) {
			return "{closed}";
		} else if (cursor.isBeforeFirst()) {
			return "{before first}";
		} else if (cursor.isAfterLast()) {
			return "{after last}";
		} else {
			final StringBuilder sb = new StringBuilder();
			final int n = cursor.getColumnCount();
			final String[] columnNames = cursor.getColumnNames();
			sb.append("{");
			for (int i = 0; i < n; i++) {
				switch (cursor.getType(i)) {
				case Cursor.FIELD_TYPE_FLOAT:
					sb.append(columnNames[i]).append("=").append(cursor.getDouble(i));
					break;
				case Cursor.FIELD_TYPE_INTEGER:
					sb.append(columnNames[i]).append("=").append(cursor.getLong(i));
					break;
				case Cursor.FIELD_TYPE_STRING:
					sb.append(columnNames[i]).append("=").append(cursor.getString(i));
					break;
				case Cursor.FIELD_TYPE_BLOB:
					sb.append(columnNames[i]).append("=").append("BLOB");
					break;
				case Cursor.FIELD_TYPE_NULL:
					sb.append(columnNames[i]).append("=").append("NULL");
					break;
				default:
					sb.append(columnNames[i]).append("=").append("UNKNOWN");
					break;
				}
				if (i < n-1) {
					sb.append(",");
				}
			}
			sb.append("}");
			return sb.toString();
		}
	}

}
