package com.serenegiant.utils;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2017 saki t_saki@serenegiant.com
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

import android.text.TextUtils;

public class ObjectHelper {

	public static boolean asBoolean(final Object val) {
		if (val instanceof Boolean) {
			return (Boolean)val;
		} else if (val instanceof Byte) {
			return ((Byte)val) != 0;
		} else if (val instanceof Short) {
			return ((Short)val) != 0;
		} else if (val instanceof Integer) {
			return ((Integer)val) != 0;
		} else if (val instanceof Long) {
			return ((Long)val) != 0;
		} else if (val instanceof Float) {
			return ((Float)val) != 0;
		} else if (val instanceof Double) {
			return ((Double)val) != 0;
		} else if (val instanceof Number) {
			return ((Number)val).doubleValue() != 0;
		} else if (val instanceof String) {
			if (TextUtils.isEmpty((String)val)) {
				try {
					// 数字の文字列
					return Double.parseDouble((String)val) != 0;
				} catch (final Exception e) {
					try {
						// 整数数字の文字列でDoubleでは変換できない時(16進数文字列とか)
						return Integer.parseInt((String)val) != 0;
					} catch (final Exception e1) {
						// これは"true"かどうかを比較するだけ
						return  Boolean.parseBoolean((String)val);
					}
				}
			} else {
				return false;
			}
		}
		return val != null;
	}

	public static int asInt(final Object val) {
		if (val instanceof Integer) {
			return (Integer)val;
		} else if (val instanceof Boolean) {
			return ((Boolean)val) ? 1 : 0;
		} else if (val instanceof Byte) {
			return ((Byte)val);
		} else if (val instanceof Short) {
			return ((Short)val);
		} else if (val instanceof Long) {
			return ((Long)val).intValue();
		} else if (val instanceof Float) {
			return ((Float)val).intValue();
		} else if (val instanceof Double) {
			return ((Double)val).intValue();
		} else if (val instanceof Number) {
			return ((Number)val).intValue();
		} else if (val instanceof String) {
			if (TextUtils.isEmpty((String)val)) {
				try {
					// 数字の文字列
					final Double v = Double.parseDouble((String)val);
					return v.intValue();
				} catch (final Exception e) {
					try {
						// 整数数字の文字列でDoubleでは変換できない時(16進数文字列とか)
						return Integer.parseInt((String)val);
					} catch (final Exception e1) {
						// これは"true"かどうかを比較するだけ
						return  Boolean.parseBoolean((String)val) ? 1 : 0;
					}
				}
			} else {
				return 0;
			}
		}
		return val != null ? val.hashCode() : 0;
	}
}
