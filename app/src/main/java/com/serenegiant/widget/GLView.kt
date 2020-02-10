package com.serenegiant.widget
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2020 saki t_saki@serenegiant.com
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

import android.content.Context
import android.opengl.GLES20
import android.os.Handler
import android.util.AttributeSet
import android.util.Log
import android.view.Choreographer
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.annotation.CallSuper
import androidx.annotation.WorkerThread
import com.serenegiant.glutils.*

/**
 * カメラ映像をVideoSource経由で取得してプレビュー表示するためのICameraGLView実装
 * SurfaceViewを継承
 * XXX useSharedContext = trueで共有コンテキストを使ったマルチスレッド処理を有効にするとGPUのドライバー内でクラッシュする端末がある
 */
open class GLView @JvmOverloads constructor(
	context: Context?, attrs: AttributeSet? = null, defStyle: Int = 0)
		: SurfaceView(context, attrs)  {

	private val mGLManager: GLManager
	private val mGLContext: GLContext
	private val mGLHandler: Handler
	@Volatile
	private var mHasSurface: Boolean = false
	private var mTarget: ISurface? = null

	init {
		if (DEBUG) Log.v(TAG, "コンストラクタ:")
		mGLManager = GLManager(GLUtils.getSupportedGLVersion())
		mGLContext = mGLManager.glContext
		mGLHandler = mGLManager.glHandler
		holder.addCallback(object : SurfaceHolder.Callback {
			override fun surfaceCreated(holder: SurfaceHolder) {
				if (DEBUG) Log.v(TAG, "surfaceCreated:")

				if ((width > 0) && (height > 0)) {
					mHasSurface = true;
					queueEvent( Runnable { onSurfaceCreated() })
				}
			}

			override fun surfaceChanged
				(holder: SurfaceHolder, format: Int,
				width: Int, height: Int) {

				if (DEBUG) Log.v(TAG, "surfaceChanged:(${width}x${height})")
				queueEvent(Runnable { onSurfaceChanged(format, width, height) })
			}

			override fun surfaceDestroyed(holder: SurfaceHolder) {
				if (DEBUG) Log.v(TAG, "surfaceDestroyed:")
				mHasSurface = false;
				queueEvent( Runnable { onSurfaceDestroyed() })
			}
		})
	}

	fun isGLES3() : Boolean {
		return mGLContext.isGLES3;
	}

	fun isOES3() : Boolean {
		return mGLContext.isOES3
	}

	fun getGLManager() : GLManager {
		return mGLManager;
	}

	fun getGLContext() : GLContext {
		return mGLContext
	}

 	protected fun makeDefault() {
 		if (mTarget != null) {
 			mTarget!!.makeCurrent()
		} else {
			mGLContext.makeDefault()
		}
 	}

	fun queueEvent(task: Runnable) {
		mGLHandler.post(task)
	}

	fun queueEvent(task: Runnable, delayMs: Long) {
		if (delayMs > 0) {
			mGLHandler.postDelayed(task, delayMs)
		} else {
			mGLHandler.post(task)
		}
	}

	fun removeEvent(task: Runnable) {
		mGLHandler.removeCallbacks(task)
	}

	private var mChoreographerCallback
		= object : Choreographer.FrameCallback {

		override fun doFrame(frameTimeNanos: Long) {
			if (mHasSurface) {
				mGLManager.postFrameCallbackDelayed(this, 0)
				makeDefault()
				drawFrame()
				mTarget!!.swap()
			}
		}
	}

	/**
	 * Surfaceが生成された時
	 * EGL/GLコンテキストを保持しているワーカースレッド上で実行される
	 */
	@WorkerThread
	@CallSuper
	protected open fun onSurfaceCreated() {
		if (DEBUG) Log.v(TAG, "onSurfaceCreated:")
		mTarget = mGLContext.egl.createFromSurface(holder.surface)
		mTarget!!.setViewPort(0, 0, width, height)
		mGLManager.postFrameCallbackDelayed(mChoreographerCallback, 0)
	}

	/**
	 * Surfaceのサイズが変更された
	 * EGL/GLコンテキストを保持しているワーカースレッド上で実行される
	 */
	@WorkerThread
	@CallSuper
	protected open fun onSurfaceChanged(
		format: Int, width: Int, height: Int) {

		if (DEBUG) Log.v(TAG, "onSurfaceChanged:(${width}x${height})")
		mTarget!!.setViewPort(0, 0, width, height)
	}

	/**
	 * 描画イベント
	 * EGL/GLコンテキストを保持しているワーカースレッド上で実行される
	 */
	@WorkerThread
	protected open fun drawFrame() {
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
	}

	/**
	 * Surfaceが破棄された
	 * EGL/GLコンテキストを保持しているワーカースレッド上で実行される
	 */
	@WorkerThread
	@CallSuper
	protected open fun onSurfaceDestroyed() {
		if (DEBUG) Log.v(TAG, "onSurfaceDestroyed:")
		mGLHandler.removeCallbacksAndMessages(null)
		mGLManager.removeFrameCallback(mChoreographerCallback)
		if (mTarget != null) {
			mTarget!!.release()
			mTarget = null
		}
	}

	companion object {
		private const val DEBUG = true // TODO set false on release
		private val TAG = GLView::class.java.simpleName
	}

}