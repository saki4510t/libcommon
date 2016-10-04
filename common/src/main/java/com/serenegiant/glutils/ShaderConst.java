package com.serenegiant.glutils;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2015 saki t_saki@serenegiant.com
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

/**
 * Created by saki on 16/08/26.
 * フラグメントシェーダーとかの文字列定数達を集める
 */
public class ShaderConst {
	public static final int GL_TEXTURE_EXTERNAL_OES	= 0x8D65;
	public static final int GL_TEXTURE_2D           = 0x0DE1;

	public static final String SHADER_VERSION = "#version 100\n";

	public static final String HEADER_2D = "";
	public static final String SAMPLER_2D = "sampler2D";

	public static final String HEADER_OES = "#extension GL_OES_EGL_image_external : require\n";
	public static final String SAMPLER_OES = "samplerExternalOES";

	public static final int KERNEL_SIZE3x3 = 9;
	public static final int KERNEL_SIZE5x５ = 25;

// 関数文字列定義
	/**
	 * RGBをHSVに変換
	 * {R[0.0-1.0], G[0.0-1.0], B([0.0-1.0]} => {H[0.0-1.0], S[0.0-1.0], V[0.0-1.0]}
	 */
	public static final String FUNC_RGB2HSV
		= "vec3 rgb2hsv(vec3 c) {\n" +
			"vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);\n" +
			"vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));\n" +
			"vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));\n" +
			"float d = q.x - min(q.w, q.y);\n" +
			"float e = 1.0e-10;\n" +
			"return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);\n" +
		"}\n";
	/**
	 * HSVをRGBに変換
	 * {H[0.0-1.0], S[0.0-1.0], V[0.0-1.0]} => {R[0.0-1.0], G[0.0-1.0], B([0.0-1.0]}
	 */
	public static final String FUNC_HSV2RGB
		= "vec3 hsv2rgb(vec3 c) {\n" +
			"vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);\n" +
			"vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);\n" +
			"return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);\n" +
		"}\n";

	/**
	 * RGBの輝度を取得
	 * 変換係数との内積を計算するだけ
	 * 係数は(0.2125, 0.7154, 0.0721)
	 */
	public static final String FUNC_GET_INTENSITY
		= "const highp vec3 luminanceWeighting = vec3(0.2125, 0.7154, 0.0721);\n" +
		"highp float getIntensity(vec3 c) {\n" +
			"return dot(c.rgb, luminanceWeighting);\n" +
		"}\n";

// 頂点シェーダー
	/**
	 * モデルビュー変換行列とテクスチャ変換行列適用するだけの頂点シェーダー
	 */
	public static final String VERTEX_SHADER = SHADER_VERSION +
		"uniform mat4 uMVPMatrix;\n" +				// モデルビュー変換行列
		"uniform mat4 uTexMatrix;\n" +				// テクスチャ変換行列
		"attribute highp vec4 aPosition;\n" +		// 頂点座標
		"attribute highp vec4 aTextureCoord;\n" +	// テクスチャ情報
		"varying highp vec2 vTextureCoord;\n" +		// フラグメントシェーダーへ引き渡すテクスチャ座標
		"void main() {\n" +
		"    gl_Position = uMVPMatrix * aPosition;\n" +
		"    vTextureCoord = (uTexMatrix * aTextureCoord).xy;\n" +
		"}\n";

// フラグメントシェーダー
	public static final String FRAGMENT_SHADER_SIMPLE_OES
		= SHADER_VERSION
		+ HEADER_OES
		+ "precision mediump float;\n"
		+ "uniform samplerExternalOES sTexture;\n"
		+ "varying highp vec2 vTextureCoord;\n"
		+ "void main() {\n"
		+ "  gl_FragColor = texture2D(sTexture, vTextureCoord);\n"
		+ "}";

	public static final String FRAGMENT_SHADER_SIMPLE
		= SHADER_VERSION
		+ HEADER_2D
		+ "precision mediump float;\n"
		+ "uniform sampler2D sTexture;\n"
		+ "varying highp vec2 vTextureCoord;\n"
		+ "void main() {\n"
		+ "  gl_FragColor = texture2D(sTexture, vTextureCoord);\n"
		+ "}";
}
