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
	/**
	 * 近傍を含めた9画素の平均値を閾値とする二値化、白黒
	 */
	public static final int EFFECT_ADAPTIVE_BIN = 12;
	/**
	 * 近傍を含めた9画素の平均値を閾値とする二値化、黄黒
	 */
	public static final int EFFECT_ADAPTIVE_BIN_YELLOW = 13;
	/**
	 * 近傍を含めた9画素の平均値を閾値とする二値化、緑黒
	 */
	public static final int EFFECT_ADAPTIVE_BIN_GREEN = 14;
	/**
	 * 近傍を含めた9画素の平均値を閾値とする二値化、白黒,反転
	 */
	public static final int EFFECT_ADAPTIVE_BIN_REVERSE = 15;
	/**
	 * 近傍を含めた9画素の平均値を閾値とする二値化、黄黒,反転
	 */
	public static final int EFFECT_ADAPTIVE_BIN_REVERSE_YELLOW = 15;
	/**
	 * 近傍を含めた9画素の平均値を閾値とする二値化、緑黒,反転
	 */
	public static final int EFFECT_ADAPTIVE_BIN_REVERSE_GREEN = 17;
	/** 内蔵映像効果の数 */
	public static final int EFFECT_NUM = 18;

	/**
	 * ソーベルフィルタ(水平方向)
	 */
	public static final int EFFECT_KERNEL_SOBEL_H = 1000;
	/**
	 * ソーベルフィルタ(垂直方向)
	 */
	public static final int EFFECT_KERNEL_SOBEL_V = 1001;
	/**
	 * ソーベルフィルタ(水平＋垂直)
	 */
	public static final int EFFECT_KERNEL_SOBEL_HV = 1002;
	/**
	 * ソーベルフィルタ2(水平方向)
	 */
	public static final int EFFECT_KERNEL_SOBEL2_H = 1003;
	/**
	 * ソーベルフィルタ2(垂直方向)
	 */
	public static final int EFFECT_KERNEL_SOBEL2_V = 1004;
	/**
	 * ソーベルフィルタ2(水平+垂直)
	 */
	public static final int EFFECT_KERNEL_SOBEL2_HV = 1005;
	/**
	 * プレヴィットフィルタ(水平方向)
	 */
	public static final int EFFECT_KERNEL_PREWITT_H = 1006;
	/**
	 * プレヴィットフィルタ(垂直方向)
	 */
	public static final int EFFECT_KERNEL_PREWITT_V = 1007;
	/**
	 * プレヴィットフィルタ(水平＋垂直)
	 */
	public static final int EFFECT_KERNEL_PREWITT_HV = 1008;
	/**
	 * ロバーツフィルタ(水平方向)
	 */
	public static final int EFFECT_KERNEL_ROBERTS_H = 1009;
	/**
	 * ロバーツフィルタ(垂直方向)
	 */
	public static final int EFFECT_KERNEL_ROBERTS_V = 1010;
	/**
	 * ロバーツフィルタ(水平＋垂直)
	 */
	public static final int EFFECT_KERNEL_ROBERTS_HV = 1011;
	/**
	 * エッジ強調4近傍(=シャープネス)
	 */
	public static final int EFFECT_KERNEL_EDGE_ENHANCE4 = 1012;
	/**
	 * エッジ強調9近傍
	 */
	public static final int EFFECT_KERNEL_EDGE_ENHANCE8 = 1013;
	/**
	 * シャープネス
	 */
	public static final int EFFECT_KERNEL_SHARPNESS = EFFECT_KERNEL_EDGE_ENHANCE4;
	/**
	 * エッジ検出
	 */
	public static final int EFFECT_KERNEL_EDGE_DETECT = 1014;
	/**
	 * エンボス
	 */
	public static final int EFFECT_KERNEL_EMBOSS = 1015;
	/**
	 * 平滑化
	 */
	public static final int EFFECT_KERNEL_SMOOTH = 1016;
	/**
	 * ガウシアンフィルタ
	 */
	public static final int EFFECT_KERNEL_GAUSSIAN = 1017;
	/**
	 * 輝度アップ
	 */
	public static final int EFFECT_KERNEL_BRIGHTEN = 1018;
	/**
	 * ラプラシアンフィルタ(8近傍)
	 */
	public static final int EFFECT_KERNEL_LAPLACIAN8 = 1019;
	/**
	 * ラプラシアンフィルタ(4近傍)
	 */
	public static final int EFFECT_KERNEL_LAPLACIAN4 = 1020;
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
	public static final int EFFECT_KERNEL_CANNY = 1021;
	/**
	 * Cannyで検出したエッジを元映像へ適用するフィルター
	 * FIXME 適用処理は要検討(今は輝度加算)
	 */
	public static final int EFFECT_KERNEL_CANNY_ENHANCE = 1022;
	/**
	 * カーネル関数によるフィルタ処理結果を元映像へ適用するフィルター
	 * カーネル関数としてはエッジ検出を想定
	 * FIXME 適用処理は要検討(今は輝度加算)
	 */
	public static final int EFFECT_KERNEL_KERNEL_ENHANCE = 1023;

	public static final int EFFECT_KERNEL_NUM = 1024;

}
