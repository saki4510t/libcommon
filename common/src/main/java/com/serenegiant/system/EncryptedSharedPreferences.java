package com.serenegiant.system;

import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Base64;

import com.serenegiant.nio.CharsetsUtils;

import java.security.GeneralSecurityException;
import java.security.spec.KeySpec;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * SharedPreferencesをラップしてデータを自動的に暗号化・復号するためのSharedPreferences実装
 */
public class EncryptedSharedPreferences implements SharedPreferences {
	private static final boolean DEBUG = true;
	private static final String TAG = EncryptedSharedPreferences.class.getSimpleName();

	/**
	 * 暗号化・復号時のエラーを通知するためのException実装
	 */
	public static class ObfuscatorException extends Exception {
		private static final long serialVersionUID = -437726590003072651L;

		public ObfuscatorException() {
		}

		public ObfuscatorException(final String message) {
			super(message);
		}

		public ObfuscatorException(final String message, final Throwable cause) {
			super(message, cause);
		}

		public ObfuscatorException(final Throwable cause) {
			super(cause);
		}

	}

	/**
	 * 暗号化・復号化インターフェース
	 */
	public interface Obfuscator {
		@Nullable
		public String encrypt(@NonNull final String key, @Nullable final String value);
		@Nullable
		public String decrypt(@NonNull final String key, @Nullable final String encrypted)
			throws ObfuscatorException;
	}

//--------------------------------------------------------------------------------
	@NonNull
	private final SharedPreferences mSharedPreferences;
	@NonNull final Obfuscator mObfuscator;

	/**
	 * コンストラクタ
	 * @param preferences ラップするSharedPreferencesオブジェクト
	 * @param obfuscator　暗号化・復号のためのObfuscator実装
	 */
	public EncryptedSharedPreferences(
		@NonNull final SharedPreferences preferences,
		@NonNull final Obfuscator obfuscator) {

		mSharedPreferences = preferences;
		mObfuscator = obfuscator;
	}

	@NonNull
	public Set<String> keySet() {
		final Map<String, ?> map = mSharedPreferences.getAll();
		return new HashSet<>(map.keySet());
	}

	@Override
	public Map<String, ?> getAll() {
		throw new UnsupportedOperationException("#getAll is not available");
	}

	@Nullable
	@Override
	public String getString(final String key, @Nullable final String defValue) {
		try {
			return unobfuscate(key, defValue);
		} catch (final ObfuscatorException e) {
			throw new IllegalArgumentException(e);
		}
	}

	@Nullable
	@Override
	public Set<String> getStringSet(final String key, @Nullable final Set<String> defValues) {
		throw new UnsupportedOperationException("#getStringSet is not available");
	}

	@Override
	public int getInt(final String key, final int defValue) {
		try {
			return Integer.parseInt(unobfuscate(key, Integer.toString(defValue)));
		} catch (final ObfuscatorException e) {
			throw new IllegalArgumentException(e);
		} catch (final NumberFormatException e) {
			throw new ClassCastException();
		}
	}

	@Override
	public long getLong(final String key, final long defValue) {
		try {
			return Long.parseLong(unobfuscate(key, Long.toString(defValue)));
		} catch (final ObfuscatorException e) {
			throw new IllegalArgumentException(e);
		} catch (final NumberFormatException e) {
			throw new ClassCastException();
		}
	}

	@Override
	public float getFloat(final String key, final float defValue) {
		try {
			return Float.parseFloat(unobfuscate(key, Float.toString(defValue)));
		} catch (final ObfuscatorException e) {
			throw new IllegalArgumentException(e);
		} catch (final NumberFormatException e) {
			throw new ClassCastException();
		}
	}

	@Override
	public boolean getBoolean(final String key, final boolean defValue) {
		try {
			return Boolean.parseBoolean(unobfuscate(key, Boolean.toString(defValue)));
		} catch (final ObfuscatorException e) {
			throw new IllegalArgumentException(e);
		}
	}

	@Override
	public boolean contains(final String key) {
		return mSharedPreferences.contains(key);
	}

	@Override
	public Editor edit() {
		return new EncryptedEditor(mSharedPreferences.edit(), mObfuscator);
	}

	@Override
	public void registerOnSharedPreferenceChangeListener(final OnSharedPreferenceChangeListener listener) {
		mSharedPreferences.registerOnSharedPreferenceChangeListener(listener);
	}

	@Override
	public void unregisterOnSharedPreferenceChangeListener(final OnSharedPreferenceChangeListener listener) {
		mSharedPreferences.unregisterOnSharedPreferenceChangeListener(listener);
	}

	@Nullable
	private String unobfuscate(@NonNull final String key, @Nullable final String defaultValue) throws ObfuscatorException {
		final String encrypted = mSharedPreferences.getString(key, null);
		if (encrypted != null) {
			return mObfuscator.decrypt(key, encrypted);
		} else {
			return defaultValue;
		}
	}

//--------------------------------------------------------------------------------

