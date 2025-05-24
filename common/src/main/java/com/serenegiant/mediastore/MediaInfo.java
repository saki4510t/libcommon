package com.serenegiant.mediastore;
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

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.util.Log;

import com.serenegiant.system.BuildCheck;

import java.io.IOException;
import java.util.Locale;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * MediaStoreの情報を保持するためのコンテナクラス
 */
public class MediaInfo implements Parcelable {
	private static final boolean DEBUG = false;	// 実働時はfalseにすること
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
	public long dateTaken;
	public long dateModified;

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
		dateTaken = src.dateTaken;
		dateModified = src.dateModified;
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
		dateTaken = in.readLong();
		dateModified = in.readLong();
	}

	/**
	 * MediaStore.Filesのクエリ結果のCursorから値を読み込んで初期化するコンストラクタ
	 * @param cursor
	 */
	MediaInfo(@NonNull final Cursor cursor) {
		this(cursor, cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)));
	}

	/**
	 * MediaStore.ImagesまたはMediaStore.Videoのクエリ結果のCursorから
	 * 値を読み込んで初期化するコンストラクタ
	 * @param cursor
	 */
	MediaInfo(@NonNull final Cursor cursor, final int mediaType) {
		loadFromCursor(cursor, mediaType);
	}

	/**
	 * MediaStore.Filesのクエリ結果のCursorからの読み込み用
	 * @param cursor
	 * @return
	 */
	MediaInfo loadFromCursor(@NonNull final Cursor cursor) {
		return loadFromCursor(cursor, cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)));
	}

	/**
	 * MediaStore.ImagesまたはMediaStore.Videoのクエリ結果のCursorからの読み込み用
	 * @param cursor
	 * @param mediaType
	 * @return
	 */
	MediaInfo loadFromCursor(@NonNull final Cursor cursor, final int mediaType) {
		final int idColNum = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID);
		final int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
		final int titleColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.TITLE);
		final int mimeTypeColNum = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE);
		final int mediaTypeColumnIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE);
		final int displayNameIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME);

		final int orientationColNum = cursor.getColumnIndex(MediaStore.MediaColumns.ORIENTATION);
		final int widthColumn = cursor.getColumnIndex(MediaStore.MediaColumns.WIDTH);
		final int heightColumn = cursor.getColumnIndex(MediaStore.MediaColumns.HEIGHT);
		final int dateTakenColNum = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_TAKEN);
		final int dateModifiedColNum = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED);

		id = cursor.getLong(idColNum);
		data = cursor.getString(dataColumn);
		title = cursor.getString(titleColumn);
		mime = cursor.getString(mimeTypeColNum);
		displayName = cursor.getString(displayNameIndex);

		this.mediaType = mediaType;
		try {
			if (widthColumn >= 0) {
				width = cursor.getInt(widthColumn);
			} else {
				width = 0;
			}
		} catch (final Exception e) {
			// ignore
		}
		try {
			if (heightColumn >= 0) {
				height = cursor.getInt(heightColumn);
			} else {
				height = 0;
			}
		} catch (final Exception e) {
			// ignore
		}
		try {
			if (BuildCheck.isAndroid10() && (orientationColNum >= 0)) {
				orientation = cursor.getInt(orientationColNum);
			} else {
				orientation = 0;
			}
		} catch (final Exception e) {
			// ignore
		}
		try {
			if (dateTakenColNum >= 0) {
				dateTaken = cursor.getLong(dateTakenColNum);
			} else {
				dateTaken = 0L;
			}
		} catch (final Exception e) {
			// ignore
		}
		try {
			if (dateModifiedColNum >= 0) {
				dateModified = cursor.getLong(dateModifiedColNum);
			} else {
				dateModified = 0L;
			}
		} catch (final Exception e) {
			// ignore
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
		dateTaken = src.dateTaken;
		dateModified = src.dateModified;

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
					pfd.checkError();
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
			", mediaType=" + mediaType + "(" + mediaType(mediaType) + ")" +
			", width=" + width +
			", height=" + height +
			", orientation=" + orientation +
			", dateTaken=" + dateTaken +
			", dateModified=" + dateModified +
			'}';
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) return true;
		if (!(o instanceof final MediaInfo mediaInfo)) return false;

		if (id != mediaInfo.id) return false;
		if (mediaType != mediaInfo.mediaType) return false;
		if (width != mediaInfo.width) return false;
		if (height != mediaInfo.height) return false;
		if (orientation != mediaInfo.orientation) return false;
		if (!Objects.equals(data, mediaInfo.data)) return false;
		if (!Objects.equals(title, mediaInfo.title)) return false;
		if (!Objects.equals(mime, mediaInfo.mime)) return false;
		if (dateTaken != mediaInfo.dateTaken) return false;
		if (dateModified != mediaInfo.dateModified) return false;
		return Objects.equals(displayName, mediaInfo.displayName);
	}

	@Override
	public int hashCode() {
		int result = Long.hashCode(id);
		result = 31 * result + (data != null ? data.hashCode() : 0);
		result = 31 * result + (title != null ? title.hashCode() : 0);
		result = 31 * result + (mime != null ? mime.hashCode() : 0);
		result = 31 * result + (displayName != null ? displayName.hashCode() : 0);
		result = 31 * result + mediaType;
		result = 31 * result + width;
		result = 31 * result + height;
		result = 31 * result + orientation;
		result = 31 * result + Long.hashCode(dateTaken);
		result = 31 * result + Long.hashCode(dateModified);
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
		dest.writeLong(dateTaken);
		dest.writeLong(dateModified);
	}

//--------------------------------------------------------------------------------
	private static String mediaType(final int mediaType) {
		return switch (mediaType) {
			case MediaStore.Files.FileColumns.MEDIA_TYPE_NONE -> "none";
			case MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE -> "image";
			case MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO -> "audio";
			case MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO -> "video";
			case MediaStore.Files.FileColumns.MEDIA_TYPE_PLAYLIST -> "playlist";
			case MediaStore.Files.FileColumns.MEDIA_TYPE_SUBTITLE -> "subtitle";
			case MediaStore.Files.FileColumns.MEDIA_TYPE_DOCUMENT -> "document";
			default -> String.format(Locale.US, "unknown:%d", mediaType);
		};
	}
}
