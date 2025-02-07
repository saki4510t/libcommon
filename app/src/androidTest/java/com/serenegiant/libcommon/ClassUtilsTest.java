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

import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.serenegiant.libcommon.TestUtils.*;
import static com.serenegiant.utils.ClassUtils.*;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

/**
 * ClassUtilsのテスト
 */
@RunWith(AndroidJUnit4.class)
public class ClassUtilsTest {

	/**
	 * 引数がプリミティブ型でもプリミティブのボクシングクラス型でもプリミティブクラスクラスを返すことを確認
	 * プリミティブ/プリミティブのボクシングクラスでない時はそのままのクラスを返す
	 */
	@Test
	public void primitiveClassTest() {
		assertEquals(Boolean.TYPE, getPrimitiveClass(true));
		assertEquals(Boolean.TYPE, getPrimitiveClass(Boolean.valueOf("true")));
		assertEquals(boolean.class, getPrimitiveClass(true));
		assertEquals(boolean.class, getPrimitiveClass(Boolean.valueOf("false")));

		assertEquals(Byte.TYPE, getPrimitiveClass((byte)0x20));
		assertEquals(Byte.TYPE, getPrimitiveClass(Byte.valueOf("32")));
		assertEquals(byte.class, getPrimitiveClass((byte)0x20));
		assertEquals(byte.class, getPrimitiveClass(Byte.valueOf("32")));

		assertEquals(Short.TYPE, getPrimitiveClass((short)0x20));
		assertEquals(Short.TYPE, getPrimitiveClass(Short.valueOf("32")));
		assertEquals(short.class, getPrimitiveClass((short)0x20));
		assertEquals(short.class, getPrimitiveClass(Short.valueOf("32")));

		assertEquals(Integer.TYPE, getPrimitiveClass(123));
		assertEquals(Integer.TYPE, getPrimitiveClass(Integer.valueOf("123")));
		assertEquals(int.class, getPrimitiveClass(123));
		assertEquals(int.class, getPrimitiveClass(Integer.valueOf("123")));

		assertEquals(Long.TYPE, getPrimitiveClass(123L));
		assertEquals(Long.TYPE, getPrimitiveClass(Long.valueOf("123")));
		assertEquals(long.class, getPrimitiveClass(123L));
		assertEquals(long.class, getPrimitiveClass(Long.valueOf("123")));

		assertEquals(Float.TYPE, getPrimitiveClass(1.0f));
		assertEquals(Float.TYPE, getPrimitiveClass(Float.valueOf("1.0")));
		assertEquals(float.class, getPrimitiveClass(1.0f));
		assertEquals(float.class, getPrimitiveClass(Float.valueOf("1.2")));

		assertEquals(Double.TYPE, getPrimitiveClass(1.0));
		assertEquals(Double.TYPE, getPrimitiveClass(Double.valueOf("1.0")));
		assertEquals(double.class, getPrimitiveClass(1.0));
		assertEquals(double.class, getPrimitiveClass(Double.valueOf("1.2")));

//		assertEquals(Void.TYPE, getPrimitiveClass(void));

		assertEquals(Value.class, getPrimitiveClass(new Value(0)));
		assertEquals(BothValue.class, getPrimitiveClass(new BothValue(0)));
		assertEquals(SerializableValue.class, getPrimitiveClass(new SerializableValue(0)));
	}

	/**
	 * 引数がプリミティブ型でもプリミティブのボクシングクラス型でもボクシングクラスを返すことを確認
	 * プリミティブ/プリミティブのボクシングクラスでない時はそのままのクラスを返す
	 */
	@Test
	public void boxingClassTest() {
		assertEquals(Boolean.class, getBoxingClass(true));
		assertEquals(Boolean.class, getBoxingClass(Boolean.valueOf("true")));
		assertNotEquals(boolean.class, getBoxingClass(true));
		assertNotEquals(boolean.class, getBoxingClass(Boolean.valueOf("false")));

		assertEquals(Byte.class, getBoxingClass((byte)0x20));
		assertEquals(Byte.class, getBoxingClass(Byte.valueOf("32")));
		assertNotEquals(byte.class, getBoxingClass((byte)0x20));
		assertNotEquals(byte.class, getBoxingClass(Byte.valueOf("32")));

		assertEquals(Short.class, getBoxingClass((short)0x20));
		assertEquals(Short.class, getBoxingClass(Short.valueOf("32")));
		assertNotEquals(short.class, getBoxingClass((short)0x20));
		assertNotEquals(short.class, getBoxingClass(Short.valueOf("32")));

		assertEquals(Integer.class, getBoxingClass(123));
		assertEquals(Integer.class, getBoxingClass(Integer.valueOf("123")));
		assertNotEquals(int.class, getBoxingClass(123));
		assertNotEquals(int.class, getBoxingClass(Integer.valueOf("123")));

		assertEquals(Long.class, getBoxingClass(123L));
		assertEquals(Long.class, getBoxingClass(Long.valueOf("123")));
		assertNotEquals(long.class, getBoxingClass(123L));
		assertNotEquals(long.class, getBoxingClass(Long.valueOf("123")));

		assertEquals(Float.class, getBoxingClass(1.0f));
		assertEquals(Float.class, getBoxingClass(Float.valueOf("1.0")));
		assertNotEquals(float.class, getBoxingClass(1.0f));
		assertNotEquals(float.class, getBoxingClass(Float.valueOf("1.2")));

		assertEquals(Double.class, getBoxingClass(1.0));
		assertEquals(Double.class, getBoxingClass(Double.valueOf("1.0")));
		assertNotEquals(double.class, getBoxingClass(1.0));
		assertNotEquals(double.class, getBoxingClass(Double.valueOf("1.2")));

//		assertEquals(Void.TYPE, getBoxingClass(void));

		assertEquals(Value.class, getBoxingClass(new Value(0)));
		assertEquals(BothValue.class, getBoxingClass(new BothValue(0)));
		assertEquals(SerializableValue.class, getBoxingClass(new SerializableValue(0)));
	}

