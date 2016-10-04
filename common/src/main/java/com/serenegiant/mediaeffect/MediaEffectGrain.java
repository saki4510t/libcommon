package com.serenegiant.mediaeffect;
/*
 * Androusb
 * Copyright (c) 2014-2016 saki t_saki@serenegiant.com
 * Distributed under the terms of the GNU Lesser General Public License (LGPL v3.0) License.
 * License details are in the file license.txt, distributed as part of this software.
 */

import android.media.effect.EffectContext;
import android.media.effect.EffectFactory;

public class MediaEffectGrain extends MediaEffect {
	/**
	 * コンストラクタ
	 * GLコンテキスト内で生成すること
	 *
	 * @param effect_context
	 * @param strength The strength of the grain effect. between 0 and 1. Zero means no change.
	 */
	public MediaEffectGrain(final EffectContext effect_context, final float strength) {
		super(effect_context, EffectFactory.EFFECT_GRAIN);
		setParameter(strength);
	}

	/**
	 * @param strength The strength of the grain effect. between 0 and 1. Zero means no change.
	 * @return
	 */
	public MediaEffectGrain setParameter(final float strength) {
		if ((strength >= 0) && (strength <= 1.0f))
			setParameter("strength", strength);
		return this;
	}
}
