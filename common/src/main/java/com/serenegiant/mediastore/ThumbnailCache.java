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

import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;

import com.serenegiant.common.BuildConfig;
import com.serenegiant.graphics.BitmapHelper;
import com.serenegiant.io.DiskLruCache;
import com.serenegiant.system.ContextUtils;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.LruCache;

/**
 * サムネイルキャッシュ
 * メモリーキャッシュとディスクキャッシュの2段構成
 */
public class ThumbnailCache {
	private static final boolean DEBUG = false;	// FIXME 実働時はfalseにすること
	private static final String TAG = ThumbnailCache.class.getSimpleName();

	private static final int DISK_CACHE_SIZE = 1024 * 1024 * 10; // 10MB
	private static final String DISK_CACHE_SUBDIR = ".thumbnailCache";
	private static final int DISK_CACHE_INDEX = 0;

	private static final Object sSync = new Object();
	/**
	 * for thumbnail cache(in memory)
	 * rate of memory usage for cache, 'CACHE_RATE = 8' means
	 * use 1/8 of available memory for image cache
	 */
	private static final int CACHE_RATE = 8;
	private static LruCache<String, Bitmap> sThumbnailCache;
	private static int sMaxDiskCacheBytes = DISK_CACHE_SIZE;
	@Nullable
	private static DiskLruCache sDiskLruCache;
	private static int sCacheSize;

	/**
	 * 初期化が必要であればサムネイルキャッシュを初期化する
	 * @param context
	 * @param maxDiskCacheBytes
	 */
	private static void prepareThumbnailCache(
		@NonNull final Context context,
		final int maxDiskCacheBytes) {

		synchronized (sSync) {
			if ((sThumbnailCache == null) || (sMaxDiskCacheBytes != maxDiskCacheBytes)) {
				if (DEBUG) Log.v(TAG, "prepareThumbnailCache:");
				sMaxDiskCacheBytes = maxDiskCacheBytes;
				if (sMaxDiskCacheBytes <= 0) {
					sMaxDiskCacheBytes = DISK_CACHE_SIZE;
				}
				if (sThumbnailCache != null) {
					sThumbnailCache.evictAll();
				}
				if ((sDiskLruCache != null) && !sDiskLruCache.isClosed()) {
					try {
						sDiskLruCache.close();
					} catch (final IOException e) {
						if (DEBUG) Log.w(TAG, e);
					}
				}
				final int memClass =
					ContextUtils.requireSystemService(context, ActivityManager.class)
					.getMemoryClass();
				// use 1/CACHE_RATE of available memory as memory cache
				sCacheSize = (1024 * 1024 * memClass) / CACHE_RATE;	// [MB] => [bytes]
				sThumbnailCache = new LruCache<String, Bitmap>(sCacheSize) {
					@Override
					protected int sizeOf(@NonNull String key, @NonNull Bitmap bitmap) {
						// control memory usage instead of bitmap counts
						return bitmap.getRowBytes() * bitmap.getHeight();	// [bytes]
					}
				};
				try {
					final File cacheDir = getDiskCacheDir(context);
					if (!cacheDir.exists()) {
						//noinspection ResultOfMethodCallIgnored
						cacheDir.mkdirs();
					}
					if (!cacheDir.canWrite()) {
						Log.w(TAG, "unable to write to cache dir!!");
					}
					if (DEBUG) Log.v(TAG, "prepareThumbnailCache:dir=" + cacheDir);
					sDiskLruCache = DiskLruCache.open(cacheDir,
						BuildConfig.VERSION_CODE, 1, sMaxDiskCacheBytes);
				} catch (final IOException e) {
					sDiskLruCache = null;
					Log.w(TAG, e);
				}
			}
		}
	}

