package com.serenegiant.utils;
/*
 * Androusb
 * Copyright (c) 2014-2016 saki t_saki@serenegiant.com
 * Distributed under the terms of the GNU Lesser General Public License (LGPL v3.0) License.
 * License details are in the file license.txt, distributed as part of this software.
 */

import android.content.Context;
import android.os.Environment;

import java.io.File;

/**
 * Imported by saki on 15/11/10.
 */
public class DiskUtils {
	/**
	 * キャッシュディレクトリのフルパスを取得する
	 * 外部ストレージが使える場合は外部ストレージのキャッシュディレクトリを、そうでない場合は内部のディレクトリを使う
	 * @param context
	 * @param uniqueName
	 * @return キャッシュディレクトリパス
	 */
	public static String getCacheDir(final Context context, final String uniqueName) {
		// 外部ストレージが使える場合はそっちのディレクトリを、そうでない場合は内部のディレクトリを使う
		final String cachePath =
				(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
				 && !Environment.isExternalStorageRemovable()	// これが使えるのはAPI9以上
				) ? context.getExternalCacheDir().getPath() : context.getCacheDir().getPath();
		return cachePath + File.separator + uniqueName;
	}
}
