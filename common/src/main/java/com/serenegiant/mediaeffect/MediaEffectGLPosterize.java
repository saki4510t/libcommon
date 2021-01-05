package com.serenegiant.mediaeffect;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2021 saki t_saki@serenegiant.com
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

import static com.serenegiant.glutils.ShaderConst.*;

/**
 * FIXME ポスタライズ, うまく動かない
 */
public class MediaEffectGLPosterize extends MediaEffectGLBase {
	private static final boolean DEBUG = false;
	private static final String TAG = "MediaEffectGLBrightness";

	private static final String FRAGMENT_SHADER_BASE = SHADER_VERSION_ES2 +
		"%s" +
		"precision highp float;\n" +
		"varying       vec2 vTextureCoord;\n" +
		"uniform %s    sTexture;\n" +
		"uniform float uColorAdjust;\n" +
		"void main() {\n" +
		"    vec4 tex = texture2D(sTexture, vTextureCoord);\n" +
		"    gl_FragColor = floor((tex * uColorAdjust) + vec4(0.5)) / uColorAdjust;\n" +
		"}\n";
	private static final String FRAGMENT_SHADER
		= String.format(FRAGMENT_SHADER_BASE, HEADER_2D, SAMPLER_2D);
	private static final String FRAGMENT_SHADER_EXT
		= String.format(FRAGMENT_SHADER_BASE, HEADER_OES_ES2, SAMPLER_OES);

	public MediaEffectGLPosterize() {
		this(10.0f);
	}

	public MediaEffectGLPosterize(final float posterize) {
		super(new MediaEffectColorAdjustDrawer(FRAGMENT_SHADER));
		setParameter(posterize);
	}

	/**
	 * 階調レベルをセット
	 * @param posterize [1,256]
	 * @return
	 */
	public MediaEffectGLPosterize setParameter(final float posterize) {
		((MediaEffectColorAdjustDrawer)mDrawer).setColorAdjust(posterize);
		return this;
	}
}
