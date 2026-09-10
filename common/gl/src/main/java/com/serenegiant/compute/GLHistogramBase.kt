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
import androidx.annotation.FloatRange
import androidx.annotation.RequiresApi
import androidx.annotation.Size
import androidx.annotation.WorkerThread
import com.serenegiant.gl.GLCompute
import com.serenegiant.gl.GLConst
import com.serenegiant.gl.GLUtils
import com.serenegiant.nio.BufferHelper
import java.nio.ByteBuffer
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * コンピュートシェーダーまたはフラグメントシェーダーを使ったヒストグラムの生成処理の共通部分
 * @param isOES ソース映像がOESテクスチャかどうか
 */
@RequiresApi(api = Build.VERSION_CODES.N)
abstract class GLHistogramBase @WorkerThread constructor(
	val isOES: Boolean,
): GLCompute {
	/**
	 * ターゲットテクスチャ
	 * GL_TEXTURE_EXTERNAL_OESまたはGL_TEXTURE_2D
	 */
	internal val mTexTarget: Int
	/**
	 * ヒストグラムを受け取るシェーダーストレージバッファオブジェクトID
	 */
	var histogramBufferId = GLConst.GL_NO_BUFFER
		private set

	/**
	 * 排他制御用
	 */
	internal val mLock = ReentrantLock()
	/**
	 * ヒストグラム計算を行うROI(Region of Interest)
	 */
	@Size(value = 4)
	internal val mROI = FloatArray(4)
	/**
	 * ヒストグラムカウント時のサンプリング間隔
	 */
	@Size(value = 2)
	internal val mStepFactor = floatArrayOf(
		4.0f, 3.0f,
	)
	/**
	 * ヒストグラム受け取り用のテクスチャをゼロクリアまたはLUTをセットするために使うIntBuffer
	 */
	private val mClearBuffer = BufferHelper.createBuffer(IntArray(HISTOGRAM_SIZE))
	/**
	 * ヒストグラム平均化補正時にヒストグラムデータを読み込むためのIntArray
	 */
	private val mReadBuffer = IntArray(HISTOGRAM_SIZE)
	/**
	 * LUT計算時のワーク
	 */
	private val mDist = FloatArray(256)

	init {
		if (DEBUG) Log.v(TAG, "コンストラクタ:isOES=$isOES")
		mTexTarget = if (isOES) GLConst.GL_TEXTURE_EXTERNAL_OES else GLES31.GL_TEXTURE_2D
		histogramBufferId = initHistogramBuffer()
	}

	@CallSuper
	@WorkerThread
	override  fun release() {
		if (DEBUG) Log.v(TAG, "release:")
		val bufferId = histogramBufferId
		histogramBufferId = GLConst.GL_NO_BUFFER
		if (bufferId != GLConst.GL_NO_BUFFER) {
			GLUtils.deleteBuffer(bufferId)
		}
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
	 * ヒストグラムカウント時のサンプリング間隔を設定
	 * @param sx 横方向のサンプリング間隔、1.0fよりも小さい値を指定したときは4.0f
	 * @param sy 縦方向のサンプリング間隔、1.0fよりも小さい値を指定したときは3.0f
	 */
	@AnyThread
	fun setStepFactor(
		@FloatRange(from = 1.0) sx: Float, @FloatRange(from = 1.0) sy: Float) {
		if (DEBUG) Log.v(TAG, "setStepFactor:($sx,$sy)")
		mLock.withLock {
			mStepFactor[0] = if (sx >= 1.0f) sx else 4.0f
			mStepFactor[1] = if (sy >= 1.0f) sy else 3.0f
		}
	}

	/**
	 * シェーダーストレージバッファのヒストグラムデータを指定したIntバッファへ読み込む
	 * EGL|GLコンテキストの存在するスレッド上で実行すること
	 * XXX IntArrayを使っているが実際にはUIntArrayなので@OptIn(ExperimentalUnsignedTypes::class)を
	 *     指定してIntArray#asUIntArrayを使ってUIntArrayへ変換してから使うこと
	 * @param buffer nullまたはHISTOGRAM_SIZEより小さい場合は内部で新しく生成する
	 * @return 引数で渡したbufferまたは内部生成されたIntArray
	 * @throws IllegalStateException ヒストグラムバッファを読み込めなかった時
	 */
	@Size(value = HISTOGRAM_SIZE.toLong())
	@Throws(IllegalStateException::class)
	@WorkerThread
	fun readHistogram(buffer: IntArray?): IntArray {
		val buf = if ((buffer == null) || (buffer.size < HISTOGRAM_SIZE)) {
			IntArray(HISTOGRAM_SIZE)
		} else {
			buffer
		}
//		if (DEBUG) Log.v(TAG, "readHistogram:$buffer")
		val bufId = histogramBufferId
		if (bufId != GLConst.GL_NO_BUFFER) {
			GLES31.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, bufId)
			if (DEBUG) GLUtils.checkGlError("readHistogram:glBindBuffer",
				DEBUG
			)
			val mapped = GLES31.glMapBufferRange(
				GLES31.GL_SHADER_STORAGE_BUFFER,
				0, HISTOGRAM_BYTES,	// lengthはバイト数なので注意
				GLES31.GL_MAP_READ_BIT)
			if (DEBUG) GLUtils.checkGlError("readHistogram:glMapBufferRange",
				DEBUG
			)
			if (mapped is ByteBuffer) {
				mapped.asIntBuffer().get(buf)
			} else if (DEBUG) {
				Log.d(TAG, "readHistogram:glMapBufferRange returned null")
			}
			GLES31.glUnmapBuffer(GLES31.GL_SHADER_STORAGE_BUFFER)
			GLES31.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, 0)
		} else {
			throw IllegalStateException("No histogram buffer")
		}

		return buf
	}

	/**
	 * ヒストグラム平坦化用のLUTを計算
	 * 累積分布関数でLUTを計算する
	 * EGL|GLコンテキストの存在するスレッド上で実行すること
	 */
	@OptIn(ExperimentalUnsignedTypes::class)
	@WorkerThread
	fun equalize() {
		val work = readHistogram(mReadBuffer).asUIntArray()	// XXX #asUIntArrayはOptInが必要
		var total = 0.0f	// ヒストグラムの全ピクセル数
		for (ix in 0..255) {
			val rgb = work[ix] + work[ix + 256] + work[ix + 512]	// R[ix] + G[ix] + B[ix]
			total += rgb.toFloat()
			mDist[ix] = rgb.toFloat()
		}
//		if (DEBUG) Log.v(TAG, "equalize:${mDist.contentToString()}")
		mClearBuffer.limit(mClearBuffer.capacity())
		mClearBuffer.position(LUT_INDEX)
		var sum = 0.0f
		for (ix in 0..255) {
			sum += mDist[ix] / total	// 正規化ヒストグラムの累積頻度を計算
			mClearBuffer.put((sum * 255.0f).toInt())	// 0..255に変換してセット
		}
		mClearBuffer.position(LUT_INDEX)
		setLUT()
	}

	/**
	 * ヒストグラム平坦化補正用のLUTをリセットする
	 */
	@WorkerThread
	fun resetEqualize() {
		if (DEBUG) Log.v(TAG, "resetEqualize:")
		mClearBuffer.limit(mClearBuffer.capacity())
		mClearBuffer.position(LUT_INDEX)
		for (ix in 0..255) {
			mClearBuffer.put(ix)
		}
		mClearBuffer.position(LUT_INDEX)
		setLUT()
	}

	/**
	 * ヒストグラム受け取り用のシェーダーストレージバッファオブジェクトを生成
	 * EGL|GLコンテキストの存在するスレッド上で実行すること
	 * @return ヒストグラム受け取り用のシェーダーストレージバッファオブジェクトID
	 */
	@WorkerThread
	private fun initHistogramBuffer(): Int {
		if (DEBUG) Log.v(TAG, "initHistogramBuffer:")
		// バッファオブジェクトを生成
		val histogramBuffer = IntArray(1)
		GLES31.glGenBuffers(1, histogramBuffer, 0)
		GLUtils.checkGlError("initHistogramBuffer:glGenBuffers", DEBUG)
		// 操作するバッファを指定
		GLES31.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, histogramBuffer[0])
		GLUtils.checkGlError("initHistogramBuffer:glBindBuffer", DEBUG)
		// デフォルトのLUTをセット
		mClearBuffer.clear()
		mClearBuffer.position(LUT_INDEX)
		for (ix in 0..255) {
			mClearBuffer.put(ix)
		}
		mClearBuffer.position(mClearBuffer.capacity())
		mClearBuffer.flip()
		GLES31.glBufferData(
			GLES31.GL_SHADER_STORAGE_BUFFER,
			HISTOGRAM_BYTES,  // sizeはバイト数なので注意
			mClearBuffer,
			GLES31.GL_DYNAMIC_COPY
		)
		GLUtils.checkGlError("initHistogramBuffer:glBufferData", DEBUG)
		// バッファの指定をクリア
		GLES31.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, 0)
		GLUtils.checkGlError("initHistogramBuffer:glBindBuffer(0)", DEBUG)
		return histogramBuffer[0]
	}

	/**
	 * ヒストグラム受け取り用のシェーダーストレージバッファオブジェクトをクリアする
	 * EGL|GLコンテキストの存在するスレッド上で実行すること
	 * @return
	 */
	@WorkerThread
	internal fun clearHistogramBuffer() {
		// 操作するバッファを指定
		// 以降バッファIDとして0を指定するまではGL_SHADER_STORAGE_BUFFERを
		// 指定したバッファの操作は全てこのbufferIDで示すバッファに対して行われる
		GLES31.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, histogramBufferId)
		if (DEBUG) GLUtils.checkGlError("clearAndBindHistogramBuffer:glBindBuffer($histogramBufferId)",
			DEBUG
		)
		resetClearBuffer()
		GLES31.glBufferSubData(
			GLES31.GL_SHADER_STORAGE_BUFFER,
			0, HISTOGRAM_BYTES,  // sizeはバイト数なので注意
			mClearBuffer
		)
		if (DEBUG) GLUtils.checkGlError("clearAndBindHistogramBuffer:glBufferData", DEBUG)
		// バッファの指定をクリア
		GLES31.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, 0)
		if (DEBUG) GLUtils.checkGlError("clearAndBindHistogramBuffer:glBindBuffer(0)",
			DEBUG
		)
	}

	private fun resetClearBuffer() {
		mClearBuffer.clear()
		mClearBuffer.position(mClearBuffer.capacity())
		mClearBuffer.flip()
	}

	/**
	 * ヒストグラム平坦化補正用のLUTをセットする
	 * mClearBufferからヒストグラムのカウント用シェーダーストレージバッファオブジェクトのインデックス1280-1535をセットする
	 * EGL|GLコンテキストの存在するスレッド上で実行すること
	 */
	@WorkerThread
	private fun setLUT() {
		// #glReadPixels, #glBufferData, #glBufferSubDataなどのGLESのバッファ関係の関数では
		// Bufferのlimit/positionの操作は無視されてる気がする
		mClearBuffer.limit(mClearBuffer.capacity())
		mClearBuffer.position(LUT_INDEX)
		// シェーダーストレージバッファオブジェクトのLUT領域を更新
		GLES31.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, histogramBufferId)
		if (DEBUG) GLUtils.checkGlError("setLUT:glBindBuffer($histogramBufferId)")
		GLES31.glBufferSubData(
			GLES31.GL_SHADER_STORAGE_BUFFER,
			LUT_INDEX * BufferHelper.SIZEOF_INT_BYTES, 256 * BufferHelper.SIZEOF_INT_BYTES,  // sizeはバイト数なので注意
			mClearBuffer
		)
		if (DEBUG) GLUtils.checkGlError("setLUT:glBufferData")
		// バッファの指定をクリア
		GLES31.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, 0)
	}

	companion object {
		private const val DEBUG = false	// set false on production
		private val TAG = GLHistogramBase::class.java.simpleName

		/**
		 * ヒストグラム平均化補正時のLUTの先頭インデックス
		 */
		const val LUT_INDEX = 1280
		/**
		 * ヒストグラムのデータ長
		 * インデックス0-255:		R
		 * インデックス256-511:	G
		 * インデックス512-767:	B
		 * インデックス768-1023:	I
		 * インデックス1024:		R max(未計算)
		 * インデックス1025:		G max(未計算)
		 * インデックス1026:		B max(未計算)
		 * インデックス1027:		I max
		 * インデックス1028:		max
		 * インデックス1280-1535:	ヒストグラム平坦化のLUT
		 */
		const val HISTOGRAM_SIZE = 256 * 6
		/**
		 * ヒストグラムのデータサイズのバイト数
		 */
		const val HISTOGRAM_BYTES = HISTOGRAM_SIZE * BufferHelper.SIZEOF_INT_BYTES
	}
}
