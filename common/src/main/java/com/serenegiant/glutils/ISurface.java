package com.serenegiant.glutils;

public interface ISurface {
	public void release();
	public void makeCurrent();
	public void swap();
	public boolean isValid();

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
