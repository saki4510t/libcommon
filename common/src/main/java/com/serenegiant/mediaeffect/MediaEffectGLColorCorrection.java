package com.serenegiant.mediaeffect;
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


import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import com.serenegiant.gl.IColorCorrection;

import java.util.Arrays;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;

import static com.serenegiant.gl.ShaderConst.FRAGMENT_SHADER_COLOR_CORRECTION_ES2;
import static com.serenegiant.gl.ShaderConst.VERTEX_SHADER_ES2;

/**
 * トーンカーブによる濃度変換後、色変換行列を適用するMediaEffectGLBase実装
 */
public class MediaEffectGLColorCorrection extends MediaEffectGLBase implements IColorCorrection {
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
	 * @param colorMatrixOffset
	 * @param toneCurve
	 * @param toneCurveOffset
	 */
	public MediaEffectGLColorCorrection(
		@Nullable @Size(min=16) final float[] colorMatrix, final int colorMatrixOffset,
		@Nullable @Size(min=NUM_TONE_CURVE) final float[] toneCurve, final int toneCurveOffset) {
		super(new MediaEffectGLColorCorrectionDrawer(
			false, VERTEX_SHADER_ES2, FRAGMENT_SHADER_COLOR_CORRECTION_ES2));
		if (DEBUG) Log.v(TAG, "コンストラクタ:");
		if (colorMatrix != null) {
			setColorMatrix(colorMatrix, colorMatrixOffset);
		}
		if (toneCurve != null) {
			setToneCurve(toneCurve, toneCurveOffset);
		}
	}

	@NonNull
	@Override
	public float[] getColorMatrix() {
		return ((MediaEffectGLColorCorrectionDrawer)mDrawer).getColorMatrix();
	}

	/**
	 * 色変換行列をセット
	 * 指定したcolorMatrixがnullまたは要素数が16+offsetよりも小さい場合は単位行列になる
	 * @param colorMatrix 色変換行列
	 * @param offset
	 */
	@Override
	public void setColorMatrix(@Nullable @Size(min=16) final float[] colorMatrix, final int offset) {
		if (DEBUG) Log.v(TAG, "setColorMatrix:" + Arrays.toString(colorMatrix));
		((MediaEffectGLColorCorrectionDrawer)mDrawer).setColorMatrix(colorMatrix, offset);
	}

	@NonNull
	@Override
	public float[] getToneCurve() {
		return ((MediaEffectGLColorCorrectionDrawer)mDrawer).getToneCurve();
	}

	/**
	 * トーンカーブをセット
	 * パラメータの各値は-1.0〜+1.0で
	 * @param params
	 * @param offset
	 */
	@Override
	public void setToneCurve(@Nullable @Size(min=NUM_TONE_CURVE) final float[] params, final int offset) {
		if (DEBUG) Log.v(TAG, "setParams:" + Arrays.toString(params));
		((MediaEffectGLColorCorrectionDrawer)mDrawer).setToneCurve(params, offset);
	}

	/**
	 * ガンマ曲線をトーンカーブとする
	 * @param gamma
	 * XXX 削除予定 IColorCorrectionインターフェースのkotlin拡張関数を使う
	 */
	public void setGamma(final float gamma) {
		// ガンマ関数を使ってトーンカーブを生成する
		final float[] curve = new float[65];
		for (int ix = 0; ix < 64; ix++) {
			final double x =  ix * 4.0;
			curve[ix] = (float)((255.0 * Math.pow(x / 255.0, 1.0 / gamma) - x) / 255.0);
		}
		curve[64] = curve[63];
		if (DEBUG) Log.v(TAG, "setGamma:" + Arrays.toString(curve));
		setToneCurve(curve, 0);
	}

	/**
	 * コントラスト調整カーブをトーンカーブとする
	 * @param strength -1.0〜+1.0, 0: 補正無し、負ならコントラスト抑制、正ならコントラスト向上
	 * XXX 削除予定 IColorCorrectionインターフェースのkotlin拡張関数を使う
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
		final float[] curve = new float[65];
		for (int ix = 0; ix < 64; ix++) {
			final double x =  ix / 32.0 * Math.PI;	// 0〜2PI
			curve[ix] = (float)(-v * Math.sin(x));
		}
		curve[64] = curve[63];
		if (DEBUG) Log.v(TAG, "setContrast:" + Arrays.toString(curve));
		setToneCurve(curve, 0);
	}

	/**
	 * シグモイド曲線をトーンカーブとする
	 * @param k
	 * @param threshold 0.0〜1.0
	 * XXX 削除予定 IColorCorrectionインターフェースのkotlin拡張関数を使う
	 */
	public void setSigmoid(final float k, final float threshold) {
		final float[] curve = new float[65];
		for (int ix = 0; ix < 64; ix++) {
			final double x =  ix * 4.0 / 255.0;
			curve[ix] = (float)(1.0 / (1.0 + Math.pow(Math.E, -k * (x - threshold))));
		}
		curve[64] = curve[63];
		if (DEBUG) Log.v(TAG, "setSigmoid:" + Arrays.toString(curve));
		setToneCurve(curve, 0);
	}

