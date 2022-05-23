package com.serenegiant.glutils;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import android.util.Log;

import com.serenegiant.egl.EGLBase;
import com.serenegiant.egl.EGLConst;
import com.serenegiant.gl.EffectDrawer2D;
import com.serenegiant.gl.GLDrawer2D;

import static com.serenegiant.gl.GLEffect.*;

/**
 * GL_TEXTURE_EXTERNAL_OESテクスチャを受け取ってSurfaceへ分配描画するクラス
 * RendererHolderにフラグメントシェーダーでのフィルター処理を追加
 * ...カラーマトリックスを掛けるほうがいいかなぁ
 * ...色はuniform変数で渡す方がいいかも
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
			callback);
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
		@Nullable final EGLBase.IContext<?> sharedContext, final int flags,
		@Nullable final RenderHolderCallback callback) {

		super(width, height, maxClientVersion, sharedContext, flags, callback);
		if (DEBUG) Log.v(TAG, "コンストラクタ:");
	}

	@Override
	@NonNull
	protected BaseRendererTask createRendererTask(final int width, final int height,
		final int maxClientVersion,
		@Nullable final EGLBase.IContext<?> sharedContext, final int flags) {

		if (DEBUG) Log.v(TAG, "createRendererTask:");
		return new MyRendererTask(this, width, height,
			maxClientVersion, sharedContext, flags);
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
		final GLDrawer2D drawer = mRendererTask.getDrawer();
		return (drawer instanceof EffectDrawer2D)
			? ((EffectDrawer2D) drawer).getCurrentEffect() : EFFECT_NON;
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

		public MyRendererTask(@NonNull final AbstractRendererHolder parent,
			final int width, final int height,
			final int maxClientVersion,
			@Nullable final EGLBase.IContext<?> sharedContext, final int flags) {
			
			super(parent, width, height, maxClientVersion, sharedContext, flags,
				new GLDrawer2D.DrawerFactory() {
					@NonNull
					@Override
					public GLDrawer2D create(final boolean isGLES3, final boolean isOES) {
						return new EffectDrawer2D(isGLES3, isOES);
					}
				});
			if (DEBUG) Log.v(TAG, "MyRendererTask#コンストラクタ:");
		}

		public void changeEffect(final int effect) {
			if (DEBUG) Log.v(TAG, "MyRendererTask#changeEffect:" + effect);
			checkFinished();
			offer(REQUEST_CHANGE_EFFECT, effect);
		}

		public void setParams(final int effect, @NonNull final float[] params) {
			if (DEBUG) Log.v(TAG, "MyRendererTask#setParams:" + effect);
			checkFinished();
			offer(REQUEST_SET_PARAMS, effect, 0, params);
		}

//================================================================================
// ワーカースレッド上での処理
//================================================================================
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
		private void handleChangeEffect(final int effect) {
			if (DEBUG) Log.v(TAG, "MyRendererTask#handleChangeEffect:" + effect);
			final GLDrawer2D drawer = getDrawer();
			if (drawer instanceof EffectDrawer2D) {
				((EffectDrawer2D)drawer).setEffect(effect);
			} else if (DEBUG) {
				Log.d(TAG, "handleChangeEffect: mDrawer is not EffectDrawer2D");
			}
		}
		
		/**
		 * 映像効果用のパラメーターをセット
		 * @param effect
		 * @param params
		 */
		@WorkerThread
		private void handleSetParam(final int effect, @NonNull final float[] params) {
			if (DEBUG) Log.v(TAG, "MyRendererTask#handleSetParam:" + effect);
			final GLDrawer2D drawer = getDrawer();
			if (drawer instanceof EffectDrawer2D) {
				((EffectDrawer2D) drawer).setParams(effect, params);
			} else if (DEBUG) {
				Log.d(TAG, "handleChangeEffect: mDrawer is not EffectDrawer2D");
			}
		}

	}

}
