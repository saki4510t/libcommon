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

import android.os.Bundle;

import com.serenegiant.utils.BundleUtils;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.ext.junit.runners.AndroidJUnit4;

@RunWith(AndroidJUnit4.class)
public class BundleUtilsTest {
   /**
    * Bundleをデバッグ用に文字列化するテスト
    * キーの順番が保証できないので組み合わせてputしたときのテストは難しい
    * BundleUtils.toStringでキーセットをソートしてから文字列化すれば順番を指定できる？
    */
   @Test
   public void toStringTest() {
      Assert.assertEquals("Bundle{null}", BundleUtils.toString(null));
      final Bundle data = new Bundle();
      Assert.assertEquals("Bundle{}", BundleUtils.toString(data));

      data.clear();
      data.putString("STRING", "12345");
      Assert.assertEquals("Bundle{STRING=12345}", BundleUtils.toString(data));

      data.clear();
      data.putString("STRING1", "12345");
      data.putString("STRING2", "67890");
      Assert.assertEquals("Bundle{STRING1=12345,STRING2=67890}", BundleUtils.toString(data));

      data.clear();
      data.putByte("BYTE", (byte)12);
      Assert.assertEquals("Bundle{BYTE=12}", BundleUtils.toString(data));

      data.clear();
      data.putByte("BYTE1", (byte)12);
      data.putByte("BYTE2", (byte)34);
      Assert.assertEquals("Bundle{BYTE1=12,BYTE2=34}", BundleUtils.toString(data));

      data.clear();
      data.putShort("SHORT", (short)1234);
      Assert.assertEquals("Bundle{SHORT=1234}", BundleUtils.toString(data));

      data.clear();
      data.putShort("SHORT1", (short)1234);
      data.putShort("SHORT2", (short)2340);
      Assert.assertEquals("Bundle{SHORT1=1234,SHORT2=2340}", BundleUtils.toString(data));

      data.clear();
      data.putInt("INT", 12345);
      Assert.assertEquals("Bundle{INT=12345}", BundleUtils.toString(data));

      data.clear();
      data.putInt("INT1", 12345);
      data.putInt("INT2", 54321);
      Assert.assertEquals("Bundle{INT1=12345,INT2=54321}", BundleUtils.toString(data));

      data.clear();
      data.putLong("LONG", 12345L);
      Assert.assertEquals("Bundle{LONG=12345}", BundleUtils.toString(data));

      data.clear();
      data.putLong("LONG1", 12345L);
      data.putLong("LONG2", 54321L);
      Assert.assertEquals("Bundle{LONG1=12345,LONG2=54321}", BundleUtils.toString(data));

      data.clear();
      data.putStringArray("STRING_ARRAY", new String[]{"abc", "def"});
      Assert.assertEquals("Bundle{STRING_ARRAY=[abc,def]}", BundleUtils.toString(data));

      data.clear();
      data.putStringArray("STRING_ARRAY1", new String[]{"abc", "def"});
      data.putStringArray("STRING_ARRAY2", new String[]{"ABC", "DEF"});
      Assert.assertEquals("Bundle{STRING_ARRAY1=[abc,def],STRING_ARRAY2=[ABC,DEF]}", BundleUtils.toString(data));

      data.clear();
      data.putByteArray("BYTE_ARRAY", new byte[]{1,2,3,4,5});
      Assert.assertEquals("Bundle{BYTE_ARRAY=[1,2,3,4,5]}", BundleUtils.toString(data));

      data.clear();
      data.putByteArray("BYTE_ARRAY1", new byte[]{1,2,3,4,5});
      data.putByteArray("BYTE_ARRAY2", new byte[]{6,7,8,9,0});
      Assert.assertEquals("Bundle{BYTE_ARRAY1=[1,2,3,4,5],BYTE_ARRAY2=[6,7,8,9,0]}", BundleUtils.toString(data));

      data.clear();
      data.putShortArray("SHORT_ARRAY", new short[]{1,2,3,4,5});
      Assert.assertEquals("Bundle{SHORT_ARRAY=[1,2,3,4,5]}", BundleUtils.toString(data));

      data.clear();
      data.putShortArray("SHORT_ARRAY1", new short[]{1,2,3,4,5});
      data.putShortArray("SHORT_ARRAY2", new short[]{6,7,8,9,0});
      Assert.assertEquals("Bundle{SHORT_ARRAY1=[1,2,3,4,5],SHORT_ARRAY2=[6,7,8,9,0]}", BundleUtils.toString(data));

      data.clear();
      data.putIntArray("INT_ARRAY", new int[]{1,2,3,4,5});
      Assert.assertEquals("Bundle{INT_ARRAY=[1,2,3,4,5]}", BundleUtils.toString(data));

      data.clear();
      data.putIntArray("INT_ARRAY1", new int[]{1,2,3,4,5});
      data.putIntArray("INT_ARRAY2", new int[]{6,7,8,9,0});
      Assert.assertEquals("Bundle{INT_ARRAY1=[1,2,3,4,5],INT_ARRAY2=[6,7,8,9,0]}", BundleUtils.toString(data));

      data.clear();
      data.putLongArray("LONG_ARRAY", new long[]{1L,2L,3L,4L,5L});
      Assert.assertEquals("Bundle{LONG_ARRAY=[1,2,3,4,5]}", BundleUtils.toString(data));

      data.clear();
      data.putLongArray("LONG_ARRAY1", new long[]{1L,2L,3L,4L,5L});
      data.putLongArray("LONG_ARRAY2", new long[]{6L,7L,8L,9L,0L});
      Assert.assertEquals("Bundle{LONG_ARRAY1=[1,2,3,4,5],LONG_ARRAY2=[6,7,8,9,0]}", BundleUtils.toString(data));
   }
}
