package com.serenegiant.mediaeffect;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2025 saki t_saki@serenegiant.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
*/

import android.media.effect.EffectContext;
import android.media.effect.EffectFactory;

public class MediaEffectContrast extends MediaEffect {
	/**
	 * コンストラクタ
	 * GLコンテキスト内で生成すること
	 *
	 * @param effectContext
	 * @param contrast The contrast multiplier. Float. 1.0 means no change; larger values will increase contrast.
	 */
	public MediaEffectContrast(final EffectContext effectContext, final float contrast) {
		super(effectContext, EffectFactory.EFFECT_CONTRAST);
		setParameter(contrast);
	}

	/**
	 * @param contrast The contrast multiplier. Float. 1.0 means no change; larger values will increase contrast.
	 * @return
	 */
	public MediaEffectContrast setParameter(final float contrast) {
		setParameter("contrast", contrast);
		return this;
	}
}
