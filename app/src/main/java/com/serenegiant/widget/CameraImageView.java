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

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;

import com.serenegiant.graphics.SurfaceDrawable;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;

import static com.serenegiant.widget.CameraDelegator.DEFAULT_PREVIEW_HEIGHT;
import static com.serenegiant.widget.CameraDelegator.DEFAULT_PREVIEW_WIDTH;

/**
 * SurfaceDrawableを使ってカメラ映像を表示するImageView実装
 */
public class CameraImageView extends AppCompatImageView
	implements CameraDelegator.ICameraView {

	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = CameraImageView.class.getSimpleName();

	@NonNull
	private final CameraDelegator mCameraDelegator;
	private SurfaceDrawable mDrawable;

	/**
	 * コンストラクタ
	 * @param context
	 */
	public CameraImageView(final Context context) {
		this(context, null, 0);
	}

	/**
	 * コンストラクタ
	 * @param context
	 * @param attrs
	 */
	public CameraImageView(final Context context, final AttributeSet attrs) {
		this(context, attrs, 0);
	}

	/**
	 * コンストラクタ
	 * @param context
	 * @param attrs
	 * @param defStyleAttr
	 */
	public CameraImageView(final Context context, final AttributeSet attrs, final int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		if (DEBUG) Log.v(TAG, "コンストラクタ:");
		// XXX GLES30はAPI>=18以降なんだけどAPI=18でもGLコンテキスト生成に失敗する端末があるのでAP1>=21に変更
		mDrawable = new SurfaceDrawable(DEFAULT_PREVIEW_WIDTH, DEFAULT_PREVIEW_HEIGHT, new SurfaceDrawable.Callback() {
			@Override
			public void onCreateSurface(@NonNull final Surface surface) {
				if (DEBUG) Log.v(TAG, "onCreateSurface:" + surface);
				mCameraDelegator.startPreview(DEFAULT_PREVIEW_WIDTH, DEFAULT_PREVIEW_HEIGHT);
			}

			@Override
			public void onDestroySurface() {
				if (DEBUG) Log.v(TAG, "onDestroySurface:");
				mCameraDelegator.stopPreview();
			}
		});
		setImageDrawable(mDrawable);
		mCameraDelegator = new CameraDelegator(this,
			DEFAULT_PREVIEW_WIDTH, DEFAULT_PREVIEW_HEIGHT) {
			@NonNull
			@Override
			protected SurfaceTexture getInputSurfaceTexture() {
				final SurfaceTexture st = mDrawable.getSurfaceTexture();
				if (DEBUG) Log.v(TAG, "getInputSurfaceTexture:" + st);
				return st;
			}

			@NonNull
			@Override
			protected ICameraRenderer createCameraRenderer(@NonNull final CameraDelegator parent) {
				return new ICameraRenderer() {
					@Override
					public void onSurfaceDestroyed() {
						if (DEBUG) Log.v(TAG, "onSurfaceDestroyed:");
					}

					@Override
					public boolean hasSurface() {
						if (DEBUG) Log.v(TAG, "hasSurface:");
						return mDrawable.isSurfaceValid();
					}

					@Override
					public void updateViewport() {
						if (DEBUG) Log.v(TAG, "updateViewport:");
					}

					@Override
					public void onPreviewSizeChanged(final int width, final int height) {
						if (DEBUG) Log.v(TAG, String.format("onPreviewSizeChanged:(%dx%d)", width, height));
					}
				};
			}
		};
		setScaleType(ScaleType.CENTER_CROP);
	}

	@Override
	public void onResume() {
		if (DEBUG) Log.v(TAG, "onResume:");
		mCameraDelegator.onResume();
	}

	@Override
	public void onPause() {
		if (DEBUG) Log.v(TAG, "onPause:");
		mCameraDelegator.onPause();
	}

	@Override
	public void queueEvent(final Runnable task) {
		if (DEBUG) Log.v(TAG, "queueEvent:" + task);
	}

	@Override
	public void setVideoSize(final int width, final int height) {
		if (DEBUG) Log.v(TAG, String.format("setVideoSize:(%dx%d)", width, height));
		mCameraDelegator.setVideoSize(width, height);
	}

	@Override
	public void addListener(final CameraDelegator.OnFrameAvailableListener listener) {
		if (DEBUG) Log.v(TAG, "addListener:");
	}

	@Override
	public void removeListener(final CameraDelegator.OnFrameAvailableListener listener) {
		if (DEBUG) Log.v(TAG, "removeListener:");
	}

	@Override
	public void setScaleMode(final int mode) {
		if (DEBUG) Log.v(TAG, "setScaleMode:");
		mCameraDelegator.setScaleMode(mode);
	}

	@Override
	public int getScaleMode() {
		if (DEBUG) Log.v(TAG, "getScaleMode:");
		return mCameraDelegator.getScaleMode();
	}

	@Override
	public int getVideoWidth() {
		if (DEBUG) Log.v(TAG, "getVideoWidth:");
		return mCameraDelegator.getWidth();
	}

	@Override
	public int getVideoHeight() {
		if (DEBUG) Log.v(TAG, "getVideoHeight:");
		return mCameraDelegator.getHeight();
	}

	@Override
	public void addSurface(final int id, final Object surface, final boolean isRecordable) {
		if (DEBUG) Log.v(TAG, "addSurface:");
	}

	@Override
	public void removeSurface(final int id) {
		if (DEBUG) Log.v(TAG, "removeSurface:");
	}
}
