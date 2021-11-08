package com.serenegiant.mediastore;
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
import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import com.serenegiant.db.CursorHelper;
import com.serenegiant.utils.FileUtils;

import java.io.FileNotFoundException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

/**
 * MediaStoreへアクセスするためのヘルパークラス
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class MediaStoreUtils {
	private static final boolean DEBUG = false;	// FIXME 実働時はfalseにすること
	private static final String TAG = MediaStoreUtils.class.getSimpleName();

	private MediaStoreUtils() {
		// インスタンス化をエラーにするためにデフォルトコンストラクタをprivateに
	}

	public static final int MEDIA_ALL = 0;
	public static final int MEDIA_IMAGE = 1;
	public static final int MEDIA_VIDEO = 2;
	protected static final int MEDIA_TYPE_NUM = 3;

	protected static final String[] PROJ_MEDIA = {
		MediaStore.MediaColumns._ID,				// index=0  for Cursor, column number=1  in SQL statement
		MediaStore.MediaColumns.TITLE,				// index=1  for Cursor, column number=2  in SQL statement
		// MEDIA_TYPE_NONE, MEDIA_TYPE_IMAGE, MEDIA_TYPE_AUDIO, MEDIA_TYPE_VIDEO, MEDIA_TYPE_PLAYLIST
		MediaStore.MediaColumns.MIME_TYPE,			// index=3  for Cursor, column number=4  in SQL statement
		MediaStore.MediaColumns.DATA,				// index=4  for Cursor, column number=5  in SQL statement
		MediaStore.MediaColumns.DISPLAY_NAME,		// index=5  for Cursor, column number=6  in SQL statement
		MediaStore.MediaColumns.WIDTH,				// index=6  for Cursor, column number=7  in SQL statement
		MediaStore.MediaColumns.HEIGHT,				// index=7  for Cursor, column number=8  in SQL statement
		MediaStore.Files.FileColumns.MEDIA_TYPE,	// index=2  for Cursor, column number=3  in SQL statement
//		MediaStore.MediaColumns.DATE_MODIFIED,		// index=8  for Cursor, column number=9  in SQL statement
//		MediaStore.MediaColumns.DATE_ADDED,			// index=9  for Cursor, column number=10 in SQL statement
	};

	protected static final String[] PROJ_MEDIA_IMAGE = {
		MediaStore.MediaColumns._ID,				// index=0  for Cursor, column number=1  in SQL statement
		MediaStore.MediaColumns.TITLE,				// index=1  for Cursor, column number=2  in SQL statement
		// MEDIA_TYPE_NONE, MEDIA_TYPE_IMAGE, MEDIA_TYPE_AUDIO, MEDIA_TYPE_VIDEO, MEDIA_TYPE_PLAYLIST
		MediaStore.MediaColumns.MIME_TYPE,			// index=3  for Cursor, column number=4  in SQL statement
		MediaStore.MediaColumns.DATA,				// index=4  for Cursor, column number=5  in SQL statement
		MediaStore.MediaColumns.DISPLAY_NAME,		// index=5  for Cursor, column number=6  in SQL statement
		MediaStore.MediaColumns.WIDTH,				// index=6  for Cursor, column number=7  in SQL statement
		MediaStore.MediaColumns.HEIGHT,				// index=7  for Cursor, column number=8  in SQL statement
//		MediaStore.MediaColumns.DATE_MODIFIED,		// index=8  for Cursor, column number=9  in SQL statement
//		MediaStore.MediaColumns.DATE_ADDED,			// index=9  for Cursor, column number=10 in SQL statement
	};

	protected static final String[] PROJ_MEDIA_VIDEO = {
		MediaStore.MediaColumns._ID,				// index=0  for Cursor, column number=1  in SQL statement
		MediaStore.MediaColumns.TITLE,				// index=1  for Cursor, column number=2  in SQL statement
		// MEDIA_TYPE_NONE, MEDIA_TYPE_IMAGE, MEDIA_TYPE_AUDIO, MEDIA_TYPE_VIDEO, MEDIA_TYPE_PLAYLIST
		MediaStore.MediaColumns.MIME_TYPE,			// index=3  for Cursor, column number=4  in SQL statement
		MediaStore.MediaColumns.DATA,				// index=4  for Cursor, column number=5  in SQL statement
		MediaStore.MediaColumns.DISPLAY_NAME,		// index=5  for Cursor, column number=6  in SQL statement
		MediaStore.MediaColumns.WIDTH,				// index=6  for Cursor, column number=7  in SQL statement
		MediaStore.MediaColumns.HEIGHT,				// index=7  for Cursor, column number=8  in SQL statement
//		MediaStore.MediaColumns.DATE_MODIFIED,		// index=8  for Cursor, column number=9  in SQL statement
//		MediaStore.MediaColumns.DATE_ADDED,			// index=9  for Cursor, column number=10 in SQL statement
	};

	protected static final String SELECTION_MEDIA_ALL
		= MediaStore.Files.FileColumns.MEDIA_TYPE + "="
		+ MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
		+ " OR "
		+ MediaStore.Files.FileColumns.MEDIA_TYPE + "="
		+ MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;

	protected static final String SELECTION_MEDIA_IMAGE
		= MediaStore.Files.FileColumns.MEDIA_TYPE + "="
		+ MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;

	protected static final String SELECTION_MEDIA_VIDEO
		= MediaStore.Files.FileColumns.MEDIA_TYPE + "="
		+ MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;

	// MEDIA_ALL, MEDIA_IMAGE, MEDIA_VIDEOの順にすること
	protected static final String[] SELECTIONS = {SELECTION_MEDIA_ALL, SELECTION_MEDIA_IMAGE, SELECTION_MEDIA_VIDEO};

	// these values should be fit to PROJ_MEDIA
	protected static final int PROJ_INDEX_ID = 0;
	protected static final int PROJ_INDEX_TITLE = 1;
	protected static final int PROJ_INDEX_MIME_TYPE = 2;
	protected static final int PROJ_INDEX_DATA = 3;
	protected static final int PROJ_INDEX_DISPLAY_NAME = 4;
	protected static final int PROJ_INDEX_WIDTH = 5;
	protected static final int PROJ_INDEX_HEIGHT = 6;
	protected static final int PROJ_INDEX_MEDIA_TYPE = 7;
//	protected static final int PROJ_INDEX_DATE_MODIFIED = 8;
//	protected static final int PROJ_INDEX_DATE_ADDED = 9;

	@SuppressLint("InlinedApi")
	public static final Uri QUERY_URI_FILES
		= (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
			? MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
			: MediaStore.Files.getContentUri("external");

	@SuppressLint("InlinedApi")
	public static final Uri QUERY_URI_IMAGES
		= (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
			? MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
			: MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

	@SuppressLint("InlinedApi")
	public static final Uri QUERY_URI_VIDEO
		= (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
			? MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
			: MediaStore.Video.Media.EXTERNAL_CONTENT_URI;

	@SuppressLint("InlinedApi")
	public static final Uri QUERY_URI_AUDIO
		= (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
			? MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
			: MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

//--------------------------------------------------------------------------------
	/**
	 * 静止画・動画等をMediaStoreへ登録してアクセスするためのUriをDocumentFileでラップして返す
	 * ContentResolver.openFileDescriptorを使ってアクセスする
	 * Android10以降の場合はIS_PENDING=1をセットするので後で#updateContentUri呼び出しが必要
	 * @param context
	 * @param mimeType
	 * @param relativePath
	 * @param nameWithExt
	 * @param dataPath
	 * @return
	 */
	@TargetApi(Build.VERSION_CODES.KITKAT)
	public static DocumentFile getContentDocument(
		@NonNull final Context context,
		@Nullable final String mimeType,
		@Nullable final String relativePath,
		@NonNull final String nameWithExt,
		@Nullable final String dataPath) {

		// DocumentFile.fromSingleUriはAPI>=19
		return DocumentFile.fromSingleUri(context,
			getContentUri(context, mimeType, relativePath, nameWithExt, dataPath));
	}

