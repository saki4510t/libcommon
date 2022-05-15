package com.serenegiant.glpipeline;
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

import com.serenegiant.glutils.EffectDrawer2D;
import com.serenegiant.glutils.GLDrawer2D;
import com.serenegiant.glutils.GLManager;
import com.serenegiant.glutils.GLSurface;
import com.serenegiant.glutils.GLUtils;
import com.serenegiant.glutils.RendererTarget;
import com.serenegiant.math.Fraction;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;
import androidx.annotation.WorkerThread;

import static com.serenegiant.glutils.GLEffect.EFFECT_NON;

/**
 * OpenGL|ESのシェーダーを使って映像効果付与をするIPipeline実装
 * 描画先のsurfaceにnullを指定すると映像効果を付与したテクスチャを次のIPipelineへ送る
 */
public class EffectPipeline extends ProxyPipeline implements ISurfacePipeline {
	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = EffectPipeline.class.getSimpleName();

	/**
	 * パイプラインチェーンに含まれるEffectPipelineを取得する
	 * 複数存在する場合は最初に見つかったものを返す
	 * @param pipeline
	 * @return
	 * @deprecated IPipeline#find(pipeline, clazz)を使う
	 */
	@Deprecated
	@Nullable
	public static EffectPipeline find(@NonNull final IPipeline pipeline) {
		// パイプラインチェーンの先頭を取得
		IPipeline p = IPipeline.findFirst(pipeline);
		// EffectPipelineが見つかるまで順番にたどる
		while (p != null) {
			if (p instanceof EffectPipeline) {
				return (EffectPipeline)p;
			}
			p = p.getPipeline();
		}
		return null;
	}

//--------------------------------------------------------------------------------
	@NonNull
	private final Object mSync = new Object();
	@NonNull
	private final GLManager mManager;

