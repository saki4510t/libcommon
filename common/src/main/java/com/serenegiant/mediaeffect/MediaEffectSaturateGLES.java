package com.serenegiant.mediaeffect;
/*
 * Androusb
 * Copyright (c) 2014-2016 saki t_saki@serenegiant.com
 * Distributed under the terms of the GNU Lesser General Public License (LGPL v3.0) License.
 * License details are in the file license.txt, distributed as part of this software.
 */

import static com.serenegiant.glutils.ShaderConst.*;

/** 彩度調整([-1.0f,+1.0f]), 0だと無調整 */
public class MediaEffectSaturateGLES extends MediaEffectGLESBase {
	private static final boolean DEBUG = false;
	private static final String TAG = "MediaEffectBrightness";

	private static final String FRAGMENT_SHADER_BASE = SHADER_VERSION +
		"%s" +
		"precision highp float;\n" +
		"varying       vec2 vTextureCoord;\n" +
		"uniform %s    sTexture;\n" +
		"uniform float uColorAdjust;\n" +
		FUNC_GET_INTENSITY +
		"void main() {\n" +
		"    highp vec4 tex = texture2D(sTexture, vTextureCoord);\n" +
		"    highp float intensity = getIntensity(tex.rgb);\n" +
		"    highp vec3 greyScaleColor = vec3(intensity, intensity, intensity);\n" +
		"    gl_FragColor = vec4(mix(greyScaleColor, tex.rgb, uColorAdjust), tex.w);\n" +
		"}\n";
	private static final String FRAGMENT_SHADER
		= String.format(FRAGMENT_SHADER_BASE, HEADER_2D, SAMPLER_2D);
	private static final String FRAGMENT_SHADER_EXT
		= String.format(FRAGMENT_SHADER_BASE, HEADER_OES, SAMPLER_OES);

	public MediaEffectSaturateGLES() {
		this(0.0f);
	}

	public MediaEffectSaturateGLES(final float saturation) {
		super(new MediaEffectColorAdjustDrawer(FRAGMENT_SHADER));
		setParameter(saturation);
	}

	/**
	 * 彩度調整
	 * @param saturation [-1.0f,+1.0f], 0なら無調整)
	 * @return
	 */
	public MediaEffectSaturateGLES setParameter(final float saturation) {
		((MediaEffectColorAdjustDrawer)mDrawer).setColorAdjust(saturation + 1.0f);
		return this;
	}
}
