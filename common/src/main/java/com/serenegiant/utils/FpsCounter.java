package com.serenegiant.utils;
/*
 * Androusb
 * Copyright (c) 2014-2016 saki t_saki@serenegiant.com
 * Distributed under the terms of the GNU Lesser General Public License (LGPL v3.0) License.
 * License details are in the file license.txt, distributed as part of this software.
 */

public class FpsCounter {
	private int cnt, prevCnt;
	private long startTime, prevTime;
	private float fps, totalFps;
	public FpsCounter() {
		reset();
	}

	public synchronized FpsCounter reset() {
		cnt = prevCnt = 0;
		startTime = prevTime = System.nanoTime() - 1;
		return this;
	}

	/**
	 * フレームをカウント
	 */
	public synchronized void count() {
		cnt++;
	}

	/**
	 * FPSの値を更新, 1秒程度毎に呼び出す
	 * @return
	 */
	public synchronized FpsCounter update() {
		final long t = System.nanoTime();
		fps = (cnt - prevCnt) * 1000000000.0f / (t - prevTime);
		prevCnt = cnt;
		prevTime = t;
		totalFps = cnt * 1000000000.0f / (t - startTime);
		return this;
	}

	public synchronized float getFps() {
		return fps;
	}

	public synchronized float getTotalFps() {
		return totalFps;
	}
}
