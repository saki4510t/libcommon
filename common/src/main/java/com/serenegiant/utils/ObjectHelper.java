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

import android.text.TextUtils;

/**
 * Objectからの型変換を行うためのヘルパークラス
 */
public class ObjectHelper {

	private ObjectHelper() {
		// インスタンス化をエラーにするためにデフォルトコンストラクタをprivateに
	}

	/**
	 * booleanへ変換
	 * @param value
	 * @param defaultValue
	 * @return
	 */
	public static boolean asBoolean(final Object value, final boolean defaultValue) {
		// XXX char(文字)の処理を入れた方が良い？intに暗黙のキャストされるから問題ない？
		if (value instanceof Boolean) {
			return (Boolean)value;
		} else if (value instanceof Byte) {
			return ((Byte)value) != 0;
		} else if (value instanceof Number) {
			// Byte/Short/Integer/Long/Float/Doubleは全てNumberを継承しているので個別の条件分岐は省いてNumberとして判定
			return ((Number)value).doubleValue() != 0;
		} else if (value instanceof String) {
			// 空文字列だけではなくブランク文字列(空白文字列だけ)もデフォルト値にする
			final String trimmedValue = ((String) value).trim();
			if (!TextUtils.isEmpty(trimmedValue)) {
				final String v = trimmedValue.toLowerCase();
				if ("true".equalsIgnoreCase(v)) {
					return true;
				} else if ("false".equalsIgnoreCase(v)) {
					return false;
				}
				// 16進文字列かも
				if (v.startsWith("0x") || v.startsWith("-0x") || v.startsWith("+0x")) {
					final String vv = v.replace("0x", "");
					try {
						return Long.parseLong(vv, 16) != 0;
					} catch (final Exception e1) {
						//
					}
				}
				try {
					// 数字の文字列
					return Double.parseDouble(v) != 0;
				} catch (final Exception e) {
					//
				}
				// 16進文字列かも
				try {
					return Integer.parseInt(v, 16) != 0;
				} catch (final Exception e) {
					//
				}
				// 16進文字列かも
				try {
					return Long.parseLong(v, 16) != 0;
				} catch (final Exception e) {
					//
				}
				return Boolean.parseBoolean(v);
			}
		}

		return defaultValue;
	}

	/**
	 * byteへ変換
	 * @param value
	 * @param defaultValue
	 * @return
	 */
	public static byte asByte(final Object value, final byte defaultValue) {
		if (value instanceof Boolean) {
			return (Boolean)value ? (byte)1 : (byte)0;
		} else if (value instanceof Number) {
			// Byte/Short/Integer/Long/Float/Doubleは全てNumberを継承しているので個別の条件分岐は省いてNumberとして判定
			return ((Number)value).byteValue();
		} else if (value instanceof String) {
			// 空文字列だけではなくブランク文字列(空白文字列だけ)もデフォルト値にする
			final String trimmedValue = ((String) value).trim();
			if (!TextUtils.isEmpty(trimmedValue)) {
				final String v = trimmedValue.toLowerCase();
				// 16進文字列かも
				if (v.startsWith("0x") || v.startsWith("-0x") || v.startsWith("+0x")) {
					final String vv = v.replace("0x", "");
					try {
						return ((Long)Long.parseLong(vv, 16)).byteValue();
					} catch (final Exception e1) {
						//
					}
				}
				try {
					// 数字の文字列
					return ((Double)Double.parseDouble(v)).byteValue();
				} catch (final Exception e) {
					//
				}
				// 16進文字列かも
				try {
					return ((Integer)Integer.parseInt(v, 16)).byteValue();
				} catch (final Exception e) {
					//
				}
				// 16進文字列かも
				try {
					return ((Long)Long.parseLong(v, 16)).byteValue();
				} catch (final Exception e) {
					//
				}
				if ("true".equalsIgnoreCase(v)) {
					return (byte)1;
				} else if ("false".equalsIgnoreCase(v)) {
					return 0;
				}
			}
		}

		return defaultValue;
	}

