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

/** 球状フィルター */
public class MediaEffectGLSphere extends MediaEffectGLBase {
	private static final boolean DEBUG = false;
	private static final String TAG = "MediaEffectGLSphere";

	private static final String FRAGMENT_SHADER_BASE_ES2
		= """
		%s
		%s
		precision mediump float;
		varying vec2 vTextureCoord;
		uniform %s sTexture;
		uniform float uColorAdjust;
		const vec2 center = vec2(0.5, 0.5);
		const float radius = 0.48;	// ちょっとだけ小さくしておく
		const float refractiveIndex = 0.71;
		void main() {
			vec2 textureCoordinateToUse = vec2(vTextureCoord.x, (vTextureCoord.y * uColorAdjust + 0.5 - 0.5 * uColorAdjust));
			float distanceFromCenter = distance(center, textureCoordinateToUse);
			float checkForPresenceWithinSphere = step(distanceFromCenter, radius);
			distanceFromCenter = distanceFromCenter / radius;
			float normalizedDepth = radius * sqrt(1.0 - distanceFromCenter * distanceFromCenter);
			vec3 sphereNormal = normalize(vec3(textureCoordinateToUse - center, normalizedDepth));
			vec3 refractedVector = 2.0 * refract(vec3(0.0, 0.0, -1.0), sphereNormal, refractiveIndex);
			refractedVector.xy = -refractedVector.xy;
			vec3 finalSphereColor = texture2D(sTexture, (refractedVector.xy + 1.0) * 0.5).rgb;
			gl_FragColor = vec4(finalSphereColor, 1.0) * checkForPresenceWithinSphere;
		}
		""";

	private static final String FRAGMENT_SHADER_ES2
		= String.format(FRAGMENT_SHADER_BASE_ES2, SHADER_VERSION_ES2, HEADER_2D, SAMPLER_2D);
	private static final String FRAGMENT_SHADER_EXT_ES2
		= String.format(FRAGMENT_SHADER_BASE_ES2, SHADER_VERSION_ES2, HEADER_OES_ES2, SAMPLER_OES);

	public MediaEffectGLSphere() {
		this(1.0f);
	}

	public MediaEffectGLSphere(final float aspectRatio) {
		super(new MediaEffectGLColorAdjustDrawer(
			false, VERTEX_SHADER_ES2, FRAGMENT_SHADER_ES2));
		setAspectRatio(aspectRatio);
	}

	public void setAspectRatio(final float aspectRatio) {
		((MediaEffectGLColorAdjustDrawer)mDrawer).setColorAdjust(aspectRatio);
	}
}
