package com.serenegiant.mediaeffect;
/*
 * Androusb
 * Copyright (c) 2014-2016 saki t_saki@serenegiant.com
 * Distributed under the terms of the GNU Lesser General Public License (LGPL v3.0) License.
 * License details are in the file license.txt, distributed as part of this software.
 */

import android.media.effect.EffectContext;
import android.media.effect.EffectFactory;

public class MediaEffectNull extends MediaEffect {
	/**
	 * コンストラクタ
	 * GLコンテキスト内で生成すること
	 * 入力テクスチャを無変換で出力テクスチャにコピーする
	 * @param effect_context
	 */
	public MediaEffectNull(final EffectContext effect_context) {
		super(effect_context, EffectFactory.EFFECT_AUTOFIX);
		setParameter("scale", 0.0f);	// scale=0.0fならコピー
	}

}
