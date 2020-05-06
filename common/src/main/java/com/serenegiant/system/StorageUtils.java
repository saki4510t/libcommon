package com.serenegiant.system;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.serenegiant.utils.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
    	    e.printStackTrace();
    	}
//		Log.i(TAG, "Path of sd card external: " + externalpath);
//		Log.i(TAG, "Path of internal memory: " + internalpath);
    	return externalpath;
    }

	/**
	 * ストレージの情報を取得
	 * FIXME アクセスできないときはnullを返す代わりにIOExceptionを投げるように変更する?
	 * @param context
	 * @param type
	 * @param saveTreeId
	 * @return アクセスできなければnull
	 */
	@Nullable
	public static StorageInfo getStorageInfo(final Context context,
											 @NonNull final String type, final int saveTreeId) {

		if (context != null) {
			try {
				// 外部保存領域が書き込み可能な場合
				// 外部ストレージへのパーミッションがないとnullが返ってくる
				final File dir = FileUtils.getCaptureDir(context, type, saveTreeId);
//					Log.i(TAG, "checkFreeSpace:dir=" + dir);
				if (dir != null) {
					final float freeSpace = dir.canWrite() ? dir.getUsableSpace() : 0;
					return new StorageInfo(dir.getTotalSpace(), (long)freeSpace);
				}
			} catch (final Exception e) {
				Log.w("getStorageInfo:", e);
			}
		}
	    return null;
	}
}
