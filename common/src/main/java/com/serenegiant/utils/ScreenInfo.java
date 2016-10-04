package com.serenegiant.utils;
/*
 * Androusb
 * Copyright (c) 2014-2016 saki t_saki@serenegiant.com
 * Distributed under the terms of the GNU Lesser General Public License (LGPL v3.0) License.
 * License details are in the file license.txt, distributed as part of this software.
 */

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Point;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

public class ScreenInfo {

	@SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
	public static JSONObject get(final Activity activity) throws JSONException {
		final JSONObject result = new JSONObject();
		try {
			final WindowManager wm = activity.getWindowManager();
			final Display display = wm.getDefaultDisplay();
			final DisplayMetrics metrics = new DisplayMetrics();
			display.getMetrics(metrics);
			try {
				result.put("widthPixels", metrics.widthPixels);
			} catch (final Exception e) {
				result.put("widthPixels", e.getMessage());
			}
			try {
				result.put("heightPixels", metrics.heightPixels);
			} catch (final Exception e) {
				result.put("heightPixels", e.getMessage());
			}
			try {
				result.put("density", metrics.density);
			} catch (final Exception e) {
				result.put("density", e.getMessage());
			}
			try {
				result.put("densityDpi", metrics.densityDpi);
			} catch (final Exception e) {
				result.put("densityDpi", e.getMessage());
			}
			try {
				result.put("scaledDensity", metrics.scaledDensity);
			} catch (final Exception e) {
				result.put("scaledDensity", e.getMessage());
			}
			try {
				result.put("xdpi", metrics.xdpi);
			} catch (final Exception e) {
				result.put("xdpi", e.getMessage());
			}
			try {
				result.put("ydpi", metrics.ydpi);
			} catch (final Exception e) {
				result.put("ydpi", e.getMessage());
			}
			try {
				final Point size = new Point();
				if (BuildCheck.isAndroid4_2()) {
					display.getRealSize(size);
					result.put("width", size.x);
					result.put("height", size.y);
				} else {
					result.put("width", display.getWidth());
					result.put("height", display.getHeight());
				}
			} catch (final Exception e) {
				result.put("size", e.getMessage());
			}
		} catch (final Exception e) {
			result.put("EXCEPTION", e.getMessage());
		}
		return result;
	}


}
