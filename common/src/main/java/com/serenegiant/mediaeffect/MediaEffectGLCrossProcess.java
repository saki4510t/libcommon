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
 * クロスプロセスフィルター
 */
public class MediaEffectGLCrossProcess extends MediaEffectGLBase {
	private static final boolean DEBUG = false;
	private static final String TAG = "MediaEffectGLCrossProcess";

	private static final String FRAGMENT_SHADER_BASE = SHADER_VERSION_ES2 +
		"""
		%s
		precision mediump float;
		uniform %s sTexture;
		varying vec2 vTextureCoord;
		void main() {
			vec4 color = texture2D(sTexture, vTextureCoord);
			vec3 ncolor = vec3(0.0, 0.0, 0.0);
			float value;
			if (color.r < 0.5) {
				value = color.r;
			} else {
				value = 1.0 - color.r;
			}
			float red = 4.0 * value * value * value;
			if (color.r < 0.5) {
				ncolor.r = red;
			} else {
				ncolor.r = 1.0 - red;
			}
			if (color.g < 0.5) {
				value = color.g;
			} else {
				value = 1.0 - color.g;
			}
			float green = 2.0 * value * value;
			if (color.g < 0.5) {
				ncolor.g = green;
			} else {
				ncolor.g = 1.0 - green;
			}
			ncolor.b = color.b * 0.5 + 0.25;
			gl_FragColor = vec4(ncolor.rgb, color.a);
		}
		""";

	private static final String FRAGMENT_SHADER
		= String.format(FRAGMENT_SHADER_BASE, HEADER_2D, SAMPLER_2D);
	private static final String FRAGMENT_SHADER_EXT
		= String.format(FRAGMENT_SHADER_BASE, HEADER_OES_ES2, SAMPLER_OES);

	public MediaEffectGLCrossProcess() {
		super(new MediaEffectGLColorAdjustDrawer(FRAGMENT_SHADER));
	}
}
