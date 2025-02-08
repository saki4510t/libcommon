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

import android.util.Log;

import com.serenegiant.utils.ArrayUtils;
import com.serenegiant.utils.ClassUtils;
import com.serenegiant.utils.JSONHelper;
import com.serenegiant.utils.ObjectHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4.class)
public class JSONHelperTest {
	private static final String TAG = JSONHelperTest.class.getSimpleName();
	private static final int NUM_VALUES = 10;
	private static final double EPS_DOUBLE = Double.MIN_NORMAL;

	/**
	 * JSONHelperとは関係ないけど、JSONとして16進数での整数指定は仕様外だけどJavaの
	 * JSONObjectのgetInt/optIntは内部的にDouble#parseDoubleへフォールバック
	 * しているので16進数での指定を受け付けることを確認
	 */
	@Test
	public void hexadecimalValueTest() {
		try {
			final JSONObject v = new JSONObject("{\"value\":0x123456}");
			assertEquals(0x123456, v.optInt("value", -1));
		} catch (final Exception e) {
			fail();
		}
	}

	@Test
	public void initTest() {
		try {
			assertNotNull(init(0, 0));
			check(new JSONObject(init(0, 0)), 0, 0);
			assertNotNull(init(1, 1));
			check(new JSONObject(init(1, 1)), 1, 1);
		} catch (final Exception e) {
			Log.w(TAG, e);
			fail();
		}
	}

	@Test
	public void optTest() {
		final int keyOffset = 1;	// これを0煮しちゃうと-0=0で符号反転したキーが重複しちゃうのでこのテストでは0以外必須
		final int valueOffset = 0;
		try {
			final String src = init(keyOffset, valueOffset);
			assertNotNull(src);
			final JSONObject json = new JSONObject(src);
			check(json, keyOffset, valueOffset);
			for (int i = 0; i < NUM_VALUES; i++) {
				// 存在しているキーに対する値取得
				assertEquals((i + valueOffset) % 2 == 0,
					JSONHelper.optBoolean(json,
						"BOOLEAN" + (i + keyOffset),
						(i + valueOffset) % 2 != 0));
				assertEquals((i + valueOffset),
					JSONHelper.optInt(json,
						"INT" + (i + keyOffset),
						(i + valueOffset + 1)));
				assertEquals((i + valueOffset) * 456L,
					JSONHelper.optLong(json,
						"LONG" + (i + keyOffset),
						(i + valueOffset + 1) * 456L));
				assertEquals((i + valueOffset) * 123.0,
					JSONHelper.optDouble(json,
						"DOUBLE" + (i + keyOffset),
						(i + valueOffset + 1) * 123.0),
					EPS_DOUBLE);
				// 存在しないキーに対する値取得
				assertEquals((i + valueOffset) % 2 != 0,
					JSONHelper.optBoolean(json,
						"BOOLEAN" + (-(i + keyOffset)),
						(i + valueOffset) % 2 != 0));
				assertEquals((i + valueOffset + 1),
					JSONHelper.optInt(json,
						"INT" + (-(i + keyOffset)),
						i + valueOffset + 1));
				assertEquals((i + valueOffset + 1) * 456L,
					JSONHelper.optLong(json,
						"LONG" + (-(i + keyOffset)),
						(i + valueOffset + 1) * 456L));
				assertEquals((i + valueOffset + 1) * 123.0,
					JSONHelper.optDouble(json,
						"DOUBLE" + (-(i + keyOffset)),
						(i + valueOffset + 1) * 123.0),
					EPS_DOUBLE);
			}
		} catch (final Exception e) {
			Log.w(TAG, e);
			fail();
		}
	}

