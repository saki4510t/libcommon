package com.serenegiant.libcommon
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

import com.serenegiant.utils.IEnumKt
import com.serenegiant.utils.asEnum
import org.junit.Assert
import org.junit.Test

class EnumExtTests {
	internal enum class TestEnum1 {
		ENUM1_1,
		ENUM1_2,
		ENUM1_3,
		ENUM1_4,
		ENUM1_5,
	}

	internal enum class TestEnum2(private val id: Int, private val label: String) : IEnumKt {
		ENUM2_1(-1, "enum2-1"),
		ENUM2_2(2, "enum2-2"),
		ENUM2_3(4, "enum2-3"),
		ENUM2_4(6, "enum2-4"),
		ENUM2_5(8, "enum2-5");

		override fun id(): Int {
			return id
		}

		override fun label(): String {
			return label
		}
	}

	/**
	 * 列挙型の検索テスト
	 */
	@Test
	fun asEnumTest1() {
		// ordinalで検索
//		Assert.assertEquals(TestEnum1.ENUM1_1, asEnum(0, TestEnum1::class))		// これは一致する関数がないと言われる
//		Assert.assertEquals(TestEnum1.ENUM1_1, asEnum(0, TestEnum1::javaClass))	// これは一致する関数がないと言われる
//		Assert.assertEquals(TestEnum1.ENUM1_1, asEnum(0, TestEnum1::class.java))// これは一致する関数がないと言われる
		Assert.assertEquals(TestEnum1.ENUM1_1, asEnum(0, TestEnum1.ENUM1_1.javaClass))	// これならOK
		Assert.assertEquals(TestEnum1.ENUM1_2, asEnum(1, TestEnum1.ENUM1_1.javaClass))
		Assert.assertEquals(TestEnum1.ENUM1_3, asEnum(2, TestEnum1.ENUM1_1.javaClass))
		Assert.assertEquals(TestEnum1.ENUM1_4, asEnum(3, TestEnum1.ENUM1_1.javaClass))
		Assert.assertEquals(TestEnum1.ENUM1_5, asEnum(4, TestEnum1.ENUM1_1.javaClass))
		// ordinalで検索
		Assert.assertEquals(TestEnum1.ENUM1_1, asEnum(0, TestEnum1.ENUM1_1))
		Assert.assertEquals(TestEnum1.ENUM1_2, asEnum(1, TestEnum1.ENUM1_1))
		Assert.assertEquals(TestEnum1.ENUM1_3, asEnum(2, TestEnum1.ENUM1_1))
		Assert.assertEquals(TestEnum1.ENUM1_4, asEnum(3, TestEnum1.ENUM1_1))
		Assert.assertEquals(TestEnum1.ENUM1_5, asEnum(4, TestEnum1.ENUM1_1))
		Assert.assertNotEquals(TestEnum1.ENUM1_1, asEnum(1, TestEnum1.ENUM1_1))
		// idで検索
		Assert.assertEquals(TestEnum2.ENUM2_1, asEnum(-1, TestEnum2.ENUM2_1.javaClass))
		Assert.assertEquals(TestEnum2.ENUM2_2, asEnum(2, TestEnum2.ENUM2_1.javaClass))
		Assert.assertEquals(TestEnum2.ENUM2_3, asEnum(4, TestEnum2.ENUM2_1.javaClass))
		Assert.assertEquals(TestEnum2.ENUM2_4, asEnum(6, TestEnum2.ENUM2_1.javaClass))
		Assert.assertEquals(TestEnum2.ENUM2_5, asEnum(8, TestEnum2.ENUM2_1.javaClass))
		// idで検索
		Assert.assertEquals(TestEnum2.ENUM2_1, asEnum(-1, TestEnum2.ENUM2_1))
		Assert.assertEquals(TestEnum2.ENUM2_2, asEnum(2, TestEnum2.ENUM2_1))
		Assert.assertEquals(TestEnum2.ENUM2_3, asEnum(4, TestEnum2.ENUM2_1))
		Assert.assertEquals(TestEnum2.ENUM2_4, asEnum(6, TestEnum2.ENUM2_1))
		Assert.assertEquals(TestEnum2.ENUM2_5, asEnum(8, TestEnum2.ENUM2_1))
		// labelで検索
		Assert.assertEquals(TestEnum2.ENUM2_1, asEnum("enum2-1", TestEnum2.ENUM2_1.javaClass))
		Assert.assertEquals(TestEnum2.ENUM2_2, asEnum("enum2-2", TestEnum2.ENUM2_1.javaClass))
		Assert.assertEquals(TestEnum2.ENUM2_3, asEnum("enum2-3", TestEnum2.ENUM2_1.javaClass))
		Assert.assertEquals(TestEnum2.ENUM2_4, asEnum("enum2-4", TestEnum2.ENUM2_1.javaClass))
		Assert.assertEquals(TestEnum2.ENUM2_5, asEnum("enum2-5", TestEnum2.ENUM2_1.javaClass))
		// labelで検索
		Assert.assertEquals(TestEnum2.ENUM2_1, asEnum("enum2-1", TestEnum2.ENUM2_1))
		Assert.assertEquals(TestEnum2.ENUM2_2, asEnum("enum2-2", TestEnum2.ENUM2_1))
		Assert.assertEquals(TestEnum2.ENUM2_3, asEnum("enum2-3", TestEnum2.ENUM2_1))
		Assert.assertEquals(TestEnum2.ENUM2_4, asEnum("enum2-4", TestEnum2.ENUM2_1))
		Assert.assertEquals(TestEnum2.ENUM2_5, asEnum("enum2-5", TestEnum2.ENUM2_1))
		// nameで検索
		Assert.assertEquals(TestEnum2.ENUM2_1, asEnum("ENUM2_1", TestEnum2.ENUM2_1.javaClass))
		Assert.assertEquals(TestEnum2.ENUM2_2, asEnum("ENUM2_2", TestEnum2.ENUM2_1.javaClass))
		Assert.assertEquals(TestEnum2.ENUM2_3, asEnum("ENUM2_3", TestEnum2.ENUM2_1.javaClass))
		Assert.assertEquals(TestEnum2.ENUM2_4, asEnum("ENUM2_4", TestEnum2.ENUM2_1.javaClass))
		Assert.assertEquals(TestEnum2.ENUM2_5, asEnum("ENUM2_5", TestEnum2.ENUM2_1.javaClass))
		// nameで検索
		Assert.assertEquals(TestEnum2.ENUM2_1, asEnum("ENUM2_1", TestEnum2.ENUM2_1))
		Assert.assertEquals(TestEnum2.ENUM2_2, asEnum("ENUM2_2", TestEnum2.ENUM2_1))
		Assert.assertEquals(TestEnum2.ENUM2_3, asEnum("ENUM2_3", TestEnum2.ENUM2_1))
		Assert.assertEquals(TestEnum2.ENUM2_4, asEnum("ENUM2_4", TestEnum2.ENUM2_1))
		Assert.assertEquals(TestEnum2.ENUM2_5, asEnum("ENUM2_5", TestEnum2.ENUM2_1))
	}

