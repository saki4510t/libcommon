package com.serenegiant.glutils;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2021 saki t_saki@serenegiant.com
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

public interface ISurface {
	public void release();
	public void makeCurrent();
	public void swap();
	public boolean isValid();

	/**
	 * Viewportを設定
	 * ここで設定した値は次回以降makeCurrentを呼んだときに復帰される
	 * @param x
	 * @param y
	 * @param width
	 * @param height
	 */
	public void setViewPort(final int x, final int y, final int width, final int height);
	/**
	 * 描画領域の幅を取得
	 * @return
	 */
	public int getWidth();

	/**
	 * 描画領域の高さを取得
	 * @return
	 */
	public int getHeight();
}
