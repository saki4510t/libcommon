package com.serenegiant.common;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2023 saki t_saki@serenegiant.com
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
import android.os.Parcel;
import android.os.Parcelable;

import com.serenegiant.utils.ArrayUtils;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.Serializable;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;

@RunWith(AndroidJUnit4.class)
public class ArrayUtilsTest {

	/**
	 * ArrayUtils.getArrayClassが想定通りの値を返すかどうかをテスト
	 */
	@Test
	public void getArrayClassTest() {
		Assert.assertNull(ArrayUtils.getArrayClass(null));
		Assert.assertNull(ArrayUtils.getArrayClass(new Value(0)));
		Assert.assertNull(ArrayUtils.getArrayClass(new Object()));

		Assert.assertEquals(Bundle.class, ArrayUtils.getArrayClass(new Bundle[0]));
		Assert.assertEquals(Bundle.class, ArrayUtils.getArrayClass(new Bundle[10]));
		Assert.assertEquals(Bundle.class, ArrayUtils.getArrayClass(new Bundle[]{new Bundle()}));

		Assert.assertEquals(String.class, ArrayUtils.getArrayClass(new String[0]));
		Assert.assertEquals(String.class, ArrayUtils.getArrayClass(new String[10]));
		Assert.assertEquals(String.class, ArrayUtils.getArrayClass(new String[]{""}));

		Assert.assertEquals(char.class, ArrayUtils.getArrayClass(new char[0]));
		Assert.assertEquals(char.class, ArrayUtils.getArrayClass(new char[10]));
		Assert.assertEquals(Character.class, ArrayUtils.getArrayClass(new Character[0]));
		Assert.assertEquals(Character.class, ArrayUtils.getArrayClass(new Character[10]));
		Assert.assertEquals(Character.class, ArrayUtils.getArrayClass(new Character[]{'a'}));

		Assert.assertEquals(byte.class, ArrayUtils.getArrayClass(new byte[0]));
		Assert.assertEquals(byte.class, ArrayUtils.getArrayClass(new byte[10]));
		Assert.assertEquals(byte.class, ArrayUtils.getArrayClass(new byte[]{Byte.decode("0")}));
		Assert.assertEquals(Byte.class, ArrayUtils.getArrayClass(new Byte[0]));
		Assert.assertEquals(Byte.class, ArrayUtils.getArrayClass(new Byte[10]));
		Assert.assertEquals(Byte.class, ArrayUtils.getArrayClass(new Byte[]{Byte.decode("0")}));

		Assert.assertEquals(short.class, ArrayUtils.getArrayClass(new short[0]));
		Assert.assertEquals(short.class, ArrayUtils.getArrayClass(new short[10]));
		Assert.assertEquals(short.class, ArrayUtils.getArrayClass(new short[]{Short.decode("0")}));
		Assert.assertEquals(Short.class, ArrayUtils.getArrayClass(new Short[0]));
		Assert.assertEquals(Short.class, ArrayUtils.getArrayClass(new Short[10]));
		Assert.assertEquals(Short.class, ArrayUtils.getArrayClass(new Short[]{Short.decode("0")}));

		Assert.assertEquals(int.class, ArrayUtils.getArrayClass(new int[0]));
		Assert.assertEquals(int.class, ArrayUtils.getArrayClass(new int[10]));
		Assert.assertEquals(int.class, ArrayUtils.getArrayClass(new int[]{Integer.decode("0")}));
		Assert.assertEquals(Integer.class, ArrayUtils.getArrayClass(new Integer[0]));
		Assert.assertEquals(Integer.class, ArrayUtils.getArrayClass(new Integer[10]));
		Assert.assertEquals(Integer.class, ArrayUtils.getArrayClass(new Integer[]{Integer.decode("0")}));

		Assert.assertEquals(long.class, ArrayUtils.getArrayClass(new long[0]));
		Assert.assertEquals(long.class, ArrayUtils.getArrayClass(new long[10]));
		Assert.assertEquals(long.class, ArrayUtils.getArrayClass(new long[]{Long.decode("0")}));
		Assert.assertEquals(Long.class, ArrayUtils.getArrayClass(new Long[0]));
		Assert.assertEquals(Long.class, ArrayUtils.getArrayClass(new Long[10]));
		Assert.assertEquals(Long.class, ArrayUtils.getArrayClass(new Long[]{Long.decode("0")}));

		Assert.assertEquals(float.class, ArrayUtils.getArrayClass(new float[0]));
		Assert.assertEquals(float.class, ArrayUtils.getArrayClass(new float[10]));
		Assert.assertEquals(float.class, ArrayUtils.getArrayClass(new float[]{Float.parseFloat("0.0")}));
		Assert.assertEquals(Float.class, ArrayUtils.getArrayClass(new Float[0]));
		Assert.assertEquals(Float.class, ArrayUtils.getArrayClass(new Float[10]));
		Assert.assertEquals(Float.class, ArrayUtils.getArrayClass(new Float[]{Float.parseFloat("0.0")}));

		Assert.assertEquals(double.class, ArrayUtils.getArrayClass(new double[0]));
		Assert.assertEquals(double.class, ArrayUtils.getArrayClass(new double[10]));
		Assert.assertEquals(double.class, ArrayUtils.getArrayClass(new double[]{Double.parseDouble("0.0")}));
		Assert.assertEquals(Double.class, ArrayUtils.getArrayClass(new Double[0]));
		Assert.assertEquals(Double.class, ArrayUtils.getArrayClass(new Double[10]));
		Assert.assertEquals(Double.class, ArrayUtils.getArrayClass(new Double[]{Double.parseDouble("0.0")}));

		// Parcelableを実装していればPARCELABLE_ARRAY
		Assert.assertEquals(ParcelableValue.class, ArrayUtils.getArrayClass(new ParcelableValue[0]));
		Assert.assertEquals(ParcelableValue.class, ArrayUtils.getArrayClass(new ParcelableValue[10]));
		Assert.assertEquals(ParcelableValue.class, ArrayUtils.getArrayClass(new ParcelableValue[]{new ParcelableValue(0)}));

		// ParcelableとSerializableの両方を実装している場合はPARCELABLE_ARRAY
		Assert.assertEquals(BothValue.class, ArrayUtils.getArrayClass(new BothValue[0]));
		Assert.assertEquals(BothValue.class, ArrayUtils.getArrayClass(new BothValue[10]));
		Assert.assertEquals(BothValue.class, ArrayUtils.getArrayClass(new BothValue[]{new BothValue(0)}));

		// Serializableを実装していればSERIALIZABLE_ARRAY
		Assert.assertEquals(SerializableValue.class, ArrayUtils.getArrayClass(new SerializableValue[0]));
		Assert.assertEquals(SerializableValue.class, ArrayUtils.getArrayClass(new SerializableValue[10]));
		Assert.assertEquals(SerializableValue.class, ArrayUtils.getArrayClass(new SerializableValue[]{new SerializableValue(0)}));

		// その他クラスの亜配列はUNKNOWN_ARRAY
		Assert.assertEquals(Value.class, ArrayUtils.getArrayClass(new Value[0]));
		Assert.assertEquals(Value.class, ArrayUtils.getArrayClass(new Value[10]));
		Assert.assertEquals(Value.class, ArrayUtils.getArrayClass(new Value[]{new Value(0)}));
		Assert.assertEquals(Void.class, ArrayUtils.getArrayClass(new Void[0]));
		Assert.assertEquals(Void.class, ArrayUtils.getArrayClass(new Void[10]));
		Assert.assertEquals(Object.class, ArrayUtils.getArrayClass(new Object[0]));
		Assert.assertEquals(Object.class, ArrayUtils.getArrayClass(new Object[10]));
	}

