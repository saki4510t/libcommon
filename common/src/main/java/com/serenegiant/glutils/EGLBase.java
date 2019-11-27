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

import android.annotation.TargetApi;
import android.os.Build;

import androidx.annotation.Nullable;

/**
 * EGLレンダリングコンテキストを生成＆使用するためのヘルパークラス
 */
public abstract class EGLBase implements EGLConst {
//--------------------------------------------------------------------------------
// ヘルパーメソッド
//--------------------------------------------------------------------------------

	/**
	 * EGL生成のヘルパーメソッド, 環境に応じてEGLBase10またはEGLBase14を生成する
	 * maxClientVersion=3, ステンシルバッファなし
	 * @param sharedContext
	 * @param withDepthBuffer
	 * @param isRecordable
	 * @return
	 */
	public static EGLBase createFrom(@Nullable final IContext sharedContext,
		final boolean withDepthBuffer, final boolean isRecordable) {

		return createFrom(3, sharedContext, withDepthBuffer, 0, isRecordable);
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
	public static EGLBase createFrom(@Nullable final IContext sharedContext,
		final boolean withDepthBuffer, final int stencilBits, final boolean isRecordable) {

		return createFrom(3, sharedContext,
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
	public static EGLBase createFrom(final int maxClientVersion,
		@Nullable final IContext sharedContext, final boolean withDepthBuffer,
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
	 * 既存のレンダリングコンテキストを共有して新しいレンダリングコンテキストを生成する
	 * レンダリングコンテキストを存在していなければcreateFromでsharedContextに
	 * nullを渡したのと同じで独立したレンダリングコンテキストを生成する
	 * @param maxClientVersion
	 * @param withDepthBuffer
	 * @param stencilBits
	 * @param isRecordable
	 * @return
	 */
	public static EGLBase createShared(final int maxClientVersion,
		final boolean withDepthBuffer,
		final int stencilBits, final boolean isRecordable) {

		if (isEGL14Supported()) {
			return new EGLBase14(maxClientVersion,
				withDepthBuffer, stencilBits, isRecordable);
		} else {
			return new EGLBase10(maxClientVersion,
				withDepthBuffer, stencilBits, isRecordable);
		}
	}

	/**
	 * 現在のスレッドの既存のレンダリングコンテキストがあればそれを共有して
	 * 新しいレンダリングコンテキストを生成する
	 * 既存のレンダリングコンテキストが存在していなければ独立したレンダリングコンテキストを
	 * 生成する
	 * @param maxClientVersion
	 * @param withDepthBuffer
	 * @param stencilBits
	 * @param isRecordable
	 * @return
	 */
	public static EGLBase createFromCurrent(final int maxClientVersion,
		final boolean withDepthBuffer, final int stencilBits, final boolean isRecordable) {

		if (isEGL14Supported()) {
			return EGLBase14.createFromCurrent(maxClientVersion,
				withDepthBuffer, stencilBits, isRecordable);
		} else {
			return EGLBase10.createFromCurrent(maxClientVersion,
				withDepthBuffer, stencilBits, isRecordable);
		}
	}

	/**
	 * EGLレンダリングコンテキストのホルダークラス
	 */
	public static abstract class IContext {
		public abstract long getNativeHandle();
		public abstract Object getEGLContext();
	}

	/**
	 * EGLコンフィグのホルダークラス
	 */
	public static abstract class IConfig {
		public abstract Object getEGLConfig();
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
		@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
		public void swap(final long presentationTimeNs);
	}

	public static boolean isEGL14Supported() {
		// XXX GLES30はAPI>=18以降なんだけどAPI=18でもGLコンテキスト生成に
		// XXX 失敗する端末があるのでこちらも合わせてAP1>=21に変更
		return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP);
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
	 * EGLレンダリングコンテキストを取得する
	 * このEGLBaseインスタンスを使って生成したEglSurfaceをmakeCurrentした状態で
	 * eglGetCurrentContextを呼び出すのと一緒
	 * @return
	 */
	public abstract IContext getContext();
	/**
	 * EGLコンフィグを取得する
	 * @return
	 */
	public abstract IConfig getConfig();
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
