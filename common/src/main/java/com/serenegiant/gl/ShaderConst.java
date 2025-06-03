package com.serenegiant.gl;
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

import android.opengl.GLES20;

/**
 * Created by saki on 16/08/26.
 * フラグメントシェーダーとかの文字列定数達を集める
 */
public class ShaderConst implements GLConst {
	public static final String SHADER_VERSION_ES2 = "#version 100\n";
	public static final String SHADER_VERSION_ES3 = "#version 300 es\n";

	public static final String HEADER_2D = "";
	public static final String SAMPLER_2D = "sampler2D";

	public static final String HEADER_OES_ES2 = "#extension GL_OES_EGL_image_external : require\n";
	public static final String HEADER_OES_ES3 = "#extension GL_OES_EGL_image_external_essl3 : require\n";
	public static final String SAMPLER_OES = "samplerExternalOES";

	public static final int KERNEL_SIZE3x3_NUM = 9;
	public static final int KERNEL_SIZE5x5_NUM = 25;

	public static final String KERNEL_SIZE3x3 = "9";
	public static final String KERNEL_SIZE5x5 = "25";
//--------------------------------------------------------------------------------
	public static final int[] TEX_NUMBERS = {
		GLES20.GL_TEXTURE0, GLES20.GL_TEXTURE1,
		GLES20.GL_TEXTURE2, GLES20.GL_TEXTURE3,
		GLES20.GL_TEXTURE4, GLES20.GL_TEXTURE5,
		GLES20.GL_TEXTURE6, GLES20.GL_TEXTURE7,
		GLES20.GL_TEXTURE8, GLES20.GL_TEXTURE9,
		GLES20.GL_TEXTURE10, GLES20.GL_TEXTURE11,
		GLES20.GL_TEXTURE12, GLES20.GL_TEXTURE13,
		GLES20.GL_TEXTURE14, GLES20.GL_TEXTURE15,
		GLES20.GL_TEXTURE16, GLES20.GL_TEXTURE17,
		GLES20.GL_TEXTURE18, GLES20.GL_TEXTURE19,
		GLES20.GL_TEXTURE20, GLES20.GL_TEXTURE21,
		GLES20.GL_TEXTURE22, GLES20.GL_TEXTURE23,
		GLES20.GL_TEXTURE24, GLES20.GL_TEXTURE25,
		GLES20.GL_TEXTURE26, GLES20.GL_TEXTURE27,
		GLES20.GL_TEXTURE28, GLES20.GL_TEXTURE29,
		GLES20.GL_TEXTURE30, GLES20.GL_TEXTURE31,
	};

//--------------------------------------------------------------------------------
	/**
	 * デフォルトの2D頂点座標配列
	 */
	public static final float[] DEFAULT_VERTICES_2D = {
		1.0f, 1.0f,		// 右上
		-1.0f, 1.0f,	// 左上
		1.0f, -1.0f,	// 右下
		-1.0f, -1.0f,	// 左下
	};
	/**
	 * デフォルトの2Dテクスチャ座標配列
	 */
	public static final float[] DEFAULT_TEXCOORD_2D = {
		1.0f, 0.0f,		// 右上
		0.0f, 0.0f,		// 左上
		1.0f, 1.0f,		// 右下
		0.0f, 1.0f,		// 左下
	};
	/**
	 * XXX 元々のDEFAULT_TEXCOORDはテクスチャ座標を上下反転させたこっちだった
	 *     これだと頂点座標はそのままでテクスチャだけ反転してしまうので
	 *     1パス処理する毎に映像が意図せず上下反転してしまう。
	 *     頂点座標とテクスチャ座標のx/y軸を合わせてどちらも変更しないとだめ。
	 */
	public static final float[] DEFAULT_TEXCOORD_FLIP_VERTICAL_2D = {
		1.0f, 1.0f,		// 右上
		0.0f, 1.0f,		// 左上
		1.0f, 0.0f,		// 右下
		0.0f, 0.0f,		// 左下
	};

//--------------------------------------------------------------------------------
// 関数文字列定義
	/**
	 * RGBをHSVに変換
	 * {R[0.0-1.0], G[0.0-1.0], B([0.0-1.0]} => {H[0.0-1.0], S[0.0-1.0], V[0.0-1.0]}
	 */
	public static final String FUNC_RGB2HSV
		= """
		vec3 rgb2hsv(vec3 c) {
		vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
		vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));
		vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));
		float d = q.x - min(q.w, q.y);
		float e = 1.0e-10;
		return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);
		}
		""";
	/**
	 * HSVをRGBに変換
	 * {H[0.0-1.0], S[0.0-1.0], V[0.0-1.0]} => {R[0.0-1.0], G[0.0-1.0], B([0.0-1.0]}
	 */
	public static final String FUNC_HSV2RGB
		= """
		vec3 hsv2rgb(vec3 c) {
		vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
		vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
		return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
		}
		""";

	public static final String HSV_FUNCTIONS = FUNC_RGB2HSV + FUNC_HSV2RGB;
	/**
	 * RGBの輝度を取得
	 * 変換係数との内積を計算するだけ
	 * 係数は(0.2125, 0.7154, 0.0721)
	 */
	public static final String FUNC_GET_INTENSITY
		= """
		const highp vec3 luminanceWeighting = vec3(0.2125, 0.7154, 0.0721);
		highp float getIntensity(vec3 c) {
		return dot(c.rgb, luminanceWeighting);
		}
		""";

//--------------------------------------------------------------------------------
// 頂点シェーダー
	/**
	 * モデルビュー変換行列とテクスチャ変換行列適用するだけの頂点シェーダー
	 * for ES2
	 */
	public static final String VERTEX_SHADER_ES2 =
		"""
		#version 100
		uniform mat4 uMVPMatrix;
		uniform mat4 uTexMatrix;
		attribute highp vec4 aPosition;
		attribute highp vec4 aTextureCoord;
		varying highp vec2 vTextureCoord;
		void main() {
		    gl_Position = uMVPMatrix * aPosition;
		    vTextureCoord = (uTexMatrix * aTextureCoord).xy;
		}
		""";

	/**
	 * モデルビュー変換行列とテクスチャ変換行列適用するだけの頂点シェーダー
	 * for ES3
	 */
	public static final String VERTEX_SHADER_ES3 =
		"""
		#version 300 es
		uniform mat4 uMVPMatrix;
		uniform mat4 uTexMatrix;
		in highp vec4 aPosition;
		in highp vec4 aTextureCoord;
		out highp vec2 vTextureCoord;
		void main() {
		    gl_Position = uMVPMatrix * aPosition;
		    vTextureCoord = (uTexMatrix * aTextureCoord).xy;
		}
		""";

//--------------------------------------------------------------------------------
// フラグメントシェーダー
	/**
	 * テクスチャを単純コピーするだけのフラグメントシェーダ
	 * for ES2
	 */
	private static final String FRAGMENT_SHADER_BASE_ES2
		= """
		%s
		%s
		precision mediump float;
		varying vec2 vTextureCoord;
		uniform %s sTexture;
		void main() {
			gl_FragColor = texture2D(sTexture, vTextureCoord);
		}
		""";

