package com.serenegiant.widget;
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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;

import com.serenegiant.common.R;

import androidx.annotation.Nullable;

public class ProgressView extends View {

	private final Object mSync = new Object();

	private int mRotation = 90;
	/**
	 * progressの最小・最大値
	 * それぞれの値でサチュレーション計算する
	 */
	private int mMin = 0, mMax = 100;
	/**
	 * progressの値をlevelに変換するための係数
	 * ClipDrawableのsetLevelに指定する値は0が完全にクリップ、10000がクリップなし
	 */
	private float mScale = 100;
	/**
	 * progressの現在値
	 */
	private int mProgress = 40;

	/**
	 * Drawableを指定しない時に使うprogress表示色
	 */
	private int mColor = 0xffff0000;
	/**
	 * progressを表示するDrawable
	 */
	private Drawable mDrawable;
	/**
	 * progressに応じてmDrawableをクリップするためのClipDrawable
	 */
	private ClipDrawable mClipDrawable;

	/**
	 * コンストラクタ
	 * @param context
	 */
	public ProgressView(final Context context) {
		this(context, null, 0);
	}

	/**
	 * コンストラクタ
	 * @param context
	 * @param attrs
	 */
	public ProgressView(final Context context, final AttributeSet attrs) {
		this(context, attrs, 0);
	}

	/**
	 * コンストラクタ
	 * @param context
	 * @param attrs
	 * @param defStyleAttr
	 */
	public ProgressView(final Context context, final AttributeSet attrs, final int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		final TypedArray a = context.getTheme().obtainStyledAttributes(
			attrs, R.styleable.ProgressView, defStyleAttr, 0);
		mDrawable = a.getDrawable(R.styleable.ProgressView_android_drawable);
		if (mDrawable == null) {
			mColor = a.getColor(R.styleable.ProgressView_android_color, mColor);
		}
		mRotation = (int)a.getFloat(R.styleable.ProgressView_android_rotation, mRotation);
		mMin = a.getInt(R.styleable.ProgressView_android_min, mMin);
		mMax = a.getInt(R.styleable.ProgressView_android_min, mMax);
		if (mMin == mMax) {
			throw new IllegalArgumentException("min and max should be different");
		}
		if (mMin > mMax) {
			final int w = mMin;
			mMin = mMax;
			mMax = w;
		}
		mProgress = a.getInt(R.styleable.ProgressView_android_progress, mProgress);
		a.recycle();
	}

	@Override
	protected void onDraw(final Canvas canvas) {
		super.onDraw(canvas);
		mClipDrawable.draw(canvas);
	}

	@Override
	protected void onLayout(final boolean changed, final int left, final int top, final int right, final int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		resize();
	}

	/**
	 * 最大・最小値をセット
	 * @param min
	 * @param max
	 */
	public void setMinMax(final int min, final int max) {
		if (((mMin != min) || (mMax != max)) && (min != max)) {
			mMin = Math.min(min, max);
			mMax = Math.max(min, max);
			resize();
		}
	}

	/**
	 * progress値を設定
	 * 最小値よりも小さければ最小値になる。最大値よりも大きければ最大値になる。
	 * @param progress
	 */
	public void setProgress(final int progress) {
		synchronized (mSync) {
			if (mProgress != progress) {
				mProgress = progress;
				// 前はpostInvalidateを呼び出せばUIスレッド以外でも更新できたと思ったんだけど
				// UIスレッドじゃないと更新できない機種/Androidのバージョンがあるのかも
				removeCallbacks(mUpdateProgressOnUITask);
				post(mUpdateProgressOnUITask);
			}
		}
	}

	/**
	 * progressをUIスレッド上で更新するためのRunnable実装
	 */
	private final Runnable mUpdateProgressOnUITask = new Runnable() {
		@Override
		public void run() {
			final int progress;
			synchronized (mSync) {
				progress = mProgress;
			}
			if (mClipDrawable != null)  {
				int level = (int)(progress * mScale) + mMin;
				if (level < 0) level = 0;
				if (level > 10000) level = 10000;
				mClipDrawable.setLevel(level);
			}
			invalidate();
		}
	};

	/**
	 * プログレスの回転方向を指定
	 * @param rotation 0:
	 */
	public void setRotation(int rotation) {
		rotation = ((rotation / 90) * 90) % 360;
		if (mRotation != rotation) {
			mRotation = rotation;
			resize();
		}
	}

	/**
	 * progress表示用の色を指定する。
	 * #setDrawableと#setColorは後から呼び出した方が優先される。
	 * @param color
	 */
	public void setColor(final int color) {
		if (mColor != color) {
			mColor = color;
			refreshDrawable(null);
		}
	}

	/**
	 * progress表示用のDrawableを指定する。
	 * #setDrawableと#setColorは後から呼び出した方が優先される。
	 * @param drawable
	 */
	public void setDrawable(final Drawable drawable) {
		if (mDrawable != drawable) {
			refreshDrawable(drawable);
		}
	}

	/**
	 * Viewのサイズ変更時の処理
	 */
	protected void resize() {
		synchronized (mSync) {
			final float progress = mProgress * mScale + mMin;
			mScale = 10000.0f / (mMax - mMin);
			mProgress = (int)((progress - mMin) / mScale);
		}
		refreshDrawable(mDrawable);
	}

	private static final int CLIP_HORIZONTAL = 0x08;
	private static final int CLIP_VERTICAL = 0x80;

	/**
	 * Progress表示用のDrawableを設定
	 * @param drawable
	 */
	@SuppressLint("RtlHardcoded")
	protected void refreshDrawable(@Nullable final Drawable drawable) {
		final int level;
		synchronized (mSync) {
			level = (int)(mProgress * mScale) + mMin;
		}
		mDrawable = drawable;
		if (mDrawable == null) {
			mDrawable = new ColorDrawable(mColor);
		}
		int gravity = Gravity.FILL_VERTICAL | CLIP_HORIZONTAL | Gravity.LEFT;
		int orientation = ClipDrawable.HORIZONTAL;
		while (mRotation < 0) {
			mRotation += 360;
		}
		mRotation %= 360;
		switch (mRotation) {
		case 90:
			gravity = Gravity.FILL_HORIZONTAL | CLIP_VERTICAL | Gravity.BOTTOM;
			orientation = ClipDrawable.VERTICAL;
			break;
		case 180:
			gravity = Gravity.FILL_VERTICAL | CLIP_HORIZONTAL | Gravity.RIGHT;
			orientation = ClipDrawable.HORIZONTAL;
			break;
		case 270:
			gravity = Gravity.FILL_HORIZONTAL | CLIP_VERTICAL | Gravity.TOP;
			orientation = ClipDrawable.VERTICAL;
			break;
		default:
			break;
		}
		// プログレス表示用のClipDrawableを生成
		mClipDrawable = new ClipDrawable(mDrawable, gravity, orientation);
		final Rect outRect = new Rect();
		getDrawingRect(outRect);
		// XXX パディングを差し引いた方がいい？
		mClipDrawable.setBounds(outRect);
		mClipDrawable.setLevel(level);
		postInvalidate();
	}
}
