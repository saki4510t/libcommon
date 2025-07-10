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
import android.opengl.Matrix;
import android.util.Log;
import android.util.SparseArray;

import java.util.Arrays;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;
import androidx.annotation.WorkerThread;

import static com.serenegiant.gl.GLEffect.*;
import static com.serenegiant.gl.ShaderConst.*;

/**
 * 映像効果付与機能を追加したGLDrawer2D
 */
public class GLEffectDrawer2D extends GLDrawer2D implements IEffect {
	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = GLEffectDrawer2D.class.getSimpleName();

	/**
	 * GLDrawer2Dインスタンス生成用のファクトリーインターフェース
	 */
	public interface DrawerFactory extends GLDrawer2D.DrawerFactory {
		@WorkerThread
		@NonNull
		public default GLEffectDrawer2D create(final boolean isGLES3, final boolean isOES) {
			return new GLEffectDrawer2D(isGLES3, isOES);
		}
	}

	/**
	 * 映像効果セット時の処理を移譲するためのインターフェース
	 */
	public interface EffectListener {
		/**
		 * 映像効果セット
		 * @param effect
		 * @param drawer
		 * @return trueを返すとEffectDrawer2Dでの処理をしない, falseならEffectDrawer2Dでの処理を行う
		 */
		public boolean onChangeEffect(final int effect,
			@NonNull final GLEffectDrawer2D drawer);
	}

	/**
	 * デフォルトのEffectListener実装クラス
	 */
	public static class DefaultEffectListener implements EffectListener {
		@Override
		public boolean onChangeEffect(final int effect, @NonNull final GLEffectDrawer2D drawer) {
			return GLEffectDrawer2D.defaultOnChangeEffect(drawer, effect);
		}
	}

	/**
	 * EffectDrawer2D用のDrawerFactoryのデフォルト実装
	 */
	public static DrawerFactory DEFAULT_EFFECT_FACTORY = new DrawerFactory() {};

	@NonNull
	private final EffectListener mEffectListener;
	@NonNull
	private final SparseArray<float[]> mParams = new SparseArray<float[]>();
	private int muParamsLoc;
	private int muTexOffsetLoc = -1;	// テクスチャオフセット(カーネル行列用)
	private int muKernelLoc = -1;		// カーネル行列(float配列)
	private int muColorAdjustLoc = -1;		// 色調整
	private int muColorMatrixLoc = -1;		// 4x4色変換行列
	private float mColorAdjust = 0.0f;		// 色調整オフセット
	@Nullable
	private float[] mCurrentParams;
	private final float[] mTexOffset = new float[KERNEL_SIZE3x3_NUM * 2];
	@Size(value=16)
	@NonNull
	protected final float[] mColorMatrix = new float[16];
	private final float[] mKernel3x3 = new float[KERNEL_SIZE3x3_NUM * 2];	// Inputs for convolution filter based shaders
	private int mKernelSize = KERNEL_SIZE3x3_NUM;
	private int mEffect;

	/**
	 * コンストラクタ
	 * 頂点シェーダーとフラグメントシェーダはデフォルトのものを使う
	 * GLコンテキストを保持したスレッド上で呼び出すこと
	 * @param isGLES3	GL|ES3を使って描画するかどうか
	 * @param isOES		外部テクスチャ(GL_TEXTURE_EXTERNAL_OES)を描画に使う場合はtrue
	 * @return
	 */
	public GLEffectDrawer2D(final boolean isGLES3, final boolean isOES) {
		this(isGLES3, isOES, null, null, null, null, null);
	}

	/**
	 * コンストラクタ
	 * 頂点シェーダーとフラグメントシェーダはデフォルトのものを使う
	 * GLコンテキストを保持したスレッド上で呼び出すこと
	 * @param isGLES3	GL|ES3を使って描画するかどうか
	 * @param isOES    外部テクスチャ(GL_TEXTURE_EXTERNAL_OES)を描画に使う場合はtrue
	 * @param effectListener
	 * @return
	 */
	public GLEffectDrawer2D(final boolean isGLES3, final boolean isOES,
							@Nullable EffectListener effectListener) {
		this(isGLES3, isOES, null, null, null, null, effectListener);
	}

	/**
	 * コンストラクタ
	 * GLコンテキストを保持したスレッド上で呼び出すこと
	 * @param isGLES3	GL|ES3を使って描画するかどうか
	 * @param vertices	頂点座標, floatを8個 = (x,y) x 4ペア
	 * @param texcoord	テクスチャ座標, floatを8個 = (s,t) x 4ペア
	 * @param isOES		外部テクスチャ(GL_TEXTURE_EXTERNAL_OES)を描画に使う場合はtrue
	 */
	public GLEffectDrawer2D(final boolean isGLES3,
							final float[] vertices, final float[] texcoord,
							final boolean isOES) {

		this(isGLES3, isOES, vertices, texcoord, null, null, null);
	}

