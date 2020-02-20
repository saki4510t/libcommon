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
import android.graphics.SurfaceTexture;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.AbsSavedState;
import android.view.MotionEvent;
import android.view.View;

import com.serenegiant.common.R;
import com.serenegiant.glutils.IRendererCommon;
import com.serenegiant.view.ViewTransformDelegater;
import com.serenegiant.view.ViewTransformer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static com.serenegiant.view.ViewTransformDelegater.*;

/**
 * FIXME 未実装 ViewTransformDelegaterを使って拡大縮小回転平行移動をするTextureView
 * FIXME まだうまく動かにゃい
 */
public class ZoomAspectScaledTextureView2
	extends AspectScaledTextureView implements IRendererCommon,
		ITransformView,
		ViewTransformDelegater.ViewTransformListener {

	private static final boolean DEBUG = false;	// TODO for debugging
	private static final String TAG = ZoomAspectScaledTextureView2.class.getSimpleName();


	/**
	 * タッチ操作の有効無効設定
	 */
	@TouchMode
	private int mHandleTouchEvent;
	@MirrorMode
    private int mMirrorMode = MIRROR_NORMAL;

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
		final TypedArray a
			= context.getTheme()
				.obtainStyledAttributes(attrs, R.styleable.ZoomAspectScaledTextureView, defStyleAttr, 0);
		try {
			// getIntegerは整数じゃなければUnsupportedOperationExceptionを投げる
			mHandleTouchEvent = a.getInteger(R.styleable.ZoomAspectScaledTextureView_handle_touch_event, TOUCH_ENABLED_ALL);
		} catch (final UnsupportedOperationException e) {
			final boolean b = a.getBoolean(R.styleable.ZoomAspectScaledTextureView_handle_touch_event, true);
			mHandleTouchEvent = b ? TOUCH_ENABLED_ALL : TOUCH_DISABLED;
		} finally {
			a.recycle();
		}
		mDelegater = new ViewTransformDelegater(this, new ViewTransformer(this) {
			@Override
			protected void setTransform(@NonNull final View view, @Nullable final Matrix transform) {
				ZoomAspectScaledTextureView2.super.setTransform(transform);
			}

			@NonNull
			@Override
			protected Matrix getTransform(@NonNull final View view, @Nullable final Matrix transform) {
				return ZoomAspectScaledTextureView2.super.getTransform(transform);
			}
		});
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

//================================================================================
	/**
	 * TextureViewに関連付けられたSurfaceTextureが利用可能になった時の処理
	 */
	@Override
	public void onSurfaceTextureAvailable(final SurfaceTexture surface, final int width, final int height) {
		super.onSurfaceTextureAvailable(surface, width, height);
		if (DEBUG) Log.v(TAG, String.format("onSurfaceTextureAvailable:(%dx%d)", width, height));
		setMirror(MIRROR_NORMAL);	// デフォルトだから適用しなくていいけど
	}

	/**
	 * SurfaceTextureのバッファーのサイズが変更された時の処理
	 */
	@Override
	public void onSurfaceTextureSizeChanged(final SurfaceTexture surface, final int width, final int height) {
		super.onSurfaceTextureSizeChanged(surface, width, height);
		if (DEBUG) Log.v(TAG, String.format("onSurfaceTextureSizeChanged:(%dx%d)", width, height));
		applyMirrorMode();
	}

//	/**
//	 * SurfaceTextureが破棄される時の処理
//	 * trueを返すとこれ以降描画処理は行われなくなる
//	 * falseを返すと自分でSurfaceTexture#release()を呼び出さないとダメ
//	 * ほとんどのアプリではtrueを返すべきである
//	 */
//	@Override
//	public boolean onSurfaceTextureDestroyed(final SurfaceTexture surface) {
//		super.onSurfaceTextureDestroyed(surface)
//		return true;
//	}

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
	public void onStartRotation(final ITransformView view) {
		if (DEBUG) Log.v(TAG, "onStartRotation:");
		// FIXME 未実装
	}

	@Override
	public void onStateChanged(final ITransformView view, final int newState) {
		if (DEBUG) Log.v(TAG, "onStateChanged:" + newState);
		// FIXME 未実装
	}

//================================================================================
	protected void init() {
		if (DEBUG) Log.v(TAG, "init:");
		super.init();
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

}
