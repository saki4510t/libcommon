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
import android.content.res.TypedArray;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.AbsSavedState;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;

import com.serenegiant.common.R;
import com.serenegiant.glutils.IRendererCommon;
import com.serenegiant.view.MeasureSpecDelegater;
import com.serenegiant.view.ViewTransformDelegater;

import static com.serenegiant.view.ViewTransformDelegater.*;

/**
 * FIXME 未実装 ViewTransformDelegaterを使って拡大縮小回転平行移動をするTextureView
 * FIXME まだうまく動かにゃい
 */
public class ZoomAspectScaledTextureView2
	extends TransformTextureView implements IRendererCommon,
		TextureView.SurfaceTextureListener,
		ViewTransformDelegater.ViewTransformListener,
		IScaledView {

	private static final boolean DEBUG = false;	// TODO for debugging
	private static final String TAG = ZoomAspectScaledTextureView2.class.getSimpleName();

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
	 * タッチ操作の有効無効設定
	 */
	@TouchMode
	private int mHandleTouchEvent;
	/**
	 * ミラーモード
	 */
	@MirrorMode
    private int mMirrorMode = MIRROR_NORMAL;
	/**
	 * スケールモード
	 */
	@ScaleMode
	private int mScaleMode;
	/**
	 * 表示内容のアスペクト比
	 * 0以下なら無視される
	 */
	private double mRequestedAspect;
	/**
	 * スケールモードがキープアスペクトの場合にViewのサイズをアスペクト比に合わせて変更するかどうか
	 */
	private boolean mNeedResizeToKeepAspect;

	private final ViewTransformDelegater mDelegater;

	/**
	 * コンストラクタ
	 * @param context
	 */
	public ZoomAspectScaledTextureView2(final Context context) {
		this(context, null, 0);
	}

	/**
	 * コンストラクタ
	 * @param context
	 * @param attrs
	 */
	public ZoomAspectScaledTextureView2(final Context context, final AttributeSet attrs) {
		this(context, attrs, 0);
	}

	/**
	 * コンストラクタ
	 * @param context
	 * @param attrs
	 * @param defStyleAttr
	 */
	public ZoomAspectScaledTextureView2(final Context context, final AttributeSet attrs, final int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		if (DEBUG) Log.v(TAG, "コンストラクタ:");
		final TypedArray a = context.getTheme().obtainStyledAttributes(
			attrs, R.styleable.IScaledView, defStyleAttr, 0);
		try {
			// getIntegerは整数じゃなければUnsupportedOperationExceptionを投げる
			mHandleTouchEvent = a.getInteger(R.styleable.ZoomAspectScaledTextureView_handle_touch_event, TOUCH_ENABLED_ALL);
			mRequestedAspect = a.getFloat(
				R.styleable.IScaledView_aspect_ratio, -1.0f);
			mScaleMode = a.getInt(
				R.styleable.IScaledView_scale_mode, SCALE_MODE_KEEP_ASPECT);
			mNeedResizeToKeepAspect = a.getBoolean(
				R.styleable.IScaledView_resize_to_keep_aspect, true);
		} catch (final UnsupportedOperationException e) {
			final boolean b = a.getBoolean(R.styleable.ZoomAspectScaledTextureView_handle_touch_event, true);
			mHandleTouchEvent = b ? TOUCH_ENABLED_ALL : TOUCH_DISABLED;
		} finally {
			a.recycle();
		}
		super.setSurfaceTextureListener(this);
		mDelegater = new ViewTransformDelegater(this, getViewTransformer()) {
			@Override
			public RectF getContentBounds() {
				return null;
			}

			@Override
			public void onInit() {

			}
		};
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

	@Override
	protected void onLayout(final boolean changed, final int left, final int top, final int right, final int bottom) {
		super.onLayout(changed, left, top, right, bottom);
//		if (DEBUG) Log.v(TAG, "onLayout:width=" + getWidth() + ",height=" + getHeight());
//		if view size(width|height) is zero(the view size not decided yet)
		if (getWidth() == 0 || getHeight() == 0) return;
		init();
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

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouchEvent(final MotionEvent event) {

		if (handleOnTouchEvent(event)) {
			return true;	// 処理済み
		}

		if (mHandleTouchEvent == TOUCH_DISABLED) {
			return super.onTouchEvent(event);
		}

//		if (DEBUG) Log.v(TAG, "onTouchEvent:");

		if (mDelegater.onTouchEvent(event)) {
			return true;
		}

		return super.onTouchEvent(event);
	}


	public boolean hasSurface() {
		return mHasSurface;
	}

//================================================================================
// SurfaceTextureListener
//================================================================================
	@Override
	public void onSurfaceTextureAvailable(final SurfaceTexture surface, final int width, final int height) {
		mHasSurface = true;
		setMirror(MIRROR_NORMAL);	// デフォルトだから適用しなくていいけど
		init();
		if (mListener != null) {
			mListener.onSurfaceTextureAvailable(surface, width, height);
		}
	}

	@Override
	public void onSurfaceTextureSizeChanged(final SurfaceTexture surface, final int width, final int height) {
		applyMirrorMode();
		if (mListener != null) {
			mListener.onSurfaceTextureSizeChanged(surface, width, height);
		}
	}

	@Override
	public boolean onSurfaceTextureDestroyed(final SurfaceTexture surface) {
		mHasSurface = false;
		if (mListener != null) {
			mListener.onSurfaceTextureDestroyed(surface);
		}
		return false;
	}

	@Deprecated
	@Override
	public void onSurfaceTextureUpdated(final SurfaceTexture surface) {
		if (mListener != null) {
			mListener.onSurfaceTextureUpdated(surface);
		}
	}
//================================================================================
	@Override
	public void setMirror(@MirrorMode final int mirror) {
		if (DEBUG) Log.v(TAG, "setMirror:" + mirror);
		if (mMirrorMode != mirror) {
			mMirrorMode = mirror;
			applyMirrorMode();
		}
	}

	@Override
	@MirrorMode
	public int getMirror() {
		return mMirrorMode;
	}

	@Override
	public void onStartRotation(final View view) {
		if (DEBUG) Log.v(TAG, "onStartRotation:");
		// FIXME 未実装
	}

	@Override
	public void onStateChanged(final View view, final int newState) {
		if (DEBUG) Log.v(TAG, "onStateChanged:" + newState);
		// FIXME 未実装
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

//================================================================================
	protected void init() {
		if (DEBUG) Log.v(TAG, "init:");
		mDelegater.init();
	}

	protected boolean handleOnTouchEvent(final MotionEvent event) {
//		if (DEBUG) Log.v(TAG, "handleOnTouchEvent:" + event);
		return false;
	}

	/**
	 * ミラーモードをTextureViewに適用
	 */
	private void applyMirrorMode() {
		if (DEBUG) Log.v(TAG, "applyMirrorMode");
		switch (mMirrorMode) {
		case MIRROR_HORIZONTAL:
			setScaleX(-1.0f);
			setScaleY(1.0f);
			break;
		case MIRROR_VERTICAL:
			setScaleX(1.0f);
			setScaleY(-1.0f);
			break;
		case MIRROR_BOTH:
			setScaleX(-1.0f);
			setScaleY(-1.0f);
			break;
		case MIRROR_NORMAL:
		default:
			setScaleX(1.0f);
			setScaleY(1.0f);
			break;
		}
	}

//================================================================================
// IAspectRatioView
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

//================================================================================
// IScaledView
//================================================================================

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
}