	public static final String FRAGMENT_SHADER_ES2
		= String.format(FRAGMENT_SHADER_BASE_ES2, SHADER_VERSION_ES2, HEADER_2D, SAMPLER_2D);
	public static final String FRAGMENT_SHADER_EXT_ES2
		= String.format(FRAGMENT_SHADER_BASE_ES2, SHADER_VERSION_ES2, HEADER_OES_ES2, SAMPLER_OES);

	/**
	 * テクスチャを単純コピーするだけのフラグメントシェーダ
	 * for ES3
	 */
	private static final String FRAGMENT_SHADER_BASE_ES3
		= """
		%s
		%s
		precision mediump float;
		in vec2 vTextureCoord;
		uniform %s sTexture;
		layout(location = 0) out vec4 o_FragColor;
		void main() {
		    o_FragColor = texture(sTexture, vTextureCoord);
		}
		""";

	public static final String FRAGMENT_SHADER_ES3
		= String.format(FRAGMENT_SHADER_BASE_ES3, SHADER_VERSION_ES3, HEADER_2D, SAMPLER_2D);
	public static final String FRAGMENT_SHADER_EXT_ES3
		= String.format(FRAGMENT_SHADER_BASE_ES3, SHADER_VERSION_ES3, HEADER_OES_ES3, SAMPLER_OES);


//--------------------------------------------------------------------------------
	/**
	 * 色変換行列を適用するフラグメントシェーダ
	 * for ES2
	 */
	private static final String FRAGMENT_SHADER_COLOR_MATRIX_BASE_ES2
		= """
		%s
		%s
		precision mediump float;
		varying vec2 vTextureCoord;
		uniform %s sTexture;
		uniform mat4 uColorMatrixLoc;
		void main() {
			vec4 cl = texture2D(sTexture, vTextureCoord);
			float a = cl.a;
			cl = vec4(cl.rgb, 1.0);
			gl_FragColor = vec4((uColorMatrixLoc * cl).rgb, a);
		}
		""";

	public static final String FRAGMENT_SHADER_COLOR_MATRIX_ES2
		= String.format(FRAGMENT_SHADER_COLOR_MATRIX_BASE_ES2,
			SHADER_VERSION_ES2, HEADER_2D, SAMPLER_2D);
	public static final String FRAGMENT_SHADER_COLOR_MATRIX_EXT_ES2
		= String.format(FRAGMENT_SHADER_BASE_ES2, SHADER_VERSION_ES2,
			HEADER_OES_ES2, SAMPLER_OES);

	/**
	 * 色変換行列を適用するフラグメントシェーダ
	 * for ES3
	 */
	private static final String FRAGMENT_SHADER_COLOR_MATRIX_BASE_ES3
		= """
		%s
		%s
		precision mediump float;
		in vec2 vTextureCoord;
		uniform %s sTexture;
		uniform mat4 uColorMatrixLoc;
		layout(location = 0) out vec4 o_FragColor;
		void main() {
			vec4 cl = texture(sTexture, vTextureCoord);
			float a = cl.a;
			cl = vec4(cl.rgb, 1.0);
		    o_FragColor = vec4((uColorMatrixLoc * cl).rgb, a);
		}
		""";

	public static final String FRAGMENT_SHADER_COLOR_MATRIX_ES3
		= String.format(FRAGMENT_SHADER_COLOR_MATRIX_BASE_ES3,
			SHADER_VERSION_ES3, HEADER_2D, SAMPLER_2D);
	public static final String FRAGMENT_SHADER_COLOR_MATRIX_EXT_ES3
		= String.format(FRAGMENT_SHADER_COLOR_MATRIX_BASE_ES3,
			SHADER_VERSION_ES3, HEADER_OES_ES3, SAMPLER_OES);

//--------------------------------------------------------------------------------
	/**
	 * 白黒二値変換するフラグメントシェーダ
	 * for ES2
	 */
	private static final String FRAGMENT_SHADER_BW_BASE_ES2
		= """
		%s
		%s
		precision mediump float;
		varying vec2 vTextureCoord;
		uniform %s sTexture;
		void main() {
			vec4 tc = texture2D(sTexture, vTextureCoord);
			float color = tc.r * 0.3 + tc.g * 0.59 + tc.b * 0.11;
			gl_FragColor = vec4(color, color, color, 1.0);
		}
		""";

	public static final String FRAGMENT_SHADER_BW_ES2
		= String.format(FRAGMENT_SHADER_BW_BASE_ES2, SHADER_VERSION_ES2, HEADER_2D, SAMPLER_2D);
	public static final String FRAGMENT_SHADER_EXT_BW_ES2
		= String.format(FRAGMENT_SHADER_BW_BASE_ES2, SHADER_VERSION_ES2, HEADER_OES_ES2, SAMPLER_OES);

	/**
	 * 白黒二値変換するフラグメントシェーダ
	 * for ES3
	 */
	private static final String FRAGMENT_SHADER_BW_BASE_ES3
		= """
		%s
		%s
		precision mediump float;
		in vec2 vTextureCoord;
		uniform %s sTexture;
		layout(location = 0) out vec4 o_FragColor;
		void main() {
		    vec4 tc = texture(sTexture, vTextureCoord);
		    float color = tc.r * 0.3 + tc.g * 0.59 + tc.b * 0.11;
		    o_FragColor = vec4(color, color, color, 1.0);
		}
		""";

	public static final String FRAGMENT_SHADER_BW_ES3
		= String.format(FRAGMENT_SHADER_BW_BASE_ES3, SHADER_VERSION_ES3, HEADER_2D, SAMPLER_2D);
	public static final String FRAGMENT_SHADER_EXT_BW_ES3
		= String.format(FRAGMENT_SHADER_BW_BASE_ES3, SHADER_VERSION_ES3, HEADER_OES_ES3, SAMPLER_OES);

//--------------------------------------------------------------------------------
	/**
	 * ナイトビジョン風に強調表示するフラグメントシェーダ
	 * for ES2
	 */
	private static final String FRAGMENT_SHADER_NIGHT_BASE_ES2
		= """
		%s
		%s
		precision mediump float;
		varying vec2 vTextureCoord;
		uniform %s sTexture;
		void main() {
			vec4 tc = texture2D(sTexture, vTextureCoord);
			float color = ((tc.r * 0.3 + tc.g * 0.59 + tc.b * 0.11) - 0.5 * 1.5) + 0.8;
			gl_FragColor = vec4(color, color + 0.15, color, 1.0);
		}
		""";

