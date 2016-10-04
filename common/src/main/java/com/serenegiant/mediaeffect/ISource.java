package com.serenegiant.mediaeffect;
/*
 * Androusb
 * Copyright (c) 2014-2016 saki t_saki@serenegiant.com
 * Distributed under the terms of the GNU Lesser General Public License (LGPL v3.0) License.
 * License details are in the file license.txt, distributed as part of this software.
 */

import com.serenegiant.glutils.TextureOffscreen;

public interface ISource {
	public ISource reset();
	public ISource resize(final int width, final int height);
	/**
	 * IEffectを適用する。1回呼び出す毎に入力と出力のオフスクリーン(テクスチャ)が入れ替わる
	 * @param effect
	 * @return
	 */
	public ISource apply(IEffect effect);
	public int getWidth();
	public int getHeight();
	public int[] getSourceTexId();
	public int getOutputTexId();
	public float[] getTexMatrix();
	public TextureOffscreen getOutputTexture();
	public void release();
}
