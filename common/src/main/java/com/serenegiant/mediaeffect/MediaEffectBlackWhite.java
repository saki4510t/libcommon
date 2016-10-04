package com.serenegiant.mediaeffect;
/*
 * Androusb
 * Copyright (c) 2014-2016 saki t_saki@serenegiant.com
 * Distributed under the terms of the GNU Lesser General Public License (LGPL v3.0) License.
 * License details are in the file license.txt, distributed as part of this software.
 */

import android.media.effect.EffectContext;
import android.media.effect.EffectFactory;

public class MediaEffectBlackWhite extends MediaEffect {
	/**
	 * コンストラクタ
	 * GLコンテキスト内で生成すること
	 *
	 * @param effect_context
	 */
	public MediaEffectBlackWhite(final EffectContext effect_context) {
		this(effect_context, 0.0f, 1.0f);
	}

	/**
	 * コンストラクタ
	 * GLコンテキスト内で生成すること
	 *
	 * @param effect_context
	 * @param black The value of the minimal pixel. 0-1
	 * @param white The value of the maximal pixel. 0-1
	 */
	public MediaEffectBlackWhite(final EffectContext effect_context, final float black, final float white) {
		super(effect_context, EffectFactory.EFFECT_BLACKWHITE);
		setParameter(black, white);
	}

	/**
	 * @param black The value of the minimal pixel. 0-1
	 * @param white The value of the maximal pixel. 0-1
	 * @return
	 */
	public MediaEffectBlackWhite setParameter(final float black, final float white) {
		setParameter("black", black);
		setParameter("white", white);
		return this;
	}
}
