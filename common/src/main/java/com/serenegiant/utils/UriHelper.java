package com.serenegiant.utils;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2017 saki t_saki@serenegiant.com
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
import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;

public final class UriHelper {
//	private static final boolean DEBUG = false;	// FIXME 実働時はfalseにすること
	private static final String TAG = UriHelper.class.getSimpleName();
	/**
	 * UriからPathへの変換処理
	 * @param cr
	 * @param uri
	 * @return String パスが見つからなければnull
	 */
	public static String getAbsolutePath(final ContentResolver cr, final Uri uri) {
		String path = null;
		try {
			final String[] columns = { MediaStore.Images.Media.DATA };
			final Cursor cursor = cr.query(uri, columns, null, null, null);
			if (cursor != null)
			try {
				if (cursor.moveToFirst())
				path = cursor.getString(0);
			} finally {
				cursor.close();
			}
		} catch (final Exception e) {
//			if (DEBUG) Log.w(TAG, e);
		}
//		Log.v("UriHandler", "getAbsolutePath:" + path);
		return path;
	}

	/**
	 * Uriからローカルパスに変換できればpathを返す
	 * @param context The context.
	 * @param uri The Uri to query.
	 * @author paulburke
	 */
	@SuppressLint("NewApi")
	@TargetApi(Build.VERSION_CODES.KITKAT)
	public static String getPath(final Context context, final Uri uri) {
		Log.i(TAG, "getPath:uri=" + uri);

	    if (BuildCheck.isKitKat() && DocumentsContract.isDocumentUri(context, uri)) {
			// DocumentProvider
	    	Log.i(TAG, "getPath:isDocumentUri,getAuthority=" + uri.getAuthority());
	        // ExternalStorageProvider
	        if (isExternalStorageDocument(uri)) {
	            final String docId = DocumentsContract.getDocumentId(uri);
				Log.i(TAG, "getPath:isDocumentUri,docId=" + docId);
				if (BuildCheck.isLollipop()) {
					Log.i(TAG, "getPath:isDocumentUri,getTreeDocumentId=" + DocumentsContract.getTreeDocumentId(uri));
				}
				final String[] split = docId.split(":");
	            final String type = split[0];

				Log.i(TAG, "getPath:type=" + type);

	            if ("primary".equalsIgnoreCase(type)) {
	                return Environment.getExternalStorageDirectory() + "/" + split[1];
	            } else {
					// プライマリストレージ以外の時は前から順に探す
	            	final String primary = Environment.getExternalStorageDirectory().getAbsolutePath();
					Log.i(TAG, "getPath:primary=" + primary);
					final File[] dirs = context.getExternalFilesDirs(null);
					final int n = dirs != null ? dirs.length : 0;
					final StringBuilder sb = new StringBuilder();
					for (int i = 0; i < n; i++) {
						final File dir = dirs[i];
						Log.i(TAG, "getPath:" + i + ")dir=" + dir);
						if ((dir != null) && dir.getAbsolutePath().startsWith(primary)) continue;
						final String dir_path = dir.getAbsolutePath();
						final String[] dir_elements = dir_path.split("/");
						final int m = dir_elements != null ? dir_elements.length : 0;
						if ((m > 1) && "storage".equalsIgnoreCase(dir_elements[1])) {
							boolean found = false;
							sb.setLength(0);
							sb.append('/').append(dir_elements[1]);
							for (int j = 2; j < m; j++) {
								if ("Android".equalsIgnoreCase(dir_elements[j])) {
									found = true;
									break;
								}
								sb.append('/').append(dir_elements[j]);
							}
							if (found) {
								final File path = new File(new File(sb.toString()), split[1]);
								Log.i(TAG, "getPath:path=" + path);
								if (path.exists() && path.canWrite()) {
									return path.getAbsolutePath();
								}
							}
						}
					}
				}
	        } else if (isDownloadsDocument(uri)) {
				// DownloadsProvider
	            final String id = DocumentsContract.getDocumentId(uri);
	            final Uri contentUri = ContentUris.withAppendedId(
	                    Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

	            return getDataColumn(context, contentUri, null, null);
	        } else if (isMediaDocument(uri)) {
				// MediaProvider
				final String docId = DocumentsContract.getDocumentId(uri);
	            final String[] split = docId.split(":");
	            final String type = split[0];

	            Uri contentUri = null;
	            if ("image".equals(type)) {
	                contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
	            } else if ("video".equals(type)) {
	                contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
	            } else if ("audio".equals(type)) {
	                contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
	            }

	            final String selection = "_id=?";
	            final String[] selectionArgs = new String[] { split[1] };

	            return getDataColumn(context, contentUri, selection, selectionArgs);
	        }
	    } else if ("content".equalsIgnoreCase(uri.getScheme())) {
			// MediaStore (and general)
			if (isGooglePhotosUri(uri)) {
				return uri.getLastPathSegment();
			}
	        return getDataColumn(context, uri, null, null);
	    } else if ("file".equalsIgnoreCase(uri.getScheme())) {
			// File
	        return uri.getPath();
	    }

	    return null;
	}

	/**
	 * Get the value of the data column for this Uri. This is useful for
	 * MediaStore Uris, and other file-based ContentProviders.
	 *
	 * @param context The context.
	 * @param uri The Uri to query.
	 * @param selection (Optional) Filter used in the query.
	 * @param selectionArgs (Optional) Selection arguments used in the query.
	 * @return The value of the _data column, which is typically a file path.
	 */
	public static String getDataColumn(final Context context, final Uri uri, final String selection, final String[] selectionArgs) {

	    Cursor cursor = null;
	    final String column = "_data";
	    final String[] projection = { column };

	    try {
	        cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
	        if ((cursor != null) && cursor.moveToFirst()) {
	            final int column_index = cursor.getColumnIndexOrThrow(column);
	            return cursor.getString(column_index);
	        }
	    } finally {
	        if (cursor != null) {
	            cursor.close();
			}
	    }
	    return null;
	}

	/**
	 * @param uri The Uri to check.
	 * @return Whether the Uri authority is ExternalStorageProvider.
	 */
	public static boolean isExternalStorageDocument(final Uri uri) {
	    return "com.android.externalstorage.documents".equals(uri.getAuthority());
	}

	/**
	 * @param uri The Uri to check.
	 * @return Whether the Uri authority is DownloadsProvider.
	 */
	public static boolean isDownloadsDocument(final Uri uri) {
	    return "com.android.providers.downloads.documents".equals(uri.getAuthority());
	}

	/**
	 * @param uri The Uri to check.
	 * @return Whether the Uri authority is MediaProvider.
	 */
	public static boolean isMediaDocument(final Uri uri) {
	    return "com.android.providers.media.documents".equals(uri.getAuthority());
	}

	public static boolean isGooglePhotosUri(final Uri uri) {
		return "com.google.android.apps.photos.content".equals(uri.getAuthority());
	}

}

