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
import static com.serenegiant.gl.ShaderConst.VERTEX_SHADER_ES2;

/** ネガポジ反転フィルター */
public class MediaEffectGLNegative extends MediaEffectGLBase {
	private static final boolean DEBUG = false;
	private static final String TAG = "MediaEffectGLNegative";

	private static final String FRAGMENT_SHADER_BASE_ES2
		= """
		%s
		%s
		precision mediump float;
		varying vec2 vTextureCoord;
		uniform %s sTexture;
		void main() {
			vec4 color = texture2D(sTexture, vTextureCoord);
			gl_FragColor = vec4(1.0 - color.rgb, color.a);
		}
		""";

	private static final String FRAGMENT_SHADER_ES2
		= String.format(FRAGMENT_SHADER_BASE_ES2, SHADER_VERSION_ES2, HEADER_2D, SAMPLER_2D);
	private static final String FRAGMENT_SHADER_EXT_ES2
		= String.format(FRAGMENT_SHADER_BASE_ES2, SHADER_VERSION_ES2, HEADER_OES_ES2, SAMPLER_OES);

	public MediaEffectGLNegative() {
		super(new MediaEffectGLDrawer.MediaEffectSingleDrawer(
			false, VERTEX_SHADER_ES2, FRAGMENT_SHADER_ES2));
	}

}
