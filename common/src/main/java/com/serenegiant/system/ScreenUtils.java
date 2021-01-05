package com.serenegiant.system;
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
import android.view.Window;
import android.view.WindowManager;

import com.serenegiant.utils.BrightnessHelper;

import androidx.annotation.NonNull;

public class ScreenUtils {

	private ScreenUtils() {
		// インスタンス化をエラーにするためにデフォルトコンストラクタをprivateに
	}

	/**
	 * 画面の自動消灯のON/OFF
	 * @param activity
	 * @param onoff
	 */
	public static void setKeepScreenOn(@NonNull final Activity activity, final boolean onoff) {
		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				final Window window = activity.getWindow();
				if (onoff) {
					window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
				} else {
					window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
					BrightnessHelper.setBrightness(activity,
						WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE);	// システム設定に戻す
				}
			}
		});
	}
}
