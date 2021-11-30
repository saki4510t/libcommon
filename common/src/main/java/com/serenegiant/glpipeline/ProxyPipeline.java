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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * IPipelineのインターフェースメソッドの中継をするだけのIPipeline実装
 */
public class ProxyPipeline implements IPipeline {
	@NonNull
	private final Object mSync = new Object();
	private int mWidth, mHeight;
	@Nullable
	private IPipeline mPipeline;

	@Override
	public void release() {
		// do nothing
	}

	@Override
	public void resize(final int width, final int height) throws IllegalStateException {
		mWidth = width;
		mHeight = height;
		final IPipeline pipeline;
		synchronized (mSync) {
			pipeline = mPipeline;
		}
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

	@Override
	public void setPipeline(@Nullable final IPipeline pipeline) {
		synchronized (mSync) {
			mPipeline = pipeline;
		}
		if (pipeline != null) {
			pipeline.resize(mWidth, mHeight);
		}
	}

	@Override
	public void onFrameAvailable(final int texId, @NonNull final float[] texMatrix) {
		final IPipeline pipeline;
		synchronized (mSync) {
			pipeline = mPipeline;
		}
		if (pipeline != null) {
			pipeline.onFrameAvailable(texId, texMatrix);
		}
	}

}
