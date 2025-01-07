package com.serenegiant.widget;
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

public interface IProgressView {
	/**
	 * 最大・最小値をセット
	 * @param min
	 * @param max
	 */
	public void setMinMax(final int min, final int max);

	/**
	 * progress値を設定
	 * 最小値よりも小さければ最小値になる。最大値よりも大きければ最大値になる。
	 * @param progress
	 */
	public void setProgress(final int progress);

	/**
	 * 現在のprogress値を取得
	 * @return
	 */
	public int getProgress();
}
