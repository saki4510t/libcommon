package com.serenegiant.glutils;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2016 saki t_saki@serenegiant.com
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
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import javax.microedition.khronos.egl.EGL10;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class EGLBase14 extends EGLBase {	// API >= 17
	private static final boolean DEBUG = false;	// TODO set false on release
	private static final String TAG = "EGLBase14";

	private static final Context EGL_NO_CONTEXT = new Context(EGL14.EGL_NO_CONTEXT);

    private Config mEglConfig = null;
    @NonNull private Context mContext = EGL_NO_CONTEXT;
//	private EGLContext mEglContext = EGL14.EGL_NO_CONTEXT;
	private EGLDisplay mEglDisplay = EGL14.EGL_NO_DISPLAY;
	private EGLContext mDefaultContext = EGL14.EGL_NO_CONTEXT;

	/**
	 * EGLレンダリングコンテキストのホルダークラス
	 */
	public static class Context extends IContext {
		private final EGLContext eglContext;

		private Context(final EGLContext context) {
			eglContext = context;
		}
	}

	public static class Config extends IConfig {
		private final EGLConfig eglConfig;

		Config(final EGLConfig eglConfig) {
			this.eglConfig = eglConfig;
		}
	}

	/**
	 * EGLレンダリングコンテキストに紐付ける描画オブジェクト
	 */
	public static class EglSurface implements IEglSurface {
		private final EGLBase14 mEglBase;
		private EGLSurface mEglSurface = EGL14.EGL_NO_SURFACE;

		private EglSurface(final EGLBase14 eglBase, final Object surface) {
			if (DEBUG) Log.v(TAG, "EglSurface:");
			if (!(surface instanceof SurfaceView)
				&& !(surface instanceof Surface)
				&& !(surface instanceof SurfaceHolder))
				throw new IllegalArgumentException("unsupported surface");
			mEglBase = eglBase;
			mEglSurface = mEglBase.createWindowSurface(surface);
		}

		private EglSurface(final EGLBase14 egl, final int width, final int height) {
			if (DEBUG) Log.v(TAG, "EglSurface:");
			mEglBase = egl;
			mEglSurface = mEglBase.createOffscreenSurface(width, height);
		}

		@Override
		public void makeCurrent() {
			mEglBase.makeCurrent(mEglSurface);
		}

		@Override
		public void swap() {
			mEglBase.swap(mEglSurface);
		}

		@Override
		public IContext getContext() {
			return mEglBase.getContext();
		}

		@Override
		public boolean isValid() {
			return (mEglSurface != null) && (mEglSurface != EGL14.EGL_NO_SURFACE)
				&& (mEglBase.getSurfaceWidth(mEglSurface) > 0) && (mEglBase.getSurfaceHeight(mEglSurface) > 0);
		}

		@Override
		public void release() {
			if (DEBUG) Log.v(TAG, "EglSurface:release:");
			mEglBase.makeDefault();
			mEglBase.destroyWindowSurface(mEglSurface);
	        mEglSurface = EGL14.EGL_NO_SURFACE;
		}
	}

	/**
	 * コンストラクタ
	 * @param sharedContext
	 * @param withDepthBuffer
	 * @param isRecordable
	 */
	public EGLBase14(final Context sharedContext, final boolean withDepthBuffer, final boolean isRecordable) {
		if (DEBUG) Log.v(TAG, "Constructor:");
		init(sharedContext, withDepthBuffer, isRecordable);
	}

	/**
	 * 関連するリソースを破棄する
	 */
	@Override
    public void release() {
		if (DEBUG) Log.v(TAG, "release:");
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
	 * Android4.1.2だとSurfaceを使えない。SurfaceTexture/SufaceHolderの場合は内部でSurfaceを生成して使っているにもかかわらず。
	 * しかもAIDLで送れるのはSurfaceだけなのに
	 * @param nativeWindow Surface/SurfaceTexture/SurfaceHolder
	 * @return
	 */
	@Override
	public EglSurface createFromSurface(final Object nativeWindow) {
		if (DEBUG) Log.v(TAG, "createFromSurface:");
		final EglSurface eglSurface = new EglSurface(this, nativeWindow);
		eglSurface.makeCurrent();
		return eglSurface;
	}

	/**
	 * 指定した大きさのオフスクリーンEglSurfaceを生成する
	 * 生成したEglSurfaceをmakeCurrentした状態で戻る
	 * @param width PBufferオフスクリーンのサイズ(0以下はだめ)
	 * @param height
	 * @return
	 */
	@Override
	public EglSurface createOffscreen(final int width, final int height) {
		if (DEBUG) Log.v(TAG, "createOffscreen:");
		final EglSurface eglSurface = new EglSurface(this, width, height);
		eglSurface.makeCurrent();
		return eglSurface;
	}

	/**
	 * GLESに文字列を問い合わせる
	 * @param what
	 * @return
	 */
 	public String queryString(final int what) {
		return EGL14.eglQueryString(mEglDisplay, what);
	}

	/**
	 * GLESバージョンを取得する
	 * @return 2または3
	 */
	@Override
	public int getGlVersion() {
		return 2;
	}

	/**
	 * EGLレンダリングコンテキストを取得する
	 * このEGLBaseインスタンスを使って生成したEglSurfaceをmakeCurrentした状態で
	 * eglGetCurrentContextを呼び出すのと一緒
	 * @return
	 */
	@Override
	public Context getContext() {
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
	 * GLインスタンスを取得する, GLES1のときのみ有効, GLES2, GLES3のときはnullを返す
	 * @return 有効なEGLレンダリングコンテキストが無ければnull
	 */
	@Override
	public @Nullable IGL getGl() {
		return null;
	}

	/**
	 * EGLレンダリングコンテキストとスレッドの紐付けを解除する
	 */
	@Override
	public void makeDefault() {
		if (DEBUG) Log.v(TAG, "makeDefault:");
        if (!EGL14.eglMakeCurrent(mEglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)) {
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

	private void init(Context sharedContext, final boolean with_depth_buffer, final boolean isRecordable) {
		if (DEBUG) Log.v(TAG, "init:");
        if (mEglDisplay != EGL14.EGL_NO_DISPLAY) {
            throw new RuntimeException("EGL already set up");
        }

        mEglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        if (mEglDisplay == EGL14.EGL_NO_DISPLAY) {
            throw new RuntimeException("eglGetDisplay failed");
        }

		final int[] version = new int[2];
        if (!EGL14.eglInitialize(mEglDisplay, version, 0, version, 1)) {
        	mEglDisplay = null;
            throw new RuntimeException("eglInitialize failed");
        }

		sharedContext = sharedContext != null ? sharedContext : EGL_NO_CONTEXT;
        if ((mContext == null) || (mContext.eglContext == EGL14.EGL_NO_CONTEXT)) {
            mEglConfig = new Config(getConfig(with_depth_buffer, isRecordable));
            if (mEglConfig == null) {
                throw new RuntimeException("chooseConfig failed");
            }
            // create EGL rendering context
	        mContext = createContext(sharedContext);
        }
        // confirm whether the EGL rendering context is successfully created
        final int[] values = new int[1];
        EGL14.eglQueryContext(mEglDisplay, mContext.eglContext, EGL14.EGL_CONTEXT_CLIENT_VERSION, values, 0);
        if (DEBUG) Log.d(TAG, "EGLContext created, client version " + values[0]);
        makeDefault();	// makeCurrent(EGL14.EGL_NO_SURFACE);
	}

	/**
	 * change context to draw this window surface
	 * @return
	 */
	private boolean makeCurrent(final EGLSurface surface) {
//		if (DEBUG) Log.v(TAG, "makeCurrent:");
        if (mEglDisplay == null) {
            if (DEBUG) Log.d(TAG, "makeCurrent:eglDisplay not initialized");
        }
        if (surface == null || surface == EGL14.EGL_NO_SURFACE) {
            final int error = EGL14.eglGetError();
            if (error == EGL14.EGL_BAD_NATIVE_WINDOW) {
                Log.e(TAG, "makeCurrent:returned EGL_BAD_NATIVE_WINDOW.");
            }
            return false;
        }
        // attach EGL renderring context to specific EGL window surface
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
        	if (DEBUG) Log.w(TAG, "swap:err=" + err);
            return err;
        }
        return EGL14.EGL_SUCCESS;
    }

    private Context createContext(final Context sharedContext) {
//		if (DEBUG) Log.v(TAG, "createContext:");

        final int[] attrib_list = {
        	EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
        	EGL14.EGL_NONE
        };
        final Context context = new Context(EGL14.eglCreateContext(mEglDisplay, mEglConfig.eglConfig, sharedContext.eglContext, attrib_list, 0));
        checkEglError("eglCreateContext");
        return context;
    }

    private void destroyContext() {
		if (DEBUG) Log.v(TAG, "destroyContext:");

        if (!EGL14.eglDestroyContext(mEglDisplay, mContext.eglContext)) {
            Log.e("destroyContext", "display:" + mEglDisplay + " context: " + mContext.eglContext);
            Log.e(TAG, "eglDestroyContex:" + EGL14.eglGetError());
        }
        mContext = EGL_NO_CONTEXT;
        if (mDefaultContext != EGL14.EGL_NO_CONTEXT) {
	        if (!EGL14.eglDestroyContext(mEglDisplay, mDefaultContext)) {
	            Log.e("destroyContext", "display:" + mEglDisplay + " context: " + mDefaultContext);
	            Log.e(TAG, "eglDestroyContext:" + EGL14.eglGetError());
	        }
	        mDefaultContext = EGL14.EGL_NO_CONTEXT;
        }
    }

	private final int[] mSurfaceDimension = new int[2];
	private final int getSurfaceWidth(final EGLSurface surface) {
		final boolean ret = EGL14.eglQuerySurface(mEglDisplay, surface, EGL14.EGL_WIDTH, mSurfaceDimension, 0);
		if (!ret) mSurfaceDimension[0] = 0;
		return mSurfaceDimension[0];
	}

	private final int getSurfaceHeight(final EGLSurface surface) {
		final boolean ret = EGL14.eglQuerySurface(mEglDisplay, surface, EGL10.EGL_HEIGHT, mSurfaceDimension, 1);
		if (!ret) mSurfaceDimension[1] = 0;
		return mSurfaceDimension[1];
	}

    private EGLSurface createWindowSurface(final Object nativeWindow) {
		if (DEBUG) Log.v(TAG, "createWindowSurface:");

        final int[] surfaceAttribs = {
                EGL14.EGL_NONE
        };
		EGLSurface result = null;
		try {
			result = EGL14.eglCreateWindowSurface(mEglDisplay, mEglConfig.eglConfig, nativeWindow, surfaceAttribs, 0);
		} catch (final IllegalArgumentException e) {
			Log.e(TAG, "eglCreateWindowSurface", e);
		}
		return result;
	}

    /**
     * Creates an EGL surface associated with an offscreen buffer.
     */
    private EGLSurface createOffscreenSurface(final int width, final int height) {
		if (DEBUG) Log.v(TAG, "createOffscreenSurface:");
        final int[] surfaceAttribs = {
                EGL14.EGL_WIDTH, width,
                EGL14.EGL_HEIGHT, height,
                EGL14.EGL_NONE
        };
		EGLSurface result = null;
		try {
			result = EGL14.eglCreatePbufferSurface(mEglDisplay, mEglConfig.eglConfig, surfaceAttribs, 0);
	        checkEglError("eglCreatePbufferSurface");
	        if (result == null) {
	            throw new RuntimeException("surface was null");
	        }
		} catch (final IllegalArgumentException e) {
			Log.e(TAG, "createOffscreenSurface", e);
		} catch (final RuntimeException e) {
			Log.e(TAG, "createOffscreenSurface", e);
		}
		return result;
    }

	private void destroyWindowSurface(EGLSurface surface) {
		if (DEBUG) Log.v(TAG, "destroySurface:");

        if (surface != EGL14.EGL_NO_SURFACE) {
        	EGL14.eglMakeCurrent(mEglDisplay,
        		EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
        	EGL14.eglDestroySurface(mEglDisplay, surface);
        }
        surface = EGL14.EGL_NO_SURFACE;
        if (DEBUG) Log.v(TAG, "destroySurface:finished");
	}

    private void checkEglError(final String msg) {
        int error;
        if ((error = EGL14.eglGetError()) != EGL14.EGL_SUCCESS) {
            throw new RuntimeException(msg + ": EGL error: 0x" + Integer.toHexString(error));
        }
    }

    @SuppressWarnings("unused")
    private EGLConfig getConfig(final boolean with_depth_buffer, final boolean isRecordable) {
        final int[] attribList = {
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_NONE, EGL14.EGL_NONE,	//EGL14.EGL_STENCIL_SIZE, 8,
                EGL14.EGL_NONE, EGL14.EGL_NONE,	//EGL_RECORDABLE_ANDROID, 1,	// this flag need to recording of MediaCodec
                EGL14.EGL_NONE,	EGL14.EGL_NONE,	//	with_depth_buffer ? EGL14.EGL_DEPTH_SIZE : EGL14.EGL_NONE,
												// with_depth_buffer ? 16 : 0,
                EGL14.EGL_NONE
        };
        int offset = 10;
        if (false) {				// ステンシルバッファ(常時未使用)
        	attribList[offset++] = EGL14.EGL_STENCIL_SIZE;
        	attribList[offset++] = 8;
        }
        if (with_depth_buffer) {	// デプスバッファ
        	attribList[offset++] = EGL14.EGL_DEPTH_SIZE;
        	attribList[offset++] = 16;
        }
        if (isRecordable && (Build.VERSION.SDK_INT >= 18)) {// MediaCodecの入力用Surfaceの場合
        	attribList[offset++] = EGL_RECORDABLE_ANDROID;
        	attribList[offset++] = 1;
        }
        for (int i = attribList.length - 1; i >= offset; i--) {
        	attribList[i] = EGL14.EGL_NONE;
        }
        final EGLConfig[] configs = new EGLConfig[1];
        final int[] numConfigs = new int[1];
        if (!EGL14.eglChooseConfig(mEglDisplay, attribList, 0, configs, 0, configs.length, numConfigs, 0)) {
        	// XXX it will be better to fallback to RGB565
            Log.w(TAG, "unable to find RGBA8888 / " + " EGLConfig");
            return null;
        }
        return configs[0];
    }
}
