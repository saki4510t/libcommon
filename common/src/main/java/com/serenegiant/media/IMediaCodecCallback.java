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
public interface IMediaCodecCallback {
	/**
	 * called when preparing finished
	 * @param codec
	 */
	public void onPrepared(IMediaCodec codec);

	/**
	 * called when start playing
	 * @param codec
	 */
	public void onStart(IMediaCodec codec);

	/**
	 * called every frame before time adjusting
	 * return true if you don't want to use internal time adjustment
	 * @param codec
	 * @param presentationTimeUs
	 * @return
	 */
	public boolean onFrameAvailable(IMediaCodec codec, long presentationTimeUs);

	/**
	 * called when playing stopped
	 * @param codec
	 */
	public void onStop(IMediaCodec codec);

	/**
	* called before releasing MediaCodec instance
	 * @param codec
	 */
	public void onRelease(IMediaCodec codec);

	/**
	 * called when error occurred
	 * @param codec
	 * @param e
	 * @return handled
	 */
	public boolean onError(IMediaCodec codec, Exception e);
}
