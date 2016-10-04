package com.serenegiant.mediaeffect;
/*
 * Androusb
 * Copyright (c) 2014-2016 saki t_saki@serenegiant.com
 * Distributed under the terms of the GNU Lesser General Public License (LGPL v3.0) License.
 * License details are in the file license.txt, distributed as part of this software.
 */

import android.media.effect.EffectContext;
import android.media.effect.EffectFactory;

public class MediaEffectCrop extends MediaEffect {
	/**
	 * コンストラクタ
	 * GLコンテキスト内で生成すること
	 *
	 * @param effect_context
	 * @param x The origin's x-value. between 0 and width of the image.
	 * @param y The origin's y-value. between 0 and height of the image.
	 * @param width The width of the cropped image. between 1 and the width of the image minus xorigin.
	 * @param height The height of the cropped image. between 1 and the height of the image minus yorigin.
	 */
	public MediaEffectCrop(final EffectContext effect_context, final int x, final int y, final int width, final int height) {
		super(effect_context, EffectFactory.EFFECT_CROP);
		setParameter(x, y, width, height);
	}

	/**
	 * @param x The origin's x-value. between 0 and width of the image.
	 * @param y The origin's y-value. between 0 and height of the image.
	 * @param width The width of the cropped image. between 1 and the width of the image minus xorigin.
	 * @param height The height of the cropped image. between 1 and the height of the image minus yorigin.
	 * @return
	 */
	public MediaEffectCrop setParameter(final int x, final int y, final int width, final int height) {
		setParameter("xorigin", x);
		setParameter("yorigin", y);
		setParameter("width", width);
		setParameter("height", height);
		return this;
	}
}
