package com.serenegiant.glutils;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

public class EglTaskDelegator {
	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = EglTaskDelegator.class.getSimpleName();

	public static final int EGL_FLAG_DEPTH_BUFFER = 0x01;
	public static final int EGL_FLAG_RECORDABLE = 0x02;
	public static final int EGL_FLAG_STENCIL_1BIT = 0x04;
	public static final int EGL_FLAG_STENCIL_2BIT = 0x08;
	public static final int EGL_FLAG_STENCIL_4BIT = 0x10;
	public static final int EGL_FLAG_STENCIL_8BIT = 0x20;

	private final int mMaxClientVersion;
	@Nullable
	private final EGLBase.IContext mSharedContext;
	private final int mFlags;

	@Nullable
	private EGLBase mEgl = null;
	@Nullable
	private EGLBase.IEglSurface mEglMasterSurface;

	/**
	 * コンストラクタ
	 * @param maxClientVersion
	 * @param sharedContext
	 * @param flags
	 */
	public EglTaskDelegator(final int maxClientVersion,
		@Nullable final EGLBase.IContext sharedContext, final int flags) {

		mMaxClientVersion = maxClientVersion;
		mSharedContext = sharedContext;
		mFlags = flags;
	}

	@WorkerThread
	public void init()  {
		if ((mSharedContext == null)
			|| (mSharedContext instanceof EGLBase.IContext)) {

			final int stencilBits
				= (mFlags & EGL_FLAG_STENCIL_1BIT) == EGL_FLAG_STENCIL_1BIT ? 1
					: ((mFlags & EGL_FLAG_STENCIL_8BIT) == EGL_FLAG_STENCIL_8BIT ? 8 : 0);
			mEgl = EGLBase.createFrom(mMaxClientVersion, mSharedContext,
				(mFlags & EGL_FLAG_DEPTH_BUFFER) == EGL_FLAG_DEPTH_BUFFER,
				stencilBits,
				(mFlags & EGL_FLAG_RECORDABLE) == EGL_FLAG_RECORDABLE);
		}
		if (mEgl == null) {
			throw new RuntimeException("failed to create EglCore");
		} else {
			mEglMasterSurface = mEgl.createOffscreen(1, 1);
			mEglMasterSurface.makeCurrent();
		}
	}

	@WorkerThread
	public void release() {
		if (mEglMasterSurface != null) {
			mEglMasterSurface.release();
			mEglMasterSurface = null;
		}
		if (mEgl != null) {
			mEgl.release();
			mEgl = null;
		}
	}

	public EGLBase getEgl() {
		return mEgl;
	}

	public EGLBase.IContext getEGLContext() {
		return mEgl.getContext();
	}

	public EGLBase.IConfig getConfig() {
		return mEgl.getConfig();
	}

	@Nullable
	public EGLBase.IContext getContext() {
		return mEgl != null ? mEgl.getContext() : null;
	}

	@WorkerThread
	public void makeCurrent() {
		mEglMasterSurface.makeCurrent();
	}

	public boolean isGLES3() {
		return getGlVersion() > 2;
	}

	public int getGlVersion() {
		return mEgl != null ? mEgl.getGlVersion() : 0;
	}
}
