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

import android.content.Context;
import android.content.res.Resources;

public class SettingsUtils {

	private SettingsUtils() {
		// インスタンス化をエラーにするためにデフォルトコンストラクタをprivateに
	}

	public static int getMinimumScreenBrightnessSetting(final Context context) {
		final Resources res = Resources.getSystem();
		int id = res.getIdentifier("config_screenBrightnessSettingMinimum", "integer", "android"); // API17+
		if (id == 0)
			id = res.getIdentifier("config_screenBrightnessDim", "integer", "android"); // lower API levels
		if (id != 0) {
			try {
				return res.getInteger(id);
			} catch (final Resources.NotFoundException e) {
				// ignore
			}
		}
		return 0;
	}

	public static int getMaximumScreenBrightnessSetting(final Context context) {
		final Resources res = Resources.getSystem();
		final int id = res.getIdentifier("config_screenBrightnessSettingMaximum", "integer", "android"); // API17+
		if (id != 0) {
			try {
				return res.getInteger(id);
			} catch (final Resources.NotFoundException e) {
				// ignore
			}
		}
		return 255;
	}
}
