package com.serenegiant.glutils;

interface IRendererTarget {
	public void release();
	public boolean isValid();
	public boolean isEnabled();
	public void setEnabled(final boolean enable);
	public boolean canDraw();
	public void draw(final IDrawer2D drawer, final int textId, final float[] texMatrix);
	public void clear(final int color);
	public void makeCurrent() throws IllegalStateException;
	public void swap() throws IllegalStateException;
	public float[] getMvpMatrix();
}
