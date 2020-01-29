package com.serenegiant.widget;

/*
 * ZoomingImageView for Android:
 * Copyright(c) 2014-2020 t_saki@serenegiant.com
 *
 * This class extends ImageView to support zooming/draging/rotating of image with touch.
 * You can replace usual ImageView with this class.
 * 
 * File name: ZoomingImageView.java
*/
/*
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
/*
 * Usage:
 * Double touch and holds while more tha long press timeout, start rotating</br>
 * When start rotating, color reversing effect execute as a default visual effect</br>
 * You can cancel the default feedback and execute addition feedback in the callback listener</br>
 * 
 * Double touch and pinch in/out zoom the image in/out.
 * 
 * Single touch with move drags the image.
 * 
 * Single touch and hold while more than long press timeouit, reset the zooming/draging/rotaing
 * and fit the image in this view.</br>
 * You can reset zooming/moving/rotating with calling #reset programmatically
 * Limitation of this class:
 * This class internally use image matrix to zoom/drag/rotate image,
 * therefore you can not set matrix with #setImageMatrix.
 * If you set matrix, it is ignored and has no effect.
 * 
 * And the scaleType is fixed to ScaleType.MATRIX. If you set in xml or programmatically other than ScaleType.MATRIX,
 * it is ignored and has no effect.
 * 
 * This class requires API level >= 8
 */

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
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

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;

/**
 * ImageView with zooming/dragging/rotating image with touch
 */
public class ZoomImageView extends AppCompatImageView
	implements ViewTransformDelegater.ITransformView {

	private static final boolean DEBUG = false;	// TODO for debugging
	private static final String TAG = ZoomImageView.class.getSimpleName();

//--------------------------------------------------------------------------------
	private final ViewTransformDelegater mDelegater;
	/**
	 * Constructor for constructing in program
	 * @param context
	 */
	public ZoomImageView(final Context context) {
		this(context, null, 0);
	}

	/**
	 * Constructor for constructing from xml
	 * @param context
	 * @param attrs
	 */
	public ZoomImageView(final Context context, final AttributeSet attrs) {
		this(context, attrs, 0);
	}

	/**
	 * Constructor for constructing from xml
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
		mDelegater.clearCallbacks();
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

	/**
	 * set the Matrix for image zooming/transforming</br>
	 * this method ignore the parameter because ZoomImageView needs to set Matrix internally
	 */
	@Override
	public void setImageMatrix(final Matrix matrix) {

		super.setImageMatrix(mDelegater.getImageMatrix());
		Log.w(TAG, "setScaleType: ignore this parameter on ZoomImageView2.");
	}


	@Override
	public void setColorFilter(final ColorFilter cf) {
		// save the ColorFilter to restore after default visual feedback on start rotating
		mDelegater.setColorFilter(cf);
		super.setColorFilter(cf);
	}

//--------------------------------------------------------------------------------
	/**
	 * ITransformViewの実装
	 * @param state
	 */
	@Override
	public void onRestoreInstanceStateSp(final Parcelable state) {
		super.onRestoreInstanceState(state);
	}

	/**
	 * ITransformViewの実装
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
	 * @return
	 */
	@NonNull
	@Override
	public Rect getDrawingRect() {
		final Rect r = new Rect();
		super.getDrawingRect(r);
		return r;
	}

	/**
	 * ITransformViewの実装
	 * @return
	 */
	@Override
	public Matrix getImageMatrixSp() {
		return super.getImageMatrix();
	}

	/**
	 * ITransformViewの実装
	 * @param matrix
	 */
	@Override
	public void setImageMatrixSp(final Matrix matrix) {
		super.setImageMatrix(matrix);
	}

	/**
	 * ITransformViewの実装
	 * @param cf
	 */
	@Override
	public void setColorFilterSp(final ColorFilter cf) {
		super.setColorFilter(cf);
	}

	/**
	 * ITransformViewの実装
	 */
	@Override
	public void init() {
		mDelegater.init();
		super.setScaleType(ScaleType.MATRIX);
		super.setImageMatrix(mDelegater.getImageMatrix());
	}

	/**
	 * ITransformViewの実装
	 */
	@Override
	public void onInit() {
		// Scale the image uniformly (maintain the image's aspect ratio)
		// so that both dimensions (width and height) of the image will be equal
		// to or less than the corresponding dimension of the view (minus padding).
		// The image is then centered in the view
		// leave to super class
		super.setScaleType(ScaleType.CENTER_INSIDE);
		// the internal Matrix in the super class(that can get with ImageView#getImageMatrix)
		// never updated when called setScaleType on current implementation.
		// therefore call setFrame to update internal Matrix.
		// but the behavior may change in the future implementation...
		setFrame(getLeft(), getTop(), getRight(), getBottom());
	}

	/**
	 * ITransformViewの実装
	 * @param maxScale
	 */
	@Override
	public void setMaxScale(final float maxScale) {
		mDelegater.setMaxScale(maxScale);
	}

	/**
	 * ITransformViewの実装
	 * @param minScale
	 */
	@Override
	public void setMinScale(final float minScale) {
		mDelegater.setMinScale(minScale);
	}

	/**
	 * set listener on start rotating (for visual/sound feedback)
	 * @param listener
	 */
	@Override
	public void setOnStartRotationListener(
		final ViewTransformDelegater.OnStartRotationListener listener) {
		mDelegater.setOnStartRotationListener(listener);
	}
	
	/**
	 * return current listener
	 * @return
	 */
	@NonNull
	public ViewTransformDelegater.OnStartRotationListener getOnStartRotationListener() {
		return mDelegater.getOnStartRotationListener();
	}
	
	/**
	 * return current scale
	 * @return
	 */
	@Override
	public float getScale() {
		return mDelegater.getScale();
	}
	
	/**
	 * return current image translate values(offset)
	 * @param result
	 * @return
	 */
	@Override
	public PointF getTranslate(final PointF result) {
		return mDelegater.getTranslate(result);
	}
	
	/**
	 * get current rotating degrees
	 */
	@Override
	public float getRotation() {
		return mDelegater.getRotation();
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

}