	/**
	 * 列挙型の検索テスト
	 */
	@Test
	fun asEnumTest2() {
		// ordinal/nameで検索
		// これはordinalでヒットする
		Assert.assertEquals(TestEnum1.ENUM1_1, asEnum(0, "ENUM1_1", TestEnum1.ENUM1_1))
		// ordinalではヒットせずnameでヒットする
		Assert.assertEquals(TestEnum1.ENUM1_1, asEnum(10, "ENUM1_1", TestEnum1.ENUM1_1))
		// これはordinalでヒットする
		Assert.assertEquals(TestEnum1.ENUM1_1, asEnum(0, "ENUM1_1", TestEnum1.ENUM1_1.javaClass))
		// ordinalではヒットせずnameでヒットする
		Assert.assertEquals(TestEnum1.ENUM1_1, asEnum(10, "ENUM1_1", TestEnum1.ENUM1_1.javaClass))
		// id/label/ordinal/nameで検索
		// idでヒットする
		Assert.assertEquals(TestEnum2.ENUM2_1, asEnum(-1, "ENUM2_1", TestEnum2.ENUM2_1))
		// ordinal/idではヒットせずnameでヒットする
		Assert.assertEquals(TestEnum2.ENUM2_1, asEnum(10, "ENUM2_1", TestEnum2.ENUM2_1))
		// ordinal/idではヒットせずlabelでヒットする
		Assert.assertEquals(TestEnum2.ENUM2_1, asEnum(10, "enum2-1", TestEnum2.ENUM2_1))
		// idでヒットする
		Assert.assertEquals(TestEnum2.ENUM2_1, asEnum(-1, "ENUM2_1", TestEnum2.ENUM2_1.javaClass))
		// ordinal/idではヒットせずlabelでヒットする
		Assert.assertEquals(TestEnum2.ENUM2_1, asEnum(10, "enum2-1", TestEnum2.ENUM2_1))
		// ordinal/id/labelではヒットせずnameでヒットする
		Assert.assertEquals(TestEnum2.ENUM2_1, asEnum(10, "ENUM2_1", TestEnum2.ENUM2_1.javaClass))
	}

	/**
	 * 見つからないときにNoSuchElementExceptionを投げることを確認
	 */
	@Test(expected = NoSuchElementException::class)
	fun raiseExceptionTest1() {
		Assert.assertEquals(TestEnum1.ENUM1_1, asEnum(10, TestEnum1.ENUM1_1))
	}

	/**
	 * 見つからないときにNoSuchElementExceptionを投げることを確認
	 */
	@Test(expected = NoSuchElementException::class)
	fun raiseExceptionTest2() {
		Assert.assertEquals(TestEnum2.ENUM2_1, asEnum(10, TestEnum2.ENUM2_1))
	}

	/**
	 * 見つからないときにNoSuchElementExceptionを投げることを確認
	 */
	@Test(expected = NoSuchElementException::class)
	fun raiseExceptionTest3() {
		Assert.assertEquals(TestEnum2.ENUM2_1, asEnum("enum", TestEnum2.ENUM2_1))
	}

	/**
	 * 見つからないときにNoSuchElementExceptionを投げることを確認
	 */
	@Test(expected = NoSuchElementException::class)
	fun raiseExceptionTest4() {
		Assert.assertEquals(TestEnum2.ENUM2_1, asEnum("ENUM2_1", TestEnum1.ENUM1_1))
	}
}
