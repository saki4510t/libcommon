package com.serenegiant.glutils;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import android.os.Message;
import android.util.Log;

import com.serenegiant.egl.EGLBase;
import com.serenegiant.egl.EGLConst;
import com.serenegiant.gl.GLEffectDrawer2D;
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

	private static final boolean DEBUG = false;	// 実働時はfalseにすること
	private static final String TAG = EffectRendererHolder.class.getSimpleName();

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
			callback, null);
	}

	/**
	 * コンストラクタ
	 * @param width
	 * @param height
	 * @param callback
	 * @param drawerFactory
	 */
	public EffectRendererHolder(
		final int width, final int height,
		@Nullable final RenderHolderCallback callback,
		@Nullable GLEffectDrawer2D.DrawerFactory drawerFactory) {

		this(width, height,
			3, null, EGLConst.EGL_FLAG_RECORDABLE,
			callback, drawerFactory);
	}

	/**
	 * コンストラクタ
	 * @param width
	 * @param height
	 * @param maxClientVersion
	 * @param sharedContext
	 * @param flags
	 * @param callback
	 * @param drawerFactory
	 */
	public EffectRendererHolder(final int width, final int height,
		final int maxClientVersion,
		@Nullable final EGLBase.IContext<?> sharedContext, final int flags,
		@Nullable final RenderHolderCallback callback,
		@Nullable GLEffectDrawer2D.DrawerFactory drawerFactory) {

		super(width, height, maxClientVersion, sharedContext, flags, callback,
			drawerFactory != null ? drawerFactory : GLEffectDrawer2D.DEFAULT_EFFECT_FACTORY);
		if (DEBUG) Log.v(TAG, "コンストラクタ:");
	}

	@Override
	@NonNull
	protected BaseRendererTask createRendererTask(final int width, final int height,
		final int maxClientVersion,
		@Nullable final EGLBase.IContext<?> sharedContext, final int flags,
		@NonNull GLDrawer2D.DrawerFactory drawerFactory) {

		if (DEBUG) Log.v(TAG, "createRendererTask:");
		final GLEffectDrawer2D.DrawerFactory factory =
			(drawerFactory instanceof GLEffectDrawer2D.DrawerFactory)
				? (GLEffectDrawer2D.DrawerFactory)drawerFactory
				: GLEffectDrawer2D.DEFAULT_EFFECT_FACTORY;
		return new MyRendererTask(this, width, height,
			maxClientVersion, sharedContext, flags,
			factory);
	}

//================================================================================
// クラス固有publicメソッド
//================================================================================
	
	/**
	 * IEffect/IEffectRendererHolderの実装
	 * 映像効果をセット
	 * 継承して独自の映像効果を追加する時はEFFECT_NUMよりも大きい値を使うこと
	 * @param effect
	 */
	@Override
	public void setEffect(final int effect) {
		((MyRendererTask)mRendererTask).changeEffect(effect);
	}
	
	/**
	 * IEffect/IEffectRendererHolderの実装
	 * 現在の映像効果番号を取得
	 * @return
	 */
	@Override
	public int getEffect() {
		final GLDrawer2D drawer = mRendererTask.getDrawer();
		return (drawer instanceof GLEffectDrawer2D)
			? ((GLEffectDrawer2D) drawer).getEffect() : EFFECT_NON;
	}

	/**
	 * IEffect/IEffectRendererHolderの実装
	 * 現在選択中の映像フィルタにパラメータ配列をセット
	 * 現在対応しているのは色強調用の映像効果のみ(n=12以上必要)
	 * @param params
	 */
	@Override
	public void setParams(@NonNull final float[] params) {
		((MyRendererTask)mRendererTask).setParams(-1, params);
	}

	/**
	 * IEffect/IEffectRendererHolderの実装
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
	 * EffectDrawer2Dで映像効果適用する前に呼ばれるコールバックインターフェースを生成
	 * #onChangeEffectがtrueを返すとEffectDrawer2Dのデフォルトの処理を行わない
	 */
	private final GLEffectDrawer2D.EffectListener mEffectListener
		= new GLEffectDrawer2D.EffectListener() {
		@WorkerThread
		@Override
		public boolean onChangeEffect(final int effect, @NonNull final GLEffectDrawer2D drawer) {
			return EffectRendererHolder.this.onChangeEffect(effect, drawer);
		}
	};

	/**
	 * EffectDrawer2Dでのデフォルトの処理前に呼び出される
	 * @param effect
	 * @param drawer
	 * @return trueを返すとEffectDrawer2Dでの処理をしない, falseならEffectDrawer2Dでの処理を行う
	 */
	@WorkerThread
	protected boolean onChangeEffect(final int effect, @NonNull final GLEffectDrawer2D drawer) {
		return false;	// falseを返すとEffectDrawer2Dでのデフォルトの処理を行う
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
			@Nullable final EGLBase.IContext<?> sharedContext, final int flags,
			@Nullable GLEffectDrawer2D.DrawerFactory factory) {
			
			super(parent, width, height, maxClientVersion, sharedContext, flags, factory);
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
		@Override
		@WorkerThread
		protected boolean handleRequest(@NonNull final Message msg) {
			boolean result = true;
			switch (msg.what) {
			case REQUEST_CHANGE_EFFECT -> handleChangeEffect(msg.arg1);
			case REQUEST_SET_PARAMS -> handleSetParam(msg.arg1, (float[]) msg.obj);
			default -> result = super.handleRequest(msg);
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
			if (drawer instanceof GLEffectDrawer2D) {
				((GLEffectDrawer2D)drawer).setEffect(effect);
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
			if (drawer instanceof GLEffectDrawer2D) {
				((GLEffectDrawer2D) drawer).setParams(effect, params);
			} else if (DEBUG) {
				Log.d(TAG, "handleChangeEffect: mDrawer is not EffectDrawer2D");
			}
		}

	}

}