	@Test
	public void toPrimitiveClassTest() {
		assertEquals(Boolean.TYPE, getPrimitiveClass(Boolean.class));
		assertEquals(Boolean.TYPE, getPrimitiveClass(Boolean.TYPE));
		assertEquals(Boolean.TYPE, getPrimitiveClass(boolean.class));

		assertEquals(Byte.TYPE, getPrimitiveClass(Byte.class));
		assertEquals(Byte.TYPE, getPrimitiveClass(Byte.TYPE));
		assertEquals(Byte.TYPE, getPrimitiveClass(byte.class));

		assertEquals(Short.TYPE, getPrimitiveClass(Short.class));
		assertEquals(Short.TYPE, getPrimitiveClass(Short.TYPE));
		assertEquals(Short.TYPE, getPrimitiveClass(short.class));

		assertEquals(Integer.TYPE, getPrimitiveClass(Integer.class));
		assertEquals(Integer.TYPE, getPrimitiveClass(Integer.TYPE));
		assertEquals(Integer.TYPE, getPrimitiveClass(int.class));

		assertEquals(Long.TYPE, getPrimitiveClass(Long.class));
		assertEquals(Long.TYPE, getPrimitiveClass(Long.TYPE));
		assertEquals(Long.TYPE, getPrimitiveClass(long.class));

		assertEquals(Float.TYPE, getPrimitiveClass(Float.class));
		assertEquals(Float.TYPE, getPrimitiveClass(Float.TYPE));
		assertEquals(Float.TYPE, getPrimitiveClass(float.class));

		assertEquals(Double.TYPE, getPrimitiveClass(Double.class));
		assertEquals(Double.TYPE, getPrimitiveClass(Double.TYPE));
		assertEquals(Double.TYPE, getPrimitiveClass(double.class));

		assertEquals(Void.TYPE, getPrimitiveClass(Void.class));
		assertEquals(Void.TYPE, getPrimitiveClass(Void.TYPE));
		assertEquals(Void.TYPE, getPrimitiveClass(void.class));

		assertEquals(Value.class, getPrimitiveClass(Value.class));
		assertEquals(BothValue.class, getPrimitiveClass(BothValue.class));
		assertEquals(SerializableValue.class, getPrimitiveClass(SerializableValue.class));
	}

	@Test
	public void toBoxingClassTest() {
		assertEquals(Boolean.class, getBoxingClass(Boolean.class));
		assertEquals(Boolean.class, getBoxingClass(Boolean.TYPE));
		assertEquals(Boolean.class, getBoxingClass(boolean.class));

		assertEquals(Byte.class, getBoxingClass(Byte.class));
		assertEquals(Byte.class, getBoxingClass(Byte.TYPE));
		assertEquals(Byte.class, getBoxingClass(byte.class));

		assertEquals(Short.class, getBoxingClass(Short.class));
		assertEquals(Short.class, getBoxingClass(Short.TYPE));
		assertEquals(Short.class, getBoxingClass(short.class));

		assertEquals(Integer.class, getBoxingClass(Integer.class));
		assertEquals(Integer.class, getBoxingClass(Integer.TYPE));
		assertEquals(Integer.class, getBoxingClass(int.class));

		assertEquals(Long.class, getBoxingClass(Long.class));
		assertEquals(Long.class, getBoxingClass(Long.TYPE));
		assertEquals(Long.class, getBoxingClass(long.class));

		assertEquals(Float.class, getBoxingClass(Float.class));
		assertEquals(Float.class, getBoxingClass(Float.TYPE));
		assertEquals(Float.class, getBoxingClass(float.class));

		assertEquals(Double.class, getBoxingClass(Double.class));
		assertEquals(Double.class, getBoxingClass(Double.TYPE));
		assertEquals(Double.class, getBoxingClass(double.class));

		assertEquals(Void.class, getBoxingClass(Void.class));
		assertEquals(Void.class, getBoxingClass(Void.TYPE));
		assertEquals(Void.class, getBoxingClass(void.class));

		assertEquals(Value.class, getBoxingClass(Value.class));
		assertEquals(BothValue.class, getBoxingClass(BothValue.class));
		assertEquals(SerializableValue.class, getBoxingClass(SerializableValue.class));
	}

}