	public static final String FRAGMENT_SHADER_NIGHT_ES2
		= String.format(FRAGMENT_SHADER_NIGHT_BASE_ES2, SHADER_VERSION_ES2, HEADER_2D, SAMPLER_2D);
	public static final String FRAGMENT_SHADER_EXT_NIGHT_ES2
		= String.format(FRAGMENT_SHADER_NIGHT_BASE_ES2, SHADER_VERSION_ES2, HEADER_OES_ES2, SAMPLER_OES);

	/**
	 * ナイトビジョン風に強調表示するフラグメントシェーダ
	 * for ES3
	 */
	private static final String FRAGMENT_SHADER_NIGHT_BASE_ES3
		= """
		%s
		%s
		precision mediump float;
		in vec2 vTextureCoord;
		uniform %s sTexture;
		layout(location = 0) out vec4 o_FragColor;
		void main() {
		    vec4 tc = texture(sTexture, vTextureCoord);
		    float color = ((tc.r * 0.3 + tc.g * 0.59 + tc.b * 0.11) - 0.5 * 1.5) + 0.8;
		    o_FragColor = vec4(color, color + 0.15, color, 1.0);
		}
		""";

	public static final String FRAGMENT_SHADER_NIGHT_ES3
		= String.format(FRAGMENT_SHADER_NIGHT_BASE_ES3, SHADER_VERSION_ES3, HEADER_2D, SAMPLER_2D);
	public static final String FRAGMENT_SHADER_EXT_NIGHT_ES3
		= String.format(FRAGMENT_SHADER_NIGHT_BASE_ES3, SHADER_VERSION_ES3, HEADER_OES_ES3, SAMPLER_OES);

//--------------------------------------------------------------------------------
	/**
	 * クロマキー合成用に緑を透過させるフラグメントシェーダ
	 * for ES2
	 */
	private static final String FRAGMENT_SHADER_CHROMA_KEY_BASE_ES2
		= """
		%s
		%s
		precision mediump float;
		varying vec2 vTextureCoord;
		uniform %s sTexture;
		void main() {
			vec4 tc = texture2D(sTexture, vTextureCoord);
			float color = ((tc.r * 0.3 + tc.g * 0.59 + tc.b * 0.11) - 0.5 * 1.5) + 0.8;
			if(tc.g > 0.6 && tc.b < 0.6 && tc.r < 0.6){
				gl_FragColor = vec4(0, 0, 0, 0.0);
			}else{
				gl_FragColor = texture2D(sTexture, vTextureCoord);
			}
		}
		""";

	public static final String FRAGMENT_SHADER_CHROMA_KEY_ES2
		= String.format(FRAGMENT_SHADER_CHROMA_KEY_BASE_ES2, SHADER_VERSION_ES2, HEADER_2D, SAMPLER_2D);
	public static final String FRAGMENT_SHADER_EXT_CHROMA_KEY_ES2
		= String.format(FRAGMENT_SHADER_CHROMA_KEY_BASE_ES2, SHADER_VERSION_ES2, HEADER_OES_ES2, SAMPLER_OES);

	/**
	 * クロマキー合成用に緑を透過させるフラグメントシェーダ
	 * for ES3
	 */
	private static final String FRAGMENT_SHADER_CHROMA_KEY_BASE_ES3
		= """
		%s
		%s
		precision mediump float;
		in vec2 vTextureCoord;
		uniform %s sTexture;
		layout(location = 0) out vec4 o_FragColor;
		void main() {
		    vec4 tc = texture(sTexture, vTextureCoord);
		    float color = ((tc.r * 0.3 + tc.g * 0.59 + tc.b * 0.11) - 0.5 * 1.5) + 0.8;
		    if(tc.g > 0.6 && tc.b < 0.6 && tc.r < 0.6){
		        o_FragColor = vec4(0, 0, 0, 0.0);
		    }else{
		        o_FragColor = texture(sTexture, vTextureCoord);
		    }
		}
		""";

	public static final String FRAGMENT_SHADER_CHROMA_KEY_ES3
		= String.format(FRAGMENT_SHADER_CHROMA_KEY_BASE_ES3, SHADER_VERSION_ES3, HEADER_2D, SAMPLER_2D);
	public static final String FRAGMENT_SHADER_EXT_CHROMA_KEY_ES3
		= String.format(FRAGMENT_SHADER_CHROMA_KEY_BASE_ES3, SHADER_VERSION_ES3, HEADER_OES_ES3, SAMPLER_OES);

//--------------------------------------------------------------------------------
	/**
	 * SQUEEZE効果付与のフラグメントシェーダ
	 * for ES2
	 */
	private static final String FRAGMENT_SHADER_SQUEEZE_BASE_ES2
		= """
		%s
		%s
		precision mediump float;
		varying vec2 vTextureCoord;
		uniform %s sTexture;
		uniform vec2 uPosition;
		void main() {
		    vec2 texCoord = vTextureCoord.xy;
		    vec2 normCoord = 2.0 * texCoord - 1.0;
		    float r = length(normCoord);
		    float phi = atan(normCoord.y + uPosition.y, normCoord.x + uPosition.x);
		    r = pow(r, 1.0/1.8) * 0.8;
		    normCoord.x = r * cos(phi);
		    normCoord.y = r * sin(phi);
		    texCoord = normCoord / 2.0 + 0.5;
		    gl_FragColor = texture2D(sTexture, texCoord);
		}
		""";

	public static final String FRAGMENT_SHADER_SQUEEZE_ES2
		= String.format(FRAGMENT_SHADER_SQUEEZE_BASE_ES2, SHADER_VERSION_ES2, HEADER_2D, SAMPLER_2D);
	public static final String FRAGMENT_SHADER_EXT_SQUEEZE_ES2
		= String.format(FRAGMENT_SHADER_SQUEEZE_BASE_ES2, SHADER_VERSION_ES2, HEADER_OES_ES2, SAMPLER_OES);

