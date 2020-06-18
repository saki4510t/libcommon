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
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;

import com.serenegiant.utils.ThreadPool;

import java.util.concurrent.FutureTask;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

/**
 * 非同期で画像読み込みを行うためのヘルパークラス(Runnableを実装)
 */
public abstract class ImageLoader implements Runnable {
	@NonNull
	protected final LoaderDrawable mParent;
	@NonNull
	private final FutureTask<Bitmap> mTask;
	private int mMediaType;
	private long mId;
	private Bitmap mBitmap;

	/**
	 * コンストラクタ
	 * @param parent
	 */
    public ImageLoader(@NonNull final LoaderDrawable parent) {
    	mParent = parent;
		mTask = new FutureTask<Bitmap>(this, null);
    }

	public long id() {
		return mId;
	}

    /**
	 * start loading
     * @param id
     */
	public synchronized void startLoad(final int mediaType, final long id) {
		mMediaType = mediaType;
		mId = id;
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
	 * @param mediaType
	 * @param id
	 * @param requestWidth
	 * @param requestHeight
	 * @return
	 */
	protected abstract Bitmap loadBitmap(
		@NonNull final Context context,
		final int mediaType, final long id,
		final int requestWidth, final int requestHeight);

	/**
	 * 指定したDrawableリソースからビットマップとして画像を取得する
	 * @param drawableRes
	 * @return
	 */
	@Nullable
	protected Bitmap loadDefaultBitmap(
		@NonNull final Context context,
		@DrawableRes final int drawableRes) {

		Bitmap result = null;
		if (drawableRes != 0) {
			Drawable drawable = ContextCompat.getDrawable(context, drawableRes);
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
				drawable = (DrawableCompat.wrap(drawable)).mutate();
			}
			if (drawable instanceof BitmapDrawable) {
				result = ((BitmapDrawable)drawable).getBitmap();
			} else {
				result = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
					drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
				final Canvas canvas = new Canvas(result);
				drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
				drawable.draw(canvas);
			}
		}
		if (result == null) {
			throw new IllegalArgumentException("failed to load from resource," + drawableRes);
		}
		return result;
	}

	@Override
	public void run() {
		int mediaType;
		long id;
		synchronized(this) {
			mediaType = mMediaType;
			id = mId;
		}
		if (!mTask.isCancelled()) {
			mBitmap = loadBitmap(mParent.getContext(),
				mediaType, id,
				mParent.getIntrinsicWidth(), mParent.getIntrinsicHeight());
		}
		if (mTask.isCancelled() || (id != mId) || (mBitmap == null)) {
			return;	// return without callback
		}
		// set callback
		mParent.scheduleSelf(mParent, 0);
	}

	public Bitmap getBitmap() {
		return mBitmap;
	}
}
