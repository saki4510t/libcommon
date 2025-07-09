package com.serenegiant.mediaeffect;
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
import android.os.Build;
import android.util.Log;

import com.serenegiant.gl.GLHistogram;
import com.serenegiant.gl.GLSurface;
import com.serenegiant.glutils.IMirror;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.N)
public class MediaEffectGLHistogram implements IMediaEffect, IMirror  {
	private static final boolean DEBUG = true;
	private static final String TAG = MediaEffectGLHistogram.class.getSimpleName();

	private final GLHistogram mGLHistogram;
	private GLSurface mOutputOffscreen;
	private volatile boolean mEnabled = true;

	/**
	 * コンストラクタ
	 * ヒストグラム平坦化補正は行わない
	 * @param isOES
	 */
	public MediaEffectGLHistogram(final boolean isOES) {
		this(isOES, 2.0f, false);
	}

	/**
	 * コンストラクタ
	 * @param isOES
	 * @param equalize ヒストグラム平坦化補正を行うかどうか
	 */
	public MediaEffectGLHistogram(final boolean isOES, final boolean equalize) {
		this(isOES, 2.0f, equalize);
	}

	/**
	 * コンストラクタ
	 * @param isOES
	 * @param maxFps ヒストグラムの最大更新頻度
	 * @param equalize ヒストグラム平坦化補正を行うかどうか
	 */
	public MediaEffectGLHistogram(
		final boolean isOES,
		final float maxFps,
		final boolean equalize) {
		mGLHistogram = new GLHistogram(isOES, maxFps, equalize);
	}

	@Override
	public void setMirror(final int mirror) {
		mGLHistogram.setMirror(mirror);
	}

	@Override
	public int getMirror() {
		return mGLHistogram.getMirror();
	}

	@Override
	public void apply(@NonNull final ISource src) {
		if (!mEnabled) return;
		final GLSurface output = src.getOutputTargetTexture();
		final int[] srcTexIds = src.getSourceTexId();
		final int width = output.getWidth();;
		final int height = output.getHeight();
		output.makeCurrent();
		if (mGLHistogram.compute(
			width, height,
			GLES20.GL_TEXTURE1, srcTexIds[0], src.getTexMatrix(), 0)) {
			mGLHistogram.equalize();
		}
		output.makeCurrent();
		try {
			mGLHistogram.draw(
				output.getWidth(), output.getHeight(),
				GLES20.GL_TEXTURE0, srcTexIds[0], src.getTexMatrix(), 0);
		} finally {
			output.swap();
		}
	}

	@Override
	public void release() {
		if (DEBUG) Log.v(TAG, "release:");
		mGLHistogram.release();
		if (mOutputOffscreen != null) {
			mOutputOffscreen.release();
			mOutputOffscreen = null;
		}
	}

	@Override
	public IMediaEffect resize(final int width, final int height) {
		return this;
	}

	@Override
	public boolean enabled() {
		return mEnabled;
	}

	@Override
	public IMediaEffect setEnable(final boolean enable) {
		mEnabled = enable;
		return this;
	}
}
