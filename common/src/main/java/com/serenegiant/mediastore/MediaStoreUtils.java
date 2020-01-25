package com.serenegiant.mediastore;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2019 saki t_saki@serenegiant.com
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

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import androidx.annotation.NonNull;

import java.io.IOException;

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
		MediaStore.Files.FileColumns._ID,				// index=0 for Cursor, column number=1 in SQL statement
		MediaStore.Files.FileColumns.TITLE,				// index=1 for Cursor, column number=2 in SQL statement
		MediaStore.Files.FileColumns.MEDIA_TYPE,		// index=2 for Cursor, column number=2 in SQL statement
		// MEDIA_TYPE_NONE, MEDIA_TYPE_IMAGE, MEDIA_TYPE_AUDIO, MEDIA_TYPE_VIDEO, MEDIA_TYPE_PLAYLIST
		MediaStore.Files.FileColumns.MIME_TYPE,			// index=3 for Cursor, column number=2 in SQL statement
		MediaStore.Files.FileColumns.DATA,				// index=4 for Cursor, column number=2 in SQL statement
		MediaStore.Files.FileColumns.DISPLAY_NAME,		// index=5 for Cursor, column number=2 in SQL statement
		MediaStore.Files.FileColumns.WIDTH,
		MediaStore.Files.FileColumns.HEIGHT,
//		MediaStore.Files.FileColumns.DATE_MODIFIED,						// index=8 for Cursor, column number=2 in SQL statement
//		MediaStore.Files.FileColumns.DATE_ADDED,						// index=9 for Cursor, column number=2 in SQL statement
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
	protected static final int PROJ_INDEX_MEDIA_TYPE = 2;
	protected static final int PROJ_INDEX_MIME_TYPE = 3;
	protected static final int PROJ_INDEX_DATA = 4;
	protected static final int PROJ_INDEX_DISPLAY_NAME = 5;
	protected static final int PROJ_INDEX_WIDTH = 6;
	protected static final int PROJ_INDEX_HEIGHT = 7;
//	protected static final int PROJ_INDEX_DATE_MODIFIED = 8;
//	protected static final int PROJ_INDEX_DATE_ADDED = 9;

	protected static final Uri QUERY_URI = MediaStore.Files.getContentUri("external");

//--------------------------------------------------------------------------------
	protected static final Bitmap getImage(@NonNull final ContentResolver cr,
		final long id,
		final int requestWidth, final int requestHeight)
			throws IOException {

		Bitmap result = null;
		final ParcelFileDescriptor pfd = cr.openFileDescriptor(
			ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id), "r");
		if (pfd != null) {
			try {
				final BitmapFactory.Options options = new BitmapFactory.Options();
				// just decode to get image size
				options.inJustDecodeBounds = true;
				BitmapFactory.decodeFileDescriptor(pfd.getFileDescriptor(), null, options);
				// calculate sub-sampling
				options.inSampleSize = calcSampleSize(options, requestWidth, requestHeight);
				options.inJustDecodeBounds = false;
				result = BitmapFactory.decodeFileDescriptor(pfd.getFileDescriptor(), null, options);
			} finally {
				pfd.close();
			}
		}
		return result;
	}

	/**
	 * calculate maximum sub-sampling size that the image size is greater or equal to requested size
	 * @param options
	 * @param requestWidth
	 * @param requestHeight
	 * @return maximum sub-sampling size
	 */
	protected static final int calcSampleSize(@NonNull final BitmapFactory.Options options,
		final int requestWidth, final int requestHeight) {

		final int imageWidth = options.outWidth;
		final int imageHeight = options.outHeight;
		int reqWidth = requestWidth, reqHeight = requestHeight;
		if (requestWidth <= 0) {
			if (requestHeight > 0)
				reqWidth = (int)(imageWidth * requestHeight / (float)imageHeight);
			else
				reqWidth = imageWidth;
		}
		if (requestHeight <= 0) {
			if (requestWidth > 0)
				reqHeight = (int)(imageHeight * requestWidth / (float)imageHeight);
			else
				reqHeight = imageHeight;
		}
		int inSampleSize = 1;
		if ((imageHeight > reqHeight) || (imageWidth > reqWidth)) {
			if (imageWidth > imageHeight) {
				inSampleSize = Math.round(imageHeight / (float)reqHeight);	// Math.floor
			} else {
				inSampleSize = Math.round(imageWidth / (float)reqWidth);	// Math.floor
			}
		}
/*		if (DEBUG) Log.v(TAG, String.format("calcSampleSize:image=(%d,%d),request=(%d,%d),inSampleSize=%d",
				imageWidth, imageHeight, reqWidth, reqHeight, inSampleSize)); */
		return inSampleSize;
	}

//--------------------------------------------------------------------------------
	/**
	 * 静止画をMediaStoreへ登録
	 * @param context
	 * @param mime "image/jpeg"等
	 * @param path 絶対パス
	 */
	public static Uri registerImage(@NonNull final Context context,
		@NonNull final String mime, @NonNull final String path) {

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
	public static Uri registerVideo(@NonNull final Context context,
		@NonNull final String mime, @NonNull final String path) {

		final ContentValues cv = new ContentValues();
		cv.put(MediaStore.Video.Media.MIME_TYPE, mime);
		cv.put(MediaStore.MediaColumns.DATA, path);
		return context.getContentResolver().insert(
			MediaStore.Video.Media.EXTERNAL_CONTENT_URI, cv);
	}
}
