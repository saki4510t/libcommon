package com.serenegiant.mediastore;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2020 saki t_saki@serenegiant.com
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

import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.util.Log;

import java.io.IOException;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static com.serenegiant.mediastore.MediaStoreUtils.*;

/**
 * MediaStoreの情報を保持するためのコンテナクラス
 */
public class MediaInfo implements Parcelable {
	private static final boolean DEBUG = false;	// FIXME 実働時はfalseにすること
	private static final String TAG = MediaInfo.class.getSimpleName();

	public static final Creator<MediaInfo> CREATOR = new Creator<MediaInfo>() {
		@Override
		public MediaInfo createFromParcel(final Parcel in) {
			return new MediaInfo(in);
		}

		@Override
		public MediaInfo[] newArray(final int size) {
			return new MediaInfo[size];
		}
	};

//--------------------------------------------------------------------------------
	public long id;
	public String data;
	public String title;
	public String mime;
	public String displayName;
	public int mediaType;
	public int width;
	public int height;
	public int orientation;

	/**
	 * デフォルトコンストラクタ
	 */
	public MediaInfo() {
	}

	/**
	 * コピーコンストラクタ
	 * @param src
	 */
	public MediaInfo(@NonNull final MediaInfo src) {
		id = src.id;
		data = src.data;
		title = src.title;
		mime = src.mime;
		displayName = src.displayName;
		mediaType = src.mediaType;
		width = src.width;
		height = src.height;
		orientation = src.orientation;
	}

	/**
	 * Parcelable用のコンストラクタ
	 * @param in
	 */
	protected MediaInfo(@NonNull final Parcel in) {
		id = in.readLong();
		data = in.readString();
		title = in.readString();
		mime = in.readString();
		displayName = in.readString();
		mediaType = in.readInt();
		width = in.readInt();
		height = in.readInt();
		orientation = in.readInt();
	}

	/**
	 * Cursorから値を読み込んで初期化するコンストラクタ
	 * @param cursor
	 */
	MediaInfo(@NonNull final Cursor cursor) {
		id = cursor.getLong(PROJ_INDEX_ID);
		data = cursor.getString(PROJ_INDEX_DATA);
		title = cursor.getString(PROJ_INDEX_TITLE);
		mime = cursor.getString(PROJ_INDEX_MIME_TYPE);
		displayName = cursor.getString(PROJ_INDEX_DISPLAY_NAME);
		mediaType = cursor.getInt(PROJ_INDEX_MEDIA_TYPE);
		try {
			width = cursor.getInt(PROJ_INDEX_WIDTH);
			height = cursor.getInt(PROJ_INDEX_HEIGHT);
		} catch (final Exception e) {
			// ignore
		}
	}

	MediaInfo loadFromCursor(@NonNull final Cursor cursor) {
		id = cursor.getLong(PROJ_INDEX_ID);
		data = cursor.getString(PROJ_INDEX_DATA);
		title = cursor.getString(PROJ_INDEX_TITLE);
		mime = cursor.getString(PROJ_INDEX_MIME_TYPE);
		displayName = cursor.getString(PROJ_INDEX_DISPLAY_NAME);
		mediaType = cursor.getInt(PROJ_INDEX_MEDIA_TYPE);
		try {
			width = cursor.getInt(PROJ_INDEX_WIDTH);
			height = cursor.getInt(PROJ_INDEX_HEIGHT);
			// Android 10以降でないとORIENTATIONが無い機種が多いのでここではセットしない
//				orientation = cursor.getInt(PROJ_INDEX_ORIENTATION);
		} catch (final Exception e) {
			// ignore
			if (DEBUG) Log.w(TAG, e);
		}
		return this;
	}

	/**
	 * セッター
	 * @param src
	 */
	public MediaInfo set(@NonNull final MediaInfo src) {
		id = src.id;
		data = src.data;
		title = src.title;
		mime = src.mime;
		displayName = src.displayName;
		mediaType = src.mediaType;
		width = src.width;
		height = src.height;
		orientation = src.orientation;
		return this;
	}

	@Nullable
	public Uri getUri() {
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

	/**
	 * このMediaInfoインスタンスが保持しているデータへアクセス可能(openできる)かどうか
	 * @param cr
	 * @return
	 */
	public boolean canRead(@NonNull final ContentResolver cr) {
		final Uri uri = getUri();
		if (uri != null) {
			try {
				final ParcelFileDescriptor pfd
					= cr.openFileDescriptor(uri, "r");
				if (pfd != null) {
					pfd.close();
					return true;
				}
			} catch (final IOException e) {
				if (DEBUG) Log.w(TAG, e);
			}
		}
		return false;
	}

	@NonNull
	@Override
	public String toString() {
		return String.format(Locale.US,
			"MediaInfo(id=%d,title=%s,displayName=%s,mediaType=%s,orientation=%d,mime=%s,data=%s)",
			id, title, displayName, mediaType(mediaType), orientation, mime, data);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(@NonNull final Parcel dest, final int flags) {
		dest.writeLong(id);
		dest.writeString(data);
		dest.writeString(title);
		dest.writeString(mime);
		dest.writeString(displayName);
		dest.writeInt(mediaType);
		dest.writeInt(width);
		dest.writeInt(height);
		dest.writeInt(orientation);
	}

//--------------------------------------------------------------------------------
	private static String mediaType(final int mediaType) {
		switch (mediaType) {
		case MediaStore.Files.FileColumns.MEDIA_TYPE_NONE:
			return "none";
		case MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE:
			return "image";
		case MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO:
			return "audio";
		case MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO:
			return "video";
		case MediaStore.Files.FileColumns.MEDIA_TYPE_PLAYLIST:
			return "playlist";
		default:
			return String.format(Locale.US, "unknown:%d", mediaType);
		}
	}
}
