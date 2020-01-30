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
import android.content.res.Configuration;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

/**
 * 表示内容を拡大縮小回転平行移動できるImageView実装
 */
public class ZoomImageView extends AppCompatImageView
	implements ViewTransformDelegater.ITransformView,
	ViewTransformDelegater.ViewTransformListener {

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
		mDelegater = new ViewTransformDelegater(this);
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
		
		if (DEBUG) Log.v(TAG, String.format("onLayout:(%d,%d)-(%d,%d)",
			left, top, right, bottom));
		// set the scale type to ScaleType.MATRIX
		mDelegater.onLayout(changed, left, top, right, bottom);
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouchEvent(final MotionEvent event) {

		// if there is no image, leave to super class
		if (!hasImage()) return super.onTouchEvent(event);

		if (DEBUG) Log.v(TAG, "onTouchEvent:");

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
	 * 最大拡大率を設定
	 * @param maxScale
	 */
	public void setMaxScale(final float maxScale) {
		mDelegater.setMaxScale(maxScale);
	}

	/**
	 * 最小縮小率を設定
	 * @param minScale
	 */
	public void setMinScale(final float minScale) {
		mDelegater.setMinScale(minScale);
	}

	/**
	 * 回転処理開始時のコールバックリスナー(ユーザーフィードバック用)を設定
	 * @param listener
	 */
	public void setViewTransformListener(
		final ViewTransformDelegater.ViewTransformListener listener) {
		mDelegater.setOnStartRotationListener(listener);
	}

	/**
	 * 現在設定されている回転処理開始時のコールバックリスナーを取得
	 * @return
	 */
	@Nullable
	public ViewTransformDelegater.ViewTransformListener getViewTransformListener() {
		return mDelegater.getOnStartRotationListener();
	}

	/**
	 * 現在の拡大縮小率を取得
	 * @return
	 */
	public float getScale() {
		return mDelegater.getScale();
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
	/**
	 * ITransformViewの実装
	 * @return
	 */
	@NonNull
	@Override
	public View getView() {
		return this;
	}

	/**
	 * ITransformViewの実装
	 * View表示内容の大きさを取得
	 * @return
	 */
	@Override
	public RectF getBounds() {
		final RectF result;
		final Drawable dr = getDrawable();
		if (dr != null) {
			result = new RectF(dr.getBounds());
		} else {
			result = new RectF();
		}
		return result;
	}

	/**
	 * ITransformViewの実装
	 * Viewのsuper#getImageMatrixを呼び出す
	 * @return
	 */
	@Override
	public Matrix getTransform(@Nullable final Matrix transform) {
		if (transform != null) {
			transform.set(getImageMatrix());
			return transform;
		} else {
			return new Matrix(getImageMatrix());
		}
	}

	/**
	 * ITransformViewの実装
	 * Viewのsuper#setImageMatrixを呼び出す
	 * @param matrix
	 */
	@Override
	public void setTransform(final Matrix matrix) {
		setImageMatrix(matrix);
	}

	/**
	 * ITransformViewの実装
	 * View表内容の拡大縮小回転平行移動を初期化
	 */
	@Override
	public void init() {
		mDelegater.init();
		super.setScaleType(ScaleType.MATRIX);
		super.setImageMatrix(mDelegater.getImageMatrix());
	}

	/**
	 * ITransformViewの実装
	 * View表内容の拡大縮小回転平行移動を初期化時の追加処理
	 */
	@Override
	public void onInit() {
		// 拡大縮小率のデフォルト値を取得するためにImageView自体にトランスフォームマトリックスを計算させる
		// CENTER_INSIDEにすればアスペクト比を維持した状態で画像全体が表示される
		// CENTER_CROPにすればアスペクト比を維持してView全体に映像が表示される
		// 　　Viewのアスペクト比と画像のアスペクト比が異なれば上下または左右のいずれかが見切れる
		super.setScaleType(ScaleType.CENTER_INSIDE);
		// ImageView#setScaleTypeを呼んだだけではトランスフォームマトリックスが更新されないので
		// ImageView#setFrameを呼んで強制的にトランスフォームマトリックスを計算させる
		setFrame(getLeft(), getTop(), getRight(), getBottom());
	}

//--------------------------------------------------------------------------------
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
	 * whether ImageView has image
	 * @return true if ImageView has image, false otherwise
	 */
	private boolean hasImage() {
		return getDrawable() != null;
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
	 */
	@Override
	public void onStartRotation(final ViewTransformDelegater.ITransformView view) {
		if (mColorReverseFilter == null) {
			mColorReverseFilter = new ColorMatrixColorFilter(new ColorMatrix(REVERSE));
		}
		super.setColorFilter(mColorReverseFilter);
		// post runnable to reset the color reversing
		if (mWaitReverseReset == null) mWaitReverseReset = new WaitReverseReset();
		postDelayed(mWaitReverseReset, REVERSING_TIMEOUT);
	}

	/**
	 * ViewTransformListenerの実装
	 * @param view
	 * @param newState
	 */
	@Override
	public void onStateChanged(final ViewTransformDelegater.ITransformView view,
		final int newState) {

		if (newState == ViewTransformDelegater.STATE_NON) {
			resetColorFilter();
		}
	}

}
