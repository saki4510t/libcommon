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

import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.MediaStore;
import android.util.Log;

import com.serenegiant.graphics.BitmapHelper;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.collection.LruCache;

public class ThumbnailCache {
	private static final boolean DEBUG = false;	// FIXME 実働時はfalseにすること
	private static final String TAG = ThumbnailCache.class.getSimpleName();

	// for thumbnail cache(in memory)
	// rate of memory usage for cache, 'CACHE_RATE = 8' means use 1/8 of available memory for image cache
	private static final int CACHE_RATE = 8;
	private static LruCache<String, Bitmap> sThumbnailCache;

	private static void prepareThumbnailCache(@NonNull final Context context) {
		if (sThumbnailCache == null) {
			final int memClass = ((ActivityManager)context
				.getSystemService(Context.ACTIVITY_SERVICE))
				.getMemoryClass();
			// use 1/CACHE_RATE of available memory as memory cache
			final int cacheSize = (1024 * 1024 * memClass) / CACHE_RATE;	// [MB] => [bytes]
			sThumbnailCache = new LruCache<String, Bitmap>(cacheSize) {
				@Override
				protected int sizeOf(@NonNull String key, @NonNull Bitmap bitmap) {
					// control memory usage instead of bitmap counts
					return bitmap.getRowBytes() * bitmap.getHeight();	// [bytes]
				}
			};
		}
	}

	private static String getKey(final long hashCode, final long id) {
		return String.format(Locale.US, "%d_%d", hashCode, id);
	}

	/**
	 * コンストラクタ
	 * @param context
	 */
	public ThumbnailCache(@NonNull final Context context) {
		prepareThumbnailCache(context);
	}

	/**
	 * サムネイルキャッシュをクリアする
	 */
	public void clearThumbnailCache() {
		sThumbnailCache.evictAll();
	}

	/**
	 * 静止画のサムネイルを取得する
	 * 可能であればキャッシュから取得する
	 * @param cr
	 * @param hashCode
	 * @param id
	 * @param requestWidth
	 * @param requestHeight
	 * @return
	 * @throws IOException
	 */
	public Bitmap getImageThumbnail(@NonNull final ContentResolver cr,
		final long hashCode, final long id,
		final int requestWidth, final int requestHeight)
			throws IOException {

		// try to get from internal thumbnail cache(in memory), this may be redundant
		final String key = getKey(hashCode, id);
		Bitmap result = sThumbnailCache.get(key);
		if (result == null) {
			if ((requestWidth <= 0) || (requestHeight <= 0)) {
				result = BitmapHelper.asBitmap(cr, id, requestWidth, requestHeight);
			} else {
				BitmapFactory.Options options = null;
				int kind = MediaStore.Images.Thumbnails.MICRO_KIND;
				if ((requestWidth > 96) || (requestHeight > 96) || (requestWidth * requestHeight > 128 * 128))
					kind = MediaStore.Images.Thumbnails.MINI_KIND;
				try {
					result = MediaStore.Images.Thumbnails.getThumbnail(cr, id, kind, options);
				} catch (final Exception e) {
					if (DEBUG) Log.w(TAG, e);
				}
			}
			if (result != null) {
				if (DEBUG) Log.v(TAG, String.format("getImageThumbnail:id=%d(%d,%d)",
					id, result.getWidth(), result.getHeight()));
				// add to internal thumbnail cache(in memory)
				sThumbnailCache.put(key, result);
			}

		}
		return result;
	}

	/**
	 * 動画のサムネイルを取得する
	 * 可能であればキャッシュから取得する
	 * @param cr
	 * @param hashCode
	 * @param id
	 * @param requestWidth
	 * @param requestHeight
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public Bitmap getVideoThumbnail(@NonNull final ContentResolver cr,
		final long hashCode, final long id,
		final int requestWidth, final int requestHeight)
			throws FileNotFoundException, IOException {

		// try to get from internal thumbnail cache(in memory), this may be redundant
		final String key = getKey(hashCode, id);
		Bitmap result = sThumbnailCache.get(key);
		if (result == null) {
			BitmapFactory.Options options = null;
			int kind = MediaStore.Video.Thumbnails.MICRO_KIND;
			if ((requestWidth > 96) || (requestHeight > 96) || (requestWidth * requestHeight > 128 * 128))
				kind = MediaStore.Video.Thumbnails.MINI_KIND;
			try {
				result = MediaStore.Video.Thumbnails.getThumbnail(cr, id, kind, options);
			} catch (final Exception e) {
				if (DEBUG) Log.w(TAG, e);
			}
			if (result != null) {
				if (DEBUG) Log.v(TAG, String.format("getVideoThumbnail:id=%d(%d,%d)",
					id, result.getWidth(), result.getHeight()));
				// add to internal thumbnail cache(in memory)
				sThumbnailCache.put(key, result);
			} else {
				Log.w(TAG, "failed to get video thumbnail ofr id=" + id);
			}

		}
		return result;
	}
}
