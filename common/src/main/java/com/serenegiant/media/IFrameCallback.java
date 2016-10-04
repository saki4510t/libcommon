package com.serenegiant.media;
/*
 * Androusb
 * Copyright (c) 2014-2016 saki t_saki@serenegiant.com
 * Distributed under the terms of the GNU Lesser General Public License (LGPL v3.0) License.
 * License details are in the file license.txt, distributed as part of this software.
 */

/**
 * callback interface
 */
public interface IFrameCallback {
	/**
	 * called when preparing finished
	 */
	void onPrepared();
	/**
	 * called when playing finished
	 */
    void onFinished();
    /**
     * called every frame before time adjusting
     * return true if you don't want to use internal time adjustment
     */
    boolean onFrameAvailable(long presentationTimeUs);
}