	/**
	 * コンストラクタ
	 * GLコンテキストを保持したスレッド上で呼び出すこと
	 * @param isGLES3	GL|ES3を使って描画するかどうか
	 * @param vertices	頂点座標, floatを8個 = (x,y) x 4ペア
	 * @param texcoord	テクスチャ座標, floatを8個 = (s,t) x 4ペア
	 * @param isOES		外部テクスチャ(GL_TEXTURE_EXTERNAL_OES)を描画に使う場合はtrue
	 * @param effectListener
	 */
	public GLEffectDrawer2D(final boolean isGLES3,
							final float[] vertices, final float[] texcoord,
							final boolean isOES,
							@Nullable EffectListener effectListener) {

		this(isGLES3, isOES, vertices, texcoord, null, null, effectListener);
	}

	/**
	 * コンストラクタ
	 * GLコンテキスト/EGLレンダリングコンテキストが有効な状態で呼ばないとダメ
	 * @param isGLES3 GL|ES3かどうか
	 * @param isOES 外部テクスチャ(GL_TEXTURE_EXTERNAL_OES)を描画に使う場合はtrue。
	 * 				通常の2Dテキスチャを描画に使うならfalse
	 * @param vertices 頂点座標, floatを8個 = (x,y) x 4ペア
	 * @param texcoord テクスチャ座標, floatを8個 = (s,t) x 4ペア
	 */
	protected GLEffectDrawer2D(
		final boolean isGLES3, final boolean isOES,
		@NonNull @Size(min=8) final float[] vertices,
		@NonNull @Size(min=8) final float[] texcoord,
		@Nullable final String vs, @Nullable final String fs,
		@Nullable EffectListener effectListener) {

		super(isGLES3, isOES, vertices, texcoord, vs, fs);
		mEffectListener = effectListener != null ? effectListener : new DefaultEffectListener();
		Matrix.setIdentityM(mColorMatrix, 0);
		setTexSize(256, 256);	// 未設定だと正常に動作しないのでデフォルトの値を入れておく
		resetShader();
	}

	@Override
	protected void init() {
		super.init();
		muKernelLoc = glGetUniformLocation("uKernel");
		muTexOffsetLoc = glGetUniformLocation("uTexOffset");
		muColorMatrixLoc = glGetUniformLocation("uColorMatrix");
		muColorAdjustLoc = glGetUniformLocation("uColorAdjust");
		if (DEBUG) Log.v(TAG, "init:uKernel=" + muKernelLoc + ",uTexOffset" + muTexOffsetLoc + ",uColorMatrix=" + muColorMatrixLoc + ",uColorAdjust=" + muColorAdjustLoc);
	}

	@Override
	protected void prepareDraw(
		final int texUnit, final int texId, @Nullable final float[] texMatrix,
		final int texOffset, @Nullable final float[] mvpMatrix, final int mvpOffset) {
		super.prepareDraw(texUnit, texId, texMatrix, texOffset, mvpMatrix, mvpOffset);
//		if (DEBUG) Log.v(TAG, "prepareDraw:");
		// テクセルオフセット
		if (muTexOffsetLoc >= 0) {
			GLES20.glUniform2fv(muTexOffsetLoc, KERNEL_SIZE3x3_NUM, mTexOffset, 0);
		}
		// カーネル関数(行列)
		if (muKernelLoc >= 0) {
			GLES20.glUniform1fv(muKernelLoc, mKernelSize, mKernel3x3, 0);
			GLUtils.checkGlError("set kernel");
		}
		// 色変換行列
		if (muColorMatrixLoc >= 0) {
			GLES20.glUniformMatrix4fv(muColorMatrixLoc, 1, false, mColorMatrix, 0);
			GLUtils.checkGlError("set color matrix");
		}
		// 色調整オフセット
		if (muColorAdjustLoc >= 0) {
			GLES20.glUniform1f(muColorAdjustLoc, mColorAdjust);
		}
	}

