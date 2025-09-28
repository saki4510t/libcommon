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

import android.graphics.SurfaceTexture;
import android.util.Log;
import android.view.Surface;

import com.serenegiant.gl.GLDrawer2D;
import com.serenegiant.gl.GLEffectDrawer2D;
import com.serenegiant.gl.GLManager;
import com.serenegiant.gl.IEffect;
import com.serenegiant.glutils.GLSurfaceReceiver;
import com.serenegiant.glutils.IMirror;
import com.serenegiant.math.Fraction;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;

/**
 * Surface/SurfaceTextureとして受け取った映像へ映像効果を付与した映像を
 * 他のPipelineからテクスチャとして利用可能とするためのヘルパークラス
 * 映像 → Surface → SurfaceEffectSourcePipeline (→ パイプライン)
 * GLSurfaceReceiverでSurfaceを経由して映像を受け取り、EffectPipelineで
 * 映像効果付与を行って次のパイプラインへ流す
 */
public class SurfaceEffectSourcePipeline
	implements GLPipelineSurfaceSource, GLSurfacePipeline, IMirror, IEffect {

	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = SurfaceEffectSourcePipeline.class.getSimpleName();

	@NonNull
	private final GLManager mManager;
	/**
	 * 自分用のGLManagerを保持しているかどうか
	 */
	private final boolean mOwnManager;
	@NonNull
	private final PipelineSourceCallback mCallback;
	@NonNull
	private final GLSurfaceReceiver mReceiver;
	@NonNull
	private final EffectPipeline mEffectPipeline;

	/**
	 * コンストラクタ
	 * 引数のGLManagerのスレッド上で動作する
	 * @param manager
	 * @param width
	 * @param height
	 * @param callback
	 */
	public SurfaceEffectSourcePipeline(
		@NonNull final GLManager manager,
		final int width, final int height,
		@NonNull final PipelineSourceCallback callback) {

		this(manager, null, PIPELINE_MODE_DEFAULT,
			width, height, callback, false);
	}

	/**
	 * コンストラクタ
	 * useSharedContext=falseなら引数のGLManagerのスレッド上で動作する
	 * useSharedContext=trueなら共有コンテキストを使って専用スレッド上で動作する
	 * XXX useSharedContext = trueで共有コンテキストを使ったマルチスレッド処理を有効にするとGPUのドライバー内でクラッシュする端末がある
	 * @param manager
	 * @param drawerFactory
	 * @param pipelineMode
	 * @param width
	 * @param height
	 * @param callback
	 * @param useSharedContext 共有コンテキストを使ってマルチスレッドで処理を行うかどうか
	 */
	public SurfaceEffectSourcePipeline(
		@NonNull final GLManager manager,
		@Nullable final GLEffectDrawer2D.DrawerFactory drawerFactory,
		@PipelineMode final int pipelineMode,
		final int width, final int height,
		@NonNull final PipelineSourceCallback callback,
		final boolean useSharedContext) {

		if (DEBUG) Log.v(TAG, "コンストラクタ:useSharedContext=" + useSharedContext + ",manager=" + manager);
		mOwnManager = useSharedContext;
		if (useSharedContext) {
			if (DEBUG) Log.v(TAG, "コンストラクタ:createShared");
			mManager = manager.createShared(null);
		} else {
			mManager = manager;
		}
		mCallback = callback;
		mEffectPipeline = new EffectPipeline(
			mManager,
			drawerFactory != null ? drawerFactory : GLEffectDrawer2D.DEFAULT_EFFECT_FACTORY,
			pipelineMode, null, null
		);
		mReceiver = new GLSurfaceReceiver(mManager,
			width, height,
			new GLSurfaceReceiver.DefaultCallback(mEffectPipeline) {
				@Override
				public void onCreateInputSurface(@NonNull final Surface surface, final int width, final int height) {
					super.onCreateInputSurface(surface, width, height);
					mCallback.onCreate(surface);
				}

				@Override
				public void onReleaseInputSurface(@NonNull final Surface surface) {
					super.onReleaseInputSurface(surface);
					mCallback.onDestroy();
				}

			}
		);
	}

	@Override
	public void release() {
		if (DEBUG) Log.v(TAG, "release:");
		if (isValid()) {
			mReceiver.release();
		}
		mEffectPipeline.release();
		if (mOwnManager) {
			mManager.release();
		}
	}

	/**
	 * GLPipelineの実装
	 * @param parent
	 */
	@Override
	public void setParent(@Nullable final GLPipeline parent) {
		throw new UnsupportedOperationException("Can't set parent to SurfaceEffectSourcePipeline");
	}

	/**
	 * GLPipelineの実装
	 * @return
	 */
	@Nullable
	@Override
	public GLPipeline getParent() {
		return null;
	}

	/**
	 * GLPipelineの実装
	 * @param pipeline
	 */
	@Override
	public void setPipeline(@Nullable final GLPipeline pipeline) {
		mEffectPipeline.setPipeline(pipeline);
	}

	/**
	 * GLPipelineの実装
	 * @return
	 */
	@Nullable
	@Override
	public GLPipeline getPipeline() {
		return mEffectPipeline.getPipeline();
	}

	/**
	 * GLPipelineの実装
	 */
	@Override
	public void remove() {
		final GLPipeline pipeline = mEffectPipeline.getPipeline();
		mEffectPipeline.setPipeline(null);
		if (pipeline != null) {
			pipeline.release();
		}
	}

	/**
	 * GLPipelineの実装
	 */
	@Override
	public void refresh() {
		mEffectPipeline.refresh();
	}

	/**
	 * GLPipelineの実装
	 * GLManagerを取得する
	 * @return
	 */
	@NonNull
	@Override
	public GLManager getGLManager() throws IllegalStateException {
		checkValid();
		return mManager;
	}

	/**
	 * GLPipelineの実装
	 * リサイズ要求
	 * @param width
	 * @param height
	 */
	@Override
	public void resize(final int width, final int height) throws IllegalStateException {
		if (DEBUG) Log.v(TAG, "resize:");
		checkValid();
		mReceiver.resize(width, height);
		mEffectPipeline.resize(width, height);
	}

	/**
	 * GLPipelineの実装
	 * VideoSourceオブジェクトが有効かどうかを取得
	 * @return
	 */
	@Override
	public boolean isValid() {
		// super#isValidはProxyPipelineなので常にtrueを返す
		return mManager.isValid() && mReceiver.isValid();
	}

	/**
	 * GLPipelineの実装
	 * パイプラインチェーンに組み込まれているかどうかを取得
	 * @return
	 */
	@Override
	public boolean isActive() {
		// 破棄されていない && 子と繋がっている
		return isValid() && mEffectPipeline.isActive();
	}

	/**
	 * GLPipelineの実装
	 * @return
	 */
	@Override
	public int getWidth() {
		return mReceiver.getWidth();
	}

	/**
	 * GLPipelineの実装
	 * @return
	 */
	@Override
	public int getHeight() {
		return mReceiver.getHeight();
	}

	/**
	 * GLSurfacePipelineの実装
	 * @param surface nullまたはSurface/SurfaceHolder/SurfaceTexture/SurfaceView
	 * @throws IllegalStateException
	 * @throws IllegalArgumentException
	 */
	@Override
	public void setSurface(@Nullable final Object surface) throws IllegalStateException, IllegalArgumentException {
		mEffectPipeline.setSurface(surface);
	}

	/**
	 * GLSurfacePipelineの実装
	 * @param surface nullまたはSurface/SurfaceHolder/SurfaceTexture/SurfaceView
	 * @param maxFps 最大フレームレート, nullまたはFraction#ZEROなら制限なし
	 * @throws IllegalStateException
	 * @throws IllegalArgumentException
	 */
	@Override
	public void setSurface(@Nullable final Object surface, @Nullable final Fraction maxFps) throws IllegalStateException, IllegalArgumentException {
		mEffectPipeline.setSurface(surface, maxFps);
	}

	/**
	 * GLSurfacePipelineの実装
	 * @return
	 */
	@Override
	public boolean hasSurface() {
		return mEffectPipeline.hasSurface();
	}

	/**
	 * GLSurfacePipelineの実装
	 * @return
	 */
	@Override
	public int getId() {
		return mEffectPipeline.getId();
	}

	/**
	 * GLPipelineSourceの実装
	 * 映像入力用のSurfaceTextureを取得
	 * @return
	 * @throws IllegalStateException
	 */
	@NonNull
	@Override
	public SurfaceTexture getInputSurfaceTexture() throws IllegalStateException {
		if (DEBUG) Log.v(TAG, "getInputSurfaceTexture:");
		checkValid();
		return mReceiver.getSurfaceTexture();
	}

	/**
	 * GLPipelineSourceの実装
	 * 映像入力用のSurfaceを取得
	 * @return
	 * @throws IllegalStateException
	 */
	@NonNull
	@Override
	public Surface getInputSurface() throws IllegalStateException {
		if (DEBUG) Log.v(TAG, "getInputSurface:");
		checkValid();
		return mReceiver.getSurface();
	}

	/**
	 * GLPipelineSourceの実装
	 * テクスチャ名を取得
	 * @return
	 */
	@Override
	public int getTexId() {
		return mReceiver.getTexId();
	}

	/**
	 * GLPipelineSourceの実装
	 * テクスチャ変換行列を取得
	 * @return
	 */
	@Size(min=16)
	@NonNull
	@Override
	public float[] getTexMatrix() {
		return mReceiver.getTexMatrix();
	}

	/**
	 * GLFrameAvailableCallbackの実装
	 * @param isGLES3
	 * @param isOES
	 * @param width
	 * @param height
	 * @param texId
	 * @param texMatrix
	 */
	@Override
	public void onFrameAvailable(
		final boolean isGLES3, final boolean isOES,
		final int width, final int height,
		final int texId, @NonNull final float[] texMatrix) {
		// ここには来ないはず, 一応EffectPipeline#onFrameAvailableを呼んでおく
		mEffectPipeline.onFrameAvailable(isGLES3, isOES, width, height, texId, texMatrix);
	}

	//--------------------------------------------------------------------------------
	/**
	 * IEffectの実装
	 * @param effect
	 */
	@Override
	public void setEffect(final int effect) {
		mEffectPipeline.setEffect(effect);
	}

	/**
	 * IEffectの実装
	 * @return
	 */
	@Override
	public int getEffect() {
		return mEffectPipeline.getEffect();
	}

	/**
	 * IEffectの実装
	 * @param params
	 */
	@Override
	public void setParams(@NonNull final float[] params) {
		mEffectPipeline.setParams(params);
	}

	/**
	 * IEffectの実装
	 * @param effect EFFECT_NONより大きいこと
	 * @param params
	 * @throws IllegalArgumentException
	 */
	@Override
	public void setParams(final int effect, @NonNull final float[] params) throws IllegalArgumentException {
		mEffectPipeline.setParams(effect, params);
	}

	//--------------------------------------------------------------------------------
	/**
	 * IMirrorの実装
	 * @param mirror 0:通常, 1:左右反転, 2:上下反転, 3:上下左右反転
	 */
	@Override
	public void setMirror(final int mirror) {
		mEffectPipeline.setMirror(mirror);
	}

	/**
	 * IMirrorの実装
	 * @return
	 */
	@Override
	public int getMirror() {
		return mEffectPipeline.getMirror();
	}

	public void setMvpMatrix(@NonNull @Size(min=16) final float[] matrix, final int offset) {
		mEffectPipeline.setMvpMatrix(matrix, offset);
	}

	//--------------------------------------------------------------------------------
	protected void checkValid() throws IllegalStateException {
		if (!mManager.isValid()) {
			throw new IllegalStateException("Already released");
		}
	}
}
