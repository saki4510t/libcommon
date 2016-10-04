package com.serenegiant.mediaeffect;
/*
 * Androusb
 * Copyright (c) 2014-2016 saki t_saki@serenegiant.com
 * Distributed under the terms of the GNU Lesser General Public License (LGPL v3.0) License.
 * License details are in the file license.txt, distributed as part of this software.
 */

import android.media.effect.EffectContext;
import android.media.effect.EffectFactory;

public class MediaEffectStraighten extends MediaEffect {
	/**
	 * コンストラクタ
	 * GLコンテキスト内で生成すること
	 *
	 * @param effect_context
	 * @param angle The angle of rotation. between -45 and +45.
	 */
	public MediaEffectStraighten(final EffectContext effect_context, final float angle) {
		super(effect_context, EffectFactory.EFFECT_STRAIGHTEN);
		setParameter(angle);
	}

	/**
	 * @param angle The angle of rotation. between -45 and +45.
	 * @return
	 */
	public MediaEffectStraighten setParameter(final float angle) {
		setParameter("angle", angle);
		return this;
	}
}
