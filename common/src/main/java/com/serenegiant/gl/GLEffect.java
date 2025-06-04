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

import static com.serenegiant.gl.ShaderConst.*;

/**
 * OpenGL|ESを使った映像効果付与のためのシェーダーを定義する定数クラス
 */
public class GLEffect {
	private GLEffect() {
		// インスタンス化を防ぐためにデフォルトコンストラクタをprivateに
	}

	public static final int EFFECT_NON = 0;
	public static final int EFFECT_GRAY = 1;
	public static final int EFFECT_GRAY_REVERSE = 2;
	public static final int EFFECT_BIN = 3;
	public static final int EFFECT_BIN_YELLOW = 4;
	public static final int EFFECT_BIN_GREEN = 5;
	public static final int EFFECT_BIN_REVERSE = 6;
	public static final int EFFECT_BIN_REVERSE_YELLOW = 7;
	public static final int EFFECT_BIN_REVERSE_GREEN = 8;
	/**
	 * 赤色黄色を強調
	 * setParamsはfloat[12] {
	 *    0.17f, 0.85f,		// 赤色&黄色の色相下側閾値, 上側閾値
	 *    0.50f, 1.0f,		// 強調する彩度下限, 上限
	 *    0.40f, 1.0f,		// 強調する明度下限, 上限
	 *    1.0f, 1.0f, 5.0f,	// 強調時のファクター(H, S, Vの順) 明度(x5.0) = 1.0
	 *    1.0f, 0.5f, 0.8f,	// 通常時のファクター(H, S, Vの順) 彩度(x0.5)と明度(x0.8)を少し落とす
	 * }
	 */
	public static final int EFFECT_EMPHASIZE_RED_YELLOW = 9;
	/**
	 * 赤色黄色と白を強調
	 * setParamsはfloat[12] {
	 *    0.17f, 0.85f,		// 赤色&黄色の色相下側閾値, 上側閾値
	 *    0.50f, 1.0f,		// 強調する彩度下限, 上限
	 *    0.40f, 1.0f,		// 強調する明度下限, 上限
	 *    1.0f, 1.0f, 5.0f,	// 強調時のファクター(H, S, Vの順) 明度(x5.0) = 1.0
	 *    1.0f, 0.5f, 0.8f,	// 通常時のファクター(H, S, Vの順) 彩度(x0.5)と明度(x0.8)を少し落とす
	 * 白のパラメータは今はなし
	 */
	public static final int EFFECT_EMPHASIZE_RED_YELLOW_WHITE = 10;
	/**
	 * 黄色と白を強調
	 * setParamsはfloat[12] {
	 *    0.17f, 0.85f,		// 赤色&黄色の色相下側閾値, 上側閾値 FIXME 未調整
	 *    0.50f, 1.0f,		// 強調する彩度下限, 上限
	 *    0.40f, 1.0f,		// 強調する明度下限, 上限
	 *    1.0f, 1.0f, 5.0f,	// 強調時のファクター(H, S, Vの順) 明度(x5.0) = 1.0
	 *    1.0f, 0.5f, 0.8f,	// 通常時のファクター(H, S, Vの順) 彩度(x0.5)と明度(x0.8)を少し落とす
	 * 白のパラメータは今はなし
	 */
	public static final int EFFECT_EMPHASIZE_YELLOW_WHITE = 11;
	/** 内蔵映像効果の数 */
	public static final int EFFECT_NUM = 12;

