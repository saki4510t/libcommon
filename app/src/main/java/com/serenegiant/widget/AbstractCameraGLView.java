package com.serenegiant.widget;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2019 saki t_saki@serenegiant.com
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
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;

import com.serenegiant.glutils.IRendererHolder;
import com.serenegiant.glutils.RenderHolderCallback;

import androidx.annotation.NonNull;

import static com.serenegiant.widget.CameraDelegator.*;

/**
 * Sub class of GLSurfaceView to display camera preview and write video frame to capturing surface
 */
public abstract class AbstractCameraGLView extends GLSurfaceView {

	private static final boolean DEBUG = false; // TODO set false on release
	private static final String TAG = AbstractCameraGLView.class.getSimpleName();

	private final CameraDelegator mCameraDelegator;
	private IRendererHolder mRendererHolder;

	/**
	 * コンストラクタ
	 * @param context
	 */
	public AbstractCameraGLView(final Context context) {
		this(context, null, 0);
	}

	/**
	 * コンストラクタ
	 * @param context
	 * @param attrs
	 */
	public AbstractCameraGLView(final Context context, final AttributeSet attrs) {
		this(context, attrs, 0);
	}

	/**
	 * コンストラクタ
	 * @param context
	 * @param attrs
	 * @param defStyle
	 */
	public AbstractCameraGLView(final Context context, final AttributeSet attrs, final int defStyle) {
		super(context, attrs);
		if (DEBUG) Log.v(TAG, "CameraGLView:");
		mCameraDelegator = new CameraDelegator(this) {
			@Override
			protected SurfaceTexture getInputSurfaceTexture() {
				return mRendererHolder != null ? mRendererHolder.getSurfaceTexture() : null;
			}

			@Override
			public void addSurface(final int id, final Object surface, final boolean isRecordable) {
				AbstractCameraGLView.this.addSurface(id, surface, isRecordable);
			}

			@Override
			public void removeSurface(final int id) {
				AbstractCameraGLView.this.removeSurface(id);
			}
		};
	}

	@Override
	public synchronized void onResume() {
		if (DEBUG) Log.v(TAG, "onResume:");
		super.onResume();
		mRendererHolder = createRendererHolder(PREVIEW_WIDTH, PREVIEW_HEIGHT, mRenderHolderCallback);
		mCameraDelegator.onResume();
	}

	@Override
	public synchronized void onPause() {
		if (DEBUG) Log.v(TAG, "onPause:");
		mCameraDelegator.onPause();
		if (mRendererHolder != null) {
			mRendererHolder.release();
			mRendererHolder = null;
		}
		super.onPause();
	}

	public void addListener(final CameraDelegator.OnFrameAvailableListener listener) {
		mCameraDelegator.addListener(listener);
	}
	
	public void removeListener(final CameraDelegator.OnFrameAvailableListener listener) {
		mCameraDelegator.removeListener(listener);
	}
	
	public void setScaleMode(final int mode) {
		mCameraDelegator.setScaleMode(mode);
	}

	public int getScaleMode() {
		return mCameraDelegator.getScaleMode();
	}

	public void setVideoSize(final int width, final int height) {
		mCameraDelegator.setVideoSize(width, height);
	}

	public int getVideoWidth() {
		return mCameraDelegator.getWidth();
	}

	public int getVideoHeight() {
		return mCameraDelegator.getHeight();
	}

	protected IRendererHolder getRendererHolder() {
		return mRendererHolder;
	}

	/**
	 * プレビュー表示用Surfaceを追加
	 * @param id
	 * @param surface
	 * @param isRecordable
	 */
	public synchronized void addSurface(final int id, final Object surface,
		final boolean isRecordable) {

		if (mRendererHolder != null) {
			mRendererHolder.addSurface(id, surface, isRecordable);
		}
	}

	/**
	 * プレビュー表示用Surfaceを除去
	 * @param id
	 */
	public synchronized void removeSurface(final int id) {
		if (mRendererHolder != null) {
			mRendererHolder.removeSurface(id);
		}
	}

	@NonNull
	protected abstract IRendererHolder createRendererHolder(
		final int width, final int height,
		final RenderHolderCallback callback);

	private final RenderHolderCallback mRenderHolderCallback
		= new RenderHolderCallback() {

		@Override
		public void onCreate(final Surface surface) {

		}

		@Override
		public void onFrameAvailable() {
			callOnFrameAvailable();
		}

		@Override
		public void onDestroy() {

		}
	};

	protected void callOnFrameAvailable() {
		mCameraDelegator.callOnFrameAvailable();
	}

}
