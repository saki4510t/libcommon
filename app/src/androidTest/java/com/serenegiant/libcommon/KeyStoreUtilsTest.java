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

import com.serenegiant.security.KeyStoreUtils;
import com.serenegiant.security.ObfuscatorException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import static org.junit.Assert.*;

/**
 * KeyStoreUtils用のインスツルメンテーションテスト用クラス
 */
@RunWith(AndroidJUnit4.class)
public class KeyStoreUtilsTest {
	private static final String TAG = KeyStoreUtilsTest.class.getSimpleName();

	@Before
	public void prepare() {
	}

	@After
	public void cleanUp() {
		final Context context = ApplicationProvider.getApplicationContext();
		KeyStoreUtils.deleteKey(context, TAG);
	}

	/**
	 * 暗号化した文字列を復号して元の文字列に戻るかどうかを確認
	 */
	@Test
	public void test1() {
		final Context context = ApplicationProvider.getApplicationContext();
		try {
			final String original = context.getPackageName();
			final String encrypted = KeyStoreUtils.encrypt(context, TAG, original);
			final String decrypted = KeyStoreUtils.decrypt(context, TAG, encrypted);
			assertEquals(original, decrypted);
		} catch (final ObfuscatorException e) {
			throw new AssertionError(e);
		}
	}

}
