package com.serenegiant.system;

import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;

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
}
