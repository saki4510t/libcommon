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

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.serenegiant.egl.EGLBase;
import com.serenegiant.egl.EGLConst;
import com.serenegiant.system.BuildCheck;
import com.serenegiant.system.SysPropReader;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import kotlin.jvm.Transient;

/**
 * 現在のスレッド上にGLコンテキストを生成する
 * コンストラクタはメイン/UIスレッドを含めた任意のスレッド上で実行可能
 * #initializeを呼び出したスレッド上にOpenGL|ESのレンダリングコンテキストが紐付けられる
 * すでにOpenGL|ESのレンダリングスレッドが紐付けられたスレッド上で#initializeを呼び出したときの
 * 挙動は不明
 */
public class GLContext implements EGLConst {
	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = GLContext.class.getSimpleName();

	/**
	 * initializeで最初の1回だけlogVersionInfoを呼ぶようにするためのフラグ
	 */
	private static boolean isOutputVersionInfo = false;

	/**
	 * 排他制御用
	 */
	@NonNull
	private final Object mSync = new Object();
	/**
	 * 初期化に使用可能なGL|ESのバージョン
	 * 1,2,3のいずれか
	 */
	private final int mMaxClientVersion;
	/**
	 * 初期化に使用する共有コンテキスト
	 * nullなら他スレッド上のレンダリングコンテキストとは独立したレンダリングコンテキストを生成する
	 * (独立していてもSurfaceTexture等を介してやり取りすること自体は可能)
	 */
	@Nullable
	private final EGLBase.IContext<?> mSharedContext;
	/**
	 * 初期化に使用するフラグ
	 */
	private final int mFlags;
	/**
	 * EGLアクセスオブジェクト
	 */
	@Transient
	@Nullable
	private EGLBase mEgl = null;
	/**
	 * レンダリングコンテキストを有効にするのに必要な描画surface
	 * #initializeで指定したSurface/SurfaceHolder/SurfaceTexture/SurfaceViewの
	 * windows surfaceから生成したもの、またはオフスクリーン
	 */
	@Transient
	@Nullable
	private EGLBase.IEglSurface mEglMasterSurface;
	/**
	 * レンダリングコンテキストを紐付けたスレッドのID
	 */
	@Transient
	private long mGLThreadId;
	/**
	 * GL|ESのエクステンション文字列のキャッシュ用
	 */
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
		this(GLUtils.getSupportedGLVersion(), null, 0);
	}

	/**
	 * コピーコンストラクタ
	 * @param src
	 */
	@SuppressWarnings("CopyConstructorMissesField")
	public GLContext(@NonNull final GLContext src) {
		this(src.getMaxClientVersion(), src.getContext(), src.getFlags());
	}

	/**
	 * コンストラクタ
	 * @param maxClientVersion 通常は2か3
	 * @param sharedContext
	 * @param flags
	 */
	public GLContext(final int maxClientVersion,
		@Nullable final EGLBase.IContext<?> sharedContext, final int flags) {

		mMaxClientVersion = maxClientVersion;
		mSharedContext = sharedContext;
		mFlags = flags;
	}

	@Override
	protected void finalize() throws Throwable {
		// #finalizeから#releaseが呼ばれるよりも前に#releaseを呼び出しておかないとGLESのエラーがログに出力される
		if (DEBUG && (mGLThreadId != 0)) throw new IllegalStateException("Not released!");
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
	@WorkerThread
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
	 * @throws IllegalArgumentException
	 */
	@WorkerThread
	public void initialize() throws IllegalArgumentException {
		initialize(null, 1, 1);
	}

	/**
	 * 初期化を実行
	 * GLコンテキストを生成するスレッド上で実行すること
	 * widthとheightがどちらも0以下で再初期化の場合は以前のマスターサーフェースのサイズを使う
	 * @param surface nullでなければコンテキスト保持用IEglSurfaceをそのsurfaceから生成する, nullの場合はダミーのオフスクリーンを生成する
	 * @param width オフスクリーンの幅 1未満の場合は1
	 * @param height オフスクリーン高さ  1未満の場合は1
	 * @param height オフスクリーン高さ  1未満の場合は1
	 * @throws IllegalArgumentException
	 */
	@WorkerThread
	public void initialize(
		@Nullable final Object surface, final int width, final int height)
			throws IllegalArgumentException {

		if ((mEgl == null)
			&& ((mSharedContext == null)
				|| (mSharedContext instanceof EGLBase.IContext))) {

			final int stencilBits
				= (mFlags & EGL_FLAG_STENCIL_1BIT) == EGL_FLAG_STENCIL_1BIT ? 1
					: ((mFlags & EGL_FLAG_STENCIL_8BIT) == EGL_FLAG_STENCIL_8BIT ? 8 : 0);
			mEgl = EGLBase.createFrom(mMaxClientVersion, mSharedContext,
				(mFlags & EGL_FLAG_DEPTH_BUFFER) == EGL_FLAG_DEPTH_BUFFER,
				stencilBits,
				(mFlags & EGL_FLAG_RECORDABLE) == EGL_FLAG_RECORDABLE);
		}
		if (mEgl != null) {
			mGlExtensions = null;
			int masterWidth = Math.max(width, 1);
			int masterHeight = Math.max(height, 1);
			if ((width <= 0) && (height <= 0) && (mEglMasterSurface != null)) {
				masterWidth = Math.max(masterWidth, mEglMasterSurface.getWidth());
				masterHeight = Math.max(masterHeight, mEglMasterSurface.getHeight());
			}
			if (mEglMasterSurface != null) {
				mEglMasterSurface.release();
				mEglMasterSurface = null;
			}
			if (GLUtils.isSupportedSurface(surface)) {
				mEglMasterSurface = mEgl.createFromSurface(surface);
			} else {
				mEglMasterSurface = mEgl.createOffscreen(masterWidth, masterHeight);
			}
			mGLThreadId = Thread.currentThread().getId();
			makeDefault();
		} else {
			throw new IllegalArgumentException("failed to create EGLBase");
		}
		if (!isOutputVersionInfo) {
			// 初回だけログにOpenGL|ESの情報を出力する
			isOutputVersionInfo = true;
			logVersionInfo();
		}
		// extension文字列をキャッシュを試みるため1回呼んでおく
		isOES3Supported();
	}

	/**
	 * EGLBase(EGLコンテキスト操作用オブジェクト)を取得
	 * @return
	 * @throws IllegalStateException
	 */
	@NonNull
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
	public EGLBase.IConfig<?> getConfig() throws IllegalStateException {
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
	 * マスターサーフェースの幅を取得
	 * マスターサーフェースが存在していればその高さ、存在していなければ1
	 * @return
	 */
	public int getMasterWidth() {
		synchronized (mSync) {
			return mEglMasterSurface != null ? mEglMasterSurface.getWidth() : 1;
		}
	}

	/**
	 * マスターサーフェースの高さを取得
	 * マスターサーフェースが存在していればその高さ、存在していなければ1
	 * @return
	 */
	public int getMasterHeight() {
		synchronized (mSync) {
			return mEglMasterSurface != null ? mEglMasterSurface.getHeight() : 1;
		}
	}

	/**
	 * IContext(EGLレンダリングコンテキストのラッパー)を取得
	 * @return
	 * @throws IllegalStateException
	 */
	@NonNull
	public EGLBase.IContext<?> getContext() throws IllegalStateException {
		synchronized (mSync) {
			final EGLBase.IContext<?> result = mEgl != null ? mEgl.getContext() : null;
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
	@WorkerThread
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
	@WorkerThread
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
	@WorkerThread
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
	 * 基本的には使わないはず
	 *
	 * eglWaitGL: コマンドキュー内のコマンドをすべて転送する, GLES20.glFinish()と同様の効果
	 * eglWaitNative: GPU側の描画処理が終了するまで実行をブロックする
	 */
	@WorkerThread
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
	@WorkerThread
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
	 * 基本的には使わないはず
	 */
	@WorkerThread
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
	 * GLコンテキストを保持しているスレッド上かどうかを取得
	 * @return
	 */
	public boolean inGLThread() {
		synchronized (mSync) {
			return mGLThreadId == Thread.currentThread().getId();
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
	 * @return GLコンテキストが無効なら0が返る, 有効なら0, 1, 2, 3のいずれか(API>=16なので1が返ることはないはずだけど)
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
	@WorkerThread
	public boolean hasExtension(@NonNull final String extension) {
		if (TextUtils.isEmpty(mGlExtensions)) {
			// GLES30#glGetStringはGLES20の継承メソッドなので条件分岐不要
			mGlExtensions = GLES20.glGetString(GLES20.GL_EXTENSIONS); // API >= 8
		}
		return (mGlExtensions != null) && mGlExtensions.contains(extension);
	}

	/**
	 * GLES2/3でGL_OES_EGL_image_externalに対応しているかどうか
	 * @return
	 */
	public boolean isOES2Supported() {
		return isGLES2() && hasExtension("GL_OES_EGL_image_external");
	}

	/**
	 * GLES3でGL_OES_EGL_image_external_essl3に対応しているかどうか
	 * @return
	 */
	public boolean isOES3Supported() {
		return isGLES3() && hasExtension("GL_OES_EGL_image_external_essl3");
	}

	/**
	 * GLスレッド上で実行されているかどうかをチェックしてGLスレッド上で無ければ
	 * IllegalThreadStateExceptionを投げる
	 * @throws IllegalStateException
	 */
	private void checkGLThread() throws IllegalStateException {
		if (!inGLThread()) {
			throw new IllegalThreadStateException("Not a GL thread");
		}
	}
//--------------------------------------------------------------------------------
	/**
	 * Writes GL version info to the log.
	 */
	@SuppressLint("InlinedApi")
	@WorkerThread
	public static void logVersionInfo() {
		Log.i(TAG, "vendor:" + GLES20.glGetString(GLES20.GL_VENDOR));
		Log.i(TAG, "renderer:" + GLES20.glGetString(GLES20.GL_RENDERER));
		Log.i(TAG, "version:" + GLES20.glGetString(GLES20.GL_VERSION));
		Log.i(TAG, "supported version:" + supportedGLESVersion());
		Log.i(TAG, "extensions:" + GLES20.glGetString(GLES20.GL_EXTENSIONS));
	}

	/**
	 * 対応するOpenGL|ESのバージョンを取得する
	 * @return 0以下なら何らかの理由でバージョンを取得できなかった
	 * 			それ以外は整数部: メジャーバージョン, 小数部: マイナーバージョン
	 */
	@SuppressLint("InlinedApi")
	@WorkerThread
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
