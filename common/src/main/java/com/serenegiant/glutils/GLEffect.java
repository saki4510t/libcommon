package com.serenegiant.glutils;
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

import static com.serenegiant.glutils.ShaderConst.*;

public class GLEffect {
	private GLEffect() {
		// インスタンス化を防ぐためにデフォルトコンストラクタをprivateに
	}

	public static final int MAX_PARAM_NUM = 18;

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

//--------------------------------------------------------------------------------
	/**
	 * グレースケール変換のためのフラグメントシェーダーのベース文字列
	 * header(HEADER_OESかHEADER_2D)とサンプラーの種類文字列(SAMPLER_OESかSAMPLER_2D)を渡すこと
	 */
	private static final String FRAGMENT_SHADER_GRAY_BASE_ES2
		= SHADER_VERSION_ES2 +
		"%s" +
		"precision mediump float;\n" +
		"varying vec2 vTextureCoord;\n" +
		"uniform %s sTexture;\n" +
		"const vec3 conv = vec3(0.3, 0.59, 0.11);\n" +
		"void main() {\n" +
		"    vec4 tc = texture2D(sTexture, vTextureCoord);\n" +
		"    float color = dot(tc.rgb, conv);\n" +
		"    vec3 cl3 = vec3(color, color, color);\n" +
		"    gl_FragColor = vec4(cl3, 1.0);\n" +
	"}\n";

	public static final String FRAGMENT_SHADER_EXT_GRAY_ES2
		= String.format(FRAGMENT_SHADER_GRAY_BASE_ES2,
			HEADER_OES_ES2, SAMPLER_OES);

	/**
	 * グレースケール変換のためのフラグメントシェーダーのベース文字列
	 * header(HEADER_OESかHEADER_2D)とサンプラーの種類文字列(SAMPLER_OESかSAMPLER_2D)を渡すこと
	 */
	private static final String FRAGMENT_SHADER_GRAY_BASE_ES3
		= SHADER_VERSION_ES3 +
		"%s" +
		"precision mediump float;\n" +
		"in vec2 vTextureCoord;\n" +
		"uniform %s sTexture;\n" +
		"const vec3 conv = vec3(0.3, 0.59, 0.11);\n" +
		"layout(location = 0) out vec4 o_FragColor;\n" +
		"void main() {\n" +
		"    vec4 tc = texture(sTexture, vTextureCoord);\n" +
		"    float color = dot(tc.rgb, conv);\n" +
		"    vec3 cl3 = vec3(color, color, color);\n" +
		"    o_FragColor = vec4(cl3, 1.0);\n" +
	"}\n";

	public static final String FRAGMENT_SHADER_EXT_GRAY_ES3
		= String.format(FRAGMENT_SHADER_GRAY_BASE_ES3,
			HEADER_OES_ES3, SAMPLER_OES);

//--------------------------------------------------------------------------------
	/**
	 * 白黒反転したグレースケール変換のためのフラグメントシェーダーのベース文字列
	 * header(HEADER_OESかHEADER_2D)とサンプラーの種類文字列(SAMPLER_OESかSAMPLER_2D)を渡すこと
	 */
	private static final String FRAGMENT_SHADER_GRAY_REVERSE_BASE_ES2
		= SHADER_VERSION_ES2 +
		"%s" +
		"precision mediump float;\n" +
		"varying vec2 vTextureCoord;\n" +
		"uniform %s sTexture;\n" +
		"const vec3 conv = vec3(0.3, 0.59, 0.11);\n" +
		"void main() {\n" +
		"    vec4 tc = texture2D(sTexture, vTextureCoord);\n" +
		"    float color = dot(tc.rgb, conv);\n" +
		"    vec3 cl3 = vec3(color, color, color);\n" +
		"    gl_FragColor = vec4(clamp(vec3(1.0, 1.0, 1.0) - cl3, 0.0, 1.0), 1.0);\n" +
	"}\n";

	public static final String FRAGMENT_SHADER_GRAY_EXT_REVERSE_ES2
		= String.format(FRAGMENT_SHADER_GRAY_REVERSE_BASE_ES2,
			HEADER_OES_ES2, SAMPLER_OES);

