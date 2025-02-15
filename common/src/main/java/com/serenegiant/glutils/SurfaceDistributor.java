package com.serenegiant.glutils;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2025 saki t_saki@serenegiant.com
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

import android.graphics.SurfaceTexture;
import android.util.Log;
import android.view.Surface;

import com.serenegiant.egl.EGLBase;
import com.serenegiant.gl.GLContext;
import com.serenegiant.gl.GLDrawer2D;
import com.serenegiant.gl.GLManager;
import com.serenegiant.math.Fraction;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Surfaceで受け取った映像を複数のSurfaceへ分配描画するためのIRendererHolder実装
 * GLSurfaceReceiverでSurfaceとして受け取った映像をテクスチャとしてGLSurfaceRendererで
 * Surfaceへ分配描画する
 */
public class SurfaceDistributor implements IRendererHolder {
	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = SurfaceDistributor.class.getSimpleName();
	private static final String RENDERER_THREAD_NAME = "SurfaceDistributor";

	/**
	 * 自分用のGLManagerを保持しているかどうか
	 */
	private final boolean mOwnManager;
	@NonNull
	private final GLManager mGlManager;
	@NonNull
	private final GLSurfaceReceiver mGLSurfaceReceiver;
	@NonNull
	private final GLSurfaceRenderer mGLSurfaceRenderer;
	private volatile boolean mReleased = false;

	/**
	 * コンストラクタ
	 * @param width
	 * @param height
	 * @param callback
	 */
	public SurfaceDistributor(
		final int width, final int height,
		@Nullable final RenderHolderCallback callback) {
		this(new GLManager(), false,
			width, height,
			GLDrawer2D.DEFAULT_FACTORY,
			callback != null ? callback : DEFAULT_CALLBACK);
	}

	/**
	 * コンストラクタ
	 * @param glManager
	 * @param width
	 * @param height
	 * @param callback
	 */
	public SurfaceDistributor(
		@NonNull final GLManager glManager, final boolean useSharedContext,
		final int width, final int height,
		@NonNull GLDrawer2D.DrawerFactory factory,
		@NonNull final RenderHolderCallback callback) {

		mOwnManager = useSharedContext;
		if (useSharedContext) {
			// 共有コンテキストを使ってマルチスレッド処理を行う時
			mGlManager = glManager.createShared(null);
		} else {
			// 映像提供元のGLコンテキスト上で実行する時
			mGlManager = glManager;
		}

		// 分配描画用にGLSurfaceRendererを生成
		mGLSurfaceRenderer = new GLSurfaceRenderer(
			mGlManager, width, height, factory);
		// Surface経由で映像を受け取るためにGLSurfaceReceiverを生成
		mGLSurfaceReceiver = new GLSurfaceReceiver(
			mGlManager, width, height,
			new GLSurfaceReceiver.DefaultCallback(mGLSurfaceRenderer) {
				@Override
				public void onCreateInputSurface(@NonNull final Surface surface, final int width, final int height) {
					super.onCreateInputSurface(surface, width, height);
					callback.onCreateSurface(surface);
				}
				@Override
				public void onReleaseInputSurface(@NonNull final Surface surface) {
					super.onReleaseInputSurface(surface);
					callback.onDestroySurface();
				}
				@Override
				public void onFrameAvailable(
					final boolean isGLES3, final boolean isOES,
					final int width, final int height,
					final int texId, @NonNull final float[] texMatrix) {
					super.onFrameAvailable(isGLES3, isOES, width, height, texId, texMatrix);
					callback.onFrameAvailable();
				}
			});
	}

	@Override
	protected void finalize() throws Throwable {
		try {
			release();
		} finally {
			super.finalize();
		}
	}

	@Override
	public boolean isRunning() {
		return !mReleased && mGLSurfaceReceiver.isValid();
	}

	/**
	 * 関係するリソースを破棄する、再利用はできない
	 */
	@Override
	public void release() {
		if (!mReleased) {
			mReleased = true;
			if (DEBUG) Log.v(TAG, "release:");
			internalRelease();
		}
	}

	/**
	 * 関係するリソースの破棄処理の実体
	 */
	protected void internalRelease() {
		if (DEBUG) Log.v(TAG, "internalRelease:");
		mGLSurfaceReceiver.release();
		mGLSurfaceRenderer.release();
		if (mOwnManager && mGlManager.isValid()) {
			mGlManager.getGLHandler().postAtFrontOfQueue(() -> {
				mGlManager.release();
			});
		}
	}

	@NonNull
	@Override
	public GLContext getGLContext() {
		return mGlManager.getGLContext();
	}

	@Nullable
	@Override
	public EGLBase.IContext<?> getContext() {
		return mGlManager.getGLContext().getContext();
	}

	@Override
	public Surface getSurface() {
		return mGLSurfaceReceiver.getSurface();
	}

	@Override
	public SurfaceTexture getSurfaceTexture() {
		return mGLSurfaceReceiver.getSurfaceTexture();
	}

	@Override
	public void reset() {
		// たぶん不要
		mGLSurfaceRenderer.refresh();
		mGLSurfaceReceiver.reCreateInputSurface();
	}

	@Override
	public void resize(final int width, final int height) throws IllegalStateException {
		if (DEBUG) Log.v(TAG, String.format("resize:(%dx%d)", width, height));
		if ((width > 0) && (height > 0)) {
			mGLSurfaceRenderer.resize(width, height);
		}
	}

	@Override
	public void addSurface(
		final int id, final Object surface,
		final boolean isRecordable)
			throws IllegalStateException, IllegalArgumentException {
		this.addSurface(id, surface, isRecordable, null);
	}

	@Override
	public void addSurface(
		final int id, final Object surface,
		final boolean isRecordable,
		@Nullable final Fraction maxFps)
			throws IllegalStateException, IllegalArgumentException {

		mGLSurfaceRenderer.addSurface(id, surface, maxFps);
	}

	@Override
	public void removeSurface(final int id) {
		mGLSurfaceRenderer.removeSurface(id);
	}

	@Override
	public void removeSurfaceAll() {
//		if (DEBUG) Log.v(TAG, "removeSurfaceAll:id=" + id);
		mGLSurfaceRenderer.removeSurfaceAll();
	}

	@Override
	public void clearSurface(final int id, final int color) {
		mGLSurfaceRenderer.clearSurface(id, color);
	}

	@Override
	public void clearSurfaceAll(final int color) {
		mGLSurfaceRenderer.clearSurfaceAll(color);
	}

	@Override
	public void setMvpMatrix(final int id, final int offset, @NonNull final float[] matrix) {
		mGLSurfaceRenderer.setMvpMatrix(id, offset, matrix);
	}

	@Override
	public boolean isEnabled(final int id) {
		return mGLSurfaceRenderer.isEnabled(id);
	}

	@Override
	public void setEnabled(final int id, final boolean enable) {
		mGLSurfaceRenderer.setEnabled(id, enable);
	}

	@Override
	public void requestFrame() {
		// FIXME 未実装
	}

	@Override
	public int getCount() {
		return mGLSurfaceRenderer.getCount();
	}

	@Override
	public void queueEvent(@NonNull final Runnable task) {
		mGlManager.runOnGLThread(task);
	}

	@Override
	public void setMirror(@MirrorMode final int mirror) {
		mGLSurfaceRenderer.setMirror(mirror);
	}

	@Override
	public int getMirror() {
		return mGLSurfaceRenderer.getMirror();
	}

}
