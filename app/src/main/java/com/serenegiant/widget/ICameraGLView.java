package com.serenegiant.widget;

import android.view.View;

public interface ICameraGLView {
	// View
	public void setOnClickListener(final View.OnClickListener listener);
	public void setOnLongClickListener(final View.OnLongClickListener listener);

	// GLSurfaceView
	public void onResume();
	public void onPause();

	public void setVideoSize(final int width, final int height);
	public void addListener(final CameraDelegator.OnFrameAvailableListener listener);
	public void removeListener(final CameraDelegator.OnFrameAvailableListener listener);
	public void setScaleMode(final int mode);
	public int getScaleMode();
	public int getVideoWidth();
	public int getVideoHeight();
	public void addSurface(final int id, final Object surface,
		final boolean isRecordable);
	public void removeSurface(final int id);
}
