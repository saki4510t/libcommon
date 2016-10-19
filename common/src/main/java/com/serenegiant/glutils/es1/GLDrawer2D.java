package com.serenegiant.glutils.es1;

import com.serenegiant.glutils.IDrawer2D;
import com.serenegiant.glutils.ITexture;

/**
 * Created by saki on 2016/10/19.
 *
 */
public class GLDrawer2D implements IDrawer2D {
	@Override
	public void release() {

	}

	@Override
	public float[] getMvpMatrix() {
		return new float[0];
	}

	@Override
	public IDrawer2D setMvpMatrix(final float[] matrix, final int offset) {
		return null;
	}

	@Override
	public void getMvpMatrix(final float[] matrix, final int offset) {

	}

	@Override
	public void draw(final int texId, final float[] tex_matrix, final int offset) {

	}

	@Override
	public void draw(final ITexture texture) {

	}
}
