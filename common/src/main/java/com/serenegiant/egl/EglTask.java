package com.serenegiant.egl;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2023 saki t_saki@serenegiant.com
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

import com.serenegiant.gl.GLContext;
import com.serenegiant.gl.GLUtils;
import com.serenegiant.utils.MessageTask;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

/**
 * Looper/Handler経由での実装だと少なくともAPI22未満では
 * Looperによる同期バリアの影響を受ける(==vsync同期してしまうので
 * 60fpsの場合最大で16ミリ秒遅延する)のでそれを避けるために
 * Looper/Handlerを使わずに簡易的にメッセージ処理を行うための
 * ヘルパークラスMessageTaskへEGL/GLコンテキスト関係の
 * 処理を追加したヘルパークラス
 * EglTaskまたはその継承クラスをTreadへ引き渡して実行する
 */
public abstract class EglTask extends MessageTask {
	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = EglTask.class.getSimpleName();

	private final int mMasterWidth;
	private final int mMasterHeight;
	@NonNull
	private final GLContext mGLContext;

	/**
	 * コンストラクタ
	 * @param sharedContext
	 * @param flags
	 */
	public EglTask(@Nullable final EGLBase.IContext<?> sharedContext, final int flags) {
		this(GLUtils.getSupportedGLVersion(), sharedContext, flags);
	}

	/**
	 * コンストラクタ
	 * @param maxClientVersion
	 * @param sharedContext
	 * @param flags
	 */
	public EglTask(final int maxClientVersion,
				   @Nullable final EGLBase.IContext<?> sharedContext, final int flags) {

		this(maxClientVersion, sharedContext, flags, 1, 1);
	}

	/**
	 * コンストラクタ
	 * @param maxClientVersion
	 * @param sharedContext
	 * @param flags
	 * @param masterWidth
	 * @param masterHeight
	 */
	public EglTask(final int maxClientVersion,
				   @Nullable final EGLBase.IContext<?> sharedContext, final int flags,
				   final int masterWidth, final int masterHeight) {

//		if (DEBUG) Log.i(TAG, "shared_context=" + shared_context);
		this(new GLContext(maxClientVersion,
			sharedContext, flags), masterWidth, masterHeight);
	}

	/**
	 * コンストラクタ
	 * @param glContext
	 */
	public EglTask(
		@NonNull final GLContext glContext,
		final int masterWidth, final int masterHeight) {

		mGLContext = glContext;
		mMasterWidth = Math.max(masterWidth, 1);
		mMasterHeight = Math.max(masterHeight, 1);
		init(0, 0, null);
	}

	/**
	 * MessageTaskの実装
	 * @param flags
	 * @param maxClientVersion
	 * @param sharedContext
	 */
	@WorkerThread
	@CallSuper
	@Override
	protected void onInit(final int flags,
		final int maxClientVersion, final Object sharedContext) {

		mGLContext.initialize(null, mMasterWidth, mMasterHeight);
	}

	/**
	 * MessageTaskの実装
	 * @return
	 * @throws InterruptedException
	 */
	@Override
	protected Request takeRequest() throws InterruptedException {
		final Request result = super.takeRequest();
		mGLContext.makeDefault();
		return result;
	}

	/**
	 * MessageTaskの実装
	 */
	@WorkerThread
	@Override
	protected void onBeforeStop() {
		mGLContext.makeDefault();
	}

	/**
	 * MessageTaskの実装
	 */
	@WorkerThread
	@Override
	protected void onRelease() {
		mGLContext.release();
	}

//--------------------------------------------------------------------------------
	@NonNull
	public GLContext getGLContext() {
		return mGLContext;
	}

	@NonNull
	public EGLBase getEgl() {
		return mGLContext.getEgl();
	}

	@NonNull
	public EGLBase.IConfig<?> getConfig() {
		return mGLContext.getConfig();
	}

	@NonNull
	public EGLBase.IContext<?> getContext() {
		return mGLContext.getContext();
	}

	public void makeCurrent() {
		mGLContext.makeDefault();
	}

	public void swap() {
		mGLContext.swap();
	}

	/**
	 * GLコンテキストのバージョンを取得
	 * @return GLコンテキストが無効なら0が返る, 有効なら0, 1, 2, 3のいずれか(API>=16なので1が返ることはないはずだけど)
	 */
	public int getGlVersion() {
		return mGLContext.getGlVersion();
	}

	/**
	 * GLコンテキストでOpenGL|ES3に対応しているかどうかを取得
	 * @return
	 */
	public boolean isGLES3() {
		return mGLContext.isGLES3();
	}

	/**
	 * 指定した文字列を含んでいるかどうかをチェック
	 * GLコンテキストが存在するスレッド上で実行すること
	 * @param extension
	 * @return
	 */
	public boolean hasExtension(@NonNull final String extension) {
		return mGLContext.hasExtension(extension);
	}

	/**
	 * GLES3でGL_OES_EGL_image_external_essl3に対応しているかどうか
	 * @return
	 */
	public boolean isOES3Supported() {
		return mGLContext.isOES3Supported();
	}
}