	/**
	 * ArrayUtils.getArrayTypeが想定通りの値を返すかどうかをテスト
	 */
	@Test
	public void getTypeTest() {
		Assert.assertEquals(ArrayUtils.ArrayType.NOT_A_ARRAY, ArrayUtils.getArrayType(null));
		Assert.assertEquals(ArrayUtils.ArrayType.NOT_A_ARRAY, ArrayUtils.getArrayType(new Value(0)));
		Assert.assertEquals(ArrayUtils.ArrayType.NOT_A_ARRAY, ArrayUtils.getArrayType(new Object()));

		Assert.assertEquals(ArrayUtils.ArrayType.BUNDLE_ARRAY, ArrayUtils.getArrayType(new Bundle[0]));
		Assert.assertEquals(ArrayUtils.ArrayType.BUNDLE_ARRAY, ArrayUtils.getArrayType(new Bundle[10]));
		Assert.assertEquals(ArrayUtils.ArrayType.BUNDLE_ARRAY, ArrayUtils.getArrayType(new Bundle[]{new Bundle()}));

		Assert.assertEquals(ArrayUtils.ArrayType.STRING_ARRAY, ArrayUtils.getArrayType(new String[0]));
		Assert.assertEquals(ArrayUtils.ArrayType.STRING_ARRAY, ArrayUtils.getArrayType(new String[10]));
		Assert.assertEquals(ArrayUtils.ArrayType.STRING_ARRAY, ArrayUtils.getArrayType(new String[]{""}));

		Assert.assertEquals(ArrayUtils.ArrayType.CHAR_ARRAY, ArrayUtils.getArrayType(new char[0]));
		Assert.assertEquals(ArrayUtils.ArrayType.CHAR_ARRAY, ArrayUtils.getArrayType(new char[10]));
		Assert.assertEquals(ArrayUtils.ArrayType.CHAR_ARRAY, ArrayUtils.getArrayType(new Character[0]));
		Assert.assertEquals(ArrayUtils.ArrayType.CHAR_ARRAY, ArrayUtils.getArrayType(new Character[10]));
		Assert.assertEquals(ArrayUtils.ArrayType.CHAR_ARRAY, ArrayUtils.getArrayType(new Character[]{'a'}));

		Assert.assertEquals(ArrayUtils.ArrayType.BYTE_ARRAY, ArrayUtils.getArrayType(new byte[0]));
		Assert.assertEquals(ArrayUtils.ArrayType.BYTE_ARRAY, ArrayUtils.getArrayType(new byte[10]));
		Assert.assertEquals(ArrayUtils.ArrayType.BYTE_ARRAY, ArrayUtils.getArrayType(new byte[]{Byte.decode("0")}));
		Assert.assertEquals(ArrayUtils.ArrayType.BYTE_ARRAY, ArrayUtils.getArrayType(new Byte[0]));
		Assert.assertEquals(ArrayUtils.ArrayType.BYTE_ARRAY, ArrayUtils.getArrayType(new Byte[10]));
		Assert.assertEquals(ArrayUtils.ArrayType.BYTE_ARRAY, ArrayUtils.getArrayType(new Byte[]{Byte.decode("0")}));

		Assert.assertEquals(ArrayUtils.ArrayType.SHORT_ARRAY, ArrayUtils.getArrayType(new short[0]));
		Assert.assertEquals(ArrayUtils.ArrayType.SHORT_ARRAY, ArrayUtils.getArrayType(new short[10]));
		Assert.assertEquals(ArrayUtils.ArrayType.SHORT_ARRAY, ArrayUtils.getArrayType(new short[]{Short.decode("0")}));
		Assert.assertEquals(ArrayUtils.ArrayType.SHORT_ARRAY, ArrayUtils.getArrayType(new Short[0]));
		Assert.assertEquals(ArrayUtils.ArrayType.SHORT_ARRAY, ArrayUtils.getArrayType(new Short[10]));
		Assert.assertEquals(ArrayUtils.ArrayType.SHORT_ARRAY, ArrayUtils.getArrayType(new Short[]{Short.decode("0")}));

		Assert.assertEquals(ArrayUtils.ArrayType.INT_ARRAY, ArrayUtils.getArrayType(new int[0]));
		Assert.assertEquals(ArrayUtils.ArrayType.INT_ARRAY, ArrayUtils.getArrayType(new int[10]));
		Assert.assertEquals(ArrayUtils.ArrayType.INT_ARRAY, ArrayUtils.getArrayType(new int[]{Integer.decode("0")}));
		Assert.assertEquals(ArrayUtils.ArrayType.INT_ARRAY, ArrayUtils.getArrayType(new Integer[0]));
		Assert.assertEquals(ArrayUtils.ArrayType.INT_ARRAY, ArrayUtils.getArrayType(new Integer[10]));
		Assert.assertEquals(ArrayUtils.ArrayType.INT_ARRAY, ArrayUtils.getArrayType(new Integer[]{Integer.decode("0")}));

		Assert.assertEquals(ArrayUtils.ArrayType.LONG_ARRAY, ArrayUtils.getArrayType(new long[0]));
		Assert.assertEquals(ArrayUtils.ArrayType.LONG_ARRAY, ArrayUtils.getArrayType(new long[10]));
		Assert.assertEquals(ArrayUtils.ArrayType.LONG_ARRAY, ArrayUtils.getArrayType(new long[]{Long.decode("0")}));
		Assert.assertEquals(ArrayUtils.ArrayType.LONG_ARRAY, ArrayUtils.getArrayType(new Long[0]));
		Assert.assertEquals(ArrayUtils.ArrayType.LONG_ARRAY, ArrayUtils.getArrayType(new Long[10]));
		Assert.assertEquals(ArrayUtils.ArrayType.LONG_ARRAY, ArrayUtils.getArrayType(new Long[]{Long.decode("0")}));

		Assert.assertEquals(ArrayUtils.ArrayType.FLOAT_ARRAY, ArrayUtils.getArrayType(new float[0]));
		Assert.assertEquals(ArrayUtils.ArrayType.FLOAT_ARRAY, ArrayUtils.getArrayType(new float[10]));
		Assert.assertEquals(ArrayUtils.ArrayType.FLOAT_ARRAY, ArrayUtils.getArrayType(new float[]{Float.parseFloat("0.0")}));
		Assert.assertEquals(ArrayUtils.ArrayType.FLOAT_ARRAY, ArrayUtils.getArrayType(new Float[0]));
		Assert.assertEquals(ArrayUtils.ArrayType.FLOAT_ARRAY, ArrayUtils.getArrayType(new Float[10]));
		Assert.assertEquals(ArrayUtils.ArrayType.FLOAT_ARRAY, ArrayUtils.getArrayType(new Float[]{Float.parseFloat("0.0")}));

		Assert.assertEquals(ArrayUtils.ArrayType.DOUBLE_ARRAY, ArrayUtils.getArrayType(new double[0]));
		Assert.assertEquals(ArrayUtils.ArrayType.DOUBLE_ARRAY, ArrayUtils.getArrayType(new double[10]));
		Assert.assertEquals(ArrayUtils.ArrayType.DOUBLE_ARRAY, ArrayUtils.getArrayType(new double[]{Double.parseDouble("0.0")}));
		Assert.assertEquals(ArrayUtils.ArrayType.DOUBLE_ARRAY, ArrayUtils.getArrayType(new Double[0]));
		Assert.assertEquals(ArrayUtils.ArrayType.DOUBLE_ARRAY, ArrayUtils.getArrayType(new Double[10]));
		Assert.assertEquals(ArrayUtils.ArrayType.DOUBLE_ARRAY, ArrayUtils.getArrayType(new Double[]{Double.parseDouble("0.0")}));

		// Parcelableを実装していればPARCELABLE_ARRAY
		Assert.assertEquals(ArrayUtils.ArrayType.PARCELABLE_ARRAY, ArrayUtils.getArrayType(new ParcelableValue[0]));
		Assert.assertEquals(ArrayUtils.ArrayType.PARCELABLE_ARRAY, ArrayUtils.getArrayType(new ParcelableValue[10]));
		Assert.assertEquals(ArrayUtils.ArrayType.PARCELABLE_ARRAY, ArrayUtils.getArrayType(new ParcelableValue[]{new ParcelableValue(0)}));

		// ParcelableとSerializableの両方を実装している場合はPARCELABLE_ARRAY
		Assert.assertEquals(ArrayUtils.ArrayType.PARCELABLE_ARRAY, ArrayUtils.getArrayType(new BothValue[0]));
		Assert.assertEquals(ArrayUtils.ArrayType.PARCELABLE_ARRAY, ArrayUtils.getArrayType(new BothValue[10]));
		Assert.assertEquals(ArrayUtils.ArrayType.PARCELABLE_ARRAY, ArrayUtils.getArrayType(new BothValue[]{new BothValue(0)}));

		// Serializableを実装していればSERIALIZABLE_ARRAY
		Assert.assertEquals(ArrayUtils.ArrayType.SERIALIZABLE_ARRAY, ArrayUtils.getArrayType(new SerializableValue[0]));
		Assert.assertEquals(ArrayUtils.ArrayType.SERIALIZABLE_ARRAY, ArrayUtils.getArrayType(new SerializableValue[10]));
		Assert.assertEquals(ArrayUtils.ArrayType.SERIALIZABLE_ARRAY, ArrayUtils.getArrayType(new SerializableValue[]{new SerializableValue(0)}));

		// その他クラスの亜配列はUNKNOWN_ARRAY
		Assert.assertEquals(ArrayUtils.ArrayType.UNKNOWN_ARRAY, ArrayUtils.getArrayType(new Value[0]));
		Assert.assertEquals(ArrayUtils.ArrayType.UNKNOWN_ARRAY, ArrayUtils.getArrayType(new Value[10]));
		Assert.assertEquals(ArrayUtils.ArrayType.UNKNOWN_ARRAY, ArrayUtils.getArrayType(new Value[]{new Value(0)}));
		Assert.assertEquals(ArrayUtils.ArrayType.UNKNOWN_ARRAY, ArrayUtils.getArrayType(new Void[0]));
		Assert.assertEquals(ArrayUtils.ArrayType.UNKNOWN_ARRAY, ArrayUtils.getArrayType(new Void[10]));
		Assert.assertEquals(ArrayUtils.ArrayType.UNKNOWN_ARRAY, ArrayUtils.getArrayType(new Object[0]));
		Assert.assertEquals(ArrayUtils.ArrayType.UNKNOWN_ARRAY, ArrayUtils.getArrayType(new Object[10]));
	}

