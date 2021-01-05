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

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Environment;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

import android.text.TextUtils;
import android.util.Log;

import com.serenegiant.system.SAFUtils;

public class FileUtils {
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
	 * @param ext 拡張子, .mp4 .png または .jpeg/.jpeg, .webp等
	 * @param saveTreeId 0: SAFを使わない, それ以外: SAFのツリーIDとみなして処理を試みる
	 * @return 書き込み出来なければnullを返す
	 */
	@Nullable
    public static final File getCaptureFile(@NonNull final Context context,
    	final String type, final String ext, final int saveTreeId) {

    	return getCaptureFile(context, type, null, ext, saveTreeId);
    }

	/**
	 * キャプチャ用のFileインスタンスを生成
	 * FIXME アクセスできないときはnullを返す代わりにIOExceptionを投げるように変更する
	 * @param context
	 * @param type Environment.DIRECTORY_MOVIES / Environment.DIRECTORY_DCIM
	 * @param prefix
	 * @param ext 拡張子, .mp4 .png または .jpeg/.jpeg, .webp等
	 * @param saveTreeId 0: SAFを使わない, それ以外: SAFのツリーIDとみなして処理を試みる
	 * @return
	 */
	@SuppressWarnings("ResultOfMethodCallIgnored")
	@Nullable
	public static final File getCaptureFile(@NonNull final Context context,
		final String type, final String prefix, final String ext, final int saveTreeId) {

	// 保存先のファイル名を生成
		File result = null;
		final String file_name = (TextUtils.isEmpty(prefix) ? getDateTimeString() : prefix + getDateTimeString()) + ext;
		if ((saveTreeId > 0) && SAFUtils.hasPermission(context, saveTreeId)) {
//			result = SAFUtils.createStorageFile(context, saveTreeId, "*/*", file_name);
//			result = SAFUtils.createStorageDir(context, saveTreeId);
			try {
				final DocumentFile dir = SAFUtils.getDir(context, saveTreeId, null);
				final String pathString = UriHelper.getPath(context, dir.getUri());
				if (!TextUtils.isEmpty(pathString)) {
					result = new File(pathString);
				}
			} catch (final IOException e) {
				Log.w(TAG, e);
			}
			if ((result == null) || !result.canWrite()) {
				Log.w(TAG, "なんでか書き込めん");
				result = null;
			}
			if (result != null) {
				result = new File(result, getDirName());
			}
		}
		if (result == null) {
			// プライマリ外部ストレージへフォールバックする(WRITE_EXTERNAL_STORAGEがないと失敗する)
			final File dir = getCaptureDir(context, type, 0);
			if (dir != null) {
				dir.mkdirs();
				if (dir.canWrite()) {
					result = dir;
				}
			}
		}
		if (result != null) {
			result = new File(result, file_name);
		}
//		Log.i(TAG, "getCaptureFile:result=" + result);
		return result;
	}

