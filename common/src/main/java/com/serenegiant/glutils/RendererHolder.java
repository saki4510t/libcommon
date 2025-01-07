package com.serenegiant.glutils;
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

import com.serenegiant.egl.EGLBase;
import com.serenegiant.egl.EGLConst;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Hold shared texture that has camera frame and draw them to registered surface if needs<br>
 */
public class RendererHolder extends AbstractRendererHolder {
//	private static final boolean DEBUG = false;	// 実働時はfalseにすること
	private static final String TAG = RendererHolder.class.getSimpleName();

	/**
	 * コンストラクタ
	 * @param width
	 * @param height
	 * @param callback
	 */
	public RendererHolder(final int width, final int height,
		@Nullable final RenderHolderCallback callback) {

		this(width, height,
			3, null, EGLConst.EGL_FLAG_RECORDABLE,
			callback);
	}

	/**
	 * コンストラクタ
	 * @param width
	 * @param height
	 * @param maxClientVersion
	 * @param sharedContext
	 * @param flags
	 * @param callback
	 */
	public RendererHolder(final int width, final int height,
		final int maxClientVersion,
		@Nullable final EGLBase.IContext<?> sharedContext, final int flags,
		@Nullable final RenderHolderCallback callback) {

		super(width, height,
			maxClientVersion, sharedContext, flags, callback);
	}

	@NonNull
	@Override
	protected BaseRendererTask createRendererTask(
		final int width, final int height,
		final int maxClientVersion,
		@Nullable final EGLBase.IContext<?> sharedContext, final int flags) {

		return new BaseRendererTask(this, width, height,
			maxClientVersion, sharedContext, flags, null);
	}
	
}
