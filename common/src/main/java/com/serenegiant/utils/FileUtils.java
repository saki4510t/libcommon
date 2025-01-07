package com.serenegiant.utils;
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

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * ファイル操作・アクセス用のヘルパークラス
 */
public class FileUtils {
	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = FileUtils.class.getSimpleName();

	private FileUtils() {
		// インスタンス化をエラーにするためにデフォルトコンストラクタをprivateに
	}

    public static String DIR_NAME;

	/**
	 * デフォルトの保存先ディレクトリ名を取得
	 * @return
	 */
	@NonNull
	public static String getDirName() {
		return TextUtils.isEmpty(DIR_NAME)
			? "Serenegiant" : DIR_NAME;
	}
	
	/**
	 * キャプチャ用のFileインスタンスを生成
	 * FIXME アクセスできないときはnullを返す代わりにIOExceptionを投げるように変更する
	 * @param context
	 * @param type Environment.DIRECTORY_MOVIES / Environment.DIRECTORY_DCIM
	 * @param prefix
	 * @param ext 拡張子, .mp4 .png または .jpeg/.jpeg, .webp等
	 * @return
	 */
	@SuppressWarnings("ResultOfMethodCallIgnored")
	@Nullable
	public static final File getCaptureFile(@NonNull final Context context,
		final String type, final String prefix, final String ext) {

		if (DEBUG) Log.v(TAG, "getCaptureFile:");
		// 保存先のファイル名を生成
		File result = null;
		final String file_name = (TextUtils.isEmpty(prefix) ? getDateTimeString() : prefix + getDateTimeString()) + ext;
		if (DEBUG) Log.v(TAG, "プライマリ外部ストレージへフォールバックする(WRITE_EXTERNAL_STORAGEがないと失敗する)");
		final File dir = getCaptureDir(context, type);
		if (dir != null) {
			dir.mkdirs();
			if (dir.canWrite()) {
				result = dir;
			}
		}
		if (result != null) {
			result = new File(result, file_name);
		}
		if (DEBUG) Log.v(TAG, "getCaptureFile:result=" + result);
		return result;
	}

	/**
	 * 外部ストレージ上の保存ディレクトリを取得する
	 * 実際にアクセスするには外部ストレージアクセスのパーミッションが必要
	 * @param context
	 * @param type 外部ストレージのディレクトリタイプ,
	 * 	Environment.DIRECTORY_MUSIC, .DIRECTORY_PODCASTS,
	 *	.DIRECTORY_RINGTONES, .DIRECTORY_ALARMS,
	 *	.DIRECTORY_NOTIFICATIONS}, .DIRECTORY_PICTURES,
	 *	.DIRECTORY_MOVIES}, .DIRECTORY_DOWNLOADS,
	 *	.DIRECTORY_DCIM, .DIRECTORY_DOCUMENTS
	 * @return
	 */
	@SuppressWarnings("ResultOfMethodCallIgnored")
	@Nullable
	public static File getCaptureDir(@NonNull final Context context, @NonNull final String type) {
		if (DEBUG) Log.v(TAG, "getCaptureDir:type=" + type);
		if (!UriHelper.isStandardDirectory(type)) {
			throw new IllegalArgumentException(type + " is not a standard directory name!");
		}
		final String dirName = getDirName();
		File dir = Environment.getExternalStoragePublicDirectory(type);	// API>=8
		if (dir.canWrite()) {
			dir.mkdirs();
		}
		if (!TextUtils.isEmpty(dirName)) {
			final boolean canWrite = dir.canWrite();
			dir = new File(dir, dirName);
			if (canWrite) {
				dir.mkdirs();
			}
		}
		if (DEBUG) Log.v(TAG, "getCaptureDir:" + dir);
		return dir;
	}

