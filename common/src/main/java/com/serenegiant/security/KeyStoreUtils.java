package com.serenegiant.security;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.security.KeyPairGeneratorSpec;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.serenegiant.nio.CharsetsUtils;

import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Calendar;
import java.util.Locale;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.security.auth.x500.X500Principal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

/**
 * KeyStoreを使って保持している秘密鍵・公開鍵を使って
 * 暗号化・復号を行うためのヘルパークラス, API>=18
 * API>=23の場合にはAES, API<23の場合はRSAで暗号化・復号する
 * 各アプリに対応する鍵がないときは自動的に生成する
 */
@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class KeyStoreUtils {
	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = KeyStoreUtils.class.getSimpleName();

	private static final String KEY_STORE_TYPE = "AndroidKeyStore";

	// API22以下で利用
	private static final String ALGORITHM_RSA = "RSA";
	private static final String CIPHER_TRANSFORMATION_RSA = "RSA/ECB/PKCS1Padding";

	@TargetApi(Build.VERSION_CODES.M)
	private static final String CIPHER_TRANSFORMATION_AES
		= KeyProperties.KEY_ALGORITHM_AES
			+ "/" + KeyProperties.BLOCK_MODE_CBC
			+ "/" + KeyProperties.ENCRYPTION_PADDING_PKCS7;

	private KeyStoreUtils() {
		// インスタンス化をエラーにするためにデフォルトコンストラクタをprivateに
	}

	public static void deleteKey(
		@NonNull final Context context,
		@Nullable final String name) {

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

	@Nullable
	public static String encrypt(
		@NonNull final Context context,
		@Nullable final String name,
		@Nullable final String value) {

		if (TextUtils.isEmpty(value)) {
			return null;
		}
		// AESでの暗号化・復号はAPI>=23なので場合分けする
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			return encryptAES(context, name, value);
		} else {
			return encryptRSA(context, name, value);
		}
	}

	@Nullable
	public static String decrypt(
		@NonNull final Context context,
		@Nullable final String name,
		@Nullable final String encrypted) throws ObfuscatorException {

		if (TextUtils.isEmpty(encrypted)) {
			return null;
		}
		// AESでの暗号化・復号はAPI>=23なので場合分けする
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			return decryptAES(context, name, encrypted);
		} else {
			return decryptRSA(context, name, encrypted);
		}
	}

	/**
	 * RSAで暗号化
	 * @param context
	 * @param name
	 * @param value
	 * @return
	 */
	private static String encryptRSA(
		@NonNull final Context context,
		@Nullable final String name,
		@NonNull final String value) {

		try {
			final KeyStore.Entry entry = getKey(context, name);
			final PublicKey publicKey = ((KeyStore.PrivateKeyEntry) entry).getCertificate().getPublicKey();
			final Cipher encryptor = Cipher.getInstance(CIPHER_TRANSFORMATION_RSA);
			encryptor.init(Cipher.ENCRYPT_MODE, publicKey);
			// Header is appended as an integrity check
			return base64Encode(
				encryptor.doFinal(value.getBytes(CharsetsUtils.UTF8)));
		} catch (final GeneralSecurityException | IOException e) {
			throw new RuntimeException("Invalid environment", e);
		}
	}

	/**
	 * RSAで復号
	 * @param context
	 * @param name
	 * @param encrypted
	 * @return
	 * @throws ObfuscatorException
	 */
	private static String decryptRSA(
		@NonNull final Context context,
		@Nullable final String name,
		@NonNull final String encrypted) throws ObfuscatorException {

		try {
			final KeyStore.Entry entry = getKey(context, name);
			final PrivateKey privateKey = ((KeyStore.PrivateKeyEntry) entry).getPrivateKey();
			final Cipher decryptor = Cipher.getInstance(CIPHER_TRANSFORMATION_RSA);
			decryptor.init(Cipher.DECRYPT_MODE, privateKey);
			return new String(decryptor.doFinal(base64decode(encrypted)), CharsetsUtils.UTF8);
		} catch (final GeneralSecurityException | IOException e) {
			throw new ObfuscatorException(e.getMessage() + (DEBUG ? (":" + encrypted) : ""));
		} catch (final IllegalArgumentException e) {
			throw new ObfuscatorException(e.getMessage() + (DEBUG ? (":" + encrypted) : ""));
		}
	}

	/**
	 * AESで暗号化
	 * @param context
	 * @param name
	 * @param value
	 * @return
	 */
	@RequiresApi(api = Build.VERSION_CODES.M)
	private static String encryptAES(
		@NonNull final Context context,
		@Nullable final String name,
		@NonNull final String value) {

		if (DEBUG) Log.v(TAG, "encryptAES:" + value);
		try {
			final KeyStore.Entry entry = getKey(context, name);
			final SecretKey secretKey = ((KeyStore.SecretKeyEntry) entry).getSecretKey();
			final Cipher encryptor = Cipher.getInstance(CIPHER_TRANSFORMATION_AES);
			encryptor.init(Cipher.ENCRYPT_MODE, secretKey);
			// Header is appended as an integrity check
			return base64Encode(encryptor.getIV()) + "|"
				+ base64Encode(
					encryptor.doFinal(value.getBytes(CharsetsUtils.UTF8)));
		} catch (final GeneralSecurityException | IOException e) {
			throw new RuntimeException("Invalid environment", e);
		}
	}

	/**
	 * AESで復号
	 * @param context
	 * @param name
	 * @param encrypted
	 * @return
	 * @throws ObfuscatorException
	 */
	@RequiresApi(api = Build.VERSION_CODES.M)
	private static String decryptAES(
		@NonNull final Context context,
		@Nullable final String name,
		@NonNull final String encrypted) throws ObfuscatorException {

		final String[] split = encrypted.split("\\|");
		if (split.length < 2) {
			throw new ObfuscatorException("Unexpected encrypted format");
		}
		if (DEBUG) Log.v(TAG, "decryptAES:encrypted=" + encrypted + ",splitNum=" + split.length);
		try {
			final KeyStore.Entry entry = getKey(context, name);
			final SecretKey secretKey = ((KeyStore.SecretKeyEntry) entry).getSecretKey();
			final Cipher decryptor = Cipher.getInstance(CIPHER_TRANSFORMATION_AES);
			decryptor.init(Cipher.DECRYPT_MODE, secretKey,
				new IvParameterSpec(base64decode(split[0])));
			return new String(
				decryptor.doFinal(base64decode(split[1])),
				CharsetsUtils.UTF8);
		} catch (final IllegalArgumentException e) {
			throw new ObfuscatorException(e.getMessage() + (DEBUG ? (":" + encrypted) : ""));
		} catch (final IOException e) {
			throw new ObfuscatorException(e.getMessage() + (DEBUG ? (":" + encrypted) : ""));
		} catch (final GeneralSecurityException e) {
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
	private static KeyStore.Entry getKey(
		@NonNull final Context context,
		@Nullable final String name) throws GeneralSecurityException, IOException {

		final String alias = context.getPackageName() + (!TextUtils.isEmpty(name) ? ":" + name : "");
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
			createKey(context, alias);
		}
		return keyStore.getEntry(alias, null);
	}

	private static void createKey(
		@NonNull final Context context,
		@NonNull final String alias) throws GeneralSecurityException {

		final Calendar start = Calendar.getInstance(Locale.ENGLISH);
		final Calendar end = Calendar.getInstance(Locale.ENGLISH);
		end.add(Calendar.YEAR, 1);
		final X500Principal principal = new X500Principal(
			"CN=" + alias + " O=" + context.getPackageName());
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			final KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(
				alias, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
				.setCertificateSubject(principal)
				.setCertificateSerialNumber(BigInteger.ONE)
				.setBlockModes(KeyProperties.BLOCK_MODE_CBC)
				.setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
//				.setKeyValidityStart(start.getTime())
//				.setKeyValidityEnd(end.getTime())
				.build();
			final KeyGenerator kg
				= KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEY_STORE_TYPE);
			kg.init(spec);
			kg.generateKey();
		} else {
			final KeyPairGenerator kpg
				= KeyPairGenerator.getInstance(ALGORITHM_RSA, KEY_STORE_TYPE);
			final KeyPairGeneratorSpec spec = new KeyPairGeneratorSpec.Builder(context)
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