	@Test
	public void toArrayTest() {
		final int keyOffset = 0;
		final int valueOffset = 0;
		final Boolean[] booleanArray = new Boolean[] {
			false, true, false, true, false, true, false, true,
		};
		final Integer[] intArray = new Integer[] {
			valueOffset, valueOffset + 1, valueOffset + 2, valueOffset + 3,
			valueOffset + 4, valueOffset + 5, valueOffset + 6, valueOffset + 7,
		};
		final Long[] longArray = new Long[] {
			(long)valueOffset, valueOffset + 1L, valueOffset + 2L, valueOffset + 3L,
			valueOffset + 4L, valueOffset + 5L, valueOffset + 6L, valueOffset + 7L,
		};
		final Double[] doubleArray = new Double[] {
			(double)valueOffset, valueOffset + 1.0, valueOffset + 2.0, valueOffset + 3.0,
			valueOffset + 4.0, valueOffset + 5.0, valueOffset + 6.0, valueOffset + 7.0,
		};
		final String[] stringArray = new String[] {
			valueOffset + "0", valueOffset + "1", valueOffset + "2", valueOffset + "3",
			valueOffset + "4", valueOffset + "5", valueOffset + "6", valueOffset + "7",
		};

		try {
			final String src = init(keyOffset, valueOffset);
			assertNotNull(src);
			final JSONObject json = new JSONObject(src);
			check(json, keyOffset, valueOffset);
			for (int i = 0; i < NUM_VALUES; i++) {
				assertTrue(compare(booleanArray, json.getJSONArray("ARRAY_BOOLEAN" + (i + keyOffset))));
				assertTrue(compare(intArray, json.getJSONArray("ARRAY_INT" + (i + keyOffset))));
				assertTrue(compare(longArray, json.getJSONArray("ARRAY_LONG" + (i + keyOffset))));
				assertTrue(compare(doubleArray, json.getJSONArray("ARRAY_DOUBLE" + (i + keyOffset))));
				assertTrue(compare(stringArray, json.getJSONArray("ARRAY_STRING" + (i + keyOffset))));
				// JSONHelper#getXXXArray
				assertArrayEquals(ArrayUtils.toPrimitiveArray(booleanArray), JSONHelper.getBooleanArray(json, "ARRAY_BOOLEAN" + (i + keyOffset)));
				assertArrayEquals(ArrayUtils.toPrimitiveArray(intArray), JSONHelper.getIntArray(json, "ARRAY_INT" + (i + keyOffset)));
				assertArrayEquals(ArrayUtils.toPrimitiveArray(longArray), JSONHelper.getLongArray(json, "ARRAY_LONG" + (i + keyOffset)));
				assertArrayEquals(ArrayUtils.toPrimitiveArray(doubleArray), JSONHelper.getDoubleArray(json, "ARRAY_DOUBLE" + (i + keyOffset)), EPS_DOUBLE);
				assertArrayEquals(stringArray, JSONHelper.getStringArray(json, "ARRAY_STRING" + (i + keyOffset)));
				// JSONHelper#toArray
				assertArrayEquals(booleanArray, JSONHelper.toArray(json.getJSONArray("ARRAY_BOOLEAN" + (i + keyOffset)), new Boolean[0]));
				assertArrayEquals(intArray, JSONHelper.toArray(json.getJSONArray("ARRAY_INT" + (i + keyOffset)), new Integer[0]));
				assertArrayEquals(longArray, JSONHelper.toArray(json.getJSONArray("ARRAY_LONG" + (i + keyOffset)), new Long[0]));
				assertArrayEquals(doubleArray, JSONHelper.toArray(json.getJSONArray("ARRAY_DOUBLE" + (i + keyOffset)), new Double[0]));
				assertArrayEquals(stringArray, JSONHelper.toArray(json.getJSONArray("ARRAY_STRING" + (i + keyOffset)), new String[0]));
			}
		} catch (final Exception e) {
			Log.w(TAG, e);
			fail();
		}
	}

