package com.serenegiant.utils
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

import com.serenegiant.utils.IEnum.EnumInterface

interface IEnumKt {
	fun id(): Int
	fun label(): String
}

/**
 * ラベルを指定して対応するenumを探す
 * IEnumKtまたはIEnum.EnumInterfaceを実装している場合はまずlabel()で検索し
 * 見つからなければnameで検索する
 * 	asEnum(TestEnum1::class, "ENUM1_1")				// これは一致する関数がないと言われる
 * 	asEnum(TestEnum1::javaClass, "ENUM1_1")			// これは一致する関数がないと言われる
 *	asEnum(TestEnum1::class.java, "ENUM1_1")		// これは一致する関数がないと言われる
 *	asEnum(TestEnum1.ENUM1_1.javaClass, "ENUM1_1")	// これならOK
 * @param label
 * @param clazz
 */
fun <E: Enum<E>> asEnum(label: String, clazz: Class<Enum<E>>): Enum<E> {
	var result: Enum<E>? = null
	if (IEnumKt::class.java.isAssignableFrom(clazz)) {
		val enums = clazz.enumConstants
		if (enums != null) {
			result = enums.firstOrNull { (it as IEnumKt).label() == label }
		}
	}
	if (EnumInterface::class.java.isAssignableFrom(clazz)) {
		val enums = clazz.enumConstants
		if (enums != null) {
			result = enums.firstOrNull { (it as EnumInterface).label() == label }
		}
	}
	if (result == null) {
		val enums = clazz.enumConstants
		if (enums != null) {
			result = enums.firstOrNull { it.name == label }
		}
	}
	if (result != null) {
		return result
	} else {
		throw NoSuchElementException("Unknown enum label=$label")
	}
}

/**
 * idを指定して対応するenumを探す
 * IEnumKtまたはIEnum.EnumInterfaceを実装している場合はまずid()で検索し
 * 見つからなければordinalで検索する
 * 	asEnum(TestEnum1::class, 0)				// これは一致する関数がないと言われる
 * 	asEnum(TestEnum1::javaClass, 0)			// これは一致する関数がないと言われる
 *	asEnum(TestEnum1::class.java, 0)		// これは一致する関数がないと言われる
 *	asEnum(TestEnum1.ENUM1_1.javaClass, 0)	// これならOK
 * @param id
 * @param clazz
 */
fun <E: Enum<E>> asEnum(id: Int, clazz: Class<Enum<E>>): Enum<E> {
	var result: Enum<E>? = null
	if (IEnumKt::class.java.isAssignableFrom(clazz)) {
		val enums = clazz.enumConstants
		if (enums != null) {
			result = enums.firstOrNull { (it as IEnumKt).id() == id }
		}
	}
	if (EnumInterface::class.java.isAssignableFrom(clazz)) {
		val enums = clazz.enumConstants
		if (enums != null) {
			result = enums.firstOrNull { (it as EnumInterface).id() == id }
		}
	}
	if (result == null) {
		val enums = clazz.enumConstants
		if (enums != null) {
			result = enums.firstOrNull { it.ordinal == id }
		}
	}
	if (result != null) {
		return result
	} else {
		throw NoSuchElementException("Unknown enum id=$id")
	}
}

/**
 * idとラベルを指定して対応するenumを検索する
 * 最初にidで検索して見つかればそれを返す
 * idで検索して見つからなければlabelで検索する
 * @param clazz
 * @param id
 * @param label
 */
fun <E: Enum<E>> asEnum(id: Int, label: String?, clazz: Class<Enum<E>>): Enum<E> {
	try {
		// 最初にidで探す
		return asEnum(id, clazz)
	} catch (e: NoSuchElementException) {
		// ignore
	}
	// 見つからなければlabelで探す
	if (label != null) {
		return asEnum(label, clazz)
	} else {
		throw NoSuchElementException()
	}
}

/**
 * ラベルを指定して対応するenumを探す
 * IEnumKtまたはIEnum.EnumInterfaceを実装している場合はまずlabel()で検索し
 * 見つからなければnameで検索する
 * @param label
 * @param a enumの型を判定するためのオブジェクト 本当はClass<Enum<E>>とかを渡したいけどうまくいかなかった
 */
fun <E: Enum<E>> asEnum(label: String, a: Enum<E>): Enum<E> {
	var result: Enum<E>? = null
	if (a is IEnumKt) {
		val enums = a.javaClass.enumConstants
		if (enums != null) {
			result = enums.firstOrNull { it.label() == label }
		}
	}
	if ((result == null) && (a is EnumInterface)) {
		val enums = a.javaClass.enumConstants
		if (enums != null) {
			result = enums.firstOrNull { it.label() == label }
		}
	}
	if (result == null) {
		val enums = a.javaClass.enumConstants
		if (enums != null) {
			result = enums.firstOrNull { it.name == label }
		}
	}
	if (result != null) {
		return result
	} else {
		throw NoSuchElementException("Unknown enum label=$label")
	}
}

/**
 * idを指定して対応するenumを探す
 * IEnumKtまたはIEnum.EnumInterfaceを実装している場合はまずid()で検索し
 * 見つからなければordinalで検索する
 * @param id
 * @param a enumの型を判定するためのオブジェクト 本当はClass<Enum<E>>とかを渡したいけどうまくいかなかった
 */
fun <E: Enum<E>> asEnum(id: Int, a: Enum<E>): Enum<E> {
	var result: Enum<E>? = null
	if (a is IEnumKt) {
		val enums = a.javaClass.enumConstants
		if (enums != null) {
			result = enums.firstOrNull { it.id() == id }
		}
	}
	if ((result == null) && (a is EnumInterface)) {
		val enums = a.javaClass.enumConstants
		if (enums != null) {
			result = enums.firstOrNull { it.id() == id }
		}
	}
	if (result == null) {
		val enums = a.javaClass.enumConstants
		if (enums != null) {
			result = enums.firstOrNull { it.ordinal == id }
		}
	}
	if (result != null) {
		return result
	} else {
		throw NoSuchElementException("Unknown enum id=$id")
	}
}

/**
 * idとラベルを指定して対応するenumを検索する
 * 最初にidで検索して見つかればそれを返す
 * idで検索して見つからなければlabelで検索する
 * @param id
 * @param label
 * @param a enumの型を判定するためのオブジェクト 本当はClass<Enum<E>>とかを渡したいけどうまくいかなかった
 */
fun <E: Enum<E>> asEnum(id: Int, label: String?, a: Enum<E>): Enum<E> {
	try {
		// 最初にidで探す
		return asEnum(id, a)
	} catch (e: NoSuchElementException) {
		// ignore
	}
	// 見つからなければlabelで探す
	if (label != null) {
		return asEnum(label, a)
	} else {
		throw NoSuchElementException()
	}
}
