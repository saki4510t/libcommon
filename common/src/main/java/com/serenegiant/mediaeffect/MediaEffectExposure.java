package com.serenegiant.mediaeffect;
/*
 * Androusb
 * Copyright (c) 2014-2016 saki t_saki@serenegiant.com
 * Distributed under the terms of the GNU Lesser General Public License (LGPL v3.0) License.
 * License details are in the file license.txt, distributed as part of this software.
 */

import android.util.Log;

import static com.serenegiant.glutils.ShaderConst.*;

/** 露出調整, -10〜+10, 0だと無調整 */
public class MediaEffectExposure extends MediaEffectGLESBase {
	private static final boolean DEBUG = false;
	private static final String TAG = "MediaEffectExposure";

	private static final String FRAGMENT_SHADER_BASE = SHADER_VERSION +
		"%s" +
		"precision highp float;\n" +
		"varying       vec2 vTextureCoord;\n" +
		"uniform %s    sTexture;\n" +
		"uniform float uColorAdjust;\n" +
		"void main() {\n" +
		"    highp vec4 tex = texture2D(sTexture, vTextureCoord);\n" +
		"    gl_FragColor = vec4(tex.rgb * pow(2.0, uColorAdjust), tex.w);\n" +
		"}\n";
	private static final String FRAGMENT_SHADER
		= String.format(FRAGMENT_SHADER_BASE, HEADER_2D, SAMPLER_2D);
	private static final String FRAGMENT_SHADER_EXT
		= String.format(FRAGMENT_SHADER_BASE, HEADER_OES, SAMPLER_OES);

	public MediaEffectExposure() {
		super(new MediaEffectColorAdjustDrawer(FRAGMENT_SHADER));
		if (DEBUG) Log.v(TAG, "コンストラクタ:");
	}

	public MediaEffectExposure(final float exposure) {
		this();
		setParameter(exposure);
	}

	/**
	 * 露出調整
	 * @param exposure -10〜+10, 0は無調整
	 * @return
	 */
	public MediaEffectExposure setParameter(final float exposure) {
		setEnable(exposure != 0.0f);
		((MediaEffectColorAdjustDrawer)mDrawer).setColorAdjust(exposure);
		return this;
	}

}
