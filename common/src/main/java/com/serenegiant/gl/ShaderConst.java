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

import androidx.annotation.Size;

/**
 * Created by saki on 16/08/26.
 * フラグメントシェーダーとかの文字列定数達を集める
 */
public class ShaderConst implements GLConst {
	public static final String SHADER_VERSION_ES2 = "#version 100\n";
	public static final String SHADER_VERSION_ES3 = "#version 300 es\n";
	public static final String SHADER_VERSION_ES31 = "#version 310 es\n";
	public static final String SHADER_VERSION_ES32 = "#version 320 es\n";

	@Deprecated
	public static final String HEADER_2D = "";
	public static final String HEADER_2D_ES2 = "";
	public static final String HEADER_2D_ES3 = "";
	@Deprecated
	public static final String SAMPLER_2D = "sampler2D";
	public static final String SAMPLER_2D_ES2 = "sampler2D";
	public static final String SAMPLER_2D_ES3 = "sampler2D";

	public static final String HEADER_OES_ES2 = "#extension GL_OES_EGL_image_external : require\n";
	public static final String HEADER_OES_ES3 = "#extension GL_OES_EGL_image_external_essl3 : require\n";
	@Deprecated
	public static final String SAMPLER_OES = "samplerExternalOES";
	public static final String SAMPLER_OES_ES2 = "samplerExternalOES";
	public static final String SAMPLER_OES_ES3 = "samplerExternalOES";

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
// 3x3カーネル関数
	public static final float[] KERNEL_NULL = { 0f, 0f, 0f,  0f, 1f, 0f,  0f, 0f, 0f};
	public static final float[] KERNEL_SOBEL_H = { 1f, 0f, -1f, 2f, 0f, -2f, 1f, 0f, -1f, };	// ソーベル(1次微分)
	public static final float[] KERNEL_SOBEL_V = { 1f, 2f, 1f, 0f, 0f, 0f, -1f, -2f, -1f, };
	public static final float[] KERNEL_SOBEL2_H = { 3f, 0f, -3f, 10f, 0f, -10f, 3f, 0f, -3f, };
	public static final float[] KERNEL_SOBEL2_V = { 3f, 10f, 3f, 0f, 0f, 0f, -3f, -10f, -3f, };
	public static final float[] KERNEL_PREWITT_H = { -1f, -1f, -1f,  0f, 0f, 0f,  1f, 1f, 1f};
	public static final float[] KERNEL_PREWITT_V = { -1f, 0f, 1f,  -1f, 0f, 1f,  -1f, 0f, 1f};
	public static final float[] KERNEL_ROBERTS_H = { 0f, 0f, 0f,  0f, 1f, 0f,  0f, 0f, -1f};
	public static final float[] KERNEL_ROBERTS_V = { 0f, 0f, 0f,  0f, 0f, -1f,  0f, 1f, 0f};
	public static final float[] KERNEL_EDGE_ENHANCE4 = { 0f, -1f, 0f, -1f, 5f, -1f, 0f, -1f, 0f,};	// エッジ強調4近傍
	public static final float[] KERNEL_EDGE_ENHANCE8 = { -1f, -1f, -1f, -1f, 9f, -1f, -1f, -1f, -1f, }; // エッジ強調8近傍
	public static final float[] KERNEL_SHARPNESS = KERNEL_EDGE_ENHANCE4; // シャープネス=エッジ強調4近傍
	public static final float[] KERNEL_EDGE_DETECT = { -1f, -1f, -1f, -1f, 8f, -1f, -1f, -1f, -1f, }; // エッジ検出
	public static final float[] KERNEL_EMBOSS = { 2f, 0f, 0f, 0f, -1f, 0f, 0f, 0f, -1f };	// エンボス, オフセット0.5f
	public static final float[] KERNEL_SMOOTH = { 1/9f, 1/9f, 1/9f, 1/9f, 1/9f, 1/9f, 1/9f, 1/9f, 1/9f, };	// 移動平均
	public static final float[] KERNEL_GAUSSIAN = { 1/16f, 2/16f, 1/16f, 2/16f, 4/16f, 2/16f, 1/16f, 2/16f, 1/16f, };	// ガウシアン(ノイズ除去/)
	public static final float[] KERNEL_BRIGHTEN = { 1f, 1f, 1f, 1f, 2f, 1f, 1f, 1f, 1f, };
	public static final float[] KERNEL_LAPLACIAN = { 1f, 1f, 1f, 1f, -8f, 1f, 1f, 1f, 1f, };	// ラプラシアン(2次微分, 8近傍)
	public static final float[] KERNEL_LAPLACIAN8 = KERNEL_LAPLACIAN;	// ラプラシアン(2次微分, 8近傍)
	public static final float[] KERNEL_LAPLACIAN4 = { 0f, 1f, 0f, 1f, -4f, 1f, 0f, 1f, 0f, };	// ラプラシアン(2次微分, 4近傍)　8近傍より輪郭線が弱い

//--------------------------------------------------------------------------------
// 関数文字列定義
	/**
	 * RGBをHSVに変換
	 * {R[0.0-1.0], G[0.0-1.0], B([0.0-1.0]} => {H[0.0-1.0], S[0.0-1.0], V[0.0-1.0]}
	 */
	public static final String FUNC_RGB2HSV =
		"""
		vec3 rgb2hsv(vec3 c) {
			const highp vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
			const highp float e = 1.0e-10;
			vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));
			vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));
			float d = q.x - min(q.w, q.y);
			return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);
		}
		""";
	/**
	 * HSVをRGBに変換
	 * {H[0.0-1.0], S[0.0-1.0], V[0.0-1.0]} => {R[0.0-1.0], G[0.0-1.0], B([0.0-1.0]}
	 */
	public static final String FUNC_HSV2RGB =
		"""
		vec3 hsv2rgb(vec3 c) {
			const highp vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
			vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
			return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
		}
		""";

	public static final String HSV_FUNCTIONS = FUNC_RGB2HSV + FUNC_HSV2RGB;

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

	/**
	 * モデルビュー変換行列とテクスチャ変換行列適用するだけの頂点シェーダー
	 * for ES3
	 */
	public static final String VERTEX_SHADER_ES31 =
		"""
		#version 310 es
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

	/**
	 * モデルビュー変換行列とテクスチャ変換行列適用するだけの頂点シェーダー
	 * for ES3
	 */
	public static final String VERTEX_SHADER_ES32 =
		"""
		#version 320 es
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
		= String.format(FRAGMENT_SHADER_BASE_ES2,
			SHADER_VERSION_ES2, HEADER_2D_ES2, SAMPLER_2D_ES2);
	public static final String FRAGMENT_SHADER_EXT_ES2
		= String.format(FRAGMENT_SHADER_BASE_ES2,
			SHADER_VERSION_ES2, HEADER_OES_ES2, SAMPLER_OES_ES2);

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
		= String.format(FRAGMENT_SHADER_BASE_ES3,
			SHADER_VERSION_ES3, HEADER_2D_ES3, SAMPLER_2D_ES3);
	public static final String FRAGMENT_SHADER_EXT_ES3
		= String.format(FRAGMENT_SHADER_BASE_ES3,
			SHADER_VERSION_ES3, HEADER_OES_ES3, SAMPLER_OES_ES3);


//--------------------------------------------------------------------------------
	/*
	 * R' | m11 m21 m31 m41 | R
	 * G' | m12 m22 m32 m42 | G
	 * B' | m13 m23 m33 m43 | B
	 * 1  | m14 m24 m34 m44 | 1
	 *
	 * R' = m11R + m21G + m31B + m41
	 * G' = m12R + m22G + m32B + m42
	 * B' = m13R + m23G + m33B + m43
	 * A' = A
	 */

	/**
	 * 無変換の色変換行列(単位行列)
	 */
	@Size(value=16)
	public static final float[] COLOR_MATRIX_IDENTITY = {
		1.0f, 0.0f, 0.0f, 0.0f,
		0.0f, 1.0f, 0.0f, 0.0f,
		0.0f, 0.0f, 1.0f, 0.0f,
		0.0f, 0.0f, 0.0f, 1.0f,
	};

	/**
	 * セピア色
	 */
	@Size(value=32)
	public static final float[] COLOR_MATRIX_SEPIA = {
		// 1つ目
		0.393f, 0.349f, 0.272f, 0.0f,
		0.769f, 0.686f, 0.534f, 0.0f,
		0.189f, 0.168f, 0.131f, 0.0f,
		0.0f,   0.0f,   0.0f,   1.0f,
		// 2つ目
		0.3588f, 0.2990f, 0.2392f, 0.0f,
		0.7044f, 0.5870f, 0.4696f, 0.0f,
		0.1368f, 0.1140f, 0.0912f, 0.0f,
		0.0f,    0.0f,    0.0f,    1.0f,
	};

	/**
	 * グレースケール
	 */
	@Size(value = 16)
	public static final float[] COLOR_MATRIX_GRAYSCALE = {
		0.2125f, 0.2125f, 0.2125f, 0.0f,
		0.7154f, 0.7154f, 0.7154f, 0.0f,
		0.0721f, 0.0721f, 0.0721f, 0.0f,
		0.0f, 0.0f,  0.0f,  1.0f,
	};

	/**
	 * 赤黒/緑黒/青黒
	 */
	@Size(value = 48)
	public static final float[] COLOR_MATRIX_BLACK_COLOR = {
		// RED-BLACK
		1.0f, 0.0f, 0.0f, 0.0f,
		1.0f, 0.0f, 0.0f, 0.0f,
		1.0f, 0.0f, 0.0f, 0.0f,
		0.0f, 0.0f, 0.0f, 1.0f,
		// GREEN-BLACK
		0.0f, 1.0f, 0.0f, 0.0f,
		0.0f, 1.0f, 0.0f, 0.0f,
		0.0f, 1.0f, 0.0f, 0.0f,
		0.0f, 0.0f, 0.0f, 1.0f,
		// BLUE-BLACK
		0.0f, 0.0f, 1.0f, 0.0f,
		0.0f, 0.0f, 1.0f, 0.0f,
		0.0f, 0.0f, 1.0f, 0.0f,
		0.0f, 0.0f, 0.0f, 1.0f,
	};

	/**
	 * コントラスト
	 */
	@Size(value = 16)
	public static final float[] COLOR_MATRIX_CONTRAST = {
		// up
		1.5f, 0.0f, 0.0f, 0.0f,
		0.0f, 1.5f, 0.0f, 0.0f,
		0.0f, 0.0f, 1.5f, 0.0f,
		0.0f, 0.0f, 0.0f, 1.0f,
		// down
		0.5f, 0.0f, 0.0f, 0.0f,
		0.0f, 0.5f, 0.0f, 0.0f,
		0.0f, 0.0f, 0.5f, 0.0f,
		0.0f, 0.0f, 0.0f, 1.0f,
	};

	/**
	 * FIXME ネガポジ反転 真っ黒になってしまう
	 * うまく適用されないのでフラグメントシェーダーでネガポジ反転させるMediaEffectGLNegativeを作った
	 */
	@Size(value = 16)
	public static final float[] COLOR_MATRIX_NEGATIVE = {
		-1.0f, 0.0f, 0.0f, 1.0f,
		0.0f, -1.0f, 0.0f, 1.0f,
		0.0f, 0.0f, -1.0f, 1.0f,
		0.0f, 0.0f, 0.0f, 1.0f,
	};

	/**
	 * 色変換行列を適用するフラグメントシェーダ
	 * for ES2
	 */
	private static final String FRAGMENT_SHADER_COLOR_MATRIX_BASE_ES2 =
		"""
		%s
		%s
		precision mediump float;
		varying vec2 vTextureCoord;
		uniform %s sTexture;
		uniform mat4 uColorMatrix;
		void main() {
			vec4 cl = texture2D(sTexture, vTextureCoord);
			float a = cl.a;
			cl = vec4(cl.rgb, 1.0);
			gl_FragColor = vec4((uColorMatrix * cl).rgb, a);
		}
		""";

	public static final String FRAGMENT_SHADER_COLOR_MATRIX_ES2
		= String.format(FRAGMENT_SHADER_COLOR_MATRIX_BASE_ES2,
			SHADER_VERSION_ES2, HEADER_2D_ES2, SAMPLER_2D_ES2);
	public static final String FRAGMENT_SHADER_COLOR_MATRIX_EXT_ES2
		= String.format(FRAGMENT_SHADER_COLOR_MATRIX_BASE_ES2,
			SHADER_VERSION_ES2, HEADER_OES_ES2, SAMPLER_OES_ES2);

	/**
	 * 色変換行列を適用するフラグメントシェーダ
	 * for ES3
	 */
	private static final String FRAGMENT_SHADER_COLOR_MATRIX_BASE_ES3 =
		"""
		%s
		%s
		precision mediump float;
		in vec2 vTextureCoord;
		uniform %s sTexture;
		uniform mat4 uColorMatrix;
		layout(location = 0) out vec4 o_FragColor;
		void main() {
			vec4 cl = texture(sTexture, vTextureCoord);
			float a = cl.a;
			cl = vec4(cl.rgb, 1.0);
		    o_FragColor = vec4((uColorMatrix * cl).rgb, a);
		}
		""";

	public static final String FRAGMENT_SHADER_COLOR_MATRIX_ES3
		= String.format(FRAGMENT_SHADER_COLOR_MATRIX_BASE_ES3,
			SHADER_VERSION_ES3, HEADER_2D_ES3, SAMPLER_2D_ES3);
	public static final String FRAGMENT_SHADER_COLOR_MATRIX_EXT_ES3
		= String.format(FRAGMENT_SHADER_COLOR_MATRIX_BASE_ES3,
			SHADER_VERSION_ES3, HEADER_OES_ES3, SAMPLER_OES_ES3);

//--------------------------------------------------------------------------------
	/**
	 * 非線形濃度変換(明るさ変換)と色変換行列を適用するフラグメントシェーダ
	 * 補正データ点数を減らすため64+1個の補正データとしてintensityがその
	 * 間に来る場合は補正値を線形補完して求める
	 * for ES2
	 */
	private static final String FRAGMENT_SHADER_COLOR_CORRECTION_BASE_ES2 =
		"""
		%s
		%s
		precision mediump float;
		varying vec2 vTextureCoord;
		uniform %s sTexture;
		uniform mat4 uColorMatrix;
		uniform float uParams[65];
		const highp float iStep = 0.0158730; // = 1.0 / 63.0;
		const highp vec3 conv = vec3(0.2125, 0.7154, 0.0721);
		void main() {
			vec4 cl = texture2D(sTexture, vTextureCoord);
			highp float intensity = dot(cl.rgb, conv);
			int index = int(clamp(0.0, 63.0, intensity * 63.0));
			highp float c0 = uParams[index];
			highp float c1 = uParams[index+1];
			highp float i0 = float(index) * iStep;
			// 線形補間
			highp float adjust = (c1 - c0) / iStep * (intensity - i0) + c0;
			float a = cl.a;
			// adjust brightness
			cl.rgb += adjust;
			gl_FragColor = vec4((uColorMatrix * cl).rgb, a);
		}
		""";

	public static final String FRAGMENT_SHADER_COLOR_CORRECTION_ES2
		= String.format(FRAGMENT_SHADER_COLOR_CORRECTION_BASE_ES2,
		SHADER_VERSION_ES2, HEADER_2D_ES2, SAMPLER_2D_ES2);
	public static final String FRAGMENT_SHADER_COLOR_CORRECTION_EXT_ES2
		= String.format(FRAGMENT_SHADER_COLOR_CORRECTION_BASE_ES2,
		SHADER_VERSION_ES2, HEADER_OES_ES2, SAMPLER_OES_ES2);

