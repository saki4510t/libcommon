package com.serenegiant.mediaeffect;
/*
 * Androusb
 * Copyright (c) 2014-2016 saki t_saki@serenegiant.com
 * Distributed under the terms of the GNU Lesser General Public License (LGPL v3.0) License.
 * License details are in the file license.txt, distributed as part of this software.
 */

import static com.serenegiant.glutils.ShaderConst.*;

/**
 * FIXME ポスタライズ, うまく動かない
 */
public class MediaEffectPosterize extends MediaEffectGLESBase {
	private static final boolean DEBUG = false;
	private static final String TAG = "MediaEffectBrightness";

	private static final String FRAGMENT_SHADER_BASE = SHADER_VERSION +
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
		= String.format(FRAGMENT_SHADER_BASE, HEADER_OES, SAMPLER_OES);

	public MediaEffectPosterize() {
		this(10.0f);
	}

	public MediaEffectPosterize(final float posterize) {
		super(new MediaEffectColorAdjustDrawer(FRAGMENT_SHADER));
		setParameter(posterize);
	}

	/**
	 * 階調レベルをセット
	 * @param posterize [1,256]
	 * @return
	 */
	public MediaEffectPosterize setParameter(final float posterize) {
		((MediaEffectColorAdjustDrawer)mDrawer).setColorAdjust(posterize);
		return this;
	}
}
