package com.serenegiant.glutils;

import android.os.Build;

public abstract class EGLBase {
	public static final Object EGL_LOCK = new Object();

	public static final int EGL_RECORDABLE_ANDROID = 0x3142;
	public static final int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
	public static final int EGL_OPENGL_ES2_BIT = 4;
	public static final int EGL_OPENGL_ES3_BIT_KHR = 0x0040;
//	public static final int EGL_SWAP_BEHAVIOR_PRESERVED_BIT = 0x0400;

	/**
	 * EGL生成のヘルパーメソッド, 環境に応じてEGLBase10またはEGLBase14を生成する
	 * @param sharedContext
	 * @param withDepthBuffer
	 * @param isRecordable
	 * @return
	 */
	public static EGLBase createFrom(final IContext sharedContext, final boolean withDepthBuffer, final boolean isRecordable) {
		// FIXME 今はEGLBaseとEGLBase14は同等では無いのでEGLBaseを優先して生成する
		if (isEGL14Supported() && (/*(sharedContext == null) ||*/ (sharedContext instanceof EGLBase14.Context))) {
			return new EGLBase14((EGLBase14.Context)sharedContext, withDepthBuffer, isRecordable);
		} else {
			return new EGLBase10((EGLBase10.Context)sharedContext, withDepthBuffer, isRecordable);
		}
	}

	/**
	 * EGLレンダリングコンテキストのホルダークラス
	 */
	public static abstract class IContext {
	}

	/**
	 * EGLレンダリングコンテキストに紐付ける描画オブジェクト
	 */
	public interface IEglSurface {
		public void makeCurrent();
		public void swap();
		public IContext getContext();
		public void release();
		public boolean isValid();
	}

	public static boolean isEGL14Supported() {
		return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2);
	}

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
	 * @return 2または3
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
	 * 指定したSurfaceからEglSurfaceを生成する
	 * 生成したEglSurfaceをmakeCurrentした状態で戻る
	 * Android4.1.2だとSurfaceを使えない。SurfaceTexture/SufaceHolderの場合は内部でSurfaceを生成して使っているにもかかわらず。
	 * しかもAIDLで送れるのはSurfaceだけなのに
	 * @param nativeWindow Surface/SurfaceTexture/SurfaceHolder
	 * @return
	 */
	public abstract IEglSurface createFromSurface(final Object nativeWindow);
	/**
	 * 指定した大きさのオフスクリーンEglSurfaceを生成する
	 * 生成したEglSurfaceをmakeCurrentした状態で戻る
	 * @param width PBufferオフスクリーンのサイズ(0以下はだめ)
	 * @param height
	 * @return
	 */
	public abstract IEglSurface createOffscreen(final int width, final int height);
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
}