	/**
	 * 非線形濃度変換(明るさ変換)と色変換行列を適用するフラグメントシェーダ
	 * 補正データ点数を減らすため64+1個の補正データとしてintensityがその
	 * 間に来る場合は補正値を線形補完して求める
	 * for ES3
	 */
	private static final String FRAGMENT_SHADER_COLOR_CORRECTION_BASE_ES3 =
		"""
		%s
		%s
		precision mediump float;
		in vec2 vTextureCoord;
		uniform %s sTexture;
		uniform mat4 uColorMatrix;
		uniform float uParams[256];
		const highp vec3 conv = vec3(0.2125, 0.7154, 0.0721);
		layout(location = 0) out vec4 o_FragColor;
		void main() {
			vec4 cl = texture(sTexture, vTextureCoord);
			highp float intensity = dot(cl.rgb, conv);
			int index = int(clamp(0.0, 63.0, intensity * 63.0));
			highp float c0 = uParams[index];
			highp float c1 = uParams[index+1];
			highp float i0 = float(index) * iStep;
			highp float adjust = (c1 - c0) / iStep * (intensity - i0) + c0;
			float a = cl.a;
			// adjust brightness
			cl.rgb += adjust;
		    o_FragColor = vec4((uColorMatrix * cl).rgb, a);
		}
		""";

	public static final String FRAGMENT_SHADER_COLOR_CORRECTION_ES3
		= String.format(FRAGMENT_SHADER_COLOR_CORRECTION_BASE_ES3,
		SHADER_VERSION_ES3, HEADER_2D_ES3, SAMPLER_2D_ES3);
	public static final String FRAGMENT_SHADER_COLOR_CORRECTION_EXT_ES3
		= String.format(FRAGMENT_SHADER_COLOR_CORRECTION_BASE_ES3,
		SHADER_VERSION_ES3, HEADER_OES_ES3, SAMPLER_OES_ES3);

//--------------------------------------------------------------------------------
	/**
	 * 白黒二値変換するフラグメントシェーダ
	 * for ES2
	 */
	private static final String FRAGMENT_SHADER_BW_BASE_ES2 =
		"""
		%s
		%s
		precision mediump float;
		varying vec2 vTextureCoord;
		uniform %s sTexture;
		const highp vec3 conv = vec3(0.2125, 0.7154, 0.0721);
		void main() {
			vec4 tc = texture2D(sTexture, vTextureCoord);
		    float intensity = dot(tc.rgb, conv);
			gl_FragColor = vec4(intensity, intensity, intensity, 1.0);
		}
		""";

	public static final String FRAGMENT_SHADER_BW_ES2
		= String.format(FRAGMENT_SHADER_BW_BASE_ES2,
			SHADER_VERSION_ES2, HEADER_2D_ES2, SAMPLER_2D_ES2);
	public static final String FRAGMENT_SHADER_EXT_BW_ES2
		= String.format(FRAGMENT_SHADER_BW_BASE_ES2,
			SHADER_VERSION_ES2, HEADER_OES_ES2, SAMPLER_OES_ES2);

	/**
	 * 白黒二値変換するフラグメントシェーダ
	 * for ES3
	 */
	private static final String FRAGMENT_SHADER_BW_BASE_ES3 =
		"""
		%s
		%s
		precision mediump float;
		in vec2 vTextureCoord;
		uniform %s sTexture;
		const highp vec3 conv = vec3(0.2125, 0.7154, 0.0721);
		layout(location = 0) out vec4 o_FragColor;
		void main() {
		    vec4 tc = texture(sTexture, vTextureCoord);
		    float intensity = dot(tc.rgb, conv);
		    o_FragColor = vec4(intensity, intensity, intensity, 1.0);
		}
		""";

	public static final String FRAGMENT_SHADER_BW_ES3
		= String.format(FRAGMENT_SHADER_BW_BASE_ES3,
			SHADER_VERSION_ES3, HEADER_2D_ES3, SAMPLER_2D_ES3);
	public static final String FRAGMENT_SHADER_EXT_BW_ES3
		= String.format(FRAGMENT_SHADER_BW_BASE_ES3,
			SHADER_VERSION_ES3, HEADER_OES_ES3, SAMPLER_OES_ES3);

//--------------------------------------------------------------------------------
	/**
	 * ナイトビジョン風に強調表示するフラグメントシェーダ
	 * for ES2
	 */
	private static final String FRAGMENT_SHADER_NIGHT_BASE_ES2 =
		"""
		%s
		%s
		precision mediump float;
		varying vec2 vTextureCoord;
		uniform %s sTexture;
		const highp vec3 conv = vec3(0.2125, 0.7154, 0.0721);
		void main() {
			vec4 tc = texture2D(sTexture, vTextureCoord);
			float color = (dot(tc.rgb, conv) - 0.5 * 1.5) + 0.8;
			gl_FragColor = vec4(color, color + 0.15, color, 1.0);
		}
		""";

	public static final String FRAGMENT_SHADER_NIGHT_ES2
		= String.format(FRAGMENT_SHADER_NIGHT_BASE_ES2,
			SHADER_VERSION_ES2, HEADER_2D_ES2, SAMPLER_2D_ES2);
	public static final String FRAGMENT_SHADER_EXT_NIGHT_ES2
		= String.format(FRAGMENT_SHADER_NIGHT_BASE_ES2,
			SHADER_VERSION_ES2, HEADER_OES_ES2, SAMPLER_OES_ES2);

	/**
	 * ナイトビジョン風に強調表示するフラグメントシェーダ
	 * for ES3
	 */
	private static final String FRAGMENT_SHADER_NIGHT_BASE_ES3 =
		"""
		%s
		%s
		precision mediump float;
		in vec2 vTextureCoord;
		uniform %s sTexture;
		const highp vec3 conv = vec3(0.2125, 0.7154, 0.0721);
		layout(location = 0) out vec4 o_FragColor;
		void main() {
		    vec4 tc = texture(sTexture, vTextureCoord);
			float color = (dot(tc.rgb, conv) - 0.5 * 1.5) + 0.8;
		    o_FragColor = vec4(color, color + 0.15, color, 1.0);
		}
		""";

	public static final String FRAGMENT_SHADER_NIGHT_ES3
		= String.format(FRAGMENT_SHADER_NIGHT_BASE_ES3,
			SHADER_VERSION_ES3, HEADER_2D_ES3, SAMPLER_2D_ES3);
	public static final String FRAGMENT_SHADER_EXT_NIGHT_ES3
		= String.format(FRAGMENT_SHADER_NIGHT_BASE_ES3,
			SHADER_VERSION_ES3, HEADER_OES_ES3, SAMPLER_OES_ES3);

//--------------------------------------------------------------------------------
	/**
	 * クロマキー合成用に緑を透過させるフラグメントシェーダ
	 * for ES2
	 */
	private static final String FRAGMENT_SHADER_CHROMA_KEY_BASE_ES2 =
		"""
		%s
		%s
		precision mediump float;
		varying vec2 vTextureCoord;
		uniform %s sTexture;
		const highp vec3 conv = vec3(0.2125, 0.7154, 0.0721);
		void main() {
			vec4 tc = texture2D(sTexture, vTextureCoord);
			float color = (dot(tc.rgb, conv) - 0.5 * 1.5) + 0.8;
			if(tc.g > 0.6 && tc.b < 0.6 && tc.r < 0.6){
				gl_FragColor = vec4(0, 0, 0, 0.0);
			}else{
				gl_FragColor = texture2D(sTexture, vTextureCoord);
			}
		}
		""";

	public static final String FRAGMENT_SHADER_CHROMA_KEY_ES2
		= String.format(FRAGMENT_SHADER_CHROMA_KEY_BASE_ES2,
			SHADER_VERSION_ES2, HEADER_2D_ES2, SAMPLER_2D_ES2);
	public static final String FRAGMENT_SHADER_EXT_CHROMA_KEY_ES2
		= String.format(FRAGMENT_SHADER_CHROMA_KEY_BASE_ES2,
			SHADER_VERSION_ES2, HEADER_OES_ES2, SAMPLER_OES_ES2);

	/**
	 * クロマキー合成用に緑を透過させるフラグメントシェーダ
	 * for ES3
	 */
	private static final String FRAGMENT_SHADER_CHROMA_KEY_BASE_ES3 =
		"""
		%s
		%s
		precision mediump float;
		in vec2 vTextureCoord;
		uniform %s sTexture;
		const highp vec3 conv = vec3(0.2125, 0.7154, 0.0721);
		layout(location = 0) out vec4 o_FragColor;
		void main() {
		    vec4 tc = texture(sTexture, vTextureCoord);
			float color = (dot(tc.rgb, conv) - 0.5 * 1.5) + 0.8;
		    if(tc.g > 0.6 && tc.b < 0.6 && tc.r < 0.6){
		        o_FragColor = vec4(0, 0, 0, 0.0);
		    }else{
		        o_FragColor = texture(sTexture, vTextureCoord);
		    }
		}
		""";

	public static final String FRAGMENT_SHADER_CHROMA_KEY_ES3
		= String.format(FRAGMENT_SHADER_CHROMA_KEY_BASE_ES3,
			SHADER_VERSION_ES3, HEADER_2D_ES3, SAMPLER_2D_ES3);
	public static final String FRAGMENT_SHADER_EXT_CHROMA_KEY_ES3
		= String.format(FRAGMENT_SHADER_CHROMA_KEY_BASE_ES3,
			SHADER_VERSION_ES3, HEADER_OES_ES3, SAMPLER_OES_ES3);

//--------------------------------------------------------------------------------
	/**
	 * SQUEEZE効果付与のフラグメントシェーダ
	 * for ES2
	 */
	private static final String FRAGMENT_SHADER_SQUEEZE_BASE_ES2 =
		"""
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
		= String.format(FRAGMENT_SHADER_SQUEEZE_BASE_ES2,
			SHADER_VERSION_ES2, HEADER_2D_ES2, SAMPLER_2D_ES2);
	public static final String FRAGMENT_SHADER_EXT_SQUEEZE_ES2
		= String.format(FRAGMENT_SHADER_SQUEEZE_BASE_ES2,
			SHADER_VERSION_ES2, HEADER_OES_ES2, SAMPLER_OES_ES2);

	/**
	 * SQUEEZE効果付与のフラグメントシェーダ
	 * for ES3
	 */
	private static final String FRAGMENT_SHADER_SQUEEZE_BASE_ES3 =
		"""
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
		= String.format(FRAGMENT_SHADER_SQUEEZE_BASE_ES3,
			SHADER_VERSION_ES3, HEADER_2D_ES3, SAMPLER_2D_ES3);
	public static final String FRAGMENT_SHADER_EXT_SQUEEZE_ES3
		= String.format(FRAGMENT_SHADER_SQUEEZE_BASE_ES3,
			SHADER_VERSION_ES3, HEADER_OES_ES3, SAMPLER_OES_ES3);

//--------------------------------------------------------------------------------
	/**
	 * TWIRL効果付与のフラグメントシェーダ
	 * for ES2
	 */
	private static final String FRAGMENT_SHADER_TWIRL_BASE_ES2 =
		"""
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
		= String.format(FRAGMENT_SHADER_TWIRL_BASE_ES2,
			SHADER_VERSION_ES2, HEADER_2D_ES2, SAMPLER_2D_ES2);
	public static final String FRAGMENT_SHADER_EXT_TWIRL_ES2
		= String.format(FRAGMENT_SHADER_TWIRL_BASE_ES2,
			SHADER_VERSION_ES2, HEADER_OES_ES2, SAMPLER_OES_ES2);

	/**
	 * TWIRL効果付与のフラグメントシェーダ
	 * for ES3
	 */
	private static final String FRAGMENT_SHADER_TWIRL_BASE_ES3 =
		"""
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
		= String.format(FRAGMENT_SHADER_TWIRL_BASE_ES3,
			SHADER_VERSION_ES3, HEADER_2D_ES3, SAMPLER_2D_ES3);
	public static final String FRAGMENT_SHADER_EXT_TWIRL_ES3
		= String.format(FRAGMENT_SHADER_TWIRL_BASE_ES3,
			SHADER_VERSION_ES3, HEADER_OES_ES3, SAMPLER_OES_ES3);

//--------------------------------------------------------------------------------
	/**
	 * TUNNEL Effect付与のフラグメントシェーダ
	 * for ES2
	 */
	private static final String FRAGMENT_SHADER_TUNNEL_BASE_ES2 =
		"""
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
		= String.format(FRAGMENT_SHADER_TUNNEL_BASE_ES2,
			SHADER_VERSION_ES2, HEADER_2D_ES2, SAMPLER_2D_ES2);
	public static final String FRAGMENT_SHADER_EXT_TUNNEL_ES2
		= String.format(FRAGMENT_SHADER_TUNNEL_BASE_ES2,
			SHADER_VERSION_ES2, HEADER_OES_ES2, SAMPLER_OES_ES2);

	/**
	 * TUNNEL Effect付与のフラグメントシェーダ
	 * for ES3
	 */
	private static final String FRAGMENT_SHADER_TUNNEL_BASE_ES3 =
		"""
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
		= String.format(FRAGMENT_SHADER_TUNNEL_BASE_ES3,
			SHADER_VERSION_ES3, HEADER_2D_ES3, SAMPLER_2D_ES3);
	public static final String FRAGMENT_SHADER_EXT_TUNNEL_ES3
		= String.format(FRAGMENT_SHADER_TUNNEL_BASE_ES3,
			SHADER_VERSION_ES3, HEADER_OES_ES3, SAMPLER_OES_ES3);

//--------------------------------------------------------------------------------
	/**
	 * Bulge Effect付与のフラグメントシェーダ
	 * for ES2
	 */
	private static final String FRAGMENT_SHADER_BULGE_BASE_ES2 =
		"""
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
		= String.format(FRAGMENT_SHADER_BULGE_BASE_ES2,
			SHADER_VERSION_ES2, HEADER_2D_ES2, SAMPLER_2D_ES2);
	public static final String FRAGMENT_SHADER_EXT_BULGE_ES2
		= String.format(FRAGMENT_SHADER_BULGE_BASE_ES2,
			SHADER_VERSION_ES2, HEADER_OES_ES2, SAMPLER_OES_ES2);

	/**
	 * Bulge Effect付与のフラグメントシェーダ
	 * for ES3
	 */
	private static final String FRAGMENT_SHADER_BULGE_BASE_ES3 =
		"""
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
		= String.format(FRAGMENT_SHADER_BULGE_BASE_ES3,
			SHADER_VERSION_ES3, HEADER_2D_ES3, SAMPLER_2D_ES3);
	public static final String FRAGMENT_SHADER_EXT_BULGE_ES3
		= String.format(FRAGMENT_SHADER_BULGE_BASE_ES3,
			SHADER_VERSION_ES3, HEADER_OES_ES3, SAMPLER_OES_ES3);

//--------------------------------------------------------------------------------
	/**
	 * Dent Effect付与のフラグメントシェーダ
	 * for ES2
	 */
	private static final String FRAGMENT_SHADER_DENT_BASE_ES2 =
		"""
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
		= String.format(FRAGMENT_SHADER_DENT_BASE_ES2,
			SHADER_VERSION_ES2, HEADER_2D_ES2, SAMPLER_2D_ES2);
	public static final String FRAGMENT_SHADER_EXT_DENT_ES2
		= String.format(FRAGMENT_SHADER_DENT_BASE_ES2,
			SHADER_VERSION_ES2, HEADER_OES_ES2, SAMPLER_OES_ES2);

	/**
	 * Dent Effect付与のフラグメントシェーダ
	 * for ES3
	 */
	private static final String FRAGMENT_SHADER_DENT_BASE_ES3 =
		"""
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
		= String.format(FRAGMENT_SHADER_DENT_BASE_ES3,
			SHADER_VERSION_ES3, HEADER_2D_ES3, SAMPLER_2D_ES3);
	public static final String FRAGMENT_SHADER_EXT_DENT_ES3
		= String.format(FRAGMENT_SHADER_DENT_BASE_ES3,
			SHADER_VERSION_ES3, HEADER_OES_ES3, SAMPLER_OES_ES3);

//--------------------------------------------------------------------------------
	/**
	 * Fisheye Effect付与のフラグメントシェーダ
	 * for ES2
	 */
	private static final String FRAGMENT_SHADER_FISHEYE_BASE_ES2 =
		"""
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
		= String.format(FRAGMENT_SHADER_FISHEYE_BASE_ES2,
			SHADER_VERSION_ES2, HEADER_2D_ES2, SAMPLER_2D_ES2);
	public static final String FRAGMENT_SHADER_EXT_FISHEYE_ES2
		= String.format(FRAGMENT_SHADER_FISHEYE_BASE_ES2,
			SHADER_VERSION_ES2, HEADER_OES_ES2, SAMPLER_OES_ES2);

	/**
	 * Fisheye Effect付与のフラグメントシェーダ
	 * for ES3
	 */
	private static final String FRAGMENT_SHADER_FISHEYE_BASE_ES3 =
		"""
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
		= String.format(FRAGMENT_SHADER_FISHEYE_BASE_ES3,
			SHADER_VERSION_ES3, HEADER_2D_ES3, SAMPLER_2D_ES3);
	public static final String FRAGMENT_SHADER_EXT_FISHEYE_ES3
		= String.format(FRAGMENT_SHADER_FISHEYE_BASE_ES3,
			SHADER_VERSION_ES3, HEADER_OES_ES3, SAMPLER_OES_ES3);

//--------------------------------------------------------------------------------
	/**
	 * Stretch Effect付与のフラグメントシェーダ
	 * for ES2
	 */
	private static final String FRAGMENT_SHADER_STRETCH_BASE_ES2 =
		"""
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
		= String.format(FRAGMENT_SHADER_STRETCH_BASE_ES2,
			SHADER_VERSION_ES2, HEADER_2D_ES2, SAMPLER_2D_ES2);
	public static final String FRAGMENT_SHADER_EXT_STRETCH_ES2
		= String.format(FRAGMENT_SHADER_STRETCH_BASE_ES2,
			SHADER_VERSION_ES2, HEADER_OES_ES2, SAMPLER_OES_ES2);

	/**
	 * Stretch Effect付与のフラグメントシェーダ
	 * for ES3
	 */
	private static final String FRAGMENT_SHADER_STRETCH_BASE_ES3 =
		"""
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
		= String.format(FRAGMENT_SHADER_STRETCH_BASE_ES3,
			SHADER_VERSION_ES3, HEADER_2D_ES3, SAMPLER_2D_ES3);
	public static final String FRAGMENT_SHADER_EXT_STRETCH_ES3
		= String.format(FRAGMENT_SHADER_STRETCH_BASE_ES3,
			SHADER_VERSION_ES3, HEADER_OES_ES3, SAMPLER_OES_ES3);

//--------------------------------------------------------------------------------
	/**
	 * Mirror Effect付与のフラグメントシェーダ
	 * for ES2
	 */
	private static final String FRAGMENT_SHADER_MIRROR_BASE_ES2 =
		"""
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
		= String.format(FRAGMENT_SHADER_MIRROR_BASE_ES2,
			SHADER_VERSION_ES2, HEADER_2D_ES2, SAMPLER_2D_ES2);
	public static final String FRAGMENT_SHADER_EXT_MIRROR_ES2
		= String.format(FRAGMENT_SHADER_MIRROR_BASE_ES2,
			SHADER_VERSION_ES2, HEADER_OES_ES2, SAMPLER_OES_ES2);

	/**
	 * Mirror Effect付与のフラグメントシェーダ
	 * for ES3
	 */
	private static final String FRAGMENT_SHADER_MIRROR_BASE_ES3 =
		"""
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
		= String.format(FRAGMENT_SHADER_MIRROR_BASE_ES3,
			SHADER_VERSION_ES3, HEADER_2D_ES3, SAMPLER_2D_ES3);
	public static final String FRAGMENT_SHADER_EXT_MIRROR_ES3
		= String.format(FRAGMENT_SHADER_MIRROR_BASE_ES3,
			SHADER_VERSION_ES3, HEADER_OES_ES3, SAMPLER_OES_ES3);

//--------------------------------------------------------------------------------
// 輝度調整のフラグメントシェーダー
	private static final String FRAGMENT_SHADER_BRIGHTNESS_BASE_ES2 =
		"""
		%s
		%s
		precision highp float;
		varying vec2 vTextureCoord;
		uniform %s sTexture;
		uniform float uColorAdjust;
		void main() {
		    highp vec4 tex = texture2D(sTexture, vTextureCoord);
		    gl_FragColor = vec4(tex.rgb + vec3(uColorAdjust, uColorAdjust, uColorAdjust), tex.w);
		}
		""";

