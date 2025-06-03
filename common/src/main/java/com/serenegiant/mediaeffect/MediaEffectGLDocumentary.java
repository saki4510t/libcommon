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
 * ドキュメンタリーフィルター
 */
public class MediaEffectGLDocumentary extends MediaEffectGLBase {
	private static final boolean DEBUG = false;
	private static final String TAG = "MediaEffectGLDocumentary";

	private static final String FRAGMENT_SHADER_BASE = SHADER_VERSION_ES2 +
		"""
		%s
		precision mediump float;
		uniform %s sTexture;
		varying vec2 vTextureCoord;
		uniform float uColorAdjust;
		const vec2 seed = vec2(1,50);
		const float step_size = 0.01;
		const vec2 vScale = vec2(1.0, 1.0);
		const vec2 vignetteCenter = vec2(0.5, 0.5);
		const vec3 vignetteColor = vec3(0.0 ,0.0, 0.0);
		float rand(vec2 loc) {
			float theta1 = dot(loc, vec2(0.9898, 0.233));
			float theta2 = dot(loc, vec2(12.0, 78.0));
			float value = cos(theta1) * sin(theta2) + sin(theta1) * cos(theta2);
			// keep value of part1 in range: (2^-14 to 2^14).
			float temp = mod(197.0 * value, 1.0) + value;
			float part1 = mod(220.0 * temp, 1.0) + temp;
			float part2 = value * 0.5453;
			float part3 = cos(theta1 + theta2) * 0.43758;
			return fract(part1 + part2 + part3);
		}
		void main() {
			// black white
			vec4 color = texture2D(sTexture, vTextureCoord);
			float dither = rand(vTextureCoord + seed);
			vec3 xform = clamp(2.0 * color.rgb, 0.0, 1.0);
			vec3 temp = clamp(2.0 * (color.rgb + step_size), 0.0, 1.0);
			vec3 new_color = clamp(xform + (temp - xform) * (dither - 0.5), 0.0, 1.0);
			// grayscale
			float gray = dot(new_color, vec3(0.299, 0.587, 0.114));
			new_color = vec3(gray, gray, gray);
			// vignette
			float d = distance(vTextureCoord, vignetteCenter);
			float percent = smoothstep(0.3, 0.75, d) * uColorAdjust;
			gl_FragColor = vec4(mix(new_color.rgb, vignetteColor, percent), color.a);
		}
		""";

	private static final String FRAGMENT_SHADER
		= String.format(FRAGMENT_SHADER_BASE, HEADER_2D, SAMPLER_2D);
	private static final String FRAGMENT_SHADER_EXT
		= String.format(FRAGMENT_SHADER_BASE, HEADER_OES_ES2, SAMPLER_OES);

	public MediaEffectGLDocumentary() {
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