	/**
	 * shortへ変換
	 * @param value
	 * @param defaultValue
	 * @return
	 */
	public static short asShort(final Object value, final short defaultValue) {
		if (value instanceof Boolean) {
			return (Boolean)value ? (short)1 : (short)0;
		} else if (value instanceof Number) {
			// Byte/Short/Integer/Long/Float/Doubleは全てNumberを継承しているので個別の条件分岐は省いてNumberとして判定
			return ((Number)value).shortValue();
		} else if (value instanceof String) {
			// 空文字列だけではなくブランク文字列(空白文字列だけ)もデフォルト値にする
			final String trimmedValue = ((String) value).trim();
			if (!TextUtils.isEmpty(trimmedValue)) {
				final String v = trimmedValue.toLowerCase();
				if ("true".equalsIgnoreCase(v)) {
					return (short)1;
				} else if ("false".equalsIgnoreCase(v)) {
					return 0;
				}
				// 16進文字列かも
				if (v.startsWith("0x") || v.startsWith("-0x") || v.startsWith("+0x")) {
					final String vv = v.replace("0x", "");
					try {
						return ((Long)Long.parseLong(vv, 16)).shortValue();
					} catch (final Exception e1) {
						//
					}
				}
				try {
					// 数字の文字列
					return ((Double)Double.parseDouble(v)).shortValue();
				} catch (final Exception e) {
					//
				}
				// 16進文字列かも
				try {
					return ((Integer)Integer.parseInt(v, 16)).shortValue();
				} catch (final Exception e) {
					//
				}
				// 16進文字列かも
				try {
					return ((Long)Long.parseLong(v, 16)).shortValue();
				} catch (final Exception e) {
					//
				}
			}
		}

		return defaultValue;
	}

	/**
	 * intへ変換
	 * @param value
	 * @param defaultValue
	 * @return
	 */
	public static int asInt(final Object value, final int defaultValue) {
		if (value instanceof Boolean) {
			return ((Boolean)value) ? 1 : 0;
		} else if (value instanceof Number) {
			// Byte/Short/Integer/Long/Float/Doubleは全てNumberを継承しているので個別の条件分岐は省いてNumberとして判定
			return ((Number)value).intValue();
		} else if (value instanceof String) {
			// 空文字列だけではなくブランク文字列(空白文字列だけ)もデフォルト値にする
			final String trimmedValue = ((String) value).trim();
			if (!TextUtils.isEmpty(trimmedValue)) {
				final String v = trimmedValue.toLowerCase();
				if ("true".equalsIgnoreCase(v)) {
					return 1;
				} else if ("false".equalsIgnoreCase(v)) {
					return 0;
				}
				// 16進文字列かも
				if (v.startsWith("0x") || v.startsWith("-0x") || v.startsWith("+0x")) {
					final String vv = v.replace("0x", "");
					try {
						return ((Long)Long.parseLong(vv, 16)).intValue();
					} catch (final Exception e1) {
						//
					}
				}
				try {
					// 数字の文字列
					return ((Double)Double.parseDouble(v)).intValue();
				} catch (final Exception e) {
					//
				}
				// 16進文字列かも
				try {
					return Integer.parseInt(v, 16);
				} catch (final Exception e2) {
					//
				}
				// 16進文字列かも
				try {
					return ((Long)Long.parseLong(v, 16)).intValue();
				} catch (final Exception e) {
					//
				}
			}
		}

		return defaultValue;
	}

	/**
	 * longへ変換
	 * @param value
	 * @param defaultValue
	 * @return
	 */
	public static long asLong(final Object value, final long defaultValue) {
		if (value instanceof Boolean) {
			return ((Boolean)value) ? 1 : 0;
		} else if (value instanceof Number) {
			// Byte/Short/Integer/Long/Float/Doubleは全てNumberを継承しているので個別の条件分岐は省いてNumberとして判定
			return ((Number)value).longValue();
		} else if (value instanceof String) {
			// 空文字列だけではなくブランク文字列(空白文字列だけ)もデフォルト値にする
			final String trimmedValue = ((String) value).trim();
			if (!TextUtils.isEmpty(trimmedValue)) {
				final String v = trimmedValue.toLowerCase();
				if ("true".equalsIgnoreCase(v)) {
					return 1;
				} else if ("false".equalsIgnoreCase(v)) {
					return 0;
				}
				// 16進文字列かも
				if (v.startsWith("0x") || v.startsWith("-0x") || v.startsWith("+0x")) {
					final String vv = v.replace("0x", "");
					try {
						return Long.parseLong(vv, 16);
					} catch (final Exception e1) {
						//
					}
				}
				try {
					// 数字の文字列
					return ((Double)Double.parseDouble(v)).longValue();
				} catch (final Exception e) {
					//
				}
				// 16進文字列かも
				try {
					return Long.parseLong(v, 16);
				} catch (final Exception e2) {
					//
				}
			}
		}

		return defaultValue;
	}