	public static final String FRAGMENT_SHADER_BRIGHTNESS_ES2
		= String.format(FRAGMENT_SHADER_BRIGHTNESS_BASE_ES2,
			SHADER_VERSION_ES2, HEADER_2D_ES2, SAMPLER_2D_ES2);
	public static final String FRAGMENT_SHADER_EXT_BRIGHTNESS_ES2
		= String.format(FRAGMENT_SHADER_BRIGHTNESS_BASE_ES2,
			SHADER_VERSION_ES2, HEADER_OES_ES2, SAMPLER_OES_ES2);

	private static final String FRAGMENT_SHADER_BRIGHTNESS_BASE_ES3 =
		"""
		%s
		%s
		precision highp float;
		in vec2 vTextureCoord;
		uniform %s sTexture;
		uniform float uColorAdjust;
		layout(location = 0) out vec4 o_FragColor;
		void main() {
		    highp vec4 tex = texture(sTexture, vTextureCoord);
		    o_FragColor = vec4(tex.rgb + vec3(uColorAdjust, uColorAdjust, uColorAdjust), tex.w);
		}
		""";

	public static final String FRAGMENT_SHADER_BRIGHTNESS_ES3
		= String.format(FRAGMENT_SHADER_BRIGHTNESS_BASE_ES3,
			SHADER_VERSION_ES3, HEADER_2D_ES3, SAMPLER_2D_ES3);
	public static final String FRAGMENT_SHADER_EXT_BRIGHTNESS_ES3
		= String.format(FRAGMENT_SHADER_BRIGHTNESS_BASE_ES3,
			SHADER_VERSION_ES3, HEADER_OES_ES3, SAMPLER_OES_ES3);

//--------------------------------------------------------------------------------
// クロスプロセスフィルターのフラグメントシェーダー
	private static final String FRAGMENT_SHADER_CROSS_PROCESS_BASE_ES2 =
		"""
		%s
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

	public static final String FRAGMENT_SHADER_CROSS_PROCESS_ES2
		= String.format(FRAGMENT_SHADER_CROSS_PROCESS_BASE_ES2,
			SHADER_VERSION_ES2, HEADER_2D_ES2, SAMPLER_2D_ES2);
	public static final String FRAGMENT_SHADER_EXT_CROSS_PROCESS_ES2
		= String.format(FRAGMENT_SHADER_CROSS_PROCESS_BASE_ES2,
			SHADER_VERSION_ES2, HEADER_OES_ES2, SAMPLER_OES_ES2);

	private static final String FRAGMENT_SHADER_CROSS_PROCESS_BASE_ES3 =
		"""
		%s
		%s
		precision mediump float;
		uniform %s sTexture;
		in vec2 vTextureCoord;
		layout(location = 0) out vec4 o_FragColor;
		void main() {
			vec4 color = texture(sTexture, vTextureCoord);
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
			o_FragColor = vec4(ncolor.rgb, color.a);
		}
		""";

	public static final String FRAGMENT_SHADER_CROSS_PROCESS_ES3
		= String.format(FRAGMENT_SHADER_CROSS_PROCESS_BASE_ES3,
			SHADER_VERSION_ES3, HEADER_2D_ES3, SAMPLER_2D_ES3);
	public static final String FRAGMENT_SHADER_EXT_CROSS_PROCESS_ES3
		= String.format(FRAGMENT_SHADER_CROSS_PROCESS_BASE_ES3,
			SHADER_VERSION_ES3, HEADER_OES_ES3, SAMPLER_OES_ES3);


//--------------------------------------------------------------------------------
// ドキュメンタリー風映像フィルターのフラグメントシェーダー
	private static final String FRAGMENT_SHADER_DOCUMENTARY_BASE_ES2 =
		"""
		%s
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
		const highp vec3 conv = vec3(0.2125, 0.7154, 0.0721);
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
			highp float intensity = dot(new_color, conv);
			new_color = vec3(intensity, intensity, intensity);
			// vignette
			float d = distance(vTextureCoord, vignetteCenter);
			float percent = smoothstep(0.3, 0.75, d) * uColorAdjust;
			gl_FragColor = vec4(mix(new_color.rgb, vignetteColor, percent), color.a);
		}
		""";

	public static final String FRAGMENT_SHADER_DOCUMENTARY_ES2
		= String.format(FRAGMENT_SHADER_DOCUMENTARY_BASE_ES2,
			SHADER_VERSION_ES2, HEADER_2D_ES2, SAMPLER_2D_ES2);
	public static final String FRAGMENT_SHADER_EXT_DOCUMENTARY_ES2
		= String.format(FRAGMENT_SHADER_DOCUMENTARY_BASE_ES2,
			SHADER_VERSION_ES2, HEADER_OES_ES2, SAMPLER_OES_ES2);

	private static final String FRAGMENT_SHADER_DOCUMENTARY_BASE_ES3 =
		"""
		%s
		%s
		precision mediump float;
		uniform %s sTexture;
		in vec2 vTextureCoord;
		uniform float uColorAdjust;
		const vec2 seed = vec2(1,50);
		const float step_size = 0.01;
		const vec2 vScale = vec2(1.0, 1.0);
		const vec2 vignetteCenter = vec2(0.5, 0.5);
		const vec3 vignetteColor = vec3(0.0 ,0.0, 0.0);
		const highp vec3 conv = vec3(0.2125, 0.7154, 0.0721);
		layout(location = 0) out vec4 o_FragColor;
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
			vec4 color = texture(sTexture, vTextureCoord);
			float dither = rand(vTextureCoord + seed);
			vec3 xform = clamp(2.0 * color.rgb, 0.0, 1.0);
			vec3 temp = clamp(2.0 * (color.rgb + step_size), 0.0, 1.0);
			vec3 new_color = clamp(xform + (temp - xform) * (dither - 0.5), 0.0, 1.0);
			// grayscale
			highp float intensity = dot(new_color, conv);
			new_color = vec3(intensity, intensity, intensity);
			// vignette
			float d = distance(vTextureCoord, vignetteCenter);
			float percent = smoothstep(0.3, 0.75, d) * uColorAdjust;
			o_FragColor = vec4(mix(new_color.rgb, vignetteColor, percent), color.a);
		}
		""";

	public static final String FRAGMENT_SHADER_DOCUMENTARY_ES3
		= String.format(FRAGMENT_SHADER_DOCUMENTARY_BASE_ES3,
			SHADER_VERSION_ES3, HEADER_2D_ES3, SAMPLER_2D_ES3);
	public static final String FRAGMENT_SHADER_EXT_DOCUMENTARY_ES3
		= String.format(FRAGMENT_SHADER_DOCUMENTARY_BASE_ES2,
			SHADER_VERSION_ES3, HEADER_OES_ES3, SAMPLER_OES_ES3);

//--------------------------------------------------------------------------------
// 露出調整のフラグメントシェーダー, -10〜+10, 0だと無調整
	private static final String FRAGMENT_SHADER_EXPOSURE_BASE_ES2 =
		"""
		%s
		%s
		precision highp float;
		varying vec2 vTextureCoord;
		uniform %s sTexture;
		uniform float uColorAdjust;
		void main() {
		    highp vec4 tex = texture2D(sTexture, vTextureCoord);
		    gl_FragColor = vec4(tex.rgb * pow(2.0, uColorAdjust), tex.w);
		}
		""";
	public static final String FRAGMENT_SHADER_EXPOSURE_ES2
		= String.format(FRAGMENT_SHADER_EXPOSURE_BASE_ES2,
			SHADER_VERSION_ES2, HEADER_2D_ES2, SAMPLER_2D_ES2);
	public static final String FRAGMENT_SHADER_EXT_EXPOSURE_ES2
		= String.format(FRAGMENT_SHADER_EXPOSURE_BASE_ES2,
			SHADER_VERSION_ES2, HEADER_OES_ES2, SAMPLER_OES_ES2);

	private static final String FRAGMENT_SHADER_EXPOSURE_BASE_ES3 =
		"""
		%s
		%s
		precision highp float;
		in vec2 vTextureCoord;
		uniform %s sTexture;
		uniform float uColorAdjust;
		layout(location = 0) out vec4 o_FragColor;
		void main() {
		    highp vec4 tex = texture(sTexture, vTextureCoord);
		    o_FragColor = vec4(tex.rgb * pow(2.0, uColorAdjust), tex.w);
		}
		""";
	public static final String FRAGMENT_SHADER_EXPOSURE_ES3
		= String.format(FRAGMENT_SHADER_EXPOSURE_BASE_ES3,
			SHADER_VERSION_ES3, HEADER_2D_ES3, SAMPLER_2D_ES3);
	public static final String FRAGMENT_SHADER_EXT_EXPOSURE_ES3
		= String.format(FRAGMENT_SHADER_EXPOSURE_BASE_ES3,
			SHADER_VERSION_ES3, HEADER_OES_ES3, SAMPLER_OES_ES3);

//--------------------------------------------------------------------------------
// ネガポジ反転フィルターのフラグメントシェーダー
	private static final String FRAGMENT_SHADER_NEGATIVE_BASE_ES2 =
		"""
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

	public static final String FRAGMENT_SHADER_NEGATIVE_ES2
		= String.format(FRAGMENT_SHADER_NEGATIVE_BASE_ES2,
			SHADER_VERSION_ES2, HEADER_2D_ES2, SAMPLER_2D_ES2);
	public static final String FRAGMENT_SHADER_EXT_NEGATIVE_ES2
		= String.format(FRAGMENT_SHADER_NEGATIVE_BASE_ES2,
			SHADER_VERSION_ES2, HEADER_OES_ES2, SAMPLER_OES_ES2);

	private static final String FRAGMENT_SHADER_NEGATIVE_BASE_ES3 =
		"""
		%s
		%s
		precision mediump float;
		in vec2 vTextureCoord;
		uniform %s sTexture;
		layout(location = 0) out vec4 o_FragColor;
		void main() {
			vec4 color = texture(sTexture, vTextureCoord);
			o_FragColor = vec4(1.0 - color.rgb, color.a);
		}
		""";

	public static final String FRAGMENT_SHADER_NEGATIVE_ES3
		= String.format(FRAGMENT_SHADER_NEGATIVE_BASE_ES3,
			SHADER_VERSION_ES3, HEADER_2D_ES3, SAMPLER_2D_ES3);
	public static final String FRAGMENT_SHADER_EXT_NEGATIVE_ES3
		= String.format(FRAGMENT_SHADER_NEGATIVE_BASE_ES3,
			SHADER_VERSION_ES3, HEADER_OES_ES3, SAMPLER_OES_ES3);

//--------------------------------------------------------------------------------
// ポスタライズフィルターのフラグメントシェーダー
	private static final String FRAGMENT_SHADER_POSTERIZE_BASE_ES2 =
		"""
		%s
		%s
		precision highp float;
		varying vec2 vTextureCoord;
		uniform %s sTexture;
		uniform float uColorAdjust;
		void main() {
			vec4 tex = texture2D(sTexture, vTextureCoord);
			gl_FragColor = floor((tex * uColorAdjust) + vec4(0.5)) / uColorAdjust;
		}
		""";
	public static final String FRAGMENT_SHADER_POSTERIZE_ES2
		= String.format(FRAGMENT_SHADER_POSTERIZE_BASE_ES2,
			SHADER_VERSION_ES2, HEADER_2D_ES2, SAMPLER_2D_ES2);
	public static final String FRAGMENT_SHADER_EXT_POSTERIZE_ES2
		= String.format(FRAGMENT_SHADER_POSTERIZE_BASE_ES2,
			SHADER_VERSION_ES2, HEADER_OES_ES2, SAMPLER_OES_ES2);

	private static final String FRAGMENT_SHADER_POSTERIZE_BASE_ES3 =
		"""
		%s
		%s
		precision highp float;
		in vec2 vTextureCoord;
		uniform %s sTexture;
		uniform float uColorAdjust;
		layout(location = 0) out vec4 o_FragColor;
		void main() {
			vec4 tex = texture(sTexture, vTextureCoord);
			o_FragColor = floor((tex * uColorAdjust) + vec4(0.5)) / uColorAdjust;
		}
		""";
	public static final String FRAGMENT_SHADER_POSTERIZE_ES3
		= String.format(FRAGMENT_SHADER_POSTERIZE_BASE_ES3,
			SHADER_VERSION_ES3, HEADER_2D_ES3, SAMPLER_2D_ES3);
	public static final String FRAGMENT_SHADER_EXT_POSTERIZE_ES3
		= String.format(FRAGMENT_SHADER_POSTERIZE_BASE_ES3,
			SHADER_VERSION_ES3, HEADER_OES_ES3, SAMPLER_OES_ES3);

