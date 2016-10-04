package com.serenegiant.mediaeffect;
/*
 * Androusb
 * Copyright (c) 2014-2016 saki t_saki@serenegiant.com
 * Distributed under the terms of the GNU Lesser General Public License (LGPL v3.0) License.
 * License details are in the file license.txt, distributed as part of this software.
 */

import android.media.effect.EffectContext;
import android.media.effect.EffectFactory;

public class MediaEffectContrast extends MediaEffect {
	/**
	 * コンストラクタ
	 * GLコンテキスト内で生成すること
	 *
	 * @param effect_context
	 * @param contrast The contrast multiplier. Float. 1.0 means no change; larger values will increase contrast.
	 */
	public MediaEffectContrast(final EffectContext effect_context, final float contrast) {
		super(effect_context, EffectFactory.EFFECT_CONTRAST);
		setParameter(contrast);
	}

	/**
	 * @param contrast The contrast multiplier. Float. 1.0 means no change; larger values will increase contrast.
	 * @return
	 */
	public MediaEffectContrast setParameter(final float contrast) {
		setParameter("contrast", contrast);
		return this;
	}
}
