package com.serenegiant.widget
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2026 saki t_saki@serenegiant.com
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

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceView
import com.serenegiant.view.R
import com.serenegiant.widget.IScaledView.ScaleMode
import kotlin.math.roundToInt

/**
 * IScaledViewを実装したSurfaceView
 */
class ScaledSurfaceView  @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyle: Int = 0
) : SurfaceView(context, attrs, defStyle), IScaledView {
	/**
	 * スケールモード
	 */
	@ScaleMode
	private var mScaleMode = IScaledView.SCALE_MODE_CROP

	/**
	 * 表示内容のアスペクト比
	 * 0以下なら無視される
	 */
	private var mRequestedAspect = -1.0

	/**
	 * スケールモードがキープアスペクトの場合にViewのサイズをアスペクト比に合わせて変更するかどうか
	 */
	private var mNeedResizeToKeepAspect = true

	init {
		if (DEBUG) Log.v(TAG, "コンストラクタ:")
		val a = context.theme.obtainStyledAttributes(attrs, R.styleable.IScaledView, defStyle, 0)
		try {
			mRequestedAspect = a.getFloat(R.styleable.IScaledView_aspect_ratio, mRequestedAspect.toFloat()).toDouble()
			mScaleMode = a.getInt(R.styleable.IScaledView_scale_mode, mScaleMode)
			mNeedResizeToKeepAspect = a.getBoolean(R.styleable.IScaledView_resize_to_keep_aspect, mNeedResizeToKeepAspect)
		} finally {
			a.recycle()
		}
	}

	/**
	 * スケールモードとアスペクト比に合わせてサイズを決める
	 * @param widthMeasureSpec
	 * @param heightMeasureSpec
	 */
	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec)
		if (DEBUG) Log.v(TAG, "onMeasure:scaleMode=$scaleMode,aspect=$aspectRatio")
		val width = MeasureSpec.getSize(widthMeasureSpec)
		val height = MeasureSpec.getSize(heightMeasureSpec)
		if (aspectRatio <= 0.0) {
			setMeasuredDimension(width, height)
		} else {
			val newWidth: Int
			val newHeight: Int
			val actualRatio = if (width > height) aspectRatio else 1f / aspectRatio
			when (scaleMode) {
				IScaledView.SCALE_MODE_CROP -> {
					// 映像がはみ出す前提でアスペクト比を維持してView全面に映像を表示するようにサイズを計算、
					if (width < height * actualRatio) {
						newHeight = height
						newWidth = (height * actualRatio).roundToInt()
					} else {
						newWidth = width
						newHeight = (width / actualRatio).roundToInt()
					}
				}
				IScaledView.SCALE_MODE_KEEP_ASPECT -> {
					// 映像がはみ出さないようにViewの上下中央にアスペクト比を維持した映像を表示するようにサイズを計算
					// 短辺または長辺のいずれかに空きが生じる場合がある
					if (width < height * actualRatio) {
						newWidth = width
						newHeight = (width / actualRatio).roundToInt()
					} else {
						newHeight = height
						newWidth = (height * actualRatio).roundToInt()
					}
				}
				else -> {
					newWidth = width
					newHeight = height
				}
			}

			if (DEBUG) Log.d(TAG, "onMeasure:MeasuredSpec=${newWidth}x$newHeight")
			setMeasuredDimension(newWidth, newHeight)
		}
	}

	/**
	 * IScaledViewの実装
	 * アスペクト比を設定する。アスペクト比=`幅 / 高さ`.
	 * XXX このメソッドを呼び出す前にgetHolder().setFixedSize(width,height)を呼び出して
	 *     Surfaceのサイズを固定しないと正常に動作しない
	 *     もしくは#setAspectRatio(width,height)を使うこと
	 * @param aspectRatio
	 */
	override fun setAspectRatio(aspectRatio: Double) {
		if (DEBUG) Log.v(TAG, "setAspectRatio:$aspectRatio")
		Log.v(TAG, "Should call this after calling getHolder().setFixedSize(width,height) otherwise this will not work well.")
		if (mRequestedAspect != aspectRatio) {
			mRequestedAspect = aspectRatio
			requestLayout()
		}
	}

	/**
	 * IScaledViewの実装
	 * アスペクト比を設定する
	 * @param width
	 * @param height
	 */
	override fun setAspectRatio(width: Int, height: Int) {
		if (DEBUG) Log.v(TAG, "setAspectRatio:(${width}x$height)")
		require(width > 0 && height > 0) { "Size cannot be zero and negative" }
		val aspectRatio = width / height.toDouble()
		holder.setFixedSize(width, height)
		if (mRequestedAspect != aspectRatio) {
			mRequestedAspect = aspectRatio
			requestLayout()
		}
	}

	/**
	 * IScaledViewの実装
	 */
	override fun getAspectRatio(): Double {
		return mRequestedAspect
	}

	/**
	 * IScaledViewの実装
	 * @param scaleMode
	 */
	override fun setScaleMode(@ScaleMode scaleMode: Int) {
		if (DEBUG) Log.v(TAG, "setScaleMode:$scaleMode")
		if (mScaleMode != scaleMode) {
			mScaleMode = scaleMode
			requestLayout()
		}
	}

	/**
	 * IScaledViewの実装
	 */
	@ScaleMode
	override fun getScaleMode(): Int {
		return mScaleMode
	}

	/**
	 * IScaledViewの実装
	 * @param keepAspect
	 */
	override fun setNeedResizeToKeepAspect(keepAspect: Boolean) {
		if (mNeedResizeToKeepAspect != keepAspect) {
			mNeedResizeToKeepAspect = keepAspect
			requestLayout()
		}
	}

	companion object {
		private const val DEBUG = false	// set false on production
		private val TAG = ScaledSurfaceView::class.java.simpleName
	}
}