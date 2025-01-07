package com.serenegiant.mediaeffect;
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

import androidx.annotation.NonNull;

import com.serenegiant.gl.GLSurface;

public interface ISource {
	/**
	 * オフスクリーンを初期状態に戻す
	 * GLコンテキスト内で呼び出すこと
	 * @return
	 */
	public ISource reset();

	/**
	 * 映像サイズを設定
	 * GLコンテキスト内で呼び出すこと
	 * @param width
	 * @param height
	 * @return
	 */
	public ISource resize(final int width, final int height);
	/**
	 * IEffectを適用する。1回呼び出す毎に入力と出力のオフスクリーン(テクスチャ)が入れ替わる
	 * GLコンテキスト内で呼び出すこと
	 * @param effect
	 * @return
	 */
	public ISource apply(IEffect effect);
	public int getWidth();
	public int getHeight();
	@NonNull
	public int[] getSourceTexId();
	public int getOutputTexId();
	public float[] getTexMatrix();
	public GLSurface getOutputTexture();

	/**
	 * 関係するリソースを破棄
	 * GLコンテキスト内で呼び出すこと
	 */
	public void release();
}