	private static final SimpleDateFormat sDateTimeFormat
		= new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US);

    /**
     * 現在の日時を表す文字列を取得する
     * @return
     */
    public static final String getDateTimeString() {
    	final GregorianCalendar now = new GregorianCalendar();
    	return sDateTimeFormat.format(now.getTime());
    }

	private static final SimpleDateFormat sTimeFormat
		= new SimpleDateFormat("HH-mm-ss", Locale.US);

	/**
	 * 現在の時刻を表す文字列を取得する
	 *
	 * @return
	 */
	public static final String getTimeString() {
		final GregorianCalendar now = new GregorianCalendar();
		return sTimeFormat.format(now.getTime());
	}


//--------------------------------------------------------------------------------
	// 外部ストレージの空き容量の制限(1分に10MBとみなす。実際は7〜8MB)
    public static float FREE_RATIO = 0.03f;					// 空き領域が3%より大きいならOK
    public static float FREE_SIZE_OFFSET = 20 * 1024 * 1024;
    public static float FREE_SIZE = 300 * 1024 * 1024;		// 空き領域が300MB以上ならOK
    public static float FREE_SIZE_MINUTE = 40 * 1024 * 1024;	// 1分当たりの動画容量(5Mbpsで38MBぐらいなので)
	public static long CHECK_INTERVAL = 45 * 1000L;	// 空き容量,EOSのチェクする間隔[ミリ秒](=45秒)
	
	/**
	 * ファイル名末尾の拡張子を取り除く
	 * @param path
	 * @return
	 */
	public static final String removeFileExtension(final String path) {
		final int ix = !TextUtils.isEmpty(path) ? path.lastIndexOf(".") : -1;
		if (ix > 0) {
			return path.substring(0, ix);
		} else {
			return path;
		}
	}
	
	/**
	 * ファイル名末尾の拡張子を置換する
	 * pathがnullまたは空文字なら何もしない
	 * 拡張子がなければ新しい拡張子を付与する
	 * @param path
	 * @param newExt ドット付きの拡張子文字列
	 * @return
	 */
	public static final String replaceFileExtension(final String path,
		@NonNull final String newExt) {
		if (!TextUtils.isEmpty(path)) {
			final int ix = path.lastIndexOf(".");
			if (ix > 0) {
				return path.substring(0, ix) + newExt;
			} else {
				return path + newExt;
			}
		}
		return path;	// == null or empty string
	}

	/**
	 * 指定した名前から拡張子(ピリオドを含まない)を切り出す
	 * @param path
	 * @return
	 */
	@NonNull
	public static String getExt(@NonNull final String path) {
		if (!TextUtils.isEmpty(path)) {
			final int ix = path.lastIndexOf(".");
			if (ix > 0) {
				return path.substring(ix + 1);
			}
		}
		return "";
	}

	/**
	 * 拡張子を含むかどうかを取得
	 * @param path
	 * @return
	 */
	public static boolean hasExt(@NonNull final String path) {
		final String ext = getExt(path);
		return !TextUtils.isEmpty(ext);
	}

	/**
	 * pathがファイルの場合は拡張子を含まないファイル名文字列を取得
	 * pathがディレクトリの場合はディレクトリ名返す
	 * @param path
	 * @return
	 */
	public static String getDisplayName(@NonNull final String path) {
		final File file = new File(path);
		return removeFileExtension(file.getName());
	}

	/**
	 * 指定したディレクトリ・ファイルを再帰的に削除する
	 * @param path
	 */
	public static void deleteAll(@NonNull final File path) throws IOException {
		if (path.isDirectory()) {
			// pathがディレクトリの時...再帰的に削除する
			@Nullable
			final File[] files = path.listFiles();
			if (files == null) {
				// ここには来ないはずだけど
				throw new IllegalArgumentException("not a directory:" + path);
			} else {
				for (final File file : files) {
					// 再帰的に削除する
					deleteAll(file);
				}
			}
			if (!path.delete()) {
				throw new IOException("failed to delete directory:" + path);
			}
		} else {
			// pathがファイルの時
			if (!path.delete()) {
				throw new IOException("failed to delete file:" + path);
			}
		}
	}

}
