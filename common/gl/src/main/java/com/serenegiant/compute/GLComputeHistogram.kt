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
import androidx.annotation.RequiresApi
import androidx.annotation.Size
import androidx.annotation.WorkerThread
import com.serenegiant.compute.GLFragmentHistogram.Companion
import com.serenegiant.gl.GLConst.TexUnit
import com.serenegiant.gl.GLUtils
import kotlin.concurrent.withLock

/**
 * OpenGL|ES3.1以降のコンピュートシェーダーを使って映像のヒストグラムを計算するヘルパークラス
 * EGL|GLコンテキストを保持するスレッド上で呼び出すこと
 * @param isOES ソース映像がOESテクスチャかどうか
 */
@RequiresApi(api = Build.VERSION_CODES.N)
class GLComputeHistogram @WorkerThread constructor(
	isOES: Boolean,
): GLHistogramBase(isOES) {
	/**
	 * コンピュートシェーダーのプログラムオブジェクトID
	 * USB_COMPUTE_SHADER=trueのときのみ有効
	 */
	private var mComputeProgram = -1

	/**
	 * ヒストグラム計算を行うROI(Region of Interest)のロケーション
	 * USB_COMPUTE_SHADER=trueのときのみ有効
	 */
	private val muROILoc: Int
	/**
	 * テクスチャ変換行列のロケーション
	 * 今はUSB_COMPUTE_SHADER=trueのときのみ有効
	 */
	private val muTexMatrixLoc: Int
	/**
	 * RGBヒストグラム生成時に頂点座標を飛び飛びにカウントするための変換係数ロケーション
	 */
	private val muStepFactorLoc: Int
	/**
	 * ヒストグラム計算を行うROI(Region of Interest)
	 */
	@Size(value = 4)
	private val mROI = FloatArray(4)

	init {
		if (DEBUG) Log.v(TAG, "コンストラクタ:isOES=$isOES")
		if (DEBUG) Log.v(TAG, "コンストラクタ:create compute shader")
		mComputeProgram = GLComputeUtils.loadShader(COMPUTE_SHADER_HISTOGRAM_COMPUTE_ES31)
		if (DEBUG) Log.v(TAG, "コンストラクタ:mComputeProgram=$mComputeProgram")
		muROILoc = GLES31.glGetUniformLocation(mComputeProgram, "uROI")
		GLUtils.checkGlError("コンストラクタ:glGetUniformLocation(uROI)", DEBUG)
		muTexMatrixLoc = GLES31.glGetUniformLocation(mComputeProgram, "uTexMatrix")
		GLUtils.checkGlError("コンストラクタ:glGetUniformLocation(uTexMatrix)", DEBUG)
		muStepFactorLoc = GLES31.glGetUniformLocation(mComputeProgram, "uStepFactor")
		GLUtils.checkGlError("glGetUniformLocation(uStepFactor)", DEBUG)
		if (DEBUG) Log.v(TAG, "コンストラクタ:muROILoc=$muROILoc,muTexMatrixLoc=$muTexMatrixLoc,muStepFactorLoc=$muStepFactorLoc")
	}

	/**
	 * リソースを破棄する
	 * EGL|GLコンテキストを保持するスレッド上で呼び出すこと
	 */
	@WorkerThread
	override fun release() {
		if (DEBUG) Log.v(TAG, "release:")
		if (mComputeProgram >= 0) {
			GLES31.glDeleteProgram(mComputeProgram)
			mComputeProgram = -1
		}
		super.release()
		if (DEBUG) Log.v(TAG, "release:finished")
	}

	/**
	 * (x1,y1)-(x2,y2)を対角とする矩形をROI(Region of Interest)として指定する
	 * 各値は映像サイズベースで全映像を対象にするなら(0, 0)-(width, height)
	 * x1<=x2またはy1<=y2の場合の動作は未定義
	 * @param x1
	 * @param y1
	 * @param x2
	 * @param y2
	 */
	@AnyThread
	override fun setROI(
		x1: Int, y1: Int,
		x2: Int, y2: Int
	) {
//		if (DEBUG) Log.v(TAG, "setROI:($x1,$y1)-($x2,$y2)")
		mLock.withLock {
			mROI[0] = x1.toFloat()
			mROI[1] = y1.toFloat()
			mROI[2] = x2.toFloat()
			mROI[3] = y2.toFloat()
		}
	}

	/**
	 * ヒストグラムを計算
	 * EGL|GLコンテキストを保持するスレッド上で呼び出すこと
	 * GLComputeの実装
	 * @param width 映像幅、ピクセル
	 * @param height 映像高さ、ピクセル
	 * @param texUnit テクスチャユニット
	 * @param texId ソース映像のテクスチャID
	 * @param texMatrix テクスチャ変換行列
	 * @param texOffset テクスチャ変換行列のオフセット
	 */
	@WorkerThread
	override fun compute(
		width: Int, height: Int,
		@TexUnit texUnit: Int, texId: Int,
		@Size(min = 16) texMatrix: FloatArray?,
		texOffset: Int
	) {
//		if (DEBUG) Log.v(TAG, "compute:")
		clearHistogramBuffer()
		GLES31.glUseProgram(mComputeProgram)
		mLock.withLock {
			// ステップファクターをセット
			GLES31.glUniform2fv(muStepFactorLoc, 1, mStepFactor, 0)
			// ROIをセット
			GLES31.glUniform2fv(muROILoc, 2, mROI, 0)
		}
		// テクスチャ変換行列をバインド
		GLES31.glUniformMatrix4fv(muTexMatrixLoc, 1, false, texMatrix, texOffset)
		if (DEBUG) GLUtils.checkGlError("compute:glUniformMatrix4fv", DEBUG)
		// ヒストグラム用バッファをバインド
		GLES31.glBindBufferBase(GLES31.GL_SHADER_STORAGE_BUFFER, 1, histogramBufferId)
		if (DEBUG) GLUtils.checkGlError("compute:glBindBufferBase,bufId=$histogramBufferId", DEBUG)
		// ソース映像のテクスチャをバインド
		GLES31.glActiveTexture(texUnit)
		if (DEBUG) GLUtils.checkGlError("compute:glActiveTexture,texUnit=$texUnit", DEBUG)
		GLES31.glBindTexture(mTexTarget, texId)
		if (DEBUG) GLUtils.checkGlError("compute:glBindTexture,texId=$texId", DEBUG)
		GLES31.glUniform1i(0, GLUtils.gLTextureUnit2Index(texUnit))
		if (DEBUG) GLUtils.checkGlError("bindTexture:glUniform1i,texUnit=$texUnit,loc=0",DEBUG)
//		GLES31.glBindImageTexture(0, texId, 0, false, 0, GLES31.GL_READ_ONLY, GLES31.GL_RGBA8);
//		if (DEBUG) GLUtils.checkGlError("compute:glBindImageTexture", DEBUG);
		// コンピュート実行
		GLES31.glDispatchCompute((width + 15) / 16, (height + 15) / 16, 1)
		if (DEBUG) GLUtils.checkGlError("compute:glDispatchCompute", DEBUG)
		GLES31.glMemoryBarrier(GLES31.GL_SHADER_STORAGE_BARRIER_BIT or GLES31.GL_BUFFER_UPDATE_BARRIER_BIT)
	}

	companion object {
		private const val DEBUG = false	// set false on production
		private val TAG = GLComputeHistogram::class.java.simpleName


		/**
		 * RGBヒストグラムをカウントするためのコンピュートシェーダー
		 */
		private const val COMPUTE_SHADER_HISTOGRAM_COMPUTE_ES31 =
			"""
			#version 310 es
			#extension GL_ANDROID_extension_pack_es31a : require
			#define STEP (255.0)
			precision highp float;
			precision highp int;
	
			layout (local_size_x = 16, local_size_y = 16) in;
			
			layout(binding = 0) uniform sampler2D srcImage;
			layout(std430, binding = 1) buffer Histogram {
				uint counts[256 * 5];
			};
			uniform vec2 uROI[2];
			uniform mat4 uTexMatrix;
			uniform highp vec2 uStepFactor;
			const highp vec3 conv = vec3(0.2125, 0.7154, 0.0721);
			void main() {
				vec4 pos = vec4(vec2(gl_GlobalInvocationID.xy), 0.0, 1.0);
				vec2 uv = (uTexMatrix * pos).xy * uStepFactor;
				if ((uv.x < uROI[0].x) || (uv.x >= uROI[1].x) || (uv.y < uROI[0].y) || (uv.y >= uROI[1].y)) return;
				vec4 color = texture(srcImage, uv / uROI[1]);
			
				// Assuming color values are in the range [0.0, 1.0]
				// Convert to integer intensity [0, STEP]
				uint indexR = uint(color.r * STEP);
				uint indexG = uint(color.g * STEP);
				uint indexB = uint(color.b * STEP);
				uint indexI = uint(dot(color.rgb, conv) * STEP);
			
				// Atomically increment the histogram bins
				uint countsR = atomicAdd(counts[       indexR], 1u) + 1u;
				uint countsG = atomicAdd(counts[256u + indexG], 1u) + 1u;
				uint countsB = atomicAdd(counts[512u + indexB], 1u) + 1u;
				uint countsI = atomicAdd(counts[768u + indexI], 1u) + 1u;
				// 最大値を更新
	//			atomicMax(counts[1024u], countsR);
	//			atomicMax(counts[1025u], countsG);
	//			atomicMax(counts[1026u], countsB);
				atomicMax(counts[1027u], countsI);
				atomicMax(counts[1028u], max(max(countsR, countsG), countsB));
			}
			"""
	}
}
