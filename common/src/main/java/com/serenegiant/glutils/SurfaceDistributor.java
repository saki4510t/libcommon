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

import androidx.annotation.AnyThread;
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

	/**
	 * IRendererHolderの実装
	 * @return
	 */
	@Override
	public boolean isRunning() {
		return !mReleased && mGLSurfaceReceiver.isValid();
	}

	/**
	 * IRendererHolderの実装
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

	/**
	 * IRendererHolderの実装
	 * @return
	 */
	@NonNull
	@Override
	public GLContext getGLContext() {
		return mGlManager.getGLContext();
	}

	/**
	 * IRendererHolderの実装
	 * @return
	 */
	@Nullable
	@Override
	public EGLBase.IContext<?> getContext() {
		return mGlManager.getGLContext().getContext();
	}

	/**
	 * IRendererHolderの実装
	 * @return
	 */
	@Override
	public Surface getSurface() {
		return mGLSurfaceReceiver.getSurface();
	}

	/**
	 * IRendererHolderの実装
	 * @return
	 */
	@Override
	public SurfaceTexture getSurfaceTexture() {
		return mGLSurfaceReceiver.getSurfaceTexture();
	}

	/**
	 * IRendererHolderの実装
	 */
	@Override
	public void reset() {
		// たぶん不要
		mGLSurfaceRenderer.refresh();
		mGLSurfaceReceiver.reCreateInputSurface();
	}

	/**
	 * IRendererHolderの実装
	 * @param width
	 * @param height
	 * @throws IllegalStateException
	 */
	@Override
	public void resize(final int width, final int height) throws IllegalStateException {
		if (DEBUG) Log.v(TAG, String.format("resize:(%dx%d)", width, height));
		if ((width > 0) && (height > 0)) {
			mGLSurfaceRenderer.resize(width, height);
		}
	}

	/**
	 * IRendererHolderの実装
	 * @param id 普通は#hashCodeを使う
	 * @param surface Surface/SurfaceHolder/SurfaceTexture/SurfaceView/TextureWrapperのいずれか
	 * @param isRecordable
	 * @throws IllegalStateException
	 * @throws IllegalArgumentException
	 */
	@Override
	public void addSurface(
		final int id, final Object surface,
		final boolean isRecordable)
			throws IllegalStateException, IllegalArgumentException {
		this.addSurface(id, surface, isRecordable, null);
	}

	/**
	 * IRendererHolderの実装
	 * @param id 普通は#hashCodeを使う
	 * @param surface Surface/SurfaceHolder/SurfaceTexture/SurfaceView/TextureWrapperのいずれか
	 * @param isRecordable
	 * @param maxFps nullまたは0以下なら制限しない
	 * @throws IllegalStateException
	 * @throws IllegalArgumentException
	 */
	@Override
	public void addSurface(
		final int id, final Object surface,
		final boolean isRecordable,
		@Nullable final Fraction maxFps)
			throws IllegalStateException, IllegalArgumentException {

		mGLSurfaceRenderer.addSurface(id, surface, maxFps);
	}

	/**
	 * IRendererHolderの実装
	 * @param id
	 */
	@Override
	public void removeSurface(final int id) {
		mGLSurfaceRenderer.removeSurface(id);
	}

	/**
	 * IRendererHolderの実装
	 */
	@Override
	public void removeSurfaceAll() {
//		if (DEBUG) Log.v(TAG, "removeSurfaceAll:id=" + id);
		mGLSurfaceRenderer.removeSurfaceAll();
	}

	/**
	 * IRendererHolderの実装
	 * 指定したIDの分配描画用のSurfaceを指定した色で塗りつぶす
	 * @param id id=0なら描画先のSurface全てを指定した色で塗りつぶす、#clearSurfaceAllと同じ
	 * @param color
	 * @throws IllegalStateException
	 */
	@AnyThread
	@Override
	public void clearSurface(final int id, final int color) {
		mGLSurfaceRenderer.clearSurface(id, color);
	}

	/**
	 * IRendererHolderの実装
	 * すべての分配描画用のSurfaceを指定した色で塗りつぶす
	 * @param color
	 * @throws IllegalStateException
	 */
	@AnyThread
	@Override
	public void clearSurfaceAll(final int color) {
		mGLSurfaceRenderer.clearSurfaceAll(color);
	}

	/**
	 * IRendererHolderの実装
	 * 指定したidに対応するSurfaceへモデルビュー変換行列を適用する
	 * @param id id=0なら全てのSurfaceへモデルビュー変換行列を適用する
	 * @param offset
	 * @param matrix
	 * @throws IllegalStateException
	 * @throws IllegalArgumentException
	 */
	@AnyThread
	@Override
	public void setMvpMatrix(final int id, final int offset, @NonNull final float[] matrix) {
		mGLSurfaceRenderer.setMvpMatrix(id, offset, matrix);
	}

	/**
	 * IRendererHolderの実装
	 * @param id
	 * @return
	 */
	@Override
	public boolean isEnabled(final int id) {
		return mGLSurfaceRenderer.isEnabled(id);
	}

	/**
	 * IRendererHolderの実装
	 * 指定したidに対応するSurfaceへの描画を一時的に有効/無効設定する
	 * @param id id=0なら全てのSurfaceへの描画を一時的に有効/無効設定する
	 * @param enable
	 */
	@AnyThread
	@Override
	public void setEnabled(final int id, final boolean enable) {
		mGLSurfaceRenderer.setEnabled(id, enable);
	}

	/**
	 * IRendererHolderの実装
	 */
	@Override
	public void requestFrame() {
		// FIXME 未実装
	}

	/**
	 * IRendererHolderの実装
	 * @return
	 */
	@Override
	public int getCount() {
		return mGLSurfaceRenderer.getCount();
	}

	/**
	 * IRendererHolderの実装
	 * @param task
	 */
	@Override
	public void queueEvent(@NonNull final Runnable task) {
		mGlManager.runOnGLThread(task);
	}

	/**
	 * IMirrorの実装
	 * @param mirror 0:通常, 1:左右反転, 2:上下反転, 3:上下左右反転
	 * @throws IllegalStateException
	 */
	@Override
	public void setMirror(@MirrorMode final int mirror) {
		mGLSurfaceRenderer.setMirror(mirror);
	}

	/**
	 * IMirrorの実装
	 * @return
	 */
	@Override
	public int getMirror() {
		return mGLSurfaceRenderer.getMirror();
	}

}
