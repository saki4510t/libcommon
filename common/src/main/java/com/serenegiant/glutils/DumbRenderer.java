package com.serenegiant.glutils;
/*
 * Androusb
 * Copyright (c) 2014-2016 saki t_saki@serenegiant.com
 * Distributed under the terms of the GNU Lesser General Public License (LGPL v3.0) License.
 * License details are in the file license.txt, distributed as part of this software.
 */

import android.graphics.SurfaceTexture;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Surface;

/**
 * OpenGL|ESでのSurfaceへの描画処理をDelegaterを介して行うためのIRenderer
 */
public class DumbRenderer implements IRenderer {
//	private static final boolean DEBUG = BuildConfig.DEBUG && false;
	private static final String TAG = DumbRenderer.class.getSimpleName();

	public interface RendererDelegater {
		public void onStart(final EGLBase eglBase);
		public void onStop(final EGLBase eglBase);
		public void onSetSurface(final EGLBase eglBase, final Object surface);
		public void onResize(final EGLBase eglBase, final int width, final int height);
		public void onDraw(final EGLBase eglBase, final Object... args);
		public void onMirror(final EGLBase eglBase, final int mirror);
	}

	/** レンダリングスレッド用のHandler排他制御用オブジェクト */
	private final Object mSync = new Object();
	private RendererTask mRendererTask;

	public DumbRenderer(final EGLBase.IContext sharedContext, final int flags, final RendererDelegater delegater) {
		mRendererTask = new RendererTask(sharedContext, flags, delegater);
		new Thread(mRendererTask, TAG).start();
		if (!mRendererTask.waitReady()) {
			// 初期化に失敗した時
			throw new RuntimeException("failed to start renderer thread");
		}
	}

	@Override
	public void release() {
		synchronized (mSync) {
			if (mRendererTask != null) {
				// 描画タスクを開放
				mRendererTask.release();
				mRendererTask = null;
			}
		}
	}

	@Override
	public void setSurface(final Surface surface) {
		synchronized (mSync) {
			if (mRendererTask != null) {
				mRendererTask.offer(REQUEST_SET_SURFACE, surface);
			}
		}
	}

	@Override
	public void setSurface(final SurfaceTexture surface) {
		synchronized (mSync) {
			if (mRendererTask != null) {
				mRendererTask.offer(REQUEST_SET_SURFACE, surface);
			}
		}
	}

	@Override
	public void setMirror(final int mirror) {
		synchronized (mSync) {
			if (mRendererTask != null) {
				mRendererTask.offer(REQUEST_MIRROR, mirror % 4);
			}
		}
	}

	@Override
	public void resize(final int width, final int height) {
		synchronized (mSync) {
			if (mRendererTask != null) {
				mRendererTask.offer(REQUEST_RESIZE, width, height);
			}
		}
	}

	@Override
	public void requestRender(final Object... args) {
		synchronized (mSync) {
			if (mRendererTask != null) {
				mRendererTask.offer(REQUEST_DRAW, args);
			}
		}
	}

	private static final int REQUEST_SET_SURFACE = 1;
	private static final int REQUEST_DRAW = 2;
	private static final int REQUEST_RESIZE = 3;
	private static final int REQUEST_MIRROR = 4;

	private static class RendererTask extends EglTask {
		private final RendererDelegater mDelegater;
		/** 最後にレンダリングしたフレームサイズ, 0ならまで一度も描画されていない */
		private int frameWidth, frameHeight, frameRotation;
		/** 描画先Surfaceのサイズ */
		private int surfaceWidth, surfaceHeight;
		/** 映像を左右反転させるかどうか */
		private boolean mirror;

		public RendererTask(final EGLBase.IContext sharedContext, final int flags, @NonNull final RendererDelegater delegater) {
			super(sharedContext, flags);
			mDelegater = delegater;
		}

		@Override
		protected void onStart() {
			makeCurrent();
			try {
				mDelegater.onStart(getEgl());
			} catch (final Exception e) {
				Log.w(TAG, e);
			}
		}

		@Override
		protected void onStop() {
			makeCurrent();
			try {
				mDelegater.onStop(getEgl());
			} catch (final Exception e) {
				Log.w(TAG, e);
			}
		}

		@Override
		protected Object processRequest(final int request, final int arg1, final int arg2, final Object obj) throws TaskBreak {
			switch (request) {
			case REQUEST_SET_SURFACE:
				handleSetSurface(obj);
				break;
			case REQUEST_DRAW:
				handleDraw(obj);
				break;
			case REQUEST_RESIZE:
				handleResize(arg1, arg2);
				break;
			case REQUEST_MIRROR:
				handleMirror(arg1);
				break;
			}
			return null;
		}

		private void handleSetSurface(final Object surface) {
			makeCurrent();
			try {
				mDelegater.onSetSurface(getEgl(), surface);
			} catch (final Exception e) {
				Log.w(TAG, e);
			}
		}

		private void handleResize(final int width, final int height) {
			if ((surfaceWidth != width) || (surfaceHeight != height)) {
				surfaceWidth = width;
				surfaceHeight = height;
				makeCurrent();
				try {
					mDelegater.onResize(getEgl(), width, height);
				} catch (final Exception e) {
					Log.w(TAG, e);
				}
				handleDraw();
			}
		}

		private void handleDraw(final Object... args) {
			makeCurrent();
			try {
				mDelegater.onDraw(getEgl(), args);
			} catch (final Exception e) {
				Log.w(TAG, e);
			}
		}

		private void handleMirror(final int mirror) {
			makeCurrent();
			try {
				mDelegater.onMirror(getEgl(), mirror);
			} catch (final Exception e) {
				Log.w(TAG, e);
			}
		}
	}

}
