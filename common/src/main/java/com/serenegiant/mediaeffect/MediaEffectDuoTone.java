package com.serenegiant.mediaeffect;
/*
 * Androusb
 * Copyright (c) 2014-2016 saki t_saki@serenegiant.com
 * Distributed under the terms of the GNU Lesser General Public License (LGPL v3.0) License.
 * License details are in the file license.txt, distributed as part of this software.
 */

import android.media.effect.EffectContext;
import android.media.effect.EffectFactory;

public class MediaEffectDuoTone extends MediaEffect {
	/**
	 * コンストラクタ
	 * GLコンテキスト内で生成すること
	 *
	 * @param effect_context
	 * @param first_color The first color tone. representing an ARGB color with 8 bits per channel. May be created using Color class.
	 * @param second_color The second color tone. Integer, representing an ARGB color with 8 bits per channel. May be created using Color class.
	 */
	public MediaEffectDuoTone(final EffectContext effect_context, final int first_color, final int second_color) {
		super(effect_context, EffectFactory.EFFECT_DUOTONE);
		setParameter(first_color, second_color);
	}

	/**
	 * @param first_color The first color tone. representing an ARGB color with 8 bits per channel. May be created using Color class.
	 * @param second_color The second color tone. Integer, representing an ARGB color with 8 bits per channel. May be created using Color class.
	 * @return
	 */
	public MediaEffectDuoTone setParameter(final int first_color, final int second_color) {
		setParameter("first_color", first_color);
		setParameter("second_color", second_color);
		return this;
	}
}