	/**
	 * 白黒反転したグレースケール変換のためのフラグメントシェーダーのベース文字列
	 * header(HEADER_OESかHEADER_2D)とサンプラーの種類文字列(SAMPLER_OESかSAMPLER_2D)を渡すこと
	 */
	private static final String FRAGMENT_SHADER_GRAY_REVERSE_BASE_ES3
		= SHADER_VERSION_ES3 +
		"%s" +
		"precision mediump float;\n" +
		"in vec2 vTextureCoord;\n" +
		"uniform %s sTexture;\n" +
		"const vec3 conv = vec3(0.3, 0.59, 0.11);\n" +
		"layout(location = 0) out vec4 o_FragColor;\n" +
		"void main() {\n" +
		"    vec4 tc = texture(sTexture, vTextureCoord);\n" +
		"    float color = dot(tc.rgb, conv);\n" +
		"    vec3 cl3 = vec3(color, color, color);\n" +
		"    o_FragColor = vec4(clamp(vec3(1.0, 1.0, 1.0) - cl3, 0.0, 1.0), 1.0);\n" +
	"}\n";

	public static final String FRAGMENT_SHADER_GRAY_EXT_REVERSE_ES3
		= String.format(FRAGMENT_SHADER_GRAY_REVERSE_BASE_ES3,
			HEADER_OES_ES3, SAMPLER_OES);

//--------------------------------------------------------------------------------
	/**
	 * 2値化のためのフラグメントシェーダーのベース文字列
	 * header(HEADER_OESかHEADER_2D)とサンプラーの種類文字列(SAMPLER_OESかSAMPLER_2D)、
	 * 変換後の明るい部分用の色を指定するための文字列(R, G, Bの順)を渡すこと
	 */
	private static final String FRAGMENT_SHADER_BIN_BASE_ES2
		= SHADER_VERSION_ES2 +
		"%s" +
		"precision mediump float;\n" +
		"varying vec2 vTextureCoord;\n" +
		"uniform %s sTexture;\n" +
		"const vec3 conv = vec3(0.3, 0.59, 0.11);\n" +
		"const vec3 cl = vec3(%s);\n" +
		"void main() {\n" +
		"    vec4 tc = texture2D(sTexture, vTextureCoord);\n" +
		"    float color = dot(tc.rgb, conv);\n" +
		"    vec3 bin = step(0.3, vec3(color, color, color));\n" +
		"    gl_FragColor = vec4(cl * bin, 1.0);\n" +
	"}\n";

	public static final String FRAGMENT_SHADER_EXT_BIN_ES2
		= String.format(FRAGMENT_SHADER_BIN_BASE_ES2,
			HEADER_OES_ES2, SAMPLER_OES, "1.0, 1.0, 1.0");

	public static final String FRAGMENT_SHADER_EXT_BIN_YELLOW_ES2
		= String.format(FRAGMENT_SHADER_BIN_BASE_ES2,
			HEADER_OES_ES2, SAMPLER_OES, "1.0, 1.0, 0.0");

	public static final String FRAGMENT_SHADER_EXT_BIN_GREEN_ES2
		= String.format(FRAGMENT_SHADER_BIN_BASE_ES2,
			HEADER_OES_ES2, SAMPLER_OES, "0.0, 1.0, 0.0");

	/**
	 * 2値化のためのフラグメントシェーダーのベース文字列
	 * header(HEADER_OESかHEADER_2D)とサンプラーの種類文字列(SAMPLER_OESかSAMPLER_2D)、
	 * 変換後の明るい部分用の色を指定するための文字列(R, G, Bの順)を渡すこと
	 */
	private static final String FRAGMENT_SHADER_BIN_BASE_ES3
		= SHADER_VERSION_ES3 +
		"%s" +
		"precision mediump float;\n" +
		"in vec2 vTextureCoord;\n" +
		"uniform %s sTexture;\n" +
		"const vec3 conv = vec3(0.3, 0.59, 0.11);\n" +
		"const vec3 cl = vec3(%s);\n" +
		"layout(location = 0) out vec4 o_FragColor;\n" +
		"void main() {\n" +
		"    vec4 tc = texture(sTexture, vTextureCoord);\n" +
		"    float color = dot(tc.rgb, conv);\n" +
		"    vec3 bin = step(0.3, vec3(color, color, color));\n" +
		"    o_FragColor = vec4(cl * bin, 1.0);\n" +
	"}\n";

	public static final String FRAGMENT_SHADER_EXT_BIN_ES3
		= String.format(FRAGMENT_SHADER_BIN_BASE_ES3,
			HEADER_OES_ES3, SAMPLER_OES, "1.0, 1.0, 1.0");

