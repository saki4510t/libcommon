package com.serenegiant.egl;
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

import android.annotation.SuppressLint;
import android.os.Build;

import com.serenegiant.gl.GLUtils;
import com.serenegiant.gl.ISurface;
import com.serenegiant.system.BuildCheck;

import androidx.annotation.ChecksSdkIntAtLeast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * EGLレンダリングコンテキストを生成＆使用するためのヘルパークラス
 * 基本的には直接生成せずににGLContextまたはGLManagerを使うこと
 */
public abstract class EGLBase implements EGLConst {
	/**
	 * EGLConfig選択時のピクセルフォーマット
	 * RGBA888
	 */
	public static final int EGL_CONFIG_RGBA = 0;
	/**
	 * EGLConfig選択時のピクセルフォーマット
	 * RGB565
	 */
	public static final int EGL_CONFIG_RGB565 = 1;

//--------------------------------------------------------------------------------
// ヘルパーメソッド
//--------------------------------------------------------------------------------

	/**
	 * 現在のスレッドにすでにレンダリングコンテキストが存在していればIContext<?>としてラップして返す
	 * レンダリングコンテキストが存在していなければnullを返す
	 * @return
	 */
	@SuppressLint("NewApi")
	@Nullable
	public static IContext<?> wrapCurrentContext() {
		if (isEGL14Supported()) {
			return EGLBase14.wrapCurrentContextImpl();
		} else {
			return EGLBase10.wrapCurrentContextImpl();
		}
	}

	/**
	 * EGLレンダリングコンテキストラップしてIContextを生成する
	 * @param context
	 * @return
	 */
	@SuppressLint("NewApi")
	@Nullable
	public static IContext<?> wrapContext(@Nullable final Object context) {
		if (context instanceof IContext) {
			return (IContext<?>)context;
		} else if (context instanceof android.opengl.EGLContext) {
			return EGLBase14.wrap((android.opengl.EGLContext)context);
		} else if (context instanceof javax.microedition.khronos.egl.EGLContext) {
			return EGLBase10.wrap((javax.microedition.khronos.egl.EGLContext)context);
		} else if (context == null) {
			return null;
		} else {
			throw new IllegalArgumentException("Unexpected shared context," + context);
		}
	}

	/**
	 * EGLConfigをラップしてIConfigを返す
	 * @param eglConfig
	 * @return
	 */
	@SuppressLint("NewApi")
	public static IConfig<?> wrapConfig(@NonNull final Object eglConfig) {
		if (eglConfig instanceof android.opengl.EGLConfig) {
			return EGLBase14.wrap((android.opengl.EGLConfig)eglConfig);
		} else if (eglConfig instanceof javax.microedition.khronos.egl.EGLConfig) {
			return EGLBase10.wrap((javax.microedition.khronos.egl.EGLConfig)eglConfig);
		} else {
			throw new IllegalArgumentException("Unexpected egl config," + eglConfig);
		}
	}

	/**
	 * EGL生成のヘルパーメソッド, 環境に応じてEGLBase10またはEGLBase14を生成する
	 * maxClientVersion=3, ステンシルバッファなし
	 * @param sharedContext
	 * @param withDepthBuffer
	 * @param isRecordable
	 * @return
	 */
	public static EGLBase createFrom(@Nullable final IContext<?> sharedContext,
		final boolean withDepthBuffer, final boolean isRecordable) {

		return createFrom(GLUtils.getSupportedGLVersion(), sharedContext, withDepthBuffer, 0, isRecordable);
	}

	/**
	 * EGL生成のヘルパーメソッド, 環境に応じてEGLBase10またはEGLBase14を生成する
	 * maxClientVersion=3
	 * @param sharedContext
	 * @param withDepthBuffer
	 * @param stencilBits
	 * @param isRecordable
	 * @return
	 */
	public static EGLBase createFrom(@Nullable final IContext<?> sharedContext,
		final boolean withDepthBuffer, final int stencilBits, final boolean isRecordable) {

		return createFrom(GLUtils.getSupportedGLVersion(), sharedContext,
			withDepthBuffer, stencilBits, isRecordable);
	}