	/**
	 * SQUEEZE効果付与のフラグメントシェーダ
	 * for ES3
	 */
	private static final String FRAGMENT_SHADER_SQUEEZE_BASE_ES3
		= """
		%s
		%s
		precision mediump float;
		in vec2 vTextureCoord;
		uniform %s sTexture;
		uniform vec2 uPosition;
		layout(location = 0) out vec4 o_FragColor;
		void main() {
		    vec2 texCoord = vTextureCoord.xy;
		    vec2 normCoord = 2.0 * texCoord - 1.0;
		    float r = length(normCoord);
		    float phi = atan(normCoord.y + uPosition.y, normCoord.x + uPosition.x);
		    r = pow(r, 1.0/1.8) * 0.8;
		    normCoord.x = r * cos(phi);
		    normCoord.y = r * sin(phi);
		    texCoord = normCoord / 2.0 + 0.5;
		    o_FragColor = texture(sTexture, texCoord);
		}
		""";

	public static final String FRAGMENT_SHADER_SQUEEZE_ES3
		= String.format(FRAGMENT_SHADER_SQUEEZE_BASE_ES3, SHADER_VERSION_ES3, HEADER_2D, SAMPLER_2D);
	public static final String FRAGMENT_SHADER_EXT_SQUEEZE_ES3
		= String.format(FRAGMENT_SHADER_SQUEEZE_BASE_ES3, SHADER_VERSION_ES3, HEADER_OES_ES3, SAMPLER_OES);

//--------------------------------------------------------------------------------
	/**
	 * TWIRL効果付与のフラグメントシェーダ
	 * for ES2
	 */
	public static final String FRAGMENT_SHADER_TWIRL_BASE_ES2
		= """
		%s
		%s
		precision mediump float;
		varying vec2 vTextureCoord;
		uniform %s sTexture;
		uniform vec2 uPosition;
		void main() {
			vec2 texCoord = vTextureCoord.xy;
			vec2 normCoord = 2.0 * texCoord - 1.0;
			// to polar coords
			float r = length(normCoord);
			float phi = atan(normCoord.y + uPosition.y, normCoord.x + uPosition.x);
			phi = phi + (1.0 - smoothstep(-0.5, 0.5, r)) * 4.0;
			normCoord.x = r * cos(phi);
			normCoord.y = r * sin(phi);
			texCoord = normCoord / 2.0 + 0.5;
			gl_FragColor = texture2D(sTexture, texCoord);
		}
		""";
	public static final String FRAGMENT_SHADER_TWIRL_ES2
		= String.format(FRAGMENT_SHADER_TWIRL_BASE_ES2, SHADER_VERSION_ES2, HEADER_2D, SAMPLER_2D);
	public static final String FRAGMENT_SHADER_EXT_TWIRL_ES2
		= String.format(FRAGMENT_SHADER_TWIRL_BASE_ES2, SHADER_VERSION_ES2, HEADER_OES_ES2, SAMPLER_OES);

	/**
	 * TWIRL効果付与のフラグメントシェーダ
	 * for ES3
	 */
	public static final String FRAGMENT_SHADER_TWIRL_BASE_ES3
		= """
		%s
		%s
		precision mediump float;
		in vec2 vTextureCoord;
		uniform %s sTexture;
		uniform vec2 uPosition;
		layout(location = 0) out vec4 o_FragColor;
		void main() {
		    vec2 texCoord = vTextureCoord.xy;
		    vec2 normCoord = 2.0 * texCoord - 1.0;
			// to polar coords
		    float r = length(normCoord);
		    float phi = atan(normCoord.y + uPosition.y, normCoord.x + uPosition.x);
		    phi = phi + (1.0 - smoothstep(-0.5, 0.5, r)) * 4.0;
		    normCoord.x = r * cos(phi);
		    normCoord.y = r * sin(phi);
		    texCoord = normCoord / 2.0 + 0.5;
		    o_FragColor = texture(sTexture, texCoord);
		}
		""";

	public static final String FRAGMENT_SHADER_TWIRL_ES3
		= String.format(FRAGMENT_SHADER_TWIRL_BASE_ES3, SHADER_VERSION_ES3, HEADER_2D, SAMPLER_2D);
	public static final String FRAGMENT_SHADER_EXT_TWIRL_ES3
		= String.format(FRAGMENT_SHADER_TWIRL_BASE_ES3, SHADER_VERSION_ES3, HEADER_OES_ES3, SAMPLER_OES);

//--------------------------------------------------------------------------------
	/**
	 * TUNNEL Effect付与のフラグメントシェーダ
	 * for ES2
	 */
	public static final String FRAGMENT_SHADER_TUNNEL_BASE_ES2
		= """
		%s
		%s
		precision mediump float;
		varying vec2 vTextureCoord;
		uniform %s sTexture;
		uniform vec2 uPosition;
		void main() {
		    vec2 texCoord = vTextureCoord.xy;
		    vec2 normCoord = 2.0 * texCoord - 1.0;
		    float r = length(normCoord);
		    float phi = atan(normCoord.y + uPosition.y, normCoord.x + uPosition.x);
		    if (r > 0.5) r = 0.5;
		    normCoord.x = r * cos(phi);
		    normCoord.y = r * sin(phi);
		    texCoord = normCoord / 2.0 + 0.5;
		    gl_FragColor = texture2D(sTexture, texCoord);
		}
		""";

	public static final String FRAGMENT_SHADER_TUNNEL_ES2
		= String.format(FRAGMENT_SHADER_TUNNEL_BASE_ES2, SHADER_VERSION_ES2, HEADER_2D, SAMPLER_2D);
	public static final String FRAGMENT_SHADER_EXT_TUNNEL_ES2
		= String.format(FRAGMENT_SHADER_TUNNEL_BASE_ES2, SHADER_VERSION_ES2, HEADER_OES_ES2, SAMPLER_OES);

	/**
	 * TUNNEL Effect付与のフラグメントシェーダ
	 * for ES3
	 */
	public static final String FRAGMENT_SHADER_TUNNEL_BASE_ES3
		= """
		%s
		%s
		precision mediump float;
		in vec2 vTextureCoord;
		uniform %s sTexture;
		uniform vec2 uPosition;
		layout(location = 0) out vec4 o_FragColor;
		void main() {
		    vec2 texCoord = vTextureCoord.xy;
		    vec2 normCoord = 2.0 * texCoord - 1.0;
		    float r = length(normCoord);
		    float phi = atan(normCoord.y + uPosition.y, normCoord.x + uPosition.x);
		    if (r > 0.5) r = 0.5;
		    normCoord.x = r * cos(phi);
		    normCoord.y = r * sin(phi);
		    texCoord = normCoord / 2.0 + 0.5;
		    o_FragColor = texture(sTexture, texCoord);
		}
		""";

