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

import android.opengl.GLES20;
import android.opengl.GLES30;
import android.util.Log;
import android.util.SparseArray;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static com.serenegiant.glutils.GLEffect.*;
import static com.serenegiant.glutils.ShaderConst.*;

/**
 * GLDrawerの継承クラスを使って映像効果を付与するためのヘルパークラス
 * このクラス自体はGLDrawer2Dの継承クラスではない
 */
public class EffectDrawer2D {
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

	@NonNull
	private final GLDrawer2D mDrawer;
	@Nullable
	private final EffectListener mEffectListener;
	private final SparseArray<float[]> mParams = new SparseArray<float[]>();
	private int muParamsLoc;
	private float[] mCurrentParams;
	private int mEffect;

	/**
	 * コンストラクタ
	 * 頂点シェーダーとフラグメントシェーダはデフォルトのものを使う
	 * @param isOES
	 * @return
	 */
	public EffectDrawer2D(final boolean isGLES3, final boolean isOES) {
		this(GLDrawer2D.create(isGLES3, isOES), null);
	}

	/**
	 * コンストラクタ
	 * 頂点シェーダーとフラグメントシェーダはデフォルトのものを使う
	 * @param isGLES3
	 * @param isOES
	 * @param effectListener
	 * @return
	 */
	public EffectDrawer2D(final boolean isGLES3, final boolean isOES,
		@Nullable EffectListener effectListener) {
		this(GLDrawer2D.create(isGLES3, isOES), effectListener);
	}

	/**
	 * コンストラクタ
	 * GLコンテキスト/EGLレンダリングコンテキストが有効な状態で呼ばないとダメ
	 *
	 * @param isGLES3
	 * @param vertices 頂点座標, floatを8個 = (x,y) x 4ペア
	 * @param texcoord テクスチャ座標, floatを8個 = (s,t) x 4ペア
	 * @param isOES    外部テクスチャ(GL_TEXTURE_EXTERNAL_OES)を描画に使う場合はtrue
	 */
	public EffectDrawer2D(final boolean isGLES3,
		final float[] vertices, final float[] texcoord,
		final boolean isOES) {

		this(GLDrawer2D.create(isGLES3, vertices, texcoord, isOES), null);
	}

	/**
	 * コンストラクタ
	 * GLコンテキスト/EGLレンダリングコンテキストが有効な状態で呼ばないとダメ
	 *
	 * @param isGLES3
	 * @param vertices 頂点座標, floatを8個 = (x,y) x 4ペア
	 * @param texcoord テクスチャ座標, floatを8個 = (s,t) x 4ペア
	 * @param isOES    外部テクスチャ(GL_TEXTURE_EXTERNAL_OES)を描画に使う場合はtrue
	 * @param effectListener
	 */
	public EffectDrawer2D(final boolean isGLES3,
		final float[] vertices, final float[] texcoord,
		final boolean isOES,
		@Nullable EffectListener effectListener) {

		this(GLDrawer2D.create(isGLES3, vertices, texcoord, isOES), effectListener);
	}

	/**
	 * コンストラクタ
	 * 既に生成済みのGLDrawer2Dインスタンスを使う時
	 * @param drawer
	 */
	public EffectDrawer2D(@NonNull final GLDrawer2D drawer) {
		this(drawer, null);
	}

	/**
	 * コンストラクタ
	 * 既に生成済みのGLDrawer2Dインスタンスを使う時
	 * @param drawer
	 * @param effectListener
	 */
	public EffectDrawer2D(@NonNull final GLDrawer2D drawer,
		@Nullable EffectListener effectListener) {
		mDrawer = drawer;
		mEffectListener = effectListener;
		resetShader();
	}

//--------------------------------------------------------------------------------
// GLDrawerへの委譲メソッド

	/**
	 * 破棄処理。GLコンテキスト/EGLレンダリングコンテキスト内で呼び出さないとダメ
	 * IDrawer2Dの実装
	 */
	@CallSuper
	public void release() {
		mDrawer.release();
	}

	/**
	 * 外部テクスチャを使うかどうか
	 * IShaderDrawer2dの実装
	 * @return
	 */
	public boolean isOES() {
		return mDrawer.isOES();
	}

	/**
	 * モデルビュー変換行列を取得(内部配列を直接返すので変更時は要注意)
	 * IDrawer2Dの実装
	 * @return
	 */
	@NonNull
	public float[] getMvpMatrix() {
		return mDrawer.getMvpMatrix();
	}

