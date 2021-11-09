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

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;

import com.serenegiant.utils.ThreadPool;

import java.util.concurrent.FutureTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * 非同期で画像読み込みを行うためのヘルパークラス(Runnableを実装)
 */
public abstract class ThumbnailLoader implements Runnable {
	private static final boolean DEBUG = false; // set false on production
	private static final String TAG = ThumbnailLoader.class.getSimpleName();

	@NonNull
	protected final ThumbnailLoaderDrawable mParent;
	@NonNull
	private final FutureTask<Bitmap> mTask;
	@NonNull
	private final MediaInfo mInfo = new MediaInfo();
	@Nullable
	private Bitmap mBitmap;

	/**
	 * コンストラクタ
	 * @param parent
	 */
    public ThumbnailLoader(@NonNull final ThumbnailLoaderDrawable parent) {
    	mParent = parent;
		mTask = new FutureTask<Bitmap>(this, null);
    }

	public long id() {
		return mInfo.id;
	}

	@Nullable
	public Uri getUri() {
		return mInfo.getUri();
	}

    /**
	 * 読み込み開始する
	 * @param info
     */
	public synchronized void startLoad(@NonNull final MediaInfo info) {
		mInfo.set(info);
		mBitmap = null;
		ThreadPool.queueEvent(mTask);
	}

	/**
	 * 読み込み中断要求する
	 */
	public void cancelLoad() {
		mTask.cancel(true);
	}

	/**
	 * 実際の読み込み処理
	 * @param context
	 * @param info
	 * @param requestWidth
	 * @param requestHeight
	 * @return
	 */
	@Nullable
	protected abstract Bitmap loadThumbnail(
		@NonNull final Context context,
		@NonNull final MediaInfo info,
		final int requestWidth, final int requestHeight);

	@Override
	public void run() {
		final MediaInfo info;
		synchronized(this) {
			info = new MediaInfo(mInfo);
		}
		if (!mTask.isCancelled()) {
			mBitmap = loadThumbnail(mParent.getContext(), info,
				mParent.getIntrinsicWidth(), mParent.getIntrinsicHeight());
		}
		if (mTask.isCancelled() || !info.equals(mInfo)) {
			return;	// return without callback
		}
		// set callback
		mParent.scheduleSelf(mParent, 0);
	}

	/**
	 * 読み込んだサムネイルを取得する
	 * @return
	 */
	@Nullable
	public Bitmap getBitmap() {
		return mBitmap;
	}
}