	public static final String FRAGMENT_SHADER_TUNNEL_ES3
		= String.format(FRAGMENT_SHADER_TUNNEL_BASE_ES3, SHADER_VERSION_ES3, HEADER_2D, SAMPLER_2D);
	public static final String FRAGMENT_SHADER_EXT_TUNNEL_ES3
		= String.format(FRAGMENT_SHADER_TUNNEL_BASE_ES3, SHADER_VERSION_ES3, HEADER_OES_ES3, SAMPLER_OES);

//--------------------------------------------------------------------------------
	/**
	 * Bulge Effect付与のフラグメントシェーダ
	 * for ES2
	 */
	public static final String FRAGMENT_SHADER_BULGE_BASE_ES2
		= """
		%s
		%s
		precision mediump float;
		varying vec2 vTextureCoord;
		uniform %s sTexture;
		uniform vec2 uPosition;
		void main() {
		    vec2 texCoord = vTextureCoord.xy;
		    vec2 normCoord = 2.0 * texCoord - 1.0;
		    float r = length(normCoord);
		    float phi = atan(normCoord.y + uPosition.y, normCoord.x + uPosition.x);
		    r = r * smoothstep(-0.1, 0.5, r);
		    normCoord.x = r * cos(phi);
		    normCoord.y = r * sin(phi);
		    texCoord = normCoord / 2.0 + 0.5;
		    gl_FragColor = texture2D(sTexture, texCoord);
		}
		""";

	public static final String FRAGMENT_SHADER_BULGE_ES2
		= String.format(FRAGMENT_SHADER_BULGE_BASE_ES2, SHADER_VERSION_ES2, HEADER_2D, SAMPLER_2D);
	public static final String FRAGMENT_SHADER_EXT_BULGE_ES2
		= String.format(FRAGMENT_SHADER_BULGE_BASE_ES2, SHADER_VERSION_ES2, HEADER_OES_ES2, SAMPLER_OES);

	/**
	 * Bulge Effect付与のフラグメントシェーダ
	 * for ES3
	 */
	public static final String FRAGMENT_SHADER_BULGE_BASE_ES3
		= """
		%s
		%s
		precision mediump float;
		in vec2 vTextureCoord;
		uniform %s sTexture;
		uniform vec2 uPosition;
		layout(location = 0) out vec4 o_FragColor;
		void main() {
		    vec2 texCoord = vTextureCoord.xy;
		    vec2 normCoord = 2.0 * texCoord - 1.0;
		    float r = length(normCoord);
		    float phi = atan(normCoord.y + uPosition.y, normCoord.x + uPosition.x);
		    r = r * smoothstep(-0.1, 0.5, r);
		    normCoord.x = r * cos(phi);
		    normCoord.y = r * sin(phi);
		    texCoord = normCoord / 2.0 + 0.5;
		    o_FragColor = texture(sTexture, texCoord);
		}
		""";

	public static final String FRAGMENT_SHADER_BULGE_ES3
		= String.format(FRAGMENT_SHADER_BULGE_BASE_ES3, SHADER_VERSION_ES3, HEADER_2D, SAMPLER_2D);
	public static final String FRAGMENT_SHADER_EXT_BULGE_ES3
		= String.format(FRAGMENT_SHADER_BULGE_BASE_ES3, SHADER_VERSION_ES3, HEADER_OES_ES3, SAMPLER_OES);

//--------------------------------------------------------------------------------
	/**
	 * Dent Effect付与のフラグメントシェーダ
	 * for ES2
	 */
	public static final String FRAGMENT_SHADER_DENT_BASE_ES2
		= """
		%s
		%s
		precision mediump float;
		varying vec2 vTextureCoord;
		uniform %s sTexture;
		uniform vec2 uPosition;
		void main() {
		    vec2 texCoord = vTextureCoord.xy;
		    vec2 normCoord = 2.0 * texCoord - 1.0;
		    float r = length(normCoord);
		    float phi = atan(normCoord.y + uPosition.y, normCoord.x + uPosition.x);
		    r = 2.0 * r - r * smoothstep(0.0, 0.7, r);
		    normCoord.x = r * cos(phi);
		    normCoord.y = r * sin(phi);
		    texCoord = normCoord / 2.0 + 0.5;
		    gl_FragColor = texture2D(sTexture, texCoord);
		}
		""";

	public static final String FRAGMENT_SHADER_DENT_ES2
		= String.format(FRAGMENT_SHADER_DENT_BASE_ES2, SHADER_VERSION_ES2, HEADER_2D, SAMPLER_2D);
	public static final String FRAGMENT_SHADER_EXT_DENT_ES2
		= String.format(FRAGMENT_SHADER_DENT_BASE_ES2, SHADER_VERSION_ES2, HEADER_OES_ES2, SAMPLER_OES);

	/**
	 * Dent Effect付与のフラグメントシェーダ
	 * for ES3
	 */
	public static final String FRAGMENT_SHADER_DENT_BASE_ES3
		= """
		%s
		%s
		precision mediump float;
		in vec2 vTextureCoord;
		uniform %s sTexture;
		uniform vec2 uPosition;
		layout(location = 0) out vec4 o_FragColor;
		void main() {
		    vec2 texCoord = vTextureCoord.xy;
		    vec2 normCoord = 2.0 * texCoord - 1.0;
		    float r = length(normCoord);
		    float phi = atan(normCoord.y + uPosition.y, normCoord.x + uPosition.x);
		    r = 2.0 * r - r * smoothstep(0.0, 0.7, r);
		    normCoord.x = r * cos(phi);
		    normCoord.y = r * sin(phi);
		    texCoord = normCoord / 2.0 + 0.5;
		    o_FragColor = texture(sTexture, texCoord);
		}
		""";

	public static final String FRAGMENT_SHADER_DENT_ES3
		= String.format(FRAGMENT_SHADER_DENT_BASE_ES3, SHADER_VERSION_ES3, HEADER_2D, SAMPLER_2D);
	public static final String FRAGMENT_SHADER_EXT_DENT_ES3
		= String.format(FRAGMENT_SHADER_DENT_BASE_ES3, SHADER_VERSION_ES3, HEADER_OES_ES3, SAMPLER_OES);

//--------------------------------------------------------------------------------
	/**
	 * Fisheye Effect付与のフラグメントシェーダ
	 * for ES2
	 */
	public static final String FRAGMENT_SHADER_FISHEYE_BASE_ES2
		= """
		%s
		%s
		precision mediump float;
		varying vec2 vTextureCoord;
		uniform %s sTexture;
		uniform vec2 uPosition;
		void main() {
		    vec2 texCoord = vTextureCoord.xy;
		    vec2 normCoord = 2.0 * texCoord - 1.0;
		    float r = length(normCoord);
		    float phi = atan(normCoord.y + uPosition.y, normCoord.x + uPosition.x);
		    r = r * r / sqrt(2.0);
		    normCoord.x = r * cos(phi);
		    normCoord.y = r * sin(phi);
		    texCoord = normCoord / 2.0 + 0.5;
		    gl_FragColor = texture2D(sTexture, texCoord);
		}
		""";

