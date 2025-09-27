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

import com.serenegiant.gl.GLManager;
import com.serenegiant.glutils.GLSurfaceReceiver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;

/**
 * 映像をSurface/SurfaceTextureとして受け取って
 * 他のPipelineからテクスチャとして利用可能とするためのヘルパークラス
 * useSharedContext=false, SurfaceSourcePipeline + SurfaceDistributePipeline ≒ IRendererHolder/RendererHolder
 * 映像 → Surface → SurfaceSourcePipeline (→ パイプライン)
 */
public class SurfaceSourcePipeline extends ProxyPipeline implements GLPipelineSurfaceSource {
	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = SurfaceSourcePipeline.class.getSimpleName();

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

	/**
	 * コンストラクタ
	 * 引数のGLManagerのスレッド上で動作する
	 * @param manager
	 * @param width
	 * @param height
	 * @param callback
	 */
	public SurfaceSourcePipeline(
		@NonNull final GLManager manager,
		final int width, final int height,
		@NonNull final PipelineSourceCallback callback) {

		this(manager, width, height, callback, false);
	}

	/**
	 * コンストラクタ
	 * useSharedContext=falseなら引数のGLManagerのスレッド上で動作する
	 * useSharedContext=trueなら共有コンテキストを使って専用スレッド上で動作する
	 * XXX useSharedContext = trueで共有コンテキストを使ったマルチスレッド処理を有効にするとGPUのドライバー内でクラッシュする端末がある
	 * @param manager
	 * @param width
	 * @param height
	 * @param callback
	 * @param useSharedContext 共有コンテキストを使ってマルチスレッドで処理を行うかどうか
	 */
	public SurfaceSourcePipeline(
		@NonNull final GLManager manager,
		final int width, final int height,
		@NonNull final PipelineSourceCallback callback,
		final boolean useSharedContext) {

		super(width, height);
		if (DEBUG) Log.v(TAG, "コンストラクタ:useSharedContext=" + useSharedContext + ",manager=" + manager);
		mOwnManager = useSharedContext;
		if (useSharedContext) {
			if (DEBUG) Log.v(TAG, "コンストラクタ:createShared");
			mManager = manager.createShared(null);
		} else {
			mManager = manager;
		}
		mCallback = callback;
		mReceiver = new GLSurfaceReceiver(mManager,
			width, height,
			new GLSurfaceReceiver.DefaultCallback(this) {
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
	public void setParent(@Nullable final GLPipeline parent) {
		super.setParent(parent);
		throw new UnsupportedOperationException("Can't set parent to GLPipelineSource");
	}

	/**
	 * GLPipelineの実装
	 * 関連するリソースを廃棄する
	 */
	@Override
	protected void internalRelease() {
		if (DEBUG) Log.v(TAG, "internalRelease:");
		if (isValid()) {
			mReceiver.release();
		}
		if (mOwnManager) {
			mManager.release();
		}
		super.internalRelease();
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
		super.resize(width, height);
		mReceiver.resize(width, height);
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
		mLock.lock();
		try {
			// 破棄されていない && 子と繋がっている
			return isValid() && (getPipeline() != null);
		} finally {
			mLock.unlock();
		}
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

//--------------------------------------------------------------------------------
	protected void checkValid() throws IllegalStateException {
		if (!mManager.isValid()) {
			throw new IllegalStateException("Already released");
		}
	}

}
