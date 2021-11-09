package com.serenegiant.widget;
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
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import com.serenegiant.common.R;
import com.serenegiant.view.MeasureSpecDelegater;

/**
 * Created by saki on 2016/12/03.
 * 指定したアスペクト比に合わせて外形サイズを変更させるFrameLayout
 */
public class AspectRatioFrameLayout extends FrameLayout implements IScaledView {

	@ScaleMode
	private int mScaleMode = SCALE_MODE_KEEP_ASPECT;
	/**
	 * 表示内容のアスペクト比
	 * 0以下なら無視される
	 */
	private double mRequestedAspect = -1.0;		// initially use default window size
	/**
	 * スケールモードがキープアスペクトの場合にViewのサイズをアスペクト比に合わせて変更するかどうか
	 */
	private boolean mNeedResizeToKeepAspect;

	/**
	 * コンストラクタ
	 * @param context
	 */
	public AspectRatioFrameLayout(final Context context) {
		this(context, null, 0);
	}

	/**
	 * コンストラクタ
	 * @param context
	 * @param attrs
	 */
	public AspectRatioFrameLayout(final Context context, final AttributeSet attrs) {
		super(context, attrs, 0);
	}

	/**
	 * コンストラクタ
	 * @param context
	 * @param attrs
	 * @param defStyleAttr
	 */
	public AspectRatioFrameLayout(final Context context, final AttributeSet attrs, final int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		final TypedArray a = context.getTheme().obtainStyledAttributes(attrs,
			R.styleable.IScaledView, defStyleAttr, 0);
		try {
			mRequestedAspect = a.getFloat(
				R.styleable.IScaledView_aspect_ratio, -1.0f);
			mScaleMode = a.getInt(
				R.styleable.IScaledView_scale_mode, SCALE_MODE_KEEP_ASPECT);
			mNeedResizeToKeepAspect = a.getBoolean(
				R.styleable.IScaledView_resize_to_keep_aspect, true);
		} finally {
			a.recycle();
		}
	}

	/**
	 * アスペクト比を保つように大きさを決める
	 */
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		final MeasureSpecDelegater.MeasureSpec spec
			= MeasureSpecDelegater.onMeasure(this,
				mRequestedAspect, mScaleMode, mNeedResizeToKeepAspect,
				widthMeasureSpec, heightMeasureSpec);
		super.onMeasure(spec.widthMeasureSpec, spec.heightMeasureSpec);
 	}

	@Override
	public void setScaleMode(final int scaleMode) {
		if (mScaleMode != scaleMode) {
			mScaleMode = scaleMode;
			requestLayout();
		}
	}

	@Override
	public int getScaleMode() {
		return mScaleMode;
	}

	@Override
	public void setAspectRatio(final double aspectRatio) {
		if (mRequestedAspect != aspectRatio) {
			mRequestedAspect = aspectRatio;
			requestLayout();
		}
	}

	@Override
	public void setAspectRatio(final int width, final int height) {
		setAspectRatio(width / (double)height);
	}

	@Override
	public double getAspectRatio() {
		return mRequestedAspect;
	}

	@Override
	public void setNeedResizeToKeepAspect(final boolean keepAspect) {
		if (mNeedResizeToKeepAspect != keepAspect) {
			mNeedResizeToKeepAspect = keepAspect;
			requestLayout();
		}
	}

}
