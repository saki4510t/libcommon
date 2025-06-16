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


import android.util.Log;

import java.util.Arrays;

import androidx.annotation.Nullable;
import androidx.annotation.Size;

import static com.serenegiant.gl.ShaderConst.FRAGMENT_SHADER_COLOR_CORRECTION_ES2;
import static com.serenegiant.gl.ShaderConst.VERTEX_SHADER_ES2;

/**
 * トーンカーブによる濃度変換後、色変換行列を適用するMediaEffectGLBase実装
 */
public class MediaEffectGLColorCorrection extends MediaEffectGLBase {
	private static final boolean DEBUG = false;
	private static final String TAG = "MediaEffectGLColorCorrection";

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
	 * コンストラクタ
	 * 色変換行列として単位行列を割り当てる(=単純コピー)
	 */
	public MediaEffectGLColorCorrection() {
		this(null, 0, null, 0);
	}

	/**
	 * コンストラクタ
	 * 指定したcolorMatrixがnullまたは要素数が16+offsetよりも小さい場合は単位行列になる
	 * @param colorMatrix 色変換行列
	 * @param matrixOffset
	 */
	public MediaEffectGLColorCorrection(
		@Nullable @Size(min=16) final float[] colorMatrix, final int matrixOffset,
		@Nullable final float[] params, final int paramsOffset) {
		super(new MediaEffectGLColorAdjustDrawer(
			false, VERTEX_SHADER_ES2, FRAGMENT_SHADER_COLOR_CORRECTION_ES2, 65));
		if (DEBUG) Log.v(TAG, "コンストラクタ:");
		if (colorMatrix != null) {
			setColorMatrix(colorMatrix, matrixOffset);
		}
		if (params != null) {
			setParams(params, paramsOffset);
		}
	}

	/**
	 * 色変換行列をセット
	 * 指定したcolorMatrixがnullまたは要素数が16+offsetよりも小さい場合は単位行列になる
	 * @param colorMatrix 色変換行列
	 * @param offset
	 */
	public void setColorMatrix(@Nullable @Size(min=16) final float[] colorMatrix, final int offset) {
		if (DEBUG) Log.v(TAG, "setColorMatrix:" + Arrays.toString(colorMatrix));
		((MediaEffectGLColorAdjustDrawer)mDrawer).setColorMatrix(colorMatrix, offset);
	}

	/**
	 * 補正パラメータをセット
	 * パラメータの各値は-1.0〜+1.0で
	 * @param params
	 * @param offset
	 */
	public void setParams(@Nullable @Size(min=16) final float[] params, final int offset) {
		if (DEBUG) Log.v(TAG, "setParams:" + Arrays.toString(params));
		((MediaEffectGLColorAdjustDrawer)mDrawer).setParams(params, offset);
	}

	/**
	 * ガンマ補正カーブを補正パラメータとする
	 * @param gamma
	 */
	public void setGamma(final float gamma) {
		// ガンマ関数を使ってトーンカーブを生成する
		final float[] params = new float[65];
		for (int ix = 0; ix < 64; ix++) {
			final double x =  ix * 4.0;
			params[ix] = (float)((255.0 * Math.pow(x / 255.0, 1.0 / gamma) - x) / 255.0);
		}
		params[64] = params[63];
		if (DEBUG) Log.v(TAG, "setGamma:" + Arrays.toString(params));
		setParams(params, 0);
	}

	/**
	 * コントラスト調整カーブを補正パラメータとする
	 * @param strength -1.0〜+1.0, 0: 補正無し、負ならコントラスト抑制、正ならコントラスト向上
	 */
	public void setContrast(final float strength) {
		// 明るさが反転しないように制限
		float v = strength;
		if (v < -1.0f) v = -1.0f;
		else if (v > 1.0f) v = 1.0f;
		v /= 5.0f;
		// とりあえずsinを使ってトーンカーブを生成する
		// strength>0(コントラスト向上)なら
		// ・intensity>0.5をより明るく
		// ・intensity<0.5をより暗く
		// strength<0(コントラスト抑制)なら
		// ・intensity>0.5をより暗く
		// ・intensity<0.5を寄明るく
		final float[] params = new float[65];
		for (int ix = 0; ix < 64; ix++) {
			final double x =  ix / 32.0 * Math.PI;	// 0〜2PI
			params[ix] = (float)(-v * Math.sin(x));
		}
		params[64] = params[63];
		if (DEBUG) Log.v(TAG, "setContrast:" + Arrays.toString(params));
		setParams(params, 0);
	}

	/**
	 * シグモイドカーブを補正パラメータとする
	 * @param k
	 * @param threshold 0.0〜1.0
	 */
	public void setSigmoid(final float k, final float threshold) {
		final float[] params = new float[65];
		for (int ix = 0; ix < 64; ix++) {
			final double x =  ix * 4.0 / 255.0;
			params[ix] = (float)(1.0 / (1.0 + Math.pow(Math.E, -k * (x - threshold))));
		}
		params[64] = params[63];
		if (DEBUG) Log.v(TAG, "setSigmoid:" + Arrays.toString(params));
		setParams(params, 0);
	}
}
