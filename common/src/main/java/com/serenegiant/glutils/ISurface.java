package com.serenegiant.glutils;

public interface ISurface {
	public void release();
	public void makeCurrent();
	public void swap();
	public boolean isValid();

	/**
	 * Viewportを設定
	 * ここで設定した値は次回以降makeCurrentを呼んだときに復帰される
	 * @param x
	 * @param y
	 * @param width
	 * @param height
	 */
	public void setViewPort(final int x, final int y, final int width, final int height);
	/**
	 * 描画領域の幅を取得
	 * @return
	 */
	public int getWidth();

	/**
	 * 描画領域の高さを取得
	 * @return
	 */
	public int getHeight();
}
