package com.serenegiant.widget
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

import android.annotation.SuppressLint
import android.content.Context
import android.opengl.GLES20
import android.opengl.Matrix
import android.os.Handler
import android.util.AttributeSet
import android.util.Log
import android.view.Choreographer
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import androidx.annotation.AnyThread
import androidx.annotation.CallSuper
import androidx.annotation.Size
import androidx.annotation.WorkerThread
import com.serenegiant.glutils.GLContext
import com.serenegiant.glutils.GLManager
import com.serenegiant.glutils.ISurface

/**
 * SurfaceViewのSurfaceへOpenGL|ESで描画するためのヘルパークラス
 * SurfaceViewを継承
 */
open class GLView @JvmOverloads constructor(
	context: Context?, attrs: AttributeSet? = null, defStyle: Int = 0)
		: SurfaceView(context, attrs, defStyle), IGLTransformView {

	/**
	 * GLスレッド上での処理
	 */
	interface GLRenderer {
		/**
		 * Surfaceが生成された時
		 * EGL/GLコンテキストを保持しているワーカースレッド上で実行される
		 */
		@WorkerThread
		fun onSurfaceCreated()
		/**
		 * Surfaceのサイズが変更された
		 * EGL/GLコンテキストを保持しているワーカースレッド上で実行される
		 */
		@WorkerThread
		fun onSurfaceChanged(format: Int, width: Int, height: Int)
		/**
		 * トランスフォームマトリックスを適用
		 */
		@WorkerThread
		fun applyTransformMatrix(@Size(min=16) transform: FloatArray)
		/**
		 * 描画イベント
		 * EGL/GLコンテキストを保持しているワーカースレッド上で実行される
		 */
		@WorkerThread
		fun drawFrame()
		/**
		 * Surfaceが破棄された
		 * EGL/GLコンテキストを保持しているワーカースレッド上で実行される
		 */
		@WorkerThread
		fun onSurfaceDestroyed()
	}

	private val mGLManager: GLManager
	private val mGLContext: GLContext
	private val mGLHandler: Handler
	@Volatile
	private var mHasSurface: Boolean = false
	/**
	 * SurfaceViewのSurfaceへOpenGL|ESで描画するためのISurfaceインスタンス
	 */
	private var mTarget: ISurface? = null

	private val mMatrix: FloatArray = FloatArray(16)
	private var mMatrixChanged = false

	private var mGLRenderer: GLRenderer? = null

	init {
		if (DEBUG) Log.v(TAG, "コンストラクタ:")
		mGLManager = GLManager()
		mGLContext = mGLManager.glContext
		mGLHandler = mGLManager.glHandler
		Matrix.setIdentityM(mMatrix, 0)
		holder.addCallback(object : SurfaceHolder.Callback {
			override fun surfaceCreated(holder: SurfaceHolder) {
				if (DEBUG) Log.v(TAG, "surfaceCreated:")

				if ((width > 0) && (height > 0)) {
					mHasSurface = true
					mMatrixChanged = true
					queueEvent { onSurfaceCreated() }
				}
			}

			override fun surfaceChanged
				(holder: SurfaceHolder, format: Int,
				width: Int, height: Int) {

				if (DEBUG) Log.v(TAG, "surfaceChanged:(${width}x${height})")
				queueEvent { onSurfaceChanged(format, width, height) }
			}

			override fun surfaceDestroyed(holder: SurfaceHolder) {
				if (DEBUG) Log.v(TAG, "surfaceDestroyed:")
				mHasSurface = false
				queueEvent { onSurfaceDestroyed() }
			}
		})
	}

	/**
	 * OpenGL|ES3.xが使用可能かどうかを取得
	 */
	@AnyThread
	fun isGLES3() : Boolean {
		return mGLContext.isGLES3
	}

	/**
	 * OpenGL|ES3.xが使用可能＆GLES3の外部テクスチャをしようかどうかどうかを取得
	 */
	@AnyThread
	fun isOES3() : Boolean {
		return mGLContext.isOES3
	}

	/**
	 * 内部使用のGLManagerインスタンスを取得
	 */
	@AnyThread
	fun getGLManager() : GLManager {
		return mGLManager
	}

	/**
	 * 内部使用のGLContextを取得
	 */
	@AnyThread
	fun getGLContext() : GLContext {
		return mGLContext
	}

	/**
	 * GLRendererをセット
	 */
	@AnyThread
	fun setRenderer(renderer: GLRenderer?) {
		queueEvent {
			mGLRenderer = renderer
		}
	}

	/**
	 * Viewportを設定
	 * @param x
	 * @param y
	 * @param width
	 * @param height
	 */
	@AnyThread
	fun setViewport(x: Int, y: Int, width: Int, height: Int) {
		queueEvent {
			if (mTarget != null) {
				mTarget!!.setViewPort(x, y, width, height)
			}
		}
	}

	/**
	 * EGL/GLコンテキストを保持しているワーカースレッド上で実行要求する
	 */
	@AnyThread
	fun queueEvent(task: Runnable) {
		mGLHandler.post(task)
	}

	/**
	 * EGL/GLコンテキストを保持しているワーカースレッド上で実行要求する
	 */
	@AnyThread
	fun queueEvent(task: Runnable, delayMs: Long) {
		if (delayMs > 0) {
			mGLHandler.postDelayed(task, delayMs)
		} else {
			mGLHandler.post(task)
		}
	}

	/**
	 * EGL/GLコンテキストを保持しているワーカースレッド上で実行待ちしていれば実行待ちを解除する
	 */
	@AnyThread
	fun removeEvent(task: Runnable) {
		mGLHandler.removeCallbacks(task)
	}

//--------------------------------------------------------------------------------
	/**
	 * IGLTransformViewの実装
	 */
	@AnyThread
	override fun setTransform(@Size(min=16) transform: FloatArray?) {
		synchronized(mMatrix) {
			if (transform != null) {
				System.arraycopy(transform, 0, mMatrix, 0, 16)
			} else {
				Matrix.setIdentityM(mMatrix, 0)
			}
			mMatrixChanged = true
		}
	}

	/**
	 * IGLTransformViewの実装
	 */
	@AnyThread
	override fun getTransform(@Size(min=16) transform: FloatArray?): FloatArray {
		var result = transform
		if (result == null) {
			result = FloatArray(16)
		}
		synchronized(mMatrix) {
			System.arraycopy(mMatrix, 0, result, 0, 16)
		}

		return result
	}

	/**
	 * IGLTransformViewの実装
	 */
	@AnyThread
	override fun getView(): View {
		return this
	}
//--------------------------------------------------------------------------------
	/**
	 * デフォルトのレンダリングコンテキストへ切り返る
	 * Surfaceが有効であればそのサーフェースへの描画コンテキスト
	 * Surfaceが無効であればEGL/GLコンテキスト保持用のオフスクリーンへの描画コンテキストになる
	 */
	@WorkerThread
 	protected fun makeDefault() {
 		if (mTarget != null) {
 			mTarget!!.makeCurrent()
		} else {
			mGLContext.makeDefault()
		}
 	}

	/**
	 * Choreographerを使ったvsync同期用描画のFrameCallback実装
	 */
	private var mChoreographerCallback
		= object : Choreographer.FrameCallback {

		override fun doFrame(frameTimeNanos: Long) {
			if (mHasSurface) {
				mGLManager.postFrameCallbackDelayed(this, 0)
				makeDefault()
				synchronized(mMatrix) {
					if (mMatrixChanged) {
						applyTransformMatrix(mMatrix)
						mMatrixChanged = false
					}
				}
				drawFrame()
				mTarget!!.swap()
			}
		}
	}

	/**
	 * Surfaceが生成された時
	 * EGL/GLコンテキストを保持しているワーカースレッド上で実行される
	 */
	@SuppressLint("WrongThread")
	@WorkerThread
	@CallSuper
	protected fun onSurfaceCreated() {
		if (DEBUG) Log.v(TAG, "onSurfaceCreated:")
		mTarget = mGLContext.egl.createFromSurface(holder.surface)
		// 画面全体へ描画するためにビューポートを設定する
		mTarget?.setViewPort(0, 0, width, height)
		mGLManager.postFrameCallbackDelayed(mChoreographerCallback, 0)
		mGLRenderer?.onSurfaceCreated()
	}

	/**
	 * Surfaceのサイズが変更された
	 * EGL/GLコンテキストを保持しているワーカースレッド上で実行される
	 */
	@WorkerThread
	@CallSuper
	protected fun onSurfaceChanged(
		format: Int, width: Int, height: Int) {

		if (DEBUG) Log.v(TAG, "onSurfaceChanged:(${width}x${height})")
		// 画面全体へ描画するためにビューポートを設定する
		mTarget?.setViewPort(0, 0, width, height)
		mGLRenderer?.onSurfaceChanged(format, width, height)
	}

	/**
	 * 描画イベント
	 * EGL/GLコンテキストを保持しているワーカースレッド上で実行される
	 */
	@WorkerThread
	protected fun drawFrame() {
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
		mGLRenderer?.drawFrame()
	}

	/**
	 * Surfaceが破棄された
	 * EGL/GLコンテキストを保持しているワーカースレッド上で実行される
	 */
	@WorkerThread
	@CallSuper
	protected fun onSurfaceDestroyed() {
		if (DEBUG) Log.v(TAG, "onSurfaceDestroyed:")
		mGLRenderer?.onSurfaceDestroyed()
		mGLHandler.removeCallbacksAndMessages(null)
		mGLManager.removeFrameCallback(mChoreographerCallback)
		if (mTarget != null) {
			mTarget!!.release()
			mTarget = null
		}
	}

	/**
	 * トランスフォームマトリックスを適用
	 */
	@WorkerThread
	@CallSuper
	protected fun applyTransformMatrix(@Size(min=16) transform: FloatArray) {
		if (DEBUG) Log.v(TAG, "applyTransformMatrix:")
		mGLRenderer?.applyTransformMatrix(transform)
	}

	companion object {
		private const val DEBUG = true // TODO set false on release
		private val TAG = GLView::class.java.simpleName
	}

}