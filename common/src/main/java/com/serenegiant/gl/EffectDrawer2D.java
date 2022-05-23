package com.serenegiant.gl;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2022 saki t_saki@serenegiant.com
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
import android.util.Log;
import android.util.SparseArray;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;

import static com.serenegiant.gl.GLEffect.*;
import static com.serenegiant.gl.ShaderConst.*;

/**
 * 映像効果付与機能を追加したGLDrawer2D
 */
public class EffectDrawer2D extends GLDrawer2D {
	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = EffectDrawer2D.class.getSimpleName();

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
			@NonNull final GLDrawer2D drawer);
	}

	@Nullable
	private final EffectListener mEffectListener;
	@NonNull
	private final SparseArray<float[]> mParams = new SparseArray<float[]>();
	private int muParamsLoc;
	@Nullable
	private float[] mCurrentParams;
	private int mEffect;

	/**
	 * コンストラクタ
	 * 頂点シェーダーとフラグメントシェーダはデフォルトのものを使う
	 * GLコンテキストを保持したスレッド上で呼び出すこと
	 * @param isGLES3	GL|ES3を使って描画するかどうか
	 * @param isOES		外部テクスチャ(GL_TEXTURE_EXTERNAL_OES)を描画に使う場合はtrue
	 * @return
	 */
	public EffectDrawer2D(final boolean isGLES3, final boolean isOES) {
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
	public EffectDrawer2D(final boolean isGLES3, final boolean isOES,
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
	public EffectDrawer2D(final boolean isGLES3,
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
	public EffectDrawer2D(final boolean isGLES3,
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
	protected EffectDrawer2D(
		final boolean isGLES3, final boolean isOES,
		@NonNull @Size(min=8) final float[] vertices,
		@NonNull @Size(min=8) final float[] texcoord,
		@Nullable final String vs, @Nullable final String fs,
		@Nullable EffectListener effectListener) {

		super(isGLES3, isOES, vertices, texcoord, vs, fs);
		mEffectListener = effectListener;
		resetShader();
	}

	/**
	 * 頂点シェーダー・フラグメントシェーダーをデフォルトに戻す
	 */
	public void resetShader() {
		super.resetShader();
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
		resetShader();
	}

	/**
	 * 映像効果をセット
	 * GLコンテキストを保持したスレッド上で呼び出すこと
	 * 継承して独自の映像効果を追加する時はEFFECT_NUMよりも大きい値を使うこと
	 * @param effect
	 */
	public void setEffect(final int effect) {
		if (mEffect != effect) {
			mEffect = effect;
			boolean handled = false;
			try {
				handled = (mEffectListener != null)
					&& mEffectListener.onChangeEffect(effect, this);
			} catch (final Exception e) {
				if (DEBUG) Log.w(TAG, e);
			}
			if (!handled) {
				switch (effect) {
				case EFFECT_NON:
					updateShader(isGLES3
						? FRAGMENT_SHADER_EXT_ES3 : FRAGMENT_SHADER_EXT_ES2);
					break;
				case EFFECT_GRAY:
					updateShader(isGLES3
						? FRAGMENT_SHADER_EXT_GRAY_ES3 : FRAGMENT_SHADER_EXT_GRAY_ES2);
					break;
				case EFFECT_GRAY_REVERSE:
					updateShader(isGLES3
						? FRAGMENT_SHADER_GRAY_EXT_REVERSE_ES3 : FRAGMENT_SHADER_GRAY_EXT_REVERSE_ES2);
					break;
				case EFFECT_BIN:
					updateShader(isGLES3
						? FRAGMENT_SHADER_EXT_BIN_ES3 : FRAGMENT_SHADER_EXT_BIN_ES2);
					break;
				case EFFECT_BIN_YELLOW:
					updateShader(isGLES3
						?FRAGMENT_SHADER_EXT_BIN_YELLOW_ES3 : FRAGMENT_SHADER_EXT_BIN_YELLOW_ES2);
					break;
				case EFFECT_BIN_GREEN:
					updateShader(isGLES3
						? FRAGMENT_SHADER_EXT_BIN_GREEN_ES3 : FRAGMENT_SHADER_EXT_BIN_GREEN_ES2);
					break;
				case EFFECT_BIN_REVERSE:
					updateShader(isGLES3
						? FRAGMENT_SHADER_EXT_BIN_REVERSE_ES3 : FRAGMENT_SHADER_EXT_BIN_REVERSE_ES2);
					break;
				case EFFECT_BIN_REVERSE_YELLOW:
					updateShader(isGLES3
						? FRAGMENT_SHADER_EXT_BIN_REVERSE_YELLOW_ES3 : FRAGMENT_SHADER_EXT_BIN_REVERSE_YELLOW_ES2);
					break;
				case EFFECT_BIN_REVERSE_GREEN:
					updateShader(isGLES3
						? FRAGMENT_SHADER_EXT_BIN_REVERSE_GREEN_ES3 : FRAGMENT_SHADER_EXT_BIN_REVERSE_GREEN_ES2);
					break;
				case EFFECT_EMPHASIZE_RED_YELLOW:
					updateShader(isGLES3
						? FRAGMENT_SHADER_EXT_EMPHASIZE_RED_YELLOWS_ES3 : FRAGMENT_SHADER_EXT_EMPHASIZE_RED_YELLOWS_ES2);
					break;
				case EFFECT_EMPHASIZE_RED_YELLOW_WHITE:
					updateShader(isGLES3
						? FRAGMENT_SHADER_EXT_EMPHASIZE_RED_YELLOW_WHITE_ES3 : FRAGMENT_SHADER_EXT_EMPHASIZE_RED_YELLOW_WHITE_ES2);
					break;
				case EFFECT_EMPHASIZE_YELLOW_WHITE:
					updateShader(isGLES3
						? FRAGMENT_SHADER_EXT_EMPHASIZE_YELLOW_WHITE_ES3 : FRAGMENT_SHADER_EXT_EMPHASIZE_YELLOW_WHITE_ES2);
					break;
				default:
					resetShader();
					break;
				}
			}
			muParamsLoc = glGetUniformLocation("uParams");
			mCurrentParams = mParams.get(effect);
			updateParams();
		}
	}

	/**
	 * 現在の映像効果番号を取得
	 * @return
	 */
	public int getCurrentEffect() {
		return mEffect;
	}

	/**
	 * 現在選択中の映像フィルタにパラメータ配列をセット
	 * 現在対応しているのは色強調用の映像効果のみ(n=12以上必要)
	 * GLコンテキストを保持したスレッド上で呼び出すこと
	 * @param params
	 */
	public void setParams(@NonNull final float[] params) {
		setParams(mEffect, params);
	}

	/**
	 * 指定した映像フィルタにパラメータ配列をセット
	 * 現在対応しているのは色強調用の映像効果のみ(n=12以上必要)
	 * GLコンテキストを保持したスレッド上で呼び出すこと
	 * @param effect EFFECT_NONより大きいこと
	 * @param params
	 * @throws IllegalArgumentException effectが範囲外ならIllegalArgumentException生成
	 */
	public void setParams(final int effect, @NonNull final float[] params)
		throws IllegalArgumentException {

		if ((effect < EFFECT_NON) || (mEffect == effect)) {
			mCurrentParams = params;
			mParams.put(mEffect, params);
			updateParams();
		} else {
			mParams.put(effect, params);
		}
	}

	private void updateParams() {
		if (DEBUG) Log.v(TAG, "MyRendererTask#updateParams:");
		final int n = Math.min(mCurrentParams != null
			? mCurrentParams.length : 0, MAX_PARAM_NUM);
		if ((muParamsLoc >= 0) && (n > 0)) {
			glUseProgram();
			GLES20.glUniform1fv(muParamsLoc, n, mCurrentParams, 0);
		}
	}

}
