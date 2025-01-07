package com.serenegiant.utils;
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

import android.util.Log;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.security.MessageDigest;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringDef;

/**
 * MessageDigestを使ったハッシュ計算用ヘルパークラス
 */
public class HashUtils {
	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = HashUtils.class.getSimpleName();

	private HashUtils() {
		// インスタンス化をエラーにするためにデフォルトコンストラクタをprivateに
	}

	public static final String HASH_ALGORITHM_MD2 = "MD2";
	public static final String HASH_ALGORITHM_MD5 = "MD5";
	public static final String HASH_ALGORITHM_SHA1 = "SHA-1";
	public static final String HASH_ALGORITHM_SHA224 = "SHA-224";
	public static final String HASH_ALGORITHM_SHA256 = "SHA-256";
	public static final String HASH_ALGORITHM_SHA384 = "SHA-384";
	public static final String HASH_ALGORITHM_SHA512_224 = "SHA-512/224";
	public static final String HASH_ALGORITHM_SHA512_256 = "SHA-512/256";
	public static final String HASH_ALGORITHM_SHA3_224 = "SHA3-224";
	public static final String HASH_ALGORITHM_SHA3_256 = "SHA3-256";
	public static final String HASH_ALGORITHM_SHA3_384 = "SHA3-384";
	public static final String HASH_ALGORITHM_SHA3_512 = "SHA3-512";

	@StringDef({
		HASH_ALGORITHM_MD2,
		HASH_ALGORITHM_MD5,
		HASH_ALGORITHM_SHA1,
		HASH_ALGORITHM_SHA224,
		HASH_ALGORITHM_SHA256,
		HASH_ALGORITHM_SHA384,
		HASH_ALGORITHM_SHA512_224,
		HASH_ALGORITHM_SHA512_256,
		HASH_ALGORITHM_SHA3_224,
		HASH_ALGORITHM_SHA3_256,
		HASH_ALGORITHM_SHA3_384,
		HASH_ALGORITHM_SHA3_512,
	})
	@Retention(RetentionPolicy.SOURCE)
	public @interface HashAlgorithm {}

	/**
	 * 指定した方法で計算したハッシュをbyte配列として返す
	 * @param algorithm　"SHA-1", "MD5", "SHA-256"
	 * @param data
	 * @return 正常に計算できればハッシュの16進数文字列, algorithmが存在しないなどで計算できなかった時null
	 */
	@Nullable
	public static byte[] getDigest(
		@HashAlgorithm @NonNull final String algorithm,
		@NonNull final byte[] data) {

		try {
			final MessageDigest digest = MessageDigest.getInstance(algorithm);
			return digest.digest(data);
		} catch (final Exception e) {
			if (DEBUG) Log.w(TAG, e);
		}
		return null;
	}

	/**
	 * 指定した方法で計算したハッシュを16進数文字列として返す
	 * @param algorithm　"SHA-1", "MD5", "SHA-256"
	 * @param data
	 * @return 正常に計算できればハッシュの16進数文字列, algorithmが存在しないなどで計算できなかった時null
	 */
	@Nullable
	public static String getDigestString(
		@HashAlgorithm @NonNull final String algorithm,
		@NonNull final byte[] data) {

		try {
			return BufferHelper.toHexString(getDigest(algorithm, data));
		} catch (final Exception e) {
			if (DEBUG) Log.w(TAG, e);
		}
		return null;
	}
}