//--------------------------------------------------------------------------------
// 彩度調整フィルターのフラグメントシェーダー
	private static final String FRAGMENT_SHADER_SATURATE_BASE_ES2 =
		"""
		%s
		%s
		precision highp float;
		varying vec2 vTextureCoord;
		uniform %s sTexture;
		uniform float uColorAdjust;
		const highp vec3 conv = vec3(0.2125, 0.7154, 0.0721);
		void main() {
			highp vec4 tex = texture2D(sTexture, vTextureCoord);
			highp float intensity = dot(tex.rgb, conv);
			highp vec3 greyScaleColor = vec3(intensity, intensity, intensity);
			gl_FragColor = vec4(mix(greyScaleColor, tex.rgb, uColorAdjust), tex.w);
		}
		""";
	public static final String FRAGMENT_SHADER_SATURATE_ES2
		= String.format(FRAGMENT_SHADER_SATURATE_BASE_ES2,
			SHADER_VERSION_ES2, HEADER_2D_ES2, SAMPLER_2D_ES2);
	public static final String FRAGMENT_SHADER_EXT_SATURATE_ES2
		= String.format(FRAGMENT_SHADER_SATURATE_BASE_ES2,
			SHADER_VERSION_ES2, HEADER_OES_ES2, SAMPLER_OES_ES2);

	private static final String FRAGMENT_SHADER_SATURATE_BASE_ES3 =
		"""
		%s
		%s
		precision highp float;
		in vec2 vTextureCoord;
		uniform %s sTexture;
		uniform float uColorAdjust;
		const highp vec3 conv = vec3(0.2125, 0.7154, 0.0721);
		layout(location = 0) out vec4 o_FragColor;
		void main() {
			highp vec4 tex = texture(sTexture, vTextureCoord);
			highp float intensity = dot(tex.rgb, conv);
			highp vec3 greyScaleColor = vec3(intensity, intensity, intensity);
			o_FragColor = vec4(mix(greyScaleColor, tex.rgb, uColorAdjust), tex.w);
		}
		""";
	public static final String FRAGMENT_SHADER_SATURATE_ES3
		= String.format(FRAGMENT_SHADER_SATURATE_BASE_ES3,
			SHADER_VERSION_ES3, HEADER_2D_ES3, SAMPLER_2D_ES3);
	public static final String FRAGMENT_SHADER_EXT_SATURATE_ES3
		= String.format(FRAGMENT_SHADER_SATURATE_BASE_ES3,
			SHADER_VERSION_ES3, HEADER_OES_ES3, SAMPLER_OES_ES3);

//--------------------------------------------------------------------------------
// 球状にレンダリングするフラグメントシェーダー
	private static final String FRAGMENT_SHADER_SPHERE_BASE_ES2 =
		"""
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
			vec3 tex = texture2D(sTexture, (refractedVector.xy + 1.0) * 0.5).rgb;
			gl_FragColor = vec4(tex, 1.0) * checkForPresenceWithinSphere;
		}
		""";

	public static final String FRAGMENT_SHADER_SPHERE_ES2
		= String.format(FRAGMENT_SHADER_SPHERE_BASE_ES2,
			SHADER_VERSION_ES2, HEADER_2D_ES2, SAMPLER_2D_ES2);
	public static final String FRAGMENT_SHADER_EXT_SPHERE_ES2
		= String.format(FRAGMENT_SHADER_SPHERE_BASE_ES2,
			SHADER_VERSION_ES2, HEADER_OES_ES2, SAMPLER_OES_ES2);

	private static final String FRAGMENT_SHADER_SPHERE_BASE_ES3 =
		"""
		%s
		%s
		precision mediump float;
		in vec2 vTextureCoord;
		uniform %s sTexture;
		uniform float uColorAdjust;
		const vec2 center = vec2(0.5, 0.5);
		const float radius = 0.48;	// ちょっとだけ小さくしておく
		const float refractiveIndex = 0.71;
		layout(location = 0) out vec4 o_FragColor;
		void main() {
			vec2 textureCoordinateToUse = vec2(vTextureCoord.x, (vTextureCoord.y * uColorAdjust + 0.5 - 0.5 * uColorAdjust));
			float distanceFromCenter = distance(center, textureCoordinateToUse);
			float checkForPresenceWithinSphere = step(distanceFromCenter, radius);
			distanceFromCenter = distanceFromCenter / radius;
			float normalizedDepth = radius * sqrt(1.0 - distanceFromCenter * distanceFromCenter);
			vec3 sphereNormal = normalize(vec3(textureCoordinateToUse - center, normalizedDepth));
			vec3 refractedVector = 2.0 * refract(vec3(0.0, 0.0, -1.0), sphereNormal, refractiveIndex);
			refractedVector.xy = -refractedVector.xy;
			vec3 tex = texture(sTexture, (refractedVector.xy + 1.0) * 0.5).rgb;
			o_FragColor = vec4(tex, 1.0) * checkForPresenceWithinSphere;
		}
		""";

	public static final String FRAGMENT_SHADER_SPHERE_ES3
		= String.format(FRAGMENT_SHADER_SPHERE_BASE_ES3,
			SHADER_VERSION_ES3, HEADER_2D_ES3, SAMPLER_2D_ES3);
	public static final String FRAGMENT_SHADER_EXT_SPHERE_ES3
		= String.format(FRAGMENT_SHADER_SPHERE_BASE_ES3,
			SHADER_VERSION_ES3, HEADER_OES_ES3, SAMPLER_OES_ES3);

