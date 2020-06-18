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

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;

import com.serenegiant.common.R;
import com.serenegiant.graphics.BitmapHelper;
import com.serenegiant.utils.ThreadPool;

import java.util.concurrent.FutureTask;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

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

	public Uri getUri() {
		return mInfo.getUri();
	}

    /**
	 * start loading
	 * @param info
     */
	public synchronized void startLoad(@NonNull final MediaInfo info) {
		mInfo.set(info);
		mBitmap = null;
		ThreadPool.queueEvent(mTask);
	}

	/**
	 * cancel loading
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
	@NonNull
	protected abstract Bitmap loadThumbnail(
		@NonNull final Context context,
		@NonNull final MediaInfo info,
		final int requestWidth, final int requestHeight);

	/**
	 * 指定したDrawableリソースからビットマップとして画像を取得する
	 * @param drawableRes
	 * @return
	 */
	@NonNull
	protected Bitmap loadDefaultThumbnail(
		@NonNull final Context context,
		@DrawableRes final int drawableRes) {

		return BitmapHelper.fromDrawable(context, drawableRes);
	}

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
		if (mBitmap == null) {
			mBitmap = loadDefaultThumbnail(mParent.getContext(), R.drawable.ic_error_outline_red_24dp);
		}
		// set callback
		mParent.scheduleSelf(mParent, 0);
	}

	public Bitmap getBitmap() {
		return mBitmap;
	}
}