	public static final String FRAGMENT_SHADER_FISHEYE_ES2
		= String.format(FRAGMENT_SHADER_FISHEYE_BASE_ES2, SHADER_VERSION_ES2, HEADER_2D, SAMPLER_2D);
	public static final String FRAGMENT_SHADER_EXT_FISHEYE_ES2
		= String.format(FRAGMENT_SHADER_FISHEYE_BASE_ES2, SHADER_VERSION_ES2, HEADER_OES_ES2, SAMPLER_OES);

	/**
	 * Fisheye Effect付与のフラグメントシェーダ
	 * for ES3
	 */
	public static final String FRAGMENT_SHADER_FISHEYE_BASE_ES3
		= """
		%s
		%s
		precision mediump float;
		in vec2 vTextureCoord;
		uniform %s sTexture;
		uniform vec2 uPosition;
		layout(location = 0) out vec4 o_FragColor;
		void main() {
		    vec2 texCoord = vTextureCoord.xy;
		    vec2 normCoord = 2.0 * texCoord - 1.0;
		    float r = length(normCoord);
		    float phi = atan(normCoord.y + uPosition.y, normCoord.x + uPosition.x);
		    r = r * r / sqrt(2.0);
		    normCoord.x = r * cos(phi);
		    normCoord.y = r * sin(phi);
		    texCoord = normCoord / 2.0 + 0.5;
		    o_FragColor = texture(sTexture, texCoord);
		}
		""";

	public static final String FRAGMENT_SHADER_FISHEYE_ES3
		= String.format(FRAGMENT_SHADER_FISHEYE_BASE_ES3, SHADER_VERSION_ES3, HEADER_2D, SAMPLER_2D);
	public static final String FRAGMENT_SHADER_EXT_FISHEYE_ES3
		= String.format(FRAGMENT_SHADER_FISHEYE_BASE_ES3, SHADER_VERSION_ES3, HEADER_OES_ES3, SAMPLER_OES);

//--------------------------------------------------------------------------------
	/**
	 * Stretch Effect付与のフラグメントシェーダ
	 * for ES2
	 */
	public static final String FRAGMENT_SHADER_STRETCH_BASE_ES2
		= """
		%s
		%s
		precision mediump float;
		varying vec2 vTextureCoord;
		uniform %s sTexture;
		uniform vec2 uPosition;
		void main() {
		    vec2 texCoord = vTextureCoord.xy;
		    vec2 normCoord = 2.0 * texCoord - 1.0;
		    vec2 s = sign(normCoord + uPosition);
		    normCoord = abs(normCoord);
		    normCoord = 0.5 * normCoord + 0.5 * smoothstep(0.25, 0.5, normCoord) * normCoord;
		    normCoord = s * normCoord;
		    texCoord = normCoord / 2.0 + 0.5;
		    gl_FragColor = texture2D(sTexture, texCoord);
		}
		""";

	public static final String FRAGMENT_SHADER_STRETCH_ES2
		= String.format(FRAGMENT_SHADER_STRETCH_BASE_ES2, SHADER_VERSION_ES2, HEADER_2D, SAMPLER_2D);
	public static final String FRAGMENT_SHADER_EXT_STRETCH_ES2
		= String.format(FRAGMENT_SHADER_STRETCH_BASE_ES2, SHADER_VERSION_ES2, HEADER_OES_ES2, SAMPLER_OES);

	/**
	 * Stretch Effect付与のフラグメントシェーダ
	 * for ES3
	 */
	public static final String FRAGMENT_SHADER_STRETCH_BASE_ES3
		= """
		%s
		%s
		precision mediump float;
		in vec2 vTextureCoord;
		uniform %s sTexture;
		uniform vec2 uPosition;
		layout(location = 0) out vec4 o_FragColor;
		void main() {
		    vec2 texCoord = vTextureCoord.xy;
		    vec2 normCoord = 2.0 * texCoord - 1.0;
		    vec2 s = sign(normCoord + uPosition);
		    normCoord = abs(normCoord);
		    normCoord = 0.5 * normCoord + 0.5 * smoothstep(0.25, 0.5, normCoord) * normCoord;
		    normCoord = s * normCoord;
		    texCoord = normCoord / 2.0 + 0.5;
		    o_FragColor = texture(sTexture, texCoord);
		}
		""";

	public static final String FRAGMENT_SHADER_STRETCH_ES3
		= String.format(FRAGMENT_SHADER_STRETCH_BASE_ES3, SHADER_VERSION_ES3, HEADER_2D, SAMPLER_2D);
	public static final String FRAGMENT_SHADER_EXT_STRETCH_ES3
		= String.format(FRAGMENT_SHADER_STRETCH_BASE_ES3, SHADER_VERSION_ES3, HEADER_OES_ES3, SAMPLER_OES);

//--------------------------------------------------------------------------------
	/**
	 * Mirror Effect付与のフラグメントシェーダ
	 * for ES2
	 */
	public static final String FRAGMENT_SHADER_MIRROR_BASE_ES2
		= """
		%s
		%s
		precision mediump float;
		varying vec2 vTextureCoord;
		uniform %s sTexture;
		uniform vec2 uPosition;
		void main() {
		    vec2 texCoord = vTextureCoord.xy;
		    vec2 normCoord = 2.0 * texCoord - 1.0;
		    normCoord.x = normCoord.x * sign(normCoord.x + uPosition.x);
		    texCoord = normCoord / 2.0 + 0.5;
		    gl_FragColor = texture2D(sTexture, texCoord);
		}
		""";

	public static final String FRAGMENT_SHADER_MIRROR_ES2
		= String.format(FRAGMENT_SHADER_MIRROR_BASE_ES2, SHADER_VERSION_ES2, HEADER_2D, SAMPLER_2D);
	public static final String FRAGMENT_SHADER_EXT_MIRROR_ES2
		= String.format(FRAGMENT_SHADER_MIRROR_BASE_ES2, SHADER_VERSION_ES2, HEADER_OES_ES2, SAMPLER_OES);

