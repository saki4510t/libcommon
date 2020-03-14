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
import android.graphics.Matrix;
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
import com.serenegiant.view.IViewTransformer;
import com.serenegiant.view.MeasureSpecDelegater;
import com.serenegiant.view.ViewTransformDelegater;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static com.serenegiant.view.ViewTransformDelegater.*;

/**
 * ViewTransformDelegaterを使って拡大縮小回転平行移動をするTextureView
 * FIXME まだ挙動がおかしい
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
	 * ミラーモード
	 */
	@MirrorMode
    private int mMirrorMode = MIRROR_NORMAL;
	/**
	 * スケールモードがキープアスペクトの場合にViewのサイズをアスペクト比に合わせて変更するかどうか
	 */
	private boolean mNeedResizeToKeepAspect;

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
		@TouchMode
		int handleTouchEvent;
		TypedArray a = context.getTheme().obtainStyledAttributes(
			attrs, R.styleable.IScaledView, defStyleAttr, 0);
		double requestedAspect = -1.0;
		int scaleMode = SCALE_MODE_KEEP_ASPECT;
		try {
			requestedAspect = a.getFloat(
				R.styleable.IScaledView_aspect_ratio, -1.0f);
			scaleMode = a.getInt(
				R.styleable.IScaledView_scale_mode, SCALE_MODE_KEEP_ASPECT);
			mNeedResizeToKeepAspect = a.getBoolean(
				R.styleable.IScaledView_resize_to_keep_aspect, true);
		} catch (final UnsupportedOperationException e) {
			Log.d(TAG, TAG, e);
		} finally {
			a.recycle();
		}
		a = context.getTheme().obtainStyledAttributes(
			attrs, R.styleable.ZoomAspectScaledTextureView, defStyleAttr, 0);
		try {
			// getIntegerは整数じゃなければUnsupportedOperationExceptionを投げる
			handleTouchEvent = a.getInteger(
				R.styleable.ZoomAspectScaledTextureView_handle_touch_event, TOUCH_ENABLED_ALL);
		} catch (final UnsupportedOperationException e) {
			Log.d(TAG, TAG, e);
			final boolean b = a.getBoolean(R.styleable.ZoomAspectScaledTextureView_handle_touch_event, true);
			handleTouchEvent = b ? TOUCH_ENABLED_ALL : TOUCH_DISABLED;
		} finally {
			a.recycle();
		}

		super.setSurfaceTextureListener(this);
		ViewTransformDelegater delegater = new ViewTransformDelegater(this) {
			@Override
			protected void setTransform(@NonNull final View view, @Nullable final Matrix transform) {
				if (DEBUG) Log.v(TAG, "setTransform:" + transform);
				superSetTransform(transform);
			}

			@NonNull
			@Override
			protected Matrix getTransform(@NonNull final View view, @Nullable final Matrix transform) {
				if (DEBUG) {
					final Matrix result = superGetTransform(transform);
					if (DEBUG) Log.v(TAG, "getTransform:" + result);
					return result;
				} else {
					return superGetTransform(transform);
				}
			}

			@Override
			public RectF getContentBounds() {
				if (DEBUG) Log.v(TAG, "getContentBounds:");
				return ZoomAspectScaledTextureView2.this.getContentBounds();
			}

			@Override
			public void onInit() {
				if (DEBUG) Log.v(TAG, "onInit:");
			}
		};
		delegater.setScaleMode(scaleMode);
		delegater.setEnableHandleTouchEvent(handleTouchEvent);
		setViewTransformer(delegater);
		setAspectRatio(requestedAspect);
	}

	/**
	 * アスペクト比を保つように大きさを決める
	 */
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		if (DEBUG) Log.v(TAG, "onMeasure:mRequestedAspect=" + getAspectRatio());
		final MeasureSpecDelegater.MeasureSpec spec
			= MeasureSpecDelegater.onMeasure(this,
				getAspectRatio(), getScaleMode(), mNeedResizeToKeepAspect,
				widthMeasureSpec, heightMeasureSpec);
		super.onMeasure(spec.widthMeasureSpec, spec.heightMeasureSpec);
	}

	@Override
	protected void onLayout(final boolean changed, final int left, final int top, final int right, final int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		if (DEBUG) Log.v(TAG, String.format("onLayout:(%d,%d)-(%d,%d)",
			left, top, right, bottom));
//		if view size(width|height) is zero(the view size not decided yet)
		if (getWidth() == 0 || getHeight() == 0) return;
		init();
	}

	@Override
	protected void onDetachedFromWindow() {
		if (DEBUG) Log.v(TAG, "onDetachedFromWindow:");
		final IViewTransformer transformer = getViewTransformer();
		if (transformer instanceof ViewTransformDelegater) {
			((ViewTransformDelegater) transformer).clearPendingTasks();
		}
		super.onDetachedFromWindow();
	}

	@Override
	protected Parcelable onSaveInstanceState() {
		if (DEBUG) Log.v(TAG, "onSaveInstanceState:");

		final IViewTransformer transformer = getViewTransformer();
		if (transformer instanceof ViewTransformDelegater) {
			return ((ViewTransformDelegater) transformer).onSaveInstanceState(super.onSaveInstanceState());
		} else {
			return super.onSaveInstanceState();
		}
	}

	@Override
	protected void onRestoreInstanceState(final Parcelable state) {
		if (DEBUG) Log.v(TAG, "onRestoreInstanceState:");

		super.onRestoreInstanceState(state);
		if (state instanceof AbsSavedState) {
			super.onRestoreInstanceState(((AbsSavedState) state).getSuperState());
		}
		final IViewTransformer transformer = getViewTransformer();
		if (transformer instanceof ViewTransformDelegater) {
			((ViewTransformDelegater) transformer).onRestoreInstanceState(state);
		}
	}

	@Override
	protected void onConfigurationChanged(final Configuration newConfig) {
		if (DEBUG) Log.v(TAG, "onConfigurationChanged:" + newConfig);

		super.onConfigurationChanged(newConfig);
		final IViewTransformer transformer = getViewTransformer();
		if (transformer instanceof ViewTransformDelegater) {
			((ViewTransformDelegater) transformer).onConfigurationChanged(newConfig);
		}
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouchEvent(final MotionEvent event) {

		if (handleOnTouchEvent(event)) {
			return true;	// 処理済み
		}

		final IViewTransformer transformer = getViewTransformer();
		if ((transformer instanceof ViewTransformDelegater)
			&& ((ViewTransformDelegater) transformer).onTouchEvent(event)) {

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
	public void onStateChanged(@NonNull final View view, final int newState) {
		if (DEBUG) Log.v(TAG, "onStateChanged:" + newState);
		// FIXME 未実装
	}

	@Override
	public void onTransformed(@NonNull final View view, @NonNull final Matrix transform) {
		if (DEBUG) Log.v(TAG, "onTransformed:" + transform);
	}

	/**
	 * タッチ操作の有効無効設定
	 * @param enabled
	 */
	public void setEnableHandleTouchEvent(@ViewTransformDelegater.TouchMode final int enabled) {
		final IViewTransformer transformer = getViewTransformer();
		if (transformer instanceof ViewTransformDelegater) {
			((ViewTransformDelegater) transformer).setEnableHandleTouchEvent(enabled);
		}
	}

	/**
	 * タッチ操作の有効無効設定を取得
	 * @return
	 */
	@ViewTransformDelegater.TouchMode
	public int getEnableHandleTouchEvent() {
		final IViewTransformer transformer = getViewTransformer();
		if (transformer instanceof ViewTransformDelegater) {
			return ((ViewTransformDelegater) transformer).getEnableHandleTouchEvent();
		} else {
			return TOUCH_ENABLED_ALL;
		}
	}

//================================================================================
	protected void init() {
		if (DEBUG) Log.v(TAG, "init:");
		final IViewTransformer transformer = getViewTransformer();
		if (transformer instanceof ViewTransformDelegater) {
			((ViewTransformDelegater) transformer).init();
		}
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

	protected RectF getContentBounds() {
		if (DEBUG) Log.v(TAG, "getContentBounds:");
		final float viewHeight = getHeight();
		return new RectF(0, 0,
			mRequestedAspect > 0 ? (float)(mRequestedAspect * viewHeight) : getWidth(),
			viewHeight);
	}

//================================================================================
// IScaledView
//================================================================================
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

	@Override
	public void setScaleMode(@ScaleMode final int scaleMode) {
		final IViewTransformer transformer = getViewTransformer();
		if (transformer instanceof ViewTransformDelegater) {
			((ViewTransformDelegater) transformer).setScaleMode(scaleMode);
		}
	}

	@ScaleMode
	@Override
	public int getScaleMode() {
		final IViewTransformer transformer = getViewTransformer();
		if (transformer instanceof ViewTransformDelegater) {
			return ((ViewTransformDelegater) transformer).getScaleMode();
		} else {
			return SCALE_MODE_STRETCH_TO_FIT;
		}
	}
}