	@SuppressWarnings("ResultOfMethodCallIgnored")
	private static File getDiskCacheDir(@NonNull final Context context) throws IOException {
		File cacheDir;
		cacheDir = context.getExternalCacheDir();
		cacheDir.mkdirs();
		if ((cacheDir == null) || !cacheDir.canWrite()) {
			// 内部ストレージのキャッシュディレクトリを試みる
			cacheDir = context.getCacheDir();
			cacheDir.mkdirs();
		}
		if ((cacheDir == null) || !cacheDir.canWrite()) {
			throw new IOException("can't write cache dir");
		}
		cacheDir = new File(cacheDir, DISK_CACHE_SUBDIR);
		cacheDir.mkdirs();
		return cacheDir;
	}

	/**
	 * コンストラクタ
	 * ディスクキャッシュの最大サイズはデフォルト(現在は10MB)
	 * @param context
	 */
	public ThumbnailCache(@NonNull final Context context) {
		prepareThumbnailCache(context, DISK_CACHE_SIZE);
	}

	/**
	 * コンストラクタ
	 * @param context
	 * @param maxDiskCacheBytes
	 */
	public ThumbnailCache(@NonNull final Context context, final int maxDiskCacheBytes) {
		prepareThumbnailCache(context, maxDiskCacheBytes);
	}

	@Override
	protected void finalize() throws Throwable {
		try {
			trim();
		} finally {
			super.finalize();
		}
	}

	/**
	 * 指定したhashCode/idに対応するキャッシュを取得する
	 * 存在しなければnull
	 * @param id
	 * @return
	 */
	@Nullable
	public Bitmap get(final long id) {
		return get(getKey(id));
	}

	/**
	 * 指定したキーに対応するキャッシュを取得する
	 * 存在しなければnull
	 * @param key
	 * @return
	 */
	@Nullable
	public Bitmap get(@NonNull final String key) {
		Bitmap result;
		synchronized (sSync) {
			// メモリーキャッシュから取得を試みる
			result = sThumbnailCache.get(key);
			if (DEBUG && (result != null)) Log.v(TAG, "get:memory cache hit!");
			if ((result == null) && (sDiskLruCache != null)) {
				// メモリーキャッシュにないときはディスクキャッシュから取得を試みる
				InputStream in = null;
				try {
					final DiskLruCache.Snapshot snapshot = sDiskLruCache.get(key);
					if (snapshot != null) {
						if (DEBUG) Log.v(TAG, "get:disk cache hit!");
						in = snapshot.getInputStream(DISK_CACHE_INDEX);
						if (in != null) {
							final FileDescriptor fd = ((FileInputStream) in).getFD();
							// Decode bitmap, but we don't want to sample so give
							// MAX_VALUE as the target dimensions
							result = BitmapHelper.asBitmap(fd,
								Integer.MAX_VALUE, Integer.MAX_VALUE);
						}
					}
				} catch (final IOException e) {
					if (DEBUG) Log.w(TAG, e);
					try {
						sDiskLruCache.remove(key);
					} catch (final IOException ex) {
						// ignore
					}
				} finally {
					try {
						if (in != null) {
							in.close();
						}
					} catch (final IOException e) {
						// ignore
					}
				}
				if (result != null) {
					// メモリーキャッシュに追加する
					sThumbnailCache.put(key, result);
				}
			}
		}
		return result;
	}

	/**
	 * 指定したidに対応するサムネイルを指定したBitmapで更新する
	 * @param id
	 * @param thumbnail
	 * @param shouldOverride
	 */
	public void put(
		final long id,
		@NonNull final Bitmap thumbnail,
		final boolean shouldOverride) {

		put(getKey(id), thumbnail, shouldOverride);
	}

	/**
	 * 指定したキーに対応するビットマップをキャッシュに追加する
	 * @param key
	 * @param bitmap
	 */
	@Deprecated
	public void put(@NonNull final String key, @NonNull final Bitmap bitmap) {
		put(key, bitmap, false);
	}