	/**
	 * EGL生成のヘルパーメソッド, 環境に応じてEGLBase10またはEGLBase14を生成する
	 * @param maxClientVersion
	 * @param sharedContext
	 * @param withDepthBuffer trueなら16ビットのデプスバッファ有り, falseならデプスバッファなし
	 * @param stencilBits 0以下ならステンシルバッファなし
	 * @param isRecordable
	 * @return
	 */
	@SuppressLint("NewApi")
	public static EGLBase createFrom(final int maxClientVersion,
		@Nullable final IContext<?> sharedContext, final boolean withDepthBuffer,
		final int stencilBits, final boolean isRecordable) {

		if (isEGL14Supported() && ((sharedContext == null)
			|| (sharedContext instanceof EGLBase14.Context))) {

			return new EGLBase14(maxClientVersion,
				(EGLBase14.Context)sharedContext,
				withDepthBuffer, stencilBits, isRecordable);
		} else {
			return new EGLBase10(maxClientVersion,
				(EGLBase10.Context)sharedContext,
				withDepthBuffer, stencilBits, isRecordable);
		}
	}

	/**
	 * EGL生成のヘルパーメソッド
	 * @param maxClientVersion
	 * @param sharedContext
	 * @param withDepthBuffer trueなら16ビットのデプスバッファ有り, falseならデプスバッファなし
	 * @param stencilBits 0以下ならステンシルバッファなし
	 * @param isRecordable
	 * @return
	 */
	@SuppressLint("NewApi")
	public static EGLBase createFrom(final int maxClientVersion,
		@Nullable final Object sharedContext, final boolean withDepthBuffer,
		final int stencilBits, final boolean isRecordable) {

		return createFrom(maxClientVersion, wrapContext(sharedContext),
			withDepthBuffer, stencilBits, isRecordable);
	}

	/**
	 * 現在のスレッドの既存のレンダリングコンテキストがあればそれを共有して
	 * 新しいレンダリングコンテキストを生成する
	 * 既存のレンダリングコンテキストが存在していなければ独立したレンダリングコンテキストを
	 * 生成する
	 * XXX 処理としてはcreateSharedとほぼ同じだけどcreateFromCurrentは描画先のeglSurfaceもチェックする
	 * @param maxClientVersion
	 * @param withDepthBuffer
	 * @param stencilBits
	 * @param isRecordable
	 * @return
	 */
	@SuppressLint("NewApi")
	public static EGLBase createFromCurrent(final int maxClientVersion,
		final boolean withDepthBuffer,
		final int stencilBits, final boolean isRecordable) {

		if (isEGL14Supported()) {
			return EGLBase14.createFromCurrentImpl(maxClientVersion,
				withDepthBuffer, stencilBits, isRecordable);
		} else {
			return EGLBase10.createFromCurrentImpl(maxClientVersion,
				withDepthBuffer, stencilBits, isRecordable);
		}
	}

	/**
	 * 現在のスレッド上にレンダリングコンテキストが存在しているかどうかを取得
	 * @return
	 */
	@SuppressLint("NewApi")
	public static boolean hasGLThread() {
		if (isEGL14Supported()) {
			return EGLBase14.hasGLThreadImpl();
		} else {
			return EGLBase10.hasGLThreadImpl();
		}
	}

	/**
	 * EGLレンダリングコンテキストのホルダークラス
	 */
	public static abstract class IContext<T> {
		@NonNull
		public final T eglContext;

		protected IContext(@NonNull final T eglContext) {
			this.eglContext = eglContext;
		}

		public T getEGLContext() {
			return eglContext;
		}

		public abstract long getNativeHandle();

		@NonNull
		@Override
		public String toString() {
			return "Context{" +
				"eglContext=" + eglContext +
				'}';
		}
	}

	/**
	 * EGLコンフィグのホルダークラス
	 */
	public static abstract class IConfig<T> {
		@NonNull
		public final T eglConfig;
		protected IConfig(@NonNull final T eglConfig) {
			this.eglConfig = eglConfig;
		}

