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

}