//--------------------------------------------------------------------------------
	/**
	 * 静止画・動画等をMediaStoreへ登録してアクセスするためのUriを返す
	 * ContentResolver.openFileDescriptorを使ってアクセスする
	 * Android10以降の場合はIS_PENDING=1をセットするので後で#updateContentUri呼び出しが必要
	 * @param context
	 * @param mimeType
	 * @param relativePath ["DCIM", "Movies", "Pictures"]から始まる相対パスまたはnull
	 * @param nameWithExt
	 * @param dataPath
	 * @return
	 * @throws FileNotFoundException
	 */
	public static Uri getContentUri(
		@NonNull final Context context,
		@Nullable final String mimeType,
		@Nullable final String relativePath,
		@NonNull final String nameWithExt,
		@Nullable final String dataPath) {

		return getContentUri(context.getContentResolver(),
			mimeType, relativePath, nameWithExt, dataPath);
	}

	/**
	 * 静止画・動画等をMediaStoreへ登録してアクセスするためのUriを返す
	 * ContentResolver.openFileDescriptorを使ってアクセスする
	 * Android10以降の場合はIS_PENDING=1をセットするので後で#updateContentUri呼び出しが必要
	 * @param cr
	 * @param mimeType
	 * @param relativePath ["DCIM", "Movies", "Pictures"]から始まる相対パスまたはnull
	 * @param nameWithExt
	 * @param dataPath
	 * @return
	 * @throws FileNotFoundException
	 */
	public static Uri getContentUri(
		@NonNull final ContentResolver cr,
		@Nullable final String mimeType,
		@Nullable final String relativePath,
		@NonNull final String nameWithExt,
		@Nullable final String dataPath) {

		final ContentValues cv = new ContentValues();
		@NonNull
		String _mimeType = mimeType != null ? mimeType.toLowerCase() : "";
		@NonNull
		final String ext = FileUtils.getExt(nameWithExt).toLowerCase();

		if (DEBUG) Log.v(TAG, "getContentUri:" +
		 	"mimeType=" + _mimeType + ",nameWithExt=" + nameWithExt
		 	+ ",ext=" + ext + ",dataPath=" + dataPath);

		final Uri queryUri;
		if (_mimeType.startsWith("*/") && (dataPath != null)) {
			// ファイル, ファイルのときはDATAでパスを指定しないと例外生成してしまう
			cv.put(MediaStore.Files.FileColumns.DISPLAY_NAME, nameWithExt);
			cv.put(MediaStore.Files.FileColumns.MIME_TYPE, _mimeType);
			queryUri = QUERY_URI_FILES;
		} else if (_mimeType.startsWith("image/")
			|| ext.equalsIgnoreCase("png")
			|| ext.equalsIgnoreCase("jpg")
			|| ext.equalsIgnoreCase("jpeg")
			|| ext.equalsIgnoreCase("webp")) {

			// 静止画
			if (TextUtils.isEmpty(_mimeType)) {
				_mimeType = "image/" + (TextUtils.isEmpty(ext) ? "*" : ext);
			}
			cv.put(MediaStore.Images.Media.DISPLAY_NAME, nameWithExt);
			cv.put(MediaStore.Images.Media.MIME_TYPE, _mimeType);
			queryUri = QUERY_URI_IMAGES;
		} else if (_mimeType.startsWith("video/")
			|| ext.equalsIgnoreCase("mp4")
			|| ext.equalsIgnoreCase("3gp")
			|| ext.equalsIgnoreCase("h264")
			|| ext.equalsIgnoreCase("mjpeg")) {

			// 動画
			if (TextUtils.isEmpty(_mimeType)) {
				_mimeType = "video/" + (TextUtils.isEmpty(ext) ? "*" : ext);
			}
			cv.put(MediaStore.Video.Media.DISPLAY_NAME, nameWithExt);
			cv.put(MediaStore.Video.Media.MIME_TYPE, _mimeType);
			queryUri = QUERY_URI_VIDEO;
		} else if (_mimeType.startsWith("audio/")
			|| ext.equalsIgnoreCase("m4a")) {

			// 音声
			if (TextUtils.isEmpty(_mimeType)) {
				_mimeType = "audio/" + (TextUtils.isEmpty(ext) ? "*" : ext);
			}
			cv.put(MediaStore.Audio.Media.DISPLAY_NAME, nameWithExt);
			cv.put(MediaStore.Audio.Media.MIME_TYPE, _mimeType);
			queryUri = QUERY_URI_AUDIO;
		} else {
			throw new IllegalArgumentException("unknown mimeType/file type,"
				+ mimeType + ",name=" + nameWithExt);
		}

		if (dataPath != null) {
			cv.put(MediaStore.Images.Media.DATA, dataPath);
		}
		cv.put(MediaStore.MediaColumns.TITLE, FileUtils.removeFileExtension(nameWithExt));

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			if (!TextUtils.isEmpty(relativePath)) {
				cv.put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath);
			}
			cv.put(MediaStore.MediaColumns.IS_PENDING, 1);
		}

		if (DEBUG) Log.v(TAG, "getContentUri:cv=" + cv);
		if (DEBUG) Log.v(TAG, "getContentUri:queryUri=" + queryUri);

		final Uri result = cr.insert(queryUri, cv);	// これはIllegalArgumentExceptionを投げることがある
		if (DEBUG) {
			Log.v(TAG, "getContentUri:result=" + result);
			final Cursor cursor = cr.query(result, null, null, null, null);
			if (cursor != null) {
				try {
					CursorHelper.dumpCursor(cursor);
				} finally {
					cursor.close();
				}
			}
		}
		return result;
	}

	/**
	 * Android10以降のときに指定したuriに対してIS_PENDING=0でContextResolver#updateを呼び出す
	 * Android10未満の場合には何もしない
	 * @param context
	 * @param uri
	 */
	public static void updateContentUri(
		@NonNull final Context context, @NonNull final Uri uri) {

		updateContentUri(context.getContentResolver(), uri);
	}

	/**
	 * Android10以降のときに指定したuriに対してIS_PENDING=0でContextResolver#updateを呼び出す
	 * Android10未満の場合には何もしない
	 * @param cr
	 * @param uri
	 */
	public static void updateContentUri(
		@NonNull final ContentResolver cr, @NonNull final Uri uri) {

		if (DEBUG) Log.v(TAG, "updateContentUri:" + uri);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			final ContentValues cv = new ContentValues();
			cv.put(MediaStore.MediaColumns.IS_PENDING, 0);
			try {
				// 引き渡されたUriがIS_PENDING=1で生成されていないときは
				// UnsupportedOperationExceptionが投げられる
				cr.update(uri, cv, null, null);
			} catch (final Exception e) {
				Log.d(TAG, "updateContentUri:", e);
			}
		}
	}

	/**
	 * 静止画をMediaStoreへ登録
	 * @param context
	 * @param mime "image/jpeg"等
	 * @param path 絶対パス
	 */
	@Deprecated
	public static Uri registerImage(@NonNull final Context context,
		@NonNull final String mime, @NonNull final String path) {

		if (DEBUG) Log.v(TAG, "registerImage:" + path);
		final ContentValues cv = new ContentValues();
		cv.put(MediaStore.Images.Media.MIME_TYPE, mime);
		cv.put(MediaStore.MediaColumns.DATA, path);
		return context.getContentResolver().insert(
			MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv);
	}

	/**
	 * 動画をMediaStoreへ登録
	 * @param context
	 * @param mime
	 * @param path
	 */
	@Deprecated
	public static Uri registerVideo(@NonNull final Context context,
		@NonNull final String mime, @NonNull final String path) {

		if (DEBUG) Log.v(TAG, "registerVideo:" + path);
		final ContentValues cv = new ContentValues();
		cv.put(MediaStore.Video.Media.MIME_TYPE, mime);
		cv.put(MediaStore.MediaColumns.DATA, path);
		return context.getContentResolver().insert(
			MediaStore.Video.Media.EXTERNAL_CONTENT_URI, cv);
	}

	@Nullable
	public static Uri getUri(final int mediaType, final long id) {
		switch (mediaType) {
		case MediaStore.Files.FileColumns.MEDIA_TYPE_NONE:
			return ContentUris.withAppendedId(
				MediaStore.Files.getContentUri("external"), id);
		case MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE:
			return ContentUris.withAppendedId(
				MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
		case MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO:
			return ContentUris.withAppendedId(
				MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
		case MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO:
			return ContentUris.withAppendedId(
				MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id);
		case MediaStore.Files.FileColumns.MEDIA_TYPE_PLAYLIST:
		default:
			return null;
		}

	}
}