	/**
	 * テクスチャのサイズを設定する
	 * @param width
	 * @param height
	 */
	public synchronized void setTexSize(final int width, final int height) {
		if (DEBUG) Log.v(TAG, "setTexSize:(" + width + "x" + height + ")");
		final float rw = 1.0f / width;
		final float rh = 1.0f / height;

		mTexOffset[0] = -rw;	mTexOffset[1] = -rh;
		mTexOffset[2] = 0f;		mTexOffset[3] = -rh;
		mTexOffset[4] = rw;		mTexOffset[5] = -rh;

		mTexOffset[6] = -rw;	mTexOffset[7] = 0f;
		mTexOffset[8] = 0f;		mTexOffset[9] = 0f;
		mTexOffset[10] = rw;	mTexOffset[11] = 0f;

		mTexOffset[12] = -rw;	mTexOffset[13] = rh;
		mTexOffset[14] = 0f;	mTexOffset[15] = rh;
		mTexOffset[16] = rw;	mTexOffset[17] = rh;
	}

	public synchronized void setKernel(final float[] values, final float colorAdj) {
		if (DEBUG) Log.v(TAG, "setKernel:");
		if ((values == null) || (values.length < KERNEL_SIZE3x3_NUM)) {
			throw new IllegalArgumentException("Kernel size is "
				+ (values != null ? values.length : 0) + " vs. " + KERNEL_SIZE3x3_NUM);
		}
		if (values.length >= KERNEL_SIZE3x3_NUM * 2) {
			mKernelSize = KERNEL_SIZE3x3_NUM * 2;
		} else {
			mKernelSize = KERNEL_SIZE3x3_NUM;
		}
		System.arraycopy(values, 0, mKernel3x3, 0, mKernelSize);
		setColorAdjust(colorAdj);
	}

	public synchronized void setColorAdjust(final float adjust) {
		if (DEBUG) Log.v(TAG, "setColorAdjust:" + adjust);
		mColorAdjust = adjust;
	}

	/**
	 * 色変換行列をセット
	 * 指定したcolorMatrixがnullまたは要素数が16+offsetよりも小さい場合は単位行列になる
	 * @param colorMatrix 色変換行列
	 * @param offset
	 */
	public synchronized void setColorMatrix(@Nullable @Size(min=16) final float[] colorMatrix, final int offset) {
		if (DEBUG) Log.v(TAG, "setColorMatrix:" + Arrays.toString(colorMatrix));
		if ((colorMatrix != null) && (colorMatrix.length >= 16 + offset)) {
			System.arraycopy(colorMatrix, offset, mColorMatrix, 0, mColorMatrix.length);
		} else {
			Matrix.setIdentityM(mColorMatrix, 0);
		}
	}

	/**
	 * 頂点シェーダー・フラグメントシェーダーをデフォルトに戻す
	 */
	public void resetShader() {
		super.resetShader();
		if (DEBUG) Log.v(TAG, "resetShader:");
		mParams.clear();
		mParams.put(EFFECT_EMPHASIZE_RED_YELLOW, new float[] {
			0.17f, 0.85f,		// 赤色&黄色の色相下側閾値, 上側閾値
			0.50f, 1.0f,		// 強調する彩度下限, 上限
			0.40f, 1.0f,		// 強調する明度下限, 上限
			1.0f, 1.0f, 5.0f,	// 強調時のファクター(H, S, Vの順) 明度(x5.0) = 1.0
			1.0f, 1.0f, 1.0f,	// 通常時のファクター(H, S, Vの順)
		});
		mParams.put(EFFECT_EMPHASIZE_RED_YELLOW_WHITE, new float[] {
			0.17f, 0.85f,		// 赤色&黄色の色相下側閾値, 上側閾値
			0.50f, 1.0f,		// 強調する彩度下限, 上限
			0.40f, 1.0f,		// 強調する明度下限, 上限
			1.0f, 1.0f, 5.0f,	// 強調時のファクター(H, S, Vの順) 明度(x5.0) = 1.0
			1.0f, 1.0f, 1.0f,	// 通常時のファクター(H, S, Vの順)
		});
		mParams.put(EFFECT_EMPHASIZE_YELLOW_WHITE, new float[] {
			0.10f, 0.19f,			// 黄色の色相h下側閾値, 上側閾値
			0.30f, 1.00f,			// 強調する彩度s下限, 上限
			0.30f, 1.00f,			// 強調する明度v下限, 上限
			1.00f, 1.00f, 5.00f,	// 強調時のファクター(H, S, Vの順) 明度(x5.0) = 1.0
			1.00f, 0.80f, 0.80f,	// 通常時のファクター(H, S, Vの順) 彩度(x0.8)と明度(x0.8)を少し落とす
			0.15f, 0.40f,			// 白強調時の彩度上限, 白強調時の明度下限
			0, 0, 0, 0,				// ダミー
		});
		mEffect = EFFECT_NON;
	}

//--------------------------------------------------------------------------------
// EffectDrawer2D固有のpublicメソッド
	/**
	 * 映像効果をリセット
	 * resetShaderのシノニム
	 * GLコンテキストを保持したスレッド上で呼び出すこと
	 */
	public void resetEffect() {
		if (DEBUG) Log.v(TAG, "resetEffect:");
		resetShader();
	}