	/**
	 * モデルビュー変換行列に行列を割り当てる
	 * IDrawer2Dの実装
	 * @param matrix 領域チェックしていないのでoffsetから16個以上必須
	 * @param offset
	 * @return
	 */
	public GLDrawer2D setMvpMatrix(@NonNull final float[] matrix, final int offset) {
		return mDrawer;
	}

	/**
	 * モデルビュー変換行列のコピーを取得
	 * IDrawer2Dの実装
	 * @param matrix 領域チェックしていないのでoffsetから16個以上必須
	 * @param offset
	 */
	public void copyMvpMatrix(@NonNull final float[] matrix, final int offset) {
		mDrawer.copyMvpMatrix(matrix, offset);
	}

	/**
	 * モデルビュー変換行列に左右・上下反転をセット
	 * @param mirror
	 */
	public void setMirror(@IRendererCommon.MirrorMode final int mirror) {
		mDrawer.setMirror(mirror);
	}

	/**
	 * 現在のモデルビュー変換行列をxy平面で指定した角度回転させる
	 * @param degrees
	 */
	public void rotate(final int degrees) {
		mDrawer.setMirror(degrees);
	}

	/**
	 * モデルビュー変換行列にxy平面で指定した角度回転させた回転行列をセットする
	 * @param degrees
	 */
	public void setRotation(final int degrees) {
		mDrawer.setRotation(degrees);
	}

	/**
	 * IGLSurfaceオブジェクトを描画するためのヘルパーメソッド
	 * IGLSurfaceオブジェクトで管理しているテクスチャ名とテクスチャ座標変換行列を使って描画する
	 * IDrawer2Dの実装
	 * @param surface
	 */
	public void draw(@NonNull final IGLSurface surface) {
		mDrawer.draw(surface);
	}

	/**
	 * 描画処理
	 * @param texId
	 * @param texMatrix
	 * @param offset
	 */
	public synchronized void draw(final int texId,
		@Nullable final float[] texMatrix, final int offset) {

		mDrawer.draw(texId, texMatrix, offset);
	}

	/**
	 * 描画処理
	 * @param texId
	 * @param texMatrix
	 * @param texOffset
	 * @param mvpMatrix
	 * @param mvpOffset
	 */
	public synchronized void draw(final int texId,
		@Nullable final float[] texMatrix, final int texOffset,
		@Nullable final float[] mvpMatrix, final int mvpOffset) {

		mDrawer.draw(GLES20.GL_TEXTURE0, texId, texMatrix, texOffset, mvpMatrix, mvpOffset);
	}

	/**
	 * 描画処理
	 * @param texUnit
	 * @param texId
	 * @param texMatrix
	 * @param texOffset
	 * @param mvpMatrix
	 * @param mvpOffset
	 */
	public synchronized void draw(
		final int texUnit, final int texId,
		@Nullable final float[] texMatrix, final int texOffset,
		@Nullable final float[] mvpMatrix, final int mvpOffset) {

		mDrawer.draw(texUnit, texId, texMatrix, texOffset, mvpMatrix, mvpOffset);
	}

	/**
	 * テクスチャ変換行列をセット
	 * @param texMatrix
	 * @param offset
	 */
	protected void updateTexMatrix(final float[] texMatrix, final int offset) {
		mDrawer.updateTexMatrix(texMatrix, offset);
	}

	/**
	 * モデルビュー変換行列をセット
	 * @param mvpMatrix
	 */
	protected void updateMvpMatrix(final float[] mvpMatrix, final int offset) {
		mDrawer.updateMvpMatrix(mvpMatrix, offset);
	}

	/**
	 * テクスチャをバインド
	 * @param texId
	 */
	protected void bindTexture(final int texId) {
		mDrawer.bindTexture(GLES20.GL_TEXTURE0, texId);
	}

	/**
	 * テクスチャをバインド
	 * @param texUnit
	 * @param texId
	 */
	protected void bindTexture(final int texUnit, final int texId) {
		mDrawer.bindTexture(texUnit, texId);
	}

	/**
	 * 頂点座標をセット
	 */
	protected void updateVertices() {
		mDrawer.updateVertices();
	}

	/**
	 * 描画実行
	 */
	protected void drawVertices() {
		mDrawer.drawVertices();
	}