//--------------------------------------------------------------------------------
// ビネットフィルターのフラグメントシェーダー
	private static final String FRAGMENT_SHADER_VIGNETTE_BASE_ES2 =
		"""
		%s
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

	public static final String FRAGMENT_SHADER_VIGNETTE_ES2
		= String.format(FRAGMENT_SHADER_VIGNETTE_BASE_ES2,
			SHADER_VERSION_ES2, HEADER_2D_ES2, SAMPLER_2D_ES2);
	public static final String FRAGMENT_SHADER_EXT_VIGNETTE_ES2
		= String.format(FRAGMENT_SHADER_VIGNETTE_BASE_ES2,
			SHADER_VERSION_ES2, HEADER_OES_ES2, SAMPLER_OES_ES2);

	private static final String FRAGMENT_SHADER_VIGNETTE_BASE_ES3 =
		"""
		%s
		%s
		precision mediump float;
		uniform %s sTexture;
		in vec2 vTextureCoord;
		uniform float uColorAdjust;
		const vec2 vignetteCenter = vec2(0.5, 0.5);
		const vec3 vignetteColor = vec3(0.0 ,0.0, 0.0);
		layout(location = 0) out vec4 o_FragColor;
		void main() {
			vec4 color = texture(sTexture, vTextureCoord);
			float d = distance(vTextureCoord, vignetteCenter);
			float percent = smoothstep(0.3, 0.75, d) * uColorAdjust;
			o_FragColor = vec4(mix(color.rgb, vignetteColor, percent), color.a);
		}
		""";

	public static final String FRAGMENT_SHADER_VIGNETTE_ES3
		= String.format(FRAGMENT_SHADER_VIGNETTE_BASE_ES3,
			SHADER_VERSION_ES3, HEADER_2D_ES3, SAMPLER_2D_ES3);
	public static final String FRAGMENT_SHADER_EXT_VIGNETTE_ES3
		= String.format(FRAGMENT_SHADER_VIGNETTE_BASE_ES3,
			SHADER_VERSION_ES3, HEADER_OES_ES3, SAMPLER_OES_ES3);

//--------------------------------------------------------------------------------
// ブラーフィルターのフラグメントシェーダー
	private static final String FRAGMENT_SHADER_BLUR_BASE_ES2 =
		"""
		%s
		%s
		precision mediump float;
		uniform %s sTexture;
		varying vec2 vTextureCoord;
		uniform float uColorAdjust;	// [-1,+1] → [-0.01,+0.01]
		void main(void) {
			float step = uColorAdjust / 100.0;
			vec3 c1 = texture2D(sTexture, vec2(vTextureCoord.s - step, vTextureCoord.t - step)).bgr;
			vec3 c2 = texture2D(sTexture, vec2(vTextureCoord.s + step, vTextureCoord.t + step)).bgr;
			vec3 c3 = texture2D(sTexture, vec2(vTextureCoord.s - step, vTextureCoord.t + step)).bgr;
			vec3 c4 = texture2D(sTexture, vec2(vTextureCoord.s + step, vTextureCoord.t - step)).bgr;
			gl_FragColor.a = 1.0;
			gl_FragColor.rgb = (c1 + c2 + c3 + c4) / 4.0;
		}
		""";

	public static final String FRAGMENT_SHADER_BLUR_ES2
		= String.format(FRAGMENT_SHADER_BLUR_BASE_ES2,
			SHADER_VERSION_ES2, HEADER_2D_ES2, SAMPLER_2D_ES2);
	public static final String FRAGMENT_SHADER_EXT_BLUR_ES2
		= String.format(FRAGMENT_SHADER_BLUR_BASE_ES2,
			SHADER_VERSION_ES2, HEADER_OES_ES2, SAMPLER_OES_ES2);

	private static final String FRAGMENT_SHADER_BLUR_BASE_ES3 =
		"""
		%s
		%s
		precision mediump float;
		uniform %s sTexture;
		in vec2 vTextureCoord;
		uniform float uColorAdjust;	// [-1,+1] → [-0.01,+0.01]
		layout(location = 0) out vec4 o_FragColor;
		void main(void) {
			float step = uColorAdjust / 100.0;
			vec3 c1 = texture2D(sTexture, vec2(vTextureCoord.s - step, vTextureCoord.t - step)).bgr;
			vec3 c2 = texture2D(sTexture, vec2(vTextureCoord.s + step, vTextureCoord.t + step)).bgr;
			vec3 c3 = texture2D(sTexture, vec2(vTextureCoord.s - step, vTextureCoord.t + step)).bgr;
			vec3 c4 = texture2D(sTexture, vec2(vTextureCoord.s + step, vTextureCoord.t - step)).bgr;
			o_FragColor.a = 1.0;
			o_FragColor.rgb = (c1 + c2 + c3 + c4) / 4.0;
		}
		""";

	public static final String FRAGMENT_SHADER_BLUR_ES3
		= String.format(FRAGMENT_SHADER_BLUR_BASE_ES3,
			SHADER_VERSION_ES3, HEADER_2D_ES3, SAMPLER_2D_ES3);
	public static final String FRAGMENT_SHADER_EXT_BLUR_ES3
		= String.format(FRAGMENT_SHADER_BLUR_BASE_ES3,
			SHADER_VERSION_ES3, HEADER_OES_ES3, SAMPLER_OES_ES3);

//--------------------------------------------------------------------------------
	private static final String FRAGMENT_SHADER_MEDIAN_3x3_BASE_ES2 =
		"""
		%s
		%s
		precision highp float;
		varying vec2 vTextureCoord;
		uniform %s sTexture;
		uniform vec2  uTexOffset[9];
		void main() {
		    vec3 p1 = texture2D(sTexture, vTextureCoord + uTexOffset[0]).rgb;
		    vec3 p2 = texture2D(sTexture, vTextureCoord + uTexOffset[1]).rgb;
		    vec3 p3 = texture2D(sTexture, vTextureCoord + uTexOffset[2]).rgb;
		    vec3 p4 = texture2D(sTexture, vTextureCoord + uTexOffset[3]).rgb;
		    vec3 p5 = texture2D(sTexture, vTextureCoord + uTexOffset[4]).rgb;
		    vec3 p6 = texture2D(sTexture, vTextureCoord + uTexOffset[5]).rgb;
		    vec3 p7 = texture2D(sTexture, vTextureCoord + uTexOffset[6]).rgb;
		    vec3 p8 = texture2D(sTexture, vTextureCoord + uTexOffset[7]).rgb;
		    vec3 p9 = texture2D(sTexture, vTextureCoord + uTexOffset[8]).rgb;

			vec3 op1 = min(p2, p3);
			vec3 op2 = max(p2, p3);
			vec3 op3 = min(p5, p6);
			vec3 op4 = max(p5, p6);
			vec3 op5 = min(p8, p9);
			vec3 op6 = max(p8, p9);
			vec3 op7 = min(p1, op1);
			vec3 op8 = max(p1, op1);
			vec3 op9 = min(p4, op3);
			vec3 op10 = max(p4, op3);
			vec3 op11 = min(p7, op5);
			vec3 op12 = max(p7, op5);
			vec3 op13 = min(op8, op2);
			vec3 op14 = max(op8, op2);
			vec3 op15 = min(op10, op4);
			vec3 op16 = max(op10, op4);
			vec3 op17 = min(op12, op6);
			vec3 op18 = max(op12, op6);
			vec3 op19 = max(op7, op9);
			vec3 op20 = min(op15, op17);
			vec3 op21 = max(op15, op17);
			vec3 op22 = min(op16, op18);
			vec3 op23 = max(op13, op20);
			vec3 op24 = min(op23, op21);
			vec3 op25 = min(op14, op22);
			vec3 op26 = max(op19, op11);
			vec3 op27 = min(op24, op25);
			vec3 op28 = max(op24, op25);
			vec3 op29 = max(op26, op27);
			vec3 op30 = min(op29, op28);
	
		    gl_FragColor = vec4(op30, 1.0);
		}
		""";

	public static final String FRAGMENT_SHADER_MEDIAN_3x3_ES2
		= String.format(FRAGMENT_SHADER_MEDIAN_3x3_BASE_ES2,
		SHADER_VERSION_ES2, HEADER_2D_ES2, SAMPLER_2D_ES2);
	public static final String FRAGMENT_SHADER_EXT_MEDIAN_3x3_ES2
		= String.format(FRAGMENT_SHADER_MEDIAN_3x3_BASE_ES2,
		SHADER_VERSION_ES2, HEADER_OES_ES2, SAMPLER_OES_ES2);

	private static final String FRAGMENT_SHADER_MEDIAN_3x3_BASE_ES3 =
		"""
		%s
		%s
		precision highp float;
		in vec2 vTextureCoord;
		uniform %s sTexture;
		uniform vec2  uTexOffset[9];
		layout(location = 0) out vec4 o_FragColor;
		void main() {
		    vec3 p1 = texture2D(sTexture, vTextureCoord + uTexOffset[0]).rgb;
		    vec3 p2 = texture2D(sTexture, vTextureCoord + uTexOffset[1]).rgb;
		    vec3 p3 = texture2D(sTexture, vTextureCoord + uTexOffset[2]).rgb;
		    vec3 p4 = texture2D(sTexture, vTextureCoord + uTexOffset[3]).rgb;
		    vec3 p5 = texture2D(sTexture, vTextureCoord + uTexOffset[4]).rgb;
		    vec3 p6 = texture2D(sTexture, vTextureCoord + uTexOffset[5]).rgb;
		    vec3 p7 = texture2D(sTexture, vTextureCoord + uTexOffset[6]).rgb;
		    vec3 p8 = texture2D(sTexture, vTextureCoord + uTexOffset[7]).rgb;
		    vec3 p9 = texture2D(sTexture, vTextureCoord + uTexOffset[8]).rgb;

			vec3 op1 = min(p2, p3);
			vec3 op2 = max(p2, p3);
			vec3 op3 = min(p5, p6);
			vec3 op4 = max(p5, p6);
			vec3 op5 = min(p8, p9);
			vec3 op6 = max(p8, p9);
			vec3 op7 = min(p1, op1);
			vec3 op8 = max(p1, op1);
			vec3 op9 = min(p4, op3);
			vec3 op10 = max(p4, op3);
			vec3 op11 = min(p7, op5);
			vec3 op12 = max(p7, op5);
			vec3 op13 = min(op8, op2);
			vec3 op14 = max(op8, op2);
			vec3 op15 = min(op10, op4);
			vec3 op16 = max(op10, op4);
			vec3 op17 = min(op12, op6);
			vec3 op18 = max(op12, op6);
			vec3 op19 = max(op7, op9);
			vec3 op20 = min(op15, op17);
			vec3 op21 = max(op15, op17);
			vec3 op22 = min(op16, op18);
			vec3 op23 = max(op13, op20);
			vec3 op24 = min(op23, op21);
			vec3 op25 = min(op14, op22);
			vec3 op26 = max(op19, op11);
			vec3 op27 = min(op24, op25);
			vec3 op28 = max(op24, op25);
			vec3 op29 = max(op26, op27);
			vec3 op30 = min(op29, op28);
	
		    o_FragColor = vec4(op30, 1.0);
		}
		""";

	public static final String FRAGMENT_SHADER_MEDIAN_3x3_ES3
		= String.format(FRAGMENT_SHADER_MEDIAN_3x3_BASE_ES3,
		SHADER_VERSION_ES3, HEADER_2D_ES3, SAMPLER_2D_ES3);
	public static final String FRAGMENT_SHADER_EXT_MEDIAN_3x3_ES3
		= String.format(FRAGMENT_SHADER_MEDIAN_3x3_BASE_ES3,
		SHADER_VERSION_ES3, HEADER_OES_ES3, SAMPLER_OES_ES3);

//--------------------------------------------------------------------------------
	/**
	 * 3x3カーネル関数によるエッジ検出のためのフラグメントシェーダ
	 * for ES2
	 */
	private static final String FRAGMENT_SHADER_KERNEL3x3_EDGE_DETECT_BASE_ES2 =
		"""
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
		    vec3 sum = t0 * uKernel[0] + t1 * uKernel[1] + t2 * uKernel[2]
		              + t3 * uKernel[3] + t4 * uKernel[4] + t5 * uKernel[5]
		              + t6 * uKernel[6] + t7 * uKernel[7] + t8 * uKernel[8];
		    float mag = length(sum);
		    gl_FragColor = vec4(vec3(mag), 1.0);
		}
		""";

	public static final String FRAGMENT_SHADER_KERNEL3x3_EDGE_DETECT_ES2
		= String.format(FRAGMENT_SHADER_KERNEL3x3_EDGE_DETECT_BASE_ES2,
			SHADER_VERSION_ES2, HEADER_2D_ES2, SAMPLER_2D_ES2);
	public static final String FRAGMENT_SHADER_EXT_KERNEL3x3_EDGE_DETECT_ES2
		= String.format(FRAGMENT_SHADER_KERNEL3x3_EDGE_DETECT_BASE_ES2,
			SHADER_VERSION_ES2, HEADER_OES_ES2, SAMPLER_OES_ES2);

	/**
	 * 3x3カーネル関数によるエッジ検出のためのフラグメントシェーダ
	 * for ES3
	 */
	private static final String FRAGMENT_SHADER_KERNEL3x3_EDGE_DETECT_BASE_ES3 =
		"""
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
		    vec3 sum = t0 * uKernel[0] + t1 * uKernel[1] + t2 * uKernel[2]
		              + t3 * uKernel[3] + t4 * uKernel[4] + t5 * uKernel[5]
		              + t6 * uKernel[6] + t7 * uKernel[7] + t8 * uKernel[8];
		    float mag = length(sum);
		    o_FragColor = vec4(vec3(mag), 1.0);
		}
		""";

	public static final String FRAGMENT_SHADER_KERNEL3x3_EDGE_DETECT_ES3
		= String.format(FRAGMENT_SHADER_KERNEL3x3_EDGE_DETECT_BASE_ES3,
			SHADER_VERSION_ES3, HEADER_2D_ES3, SAMPLER_2D_ES3);
	public static final String FRAGMENT_SHADER_EXT_KERNEL3x3_EDGE_DETECT_ES3
		= String.format(FRAGMENT_SHADER_KERNEL3x3_EDGE_DETECT_BASE_ES3,
			SHADER_VERSION_ES3, HEADER_OES_ES3, SAMPLER_OES_ES3);

//--------------------------------------------------------------------------------
	/**
	 * 3x3カーネル関数によるエッジ検出のためのフラグメントシェーダ(水平＋垂直)
	 * for ES2
	 */
	private static final String FRAGMENT_SHADER_KERNEL3x3_EDGE_DETECT_HV_BASE_ES2 =
		"""
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
		    vec3 sumV = t0 * uKernel[ 9] + t1 * uKernel[10] + t2 * uKernel[11]
		              + t3 * uKernel[12] + t4 * uKernel[13] + t5 * uKernel[14]
		              + t6 * uKernel[15] + t7 * uKernel[16] + t8 * uKernel[17];
		    float mag = length(sumH) + length(sumV);
		    gl_FragColor = vec4(vec3(mag), 1.0);
		}
		""";

	public static final String FRAGMENT_SHADER_KERNEL3x3_EDGE_DETECT_HV_ES2
		= String.format(FRAGMENT_SHADER_KERNEL3x3_EDGE_DETECT_HV_BASE_ES2,
		SHADER_VERSION_ES2, HEADER_2D_ES2, SAMPLER_2D_ES2);
	public static final String FRAGMENT_SHADER_EXT_KERNEL3x3_EDGE_DETECT_HV_ES2
		= String.format(FRAGMENT_SHADER_KERNEL3x3_EDGE_DETECT_HV_BASE_ES2,
		SHADER_VERSION_ES2, HEADER_OES_ES2, SAMPLER_OES_ES2);

	/**
	 * 3x3カーネル関数によるエッジ検出のためのフラグメントシェーダ(水平＋垂直)
	 * for ES3
	 */
	private static final String FRAGMENT_SHADER_KERNEL3x3_EDGE_DETECT_HV_BASE_ES3 =
		"""
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
		    vec3 sumV = t0 * uKernel[ 9] + t1 * uKernel[10] + t2 * uKernel[11]
		              + t3 * uKernel[12] + t4 * uKernel[13] + t5 * uKernel[14]
		              + t6 * uKernel[15] + t7 * uKernel[16] + t8 * uKernel[17];
		    float mag = length(sumH) + length(sumV);
		    o_FragColor = vec4(vec3(mag), 1.0);
		}
		""";

	public static final String FRAGMENT_SHADER_KERNEL3x3_EDGE_DETECT_HV_ES3
		= String.format(FRAGMENT_SHADER_KERNEL3x3_EDGE_DETECT_HV_BASE_ES3,
		SHADER_VERSION_ES3, HEADER_2D_ES3, SAMPLER_2D_ES3);
	public static final String FRAGMENT_SHADER_EXT_KERNEL3x3_EDGE_DETECT_HV_ES3
		= String.format(FRAGMENT_SHADER_KERNEL3x3_EDGE_DETECT_HV_BASE_ES3,
		SHADER_VERSION_ES3, HEADER_OES_ES3, SAMPLER_OES_ES3);

//--------------------------------------------------------------------------------
	/**
	 * カーネル関数による映像効果付与のフラグメントシェーダ
	 * for ES2
	 */
	private static final String FRAGMENT_SHADER_FILT3x3_BASE_ES2 =
		"""
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
		= String.format(FRAGMENT_SHADER_FILT3x3_BASE_ES2,
			SHADER_VERSION_ES2, HEADER_2D_ES2, SAMPLER_2D_ES2);
	public static final String FRAGMENT_SHADER_EXT_FILT3x3_ES2
		= String.format(FRAGMENT_SHADER_FILT3x3_BASE_ES2,
			SHADER_VERSION_ES2, HEADER_OES_ES2, SAMPLER_OES_ES2);

	/**
	 * カーネル関数による映像効果付与のフラグメントシェーダ
	 * for ES3
	 */
	private static final String FRAGMENT_SHADER_FILT3x3_BASE_ES3 =
		"""
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
		= String.format(FRAGMENT_SHADER_FILT3x3_BASE_ES3,
			SHADER_VERSION_ES3, HEADER_2D_ES3, SAMPLER_2D_ES3);
	public static final String FRAGMENT_SHADER_EXT_FILT3x3_ES3
		= String.format(FRAGMENT_SHADER_FILT3x3_BASE_ES3,
			SHADER_VERSION_ES3, HEADER_OES_ES3, SAMPLER_OES_ES3);

//--------------------------------------------------------------------------------
	/**
	 * グレースケール変換のためのフラグメントシェーダーのベース文字列
	 * header(HEADER_OESかHEADER_2D)とサンプラーの種類文字列(SAMPLER_OESかSAMPLER_2D)を渡すこと
	 */
	private static final String FRAGMENT_SHADER_GRAY_BASE_ES2
		= """
		%s
		%s
		precision mediump float;
		varying vec2 vTextureCoord;
		uniform %s sTexture;
		const highp vec3 conv = vec3(0.2125, 0.7154, 0.0721);
		void main() {
		    vec4 tc = texture2D(sTexture, vTextureCoord);
		    highp float intensity = dot(tc.rgb, conv);
		    highp vec3 cl3 = vec3(intensity, intensity, intensity);
		    gl_FragColor = vec4(cl3, 1.0);
		}
		""";

	public static final String FRAGMENT_SHADER_GRAY_ES2
		= String.format(FRAGMENT_SHADER_GRAY_BASE_ES2,
		SHADER_VERSION_ES2, HEADER_2D_ES2, SAMPLER_2D_ES2);
	public static final String FRAGMENT_SHADER_EXT_GRAY_ES2
		= String.format(FRAGMENT_SHADER_GRAY_BASE_ES2,
		SHADER_VERSION_ES2, HEADER_OES_ES2, SAMPLER_OES_ES2);

	/**
	 * グレースケール変換のためのフラグメントシェーダーのベース文字列
	 * header(HEADER_OESかHEADER_2D)とサンプラーの種類文字列(SAMPLER_OESかSAMPLER_2D)を渡すこと
	 */
	private static final String FRAGMENT_SHADER_GRAY_BASE_ES3
		= """
		%s
		%s
		precision mediump float;
		in vec2 vTextureCoord;
		uniform %s sTexture;
		const highp vec3 conv = vec3(0.2125, 0.7154, 0.0721);
		layout(location = 0) out vec4 o_FragColor;
		void main() {
		    vec4 tc = texture(sTexture, vTextureCoord);
		    highp float intensity = dot(tc.rgb, conv);
		    highp vec3 cl3 = vec3(intensity, intensity, intensity);
		    o_FragColor = vec4(cl3, 1.0);
		}
		""";

	public static final String FRAGMENT_SHADER_GRAY_ES3
		= String.format(FRAGMENT_SHADER_GRAY_BASE_ES3,
		SHADER_VERSION_ES3, HEADER_2D_ES3, SAMPLER_2D_ES3);
	public static final String FRAGMENT_SHADER_EXT_GRAY_ES3
		= String.format(FRAGMENT_SHADER_GRAY_BASE_ES3,
		SHADER_VERSION_ES3, HEADER_OES_ES3, SAMPLER_OES_ES3);

//--------------------------------------------------------------------------------
	/**
	 * 白黒反転したグレースケール変換のためのフラグメントシェーダーのベース文字列
	 * header(HEADER_OESかHEADER_2D)とサンプラーの種類文字列(SAMPLER_OESかSAMPLER_2D)を渡すこと
	 */
	private static final String FRAGMENT_SHADER_GRAY_REVERSE_BASE_ES2
		= """
		%s
		%s
		precision mediump float;
		varying vec2 vTextureCoord;
		uniform %s sTexture;
		const highp vec3 conv = vec3(0.2125, 0.7154, 0.0721);
		void main() {
		    vec4 tc = texture2D(sTexture, vTextureCoord);
		    highp float intensity = dot(tc.rgb, conv);
		    highp vec3 cl3 = vec3(intensity, intensity, intensity);
		    gl_FragColor = vec4(clamp(vec3(1.0, 1.0, 1.0) - cl3, 0.0, 1.0), 1.0);
		}
		""";

	public static final String FRAGMENT_SHADER_GRAY_REVERSE_ES2
		= String.format(FRAGMENT_SHADER_GRAY_REVERSE_BASE_ES2,
		SHADER_VERSION_ES2, HEADER_2D_ES2, SAMPLER_2D_ES2);
	public static final String FRAGMENT_SHADER_GRAY_EXT_REVERSE_ES2
		= String.format(FRAGMENT_SHADER_GRAY_REVERSE_BASE_ES2,
		SHADER_VERSION_ES2, HEADER_OES_ES2, SAMPLER_OES_ES2);

	/**
	 * 白黒反転したグレースケール変換のためのフラグメントシェーダーのベース文字列
	 * header(HEADER_OESかHEADER_2D)とサンプラーの種類文字列(SAMPLER_OESかSAMPLER_2D)を渡すこと
	 */
	private static final String FRAGMENT_SHADER_GRAY_REVERSE_BASE_ES3
		= """
		%s
		%s
		precision mediump float;
		in vec2 vTextureCoord;
		uniform %s sTexture;
		const highp vec3 conv = vec3(0.2125, 0.7154, 0.0721);
		layout(location = 0) out vec4 o_FragColor;
		void main() {
		    vec4 tc = texture(sTexture, vTextureCoord);
		    highp float intensity = dot(tc.rgb, conv);
		    highp vec3 cl3 = vec3(intensity, intensity, intensity);
		    o_FragColor = vec4(clamp(vec3(1.0, 1.0, 1.0) - cl3, 0.0, 1.0), 1.0);
		}
		""";

	public static final String FRAGMENT_SHADER_GRAY_REVERSE_ES3
		= String.format(FRAGMENT_SHADER_GRAY_REVERSE_BASE_ES3,
		SHADER_VERSION_ES3, HEADER_2D_ES3, SAMPLER_2D_ES3);
	public static final String FRAGMENT_SHADER_GRAY_EXT_REVERSE_ES3
		= String.format(FRAGMENT_SHADER_GRAY_REVERSE_BASE_ES3,
		SHADER_VERSION_ES3, HEADER_OES_ES3, SAMPLER_OES_ES3);

//--------------------------------------------------------------------------------
	/**
	 * 2値化のためのフラグメントシェーダーのベース文字列
	 * header(HEADER_OESかHEADER_2D)とサンプラーの種類文字列(SAMPLER_OESかSAMPLER_2D)、
	 * 変換後の明るい部分用の色を指定するための文字列(R, G, Bの順)を渡すこと
	 */
	private static final String FRAGMENT_SHADER_BIN_BASE_ES2
		= """
		%s
		%s
		precision mediump float;
		varying vec2 vTextureCoord;
		uniform %s sTexture;
		const highp vec3 conv = vec3(0.2125, 0.7154, 0.0721);
		const vec3 cl = vec3(%s);
		void main() {
		    vec4 tc = texture2D(sTexture, vTextureCoord);
		    highp float intensity = dot(tc.rgb, conv);
		    highp vec3 grayScale = vec3(intensity, intensity, intensity);
		    vec3 bin = step(0.3, grayScale);
		    gl_FragColor = vec4(cl * bin, 1.0);
		}
		""";

	public static final String FRAGMENT_SHADER_BIN_ES2
		= String.format(FRAGMENT_SHADER_BIN_BASE_ES2,
		SHADER_VERSION_ES2, HEADER_2D_ES2, SAMPLER_2D_ES2, "1.0, 1.0, 1.0");
	public static final String FRAGMENT_SHADER_EXT_BIN_ES2
		= String.format(FRAGMENT_SHADER_BIN_BASE_ES2,
		SHADER_VERSION_ES2, HEADER_OES_ES2, SAMPLER_OES_ES2, "1.0, 1.0, 1.0");

	public static final String FRAGMENT_SHADER_BIN_YELLOW_ES2
		= String.format(FRAGMENT_SHADER_BIN_BASE_ES2,
		SHADER_VERSION_ES2, HEADER_2D_ES2, SAMPLER_2D_ES2, "1.0, 1.0, 0.0");
	public static final String FRAGMENT_SHADER_EXT_BIN_YELLOW_ES2
		= String.format(FRAGMENT_SHADER_BIN_BASE_ES2,
		SHADER_VERSION_ES2, HEADER_OES_ES2, SAMPLER_OES_ES2, "1.0, 1.0, 0.0");

	public static final String FRAGMENT_SHADER_BIN_GREEN_ES2
		= String.format(FRAGMENT_SHADER_BIN_BASE_ES2,
		SHADER_VERSION_ES2, HEADER_2D_ES2, SAMPLER_2D_ES2, "0.0, 1.0, 0.0");
	public static final String FRAGMENT_SHADER_EXT_BIN_GREEN_ES2
		= String.format(FRAGMENT_SHADER_BIN_BASE_ES2,
		SHADER_VERSION_ES2, HEADER_OES_ES2, SAMPLER_OES_ES2, "0.0, 1.0, 0.0");

	/**
	 * 2値化のためのフラグメントシェーダーのベース文字列
	 * header(HEADER_OESかHEADER_2D)とサンプラーの種類文字列(SAMPLER_OESかSAMPLER_2D)、
	 * 変換後の明るい部分用の色を指定するための文字列(R, G, Bの順)を渡すこと
	 */
	private static final String FRAGMENT_SHADER_BIN_BASE_ES3
		= """
		%s
		%s
		precision mediump float;
		in vec2 vTextureCoord;
		uniform %s sTexture;
		const highp vec3 conv = vec3(0.2125, 0.7154, 0.0721);
		const vec3 cl = vec3(%s);
		layout(location = 0) out vec4 o_FragColor;
		void main() {
		    vec4 tc = texture(sTexture, vTextureCoord);
		    highp float intensity = dot(tc.rgb, conv);
		    highp vec3 grayScale = vec3(intensity, intensity, intensity);
		    vec3 bin = step(0.3, grayScale);
		    o_FragColor = vec4(cl * bin, 1.0);
		}
		""";

	public static final String FRAGMENT_SHADER_BIN_ES3
		= String.format(FRAGMENT_SHADER_BIN_BASE_ES3,
		SHADER_VERSION_ES3, HEADER_2D_ES3, SAMPLER_2D_ES3, "1.0, 1.0, 1.0");
	public static final String FRAGMENT_SHADER_EXT_BIN_ES3
		= String.format(FRAGMENT_SHADER_BIN_BASE_ES3,
		SHADER_VERSION_ES3, HEADER_OES_ES3, SAMPLER_OES_ES3, "1.0, 1.0, 1.0");

	public static final String FRAGMENT_SHADER_BIN_YELLOW_ES3
		= String.format(FRAGMENT_SHADER_BIN_BASE_ES3,
		SHADER_VERSION_ES3, HEADER_2D_ES3, SAMPLER_2D_ES3, "1.0, 1.0, 0.0");
	public static final String FRAGMENT_SHADER_EXT_BIN_YELLOW_ES3
		= String.format(FRAGMENT_SHADER_BIN_BASE_ES3,
		SHADER_VERSION_ES3, HEADER_OES_ES3, SAMPLER_OES_ES3, "1.0, 1.0, 0.0");

	public static final String FRAGMENT_SHADER_BIN_GREEN_ES3
		= String.format(FRAGMENT_SHADER_BIN_BASE_ES3,
		SHADER_VERSION_ES3, HEADER_2D_ES3, SAMPLER_2D_ES3, "0.0, 1.0, 0.0");
	public static final String FRAGMENT_SHADER_EXT_BIN_GREEN_ES3
		= String.format(FRAGMENT_SHADER_BIN_BASE_ES3,
		SHADER_VERSION_ES3, HEADER_OES_ES3, SAMPLER_OES_ES3, "0.0, 1.0, 0.0");

//--------------------------------------------------------------------------------
	/**
	 * 適応的2値化のためのフラグメントシェーダーのベース文字列
	 * 対象画素と近傍8画素の平均値を閾値とする
	 * XXX 画像内の輝度の差が大きくても二値化できるけど全体的にノイズが多くなってしまう
	 *     平滑化してから適用したり、閾値を平均値の代わりに中央値にしたりもっと広い範囲を
	 *     サンプリングした方がよいかも
	 * header(HEADER_OESかHEADER_2D)とサンプラーの種類文字列(SAMPLER_OESかSAMPLER_2D)、
	 * 変換後の明るい部分用の色を指定するための文字列(R, G, Bの順)を渡すこと
	 */
	private static final String FRAGMENT_SHADER_ADAPTIVE_BIN_BASE_ES2
		= """
		%s
		%s
		precision mediump float;
		varying vec2 vTextureCoord;
		uniform %s sTexture;
		uniform vec2 uTexOffset[9];
		const highp vec3 conv = vec3(0.2125, 0.7154, 0.0721);
		const vec3 cl = vec3(%s);
		void main() {
		    vec4 ave = vec4(0.0);
		    vec4 tc = texture2D(sTexture, vTextureCoord);
		    ave += texture2D(sTexture, vTextureCoord + uTexOffset[0]) / 9.0;
		    ave += texture2D(sTexture, vTextureCoord + uTexOffset[1]) / 9.0;
		    ave += texture2D(sTexture, vTextureCoord + uTexOffset[2]) / 9.0;
		    ave += texture2D(sTexture, vTextureCoord + uTexOffset[3]) / 9.0;
		    ave += texture2D(sTexture, vTextureCoord + uTexOffset[4]) / 9.0;
		    ave += texture2D(sTexture, vTextureCoord + uTexOffset[5]) / 9.0;
		    ave += texture2D(sTexture, vTextureCoord + uTexOffset[6]) / 9.0;
		    ave += texture2D(sTexture, vTextureCoord + uTexOffset[7]) / 9.0;
		    ave += texture2D(sTexture, vTextureCoord + uTexOffset[8]) / 9.0;
		    highp float threshold = dot(ave.rgb, conv);
		    highp float intensity = dot(tc.rgb, conv);
		    vec3 bin = step(threshold, vec3(intensity, intensity, intensity));
		    gl_FragColor = vec4(cl * bin, 1.0);
		}
		""";

	public static final String FRAGMENT_SHADER_ADAPTIVE_BIN_ES2
		= String.format(FRAGMENT_SHADER_ADAPTIVE_BIN_BASE_ES2,
		SHADER_VERSION_ES2, HEADER_2D_ES2, SAMPLER_2D_ES2, "1.0, 1.0, 1.0");
	public static final String FRAGMENT_SHADER_EXT_ADAPTIVE_BIN_ES2
		= String.format(FRAGMENT_SHADER_ADAPTIVE_BIN_BASE_ES2,
		SHADER_VERSION_ES2, HEADER_OES_ES2, SAMPLER_OES_ES2, "1.0, 1.0, 1.0");

	public static final String FRAGMENT_SHADER_ADAPTIVE_BIN_YELLOW_ES2
		= String.format(FRAGMENT_SHADER_ADAPTIVE_BIN_BASE_ES2,
		SHADER_VERSION_ES2, HEADER_2D_ES2, SAMPLER_2D_ES2, "1.0, 1.0, 0.0");
	public static final String FRAGMENT_SHADER_EXT_ADAPTIVE_BIN_YELLOW_ES2
		= String.format(FRAGMENT_SHADER_ADAPTIVE_BIN_BASE_ES2,
		SHADER_VERSION_ES2, HEADER_OES_ES2, SAMPLER_OES_ES2, "1.0, 1.0, 0.0");

	public static final String FRAGMENT_SHADER_ADAPTIVE_BIN_GREEN_ES2
		= String.format(FRAGMENT_SHADER_ADAPTIVE_BIN_BASE_ES2,
		SHADER_VERSION_ES2, HEADER_2D_ES2, SAMPLER_2D_ES2, "0.0, 1.0, 0.0");
	public static final String FRAGMENT_SHADER_EXT_ADAPTIVE_BIN_GREEN_ES2
		= String.format(FRAGMENT_SHADER_ADAPTIVE_BIN_BASE_ES2,
		SHADER_VERSION_ES2, HEADER_OES_ES2, SAMPLER_OES_ES2, "0.0, 1.0, 0.0");

	/**
	 * 適応的2値化のためのフラグメントシェーダーのベース文字列
	 * 対象画素と近傍8画素の平均値を閾値とする
	 * header(HEADER_OESかHEADER_2D)とサンプラーの種類文字列(SAMPLER_OESかSAMPLER_2D)、
	 * 変換後の明るい部分用の色を指定するための文字列(R, G, Bの順)を渡すこと
	 */
	private static final String FRAGMENT_SHADER_ADAPTIVE_BIN_BASE_ES3
		= """
		%s
		%s
		precision mediump float;
		in vec2 vTextureCoord;
		uniform %s sTexture;
		uniform vec2 uTexOffset[9];
		const highp vec3 conv = vec3(0.2125, 0.7154, 0.0721);
		const vec3 cl = vec3(%s);
		layout(location = 0) out vec4 o_FragColor;
		void main() {
		    vec4 ave = vec4(0.0);
		    vec4 tc = texture(sTexture, vTextureCoord);
		    ave += texture(sTexture, vTextureCoord + uTexOffset[0]) / 9.0;
		    ave += texture(sTexture, vTextureCoord + uTexOffset[1]) / 9.0;
		    ave += texture(sTexture, vTextureCoord + uTexOffset[2]) / 9.0;
		    ave += texture(sTexture, vTextureCoord + uTexOffset[3]) / 9.0;
		    ave += texture(sTexture, vTextureCoord + uTexOffset[4]) / 9.0;
		    ave += texture(sTexture, vTextureCoord + uTexOffset[5]) / 9.0;
		    ave += texture(sTexture, vTextureCoord + uTexOffset[6]) / 9.0;
		    ave += texture(sTexture, vTextureCoord + uTexOffset[7]) / 9.0;
		    ave += texture(sTexture, vTextureCoord + uTexOffset[8]) / 9.0;
		    highp float threshold = dot(ave.rgb, conv);
		    highp float intensity = dot(tc.rgb, conv);
		    vec3 bin = step(threshold, vec3(intensity, intensity, intensity));
		    o_FragColor = vec4(cl * bin, 1.0);
		}
		""";

	public static final String FRAGMENT_SHADER_ADAPTIVE_BIN_ES3
		= String.format(FRAGMENT_SHADER_ADAPTIVE_BIN_BASE_ES3,
		SHADER_VERSION_ES3, HEADER_2D_ES3, SAMPLER_2D_ES3, "1.0, 1.0, 1.0");
	public static final String FRAGMENT_SHADER_EXT_ADAPTIVE_BIN_ES3
		= String.format(FRAGMENT_SHADER_ADAPTIVE_BIN_BASE_ES3,
		SHADER_VERSION_ES3, HEADER_OES_ES3, SAMPLER_OES_ES3, "1.0, 1.0, 1.0");

	public static final String FRAGMENT_SHADER_ADAPTIVE_BIN_YELLOW_ES3
		= String.format(FRAGMENT_SHADER_ADAPTIVE_BIN_BASE_ES3,
		SHADER_VERSION_ES3, HEADER_2D_ES3, SAMPLER_2D_ES3, "1.0, 1.0, 0.0");
	public static final String FRAGMENT_SHADER_EXT_ADAPTIVE_BIN_YELLOW_ES3
		= String.format(FRAGMENT_SHADER_ADAPTIVE_BIN_BASE_ES3,
		SHADER_VERSION_ES3, HEADER_OES_ES3, SAMPLER_OES_ES3, "1.0, 1.0, 0.0");

	public static final String FRAGMENT_SHADER_ADAPTIVE_BIN_GREEN_ES3
		= String.format(FRAGMENT_SHADER_ADAPTIVE_BIN_BASE_ES3,
		SHADER_VERSION_ES3, HEADER_2D_ES3, SAMPLER_2D_ES3, "0.0, 1.0, 0.0");
	public static final String FRAGMENT_SHADER_EXT_ADAPTIVE_BIN_GREEN_ES3
		= String.format(FRAGMENT_SHADER_ADAPTIVE_BIN_BASE_ES3,
		SHADER_VERSION_ES3, HEADER_OES_ES3, SAMPLER_OES_ES3, "0.0, 1.0, 0.0");

	//--------------------------------------------------------------------------------
	/**
	 * 反転した2値化のためのフラグメントシェーダーのベース文字列
	 * header(HEADER_OESかHEADER_2D)とサンプラーの種類文字列(SAMPLER_OESかSAMPLER_2D)、
	 * 変換後の明るい部分用の色を指定するための文字列(R, G, Bの順)を渡すこと
	 */
	private static final String FRAGMENT_SHADER_BIN_REVERSE_BASE_ES2
		= """
		%s
		%s
		precision mediump float;
		varying vec2 vTextureCoord;
		uniform %s sTexture;
		const highp vec3 conv = vec3(0.2125, 0.7154, 0.0721);
		const vec3 cl = vec3(%s);
		void main() {
		    vec4 tc = texture2D(sTexture, vTextureCoord);
		    highp float intensity = dot(tc.rgb, conv);
		    highp vec3 grayScale = vec3(intensity, intensity, intensity);
		    vec3 bin = step(0.3, grayScale);
		    gl_FragColor = vec4(cl * (vec3(1.0, 1.0, 1.0) - bin), 1.0);
		}
		""";

	public static final String FRAGMENT_SHADER_BIN_REVERSE_ES2
		= String.format(FRAGMENT_SHADER_BIN_REVERSE_BASE_ES2,
		SHADER_VERSION_ES2, HEADER_2D_ES2, SAMPLER_2D_ES2, "1.0, 1.0, 1.0");
	public static final String FRAGMENT_SHADER_EXT_BIN_REVERSE_ES2
		= String.format(FRAGMENT_SHADER_BIN_REVERSE_BASE_ES2,
		SHADER_VERSION_ES2, HEADER_OES_ES2, SAMPLER_OES_ES2, "1.0, 1.0, 1.0");

	public static final String FRAGMENT_SHADER_BIN_REVERSE_YELLOW_ES2
		= String.format(FRAGMENT_SHADER_BIN_REVERSE_BASE_ES2,
		SHADER_VERSION_ES2, HEADER_2D_ES2, SAMPLER_2D_ES2, "1.0, 1.0, 0.0");
	public static final String FRAGMENT_SHADER_EXT_BIN_REVERSE_YELLOW_ES2
		= String.format(FRAGMENT_SHADER_BIN_REVERSE_BASE_ES2,
		SHADER_VERSION_ES2, HEADER_OES_ES2, SAMPLER_OES_ES2, "1.0, 1.0, 0.0");

	public static final String FRAGMENT_SHADER_BIN_REVERSE_GREEN_ES2
		= String.format(FRAGMENT_SHADER_BIN_REVERSE_BASE_ES2,
		SHADER_VERSION_ES2, HEADER_2D_ES2, SAMPLER_2D_ES2, "0.0, 1.0, 0.0");
	public static final String FRAGMENT_SHADER_EXT_BIN_REVERSE_GREEN_ES2
		= String.format(FRAGMENT_SHADER_BIN_REVERSE_BASE_ES2,
		SHADER_VERSION_ES2, HEADER_OES_ES2, SAMPLER_OES_ES2, "0.0, 1.0, 0.0");

	/**
	 * 反転した2値化のためのフラグメントシェーダーのベース文字列
	 * header(HEADER_OESかHEADER_2D)とサンプラーの種類文字列(SAMPLER_OESかSAMPLER_2D)、
	 * 変換後の明るい部分用の色を指定するための文字列(R, G, Bの順)を渡すこと
	 */
	private static final String FRAGMENT_SHADER_BIN_REVERSE_BASE_ES3
		= """
		%s
		%s
		precision mediump float;
		in vec2 vTextureCoord;
		uniform %s sTexture;
		const highp vec3 conv = vec3(0.2125, 0.7154, 0.0721);
		const vec3 cl = vec3(%s);
		layout(location = 0) out vec4 o_FragColor;
		void main() {
		    vec4 tc = texture(sTexture, vTextureCoord);
		    highp float intensity = dot(tc.rgb, conv);
		    highp vec3 grayScale = vec3(intensity, intensity, intensity);
		    vec3 bin = step(0.3, grayScale);
		    o_FragColor = vec4(cl * (vec3(1.0, 1.0, 1.0) - bin), 1.0);
		}
		""";

	public static final String FRAGMENT_SHADER_BIN_REVERSE_ES3
		= String.format(FRAGMENT_SHADER_BIN_REVERSE_BASE_ES3,
		SHADER_VERSION_ES3, HEADER_2D_ES3, SAMPLER_2D_ES3, "1.0, 1.0, 1.0");
	public static final String FRAGMENT_SHADER_EXT_BIN_REVERSE_ES3
		= String.format(FRAGMENT_SHADER_BIN_REVERSE_BASE_ES3,
		SHADER_VERSION_ES3, HEADER_OES_ES3, SAMPLER_OES_ES3, "1.0, 1.0, 1.0");

	public static final String FRAGMENT_SHADER_BIN_REVERSE_YELLOW_ES3
		= String.format(FRAGMENT_SHADER_BIN_REVERSE_BASE_ES3,
		SHADER_VERSION_ES3, HEADER_2D_ES3, SAMPLER_2D_ES3, "1.0, 1.0, 0.0");
	public static final String FRAGMENT_SHADER_EXT_BIN_REVERSE_YELLOW_ES3
		= String.format(FRAGMENT_SHADER_BIN_REVERSE_BASE_ES3,
		SHADER_VERSION_ES3, HEADER_OES_ES3, SAMPLER_OES_ES3, "1.0, 1.0, 0.0");

	public static final String FRAGMENT_SHADER_BIN_REVERSE_GREEN_ES3
		= String.format(FRAGMENT_SHADER_BIN_REVERSE_BASE_ES3,
		SHADER_VERSION_ES3, HEADER_2D_ES3, SAMPLER_2D_ES3, "0.0, 1.0, 0.0");
	public static final String FRAGMENT_SHADER_EXT_BIN_REVERSE_GREEN_ES3
		= String.format(FRAGMENT_SHADER_BIN_REVERSE_BASE_ES3,
		SHADER_VERSION_ES3, HEADER_OES_ES3, SAMPLER_OES_ES3, "0.0, 1.0, 0.0");

//--------------------------------------------------------------------------------
	/**
	 * 赤と黄色を強調するためのフラグメントシェーダーのベース文字列
	 */
	private static final String FRAGMENT_SHADER_EMPHASIZE_RED_YELLOW_BASE_ES2
		= """
		%s
		%s
		#define MAX_PARAM_NUM 18
		precision mediump float;
		varying vec2 vTextureCoord;
		uniform %s sTexture;
		uniform float uParams[MAX_PARAM_NUM];
		%s
		void main() {
		    vec3 hsv = rgb2hsv(texture2D(sTexture, vTextureCoord).rgb); // RGB=>HSV
		    if (   ((hsv.g >= uParams[2]) && (hsv.g <= uParams[3]))     // s
		        && ((hsv.b >= uParams[4]) && (hsv.b <= uParams[5]))     // v
		        && ((hsv.r <= uParams[0]) || (hsv.r >= uParams[1])) ) { // h
		        hsv = hsv * vec3(uParams[6], uParams[7], uParams[8]);   // red and yellow
		    } else {
		        hsv = hsv * vec3(uParams[9], uParams[10], uParams[11]); // others
		    }
		    gl_FragColor = vec4(hsv2rgb(clamp(hsv, 0.0, 1.0)), 1.0);    // HSV=>RGB
		}
		""";

	public static final String FRAGMENT_SHADER_EMPHASIZE_RED_YELLOWS_ES2
		= String.format(FRAGMENT_SHADER_EMPHASIZE_RED_YELLOW_BASE_ES2,
		SHADER_VERSION_ES2, HEADER_2D_ES2, SAMPLER_2D_ES2,
		HSV_FUNCTIONS);
	public static final String FRAGMENT_SHADER_EXT_EMPHASIZE_RED_YELLOWS_ES2
		= String.format(FRAGMENT_SHADER_EMPHASIZE_RED_YELLOW_BASE_ES2,
		SHADER_VERSION_ES2, HEADER_OES_ES2, SAMPLER_OES_ES2,
		HSV_FUNCTIONS);

	/**
	 * 赤と黄色を強調するためのフラグメントシェーダーのベース文字列
	 */
	private static final String FRAGMENT_SHADER_EMPHASIZE_RED_YELLOW_BASE_ES3
		= """
		%s
		%s
		#define MAX_PARAM_NUM 18
		precision mediump float;
		in vec2 vTextureCoord;
		uniform %s sTexture;
		uniform float uParams[MAX_PARAM_NUM];
		layout(location = 0) out vec4 o_FragColor;
		%s
		void main() {
		    vec3 hsv = rgb2hsv(texture(sTexture, vTextureCoord).rgb);   // RGB=>HSV
		    if (   ((hsv.g >= uParams[2]) && (hsv.g <= uParams[3]))     // s
		        && ((hsv.b >= uParams[4]) && (hsv.b <= uParams[5]))     // v
		        && ((hsv.r <= uParams[0]) || (hsv.r >= uParams[1])) ) { // h
		        hsv = hsv * vec3(uParams[6], uParams[7], uParams[8]);   // red and yellow
		    } else {
		        hsv = hsv * vec3(uParams[9], uParams[10], uParams[11]); // others
		    }
		    o_FragColor = vec4(hsv2rgb(clamp(hsv, 0.0, 1.0)), 1.0);     // HSV=>RGB
		}
		""";

	public static final String FRAGMENT_SHADER_EMPHASIZE_RED_YELLOWS_ES3
		= String.format(FRAGMENT_SHADER_EMPHASIZE_RED_YELLOW_BASE_ES3,
		SHADER_VERSION_ES3, HEADER_2D_ES3, SAMPLER_2D_ES3,
		HSV_FUNCTIONS);
	public static final String FRAGMENT_SHADER_EXT_EMPHASIZE_RED_YELLOWS_ES3
		= String.format(FRAGMENT_SHADER_EMPHASIZE_RED_YELLOW_BASE_ES3,
		SHADER_VERSION_ES3, HEADER_OES_ES3, SAMPLER_OES_ES3,
		HSV_FUNCTIONS);

//--------------------------------------------------------------------------------
	/**
	 * 赤と黄色と白色を強調するためのフラグメントシェーダーのベース文字列
	 */
	private static final String FRAGMENT_SHADER_EMPHASIZE_RED_YELLOW_WHITE_BASE_ES2
		= """
		%s
		%s
		#define MAX_PARAM_NUM 18
		precision mediump float;
		varying vec2 vTextureCoord;
		uniform %s sTexture;
		uniform float uParams[MAX_PARAM_NUM];
		%s
		void main() {
		    vec3 hsv = rgb2hsv(texture2D(sTexture, vTextureCoord).rgb);  // RGB=>HSV
		    if (   ((hsv.g >= uParams[2]) && (hsv.g <= uParams[3]))      // s
		        && ((hsv.b >= uParams[4]) && (hsv.b <= uParams[5]))      // v
		        && ((hsv.r <= uParams[0]) || (hsv.r >= uParams[1])) ) {  // h
		        hsv = hsv * vec3(uParams[6], uParams[7], uParams[8]);    // read and yellow
		    } else if ((hsv.g < uParams[12]) && (hsv.b < uParams[13])) { // 彩度が一定以下, 明度が一定以下なら
		        hsv = hsv * vec3(1.0, 0.0, 2.0);                         // 色相そのまま, 彩度0, 明度x2
		    } else {
		        hsv = hsv * vec3(uParams[9], uParams[10], uParams[11]);  // others
		    }
		    gl_FragColor = vec4(hsv2rgb(clamp(hsv, 0.0, 1.0)), 1.0);     // HSV=>RGB
		}
		""";

	public static final String FRAGMENT_SHADER_EMPHASIZE_RED_YELLOW_WHITE_ES2
		= String.format(FRAGMENT_SHADER_EMPHASIZE_RED_YELLOW_WHITE_BASE_ES2,
		SHADER_VERSION_ES2, HEADER_2D_ES2, SAMPLER_2D_ES2,
		HSV_FUNCTIONS);
	public static final String FRAGMENT_SHADER_EXT_EMPHASIZE_RED_YELLOW_WHITE_ES2
		= String.format(FRAGMENT_SHADER_EMPHASIZE_RED_YELLOW_WHITE_BASE_ES2,
		SHADER_VERSION_ES2, HEADER_OES_ES2, SAMPLER_OES_ES2,
		HSV_FUNCTIONS);

	/**
	 * 赤と黄色と白色を強調するためのフラグメントシェーダーのベース文字列
	 */
	private static final String FRAGMENT_SHADER_EMPHASIZE_RED_YELLOW_WHITE_BASE_ES3
		= """
		%s
		%s
		#define MAX_PARAM_NUM 18
		precision mediump float;
		in vec2 vTextureCoord;
		uniform %s sTexture;
		uniform float uParams[MAX_PARAM_NUM];
		layout(location = 0) out vec4 o_FragColor;
		%s
		void main() {
		    vec3 hsv = rgb2hsv(texture(sTexture, vTextureCoord).rgb);    // RGB=>HSV
		    if (   ((hsv.g >= uParams[2]) && (hsv.g <= uParams[3]))      // s
		        && ((hsv.b >= uParams[4]) && (hsv.b <= uParams[5]))      // v
		        && ((hsv.r <= uParams[0]) || (hsv.r >= uParams[1])) ) {  // h
		        hsv = hsv * vec3(uParams[6], uParams[7], uParams[8]);    // red and yellow
		    } else if ((hsv.g < uParams[12]) && (hsv.b < uParams[13])) { // 彩度が一定以下, 明度が一定以下なら
		        hsv = hsv * vec3(1.0, 0.0, 2.0);                         // 色相そのまま, 彩度0, 明度x2
		    } else {
		        hsv = hsv * vec3(uParams[9], uParams[10], uParams[11]);  // others
		    }
		    o_FragColor = vec4(hsv2rgb(clamp(hsv, 0.0, 1.0)), 1.0);      // HSV=>RGB
		}
		""";

	public static final String FRAGMENT_SHADER_EMPHASIZE_RED_YELLOW_WHITE_ES3
		= String.format(FRAGMENT_SHADER_EMPHASIZE_RED_YELLOW_WHITE_BASE_ES3,
		SHADER_VERSION_ES3, HEADER_2D_ES3, SAMPLER_2D_ES3,
		HSV_FUNCTIONS);
	public static final String FRAGMENT_SHADER_EXT_EMPHASIZE_RED_YELLOW_WHITE_ES3
		= String.format(FRAGMENT_SHADER_EMPHASIZE_RED_YELLOW_WHITE_BASE_ES3,
		SHADER_VERSION_ES3, HEADER_OES_ES3, SAMPLER_OES_ES3,
		HSV_FUNCTIONS);

//--------------------------------------------------------------------------------
	/**
	 * 黄色と白を強調するためのフラグメントシェーダーのベース文字列
	 * 今はFRAGMENT_SHADER_EMPHASIZE_RED_YELLOW_WHITE_BASEと同じ(違うパラメータ渡せば良いだけなので)
	 */
	private static final String FRAGMENT_SHADER_EMPHASIZE_YELLOW_WHITE_BASE_ES2
		= """
		%s
		%s
		#define MAX_PARAM_NUM 18
		precision mediump float;
		varying vec2 vTextureCoord;
		uniform %s sTexture;
		uniform float uParams[MAX_PARAM_NUM];
		%s
		void main() {
		    vec3 hsv = rgb2hsv(texture2D(sTexture, vTextureCoord).rgb);  // RGB=>HSV
		    if (   ((hsv.r >= uParams[0]) && (hsv.r <= uParams[1]))      // h
		        && ((hsv.g >= uParams[2]) && (hsv.g <= uParams[3]))      // s
		        && ((hsv.b >= uParams[4]) && (hsv.b <= uParams[5])) ) {  // v
		        hsv = hsv * vec3(uParams[6], uParams[7], uParams[8]);    // yellow
		    } else if ((hsv.g < uParams[12]) && (hsv.b > uParams[13])) { // 彩度が一定以下, 明度が一定以上なら
		        hsv = hsv * vec3(1.0, 0.0, 2.0);                         // 色相そのまま, 彩度0, 明度x2
		    } else {
		        hsv = hsv * vec3(uParams[9], uParams[10], uParams[11]);  // others
		    }
		    gl_FragColor = vec4(hsv2rgb(clamp(hsv, 0.0, 1.0)), 1.0);     // HSV=>RGB
		}
		""";

	public static final String FRAGMENT_SHADER_EMPHASIZE_YELLOW_WHITE_ES2
		= String.format(FRAGMENT_SHADER_EMPHASIZE_YELLOW_WHITE_BASE_ES2,
		SHADER_VERSION_ES2, HEADER_2D_ES2, SAMPLER_2D_ES2,
		HSV_FUNCTIONS);
	public static final String FRAGMENT_SHADER_EXT_EMPHASIZE_YELLOW_WHITE_ES2
		= String.format(FRAGMENT_SHADER_EMPHASIZE_YELLOW_WHITE_BASE_ES2,
		SHADER_VERSION_ES2, HEADER_OES_ES2, SAMPLER_OES_ES2,
		HSV_FUNCTIONS);

	/**
	 * 黄色と白を強調するためのフラグメントシェーダーのベース文字列
	 * 今はFRAGMENT_SHADER_EMPHASIZE_RED_YELLOW_WHITE_BASEと同じ(違うパラメータ渡せば良いだけなので)
	 */
	private static final String FRAGMENT_SHADER_EMPHASIZE_YELLOW_WHITE_BASE_ES3
		= """
		%s
		%s
		#define MAX_PARAM_NUM 18
		precision mediump float;
		in vec2 vTextureCoord;
		uniform %s sTexture;
		uniform float uParams[MAX_PARAM_NUM];
		layout(location = 0) out vec4 o_FragColor;
		%s
		void main() {
		    vec3 hsv = rgb2hsv(texture(sTexture, vTextureCoord).rgb);    // RGBをHSVに変換
		    if (   ((hsv.r >= uParams[0]) && (hsv.r <= uParams[1]))      // h
		        && ((hsv.g >= uParams[2]) && (hsv.g <= uParams[3]))      // s
		        && ((hsv.b >= uParams[4]) && (hsv.b <= uParams[5])) ) {  // v
		        hsv = hsv * vec3(uParams[6], uParams[7], uParams[8]);    // yellow
		    } else if ((hsv.g < uParams[12]) && (hsv.b > uParams[13])) { // 彩度が一定以下, 明度が一定以上なら
		        hsv = hsv * vec3(1.0, 0.0, 2.0);                         // 色相そのまま, 彩度0, 明度x2
		    } else {
		        hsv = hsv * vec3(uParams[9], uParams[10], uParams[11]);  // others
		    }
		    o_FragColor = vec4(hsv2rgb(clamp(hsv, 0.0, 1.0)), 1.0);      // HSV=>RGB
		}
		""";

	public static final String FRAGMENT_SHADER_EMPHASIZE_YELLOW_WHITE_ES3
		= String.format(FRAGMENT_SHADER_EMPHASIZE_YELLOW_WHITE_BASE_ES3,
		SHADER_VERSION_ES3, HEADER_2D_ES3, SAMPLER_2D_ES3,
		HSV_FUNCTIONS);
	public static final String FRAGMENT_SHADER_EXT_EMPHASIZE_YELLOW_WHITE_ES3
		= String.format(FRAGMENT_SHADER_EMPHASIZE_YELLOW_WHITE_BASE_ES3,
		SHADER_VERSION_ES3, HEADER_OES_ES3, SAMPLER_OES_ES3,
		HSV_FUNCTIONS);

	private static final String FRAGMENT_SHADER_CANNY_BASE_ES2 =
		"""
		%s
		%s
		#define KERNEL_SIZE3x3 %s
		precision highp float;
		varying       vec2 vTextureCoord;
		uniform %s    sTexture;
		uniform float uKernel[18];
		uniform vec2  uTexOffset[KERNEL_SIZE3x3];
		uniform float uColorAdjust;
		const float lowerThreshold = 0.4;	// lowerとupperの値を入れ替えると白黒反転する
		const float upperThreshold = 0.8;
		void main() {
			vec4 magdir = texture2D(sTexture, vTextureCoord);
			vec2 offset = ((magdir.gb * 2.0) - 1.0) * uTexOffset[8];
			float first = texture2D(sTexture, vTextureCoord + offset).r;
			float second = texture2D(sTexture, vTextureCoord - offset).r;
			float multiplier = step(first, magdir.r);
			multiplier = multiplier * step(second, magdir.r);
			float threshold = smoothstep(lowerThreshold, upperThreshold, magdir.r);
			multiplier = multiplier * threshold;
			gl_FragColor = vec4(multiplier, multiplier, multiplier, 1.0);
		}
		""";
	//----
//		"const float threshold = 0.2;\n" +
//		"const vec2 unshift = vec2(1.0 / 256.0, 1.0);\n" +
//		"const float atan0   = 0.414213;\n" +
//		"const float atan45  = 2.414213;\n" +
//		"const float atan90  = -2.414213;\n" +
//		"const float atan135 = -0.414213;\n" +
//		"vec2 atanForCanny(float x) {\n" +
//		"    if (x < atan0 && x > atan135) {\n" +
//		"        return vec2(1.0, 0.0);\n" +
//		"    }\n" +
//		"    if (x < atan90 && x > atan45) {\n" +
//		"        return vec2(0.0, 1.0);\n" +
//		"    }\n" +
//		"    if (x > atan135 && x < atan90) {\n" +
//		"        return vec2(-1.0, 1.0);\n" +
//		"    }\n" +
//		"    return vec2(1.0, 1.0);\n" +
//		"}\n" +
//		"vec4 cannyEdge(vec2 coords) {\n" +
//		"    vec4 color = texture2D(sTexture, coords);\n" +
//		"    color.z = dot(color.zw, unshift);\n" +
//		"    if (color.z > threshold) {\n" +
//		"        color.x -= 0.5;\n" +
//		"        color.y -= 0.5;\n" +
//		"        vec2 offset = atanForCanny(color.y / color.x);\n" +
//		"        offset.x *= uTexOffset[7];\n" +
//		"        offset.y *= uTexOffset[8];\n" +
//		"        vec4 forward  = texture2D(sTexture, coords + offset);\n" +
//		"        vec4 backward = texture2D(sTexture, coords - offset);\n" +
//		"        forward.z  = dot(forward.zw, unshift);\n" +
//		"        backward.z = dot(backward.zw, unshift);\n" +
//		"        if (forward.z >= color.z ||\n" +
//		"            backward.z >= color.z) {\n" +
//		"            return vec4(0.0, 0.0, 0.0, 1.0);\n" +
//		"        } else {\n" +
//		"            color.x += 0.5; color.y += 0.5;\n" +
//		"            return vec4(1.0, color.x, color.y, 1.0);\n" +
//		"        }\n" +
//		"    }\n" +
//		"    return vec4(0.0, 0.0, 0.0, 1.0);\n" +
//		"}\n" +
//		"void main() {\n" +
//		"    gl_FragColor = cannyEdge(vTextureCoord);\n" +
//		"}\n";
	public static final String FRAGMENT_SHADER_CANNY_ES2
		= String.format(FRAGMENT_SHADER_CANNY_BASE_ES2,
		SHADER_VERSION_ES2, HEADER_2D_ES2, KERNEL_SIZE3x3, SAMPLER_2D_ES2);
	public static final String FRAGMENT_SHADER_EXT_CANNY_ES2
		= String.format(FRAGMENT_SHADER_CANNY_BASE_ES2,
		SHADER_VERSION_ES2, HEADER_OES_ES2, KERNEL_SIZE3x3, SAMPLER_OES_ES2);

	private static final String FRAGMENT_SHADER_CANNY_BASE_ES3 =
		"""
		%s
		%s
		#define KERNEL_SIZE3x3 %s
		precision highp float;
		in vec2 vTextureCoord;
		uniform %s    sTexture;
		uniform float uKernel[18];
		uniform vec2  uTexOffset[KERNEL_SIZE3x3];
		uniform float uColorAdjust;
		const float lowerThreshold = 0.4;	// lowerとupperの値を入れ替えると白黒反転する
		const float upperThreshold = 0.8;
		layout(location = 0) out vec4 o_FragColor;
		void main() {
			vec4 magdir = texture(sTexture, vTextureCoord);
			vec2 offset = ((magdir.gb * 2.0) - 1.0) * uTexOffset[8];
			float first = texture(sTexture, vTextureCoord + offset).r;
			float second = texture(sTexture, vTextureCoord - offset).r;
			float multiplier = step(first, magdir.r);
			multiplier = multiplier * step(second, magdir.r);
			float threshold = smoothstep(lowerThreshold, upperThreshold, magdir.r);
			multiplier = multiplier * threshold;
			o_FragColor = vec4(multiplier, multiplier, multiplier, 1.0);
		}
		""";
	//----
//		"const float threshold = 0.2;\n" +
//		"const vec2 unshift = vec2(1.0 / 256.0, 1.0);\n" +
//		"const float atan0   = 0.414213;\n" +
//		"const float atan45  = 2.414213;\n" +
//		"const float atan90  = -2.414213;\n" +
//		"const float atan135 = -0.414213;\n" +
//		"vec2 atanForCanny(float x) {\n" +
//		"    if (x < atan0 && x > atan135) {\n" +
//		"        return vec2(1.0, 0.0);\n" +
//		"    }\n" +
//		"    if (x < atan90 && x > atan45) {\n" +
//		"        return vec2(0.0, 1.0);\n" +
//		"    }\n" +
//		"    if (x > atan135 && x < atan90) {\n" +
//		"        return vec2(-1.0, 1.0);\n" +
//		"    }\n" +
//		"    return vec2(1.0, 1.0);\n" +
//		"}\n" +
//		"vec4 cannyEdge(vec2 coords) {\n" +
//		"    vec4 color = texture(sTexture, coords);\n" +
//		"    color.z = dot(color.zw, unshift);\n" +
//		"    if (color.z > threshold) {\n" +
//		"        color.x -= 0.5;\n" +
//		"        color.y -= 0.5;\n" +
//		"        vec2 offset = atanForCanny(color.y / color.x);\n" +
//		"        offset.x *= uTexOffset[7];\n" +
//		"        offset.y *= uTexOffset[8];\n" +
//		"        vec4 forward  = texture(sTexture, coords + offset);\n" +
//		"        vec4 backward = texture(sTexture, coords - offset);\n" +
//		"        forward.z  = dot(forward.zw, unshift);\n" +
//		"        backward.z = dot(backward.zw, unshift);\n" +
//		"        if (forward.z >= color.z ||\n" +
//		"            backward.z >= color.z) {\n" +
//		"            return vec4(0.0, 0.0, 0.0, 1.0);\n" +
//		"        } else {\n" +
//		"            color.x += 0.5; color.y += 0.5;\n" +
//		"            return vec4(1.0, color.x, color.y, 1.0);\n" +
//		"        }\n" +
//		"    }\n" +
//		"    return vec4(0.0, 0.0, 0.0, 1.0);\n" +
//		"}\n" +
//		"void main() {\n" +
//		"    o_FragColor = cannyEdge(vTextureCoord);\n" +
//		"}\n";
	public static final String FRAGMENT_SHADER_CANNY_ES3
		= String.format(FRAGMENT_SHADER_CANNY_BASE_ES3,
		SHADER_VERSION_ES3, HEADER_2D_ES3, KERNEL_SIZE3x3, SAMPLER_2D_ES3);
	public static final String FRAGMENT_SHADER_EXT_CANNY_ES3
		= String.format(FRAGMENT_SHADER_CANNY_BASE_ES3,
		SHADER_VERSION_ES3, HEADER_OES_ES3, KERNEL_SIZE3x3, SAMPLER_OES_ES3);

//--------------------------------------------------------------------------------
// Cannyエッジ検出の結果を元画像へ適用してみる試み

	private static final String FRAGMENT_SHADER_CANNY_ENHANCE_BASE_ES2 =
		"""
		%s
		%s
		#define KERNEL_SIZE3x3 %s
		precision highp float;
		varying       vec2 vTextureCoord;
		uniform %s    sTexture;
		uniform float uKernel[18];
		uniform vec2  uTexOffset[KERNEL_SIZE3x3];
		uniform float uColorAdjust;
		const float contrast = 1.2;
		const float lowerThreshold = 0.4;	// lowerとupperの値を入れ替えると白黒反転する
		const float upperThreshold = 0.8;
		void main() {
			vec4 magdir = texture2D(sTexture, vTextureCoord);
			vec2 offset = ((magdir.gb * 2.0) - 1.0) * uTexOffset[8];
			float first = texture2D(sTexture, vTextureCoord + offset).r;
			float second = texture2D(sTexture, vTextureCoord - offset).r;
			float multiplier = step(first, magdir.r);
			multiplier = multiplier * step(second, magdir.r);
			float threshold = smoothstep(lowerThreshold, upperThreshold, magdir.r);
			multiplier = multiplier * threshold * uColorAdjust;
			// FIXME 検出したエッジ部分の適用方法は要検討
		#if 1
			// increase brightness on detected edge
			magdir.rgb += multiplier;
		#elif 1
			// increase contrast
			magdir.rgb = ((magdir.rgb - 0.5) * max(multiplier + contrast, 0.0)) + 0.5;
		#elif 1
			// increase brightness on detected edge
			magdir.rgb += multiplier;
			// increase contrast
			magdir.rgb = ((magdir.rgb - 0.5) * max(contrast, 0.0)) + 0.5;
		#endif
			gl_FragColor = magdir;
		}
		""";
	public static final String FRAGMENT_SHADER_CANNY_ENHANCE_ES2
		= String.format(FRAGMENT_SHADER_CANNY_ENHANCE_BASE_ES2,
		SHADER_VERSION_ES2, HEADER_2D_ES2, KERNEL_SIZE3x3, SAMPLER_2D_ES2);
	public static final String FRAGMENT_SHADER_EXT_CANNY_ENHANCE_ES2
		= String.format(FRAGMENT_SHADER_CANNY_ENHANCE_BASE_ES2,
		SHADER_VERSION_ES2, HEADER_OES_ES2, KERNEL_SIZE3x3, SAMPLER_OES_ES2);

	private static final String FRAGMENT_SHADER_CANNY_ENHANCE_BASE_ES3 =
		"""
		%s
		%s
		#define KERNEL_SIZE3x3 %s
		precision highp float;
		in vec2 vTextureCoord;
		uniform %s    sTexture;
		uniform float uKernel[18];
		uniform vec2  uTexOffset[KERNEL_SIZE3x3];
		uniform float uColorAdjust;
		const float contrast = 1.2;
		const float lowerThreshold = 0.4;	// lowerとupperの値を入れ替えると白黒反転する
		const float upperThreshold = 0.8;
		layout(location = 0) out vec4 o_FragColor;
		void main() {
			vec4 magdir = texture(sTexture, vTextureCoord);
			vec2 offset = ((magdir.gb * 2.0) - 1.0) * uTexOffset[8];
			float first = texture(sTexture, vTextureCoord + offset).r;
			float second = texture(sTexture, vTextureCoord - offset).r;
			float multiplier = step(first, magdir.r);
			multiplier = multiplier * step(second, magdir.r);
			float threshold = smoothstep(lowerThreshold, upperThreshold, magdir.r);
			multiplier = multiplier * threshold * uColorAdjust;
			// FIXME 検出したエッジ部分の適用方法は要検討
		#if 1
			// increase brightness on detected edge
			magdir.rgb += multiplier;
		#elif 1
			// increase contrast
			magdir.rgb = ((magdir.rgb - 0.5) * max(multiplier + contrast, 0.0)) + 0.5;
		#elif 1
			// increase brightness on detected edge
			magdir.rgb += multiplier;
			// increase contrast
			magdir.rgb = ((magdir.rgb - 0.5) * max(contrast, 0.0)) + 0.5;
		#endif
			o_FragColor = magdir;
		}
		""";
	public static final String FRAGMENT_SHADER_CANNY_ENHANCE_ES3
		= String.format(FRAGMENT_SHADER_CANNY_ENHANCE_BASE_ES3,
		SHADER_VERSION_ES3, HEADER_2D_ES3, KERNEL_SIZE3x3, SAMPLER_2D_ES3);
	public static final String FRAGMENT_SHADER_EXT_CANNY_ENHANCE_ES3
		= String.format(FRAGMENT_SHADER_CANNY_ENHANCE_BASE_ES3,
		SHADER_VERSION_ES3, HEADER_OES_ES3, KERNEL_SIZE3x3, SAMPLER_OES_ES3);

//--------------------------------------------------------------------------------
// カーネル関数によるフィルタ処理を元映像へ適用してみる試み
// カーネル関数としてはエッジ検出処理を想定

	private static final String FRAGMENT_SHADER_KERNEL_ENHANCE_BASE_ES2 =
		"""
		%s
		%s
		#define KERNEL_SIZE3x3 9
		precision highp float;
		varying vec2 vTextureCoord;
		uniform %s sTexture;
		uniform float uKernel[18];
		uniform vec2 uTexOffset[KERNEL_SIZE3x3];
		uniform float uColorAdjust;
		const float contrast = 1.2;
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
		    vec4 color = texture2D(sTexture, vTextureCoord);
		#if 1
			// increase brightness on detected edge
			color.rgb += (sum.rgb * uColorAdjust);
		#elif 1
			// increase contrast