	/**
	 * 映像効果をセット
	 * GLコンテキストを保持したスレッド上で呼び出すこと
	 * 継承して独自の映像効果を追加する時はEFFECT_NUMよりも大きい値を使うこと
	 * @param effect
	 */
	@Override
	public void setEffect(final int effect) {
		if (mEffect != effect) {
			if (DEBUG) Log.v(TAG, "setEffect:" + mEffect + "=>" + effect);
			mEffect = effect;
			boolean handled = false;
			try {
				handled = mEffectListener.onChangeEffect(effect, this);
			} catch (final Exception e) {
				if (DEBUG) Log.w(TAG, e);
			}
			if (!handled) {
				defaultOnChangeEffect(this, effect);
			}
			muParamsLoc = glGetUniformLocation("uParams");
			mCurrentParams = mParams.get(effect);
			updateParams();
		}
	}

	/**
	 * IEffectの実装
	 * 現在の映像効果番号を取得
	 * @return
	 */
	@Override
	public int getEffect() {
		return mEffect;
	}

	/**
	 * IEffectの実装
	 * 現在選択中の映像フィルタにパラメータ配列をセット
	 * 現在対応しているのは色強調用の映像効果のみ(n=12以上必要)
	 * GLコンテキストを保持したスレッド上で呼び出すこと
	 * @param params
	 */
	@Override
	public void setParams(@NonNull final float[] params) {
		setParams(mEffect, params);
	}

	/**
	 * IEffectの実装
	 * 指定した映像フィルタにパラメータ配列をセット
	 * 現在対応しているのは色強調用の映像効果のみ(n=12以上必要)
	 * GLコンテキストを保持したスレッド上で呼び出すこと
	 * @param effect EFFECT_NONより小さい時は現在選択中の映像フィルタにパラメータ配列をセットする
	 * @param params
	 * @throws IllegalArgumentException effectが範囲外ならIllegalArgumentException生成
	 */
	@Override
	public void setParams(final int effect, @NonNull final float[] params)
		throws IllegalArgumentException {

		if (DEBUG) Log.v(TAG, "setParams:effect=" + effect + ",params=" + Arrays.toString(params));
		if ((effect < EFFECT_NON) || (mEffect == effect)) {
			mCurrentParams = params;
			mParams.put(mEffect, params);
			updateParams();
		} else {
			mParams.put(effect, params);
		}
	}

	private void updateParams() {
		if (DEBUG) Log.v(TAG, "updateParams:");
		final int n = Math.min(mCurrentParams != null
			? mCurrentParams.length : 0, 18);
		if ((muParamsLoc >= 0) && (n > 0)) {
			glUseProgram();
			GLES20.glUniform1fv(muParamsLoc, n, mCurrentParams, 0);
		}
	}

