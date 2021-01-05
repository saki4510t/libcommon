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
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLExt;
import android.opengl.EGLSurface;
import android.opengl.GLES10;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.util.Log;

import com.serenegiant.system.BuildCheck;

/**
 * EGLレンダリングコンテキストを生成＆使用するためのヘルパークラス
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
/*package*/ class EGLBase14 extends EGLBase {	// API >= 17
	private static final boolean DEBUG = false;	// TODO set false on release
	private static final String TAG = EGLBase14.class.getSimpleName();

	private static final Context EGL_NO_CONTEXT = wrap(EGL14.EGL_NO_CONTEXT);

	/**
	 * EGLレンダリングコンテキストラップしてContext extends IContextを生成する
	 * @param context
	 * @return
	 */
	public static Context wrap(@NonNull final EGLContext context) {
		return new Context(context);
	}

	/**
	 * EGLConfigをラップしてConfig extends IConfigを返す
	 * @param eglConfig
	 * @return
	 */
	public static Config wrap(@NonNull final EGLConfig eglConfig) {
		return new Config(eglConfig);
	}

//--------------------------------------------------------------------------------

	/**
	 * EGLレンダリングコンテキストのホルダークラス
	 */
	public static class Context extends IContext {
		public final EGLContext eglContext;

		private Context(final EGLContext context) {
			eglContext = context;
		}
		
		@Override
		@SuppressLint("NewApi")
		public long getNativeHandle() {
			return eglContext != null ?
				(BuildCheck.isLollipop()
					? eglContext.getNativeHandle() : eglContext.getHandle()) : 0L;
		}

		@Override
		public Object getEGLContext() {
			return eglContext;
		}

		@NonNull
		@Override
		public String toString() {
			return "Context{" +
				"eglContext=" + eglContext +
				'}';
		}
	} // Context

	public static class Config extends IConfig {
		public final EGLConfig eglConfig;

		private Config(final EGLConfig eglConfig) {
			this.eglConfig = eglConfig;
		}

		@Override
		public EGLConfig getEGLConfig() {
			return eglConfig;
		}

		@NonNull
		@Override
		public String toString() {
			return "Config{" +
				"eglConfig=" + eglConfig +
				'}';
		}
	} // Config

	/**
	 * EGLレンダリングコンテキストに紐付ける描画オブジェクト
	 */
	private static class EglSurface implements IEglSurface {
		@NonNull
		private final EGLBase14 mEglBase;
		@NonNull
		private EGLSurface mEglSurface;
		private boolean mOwnSurface;
		private int viewPortX, viewPortY, viewPortWidth, viewPortHeight;

		/**
		 * Surface(Surface/SurfaceTexture/SurfaceHolder/SurfaceView)に
		 * 関係付けられたEglSurfaceを生成するコンストラクタ
		 * @param eglBase
		 * @param surface
		 * @throws IllegalArgumentException
		 */
		private EglSurface(@NonNull final EGLBase14 eglBase, final Object surface)
			throws IllegalArgumentException {

//			if (DEBUG) Log.v(TAG, "EglSurface:");
			mEglBase = eglBase;
			if (GLUtils.isSupportedSurface(surface)) {
				mEglSurface = mEglBase.createWindowSurface(surface);
				mOwnSurface = true;
				setViewPort(0, 0, getWidth(), getHeight());
			} else {
				throw new IllegalArgumentException("unsupported surface");
			}
		}

		/**
		 * 指定した大きさを持つオフスクリーンEglSurface(PBuffer)
		 * width/heightの少なくとも一方が0以下なら最小サイズとして1x1のオフスクリーンにする
		 * @param eglBase
		 * @param width
		 * @param height
		 */
		private EglSurface(@NonNull final EGLBase14 eglBase,
			final int width, final int height) {

//			if (DEBUG) Log.v(TAG, "EglSurface:");
			mEglBase = eglBase;
			if ((width <= 0) || (height <= 0)) {
				// width/heightの少なくとも一方が0以下なら最小サイズで1x1のオフスクリーンを生成する
				mEglSurface = mEglBase.createOffscreenSurface(1, 1);
			} else {
				mEglSurface = mEglBase.createOffscreenSurface(width, height);
			}
			mOwnSurface = true;
			setViewPort(0, 0, getWidth(), getHeight());
		}

		/**
		 * eglGetCurrentSurfaceで取得したEGLSurfaceをラップする
		 * @param eglBase
		 */
		private EglSurface(@NonNull final EGLBase14 eglBase) {
			mEglBase = eglBase;
			mEglSurface = EGL14.eglGetCurrentSurface(EGL14.EGL_DRAW);
			mOwnSurface = false;
			setViewPort(0, 0, getWidth(), getHeight());
		}

		@Override
		public void release() {
//			if (DEBUG) Log.v(TAG, "EglSurface:release:");
			mEglBase.makeDefault();
			if (mOwnSurface) {
				mEglBase.destroyWindowSurface(mEglSurface);
			}
			mEglSurface = EGL14.EGL_NO_SURFACE;
		}

		@Override
		public void makeCurrent() {
			mEglBase.makeCurrent(mEglSurface);
			setViewPort(viewPortX, viewPortY, viewPortWidth, viewPortHeight);
		}

		/**
		 * Viewportを設定
		 * ここで設定した値は次回以降makeCurrentを呼んだときに復帰される
		 * @param x
		 * @param y
		 * @param width
		 * @param height
		 */
		@Override
		public void setViewPort(final int x, final int y, final int width, final int height) {
			viewPortX = x;
			viewPortY = y;
			viewPortWidth = width;
			viewPortHeight = height;

			final int glVersion = mEglBase.getGlVersion();
			if (glVersion >= 3) {
				GLES30.glViewport(x, y, width, height);
			} else if (mEglBase.getGlVersion() >= 2) {
				GLES20.glViewport(x, y, width, height);
			} else {
				GLES10.glViewport(x, y, width, height);
			}
		}

		@Override
		public void swap() {
			mEglBase.swap(mEglSurface);
		}

		@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
		@Override
		public void swap(final long presentationTimeNs) {
			mEglBase.swap(mEglSurface, presentationTimeNs);
		}

		@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
		public void setPresentationTime(final long presentationTimeNs) {
			EGLExt.eglPresentationTimeANDROID(mEglBase.mEglDisplay,	// API>=18
				mEglSurface, presentationTimeNs);
		}

		@Override
		public boolean isValid() {
			return (mEglSurface != EGL14.EGL_NO_SURFACE)
				&& (mEglBase.getSurfaceWidth(mEglSurface) > 0)
				&& (mEglBase.getSurfaceHeight(mEglSurface) > 0);
		}

		@Override
		public int getWidth() {
			return mEglBase.getSurfaceWidth(mEglSurface);
		}

		@Override
		public int getHeight() {
			return mEglBase.getSurfaceHeight(mEglSurface);
		}

		@NonNull
		@Override
		public String toString() {
			return "EglSurface{" +
				"mEglBase=" + mEglBase +
				", mEglSurface=" + mEglSurface +
				", mOwnSurface=" + mOwnSurface +
				", viewPortX=" + viewPortX +
				", viewPortY=" + viewPortY +
				", viewPortWidth=" + viewPortWidth +
				", viewPortHeight=" + viewPortHeight +
				'}';
		}
	} // EglSurface

//--------------------------------------------------------------------------------
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
	/*package*/ static EGLBase createFromCurrentImpl(final int maxClientVersion,
		final boolean withDepthBuffer, final int stencilBits, final boolean isRecordable) {

		Context context = null;
		final EGLContext currentContext = EGL14.eglGetCurrentContext();
		final EGLSurface currentSurface = EGL14.eglGetCurrentSurface(EGL14.EGL_DRAW);
		if ((currentContext != null) && (currentSurface != null)) {
			context = wrap(currentContext);
		}
		return new EGLBase14(maxClientVersion, context, withDepthBuffer, stencilBits, isRecordable);
	}

//--------------------------------------------------------------------------------
	@NonNull
	private Context mContext = EGL_NO_CONTEXT;
	@NonNull
	private EGLDisplay mEglDisplay = EGL14.EGL_NO_DISPLAY;
	   private Config mEglConfig = null;
	private int mGlVersion = 2;

	private EGLContext mDefaultContext = EGL14.EGL_NO_CONTEXT;

	/**
	 * コンストラクタ
	 * @param maxClientVersion
	 * @param sharedContext
	 * @param withDepthBuffer
	 * @param isRecordable
	 */
	/*package*/ EGLBase14(final int maxClientVersion,
		@Nullable final Context sharedContext, final boolean withDepthBuffer,
		final int stencilBits, final boolean isRecordable) {

		super();
//		if (DEBUG) Log.v(TAG, "Constructor:");
		init(maxClientVersion, sharedContext, withDepthBuffer, stencilBits, isRecordable);
	}

	/**
	 * コンストラクタ
	 * @param maxClientVersion
	 * @param withDepthBuffer
	 * @param isRecordable
	 */
	/*package*/ EGLBase14(final int maxClientVersion,
		final boolean withDepthBuffer,
		final int stencilBits, final boolean isRecordable) {

		super();
//		if (DEBUG) Log.v(TAG, "Constructor:");
		init(maxClientVersion, wrap(EGL14.eglGetCurrentContext()),
			withDepthBuffer, stencilBits, isRecordable);
	}

	/**
	 * 関連するリソースを破棄する
	 */
	@Override
    public void release() {
//		if (DEBUG) Log.v(TAG, "release:");
        if (mEglDisplay != EGL14.EGL_NO_DISPLAY) {
	    	destroyContext();
	        EGL14.eglTerminate(mEglDisplay);
	        EGL14.eglReleaseThread();
        }
        mEglDisplay = EGL14.EGL_NO_DISPLAY;
        mContext = EGL_NO_CONTEXT;
    }

	/**
	 * 指定したSurfaceからEglSurfaceを生成する
	 * 生成したEglSurfaceをmakeCurrentした状態で戻る
	 * @param nativeWindow Surface/SurfaceTexture/SurfaceHolder/SurfaceView
	 * @return
	 */
	@Override
	public IEglSurface createFromSurface(final Object nativeWindow) {
//		if (DEBUG) Log.v(TAG, "createFromSurface:");
		final IEglSurface result = new EglSurface(this, nativeWindow);
		result.makeCurrent();
		return result;
	}

	/**
	 * 指定した大きさのオフスクリーンEglSurfaceを生成する
	 * 生成したEglSurfaceをmakeCurrentした状態で戻る
	 * @param width PBufferオフスクリーンのサイズ(0以下はだめ)
	 * @param height
	 * @return
	 */
	@Override
	public IEglSurface createOffscreen(final int width, final int height) {
//		if (DEBUG) Log.v(TAG, "createOffscreen:");
		final IEglSurface result = new EglSurface(this, width, height);
		result.makeCurrent();
		return result;
	}

	/**
	 * eglGetCurrentSurfaceで取得したEGLSurfaceをラップする
	 * @return
	 */
	@Override
	public IEglSurface wrapCurrent() {
//		if (DEBUG) Log.v(TAG, "createOffscreen:");
		final IEglSurface result = new EglSurface(this);
		result.makeCurrent();
		return result;
	}

	/**
	 * GLESに文字列を問い合わせる
	 * @param what
	 * @return
	 */
	@Override
 	public String queryString(final int what) {
		return EGL14.eglQueryString(mEglDisplay, what);
	}

	/**
	 * GLESバージョンを取得する
	 * @return 1, 2または3
	 */
	@Override
	public int getGlVersion() {
		return mGlVersion;
	}

	/**
	 * EGLレンダリングコンテキストが有効かどうか
	 * @return
	 */
	@Override
	public boolean isValidContext() {
		return (mContext != null) && (mContext.eglContext != EGL14.EGL_NO_CONTEXT);
	}

	/**
	 * EGLレンダリングコンテキストを取得する
	 * このEGLBaseインスタンスを使って生成したEglSurfaceをmakeCurrentした状態で
	 * eglGetCurrentContextを呼び出すのと一緒
	 * @return
	 * @throws IllegalStateException
	 */
	@Override
	public Context getContext() throws IllegalStateException {
		if (!isValidContext()) {
			throw new IllegalStateException();
		}
		return mContext;
	}

	/**
	 * EGLコンフィグを取得する
	 * @return
	 */
	@Override
	public Config getConfig() {
		return mEglConfig;
	}

	/**
	 * EGLレンダリングコンテキストとスレッドの紐付けを解除する
	 */
	@Override
	public void makeDefault() {
//		if (DEBUG) Log.v(TAG, "makeDefault:");
        if (!EGL14.eglMakeCurrent(mEglDisplay,
        	EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)) {

            Log.w("TAG", "makeDefault" + EGL14.eglGetError());
        }
	}

	/**
	 * eglWaitGLとeglWaitNativeを呼ぶ
	 *
	 * eglWaitGL: コマンドキュー内のコマンドをすべて転送する, GLES20.glFinish()と同様の効果
	 * eglWaitNative: GPU側の描画処理が終了するまで実行をブロックする
	 */
	@Override
	public void sync() {
		EGL14.eglWaitGL();	// GLES20.glFinish()と同様の効果
		EGL14.eglWaitNative(EGL14.EGL_CORE_NATIVE_ENGINE);
	}

	/**
	 * eglWaitGLを呼ぶ
	 * コマンドキュー内のコマンドをすべて転送する, GLES20.glFinish()と同様の効果
	 */
	@Override
	public void waitGL() {
		EGL14.eglWaitGL();	// GLES20.glFinish()と同様の効果
	}

	/**
	 * eglWaitNativeを呼ぶ
	 * GPU側の描画処理が終了するまで実行をブロックする
	 */
	@Override
	public void waitNative() {
		EGL14.eglWaitNative(EGL14.EGL_CORE_NATIVE_ENGINE);
	}

	/**
	 * 初期化の下請け
	 * @param maxClientVersion
	 * @param sharedContext
	 * @param withDepthBuffer
	 * @param stencilBits
	 * @param isRecordable
	 */
	private void init(final int maxClientVersion,
		@Nullable Context sharedContext,
		final boolean withDepthBuffer, final int stencilBits, final boolean isRecordable) {

		if (DEBUG) Log.v(TAG, "init:");
        if (mEglDisplay != EGL14.EGL_NO_DISPLAY) {
            throw new RuntimeException("EGL already set up");
        }

        mEglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        if (mEglDisplay == EGL14.EGL_NO_DISPLAY) {
            throw new RuntimeException("eglGetDisplay failed");
        }
		// EGLのバージョンを取得
		final int[] version = new int[2];
        if (!EGL14.eglInitialize(mEglDisplay, version, 0, version, 1)) {
        	mEglDisplay = EGL14.EGL_NO_DISPLAY;
            throw new RuntimeException("eglInitialize failed");
        }

		sharedContext = (sharedContext != null) ? sharedContext : EGL_NO_CONTEXT;

		if (DEBUG) Log.d(TAG, "init:maxClientVersion=" + maxClientVersion);
		EGLConfig config;
		if (maxClientVersion >= 3) {
			if (DEBUG) Log.d(TAG, "init:GLES3で取得できるかどうか試してみる");
			config = getConfig(3, withDepthBuffer, stencilBits, isRecordable);
			if (config != null) {
				final EGLContext context = createContext(sharedContext, config, 3);
				if (EGL14.eglGetError() == EGL14.EGL_SUCCESS) {
					// ここは例外生成したくないのでcheckEglErrorの代わりに自前でチェック
					mEglConfig = wrap(config);
					mContext = wrap(context);
					mGlVersion = 3;
				}
			}
		}
		// GLES3で取得できなかった時はGLES2を試みる
		if ((maxClientVersion >= 2) && !isValidContext()) {

			if (DEBUG) Log.d(TAG, "init:GLES2を試みる");
			config = getConfig(2, withDepthBuffer, stencilBits, isRecordable);
			if (config == null) {
				throw new RuntimeException("chooseConfig failed");
			}
			try {
				// create EGL rendering context
				final EGLContext context = createContext(sharedContext, config, 2);
				checkEglError("eglCreateContext");
				mEglConfig = wrap(config);
				mContext = wrap(context);
				mGlVersion = 2;
			} catch (final Exception e) {
				if (isRecordable) {
					config = getConfig(2, withDepthBuffer, stencilBits, false);
					if (config == null) {
						throw new RuntimeException("chooseConfig failed");
					}
					// create EGL rendering context
					final EGLContext context = createContext(sharedContext, config, 2);
					checkEglError("eglCreateContext");
					mEglConfig = wrap(config);
					mContext = wrap(context);
					mGlVersion = 2;
				}
			}
        }
        if (!isValidContext()) {
			config = getConfig(1, withDepthBuffer, stencilBits, isRecordable);
			if (config == null) {
				throw new RuntimeException("chooseConfig failed");
			}
			// create EGL rendering context
			final EGLContext context = createContext(sharedContext, config, 1);
			checkEglError("eglCreateContext");
			mEglConfig = wrap(config);
			mContext = wrap(context);
			mGlVersion = 1;
		}
        // confirm whether the EGL rendering context is successfully created
        final int[] values = new int[1];
        EGL14.eglQueryContext(mEglDisplay,
        	mContext.eglContext, EGL14.EGL_CONTEXT_CLIENT_VERSION, values, 0);
		if (EGL14.eglGetError() == EGL14.EGL_SUCCESS) {
			Log.d(TAG, String.format("EGLContext created, client version %d(request %d) ",
				values[0], maxClientVersion));
		}
        makeDefault();	// makeCurrent(EGL14.EGL_NO_SURFACE);
	}

	/**
	 * change context to draw this window surface
	 * @return
	 */
	private boolean makeCurrent(final EGLSurface surface) {
//		if (DEBUG) Log.v(TAG, "makeCurrent:");
/*        if (mEglDisplay == null) {
			if (DEBUG) Log.d(TAG, "makeCurrent:eglDisplay not initialized");
        } */
        if (surface == null || surface == EGL14.EGL_NO_SURFACE) {
            final int error = EGL14.eglGetError();
            if (error == EGL14.EGL_BAD_NATIVE_WINDOW) {
                Log.e(TAG, "makeCurrent:returned EGL_BAD_NATIVE_WINDOW.");
            }
            return false;
        }
        // attach EGL rendering context to specific EGL window surface
        if (!EGL14.eglMakeCurrent(mEglDisplay, surface, surface, mContext.eglContext)) {
            Log.w("TAG", "eglMakeCurrent" + EGL14.eglGetError());
            return false;
        }
        return true;
	}

	private int swap(final EGLSurface surface) {
//		if (DEBUG) Log.v(TAG, "swap:");
        if (!EGL14.eglSwapBuffers(mEglDisplay, surface)) {
        	final int err = EGL14.eglGetError();
//        	if (DEBUG) Log.w(TAG, "swap:err=" + err);
            return err;
        }
        return EGL14.EGL_SUCCESS;
    }

    private int swap(final EGLSurface surface, final long presentationTimeNs) {
//		if (DEBUG) Log.v(TAG, "swap:");
		EGLExt.eglPresentationTimeANDROID(mEglDisplay, surface, presentationTimeNs);	// API>=18
        if (!EGL14.eglSwapBuffers(mEglDisplay, surface)) {
        	final int err = EGL14.eglGetError();
//        	if (DEBUG) Log.w(TAG, "swap:err=" + err);
            return err;
        }
        return EGL14.EGL_SUCCESS;
	}

    private EGLContext createContext(final Context sharedContext,
    	final EGLConfig config, final int version) {

		if (DEBUG) Log.v(TAG, "createContext:version=" + version);

        final int[] attrib_list = {
        	EGL14.EGL_CONTEXT_CLIENT_VERSION, version,
        	EGL14.EGL_NONE
        };
		final EGLContext context = EGL14.eglCreateContext(mEglDisplay,
			config, sharedContext.eglContext, attrib_list, 0);
//		checkEglError("eglCreateContext");
        return context;
    }

    private void destroyContext() {
//		if (DEBUG) Log.v(TAG, "destroyContext:");

        if (!EGL14.eglDestroyContext(mEglDisplay, mContext.eglContext)) {
            Log.e("destroyContext", "display:" + mEglDisplay
            	+ " context: " + mContext.eglContext);
            Log.e(TAG, "eglDestroyContext:" + EGL14.eglGetError());
        }
        mContext = EGL_NO_CONTEXT;
        if (mDefaultContext != EGL14.EGL_NO_CONTEXT) {
	        if (!EGL14.eglDestroyContext(mEglDisplay, mDefaultContext)) {
	            Log.e("destroyContext", "display:" + mEglDisplay
	            	+ " context: " + mDefaultContext);
	            Log.e(TAG, "eglDestroyContext:" + EGL14.eglGetError());
	        }
	        mDefaultContext = EGL14.EGL_NO_CONTEXT;
        }
    }

	private final int[] mSurfaceDimension = new int[2];
	private final int getSurfaceWidth(final EGLSurface surface) {
		final boolean ret = EGL14.eglQuerySurface(mEglDisplay,
			surface, EGL14.EGL_WIDTH, mSurfaceDimension, 0);
		if (!ret) mSurfaceDimension[0] = 0;
		return mSurfaceDimension[0];
	}

	private final int getSurfaceHeight(final EGLSurface surface) {
		final boolean ret = EGL14.eglQuerySurface(mEglDisplay,
			surface, EGL14.EGL_HEIGHT, mSurfaceDimension, 1);
		if (!ret) mSurfaceDimension[1] = 0;
		return mSurfaceDimension[1];
	}

	/**
	 * nativeWindow should be one of the Surface, SurfaceHolder and SurfaceTexture
	 * @param nativeWindow
	 * @return
	 */
	@NonNull
    private final EGLSurface createWindowSurface(final Object nativeWindow)
    	throws IllegalArgumentException {

//		if (DEBUG) Log.v(TAG, "createWindowSurface:nativeWindow=" + nativeWindow);

        final int[] surfaceAttribs = {
			EGL14.EGL_NONE
        };
		EGLSurface result;
		try {
			result = EGL14.eglCreateWindowSurface(mEglDisplay,
				mEglConfig.eglConfig, nativeWindow, surfaceAttribs, 0);
			if (result == null || result == EGL14.EGL_NO_SURFACE) {
				final int error = EGL14.eglGetError();
				if (error == EGL14.EGL_BAD_NATIVE_WINDOW) {
					Log.e(TAG, "createWindowSurface returned EGL_BAD_NATIVE_WINDOW.");
				}
				throw new RuntimeException("createWindowSurface failed error=" + error);
			}
			makeCurrent(result);
			// 画面サイズ・フォーマットの取得
		} catch (final IllegalArgumentException e) {
			throw e;
		} catch (final Exception e) {
			Log.e(TAG, "eglCreateWindowSurface", e);
			throw new IllegalArgumentException(e);
		}
		return result;
	}

    /**
     * Creates an EGL surface associated with an offscreen buffer.
     */
    @NonNull
    private final EGLSurface createOffscreenSurface(final int width, final int height)
		throws IllegalArgumentException {

//		if (DEBUG) Log.v(TAG, "createOffscreenSurface:");
        final int[] surfaceAttribs = {
                EGL14.EGL_WIDTH, width,
                EGL14.EGL_HEIGHT, height,
                EGL14.EGL_NONE
        };
		EGLSurface result;
		try {
			result = EGL14.eglCreatePbufferSurface(mEglDisplay,
				mEglConfig.eglConfig, surfaceAttribs, 0);
	        checkEglError("eglCreatePbufferSurface");
			if (result == null || result == EGL14.EGL_NO_SURFACE) {
				final int error = EGL14.eglGetError();
				throw new RuntimeException("createOffscreenSurface failed error=" + error);
	        }
		} catch (final IllegalArgumentException e) {
			throw e;
		} catch (final Exception e) {
			Log.e(TAG, "createOffscreenSurface", e);
			throw new IllegalArgumentException(e);
		}
		return result;
    }

	private void destroyWindowSurface(EGLSurface surface) {
//		if (DEBUG) Log.v(TAG, "destroySurface:");

        if (surface != EGL14.EGL_NO_SURFACE) {
        	EGL14.eglMakeCurrent(mEglDisplay,
        		EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
        	EGL14.eglDestroySurface(mEglDisplay, surface);
        }
//		if (DEBUG) Log.v(TAG, "destroySurface:finished");
	}

    private void checkEglError(final String msg) {
        int error;
        if ((error = EGL14.eglGetError()) != EGL14.EGL_SUCCESS) {
            throw new RuntimeException(msg + ": EGL error: 0x" + Integer.toHexString(error));
        }
    }

    private EGLConfig getConfig(final int version,
    	final boolean hasDepthBuffer, final int stencilBits, final boolean isRecordable) {

		if (DEBUG) Log.v(TAG, "getConfig:version=" + version
			+ ",hasDepthBuffer=" + hasDepthBuffer + ",stencilBits=" + stencilBits
			+ ",isRecordable=" + isRecordable);
		int renderableType = EGL_OPENGL_ES2_BIT;
		if (version >= 3) {
			renderableType |= EGL_OPENGL_ES3_BIT_KHR;
		}
        final int[] attribList = {
			EGL14.EGL_RENDERABLE_TYPE, renderableType,
			EGL14.EGL_RED_SIZE, 8,
			EGL14.EGL_GREEN_SIZE, 8,
			EGL14.EGL_BLUE_SIZE, 8,
			EGL14.EGL_ALPHA_SIZE, 8,
//        	EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT | swapBehavior,
			EGL14.EGL_NONE, EGL14.EGL_NONE,	//EGL14.EGL_STENCIL_SIZE, 8,
			// this flag need to recording of MediaCodec
			EGL14.EGL_NONE, EGL14.EGL_NONE,	//EGL_RECORDABLE_ANDROID, 1,
			EGL14.EGL_NONE,	EGL14.EGL_NONE,	//	with_depth_buffer ? EGL14.EGL_DEPTH_SIZE : EGL14.EGL_NONE,
											// with_depth_buffer ? 16 : 0,
			EGL14.EGL_NONE
        };
        int offset = 10;
        if (stencilBits > 0) {	// ステンシルバッファ(常時未使用)
        	attribList[offset++] = EGL14.EGL_STENCIL_SIZE;
        	attribList[offset++] = stencilBits;
        }
        if (hasDepthBuffer) {	// デプスバッファ
        	attribList[offset++] = EGL14.EGL_DEPTH_SIZE;
        	attribList[offset++] = 16;
        }
        if (isRecordable && BuildCheck.isAndroid4_3()) {// MediaCodecの入力用Surfaceの場合
        	attribList[offset++] = EGL_RECORDABLE_ANDROID;
        	attribList[offset++] = 1;
        }
        for (int i = attribList.length - 1; i >= offset; i--) {
        	attribList[i] = EGL14.EGL_NONE;
        }
        EGLConfig config = internalGetConfig(attribList);
		if ((config == null) && (version == 2)) {
			if (isRecordable) {
				// EGL_RECORDABLE_ANDROIDをつけると失敗する機種もあるので取り除く
				final int n = attribList.length;
				for (int i = 10; i < n - 1; i += 2) {
					if (attribList[i] == EGL_RECORDABLE_ANDROID) {
						for (int j = i; j < n; j++) {
							attribList[j] = EGL14.EGL_NONE;
						}
						break;
					}
				}
				config = internalGetConfig(attribList);
			}
		}
		if (config == null) {
			Log.w(TAG, "try to fallback to RGB565");
			attribList[3] = 5;
			attribList[5] = 6;
			attribList[7] = 5;
			attribList[9] = 0;
			config = internalGetConfig(attribList);
		}
        return config;
    }

	private EGLConfig internalGetConfig(final int[] attribList) {
		final EGLConfig[] configs = new EGLConfig[1];
		final int[] numConfigs = new int[1];
		if (!EGL14.eglChooseConfig(mEglDisplay,
			attribList, 0, configs, 0, configs.length, numConfigs, 0)) {
			return null;
		}
		return configs[0];
	}
}