	/**
	 * floatへ変換
	 * @param value
	 * @param defaultValue
	 * @return
	 */
	public static float asFloat(final Object value, final float defaultValue) {
		if (value instanceof Boolean) {
			return ((Boolean)value) ? 1 : 0;
		} else if (value instanceof Number) {
			// Byte/Short/Integer/Long/Float/Doubleは全てNumberを継承しているので個別の条件分岐は省いてNumberとして判定
			return ((Number)value).floatValue();
		} else if (value instanceof String) {
			// 空文字列だけではなくブランク文字列(空白文字列だけ)もデフォルト値にする
			final String trimmedValue = ((String) value).trim();
			if (!TextUtils.isEmpty(trimmedValue)) {
				final String v = trimmedValue.toLowerCase();
				if ("true".equalsIgnoreCase(v)) {
					return 1;
				} else if ("false".equalsIgnoreCase(v)) {
					return 0;
				}
				// 16進文字列かも
				if (v.startsWith("0x") || v.startsWith("-0x") || v.startsWith("+0x")) {
					final String vv = v.replace("0x", "");
					try {
						return ((Long)Long.parseLong(vv, 16)).floatValue();
					} catch (final Exception e1) {
						//
					}
				}
				try {
					// 数字の文字列
					return ((Double)Double.parseDouble(v)).floatValue();
				} catch (final Exception e) {
					//
				}
				// 16進文字列かも
				try {
					return ((Long)Long.parseLong(v, 16)).floatValue();
				} catch (final Exception e2) {
					//
				}
			}
		}

		return defaultValue;
	}

	/**
	 * doubleへ変換
	 * @param value
	 * @param defaultValue
	 * @return
	 */
	public static double asDouble(final Object value, final double defaultValue) {
		if (value instanceof Boolean) {
			return ((Boolean)value) ? 1 : 0;
		} else if (value instanceof Number) {
			// Byte/Short/Integer/Long/Float/Doubleは全てNumberを継承しているので個別の条件分岐は省いてNumberとして判定
			return ((Number)value).doubleValue();
		} else if (value instanceof String) {
			// 空文字列だけではなくブランク文字列(空白文字列だけ)もデフォルト値にする
			final String trimmedValue = ((String) value).trim();
			if (!TextUtils.isEmpty(trimmedValue)) {
				final String v = trimmedValue.toLowerCase();
				if ("true".equalsIgnoreCase(v)) {
					return 1;
				} else if ("false".equalsIgnoreCase(v)) {
					return 0;
				}
				// 16進文字列かも
				if (v.startsWith("0x") || v.startsWith("-0x") || v.startsWith("+0x")) {
					final String vv = v.replace("0x", "");
					try {
						return ((Long)Long.parseLong(vv, 16)).doubleValue();
					} catch (final Exception e1) {
						//
					}
				}
				try {
					// 数字の文字列
					return Double.parseDouble(v);
				} catch (final Exception e) {
					//
				}
				// 16進文字列かも
				try {
					return ((Long)Long.parseLong(v, 16)).doubleValue();
				} catch (final Exception e2) {
					//
				}
			}
		}

		return defaultValue;
	}

	/**
	 * ObjectをasXXXで変換して比較する
	 * valueがプリミティブ以外の場合はvalue#equals(object)で比較する
	 * @param value
	 * @param object
	 * @return
	 * @param <T>
	 */
	public static <T> boolean equals(final T value, final Object object) {
		if (value == null) {
			return (object == null);
		}
		// これより下はvalue!=null
		final Class<?> valueClass = ClassUtils.getPrimitiveClass(value);
		if (valueClass == Boolean.TYPE) {
			return (boolean)value == asBoolean(object, !(boolean)value);
		} else if (valueClass == Byte.TYPE) {
			return (byte)value == asByte(object, (byte)(~(byte)value));
		} else if (valueClass == Short.TYPE) {
			return (short)value == asShort(object, (short)(~(short)value));
		} else if (valueClass == Integer.TYPE) {
			return (int)value == asInt(object, ~(int)value);
		} else if (valueClass == Long.TYPE) {
			return (long)value == asLong(object, ~(long)value);
		} else if (valueClass == Float.TYPE) {
			return (float)value == asFloat(object, ((float)value + 1.0f) / 2.0f);
		} else if (valueClass == Double.TYPE) {
			return (double)value == asDouble(object, ((double)value + 1.0) / 2.0);
		} else {
			return value.equals(object);
		}
	}
}
