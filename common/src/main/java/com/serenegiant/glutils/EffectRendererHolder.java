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

import android.annotation.SuppressLint;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import android.opengl.GLES20;
import android.opengl.GLES30;
import android.util.Log;
import android.util.SparseArray;

import static com.serenegiant.glutils.ShaderConst.*;
import static com.serenegiant.glutils.GLEffect.*;

/**
 * GL_TEXTURE_EXTERNAL_OESテクスチャを受け取ってSurfaceへ分配描画するクラス
 * RendererHolderにフラグメントシェーダーでのフィルター処理を追加
 * ...カラーマトリックスを掛けるほうがいいかなぁ
 * ...色はuniform変数で渡す方がいいかも
 * FIXME EffectDrawer2Dを使うように変更する
 */
public class EffectRendererHolder extends AbstractRendererHolder
	implements IEffectRendererHolder {

	private static final boolean DEBUG = false;	// FIXME 実働時はfalseにすること
	private static final String TAG = EffectRendererHolder.class.getSimpleName();

//================================================================================
	/**
	 * コンストラクタ
	 * @param width
	 * @param height
	 * @param callback
	 */
	public EffectRendererHolder(final int width, final int height,
		@Nullable final RenderHolderCallback callback) {

		this(width, height,
			3, null, EGLConst.EGL_FLAG_RECORDABLE,
			false, callback);
	}

	/**
	 * コンストラクタ
	 * @param width
	 * @param height
	 * @param enableVSync Choreographerを使ってvsync同期して映像更新するかどうか
	 * @param callback
	 */
	public EffectRendererHolder(final int width, final int height,
		final boolean enableVSync,
		@Nullable final RenderHolderCallback callback) {

		this(width, height,
			3, null, EGLConst.EGL_FLAG_RECORDABLE,
			enableVSync, callback);
	}

	/**
	 * コンストラクタ
	 * @param width
	 * @param height
	 * @param maxClientVersion
	 * @param sharedContext
	 * @param flags
	 * @param callback
	 */
	public EffectRendererHolder(final int width, final int height,
		final int maxClientVersion,
		@Nullable final EGLBase.IContext sharedContext, final int flags,
		@Nullable final RenderHolderCallback callback) {
		this(width, height,
			3, null, EGLConst.EGL_FLAG_RECORDABLE,
			false, callback);
	}

	/**
	 * コンストラクタ
	 * @param width
	 * @param height
	 * @param maxClientVersion
	 * @param sharedContext
	 * @param enableVSync
	 * @param flags
	 * @param callback
	 */
	public EffectRendererHolder(final int width, final int height,
		final int maxClientVersion,
		@Nullable final EGLBase.IContext sharedContext, final int flags,
		final boolean enableVSync,
		@Nullable final RenderHolderCallback callback) {

		super(width, height, maxClientVersion, sharedContext, flags, enableVSync, callback);
		if (DEBUG) Log.v(TAG, "コンストラクタ:");
	}

	@Override
	@NonNull
	protected BaseRendererTask createRendererTask(final int width, final int height,
		final int maxClientVersion,
		@Nullable final EGLBase.IContext sharedContext, final int flags,
		final boolean enableVsync) {

		if (DEBUG) Log.v(TAG, "createRendererTask:");
		return new MyRendererTask(this, width, height,
			maxClientVersion, sharedContext, flags, enableVsync);
	}

//================================================================================
// クラス固有publicメソッド
//================================================================================
	
	/**
	 * IEffectRendererHolderの実装
	 * 映像効果をセット
	 * 継承して独自の映像効果を追加する時はEFFECT_NUMよりも大きい値を使うこと
	 * @param effect
	 */
	@Override
	public void changeEffect(final int effect) {
		((MyRendererTask)mRendererTask).changeEffect(effect);
	}
	
	/**
	 * IEffectRendererHolderの実装
	 * 現在の映像効果番号を取得
	 * @return
	 */
	@Override
	public int getCurrentEffect() {
		if (DEBUG) Log.v(TAG, "getCurrentEffect:" + ((MyRendererTask)mRendererTask).mEffect);
		return ((MyRendererTask)mRendererTask).mEffect;
	}

	/**
	 * IEffectRendererHolderの実装
	 * 現在選択中の映像フィルタにパラメータ配列をセット
	 * 現在対応しているのは色強調用の映像効果のみ(n=12以上必要)
	 * @param params
	 */
	@Override
	public void setParams(@NonNull final float[] params) {
		((MyRendererTask)mRendererTask).setParams(-1, params);
	}

	/**
	 * IEffectRendererHolderの実装
	 * 指定した映像フィルタにパラメータ配列をセット
	 * 現在対応しているのは色強調用の映像効果のみ(n=12以上必要)
	 * @param effect EFFECT_NONより大きいこと
	 * @param params
	 * @throws IllegalArgumentException effectが範囲外ならIllegalArgumentException生成
	 */
	@Override
	public void setParams(final int effect, @NonNull final float[] params)
		throws IllegalArgumentException {

		if (effect > EFFECT_NON) {
			((MyRendererTask)mRendererTask).setParams(effect, params);
		} else {
			throw new IllegalArgumentException("invalid effect number:" + effect);
		}
	}
	
	/**
	 * 内蔵映像効果以外のeffectを指定したときの処理
	 * 描画用のワーカースレッド上で呼び出される
	 * このクラスでは無変換のフラグメントシェーダーを適用する
	 * @param effect
	 * @param drawer GLDrawer2Dインスタンス
	 */
	protected void handleDefaultEffect(final int effect,
		@NonNull final GLDrawer2D drawer) {

		drawer.resetShader();
	}
	
//================================================================================
// 実装
//================================================================================
	private static final int REQUEST_CHANGE_EFFECT = 100;
	private static final int REQUEST_SET_PARAMS = 101;

	/**
	 * ワーカースレッド上でOpenGL|ESを用いてマスター映像を分配描画するためのインナークラス
	 */
	protected static final class MyRendererTask extends BaseRendererTask {

		private final SparseArray<float[]> mParams = new SparseArray<float[]>();
		private int muParamsLoc;
		private float[] mCurrentParams;
		private int mEffect;

		public MyRendererTask(@NonNull final AbstractRendererHolder parent,
			final int width, final int height,
			final int maxClientVersion,
			@Nullable final EGLBase.IContext sharedContext, final int flags,
			final boolean enableVsync) {
			
			super(parent, width, height, maxClientVersion, sharedContext, flags, enableVsync);
			if (DEBUG) Log.v(TAG, "MyRendererTask#コンストラクタ:");
		}

		public void changeEffect(final int effect) {
			if (DEBUG) Log.v(TAG, "MyRendererTask#changeEffect:" + effect);
			checkFinished();
			if (mEffect != effect) {
				offer(REQUEST_CHANGE_EFFECT, effect);
			}
		}

		public void setParams(final int effect, @NonNull final float[] params) {
			if (DEBUG) Log.v(TAG, "MyRendererTask#setParams:" + effect);
			checkFinished();
			offer(REQUEST_SET_PARAMS, effect, 0, params);
		}

//================================================================================
// ワーカースレッド上での処理
//================================================================================
		/**
		 * ワーカースレッド開始時の処理(ここはワーカースレッド上)
		 */
		@SuppressLint("NewApi")
		@WorkerThread
		@Override
		protected void internalOnStart() {
			super.internalOnStart();
			if (DEBUG) Log.v(TAG, "MyRendererTask#internalOnStart:");
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
			handleChangeEffect(EFFECT_NON);
//			if (DEBUG) Log.v(TAG, "onStart:finished");
		}

		@WorkerThread
		@Override
		protected Object handleRequest(final int request,
			final int arg1, final int arg2, final Object obj) {

			Object result = null;
			switch (request) {
			case REQUEST_CHANGE_EFFECT:
				handleChangeEffect(arg1);
				break;
			case REQUEST_SET_PARAMS:
				handleSetParam(arg1, (float[])obj);
				break;
			default:
				result = super.handleRequest(request, arg1, arg2, obj);
				break;
			}
			return result;
		}

		/**
		 * 映像効果を変更
		 * @param effect
		 */
		@WorkerThread
		protected void handleChangeEffect(final int effect) {
			if (DEBUG) Log.v(TAG, "MyRendererTask#handleChangeEffect:" + effect);
			mEffect = effect;
			if (mDrawer != null) {
				switch (effect) {
				case EFFECT_NON:
					mDrawer.updateShader(isGLES3()
						? FRAGMENT_SHADER_EXT_ES3 : FRAGMENT_SHADER_EXT_ES2);
					break;
				case EFFECT_GRAY:
					mDrawer.updateShader(isGLES3()
						? FRAGMENT_SHADER_EXT_GRAY_ES3 : FRAGMENT_SHADER_EXT_GRAY_ES2);
					break;
				case EFFECT_GRAY_REVERSE:
					mDrawer.updateShader(isGLES3()
						? FRAGMENT_SHADER_GRAY_EXT_REVERSE_ES3 : FRAGMENT_SHADER_GRAY_EXT_REVERSE_ES2);
					break;
				case EFFECT_BIN:
					mDrawer.updateShader(isGLES3()
						? FRAGMENT_SHADER_EXT_BIN_ES3 : FRAGMENT_SHADER_EXT_BIN_ES2);
					break;
				case EFFECT_BIN_YELLOW:
					mDrawer.updateShader(isGLES3()
						?FRAGMENT_SHADER_EXT_BIN_YELLOW_ES3 : FRAGMENT_SHADER_EXT_BIN_YELLOW_ES2);
					break;
				case EFFECT_BIN_GREEN:
					mDrawer.updateShader(isGLES3()
						? FRAGMENT_SHADER_EXT_BIN_GREEN_ES3 : FRAGMENT_SHADER_EXT_BIN_GREEN_ES2);
					break;
				case EFFECT_BIN_REVERSE:
					mDrawer.updateShader(isGLES3()
						? FRAGMENT_SHADER_EXT_BIN_REVERSE_ES3 : FRAGMENT_SHADER_EXT_BIN_REVERSE_ES2);
					break;
				case EFFECT_BIN_REVERSE_YELLOW:
					mDrawer.updateShader(isGLES3()
						? FRAGMENT_SHADER_EXT_BIN_REVERSE_YELLOW_ES3 : FRAGMENT_SHADER_EXT_BIN_REVERSE_YELLOW_ES2);
					break;
				case EFFECT_BIN_REVERSE_GREEN:
					mDrawer.updateShader(isGLES3()
						? FRAGMENT_SHADER_EXT_BIN_REVERSE_GREEN_ES3 : FRAGMENT_SHADER_EXT_BIN_REVERSE_GREEN_ES2);
					break;
				case EFFECT_EMPHASIZE_RED_YELLOW:
					mDrawer.updateShader(isGLES3()
						? FRAGMENT_SHADER_EXT_EMPHASIZE_RED_YELLOWS_ES3 : FRAGMENT_SHADER_EXT_EMPHASIZE_RED_YELLOWS_ES2);
					break;
				case EFFECT_EMPHASIZE_RED_YELLOW_WHITE:
					mDrawer.updateShader(isGLES3()
						? FRAGMENT_SHADER_EXT_EMPHASIZE_RED_YELLOW_WHITE_ES3 : FRAGMENT_SHADER_EXT_EMPHASIZE_RED_YELLOW_WHITE_ES2);
					break;
				case EFFECT_EMPHASIZE_YELLOW_WHITE:
					mDrawer.updateShader(isGLES3()
						? FRAGMENT_SHADER_EXT_EMPHASIZE_YELLOW_WHITE_ES3 : FRAGMENT_SHADER_EXT_EMPHASIZE_YELLOW_WHITE_ES2);
					break;
				default:
					try {
						((EffectRendererHolder)getParent())
							.handleDefaultEffect(effect, mDrawer);
					} catch (final Exception e) {
						mDrawer.resetShader();
						Log.w(TAG, e);
					}
					break;
				}
				muParamsLoc = mDrawer.glGetUniformLocation("uParams");
				mCurrentParams = mParams.get(effect);
				updateParams();
			} else if (DEBUG) Log.d(TAG, "handleChangeEffect: mDrawer is not IShaderDrawer2d");
		}
		
		/**
		 * 映像効果用のパラメーターをセット
		 * @param effect
		 * @param params
		 */
		@WorkerThread
		private void handleSetParam(final int effect, @NonNull final float[] params) {
			if (DEBUG) Log.v(TAG, "MyRendererTask#handleSetParam:" + effect);
			if ((effect < EFFECT_NON) || (mEffect == effect)) {
				mCurrentParams = params;
				mParams.put(mEffect, params);
				updateParams();
			} else {
				mParams.put(effect, params);
			}
		}
		
		/**
		 * 映像効果用のパラメータをGPUへ適用
		 */
		@WorkerThread
		private void updateParams() {
			if (DEBUG) Log.v(TAG, "MyRendererTask#updateParams:");
			final int n = Math.min(mCurrentParams != null
				? mCurrentParams.length : 0, MAX_PARAM_NUM);
			if ((muParamsLoc >= 0) && (n > 0)) {
				if (mDrawer != null) {
					mDrawer.glUseProgram();
				} else if (DEBUG) Log.d(TAG, "handleChangeEffect: mDrawer is null");
				if (isGLES3()) {
					GLES30.glUniform1fv(muParamsLoc, n, mCurrentParams, 0);
				} else {
					GLES20.glUniform1fv(muParamsLoc, n, mCurrentParams, 0);
				}
			}
		}

	}

}
