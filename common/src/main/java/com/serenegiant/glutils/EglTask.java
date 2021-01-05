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

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.serenegiant.utils.MessageTask;

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
//	private static final boolean DEBUG = false;
//	private static final String TAG = "EglTask";

	private final GLContext mGLContext;

	/**
	 * コンストラクタ
	 * @param sharedContext
	 * @param flags
	 */
	@Deprecated
	public EglTask(@Nullable final EGLBase.IContext sharedContext, final int flags) {
		this(GLUtils.getSupportedGLVersion(), sharedContext, flags);
	}

	/**
	 * コンストラクタ
	 * @param maxClientVersion
	 * @param sharedContext
	 * @param flags
	 */
	public EglTask(final int maxClientVersion,
		@Nullable final EGLBase.IContext sharedContext, final int flags) {

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
		@Nullable final EGLBase.IContext sharedContext, final int flags,
		final int masterWidth, final int masterHeight) {

//		if (DEBUG) Log.i(TAG, "shared_context=" + shared_context);
		mGLContext = new GLContext(maxClientVersion,
			sharedContext, flags,
			masterWidth, masterHeight);
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

		mGLContext.initialize();
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
	public GLContext getGLContext() {
		return mGLContext;
	}

	public EGLBase getEgl() {
		return mGLContext.getEgl();
	}

	public EGLBase.IConfig getConfig() {
		return mGLContext.getConfig();
	}

	@Nullable
	public EGLBase.IContext getContext() {
		return mGLContext.getContext();
	}

	public void makeCurrent() {
		mGLContext.makeDefault();
	}

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
	public boolean isOES3() {
		return mGLContext.isOES3();
	}
}
