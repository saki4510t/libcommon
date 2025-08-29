package com.serenegiant.app;
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
 *
*/

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.serenegiant.system.ContextUtils;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;

/**
 * Activityの操作用ヘルパーメソッドを集めたユーティリティクラス
 */
public class ActivityUtils {
	private static final boolean DEBUG = false;
	private static final String TAG = ActivityUtils.class.getSimpleName();

	private ActivityUtils() {
		// インスタンス化をエラーとするためにデフォルトコンストラクタをprivateに
	}

	/**
	 * 自分のアプリのIdを取得
	 * @param context
	 * @return
	 */
	public static int getAppId(@NonNull final Context context) {
	    int id = -1;
	    final ActivityManager activityManager = ContextUtils.requireSystemService(context, ActivityManager.class);
	    final List<ActivityManager.RunningTaskInfo> recentTasks = activityManager.getRunningTasks(Integer.MAX_VALUE);
	    for (int i = 0; i < recentTasks.size(); i++) {
	        if (recentTasks.get(i).baseActivity.getPackageName().equals(context.getPackageName())) {
	            id = recentTasks.get(i).id;
	            break;
	        }
	    }
		if (DEBUG) Log.v(TAG, "getAppId:" + id);
	    return id;
	}

	/**
	 * 自分のアプリをフォアグラウンドへ移動
	 * XXX このメソッドでActivityをフォアグラウンドへ移動させるとUSBパーミッション要求のダイアログが消えてしまう
	 * @param context
	 */
	@RequiresPermission(android.Manifest.permission.REORDER_TASKS)
	public static void moveTaskToFront(@NonNull final Context context) {
		if (DEBUG) Log.v(TAG, "moveTaskToFront:");
	    final int id = getAppId(context);
	    if (id > 0) {
			final ActivityManager activityManager = ContextUtils.requireSystemService(context, ActivityManager.class);
			if (DEBUG) Log.v(TAG, "moveTaskToFront:" + id);
	        activityManager.moveTaskToFront(id, ActivityManager.MOVE_TASK_WITH_HOME);
	    }
	}

	/**
	 * Activityをフォアグラウンドへ移動
	 * Activityをフォアグラウンド/タスクのトップへ移動
	 * AndroidManifest.xmlで対象のActivityへandroid:launchMode="singleTask"を指定
	 * @param activity
	 */
	public static void bringToForeground(@NonNull final Activity activity) {
		if ((activity != null) && !activity.isFinishing()) {
			final Intent intent = new Intent(activity, activity.getClass())
				.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			activity.startActivity(intent);
		}
	}

	/**
	 * 自分のアプリをフォアグラウンドへ移動
	 * (ランチャーActivityをフォアグランド/タスクのトップへ移動)
	 * AndroidManifest.xmlでランチャーActivityへandroid:launchMode="singleTask"を指定
	 * @param context
	 */
	public static void bringToForeground(@NonNull final Context context) {
		final Intent intent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName())
			.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(intent);
	}

}
