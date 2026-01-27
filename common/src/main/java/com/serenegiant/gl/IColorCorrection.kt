package com.serenegiant.gl
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2026 saki t_saki@serenegiant.com
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

import androidx.annotation.Size
import com.serenegiant.gl.IColorCorrection.Companion.NUM_TONE_CURVE
import kotlin.math.pow
import kotlin.math.sin

/**
 * トーンカーブによる濃度変換後色変換行列を適用可能なクラスが実装するメソッドを定義するインターフェース
 */
interface IColorCorrection {
	/**
	 * 4x4色変換行列を取得
	 */
	fun getColorMatrix(): FloatArray

	/**
	 * 4x4色変換行列を設定
	 * @param colorMatrix nullまたはoffset+16要素以上のfloat配列
	 * @param offset
	 */
	fun setColorMatrix(@Size(min = 16) colorMatrix: FloatArray?, offset: Int)

	/**
	 * トーンカーブを取得
	 */
	fun getToneCurve(): FloatArray

	/**
	 * トーンカーブを設定
	 * @param curve nullまたはoffset+65要素以上のfloat配列
	 * @param offset
	 */
	fun setToneCurve(@Size(min = 65) curve: FloatArray?, offset: Int)

	companion object {
		 const val NUM_TONE_CURVE = 65
	}
}

/**
 * 	ガンマ関数をトーンカーブとして設定する
 */
fun IColorCorrection.setGamma(gamma: Float) {
	val curve = FloatArray(NUM_TONE_CURVE)
	for (ix in 0..NUM_TONE_CURVE - 2) {
		val x = ix * 4.0
		curve[ix] = ((255.0 * (x / 255.0).pow(1.0 / gamma) - x) / 255.0).toFloat()
	}
	curve[NUM_TONE_CURVE - 1] = curve[NUM_TONE_CURVE - 2]
	setToneCurve(curve, 0)
}

/**
 * コントラスト調整カーブをトーンカーブとして設定する
 * @param strength -1.0〜+1.0, 0: 補正無し、負ならコントラスト抑制、正ならコントラスト向上
 */
fun IColorCorrection.setContrast(strength: Float) {
	// 明るさが反転しないように制限
	var v = strength
	if (v < -1.0f) v = -1.0f
	else if (v > 1.0f) v = 1.0f
	v /= 5.0f
	// とりあえずsinを使ってトーンカーブを生成する
	// strength>0(コントラスト向上)なら
	// ・intensity>0.5をより明るく
	// ・intensity<0.5をより暗く
	// strength<0(コントラスト抑制)なら
	// ・intensity>0.5をより暗く
	// ・intensity<0.5を寄明るく
	val curve = FloatArray(NUM_TONE_CURVE)
	for (ix in 0..NUM_TONE_CURVE - 2) {
		val x = ix / 32.0 * Math.PI // 0〜2PI
		curve[ix] = (-v * sin(x)).toFloat()
	}
	curve[NUM_TONE_CURVE - 1] = curve[NUM_TONE_CURVE - 2]
	setToneCurve(curve, 0)
}

/**
 * シグモイド曲線をトーンカーブとして設定する
 */
fun IColorCorrection.setSigmoid(k: Float, threshold: Float) {
	val curve = FloatArray(NUM_TONE_CURVE)
	for (ix in 0..NUM_TONE_CURVE - 2) {
		val x = ix * 4.0 / 255.0
		curve[ix] = (1.0 / (1.0 + Math.E.pow(-k * (x - threshold)))).toFloat()
	}
	curve[NUM_TONE_CURVE - 1] = curve[NUM_TONE_CURVE - 2]
	setToneCurve(curve, 0)
}
