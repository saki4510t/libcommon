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

import android.database.Cursor;
import android.provider.MediaStore;

import java.util.Locale;

import androidx.annotation.NonNull;

import static com.serenegiant.mediastore.MediaStoreUtils.*;

public class MediaInfo {
	public long id;
	public String data;
	public String title;
	public String mime;
	public String displayName;
	public int mediaType;
	public int width;
	public int height;

	protected MediaInfo loadFromCursor(final Cursor cursor) {
		if (cursor != null) {
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
		return this;
	}

	@NonNull
	@Override
	public String toString() {
		return String.format("MediaInfo(title=%s,displayName=%s,mediaType=%s,mime=%s,data=%s)",
			title, displayName, mediaType(mediaType), mime, data);
	}

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