//			color.rgb = ((color.rgb - 0.5) * max(contrast, 0.0)) + 0.5;
			color.rgb = ((color.rgb - 0.5) * (max(sum.rgb, 0.0) * uColorAdjust + 1.0)) + 0.5;
		#elsif
			// increase brightness on detected edge
			color.rgb += (sum.rgb * uColorAdjust);
			color.rgb = ((color.rgb - 0.5) * (max(sum.rgb, 0.0) * uColorAdjust + 1.0)) + 0.5;
		#endif
			gl_FragColor = color;
		}
		""";
	public static final String FRAGMENT_SHADER_KERNEL_ENHANCE_ES2
		= String.format(FRAGMENT_SHADER_KERNEL_ENHANCE_BASE_ES2,
		SHADER_VERSION_ES2, HEADER_2D_ES2, SAMPLER_2D_ES2);
	public static final String FRAGMENT_SHADER_EXT_KERNEL_ENHANCE_ES2
		= String.format(FRAGMENT_SHADER_KERNEL_ENHANCE_BASE_ES2,
		SHADER_VERSION_ES2, HEADER_OES_ES2, SAMPLER_OES_ES2);

	private static final String FRAGMENT_SHADER_KERNEL_ENHANCE_BASE_ES3 =
		"""
		%s
		%s
		#define KERNEL_SIZE3x3 9
		precision highp float;
		in vec2 vTextureCoord;
		uniform %s sTexture;
		uniform float uKernel[18];
		uniform vec2 uTexOffset[KERNEL_SIZE3x3];
		uniform float uColorAdjust;
		const float contrast = 1.2;
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
		    vec4 color = texture(sTexture, vTextureCoord);
		#if 1
			// increase brightness on detected edge
			color.rgb += (sum.rgb * uColorAdjust);
		#elif 1
			// increase contrast
//			color.rgb = ((color.rgb - 0.5) * max(contrast, 0.0)) + 0.5;
			color.rgb = ((color.rgb - 0.5) * (max(sum.rgb, 0.0) * uColorAdjust + 1.0)) + 0.5;
		#elsif
			// increase brightness on detected edge
			color.rgb += (sum.rgb * uColorAdjust);
			color.rgb = ((color.rgb - 0.5) * (max(sum.rgb, 0.0) * uColorAdjust + 1.0)) + 0.5;
		#endif
			o_FragColor = color;
		}
		""";
	public static final String FRAGMENT_SHADER_KERNEL_ENHANCE_ES3
		= String.format(FRAGMENT_SHADER_KERNEL_ENHANCE_BASE_ES3,
		SHADER_VERSION_ES3, HEADER_2D_ES3, SAMPLER_2D_ES3);
	public static final String FRAGMENT_SHADER_EXT_KERNEL_ENHANCE_ES3
		= String.format(FRAGMENT_SHADER_KERNEL_ENHANCE_BASE_ES3,
		SHADER_VERSION_ES3, HEADER_OES_ES3, SAMPLER_OES_ES3);
}