	/**
	 * 暗号化してSharedPreferencesへ保存するためのEditor実装
	 */
	private static class EncryptedEditor implements Editor {
		@NonNull
		private final Editor mEditor;
		@NonNull final Obfuscator mObfuscator;

		public EncryptedEditor(@NonNull final Editor editor, @NonNull final Obfuscator obfuscator) {
			mEditor = editor;
			mObfuscator = obfuscator;
		}

		@Override
		public Editor putString(final String key, @Nullable final String value) {
			if (!TextUtils.isEmpty(value)) {
				mEditor.putString(key, obfuscate(key, value));
			} else {
				remove(key);
			}
			return this;
		}

		@Override
		public Editor putStringSet(final String key, @Nullable final Set<String> values) {
			throw new UnsupportedOperationException("#putStringSet is not available");
		}

		@Override
		public Editor putInt(final String key, final int value) {
			putString(key, Integer.toString(value));
			return this;
		}

		@Override
		public Editor putLong(final String key, final long value) {
			putString(key, Long.toString(value));
			return this;
		}

		@Override
		public Editor putFloat(final String key, final float value) {
			putString(key, Float.toString(value));
			return this;
		}

		@Override
		public Editor putBoolean(final String key, final boolean value) {
			putString(key, Boolean.toString(value));
			return this;
		}

		@Override
		public Editor remove(final String key) {
			mEditor.remove(key);
			return this;
		}

		@Override
		public Editor clear() {
			mEditor.clear();
			return this;
		}

		@Override
		public boolean commit() {
			return mEditor.commit();
		}

		@Override
		public void apply() {
			mEditor.apply();
		}

		@Nullable
		private String obfuscate(@NonNull final String key, @NonNull final String value) {
			return mObfuscator.encrypt(key, value);
		}
	}

//--------------------------------------------------------------------------------
	/**
	 * AES暗号化・復号によるObfuscator実装
	 */
	public static class AESObfuscator implements Obfuscator {
		private static final String KEYGEN_ALGORITHM = "PBEWITHSHAAND256BITAES-CBC-BC";
		private static final String CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";
		private static final String header = "com.serenegiant.AESObfuscator-1|";

		private final Cipher mEncryptor;
		private final Cipher mDecryptor;

		public AESObfuscator(
			@Nullable final char[] password,
			@NonNull final byte[] salt, @NonNull final byte[] iv) throws GeneralSecurityException {

			final SecretKeyFactory factory = SecretKeyFactory.getInstance(KEYGEN_ALGORITHM);
			final KeySpec keySpec = new PBEKeySpec(password, salt, 1024, 256);
			final SecretKey tmp = factory.generateSecret(keySpec);
			final SecretKey secret = new SecretKeySpec(tmp.getEncoded(), "AES");
			mEncryptor = Cipher.getInstance(CIPHER_ALGORITHM);
			mEncryptor.init(Cipher.ENCRYPT_MODE, secret, new IvParameterSpec(iv));
			mDecryptor = Cipher.getInstance(CIPHER_ALGORITHM);
			mDecryptor.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(iv));
		}

		@Nullable
		@Override
		public String encrypt(@NonNull final String key, @Nullable final String value) {
			if (TextUtils.isEmpty(value)) {
				return null;
			}
			try {
				// Header is appended as an integrity check
				return new String(
					Base64.encode(
						mEncryptor.doFinal(
							(header + key + value).getBytes(CharsetsUtils.UTF8)),
						Base64.DEFAULT),
					CharsetsUtils.UTF8);
			} catch (final GeneralSecurityException e) {
				throw new RuntimeException("Invalid environment", e);
			}
		}

		@Nullable
		@Override
		public String decrypt(
			@NonNull final String key,
			@Nullable final String encrypted) throws ObfuscatorException {

			if (TextUtils.isEmpty(encrypted)) {
				return null;
			}
			try {
				final String result = new String(mDecryptor.doFinal(Base64.decode(encrypted, Base64.DEFAULT)), CharsetsUtils.UTF8);
				final int headerIndex = result.indexOf(header+key);
				if (headerIndex != 0) {
					throw new ObfuscatorException("Header not found (invalid data or key)" + (DEBUG ? (":" + encrypted) : ""));
				}
				return result.substring(header.length() + key.length());
			} catch (final IllegalArgumentException e) {
				throw new ObfuscatorException(e.getMessage() + (DEBUG ? (":" + encrypted) : ""));
			} catch (final IllegalBlockSizeException e) {
				throw new ObfuscatorException(e.getMessage() + (DEBUG ? (":" + encrypted) : ""));
			} catch (final BadPaddingException e) {
				throw new ObfuscatorException(e.getMessage() + (DEBUG ? (":" + encrypted) : ""));
			}
		}
	}
}