		@NonNull
		public T getEGLConfig() {
			return eglConfig;
		}

		@NonNull
		@Override
		public String toString() {
			return "Config{" +
				"eglConfig=" + eglConfig +
				'}';
		}
	}

	/**
	 * EGLレンダリングコンテキストに紐付ける描画オブジェクト
	 */
	public interface IEglSurface extends ISurface {
		/**
		 * swap with presentation time[ns]
		 * only works well now when using EGLBase14
		 * @param presentationTimeNs
		 */
		public void swap(final long presentationTimeNs);
	}

	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.LOLLIPOP)
	public static boolean isEGL14Supported() {
		// XXX GLES30はAPI>=18以降なんだけどAPI=18でもGLコンテキスト生成に
		// XXX 失敗する端末があるのでこちらも合わせてAP1>=21に変更
		return BuildCheck.isAPI21();
	}

	protected EGLBase() {
	}

//--------------------------------------------------------------------------------
// インターフェースメソッド
//--------------------------------------------------------------------------------
	/**
	 * 関連するリソースを破棄する
	 */
	public abstract void release();
	/**
	 * GLESに文字列を問い合わせる
	 * @param what
	 * @return
	 */
	public abstract String queryString(final int what);
	/**
	 * GLESバージョンを取得する
	 * @return 1, 2または3
	 */
	public abstract int getGlVersion();
	/**
	 * EGLレンダリングコンテキストが有効かどうか
	 * @return
	 */
	public abstract boolean isValidContext();
	/**
	 * EGLレンダリングコンテキストを取得する
	 * このEGLBaseインスタンスを使って生成したEglSurfaceをmakeCurrentした状態で
	 * eglGetCurrentContextを呼び出すのと一緒
	 * @return
	 * @throws IllegalStateException
	 */
	@NonNull
	public abstract IContext<?> getContext() throws IllegalStateException;
	/**
	 * EGLコンフィグを取得する
	 * @return
	 */
	@NonNull
	public abstract IConfig<?> getConfig();
	/**
	 * 指定したSurfaceからIEglSurfaceを生成する
	 * 生成したIEglSurfaceをmakeCurrentした状態で戻る
	 * @param nativeWindow Surface/SurfaceTexture/SurfaceHolder/SurfaceView
	 * @return
	 */
	public abstract IEglSurface createFromSurface(final Object nativeWindow);
	/**
	 * 指定した大きさのオフスクリーンIEglSurfaceを生成する
	 * 生成したIEglSurfaceをmakeCurrentした状態で戻る
	 * @param width PBufferオフスクリーンのサイズ(0以下はだめ)
	 * @param height
	 * @return
	 */
	public abstract IEglSurface createOffscreen(final int width, final int height);
	/**
	 * eglGetCurrentSurfaceで取得したEGLSurfaceをラップする
	 * @return
	 */
	public abstract IEglSurface wrapCurrent();
	/**
	 * EGLレンダリングコンテキストとスレッドの紐付けを解除する
	 */
	public abstract void makeDefault();

	/**
	 * eglWaitGLとeglWaitNativeを呼ぶ
	 *
	 * eglWaitGL: コマンドキュー内のコマンドをすべて転送する, GLES20.glFinish()と同様の効果
	 * eglWaitNative: GPU側の描画処理が終了するまで実行をブロックする
	 */
	public abstract void sync();

	/**
	 * eglWaitGLを呼ぶ
	 * コマンドキュー内のコマンドをすべて転送する, GLES20.glFinish()と同様の効果
	 */
	public abstract void waitGL();

	/**
	 * eglWaitNativeを呼ぶ
	 * GPU側の描画処理が終了するまで実行をブロックする
	 */
	public abstract void waitNative();

	/**
	 * GLES3で初期化したかどうか
	 * @return
	 */
	public boolean isGLES3() {
		return getGlVersion() >= 3;
	}

	/**
	 * GLES2またはGLES3で初期化したかどうか
	 * @return
	 */
	public boolean isGLES2() {
		return getGlVersion() >= 2;
	}
}
