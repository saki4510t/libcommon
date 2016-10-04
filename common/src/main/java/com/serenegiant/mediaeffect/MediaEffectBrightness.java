package com.serenegiant.mediaeffect;
/*
 * Androusb
 * Copyright (c) 2014-2016 saki t_saki@serenegiant.com
 * Distributed under the terms of the GNU Lesser General Public License (LGPL v3.0) License.
 * License details are in the file license.txt, distributed as part of this software.
 */

import static com.serenegiant.glutils.ShaderConst.*;

/** 明るさ調整([-1.0f,+1.0f], RGB各成分に単純加算), 0だと無調整 */
public class MediaEffectBrightness extends MediaEffectGLESBase {
	private static final boolean DEBUG = false;
	private static final String TAG = "MediaEffectBrightness";

	private static final String FRAGMENT_SHADER_BASE = SHADER_VERSION +
		"%s" +
		"precision highp float;\n" +
		"varying       vec2 vTextureCoord;\n" +
		"uniform %s    sTexture;\n" +
		"uniform float uColorAdjust;\n" +
		"void main() {\n" +
		"    highp vec4 tex = texture2D(sTexture, vTextureCoord);\n" +
		"    gl_FragColor = vec4(tex.rgb + vec3(uColorAdjust, uColorAdjust, uColorAdjust), tex.w);\n" +
		"}\n";
	private static final String FRAGMENT_SHADER
		= String.format(FRAGMENT_SHADER_BASE, HEADER_2D, SAMPLER_2D);
	private static final String FRAGMENT_SHADER_EXT
		= String.format(FRAGMENT_SHADER_BASE, HEADER_OES, SAMPLER_OES);

	public MediaEffectBrightness() {
		this(0.0f);
	}

	public MediaEffectBrightness(final float brightness) {
		super(new MediaEffectColorAdjustDrawer(FRAGMENT_SHADER));
		setParameter(brightness);
	}

	/**
	 * 露出調整
	 * @param brightness [-1.0f,+1.0f], RGB各成分に単純加算)
	 * @return
	 */
	public MediaEffectBrightness setParameter(final float brightness) {
		setEnable(brightness != 0.0f);
		((MediaEffectColorAdjustDrawer)mDrawer).setColorAdjust(brightness);
		return this;
	}
}
