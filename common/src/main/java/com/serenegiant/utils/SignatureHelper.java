package com.serenegiant.utils;
/*
 * Androusb
 * Copyright (c) 2014-2016 saki t_saki@serenegiant.com
 * Distributed under the terms of the GNU Lesser General Public License (LGPL v3.0) License.
 * License details are in the file license.txt, distributed as part of this software.
 */

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.text.TextUtils;

public class SignatureHelper {
	/**
	 * apkの署名が指定したkeyと一致しているかどうかをチェック
	 * @param context
	 * @param key
	 * @return true:一致している
	 * @throws IllegalArgumentException
	 * @throws PackageManager.NameNotFoundException
	 */
	public static boolean checkSignature(final Context context, final String key)
		throws IllegalArgumentException, PackageManager.NameNotFoundException {

		if ((context == null) || TextUtils.isEmpty(key)) {
			throw new IllegalArgumentException("context or key is null");
		}
		final Signature expected = new Signature(key);
		boolean result = true;
		final PackageManager pm = context.getPackageManager();
		final PackageInfo packageInfo = pm.getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNATURES);

		// 通常[0]のみ
		for (int i = 0; i < packageInfo.signatures.length; i++) {
			result &= expected.equals(packageInfo.signatures[i]);
		}
		return result;
	}

	/**
	 * apkの署名を取得
	 * @param context
	 * @return 署名を取得できなければnull
	 */
	public static String getSignature(final Context context) {
		if (context != null) {
			final PackageManager pm = context.getPackageManager();
			try {
				final PackageInfo packageInfo = pm.getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNATURES);
				// 通常[0]のみ
				for (int i = 0; i < packageInfo.signatures.length; i++) {
					final Signature signature = packageInfo.signatures[i];
					if (signature != null) {
						return signature.toCharsString();
					}
				}
			} catch (final Exception e) {
			}
		}
		return null;
	}

	/**
	 * apkの署名を取得
	 * @param context
	 * @return 署名を取得できなければnull
	 */
	public static byte[] getSignatureBytes(final Context context) {
		if (context != null) {
			final PackageManager pm = context.getPackageManager();
			try {
				final PackageInfo packageInfo = pm.getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNATURES);
				// 通常[0]のみ
				for (int i = 0; i < packageInfo.signatures.length; i++) {
					final Signature signature = packageInfo.signatures[i];
					if (signature != null) {
						return signature.toByteArray();
					}
				}
			} catch (final Exception e) {
			}
		}
		return null;
	}

}
