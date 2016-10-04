package com.serenegiant.utils;
/*
 * Androusb
 * Copyright (c) 2014-2016 saki t_saki@serenegiant.com
 * Distributed under the terms of the GNU Lesser General Public License (LGPL v3.0) License.
 * License details are in the file license.txt, distributed as part of this software.
 */

import android.app.Activity;
import android.view.WindowManager;

public class BrightnessHelper {
	public static void setBrightness(final Activity activity, final float brightness) {
		final WindowManager.LayoutParams lp = activity.getWindow().getAttributes();
		float _brightness = brightness;
		if (brightness > 1.0f) {
			_brightness = 1.0f;
		}
		lp.screenBrightness = _brightness;
		activity.getWindow().setAttributes(lp);
	}

	public float getBrightness(final Activity activity) {
		final WindowManager.LayoutParams lp = activity.getWindow().getAttributes();
		return lp.screenBrightness;
	}
}
