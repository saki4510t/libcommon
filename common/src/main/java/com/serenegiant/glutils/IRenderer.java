package com.serenegiant.glutils;
/*
 * Androusb
 * Copyright (c) 2014-2016 saki t_saki@serenegiant.com
 * Distributed under the terms of the GNU Lesser General Public License (LGPL v3.0) License.
 * License details are in the file license.txt, distributed as part of this software.
 */

import android.graphics.SurfaceTexture;
import android.view.Surface;

public interface IRenderer extends IRendererCommon {

	/**
	 * 関係するすべてのリソースを開放する。再利用できない
	 */
	public void release();

	/**
	 * 描画先のSurfaceをセット
	 * @param surface
	 */
	public void setSurface(final Surface surface);

	/**
	 * 描画先のSurfaceをセット
	 * @param surface
	 */
	public void setSurface(final SurfaceTexture surface);

	/**
	 * Surfaceサイズを変更
	 * @param width
	 * @param height
	 */
	public void resize(final int width, final int height);

	/**
	 * 描画要求
	 * @param args
	 */
	public void requestRender(final Object... args);
}
