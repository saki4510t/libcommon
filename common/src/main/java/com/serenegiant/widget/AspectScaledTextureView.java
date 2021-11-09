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
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.util.Log;
import android.view.TextureView;

import com.serenegiant.common.R;
import com.serenegiant.view.MeasureSpecDelegater;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * View/表示内容のスケーリング処理を追加したTextureView
 * スケーリングモードがSCALE_MODE_KEEP_ASPECTのときはViewのサイズ変更を行う
 */
public class AspectScaledTextureView extends TransformTextureView
	implements TextureView.SurfaceTextureListener,
		IScaledView {

	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = AspectScaledTextureView.class.getSimpleName();

	protected final Matrix mImageMatrix = new Matrix();
	/**
	 * スケールモード
	 */
	@ScaleMode
	private int mScaleMode;
	/**
	 * 表示内容のアスペクト比 (幅 ÷ 高さ)
	 * 0以下なら無視される
	 */
	private double mRequestedAspect;
	/**
	 * スケールモードがキープアスペクトの場合にViewのサイズをアスペクト比に合わせて変更するかどうか
	 */
	private boolean mNeedResizeToKeepAspect;
	/**
	 * プレビュー表示用のSurfaceTextureが存在しているかどうか
	 */
	private volatile boolean mHasSurface;
	/**
	 * SurfaceTextureListenerを自View内で使うため外部からセットされた
	 * SurfaceTextureListenerは自前で保持＆呼び出す
	 */
	private SurfaceTextureListener mListener;

	/**
	 * コンストラクタ
	 * @param context
	 */
	public AspectScaledTextureView(@NonNull final Context context) {
		this(context, null, 0);
	}

	/**
	 * コンストラクタ
	 * @param context
	 * @param attrs
	 */
	public AspectScaledTextureView(@NonNull final Context context,
		@Nullable final AttributeSet attrs) {

		this(context, attrs, 0);
	}

	/**
	 * コンストラクタ
	 * @param context
	 * @param attrs
	 * @param defStyleAttr
	 */
	public AspectScaledTextureView(@NonNull final Context context,
		@Nullable final AttributeSet attrs, final int defStyleAttr) {

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
		super.setSurfaceTextureListener(this);
	}

	/**
	 * アスペクト比を保つように大きさを決める
	 */
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//		if (DEBUG) Log.v(TAG, "onMeasure:mRequestedAspect=" + mRequestedAspect);
		final MeasureSpecDelegater.MeasureSpec spec
			= MeasureSpecDelegater.onMeasure(this,
				mRequestedAspect, mScaleMode, mNeedResizeToKeepAspect,
				widthMeasureSpec, heightMeasureSpec);
		super.onMeasure(spec.widthMeasureSpec, spec.heightMeasureSpec);
	}

	private int prevWidth = -1;
	private int prevHeight = -1;
	@Override
	protected void onLayout(final boolean changed,
		final int left, final int top, final int right, final int bottom) {

		super.onLayout(changed, left, top, right, bottom);
		if (DEBUG) Log.v(TAG, String.format("onLayout:(%dx%d)", getWidth(), getHeight()));
//		if view size(width|height) is zero(the view size not decided yet)
		if (getWidth() == 0 || getHeight() == 0) return;
		if (prevWidth != getWidth() || prevHeight != getHeight()) {
			prevWidth = getWidth();
			prevHeight = getHeight();
			onResize(prevWidth, prevHeight);
		}
		init();
	}
	
	/**
	 * @param listener
	 */
	@Override
	public final void setSurfaceTextureListener(final SurfaceTextureListener listener) {
		mListener = listener;
		// 自分自身を登録してあるのでsuper#setSurfaceTextureListenerは呼ばない
	}

	@Override
	public SurfaceTextureListener getSurfaceTextureListener() {
		// 自分自身を登録してあるのでsuper#getSurfaceTextureListenerは呼ばない
		return mListener;
	}

	protected void onResize(final int width, final int height) {
	}

	public boolean hasSurface() {
		return mHasSurface;
	}

