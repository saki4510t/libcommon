package com.serenegiant.glpipeline;
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

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import com.serenegiant.gl.GLDrawer2D;
import com.serenegiant.gl.GLEffectDrawer2D;
import com.serenegiant.gl.GLManager;
import com.serenegiant.glutils.IMirror;
import com.serenegiant.gl.RendererTarget;
import com.serenegiant.math.Fraction;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;
import androidx.annotation.WorkerThread;

/**
 * テクスチャが更新されたときにSurfaceへ転送するGLPipeline実装
 * このクラスは他のGLSurfacePipelineインターフェース実装クラスと異なり、
 * アップストリームからのテクスチャを変更せずそのまま次のパイプランへ送る
 * パイプライン → SurfaceRendererPipeline (→ パイプライン)
 *                → Surface
 */
public class SurfaceRendererPipeline extends ProxyPipeline
	implements GLSurfacePipeline, IMirror {

	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = SurfaceRendererPipeline.class.getSimpleName();

	@NonNull
	private final GLManager mManager;
	@NonNull
	private final GLDrawer2D.DrawerFactory mDrawerFactory;

	@Nullable
	private GLDrawer2D mDrawer;
	@Nullable
	private RendererTarget mRendererTarget;
	@MirrorMode
	private int mMirror = MIRROR_NORMAL;
	private int mSurfaceId = 0;
	/**
	 * モデルビュー変換行列
	 */
	@Size(value=16)
	@NonNull
	private final float[] mMvpMatrix = new float[16];
	/**
	 * コンストラクタ
	 * @param manager
	 * @param manager
	 * @throws IllegalStateException
	 * @throws IllegalArgumentException
	 */
	public SurfaceRendererPipeline(
		@NonNull final GLManager manager)
			throws IllegalStateException, IllegalArgumentException {
		this(manager, GLDrawer2D.DEFAULT_FACTORY, null, null);
	}

	/**
	 * コンストラクタ
	 * 対応していないSurface形式の場合はIllegalArgumentExceptionを投げる
	 * @param manager
	 * @param surface nullまたはSurface/SurfaceHolder/SurfaceTexture/SurfaceView/TextureWrapper/IGLSurface/ISurfaceのいずれかまたはその子クラス
	 * @param maxFps 最大フレームレート, nullまたはFraction#ZEROなら制限なし
	 * @throws IllegalStateException
	 * @throws IllegalArgumentException
	 */
	public SurfaceRendererPipeline(
		@NonNull final GLManager manager,
		@Nullable final Object surface, @Nullable final Fraction maxFps)
		throws IllegalStateException, IllegalArgumentException {
		this(manager, GLDrawer2D.DEFAULT_FACTORY, surface, maxFps);
	}

	/**
	 * コンストラクタ
	 * 対応していないSurface形式の場合はIllegalArgumentExceptionを投げる
	 * @param manager
	 * @param drawerFactory
	 * @param surface nullまたはSurface/SurfaceHolder/SurfaceTexture/SurfaceView/TextureWrapper/IGLSurface/ISurfaceのいずれかまたはその子クラス
	 * @param maxFps 最大フレームレート, nullまたはFraction#ZEROなら制限なし
	 * @throws IllegalStateException
	 * @throws IllegalArgumentException
	 */
	public SurfaceRendererPipeline(
		@NonNull final GLManager manager,
		@NonNull GLDrawer2D.DrawerFactory drawerFactory,
		@Nullable final Object surface, @Nullable final Fraction maxFps)
			throws IllegalStateException, IllegalArgumentException {

		super();
		if (DEBUG) Log.v(TAG, "コンストラクタ:");
		if ((surface != null) && !RendererTarget.isSupportedSurface(surface)) {
			throw new IllegalArgumentException("Unsupported surface type!," + surface);
		}
		mManager = manager;
		mDrawerFactory = drawerFactory;
		Matrix.setIdentityM(mMvpMatrix, 0);
		manager.runOnGLThread(() -> {
			createTargetOnGL(surface, maxFps);
		});
	}

	@Override
	protected void internalRelease() {
		if (DEBUG) Log.v(TAG, "internalRelease:");
		if (isValid()) {
			releaseTarget();
		}
		super.internalRelease();
	}

	/**
	 * GLSurfacePipelineの実装
	 * 描画先のSurfaceを差し替え, 最大フレームレートの制限をしない
	 * 対応していないSurface形式の場合はIllegalArgumentExceptionを投げる
	 * @param surface nullまたはSurface/SurfaceHolder/SurfaceTexture/SurfaceView
	 * @throws IllegalStateException
	 * @throws IllegalArgumentException
	 */
	@Override
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
	@Override
	public void setSurface(
		@Nullable final Object surface,
		@Nullable final Fraction maxFps) throws IllegalStateException, IllegalArgumentException {

		if (DEBUG) Log.v(TAG, "setSurface:" + surface);
		if (!isValid()) {
			throw new IllegalStateException("already released?");
		}
		if ((surface != null) && !RendererTarget.isSupportedSurface(surface)) {
			throw new IllegalArgumentException("Unsupported surface type!," + surface);
		}
		mManager.runOnGLThread(() -> {
			createTargetOnGL(surface, maxFps);
		});
	}

	@Override
	public boolean hasSurface() {
		mLock.lock();
		try {
			return mSurfaceId != 0;
		} finally {
			mLock.unlock();
		}
	}

	@Override
	public boolean isValid() {
		return super.isValid() && mManager.isValid();
	}

	/**
	 * セットされているSurface識別用のidを取得
	 * @return Surfaceがセットされていればそのid(#hashCode)、セットされていなければ0を返す
	 */
	@Override
	public int getId() {
		mLock.lock();
		try {
			return mSurfaceId;
		} finally {
			mLock.unlock();
		}
	}

	@Override
	public void setMirror(@MirrorMode final int mirror) {
		mLock.lock();
		try {
			if (mMirror != mirror) {
				mMirror = mirror;
				mManager.runOnGLThread(() -> {
					if (mRendererTarget != null) {
						mRendererTarget.setMirror(mirror);
					}
				});
			}
		} finally {
			mLock.unlock();
		}
	}

	@MirrorMode
	@Override
	public int getMirror() {
		mLock.lock();
		try {
			return mMirror;
		} finally {
			mLock.unlock();
		}
	}

	public void setMvpMatrix(@NonNull @Size(min=16) final float[] matrix, final int offset) {
		System.arraycopy(matrix, offset, mMvpMatrix, 0, 16);
		final GLDrawer2D drawer = mDrawer;
		if (drawer != null) {
			drawer.setMvpMatrix(matrix, offset);
		}
	}

	private int cnt;
	@WorkerThread
	@Override
	public void onFrameAvailable(
		final boolean isGLES3,
		final boolean isOES,
		final int width, final int height,
		final int texId, @NonNull @Size(min=16) final float[] texMatrix) {

		super.onFrameAvailable(isGLES3, isOES, width, height, texId, texMatrix);
		if (isValid()) {
			if ((mDrawer == null) || isOES != mDrawer.isOES()) {
				// 初回またはGLPipelineを繋ぎ変えたあとにテクスチャが変わるかもしれない
				if (mDrawer != null) {
					mDrawer.release();
				}
				if (DEBUG) Log.v(TAG, "onFrameAvailable:create GLDrawer2D");
				mDrawer = mDrawerFactory.create(isGLES3, isOES);
				mDrawer.setMvpMatrix(mMvpMatrix, 0);
				if (mDrawer instanceof GLEffectDrawer2D) {
					((GLEffectDrawer2D) mDrawer).setTexSize(width, height);
				}
			}
			@NonNull
			final GLDrawer2D drawer = mDrawer;
			@Nullable
			final RendererTarget target = mRendererTarget;
			if ((target != null)
				&& target.canDraw()) {
				target.draw(drawer, GLES20.GL_TEXTURE0, texId, texMatrix);
				if (DEBUG && (++cnt % 100) == 0) {
					Log.v(TAG, "onFrameAvailable:" + cnt);
				}
			}
		}
	}

	@Override
	public void refresh() {
		super.refresh();
		// XXX #removeでパイプラインチェーンのどれかを削除するとなぜか映像が表示されなくなってしまうことへのワークアラウンド
		// XXX パイプライン中のどれかでシェーダーを再生成すると表示されるようになる
		if (isValid()) {
			mManager.runOnGLThread(() -> {
				if (DEBUG) Log.v(TAG, "refresh#run:release drawer");
				GLDrawer2D drawer = mDrawer;
				mDrawer = null;
				if (drawer != null) {
					drawer.release();
				}
			});
		}
	}

	/**
	 * 描画先のSurfaceを生成
	 * @param surface nullまたはSurface/SurfaceHolder/SurfaceTexture/SurfaceView/TextureWrapper/IGLSurface/ISurfaceのいずれかまたはその子クラス
	 * @param maxFps
	 */
	@WorkerThread
	private void createTargetOnGL(@Nullable final Object surface, @Nullable final Fraction maxFps) {
		if (DEBUG) Log.v(TAG, "createTarget:" + surface);
		if ((mRendererTarget != null) && (mRendererTarget.getSurface() != surface)) {
			// すでにRendererTargetが生成されていて描画先surfaceが変更された時
			mSurfaceId = 0;
			mRendererTarget.release();
			mRendererTarget = null;
		}
		if ((mRendererTarget == null) && (surface != null)) {
			mRendererTarget = RendererTarget.newInstance(
				mManager.getEgl(), surface, maxFps != null ? maxFps.asFloat() : 0);
		}
		if (mRendererTarget != null) {
			mRendererTarget.setMirror(mMirror);
			mLock.lock();
			try {
				mSurfaceId = mRendererTarget.getId();
			} finally {
				mLock.unlock();
			}
		}
	}

	@WorkerThread
	private void releaseTarget() {
		final GLDrawer2D drawer;
		final RendererTarget target;
		mLock.lock();
		try {
			mSurfaceId = 0;
			drawer = mDrawer;
			mDrawer = null;
			target = mRendererTarget;
			mRendererTarget = null;
		} finally {
			mLock.unlock();
		}
		if ((drawer != null) || (target != null)) {
			if (DEBUG) Log.v(TAG, "releaseTarget:");
			if (mManager.isValid()) {
				try {
					mManager.runOnGLThread(() -> {
						if (drawer != null) {
							if (DEBUG) Log.v(TAG, "releaseTarget:release drawer");
							drawer.release();
						}
						if (target != null) {
							if (DEBUG) Log.v(TAG, "releaseTarget:release target");
							target.release();
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
