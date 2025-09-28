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
import com.serenegiant.gl.GLSurface;
import com.serenegiant.glutils.IMirror;
import com.serenegiant.gl.RendererTarget;
import com.serenegiant.math.Fraction;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;
import androidx.annotation.WorkerThread;

/**
 * 任意のGLDrawer2D(とその継承クラス)を使ってテクスチャを描画するGLPipeline実装
 * 描画先のsurfaceにnullを指定すると描画後のテクスチャを次のGLPipelineへ送る
 * パイプライン → DrawerPipeline (→ パイプライン)
 *                (→ Surface)
 */
public class DrawerPipeline extends ProxyPipeline
	implements GLSurfacePipeline, IMirror {
	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = DrawerPipeline.class.getSimpleName();

	@NonNull
	private final GLManager mManager;
	@NonNull
	private final GLDrawer2D.DrawerFactory mDrawerFactory;
	/**
	 * 次のパイプラインへ送るテクスチャの挙動指定フラグ
	 */
	@PipelineMode
	private final int mPipelineMode;

	/**
	 * 描画処理した後のテクスチャを次のGLPipelineへ送るか前のパイプラインからの
	 * テクスチャをそのまま次のパイプラインへ送るか
	 */
	private volatile boolean mPathThrough = true;
	@Nullable
	private GLDrawer2D mDrawer;
	@Nullable
	private Fraction mMaxFps;
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
	 * Surfaceへの描画用
	 */
	@Nullable
	private RendererTarget mSurfaceTarget = null;

	/**
	 * 描画処理した結果を次のGLPipelineへ送る場合のワーク用GLSurface
	 */
	@Nullable
	private GLSurface mOffscreenSurface = null;
	/**
	 * オフスクリーンのGLSurfaceへの描画用
	 */
	@Nullable
	private RendererTarget mOffscreenTarget = null;

	/**
	 * コンストラクタ
	 * 明示的に#setSurfaceでSurfaceを指定しない場合、GLDrawer2Dで描画した映像を次のパイプラインへ送る
	 * null以外のSurfaceを指定した場合は前のパイプラインからのテクスチャをそのまま次のパイプラインへ送る
	 * @param manager
	 * @throws IllegalStateException
	 * @throws IllegalArgumentException
	 */
	public DrawerPipeline(
		@NonNull final GLManager manager)
		throws IllegalStateException, IllegalArgumentException {

		this(manager, GLDrawer2D.DEFAULT_FACTORY, PIPELINE_MODE_DEFAULT, null, null);
	}

	/**
	 * コンストラクタ
	 * 明示的に#setSurfaceでSurfaceを指定しない場合、GLDrawer2Dで描画した映像を次のパイプラインへ送る
	 * null以外のSurfaceを指定した場合は前のパイプラインからのテクスチャをそのまま次のパイプラインへ送る
	 * @param manager
	 * @param surface nullまたはSurface/SurfaceHolder/SurfaceTexture/SurfaceView
	 * @param maxFps 最大フレームレート, nullまたはFraction#ZEROなら制限なし
	 * @throws IllegalStateException
	 * @throws IllegalArgumentException
	 */
	public DrawerPipeline(
		@NonNull final GLManager manager,
		@Nullable final Object surface, @Nullable final Fraction maxFps)
		throws IllegalStateException, IllegalArgumentException {

		this(manager,  GLDrawer2D.DEFAULT_FACTORY, PIPELINE_MODE_DEFAULT, surface, maxFps);
	}

	/**
	 * コンストラクタ
	 * 明示的に#setSurfaceでSurfaceを指定しない場合、GLDrawer2Dで描画した映像を次のパイプラインへ送る
	 * null以外のSurfaceを指定した場合は前のパイプラインからのテクスチャをそのまま次のパイプラインへ送る
	 * @param manager
	 * @param drawerFactory
	 * @throws IllegalStateException
	 * @throws IllegalArgumentException
	 */
	public DrawerPipeline(
		@NonNull final GLManager manager, @NonNull final GLDrawer2D.DrawerFactory drawerFactory)
		throws IllegalStateException, IllegalArgumentException {

		this(manager,  drawerFactory, PIPELINE_MODE_DEFAULT, null, null);
	}

	/**
	 * コンストラクタ
	 * 対応していないSurface形式の場合はIllegalArgumentExceptionを投げる
	 * #setSurfaceでsurfaceをセットしたかどうかにかかわらずpipelineModeで指定したとおりのテクスチャを次のパイプラインへ送る
	 * @param manager
	 * @param drawerFactory
	 * @param pipelineMode
	 * @param surface nullまたはSurface/SurfaceHolder/SurfaceTexture/SurfaceView/TextureWrapper/IGLSurface/ISurfaceのいずれかまたはその子クラス
	 * @param maxFps 最大フレームレート, nullまたはFraction#ZEROなら制限なし
	 * @throws IllegalStateException
	 * @throws IllegalArgumentException
	 */
	public DrawerPipeline(
		@NonNull final GLManager manager,
		@NonNull final GLDrawer2D.DrawerFactory drawerFactory,
		@PipelineMode final int pipelineMode,
		@Nullable final Object surface, @Nullable final Fraction maxFps)
			throws IllegalStateException, IllegalArgumentException {

		super();
		if (DEBUG) Log.v(TAG, "コンストラクタ:");
		if ((surface != null) && !RendererTarget.isSupportedSurface(surface)) {
			throw new IllegalArgumentException("Unsupported surface type!," + surface);
		}
		mManager = manager;
		mDrawerFactory = drawerFactory;
		mPipelineMode = pipelineMode;
		Matrix.setIdentityM(mMvpMatrix, 0);
		manager.runOnGLThread(() -> {
			createTargetOnGL(surface, maxFps);
		});
	}

	@Override
	protected void internalRelease() {
		if (DEBUG) Log.v(TAG, "internalRelease:");
		if (isValid()) {
			releaseAll();
		}
		super.internalRelease();
	}

	/**
	 * GLSurfacePipelineの実装
	 * 描画先のSurfaceを差し替え, 最大フレームレートの制限をしない
	 * 対応していないSurface形式の場合はIllegalArgumentExceptionを投げる
	 * @param surface nullまたはSurface/SurfaceHolder/SurfaceTexture/SurfaceView/TextureWrapper/IGLSurface/ISurfaceのいずれかまたはその子クラス
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
	 * @param surface nullまたはSurface/SurfaceHolder/SurfaceTexture/SurfaceView/TextureWrapper/IGLSurface/ISurfaceのいずれかまたはその子クラス
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

	/**
	 * 描画先のSurfaceをセットしているかどうか
	 * #isEffectOnlyの符号反転したのものと実質的には同じ
	 * @return
	 */
	@Override
	public boolean hasSurface() {
		mLock.lock();
		try {
			return mSurfaceId != 0;
		} finally {
			mLock.unlock();
		}
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
	public boolean isValid() {
		return super.isValid() && mManager.isValid();
	}

	@Override
	public void setMirror(@MirrorMode final int mirror) {
		if (DEBUG) Log.v(TAG, "setMirror:" + mirror);
		mLock.lock();
		try {
			if (mMirror != mirror) {
				mMirror = mirror;
				mManager.runOnGLThread(() -> {
					if (mSurfaceTarget != null) {
						mSurfaceTarget.setMirror(IMirror.flipVertical(mirror));
					}
					if (mOffscreenTarget != null) {
						mOffscreenTarget.setMirror(mirror);
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

		if ((mDrawer == null) || (isGLES3 != mDrawer.isGLES3) || (isOES != mDrawer.isOES())) {
			// 初回またはGLPipelineを繋ぎ変えたあとにテクスチャが変わるかもしれない
			if (mDrawer != null) {
				mDrawer.release();
				mDrawer = null;
			}
			if (DEBUG) Log.v(TAG, "onFrameAvailable:create GLDrawer2D");
			mDrawer = mDrawerFactory.create(isGLES3, isOES);
			mDrawer.setMvpMatrix(mMvpMatrix, 0);
			if (!isOES) {
				// XXX DrawerPipelineTestでGL_TEXTURE_2D/GL_TEXTURE_EXTERNAL_OESを映像ソースとして
				//     GLUtils#glCopyTextureToBitmapでBitmap変換時のテクスチャ変換行列適用と
				//     DrawerPipelineを0, 1, 2, 3個連結した場合の結果から全ての組み合わせでテストが通るのは、
				//     GLUtils#glCopyTextureToBitmapとは逆で、
				//     ・GL_TEXTURE_EXTERNAL_OESの時はそのまま
				//     ・GL_TEXTURE_2Dの時は上下反転させないとだめみたい
				mDrawer.setMirror(MIRROR_VERTICAL);
			}
			if (mDrawer instanceof GLEffectDrawer2D) {
				((GLEffectDrawer2D) mDrawer).setTexSize(width, height);
			}
		}
		if ((mOffscreenSurface != null)
			&& ((mOffscreenSurface.getWidth() != getWidth()) || (mOffscreenSurface.getHeight() != getHeight()))) {
			// オフスクリーンを使って描画処理したテクスチャを次へ渡すときで
			// オフスクリーンのリサイズが必要なとき
			reCreateTargetOnGL(null, mMaxFps);
		}

		final GLDrawer2D drawer = mDrawer;
		if (drawer != null) {
			renderTarget(drawer, mSurfaceTarget, texId, texMatrix);
			renderTarget(drawer, mOffscreenTarget, texId, texMatrix);
		}
		if (mPathThrough || (mOffscreenTarget == null)) {	// mOffscreenSurfaceのnullチェックはバグ避け
			// こっちはオリジナルのテクスチャを渡す
			super.onFrameAvailable(isGLES3, isOES, width, height, texId, texMatrix);
		} else {
			// 描画処理した後のたテクスチャを次へ渡す
			super.onFrameAvailable(
				isGLES3, mOffscreenSurface.isOES(),
				width, height,
				mOffscreenSurface.getTexId(), mOffscreenSurface.getTexMatrix());
		}
		if (DEBUG && (++cnt % 100) == 0) {
			Log.v(TAG, "onFrameAvailable:path through=" + mPathThrough + "," + cnt);
		}
	}

	private static void renderTarget(
		@NonNull final GLDrawer2D drawer,
		@Nullable final RendererTarget target,
		final int texId, @NonNull @Size(min=16) final float[] texMatrix) {
		if ((target != null)
			&& target.canDraw()) {
			target.draw(drawer, GLES20.GL_TEXTURE0, texId, texMatrix);
		}
	}

	@Override
	public void refresh() {
		super.refresh();
		if (DEBUG) Log.v(TAG, "refresh:");
		// XXX #removeでパイプラインチェーンのどれかを削除するとなぜか映像が表示されなくなってしまうことへのワークアラウンド
		//     パイプライン中のどれかでシェーダーを再生成すると表示されるようになる
		if (isValid()) {
			mManager.runOnGLThread(() -> {
				if (DEBUG) Log.v(TAG, "refresh#run:release drawer");
				if (mDrawer != null) {
					mDrawer.release();
					mDrawer = null;
				}
			});
		}
	}

	@CallSuper
	@Override
	public void resize(final int width, final int height) throws IllegalStateException {
		super.resize(width, height);
		if (DEBUG) Log.v(TAG, String.format("resize:(%dx%d)", width, height));
		mManager.runOnGLThread(() -> {
			if (DEBUG) Log.v(TAG, "resize#run:");
			if (mDrawer != null) {
				mDrawer.release();
				mDrawer = null;
			}
		});
	}

//--------------------------------------------------------------------------------
	private void releaseAll() {
		if (DEBUG) Log.v(TAG, "releaseAll:");
		if (mManager.isValid()) {
			try {
				mManager.runOnGLThread(() -> {
					if (DEBUG) Log.v(TAG, "releaseAll#run:");
					releaseTargetOnGL();
					if (mDrawer != null) {
						mDrawer.release();
						mDrawer = null;
					}
				});
			} catch (final Exception e) {
				if (DEBUG) Log.w(TAG, e);
			}
		} else if (DEBUG) {
			Log.w(TAG, "releaseAll:unexpectedly GLManager is already released!");
		}
	}

	/**
	 * Surface/オフスクリーンへの描画用RendererTargetと関係するオブジェクトを破棄する
	 */
	@WorkerThread
	private void releaseTargetOnGL() {
		mLock.lock();
		try {
			mSurfaceId = 0;
		} finally {
			mLock.unlock();
		}
		if (mSurfaceTarget != null) {
			if (DEBUG) Log.v(TAG, "releaseTargetOnGL:release target for surface");
			mSurfaceTarget.release();
			mSurfaceTarget = null;
		}
		if (mOffscreenTarget != null) {
			if (DEBUG) Log.v(TAG, "releaseTargetOnGL:release target for offscreen");
			mOffscreenTarget.release();
			mOffscreenTarget = null;
		}
		if (mOffscreenSurface != null) {
			if (DEBUG) Log.v(TAG, "releaseTargetOnGL:release offscreen");
			mOffscreenSurface.release();
			mOffscreenSurface = null;
		}
		mPathThrough = true;
	}

	/**
	 * 描画先のSurfaceを生成
	 * @param surface nullまたはSurface/SurfaceHolder/SurfaceTexture/SurfaceView/TextureWrapper/IGLSurface/ISurfaceのいずれかまたはその子クラス
	 * @param maxFps
	 */
	@WorkerThread
	private void createTargetOnGL(@Nullable final Object surface, @Nullable final Fraction maxFps) {
		if (DEBUG) Log.v(TAG, "createTarget:" + surface);
		if ((mSurfaceTarget == null) || (mSurfaceTarget.getSurface() != surface)) {
			reCreateTargetOnGL(surface, maxFps);
		}
	}

	/**
	 * 描画先のSurfaceを生成
	 * @param surface nullまたはSurface/SurfaceHolder/SurfaceTexture/SurfaceView/TextureWrapper/IGLSurface/ISurfaceのいずれかまたはその子クラス
	 * @param maxFps
	 */
	@WorkerThread
	private void reCreateTargetOnGL(@Nullable final Object surface, @Nullable final Fraction maxFps) {
		if (DEBUG) Log.v(TAG, "reCreateTargetOnGL:" + surface);
		mSurfaceId = 0;
		mMaxFps = maxFps;
		releaseTargetOnGL();
		if (isValid()) {
			if (RendererTarget.isSupportedSurface(surface)) {
				// 有効なSurfaceが引き渡されたとき
				mSurfaceTarget = RendererTarget.newInstance(
					mManager.getEgl(), surface, maxFps != null ? maxFps.asFloat() : 0);
			}
			// 前のパイプラインからのテクスチャをそのまま次のパイプラインへ渡すかどうか
			mPathThrough = (mPipelineMode & PIPELINE_MODE_PATH_THROUGH) == PIPELINE_MODE_PATH_THROUGH;
			if (mPipelineMode == PIPELINE_MODE_DEFAULT) {
				// デフォルトの挙動は有効なSurfaceが無い時にfalse
				mPathThrough = mSurfaceTarget != null;
			}
			if (!mPathThrough) {
				if (DEBUG) Log.v(TAG, String.format("reCreateTargetOnGL:create GLSurface as offscreen(%dx%d)", getWidth(), getHeight()));
				mOffscreenSurface = GLSurface.newInstance(
					mManager.isGLES3(), GLES20.GL_TEXTURE0,
					getWidth(), getHeight());
				mOffscreenTarget = RendererTarget.newInstance(
					mManager.getEgl(), mOffscreenSurface, maxFps != null ? maxFps.asFloat() : 0);
			}
		}
		if (mSurfaceTarget != null) {
			mLock.lock();
			try {
				mSurfaceId = mSurfaceTarget.getId();
			} finally {
				mLock.unlock();
			}
			mSurfaceTarget.setMirror(IMirror.flipVertical(mMirror));
		}
		if (mOffscreenTarget != null) {
			mOffscreenTarget.setMirror(mMirror);
		}
	}

}
