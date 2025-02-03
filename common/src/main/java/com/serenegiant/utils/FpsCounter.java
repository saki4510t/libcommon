package com.serenegiant.utils;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2025 saki t_saki@serenegiant.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
*/

import com.serenegiant.system.Time;

import java.util.concurrent.locks.ReentrantLock;

/**
 * フレームレート測定用ヘルパークラス
 */
public class FpsCounter {
	private final ReentrantLock mLock = new ReentrantLock();
	private int mCnt, mPrevCnt;
	private long mStartTime, mPrevTime;
	private float mFps, mTotalFps;

	/**
	 * コンストラクタ
	 */
	public FpsCounter() {
		reset();
	}

	/**
	 * カウンタ・フレームレートをリセット
	 * @return
	 */
	public FpsCounter reset() {
		mLock.lock();
		try {
			mCnt = mPrevCnt = 0;
			mStartTime = mPrevTime = Time.nanoTime() - 1;
		} finally {
			mLock.unlock();
		}

		return this;
	}

	/**
	 * フレームをカウント
	 */
	public void count() {
		mLock.lock();
		try {
			mCnt++;
		} finally {
			mLock.unlock();
		}
	}

	/**
	 * FPSの値を更新, 1秒程度毎に呼び出す
	 * @return
	 */
	public FpsCounter update() {
		final long t = Time.nanoTime();
		int cnt, prevCnt;
		mLock.lock();
		try {
			cnt = mCnt;
			prevCnt = mPrevCnt;
			mPrevCnt = cnt;
		} finally {
			mLock.unlock();
		}
		mFps = (cnt - prevCnt) * 1000000000.0f / (t - mPrevTime);
		mPrevTime = t;
		mTotalFps = cnt * 1000000000.0f / (t - mStartTime);

		return this;
	}

	/**
	 * #update呼び出し時のフレームレートを取得
	 * 原則として#updateを呼び出したのと同じスレッド上で呼び出すこと
	 * @return
	 */
	public float getFps() {
		return mFps;
	}

	/**
	 * #reset呼び出しからの平均フレームレートを取得
	 * 原則として#updateを呼び出したのと同じスレッド上で呼び出すこと
	 * @return
	 */
	public float getTotalFps() {
		return mTotalFps;
	}
}
