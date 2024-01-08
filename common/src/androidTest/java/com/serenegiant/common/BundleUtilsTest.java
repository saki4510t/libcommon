package com.serenegiant.common;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2024 saki t_saki@serenegiant.com
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
      data.putByte("BYTE", (byte)12);
      Assert.assertEquals("Bundle{BYTE=12}", BundleUtils.toString(data));

      data.clear();
      data.putShort("SHORT", (short)1234);
      Assert.assertEquals("Bundle{SHORT=1234}", BundleUtils.toString(data));

      data.clear();
      data.putInt("INT", 12345);
      Assert.assertEquals("Bundle{INT=12345}", BundleUtils.toString(data));

      data.clear();
      data.putLong("LONG", 12345L);
      Assert.assertEquals("Bundle{LONG=12345}", BundleUtils.toString(data));

      data.clear();
      data.putStringArray("STRING_ARRAY", new String[]{"abc", "def"});
      Assert.assertEquals("Bundle{STRING_ARRAY=[abc,def]}", BundleUtils.toString(data));

      data.clear();
      data.putByteArray("BYTE_ARRAY", new byte[]{1,2,3,4,5});
      Assert.assertEquals("Bundle{BYTE_ARRAY=[1,2,3,4,5]}", BundleUtils.toString(data));

      data.clear();
      data.putShortArray("SHORT_ARRAY", new short[]{1,2,3,4,5});
      Assert.assertEquals("Bundle{SHORT_ARRAY=[1,2,3,4,5]}", BundleUtils.toString(data));

      data.clear();
      data.putIntArray("INT_ARRAY", new int[]{1,2,3,4,5});
      Assert.assertEquals("Bundle{INT_ARRAY=[1,2,3,4,5]}", BundleUtils.toString(data));

      data.clear();
      data.putLongArray("LONG_ARRAY", new long[]{1L,2L,3L,4L,5L});
      Assert.assertEquals("Bundle{LONG_ARRAY=[1,2,3,4,5]}", BundleUtils.toString(data));
   }
}
