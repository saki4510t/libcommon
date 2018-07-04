package com.serenegiant.utils;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2018 saki t_saki@serenegiant.com
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
import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.xmlpull.v1.XmlPullParser;

/**
 * XmlPullParserのヘルパークラス
 */
public class XmlHelper {
	/**
	 * read as integer values with default value from xml(w/o exception throws)
	 * resource integer id is also resolved into integer
	 * @param parser
	 * @param namespace
	 * @param name
	 * @param defaultValue
	 * @return
	 */
	public static final int getAttributeInteger(@NonNull final Context context,
		@NonNull final XmlPullParser parser,
		final String namespace, final String name, final int defaultValue) {

		int result = defaultValue;
		try {
			String v = parser.getAttributeValue(namespace, name);
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
		} catch (final Resources.NotFoundException e) {
			result = defaultValue;
		} catch (final NumberFormatException e) {
			result = defaultValue;
		} catch (final NullPointerException e) {
			result = defaultValue;
		}
		return result;
	}

	/**
	 * read as boolean values with default value from xml(w/o exception throws)
	 * resource boolean id is also resolved into boolean
	 * if the value is zero, return false, if the value is non-zero integer, return true
	 * @param context
	 * @param parser
	 * @param namespace
	 * @param name
	 * @param defaultValue
	 * @return
	 */
	public static final boolean getAttributeBoolean(@NonNull final Context context,
		@NonNull final XmlPullParser parser,
		final String namespace, final String name, final boolean defaultValue) {

		boolean result = defaultValue;
		try {
			String v = parser.getAttributeValue(namespace, name);
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
		} catch (final Resources.NotFoundException e) {
			result = defaultValue;
		} catch (final NumberFormatException e) {
			result = defaultValue;
		} catch (final NullPointerException e) {
			result = defaultValue;
		}
		return result;
	}

	/**
	 * read as String attribute with default value from xml(w/o exception throws)
	 * resource string id is also resolved into string
	 * @param parser
	 * @param namespace
	 * @param name
	 * @param defaultValue
	 * @return
	 */
	public static final String getAttributeString(@NonNull final Context context,
		@NonNull final XmlPullParser parser,
		final String namespace, final String name, final String defaultValue) {

		String result;
		try {
			result = parser.getAttributeValue(namespace, name);
			if (result == null)
				result = defaultValue;
			if (!TextUtils.isEmpty(result) && result.startsWith("@")) {
				final String r = result.substring(1);
				final int resId = context.getResources().getIdentifier(r, null, context.getPackageName());
				if (resId > 0) {
					result = context.getResources().getString(resId);
				}
			}
		} catch (final Resources.NotFoundException e) {
			result = defaultValue;
		} catch (final NumberFormatException e) {
			result = defaultValue;
		} catch (final NullPointerException e) {
			result = defaultValue;
		}
		return result;
	}

	/**
	 * read as String attribute with default value from xml(w/o exception throws)
	 * resource string id is also resolved into string
	 * @param parser
	 * @param namespace
	 * @param name
	 * @param defaultValue
	 * @return
	 */
	public static final CharSequence getAttributeText(@NonNull final Context context,
		@NonNull final XmlPullParser parser,
		final String namespace, final String name, final CharSequence defaultValue) {

		CharSequence result;
		try {
			result = parser.getAttributeValue(namespace, name);
			if (result == null)
				result = defaultValue;
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
		} catch (final Resources.NotFoundException e) {
			result = defaultValue;
		} catch (final NumberFormatException e) {
			result = defaultValue;
		} catch (final NullPointerException e) {
			result = defaultValue;
		}
		return result;
	}
}
