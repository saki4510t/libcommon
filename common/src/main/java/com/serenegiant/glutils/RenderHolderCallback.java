package com.serenegiant.glutils;
/*
 * Androusb
 * Copyright (c) 2014-2016 saki t_saki@serenegiant.com
 * Distributed under the terms of the GNU Lesser General Public License (LGPL v3.0) License.
 * License details are in the file license.txt, distributed as part of this software.
 */

import android.view.Surface;

/**
 * RenderHolderのコールバックリスナー
 */
public interface RenderHolderCallback {
	public void onCreate(Surface surface);
	public void onFrameAvailable();
	public void onDestroy();
}