	/**
	 * Mirror Effect付与のフラグメントシェーダ
	 * for ES3
	 */
	public static final String FRAGMENT_SHADER_MIRROR_BASE_ES3
		= """
		%s
		%s
		precision mediump float;
		in vec2 vTextureCoord;
		uniform %s sTexture;
		uniform vec2 uPosition;
		layout(location = 0) out vec4 o_FragColor;
		void main() {
		    vec2 texCoord = vTextureCoord.xy;
		    vec2 normCoord = 2.0 * texCoord - 1.0;
		    normCoord.x = normCoord.x * sign(normCoord.x + uPosition.x);
		    texCoord = normCoord / 2.0 + 0.5;
		    o_FragColor = texture(sTexture, texCoord);
		}
		""";

	public static final String FRAGMENT_SHADER_MIRROR_ES3
		= String.format(FRAGMENT_SHADER_MIRROR_BASE_ES3, SHADER_VERSION_ES3, HEADER_2D, SAMPLER_2D);
	public static final String FRAGMENT_SHADER_EXT_MIRROR_ES3
		= String.format(FRAGMENT_SHADER_MIRROR_BASE_ES3, SHADER_VERSION_ES3, HEADER_OES_ES3, SAMPLER_OES);

//--------------------------------------------------------------------------------
	/**
	 * Sobel Effect付与のフラグメントシェーダ
	 * for ES2
	 */
	public static final String FRAGMENT_SHADER_SOBEL_BASE_ES2
		= """
		%s
		%s
		#define KERNEL_SIZE3x3 9
		precision highp float;
		varying       vec2 vTextureCoord;
		uniform %s    sTexture;
		uniform float uKernel[18];
		uniform vec2  uTexOffset[KERNEL_SIZE3x3];
		uniform float uColorAdjust;
		void main() {
		    vec3 t0 = texture2D(sTexture, vTextureCoord + uTexOffset[0]).rgb;
		    vec3 t1 = texture2D(sTexture, vTextureCoord + uTexOffset[1]).rgb;
		    vec3 t2 = texture2D(sTexture, vTextureCoord + uTexOffset[2]).rgb;
		    vec3 t3 = texture2D(sTexture, vTextureCoord + uTexOffset[3]).rgb;
		    vec3 t4 = texture2D(sTexture, vTextureCoord + uTexOffset[4]).rgb;
		    vec3 t5 = texture2D(sTexture, vTextureCoord + uTexOffset[5]).rgb;
		    vec3 t6 = texture2D(sTexture, vTextureCoord + uTexOffset[6]).rgb;
		    vec3 t7 = texture2D(sTexture, vTextureCoord + uTexOffset[7]).rgb;
		    vec3 t8 = texture2D(sTexture, vTextureCoord + uTexOffset[8]).rgb;
		    vec3 sumH = t0 * uKernel[0] + t1 * uKernel[1] + t2 * uKernel[2]
		              + t3 * uKernel[3] + t4 * uKernel[4] + t5 * uKernel[5]
		              + t6 * uKernel[6] + t7 * uKernel[7] + t8 * uKernel[8];
		    //vec3 sumV = t0 * uKernel[ 9] + t1 * uKernel[10] + t2 * uKernel[11]
		    //          + t3 * uKernel[12] + t4 * uKernel[13] + t5 * uKernel[14]
		    //          + t6 * uKernel[15] + t7 * uKernel[16] + t8 * uKernel[17];
		    //float mag = length(abs(sumH) + abs(sumV));
		    float mag = length(sumH);
		    gl_FragColor = vec4(vec3(mag), 1.0);
		}
		""";

	public static final String FRAGMENT_SHADER_SOBEL_ES2
		= String.format(FRAGMENT_SHADER_SOBEL_BASE_ES2, SHADER_VERSION_ES2, HEADER_2D, SAMPLER_2D);
	public static final String FRAGMENT_SHADER_EXT_SOBEL_ES2
		= String.format(FRAGMENT_SHADER_SOBEL_BASE_ES2, SHADER_VERSION_ES2, HEADER_OES_ES2, SAMPLER_OES);

	/**
	 * Sobel Effect付与のフラグメントシェーダ
	 * for ES3
	 */
	public static final String FRAGMENT_SHADER_SOBEL_BASE_ES3
		= """
		%s
		%s
		#define KERNEL_SIZE3x3 9
		precision highp float;
		in       vec2 vTextureCoord;
		uniform %s    sTexture;
		uniform float uKernel[18];
		uniform vec2  uTexOffset[KERNEL_SIZE3x3];
		uniform float uColorAdjust;
		layout(location = 0) out vec4 o_FragColor;
		void main() {
		    vec3 t0 = texture(sTexture, vTextureCoord + uTexOffset[0]).rgb;
		    vec3 t1 = texture(sTexture, vTextureCoord + uTexOffset[1]).rgb;
		    vec3 t2 = texture(sTexture, vTextureCoord + uTexOffset[2]).rgb;
		    vec3 t3 = texture(sTexture, vTextureCoord + uTexOffset[3]).rgb;
		    vec3 t4 = texture(sTexture, vTextureCoord + uTexOffset[4]).rgb;
		    vec3 t5 = texture(sTexture, vTextureCoord + uTexOffset[5]).rgb;
		    vec3 t6 = texture(sTexture, vTextureCoord + uTexOffset[6]).rgb;
		    vec3 t7 = texture(sTexture, vTextureCoord + uTexOffset[7]).rgb;
		    vec3 t8 = texture(sTexture, vTextureCoord + uTexOffset[8]).rgb;
		    vec3 sumH = t0 * uKernel[0] + t1 * uKernel[1] + t2 * uKernel[2]
		              + t3 * uKernel[3] + t4 * uKernel[4] + t5 * uKernel[5]
		              + t6 * uKernel[6] + t7 * uKernel[7] + t8 * uKernel[8];
		    //vec3 sumV = t0 * uKernel[ 9] + t1 * uKernel[10] + t2 * uKernel[11]
		    //          + t3 * uKernel[12] + t4 * uKernel[13] + t5 * uKernel[14]
		    //          + t6 * uKernel[15] + t7 * uKernel[16] + t8 * uKernel[17];
		    //float mag = length(abs(sumH) + abs(sumV));
		    float mag = length(sumH);
		    o_FragColor = vec4(vec3(mag), 1.0);
		}
		""";

