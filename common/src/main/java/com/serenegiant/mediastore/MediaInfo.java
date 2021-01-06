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

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
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
	 * MediaStore.Filesのクエリ結果のCursorから値を読み込んで初期化するコンストラクタ
	 * @param cursor
	 */
	MediaInfo(@NonNull final Cursor cursor) {
		this(cursor, cursor.getInt(PROJ_INDEX_MEDIA_TYPE));
	}

	/**
	 * MediaStore.ImagesまたはMediaStore.Videoのクエリ結果のCursorから
	 * 値を読み込んで初期化するコンストラクタ
	 * @param cursor
	 */
	MediaInfo(@NonNull final Cursor cursor, final int mediaType) {
		id = cursor.getLong(PROJ_INDEX_ID);
		data = cursor.getString(PROJ_INDEX_DATA);
		title = cursor.getString(PROJ_INDEX_TITLE);
		mime = cursor.getString(PROJ_INDEX_MIME_TYPE);
		displayName = cursor.getString(PROJ_INDEX_DISPLAY_NAME);
		this.mediaType = mediaType;
		try {
			width = cursor.getInt(PROJ_INDEX_WIDTH);
			height = cursor.getInt(PROJ_INDEX_HEIGHT);
		} catch (final Exception e) {
			// ignore
		}
	}

	/**
	 * MediaStore.Filesのクエリ結果のCursorからの読み込み用
	 * @param cursor
	 * @return
	 */
	MediaInfo loadFromCursor(@NonNull final Cursor cursor) {
		return loadFromCursor(cursor, cursor.getInt(PROJ_INDEX_MEDIA_TYPE));
	}

	/**
	 * MediaStore.ImagesまたはMediaStore.Videoのクエリ結果のCursorからの読み込み用
	 * @param cursor
	 * @param mediaType
	 * @return
	 */
	MediaInfo loadFromCursor(@NonNull final Cursor cursor, final int mediaType) {
		id = cursor.getLong(PROJ_INDEX_ID);
		data = cursor.getString(PROJ_INDEX_DATA);
		title = cursor.getString(PROJ_INDEX_TITLE);
		mime = cursor.getString(PROJ_INDEX_MIME_TYPE);
		displayName = cursor.getString(PROJ_INDEX_DISPLAY_NAME);
		this.mediaType = mediaType;
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
		return MediaStoreUtils.getUri(mediaType, id);
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

	/**
	 * エラーチェック
	 * @param cr
	 * @throws IOException
	 */
	public void checkError(@NonNull final ContentResolver cr) throws IOException {
		final Uri uri = getUri();
		if (uri != null) {
			final ParcelFileDescriptor pfd
				= cr.openFileDescriptor(uri, "r");
			if (pfd != null) {
				try {
					if (pfd.getFd() == 0) {
						throw new IOException("Failed to get fd");
					}
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
						pfd.checkError();
					}
				} finally {
					pfd.close();
				}
			} else {
				throw new IOException("Failed to open uri");
			}
		} else {
			throw new IOException("Wrong uri");
		}
	}

	@NonNull
	@Override
	public String toString() {
		return "MediaInfo{" +
			"id=" + id +
			", data='" + data + '\'' +
			", title='" + title + '\'' +
			", mime='" + mime + '\'' +
			", displayName='" + displayName + '\'' +
			", mediaType=" + mediaType +
			", width=" + width +
			", height=" + height +
			", orientation=" + orientation +
			'}';
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) return true;
		if (!(o instanceof MediaInfo)) return false;

		final MediaInfo mediaInfo = (MediaInfo) o;

		if (id != mediaInfo.id) return false;
		if (mediaType != mediaInfo.mediaType) return false;
		if (width != mediaInfo.width) return false;
		if (height != mediaInfo.height) return false;
		if (orientation != mediaInfo.orientation) return false;
		if (data != null ? !data.equals(mediaInfo.data) : mediaInfo.data != null) return false;
		if (title != null ? !title.equals(mediaInfo.title) : mediaInfo.title != null) return false;
		if (mime != null ? !mime.equals(mediaInfo.mime) : mediaInfo.mime != null) return false;
		return displayName != null ? displayName.equals(mediaInfo.displayName) : mediaInfo.displayName == null;
	}

	@Override
	public int hashCode() {
		int result = (int) (id ^ (id >>> 32));
		result = 31 * result + (data != null ? data.hashCode() : 0);
		result = 31 * result + (title != null ? title.hashCode() : 0);
		result = 31 * result + (mime != null ? mime.hashCode() : 0);
		result = 31 * result + (displayName != null ? displayName.hashCode() : 0);
		result = 31 * result + mediaType;
		result = 31 * result + width;
		result = 31 * result + height;
		result = 31 * result + orientation;
		return result;
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
