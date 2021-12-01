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
import android.graphics.drawable.Drawable
import android.opengl.GLES20
import android.opengl.Matrix
import android.util.AttributeSet
import android.util.Log
import android.view.Surface
import androidx.annotation.Size
import androidx.annotation.WorkerThread
import androidx.core.content.ContextCompat
import com.serenegiant.glpipeline.Distributor
import com.serenegiant.glpipeline.IPipelineSource.PipelineSourceCallback
import com.serenegiant.glpipeline.ImageSource
import com.serenegiant.glpipeline.VideoSource
import com.serenegiant.glutils.GLDrawer2D
import com.serenegiant.graphics.BitmapHelper
import com.serenegiant.libcommon.R

/**
 * カメラからの映像の代わりに静止画をプレビュー表示するためのICameraView実装
 * GLViewを継承
 * XXX useSharedContext = trueで共有コンテキストを使ったマルチスレッド処理を有効にするとGPUのドライバー内でクラッシュする端末がある
 */
class DummyCameraGLView @JvmOverloads constructor(
	context: Context?, attrs: AttributeSet? = null, defStyle: Int = 0)
		: AspectScaledGLView(context, attrs, defStyle), ICameraView {

	private var mImageSource: ImageSource? = null
	private var mDistributor: Distributor? = null
	private val mMvpMatrix = FloatArray(16)
	@Volatile
	private var mHasSurface = false

	init {
		if (DEBUG) Log.v(TAG, "コンストラクタ:")
		setRenderer(object : GLRenderer {
			@SuppressLint("WrongThread")
			@WorkerThread
			override fun onSurfaceCreated() {
				if (DEBUG) Log.v(TAG, "onSurfaceCreated:")
				mDrawer = GLDrawer2D.create(isOES3(), false)
				mDrawer!!.setMvpMatrix(mMvpMatrix, 0)
				GLES20.glClearColor(1.0f, 1.0f, 0.0f, 1.0f)
				mHasSurface = true
			}

			@WorkerThread
			override fun onSurfaceChanged(format: Int, width: Int, height: Int) {
				mImageSource!!.resize(width, height)
			}

			@SuppressLint("WrongThread")
			@WorkerThread
			override fun drawFrame() {
				if (mHasSurface && (mImageSource != null) && mImageSource!!.isValid) {
					handleDraw(mImageSource!!.texId, mImageSource!!.texMatrix)
				}
			}

			@WorkerThread
			override fun onSurfaceDestroyed() {
				mHasSurface = false
				if (mDrawer != null) {
//					mDrawer!!.release()	// GT-N7100で動作がおかしくなる
					mDrawer = null
				}
			}

			override fun applyTransformMatrix(@Size(min=16) transform: FloatArray) {
				if (mDrawer != null) {
					System.arraycopy(transform, 0, mMvpMatrix, 0, 16)
					mDrawer!!.setMvpMatrix(mMvpMatrix, 0)
				}
			}
		})
		Matrix.setIdentityM(mMvpMatrix, 0)
	}

	/**
	 * ICameraViewの実装
	 */
	@Synchronized
	override fun onResume() {
		if (DEBUG) Log.v(TAG, "onResume:")
		val dr: Drawable? = ContextCompat.getDrawable(context, R.mipmap.ic_launcher)
		val image = if (dr != null) BitmapHelper.fromDrawable(dr, 640, 480) else null
		mImageSource =
			ImageSource(getGLManager(), image, null)
	}

	/**
	 * ICameraViewの実装
	 */
	@Synchronized
	override fun onPause() {
		if (DEBUG) Log.v(TAG, "onPause:")
		if (mImageSource != null) {
			mImageSource!!.pipeline = null
		}
		if (mDistributor != null) {
			mDistributor!!.release()
			mDistributor = null
		}
		if (mImageSource != null) {
			mImageSource!!.release()
			mImageSource = null
		}
	}

	/**
	 * ICameraViewの実装
	 */
	override fun addListener(listener: CameraDelegator.OnFrameAvailableListener) {
	}

	/**
	 * ICameraViewの実装
	 */
	override fun removeListener(listener: CameraDelegator.OnFrameAvailableListener) {
	}

	/**
	 * ICameraViewの実装
	 */
	override fun setScaleMode(mode: Int) {
		// FIXME 未実装
	}

	/**
	 * ICameraViewの実装
	 */
	override fun getScaleMode(): Int {
		return IScaledView.SCALE_MODE_STRETCH_TO_FIT
	}

	/**
	 * ICameraViewの実装
	 */
	override fun setVideoSize(width: Int, height: Int) {
		if (mImageSource != null) {
			val dr: Drawable? = ContextCompat.getDrawable(context, R.mipmap.ic_launcher)
			val image = if (dr != null) BitmapHelper.fromDrawable(dr, width, height) else null
			mImageSource!!.setSource(image, null)
		}
	}

	/**
	 * ICameraViewの実装
	 */
	override fun getVideoWidth(): Int {
		return mImageSource!!.width
	}

	/**
	 * ICameraViewの実装
	 */
	override fun getVideoHeight(): Int {
		return mImageSource!!.height
	}

	/**
	 * プレビュー表示用Surfaceを追加
	 * @param id
	 * @param surface
	 * @param isRecordable
	 */
	@Synchronized
	override fun addSurface(
		id: Int, surface: Any,
		isRecordable: Boolean) {

		if (DEBUG) Log.v(TAG, "addSurface:$id")
		if (mDistributor == null) {
			mDistributor = Distributor(mImageSource!!)
			mImageSource!!.pipeline = mDistributor!!
		}
		mDistributor!!.addSurface(id, surface, isRecordable)
	}

	/**
	 * プレビュー表示用Surfaceを除去
	 * @param id
	 */
	@Synchronized
	override fun removeSurface(id: Int) {
		if (DEBUG) Log.v(TAG, "removeSurface:$id")
		mDistributor?.removeSurface(id)
	}

	override fun isRecordingSupported(): Boolean {
		return true
	}

	/**
	 * VideoSourceインスタンスを生成
	 * @param width
	 * @param height
	 * @return
	 */
	private fun createVideoSource(
		width: Int, height: Int): VideoSource {

		return VideoSource(getGLManager(), width, height,
			object : PipelineSourceCallback {

				override fun onCreate(surface: Surface) {
					if (DEBUG) Log.v(TAG, "PipelineSourceCallback#onCreate:$surface")
				}

				override fun onDestroy() {
					if (DEBUG) Log.v(TAG, "PipelineSourceCallback#onDestroy:")
				}
			}
			, USE_SHARED_CONTEXT)
	}

	private var mDrawer: GLDrawer2D? = null
	private var cnt2 = 0
	/**
	 * 描画処理の実体
	 * レンダリングスレッド上で実行
	 * @param texId
	 * @param texMatrix
	 */
	@WorkerThread
	private fun handleDraw(texId: Int, texMatrix: FloatArray) {
		if (DEBUG && ((++cnt2 % 100) == 0)) Log.v(TAG, "handleDraw:$cnt2")
		// draw to preview screen
		if (mHasSurface && (mDrawer != null)) {
			mDrawer!!.draw(texId, texMatrix, 0)
			GLES20.glFlush()
		}
	}

	companion object {
		private const val DEBUG = true // TODO set false on release
		private val TAG = DummyCameraGLView::class.java.simpleName
		/**
		 * 共有GLコンテキストコンテキストを使ったマルチスレッド処理を行うかどうか
		 */
		private const val USE_SHARED_CONTEXT = false
	}

}