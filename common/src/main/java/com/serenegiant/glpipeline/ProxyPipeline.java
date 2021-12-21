package com.serenegiant.glpipeline;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2021 saki t_saki@serenegiant.com
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

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * IPipelineのインターフェースメソッドの中継をするだけのIPipeline実装
 */
public class ProxyPipeline implements IPipeline {
	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = ProxyPipeline.class.getSimpleName();

	private static final int DEFAULT_WIDTH = 640;
	private static final int DEFAULT_HEIGHT = 480;

	@NonNull
	private final Object mSync = new Object();
	private int mWidth, mHeight;
	@Nullable
	private IPipeline mParent;
	@Nullable
	private IPipeline mPipeline;

	/**
	 * デフォルトコンストラクタ
	 */
	public ProxyPipeline() {
		this(DEFAULT_WIDTH, DEFAULT_HEIGHT);
	}

	/**
	 * コンストラクタ
	 * @param width
	 * @param height
	 */
	protected ProxyPipeline(final int width, final int height) {
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

	@CallSuper
	@Override
	public void release() {
		if (DEBUG) Log.v(TAG, "release:" + this);
		final IPipeline pipeline;
		synchronized (mSync) {
			pipeline = mPipeline;
			mPipeline = null;
			mParent = null;
		}
		if (pipeline != null) {
			pipeline.release();
		}
	}

	@CallSuper
	@Override
	public void resize(final int width, final int height) throws IllegalStateException {
		mWidth = width;
		mHeight = height;
		final IPipeline pipeline = getPipeline();
		if (pipeline != null) {
			pipeline.resize(width, height);
		}
	}

	@Override
	public boolean isValid() {
		return true;
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
	 * 呼び出し元のIPipelineインスタンスを設定する
	 * @param parent
	 */
	@CallSuper
	public void setParent(@Nullable final IPipeline parent) {
		if (DEBUG) Log.v(TAG, "setParent:" + this + ",parent=" + parent);
		synchronized (mSync) {
			mParent = parent;
		}
	}
	@Nullable
	@Override
	public IPipeline getParent() {
		synchronized (mSync) {
			return mParent;
		}
	}

	@CallSuper
	@Override
	public void setPipeline(@Nullable final IPipeline pipeline) {
		if (DEBUG) Log.v(TAG, "setPipeline:" + this + ",pipeline=" + pipeline);
		synchronized (mSync) {
			mPipeline = pipeline;
		}
		if (pipeline != null) {
			pipeline.setParent(this);
			pipeline.resize(mWidth, mHeight);
		}
	}

	@Nullable
	public IPipeline getPipeline() {
		synchronized (mSync) {
			return mPipeline;
		}
	}

	@CallSuper
	@Override
	public void remove() {
		if (DEBUG) Log.v(TAG, "remove:" + this);
		IPipeline parent;
		synchronized (mSync) {
			parent = mParent;
			if (mParent != null) {
				mParent.setPipeline(mPipeline);
			}
			mParent = null;
			mPipeline = null;
		}
		if (parent != null) {
			parent = IPipeline.findFirst(parent);
			parent.refresh();
		}
	}

	@CallSuper
	@Override
	public void onFrameAvailable(final boolean isOES, final int texId, @NonNull final float[] texMatrix) {
		final IPipeline pipeline;
		synchronized (mSync) {
			pipeline = mPipeline;
		}
		if (pipeline != null) {
			pipeline.onFrameAvailable(isOES, texId, texMatrix);
		}
	}

	@Override
	public void refresh() {
		final IPipeline pipeline;
		synchronized (mSync) {
			pipeline = mPipeline;
		}
		if (pipeline != null) {
			pipeline.refresh();
		}
	}
}
