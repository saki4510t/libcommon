package com.serenegiant.common;

import android.content.Context;
import android.content.SharedPreferences;

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
 * com.serenegiant.system.EncryptedSharedPreferences用のインスツルメンテーションテスト用クラス
 */
@RunWith(AndroidJUnit4.class)
public class EncryptedSharedPreferencesTest {
	private static final String TAG = EncryptedSharedPreferencesTest.class.getSimpleName();

	private static final byte[] SALT = {
		(byte)0x69, (byte)0x27, (byte)0x3f, (byte)0x61,
		(byte)0x95, (byte)0xc7, (byte)0xa3, (byte)0x3f,
		(byte)0x33, (byte)0xe3, (byte)0x25, (byte)0xc3,
		(byte)0x9e, (byte)0x2d, (byte)0xdc, (byte)0x8f,
		(byte)0xf3, (byte)0xa5, (byte)0x0c, (byte)0x59,
	};

	private static final byte[] IV = {
		16, 74, 71, -80, 32, 101, -47, 72, 117, -14, 0, -29, 70, 65, -12, 74
	};

	private static double EPS_FLOAT = Float.MIN_NORMAL;

	@Before
	public void prepare() {
		final Context context = ApplicationProvider.getApplicationContext();
		try {
			final SharedPreferences original
				= context.getSharedPreferences(context.getPackageName(), 0);
			original.edit().clear().apply();

			final SharedPreferences preferences
				= new EncryptedSharedPreferences(original,
					new EncryptedSharedPreferences.AESObfuscator(TAG.toCharArray(), SALT, IV));
			assertFalse(preferences.contains("KEY1"));
			setValues(preferences);
		} catch (final GeneralSecurityException e) {
			throw new AssertionError(e);
		}
	}

	@After
	public void cleanUp() {
		final Context context = ApplicationProvider.getApplicationContext();
		final SharedPreferences original
			= context.getSharedPreferences(context.getPackageName(), 0);
		original.edit().clear().apply();
	}

	/**
	 * 正常に読み書きできるかどうかを確認
	 */
	@Test
	public void test1() {
		final Context context = ApplicationProvider.getApplicationContext();
		try {
			final SharedPreferences preferences
				= new EncryptedSharedPreferences(
					context.getSharedPreferences(context.getPackageName(), 0),
					new EncryptedSharedPreferences.AESObfuscator(TAG.toCharArray(), SALT, IV));

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

	/**
	 * パスワードが異なる場合でもキー名へのアクセス自体は正常なはず
	 */
	@Test
	public void test2() {
		final Context context = ApplicationProvider.getApplicationContext();
		try {
			final SharedPreferences preferences
				= new EncryptedSharedPreferences(
					context.getSharedPreferences(context.getPackageName(), 0),
					new EncryptedSharedPreferences.AESObfuscator(null, SALT, IV));
			// キー名は暗号化していないのでアクセスできるはず
			assertTrue(preferences.contains("KEY1"));
		} catch (final GeneralSecurityException e) {
			throw new AssertionError(e);
		}
	}

	/**
	 * パスワードが異なる場合は値の取得時にIllegalArgumentExceptionを投げるはず
	 */
	@Test
	public void test3() {
		final Context context = ApplicationProvider.getApplicationContext();
		try {
			final SharedPreferences preferences
				= new EncryptedSharedPreferences(
					context.getSharedPreferences(context.getPackageName(), 0),
					new EncryptedSharedPreferences.AESObfuscator(null, SALT, IV));

			Throwable ex = null;
			try {
				assertEquals("VALUE1", preferences.getString("KEY1", ""));
			} catch (final Throwable t) {
				ex = t;
			}
			assertTrue(ex instanceof IllegalArgumentException);

			ex = null;
			try {
				assertTrue(preferences.getBoolean("KEY2", false));
			} catch (final Throwable t) {
				ex = t;
			}
			assertTrue(ex instanceof IllegalArgumentException);

			ex = null;
			try {
				assertFalse(preferences.getBoolean("KEY3", true));
			} catch (final Throwable t) {
				ex = t;
			}
			assertTrue(ex instanceof IllegalArgumentException);

			ex = null;
			try {
				assertEquals(3.141592f, preferences.getFloat("KEY4", 0), EPS_FLOAT);
			} catch (final Throwable t) {
				ex = t;
			}
			assertTrue(ex instanceof IllegalArgumentException);

			ex = null;
			try {
				assertEquals(Float.MIN_VALUE, preferences.getFloat("KEY5", 0), EPS_FLOAT);
			} catch (final Throwable t) {
				ex = t;
			}
			assertTrue(ex instanceof IllegalArgumentException);

			ex = null;
			try {
				assertEquals(Float.MAX_VALUE, preferences.getFloat("KEY6", 0), EPS_FLOAT);
			} catch (final Throwable t) {
				ex = t;
			}
			assertTrue(ex instanceof IllegalArgumentException);

			ex = null;
			try {
				assertEquals(34567, preferences.getInt("KEY7", 0));
			} catch (final Throwable t) {
				ex = t;
			}
			assertTrue(ex instanceof IllegalArgumentException);

			ex = null;
			try {
				assertEquals(Integer.MIN_VALUE, preferences.getInt("KEY8", 0));
			} catch (final Throwable t) {
				ex = t;
			}
			assertTrue(ex instanceof IllegalArgumentException);

			preferences.edit()
				.remove("KEY9")
				.remove("KEY10")
				.remove("KEY11")
				.apply();
			assertEquals(0, preferences.getLong("KEY9", 0));
			assertEquals(Long.MIN_VALUE, preferences.getLong("KEY10", Long.MIN_VALUE));
			assertEquals(Long.MAX_VALUE, preferences.getLong("KEY11", Long.MAX_VALUE));

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
