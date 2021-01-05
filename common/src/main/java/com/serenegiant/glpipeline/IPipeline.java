package com.serenegiant.glpipeline;
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

import com.serenegiant.glutils.GLManager;

import androidx.annotation.NonNull;

public interface IPipeline {
	/**
	 * 関係するリソースを破棄
	 */
	public void release();

	/**
	 * GLManagerを取得する
	 * @return
	 * @throws IllegalStateException
	 */
	@NonNull
	public GLManager getGLManager() throws IllegalStateException;

	/**
	 * リサイズ要求
	 * @param width
	 * @param height
	 * @throws IllegalStateException
	 */
	public void resize(final int width, final int height) throws IllegalStateException;

	/**
	 * オブジェクトが有効かどうかを取得
	 * @return
	 */
	public boolean isValid();

	/**
	 * 映像幅を取得
	 * @return
	 */
	public int getWidth();

	/**
	 * 映像高さを取得
	 * @return
	 */
	public int getHeight();
}
