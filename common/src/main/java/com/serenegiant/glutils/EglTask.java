package com.serenegiant.glutils;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2020 saki t_saki@serenegiant.com
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

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.serenegiant.utils.MessageTask;

public abstract class EglTask extends MessageTask {
//	private static final boolean DEBUG = false;
//	private static final String TAG = "EglTask";

	private final GLContext mGLContext;

	@Deprecated
	public EglTask(@Nullable final EGLBase.IContext sharedContext, final int flags) {
		this(3, sharedContext, flags);
	}

	public EglTask(final int maxClientVersion,
		@Nullable final EGLBase.IContext sharedContext, final int flags) {

//		if (DEBUG) Log.i(TAG, "shared_context=" + shared_context);
		mGLContext = new GLContext(maxClientVersion, sharedContext, flags);
		init(0, 0, null);
	}

	/**
	 * MessageTaskの実装
	 * @param flags
	 * @param maxClientVersion
	 * @param sharedContext
	 */
	@WorkerThread
	@Override
	protected void onInit(final int flags,
		final int maxClientVersion, final Object sharedContext) {

		mGLContext.initialize();
	}

	/**
	 * MessageTaskの実装
	 * @return
	 * @throws InterruptedException
	 */
	@Override
	protected Request takeRequest() throws InterruptedException {
		final Request result = super.takeRequest();
		mGLContext.makeDefault();
		return result;
	}

	/**
	 * MessageTaskの実装
	 */
	@WorkerThread
	@Override
	protected void onBeforeStop() {
		mGLContext.makeDefault();
	}

	/**
	 * MessageTaskの実装
	 */
	@WorkerThread
	@Override
	protected void onRelease() {
		mGLContext.release();
	}

//--------------------------------------------------------------------------------
	public GLContext getGLContext() {
		return mGLContext;
	}

	public EGLBase getEgl() {
		return mGLContext.getEgl();
	}

	public EGLBase.IConfig getConfig() {
		return mGLContext.getConfig();
	}

	@Nullable
	public EGLBase.IContext getContext() {
		return mGLContext.getContext();
	}

	public void makeCurrent() {
		mGLContext.makeDefault();
	}

	public boolean isGLES3() {
		return mGLContext.isGLES3();
	}
}
