package com.serenegiant.glutils;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2019 saki t_saki@serenegiant.com
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
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.util.Log;

import com.serenegiant.system.BuildCheck;
import com.serenegiant.system.SysPropReader;

import androidx.annotation.Nullable;

/**
 * 現在のスレッド上にGLコンテキストを生成する
 */
public class GLContext implements EGLConst {
	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = GLContext.class.getSimpleName();

	private final Object mSync = new Object();
	private final int mMaxClientVersion;
	@Nullable
	final EGLBase.IContext mSharedContext;
	private final int mFlags;
	@Nullable
	private EGLBase mEgl = null;
	@Nullable
	private ISurface mEglMasterSurface;
	private long mGLThreadId;

	/**
	 * コンストラクタ
	 * @param maxClientVersion 通常は2か3
	 * @param sharedContext 共有コンテキストの親となるIContext, nullなら自分がマスターのコンテキストとなる
	 * @param flags
	 */
	public GLContext(final int maxClientVersion,
		@Nullable final EGLBase.IContext sharedContext, final int flags) {

		mMaxClientVersion = maxClientVersion;
		mSharedContext = sharedContext;
		mFlags = flags;
	}

	@Override
	protected void finalize() throws Throwable {
		try {
			release();
		} finally {
			super.finalize();
		}
	}

	/**
	 * 関連するリソースを破棄する
	 * コンストラクタを呼び出したスレッド上で実行すること
	 */
	public void release() {
		synchronized (mSync) {
			mGLThreadId = 0;
			if (mEglMasterSurface != null) {
				mEglMasterSurface.release();
				mEglMasterSurface = null;
			}
			if (mEgl != null) {
				mEgl.release();
				mEgl = null;
			}
		}
	}

	/**
	 * 初期化を実行
	 * GLコンテキストを生成するスレッド上で実行すること
	 * @throws RuntimeException
	 */
	public void initialize() throws RuntimeException {
		if ((mSharedContext == null)
			|| (mSharedContext instanceof EGLBase.IContext)) {

			final int stencilBits
				= (mFlags & EGL_FLAG_STENCIL_1BIT) == EGL_FLAG_STENCIL_1BIT ? 1
					: ((mFlags & EGL_FLAG_STENCIL_8BIT) == EGL_FLAG_STENCIL_8BIT ? 8 : 0);
			mEgl = EGLBase.createFrom(mMaxClientVersion, mSharedContext,
				(mFlags & EGL_FLAG_DEPTH_BUFFER) == EGL_FLAG_DEPTH_BUFFER,
				stencilBits,
				(mFlags & EGL_FLAG_RECORDABLE) == EGL_FLAG_RECORDABLE);
		}
		if (mEgl != null) {
			mEglMasterSurface = mEgl.createOffscreen(1, 1);
			mGLThreadId = Thread.currentThread().getId();
		} else {
			throw new RuntimeException("failed to create EglCore");
		}
		logVersionInfo();
	}

	/**
	 * EGLBaseを取得
	 * @return
	 * @throws IllegalStateException
	 */
	public EGLBase getEgl() throws IllegalStateException {
		synchronized (mSync) {
			if (mEgl != null) {
				return mEgl;
			} else {
				throw new IllegalStateException();
			}
		}
	}

	/**
	 * EGLBase生成時のIConfigを取得
	 * @return
	 * @throws IllegalStateException
	 */
	public EGLBase.IConfig getConfig() throws IllegalStateException {
		synchronized (mSync) {
			if (mEgl != null) {
				return mEgl.getConfig();
			} else {
				throw new IllegalStateException();
			}
		}
	}

	/**
	 * EGLBase生成時のmaxClientVersionを取得
	 * @return
	 */
	public int getMaxClientVersion() {
		return mMaxClientVersion;
	}

	/**
	 * EGLBase生成時のflagsを取得
	 * @return
	 */
	public int getFlags() {
		return mFlags;
	}