	/**
	 * デフォルトの内蔵映像効果切り替え処理
	 * @param drawer
	 * @param effect
	 * @return
	 */
	private static boolean defaultOnChangeEffect(@NonNull final GLEffectDrawer2D drawer, final int effect) {
		if (DEBUG) Log.i(TAG, "defaultOnChangeEffect:" + effect);
		final boolean isGLES3 = drawer.isGLES3;
		final boolean isOES = drawer.isOES();
		boolean handled = true;
		switch (effect) {
		case EFFECT_NON -> drawer.updateShader(drawer.isGLES3
			? (isOES ? FRAGMENT_SHADER_EXT_ES3 : FRAGMENT_SHADER_ES3)
			: (isOES ? FRAGMENT_SHADER_EXT_ES2 : FRAGMENT_SHADER_ES2));
		case EFFECT_GRAY -> drawer.updateShader(isGLES3
			? (isOES ? FRAGMENT_SHADER_EXT_GRAY_ES3 : FRAGMENT_SHADER_GRAY_ES3)
			: (isOES ? FRAGMENT_SHADER_EXT_GRAY_ES2 : FRAGMENT_SHADER_GRAY_ES2));
		case EFFECT_GRAY_REVERSE -> drawer.updateShader(isGLES3
			? (isOES ? FRAGMENT_SHADER_GRAY_EXT_REVERSE_ES3 : FRAGMENT_SHADER_GRAY_REVERSE_ES3)
			: (isOES ? FRAGMENT_SHADER_GRAY_EXT_REVERSE_ES2 : FRAGMENT_SHADER_GRAY_REVERSE_ES2));
		case EFFECT_BIN -> drawer.updateShader(isGLES3
			? (isOES ? FRAGMENT_SHADER_EXT_BIN_ES3 : FRAGMENT_SHADER_BIN_ES3)
			: (isOES ? FRAGMENT_SHADER_EXT_BIN_ES2 : FRAGMENT_SHADER_BIN_ES2));
		case EFFECT_BIN_YELLOW -> drawer.updateShader(isGLES3
			? (isOES ? FRAGMENT_SHADER_EXT_BIN_YELLOW_ES3 : FRAGMENT_SHADER_BIN_YELLOW_ES3)
			: (isOES ? FRAGMENT_SHADER_EXT_BIN_YELLOW_ES2 : FRAGMENT_SHADER_BIN_YELLOW_ES2));
		case EFFECT_BIN_GREEN -> drawer.updateShader(isGLES3
			? (isOES ? FRAGMENT_SHADER_EXT_BIN_GREEN_ES3 : FRAGMENT_SHADER_BIN_GREEN_ES3)
			: (isOES ? FRAGMENT_SHADER_EXT_BIN_GREEN_ES2 : FRAGMENT_SHADER_BIN_GREEN_ES2));
		case EFFECT_BIN_REVERSE -> drawer.updateShader(isGLES3
			? (isOES ? FRAGMENT_SHADER_EXT_BIN_REVERSE_ES3 : FRAGMENT_SHADER_BIN_REVERSE_ES3)
			: (isOES ? FRAGMENT_SHADER_EXT_BIN_REVERSE_ES2 : FRAGMENT_SHADER_BIN_REVERSE_ES2));
		case EFFECT_BIN_REVERSE_YELLOW -> drawer.updateShader(isGLES3
			? (isOES ? FRAGMENT_SHADER_EXT_BIN_REVERSE_YELLOW_ES3 : FRAGMENT_SHADER_BIN_REVERSE_YELLOW_ES3)
			: (isOES ? FRAGMENT_SHADER_EXT_BIN_REVERSE_YELLOW_ES2 : FRAGMENT_SHADER_BIN_REVERSE_YELLOW_ES2));
		case EFFECT_BIN_REVERSE_GREEN -> drawer.updateShader(isGLES3
			? (isOES ? FRAGMENT_SHADER_EXT_BIN_REVERSE_GREEN_ES3 : FRAGMENT_SHADER_BIN_REVERSE_GREEN_ES3)
			: (isOES ? FRAGMENT_SHADER_EXT_BIN_REVERSE_GREEN_ES2 : FRAGMENT_SHADER_BIN_REVERSE_GREEN_ES2));
		case EFFECT_EMPHASIZE_RED_YELLOW -> {
			drawer.updateShader(isGLES3
				? (isOES ? FRAGMENT_SHADER_EXT_EMPHASIZE_RED_YELLOWS_ES3 : FRAGMENT_SHADER_EMPHASIZE_RED_YELLOWS_ES3)
				: (isOES ? FRAGMENT_SHADER_EXT_EMPHASIZE_RED_YELLOWS_ES2 : FRAGMENT_SHADER_EMPHASIZE_RED_YELLOWS_ES2));
			drawer.setParams(effect, new float[] {
				0.17f, 0.85f,			// 赤色&黄色の色相下側閾値, 上側閾値
				0.50f, 1.0f,			// 強調する彩度下限, 上限
				0.40f, 1.0f,			// 強調する明度下限, 上限
				1.0f, 1.0f, 5.0f,		// 強調時のファクター(H, S, Vの順) 明度(x5.0) = 1.0
				1.0f, 1.0f, 1.0f,		// 通常時のファクター(H, S, Vの順)
			});
		}
		case EFFECT_EMPHASIZE_RED_YELLOW_WHITE -> {
			drawer.updateShader(isGLES3
				? (isOES ? FRAGMENT_SHADER_EXT_EMPHASIZE_RED_YELLOW_WHITE_ES3 : FRAGMENT_SHADER_EMPHASIZE_RED_YELLOW_WHITE_ES3)
				: (isOES ? FRAGMENT_SHADER_EXT_EMPHASIZE_RED_YELLOW_WHITE_ES2 : FRAGMENT_SHADER_EMPHASIZE_RED_YELLOW_WHITE_ES2));
			drawer.setParams(effect, new float[] {
				0.17f, 0.85f,			// 赤色&黄色の色相下側閾値, 上側閾値
				0.50f, 1.0f,			// 強調する彩度下限, 上限
				0.40f, 1.0f,			// 強調する明度下限, 上限
				1.0f, 1.0f, 5.0f,		// 強調時のファクター(H, S, Vの順) 明度(x5.0) = 1.0
				1.0f, 1.0f, 1.0f,		// 通常時のファクター(H, S, Vの順)
			});
		}
		case EFFECT_EMPHASIZE_YELLOW_WHITE -> {
			drawer.updateShader(isGLES3
				? (isOES ? FRAGMENT_SHADER_EXT_EMPHASIZE_YELLOW_WHITE_ES3 : FRAGMENT_SHADER_EMPHASIZE_YELLOW_WHITE_ES3)
				: (isOES ? FRAGMENT_SHADER_EXT_EMPHASIZE_YELLOW_WHITE_ES2 : FRAGMENT_SHADER_EMPHASIZE_YELLOW_WHITE_ES2));
			drawer.setParams(effect, new float[] {
				0.10f, 0.19f,			// 黄色の色相h下側閾値, 上側閾値
				0.30f, 1.00f,			// 強調する彩度s下限, 上限
				0.30f, 1.00f,			// 強調する明度v下限, 上限
				1.00f, 1.00f, 5.00f,	// 強調時のファクター(H, S, Vの順) 明度(x5.0) = 1.0
				1.00f, 0.80f, 0.80f,	// 通常時のファクター(H, S, Vの順) 彩度(x0.8)と明度(x0.8)を少し落とす
				0.15f, 0.40f,			// 白強調時の彩度上限, 白強調時の明度下限
				0, 0, 0, 0,				// ダミー
			});
		}
		case EFFECT_ADAPTIVE_BIN -> drawer.updateShader(isGLES3
			? (isOES ? FRAGMENT_SHADER_EXT_ADAPTIVE_BIN_ES3 : FRAGMENT_SHADER_ADAPTIVE_BIN_ES3)
			: (isOES ? FRAGMENT_SHADER_EXT_ADAPTIVE_BIN_ES2 : FRAGMENT_SHADER_ADAPTIVE_BIN_ES2));
		case EFFECT_ADAPTIVE_BIN_YELLOW -> drawer.updateShader(isGLES3
			? (isOES ? FRAGMENT_SHADER_EXT_ADAPTIVE_BIN_YELLOW_ES3 : FRAGMENT_SHADER_ADAPTIVE_BIN_YELLOW_ES3)
			: (isOES ? FRAGMENT_SHADER_EXT_ADAPTIVE_BIN_YELLOW_ES2 : FRAGMENT_SHADER_ADAPTIVE_BIN_YELLOW_ES2));
		case EFFECT_ADAPTIVE_BIN_GREEN -> drawer.updateShader(isGLES3
			? (isOES ? FRAGMENT_SHADER_EXT_ADAPTIVE_BIN_GREEN_ES3 : FRAGMENT_SHADER_ADAPTIVE_BIN_GREEN_ES3)
			: (isOES ? FRAGMENT_SHADER_EXT_ADAPTIVE_BIN_GREEN_ES2 : FRAGMENT_SHADER_ADAPTIVE_BIN_GREEN_ES2));
		default -> {
			handled = onChangeKernel(drawer, effect);
		}
		}
		return handled;
	}

