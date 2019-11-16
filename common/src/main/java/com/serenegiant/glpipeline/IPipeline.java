package com.serenegiant.glpipeline;

import com.serenegiant.glutils.GLManager;

public interface IPipeline {
	/**
	 * 関係するリソースを破棄
	 */
	public void release();

	/**
	 * GLManagerを取得する
	 * @return
	 * @throws IllegalStateException
	 */
	public GLManager getGLManager() throws IllegalStateException;

	/**
	 * リサイズ要求
	 * @param width
	 * @param height
	 * @throws IllegalStateException
	 */
	public void resize(final int width, final int height) throws IllegalStateException;

	/**
	 * オブジェクトが有効かどうかを取得
	 * @return
	 */
	public boolean isValid();

	/**
	 * 映像幅を取得
	 * @return
	 */
	public int getWidth();

	/**
	 * 映像高さを取得
	 * @return
	 */
	public int getHeight();
}
