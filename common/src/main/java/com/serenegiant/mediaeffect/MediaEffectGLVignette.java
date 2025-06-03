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

import static com.serenegiant.gl.ShaderConst.HEADER_2D;
import static com.serenegiant.gl.ShaderConst.HEADER_OES_ES2;
import static com.serenegiant.gl.ShaderConst.SAMPLER_2D;
import static com.serenegiant.gl.ShaderConst.SAMPLER_OES;
import static com.serenegiant.gl.ShaderConst.SHADER_VERSION_ES2;

/**
 * ビネットフィルター
 * XXX とりあえず外周部を暗くする、ぼかす処理も入れた方がいいかも？
 */
public class MediaEffectGLVignette extends MediaEffectGLBase {
	private static final boolean DEBUG = false;
	private static final String TAG = "MediaEffectGLDocumentary";

	private static final String FRAGMENT_SHADER_BASE = SHADER_VERSION_ES2 +
		"""
		%s
		precision mediump float;
		uniform %s sTexture;
		varying vec2 vTextureCoord;
		uniform float uColorAdjust;
		const vec2 vignetteCenter = vec2(0.5, 0.5);
		const vec3 vignetteColor = vec3(0.0 ,0.0, 0.0);
		void main() {
			vec4 color = texture2D(sTexture, vTextureCoord);
			float d = distance(vTextureCoord, vignetteCenter);
			float percent = smoothstep(0.3, 0.75, d) * uColorAdjust;
			gl_FragColor = vec4(mix(color.rgb, vignetteColor, percent), color.a);
		}
		""";

	private static final String FRAGMENT_SHADER
		= String.format(FRAGMENT_SHADER_BASE, HEADER_2D, SAMPLER_2D);
	private static final String FRAGMENT_SHADER_EXT
		= String.format(FRAGMENT_SHADER_BASE, HEADER_OES_ES2, SAMPLER_OES);

	public MediaEffectGLVignette() {
		super(new MediaEffectGLColorAdjustDrawer(FRAGMENT_SHADER));
		setParameter(1f);
	}

	/**
	 * @param scale The scale of vignetting. between 0 and 1. 0 means no change.
	 * @return
	 */
	public void setParameter(final float scale) {
		((MediaEffectGLColorAdjustDrawer)mDrawer).setColorAdjust(scale);
	}

}
