package com.serenegiant.mediaeffect;
/*
 * Androusb
 * Copyright (c) 2014-2016 saki t_saki@serenegiant.com
 * Distributed under the terms of the GNU Lesser General Public License (LGPL v3.0) License.
 * License details are in the file license.txt, distributed as part of this software.
 */

import android.media.effect.EffectContext;
import android.media.effect.EffectFactory;

public class MediaEffectAutoFix extends MediaEffect {
	/**
	 * コンストラクタ
	 * GLコンテキスト内で生成すること
	 *
	 * @param effect_context
	 * @param scale The scale of the adjustment. between 0 and 1. Zero means no adjustment, while 1 indicates the maximum amount of adjustment.
	 */
	public MediaEffectAutoFix(final EffectContext effect_context, final float scale) {
		super(effect_context, EffectFactory.EFFECT_AUTOFIX);
		setParameter(scale);
	}

	/**
	 * @param scale The scale of the adjustment. between 0 and 1. Zero means no adjustment, while 1 indicates the maximum amount of adjustment.
	 * @return
	 */
	public MediaEffectAutoFix setParameter(final float scale) {
		setEnable(scale != 0.0f);
		setParameter("scale", scale);
		return this;
	}
}
