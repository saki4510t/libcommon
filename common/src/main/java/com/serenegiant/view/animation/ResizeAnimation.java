package com.serenegiant.view.animation;
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

import androidx.annotation.NonNull;

import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;

import com.serenegiant.view.ViewUtils;

/**
 * Viewのりサイズを行うAnimationクラス
 * 見た目だけじゃなくて実際のサイズも変更する
 */

public class ResizeAnimation extends Animation {
	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = ResizeAnimation.class.getSimpleName();

	@NonNull
	private final View mTargetView;
	private final int mStartWidth, mStartHeight;
	private final int mDiffWidth, mDiffHeight;

	public ResizeAnimation(@NonNull final View view,
		final int startWidth, final int startHeight,
		final int endWidth, final int endHeight) {

		mTargetView = view;
		mStartWidth = startWidth;
		mStartHeight = startHeight;
		mDiffWidth = endWidth - startWidth;
		mDiffHeight = endHeight - startHeight;
		if (DEBUG) Log.v(TAG, String.format("コンストラクタ:(%dx%d)→(%dx%d)",
			startWidth, startHeight, endWidth, endHeight));
		ViewUtils.requestResize(mTargetView, startWidth, startHeight);
	}
	
	@Override
	protected void applyTransformation(final float interpolatedTime,
		final Transformation t) {

		super.applyTransformation(interpolatedTime, t);	// this is empty method now
		
		ViewUtils.requestResize(mTargetView,
			(int)(mStartWidth + mDiffWidth * interpolatedTime),
			(int)(mStartHeight + mDiffHeight * interpolatedTime));
	}
	
	@Override
	public void initialize(final int width, final int height,
		final int parentWidth, final int parentHeight) {

		super.initialize(width, height, parentWidth, parentHeight);
	}
	
	@Override
	public boolean willChangeBounds() {
		return (mDiffWidth != 0) || (mDiffHeight != 0);
	}
}