	/**
	 * テスト用のJSON文字列を生成する
	 * @param keyOffset
	 * @param valueOffset
	 * @return
	 * @throws JSONException
	 */
	private static String init(final int keyOffset, final int valueOffset) throws JSONException {
		final JSONArray booleanArray = new JSONArray(new boolean[] {
			false, true, false, true, false, true, false, true,
		});
		final JSONArray intArray = new JSONArray(new int[] {
			valueOffset, valueOffset + 1, valueOffset + 2, valueOffset + 3,
			valueOffset + 4, valueOffset + 5, valueOffset + 6, valueOffset + 7,
		});
		final JSONArray longArray = new JSONArray(new long[] {
			(long)valueOffset, valueOffset + 1L, valueOffset + 2L, valueOffset + 3L,
			valueOffset + 4L, valueOffset + 5L, valueOffset + 6L, valueOffset + 7L,
		});
		final JSONArray doubleArray = new JSONArray(new double[] {
			(double)valueOffset, valueOffset + 1.0, valueOffset + 2.0, valueOffset + 3.0,
			valueOffset + 4.0, valueOffset + 5.0, valueOffset + 6.0, valueOffset + 7.0,
		});
		final JSONArray stringArray = new JSONArray(new String[] {
			valueOffset + "0", valueOffset + "1", valueOffset + "2", valueOffset + "3",
			valueOffset + "4", valueOffset + "5", valueOffset + "6", valueOffset + "7",
		});

		final JSONObject result = new JSONObject();
		for (int i = 0; i < NUM_VALUES; i++) {
			result.put("BOOLEAN" + (i + keyOffset), (i + valueOffset) % 2 == 0);
			result.put("INT" + (i + keyOffset), (i + valueOffset));
			result.put("LONG" + (i + keyOffset), (i + valueOffset) * 456L);
			result.put("DOUBLE" + (i + keyOffset), (i + valueOffset) * 123.0f);
			result.put("STRING" + (i + keyOffset), Integer.toString((i + valueOffset) * 789));
			result.put("NULL" + (i + keyOffset), null);	// nullをセットするとそのキー自体が削除される
			result.put("ARRAY_BOOLEAN" + (i + keyOffset), booleanArray);
			result.put("ARRAY_INT" + (i + keyOffset), intArray);
			result.put("ARRAY_LONG" + (i + keyOffset), longArray);
			result.put("ARRAY_DOUBLE" + (i + keyOffset), doubleArray);
			result.put("ARRAY_STRING" + (i + keyOffset), stringArray);
		}
		final JSONObject obj = new JSONObject(result.toString());
		final JSONArray objArray = new JSONArray(new JSONObject[] {
			obj, obj, obj, obj, obj, obj, obj, obj,
		});
		for (int i = 0; i < NUM_VALUES; i++) {
			result.put("ARRAY_OBJECT" + (i + keyOffset), objArray);
			result.put("CHILD" + (i + keyOffset), obj);
		}

		return result.toString();
	}