	private static boolean onChangeKernel(@NonNull final GLEffectDrawer2D drawer, final int effect) {
		if (DEBUG) Log.v(TAG, "onChangeKernel:effect=" + effect);
		final boolean isGLES3 = drawer.isGLES3;
		final boolean isOES = drawer.isOES();
		float[] kernel = null;
		boolean handled = true;
		int shaderType = 0;
		switch (effect) {
		case EFFECT_KERNEL_SOBEL_H -> {
			/*if (DEBUG)*/ Log.v(TAG, "onChangeKernel:EFFECT_KERNEL_SOBEL_H");
			shaderType = 1;
			kernel = KERNEL_SOBEL_H;
		}
		case EFFECT_KERNEL_SOBEL_V -> {
			/*if (DEBUG)*/ Log.v(TAG, "onChangeKernel:EFFECT_KERNEL_SOBEL_V");
			shaderType = 1;
			kernel = KERNEL_SOBEL_V;
		}
		case EFFECT_KERNEL_SOBEL_HV -> {
			/*if (DEBUG)*/ Log.v(TAG, "onChangeKernel:EFFECT_KERNEL_SOBEL_HV");
			shaderType = 2;
			kernel = concat(KERNEL_SOBEL_H, KERNEL_SOBEL_V);
		}
		case EFFECT_KERNEL_SOBEL2_H -> {
			/*if (DEBUG)*/ Log.v(TAG, "onChangeKernel:EFFECT_KERNEL_SOBEL2_H");
			shaderType = 1;
			kernel = KERNEL_SOBEL2_H;
		}
		case EFFECT_KERNEL_SOBEL2_V -> {
			/*if (DEBUG)*/ Log.v(TAG, "onChangeKernel:EFFECT_KERNEL_SOBEL2_V");
			shaderType = 1;
			kernel = KERNEL_SOBEL2_V;
		}
		case EFFECT_KERNEL_SOBEL2_HV -> {
			/*if (DEBUG)*/ Log.v(TAG, "onChangeKernel:EFFECT_KERNEL_SOBEL2_HV");
			shaderType = 2;
			kernel = concat(KERNEL_SOBEL2_H, KERNEL_SOBEL2_V);
		}
		case EFFECT_KERNEL_PREWITT_H -> {
			/*if (DEBUG)*/ Log.v(TAG, "onChangeKernel:EFFECT_KERNEL_PREWITT_H");
			shaderType = 1;
			kernel = KERNEL_PREWITT_H;
		}
		case EFFECT_KERNEL_PREWITT_V -> {
			/*if (DEBUG)*/ Log.v(TAG, "onChangeKernel:EFFECT_KERNEL_PREWITT_V");
			shaderType = 1;
			kernel = KERNEL_PREWITT_V;
		}
		case EFFECT_KERNEL_PREWITT_HV -> {
			/*if (DEBUG)*/ Log.v(TAG, "onChangeKernel:EFFECT_KERNEL_PREWITT_HV");
			shaderType = 2;
			kernel = concat(KERNEL_PREWITT_H, KERNEL_PREWITT_V);
		}
		case EFFECT_KERNEL_ROBERTS_H -> {
			/*if (DEBUG)*/ Log.v(TAG, "onChangeKernel:EFFECT_KERNEL_ROBERTS_H");
			shaderType = 1;
			kernel = KERNEL_ROBERTS_H;
		}
		case EFFECT_KERNEL_ROBERTS_V -> {
			/*if (DEBUG)*/ Log.v(TAG, "onChangeKernel:EFFECT_KERNEL_ROBERTS_V");
			shaderType = 1;
			kernel = KERNEL_ROBERTS_V;
		}
		case EFFECT_KERNEL_ROBERTS_HV -> {
			/*if (DEBUG)*/ Log.v(TAG, "onChangeKernel:EFFECT_KERNEL_ROBERTS_HV");
			shaderType = 2;
			kernel = concat(KERNEL_ROBERTS_H, KERNEL_ROBERTS_V);
		}
		case EFFECT_KERNEL_SHARPNESS -> {	// = EFFECT_KERNEL_EDGE_ENHANCE4
			/*if (DEBUG)*/ Log.v(TAG, "onChangeKernel:EFFECT_KERNEL_SHARPNESS");
			kernel = KERNEL_SHARPNESS;
		}
		case EFFECT_KERNEL_EDGE_ENHANCE8 -> {
			/*if (DEBUG)*/ Log.v(TAG, "onChangeKernel:EFFECT_KERNEL_EDGE_ENHANCE8");
			kernel = KERNEL_EDGE_ENHANCE8;
		}
		case EFFECT_KERNEL_EDGE_DETECT -> {
			/*if (DEBUG)*/ Log.v(TAG, "onChangeKernel:EFFECT_KERNEL_EDGE_DETECT");
			kernel = KERNEL_EDGE_DETECT;
		}
		case EFFECT_KERNEL_EMBOSS -> {
			/*if (DEBUG)*/ Log.v(TAG, "onChangeKernel:EFFECT_KERNEL_EMBOSS");
			kernel = KERNEL_EMBOSS;
		}
		case EFFECT_KERNEL_SMOOTH -> {
			/*if (DEBUG)*/ Log.v(TAG, "onChangeKernel:EFFECT_KERNEL_SMOOTH");
			kernel = KERNEL_SMOOTH;
		}
		case EFFECT_KERNEL_GAUSSIAN -> {
			/*if (DEBUG)*/ Log.v(TAG, "onChangeKernel:EFFECT_KERNEL_GAUSSIAN");
			kernel = KERNEL_GAUSSIAN;
		}
		case EFFECT_KERNEL_BRIGHTEN -> {
			/*if (DEBUG)*/ Log.v(TAG, "onChangeKernel:EFFECT_KERNEL_BRIGHTEN");
			kernel = KERNEL_BRIGHTEN;
		}
		case EFFECT_KERNEL_LAPLACIAN8 -> {
			/*if (DEBUG)*/ Log.v(TAG, "onChangeKernel:EFFECT_KERNEL_LAPLACIAN8");
			kernel = KERNEL_LAPLACIAN8;
		}
		case EFFECT_KERNEL_LAPLACIAN4 -> {
			/*if (DEBUG)*/ Log.v(TAG, "onChangeKernel:EFFECT_KERNEL_LAPLACIAN4");
			kernel = KERNEL_LAPLACIAN4;
		}
		case EFFECT_KERNEL_CANNY -> {
			/*if (DEBUG)*/ Log.v(TAG, "onChangeKernel:EFFECT_KERNEL_CANNY");
			shaderType = -1;
			drawer.updateShader(isGLES3
				? (isOES ? FRAGMENT_SHADER_EXT_CANNY_ES3 : FRAGMENT_SHADER_CANNY_ES3)
				: (isOES ? FRAGMENT_SHADER_EXT_CANNY_ES2 : FRAGMENT_SHADER_CANNY_ES2));
		}
		case EFFECT_KERNEL_CANNY_ENHANCE -> {
			/*if (DEBUG)*/ Log.v(TAG, "onChangeKernel:EFFECT_KERNEL_CANNY_ENHANCE");
			shaderType = -1;
			drawer.updateShader(isGLES3
				? (isOES ? FRAGMENT_SHADER_EXT_CANNY_ENHANCE_ES3 : FRAGMENT_SHADER_CANNY_ENHANCE_ES3)
				: (isOES ? FRAGMENT_SHADER_EXT_CANNY_ENHANCE_ES2 : FRAGMENT_SHADER_CANNY_ENHANCE_ES2));
			drawer.setColorAdjust(0.5f);
		}
		case EFFECT_KERNEL_KERNEL_ENHANCE -> {
			/*if (DEBUG)*/ Log.v(TAG, "onChangeKernel:EFFECT_KERNEL_KERNEL_ENHANCE");
			shaderType = -1;
			drawer.updateShader(isGLES3
				? (isOES ? FRAGMENT_SHADER_EXT_KERNEL_ENHANCE_ES3 : FRAGMENT_SHADER_KERNEL_ENHANCE_ES3)
				: (isOES ? FRAGMENT_SHADER_EXT_KERNEL_ENHANCE_ES2 : FRAGMENT_SHADER_KERNEL_ENHANCE_ES2));
			kernel = KERNEL_SOBEL_V;
			drawer.setColorAdjust(0.25f);
		}
		default -> {
			drawer.resetShader();
			handled = false;
		}
		}
		if (handled) {
			if (shaderType == 1) {
				if (DEBUG) Log.v(TAG, "onChangeKernel:set kernel edge detect shader");
				drawer.updateShader(isGLES3
					? (isOES ? FRAGMENT_SHADER_EXT_KERNEL3x3_EDGE_DETECT_ES3 : FRAGMENT_SHADER_KERNEL3x3_EDGE_DETECT_ES3)
					: (isOES ? FRAGMENT_SHADER_EXT_KERNEL3x3_EDGE_DETECT_ES2 : FRAGMENT_SHADER_KERNEL3x3_EDGE_DETECT_ES2));
			} else if (shaderType == 2) {
				if (DEBUG) Log.v(TAG, "onChangeKernel:set kernel edge detect(horizontal+vertical) shader");
				drawer.updateShader(isGLES3
					? (isOES ? FRAGMENT_SHADER_EXT_KERNEL3x3_EDGE_DETECT_HV_ES3 : FRAGMENT_SHADER_KERNEL3x3_EDGE_DETECT_HV_ES3)
					: (isOES ? FRAGMENT_SHADER_EXT_KERNEL3x3_EDGE_DETECT_HV_ES2 : FRAGMENT_SHADER_KERNEL3x3_EDGE_DETECT_HV_ES2));

			} else if (shaderType >= 0) {
				if (DEBUG) Log.v(TAG, "onChangeKernel:set normal 3x3 kernel shader");
				drawer.updateShader(isGLES3
					? (isOES ? FRAGMENT_SHADER_EXT_FILT3x3_ES3 : FRAGMENT_SHADER_FILT3x3_ES3)
					: (isOES ? FRAGMENT_SHADER_EXT_FILT3x3_ES2 : FRAGMENT_SHADER_FILT3x3_ES2));
			}
		}
		if (handled && (kernel != null)) {
			if (DEBUG) Log.v(TAG, "onChangeKernel:set kernel");
			drawer.setKernel(kernel, drawer.mColorAdjust);
		}
		return handled;
	}

	@NonNull
	private static float[] concat(@NonNull final float[] array1, @NonNull final float[] array2) {
		final int n = array1.length + array2.length;
		final float[] result = new float[n];
		System.arraycopy(array1, 0, result, 0, array1.length);
		System.arraycopy(array2, 0, result, array1.length, array2.length);
		return result;
	}
}

