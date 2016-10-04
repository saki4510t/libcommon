package com.serenegiant.mediaeffect;
/*
 * Androusb
 * Copyright (c) 2014-2016 saki t_saki@serenegiant.com
 * Distributed under the terms of the GNU Lesser General Public License (LGPL v3.0) License.
 * License details are in the file license.txt, distributed as part of this software.
 */

import android.graphics.Bitmap;
import android.media.effect.EffectContext;
import android.media.effect.EffectFactory;

public class MediaEffectBitmapOverlay extends MediaEffect {
	/**
	 * コンストラクタ
	 * GLコンテキスト内で生成すること
	 *
	 * @param effect_context
	 * @param bitmap The overlay bitmap.
	 */
	public MediaEffectBitmapOverlay(final EffectContext effect_context, final Bitmap bitmap) {
		super(effect_context, EffectFactory.EFFECT_BITMAPOVERLAY);
		setParameter(bitmap);
	}

	/**
	 * @param bitmap The overlay bitmap.
	 * @return
	 */
	public MediaEffectBitmapOverlay setParameter(final Bitmap bitmap) {
		setParameter("bitmap", bitmap);
		return this;
	}
}
