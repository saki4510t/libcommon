package com.serenegiant.glutils;
/*
 * Androusb
 * Copyright (c) 2014-2016 saki t_saki@serenegiant.com
 * Distributed under the terms of the GNU Lesser General Public License (LGPL v3.0) License.
 * License details are in the file license.txt, distributed as part of this software.
 */

public interface IRendererCommon {
	// IRendererHolder, CameraViewInterfaceと一致させること
	public static final int MIRROR_NORMAL = 0;
	public static final int MIRROR_HORIZONTAL = 1;
	public static final int MIRROR_VERTICAL = 2;
	public static final int MIRROR_BOTH = 3;
	public static final int MIRROR_NUM = 4;

	/**
	 * 映像を上下左右反転させるかどうかをセット
	 * @param mirror 0:通常, 1:左右反転, 2:上下反転, 3:上下左右反転
	 */
	public void setMirror(final int mirror);
}
