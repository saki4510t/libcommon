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

import com.serenegiant.glpipeline.IPipelineSource;
import com.serenegiant.glpipeline.VideoSource;
import com.serenegiant.glutils.GLManager;

import androidx.annotation.NonNull;

import static com.serenegiant.widget.CameraDelegator.OnFrameAvailableListener;
import static com.serenegiant.widget.CameraDelegator.*;

/**
 * Sub class of GLSurfaceView to display camera preview and write video frame to capturing surface
 */
public class VideoSourceCameraGLView
	extends GLSurfaceView implements ICameraGLView {

	private static final boolean DEBUG = true; // TODO set false on release
	private static final String TAG = VideoSourceCameraGLView.class.getSimpleName();

	private final CameraDelegator mCameraDelegator;
	private final GLManager mGLManager;
	private VideoSource mVideoSource;

	/**
	 * コンストラクタ
	 * @param context
	 */
	public VideoSourceCameraGLView(final Context context) {
		this(context, null, 0);
	}

	/**
	 * コンストラクタ
	 * @param context
	 * @param attrs
	 */
	public VideoSourceCameraGLView(final Context context, final AttributeSet attrs) {
		this(context, attrs, 0);
	}

	/**
	 * コンストラクタ
	 * @param context
	 * @param attrs
	 * @param defStyle
	 */
	public VideoSourceCameraGLView(final Context context, final AttributeSet attrs, final int defStyle) {
		super(context, attrs);
		if (DEBUG) Log.v(TAG, "CameraGLView:");
		mGLManager = new GLManager();
		mCameraDelegator = new CameraDelegator(this) {
			@Override
			protected SurfaceTexture getInputSurfaceTexture() {
				return mVideoSource != null ? mVideoSource.getInputSurfaceTexture() : null;
			}

			@Override
			public void addSurface(final int id, final Object surface, final boolean isRecordable) {
				VideoSourceCameraGLView.this.addSurface(id, surface, isRecordable);
			}

			@Override
			public void removeSurface(final int id) {
				VideoSourceCameraGLView.this.removeSurface(id);
			}
		};
	}

	@Override
	public synchronized void onResume() {
		if (DEBUG) Log.v(TAG, "onResume:");
		super.onResume();
		mVideoSource = createVideoSource(PREVIEW_WIDTH, PREVIEW_HEIGHT);
		mCameraDelegator.onResume();
	}

	@Override
	public synchronized void onPause() {
		if (DEBUG) Log.v(TAG, "onPause:");
		mCameraDelegator.onPause();
		if (mVideoSource != null) {
			mVideoSource.release();
			mVideoSource = null;
		}
		super.onPause();
	}

	@Override
	public void addListener(final OnFrameAvailableListener listener) {
		mCameraDelegator.addListener(listener);
	}

	@Override
	public void removeListener(final OnFrameAvailableListener listener) {
		mCameraDelegator.removeListener(listener);
	}
	
	@Override
	public void setScaleMode(final int mode) {
		mCameraDelegator.setScaleMode(mode);
	}

	@Override
	public int getScaleMode() {
		return mCameraDelegator.getScaleMode();
	}

	@Override
	public void setVideoSize(final int width, final int height) {
		mCameraDelegator.setVideoSize(width, height);
	}

	@Override
	public int getVideoWidth() {
		return mCameraDelegator.getWidth();
	}

	@Override
	public int getVideoHeight() {
		return mCameraDelegator.getHeight();
	}

	/**
	 * プレビュー表示用Surfaceを追加
	 * @param id
	 * @param surface
	 * @param isRecordable
	 */
	@Override
	public synchronized void addSurface(final int id, final Object surface,
		final boolean isRecordable) {

		if (DEBUG) Log.v(TAG, "addSurface:" + id);
		// FIXME 未実装
	}

	/**
	 * プレビュー表示用Surfaceを除去
	 * @param id
	 */
	@Override
	public synchronized void removeSurface(final int id) {
		if (DEBUG) Log.v(TAG, "removeSurface:" + id);
		// FIXME 未実装
	}

	@NonNull
	protected VideoSource createVideoSource(
		final int width, final int height) {

		return new VideoSource(mGLManager, new IPipelineSource.PipelineSourceCallback() {
			private int cnt;

			@Override
			public void onCreate(@NonNull final Surface surface) {
				if (DEBUG) Log.v(TAG, "PipelineSourceCallback#onCreate:" + surface);
			}

			@Override
			public void onDestroy() {
				if (DEBUG) Log.v(TAG, "PipelineSourceCallback#onDestroy:");
			}
		});
	}

}
