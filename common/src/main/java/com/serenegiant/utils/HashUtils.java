package com.serenegiant.utils;

import android.util.Log;

import java.security.MessageDigest;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class HashUtils {
	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = HashUtils.class.getSimpleName();

	/**
	 * 指定した方法で計算したハッシュをbyte配列として返す
	 * @param algorithm　"SHA-1", "MD5", "SHA-256"
	 * @param data
	 * @return 正常に計算できればハッシュの16進数文字列, algorithmが存在しないなどで計算できなかった時null
	 */
	@Nullable
	public static byte[] getDigest(@NonNull final String algorithm, @NonNull final byte[] data) {
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
	public static String getDigestString(@NonNull final String algorithm, @NonNull final byte[] data) {
		try {
			return BufferHelper.toHexString(getDigest(algorithm, data));
		} catch (final Exception e) {
			if (DEBUG) Log.w(TAG, e);
		}
		return null;
	}
}
