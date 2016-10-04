package com.serenegiant.mediaeffect;
/*
 * Androusb
 * Copyright (c) 2014-2016 saki t_saki@serenegiant.com
 * Distributed under the terms of the GNU Lesser General Public License (LGPL v3.0) License.
 * License details are in the file license.txt, distributed as part of this software.
 */

import android.media.effect.EffectContext;
import android.media.effect.EffectFactory;

public class MediaEffectTint extends MediaEffect {
	/**
	 * コンストラクタ
	 * GLコンテキスト内で生成すること
	 *
	 * @param effect_context
	 * @param tint The color of the tint. representing an ARGB color with 8 bits per channel. May be created using Color class.
	 */
	public MediaEffectTint(final EffectContext effect_context, final int tint) {
		super(effect_context, EffectFactory.EFFECT_TINT);
		setParameter(tint);
	}

	/**
	 * @param tint The color of the tint. representing an ARGB color with 8 bits per channel. May be created using Color class.
	 * @return
	 */
	public MediaEffectTint setParameter(final int tint) {
		setParameter("tint", tint);
		return this;
	}
}
