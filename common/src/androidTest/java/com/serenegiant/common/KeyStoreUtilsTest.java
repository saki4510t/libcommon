package com.serenegiant.common;

import android.content.Context;
import android.content.SharedPreferences;

import com.serenegiant.security.KeyStoreUtils;
import com.serenegiant.security.ObfuscatorException;
import com.serenegiant.system.EncryptedSharedPreferences;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.security.GeneralSecurityException;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
