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
import android.annotation.TargetApi;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.serenegiant.system.BuildCheck;
import com.serenegiant.system.SysPropReader;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;
import kotlin.jvm.Transient;

/**
 * 現在のスレッド上にGLコンテキストを生成する
 */
public class GLContext implements EGLConst {
	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = GLContext.class.getSimpleName();

	/**
	 * initializeで最初の1回だけlogVersionInfoを呼ぶようにするためのフラグ
	 */
	private static boolean isOutputVersionInfo = false;

	private final Object mSync = new Object();
	private final int mMaxClientVersion;
	@Nullable
	private final EGLBase.IContext mSharedContext;
	private final int mFlags;
	@Size(min=1)
	private final int mMasterWidth;
	@Size(min=1)
	private final int mMasterHeight;
	@Transient
	@Nullable
	private EGLBase mEgl = null;
	@Transient
	@Nullable
	private EGLBase.IEglSurface mEglMasterSurface;
	@Transient
	private long mGLThreadId;
	@Transient
	@Nullable
	private String mGlExtensions;

	/**
	 * コンストラクタ
	 * 端末がサポートしている最も大きなGL|ESのバージョンを使う
	 * 共有コンテキストなし
	 * GLコンテキスト用のオフスクリーンサイズは1x1
	 */
	public GLContext() {
		this(GLUtils.getSupportedGLVersion(), null, 0, 1, 1);
	}

	/**
	 * コンストラクタ
	 * @param maxClientVersion 通常は2か3
	 * @param sharedContext 共有コンテキストの親となるIContext, nullなら自分がマスターのコンテキストとなる
	 * @param flags
	 */
	public GLContext(final int maxClientVersion,
		@Nullable final EGLBase.IContext sharedContext, final int flags) {

		this(maxClientVersion, sharedContext, flags, 1, 1);
	}

	/**
	 * コピーコンストラクタ
	 * @param src
	 */
	@SuppressWarnings("CopyConstructorMissesField")
	public GLContext(@NonNull final GLContext src) {
		this(src.getMaxClientVersion(), src.getContext(), src.getFlags(), 1, 1);
	}

	/**
	 * コンストラクタ
	 * @param maxClientVersion
	 * @param sharedContext
	 * @param flags
	 * @param width コンテキスト用のオフスクリーンの幅
	 * @param height　 コンテキスト用のオフスクリーンの高さ
	 */
	public GLContext(final int maxClientVersion,
		@Nullable final EGLBase.IContext sharedContext, final int flags,
		@Size(min=1) final int width, @Size(min=1) final int height) {

		mMaxClientVersion = maxClientVersion;
		mSharedContext = sharedContext;
		mFlags = flags;
		mMasterWidth = width > 0 ? width : 1;
		mMasterHeight = height > 0 ? height : 1;
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
	 * #initializeを呼び出したスレッド上で実行すること
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
		initialize(null);
	}