	public static final String FRAGMENT_SHADER_SOBEL_ES3
		= String.format(FRAGMENT_SHADER_SOBEL_BASE_ES3, SHADER_VERSION_ES3, HEADER_2D, SAMPLER_2D);
	public static final String FRAGMENT_SHADER_EXT_SOBEL_ES3
		= String.format(FRAGMENT_SHADER_SOBEL_BASE_ES3, SHADER_VERSION_ES3, HEADER_OES_ES3, SAMPLER_OES);

//--------------------------------------------------------------------------------
	public static final float[] KERNEL_NULL = { 0f, 0f, 0f,  0f, 1f, 0f,  0f, 0f, 0f};
	public static final float[] KERNEL_SOBEL_H = { 1f, 0f, -1f, 2f, 0f, -2f, 1f, 0f, -1f, };	// ソーベル(1次微分)
	public static final float[] KERNEL_SOBEL_V = { 1f, 2f, 1f, 0f, 0f, 0f, -1f, -2f, -1f, };
	public static final float[] KERNEL_SOBEL2_H = { 3f, 0f, -3f, 10f, 0f, -10f, 3f, 0f, -3f, };
	public static final float[] KERNEL_SOBEL2_V = { 3f, 10f, 3f, 0f, 0f, 0f, -3f, -10f, -3f, };
	public static final float[] KERNEL_SHARPNESS = { 0f, -1f, 0f, -1f, 5f, -1f, 0f, -1f, 0f,};	// シャープネス
	public static final float[] KERNEL_EDGE_DETECT = { -1f, -1f, -1f, -1f, 8f, -1f, -1f, -1f, -1f, }; // エッジ検出
	public static final float[] KERNEL_EMBOSS = { 2f, 0f, 0f, 0f, -1f, 0f, 0f, 0f, -1f };	// エンボス, オフセット0.5f
	public static final float[] KERNEL_SMOOTH = { 1/9f, 1/9f, 1/9f, 1/9f, 1/9f, 1/9f, 1/9f, 1/9f, 1/9f, };	// 移動平均
	public static final float[] KERNEL_GAUSSIAN = { 1/16f, 2/16f, 1/16f, 2/16f, 4/16f, 2/16f, 1/16f, 2/16f, 1/16f, };	// ガウシアン(ノイズ除去/)
	public static final float[] KERNEL_BRIGHTEN = { 1f, 1f, 1f, 1f, 2f, 1f, 1f, 1f, 1f, };
	public static final float[] KERNEL_LAPLACIAN = { 1f, 1f, 1f, 1f, -8f, 1f, 1f, 1f, 1f, };	// ラプラシアン(2次微分, 8近傍)
	public static final float[] KERNEL_LAPLACIAN8 = KERNEL_LAPLACIAN;	// ラプラシアン(2次微分, 8近傍)
	public static final float[] KERNEL_LAPLACIAN4 = { 0f, 1f, 0f, 1f, -4f, 1f, 0f, 1f, 0f, };	// ラプラシアン(2次微分, 4近傍)　8近傍より輪郭線が弱い

	/**
	 * カーネル関数による映像効果付与のフラグメントシェーダ
	 * for ES2
	 */
	private static final String FRAGMENT_SHADER_FILT3x3_BASE_ES2
		= """
		%s
		%s
		#define KERNEL_SIZE3x3 9
		precision highp float;
		varying       vec2 vTextureCoord;
		uniform %s    sTexture;
		uniform float uKernel[18];
		uniform vec2  uTexOffset[KERNEL_SIZE3x3];
		uniform float uColorAdjust;
		void main() {
		    vec4 sum = vec4(0.0);
		    sum += texture2D(sTexture, vTextureCoord + uTexOffset[0]) * uKernel[0];
		    sum += texture2D(sTexture, vTextureCoord + uTexOffset[1]) * uKernel[1];
		    sum += texture2D(sTexture, vTextureCoord + uTexOffset[2]) * uKernel[2];
		    sum += texture2D(sTexture, vTextureCoord + uTexOffset[3]) * uKernel[3];
		    sum += texture2D(sTexture, vTextureCoord + uTexOffset[4]) * uKernel[4];
		    sum += texture2D(sTexture, vTextureCoord + uTexOffset[5]) * uKernel[5];
		    sum += texture2D(sTexture, vTextureCoord + uTexOffset[6]) * uKernel[6];
		    sum += texture2D(sTexture, vTextureCoord + uTexOffset[7]) * uKernel[7];
		    sum += texture2D(sTexture, vTextureCoord + uTexOffset[8]) * uKernel[8];
		    gl_FragColor = sum + uColorAdjust;
		}
		""";

	public static final String FRAGMENT_SHADER_FILT3x3_ES2
		= String.format(FRAGMENT_SHADER_FILT3x3_BASE_ES2, SHADER_VERSION_ES2, HEADER_2D, SAMPLER_2D);
	public static final String FRAGMENT_SHADER_EXT_FILT3x3_ES2
		= String.format(FRAGMENT_SHADER_FILT3x3_BASE_ES2, SHADER_VERSION_ES2, HEADER_OES_ES2, SAMPLER_OES);

	/**
	 * カーネル関数による映像効果付与のフラグメントシェーダ
	 * for ES3
	 */
	private static final String FRAGMENT_SHADER_FILT3x3_BASE_ES3
		= """
		%s
		%s
		#define KERNEL_SIZE3x3 9
		precision highp float;
		in       vec2 vTextureCoord;
		uniform %s    sTexture;
		uniform float uKernel[18];
		uniform vec2  uTexOffset[KERNEL_SIZE3x3];
		uniform float uColorAdjust;
		layout(location = 0) out vec4 o_FragColor;
		void main() {
		    vec4 sum = vec4(0.0);
		    sum += texture(sTexture, vTextureCoord + uTexOffset[0]) * uKernel[0];
		    sum += texture(sTexture, vTextureCoord + uTexOffset[1]) * uKernel[1];
		    sum += texture(sTexture, vTextureCoord + uTexOffset[2]) * uKernel[2];
		    sum += texture(sTexture, vTextureCoord + uTexOffset[3]) * uKernel[3];
		    sum += texture(sTexture, vTextureCoord + uTexOffset[4]) * uKernel[4];
		    sum += texture(sTexture, vTextureCoord + uTexOffset[5]) * uKernel[5];
		    sum += texture(sTexture, vTextureCoord + uTexOffset[6]) * uKernel[6];
		    sum += texture(sTexture, vTextureCoord + uTexOffset[7]) * uKernel[7];
		    sum += texture(sTexture, vTextureCoord + uTexOffset[8]) * uKernel[8];
		    o_FragColor = sum + uColorAdjust;
		}
		""";

	public static final String FRAGMENT_SHADER_FILT3x3_ES3
		= String.format(FRAGMENT_SHADER_FILT3x3_BASE_ES3, SHADER_VERSION_ES3, HEADER_2D, SAMPLER_2D);
	public static final String FRAGMENT_SHADER_EXT_FILT3x3_ES3
		= String.format(FRAGMENT_SHADER_FILT3x3_BASE_ES3, SHADER_VERSION_ES3, HEADER_OES_ES3, SAMPLER_OES);

}
