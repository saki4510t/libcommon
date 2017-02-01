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

import android.content.SharedPreferences;

/**
 * Created by saki on 2016/11/05.
 *
 */
public class PrefHelper {
	public static int getShort(final SharedPreferences pref, final String key, final short defaultValue) {
		boolean success = false;
		short result = defaultValue;
		try {
			result = (short)pref.getInt(key, defaultValue);
			success = true;
		} catch (final Exception e) {
		}
		if (!success) {
			try {
				final String v = pref.getString(key, Short.toString(defaultValue));
				try {
					result = (short)Integer.parseInt(v);
					success = true;
				} catch (final Exception e1) {
				}
				if (!success) {
					// 16進文字列かも
					try {
						result = (short)Integer.parseInt(v, 16);
						success = true;
					} catch (final Exception e2) {

					}
				}
				if (!success) {
					// 16進文字列かも
					try {
						result = (short)Integer.parseInt("0x" + v, 16);
					} catch (final Exception e2) {

					}
				}
			} catch (final Exception e) {
				// ignore
			}
		}
		return result;
	}

	public static int getInt(final SharedPreferences pref, final String key, final int defaultValue) {
		boolean success = false;
		int result = defaultValue;
		try {
			result = (int)pref.getLong(key, defaultValue);
			success = true;
		} catch (final Exception e) {
		}
		if (!success) {
			try {
				final String v = pref.getString(key, Integer.toString(defaultValue));
				try {
					result = (int)Long.parseLong(v);
					success = true;
				} catch (final Exception e1) {
				}
				if (!success) {
					// 16進文字列かも
					try {
						result = (int)Long.parseLong(v, 16);
						success = true;
					} catch (final Exception e2) {

					}
				}
				if (!success) {
					// 16進文字列かも
					try {
						result = (int)Long.parseLong("0x" + v, 16);
					} catch (final Exception e2) {

					}
				}
			} catch (final Exception e) {
				// ignore
			}
		}
		return result;
	}

	public static long getLong(final SharedPreferences pref, final String key, final long defaultValue) {
		boolean success = false;
		long result = defaultValue;
		try {
			result = pref.getLong(key, defaultValue);
			success = true;
		} catch (final Exception e) {
		}
		if (!success) {
			try {
				final String v = pref.getString(key, Long.toString(defaultValue));
				try {
					result = Long.parseLong(v);
					success = true;
				} catch (final Exception e1) {
				}
				if (!success) {
					// 16進文字列かも
					try {
						result = Long.parseLong(v, 16);
						success = true;
					} catch (final Exception e2) {

					}
				}
				if (!success) {
					// 16進文字列かも
					try {
						result = Long.parseLong("0x" + v, 16);
					} catch (final Exception e2) {

					}
				}
			} catch (final Exception e) {
				// ignore
			}
		}
		return result;
	}

	public static float getFloat(final SharedPreferences pref, final String key, final float defaultValue) {
		float result = defaultValue;
		try {
			result = pref.getFloat(key, defaultValue);
		} catch (final Exception e) {
			try {
				result = (float)Double.parseDouble(pref.getString(key, Float.toString(defaultValue)));
			} catch (final Exception e1) {
				// ignore
			}
		}
		return result;
	}

	public static double getDouble(final SharedPreferences pref, final String key, final double defaultValue) {
		double result = defaultValue;
		try {
			result = Double.parseDouble(pref.getString(key, Double.toString(defaultValue)));
		} catch (final Exception e1) {
			// ignore
		}
		return result;
	}

}