	/**
	 * 色変換行列とトーンカーブ補正可能ななMediaEffectGLDrawer.MediaEffectSingleDrawer実装
	 * 使用できるテクスチャは1つだけ
	 */
	private static class MediaEffectGLColorCorrectionDrawer
		extends MediaEffectGLDrawer.MediaEffectSingleDrawer implements IColorCorrection {

		private final int muColorAdjustLoc;		// 色調整
		private final int muColorMatrixLoc;		// 4x4色変換行列
		private final int muParamsLoc;			// 補正パラメータ
		private float mColorAdjust;				// 色調整オフセット
		@Size(value=16)
		@NonNull
		protected final float[] mColorMatrix = new float[16];
		@Size(value= IColorCorrection.NUM_TONE_CURVE)
		@NonNull
		protected final float[] mToneCurve = new float[IColorCorrection.NUM_TONE_CURVE];

		public MediaEffectGLColorCorrectionDrawer(final String fss) {
			this(false, VERTEX_SHADER_ES2, fss);
		}

		public MediaEffectGLColorCorrectionDrawer(
			final boolean isOES, final String fss) {

			this(isOES, VERTEX_SHADER_ES2, fss);
		}

		public MediaEffectGLColorCorrectionDrawer(
			final boolean isOES, final String vss, final String fss) {

			super(isOES, vss, fss);
			int uColorAdjust = GLES20.glGetUniformLocation(getProgram(), "uColorAdjust");
			if (uColorAdjust < 0) {
				uColorAdjust = -1;
			}
			muColorAdjustLoc = uColorAdjust;
			int muColorMatrix = GLES20.glGetUniformLocation(getProgram(), "uColorMatrix");
			if (muColorMatrix < 0) {
				muColorMatrix = -1;
			}
			muColorMatrixLoc = muColorMatrix;
			int uParams = GLES20.glGetUniformLocation(getProgram(), "uParams");
			if (uParams < 0) {
				uParams = -1;
			}
			muParamsLoc = uParams;
			Matrix.setIdentityM(mColorMatrix, 0);
			setToneCurve(null, 0);
		}

		public void setColorAdjust(final float adjust) {
			synchronized (mSync) {
				mColorAdjust = adjust;
			}
		}

		@Size(value=16)
		@NonNull
		@Override
		public float[] getColorMatrix() {
			synchronized (mSync) {
				return mColorMatrix;
			}
		}

		/**
		 * 色変換行列をセット
		 * 指定したcolorMatrixがnullまたは要素数が16+offsetよりも小さい場合は単位行列になる
		 * @param colorMatrix 色変換行列
		 * @param offset
		 */
		@Override
		public void setColorMatrix(@Nullable @Size(min=16) final float[] colorMatrix, final int offset) {
			synchronized (mSync) {
				if ((colorMatrix != null) && (colorMatrix.length >= 16 + offset)) {
					System.arraycopy(colorMatrix, offset, mColorMatrix, 0, mColorMatrix.length);
				} else {
					Matrix.setIdentityM(mColorMatrix, 0);
				}
			}
		}

		@Size(value= IColorCorrection.NUM_TONE_CURVE)
		@NonNull
		@Override
		public float[] getToneCurve() {
			synchronized (mSync) {
				return mToneCurve;
			}
		}

		/**
		 * 補正パラメータをセット
		 * paramsがnullまたは必要な個数に満たない場合は0クリアする
		 * @param toneCurve
		 * @param offset
		 */
		@Override
		public void setToneCurve(@Nullable @Size(min= IColorCorrection.NUM_TONE_CURVE) final float[] toneCurve, final int offset) {
			synchronized (mSync) {
				if ((toneCurve != null) && (toneCurve.length >= mToneCurve.length + offset)) {
					System.arraycopy(toneCurve, offset, mToneCurve, 0, mToneCurve.length);
				} else {
					Arrays.fill(mToneCurve, 0.0f);
				}
			}
		}

		@Override
		protected void preDraw(
			@NonNull final int[] texIds,
			final float[] texMatrix, final int offset) {

			super.preDraw(texIds, texMatrix, offset);
			// 色調整オフセット
			if (muColorAdjustLoc >= 0) {
				synchronized (mSync) {
					GLES20.glUniform1f(muColorAdjustLoc, mColorAdjust);
				}
			}
			if (muColorMatrixLoc >= 0) {
				GLES20.glUniformMatrix4fv(muColorMatrixLoc, 1, false, mColorMatrix, 0);
			}
			final int n = mToneCurve.length;
			if ((muParamsLoc >= 0) && (n > 0)) {
				GLES20.glUniform1fv(muParamsLoc, n, mToneCurve, 0);
			}
		}
	}

}
