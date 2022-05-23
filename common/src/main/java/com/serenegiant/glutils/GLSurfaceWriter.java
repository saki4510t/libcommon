package com.serenegiant.glutils;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2022 saki t_saki@serenegiant.com
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

import android.opengl.GLES20;
import android.util.Log;

import com.serenegiant.gl.GLDrawer2D;
import com.serenegiant.gl.GLManager;
import com.serenegiant.gl.GLUtils;
import com.serenegiant.gl.RendererTarget;
import com.serenegiant.math.Fraction;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;
import androidx.annotation.WorkerThread;

/**
 * 受け取ったテクスチャをSurfaceへ転送するクラス
 * XXX 削除するかも
 */
public class GLSurfaceWriter implements IMirror {
	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = GLSurfaceWriter.class.getSimpleName();

	@NonNull
	private final Object mSync = new Object();
	@NonNull
	private final GLManager mManager;
	/**
	 * 自分用のGLManagerを保持しているかどうか
	 */
	private final boolean mOwnManager;
	private volatile boolean mReleased = false;

	@Nullable
	private GLDrawer2D mDrawer;
	@Nullable
	private RendererTarget mRendererTarget;
	@MirrorMode
	private int mMirror = MIRROR_NORMAL;

	/**
	 * コンストラクタ
	 * @param manager
	 */
	public GLSurfaceWriter(
		@NonNull final GLManager manager, final boolean useSharedContext) {

		mOwnManager = useSharedContext;
		if (useSharedContext) {
			mManager = manager.createShared(null);
		} else {
			mManager = manager;
		}
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
	 * 関係するリソースを破棄する、再利用はできない
	 */
	public final void release() {
		if (!mReleased) {
			if (DEBUG) Log.v(TAG, "release:");
			mReleased = true;
			internalRelease();
		}
	}

	protected void internalRelease() {
		if (mManager.isValid()) {
			mManager.runOnGLThread(() -> {
				releaseTargetOnGL();
				if (mOwnManager) {
					mManager.release();
				}
			});
		}
	}

	/**
	 * GLSurfaceWriterが破棄されておらず利用可能かどうかを取得
	 * @return
	 */
	public boolean isValid() {
		return !mReleased && mManager.isValid();
	}

	/**
	 * 描画先のSurfaceをセットしているかどうか
	 * #isEffectOnlyの符号反転したのものと実質的には同じ
	 * @return
	 */
	public boolean hasSurface() {
		synchronized (mSync) {
			return mRendererTarget != null;
		}
	}

	/**
	 * セットされているSurface識別用のidを取得
	 * @return Surfaceがセットされていればそのid(#hashCode)、セットされていなければ0を返す
	 */
	public int getId() {
		synchronized (mSync) {
			return mRendererTarget != null ? mRendererTarget.getId() : 0;
		}
	}

	@Override
	public void setMirror(@MirrorMode final int mirror) {
		synchronized (mSync) {
			if (mMirror != mirror) {
				mMirror = mirror;
				mManager.runOnGLThread(() -> {
					if (mRendererTarget != null) {
						mRendererTarget.setMirror(IMirror.flipVertical(mirror));
					}
				});
			}
		}
	}

	@MirrorMode
	@Override
	public int getMirror() {
		synchronized (mSync) {
			return mMirror;
		}
	}

	/**
	 * GLSurfacePipelineの実装
	 * 描画先のSurfaceを差し替え, 最大フレームレートの制限をしない
	 * 対応していないSurface形式の場合はIllegalArgumentExceptionを投げる
	 * @param surface nullまたはSurface/SurfaceHolder/SurfaceTexture/SurfaceView
	 * @throws IllegalStateException
	 * @throws IllegalArgumentException
	 */
	public void setSurface(@Nullable final Object surface)
		throws IllegalStateException, IllegalArgumentException {

		setSurface(surface, null);
	}

	/**
	 * GLSurfacePipelineの実装
	 * 描画先のSurfaceを差し替え
	 * 対応していないSurface形式の場合はIllegalArgumentExceptionを投げる
	 * @param surface nullまたはSurface/SurfaceHolder/SurfaceTexture/SurfaceView
	 * @param maxFps 最大フレームレート, nullまたはFraction#ZEROなら制限なし
	 * @throws IllegalStateException
	 * @throws IllegalArgumentException
	 */
	public void setSurface(
		@Nullable final Object surface,
		@Nullable final Fraction maxFps) throws IllegalStateException, IllegalArgumentException {

		if (DEBUG) Log.v(TAG, "setSurface:" + surface);
		if (!isValid()) {
			throw new IllegalStateException("already released?");
		}
		if ((surface == null) || GLUtils.isSupportedSurface(surface)) {
			mManager.runOnGLThread(() -> {
				createTargetOnGL(surface, maxFps);
			});
		} else {
			throw new IllegalArgumentException("Unsupported surface type!," + surface);
		}
	}

	private int cnt;

	/**
	 * 映像をセット
	 * @param isOES
	 * @param texId
	 * @param texMatrix
	 */
	public void onFrameAvailable(
		final boolean isOES, final int texId,
		@NonNull @Size(min=16) final float[] texMatrix) {

		if (isValid()) {
			if (mManager.isGLThread()) {
				renderTargetOnGL(isOES, texId, texMatrix);
			} else {
				final Semaphore sem = new Semaphore(0);
				mManager.runOnGLThread(() -> {
					renderTargetOnGL(isOES, texId, texMatrix);
					sem.release();
				});
				try {
					if (!sem.tryAcquire(50, TimeUnit.MILLISECONDS)) {
						if (DEBUG) Log.w(TAG, "rendering may failed.");
					}
				} catch (final InterruptedException e) {
					if (DEBUG) Log.w(TAG, e);
				}
			}
		}
	}

//--------------------------------------------------------------------------------
	private void renderTargetOnGL(
		final boolean isOES, final int texId,
		@NonNull @Size(min=16) final float[] texMatrix) {

		@NonNull
		final GLDrawer2D drawer;
		@Nullable
		final RendererTarget target;
		synchronized (mSync) {
			if ((mDrawer == null) || isOES != mDrawer.isOES()) {
				// 初回またはGLPipelineを繋ぎ変えたあとにテクスチャが変わるかもしれない
				if (mDrawer != null) {
					mDrawer.release();
				}
				if (DEBUG) Log.v(TAG, "onFrameAvailable:create GLDrawer2D");
				mDrawer = GLDrawer2D.create(mManager.isGLES3(), isOES);
			}
			drawer = mDrawer;
			target = mRendererTarget;
		}
		if ((target != null)
			&& target.canDraw()) {
			target.draw(drawer, GLES20.GL_TEXTURE0, texId, texMatrix);
			if (DEBUG && (++cnt % 100) == 0) {
				Log.v(TAG, "onFrameAvailable:" + cnt);
			}
		}
	}

	/**
	 * 描画先のSurfaceを生成
	 * @param surface
	 * @param maxFps
	 */
	@WorkerThread
	private void createTargetOnGL(@Nullable final Object surface, @Nullable final Fraction maxFps) {
		if (DEBUG) Log.v(TAG, "createTarget:" + surface);
		synchronized (mSync) {
			synchronized (mSync) {
				if ((mRendererTarget != null) && (mRendererTarget.getSurface() != surface)) {
					// すでにRendererTargetが生成されていて描画先surfaceが変更された時
					mRendererTarget.release();
					mRendererTarget = null;
				}
				if ((mRendererTarget == null) && (surface != null)) {
					mRendererTarget = RendererTarget.newInstance(
						mManager.getEgl(), surface, maxFps != null ? maxFps.asFloat() : 0);
				}
				if (mRendererTarget != null) {
					mRendererTarget.setMirror(IMirror.flipVertical(mMirror));
				}
			}
		}
	}

	@WorkerThread
	private void releaseTargetOnGL() {
		final GLDrawer2D drawer;
		final RendererTarget target;
		synchronized (mSync) {
			drawer = mDrawer;
			mDrawer = null;
			target = mRendererTarget;
			mRendererTarget = null;
		}
		if ((drawer != null) || (target != null)) {
			if (DEBUG) Log.v(TAG, "releaseTarget:");
			if (mManager.isValid()) {
				try {
					mManager.runOnGLThread(new Runnable() {
						@WorkerThread
						@Override
						public void run() {
							if (drawer != null) {
								if (DEBUG) Log.v(TAG, "releaseTarget:release drawer");
								drawer.release();
							}
							if (target != null) {
								if (DEBUG) Log.v(TAG, "releaseTarget:release target");
								target.release();
							}
						}
					});
				} catch (final Exception e) {
					if (DEBUG) Log.w(TAG, e);
				}
			} else if (DEBUG) {
				Log.w(TAG, "releaseTarget:unexpectedly GLManager is already released!");
			}
		}
	}
}
