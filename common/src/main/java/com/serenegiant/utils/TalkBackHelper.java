package com.serenegiant.utils;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2018 saki t_saki@serenegiant.com
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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.accessibility.AccessibilityEventCompat;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;

public class TalkBackHelper {
	/**
	 * Accessibilityが有効になっているかどうかを取得
	 * @param context
	 * @return
	 */
	public static boolean isEnabled(@NonNull final Context context) {
		final AccessibilityManager manager = (AccessibilityManager) context
			.getSystemService(Context.ACCESSIBILITY_SERVICE);
		return manager.isEnabled();
	}

	/**
	 * 指定したテキストをTalkBackで読み上げる(TalkBackが有効な場合)
	 * @param text
	 */
	public static void announceText(@NonNull final Context context,
		@Nullable final CharSequence text) {

		if ((text == null) || (context == null)) return;
		final AccessibilityManager manager = (AccessibilityManager) context
			.getSystemService(Context.ACCESSIBILITY_SERVICE);
		if (manager.isEnabled()) {
			final AccessibilityEvent event = AccessibilityEvent.obtain();
			event.setEventType(AccessibilityEventCompat.TYPE_ANNOUNCEMENT);
		    event.setClassName(TalkBackHelper.class.getName());
		    event.setPackageName(context.getPackageName());
		    event.getText().add(text);
		    manager.sendAccessibilityEvent(event);
		}
	}
}