//================================================================================
// SurfaceTextureListener
//================================================================================
	@Override
	public void onSurfaceTextureAvailable(@NonNull final SurfaceTexture surface, final int width, final int height) {
		mHasSurface = true;
		init();
		if (mListener != null) {
			mListener.onSurfaceTextureAvailable(surface, width, height);
		}
	}

	@Override
	public void onSurfaceTextureSizeChanged(@NonNull final SurfaceTexture surface, final int width, final int height) {
		if (mListener != null) {
			mListener.onSurfaceTextureSizeChanged(surface, width, height);
		}
	}

	@Override
	public boolean onSurfaceTextureDestroyed(@NonNull final SurfaceTexture surface) {
		mHasSurface = false;
		if (mListener != null) {
			mListener.onSurfaceTextureDestroyed(surface);
		}
		return false;
	}

	@Deprecated
	@Override
	public void onSurfaceTextureUpdated(@NonNull final SurfaceTexture surface) {
		if (mListener != null) {
			mListener.onSurfaceTextureUpdated(surface);
		}
	}

//================================================================================
// IScaledView
//================================================================================
	/**
	 * アスペクト比を設定する。アスペクト比=<code>幅 / 高さ</code>.
	 */
	@Override
	public void setAspectRatio(final double aspectRatio) {
//		if (DEBUG) Log.v(TAG, "setAspectRatio");
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
	public void setScaleMode(@ScaleMode final int scale_mode) {
		if (mScaleMode != scale_mode) {
			mScaleMode = scale_mode;
			requestLayout();
		}
	}

	@ScaleMode
	@Override
	public int getScaleMode() {
		return mScaleMode;
	}

	@Override
	public void setNeedResizeToKeepAspect(final boolean keepAspect) {
		if (mNeedResizeToKeepAspect != keepAspect) {
			mNeedResizeToKeepAspect = keepAspect;
			requestLayout();
		}
	}

//================================================================================
// 実際の実装
//================================================================================
	/**
	 * 拡大縮小回転状態をリセット
	 */
	protected void init() {
		// update image size
		// current implementation of ImageView always hold its image as a Drawable
		// (that can get ImageView#getDrawable)
		// therefore update the image size from its Drawable
		// set limit rectangle that the image can move
		final int viewWidth = getWidth();
		final int viewHeight = getHeight();
		// apply matrix
		mImageMatrix.reset();
		switch (mScaleMode) {
		case SCALE_MODE_STRETCH_TO_FIT:
			// 何もしない
			break;
		case SCALE_MODE_KEEP_ASPECT:
		case SCALE_MODE_CROP: // FIXME もう少し式を整理できそう
			final double contentWidth = mRequestedAspect > 0 ? mRequestedAspect * viewHeight : viewHeight;
			final double contentHeight = viewHeight;
			if (DEBUG) Log.v(TAG,
				String.format("init:" +
				 	"view(%dx%d),content(%.0fx%.0f),aspect=%f",
					viewWidth, viewHeight,
					contentWidth, contentHeight,
					mRequestedAspect));
			final double scaleX = viewWidth / contentWidth;
			final double scaleY = viewHeight / contentHeight;
			final double scale = (mScaleMode == SCALE_MODE_CROP)
				? Math.max(scaleX,  scaleY)		// SCALE_MODE_CROP
				: Math.min(scaleX, scaleY);		// SCALE_MODE_KEEP_ASPECT
			final double width = scale * contentWidth;
			final double height = scale * contentHeight;
			if (DEBUG) Log.v(TAG, String.format("init:scaleMode=%d,size(%1.0f,%1.0f),scale(%f,%f)→%f,mat(%f,%f)",
				mScaleMode,
				width, height,
				scaleX, scaleY, scale,
				width / viewWidth, height / viewHeight));
			mImageMatrix.postScale(
				(float)(width / viewWidth), (float)(height / viewHeight),
				viewWidth / 2.0f, viewHeight / 2.0f);
			break;
		}
		if (DEBUG) Log.v(TAG, "init:" + mImageMatrix);
		setTransform(mImageMatrix);
	}

}