	/**
	 * ソーベルフィルタ(水平方向)
	 */
	public static final int EFFECT_KERNEL_SOBEL_H = 1000;
	/**
	 * ソーベルフィルタ(垂直方向)
	 */
	public static final int EFFECT_KERNEL_SOBEL_V = 1001;
	/**
	 * ソーベルフィルタ2(水平方向)
	 */
	public static final int EFFECT_KERNEL_SOBEL2_H = 1002;
	/**
	 * ソーベルフィルタ2(垂直方向)
	 */
	public static final int EFFECT_KERNEL_SOBEL2_V = 1003;
	/**
	 * エッジ強調4近傍(=シャープネス)
	 */
	public static final int EFFECT_KERNEL_EDGE_ENHANCE4 = 1004;
	/**
	 * エッジ強調9近傍
	 */
	public static final int EFFECT_KERNEL_EDGE_ENHANCE8 = 1005;
	/**
	 * シャープネス
	 */
	public static final int EFFECT_KERNEL_SHARPNESS = EFFECT_KERNEL_EDGE_ENHANCE4;
	/**
	 * エッジ検出
	 */
	public static final int EFFECT_KERNEL_EDGE_DETECT = 1006;
	/**
	 * エンボス
	 */
	public static final int EFFECT_KERNEL_EMBOSS = 1007;
	/**
	 * 平滑化
	 */
	public static final int EFFECT_KERNEL_SMOOTH = 1008;
	/**
	 * ガウシアンフィルタ
	 */
	public static final int EFFECT_KERNEL_GAUSSIAN = 1009;
	/**
	 * 輝度アップ
	 */
	public static final int EFFECT_KERNEL_BRIGHTEN = 1010;
	/**
	 * ラプラシアンフィルタ(8近傍)
	 */
	public static final int EFFECT_KERNEL_LAPLACIAN8 = 1011;
	/**
	 * ラプラシアンフィルタ(4近傍)
	 */
	public static final int EFFECT_KERNEL_LAPLACIAN4 = 1012;
	/**
	 * ラプラシアンフィルタ(8近傍)
	 */
	public static final int EFFECT_KERNEL_LAPLACIAN = EFFECT_KERNEL_LAPLACIAN8;
	/**
	 * Cannyのアルゴリズムによるエッジ強調処理用
	 * MediaEffectGLCannyからインポート
	 * XXX アルゴリズム的にはガウシアンフィルタによるノイズ除去＋ソーベルフィルタでの輪郭強調が
	 *     ベースなのでとりあえずカーネルフィルタの定数にしているけど、実際のフラグメントシェーダーでは
	 *     カーネル関数は使っていない
	 */
	public static final int EFFECT_KERNEL_CANNY = 1013;

	/**
	 * Cannyで検出したエッジを元映像へ適用するフィルター
	 * FIXME 適用処理は要検討(今は輝度加算)
	 */
	public static final int EFFECT_KERNEL_CANNY_ENHANCE = 1014;
	/**
	 * カーネル関数によるフィルタ処理結果を元映像へ適用するフィルター
	 * カーネル関数としてはエッジ検出を想定
	 * FIXME 適用処理は要検討(今は輝度加算)
	 */
	public static final int EFFECT_KERNEL_KERNEL_ENHANCE = 1015;

	public static final int EFFECT_KERNEL_NUM = 1016;

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
		const vec3 conv = vec3(0.3, 0.59, 0.11);
		void main() {
		    vec4 tc = texture2D(sTexture, vTextureCoord);
		    float color = dot(tc.rgb, conv);
		    vec3 cl3 = vec3(color, color, color);
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
		const vec3 conv = vec3(0.3, 0.59, 0.11);
		layout(location = 0) out vec4 o_FragColor;
		void main() {
		    vec4 tc = texture(sTexture, vTextureCoord);
		    float color = dot(tc.rgb, conv);
		    vec3 cl3 = vec3(color, color, color);
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
		const vec3 conv = vec3(0.3, 0.59, 0.11);
		void main() {
		    vec4 tc = texture2D(sTexture, vTextureCoord);
		    float color = dot(tc.rgb, conv);
		    vec3 cl3 = vec3(color, color, color);
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
		const vec3 conv = vec3(0.3, 0.59, 0.11);
		layout(location = 0) out vec4 o_FragColor;
		void main() {
		    vec4 tc = texture(sTexture, vTextureCoord);
		    float color = dot(tc.rgb, conv);
		    vec3 cl3 = vec3(color, color, color);
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
		const vec3 conv = vec3(0.3, 0.59, 0.11);
		const vec3 cl = vec3(%s);
		void main() {
		    vec4 tc = texture2D(sTexture, vTextureCoord);
		    float color = dot(tc.rgb, conv);
		    vec3 bin = step(0.3, vec3(color, color, color));
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
		const vec3 conv = vec3(0.3, 0.59, 0.11);
		const vec3 cl = vec3(%s);
		layout(location = 0) out vec4 o_FragColor;
		void main() {
		    vec4 tc = texture(sTexture, vTextureCoord);
		    float color = dot(tc.rgb, conv);
		    vec3 bin = step(0.3, vec3(color, color, color));
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
		const vec3 conv = vec3(0.3, 0.59, 0.11);
		const vec3 cl = vec3(%s);
		void main() {
		    vec4 tc = texture2D(sTexture, vTextureCoord);
		    float color = dot(tc.rgb, conv);
		    vec3 bin = step(0.3, vec3(color, color, color));
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
		const vec3 conv = vec3(0.3, 0.59, 0.11);
		const vec3 cl = vec3(%s);
		layout(location = 0) out vec4 o_FragColor;
		void main() {
		    vec4 tc = texture(sTexture, vTextureCoord);
		    float color = dot(tc.rgb, conv);
		    vec3 bin = step(0.3, vec3(color, color, color));
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
