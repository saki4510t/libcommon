package com.serenegiant.system;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.security.KeyPairGeneratorSpec;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.serenegiant.nio.CharsetsUtils;
import com.serenegiant.security.ObfuscatorException;

import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.KeySpec;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.x500.X500Principal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

/**
 * SharedPreferencesをラップしてデータを自動的に暗号化・復号するためのSharedPreferences実装
 */
public class EncryptedSharedPreferences implements SharedPreferences {
	private static final boolean DEBUG = false;
	private static final String TAG = EncryptedSharedPreferences.class.getSimpleName();

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
						Base64.NO_WRAP),
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
				final String result = new String(mDecryptor.doFinal(Base64.decode(encrypted, Base64.NO_WRAP)), CharsetsUtils.UTF8);
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

	/**
	 * KeyStoreを使って保持している秘密鍵・公開鍵を使って
	 * 暗号化・復号を行うためのヘルパークラス, API>=18
	 * API>=23の場合にはAES, API<23の場合はRSAで暗号化・復号する
	 * 各アプリに対応する鍵がないときは自動的に生成する
	 */
	@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
	public static class KeyStoreObfuscator implements Obfuscator {

		public static void deleteKey(@NonNull final Context context, @Nullable final String name) {
			final String alias = context.getPackageName() + (!TextUtils.isEmpty(name) ? ":" + name : "");
			try {
				final KeyStore keyStore = KeyStore.getInstance(KEY_STORE_TYPE);
				keyStore.load(null);
				boolean containsAlias = keyStore.containsAlias(alias);
				if (containsAlias) {
					keyStore.deleteEntry(alias);
				}
			} catch (final GeneralSecurityException | IOException e) {
				if (DEBUG) Log.w(TAG, e);
			}
		}

		private static final String KEY_STORE_TYPE = "AndroidKeyStore";
		private static final String header = "com.serenegiant.KeyStoreObfuscator-1|";

		// API22以下で利用
		private static final String ALGORITHM_RSA = "RSA";
		private static final String CIPHER_TRANSFORMATION_RSA = "RSA/ECB/PKCS1Padding";

		@TargetApi(Build.VERSION_CODES.M)
		private static final String CIPHER_TRANSFORMATION_AES
			= KeyProperties.KEY_ALGORITHM_AES
				+ "/" + KeyProperties.BLOCK_MODE_CBC
				+ "/" + KeyProperties.ENCRYPTION_PADDING_PKCS7;

		@NonNull
		private final Context mContext;
		@NonNull
		private final String alias;

		/**
		 * コンストラクタ
		 * API>=23の場合にはAES, API<23の場合はRSAで暗号化・復号する
		 * @param context
		 * @param name
		 * @throws GeneralSecurityException
		 */
		public KeyStoreObfuscator(@NonNull final Context context, final String name)
			throws GeneralSecurityException {

			mContext = context;
			alias = context.getPackageName() + (!TextUtils.isEmpty(name) ? ":" + name : "");
			try {
				final KeyStore.Entry entry = getKey();
				if (entry == null) {
					throw new GeneralSecurityException("key entry not found");
				}
			} catch (final IOException e) {
				throw new GeneralSecurityException(e);
			}
		}

		@Nullable
		@Override
		public String encrypt(@NonNull final String key, @Nullable final String value) {
			if (TextUtils.isEmpty(value)) {
				return null;
			}
			// AESでの暗号化・復号はAPI>=23なので場合分けする
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				return encryptAES(key, value);
			} else {
				return encryptRSA(key, value);
			}
		}

		@Nullable
		@Override
		public String decrypt(
			@NonNull final String key, @Nullable final String encrypted)
				throws ObfuscatorException {

			if (TextUtils.isEmpty(encrypted)) {
				return null;
			}
			// AESでの暗号化・復号はAPI>=23なので場合分けする
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				return decryptAES(key, encrypted);
			} else {
				return decryptRSA(key, encrypted);
			}
		}

		/**
		 * RSAで暗号化
		 * @param key
		 * @param value
		 * @return
		 */
		private String encryptRSA(@NonNull final String key, @NonNull final String value) {
			try {
				final KeyStore.Entry entry = getKey();
				final PublicKey publicKey = ((KeyStore.PrivateKeyEntry) entry).getCertificate().getPublicKey();
				final Cipher encryptor = Cipher.getInstance(CIPHER_TRANSFORMATION_RSA);
				encryptor.init(Cipher.ENCRYPT_MODE, publicKey);
				// Header is appended as an integrity check
				return base64Encode(
					encryptor.doFinal((header + key + value).getBytes(CharsetsUtils.UTF8)));
			} catch (final GeneralSecurityException | IOException e) {
				throw new RuntimeException("Invalid environment", e);
			}
		}

		/**
		 * RSAで復号
		 * @param key
		 * @param encrypted
		 * @return
		 * @throws ObfuscatorException
		 */
		private String decryptRSA(
			@NonNull final String key, @NonNull final String encrypted)
				throws ObfuscatorException {

			try {
				final KeyStore.Entry entry = getKey();
				final PrivateKey privateKey = ((KeyStore.PrivateKeyEntry) entry).getPrivateKey();
				final Cipher decryptor = Cipher.getInstance(CIPHER_TRANSFORMATION_RSA);
				decryptor.init(Cipher.DECRYPT_MODE, privateKey);
				final String result = new String(decryptor.doFinal(base64decode(encrypted)), CharsetsUtils.UTF8);
				final int headerIndex = result.indexOf(header+key);
				if (headerIndex != 0) {
					throw new ObfuscatorException("Header not found (invalid data or key)" + (DEBUG ? (":" + encrypted) : ""));
				}
				return result.substring(header.length() + key.length());
			} catch (final GeneralSecurityException | IOException e) {
				throw new ObfuscatorException(e.getMessage() + (DEBUG ? (":" + encrypted) : ""));
			} catch (final IllegalArgumentException e) {
				throw new ObfuscatorException(e.getMessage() + (DEBUG ? (":" + encrypted) : ""));
			}
		}

		/**
		 * AESで暗号化
		 * @param key
		 * @param value
		 * @return
		 */
		@RequiresApi(api = Build.VERSION_CODES.M)
		private String encryptAES(@NonNull final String key, @NonNull final String value) {
			if (DEBUG) Log.v(TAG, "encryptAES:key=" + key + ",value=" + value);
			try {
				final KeyStore.Entry entry = getKey();
				final SecretKey secretKey = ((KeyStore.SecretKeyEntry) entry).getSecretKey();
				final Cipher encryptor = Cipher.getInstance(CIPHER_TRANSFORMATION_AES);
				encryptor.init(Cipher.ENCRYPT_MODE, secretKey);
				// Header is appended as an integrity check
				return base64Encode(encryptor.getIV()) + "|"
					+ base64Encode(
						encryptor.doFinal((header + key + value).getBytes(CharsetsUtils.UTF8)));
			} catch (final GeneralSecurityException | IOException e) {
				throw new RuntimeException("Invalid environment", e);
			}
		}

		/**
		 * AESで復号
		 * @param key
		 * @param encrypted
		 * @return
		 * @throws ObfuscatorException
		 */
		@RequiresApi(api = Build.VERSION_CODES.M)
		private String decryptAES(@NonNull final String key, @NonNull final String encrypted)
			throws ObfuscatorException {

			final String[] split = encrypted.split("\\|");
			if (split.length < 2) {
				throw new ObfuscatorException("Unexpected encrypted format");
			}
			if (DEBUG) Log.v(TAG, "decryptAES:key=" + key + ",encrypted=" + encrypted + ",splitNum=" + split.length);
			try {
				final KeyStore.Entry entry = getKey();
				final SecretKey secretKey = ((KeyStore.SecretKeyEntry) entry).getSecretKey();
				final Cipher decryptor = Cipher.getInstance(CIPHER_TRANSFORMATION_AES);
				decryptor.init(Cipher.DECRYPT_MODE, secretKey,
					new IvParameterSpec(base64decode(split[0])));
				final String result = new String(
					decryptor.doFinal(base64decode(split[1])),
					CharsetsUtils.UTF8);
				final int headerIndex = result.indexOf(header+key);
				if (headerIndex != 0) {
					throw new ObfuscatorException("Header not found (invalid data or key)" + (DEBUG ? (":" + encrypted) : ""));
				}
				if (DEBUG) Log.v(TAG, "decryptAES:key=" + key + ",result=" + result);
				return result.substring(header.length() + key.length());
			} catch (final GeneralSecurityException e) {
				throw new ObfuscatorException(e.getMessage() + (DEBUG ? (":" + encrypted) : ""));
			} catch (final IllegalArgumentException e) {
				throw new ObfuscatorException(e.getMessage() + (DEBUG ? (":" + encrypted) : ""));
			} catch (final IOException e) {
				throw new ObfuscatorException(e.getMessage() + (DEBUG ? (":" + encrypted) : ""));
			}

		}

		/**
		 * KeyStoreからKeyStore.Entryを取得する
		 * 対象となるエリアスに対応するエントリーがないときは自動的に生成する
		 * @return
		 * @throws GeneralSecurityException
		 * @throws IOException
		 */
		@Nullable
		private KeyStore.Entry getKey()
			throws GeneralSecurityException, IOException {

			final KeyStore keyStore = KeyStore.getInstance(KEY_STORE_TYPE);
			keyStore.load(null);
			boolean containsAlias = keyStore.containsAlias(alias);
			if (containsAlias) {
				final Key key = keyStore.getKey(alias, null);
				if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
					&& ALGORITHM_RSA.equals(key.getAlgorithm())) {

					if (DEBUG) Log.v(TAG, "getKey:key algorithm mismatch");
					keyStore.deleteEntry(alias);
					containsAlias = false;
				}
			}
			if (!containsAlias) {
				createKey();
			}
			return keyStore.getEntry(alias, null);
		}

		private void createKey()
			throws GeneralSecurityException {

			final Calendar start = Calendar.getInstance(Locale.ENGLISH);
			final Calendar end = Calendar.getInstance(Locale.ENGLISH);
			end.add(Calendar.YEAR, 1);
			final X500Principal principal = new X500Principal(
				"CN=" + alias + " O=" + mContext.getPackageName());
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				final KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(
					alias, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
					.setCertificateSubject(principal)
					.setCertificateSerialNumber(BigInteger.ONE)
					.setBlockModes(KeyProperties.BLOCK_MODE_CBC)
					.setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
//					.setKeyValidityStart(start.getTime())
//					.setKeyValidityEnd(end.getTime())
					.build();
				final KeyGenerator kg
					= KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEY_STORE_TYPE);
				kg.init(spec);
				kg.generateKey();
			} else {
				final KeyPairGenerator kpg
					= KeyPairGenerator.getInstance(ALGORITHM_RSA, KEY_STORE_TYPE);
				final KeyPairGeneratorSpec spec = new KeyPairGeneratorSpec.Builder(mContext)
						.setAlias(alias)
						.setSubject(principal)
						.setSerialNumber(BigInteger.ONE)
						.setStartDate(start.getTime())
						.setEndDate(end.getTime())
						.build();
				kpg.initialize(spec);
				kpg.generateKeyPair();
			}
		}

		@NonNull
		private static String base64Encode(@NonNull final byte[] bytes) {
			return new String(Base64.encode(bytes, Base64.NO_WRAP), CharsetsUtils.UTF8);
		}

		@NonNull
		private static byte[] base64decode(@NonNull final String encoded) {
			return Base64.decode(encoded, Base64.NO_WRAP);
		}
	}
}