	public static final String FRAGMENT_SHADER_EXT_BIN_YELLOW_ES3
		= String.format(FRAGMENT_SHADER_BIN_BASE_ES3,
			HEADER_OES_ES3, SAMPLER_OES, "1.0, 1.0, 0.0");

	public static final String FRAGMENT_SHADER_EXT_BIN_GREEN_ES3
		= String.format(FRAGMENT_SHADER_BIN_BASE_ES3,
			HEADER_OES_ES3, SAMPLER_OES, "0.0, 1.0, 0.0");

//--------------------------------------------------------------------------------
	/**
	 * 反転した2値化のためのフラグメントシェーダーのベース文字列
	 * header(HEADER_OESかHEADER_2D)とサンプラーの種類文字列(SAMPLER_OESかSAMPLER_2D)、
	 * 変換後の明るい部分用の色を指定するための文字列(R, G, Bの順)を渡すこと
	 */
	private static final String FRAGMENT_SHADER_BIN_REVERSE_BASE_ES2
		= SHADER_VERSION_ES2 +
		"%s" +
		"precision mediump float;\n" +
		"varying vec2 vTextureCoord;\n" +
		"uniform %s sTexture;\n" +
		"const vec3 conv = vec3(0.3, 0.59, 0.11);\n" +
		"const vec3 cl = vec3(%s);\n" +
		"void main() {\n" +
		"    vec4 tc = texture2D(sTexture, vTextureCoord);\n" +
		"    float color = dot(tc.rgb, conv);\n" +
		"    vec3 bin = step(0.3, vec3(color, color, color));\n" +
		"    gl_FragColor = vec4(cl * (vec3(1.0, 1.0, 1.0) - bin), 1.0);\n" +
	"}\n";

	public static final String FRAGMENT_SHADER_EXT_BIN_REVERSE_ES2
		= String.format(FRAGMENT_SHADER_BIN_REVERSE_BASE_ES2,
			HEADER_OES_ES2, SAMPLER_OES, "1.0, 1.0, 1.0");

	public static final String FRAGMENT_SHADER_EXT_BIN_REVERSE_YELLOW_ES2
		= String.format(FRAGMENT_SHADER_BIN_REVERSE_BASE_ES2,
			HEADER_OES_ES2, SAMPLER_OES, "1.0, 1.0, 0.0");

	public static final String FRAGMENT_SHADER_EXT_BIN_REVERSE_GREEN_ES2
		= String.format(FRAGMENT_SHADER_BIN_REVERSE_BASE_ES2,
			HEADER_OES_ES2, SAMPLER_OES, "0.0, 1.0, 0.0");

	/**
	 * 反転した2値化のためのフラグメントシェーダーのベース文字列
	 * header(HEADER_OESかHEADER_2D)とサンプラーの種類文字列(SAMPLER_OESかSAMPLER_2D)、
	 * 変換後の明るい部分用の色を指定するための文字列(R, G, Bの順)を渡すこと
	 */
	private static final String FRAGMENT_SHADER_BIN_REVERSE_BASE_ES3
		= SHADER_VERSION_ES3 +
		"%s" +
		"precision mediump float;\n" +
		"in vec2 vTextureCoord;\n" +
		"uniform %s sTexture;\n" +
		"const vec3 conv = vec3(0.3, 0.59, 0.11);\n" +
		"const vec3 cl = vec3(%s);\n" +
		"layout(location = 0) out vec4 o_FragColor;\n" +
		"void main() {\n" +
		"    vec4 tc = texture(sTexture, vTextureCoord);\n" +
		"    float color = dot(tc.rgb, conv);\n" +
		"    vec3 bin = step(0.3, vec3(color, color, color));\n" +
		"    o_FragColor = vec4(cl * (vec3(1.0, 1.0, 1.0) - bin), 1.0);\n" +
	"}\n";

	public static final String FRAGMENT_SHADER_EXT_BIN_REVERSE_ES3
		= String.format(FRAGMENT_SHADER_BIN_REVERSE_BASE_ES3,
			HEADER_OES_ES3, SAMPLER_OES, "1.0, 1.0, 1.0");

	public static final String FRAGMENT_SHADER_EXT_BIN_REVERSE_YELLOW_ES3
		= String.format(FRAGMENT_SHADER_BIN_REVERSE_BASE_ES3,
			HEADER_OES_ES3, SAMPLER_OES, "1.0, 1.0, 0.0");

