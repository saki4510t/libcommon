package com.serenegiant.mediaeffect;
/*
 * Androusb
 * Copyright (c) 2014-2016 saki t_saki@serenegiant.com
 * Distributed under the terms of the GNU Lesser General Public License (LGPL v3.0) License.
 * License details are in the file license.txt, distributed as part of this software.
 */

import android.media.effect.EffectContext;
import android.media.effect.EffectFactory;
import android.text.TextUtils;

public class MediaEffectBackDropper extends MediaEffect {
	/**
	 * コンストラクタ
	 * GLコンテキスト内で生成すること
	 *
	 * @param effect_context
	 * @param source A URI for the background video to use. This parameter must be supplied before calling apply() for the first time.
	 */
	public MediaEffectBackDropper(final EffectContext effect_context, final String source) {
		super(effect_context, EffectFactory.EFFECT_BACKDROPPER);
		setParameter(source);
	}

	/**
	 * @param source A URI for the background video to use. This parameter must be supplied before calling apply() for the first time.
	 * @return
	 */
	public MediaEffectBackDropper setParameter(final String source) {
		if (!TextUtils.isEmpty(source))
			setParameter("source", source);
		return this;
	}
}
