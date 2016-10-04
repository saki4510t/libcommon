package com.serenegiant.media;
/*
 * Androusb
 * Copyright (c) 2014-2016 saki t_saki@serenegiant.com
 * Distributed under the terms of the GNU Lesser General Public License (LGPL v3.0) License.
 * License details are in the file license.txt, distributed as part of this software.
 */

import java.io.IOException;

public interface IMediaCodec {
	static final int TIMEOUT_USEC = 10000;	// 10[msec]
	public void prepare() throws IOException;
	public void start();
	public void stop();
	public void release();
	public boolean isPrepared();
	public boolean isRunning();
}