	public static final String FRAGMENT_SHADER_EXT_BIN_REVERSE_GREEN_ES3
		= String.format(FRAGMENT_SHADER_BIN_REVERSE_BASE_ES3,
			HEADER_OES_ES3, SAMPLER_OES, "0.0, 1.0, 0.0");

//--------------------------------------------------------------------------------
	/**
	 * 赤と黄色を強調するためのフラグメントシェーダーのベース文字列
	 */
	private static final String FRAGMENT_SHADER_EMPHASIZE_RED_YELLOW_BASE_ES2
		= SHADER_VERSION_ES2 +
		"%s" +
		"precision mediump float;\n" +
		"varying vec2 vTextureCoord;\n" +
		"uniform %s sTexture;\n" +
		"uniform float uParams[" + MAX_PARAM_NUM + "];\n" +
		FUNC_RGB2HSV +
		FUNC_HSV2RGB +
		"void main() {\n" +
		"    vec3 hsv = rgb2hsv(texture2D(sTexture, vTextureCoord).rgb);\n" +	// RGBをHSVに変換
		"    if ( ((hsv.g >= uParams[2]) && (hsv.g <= uParams[3]))\n" +			// s
		"        && ((hsv.b >= uParams[4]) && (hsv.b <= uParams[5]))\n" +		// v
		"        && ((hsv.r <= uParams[0]) || (hsv.r >= uParams[1])) ) {\n" +	// h
		"        hsv = hsv * vec3(uParams[6], uParams[7], uParams[8]);\n" +		// 赤色と黄色の範囲
		"    } else {\n" +
		"        hsv = hsv * vec3(uParams[9], uParams[10], uParams[11]);\n" +	// それ以外なら
		"    }\n" +
		"    gl_FragColor = vec4(hsv2rgb(clamp(hsv, 0.0, 1.0)), 1.0);\n" +		// HSVをRGBに戻す
	"}\n";

	public static final String FRAGMENT_SHADER_EXT_EMPHASIZE_RED_YELLOWS_ES2
		= String.format(FRAGMENT_SHADER_EMPHASIZE_RED_YELLOW_BASE_ES2,
			HEADER_OES_ES2, SAMPLER_OES);

	/**
	 * 赤と黄色を強調するためのフラグメントシェーダーのベース文字列
	 */
	private static final String FRAGMENT_SHADER_EMPHASIZE_RED_YELLOW_BASE_ES3
		= SHADER_VERSION_ES3 +
		"%s" +
		"precision mediump float;\n" +
		"in vec2 vTextureCoord;\n" +
		"uniform %s sTexture;\n" +
		"uniform float uParams[" + MAX_PARAM_NUM + "];\n" +
		"layout(location = 0) out vec4 o_FragColor;\n" +
		FUNC_RGB2HSV +
		FUNC_HSV2RGB +
		"void main() {\n" +
		"    vec3 hsv = rgb2hsv(texture(sTexture, vTextureCoord).rgb);\n" +	// RGBをHSVに変換
		"    if ( ((hsv.g >= uParams[2]) && (hsv.g <= uParams[3]))\n" +			// s
		"        && ((hsv.b >= uParams[4]) && (hsv.b <= uParams[5]))\n" +		// v
		"        && ((hsv.r <= uParams[0]) || (hsv.r >= uParams[1])) ) {\n" +	// h
		"        hsv = hsv * vec3(uParams[6], uParams[7], uParams[8]);\n" +		// 赤色と黄色の範囲
		"    } else {\n" +
		"        hsv = hsv * vec3(uParams[9], uParams[10], uParams[11]);\n" +	// それ以外なら
		"    }\n" +
		"    o_FragColor = vec4(hsv2rgb(clamp(hsv, 0.0, 1.0)), 1.0);\n" +		// HSVをRGBに戻す
	"}\n";

