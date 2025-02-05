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
		} else if (value instanceof Short) {
			return ((Short)value) != 0;
		} else if (value instanceof Integer) {
			return ((Integer)value) != 0;
		} else if (value instanceof Long) {
			return ((Long)value) != 0;
		} else if (value instanceof Float) {
			return ((Float)value) != 0;
		} else if (value instanceof Double) {
			return ((Double)value) != 0;
		} else if (value instanceof Number) {
			return ((Number)value).doubleValue() != 0;
		} else if (value instanceof String) {
			// 空文字列だけではなく空白文字列だけもデフォルト値にする
			final String trimmedValue = ((String) value).trim();
			if (!TextUtils.isEmpty(trimmedValue)) {
				final String v = trimmedValue;
				// 16進文字列かも
				if (v.startsWith("0x") || v.startsWith("0X")) {
					try {
						return Integer.parseInt(v.substring(2), 16) != 0;
					} catch (final Exception e1) {
						return Long.parseLong(v.substring(2), 16) != 0;
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
				return Boolean.parseBoolean((String)value);
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
		} else if (value instanceof Byte) {
			return ((Byte)value);
		} else if (value instanceof Short) {
			return ((Short)value).byteValue();
		} else if (value instanceof Integer) {
			return ((Integer)value).byteValue();
		} else if (value instanceof Long) {
			return ((Long)value).byteValue();
		} else if (value instanceof Float) {
			return ((Float)value).byteValue();
		} else if (value instanceof Double) {
			return ((Double)value).byteValue();
		} else if (value instanceof Number) {
			return ((Number)value).byteValue();
		} else if (value instanceof String) {
			if (!TextUtils.isEmpty((String)value)) {
				final String v = (String)value;
				// 16進文字列かも
				if (v.startsWith("0x") || v.startsWith("0X")) {
					try {
						return (byte)Integer.parseInt(v.substring(2), 16);
					} catch (final Exception e1) {
						return (byte)Long.parseLong(v.substring(2), 16);
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
				return  Boolean.parseBoolean((String)value) ? (byte)1 : (byte)0;
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
		} else if (value instanceof Byte) {
			return ((Byte)value);
		} else if (value instanceof Short) {
			return ((Short)value);
		} else if (value instanceof Integer) {
			return ((Integer)value).shortValue();
		} else if (value instanceof Long) {
			return ((Long)value).shortValue();
		} else if (value instanceof Float) {
			return ((Float)value).shortValue();
		} else if (value instanceof Double) {
			return ((Double)value).shortValue();
		} else if (value instanceof Number) {
			return ((Number)value).shortValue();
		} else if (value instanceof String) {
			if (!TextUtils.isEmpty((String)value)) {
				final String v = (String)value;
				// 16進文字列かも
				if (v.startsWith("0x") || v.startsWith("0X")) {
					try {
						return (short)Integer.parseInt(v.substring(2), 16);
					} catch (final Exception e1) {
						return (short)Long.parseLong(v.substring(2), 16);
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
				return  Boolean.parseBoolean((String)value) ? (short)1 : (short)0;
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
		} else if (value instanceof Byte) {
			return ((Byte)value);
		} else if (value instanceof Short) {
			return ((Short)value);
		} else if (value instanceof Integer) {
			return (Integer)value;
		} else if (value instanceof Long) {
			return ((Long)value).intValue();
		} else if (value instanceof Float) {
			return ((Float)value).intValue();
		} else if (value instanceof Double) {
			return ((Double)value).intValue();
		} else if (value instanceof Number) {
			return ((Number)value).intValue();
		} else if (value instanceof String) {
			if (!TextUtils.isEmpty((String)value)) {
				final String v = (String)value;
				// 16進文字列かも
				if (v.startsWith("0x") || v.startsWith("0X")) {
					try {
						return Integer.parseInt(v.substring(2), 16);
					} catch (final Exception e1) {
						return ((Long)Long.parseLong(v.substring(2), 16)).intValue();
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
				return Boolean.parseBoolean((String)value) ? 1 : 0;
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
		} else if (value instanceof Byte) {
			return ((Byte)value);
		} else if (value instanceof Short) {
			return ((Short)value);
		} else if (value instanceof Integer) {
			return (Integer)value;
		} else if (value instanceof Long) {
			return (Long)value;
		} else if (value instanceof Float) {
			return ((Float)value).longValue();
		} else if (value instanceof Double) {
			return ((Double)value).longValue();
		} else if (value instanceof Number) {
			return ((Number)value).longValue();
		} else if (value instanceof String) {
			if (!TextUtils.isEmpty((String)value)) {
				final String v = (String)value;
				// 16進文字列かも
				if (v.startsWith("0x") || v.startsWith("0X")) {
					try {
						return Long.parseLong(v.substring(2), 16);
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
				return Boolean.parseBoolean((String)value) ? 1 : 0;
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
		} else if (value instanceof Byte) {
			return ((Byte)value);
		} else if (value instanceof Short) {
			return ((Short)value);
		} else if (value instanceof Integer) {
			return (Integer)value;
		} else if (value instanceof Long) {
			return ((Long)value).floatValue();
		} else if (value instanceof Float) {
			return (Float)value;
		} else if (value instanceof Double) {
			return ((Double)value).floatValue();
		} else if (value instanceof Number) {
			return ((Number)value).floatValue();
		} else if (value instanceof String) {
			if (!TextUtils.isEmpty((String)value)) {
				final String v = (String)value;
				// 16進文字列かも
				if (v.startsWith("0x") || v.startsWith("0X")) {
					try {
						return ((Long)Long.parseLong(v.substring(2), 16)).floatValue();
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
				return Boolean.parseBoolean((String)value) ? 1 : 0;
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
		} else if (value instanceof Byte) {
			return ((Byte)value);
		} else if (value instanceof Short) {
			return ((Short)value);
		} else if (value instanceof Integer) {
			return (Integer)value;
		} else if (value instanceof Long) {
			return ((Long)value).doubleValue();
		} else if (value instanceof Float) {
			return (Float)value;
		} else if (value instanceof Double) {
			return (Double)value;
		} else if (value instanceof Number) {
			return ((Number)value).doubleValue();
		} else if (value instanceof String) {
			if (!TextUtils.isEmpty((String)value)) {
				final String v = (String)value;
				// 16進文字列かも
				if (v.startsWith("0x") || v.startsWith("0X")) {
					try {
						return ((Long)Long.parseLong(v.substring(2), 16)).doubleValue();
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
				return Boolean.parseBoolean((String)value) ? 1 : 0;
			}
		}
		return defaultValue;
	}
}
