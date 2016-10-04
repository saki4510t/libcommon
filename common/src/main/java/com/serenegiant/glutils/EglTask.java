package com.serenegiant.glutils;
/*
 * Androusb
 * Copyright (c) 2014-2016 saki t_saki@serenegiant.com
 * Distributed under the terms of the GNU Lesser General Public License (LGPL v3.0) License.
 * License details are in the file license.txt, distributed as part of this software.
 */

import com.serenegiant.utils.MessageTask;

public abstract class EglTask extends MessageTask {
//	private static final boolean DEBUG = false;
//	private static final String TAG = "EglTask";

	public static final int EGL_FLAG_DEPTH_BUFFER = 1;
	public static final int EGL_FLAG_RECORDABLE = 2;

	private EGLBase mEgl = null;
	private EGLBase.IEglSurface mEglHolder;

	public EglTask(final EGLBase.IContext sharedContext, final int flags) {
//		if (DEBUG) Log.i(TAG, "shared_context=" + shared_context);
		init(flags, 0, sharedContext);
	}

	@Override
	protected void onInit(final int arg1, final int arg2, final Object obj) {
		if ((obj == null) || (obj instanceof EGLBase.IContext))
			mEgl = EGLBase.createFrom((EGLBase.IContext)obj,
				(arg1 & EGL_FLAG_DEPTH_BUFFER) == EGL_FLAG_DEPTH_BUFFER,
				(arg1 & EGL_FLAG_RECORDABLE) == EGL_FLAG_RECORDABLE);
		if (mEgl == null) {
			callOnError(new RuntimeException("failed to create EglCore"));
			releaseSelf();
		} else {
			mEglHolder = mEgl.createOffscreen(1, 1);
			mEglHolder.makeCurrent();
		}
	}

	@Override
	protected Request takeRequest() throws InterruptedException {
		final Request result = super.takeRequest();
		mEglHolder.makeCurrent();
		return result;
	}

	@Override
	protected void onBeforeStop() {
		mEglHolder.makeCurrent();
	}

	@Override
	protected void onRelease() {
		mEglHolder.release();
		mEgl.release();
	}

	protected EGLBase getEgl() {
		return mEgl;
	}

	protected EGLBase.IContext getContext() {
		return mEgl != null ? mEgl.getContext() : null;
	}

	protected void makeCurrent() {
		mEglHolder.makeCurrent();
	}

	protected boolean isGLES3() {
		return (mEgl != null) && (mEgl.getGlVersion() > 2);
	}
}
