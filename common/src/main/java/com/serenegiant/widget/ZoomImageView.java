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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.AbsSavedState;
import android.view.MotionEvent;
import android.view.View;

import com.serenegiant.common.R;
import com.serenegiant.view.ViewTransformDelegater;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * 表示内容を拡大縮小回転平行移動できるImageView実装
 */
public class ZoomImageView extends TransformImageView
	implements ViewTransformDelegater.ViewTransformListener,
		IScaledView {

	private static final boolean DEBUG = false;	// TODO for debugging
	private static final String TAG = ZoomImageView.class.getSimpleName();

	/**
	 * 色反転後にもとに戻すまでの待機時間[ミリ秒]
	 */
	private static final int REVERSING_TIMEOUT = 100;

	/**
	 * 色を反転させるための色変換行列
	 */
	private static final float[] REVERSE = {
	    -1.0f,   0.0f,   0.0f,  0.0f,  255.0f,
	     0.0f,  -1.0f,   0.0f,  0.0f,  255.0f,
	     0.0f,   0.0f,  -1.0f,  0.0f,  255.0f,
	     0.0f,   0.0f,   0.0f,  1.0f,    0.0f,
	};

//--------------------------------------------------------------------------------
	private final ViewTransformDelegater mDelegater;

	/**
	 * ColorFilter to reverse the color of the image
	 * for default visual feedbak on start rotating
	 */
	private ColorFilter mColorReverseFilter;
	/**
	 * 色反転後に元に戻すためのオリジナルのカラーフィルターを保持
	 */
	private ColorFilter mSavedColorFilter;
	/**
	 * 一定時間後に色反転を元に戻すためのRunnable
	 */
	private WaitReverseReset mWaitReverseReset;

	@Nullable
	private ViewTransformDelegater.ViewTransformListener mViewTransformListener;

	/**
	 * コンストラクタ
	 * @param context
	 */
	public ZoomImageView(final Context context) {
		this(context, null, 0);
	}

	/**
	 * コンストラクタ
	 * @param context
	 * @param attrs
	 */
	public ZoomImageView(final Context context, final AttributeSet attrs) {
		this(context, attrs, 0);
	}

	/**
	 * コンストラクタ
	 * @param context
	 * @param attrs
	 * @param defStyle
	 */
	public ZoomImageView(final Context context, final AttributeSet attrs, final int defStyle) {
		super(context, attrs, defStyle);
		if (DEBUG) Log.v(TAG, "コンストラクタ:");
		TypedArray a = context.getTheme().obtainStyledAttributes(
			attrs, R.styleable.IScaledView, defStyle, 0);
		double requestedAspect = -1.0;
		int scaleMode = SCALE_MODE_KEEP_ASPECT;
		try {
			requestedAspect = a.getFloat(
				R.styleable.IScaledView_aspect_ratio, -1.0f);
			scaleMode = a.getInt(
				R.styleable.IScaledView_scale_mode, SCALE_MODE_KEEP_ASPECT);
		} catch (final UnsupportedOperationException e) {
			Log.d(TAG, TAG, e);
		} finally {
			a.recycle();
		}

		mDelegater = new ViewTransformDelegater(this) {
			@Override
			protected void setTransform(@NonNull final View view, @Nullable final Matrix transform) {
				if (DEBUG) Log.v(TAG, "setTransform:" + transform);
				superSetImageMatrix(transform);
			}

			@NonNull
			@Override
			protected Matrix getTransform(@NonNull final View view, @Nullable final Matrix transform) {
				if (DEBUG) {
					final Matrix result = superGetImageMatrix(transform);
					if (DEBUG) Log.v(TAG, "getTransform:" + result);
					return result;
				} else {
					return superGetImageMatrix(transform);
				}
			}

			/**
			 * ITransformViewの実装
			 * View表示内容の大きさを取得
			 * @return
			 */
			@Override
			public RectF getContentBounds() {
				final RectF result;
				final Drawable dr = getDrawable();
				if (dr != null) {
					result = new RectF(dr.getBounds());
				} else {
					result = new RectF();
				}
				if (DEBUG) Log.v(TAG, "getContentBounds:" + result);
				return result;
			}

			/**
			 * ITransformViewの実装
			 * View表内容の拡大縮小回転平行移動を初期化時の追加処理
			 */
			@Override
			public void onInit() {
				if (DEBUG) Log.v(TAG, "onInit:");
				// 拡大縮小率のデフォルト値を取得するためにImageView自体にトランスフォームマトリックスを計算させる
				// CENTER_INSIDEにすればアスペクト比を維持した状態で画像全体が表示される
				// CENTER_CROPにすればアスペクト比を維持してView全体に映像が表示される
				// 　　Viewのアスペクト比と画像のアスペクト比が異なれば上下または左右のいずれかが見切れる
				superSetScaleType(ScaleType.CENTER_INSIDE);
				// ImageView#setScaleTypeを呼んだだけではトランスフォームマトリックスが更新されないので
				// ImageView#setFrameを呼んで強制的にトランスフォームマトリックスを計算させる
				setFrame(getLeft(), getTop(), getRight(), getBottom());
			}
		};
		mDelegater.setScaleMode(scaleMode);
		setViewTransformer(mDelegater);
		setAspectRatio(requestedAspect);
	}

	@Override
	protected void onDetachedFromWindow() {
		if (DEBUG) Log.v(TAG, "onDetachedFromWindow:");
		mDelegater.clearPendingTasks();
		super.onDetachedFromWindow();
	}

	@Override
	protected Parcelable onSaveInstanceState() {
		if (DEBUG) Log.v(TAG, "onSaveInstanceState:");

		return mDelegater.onSaveInstanceState(super.onSaveInstanceState());
	}

	@Override
	protected void onRestoreInstanceState(final Parcelable state) {
		if (DEBUG) Log.v(TAG, "onRestoreInstanceState:");

		super.onRestoreInstanceState(state);
		if (state instanceof AbsSavedState) {
			super.onRestoreInstanceState(((AbsSavedState) state).getSuperState());
		}
		mDelegater.onRestoreInstanceState(state);
	}

	@Override
	protected void onConfigurationChanged(final Configuration newConfig) {
		if (DEBUG) Log.v(TAG, "onConfigurationChanged:" + newConfig);

		super.onConfigurationChanged(newConfig);
		mDelegater.onConfigurationChanged(newConfig);
	}

	@Override
	protected void onLayout(final boolean changed,
		final int left, final int top, final int right, final int bottom) {
		
		super.onLayout(changed, left, top, right, bottom);

		// if view size(width|height) is zero(the view size not decided yet)
		// or no image assigned, skip initialization
		if (getWidth() == 0 || getHeight() == 0 || !hasImage()) return;
		
		if (DEBUG) Log.v(TAG, String.format("onLayout:(%d,%d)-(%d,%d),changed=",
			left, top, right, bottom) + changed);
		init();
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouchEvent(final MotionEvent event) {

		// if there is no image, leave to super class
		if (!hasImage()) return super.onTouchEvent(event);

		if (mDelegater.onTouchEvent(event)) {
			return true;
		}

		return super.onTouchEvent(event);
	}

//--------------------------------------------------------------------------------
// ImageViewをoverride
	/**
	 * set the scale type</br>
	 * this method ignore the parameter because this class always needs to set ScaleType.MATRIX internally.
	 */
	@Override
	public void setScaleType(final ScaleType scaleType) {

		super.setScaleType(ScaleType.MATRIX);
		Log.w(TAG, "setScaleType: ignore this parameter on ZoomImageView2, fixed to ScaleType.MATRIX.");
	}

	@Override
	public void setColorFilter(final ColorFilter cf) {
		super.setColorFilter(cf);
		// save the ColorFilter to restore after default visual feedback on start rotating
		mSavedColorFilter = cf;
	}

//--------------------------------------------------------------------------------
	/**
	 * whether ImageView has image
	 * @return true if ImageView has image, false otherwise
	 */
	public boolean hasImage() {
		return getDrawable() != null;
	}

	/**
	 * get new Bitmap image that currently displayed on this view(applied zooming/moving/rotating).
	 * @return
	 */
	public Bitmap getCurrentImage() {
		final Bitmap offscreen = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
		final Canvas canvas = new Canvas(offscreen);
		// modified to support drawables other than BitmapDrawable
		canvas.setMatrix(super.getImageMatrix());
		super.getDrawable().draw(canvas);
		return offscreen;
	}

	/**
	 * get new partial Bitmap image from currently displayed on this view(applied zooming/moving/rotating)
	 * @param frame: framing rectangle that you want to cut image from the view (as view coordinates)
	 * @return
	 */
	public Bitmap getCurrentImage(final Rect frame) {
		Bitmap image = getCurrentImage();
		if ((frame != null) && !frame.isEmpty()) {
			final Bitmap tmp = Bitmap.createBitmap(image,
				frame.left, frame.top, frame.width(), frame.height(), null, false);
			image.recycle();
			image = tmp;
		}
		return image;
	}

	/**
	 * タッチ操作の有効無効設定
	 * @param enabled
	 */
	public void setEnableHandleTouchEvent(@ViewTransformDelegater.TouchMode final int enabled) {
		mDelegater.setEnableHandleTouchEvent(enabled);
	}

	/**
	 * タッチ操作の有効無効設定を取得
	 * @return
	 */
	@ViewTransformDelegater.TouchMode
	public int getEnableHandleTouchEvent() {
		return mDelegater.getEnableHandleTouchEvent();
	}

	/**
	 * 最大拡大率を設定
	 * @param maxScale
	 */
	public void setMaxScale(final float maxScale)
		throws IllegalArgumentException {

		mDelegater.setMaxScale(maxScale);
	}

	/**
	 * 現在の最大拡大率を取得
	 * @return
	 */
	public float getMaxScale() {
		return mDelegater.getMaxScale();
	}

	/**
	 * 最小縮小率を設定
	 * @param minScale
	 */
	public void setMinScale(final float minScale)
		throws IllegalArgumentException {

		mDelegater.setMinScale(minScale);
	}

	/**
	 * 現在の最小縮小率を取得
	 * @return
	 */
	public float getMinScale() {
		return mDelegater.getMinScale();
	}

	/**
	 * 回転処理開始時のコールバックリスナー(ユーザーフィードバック用)を設定
	 * @param listener
	 */
	public void setViewTransformListener(
		final ViewTransformDelegater.ViewTransformListener listener) {
		mViewTransformListener = listener;
	}

	/**
	 * 現在設定されている回転処理開始時のコールバックリスナーを取得
	 * @return
	 */
	@Nullable
	public ViewTransformDelegater.ViewTransformListener getViewTransformListener() {
		return mViewTransformListener;
	}

	/**
	 * 現在の拡大縮小率を取得
	 * FIXME 一度も手動で拡大縮小移動処理を行っていないときに常に1.0fになってしまう
	 * @return
	 */
	public float getScale() {
		return mDelegater.getScale();
	}

	/**
	 * 拡大縮小率を設定
	 * @param scale
	 */
	public void setScale(final float scale) {
		mDelegater.setScale(scale);
	}

	/**
	 * 拡大縮小率を相対値で設定
	 * @param scaleDelta
	 */
	public void setScaleRelative(final float scaleDelta) {
		mDelegater.setScaleRelative(scaleDelta);
	}

	/**
	 * 現在のView(の表示内容)並行移動量(オフセット)を取得
	 * @param result
	 * @return
	 */
	@NonNull
	public PointF getTranslate(@NonNull final PointF result) {
		return mDelegater.getTranslate(result);
	}

	/**
	 * ITransformViewの実装
	 * 現在のView表示内容の回転角度を取得
	 */
	@Override
	public float getRotation() {
		return mDelegater.getRotation();
	}

//--------------------------------------------------------------------------------
// IScaledView
	private double mRequestedAspect = -1.0;
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

	/**
	 * IScaledViewの実装
	 * 拡大縮小方法をセット
	 * @param scaleMode SCALE_MODE_KEEP_ASPECT, SCALE_MODE_STRETCH, SCALE_MODE_CROP
	 */
	@Override
	public void setScaleMode(@ScaleMode final int scaleMode) {
		mDelegater.setScaleMode(scaleMode);
	}

	/**
	 * IScaledViewの実装
	 * 現在の拡大縮小方法を取得
	 * @return
	 */
	@ScaleMode
	@Override
	public int getScaleMode() {
		return mDelegater.getScaleMode();
	}

	@Override
	public void setNeedResizeToKeepAspect(final boolean keepAspect) {
		mDelegater.setKeepAspect(keepAspect);
	}

//--------------------------------------------------------------------------------
	/**
	 * View表内容の拡大縮小回転平行移動を初期化
	 */
	private void init() {
		if (DEBUG) Log.v(TAG, "init:");
		mDelegater.init();
		superSetScaleType(ScaleType.MATRIX);
		superSetImageMatrix(mDelegater.getTransform(null));
	}

	/**
	 * 一定時間後に色反転を元に戻すためのRunnable実装
	 */
	private final class WaitReverseReset implements Runnable {
		@Override
		public void run() {
			resetColorFilter();
		}
	}

	/**
	 * オリジナルのカラーフィルターを適用
	 */
	private void resetColorFilter() {
		super.setColorFilter(mSavedColorFilter);
	}

	/**
	 * ViewTransformListenerの実装
	 * @param view
	 * @param newState
	 */
	@Override
	public void onStateChanged(@NonNull final View view, final int newState) {
		if (DEBUG) Log.v(TAG, "onStateChanged:" + newState);
		switch (newState) {
		case ViewTransformDelegater.STATE_ROTATING:
			if (mColorReverseFilter == null) {
				mColorReverseFilter = new ColorMatrixColorFilter(new ColorMatrix(REVERSE));
			}
			super.setColorFilter(mColorReverseFilter);
			// post runnable to reset the color reversing
			if (mWaitReverseReset == null) mWaitReverseReset = new WaitReverseReset();
			postDelayed(mWaitReverseReset, REVERSING_TIMEOUT);
			break;
		case ViewTransformDelegater.STATE_NON:
			resetColorFilter();
			break;
		}
		if (mViewTransformListener != null) {
			mViewTransformListener.onStateChanged(view, newState);
		}
	}

	/**
	 * ViewTransformListenerの実装
	 * @param view
	 * @param transform
	 */
	@Override
	public void onTransformed(@NonNull final View view, @NonNull final Matrix transform) {
		if (DEBUG) Log.v(TAG, "onTransformed:" + transform);
		if (mViewTransformListener != null) {
			mViewTransformListener.onTransformed(view, transform);
		}
	}

}
