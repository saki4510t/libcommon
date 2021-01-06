package com.serenegiant.mediastore;
/*
 * LoaderDrawable is a descendent of Drawable to load image asynchronusly and draw
 * We want to use BitmapDrawable but we can't because it has no public/protected method
 * to set Bitmap after construction.
 *
 * Most code of LoaderDrawable came from BitmapJobDrawable.java in Android Gallery app
 *
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;

import com.serenegiant.graphics.MatrixUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public abstract class ThumbnailLoaderDrawable extends Drawable implements Runnable {
	private static final boolean DEBUG = false;	// FIXME 実働時はfalseにすること
	private static final String TAG = ThumbnailLoaderDrawable.class.getSimpleName();

	private static final int DEFAULT_PAINT_FLAGS =
		Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG;

	private final Context mContext;
    private final Paint mPaint = new Paint(DEFAULT_PAINT_FLAGS);
    private final Paint mDebugPaint = new Paint(DEFAULT_PAINT_FLAGS);
    private final Matrix mDrawMatrix = new Matrix();
	private Bitmap mBitmap;
    private int mRotationDegree = 0;
    private ThumbnailLoader mLoader;
	private int mTargetDensity = DisplayMetrics.DENSITY_DEFAULT;
    private int mBitmapWidth, mBitmapHeight;

	/**
	 * コンストラクタ
	 * XXX 仮のサイズをセットしておかないとListView/GridView/RecyclerView等の描画が極端に遅くなる
	 * @param context
	 * @param width
	 * @param height
	 */
	public ThumbnailLoaderDrawable(
		@NonNull final Context context,
		final int width, final int height) {

		mContext = context;
		mDebugPaint.setColor(Color.RED);
		mDebugPaint.setTextSize(18);
		mBitmapWidth = width;
		mBitmapHeight = height;
	}

    @Override
    protected void onBoundsChange(final Rect bounds) {
        super.onBoundsChange(bounds);
        updateDrawMatrix(getBounds());
    }

    @Override
	public void draw(@NonNull final Canvas canvas) {
        final Rect bounds = getBounds();
        if (mBitmap != null) {
            canvas.save();
            try {
				canvas.clipRect(bounds);
				canvas.concat(mDrawMatrix);
				canvas.rotate(mRotationDegree, bounds.centerX(), bounds.centerY());
				canvas.drawBitmap(mBitmap, 0, 0, mPaint);
			} finally {
				canvas.restore();
			}
        } else {
        	// 画像が設定されていないとき
            mPaint.setColor(0xFFCCCCCC);
            canvas.drawRect(bounds, mPaint);
        }
		if (DEBUG) {
			canvas.drawText(Long.toString(mLoader != null ? mLoader.id() : -1),
				bounds.centerX(), bounds.centerY(), mDebugPaint);
		}
	}

	@NonNull
	protected Context getContext() {
		return mContext;
	}

	private void updateDrawMatrix(@NonNull final Rect bounds) {
	    if (mBitmap == null || bounds.isEmpty()) {
	        mDrawMatrix.reset();
	        return;
	    }

		// FIXME mRotationDegreeを考慮して計算しないといけない
		MatrixUtils.updateDrawMatrix(MatrixUtils.ScaleType.CENTER_CROP,
			mDrawMatrix,
			bounds.width(), bounds.height(),
			mBitmap.getWidth(), mBitmap.getHeight());
	    invalidateSelf();
	}

	@Override
	public void setAlpha(final int alpha) {
        int oldAlpha = mPaint.getAlpha();
        if (alpha != oldAlpha) {
            mPaint.setAlpha(alpha);
            invalidateSelf();
        }
	}

	@Override
	public void setColorFilter(@Nullable final ColorFilter cf) {
        mPaint.setColorFilter(cf);
        invalidateSelf();
	}

    @Override
    public int getIntrinsicWidth() {
    	return mBitmapWidth;
    }

    @Override
    public int getIntrinsicHeight() {
    	return mBitmapHeight;
    }

	@Override
	public int getOpacity() {
        final Bitmap bm = mBitmap;
        return (bm == null || bm.hasAlpha() || mPaint.getAlpha() < 255) ?
                PixelFormat.TRANSLUCENT : PixelFormat.OPAQUE;
	}

	/**
	 * Set the density scale at which this drawable will be rendered. This
	 * method assumes the drawable will be rendered at the same density as the
	 * specified canvas.
	 *
	 * @param canvas The Canvas from which the density scale must be obtained.
	 *
	 * @see Bitmap#setDensity(int)
	 * @see Bitmap#getDensity()
	 */
	public void setTargetDensity(@NonNull final  Canvas canvas) {
		setTargetDensity(canvas.getDensity());
	}

	/**
	 * Set the density scale at which this drawable will be rendered.
	 *
	 * @param metrics The DisplayMetrics indicating the density scale for this drawable.
	 *
	 * @see Bitmap#setDensity(int)
	 * @see Bitmap#getDensity()
	 */
	public void setTargetDensity(@NonNull final DisplayMetrics metrics) {
		setTargetDensity(metrics.densityDpi);
	}

	/**
	 * Set the density at which this drawable will be rendered.
	 *
	 * @param density The density scale for this drawable.
	 *
	 * @see Bitmap#setDensity(int)
	 * @see Bitmap#getDensity()
	 */
	public void setTargetDensity(final int density) {
		if (mTargetDensity != density) {
			mTargetDensity = density == 0 ? DisplayMetrics.DENSITY_DEFAULT : density;
			if (mBitmap != null) {
				computeBitmapSize();
			}
			invalidateSelf();
		}
	}

	public void setRotation(final int rotationDegree) {
		if (mRotationDegree != rotationDegree) {
			mRotationDegree = rotationDegree;
			invalidateSelf();
		}
	}

	public int getRotation() {
		return mRotationDegree;
	}

    /**
     * callback to set bitmap on UI thread after asynchronous loading
     * request call this callback in ThumbnailLoader#run at the end of asyncronus loading
     */
	@Override
	public void run() {
		setBitmap(mLoader.getBitmap());
	}

	@NonNull
	protected abstract ThumbnailLoader createLoader();

	/**
	 * 指定したgroupId/idに対応するキャッシュを取得する
	 * 存在しなければnull
	 * @param id
	 * @return
	 */
	@Nullable
	protected abstract Bitmap checkCache(final long id);

	/**
	 * start loading image asynchronously
	 * @param info
	 */
	public void startLoad(@NonNull final MediaInfo info) {

		if (mLoader != null) {
			mLoader.cancelLoad();
		}

		// キャッシュから取得を試みる
		final Bitmap newBitmap = checkCache(info.id);
		if (newBitmap == null) {
			// キャッシュから取得できなかったときは非同期読み込み要求する
			mBitmap = null;
			// re-using ThumbnailLoader will cause several problems on some devices...
			mLoader = createLoader();
			mLoader.startLoad(info);
		} else {
			// キャッシュから取得できたとき
			setBitmap(newBitmap);
		}
		invalidateSelf();
	}

	protected void setBitmap(@Nullable final Bitmap bitmap) {
		if (bitmap != mBitmap) {
			mBitmap = bitmap;
			computeBitmapSize();
            updateDrawMatrix(getBounds());
		}
	}

	private void computeBitmapSize() {
		if (mBitmap != null) {
			mBitmapWidth = mBitmap.getScaledWidth(mTargetDensity);
			mBitmapHeight = mBitmap.getScaledHeight(mTargetDensity);
		} else {
			mBitmapWidth = mBitmapHeight = -1;
		}
	}
}
