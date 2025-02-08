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

import com.serenegiant.utils.ObjectHelper;

import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class ObjectHelperTest {

	private static final double EPS_FLOAT = Float.MIN_NORMAL;
	private static final double EPS_DOUBLE = Double.MIN_NORMAL;

	@Test
	public void asBooleanTest() {
		// booleanまたはbooleanの文字列ならその値を返す
		assertFalse(ObjectHelper.asBoolean(false, true));
		assertTrue(ObjectHelper.asBoolean(true, false));
		assertFalse(ObjectHelper.asBoolean("false", true));
		assertTrue(ObjectHelper.asBoolean("true", false));
		assertFalse(ObjectHelper.asBoolean("False", true));
		assertTrue(ObjectHelper.asBoolean("True", false));
		assertFalse(ObjectHelper.asBoolean("FALSE", true));
		assertTrue(ObjectHelper.asBoolean("TRUE", false));
		// 数値として0になればfalseとみなす
		assertFalse(ObjectHelper.asBoolean(0, true));
		assertFalse(ObjectHelper.asBoolean(0.0f, true));
		assertFalse(ObjectHelper.asBoolean(0.0, true));
		assertFalse(ObjectHelper.asBoolean("0", true));
		assertFalse(ObjectHelper.asBoolean("0.0", true));
		assertFalse(ObjectHelper.asBoolean("0x0", true));
		assertFalse(ObjectHelper.asBoolean("-0x0", true));
		assertFalse(ObjectHelper.asBoolean("+0x0", true));
		// 空文字列はデフォルト値を返す
		assertFalse(ObjectHelper.asBoolean("", false));
		assertTrue(ObjectHelper.asBoolean("", true));
		// ブランク文字列(String#trimで空文字列になる空白だけの文字列)はデフォルト値を返す
		assertFalse(ObjectHelper.asBoolean("    ", false));
		assertTrue(ObjectHelper.asBoolean("    ", true));
		// nullはデフォルト値を返す
		assertFalse(ObjectHelper.asBoolean(null, false));
		assertTrue(ObjectHelper.asBoolean(null, true));
		for (int i = 1; i < 100; i++) {
			// 数値として0以外になればtrueと見なす
			assertTrue(ObjectHelper.asBoolean(i, false));
			assertTrue(ObjectHelper.asBoolean((byte)i, false));
			assertTrue(ObjectHelper.asBoolean((short)i, false));
			assertTrue(ObjectHelper.asBoolean((long)i, false));
			assertTrue(ObjectHelper.asBoolean(i * 0.123f, false));
			assertTrue(ObjectHelper.asBoolean(i * 0.456, false));
			assertTrue(ObjectHelper.asBoolean("" + i, false));
			assertTrue(ObjectHelper.asBoolean("0x" + i, false));
			assertTrue(ObjectHelper.asBoolean("-0x" + i, false));
			assertTrue(ObjectHelper.asBoolean("+0x" + i, false));
		}
		for (int i = 0; i < 26; i++) {
			// 解析できない文字はデフォルト値を返す
			assertFalse(ObjectHelper.asBoolean((char)('a' + i), false));
			assertFalse(ObjectHelper.asBoolean((char)('A' + i), false));
			assertTrue(ObjectHelper.asBoolean((char)('a' + i), true));
			assertTrue(ObjectHelper.asBoolean((char)('A' + i), true));
		}
		// booleanとも数値とも解析できない文字列はfalseを返す
		assertFalse(ObjectHelper.asBoolean("abcdefg", false));
		assertTrue(ObjectHelper.asBoolean("abcdefg", true));
	}

	@Test
	public void asByteTest() {
		// booleanまたはbooleanの文字列ならその値を返す
		assertEquals(0, ObjectHelper.asByte(false, Byte.MIN_VALUE));
		assertEquals(1, ObjectHelper.asByte(true, Byte.MIN_VALUE));
		assertEquals(0, ObjectHelper.asByte("false", Byte.MIN_VALUE));
		assertEquals(1, ObjectHelper.asByte("true", Byte.MIN_VALUE));
		assertEquals(0, ObjectHelper.asByte("False", Byte.MIN_VALUE));
		assertEquals(1, ObjectHelper.asByte("True", Byte.MIN_VALUE));
		assertEquals(0, ObjectHelper.asByte("FALSE", Byte.MIN_VALUE));
		assertEquals(1, ObjectHelper.asByte("TRUE", Byte.MIN_VALUE));
		assertEquals(Byte.MIN_VALUE, ObjectHelper.asByte(Byte.MIN_VALUE, Byte.MAX_VALUE));
		assertEquals(Byte.MAX_VALUE, ObjectHelper.asByte(Byte.MAX_VALUE, Byte.MIN_VALUE));
		for (byte i = -100; i < 100; i++) {
			// 空文字列はデフォルト値を返す
			assertEquals(i, ObjectHelper.asByte("", i));
			// ブランク文字列(String#trimで空文字列になる空白だけの文字列)はデフォルト値を返す
			assertEquals(i, ObjectHelper.asByte("    ", i));
			assertEquals(i, ObjectHelper.asByte("    ", i));
			// nullはデフォルト値を返す
			assertEquals(i, ObjectHelper.asByte(null, i));
			// 数値として解析できればその値のint値を返す
			assertEquals(i, ObjectHelper.asByte((byte)i, Byte.MIN_VALUE));
			assertEquals(i, ObjectHelper.asByte((short)i, Byte.MIN_VALUE));
			assertEquals(i, ObjectHelper.asByte((int)i, Byte.MIN_VALUE));
			assertEquals(i, ObjectHelper.asByte((long)i, Byte.MIN_VALUE));
			assertEquals((byte)(i * 0.123f), ObjectHelper.asByte(i * 0.123f, Byte.MIN_VALUE));
			assertEquals((byte)(i * 0.456), ObjectHelper.asByte(i * 0.456, Byte.MIN_VALUE));
			assertEquals(i, ObjectHelper.asByte(i + "", Byte.MIN_VALUE));
			assertEquals(i, ObjectHelper.asByte(i + ".", Byte.MIN_VALUE));
			assertEquals(i, ObjectHelper.asByte(i + ".0", Byte.MIN_VALUE));
			if (i < 0) {
				assertEquals(i, ObjectHelper.asByte("-0x" + Integer.toHexString(Math.abs(i)), Byte.MIN_VALUE));
			} else {
				assertEquals(i, ObjectHelper.asByte("0x" + Integer.toHexString(i), Byte.MIN_VALUE));
				assertEquals(i, ObjectHelper.asByte("+0x" + Integer.toHexString(i), Byte.MIN_VALUE));
			}
			// booleanとも数値とも解析できない文字列はデフォルト値を返す
			assertEquals(i, ObjectHelper.asByte("abcdefg" + i, i));
		}
		for (byte i = 0; i < 26; i++) {
			// 解析できない文字はデフォルト値を返す
			assertEquals(i, ObjectHelper.asByte((char)('a' + i), i));
			assertEquals(i, ObjectHelper.asByte((char)('A' + i), i));
		}
	}

	@Test
	public void asShortTest() {
		// booleanまたはbooleanの文字列ならその値を返す
		assertEquals(0, ObjectHelper.asShort(false, Short.MIN_VALUE));
		assertEquals(1, ObjectHelper.asShort(true, Short.MIN_VALUE));
		assertEquals(0, ObjectHelper.asShort("false", Short.MIN_VALUE));
		assertEquals(1, ObjectHelper.asShort("true", Short.MIN_VALUE));
		assertEquals(0, ObjectHelper.asShort("False", Short.MIN_VALUE));
		assertEquals(1, ObjectHelper.asShort("True", Short.MIN_VALUE));
		assertEquals(0, ObjectHelper.asShort("FALSE", Short.MIN_VALUE));
		assertEquals(1, ObjectHelper.asShort("TRUE", Short.MIN_VALUE));
		assertEquals(Short.MIN_VALUE, ObjectHelper.asShort(Short.MIN_VALUE, Short.MAX_VALUE));
		assertEquals(Short.MAX_VALUE, ObjectHelper.asShort(Short.MAX_VALUE, Short.MIN_VALUE));
		for (short i = -100; i < 100; i++) {
			// 空文字列はデフォルト値を返す
			assertEquals(i, ObjectHelper.asShort("", i));
			// ブランク文字列(String#trimで空文字列になる空白だけの文字列)はデフォルト値を返す
			assertEquals(i, ObjectHelper.asShort("    ", i));
			assertEquals(i, ObjectHelper.asShort("    ", i));
			// nullはデフォルト値を返す
			assertEquals(i, ObjectHelper.asShort(null, i));
			// 数値として解析できればその値のint値を返す
			assertEquals(i, ObjectHelper.asShort((byte)i, Short.MIN_VALUE));
			assertEquals(i, ObjectHelper.asShort((short)i, Short.MIN_VALUE));
			assertEquals(i, ObjectHelper.asShort((int)i, Short.MIN_VALUE));
			assertEquals(i, ObjectHelper.asShort((long)i, Short.MIN_VALUE));
			assertEquals((short)(i * 0.123f), ObjectHelper.asShort(i * 0.123f, Short.MIN_VALUE));
			assertEquals((short)(i * 0.456), ObjectHelper.asShort(i * 0.456, Short.MIN_VALUE));
			assertEquals(i, ObjectHelper.asShort(i + "", Short.MIN_VALUE));
			assertEquals(i, ObjectHelper.asShort(i + ".", Short.MIN_VALUE));
			assertEquals(i, ObjectHelper.asShort(i + ".0", Short.MIN_VALUE));
			if (i < 0) {
				assertEquals(i, ObjectHelper.asShort("-0x" + Integer.toHexString(Math.abs(i)), Short.MIN_VALUE));
			} else {
				assertEquals(i, ObjectHelper.asShort("0x" + Integer.toHexString(i), Short.MIN_VALUE));
				assertEquals(i, ObjectHelper.asShort("+0x" + Integer.toHexString(i), Short.MIN_VALUE));
			}
			// booleanとも数値とも解析できない文字列はデフォルト値を返す
			assertEquals(i, ObjectHelper.asShort("abcdefg" + i, i));
		}
		for (short i = 0; i < 26; i++) {
			// 解析できない文字はデフォルト値を返す
			assertEquals(i, ObjectHelper.asShort((char)('a' + i), i));
			assertEquals(i, ObjectHelper.asShort((char)('A' + i), i));
		}
	}

	@Test
	public void asIntTest() {
		// booleanまたはbooleanの文字列ならその値を返す
		assertEquals(0, ObjectHelper.asInt(false, Integer.MIN_VALUE));
		assertEquals(1, ObjectHelper.asInt(true, Integer.MIN_VALUE));
		assertEquals(0, ObjectHelper.asInt("false", Integer.MIN_VALUE));
		assertEquals(1, ObjectHelper.asInt("true", Integer.MIN_VALUE));
		assertEquals(0, ObjectHelper.asInt("False", Integer.MIN_VALUE));
		assertEquals(1, ObjectHelper.asInt("True", Integer.MIN_VALUE));
		assertEquals(0, ObjectHelper.asInt("FALSE", Integer.MIN_VALUE));
		assertEquals(1, ObjectHelper.asInt("TRUE", Integer.MIN_VALUE));
		assertEquals(Integer.MIN_VALUE, ObjectHelper.asInt(Integer.MIN_VALUE, Integer.MAX_VALUE));
		assertEquals(Integer.MAX_VALUE, ObjectHelper.asInt(Integer.MAX_VALUE, Integer.MIN_VALUE));
		for (int i = -100; i < 100; i++) {
			// 空文字列はデフォルト値を返す
			assertEquals(i, ObjectHelper.asInt("", i));
			// ブランク文字列(String#trimで空文字列になる空白だけの文字列)はデフォルト値を返す
			assertEquals(i, ObjectHelper.asInt("    ", i));
			assertEquals(i, ObjectHelper.asInt("    ", i));
			// nullはデフォルト値を返す
			assertEquals(i, ObjectHelper.asInt(null, i));
			// 数値として解析できればその値のint値を返す
			assertEquals(i, ObjectHelper.asInt((byte)i, Integer.MIN_VALUE));
			assertEquals(i, ObjectHelper.asInt((short)i, Integer.MIN_VALUE));
			assertEquals(i, ObjectHelper.asInt((int)i, Integer.MIN_VALUE));
			assertEquals(i, ObjectHelper.asInt((long)i, Integer.MIN_VALUE));
			assertEquals((int)(i * 0.123f), ObjectHelper.asInt(i * 0.123f, Integer.MIN_VALUE));
			assertEquals((int)(i * 0.456), ObjectHelper.asInt(i * 0.456, Integer.MIN_VALUE));
			assertEquals(i, ObjectHelper.asInt(i + "", Integer.MIN_VALUE));
			assertEquals(i, ObjectHelper.asInt(i + ".", Integer.MIN_VALUE));
			assertEquals(i, ObjectHelper.asInt(i + ".0", Integer.MIN_VALUE));
			if (i < 0) {
				assertEquals(i, ObjectHelper.asInt("-0x" + Integer.toHexString(Math.abs(i)), Integer.MIN_VALUE));
			} else {
				assertEquals(i, ObjectHelper.asInt("0x" + Integer.toHexString(i), Integer.MIN_VALUE));
				assertEquals(i, ObjectHelper.asInt("+0x" + Integer.toHexString(i), Integer.MIN_VALUE));
			}
			// booleanとも数値とも解析できない文字列はデフォルト値を返す
			assertEquals(i, ObjectHelper.asInt("abcdefg" + i, i));
		}
		for (int i = 0; i < 26; i++) {
			// 解析できない文字はデフォルト値を返す
			assertEquals(i, ObjectHelper.asInt((char)('a' + i), i));
			assertEquals(i, ObjectHelper.asInt((char)('A' + i), i));
		}
	}

	@Test
	public void asLongTest() {
		// booleanまたはbooleanの文字列ならその値を返す
		assertEquals(0L, ObjectHelper.asLong(false, Long.MIN_VALUE));
		assertEquals(1L, ObjectHelper.asLong(true, Long.MIN_VALUE));
		assertEquals(0L, ObjectHelper.asLong("false", Long.MIN_VALUE));
		assertEquals(1L, ObjectHelper.asLong("true", Long.MIN_VALUE));
		assertEquals(0L, ObjectHelper.asLong("False", Long.MIN_VALUE));
		assertEquals(1L, ObjectHelper.asLong("True", Long.MIN_VALUE));
		assertEquals(0L, ObjectHelper.asLong("FALSE", Long.MIN_VALUE));
		assertEquals(1L, ObjectHelper.asLong("TRUE", Long.MIN_VALUE));
		assertEquals(Long.MIN_VALUE, ObjectHelper.asLong(Long.MIN_VALUE, Long.MAX_VALUE));
		assertEquals(Long.MAX_VALUE, ObjectHelper.asLong(Long.MAX_VALUE, Long.MIN_VALUE));
		for (long i = -100L; i < 100; i++) {
			// 空文字列はデフォルト値を返す
			assertEquals(i, ObjectHelper.asLong("", i));
			// ブランク文字列(String#trimで空文字列になる空白だけの文字列)はデフォルト値を返す
			assertEquals(i, ObjectHelper.asLong("    ", i));
			assertEquals(i, ObjectHelper.asLong("    ", i));
			// nullはデフォルト値を返す
			assertEquals(i, ObjectHelper.asLong(null, i));
			// 数値として解析できればその値のint値を返す
			assertEquals(i, ObjectHelper.asLong((byte)i, Long.MIN_VALUE));
			assertEquals(i, ObjectHelper.asLong((short)i, Long.MIN_VALUE));
			assertEquals(i, ObjectHelper.asLong((int)i, Long.MIN_VALUE));
			assertEquals(i, ObjectHelper.asLong((long)i, Long.MIN_VALUE));
			assertEquals((long)(i * 0.123f), ObjectHelper.asLong(i * 0.123f, Long.MIN_VALUE));
			assertEquals((long)(i * 0.456), ObjectHelper.asLong(i * 0.456, Long.MIN_VALUE));
			assertEquals(i, ObjectHelper.asLong(i + "", Long.MIN_VALUE));
			assertEquals(i, ObjectHelper.asLong(i + ".", Long.MIN_VALUE));
			assertEquals(i, ObjectHelper.asLong(i + ".0", Long.MIN_VALUE));
			if (i < 0) {
				assertEquals(i, ObjectHelper.asLong("-0x" + Long.toHexString(Math.abs(i)), Long.MIN_VALUE));
			} else {
				assertEquals(i, ObjectHelper.asLong("0x" + Long.toHexString(i), Long.MIN_VALUE));
				assertEquals(i, ObjectHelper.asLong("+0x" + Long.toHexString(i), Long.MIN_VALUE));
			}
			// booleanとも数値とも解析できない文字列はデフォルト値を返す
			assertEquals(i, ObjectHelper.asLong("abcdefg" + i, i));
		}
		for (int i = 0; i < 26; i++) {
			// 解析できない文字はデフォルト値を返す
			assertEquals(i, ObjectHelper.asLong((char)('a' + i), i));
			assertEquals(i, ObjectHelper.asLong((char)('A' + i), i));
		}
	}

	@Test
	public void asFloatTest() {
		// booleanまたはbooleanの文字列ならその値を返す
		assertEquals(0, ObjectHelper.asFloat(false, Float.MIN_VALUE), EPS_FLOAT);
		assertEquals(1, ObjectHelper.asFloat(true, Float.MIN_VALUE), EPS_FLOAT);
		assertEquals(0, ObjectHelper.asFloat("false", Float.MIN_VALUE), EPS_FLOAT);
		assertEquals(1, ObjectHelper.asFloat("true", Float.MIN_VALUE), EPS_FLOAT);
		assertEquals(0, ObjectHelper.asFloat("False", Float.MIN_VALUE), EPS_FLOAT);
		assertEquals(1, ObjectHelper.asFloat("True", Float.MIN_VALUE), EPS_FLOAT);
		assertEquals(0, ObjectHelper.asFloat("FALSE", Float.MIN_VALUE), EPS_FLOAT);
		assertEquals(1, ObjectHelper.asFloat("TRUE", Float.MIN_VALUE), EPS_FLOAT);
		assertEquals(Float.MIN_VALUE, ObjectHelper.asFloat(Float.MIN_VALUE, Float.MAX_VALUE), EPS_FLOAT);
		assertEquals(Float.MAX_VALUE, ObjectHelper.asFloat(Float.MAX_VALUE, Float.MIN_VALUE), EPS_FLOAT);
		for (int i = -100; i < 100; i++) {
			// 空文字列はデフォルト値を返す
			assertEquals(i, ObjectHelper.asFloat("", i), EPS_FLOAT);
			// ブランク文字列(String#trimで空文字列になる空白だけの文字列)はデフォルト値を返す
			assertEquals(i, ObjectHelper.asFloat("    ", i), EPS_FLOAT);
			assertEquals(i, ObjectHelper.asFloat("    ", i), EPS_FLOAT);
			// nullはデフォルト値を返す
			assertEquals(i, ObjectHelper.asFloat(null, i), EPS_FLOAT);
			// 数値として解析できればその値のint値を返す
			assertEquals(i, ObjectHelper.asFloat((byte)i, Float.MIN_VALUE), EPS_FLOAT);
			assertEquals(i, ObjectHelper.asFloat((short)i, Float.MIN_VALUE), EPS_FLOAT);
			assertEquals(i, ObjectHelper.asFloat((int)i, Float.MIN_VALUE), EPS_FLOAT);
			assertEquals(i, ObjectHelper.asFloat((float)i, Float.MIN_VALUE), EPS_FLOAT);
			assertEquals((float)(i * 0.123f), ObjectHelper.asFloat(i * 0.123f, Float.MIN_VALUE), EPS_FLOAT);
			assertEquals((float)(i * 0.456), ObjectHelper.asFloat(i * 0.456, Float.MIN_VALUE), EPS_FLOAT);
			assertEquals(i, ObjectHelper.asFloat(i + "", Float.MIN_VALUE), EPS_FLOAT);
			assertEquals(i, ObjectHelper.asFloat(i + ".", Float.MIN_VALUE), EPS_FLOAT);
			assertEquals(i, ObjectHelper.asFloat(i + ".0", Float.MIN_VALUE), EPS_FLOAT);
			assertEquals(i, ObjectHelper.asFloat(Float.toHexString(i), Float.MIN_VALUE), EPS_FLOAT);
			// booleanとも数値とも解析できない文字列はデフォルト値を返す
			assertEquals(i, ObjectHelper.asFloat("abcdefg" + i, i), EPS_FLOAT);
		}
		for (int i = 0; i < 26; i++) {
			// 解析できない文字はデフォルト値を返す
			assertEquals(i, ObjectHelper.asFloat((char)('a' + i), i), EPS_FLOAT);
			assertEquals(i, ObjectHelper.asFloat((char)('A' + i), i), EPS_FLOAT);
		}
	}

	@Test
	public void asDoubleTest() {
		// booleanまたはbooleanの文字列ならその値を返す
		assertEquals(0, ObjectHelper.asDouble(false, Double.MIN_VALUE), EPS_DOUBLE);
		assertEquals(1, ObjectHelper.asDouble(true, Double.MIN_VALUE), EPS_DOUBLE);
		assertEquals(0, ObjectHelper.asDouble("false", Double.MIN_VALUE), EPS_DOUBLE);
		assertEquals(1, ObjectHelper.asDouble("true", Double.MIN_VALUE), EPS_DOUBLE);
		assertEquals(0, ObjectHelper.asDouble("False", Double.MIN_VALUE), EPS_DOUBLE);
		assertEquals(1, ObjectHelper.asDouble("True", Double.MIN_VALUE), EPS_DOUBLE);
		assertEquals(0, ObjectHelper.asDouble("FALSE", Double.MIN_VALUE), EPS_DOUBLE);
		assertEquals(1, ObjectHelper.asDouble("TRUE", Double.MIN_VALUE), EPS_DOUBLE);
		assertEquals(Double.MIN_VALUE, ObjectHelper.asDouble(Double.MIN_VALUE, Double.MAX_VALUE), EPS_DOUBLE);
		assertEquals(Double.MAX_VALUE, ObjectHelper.asDouble(Double.MAX_VALUE, Double.MIN_VALUE), EPS_DOUBLE);
		for (int i = -100; i < 100; i++) {
			// 空文字列はデフォルト値を返す
			assertEquals(i, ObjectHelper.asDouble("", i), EPS_DOUBLE);
			// ブランク文字列(String#trimで空文字列になる空白だけの文字列)はデフォルト値を返す
			assertEquals(i, ObjectHelper.asDouble("    ", i), EPS_DOUBLE);
			assertEquals(i, ObjectHelper.asDouble("    ", i), EPS_DOUBLE);
			// nullはデフォルト値を返す
			assertEquals(i, ObjectHelper.asDouble(null, i), EPS_DOUBLE);
			// 数値として解析できればその値のint値を返す
			assertEquals(i, ObjectHelper.asDouble((byte)i, Double.MIN_VALUE), EPS_DOUBLE);
			assertEquals(i, ObjectHelper.asDouble((short)i, Double.MIN_VALUE), EPS_DOUBLE);
			assertEquals(i, ObjectHelper.asDouble((int)i, Double.MIN_VALUE), EPS_DOUBLE);
			assertEquals(i, ObjectHelper.asDouble((float)i, Double.MIN_VALUE), EPS_DOUBLE);
			assertEquals((double) (i * 0.123f), ObjectHelper.asDouble(i * 0.123f, Double.MIN_VALUE), EPS_DOUBLE);
			assertEquals((double)(i * 0.456), ObjectHelper.asDouble(i * 0.456, Double.MIN_VALUE), EPS_DOUBLE);
			assertEquals(i, ObjectHelper.asDouble(i + "", Double.MIN_VALUE), EPS_DOUBLE);
			assertEquals(i, ObjectHelper.asDouble(i + ".", Double.MIN_VALUE), EPS_DOUBLE);
			assertEquals(i, ObjectHelper.asDouble(i + ".0", Double.MIN_VALUE), EPS_DOUBLE);
			assertEquals(i, ObjectHelper.asDouble(Double.toHexString(i), Double.MIN_VALUE), EPS_DOUBLE);
			// booleanとも数値とも解析できない文字列はデフォルト値を返す
			assertEquals(i, ObjectHelper.asDouble("abcdefg" + i, i), EPS_DOUBLE);
		}
		for (int i = 0; i < 26; i++) {
			// 解析できない文字はデフォルト値を返す
			assertEquals(i, ObjectHelper.asDouble((char)('a' + i), i), EPS_DOUBLE);
			assertEquals(i, ObjectHelper.asDouble((char)('A' + i), i), EPS_DOUBLE);
		}
	}

	@Test
	public void asTest() {
		assertEquals(false, ObjectHelper.as(false, true));
		assertEquals(true, ObjectHelper.as(true, false));
		assertEquals(false, ObjectHelper.as("false", true));
		assertEquals(true, ObjectHelper.as("true", false));
		assertEquals(false, ObjectHelper.as("False", true));
		assertEquals(true, ObjectHelper.as("True", false));
		assertEquals(false, ObjectHelper.as("FALSE", true));
		assertEquals(true, ObjectHelper.as("TRUE", false));
		// assertEquals(long,long)とassertEquals(Object,Object)が決定できないので明示的にlongへキャスト
		assertEquals(Byte.MIN_VALUE, (long)ObjectHelper.as(Byte.MIN_VALUE, Byte.MAX_VALUE));
		assertEquals(Byte.MAX_VALUE, (long)ObjectHelper.as(Byte.MAX_VALUE, Byte.MIN_VALUE));
		assertEquals(Short.MIN_VALUE, (long)ObjectHelper.as(Short.MIN_VALUE, Short.MAX_VALUE));
		assertEquals(Short.MAX_VALUE, (long)ObjectHelper.as(Short.MAX_VALUE, Short.MIN_VALUE));
		assertEquals(Integer.MIN_VALUE, (long)ObjectHelper.as(Integer.MIN_VALUE, Integer.MAX_VALUE));
		assertEquals(Integer.MAX_VALUE, (long)ObjectHelper.as(Integer.MAX_VALUE, Integer.MIN_VALUE));
		assertEquals(Long.MIN_VALUE, (long)ObjectHelper.as(Long.MIN_VALUE, Long.MAX_VALUE));
		assertEquals(Long.MAX_VALUE, (long)ObjectHelper.as(Long.MAX_VALUE, Long.MIN_VALUE));
		assertEquals(Float.MIN_VALUE, ObjectHelper.as(Float.MIN_VALUE, Float.MAX_VALUE), EPS_FLOAT);
		assertEquals(Float.MAX_VALUE, ObjectHelper.as(Float.MAX_VALUE, Float.MIN_VALUE), EPS_FLOAT);
		assertEquals(Double.MIN_VALUE, ObjectHelper.as(Double.MIN_VALUE, Double.MAX_VALUE), EPS_DOUBLE);
		assertEquals(Double.MAX_VALUE, ObjectHelper.as(Double.MAX_VALUE, Double.MIN_VALUE), EPS_DOUBLE);
		for (int i = -100; i < 100; i++) {
			// 文字列はそのまま返す
			assertEquals("", ObjectHelper.as("", "" + i));
			assertEquals("    ", ObjectHelper.as("    ", "" + i));
			assertEquals("abcdefg", ObjectHelper.as("abcdefg", "" + i));
			// 数値として解析できればその値のint値を返す
			assertEquals(i, (long)ObjectHelper.as((byte)i, Byte.MIN_VALUE));
			assertEquals(i, (long)ObjectHelper.as((short)i, Short.MIN_VALUE));
			assertEquals(i, (long)ObjectHelper.as((int)i, Integer.MIN_VALUE));
			assertEquals(i, (long)ObjectHelper.as((int)i, (Integer.valueOf(Integer.MIN_VALUE))));
			assertEquals(i, (long)ObjectHelper.as((long)i, Long.MIN_VALUE));
			assertEquals(i * 0.123f, ObjectHelper.as(i * 0.123f, Float.MIN_VALUE), EPS_FLOAT);
			assertEquals(i * 0.456, ObjectHelper.as(i * 0.456, Double.MIN_VALUE), EPS_DOUBLE);
			assertEquals(i, (long)ObjectHelper.as(i + "", Integer.MIN_VALUE));
			if (i < 0) {
				assertEquals(i, (long)ObjectHelper.as("-0x" + Integer.toHexString(Math.abs(i)), Integer.MIN_VALUE));
			} else {
				assertEquals(i, (long)ObjectHelper.as("0x" + Integer.toHexString(i), Integer.MIN_VALUE));
			}
			assertEquals(i, ObjectHelper.as(i + ".", Double.MIN_VALUE), EPS_DOUBLE);
			assertEquals(i, ObjectHelper.as(i + ".0", Double.MIN_VALUE), EPS_DOUBLE);
			assertEquals(i, ObjectHelper.as(Double.toHexString(i), Double.MIN_VALUE), EPS_DOUBLE);
			// 解析できない文字列はデフォルト値を返す
			assertEquals(i, (long)ObjectHelper.as("abcdefg" + i, i));
		}
		for (int i = 0; i < 26; i++) {
			// 解析できない文字はデフォルト値を返す
			assertEquals(i, (long)ObjectHelper.as((char)('a' + i), i));
			assertEquals(i, (long)ObjectHelper.as((char)('A' + i), i));
		}
	}
}
