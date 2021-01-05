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

import android.util.Log;

import static com.serenegiant.glutils.ShaderConst.*;

/** 露出調整, -10〜+10, 0だと無調整 */
public class MediaEffectGLExposure extends MediaEffectGLBase {
	private static final boolean DEBUG = false;
	private static final String TAG = "MediaEffectGLExposure";

	private static final String FRAGMENT_SHADER_BASE = SHADER_VERSION_ES2 +
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
		= String.format(FRAGMENT_SHADER_BASE, HEADER_OES_ES2, SAMPLER_OES);

	public MediaEffectGLExposure() {
		super(new MediaEffectColorAdjustDrawer(FRAGMENT_SHADER));
		if (DEBUG) Log.v(TAG, "コンストラクタ:");
	}

	public MediaEffectGLExposure(final float exposure) {
		this();
		setParameter(exposure);
	}

	/**
	 * 露出調整
	 * @param exposure -10〜+10, 0は無調整
	 * @return
	 */
	public MediaEffectGLExposure setParameter(final float exposure) {
		setEnable(exposure != 0.0f);
		((MediaEffectColorAdjustDrawer)mDrawer).setColorAdjust(exposure);
		return this;
	}

}
