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
import android.util.Log;

import com.serenegiant.gl.EffectDrawer2D;
import com.serenegiant.gl.GLDrawer2D;
import com.serenegiant.gl.GLManager;
import com.serenegiant.gl.GLSurface;
import com.serenegiant.gl.GLUtils;
import com.serenegiant.glutils.IMirror;
import com.serenegiant.gl.RendererTarget;
import com.serenegiant.math.Fraction;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;
import androidx.annotation.WorkerThread;

import static com.serenegiant.gl.GLEffect.EFFECT_NON;

/**
 * OpenGL|ESのシェーダーを使って映像効果付与をするGLPipeline実装
 * 描画先のsurfaceにnullを指定すると映像効果を付与したテクスチャを次のGLPipelineへ送る
 * パイプライン → EffectPipeline (→ パイプライン)
 *                (→ Surface)
 */
public class EffectPipeline extends ProxyPipeline
	implements GLSurfacePipeline, IMirror {
	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = EffectPipeline.class.getSimpleName();

	@NonNull
	private final GLManager mManager;

	@Nullable
	private EffectDrawer2D mDrawer;
	private int mEffect = EFFECT_NON;
	@Nullable
	private Fraction mMaxFps;
	@Nullable
	private RendererTarget mRendererTarget;
	/**
	 * 映像効果付与してそのまま次のGLPipelineへ送るかSurfaceへ描画するか
	 * setSurfaceで有効な描画先Surfaceをセットしていればfalse、セットしていなければtrue
	 */
	private volatile boolean mEffectOnly;
	/**
	 * 映像効果付与してそのまま次のGLPipelineへ送る場合のワーク用GLSurface
	 */
	@Nullable
	private GLSurface mOffscreenSurface;
	@MirrorMode
	private int mMirror = MIRROR_NORMAL;
	private int mSurfaceId = 0;

	/**
	 * コンストラクタ
	 * @param manager
	 * @param manager
	 * @throws IllegalStateException
	 * @throws IllegalArgumentException
	 */
	public EffectPipeline(@NonNull final GLManager manager)
			throws IllegalStateException, IllegalArgumentException {
		this(manager,  null, null);
	}

	/**
	 * コンストラクタ
	 * @param manager
	 * 対応していないSurface形式の場合はIllegalArgumentExceptionを投げる
	 * @param surface nullまたはSurface/SurfaceHolder/SurfaceTexture/SurfaceView
	 * @param maxFps 最大フレームレート, nullまたはFraction#ZEROなら制限なし
	 * @throws IllegalStateException
	 * @throws IllegalArgumentException
	 */
	public EffectPipeline(
		@NonNull final GLManager manager,
		@Nullable final Object surface, @Nullable final Fraction maxFps)
			throws IllegalStateException, IllegalArgumentException {

		super();
		if (DEBUG) Log.v(TAG, "コンストラクタ:");
		if ((surface != null) && !GLUtils.isSupportedSurface(surface)) {
			throw new IllegalArgumentException("Unsupported surface type!," + surface);
		}
		mManager = manager;
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
		if ((surface != null) && !GLUtils.isSupportedSurface(surface)) {
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

	/**
	 * 映像効果付与をSurfaceへせずに次のGLPipelineへ送るだけかどうか
	 * コンストラクタまたはsetSurfaceで描画先のsurfaceにnullを指定するとtrue
	 * @return
	 */
	public boolean isEffectOnly() {
		return mEffectOnly;
	}

	@Override
	public void setMirror(@MirrorMode final int mirror) {
		if (DEBUG) Log.v(TAG, "setMirror:" + mirror);
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

	private int cnt;
	@WorkerThread
	@Override
	public void onFrameAvailable(
		final boolean isGLES3,
		final boolean isOES, final int texId,
		@NonNull @Size(min=16) final float[] texMatrix) {

		if (isValid()) {
			if ((mDrawer == null) || (isGLES3 != mDrawer.isGLES3) || (isOES != mDrawer.isOES())) {
				// 初回またはGLPipelineを繋ぎ変えたあとにテクスチャが変わるかもしれない
				if (mDrawer != null) {
					mDrawer.release();
				}
				if (DEBUG) Log.v(TAG, "onFrameAvailable:create GLDrawer2D");
				mDrawer = new EffectDrawer2D(isGLES3, isOES, mEffectListener);
				mDrawer.setMirror(MIRROR_VERTICAL);	// FIXME 他も同じだけどなぜか上下反転させないとテスト通らない
				mDrawer.setEffect(mEffect);
			}
			if (mEffectOnly && (mOffscreenSurface != null)
				&& ((mOffscreenSurface.getWidth() != getWidth()) || (mOffscreenSurface.getHeight() != getHeight()))) {
				// オフスクリーンを使って描画処理したテクスチャを次へ渡すときで
				// オフスクリーンのリサイズが必要なとき
				reCreateTargetOnGL(null, mMaxFps);
			}
			@NonNull
			final EffectDrawer2D drawer = mDrawer;
			@Nullable
			final RendererTarget target = mRendererTarget;
			if ((target != null)
				&& target.canDraw()) {
				target.draw(drawer, GLES20.GL_TEXTURE0, texId, texMatrix);
			}
			if (mEffectOnly && (mOffscreenSurface != null)) {
				if (DEBUG && (++cnt % 100) == 0) {
					Log.v(TAG, "onFrameAvailable:effectOnly," + cnt);
				}
				// 映像効果付与したテクスチャを次へ渡す
				super.onFrameAvailable(isGLES3, mOffscreenSurface.isOES(), mOffscreenSurface.getTexId(), mOffscreenSurface.getTexMatrix());
			} else {
				if (DEBUG && (++cnt % 100) == 0) {
					Log.v(TAG, "onFrameAvailable:" + cnt);
				}
				// こっちはオリジナルのテクスチャを渡す
				super.onFrameAvailable(isGLES3, isOES, texId, texMatrix);
			}
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
				EffectDrawer2D drawer = mDrawer;
				mDrawer = null;
				if (drawer != null) {
					mLock.lock();
					try {
						mEffect = drawer.getCurrentEffect();
					} finally {
						mLock.unlock();
					}
					drawer.release();
				}
			});
		}
	}

//--------------------------------------------------------------------------------
	/**
	 * 映像効果をリセット
	 * @throws IllegalStateException
	 */
	public void resetEffect() throws IllegalStateException {
		if (isValid()) {
			mManager.runOnGLThread(() -> {
				if (mDrawer != null) {
					mDrawer.resetEffect();
					mLock.lock();
					try {
						mEffect = mDrawer.getCurrentEffect();
					} finally {
						mLock.unlock();
					}
				}
			});
		} else {
			throw new IllegalStateException("already released!");
		}
	}

	/**
	 * 映像効果をセット
	 * @param effect
	 * @throws IllegalStateException
	 * @deprecated use #changeEffect instead
	 */
	@Deprecated
	public void setEffect(final int effect) throws IllegalStateException {
		changeEffect(effect);
	}

	/**
	 * 映像効果をセット
	 * @param effect
	 * @throws IllegalStateException
	 */
	public void changeEffect(final int effect) throws IllegalStateException {
		if (DEBUG) Log.v(TAG, "setEffect:" + effect);
		if (isValid()) {
			mManager.runOnGLThread(() -> {
				if (DEBUG) Log.v(TAG, "setEffect#run:" + effect);
				if (mDrawer != null) {
					mDrawer.setEffect(effect);
					mLock.lock();
					try {
						mEffect = mDrawer.getCurrentEffect();
					} finally {
						mLock.unlock();
					}
				}
			});
		} else {
			throw new IllegalStateException("already released!");
		}
	}

	public int getCurrentEffect() {
		if (DEBUG) Log.v(TAG, "getCurrentEffect:" + mDrawer.getCurrentEffect());
		mLock.lock();
		try {
			return mEffect;
		} finally {
			mLock.unlock();
		}
	}

	/**
	 * 現在選択中の映像フィルタにパラメータ配列をセット
	 * 現在対応しているのは色強調用の映像効果のみ(n=12以上必要)
	 * @param params
	 */
	public void setParams(@NonNull final float[] params) throws IllegalStateException {
		setParams(mEffect, params);
	}

	/**
	 * 指定した映像フィルタにパラメータ配列をセット
	 * 現在対応しているのは色強調用の映像効果のみ(n=12以上必要)
	 * @param effect EFFECT_NONより大きいこと
	 * @param params
	 * @throws IllegalStateException
	 */
	public void setParams(final int effect, @NonNull final float[] params)
		throws IllegalStateException {

		if (isValid()) {
			mManager.runOnGLThread(() -> {
				if (DEBUG) Log.v(TAG, "setEffect#run:" + effect);
				if (mDrawer != null) {
					mDrawer.setParams(effect, params);
				}
			});
		} else {
			throw new IllegalStateException("already released!");
		}
	}

	//--------------------------------------------------------------------------------
	private void releaseAll() {
		final EffectDrawer2D drawer = mDrawer;
		final RendererTarget target;
		final GLSurface w;
		mDrawer = null;
		mLock.lock();
		try {
			mSurfaceId = 0;
			target = mRendererTarget;
			mRendererTarget = null;
			w = mOffscreenSurface;
			mOffscreenSurface = null;
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
						if (w != null) {
							w.release();
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

	/**
	 * 描画先のSurfaceを生成
	 * @param surface
	 * @param maxFps
	 */
	@WorkerThread
	private void createTargetOnGL(@Nullable final Object surface, @Nullable final Fraction maxFps) {
		if (DEBUG) Log.v(TAG, "createTarget:" + surface);
		if ((mRendererTarget == null) || (mRendererTarget.getSurface() != surface)) {
			reCreateTargetOnGL(surface, maxFps);
		}
	}

	/**
	 * 描画先のSurfaceを生成
	 * @param surface
	 * @param maxFps
	 */
	@WorkerThread
	private void reCreateTargetOnGL(@Nullable final Object surface, @Nullable final Fraction maxFps) {
		if (DEBUG) Log.v(TAG, "reCreateTargetOnGL:" + surface);
		mSurfaceId = 0;
		mMaxFps = maxFps;
		if (mRendererTarget != null) {
			mRendererTarget.release();
			mRendererTarget = null;
		}
		if (mOffscreenSurface != null) {
			mOffscreenSurface.release();
			mOffscreenSurface = null;
		}
		if (GLUtils.isSupportedSurface(surface)) {
			mRendererTarget = RendererTarget.newInstance(
				mManager.getEgl(), surface, maxFps != null ? maxFps.asFloat() : 0);
			mEffectOnly = false;
		} else {
			if (DEBUG) Log.v(TAG, "createTarget:create GLSurface as work texture");
			mOffscreenSurface = GLSurface.newInstance(
				mManager.isGLES3(), GLES20.GL_TEXTURE0,
				getWidth(), getHeight());
			mRendererTarget = RendererTarget.newInstance(
				mManager.getEgl(), mOffscreenSurface, maxFps != null ? maxFps.asFloat() : 0);
			mEffectOnly = true;
		}
		mLock.lock();
		try {
			mSurfaceId = mRendererTarget.getId();
		} finally {
			mLock.unlock();
		}
		mRendererTarget.setMirror(mMirror);
	}

	final EffectDrawer2D.EffectListener mEffectListener
		= new EffectDrawer2D.EffectListener() {
		@WorkerThread
		@Override
		public boolean onChangeEffect(final int effect, @NonNull final GLDrawer2D drawer) {
			return EffectPipeline.this.onChangeEffect(effect, drawer);
		}
	};

	/**
	 * changeEffectで映像効果を指定したときに内蔵の映像効果設定処理を実行する前に呼ばれる
	 * GLEffect.NON〜GLEffect.EFFECT_NUM(12)-1についてはEffectDrawer2Dで既定の設定が可能
	 * effectにそれ以外を指定し#onChangeEffectがfalse返すとシェーダーがリセットされてEFFECT_NONになる
	 * @param effect
	 * @param drawer
	 * @return trueを返すと処理済みで内蔵の映像効果設定処理を行わない、
	 *         falseを返すと内蔵の映像効果設定処理を行う
	 */
	@WorkerThread
	protected boolean onChangeEffect(final int effect, @NonNull final GLDrawer2D drawer) {
		return false;
	}
}
