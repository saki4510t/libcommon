package com.serenegiant.mediaeffect;
/*
 * Androusb
 * Copyright (c) 2014-2016 saki t_saki@serenegiant.com
 * Distributed under the terms of the GNU Lesser General Public License (LGPL v3.0) License.
 * License details are in the file license.txt, distributed as part of this software.
 */

import android.media.effect.EffectContext;
import android.media.effect.EffectFactory;

public class MediaEffectSaturate extends MediaEffect {
	/**
	 * コンストラクタ
	 * GLコンテキスト内で生成すること
	 *
	 * @param effect_context
	 * @param scale The scale of color saturation. between -1 and 1. 0 means no change, while -1 indicates full desaturation, i.e. grayscale.
	 */
	public MediaEffectSaturate(final EffectContext effect_context, final float scale) {
		super(effect_context, EffectFactory.EFFECT_SATURATE);
		setParameter(scale);
	}

	/**
	 * @param saturation The scale of color saturation. between -1 and 1. 0 means no change, while -1 indicates full desaturation, i.e. grayscale.
	 * @return
	 */
	public MediaEffectSaturate setParameter(final float saturation) {
		setEnable(saturation != 0.0f);
		setParameter("scale", saturation);
		return this;
	}
}