	private static class ParcelableValue implements Parcelable {
		public final int value;

		ParcelableValue(final int value) {
			this.value = value;
		}

		ParcelableValue(@NonNull final Parcel src) {
			value = src.readInt();
		}

		@Override
		public int describeContents() {
			return 0;
		}

		@Override
		public void writeToParcel(@NonNull final Parcel dst, final int flags) {
			dst.writeInt(value);
		}

		public static final Parcelable.Creator<?> CREATOR
			= new Parcelable.Creator<ParcelableValue>() {
			@Override
			public ParcelableValue createFromParcel(@NonNull final Parcel src) {
				return new ParcelableValue(src);
			}

			@Override
			public ParcelableValue[] newArray(final int size) {
				return new ParcelableValue[0];
			}
		};
	}

	private static class BothValue implements Parcelable, Serializable {
		public final int value;

		BothValue(final int value) {
			this.value = value;
		}

		BothValue(@NonNull final Parcel src) {
			value = src.readInt();
		}

		@Override
		public int describeContents() {
			return 0;
		}

		@Override
		public void writeToParcel(@NonNull final Parcel dst, final int flags) {
			dst.writeInt(value);
		}

		public static final Parcelable.Creator<?> CREATOR
			= new Parcelable.Creator<BothValue>() {
			@Override
			public BothValue createFromParcel(@NonNull final Parcel src) {
				return new BothValue(src);
			}

			@Override
			public BothValue[] newArray(final int size) {
				return new BothValue[0];
			}
		};
	}

	private static class SerializableValue implements Serializable {
		public final int value;

		public SerializableValue(final int value) {
			this.value = value;
		}
	}

	private static class Value {
		public final int value;

		public Value(final int value) {
			this.value = value;
		}
	}
}