	private void check(
		@NonNull final JSONObject json,
		final int keyOffset, final int valueOffset) throws JSONException {

		final Boolean[] booleanArray = new Boolean[] {
			false, true, false, true, false, true, false, true,
		};
		final Integer[] intArray = new Integer[] {
			valueOffset, valueOffset + 1, valueOffset + 2, valueOffset + 3,
			valueOffset + 4, valueOffset + 5, valueOffset + 6, valueOffset + 7,
		};
		final Long[] longArray = new Long[] {
			(long)valueOffset, valueOffset + 1L, valueOffset + 2L, valueOffset + 3L,
			valueOffset + 4L, valueOffset + 5L, valueOffset + 6L, valueOffset + 7L,
		};
		final Double[] doubleArray = new Double[] {
			(double)valueOffset, valueOffset + 1.0, valueOffset + 2.0, valueOffset + 3.0,
			valueOffset + 4.0, valueOffset + 5.0, valueOffset + 6.0, valueOffset + 7.0,
		};
		final String[] stringArray = new String[] {
			valueOffset + "0", valueOffset + "1", valueOffset + "2", valueOffset + "3",
			valueOffset + "4", valueOffset + "5", valueOffset + "6", valueOffset + "7",
		};

		final int offset2 = valueOffset + 10;
		for (int i = 0; i < NUM_VALUES; i++) {
			assertEquals((i + valueOffset) % 2 == 0, json.optBoolean("BOOLEAN" + (i + keyOffset), (i + offset2) % 2 == 0));
			assertEquals((i + valueOffset), json.optInt("INT" + (i + keyOffset), i + offset2));
			assertEquals((i + valueOffset) * 456L, json.optLong("LONG" + (i + keyOffset), (i + offset2) * 456L));
			assertEquals((i + valueOffset) * 123.0f, json.optDouble("DOUBLE" + (i + keyOffset), (i + offset2) *123.0f), 0.0001f);
			assertEquals(Integer.toString((i + valueOffset) * 789), json.optString("STRING" + (i + keyOffset), Integer.toString((i + offset2) * 789)));

			assertTrue(json.has("ARRAY_BOOLEAN" + (i + keyOffset)));
			assertNotNull(json.getJSONArray("ARRAY_BOOLEAN" + (i + keyOffset)));
			final JSONArray bArray = json.getJSONArray("ARRAY_BOOLEAN" + (i + keyOffset));
			assertEquals(booleanArray.length, bArray.length());
			assertTrue(compare(booleanArray, bArray));

			assertTrue(json.has("ARRAY_INT" + (i + keyOffset)));
			assertNotNull(json.getJSONArray("ARRAY_INT" + (i + keyOffset)));
			final JSONArray iArray = json.getJSONArray("ARRAY_INT" + (i + keyOffset));
			assertEquals(intArray.length, iArray.length());
			assertTrue(compare(intArray, iArray));

			assertTrue(json.has("ARRAY_LONG" + (i + keyOffset)));
			assertNotNull(json.getJSONArray("ARRAY_LONG" + (i + keyOffset)));
			final JSONArray lArray = json.getJSONArray("ARRAY_LONG" + (i + keyOffset));
			assertEquals(longArray.length, lArray.length());
			assertTrue(compare(longArray, lArray));

			assertTrue(json.has("ARRAY_DOUBLE" + (i + keyOffset)));
			assertNotNull(json.getJSONArray("ARRAY_DOUBLE" + (i + keyOffset)));
			final JSONArray dArray = json.getJSONArray("ARRAY_DOUBLE" + (i + keyOffset));
			assertEquals(doubleArray.length, dArray.length());
			assertTrue(compare(doubleArray, dArray));

			assertTrue(json.has("ARRAY_STRING" + (i + keyOffset)));
			assertNotNull(json.getJSONArray("ARRAY_STRING" + (i + keyOffset)));
			final JSONArray sArray = json.getJSONArray("ARRAY_STRING" + (i + keyOffset));
			assertEquals(stringArray.length, sArray.length());
			assertTrue(compare(stringArray, sArray));

			assertTrue(json.has("ARRAY_OBJECT" + (i + keyOffset)));
			assertNotNull(json.getJSONArray("ARRAY_OBJECT" + (i + keyOffset)));

			assertTrue(json.has("CHILD" + (i + keyOffset)));

			// nullをセットするとそのキー自体が削除されるので#hasはfalseを返す。
			// このとき#getはJSONExceptionを投げるけど#optと#isNullは想定通りの値が返る
			assertFalse(json.has("NULL" + (i + keyOffset)));
			assertNull(json.opt("NULL" + (i + keyOffset)));
			assertTrue(json.isNull("NULL" + (i + keyOffset)));
		}
	}

	/**
	 * JSONArrayの中の値が指定した配列と同じかどうかを確認
	 * JSONArrayをlong[], float[], double[]で初期化した場合にもJSONArray#getやJSONArray#optが
	 * 返す値の型がintになってエラーになってしまうので、JSONArrayから取得した値を指定した配列側の
	 * 型に合わせて比較するために、ObjectHelper#equalsを使って判定している
	 * @param expectedArray
	 * @param jsonArray
	 * @return
	 * @param <T>
	 */
	private static <T> boolean compare(
		@NonNull final T[] expectedArray, @NonNull final JSONArray jsonArray) {
		final int sz = expectedArray.length;
		if (sz != jsonArray.length()) return false;
		for (int i = 0; i < sz; i++) {
			final Object value = jsonArray.opt(i);
			if (!ObjectHelper.equals(expectedArray[i], value)) {
				Log.i(TAG, "compare:a=" + expectedArray[i] + "(" + ClassUtils.getClass(expectedArray[i])
					+ "),b=" + value + "(" + ClassUtils.getClass(value) + ")");
				return false;
			}
		}

		return true;
	}
}
