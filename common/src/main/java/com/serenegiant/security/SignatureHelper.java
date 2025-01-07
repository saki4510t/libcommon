package com.serenegiant.security;
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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;

import com.serenegiant.system.BuildCheck;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * apk署名検証用のヘルパークラス
 */
public class SignatureHelper {
	private SignatureHelper() {
		// インスタンス化をエラーにするためにデフォルトコンストラクタをprivateに
	}

	/**
	 * apkの署名が指定したkeyと一致しているかどうかをチェック
	 * @param context
	 * @param key
	 * @return true:一致している
	 * @throws IllegalArgumentException
	 * @throws PackageManager.NameNotFoundException
	 */
	@SuppressLint("NewApi")
	public static boolean checkSignature(@NonNull final Context context, final String key)
		throws IllegalArgumentException, PackageManager.NameNotFoundException {

		if (TextUtils.isEmpty(key)) {
			throw new IllegalArgumentException("context or key is null");
		}
		final Signature expected = new Signature(key);
		boolean result = true;
		final PackageManager pm = context.getPackageManager();
		final Signature[] signatures;
		if (BuildCheck.isPie()) {
			final PackageInfo packageInfo = pm.getPackageInfo(context.getPackageName(),
				PackageManager.GET_SIGNING_CERTIFICATES);
			if (packageInfo.signingInfo.hasMultipleSigners()) {
				signatures = packageInfo.signingInfo.getApkContentsSigners();
			} else {
				signatures = packageInfo.signingInfo.getSigningCertificateHistory();
			}
		} else {
			@SuppressLint("PackageManagerGetSignatures")
			final PackageInfo packageInfo = pm.getPackageInfo(context.getPackageName(),
				PackageManager.GET_SIGNATURES);
			signatures = packageInfo.signatures;
		}
		// 通常[0]のみだけど、悪さをするやつが元の署名を残したまま後ろに署名を追加したりするので全てをチェックすべし
		for (int i = 0; i < signatures.length; i++) {
			result &= expected.equals(signatures[i]);
		}
		return result;
	}

	/**
	 * apkの署名を取得
	 * @param context
	 * @return 署名を取得できなければnull, 複数の署名があれば全てを繋げて返す
	 * @throws PackageManager.NameNotFoundException
	 */
	@SuppressLint("NewApi")
	@Nullable
	public static String getSignature(@NonNull final Context context)
		throws PackageManager.NameNotFoundException {

		final PackageManager pm = context.getPackageManager();
		final Signature[] signatures;
		if (BuildCheck.isPie()) {
			final PackageInfo packageInfo = pm.getPackageInfo(context.getPackageName(),
				PackageManager.GET_SIGNING_CERTIFICATES);
			if (packageInfo.signingInfo.hasMultipleSigners()) {
				signatures = packageInfo.signingInfo.getApkContentsSigners();
			} else {
				signatures = packageInfo.signingInfo.getSigningCertificateHistory();
			}
		} else {
			@SuppressLint("PackageManagerGetSignatures")
			final PackageInfo packageInfo = pm.getPackageInfo(context.getPackageName(),
				PackageManager.GET_SIGNATURES);
			signatures = packageInfo.signatures;
		}
		// 通常[0]のみだけど、悪さをするやつが元の署名を残したまま後ろに署名を追加したりするので全てをチェックすべし
		// 全部つなげて返す
		int cnt = 0;
		final StringBuilder sb = new StringBuilder();
		for (int i = 0; i < signatures.length; i++) {
			final Signature signature = signatures[i];
			if (signature != null) {
				if (cnt != 0) {
					sb.append('/');
				}
				sb.append(signature.toCharsString());
			}
		}
		return sb.toString();
	}

	/**
	 * apkの署名を取得
	 * @param context
	 * @return 署名を取得できなければnull, 複数の署名があれば全てを繋げて返す
	 * @throws PackageManager.NameNotFoundException
	 */
	@SuppressLint("NewApi")
	@Nullable
	public static byte[] getSignatureBytes(@NonNull final Context context)
		throws PackageManager.NameNotFoundException {

		final PackageManager pm = context.getPackageManager();
		final Signature[] signatures;
		if (BuildCheck.isPie()) {
			final PackageInfo packageInfo = pm.getPackageInfo(context.getPackageName(),
				PackageManager.GET_SIGNING_CERTIFICATES);
			if (packageInfo.signingInfo.hasMultipleSigners()) {
				signatures = packageInfo.signingInfo.getApkContentsSigners();
			} else {
				signatures = packageInfo.signingInfo.getSigningCertificateHistory();
			}
		} else {
			@SuppressLint("PackageManagerGetSignatures")
			final PackageInfo packageInfo = pm.getPackageInfo(context.getPackageName(),
				PackageManager.GET_SIGNATURES);
			signatures = packageInfo.signatures;
		}
		ByteBuffer result = ByteBuffer.allocate(1024);
		// 通常[0]のみだけど、悪さをするやつが元の署名を残したまま後ろに署名を追加したりするので全てをチェックすべし
		// 全部つなげて返す
		for (int i = 0; i < signatures.length; i++) {
			final Signature signature = signatures[i];
			if (signature != null) {
				final byte[] bytes = signature.toByteArray();
				final int n = bytes != null ? bytes.length : 0;
				if (n > 0) {
					if (n > result.remaining()) {
						result.flip();
						final ByteBuffer temp = ByteBuffer.allocate(result.capacity() + n * 2);
						temp.put(result);
						result = temp;
					}
					result.put(bytes);
				}
			}
		}
		result.flip();
		final int n = result.limit();
		if (n > 0) {
			final byte[] bytes = new byte[n];
			result.get(bytes);
			return bytes;
		}
		return null;
	}

	/**
	 * apk署名のハッシュを取得
	 * @param context
	 * @param algorithm ハッシュのアルゴリズム, "SHA-1", "MD5", "SHA-256"
	 * @return
	 * @throws PackageManager.NameNotFoundException
	 * @throws NoSuchAlgorithmException
	 */
	@SuppressLint("NewApi")
	public static byte[] getSignaturesDigest(
		@NonNull final Context context, @NonNull final String algorithm)
		throws PackageManager.NameNotFoundException, NoSuchAlgorithmException {

		final PackageManager pm = context.getPackageManager();
		final Signature[] signatures;
		if (BuildCheck.isPie()) {
			final PackageInfo packageInfo = pm.getPackageInfo(context.getPackageName(),
				PackageManager.GET_SIGNING_CERTIFICATES);
			if (packageInfo.signingInfo.hasMultipleSigners()) {
				signatures = packageInfo.signingInfo.getApkContentsSigners();
			} else {
				signatures = packageInfo.signingInfo.getSigningCertificateHistory();
			}
		} else {
			@SuppressLint("PackageManagerGetSignatures")
			final PackageInfo packageInfo = pm.getPackageInfo(context.getPackageName(),
				PackageManager.GET_SIGNATURES);
			signatures = packageInfo.signatures;
		}
		final MessageDigest digest = MessageDigest.getInstance(algorithm);
		for (int i = 0; i < signatures.length; i++) {
			final Signature signature = signatures[i];
			if (signature != null) {
				digest.update(signature.toByteArray());
			}
		}
		return digest.digest();
	}
}