	/**
	 * 指定したキーに対応するビットマップをキャッシュに追加する
	 * @param key
	 * @param bitmap
	 * @param shouldOverride
	 */
	public void put(
		@NonNull final String key,
		@NonNull final Bitmap bitmap,
		final boolean shouldOverride) {

		if (DEBUG) Log.v(TAG, "put:key=" + key);
		synchronized (sSync) {
			final Bitmap cached = sThumbnailCache.get(key);
			if ((cached == null) || shouldOverride) {
				sThumbnailCache.put(key, bitmap);
			}
			if (sDiskLruCache != null) {
				// ディスクキャッシュへの追加処理
				OutputStream out = null;
				try {
					if (!sDiskLruCache.contains(key) || shouldOverride) {
						// ディスクキャッシュに保存する時
						final DiskLruCache.Editor editor = sDiskLruCache.edit(key);
						if (editor != null) {
							out = editor.newOutputStream(DISK_CACHE_INDEX);
							bitmap.compress(
								Bitmap.CompressFormat.JPEG, 90, out);
							editor.commit();
							out.close();
						}
					}
				} catch (final IOException e) {
					if (DEBUG) Log.w(TAG, e);
				} catch (final Exception e) {
					if (DEBUG) Log.w(TAG, e);
				} finally {
					try {
						if (out != null) {
							out.close();
						}
					} catch (final IOException e) {
						if (DEBUG) Log.w(TAG, e);
					}
				}
			}
		}
	}

	/**
	 * キャッシュをクリアする
	 */
	public void clear() {
		if (DEBUG) Log.v(TAG, "clear:");
		synchronized (sSync) {
			sThumbnailCache.evictAll();
			if (sDiskLruCache != null) {
				try {
					sDiskLruCache.delete();
				} catch (final IOException e) {
					if (DEBUG) Log.w(TAG, e);
				}
			}
		}
	}

	/**
	 * メモリーキャッシュの過剰分を破棄する
	 */
	public void trim() {
		if (DEBUG) Log.v(TAG, "trim:");
		synchronized (sSync) {
			sThumbnailCache.trimToSize(sCacheSize);
			if (sDiskLruCache != null) {
				try {
					sDiskLruCache.flush();
				} catch (final IOException e) {
					if (DEBUG) Log.w(TAG, e);
				}
			}
		}
	}

	/**
	 * 指定したキーに対応するキャッシュエントリーを削除する
	 * @param key
	 */
	public void remove(final String key) {
		if (DEBUG) Log.v(TAG, "remove:key=" + key);
		synchronized (sSync) {
			sThumbnailCache.remove(key);
			if (sDiskLruCache != null) {
				try {
					sDiskLruCache.remove(key);
				} catch (final IOException e) {
					// ignore
				}
			}
		}
	}

