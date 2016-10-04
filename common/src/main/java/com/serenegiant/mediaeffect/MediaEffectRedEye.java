package com.serenegiant.mediaeffect;
/*
 * Androusb
 * Copyright (c) 2014-2016 saki t_saki@serenegiant.com
 * Distributed under the terms of the GNU Lesser General Public License (LGPL v3.0) License.
 * License details are in the file license.txt, distributed as part of this software.
 */

import android.media.effect.EffectContext;
import android.media.effect.EffectFactory;

public class MediaEffectRedEye extends MediaEffect {
	/**
	 * コンストラクタ
	 * GLコンテキスト内で生成すること
	 *
	 * @param effect_context
	 * @param centers Multiple center points (x, y) of the red eye regions. An array of floats, where (f[2*i], f[2*i+1]) specifies the center of the i'th eye. Coordinate values are expected to be normalized between 0 and 1.
	 */
	public MediaEffectRedEye(final EffectContext effect_context, final float[] centers) {
		super(effect_context, EffectFactory.EFFECT_REDEYE);
		setParameter(centers);
	}

	/**
	 * @param centers Multiple center points (x, y) of the red eye regions. An array of floats, where (f[2*i], f[2*i+1]) specifies the center of the i'th eye. Coordinate values are expected to be normalized between 0 and 1.
	 * @return
	 */
	public MediaEffectRedEye setParameter(final float[] centers) {
		setParameter("centers", centers);
		return this;
	}
}