	public static final String FRAGMENT_SHADER_EXT_EMPHASIZE_RED_YELLOWS_ES3
		= String.format(FRAGMENT_SHADER_EMPHASIZE_RED_YELLOW_BASE_ES3,
			HEADER_OES_ES3, SAMPLER_OES);

//--------------------------------------------------------------------------------
	/**
	 * 赤と黄色と白色を強調するためのフラグメントシェーダーのベース文字列
	 */
	private static final String FRAGMENT_SHADER_EMPHASIZE_RED_YELLOW_WHITE_BASE_ES2
		= SHADER_VERSION_ES2 +
		"%s" +
		"precision mediump float;\n" +
		"varying vec2 vTextureCoord;\n" +
		"uniform %s sTexture;\n" +
		"uniform float uParams[" + MAX_PARAM_NUM + "];\n" +
		FUNC_RGB2HSV +
		FUNC_HSV2RGB +
		"void main() {\n" +
		"    vec3 hsv = rgb2hsv(texture2D(sTexture, vTextureCoord).rgb);\n" +	// RGBをHSVに変換
		"    if ( ((hsv.g >= uParams[2]) && (hsv.g <= uParams[3]))\n" +			// s
		"        && ((hsv.b >= uParams[4]) && (hsv.b <= uParams[5]))\n" +		// v
		"        && ((hsv.r <= uParams[0]) || (hsv.r >= uParams[1])) ) {\n" +	// h
		"        hsv = hsv * vec3(uParams[6], uParams[7], uParams[8]);\n" +		// 赤色と黄色の範囲
		"    } else if ((hsv.g < uParams[12]) && (hsv.b < uParams[13])) {\n" +	// 彩度が一定以下, 明度が一定以下なら
		"        hsv = hsv * vec3(1.0, 0.0, 2.0);\n" +							// 色相そのまま, 彩度0, 明度x2
		"    } else {\n" +
		"        hsv = hsv * vec3(uParams[9], uParams[10], uParams[11]);\n" +	// それ以外なら
		"    }\n" +
		"    gl_FragColor = vec4(hsv2rgb(clamp(hsv, 0.0, 1.0)), 1.0);\n" +		// HSVをRGBに戻す
	"}\n";

	public static final String FRAGMENT_SHADER_EXT_EMPHASIZE_RED_YELLOW_WHITE_ES2
		= String.format(FRAGMENT_SHADER_EMPHASIZE_RED_YELLOW_WHITE_BASE_ES2,
			HEADER_OES_ES2, SAMPLER_OES);

	/**
	 * 赤と黄色と白色を強調するためのフラグメントシェーダーのベース文字列
	 */
	private static final String FRAGMENT_SHADER_EMPHASIZE_RED_YELLOW_WHITE_BASE_ES3
		= SHADER_VERSION_ES3 +
		"%s" +
		"precision mediump float;\n" +
		"in vec2 vTextureCoord;\n" +
		"uniform %s sTexture;\n" +
		"uniform float uParams[" + MAX_PARAM_NUM + "];\n" +
		"layout(location = 0) out vec4 o_FragColor;\n" +
		FUNC_RGB2HSV +
		FUNC_HSV2RGB +
		"void main() {\n" +
		"    vec3 hsv = rgb2hsv(texture(sTexture, vTextureCoord).rgb);\n" +		// RGBをHSVに変換
		"    if ( ((hsv.g >= uParams[2]) && (hsv.g <= uParams[3]))\n" +			// s
		"        && ((hsv.b >= uParams[4]) && (hsv.b <= uParams[5]))\n" +		// v
		"        && ((hsv.r <= uParams[0]) || (hsv.r >= uParams[1])) ) {\n" +	// h
		"        hsv = hsv * vec3(uParams[6], uParams[7], uParams[8]);\n" +		// 赤色と黄色の範囲
		"    } else if ((hsv.g < uParams[12]) && (hsv.b < uParams[13])) {\n" +	// 彩度が一定以下, 明度が一定以下なら
		"        hsv = hsv * vec3(1.0, 0.0, 2.0);\n" +							// 色相そのまま, 彩度0, 明度x2
		"    } else {\n" +
		"        hsv = hsv * vec3(uParams[9], uParams[10], uParams[11]);\n" +	// それ以外なら
		"    }\n" +
		"    o_FragColor = vec4(hsv2rgb(clamp(hsv, 0.0, 1.0)), 1.0);\n" +		// HSVをRGBに戻す
	"}\n";