	/**
	 * 初期化を実行
	 * GLコンテキストを生成するスレッド上で実行すること
	 * @param surface nullでなければコンテキスト保持用IEglSurfaceをそのsurfaceから生成する
	 * @throws RuntimeException
	 */
	public void initialize(@Nullable final Object surface) throws RuntimeException {
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
			if (GLUtils.isSupportedSurface(surface)) {
				mEglMasterSurface = mEgl.createFromSurface(surface);
			} else {
				mEglMasterSurface = mEgl.createOffscreen(mMasterWidth, mMasterHeight);
			}
			mGLThreadId = Thread.currentThread().getId();
		} else {
			throw new RuntimeException("failed to create EglCore");
		}
		if (!isOutputVersionInfo) {
			isOutputVersionInfo = true;
			logVersionInfo();
		}
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
		synchronized (mSync) {
			final EGLBase.IContext result = mEgl != null ? mEgl.getContext() : null;
			if (result == null) {
				throw new IllegalStateException();
			}
			return result;
		}
	}

	/**
	 * マスターコンテキストを選択
	 * @throws IllegalStateException
	 */
	public void makeDefault() throws IllegalStateException {
		synchronized (mSync) {
			if ((mEgl != null) && (mEglMasterSurface != null)) {
				mEglMasterSurface.makeCurrent();
			} else {
				throw new IllegalStateException();
			}
		}
	}

	/**
	 * マスターコンテキストをswap
	 * @throws IllegalStateException
	 */
	public void swap() throws IllegalStateException {
		synchronized (mSync) {
			if ((mEgl != null) && (mEglMasterSurface != null)) {
				mEglMasterSurface.swap();
			} else {
				throw new IllegalStateException();
			}
		}
	}

	/**
	 * マスターコンテキストをswap
	 * @param presentationTimeNs
	 * @throws IllegalStateException
	 */
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
	public void swap(final long presentationTimeNs) throws IllegalStateException {
		synchronized (mSync) {
			if ((mEgl != null) && (mEglMasterSurface != null)) {
				mEglMasterSurface.swap(presentationTimeNs);
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
	 * 指定した文字列を含んでいるかどうかをチェック
	 * GLコンテキストが存在するスレッド上で実行すること
	 * @param extension
	 * @return
	 */
	public boolean hasExtension(@NonNull final String extension) {
		if (TextUtils.isEmpty(mGlExtensions)) {
			if (isGLES2()) {
				mGlExtensions = GLES20.glGetString(GLES20.GL_EXTENSIONS); // API >= 8
			} else {
				mGlExtensions = GLES30.glGetString(GLES30.GL_EXTENSIONS); // API >= 18
			}
		}
		return (mGlExtensions != null) && mGlExtensions.contains(extension);
	}

	/**
	 * GLES2/3でGL_OES_EGL_image_externalに対応しているかどうか
	 * @return
	 */
	public boolean isOES2() {
		return isGLES2() && hasExtension("GL_OES_EGL_image_external");
	}

	/**
	 * GLES3でGL_OES_EGL_image_external_essl3に対応しているかどうか
	 * @return
	 */
	public boolean isOES3() {
		return isGLES3() && hasExtension("GL_OES_EGL_image_external_essl3");
	}

//--------------------------------------------------------------------------------
	/**
	 * Writes GL version info to the log.
	 */
	@SuppressLint("InlinedApi")
	public static void logVersionInfo() {
		Log.i(TAG, "vendor:" + GLES20.glGetString(GLES20.GL_VENDOR));
		Log.i(TAG, "renderer:" + GLES20.glGetString(GLES20.GL_RENDERER));
		Log.i(TAG, "version:" + GLES20.glGetString(GLES20.GL_VERSION));
		Log.i(TAG, "supported version:" + supportedGLESVersion());
	}

	/**
	 * 対応するOpenGL|ESのバージョンを取得する
	 * @return 0以下なら何らかの理由でバージョンを取得できなかった
	 * 			それ以外は整数部: メジャーバージョン, 小数部: マイナーバージョン
	 */
	@SuppressLint("InlinedApi")
	public static float supportedGLESVersion() {
		float result = 0.0f;

		if (BuildCheck.isAndroid4_3()) {
			final int[] values = new int[1];
			GLES30.glGetIntegerv(GLES30.GL_MAJOR_VERSION, values, 0);
			final int majorVersion = values[0];
			GLES30.glGetIntegerv(GLES30.GL_MINOR_VERSION, values, 0);
			final int minorVersion = values[0];
			if (GLES30.glGetError() == GLES30.GL_NO_ERROR) {
				result = majorVersion + minorVersion * 0.1f;
			}
		}
		if (result <= 0.0f) {
			// バージョンを取得できなかったときは
			// ro.opengles.versionプロパティからの読み込みを試みる
			final String openGLESVersionString
				= SysPropReader.read("ro.opengles.version");
			if (!TextUtils.isEmpty(openGLESVersionString)) {
				try {
					final int openGLESVersion = Integer.parseInt(openGLESVersionString);
					result = ((openGLESVersion & 0xffff0000) >> 16)
						+ 0.1f * (openGLESVersion & 0x0000ffff);
				} catch (final NumberFormatException e) {
					if (DEBUG) Log.w(TAG, e);
				}
			} else {
				if (DEBUG) Log.v(TAG, "supportedGLESVersion:has no ro.opengles.version value");
			}
		}
		return result;
	}
}
