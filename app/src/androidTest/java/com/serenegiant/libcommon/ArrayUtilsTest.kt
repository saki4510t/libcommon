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

import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.serenegiant.libcommon.TestUtils.*
import com.serenegiant.utils.ArrayUtils
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ArrayUtilsTest {
	/**
	 * ArrayUtils.getArrayClassが想定通りの値を返すかどうかをテスト
	 * kotlinのChar/Byte/Short/Int/Long/Float/Doubleの
	 * ::class.javaと::class.javaPrimitiveTypeは同じみたい(どちらもJVMのプリミティブタイプを表してる)
	 * でもkotlinのChar/Byte/Short/Int/Long/Float/DoubleのarrayOfNullsやarrayOfで作った配列は
	 * kotlinのクラスではなくjava.langパッケージの対応するクラスになる
	 */
	@Test
	fun arrayClassTest() {
		assertNull(ArrayUtils.getArrayClass(null))
		assertNull(ArrayUtils.getArrayClass(Value(0)))
		assertNull(ArrayUtils.getArrayClass(Any()))

		assertEquals(Bundle::class.java, ArrayUtils.getArrayClass(arrayOfNulls<Bundle>(0)))
		assertEquals(Bundle::class.java, ArrayUtils.getArrayClass(arrayOfNulls<Bundle>(10)))
		assertEquals(Bundle::class.java, ArrayUtils.getArrayClass(arrayOf(Bundle())))

		assertEquals(String::class.java, ArrayUtils.getArrayClass(arrayOfNulls<String>(0)))
		assertEquals(String::class.java, ArrayUtils.getArrayClass(arrayOfNulls<String>(10)))
		assertEquals(String::class.java, ArrayUtils.getArrayClass(arrayOf("")))

		assertEquals(Char::class.java, ArrayUtils.getArrayClass(CharArray(0)))
		assertEquals(Char::class.java, ArrayUtils.getArrayClass(CharArray(10)))

		assertEquals(Character::class.java, ArrayUtils.getArrayClass(arrayOfNulls<Char>(0)))
		assertEquals(Character::class.java, ArrayUtils.getArrayClass(arrayOfNulls<Char>(10)))
		assertEquals(Character::class.java, ArrayUtils.getArrayClass(arrayOf(0x20.toChar())))
		assertEquals(Character::class.java, ArrayUtils.getArrayClass(arrayOf('a')))

		assertEquals(Byte::class.java, ArrayUtils.getArrayClass(ByteArray(0)))
		assertEquals(Byte::class.java, ArrayUtils.getArrayClass(ByteArray(10)))
		assertEquals(Byte::class.java, ArrayUtils.getArrayClass(byteArrayOf(java.lang.Byte.decode("0"))))

		assertEquals(java.lang.Byte::class.java, ArrayUtils.getArrayClass(arrayOfNulls<Byte>(0)))
		assertEquals(java.lang.Byte::class.java, ArrayUtils.getArrayClass(arrayOfNulls<Byte>(10)))
		assertEquals(java.lang.Byte::class.java, ArrayUtils.getArrayClass(arrayOf(0x20.toByte())))
		assertEquals(java.lang.Byte::class.java, ArrayUtils.getArrayClass(arrayOf(java.lang.Byte.decode("0"))))

		assertEquals(Short::class.java, ArrayUtils.getArrayClass(ShortArray(0)))
		assertEquals(Short::class.java, ArrayUtils.getArrayClass(ShortArray(10)))
		assertEquals(Short::class.java, ArrayUtils.getArrayClass(shortArrayOf(java.lang.Short.decode("0"))))

		assertEquals(java.lang.Short::class.java, ArrayUtils.getArrayClass(arrayOfNulls<Short>(0)))
		assertEquals(java.lang.Short::class.java, ArrayUtils.getArrayClass(arrayOfNulls<Short>(10)))
		assertEquals(java.lang.Short::class.java, ArrayUtils.getArrayClass(arrayOf(0x20.toShort())))
		assertEquals(java.lang.Short::class.java, ArrayUtils.getArrayClass(arrayOf(java.lang.Short.decode("0"))))

		// Int::class.javaとInt::class.javaPrimitiveTypeは同じなんかな
		assertEquals(Int::class.java, ArrayUtils.getArrayClass(IntArray(0)))
		assertEquals(Int::class.java, ArrayUtils.getArrayClass(IntArray(10)))
		assertEquals(Int::class.java, ArrayUtils.getArrayClass(intArrayOf(Integer.decode("0"))))

		assertEquals(Integer::class.java, ArrayUtils.getArrayClass(arrayOfNulls<Int>(0)))
		assertEquals(Integer::class.java, ArrayUtils.getArrayClass(arrayOfNulls<Int>(10)))
		assertEquals(Integer::class.java, ArrayUtils.getArrayClass(arrayOf(0x20)))
		assertEquals(Integer::class.java, ArrayUtils.getArrayClass(arrayOf(Integer.decode("0"))))

		assertEquals(Long::class.java, ArrayUtils.getArrayClass(LongArray(0)))
		assertEquals(Long::class.java, ArrayUtils.getArrayClass(LongArray(10)))
		assertEquals(Long::class.java, ArrayUtils.getArrayClass(longArrayOf(java.lang.Long.decode("0"))))

		assertEquals(java.lang.Long::class.java, ArrayUtils.getArrayClass(arrayOfNulls<Long>(0)))
		assertEquals(java.lang.Long::class.java, ArrayUtils.getArrayClass(arrayOfNulls<Long>(10)))
		assertEquals(java.lang.Long::class.java, ArrayUtils.getArrayClass(arrayOf(0x20.toLong())))
		assertEquals(java.lang.Long::class.java, ArrayUtils.getArrayClass(arrayOf(java.lang.Long.decode("0"))))

		assertEquals(Float::class.java, ArrayUtils.getArrayClass(FloatArray(0)))
		assertEquals(Float::class.java, ArrayUtils.getArrayClass(FloatArray(10)))
		assertEquals(Float::class.java, ArrayUtils.getArrayClass(floatArrayOf("0.0".toFloat())))

		assertEquals(java.lang.Float::class.java, ArrayUtils.getArrayClass(arrayOfNulls<Float>(0)))
		assertEquals(java.lang.Float::class.java, ArrayUtils.getArrayClass(arrayOfNulls<Float>(10)))
		assertEquals(java.lang.Float::class.java, ArrayUtils.getArrayClass(arrayOf(0x20.toFloat())))
		assertEquals(java.lang.Float::class.java, ArrayUtils.getArrayClass(arrayOf("0.0".toFloat())))

		assertEquals(Double::class.java, ArrayUtils.getArrayClass(DoubleArray(0)))
		assertEquals(Double::class.java, ArrayUtils.getArrayClass(DoubleArray(10)))
		assertEquals(Double::class.java, ArrayUtils.getArrayClass(doubleArrayOf("0.0".toDouble())))

		assertEquals(java.lang.Double::class.java, ArrayUtils.getArrayClass(arrayOfNulls<Double>(0)))
		assertEquals(java.lang.Double::class.java, ArrayUtils.getArrayClass(arrayOfNulls<Double>(10)))
		assertEquals(java.lang.Double::class.java, ArrayUtils.getArrayClass(arrayOf(0x20.toDouble())))
		assertEquals(java.lang.Double::class.java, ArrayUtils.getArrayClass(arrayOf("0.0".toDouble())))

		// Parcelableを実装していればPARCELABLE_ARRAY
		assertEquals(ParcelableValue::class.java, ArrayUtils.getArrayClass(arrayOfNulls<ParcelableValue>(0)))
		assertEquals(ParcelableValue::class.java, ArrayUtils.getArrayClass(arrayOfNulls<ParcelableValue>(10)))
		assertEquals(ParcelableValue::class.java, ArrayUtils.getArrayClass(arrayOf(ParcelableValue(0))))

		// ParcelableとSerializableの両方を実装している場合はPARCELABLE_ARRAY
		assertEquals(BothValue::class.java, ArrayUtils.getArrayClass(arrayOfNulls<BothValue>(0)))
		assertEquals(BothValue::class.java, ArrayUtils.getArrayClass(arrayOfNulls<BothValue>(10)))
		assertEquals(BothValue::class.java, ArrayUtils.getArrayClass(arrayOf(BothValue(0))))

		// Serializableを実装していればSERIALIZABLE_ARRAY
		assertEquals(SerializableValue::class.java, ArrayUtils.getArrayClass(arrayOfNulls<SerializableValue>(0)))
		assertEquals(SerializableValue::class.java, ArrayUtils.getArrayClass(arrayOfNulls<SerializableValue>(10)))
		assertEquals(SerializableValue::class.java, ArrayUtils.getArrayClass(arrayOf(SerializableValue(0))))

		// その他クラスの亜配列はUNKNOWN_ARRAY
		assertEquals(Value::class.java, ArrayUtils.getArrayClass(arrayOfNulls<Value>(0)))
		assertEquals(Value::class.java, ArrayUtils.getArrayClass(arrayOfNulls<Value>(10)))
		assertEquals(Value::class.java, ArrayUtils.getArrayClass(arrayOf(Value(0))))
		assertEquals(Void::class.java, ArrayUtils.getArrayClass(arrayOfNulls<Void>(0)))
		assertEquals(Void::class.java, ArrayUtils.getArrayClass(arrayOfNulls<Void>(10)))
		assertEquals(Any::class.java, ArrayUtils.getArrayClass(arrayOfNulls<Any>(0)))
		assertEquals(Any::class.java, ArrayUtils.getArrayClass(arrayOfNulls<Any>(10)))
	}

	/**
	 * ArrayUtils.getPrimitiveArrayClassが想定通りの値を返すかどうかをテスト
	 * ArrayUtils.getPrimitiveArrayClassの実体はClassUtils.getPrimitiveClass(ArrayUtils.getArrayClass)だけど
	 * Java側でClassUtils.getPrimitiveClass(ArrayUtils.getArrayClass)を呼ぶのと
	 * kotlin側でClassUtils.getPrimitiveClass(ArrayUtils.getArrayClass)を呼ぶのとは結果が違う
	 * (Java側で呼べばプリミティブの型クラスが返ってくるけどkotlin側で呼ぶとjava.lang.Classが返ってくる)
	 */
	@Test
	fun primitiveArrayClassTest() {
		assertNull(ArrayUtils.getPrimitiveArrayClass(null))
		assertNull(ArrayUtils.getPrimitiveArrayClass(Value(0)))
		assertNull(ArrayUtils.getPrimitiveArrayClass(Any()))

		assertEquals(Bundle::class.java, ArrayUtils.getPrimitiveArrayClass(arrayOfNulls<Bundle>(0)))
		assertEquals(Bundle::class.java, ArrayUtils.getPrimitiveArrayClass(arrayOfNulls<Bundle>(10)))
		assertEquals(Bundle::class.java, ArrayUtils.getPrimitiveArrayClass(arrayOf(Bundle())))

		assertEquals(String::class.java, ArrayUtils.getPrimitiveArrayClass(arrayOfNulls<String>(0)))
		assertEquals(String::class.java, ArrayUtils.getPrimitiveArrayClass(arrayOfNulls<String>(10)))
		assertEquals(String::class.java, ArrayUtils.getPrimitiveArrayClass(arrayOf("")))

		assertEquals(Char::class.java, ArrayUtils.getPrimitiveArrayClass(CharArray(0)))
		assertEquals(Char::class.java, ArrayUtils.getPrimitiveArrayClass(CharArray(10)))

		assertEquals(Char::class.java, ArrayUtils.getPrimitiveArrayClass(arrayOfNulls<Char>(0)))
		assertEquals(Char::class.java, ArrayUtils.getPrimitiveArrayClass(arrayOfNulls<Char>(10)))
		assertEquals(Char::class.java, ArrayUtils.getPrimitiveArrayClass(arrayOf(0x20.toChar())))
		assertEquals(Char::class.java, ArrayUtils.getPrimitiveArrayClass(arrayOf('a')))

		assertEquals(Byte::class.java, ArrayUtils.getPrimitiveArrayClass(ByteArray(0)))
		assertEquals(Byte::class.java, ArrayUtils.getPrimitiveArrayClass(ByteArray(10)))
		assertEquals(Byte::class.java, ArrayUtils.getPrimitiveArrayClass(byteArrayOf(java.lang.Byte.decode("0"))))

		assertEquals(Byte::class.java, ArrayUtils.getPrimitiveArrayClass(arrayOfNulls<Byte>(0)))
		assertEquals(Byte::class.java, ArrayUtils.getPrimitiveArrayClass(arrayOfNulls<Byte>(10)))
		assertEquals(Byte::class.java, ArrayUtils.getPrimitiveArrayClass(arrayOf(0x20.toByte())))
		assertEquals(Byte::class.java, ArrayUtils.getPrimitiveArrayClass(arrayOf(java.lang.Byte.decode("0"))))

		assertEquals(Short::class.java, ArrayUtils.getPrimitiveArrayClass(ShortArray(0)))
		assertEquals(Short::class.java, ArrayUtils.getPrimitiveArrayClass(ShortArray(10)))
		assertEquals(Short::class.java, ArrayUtils.getPrimitiveArrayClass(shortArrayOf(java.lang.Short.decode("0"))))

		assertEquals(Short::class.java, ArrayUtils.getPrimitiveArrayClass(arrayOfNulls<Short>(0)))
		assertEquals(Short::class.java, ArrayUtils.getPrimitiveArrayClass(arrayOfNulls<Short>(10)))
		assertEquals(Short::class.java, ArrayUtils.getPrimitiveArrayClass(arrayOf(0x20.toShort())))
		assertEquals(Short::class.java, ArrayUtils.getPrimitiveArrayClass(arrayOf(java.lang.Short.decode("0"))))

		assertEquals(Int::class.java, ArrayUtils.getPrimitiveArrayClass(IntArray(0)))
		assertEquals(Int::class.java, ArrayUtils.getPrimitiveArrayClass(IntArray(10)))
		assertEquals(Int::class.java, ArrayUtils.getPrimitiveArrayClass(intArrayOf(Integer.decode("0"))))

		assertEquals(Int::class.java, ArrayUtils.getPrimitiveArrayClass(arrayOfNulls<Int>(0)))
		assertEquals(Int::class.java, ArrayUtils.getPrimitiveArrayClass(arrayOfNulls<Int>(10)))
		assertEquals(Int::class.java, ArrayUtils.getPrimitiveArrayClass(arrayOf(0x20)))
		assertEquals(Int::class.java, ArrayUtils.getPrimitiveArrayClass(arrayOf(Integer.decode("0"))))

		assertEquals(Long::class.java, ArrayUtils.getPrimitiveArrayClass(LongArray(0)))
		assertEquals(Long::class.java, ArrayUtils.getPrimitiveArrayClass(LongArray(10)))
		assertEquals(Long::class.java, ArrayUtils.getPrimitiveArrayClass(longArrayOf(java.lang.Long.decode("0"))))

		assertEquals(Long::class.java, ArrayUtils.getPrimitiveArrayClass(arrayOfNulls<Long>(0)))
		assertEquals(Long::class.java, ArrayUtils.getPrimitiveArrayClass(arrayOfNulls<Long>(10)))
		assertEquals(Long::class.java, ArrayUtils.getPrimitiveArrayClass(arrayOf(0x20.toLong())))
		assertEquals(Long::class.java, ArrayUtils.getPrimitiveArrayClass(arrayOf(java.lang.Long.decode("0"))))

		assertEquals(Float::class.java, ArrayUtils.getPrimitiveArrayClass(FloatArray(0)))
		assertEquals(Float::class.java, ArrayUtils.getPrimitiveArrayClass(FloatArray(10)))
		assertEquals(Float::class.java, ArrayUtils.getPrimitiveArrayClass(floatArrayOf("0.0".toFloat())))

		assertEquals(Float::class.java, ArrayUtils.getPrimitiveArrayClass(arrayOfNulls<Float>(0)))
		assertEquals(Float::class.java, ArrayUtils.getPrimitiveArrayClass(arrayOfNulls<Float>(10)))
		assertEquals(Float::class.java, ArrayUtils.getPrimitiveArrayClass(arrayOf(0x20.toFloat())))
		assertEquals(Float::class.java, ArrayUtils.getPrimitiveArrayClass(arrayOf("0.0".toFloat())))

		assertEquals(Double::class.java, ArrayUtils.getPrimitiveArrayClass(DoubleArray(0)))
		assertEquals(Double::class.java, ArrayUtils.getPrimitiveArrayClass(DoubleArray(10)))
		assertEquals(Double::class.java, ArrayUtils.getPrimitiveArrayClass(doubleArrayOf("0.0".toDouble())))

		assertEquals(Double::class.java, ArrayUtils.getPrimitiveArrayClass(arrayOfNulls<Double>(0)))
		assertEquals(Double::class.java, ArrayUtils.getPrimitiveArrayClass(arrayOfNulls<Double>(10)))
		assertEquals(Double::class.java, ArrayUtils.getPrimitiveArrayClass(arrayOf(0x20.toDouble())))
		assertEquals(Double::class.java, ArrayUtils.getPrimitiveArrayClass(arrayOf("0.0".toDouble())))

		// Parcelableを実装していればPARCELABLE_ARRAY
		assertEquals(ParcelableValue::class.java, ArrayUtils.getPrimitiveArrayClass(arrayOfNulls<ParcelableValue>(0)))
		assertEquals(ParcelableValue::class.java, ArrayUtils.getPrimitiveArrayClass(arrayOfNulls<ParcelableValue>(10)))
		assertEquals(ParcelableValue::class.java, ArrayUtils.getPrimitiveArrayClass(arrayOf(ParcelableValue(0))))

		// ParcelableとSerializableの両方を実装している場合はPARCELABLE_ARRAY
		assertEquals(BothValue::class.java, ArrayUtils.getPrimitiveArrayClass(arrayOfNulls<BothValue>(0)))
		assertEquals(BothValue::class.java, ArrayUtils.getPrimitiveArrayClass(arrayOfNulls<BothValue>(10)))
		assertEquals(BothValue::class.java, ArrayUtils.getPrimitiveArrayClass(arrayOf(BothValue(0))))

		// Serializableを実装していればSERIALIZABLE_ARRAY
		assertEquals(SerializableValue::class.java, ArrayUtils.getPrimitiveArrayClass(arrayOfNulls<SerializableValue>(0)))
		assertEquals(SerializableValue::class.java, ArrayUtils.getPrimitiveArrayClass(arrayOfNulls<SerializableValue>(10)))
		assertEquals(SerializableValue::class.java, ArrayUtils.getPrimitiveArrayClass(arrayOf(SerializableValue(0))))

		// その他クラスの亜配列はUNKNOWN_ARRAY
		assertEquals(Value::class.java, ArrayUtils.getPrimitiveArrayClass(arrayOfNulls<Value>(0)))
		assertEquals(Value::class.java, ArrayUtils.getPrimitiveArrayClass(arrayOfNulls<Value>(10)))
		assertEquals(Value::class.java, ArrayUtils.getPrimitiveArrayClass(arrayOf(Value(0))))
//		assertEquals(Void::class.java, ArrayUtils.getPrimitiveArrayClass(arrayOfNulls<Void>(0)))
//		assertEquals(Void::class.java, ArrayUtils.getPrimitiveArrayClass(arrayOfNulls<Void>(10)))
		assertEquals(Any::class.java, ArrayUtils.getPrimitiveArrayClass(arrayOfNulls<Any>(0)))
		assertEquals(Any::class.java, ArrayUtils.getPrimitiveArrayClass(arrayOfNulls<Any>(10)))
	}

	/**
	 * ArrayUtils.getArrayTypeが想定通りの値を返すかどうかをテスト
	 */
	@Test
	fun typeTest() {
		assertEquals(ArrayUtils.ArrayType.NOT_A_ARRAY, ArrayUtils.getArrayType(null))
		assertEquals(ArrayUtils.ArrayType.NOT_A_ARRAY, ArrayUtils.getArrayType(Value(0)))
		assertEquals(ArrayUtils.ArrayType.NOT_A_ARRAY, ArrayUtils.getArrayType(Any()))

		assertEquals(ArrayUtils.ArrayType.BUNDLE_ARRAY, ArrayUtils.getArrayType(arrayOfNulls<Bundle>(0)))
		assertEquals(ArrayUtils.ArrayType.BUNDLE_ARRAY, ArrayUtils.getArrayType(arrayOfNulls<Bundle>(10)))
		assertEquals(ArrayUtils.ArrayType.BUNDLE_ARRAY, ArrayUtils.getArrayType(arrayOf(Bundle())))

		assertEquals(ArrayUtils.ArrayType.STRING_ARRAY, ArrayUtils.getArrayType(arrayOfNulls<String>(0)))
		assertEquals(ArrayUtils.ArrayType.STRING_ARRAY, ArrayUtils.getArrayType(arrayOfNulls<String>(10)))
		assertEquals(ArrayUtils.ArrayType.STRING_ARRAY, ArrayUtils.getArrayType(arrayOf("")))

		assertEquals(ArrayUtils.ArrayType.CHAR_ARRAY, ArrayUtils.getArrayType(CharArray(0)))
		assertEquals(ArrayUtils.ArrayType.CHAR_ARRAY, ArrayUtils.getArrayType(CharArray(10)))
		assertEquals(ArrayUtils.ArrayType.CHAR_ARRAY, ArrayUtils.getArrayType(arrayOfNulls<Char>(0)))
		assertEquals(ArrayUtils.ArrayType.CHAR_ARRAY, ArrayUtils.getArrayType(arrayOfNulls<Char>(10)))
		assertEquals(ArrayUtils.ArrayType.CHAR_ARRAY, ArrayUtils.getArrayType(arrayOf('a')))

		assertEquals(ArrayUtils.ArrayType.BYTE_ARRAY, ArrayUtils.getArrayType(ByteArray(0)))
		assertEquals(ArrayUtils.ArrayType.BYTE_ARRAY, ArrayUtils.getArrayType(ByteArray(10)))
		assertEquals(ArrayUtils.ArrayType.BYTE_ARRAY, ArrayUtils.getArrayType(byteArrayOf(java.lang.Byte.decode("0"))))
		assertEquals(ArrayUtils.ArrayType.BYTE_ARRAY, ArrayUtils.getArrayType(arrayOfNulls<Byte>(0)))
		assertEquals(ArrayUtils.ArrayType.BYTE_ARRAY, ArrayUtils.getArrayType(arrayOfNulls<Byte>(10)))
		assertEquals(ArrayUtils.ArrayType.BYTE_ARRAY, ArrayUtils.getArrayType(arrayOf(java.lang.Byte.decode("0"))))

		assertEquals(ArrayUtils.ArrayType.SHORT_ARRAY, ArrayUtils.getArrayType(ShortArray(0)))
		assertEquals(ArrayUtils.ArrayType.SHORT_ARRAY, ArrayUtils.getArrayType(ShortArray(10)))
		assertEquals(ArrayUtils.ArrayType.SHORT_ARRAY, ArrayUtils.getArrayType(shortArrayOf(java.lang.Short.decode("0"))))
		assertEquals(ArrayUtils.ArrayType.SHORT_ARRAY, ArrayUtils.getArrayType(arrayOfNulls<Short>(0)))
		assertEquals(ArrayUtils.ArrayType.SHORT_ARRAY, ArrayUtils.getArrayType(arrayOfNulls<Short>(10)))
		assertEquals(ArrayUtils.ArrayType.SHORT_ARRAY, ArrayUtils.getArrayType(arrayOf(java.lang.Short.decode("0"))))

		assertEquals(ArrayUtils.ArrayType.INT_ARRAY, ArrayUtils.getArrayType(IntArray(0)))
		assertEquals(ArrayUtils.ArrayType.INT_ARRAY, ArrayUtils.getArrayType(IntArray(10)))
		assertEquals(ArrayUtils.ArrayType.INT_ARRAY, ArrayUtils.getArrayType(intArrayOf(Integer.decode("0"))))
		assertEquals(ArrayUtils.ArrayType.INT_ARRAY, ArrayUtils.getArrayType(arrayOfNulls<Int>(0)))
		assertEquals(ArrayUtils.ArrayType.INT_ARRAY, ArrayUtils.getArrayType(arrayOfNulls<Int>(10)))
		assertEquals(ArrayUtils.ArrayType.INT_ARRAY, ArrayUtils.getArrayType(arrayOf(Integer.decode("0"))))

		assertEquals(ArrayUtils.ArrayType.LONG_ARRAY, ArrayUtils.getArrayType(LongArray(0)))
		assertEquals(ArrayUtils.ArrayType.LONG_ARRAY, ArrayUtils.getArrayType(LongArray(10)))
		assertEquals(ArrayUtils.ArrayType.LONG_ARRAY, ArrayUtils.getArrayType(longArrayOf(java.lang.Long.decode("0"))))
		assertEquals(ArrayUtils.ArrayType.LONG_ARRAY, ArrayUtils.getArrayType(arrayOfNulls<Long>(0)))
		assertEquals(ArrayUtils.ArrayType.LONG_ARRAY, ArrayUtils.getArrayType(arrayOfNulls<Long>(10)))
		assertEquals(ArrayUtils.ArrayType.LONG_ARRAY, ArrayUtils.getArrayType(arrayOf(java.lang.Long.decode("0"))))

		assertEquals(ArrayUtils.ArrayType.FLOAT_ARRAY, ArrayUtils.getArrayType(FloatArray(0)))
		assertEquals(ArrayUtils.ArrayType.FLOAT_ARRAY, ArrayUtils.getArrayType(FloatArray(10)))
		assertEquals(ArrayUtils.ArrayType.FLOAT_ARRAY, ArrayUtils.getArrayType(floatArrayOf("0.0".toFloat())))
		assertEquals(ArrayUtils.ArrayType.FLOAT_ARRAY, ArrayUtils.getArrayType(arrayOfNulls<Float>(0)))
		assertEquals(ArrayUtils.ArrayType.FLOAT_ARRAY, ArrayUtils.getArrayType(arrayOfNulls<Float>(10)))
		assertEquals(ArrayUtils.ArrayType.FLOAT_ARRAY, ArrayUtils.getArrayType(arrayOf("0.0".toFloat())))

		assertEquals(ArrayUtils.ArrayType.DOUBLE_ARRAY, ArrayUtils.getArrayType(DoubleArray(0)))
		assertEquals(ArrayUtils.ArrayType.DOUBLE_ARRAY, ArrayUtils.getArrayType(DoubleArray(10)))
		assertEquals(ArrayUtils.ArrayType.DOUBLE_ARRAY, ArrayUtils.getArrayType(doubleArrayOf("0.0".toDouble())))
		assertEquals(ArrayUtils.ArrayType.DOUBLE_ARRAY, ArrayUtils.getArrayType(arrayOfNulls<Double>(0)))
		assertEquals(ArrayUtils.ArrayType.DOUBLE_ARRAY, ArrayUtils.getArrayType(arrayOfNulls<Double>(10)))
		assertEquals(ArrayUtils.ArrayType.DOUBLE_ARRAY, ArrayUtils.getArrayType(arrayOf("0.0".toDouble())))

		// Parcelableを実装していればPARCELABLE_ARRAY
		assertEquals(ArrayUtils.ArrayType.PARCELABLE_ARRAY, ArrayUtils.getArrayType(arrayOfNulls<ParcelableValue>(0)))
		assertEquals(ArrayUtils.ArrayType.PARCELABLE_ARRAY, ArrayUtils.getArrayType(arrayOfNulls<ParcelableValue>(10)))
		assertEquals(ArrayUtils.ArrayType.PARCELABLE_ARRAY, ArrayUtils.getArrayType(arrayOf(ParcelableValue(0))))

		// ParcelableとSerializableの両方を実装している場合はPARCELABLE_ARRAY
		assertEquals(ArrayUtils.ArrayType.PARCELABLE_ARRAY, ArrayUtils.getArrayType(arrayOfNulls<BothValue>(0)))
		assertEquals(ArrayUtils.ArrayType.PARCELABLE_ARRAY, ArrayUtils.getArrayType(	arrayOfNulls<BothValue>(10)))
		assertEquals(ArrayUtils.ArrayType.PARCELABLE_ARRAY, ArrayUtils.getArrayType(arrayOf(BothValue(0))))

		// Serializableを実装していればSERIALIZABLE_ARRAY
		assertEquals(ArrayUtils.ArrayType.SERIALIZABLE_ARRAY, ArrayUtils.getArrayType(arrayOfNulls<SerializableValue>(0)))
		assertEquals(ArrayUtils.ArrayType.SERIALIZABLE_ARRAY, ArrayUtils.getArrayType(arrayOfNulls<SerializableValue>(10)))
		assertEquals(ArrayUtils.ArrayType.SERIALIZABLE_ARRAY, ArrayUtils.getArrayType(arrayOf(SerializableValue(0))))

		// その他クラスの亜配列はUNKNOWN_ARRAY
		assertEquals(ArrayUtils.ArrayType.UNKNOWN_ARRAY, ArrayUtils.getArrayType(arrayOfNulls<Value>(0)))
		assertEquals(ArrayUtils.ArrayType.UNKNOWN_ARRAY, ArrayUtils.getArrayType(arrayOfNulls<Value>(10)))
		assertEquals(ArrayUtils.ArrayType.UNKNOWN_ARRAY, ArrayUtils.getArrayType(arrayOf(Value(0))))
		assertEquals(ArrayUtils.ArrayType.UNKNOWN_ARRAY, ArrayUtils.getArrayType(arrayOfNulls<Void>(0)))
		assertEquals(ArrayUtils.ArrayType.UNKNOWN_ARRAY, ArrayUtils.getArrayType(arrayOfNulls<Void>(10)))
		assertEquals(ArrayUtils.ArrayType.UNKNOWN_ARRAY, ArrayUtils.getArrayType(arrayOfNulls<Any>(0)))
		assertEquals(ArrayUtils.ArrayType.UNKNOWN_ARRAY, ArrayUtils.getArrayType(arrayOfNulls<Any>(10)))
	}
}