	/**
	 * 描画の後処理
	 */
	protected void finishDraw() {
		mDrawer.finishDraw();
	}

	/**
	 * テクスチャ名生成のヘルパーメソッド
	 * GLHelper#initTexを呼び出すだけ
	 * @return texture ID
	 */
	public int initTex() {
		return mDrawer.initTex();
	}

	/**
	 * テクスチャ名生成のヘルパーメソッド
	 * GLHelper#initTexを呼び出すだけ
	 * @param texUnit
	 * @param filterParam
	 * @return
	 */
	public int initTex(final int texUnit, final int filterParam) {
		return mDrawer.initTex(texUnit, filterParam);
	}

	/**
	 * テクスチャ名破棄のヘルパーメソッド
	 * GLHelper.deleteTexを呼び出すだけ
	 * @param hTex
	 */
	public void deleteTex(final int hTex) {
		mDrawer.deleteTex(hTex);
	}

	/**
	 * 頂点シェーダー・フラグメントシェーダーを変更する
	 * GLコンテキスト/EGLレンダリングコンテキスト内で呼び出さないとダメ
	 * glUseProgramが呼ばれた状態で返る
	 * @param vs 頂点シェーダー文字列
	 * @param fs フラグメントシェーダー文字列
	 */
	public synchronized void updateShader(@NonNull final String vs, @NonNull final String fs) {
		mDrawer.updateShader(vs, fs);
	}

	/**
	 * シェーダーを破棄
	 */
	protected void releaseShader() {
		mDrawer.releaseShader();
	}

	protected int loadShader(@NonNull final String vs, @NonNull final String fs) {
		return mDrawer.loadShader(vs, fs);
	}

	protected void internalReleaseShader(final int program) {
		mDrawer.internalReleaseShader(program);
	}

	/**
	 * フラグメントシェーダーを変更する
	 * GLコンテキスト/EGLレンダリングコンテキスト内で呼び出さないとダメ
	 * glUseProgramが呼ばれた状態で返る
	 * @param fs フラグメントシェーダー文字列
	 */
	public void updateShader(@NonNull final String fs) {
		mDrawer.updateShader(fs);
	}

