package com.serenegiant.system;
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
*/

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.StatFs;
import android.text.TextUtils;
import android.util.Log;

import com.serenegiant.utils.FileUtils;
import com.serenegiant.utils.UriHelper;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

public class StorageUtils {
	private StorageUtils() {
		// インスタンス化をエラーにするためにデフォルトコンストラクタをprivateにする
	}

	/**
	 * 外部ストレージ名の取得を試みる
	 * @return
	 */
	@Nullable
	public static String getExternalMounts() {
    	String externalpath = null;
    	String internalpath = "";

    	final Runtime runtime = Runtime.getRuntime();
    	try {
    		String line;
    		final Process proc = runtime.exec("mount");
    		final BufferedReader br = new BufferedReader(
    			new InputStreamReader(proc.getInputStream()));
    		while ((line = br.readLine()) != null) {
//    			Log.i(TAG, "getExternalMounts:" + line);
    			if (line.contains("secure")) continue;
    			if (line.contains("asec")) continue;

    			if (line.contains("fat")) {//external card
    				final String[] columns = line.split(" ");
    				if (columns != null && (columns.length > 1) && !TextUtils.isEmpty(columns[1])) {
    					externalpath = columns[1];
    					if (!externalpath.endsWith("/")) {
    						externalpath = externalpath + "/";
    					}
    				}
    			} else if (line.contains("fuse")) {//internal storage
    				final String[] columns = line.split(" ");
    				if (columns != null && columns.length > 1) {
    					internalpath = internalpath.concat("[" + columns[1] + "]");
    				}
    			}
    		}
    	} catch (final Exception e) {
			// ignore
    	}
//		Log.i(TAG, "Path of sd card external: " + externalpath);
//		Log.i(TAG, "Path of internal memory: " + internalpath);
    	return externalpath;
    }

	/**
	 * ストレージの情報を取得
	 * @param context
	 * @param type
	 * @return
	 * @throws IOException
	 */
	@SuppressLint({"UsableSpace"})
	@NonNull
	public static StorageInfo getStorageInfo(
		@NonNull final Context context,
		@NonNull final String type) throws IOException {

		try {
			// 外部保存領域が書き込み可能な場合
			// 外部ストレージへのパーミッションがないとnullが返ってくる
			final File dir = FileUtils.getCaptureDir(context, type);
//					Log.i(TAG, "checkFreeSpace:dir=" + dir);
			if (dir != null) {
				final long freeSpace = dir.canWrite() ? dir.getUsableSpace() : 0L;
				return new StorageInfo(dir.getTotalSpace(), freeSpace);
			}
		} catch (final Exception e) {
			Log.w("getStorageInfo:", e);
		}
		throw new IOException();
	}

	/**
	 * 全容量と空き容量を返す
	 * @param context
	 * @param dir
	 * @return
	 * @throws IOException
	 */
	@SuppressLint({"NewApi", "UsableSpace"})
	@NonNull
	public static StorageInfo getStorageInfo(
		@NonNull final Context context,
		@NonNull final DocumentFile dir) throws IOException {

		try {
			final String path = UriHelper.getPath(context, dir.getUri());
			if (path != null) {
				// FIXME もしプライマリーストレージの場合はアクセス権無くても容量取得できるかも
				// FIXME StorageManagerを使うようにする？
				final File file = new File(path);
				if (file.isDirectory() && file.canRead()) {
					final long total = file.getTotalSpace();
					long free = file.getFreeSpace();
					if (free < file.getUsableSpace()) {
						free = file.getUsableSpace();
					}
					return new StorageInfo(total, free);
				}
			}
		} catch (final Exception e) {
			// ignore
		}
		if (BuildCheck.isJellyBeanMR2()) {
			try {
				final String path = UriHelper.getPath(context, dir.getUri());
				final StatFs fs = new StatFs(path);
				return new StorageInfo(fs.getTotalBytes(), fs.getAvailableBytes());
			} catch (final Exception e) {
				// ignore
			}
		}
		throw new IOException();
	}
}
