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

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import com.serenegiant.system.BuildCheck;
import com.serenegiant.system.SAFUtils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

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
	 * @param ext 拡張子, .mp4 .png または .jpeg/.jpeg, .webp等
	 * @param saveTreeId 0: SAFを使わない, それ以外: SAFのツリーIDとみなして処理を試みる
	 * @return 書き込み出来なければnullを返す
	 */
	@Deprecated
	@SuppressWarnings("deprecation")
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
	 * @param ext 拡張子, .mp4 .png または .jpeg/.jpeg, .webp等
	 * @return 書き込み出来なければnullを返す
	 */
	@Nullable
    public static final File getCaptureFile(@NonNull final Context context,
    	final String type, final String ext) {

    	return getCaptureFile(context, type, null, ext);
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
	@Deprecated
	@SuppressWarnings("ResultOfMethodCallIgnored")
	@SuppressLint("NewApi")
	@Nullable
	public static final File getCaptureFile(@NonNull final Context context,
		final String type, final String prefix, final String ext, final int saveTreeId) {

		if (DEBUG) Log.v(TAG, "getCaptureFile:");
		// 保存先のファイル名を生成
		File result = null;
		final String file_name = (TextUtils.isEmpty(prefix) ? getDateTimeString() : prefix + getDateTimeString()) + ext;
		if (BuildCheck.isAPI21() &&
			(saveTreeId > 0) && SAFUtils.hasPermission(context, saveTreeId)) {	// API>=19

			if (DEBUG) Log.v(TAG, "getCaptureFile:SAFでアクセスする");
			try {
				final DocumentFile dir = SAFUtils.getDir(context, saveTreeId, null);	// API>=19
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
			if (DEBUG) Log.v(TAG, "プライマリ外部ストレージへフォールバックする(WRITE_EXTERNAL_STORAGEがないと失敗する)");
			final File dir = getCaptureDir(context, type);
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
		if (DEBUG) Log.v(TAG, "getCaptureFile:result=" + result);
		return result;
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
	 * キャプチャ用のディレクトリを示すFileインスタンスを取得する
	 * FIXME アクセスできないときはnullを返す代わりにIOExceptionを投げるように変更する
	 * @param context
	 * @param type　SAFではなく外部ストレージへ保存する場合のディレクトリタイプ,
	 * 	Environment.DIRECTORY_MUSIC, .DIRECTORY_PODCASTS,
	 *	.DIRECTORY_RINGTONES, .DIRECTORY_ALARMS,
	 *	.DIRECTORY_NOTIFICATIONS}, .DIRECTORY_PICTURES,
	 *	.DIRECTORY_MOVIES}, .DIRECTORY_DOWNLOADS,
	 *	.DIRECTORY_DCIM, .DIRECTORY_DOCUMENTS
	 * @param saveTreeId 0: SAFを使わない, それ以外: SAFのツリーIDとみなして処理を試みる
	 * @return
	 */
	@Deprecated
	@SuppressLint("NewApi")
	@SuppressWarnings("ResultOfMethodCallIgnored")
	@Nullable
	public static final File getCaptureDir(@NonNull final Context context,
		@NonNull final String type, final int saveTreeId) {

		if (DEBUG) Log.v(TAG, "getCaptureDir:saveTreeId=" + saveTreeId + ", context=" + context);
		File result = null;
		if (BuildCheck.isAPI21()
			&& (saveTreeId != 0) && SAFUtils.hasPermission(context, saveTreeId)) {	// API>=19

			try {
				final DocumentFile dir = SAFUtils.getDir(context, saveTreeId, null);	// API>=19
				final String pathString = UriHelper.getPath(context, dir.getUri());
				if (!TextUtils.isEmpty(pathString)) {
					result = new File(pathString);
				}
			} catch (final IOException e) {
				Log.w(TAG, e);
			}
			if (DEBUG) Log.v(TAG, "getCaptureDir:createStorageDir=" + result);
		}
		if (!UriHelper.isStandardDirectory(type)) {
			throw new IllegalArgumentException(type + " is not a standard directory name!");
		}
		final File dir = result != null
			? new File(result, getDirName())
			: new File(Environment.getExternalStoragePublicDirectory(type), getDirName());
		dir.mkdirs();	// Nexus5だとパスが全部存在しないと値がちゃんと返ってこないのでパスを生成
		if (DEBUG) Log.v(TAG, "getCaptureDir:" + result);
        if (dir.canWrite()) {
        	return dir;
        }
		return null;
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
	@SuppressWarnings("deprecation")
	@Deprecated
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
	 * Deprecated StorageUtils/StorageInfoを使う
	 * @param context
	 * @param ratio 空き容量の割合(0-1]
	 * @param minFree 最小空き容量[バイト]
	 * @param saveTreeId 0: SAFを使わない, それ以外: SAFのツリーIDとみなして処理を試みる
	 * @return 使用可能であればtrue
	 */
	@SuppressWarnings("deprecation")
	@Deprecated
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
					@SuppressLint("UsableSpace")
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
	@Deprecated
	@SuppressLint("UsableSpace")
	@SuppressWarnings("deprecation")
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
	@Deprecated
	@SuppressWarnings("deprecation")
	public static final float getFreeRatio(final Context context,
		final String type, final int saveTreeId) {

		if (context != null) {
			final File dir = getCaptureDir(context, type, saveTreeId);
			if (dir != null) {
				@SuppressLint("UsableSpace")
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
			} else if (files.length > 0) {
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