	/**
	 * 頂点シェーダー・フラグメントシェーダーをデフォルトに戻す
	 */
	public void resetShader() {
		mDrawer.resetShader();
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

	/**
	 * アトリビュート変数のロケーションを取得
	 * glUseProgramが呼ばれた状態で返る
	 * @param name
	 * @return
	 */
	public int glGetAttribLocation(@NonNull final String name) {
		return mDrawer.glGetAttribLocation(name);
	}

	/**
	 * ユニフォーム変数のロケーションを取得
	 * glUseProgramが呼ばれた状態で返る
	 * @param name
	 * @return
	 */
	public int glGetUniformLocation(@NonNull final String name) {
		return mDrawer.glGetUniformLocation(name);
	}

	/**
	 * glUseProgramが呼ばれた状態で返る
	 */
	public void glUseProgram() {
		mDrawer.glUseProgram();
	}

	/**
	 * シェーダープログラム変更時の初期化処理
	 * glUseProgramが呼ばれた状態で返る
	 */
	protected void init() {
		mDrawer.init();
	}

	/**
	 * シェーダープログラムが使用可能かどうかをチェック
	 * @param program
	 * @return
	 */
	protected boolean validateProgram(final int program) {
		return mDrawer.validateProgram(program);
	}

//--------------------------------------------------------------------------------
// EffectDrawer2D固有のpublicメソッド
	/**
	 * 映像効果をリセット
	 * resetShaderのシノニム
	 */
	public void resetEffect() {
		resetShader();
	}

	/**
	 * 映像効果をセット
	 * 継承して独自の映像効果を追加する時はEFFECT_NUMよりも大きい値を使うこと
	 * @param effect
	 */
	public void setEffect(final int effect) {
		if (mEffect != effect) {
			mEffect = effect;
			boolean handled = false;
			try {
				handled = (mEffectListener != null)
					&& mEffectListener.onChangeEffect(effect, mDrawer);
			} catch (final Exception e) {
				if (DEBUG) Log.w(TAG, e);
			}
			if (!handled) {
				switch (effect) {
				case EFFECT_NON:
					mDrawer.updateShader(mDrawer.isGLES3
						? FRAGMENT_SHADER_EXT_ES3 : FRAGMENT_SHADER_EXT_ES2);
					break;
				case EFFECT_GRAY:
					mDrawer.updateShader(mDrawer.isGLES3
						? FRAGMENT_SHADER_EXT_GRAY_ES3 : FRAGMENT_SHADER_EXT_GRAY_ES2);
					break;
				case EFFECT_GRAY_REVERSE:
					mDrawer.updateShader(mDrawer.isGLES3
						? FRAGMENT_SHADER_GRAY_EXT_REVERSE_ES3 : FRAGMENT_SHADER_GRAY_EXT_REVERSE_ES2);
					break;
				case EFFECT_BIN:
					mDrawer.updateShader(mDrawer.isGLES3
						? FRAGMENT_SHADER_EXT_BIN_ES3 : FRAGMENT_SHADER_EXT_BIN_ES2);
					break;
				case EFFECT_BIN_YELLOW:
					mDrawer.updateShader(mDrawer.isGLES3
						?FRAGMENT_SHADER_EXT_BIN_YELLOW_ES3 : FRAGMENT_SHADER_EXT_BIN_YELLOW_ES2);
					break;
				case EFFECT_BIN_GREEN:
					mDrawer.updateShader(mDrawer.isGLES3
						? FRAGMENT_SHADER_EXT_BIN_GREEN_ES3 : FRAGMENT_SHADER_EXT_BIN_GREEN_ES2);
					break;
				case EFFECT_BIN_REVERSE:
					mDrawer.updateShader(mDrawer.isGLES3
						? FRAGMENT_SHADER_EXT_BIN_REVERSE_ES3 : FRAGMENT_SHADER_EXT_BIN_REVERSE_ES2);
					break;
				case EFFECT_BIN_REVERSE_YELLOW:
					mDrawer.updateShader(mDrawer.isGLES3
						? FRAGMENT_SHADER_EXT_BIN_REVERSE_YELLOW_ES3 : FRAGMENT_SHADER_EXT_BIN_REVERSE_YELLOW_ES2);
					break;
				case EFFECT_BIN_REVERSE_GREEN:
					mDrawer.updateShader(mDrawer.isGLES3
						? FRAGMENT_SHADER_EXT_BIN_REVERSE_GREEN_ES3 : FRAGMENT_SHADER_EXT_BIN_REVERSE_GREEN_ES2);
					break;
				case EFFECT_EMPHASIZE_RED_YELLOW:
					mDrawer.updateShader(mDrawer.isGLES3
						? FRAGMENT_SHADER_EXT_EMPHASIZE_RED_YELLOWS_ES3 : FRAGMENT_SHADER_EXT_EMPHASIZE_RED_YELLOWS_ES2);
					break;
				case EFFECT_EMPHASIZE_RED_YELLOW_WHITE:
					mDrawer.updateShader(mDrawer.isGLES3
						? FRAGMENT_SHADER_EXT_EMPHASIZE_RED_YELLOW_WHITE_ES3 : FRAGMENT_SHADER_EXT_EMPHASIZE_RED_YELLOW_WHITE_ES2);
					break;
				case EFFECT_EMPHASIZE_YELLOW_WHITE:
					mDrawer.updateShader(mDrawer.isGLES3
						? FRAGMENT_SHADER_EXT_EMPHASIZE_YELLOW_WHITE_ES3 : FRAGMENT_SHADER_EXT_EMPHASIZE_YELLOW_WHITE_ES2);
					break;
				default:
					mDrawer.resetShader();
					break;
				}
			}
			muParamsLoc = mDrawer.glGetUniformLocation("uParams");
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
	 * @param params
	 */
	public void setParams(@NonNull final float[] params) {
		setParams(mEffect, params);
	}

	/**
	 * 指定した映像フィルタにパラメータ配列をセット
	 * 現在対応しているのは色強調用の映像効果のみ(n=12以上必要)
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
			if (mDrawer != null) {
				mDrawer.glUseProgram();
			} else if (DEBUG) Log.d(TAG, "handleChangeEffect: mDrawer is null");
			if (mDrawer.isGLES3) {
				GLES30.glUniform1fv(muParamsLoc, n, mCurrentParams, 0);
			} else {
				GLES20.glUniform1fv(muParamsLoc, n, mCurrentParams, 0);
			}
		}
	}

}