	/**
	 * MediaInfoで指定したコンテンツのサムネイルを取得する
	 * 現在は静止画と動画のみに対応
	 * @param cr
	 * @param info
	 * @param requestWidth
	 * @param requestHeight
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	@NonNull
	public Bitmap getThumbnail(
		@NonNull final ContentResolver cr,
		@NonNull final MediaInfo info,
		final int requestWidth, final int requestHeight)
			throws FileNotFoundException, IOException {

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			return cr.loadThumbnail(info.getUri(), new Size(requestWidth, requestHeight), null);
		} else {
			final Bitmap result;
			if (info.mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE) {
				if ((requestWidth <= 0) || (requestHeight <= 0)) {
					result = BitmapHelper.asBitmap(cr, info.getUri(), requestWidth, requestHeight);
				} else {
					result = getImageThumbnail(cr, info.id, requestWidth, requestHeight);
				}
			} else if (info.mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) {
				result = getVideoThumbnail(cr, info.id, requestWidth, requestHeight);
			} else {
				throw new UnsupportedOperationException("unexpected mediaType");
			}
			if (result == null) {
				throw new IOException("failed to load thumbnail," + info);
			}
			return result;
		}
	}

	/**
	 * 静止画のサムネイルを取得する
	 * 可能であればキャッシュから取得する
	 * @param cr
	 * @param id
	 * @param requestWidth
	 * @param requestHeight
	 * @return
	 * @throws IOException
	 */
	@NonNull
	public Bitmap getImageThumbnail(
		@NonNull final ContentResolver cr, final long id,
		final int requestWidth, final int requestHeight)
			throws FileNotFoundException, IOException {

		// try to get from internal thumbnail cache(in memory), this may be redundant
		final String key = getKey(id);
		Bitmap result;
		synchronized (sSync) {
			result = get(key);
			if (result == null) {
				if ((requestWidth <= 0) || (requestHeight <= 0)) {
					result = BitmapHelper.asBitmap(cr, id, requestWidth, requestHeight);
				} else {
					int kind = MediaStore.Images.Thumbnails.MICRO_KIND;
					if ((requestWidth > 96) || (requestHeight > 96) || (requestWidth * requestHeight > 128 * 128)) {
						kind = MediaStore.Images.Thumbnails.MINI_KIND;
					}
					try {
						// XXX ContentResolverには存在するが実ファイルがすでに削除されていると
						// XXX ここでFileNotFoundExceptionが投げられるんだけどキャッチできない
						result = MediaStore.Images.Thumbnails.getThumbnail(cr, id, kind, null);
					} catch (final Exception e) {
						if (DEBUG) Log.w(TAG, e);
						remove(key);
						throw (e instanceof IOException) ? (IOException)e :  new IOException(e);
					}
				}
				if (result != null) {
					final int orientation = BitmapHelper.getOrientation(cr, id);
					if (orientation != 0) {
						final Bitmap newBitmap = BitmapHelper.rotateBitmap(result, orientation);
						result.recycle();
						result = newBitmap;
					}
					if (DEBUG) Log.v(TAG, String.format("getImageThumbnail:id=%d(%d,%d)",
						id, result.getWidth(), result.getHeight()));
					// add to internal thumbnail cache(in memory)
					put(key, result, false);
				} else {
					throw new IOException("failed to get thumbnail,key=" + key + "/id=" + id);
				}
			}
		}
		return result;
	}

	/**
	 * 動画のサムネイルを取得する
	 * 可能であればキャッシュから取得する
	 * @param cr
	 * @param id
	 * @param requestWidth
	 * @param requestHeight
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	@NonNull
	public Bitmap getVideoThumbnail(
		@NonNull final ContentResolver cr, final long id,
		final int requestWidth, final int requestHeight)
			throws FileNotFoundException, IOException {

		// try to get from internal thumbnail cache(in memory), this may be redundant
		final String key = getKey(id);
		Bitmap result;
		synchronized (sSync) {
			result = get(key);
			if (result == null) {
				int kind = MediaStore.Video.Thumbnails.MICRO_KIND;
				if ((requestWidth > 96) || (requestHeight > 96) || (requestWidth * requestHeight > 128 * 128)) {
					kind = MediaStore.Video.Thumbnails.MINI_KIND;
				}
				try {
					// XXX ContentResolverには存在するが実ファイルがすでに削除されていると
					// XXX ここでFileNotFoundExceptionが投げられるんだけどキャッチできない
					result = MediaStore.Video.Thumbnails.getThumbnail(cr, id, kind, null);
				} catch (final Exception e) {
					if (DEBUG) Log.w(TAG, e);
					remove(key);
					throw (e instanceof IOException) ? (IOException)e :  new IOException(e);
				}
				if (result != null) {
					if (DEBUG) Log.v(TAG, String.format("getVideoThumbnail:id=%d(%d,%d)",
						id, result.getWidth(), result.getHeight()));
					// XXX 動画はExifが無いはずなのとAndroid10未満だとorientationフィールドが無い可能性が高いので実際には回転しないかも
					final int orientation = BitmapHelper.getOrientation(cr, id);
					if (orientation != 0) {
						final Bitmap newBitmap = BitmapHelper.rotateBitmap(result, orientation);
						result.recycle();
						result = newBitmap;
					}
					// add to internal thumbnail cache(in memory)
					put(key, result, false);
				} else {
					throw new IOException("failed to get thumbnail,key=" + key + "/id=" + id);
				}
			}
		}
		return result;
	}

	/**
	 * キャッシュエントリー用のキー文字列生成
	 * @param id
	 * @return
	 */
	private static String getKey(final long id) {
		return String.format(Locale.US, "%x", id);
	}

}
