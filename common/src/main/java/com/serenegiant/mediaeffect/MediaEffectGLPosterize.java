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

import static com.serenegiant.gl.ShaderConst.FRAGMENT_SHADER_POSTERIZE_ES2;

/**
 * FIXME ポスタライズ, うまく動かない
 */
public class MediaEffectGLPosterize extends MediaEffectGLBase {
	private static final boolean DEBUG = false;
	private static final String TAG = "MediaEffectGLPosterize";

	public MediaEffectGLPosterize() {
		this(10.0f);
	}

	public MediaEffectGLPosterize(final float posterize) {
		super(new MediaEffectGLColorAdjustDrawer(FRAGMENT_SHADER_POSTERIZE_ES2));
		setParameter(posterize);
	}

	/**
	 * 階調レベルをセット
	 * @param posterize [1,256]
	 * @return
	 */
	public MediaEffectGLPosterize setParameter(final float posterize) {
		((MediaEffectGLColorAdjustDrawer)mDrawer).setColorAdjust(posterize);
		return this;
	}
}
