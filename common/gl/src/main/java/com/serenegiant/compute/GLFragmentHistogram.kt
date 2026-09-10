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
import android.opengl.Matrix
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.Size
import androidx.annotation.WorkerThread
import com.serenegiant.gl.GLConst
import com.serenegiant.gl.GLUtils
import com.serenegiant.gl.ShaderConst
import com.serenegiant.nio.BufferHelper
import kotlin.math.min

/**
 * OpenGL|ES3.1以降のフラグメントシェーダーを使って映像のヒストグラムを計算するヘルパークラス
 * EGL|GLコンテキストを保持するスレッド上で呼び出すこと
 * @param isOES ソース映像がOESテクスチャかどうか
 */
@RequiresApi(api = Build.VERSION_CODES.N)
class GLFragmentHistogram  @WorkerThread constructor(
	isOES: Boolean,
): GLHistogramBase(isOES) {
	/**
	 * 頂点座標用バッファオブジェクト名
	 */
	private var mBufVertex = GLConst.GL_NO_BUFFER

	/**
	 * テクスチャ座標用バッファオブジェクト名
	 */
	private var mBufTexCoord = GLConst.GL_NO_BUFFER

	/**
	 * 頂点の数
	 */
	private val mVertexNum = (min(
		ShaderConst.DEFAULT_VERTICES_2D.size.toDouble(),
		ShaderConst.DEFAULT_TEXCOORD_2D.size.toDouble()
	) / 2).toInt()

	/**
	 * 頂点配列のサイズ
	 */
	private val mVertexSz = mVertexNum * 2

	/**
	 * 頂点座標
	 */
	private val pVertex = BufferHelper.createBuffer(ShaderConst.DEFAULT_VERTICES_2D)

	/**
	 * テクスチャ座標
	 */
	private val pTexCoord = BufferHelper.createBuffer(ShaderConst.DEFAULT_TEXCOORD_2D)
	val hProgram: Int

	@Size(value = 16)
	val mMvpMatrix = FloatArray(16)
	private var mRelease = false

	/**
	 * 頂点座標のlocation
	 */
	private val maPositionLoc: Int
	/**
	 * テクスチャ座標のlocation
	 */
	private val maTextureCoordLoc: Int
	/**
	 * モデルビュー変換行列のlocation
	 */
	private val muMVPMatrixLoc: Int
	/**
	 * テクスチャ座標変換行列のlocation
	 */
	private val muTexMatrixLoc: Int
	/**
	 * 使用するテクスチャユニットのlocation
	 */
	private val muTextureLoc: Int
	/**
	 * ヒストグラムを受け取るテクスチャRGBのlocation
	 */
	private val muHistogramRGBLoc: Int
	/**
	 * RGBヒストグラム生成時に頂点座標を飛び飛びにカウントするための変換係数
	 */
	private val muStepFactorLoc: Int

	init {
		if (DEBUG) Log.v(TAG, "コンストラクタ:create shader")
		hProgram = GLUtils.loadShader(
			VERTEX_SHADER_STEPPED_ES31,
			FRAGMENT_SHADER_HISTOGRAM_CNT_SSBO_ES31
		)
		GLES31.glUseProgram(hProgram)
		// locationの取得処理
		maPositionLoc = GLES31.glGetAttribLocation(hProgram, "aPosition")
		GLUtils.checkGlError("glGetAttribLocation(aPosition)", DEBUG)
		maTextureCoordLoc = GLES31.glGetAttribLocation(hProgram, "aTextureCoord")
		GLUtils.checkGlError("glGetAttribLocation(aTextureCoord)", DEBUG)
		muMVPMatrixLoc = GLES31.glGetUniformLocation(hProgram, "uMVPMatrix")
		GLUtils.checkGlError("glGetUniformLocation(uMVPMatrix)", DEBUG)
		muTexMatrixLoc = GLES31.glGetUniformLocation(hProgram, "uTexMatrix")
		GLUtils.checkGlError("glGetUniformLocation(uTexMatrix)", DEBUG)
		muTextureLoc = GLES31.glGetUniformLocation(hProgram, "sTexture")
		GLUtils.checkGlError("glGetAttribLocation(sTexture)", DEBUG)
		muHistogramRGBLoc = GLES31.glGetUniformLocation(hProgram, "uHistogramRGB")
		GLUtils.checkGlError("glGetUniformLocation(uHistogramRGB)", DEBUG)
		if (DEBUG) Log.v(TAG,
			"コンストラクタ:aPosition=" + maPositionLoc
				+ ",aTextureCoord=" + maTextureCoordLoc
				+ ",uMVPMatrix=" + muMVPMatrixLoc
				+ ",uTexMatrix=" + muTexMatrixLoc
				+ ",sTexture=" + muTextureLoc
				+ ",uHistogramRGB=" + muHistogramRGBLoc
		)
		// テクスチャ変換行列とモデルビュー変換行列の初期化処理
		Matrix.setIdentityM(mMvpMatrix, 0)
		GLES31.glUniformMatrix4fv(muMVPMatrixLoc, 1, false, mMvpMatrix, 0)
		GLUtils.checkGlError("glUniformMatrix4fv(muMVPMatrixLoc)", DEBUG)
		GLES31.glUniformMatrix4fv(muTexMatrixLoc, 1, false, mMvpMatrix, 0)
		GLUtils.checkGlError("glUniformMatrix4fv(muTexMatrixLoc)", DEBUG)
		muStepFactorLoc = GLES31.glGetUniformLocation(hProgram, "uStepFactor")
		GLUtils.checkGlError("glGetUniformLocation(uStepFactor)", DEBUG)
		// テクスチャ座標と頂点座標の初期化処理
		updateVertices()
	}

	@WorkerThread
	override fun release() {
		if (DEBUG) Log.v(TAG, "release:")
		if (!mRelease) {
			mRelease = true
			if (DEBUG) Log.v(TAG, "release:")
			if (mBufVertex > GLConst.GL_NO_BUFFER) {
				GLUtils.deleteBuffer(mBufVertex)
				mBufVertex = GLConst.GL_NO_BUFFER
			}
			if (mBufTexCoord > GLConst.GL_NO_BUFFER) {
				GLUtils.deleteBuffer(mBufTexCoord)
				mBufTexCoord = GLConst.GL_NO_BUFFER
			}
			GLES31.glDeleteProgram(hProgram)
		}
		super.release()
		if (DEBUG) Log.v(TAG, "release:finished")
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
		width: Int,
		height: Int,
		texUnit: Int,
		texId: Int,
		texMatrix: FloatArray?,
		texOffset: Int
	) {
//		if (DEBUG) Log.v(TAG, "compute:");
		clearHistogramBuffer()
		// 描画準備
		GLES31.glUseProgram(hProgram)
		// ステップファクターをセット
		GLES31.glUniform2fv(muStepFactorLoc, 1, mStepFactor, 0)
		if (texMatrix != null) {
			// テクスチャ変換行列が指定されている時
			GLES31.glUniformMatrix4fv(muTexMatrixLoc, 1, false, texMatrix, texOffset)
		}
		if (muMVPMatrixLoc >= 0) {
			GLES31.glUniformMatrix4fv(muMVPMatrixLoc, 1, false, mMvpMatrix, 0)
		}
		// ヒストグラムデータ用のテクスチャ/バッファをバインド
		GLES31.glBindBufferBase(GLES31.GL_SHADER_STORAGE_BUFFER, 1, histogramBufferId)
		if (DEBUG) GLUtils.checkGlError("bindTexture:glBindBufferBase", DEBUG)
		// 映像ソースのテクスチャをバインド
		GLES31.glActiveTexture(texUnit)
		if (DEBUG) GLUtils.checkGlError("bindTexture:glActiveTexture,texUnit=$texUnit,loc=$muTextureLoc", DEBUG)
		GLES31.glBindTexture(mTexTarget, texId)
		if (DEBUG) GLUtils.checkGlError("bindTexture:glBindTexture,texUnit=$texUnit,loc=$muTextureLoc", DEBUG)
		GLES31.glUniform1i(muTextureLoc, GLUtils.gLTextureUnit2Index(texUnit))
		if (DEBUG) GLUtils.checkGlError("bindTexture:glUniform1i,texUnit=$texUnit,loc=$muTextureLoc", DEBUG)
		// 描画実行
		GLES31.glDrawArrays(GLES31.GL_TRIANGLE_STRIP, 0, mVertexNum)
		// 描画終了処理
		GLES31.glBindTexture(mTexTarget, 0)
		GLES31.glUseProgram(0)
	}

	/**
	 * 頂点座標をセット
	 */
	private fun updateVertices() {
		if (DEBUG) Log.v(TAG, "updateVertices:")
		if (USE_VBO) {
			if (mBufVertex <= GLConst.GL_NO_BUFFER) {
				pVertex.clear()
				mBufVertex = GLUtils.createBuffer(GLES31.GL_ARRAY_BUFFER, pVertex, GLES31.GL_STATIC_DRAW)
				if (DEBUG) Log.v(TAG, "updateVertices:create buffer object for vertex,$mBufVertex")
			}
			if (mBufTexCoord <= GLConst.GL_NO_BUFFER) {
				pTexCoord.clear()
				mBufTexCoord = GLUtils.createBuffer(GLES31.GL_ARRAY_BUFFER, pTexCoord, GLES31.GL_STATIC_DRAW)
				if (DEBUG) Log.v(TAG, "updateVertices:create buffer object for tex coord,$mBufTexCoord")
			}
			// 頂点座標をセット
			GLES31.glBindBuffer(GLES31.GL_ARRAY_BUFFER, mBufVertex)
			GLES31.glVertexAttribPointer(maPositionLoc, 2, GLES31.GL_FLOAT, false, 0, 0)
			GLES31.glEnableVertexAttribArray(maPositionLoc)
			// テクスチャ座標をセット
			GLES31.glBindBuffer(GLES31.GL_ARRAY_BUFFER, mBufTexCoord)
			GLES31.glVertexAttribPointer(maTextureCoordLoc, 2, GLES31.GL_FLOAT, false, 0, 0)
			GLES31.glEnableVertexAttribArray(maTextureCoordLoc)
		} else {
			// 頂点座標をセット
			pVertex.clear()
			GLES31.glVertexAttribPointer(maPositionLoc, 2, GLES31.GL_FLOAT, false, mVertexSz, pVertex)
			GLES31.glEnableVertexAttribArray(maPositionLoc)
			// テクスチャ座標をセット
			pTexCoord.clear()
			GLES31.glVertexAttribPointer(maTextureCoordLoc, 2, GLES31.GL_FLOAT, false, mVertexSz, pTexCoord)
			GLES31.glEnableVertexAttribArray(maTextureCoordLoc)
		}
	}

	companion object {
		private const val DEBUG = false	// set false on production
		private val TAG = GLFragmentHistogram::class.java.simpleName

		/**
		 * バッファオブジェクトを使って描画するかどうか
		 */
		private const val USE_VBO = true

		/**
		 * RGBヒストグラムカウント用のモデルビュー変換行列とテクスチャ変換行列適用する頂点シェーダー
		 * for ES3
		 */
		private const val VERTEX_SHADER_STEPPED_ES31 =
			"""
			#version 310 es
			uniform mat4 uMVPMatrix;
			uniform mat4 uTexMatrix;
			uniform highp vec2 uStepFactor;
			in highp vec4 aPosition;
			in highp vec4 aTextureCoord;
			out highp vec2 vTextureCoord;
			void main() {
				gl_Position = uMVPMatrix * aPosition;
				vTextureCoord = (uTexMatrix * aTextureCoord).xy * uStepFactor;
			}
			"""

		/**
		 * RGBヒストグラムのカウント用フラグメントシェーダー
		 * Geminiのレスポンスから作成
		 */
		private const val FRAGMENT_SHADER_HISTOGRAM_CNT_SSBO_ES31 =
			"""
			#version 310 es
			#extension GL_ANDROID_extension_pack_es31a : require
			#define STEP (255.0)
			precision highp float;
			precision highp int;
			
			in vec2 vTextureCoord;
			uniform sampler2D sTexture;
			layout(std430, binding = 1) buffer Histogram {
				uint counts[256 * 6];
			};
			const highp vec3 conv = vec3(0.2125, 0.7154, 0.0721);

			void main() {
				if ((vTextureCoord.x < 0.0) || (vTextureCoord.x > 1.0)
					|| (vTextureCoord.y < 0.0) || (vTextureCoord.y > 1.0)) return;
				vec4 color = texture(sTexture, vTextureCoord);
			
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
