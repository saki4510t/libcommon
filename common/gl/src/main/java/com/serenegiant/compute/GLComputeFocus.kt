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
import com.serenegiant.gl.GLUtils
import com.serenegiant.gl.ShaderConst
import kotlin.concurrent.withLock

/**
 * コンピュートシェーダーでラプラシアンフィルタ処理を行いエッジ強度の分布を計算する
 * @param isOES
 */
@RequiresApi(api = Build.VERSION_CODES.N)
class GLComputeFocus @WorkerThread constructor(
	isOES: Boolean,
): GLFocusBase(isOES, false) {
	/**
	 * コンピュートシェーダーのプログラムオブジェクトID
	 * USB_COMPUTE_SHADER=trueのときのみ有効
	 */
	private var mComputeProgram = -1
	/**
	 * フォーカス強度計算を行うROI(Region of Interest)のロケーション
	 */
	private val muROILoc: Int
	/**
	 * テクスチャ変換行列のロケーション
	 */
	private val muTexMatrixLoc: Int
	/**
	 * カーネル関数のロケーション
	 */
	private val muKernelLoc: Int
	/**
	 * テクセルオフセットのロケーション
	 */
	private val muTexOffsetLoc: Int
	/**
	 * フォーカス強度分布計算を行うROI(Region of Interest)
	 */
	@Size(value = 4)
	private val mROI = FloatArray(4)
	/**
	 * カーネル関数
	 */
	private val mKernel3x3 = FloatArray(KERNEL_SIZE3X3_NUM * 2)

	init {
		if (DEBUG) Log.v(TAG, "コンストラクタ:create compute shader")
		mComputeProgram = GLComputeUtils.loadShader(
//			COMPUTE_SHADER_FOCUS_COMPUTE_ES31
			COMPUTE_SHADER_FOCUS_GRAY_COMPUTE_ES31
		)
		if (DEBUG) Log.v(TAG, "コンストラクタ:mComputeProgram=$mComputeProgram")
		muROILoc = GLES31.glGetUniformLocation(mComputeProgram, "uROI")
		GLUtils.checkGlError("コンストラクタ:glGetUniformLocation(uROI)", DEBUG)
		muTexMatrixLoc = GLES31.glGetUniformLocation(mComputeProgram, "uTexMatrix")
		GLUtils.checkGlError("コンストラクタ:glGetUniformLocation(uTexMatrix)", DEBUG)
		muKernelLoc = GLES31.glGetUniformLocation(mComputeProgram, "uKernel")
		GLUtils.checkGlError("コンストラクタ:glGetUniformLocation(uKernel)", DEBUG)
		muTexOffsetLoc = GLES31.glGetUniformLocation(mComputeProgram, "uTexOffset")
		GLUtils.checkGlError("コンストラクタ:glGetUniformLocation(uTexOffset)", DEBUG)
		// 3x3ラプラシアンフィルタ
		System.arraycopy(ShaderConst.KERNEL_LAPLACIAN8, 0, mKernel3x3, 0, KERNEL_SIZE3X3_NUM)
	}

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

	@WorkerThread
	override fun compute(
		width: Int,
		height: Int,
		texUnit: Int,
		texId: Int,
		texMatrix: FloatArray?,
		texMatrixOffset: Int
	) {
//		if (DEBUG) Log.v(TAG, "compute:")
		clearFocusBuffer()
		GLES31.glUseProgram(mComputeProgram)
		val texlOffsets = updateTexlOffset(width, height)

		mLock.withLock {
			// ROIをセット
			GLES31.glUniform2fv(muROILoc, 2, mROI, 0)
		}
		// テクセルオフセット
		GLES31.glUniform2fv(muTexOffsetLoc, KERNEL_SIZE3X3_NUM, texlOffsets, 0)
		// カーネル関数(行列)
		GLES31.glUniform1fv(muKernelLoc, KERNEL_SIZE3X3_NUM, mKernel3x3, 0)
		GLUtils.checkGlError("set kernel", DEBUG)
		// テクスチャ変換行列をバインド
		GLES31.glUniformMatrix4fv(muTexMatrixLoc, 1, false, texMatrix, texMatrixOffset)
		if (DEBUG) GLUtils.checkGlError("compute:glUniformMatrix4fv", DEBUG)
		// フォーカス強度分布用バッファをバインド, コンピュートシェーダーのbindingで指定した値(=2)へバインドする
		GLES31.glBindBufferBase(GLES31.GL_SHADER_STORAGE_BUFFER, 2, focusBufferId)
		if (DEBUG) GLUtils.checkGlError("compute:glBindBufferBase,bufId=$focusBufferId", DEBUG)
		// ソース映像のテクスチャをバインド
		GLES31.glActiveTexture(texUnit)
		if (DEBUG) GLUtils.checkGlError("compute:glActiveTexture,texUnit=$texUnit", DEBUG)
		GLES31.glBindTexture(mTexTarget, texId)
		if (DEBUG) GLUtils.checkGlError("compute:glBindTexture,texId=$texId", DEBUG)
		// コンピュート実行
		GLES31.glDispatchCompute((width + 15) / 16, (height + 15) / 16, 1)
		if (DEBUG) GLUtils.checkGlError("compute:glDispatchCompute", DEBUG)
		GLES31.glMemoryBarrier(GLES31.GL_SHADER_STORAGE_BARRIER_BIT or GLES31.GL_BUFFER_UPDATE_BARRIER_BIT)
	}

	companion object {
		private const val DEBUG = true	// set false on production
		private val TAG = GLComputeFocus::class.java.simpleName

		/**
		 * ラプラシアンフィルタを使ったフォーカス強度計算用コンピュートシェーダー
		 */
		private const val COMPUTE_SHADER_FOCUS_COMPUTE_ES31 =
			"""
			#version 310 es
			#extension GL_ANDROID_extension_pack_es31a : require
			#define KERNEL_SIZE3x3 (9)
			precision highp float;

			layout(local_size_x = 16, local_size_y = 16) in;
			layout(binding = 0) uniform sampler2D srcImage;
			layout(std430, binding = 2) buffer focus {
				uint counts[${FOCUS_COUNT_NUM}];
			};

			uniform vec2 uROI[2];
			uniform mat4 uTexMatrix;
			uniform float uKernel[18];
			uniform vec2 uTexOffset[KERNEL_SIZE3x3];
			const highp float FACTOR = 40.0; 
			void main() {
				vec4 pos = vec4(vec2(gl_GlobalInvocationID.xy), 0.0, 1.0);
				vec2 uv = (uTexMatrix * pos).xy;
				if ((uv.x < uROI[0].x) || (uv.x >= uROI[1].x) || (uv.y < uROI[0].y) || (uv.y >= uROI[1].y)) return;
				vec2 tuv = uv / uROI[1];
				vec3 t0 = texture(srcImage, tuv + uTexOffset[0]).rgb;
				vec3 t1 = texture(srcImage, tuv + uTexOffset[1]).rgb;
				vec3 t2 = texture(srcImage, tuv + uTexOffset[2]).rgb;
				vec3 t3 = texture(srcImage, tuv + uTexOffset[3]).rgb;
				vec3 t4 = texture(srcImage, tuv + uTexOffset[4]).rgb;
				vec3 t5 = texture(srcImage, tuv + uTexOffset[5]).rgb;
				vec3 t6 = texture(srcImage, tuv + uTexOffset[6]).rgb;
				vec3 t7 = texture(srcImage, tuv + uTexOffset[7]).rgb;
				vec3 t8 = texture(srcImage, tuv + uTexOffset[8]).rgb;
				vec3 sum = t0 * uKernel[0] + t1 * uKernel[1] + t2 * uKernel[2]
						  + t3 * uKernel[3] + t4 * uKernel[4] + t5 * uKernel[5]
						  + t6 * uKernel[6] + t7 * uKernel[7] + t8 * uKernel[8];
			    float mag = length(sum);
				uint index = uint(clamp(mag * FACTOR, 0.0, ${FOCUS_HISTOGRAM_NUM-1}.0));
				uint countsFocus = atomicAdd(counts[index], 1u) + 1u;
				atomicMax(counts[${FOCUS_MAX_INDEX}u], countsFocus);
			}				
			"""

		/**
		 * ラプラシアンフィルタを使ったフォーカス強度計算用コンピュートシェーダー
		 * RGBで計算する代わりにグレースケール画像としてラプラシアンフィルタを適用する
		 */
		private const val COMPUTE_SHADER_FOCUS_GRAY_COMPUTE_ES31 =
			"""
			#version 310 es
			#extension GL_ANDROID_extension_pack_es31a : require
			#define KERNEL_SIZE3x3 (9)
			#define STEP (1.0)
			precision highp float;

			layout(local_size_x = 16, local_size_y = 16) in;
			layout(binding = 0) uniform sampler2D srcImage;
			layout(std430, binding = 2) buffer focus {
				uint counts[${FOCUS_COUNT_NUM}];
			};

			uniform vec2 uROI[2];
			uniform mat4 uTexMatrix;
			uniform float uKernel[18];
			uniform vec2 uTexOffset[KERNEL_SIZE3x3];
			const highp float FACTOR = 80.0; 
			const highp vec3 conv = vec3(0.2125, 0.7154, 0.0721);
			void main() {
				vec4 pos = vec4(vec2(gl_GlobalInvocationID.xy), 0.0, 1.0);
				vec2 uv = (uTexMatrix * pos).xy;
				if ((uv.x < uROI[0].x) || (uv.x >= uROI[1].x) || (uv.y < uROI[0].y) || (uv.y >= uROI[1].y)) return;
				vec2 tuv = uv / uROI[1];
				float t0 = dot(texture(srcImage, tuv + uTexOffset[0]).rgb, conv) * STEP;
				float t1 = dot(texture(srcImage, tuv + uTexOffset[1]).rgb, conv) * STEP;
				float t2 = dot(texture(srcImage, tuv + uTexOffset[2]).rgb, conv) * STEP;
				float t3 = dot(texture(srcImage, tuv + uTexOffset[3]).rgb, conv) * STEP;
				float t4 = dot(texture(srcImage, tuv + uTexOffset[4]).rgb, conv) * STEP;
				float t5 = dot(texture(srcImage, tuv + uTexOffset[5]).rgb, conv) * STEP;
				float t6 = dot(texture(srcImage, tuv + uTexOffset[6]).rgb, conv) * STEP;
				float t7 = dot(texture(srcImage, tuv + uTexOffset[7]).rgb, conv) * STEP;
				float t8 = dot(texture(srcImage, tuv + uTexOffset[8]).rgb, conv) * STEP;
				float sum = t0 * uKernel[0] + t1 * uKernel[1] + t2 * uKernel[2]
						  + t3 * uKernel[3] + t4 * uKernel[4] + t5 * uKernel[5]
						  + t6 * uKernel[6] + t7 * uKernel[7] + t8 * uKernel[8];
			    float mag = abs(sum);
				uint index = uint(clamp(mag * FACTOR, 0.0, ${FOCUS_HISTOGRAM_NUM-1}.0));
				uint countsFocus = atomicAdd(counts[index], 1u) + 1u;
				atomicMax(counts[${FOCUS_MAX_INDEX}u], countsFocus);
			}				
			"""

		private const val COMPUTE_SHADER_FOCUS_READ_MIPMAP_COMPUTE_ES31 =
			"""
			#version 310 es
			#extension GL_ANDROID_extension_pack_es31a : require
			precision highp float;

			layout(local_size_x = 16, local_size_y = 16) in;
			layout(binding = 0) uniform sampler2D uEdgeMipmapTexture;
			uniform float uMaxMipLevel;
			layout(std430, binding = 2) buffer focus {
				uint counts[${FOCUS_COUNT_NUM}];
			};
			const highp float FACTOR = 1000.0; 
			void main() {
				float globalFocusScore = textureLod(uEdgeMipmapTexture, vec2(0.5, 0.5), uMaxMipLevel).r;
				uint strength = uint(globalFocusScore * FACTOR);
				atomicMax(counts[${FOCUS_STRENGTH_INDEX}u], countsFocus);
			}
			"""
	}
}
