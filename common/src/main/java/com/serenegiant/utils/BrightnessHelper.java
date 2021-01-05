package com.serenegiant.utils;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2021 saki t_saki@serenegiant.com
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

import android.app.Activity;
import androidx.annotation.NonNull;

import android.view.Window;
import android.view.WindowManager;

public class BrightnessHelper {
	private BrightnessHelper() {
		// インスタンス化をエラーにするためにデフォルトコンストラクタをprivateに
	}

	public static void setBrightness(@NonNull final Activity activity, final float brightness) {
		if (activity.isFinishing()) return;
		final Window win = activity.getWindow();
		final WindowManager.LayoutParams lp = win.getAttributes();
		float _brightness = brightness;
		if (brightness > 1.0f) {
			_brightness = 1.0f;
		} else if (brightness < -1.0f) {
			_brightness = -1.0f;
		}
		lp.screenBrightness = _brightness;
		lp.buttonBrightness = _brightness;
		win.setAttributes(lp);
	}

	public float getBrightness(@NonNull final Activity activity) {
		final WindowManager.LayoutParams lp = activity.getWindow().getAttributes();
		return lp.screenBrightness;
	}
}
