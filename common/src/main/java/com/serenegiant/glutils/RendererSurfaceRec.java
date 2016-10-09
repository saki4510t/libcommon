package com.serenegiant.glutils;

import android.opengl.GLES20;
import android.opengl.Matrix;

/**
 * Created by saki on 2016/10/09.
 * 同じ内容のクラスだったからEffectRendererHolder/RendererHolderのインナークラスを外に出した
 */
class RendererSurfaceRec {
	/** 元々の分配描画用Surface */
	private Object mSurface;
	/** 分配描画用Surfaceを元に生成したOpenGL|ESで描画する為のEglSurface */
	private EGLBase.IEglSurface mTargetSurface;
	final float[] mMvpMatrix = new float[16];

	public RendererSurfaceRec(final EGLBase egl, final Object surface) {
		mSurface = surface;
		mTargetSurface = egl.createFromSurface(surface);
		Matrix.setIdentityM(mMvpMatrix, 0);
	}

	/**
	 * 生成したEglSurfaceを破棄する
	 */
	public void release() {
		if (mTargetSurface != null) {
			mTargetSurface.release();
			mTargetSurface = null;
		}
		mSurface = null;
	}

	public boolean isValid() {
		return (mTargetSurface != null) && mTargetSurface.isValid();
	}

	public boolean canDraw() {
		return true;
	}

	public void draw(final GLDrawer2D drawer, final int textId, final float[] texMatrix) {
		mTargetSurface.makeCurrent();
		// 本来は映像が全面に描画されるので#glClearでクリアする必要はないけどハングアップする機種があるのでクリアしとく
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
		drawer.setMvpMatrix(mMvpMatrix, 0);
		drawer.draw(textId, texMatrix, 0);
		mTargetSurface.swap();
	}

	static class RendererSurfaceRecHasWait extends RendererSurfaceRec {
		private long mPrevDraw;
		private final long mIntervalsNs;
		public RendererSurfaceRecHasWait(final EGLBase egl, final Object surface, final int maxFps) {
			super(egl, surface);
			mIntervalsNs = (maxFps > 0) ? 1000000000L / maxFps : 0L;
			mPrevDraw = System.nanoTime();
		}

		@Override
		public boolean canDraw() {
			return (mIntervalsNs == 0L) || ((System.nanoTime() - mPrevDraw) > mIntervalsNs);
		}

		@Override
		public void draw(final GLDrawer2D drawer, final int textId, final float[] texMatrix) {
			super.draw(drawer, textId, texMatrix);
			mPrevDraw = System.nanoTime();
		}
	}

}