	public static final String FRAGMENT_SHADER_EXT_EMPHASIZE_RED_YELLOW_WHITE_ES3
		= String.format(FRAGMENT_SHADER_EMPHASIZE_RED_YELLOW_WHITE_BASE_ES3,
			HEADER_OES_ES3, SAMPLER_OES);

//--------------------------------------------------------------------------------
	/**
	 * 黄色と白を強調するためのフラグメントシェーダーのベース文字列
	 * 今はFRAGMENT_SHADER_EMPHASIZE_RED_YELLOW_WHITE_BASEと同じ(違うパラメータ渡せば良いだけなので)
	 */
	private static final String FRAGMENT_SHADER_EMPHASIZE_YELLOW_WHITE_BASE_ES2
		= SHADER_VERSION_ES2 +
		"%s" +
		"precision mediump float;\n" +
		"varying vec2 vTextureCoord;\n" +
		"uniform %s sTexture;\n" +
		"uniform float uParams[" + MAX_PARAM_NUM + "];\n" +
		FUNC_RGB2HSV +
		FUNC_HSV2RGB +
		"void main() {\n" +
		"    vec3 rgb = texture2D(sTexture, vTextureCoord).rgb;\n" +			// RGB
		"    vec3 hsv = rgb2hsv(rgb);\n" +										// RGBをHSVに変換
		"    if (   ((hsv.r >= uParams[0]) && (hsv.r <= uParams[1]))\n" +		// h
		"        && ((hsv.g >= uParams[2]) && (hsv.g <= uParams[3]))\n" +		// s
		"        && ((hsv.b >= uParams[4]) && (hsv.b <= uParams[5])) ) {\n" +	// v
		"        hsv = hsv * vec3(uParams[6], uParams[7], uParams[8]);\n" +		// 黄色の範囲
		"    } else if ((hsv.g < uParams[12]) && (hsv.b > uParams[13])) {\n" +	// 彩度が一定以下, 明度が一定以上なら
		"        hsv = hsv * vec3(1.0, 0.0, 2.0);\n" +							// 色相そのまま, 彩度0, 明度x2
		"    } else {\n" +
		"        hsv = hsv * vec3(uParams[9], uParams[10], uParams[11]);\n" +	// それ以外なら
		"    }\n" +
		"    gl_FragColor = vec4(hsv2rgb(clamp(hsv, 0.0, 1.0)), 1.0);\n" +		// HSVをRGBに戻す
	"}\n";

	public static final String FRAGMENT_SHADER_EXT_EMPHASIZE_YELLOW_WHITE_ES2
		= String.format(FRAGMENT_SHADER_EMPHASIZE_YELLOW_WHITE_BASE_ES2,
			HEADER_OES_ES2, SAMPLER_OES);

	/**
	 * 黄色と白を強調するためのフラグメントシェーダーのベース文字列
	 * 今はFRAGMENT_SHADER_EMPHASIZE_RED_YELLOW_WHITE_BASEと同じ(違うパラメータ渡せば良いだけなので)
	 */
	private static final String FRAGMENT_SHADER_EMPHASIZE_YELLOW_WHITE_BASE_ES3
		= SHADER_VERSION_ES3 +
		"%s" +
		"precision mediump float;\n" +
		"in vec2 vTextureCoord;\n" +
		"uniform %s sTexture;\n" +
		"uniform float uParams[" + MAX_PARAM_NUM + "];\n" +
		"layout(location = 0) out vec4 o_FragColor;\n" +
		FUNC_RGB2HSV +
		FUNC_HSV2RGB +
		"void main() {\n" +
		"    vec3 rgb = texture(sTexture, vTextureCoord).rgb;\n" +			// RGB
		"    vec3 hsv = rgb2hsv(rgb);\n" +										// RGBをHSVに変換
		"    if (   ((hsv.r >= uParams[0]) && (hsv.r <= uParams[1]))\n" +		// h
		"        && ((hsv.g >= uParams[2]) && (hsv.g <= uParams[3]))\n" +		// s
		"        && ((hsv.b >= uParams[4]) && (hsv.b <= uParams[5])) ) {\n" +	// v
		"        hsv = hsv * vec3(uParams[6], uParams[7], uParams[8]);\n" +		// 黄色の範囲
		"    } else if ((hsv.g < uParams[12]) && (hsv.b > uParams[13])) {\n" +	// 彩度が一定以下, 明度が一定以上なら
		"        hsv = hsv * vec3(1.0, 0.0, 2.0);\n" +							// 色相そのまま, 彩度0, 明度x2
		"    } else {\n" +
		"        hsv = hsv * vec3(uParams[9], uParams[10], uParams[11]);\n" +	// それ以外なら
		"    }\n" +
		"    o_FragColor = vec4(hsv2rgb(clamp(hsv, 0.0, 1.0)), 1.0);\n" +		// HSVをRGBに戻す
	"}\n";

	public static final String FRAGMENT_SHADER_EXT_EMPHASIZE_YELLOW_WHITE_ES3
		= String.format(FRAGMENT_SHADER_EMPHASIZE_YELLOW_WHITE_BASE_ES3,
			HEADER_OES_ES3, SAMPLER_OES);
}