	/**
	 * IContextを取得
	 * @return
	 * @throws IllegalStateException
	 */
	public EGLBase.IContext getContext() throws IllegalStateException {
		if (mEgl != null) {
			mEgl.getContext();
		} else {
			throw new IllegalStateException();
		}
		return mEgl != null ? mEgl.getContext() : null;
	}

	/**
	 * マスターコンテキストを選択
	 * @throws IllegalStateException
	 */
	public void makeDefault() throws IllegalStateException {
		synchronized (mSync) {
			if (mEgl != null) {
				mEglMasterSurface.makeCurrent();
			} else {
				throw new IllegalStateException();
			}
		}
	}

	/**
	 * eglWaitGLとeglWaitNativeを呼ぶ
	 *
	 * eglWaitGL: コマンドキュー内のコマンドをすべて転送する, GLES20.glFinish()と同様の効果
	 * eglWaitNative: GPU側の描画処理が終了するまで実行をブロックする
	 */
	public void sync() throws IllegalStateException {
		synchronized (mSync) {
			if (mEgl != null) {
				mEgl.sync();
			} else {
				throw new IllegalStateException();
			}
		}
	}

	/**
	 * eglWaitGLを呼ぶ
	 * コマンドキュー内のコマンドをすべて転送する, GLES20.glFinish()と同様の効果
	 */
	public void waitGL() throws IllegalStateException {
		synchronized (mSync) {
			if (mEgl != null) {
				mEgl.waitGL();
			} else {
				throw new IllegalStateException();
			}
		}
	}

	/**
	 * eglWaitNativeを呼ぶ
	 * GPU側の描画処理が終了するまで実行をブロックする
	 */
	public void waitNative() throws IllegalStateException {
		synchronized (mSync) {
			if (mEgl != null) {
				mEgl.waitNative();
			} else {
				throw new IllegalStateException();
			}
		}
	}

	/**
	 * GLコンテキストを保持しているスレッドのIDを返す
	 * (== #initializeを実行したときのスレッドのID)
	 * @return
	 */
	public long getGLThreadId() {
		synchronized (mSync) {
			return mGLThreadId;
		}
	}

	/**
	 * GLES2以上で初期化されているかどうか
	 * @return
	 */
	public boolean isGLES2() {
		return getGlVersion() > 1;
	}

	/**
	 * GLES3以上で初期化されているかどうか
	 * @return
	 */
	public boolean isGLES3() {
		return getGlVersion() > 2;
	}

	/**
	 * GLコンテキストのバージョンを取得
	 * @return
	 */
	public int getGlVersion() {
		synchronized (mSync) {
			return  (mEgl != null) ? mEgl.getGlVersion() : 0;
		}
	}

	/**
	 * Writes GL version info to the log.
	 */
	@SuppressLint("InlinedApi")
	public static void logVersionInfo() {
		Log.i(TAG, "vendor  : " + GLES20.glGetString(GLES20.GL_VENDOR));
		Log.i(TAG, "renderer: " + GLES20.glGetString(GLES20.GL_RENDERER));
		Log.i(TAG, "version : " + GLES20.glGetString(GLES20.GL_VERSION));

		if (BuildCheck.isAndroid4_3()) {
			final int[] values = new int[1];
			GLES30.glGetIntegerv(GLES30.GL_MAJOR_VERSION, values, 0);
			final int majorVersion = values[0];
			GLES30.glGetIntegerv(GLES30.GL_MINOR_VERSION, values, 0);
			final int minorVersion = values[0];
			if (GLES30.glGetError() == GLES30.GL_NO_ERROR) {
				Log.i(TAG, "version: " + majorVersion + "." + minorVersion);
			}
		}
		final String openGLESVersionString
			= SysPropReader.read("ro.opengles.version");
		int openGLESVersion;
		try {
			openGLESVersion = Integer.parseInt(openGLESVersionString);
		} catch (final NumberFormatException e) {
			if (DEBUG) Log.w(TAG, e);
			openGLESVersion = 0;
		}
		Log.i(TAG, String.format("ro.opengles.version=%s(0x%08x)",
			openGLESVersionString, openGLESVersion));
	}
}
