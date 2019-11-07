package com.serenegiant.glutils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class OverlayRendererHolder extends AbstractRendererHolder {
//	private static final boolean DEBUG = false;	// FIXME 実働時はfalseにすること
	private static final String TAG = OverlayRendererHolder.class.getSimpleName();

	public OverlayRendererHolder(final int width, final int height,
		@Nullable final RenderHolderCallback callback) {

		this(width, height,
			3, null, EglTask.EGL_FLAG_RECORDABLE,
			callback);
	}

	public OverlayRendererHolder(final int width, final int height,
		final int maxClientVersion,
		@Nullable final EGLBase.IContext sharedContext, final int flags,
		@Nullable final RenderHolderCallback callback) {

		super(width, height, maxClientVersion, sharedContext, flags, callback);
	}

	@NonNull
	@Override
	protected BaseRendererTask createRendererTask(
		final int width, final int height,
		final int maxClientVersion,
		@Nullable final EGLBase.IContext sharedContext, final int flags) {

		// FIXME 未実装、今はRendererHolderと同じ動作
		return new BaseRendererTask(this, width, height,
			maxClientVersion, sharedContext, flags);
	}
}