	@Nullable
	private EffectDrawer2D mDrawer;
	private int mEffect = EFFECT_NON;
	@Nullable
	private RendererTarget mRendererTarget;
	/**
	 * 映像効果付与してそのまま次のIPipelineへ送るかSurfaceへ描画するか
	 * setSurfaceで有効な描画先Surfaceをセットしていればfalse、セットしていなければtrue
	 */
	private volatile boolean mEffectOnly;
	/**
	 * 映像効果付与してそのまま次のIPipelineへ送る場合のワーク用GLSurface
	 */
	@Nullable
	private GLSurface work;

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
		manager.runOnGLThread(new Runnable() {
			@WorkerThread
			@Override
			public void run() {
				mDrawer = new EffectDrawer2D(manager.isGLES3(), true, mEffectListener);
				createTarget(surface, maxFps);
			}
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
	 * ISurfacePipelineの実装
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
	 * ISurfacePipelineの実装
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
		mManager.runOnGLThread(new Runnable() {
			@WorkerThread
			@Override
			public void run() {
				createTarget(surface, maxFps);
			}
		});
	}

	/**
	 * 描画先のSurfaceをセットしているかどうか
	 * #isEffectOnlyの符号反転したのものと実質的には同じ
	 * @return
	 */
	@Override
	public boolean hasSurface() {
		synchronized (mSync) {
			return mRendererTarget != null;
		}
	}

	/**
	 * セットされているSurface識別用のidを取得
	 * @return Surfaceがセットされていればそのid(#hashCode)、セットされていなければ0を返す
	 */
	@Override
	public int getId() {
		synchronized (mSync) {
			return mRendererTarget != null ? mRendererTarget.getId() : 0;
		}
	}

	@Override
	public boolean isValid() {
		return super.isValid() && mManager.isValid();
	}

	/**
	 * 映像効果付与をSurfaceへせずに次のIPipelineへ送るだけかどうか
	 * コンストラクタまたはsetSurfaceで描画先のsurfaceにnullを指定するとtrue
	 * @return
	 */
	public boolean isEffectOnly() {
		return mEffectOnly;
	}

	@CallSuper
	@Override
	public void remove() {
		super.remove();
		releaseTarget();
	}

	private int cnt;
	@WorkerThread
	@Override
	public void onFrameAvailable(
		final boolean isOES, final int texId,
		@NonNull @Size(min=16) final float[] texMatrix) {

		if (isValid()) {
			@NonNull
			final EffectDrawer2D drawer;
			@Nullable
			final RendererTarget target;
			if ((mDrawer == null) || (isOES != mDrawer.isOES())) {
				// 初回またはIPipelineを繋ぎ変えたあとにテクスチャが変わるかもしれない
				if (mDrawer != null) {
					mDrawer.release();
				}
				if (DEBUG) Log.v(TAG, "onFrameAvailable:create GLDrawer2D");
				mDrawer = new EffectDrawer2D(mManager.isGLES3(), isOES, mEffectListener);
				mDrawer.setEffect(mEffect);
			}
			drawer = mDrawer;
			synchronized (mSync) {
				target = mRendererTarget;
			}
			if ((target != null)
				&& target.canDraw()) {
				target.draw(drawer.getDrawer(), GLES20.GL_TEXTURE0, texId, texMatrix);
			}
			if (mEffectOnly && (work != null)) {
				if (DEBUG && (++cnt % 100) == 0) {
					Log.v(TAG, "onFrameAvailable:effectOnly," + cnt);
				}
				// 映像効果付与したテクスチャを次へ渡す
				super.onFrameAvailable(work.isOES(), work.getTexId(), work.getTexMatrix());
			} else {
				if (DEBUG && (++cnt % 100) == 0) {
					Log.v(TAG, "onFrameAvailable:" + cnt);
				}
				// こっちはオリジナルのテクスチャを渡す
				super.onFrameAvailable(isOES, texId, texMatrix);
			}
		}
	}

	@Override
	public void refresh() {
		super.refresh();
		// XXX #removeでパイプラインチェーンのどれかを削除するとなぜか映像が表示されなくなってしまうことへのワークアラウンド
		// XXX パイプライン中のどれかでシェーダーを再生成すると表示されるようになる
		if (isValid()) {
			mManager.runOnGLThread(new Runnable() {
				@WorkerThread
				@Override
				public void run() {
					if (DEBUG) Log.v(TAG, "refresh#run:release drawer");
					EffectDrawer2D drawer = mDrawer;
					mDrawer = null;
					if (drawer != null) {
						synchronized (mSync) {
							mEffect = drawer.getCurrentEffect();
						}
						drawer.release();
					}
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
			mManager.runOnGLThread(new Runnable() {
				@WorkerThread
				@Override
				public void run() {
					if (mDrawer != null) {
						mDrawer.resetEffect();
						synchronized (mSync) {
							mEffect = mDrawer.getCurrentEffect();
						}
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
	 */
	public void setEffect(final int effect) throws IllegalStateException {
		if (DEBUG) Log.v(TAG, "setEffect:" + effect);
		if (isValid()) {
			mManager.runOnGLThread(new Runnable() {
				@WorkerThread
				@Override
				public void run() {
					if (DEBUG) Log.v(TAG, "setEffect#run:" + effect);
					if (mDrawer != null) {
						mDrawer.setEffect(effect);
						synchronized (mSync) {
							mEffect = mDrawer.getCurrentEffect();
						}
					}
				}
			});
		} else {
			throw new IllegalStateException("already released!");
		}
	}

	public int getCurrentEffect() {
		if (DEBUG) Log.v(TAG, "getCurrentEffect:" + mDrawer.getCurrentEffect());
		synchronized (mSync) {
			return mEffect;
		}
	}
//--------------------------------------------------------------------------------
	final EffectDrawer2D.EffectListener mEffectListener
		= new EffectDrawer2D.EffectListener() {
			@WorkerThread
			@Override
			public boolean onChangeEffect(final int effect, @NonNull final GLDrawer2D drawer) {
				return EffectPipeline.this.onChangeEffect(effect, drawer);
			}
		};

	/**
	 * 描画先のSurfaceを生成
	 * @param surface
	 * @param maxFps
	 */
	@WorkerThread
	private void createTarget(@Nullable final Object surface, @Nullable final Fraction maxFps) {
		if (DEBUG) Log.v(TAG, "createTarget:" + surface);
		synchronized (mSync) {
			if ((mRendererTarget == null) || (mRendererTarget.getSurface() != surface)) {
				if (mRendererTarget != null) {
					mRendererTarget.release();
					mRendererTarget = null;
				}
				if (work != null) {
					work.release();
					work = null;
				}
				if (GLUtils.isSupportedSurface(surface)) {
					mRendererTarget = RendererTarget.newInstance(
						mManager.getEgl(), surface, maxFps != null ? maxFps.asFloat() : 0);
					mEffectOnly = false;
				} else {
					if (DEBUG) Log.v(TAG, "createTarget:create GLSurface as work texture");
					work = GLSurface.newInstance(mManager.isGLES3(), getWidth(), getHeight());
					mRendererTarget = RendererTarget.newInstance(
						mManager.getEgl(), work, maxFps != null ? maxFps.asFloat() : 0);
					mEffectOnly = true;
				}
			}
		}
	}

	private void releaseTarget() {
		final EffectDrawer2D drawer = mDrawer;
		final RendererTarget target;
		final GLSurface w;
		mDrawer = null;
		synchronized (mSync) {
			target = mRendererTarget;
			mRendererTarget = null;
			w = work;
			work = null;
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
							if (w != null) {
								w.release();
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

	@WorkerThread
	protected boolean onChangeEffect(final int effect, @NonNull final GLDrawer2D drawer) {
		return false;
	}
}
