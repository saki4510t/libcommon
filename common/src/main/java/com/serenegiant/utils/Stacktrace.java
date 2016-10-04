package com.serenegiant.utils;
/*
 * Androusb
 * Copyright (c) 2014-2016 saki t_saki@serenegiant.com
 * Distributed under the terms of the GNU Lesser General Public License (LGPL v3.0) License.
 * License details are in the file license.txt, distributed as part of this software.
 */

import android.util.Log;

public class Stacktrace {
	private static final String TAG = "Stacktrace";
	public static void print() {
		final Throwable t = new Throwable();
		final StringBuilder sb = new StringBuilder();
		final StackTraceElement[] elms = t.getStackTrace();
		boolean top = true;
		if (elms != null) {
			for (final StackTraceElement elm: elms) {
				if (!top && (elm != null)) {
					sb.append(elm.toString()).append("\n");
				} else {
					top = false;
				}
			}
		}
		Log.i(TAG, sb.toString());
	}
}
