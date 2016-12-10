package com.serenegiant.glutils;

/**
 * Created by saki on 2016/10/19.
 *
 */
public interface IDrawer2D {
	public void release();
	public float[] getMvpMatrix();
	public IDrawer2D setMvpMatrix(final float[] matrix, final int offset);
	public void getMvpMatrix(final float[] matrix, final int offset);
	public void draw(final int texId, final float[] tex_matrix, final int offset);
	public void draw(final ITexture texture);
	public void draw(final TextureOffscreen offscreen);
}
