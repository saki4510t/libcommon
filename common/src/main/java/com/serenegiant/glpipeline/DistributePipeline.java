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

import android.util.Log;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.locks.ReentrantLock;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;

/**
 * 複数のGLPipelineへテクスチャを分配するGLPipeline実装
 * SurfaceDistributePipelineはSurfaceへ分配描画するのに対し
 * DistributePipelineは下流のGLPipeline#onFrameAvailableを呼び出すだけ
 * パイプライン → DistributePipeline (→ パイプライン)
 *                → パイプライン
 *                → パイプライン
 *                → ...
 */
public class DistributePipeline implements GLPipeline {
	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = DistributePipeline.class.getSimpleName();

	private static final int DEFAULT_WIDTH = 640;
	private static final int DEFAULT_HEIGHT = 480;

	/**
	 * 排他制御用
	 */
	@NonNull
	private final ReentrantLock mLock = new ReentrantLock();
	private int mWidth, mHeight;
	@Nullable
	private GLPipeline mParent;
	@NonNull
	private final Set<GLPipeline> mPipelines = new CopyOnWriteArraySet<>();
	private volatile boolean mReleased = false;

	/**
	 * デフォルトコンストラクタ
	 */
	public DistributePipeline() {
		this(DEFAULT_WIDTH, DEFAULT_HEIGHT);
	}

	/**
	 * コンストラクタ
	 * @param width
	 * @param height
	 */
	protected DistributePipeline(final int width, final int height) {
		if (DEBUG) Log.v(TAG, "コンストラクタ:");
		if ((width > 0) || (height > 0)) {
			mWidth = width;
			mHeight = height;
		} else {
			mWidth = DEFAULT_WIDTH;
			mHeight = DEFAULT_HEIGHT;
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

	@Override
	public final void release() {
		if (!mReleased) {
			mReleased = true;
			internalRelease();
		}
	}

	@CallSuper
	protected void internalRelease() {
		if (DEBUG) Log.v(TAG, "internalRelease:" + this);
		mReleased = true;
		mLock.lock();
		try {
			mParent = null;
		} finally {
			mLock.unlock();
		}
		for (final GLPipeline pipeline: mPipelines) {
			pipeline.release();
		}
		mPipelines.clear();
	}

	@CallSuper
	@Override
	public void resize(final int width, final int height) throws IllegalStateException {
		if (!mReleased) {
			mWidth = width;
			mHeight = height;
			for (final GLPipeline pipeline: mPipelines) {
				pipeline.resize(width, height);
			}
		} else {
			throw new IllegalStateException("already released!");
		}
	}

	/**
	 * GLPipelineの実装
	 * オブジェクトが有効かどうかを取得
	 * @return
	 */
	@Override
	public boolean isValid() {
		return !mReleased;
	}

	/**
	 * GLPipelineの実装
	 * パイプラインチェーンに組み込まれているかどうかを取得
	 * @return
	 */
	public boolean isActive() {
		mLock.lock();
		try {
			return !mReleased && (mParent != null);
		} finally {
			mLock.unlock();
		}
	}

	@Override
	public int getWidth() {
		return mWidth;
	}

	@Override
	public int getHeight() {
		return mHeight;
	}

	/**
	 * 呼び出し元のGLPipelineインスタンスを設定する
	 * @param parent
	 */
	@CallSuper
	public void setParent(@Nullable final GLPipeline parent) {
		if (!mReleased) {
			if (DEBUG) Log.v(TAG, "setParent:" + this + ",parent=" + parent);
			mLock.lock();
			try {
				mParent = parent;
			} finally {
				mLock.unlock();
			}
		} else {
			throw new IllegalStateException("already released!");
		}
	}

	@Nullable
	@Override
	public GLPipeline getParent() {
		mLock.lock();
		try {
			return mParent;
		} finally {
			mLock.unlock();
		}
	}

	@CallSuper
	public void addPipeline(@NonNull final GLPipeline pipeline) {
		if (!mReleased) {
			mPipelines.add(pipeline);
			pipeline.setParent(this);
			pipeline.resize(mWidth, mHeight);
		} else {
			throw new IllegalStateException("already released!");
		}
	}

	@CallSuper
	public void removePipeline(@NonNull final GLPipeline pipeline) {
		if (!mReleased) {
			mPipelines.remove(pipeline);
			pipeline.setParent(null);
		} else {
			throw new IllegalStateException("already released!");
		}
	}

	/**
	 * 下流のGLPipelineを追加する
	 * 通常のGLPipelineと異なり
	 * ・2回以上呼び出した場合も置換ではなく追加となる
	 * ・nullを渡すとIllegalArgumentExceptionを投げる
	 * #addPipeline/#removePipelineを使う方が良い
	 * @param pipeline
	 */
	@CallSuper
	@Override
	public void setPipeline(@Nullable final GLPipeline pipeline) {
		if (DEBUG) Log.v(TAG, "setPipeline:" + this + ",pipeline=" + pipeline);
		if (pipeline == null) {
			throw new IllegalArgumentException("DistributePipeline#setPipeline can't accept null!");
		} else {
			addPipeline(pipeline);
		}
	}

	@Nullable
	public GLPipeline getPipeline() {
		// 最初に見つかった物またはnullを返す
		for (final GLPipeline pipeline: mPipelines) {
			return pipeline;
		}
		return null;
	}

	/**
	 * 他のGLPipelineと違って自分の下流に複数のGLPipelineが存在している可能性があるので
	 * 下流が1つでないければ自動的にはつなぎ替えることはできない
	 */
	@CallSuper
	@Override
	public void remove() {
		if (DEBUG) Log.v(TAG, "remove:" + this);
		final GLPipeline first = GLPipeline.findFirst(this);
		GLPipeline parent;
		mLock.lock();
		try {
			parent = mParent;
			if (mParent != null) {
				if (mPipelines.size() == 1) {
					// 下流が1つだけならつなぎ替える
					mParent.setPipeline(getPipeline());
				} else {
					mParent.setPipeline(null);
					Log.d(TAG, "#remove can't rebuild pipeline chain!");
				}
			}
			mParent = null;
		} finally {
			mLock.unlock();
		}
		if (first != this) {
			GLPipeline.validatePipelineChain(first);
			parent.refresh();
		}
	}

	@CallSuper
	@Override
	public void onFrameAvailable(
		final boolean isGLES3,
		final boolean isOES,
		final int width, final int height,
		final int texId, @NonNull @Size(min=16) final float[] texMatrix) {

		if (!mReleased) {
			for (final GLPipeline pipeline: mPipelines) {
				if (pipeline != null) {
					pipeline.onFrameAvailable(isGLES3, isOES, width, height, texId, texMatrix);
				}
			}
		}
	}

	@Override
	public void refresh() {
		if (!mReleased) {
			for (final GLPipeline pipeline: mPipelines) {
				pipeline.refresh();
			}
		}
	}
}
