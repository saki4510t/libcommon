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

import android.content.Context;
import android.content.SharedPreferences;

import com.serenegiant.preference.EncryptedSharedPreferences;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.security.GeneralSecurityException;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import static org.junit.Assert.*;

/**
 * com.serenegiant.system.EncryptedSharedPreferences用のインスツルメンテーションテスト用クラス
 * EncryptedSharedPreferences.KeyStoreObfuscatorを使って暗号化・復号を行う
 */
@RunWith(AndroidJUnit4.class)
public class EncryptedSharedPrefKeyStoreTest {
	private static final String TAG = EncryptedSharedPrefKeyStoreTest.class.getSimpleName();

	private static final double EPS_FLOAT = Float.MIN_NORMAL;

	@Before
	public void prepare() {
		final Context context = ApplicationProvider.getApplicationContext();
		final String prefName = context.getPackageName();
		try {
			final SharedPreferences original
				= context.getSharedPreferences(prefName, 0);
			original.edit().clear().apply();

			final SharedPreferences preferences
				= new EncryptedSharedPreferences(original,
					new EncryptedSharedPreferences.KeyStoreObfuscator(context, prefName));
			assertFalse(preferences.contains("KEY1"));
			setValues(preferences);
		} catch (final GeneralSecurityException e) {
			throw new AssertionError(e);
		}
	}

	@After
	public void cleanUp() {
		final Context context = ApplicationProvider.getApplicationContext();
		final String prefName = context.getPackageName();
		final SharedPreferences original
			= context.getSharedPreferences(prefName, 0);
		original.edit().clear().apply();
		EncryptedSharedPreferences.KeyStoreObfuscator.deleteKey(context, prefName);
	}

	/**
	 * 正常に読み書きできるかどうかを確認
	 */
	@Test
	public void test1() {
		final Context context = ApplicationProvider.getApplicationContext();
		final String prefName = context.getPackageName();
		try {
			final SharedPreferences preferences
				= new EncryptedSharedPreferences(
					context.getSharedPreferences(context.getPackageName(), 0),
					new EncryptedSharedPreferences.KeyStoreObfuscator(context, prefName));

			assertTrue(preferences.contains("KEY1"));
			assertEquals("VALUE1", preferences.getString("KEY1", ""));
			assertTrue(preferences.getBoolean("KEY2", false));
			assertFalse(preferences.getBoolean("KEY3", true));
			assertEquals(3.141592f, preferences.getFloat("KEY4", 0), EPS_FLOAT);
			assertEquals(Float.MIN_VALUE, preferences.getFloat("KEY5", 0), EPS_FLOAT);
			assertEquals(Float.MAX_VALUE, preferences.getFloat("KEY6", 0), EPS_FLOAT);
			assertEquals(34567, preferences.getInt("KEY7", 0));
			assertEquals(Integer.MIN_VALUE, preferences.getInt("KEY8", 0));
			assertEquals(987654321L, preferences.getLong("KEY9", 0));
			assertEquals(Long.MIN_VALUE, preferences.getLong("KEY10", 0));
			assertEquals(Long.MAX_VALUE, preferences.getLong("KEY11", 0));
			preferences.edit()
				.remove("KEY9")
				.remove("KEY10")
				.remove("KEY11")
				.apply();
			assertEquals(123456789L, preferences.getLong("KEY9", 123456789L));
			assertEquals(Long.MAX_VALUE, preferences.getLong("KEY10", Long.MAX_VALUE));
			assertEquals(Long.MIN_VALUE, preferences.getLong("KEY11", Long.MIN_VALUE));
		} catch (final GeneralSecurityException e) {
			throw new AssertionError(e);
		}
	}

	private static void setValues(@NonNull final SharedPreferences preferences) {
		preferences.edit()
			.putString("KEY1", "VALUE1")
			.putBoolean("KEY2", true)
			.putBoolean("KEY3", false)
			.putFloat("KEY4", 3.141592f)
			.putFloat("KEY5", Float.MIN_VALUE)
			.putFloat("KEY6", Float.MAX_VALUE)
			.putInt("KEY7", 34567)
			.putInt("KEY8", Integer.MIN_VALUE)
			.putLong("KEY9", 987654321L)
			.putLong("KEY10", Long.MIN_VALUE)
			.putLong("KEY11", Long.MAX_VALUE)
			.apply();
	}
}
