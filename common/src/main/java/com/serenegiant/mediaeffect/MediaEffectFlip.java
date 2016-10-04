package com.serenegiant.mediaeffect;
/*
 * Androusb
 * Copyright (c) 2014-2016 saki t_saki@serenegiant.com
 * Distributed under the terms of the GNU Lesser General Public License (LGPL v3.0) License.
 * License details are in the file license.txt, distributed as part of this software.
 */

import android.media.effect.EffectContext;
import android.media.effect.EffectFactory;

public class MediaEffectFlip extends MediaEffect {
	/**
	 * コンストラクタ
	 * GLコンテキスト内で生成すること
	 *
	 * @param effect_context
	 * @param flip_vertical
	 * @param flip_horizontal
	 */
	public MediaEffectFlip(final EffectContext effect_context, final boolean flip_vertical, final boolean flip_horizontal) {
		super(effect_context, EffectFactory.EFFECT_FLIP);
		setParameter(flip_vertical, flip_horizontal);
	}

	/**
	 *
	 * @param flip_vertical
	 * @param flip_horizontal
	 * @return
	 */
	public MediaEffectFlip setParameter(final boolean flip_vertical, final boolean flip_horizontal) {
		setParameter("vertical", flip_vertical);
		setParameter("horizontal", flip_horizontal);
		return this;
	}
}
