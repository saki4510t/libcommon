package com.serenegiant.media;
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
import android.util.Log;

import com.serenegiant.system.BuildCheck;
import com.serenegiant.system.PermissionCheck;
import com.serenegiant.system.SAFUtils;
import com.serenegiant.utils.FileUtils;

import java.io.File;
import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

public class MediaFileUtils {
	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = MediaFileUtils.class.getSimpleName();

	private MediaFileUtils() {
		// インスtナンス化をエラーにするためのデフォルトコンストラクトをprivateに
	}

	/**
	 * キャプチャ用のディレクトリを取得、取得できなければnull
	 * こっちはSAF経由でアクセス可能な場合のみ指定場所を示すDocumentFileを返す
	 * @param context
	 * @param saveTreeId 0: SAFを使わない, それ以外: SAFのツリーIDとみなして処理を試みる
	 * @return
	 */
	@Nullable
	public static synchronized DocumentFile getSAFRecordingRoot(
		@NonNull final Context context,
		final int saveTreeId) {

		DocumentFile root = null;
		if (BuildCheck.isAPI21()) {
			if (SAFUtils.hasPermission(context, saveTreeId)) {
				try {
					root = SAFUtils.getDir(context, saveTreeId, FileUtils.DIR_NAME);
					if (!root.exists() || !root.canWrite()) {
						Log.d(TAG, "path will be wrong, will already be removed,"
							+ (root != null ? root.getUri() : null));
						root = null;
					}
				} catch (final IOException | IllegalStateException e) {
					root = null;
					Log.d(TAG, "path is wrong, will already be removed.", e);
				}
			}
			if (root == null) {
				if (DEBUG) Log.d(TAG, "getSAFRecordingRoot:保存先を取得できなかったので念のためにセカンダリーストレージアクセスのパーミッションも落としておく");
				SAFUtils.releasePersistableUriPermission(context, saveTreeId);
			}
		}
		return root;
	}

	/**
	 * キャプチャ用のディレクトリを取得、取得できなければnull
	 * SAF経由での録画ディレクトリ、またはSAF経由でアクセス出来ないとき＆外部ストレージへの書き込みパーミッションがあれば
	 * 外部ストレージの${type}ディレクトリを返す
	 * "${DIRECTORY_MOVIES}/${FileUtils.DIR_NAME}" directory on primary storage as default value
	 * if user change
	 * @param context
	 * @param type SAFではなく外部ストレージへ保存する場合のディレクトリタイプ, Environment.DIRECTORY_MOVIES / DIRECTORY_DCIM
	 * @param saveTreeId 0: SAFを使わない, それ以外: SAFのツリーIDとみなして処理を試みる
	 * @return
	 */
	@Nullable
	public static synchronized DocumentFile getRecordingRoot(
		@NonNull final Context context,
		@NonNull final String type,
		final int saveTreeId) {

		if (DEBUG) Log.v(TAG, "getRecordingRoot:type=" + type);
		// SAF経由での録画用ディレクトリ取得を試みる
		DocumentFile root = getSAFRecordingRoot(context, saveTreeId);
		if ((root == null) && PermissionCheck.hasWriteExternalStorage(context)) {
			// SAF経由で録画用ディレクトリを取得できなかったが外部ストレージのアクセスパーミッションがある時
			if (DEBUG) Log.d(TAG, "getRecordingRoot:アプリが外部ストレージへのアクセスパーミッションを保持していればパスの取得を試みる");
			final File captureDir
				= FileUtils.getCaptureDir(context, type);
			if ((captureDir != null) && captureDir.canWrite()) {
				root = DocumentFile.fromFile(captureDir);
				// こっちの場合は既にディレクトリ名としてFileUtils.DIR_NAMEが付加されてくるはずなのでここでは追加しない
			}
		}
		if (DEBUG) Log.v(TAG, "getRecordingRoot:root=" + root + ",uri=" + (root != null ? root.getUri() : ""));
		return root;
	}

	/**
	 * 静止画/動画保存用のDocumentFileを取得
	 * @param context
	 * @param saveTreeId 0: SAFを使わない, それ以外: SAFのツリーIDとみなして処理を試みる
	 * @param mime
	 * @param type SAFではなく外部ストレージへ保存する場合のディレクトリタイプ, Environment.DIRECTORY_MOVIES / DIRECTORY_DCIM
	 * @param ext 拡張子、ピリオドを含む(例：".jpeg", ".png", ".mp4")
	 * @return
	 * @throws IOException
	 */
	public static synchronized DocumentFile getRecordingFile(
		@NonNull final Context context,
		final int saveTreeId,
		@NonNull final String type, @Nullable final String mime,
		@NonNull final String ext) throws IOException {

		final DocumentFile root = getRecordingRoot(context, type, saveTreeId);
		if (root != null) {
			return SAFUtils.getFile(root, null,
				mime, FileUtils.getDateTimeString() + ext);
		} else {
			throw new IOException("Failed to get recording root dir");
		}
	}

	/**
	 * 静止画/動画保存用のDocumentFileを取得
	 * SAFUtils.getFileのシノニム
	 * @param root
	 * @param dirs
	 * @param mime
	 * @param fileNameWithExt
	 * @return
	 * @throws IOException
	 */
	@Deprecated
	@NonNull
	public static synchronized DocumentFile getRecordingFile(
		@NonNull final DocumentFile root, @Nullable final String dirs,
		@Nullable final String mime,
		@NonNull final String fileNameWithExt) throws IOException {

		return SAFUtils.getFile(root, dirs, mime, fileNameWithExt);
	}

}