	/**
	 * キャプチャ用のディレクトリを示すFileインスタンスを取得する
	 * FIXME アクセスできないときはnullを返す代わりにIOExceptionを投げるように変更する
	 * @param context
	 * @param type
	 * @param saveTreeId 0: SAFを使わない, それ以外: SAFのツリーIDとみなして処理を試みる
	 * @return
	 */
	@SuppressWarnings("ResultOfMethodCallIgnored")
	@SuppressLint("NewApi")
	@Nullable
	public static final File getCaptureDir(@NonNull final Context context,
		final String type, final int saveTreeId) {

//		Log.i(TAG, "getCaptureDir:saveTreeId=" + saveTreeId + ", context=" + context);
		File result = null;
		if ((saveTreeId != 0) && SAFUtils.hasPermission(context, saveTreeId)) {
//			result = SAFUtils.createStorageDir(context, saveTreeId);
			try {
				final DocumentFile dir = SAFUtils.getDir(context, saveTreeId, null);
				final String pathString = UriHelper.getPath(context, dir.getUri());
				if (!TextUtils.isEmpty(pathString)) {
					result = new File(pathString);
				}
			} catch (final IOException e) {
				Log.w(TAG, e);
			}
//			Log.i(TAG, "getCaptureDir:createStorageDir=" + result);
		}
		final File dir = result != null
			? new File(result, getDirName())
			: new File(Environment.getExternalStoragePublicDirectory(type), getDirName());
		dir.mkdirs();	// Nexus5だとパスが全部存在しないと値がちゃんと返ってこないのでパスを生成
//		Log.i(TAG, "getCaptureDir:" + result);
        if (dir.canWrite()) {
        	return dir;
        }
		return null;
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


//--------------------------------------------------------------------------------
	// 外部ストレージの空き容量の制限(1分に10MBとみなす。実際は7〜8MB)
    public static float FREE_RATIO = 0.03f;					// 空き領域が3%より大きいならOK
    public static float FREE_SIZE_OFFSET = 20 * 1024 * 1024;
    public static float FREE_SIZE = 300 * 1024 * 1024;		// 空き領域が300MB以上ならOK
    public static float FREE_SIZE_MINUTE = 40 * 1024 * 1024;	// 1分当たりの動画容量(5Mbpsで38MBぐらいなので)
	public static long CHECK_INTERVAL = 45 * 1000L;	// 空き容量,EOSのチェクする間隔[ミリ秒](=45秒)
	
	/**
	 * プライマリー外部ストレージの空き容量のチェック
  * プライマリー外部ストレージの空き容量がFREE_RATIO(5%)以上かつFREE_SIZE(20MB)以上ならtrueを返す
	 * @param context
	 * @param max_duration
	 * @param startTime
	 * @param saveTreeId 0: SAFを使わない, それ以外: SAFのツリーIDとみなして処理を試みる
	 * @return 使用可能であればtrue
	 */
    public static final boolean checkFreeSpace(final Context context,
    	final long max_duration, final long startTime, final int saveTreeId) {
//		Log.i(TAG, "checkFreeSpace:save_tree_id=" + save_tree_id + ", context=" + context);
    	if (context == null) return false;
    	return checkFreeSpace(context, FREE_RATIO,
    		max_duration > 0	// 最大録画時間が設定されている時
        	? (max_duration - (System.currentTimeMillis() - startTime)) / 60000.f
        		* FREE_SIZE_MINUTE + FREE_SIZE_OFFSET
        	: FREE_SIZE, saveTreeId);
    }

	/**
	 * プライマリー外部ストレージの空き容量のチェック
	 * @param context
	 * @param ratio 空き容量の割合(0-1]
	 * @param minFree 最小空き容量[バイト]
	 * @param saveTreeId 0: SAFを使わない, それ以外: SAFのツリーIDとみなして処理を試みる
	 * @return 使用可能であればtrue
	 */
    public static final boolean checkFreeSpace(final Context context,
    	final float ratio, final float minFree, final int saveTreeId) {

//    	if (DEBUG) Log.v(TAG, String.format("checkFreeSpace:ratio=%f,min=%f", ratio, minFree));
//		Log.i(TAG, "checkFreeSpace:context=" + context + ", save_tree_id=" + save_tree_id);
    	if (context == null) return false;
    	boolean result = false;
		try {
//			Log.v("checkFreeSpace", "getExternalStorageState=" + Environment.getExternalStorageState());
//			final String state = Environment.getExternalStorageState();
//			if (Environment.MEDIA_MOUNTED.equals(state) ||
//				!Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            	// 外部保存領域が書き込み可能な場合
				// 外部ストレージへのパーミッションがないとnullが返ってくる
				final File dir = getCaptureDir(context, Environment.DIRECTORY_DCIM, saveTreeId);
//				Log.i(TAG, "checkFreeSpace:dir=" + dir);
				if (dir != null) {
					final float freeSpace = dir.canWrite() ? dir.getUsableSpace() : 0;
					if (dir.getTotalSpace() > 0) {
						result = (freeSpace / dir.getTotalSpace() > ratio) || (freeSpace > minFree);
					}
				}
//				Log.v("checkFreeSpace:", "freeSpace=" + freeSpace);
//				Log.v("checkFreeSpace:", "getTotalSpace=" + dir.getTotalSpace());
//				Log.v("checkFreeSpace:", "result=" + result);
//			}
		} catch (final Exception e) {
			Log.w("checkFreeSpace:", e);
		}
        return result;
    }

	/**
	 * 使用可能な空き容量を取得
	 * @param context
	 * @param type Environment.DIRECTORY_DCIM等
	 * @param saveTreeId 0: SAFを使わない, それ以外: SAFのツリーIDとみなして処理を試みる
	 * @return
	 */
	public static final long getAvailableFreeSpace(final Context context,
		final String type, final int saveTreeId) {

		long result = 0;
		if (context != null) {
			final File dir = getCaptureDir(context, type, saveTreeId);
			if (dir != null) {
				result = dir.canWrite() ? dir.getUsableSpace() : 0;
			}
		}
		return result;
	}

	/**
	 * 使用可能な空き容量の割合を取得
	 * @param context
	 * @param type Environment.DIRECTORY_DCIM等
	 * @param saveTreeId 0: SAFを使わない, それ以外: SAFのツリーIDとみなして処理を試みる
	 * @return
	 */
	public static final float getFreeRatio(final Context context,
		final String type, final int saveTreeId) {

		if (context != null) {
			final File dir = getCaptureDir(context, type, saveTreeId);
			if (dir != null) {
				final float freeSpace = dir.canWrite() ? dir.getUsableSpace() : 0;
				if (dir.getTotalSpace() > 0) {
					return freeSpace / dir.getTotalSpace();
				}
			}
		}
		return 0;
	}

//--------------------------------------------------------------------------------
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
}
