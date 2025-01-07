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

import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;

import com.serenegiant.nio.CharsetsUtils;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import androidx.annotation.NonNull;
import androidx.annotation.RawRes;

/**
 * デフォルト値付きでリソース値を取得するためのヘルパークラス
 */
public class ResourceHelper {

	private ResourceHelper() {
		// インスタンス化をエラーにするためにデフォルトコンストラクタをprivateに
	}

	public static int get(@NonNull final Context context,
		final String value, final int defaultValue)
		throws Resources.NotFoundException, NumberFormatException, NullPointerException {

		int result = defaultValue;
		String v = value;
		if (!TextUtils.isEmpty(v) && v.startsWith("@")) {
			final String r = v.substring(1);
			final int resId = context.getResources().getIdentifier(r, null, context.getPackageName());
			if (resId > 0) {
				result = context.getResources().getInteger(resId);
			}
		} else {
			int radix = 10;
			if (v != null && v.length() > 2 && v.charAt(0) == '0' &&
				(v.charAt(1) == 'x' || v.charAt(1) == 'X')) {
				// allow hex values starting with 0x or 0X
				radix = 16;
				v = v.substring(2);
			}
			result = Integer.parseInt(v, radix);
		}

		return result;
	}

	public static boolean get(@NonNull final Context context,
		final String value, final boolean defaultValue)
			throws Resources.NotFoundException, NumberFormatException, NullPointerException {

		boolean result = defaultValue;
		String v = value;
		if ("TRUE".equalsIgnoreCase(v)) {
			result = true;
		} else if ("FALSE".equalsIgnoreCase(v)) {
			result = false;
		} else if (!TextUtils.isEmpty(v) && v.startsWith("@")) {
			final String r = v.substring(1);
			final int resId = context.getResources().getIdentifier(r, null, context.getPackageName());
			if (resId > 0) {
				result = context.getResources().getBoolean(resId);
			}
		} else {
			int radix = 10;
			if (v != null && v.length() > 2 && v.charAt(0) == '0' &&
				(v.charAt(1) == 'x' || v.charAt(1) == 'X')) {
				// allow hex values starting with 0x or 0X
				radix = 16;
				v = v.substring(2);
			}
			final int val = Integer.parseInt(v, radix);
			result = val != 0;
		}

		return result;
	}

	public static String get(@NonNull final Context context,
		final String value, final String defaultValue)
			throws Resources.NotFoundException, NullPointerException {

		String result = value;
		if (result == null) {
			result = defaultValue;
		}
		if (!TextUtils.isEmpty(result) && result.startsWith("@")) {
			final String r = result.substring(1);
			final int resId = context.getResources().getIdentifier(r, null, context.getPackageName());
			if (resId > 0) {
				result = context.getResources().getString(resId);
			}
		}

		return result;
	}

	public static CharSequence get(@NonNull final Context context,
		final CharSequence value, final CharSequence defaultValue)
			throws Resources.NotFoundException, NullPointerException {

		CharSequence result = value;
		if (result == null) {
			result = defaultValue;
		}
		if (!TextUtils.isEmpty(result)) {
			final String s = result.toString();
			if (s.startsWith("@")) {
				final String r = s.substring(1);
				final int resId = context.getResources().getIdentifier(r, null, context.getPackageName());
				if (resId > 0) {
					result = context.getResources().getText(resId);
				}
			}
		}

		return result;
	}

	/**
	 * rawリソースをバイト配列に読み込む
	 * @param context
	 * @param resId
	 * @return
	 * @throws IOException
	 */
	public static byte[] getRawResource(@NonNull final Context context, @RawRes final int resId) throws IOException {
	    final InputStream is = new BufferedInputStream(context.getResources().openRawResource(resId));
	    final ByteArrayOutputStream bout = new ByteArrayOutputStream();
	    final byte[] readBuffer = new byte[4 * 1024];
	    try {
	        int read;
	        do {
	            read = is.read(readBuffer, 0, readBuffer.length);
	            if(read == -1) {
	                break;
	            }
	            bout.write(readBuffer, 0, read);
	        } while(true);
	        return bout.toByteArray();
	    } finally {
	        is.close();
	    }
	}

	/**
	 * エンコードを指定してrawリソースからUTF-8文字列を読み込む
	 * @param context
	 * @param resId
	 * @param encoding
	 * @return
	 * @throws IOException
	 */
	public static String getRawString(
		@NonNull final Context context, @RawRes final int resId,
		@NonNull final Charset encoding) throws IOException {
	    return new String(getRawResource(context, resId), encoding);
	}

	/**
	 * rawリソースからUTF-8文字列を読み込む
	 * @param context
	 * @param resId
	 * @return
	 * @throws IOException
	 */
	public static String getRawString(
		@NonNull final Context context, @RawRes final int resId) throws IOException {
	    return new String(getRawResource(context, resId), CharsetsUtils.UTF8);
	}

}
