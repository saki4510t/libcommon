package com.serenegiant.compute
/*
* libcommon
* utility/helper classes for myself
*
* Copyright (c) 2014-2026 saki t_saki@serenegiant.com
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

import android.opengl.GLES31
import android.os.Build
import android.util.Log
import androidx.annotation.AnyThread
import androidx.annotation.CallSuper
import androidx.annotation.RequiresApi
import androidx.annotation.Size
import androidx.annotation.WorkerThread
import com.serenegiant.gl.GLCompute
import com.serenegiant.gl.GLConst
import com.serenegiant.gl.GLUtils
import com.serenegiant.nio.BufferHelper
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * コンピュートシェーダーまたはフラグメントシェーダーを使ったフォーカス強度分布計算の共通部分
 * @param isOES ソース映像がOESテクスチャかどうか
 */
@RequiresApi(api = Build.VERSION_CODES.N)
abstract class GLFocusBase @WorkerThread constructor(
	val isOES: Boolean,
	private val useMipmap: Boolean,
): GLCompute {
	/**
	 * ターゲットテクスチャ
	 * GL_TEXTURE_EXTERNAL_OESまたはGL_TEXTURE_2D
	 */
	internal val mTexTarget: Int
	/**
	 * フォーカス強度分布の計算値を受け取るシェーダーストレージバッファオブジェクトID
	 */
	var focusBufferId = GLConst.GL_NO_BUFFER
		private set

	/**
	 * 排他制御用
	 */
	internal val mLock = ReentrantLock()
	/**
	 * フォーカス強度分布計算を行うROI(Region of Interest)
	 */
	@Size(value = 4)
	internal val mROI = FloatArray(4)
	/**
	 * フォーカス強度分布受け取り用のバッファをゼロクリアセットするために使うIntBuffer
	 */
	private var mClearBuffer = BufferHelper.createBuffer(IntArray(FOCUS_COUNT_NUM))
	/**
	 * 近傍テクセル計算用のオフセット値(テクスチャサイズから計算する)
	 */
	private val mTexOffset = FloatArray(KERNEL_SIZE3X3_NUM * 2)
	/**
	 * 映像の幅
	 */
	private var mImageWidth = 0
	/**
	 * 映像の高さ
	 */
	private var mImageHeight = 0

	init {
		if (DEBUG) Log.v(TAG, "コンストラクタ:")
		mTexTarget = if (isOES) GLConst.GL_TEXTURE_EXTERNAL_OES else GLES31.GL_TEXTURE_2D
		focusBufferId = initFocusBuffer()
	}

	@CallSuper
	@WorkerThread
	override  fun release() {
		if (DEBUG) Log.v(TAG, "release:")
		releaseFocusBuffer()
		if (DEBUG) Log.v(TAG, "release:finished")
	}

	/**
	 * (x1,y1)-(x2,y2)を対角とする矩形をROI(Region of Interest)として指定する
	 * @param x1
	 * @param y1
	 * @param x2
	 * @param y2
	 */
	@AnyThread
	fun setROI(
		x1: Float, y1: Float,
		x2: Float, y2: Float
	) {
//		if (DEBUG) Log.v(TAG, "setROI:($x1,$y1)-($x2,$y2)")
		mLock.withLock {
			mROI[0] = x1
			mROI[1] = y1
			mROI[2] = x2
			mROI[3] = y2
		}
	}

	/**
	 * シェーダーストレージバッファのフォーカス強度分布を指定したIntバッファへ読み込む
	 * EGL|GLコンテキストの存在するスレッド上で実行すること
	 * XXX IntArrayを使っているが実際にはUIntArrayなので@OptIn(ExperimentalUnsignedTypes::class)を
	 *     指定してIntArray#asUIntArrayを使ってUIntArrayへ変換してから使うこと
	 * @param buffer nullまたはFOCUS_COUNT_BYTESより小さい場合は内部で新しく生成する
	 * @return 引数で渡したbufferまたは内部生成されたIntArray
	 * @throws IllegalStateException フォーカス強度バッファを読み込めなかった時
	 */
	@Size(value = FOCUS_COUNT_NUM.toLong())
	@Throws(IllegalStateException::class)
	@WorkerThread
	fun readFocusBuffer(buffer: IntArray?): IntArray {
		val buf = if ((buffer == null) || (buffer.size < FOCUS_COUNT_NUM)) {
			IntArray(FOCUS_COUNT_NUM)
		} else {
			buffer
		}
//		if (DEBUG) Log.v(TAG, "readFocusBuffer:$buffer")
		val bufId = focusBufferId
		if (bufId != GLConst.GL_NO_BUFFER) {
			GLES31.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, bufId)
			GLES31.glMemoryBarrier(GLES31.GL_SHADER_STORAGE_BARRIER_BIT)
			if (DEBUG) GLUtils.checkGlError("readFocusBuffer:glBindBuffer", DEBUG)
			val mapped = GLES31.glMapBufferRange(
				GLES31.GL_SHADER_STORAGE_BUFFER,
				0, FOCUS_COUNT_BYTES,	// lengthはバイト数なので注意
				GLES31.GL_MAP_READ_BIT)
			if (DEBUG) GLUtils.checkGlError("readFocusBuffer:glMapBufferRange", DEBUG)
			if (mapped is ByteBuffer) {
				mapped.order(ByteOrder.nativeOrder())	// ここでバイトオーダーを指定しないといけない!
					.asIntBuffer().get(buf)
			} else if (DEBUG) {
				Log.d(TAG, "readFocusBuffer:glMapBufferRange returned null")
			}
			GLES31.glUnmapBuffer(GLES31.GL_SHADER_STORAGE_BUFFER)
			GLES31.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, 0)
		} else {
			throw IllegalStateException("No histogram buffer")
		}

		return buf
	}

	internal fun updateTexlOffset(width: Int, height: Int): FloatArray {
		if ((mImageWidth != width) || (mImageHeight != height)) {
			if (DEBUG) Log.v(TAG, "prepare:(${width}x$height)")
			val rw = 1.0f
			val rh = 1.0f

			//	x						y
			mTexOffset[0] = -rw; 	mTexOffset[1] = -rh
			mTexOffset[2] = 0f;		mTexOffset[3] = -rh
			mTexOffset[4] = rw;		mTexOffset[5] = -rh

			mTexOffset[6] = -rw;	mTexOffset[7] = 0f
			mTexOffset[8] = 0f;		mTexOffset[9] = 0f
			mTexOffset[10] = rw;	mTexOffset[11] = 0f

			mTexOffset[12] = -rw;	mTexOffset[13] = rh
			mTexOffset[14] = 0f;	mTexOffset[15] = rh
			mTexOffset[16] = rw;	mTexOffset[17] = rh

			mImageWidth = width
			mImageHeight = height
		}

		return mTexOffset
	}

	@WorkerThread
	internal fun clearFocusBuffer() {
		resetClearBuffer()
		// 操作するバッファを指定
		// 以降バッファIDとして0を指定するまではGL_SHADER_STORAGE_BUFFERを
		// 指定したバッファの操作は全てこのbufferIDで示すバッファに対して行われる
		GLES31.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, focusBufferId)
		if (DEBUG) GLUtils.checkGlError("clearAndBindHistogramBuffer:glBindBuffer($focusBufferId)", DEBUG)
		GLES31.glBufferData(
			GLES31.GL_SHADER_STORAGE_BUFFER,
			FOCUS_COUNT_BYTES,  // sizeはバイト数なので注意
			mClearBuffer,
			GLES31.GL_DYNAMIC_COPY
		)
		GLUtils.checkGlError("initHistogramBuffer:glBufferData", DEBUG)
		// バッファの指定をクリア
		GLES31.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, 0)
		if (DEBUG) GLUtils.checkGlError("clearAndBindHistogramBuffer:glBindBuffer(0)", DEBUG)
	}

	/**
	 * フォーカス強度受け取り用のシェーダーストレージバッファオブジェクトを生成
	 * EGL|GLコンテキストの存在するスレッド上で実行すること
	 * @return シェーダーストレージバッファオブジェクトID
	 */
	@WorkerThread
	private fun initFocusBuffer(): Int {
		if (DEBUG) Log.v(TAG, "initFocusBuffer:")
		// バッファオブジェクトを生成
		val focusBufferIds = IntArray(1)
		GLES31.glGenBuffers(1, focusBufferIds, 0)
		GLUtils.checkGlError("initFocusBuffer:glGenBuffers", DEBUG)
		// 操作するバッファを指定
		GLES31.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, focusBufferIds[0])
		GLUtils.checkGlError("initFocusBuffer:glBindBuffer", DEBUG)
		mClearBuffer.position(mClearBuffer.capacity())
		mClearBuffer.flip()
		GLES31.glBufferData(
			GLES31.GL_SHADER_STORAGE_BUFFER,
			FOCUS_COUNT_BYTES,  // sizeはバイト数なので注意
			mClearBuffer,
			GLES31.GL_DYNAMIC_COPY
		)
		GLUtils.checkGlError("initFocusBuffer:glBufferData", DEBUG)
		// バッファの指定をクリア
		GLES31.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, 0)
		GLUtils.checkGlError("initFocusBuffer:glBindBuffer(0)", DEBUG)
		return focusBufferIds[0]
	}

	private fun releaseFocusBuffer() {
		val bufferId = focusBufferId
		focusBufferId = GLConst.GL_NO_BUFFER
		if (bufferId != GLConst.GL_NO_BUFFER) {
			GLUtils.deleteBuffer(bufferId)
		}
	}

	private fun resetClearBuffer() {
		mClearBuffer.clear()
		mClearBuffer.position(mClearBuffer.capacity())
		mClearBuffer.flip()
	}

	companion object {
		private const val DEBUG = true	// set false on production
		private val TAG = GLFocusBase::class.java.simpleName

		const val FOCUS_HISTOGRAM_NUM = 500						// ix=0..499
		const val FOCUS_MAX_INDEX = FOCUS_HISTOGRAM_NUM			// ix=500
		const val FOCUS_STRENGTH_INDEX = FOCUS_MAX_INDEX + 1	// ix=501

		/**
		 * フォーカス強度分布のデータ数
		 */
		internal const val FOCUS_COUNT_NUM = FOCUS_STRENGTH_INDEX + 1
		/**
		 * フォーカス強度分布用バッファのバイト数
		 */
		internal const val FOCUS_COUNT_BYTES = FOCUS_COUNT_NUM * BufferHelper.SIZEOF_INT_BYTES
		/**
		 * カーネル関数のサイズ
		 */
		internal const val KERNEL_SIZE3X3_NUM = 9
	}
}
