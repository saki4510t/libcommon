package com.serenegiant.gl
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
import androidx.annotation.AnyThread
import androidx.annotation.FloatRange
import androidx.annotation.IntDef
import androidx.annotation.RequiresApi
import androidx.annotation.Size
import androidx.annotation.WorkerThread
import com.serenegiant.compute.GLComputeHistogram
import com.serenegiant.compute.GLFragmentHistogram
import com.serenegiant.compute.GLHistogramBase
import com.serenegiant.gl.GLConst.TexTarget
import com.serenegiant.gl.GLConst.TexUnit
import com.serenegiant.graphics.IMirror
import com.serenegiant.graphics.IMirror.MirrorMode
import com.serenegiant.graphics.MatrixUtils
import com.serenegiant.system.Time
import com.serenegiant.nio.BufferHelper
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.min

/**
 * RGBヒストグラム作成のヘルパークラス
 * OpenGL|ES3.1以降が必要
 * XXX 公式にはAPI>=21でES3.1対応だけど一部端末でES3の機能が抜けている場合があるのでAPI>=24とする
 * ヒストグラム平坦化による補正描画を行う場合には定期的に#compute, ##, #drawを呼び出す必要がある
 *
 * コンストラクタでequalize=falseとしてあとからヒストグラム平坦化補正を有効にはできない
 *
 * コンストラクタでequalize=trueとして#equalizeを呼ばない場合、または#resetEqualizeを呼んで
 * LUTを呼ぶとヒストグラム平坦化補正無しと同等の描画になる。
 * ただしequalize=trueの場合はt#drawで常にRGB→HSV→RGBの変換とKUT参照が実行されるためヒストグラム
 * 平坦化補正が不要な場合はequalize=falseで生成するべき
 * @param isOES ソース映像がOESテクスチャかどうか
 * @param maxFps ヒストグラムの最大更新頻度 デフォルトは2.0fps
 * @param equalize #drawでヒストグラム平坦化による補正描画を行うかどうか, デフォルトはfalseでヒストグラム平坦化による補正描画は行わない
 * @param useComputeShader コンピュートシェーダーを使ってヒストグラムを計算するかどうか、
 *        trueならコンピュートシェーダーを使う、falseならフラグメントシェーダーを使ってヒストグラムを計算する
 *        デフォルトはtrueでコンピュートシェーダーを使う
 */
@RequiresApi(api = Build.VERSION_CODES.N)
class GLHistogram @WorkerThread @JvmOverloads constructor(
	@JvmField
	val isOES: Boolean,
	@FloatRange(from = 0.0) maxFps: Float = 2.0f,
	@JvmField
	val equalize: Boolean = false,
	private val useComputeShader: Boolean = true,
) : IMirror {
	/**
	 * ヒストグラムの次回更新予定時間、ナノ秒
	 */
	private var mNextDrawNs: Long
	/**
	 * ヒストグラムの更新周期、ナノ秒
	 */
	private val mIntervalsNs: Long
	/**
	 * ヒストグラムの更新時刻の幅、ナノ秒
	 */
	private val mIntervalsDeltaNs: Long
	/**
	 * ヒストグラムを計算するためのGLHistogramBase
	 */
	private var mGLCompute: GLHistogramBase? = null

	/**
	 * テクスチャターゲット
	 * GL_TEXTURE_EXTERNAL_OESかGL_TEXTURE_2D
	 */
	@TexTarget
	val texTarget: Int = if (isOES) GLConst.GL_TEXTURE_EXTERNAL_OES else GLES31.GL_TEXTURE_2D
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
	/**
	 * GLHistogramBaseで生成したヒストグラムデータを保持しているシェーダーストレージバッファオブジェクトのID
	 */
	private val histogramRGBId: Int
	/**
	 * 元映像(と必要に応じてヒストグラム)を描画するためのシェーダー
	 */
	private val hProgram: Int
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
	 * ヒストグラム表示領域のロケーション
	 */
	private val muEmbedRegionLoc: Int
	/**
	 * ヒストグラムの種類指定用ロケーション
	 */
	private val muHistogramTypeLoc: Int
	/**
	 * 排他制御用
	 */
	private val mLock = ReentrantLock()
	/**
	 * ヒストグラムの表示領域を保持するfloat配列
	 * [minU,minV]-[maxU,maxVにヒストグラムを表示する]
	 * ぞれぞれの値は[0.0..1.0]
	 */
	@Size(value = 4)
	private val mHistogramRegion = floatArrayOf(
		0.1f, 0.75f,  // minU, minV,
		0.6f, 0.9f,  // maxU, maxV,
	)
	/**
	 * 現在のミラー設定
	 */
	@MirrorMode
	private var mMirror = IMirror.MIRROR_NORMAL

	init {
		if (DEBUG) Log.v(TAG, "コンストラクタ:isOES=$isOES,useComputeShader=$useComputeShader,maxFps=$maxFps")
		mIntervalsNs = Math.round(1000000000.0 / (if (maxFps > 0.0f) maxFps else 2.0f))
		mIntervalsDeltaNs = -Math.round(mIntervalsNs * 0.03) // 3%ならショートしても良いことにする
		mNextDrawNs = Time.nanoTime() + mIntervalsNs
		val compute = if (useComputeShader) {
			if (DEBUG) Log.v(TAG, "コンストラクタ:create GLComputeHistogram")
			GLComputeHistogram(isOES)
		} else {
			if (DEBUG) Log.v(TAG, "コンストラクタ:create GLFragmentHistogram")
			GLFragmentHistogram(isOES)
		} // if (useComputeShader) {
		mGLCompute = compute
		histogramRGBId = compute.histogramBufferId
		if (DEBUG) Log.v(TAG, "コンストラクタ:create mRendererDrawer,isOES=$isOES,equalize=$equalize")
		if (DEBUG) Log.v(TAG, "コンストラクタ:create shader")
		hProgram = GLUtils.loadShader(
			ShaderConst.VERTEX_SHADER_ES31,
			if (equalize) FRAGMENT_SHADER_MIX_SSBO_EQ_ES31 else FRAGMENT_SHADER_MIX_SSBO_ES31)
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
		muEmbedRegionLoc = GLES31.glGetUniformLocation(hProgram, "uEmbedRegion")
		GLUtils.checkGlError("コンストラクタ:glGetUniformLocation(sTexture2)", DEBUG)
		muHistogramTypeLoc = GLES31.glGetUniformLocation(hProgram, "uHistogramType")
		GLUtils.checkGlError("コンストラクタ:glGetUniformLocation(uHistogramType)", DEBUG)
		if (DEBUG) Log.v(TAG,
			"コンストラクタ:aPosition=" + maPositionLoc
				+ ",aTextureCoord=" + maTextureCoordLoc
				+ ",uMVPMatrix=" + muMVPMatrixLoc
				+ ",uTexMatrix=" + muTexMatrixLoc
				+ ",sTexture=" + muTextureLoc
		)
		// テクスチャ変換行列とモデルビュー変換行列の初期化処理
		Matrix.setIdentityM(mMvpMatrix, 0)
		GLES31.glUniformMatrix4fv(muMVPMatrixLoc, 1, false, mMvpMatrix, 0)
		GLUtils.checkGlError("glUniformMatrix4fv(muMVPMatrixLoc)", DEBUG)
		GLES31.glUniformMatrix4fv(muTexMatrixLoc, 1, false, mMvpMatrix, 0)
		GLUtils.checkGlError("glUniformMatrix4fv(muTexMatrixLoc)", DEBUG)
		// テクスチャ座標と頂点座標の初期化処理
		updateVertices()
		if (!isOES) {
			mirror = IMirror.MIRROR_VERTICAL
		}
	}

	/**
	 * ヒストグラムを計算
	 * @param width 映像幅、ピクセル
	 * @param height 映像高さ、ピクセル
	 * @param texUnit テクスチャユニット
	 * @param texId テクスチャID
	 * @param texMatrix テクスチャ変換行列
	 * @param texOffset テクスチャ変換行列のオフセット
	 * @return ヒストグラムの計算・更新を行ったかどうかs
	 */
	@WorkerThread
	fun compute(
		width: Int, height: Int,
		@TexUnit texUnit: Int, texId: Int,
		@Size(min = 16) texMatrix: FloatArray?,
		texOffset: Int
	): Boolean {
		val startTimeNs = Time.nanoTime()
		val result = (startTimeNs - mNextDrawNs) > mIntervalsDeltaNs
		if (result) {
			mNextDrawNs = startTimeNs + mIntervalsNs
			mGLCompute?.let { compute ->
				compute.setROI(0, 0, width, height)
				compute.compute(width, height, GLES31.GL_TEXTURE0, texId, texMatrix, texOffset)
			}
		}

		return result
	}

	/**
	 * ヒストグラムのカウントと描画を実行
	 * EGL|GLコンテキストの存在するスレッド上で実行すること
	 * コンピュートシェーダー:
	 * 1280x720で0.4ms弱@SM-M26かかかる
	 * 1920x1080で1.1ms弱@SM-M26かかかる
	 * 通常のシェーダー:
	 * 1280x720で0.36ms弱@SM-M26かかかる
	 * 1920x1080で1.06ms弱@SM-M26かかかる
	 * @param width 映像幅、ピクセル
	 * @param height 映像高さ、ピクセル
	 * @param texUnit テクスチャユニット
	 * @param texId テクスチャID
	 * @param texMatrix テクスチャ変換行列
	 * @param texOffset テクスチャ変換行列のオフセット
	 */
	@WorkerThread
	fun draw(
		width: Int, height: Int,
		@TexUnit texUnit: Int, texId: Int,
		@Size(min = 16) texMatrix: FloatArray?, texOffset: Int
	) {
		GLES31.glUseProgram(hProgram)
		if (DEBUG) GLUtils.checkGlError("draw:glUseProgram", DEBUG)
		mLock.withLock {
			GLES31.glUniform4fv(muEmbedRegionLoc, 1, mHistogramRegion, 0)
			GLES31.glUniform1i(muHistogramTypeLoc, histogramType)
		}
		if (DEBUG) GLUtils.checkGlError("draw:glUniform4fv,loc=$muEmbedRegionLoc", DEBUG)
		if (texMatrix != null) {
			// テクスチャ変換行列が指定されている時
			GLES31.glUniformMatrix4fv(muTexMatrixLoc, 1, false, texMatrix, texOffset)
		}
		if (muMVPMatrixLoc >= 0) {
			GLES31.glUniformMatrix4fv(muMVPMatrixLoc, 1, false, mMvpMatrix, 0)
		}
		// ヒストグラムデータ用のテクスチャ/バッファをバインド
		GLES31.glBindBufferBase(GLES31.GL_SHADER_STORAGE_BUFFER, 1, histogramRGBId)
		if (DEBUG) GLUtils.checkGlError("bindTexture:glBindBufferBase", DEBUG)
		// 映像ソースのテクスチャをバインド
		GLES31.glActiveTexture(texUnit)
		if (DEBUG) GLUtils.checkGlError("bindTexture:glActiveTexture,texUnit=$texUnit,loc=$muTextureLoc", DEBUG)
		GLES31.glBindTexture(texTarget, texId)
		if (DEBUG) GLUtils.checkGlError("bindTexture:glBindTexture,texUnit=$texUnit,loc=$muTextureLoc", DEBUG)
		GLES31.glUniform1i(muTextureLoc, GLUtils.gLTextureUnit2Index(texUnit))
		if (DEBUG) GLUtils.checkGlError("bindTexture:glUniform1i,texUnit=$texUnit,loc=$muTextureLoc", DEBUG)
		// 描画実行
		GLES31.glDrawArrays(GLES31.GL_TRIANGLE_STRIP, 0, mVertexNum)
		// 描画終了処理
		GLES31.glBindTexture(texTarget, 0)
		GLES31.glUseProgram(0)
	}

	/**
	 * 関係するリソースを破棄
	 * EGL|GLコンテキストの存在するスレッド上で実行すること
	 */
	@WorkerThread
	fun release() {
		if (DEBUG) Log.v(TAG, "release:")
		mGLCompute?.release()
		mGLCompute = null
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
	}

	/**
	 * ミラー設定をセット
	 * IMirrorの実装
	 * @param mirror
	 */
	override fun setMirror(@MirrorMode mirror: Int) {
		mLock.withLock {
			if (mMirror != mirror) {
				mMirror = mirror
				MatrixUtils.setMirror(mMvpMatrix, 0, mirror)
			}
		}
	}

	/**
	 * 現在のミラー設定を取得
	 * IMirrorの実装
	 * @return 現在のミラー設定
	 */
	@MirrorMode
	override fun getMirror(): Int {
		return mMirror
	}

	/**
	 * ヒストグラムの表示領域を設定する
	 * [minU,minV]-[maxU,maxVにヒストグラムを表示する]
	 * ぞれぞれの値は[0.0..1.0]
	 * @param minU ヒストグラム表示矩形頂点1(テクスチャ座標)u
	 * @param minV ヒストグラム表示矩形頂点1(テクスチャ座標)v
	 * @param maxU ヒストグラム表示矩形頂点2(テクスチャ座標)u
	 * @param maxV ヒストグラム表示矩形頂点2(テクスチャ座標)v
	 */
	@AnyThread
	fun setHistogram(
		minU: Float, minV: Float,
		maxU: Float, maxV: Float
	) {
		mLock.withLock {
			mHistogramRegion[0] = minU
			mHistogramRegion[1] = minV
			mHistogramRegion[2] = maxU
			mHistogramRegion[3] = maxV
		}
	}

	/**
	 * ヒストグラムカウント時のサンプリング間隔を設定
	 */
	@AnyThread
	fun setStepFactor(
		@FloatRange(from = 1.0) sx: Float, @FloatRange(from = 1.0) sy: Float) {
		mGLCompute?.let { compute ->
			compute.setStepFactor(sx, sy)
		}
	}

	/**
	 * ヒストグラムの種類
	 */
	@HistogramType
	var histogramType: Int = HISTOGRAM_RGB

	/**
	 * ヒストグラム平坦化用のLUTを計算
	 * 累積分布関数でLUTを計算する
	 * EGL|GLコンテキストの存在するスレッド上で実行すること
	 */
	@OptIn(ExperimentalUnsignedTypes::class)
	@WorkerThread
	fun equalize() {
		mGLCompute?.let { compute ->
			compute.equalize()
		}
	}

	/**
	 * ヒストグラム平坦化補正用のLUTをリセットする
	 */
	@WorkerThread
	fun resetEqualize() {
		mGLCompute?.let { compute ->
			compute.resetEqualize()
		}
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
		private const val DEBUG = false // set false on production
		private val TAG = GLHistogram::class.java.simpleName

		/**
		 * ヒストグラムを描画しない
		 */
		const val HISTOGRAM_NON = 0
		/**
		 * R成分のヒストグラムを描画
		 */
		const val HISTOGRAM_R = 1
		/**
		 * G成分のヒストグラムを描画
		 */
		const val HISTOGRAM_G = 2
		/**
		 * B成分のヒストグラムを描画
		 */
		const val HISTOGRAM_B = 4
		/**
		 * 輝度成分のヒストグラムを描画
		 */
		const val HISTOGRAM_I = 8
		/**
		 * RGBのヒストグラムを描画
		 */
		const val HISTOGRAM_RGB = HISTOGRAM_R or HISTOGRAM_G or HISTOGRAM_B

		/**
		 * ヒストグラムの種類
		 */
		@IntDef(
			HISTOGRAM_NON,
			HISTOGRAM_R,
			HISTOGRAM_G,
			HISTOGRAM_B,
			HISTOGRAM_I,
			HISTOGRAM_RGB,
		)
		@Retention(AnnotationRetention.SOURCE)
		annotation class HistogramType

		/**
		 * バッファオブジェクトを使って描画するかどうか
		 */
		private const val USE_VBO = true

		/**
		 * 元映像にRGBヒストグラムを合成して描画するフラグメントシェーダー
		 * これは今は使っていない
		 */
		private const val FRAGMENT_SHADER_HISTOGRAM_DRAW_SSBO_ES31 =
			"""
			#version 310 es
			#define STEP (255.0)
			precision highp float;
			precision highp int;
	
			in vec2 vTextureCoord;
			uniform sampler2D sTexture;
			layout(std430, binding = 1) buffer Histogram {
				uint counts[256 * 6];
			};
			layout(location = 0) out vec4 o_FragColor;
			const float EPS = 0.01;
			const float SCALE = 0.5;
			void main() {
				vec4 color = texture(sTexture, vTextureCoord);
				uint index = uint(vTextureCoord.x * STEP);	// 0..STEP
				// ヒストグラムを取得
				float countsR = float(counts[index]);
				float countsG = float(counts[index + 256u]);
				float countsB = float(counts[index + 512u]);
	//			float countsI = float(counts[index + 768u]);
				// 最大値を取得して正規化
				float max = float(counts[1028u]);
				vec3 counts = (vec3(countsR, countsG, countsB) / max) * SCALE;
				vec3 countsL = counts - EPS;
			
				vec3 histogram = vec3(color.rgb);
				float histogramY = 1.0 - vTextureCoord.y;
				if ((histogramY > countsL.r) && (histogramY < counts.r)) {
					histogram.r = 1.0;
				}
				if ((histogramY > countsL.g) && (histogramY < counts.g)) {
					histogram.g = 1.0;
				}
				if ((histogramY > countsL.b) && (histogramY < counts.b)) {
					histogram.b = 1.0;
				}
				o_FragColor = vec4(mix(color.rgb, histogram, 0.5), color.a);
			}
			"""

		/**
		 * 元映像のテクスチャとヒストグラムを合成して表示するフラグメントシェーダー
		 */
		private const val FRAGMENT_SHADER_MIX_SSBO_ES31 =
			"""
			#version 310 es
			#define STEP (255.0)
			precision highp float;
			const float bkColor = 0.2;
			in vec2 vTextureCoord;
			uniform sampler2D sTexture;
			layout(std430, binding = 1) buffer Histogram {
				uint counts[256 * 6];
			};
			// Format: vec4(minU, minV, maxU, maxV) in normalized UV space (0.0 to 1.0)
			uniform vec4 uEmbedRegion;
			uniform int uHistogramType;
			layout(location = 0) out vec4 o_FragColor;
			const float EPS = 0.01;
			const float SCALE = 0.75;
			void main() {
				bool isInsideEmbedRegion =
					vTextureCoord.x >= uEmbedRegion.x
					&& vTextureCoord.x <= uEmbedRegion.z
					&& vTextureCoord.y >= uEmbedRegion.y
					&& vTextureCoord.y <= uEmbedRegion.w;
				highp vec4 tex1 = texture(sTexture, vTextureCoord);
				if (isInsideEmbedRegion) {
					vec2 relativePosInEmbedRegion = vTextureCoord - uEmbedRegion.xy;
					vec2 embedRegionSize = uEmbedRegion.zw - uEmbedRegion.xy; // (maxU - minU, maxV - minV)
					vec2 uvForTexture2 = relativePosInEmbedRegion / embedRegionSize;
					uint index = uint(uvForTexture2.x * STEP);	// 0..STEP
					// ヒストグラムを取得
					float countsR = float(counts[index]);
					float countsG = float(counts[index + 256u]);
					float countsB = float(counts[index + 512u]);
	//				float countsI = float(counts[index + 768u]);
					// 最大値を取得して正規化
					float max = float(counts[1028u]) * SCALE;
					vec3 counts = vec3(countsR, countsG, countsB) / max;
					vec3 countsL = counts - EPS;
					vec3 histogram = vec3(0.0);
					float histogramY = 1.0 - uvForTexture2.y;
					if (((uHistogramType & 1) == 1) && (histogramY > countsL.r) && (histogramY < counts.r)) {
						histogram.r = 1.0;
					}
					if (((uHistogramType & 2) == 2) && (histogramY > countsL.g) && (histogramY < counts.g)) {
						histogram.g = 1.0;
					}
					if (((uHistogramType & 4) == 4) && (histogramY > countsL.b) && (histogramY < counts.b)) {
						histogram.b = 1.0;
					}
					if (((uHistogramType & 8) == 8) && (histogramY > countsL.b) && (histogramY < counts.b)) {
						histogram.r = 1.0;
						histogram.g = 1.0;
						histogram.b = 1.0;
					}
					o_FragColor = vec4(mix(tex1.rgb, histogram, 0.5), tex1.a);
				} else {
					o_FragColor = tex1;
				}
			}
			"""

		/**
		 * 元映像のテクスチャとストグラムを合成して表示するフラグメントシェーダー
		 * ヒストグラム平坦化のLUT値を使って輝度を調整する
		 */
		private const val FRAGMENT_SHADER_MIX_SSBO_EQ_ES31 =
			"""
			#version 310 es
			#define STEP (255.0)
			precision highp float;
			const float bkColor = 0.2;
			in vec2 vTextureCoord;
			uniform sampler2D sTexture;
			layout(std430, binding = 1) buffer Histogram {
				uint counts[256 * 6];
			};
			// Format: vec4(minU, minV, maxU, maxV) in normalized UV space (0.0 to 1.0)
			uniform vec4 uEmbedRegion;
			uniform int uHistogramType;
			layout(location = 0) out vec4 o_FragColor;
			const float EPS = 0.01;
			const float SCALE = 0.75;
			vec3 rgb2hsv(vec3 c) {
				const highp vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
				const highp float e = 1.0e-10;
				vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));
				vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));
				float d = q.x - min(q.w, q.y);
				return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);
			}
			vec3 hsv2rgb(vec3 c) {
				const highp vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
				vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
				return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
			}
			void main() {
				bool isInsideEmbedRegion =
					(uHistogramType & 9) != 0
					&& vTextureCoord.x >= uEmbedRegion.x
					&& vTextureCoord.x <= uEmbedRegion.z
					&& vTextureCoord.y >= uEmbedRegion.y
					&& vTextureCoord.y <= uEmbedRegion.w;
				highp vec4 tex1 = texture(sTexture, vTextureCoord);
				// ヒストグラム平坦化補正用LUTを参照して輝度を調整
				highp vec3 hsv = rgb2hsv(tex1.rgb);
				hsv.z = float(counts[1280u + uint(hsv.z * 255.0)]) / 255.0;
				tex1 = vec4(hsv2rgb(hsv), tex1.a);
				// 描画処理
				if (isInsideEmbedRegion) {
					vec2 relativePosInEmbedRegion = vTextureCoord - uEmbedRegion.xy;
					vec2 embedRegionSize = uEmbedRegion.zw - uEmbedRegion.xy; // (maxU - minU, maxV - minV)
					vec2 uvForTexture2 = relativePosInEmbedRegion / embedRegionSize;
					uint index = uint(uvForTexture2.x * STEP);	// 0..STEP
					// ヒストグラムを取得
					float countsR = float(counts[index]);
					float countsG = float(counts[index + 256u]);
					float countsB = float(counts[index + 512u]);
	//				float countsI = float(counts[index + 768u]);
					// 最大値を取得して正規化
					float max = float(counts[1028u]) * SCALE;
					vec3 counts = vec3(countsR, countsG, countsB) / max;
					vec3 countsL = counts - EPS;
					vec3 histogram = vec3(0.0);
					float histogramY = 1.0 - uvForTexture2.y;
					if (((uHistogramType & 1) == 1) && (histogramY > countsL.r) && (histogramY < counts.r)) {
						histogram.r = 1.0;
					}
					if (((uHistogramType & 2) == 2) && (histogramY > countsL.g) && (histogramY < counts.g)) {
						histogram.g = 1.0;
					}
					if (((uHistogramType & 4) == 4) && (histogramY > countsL.b) && (histogramY < counts.b)) {
						histogram.b = 1.0;
					}
					if (((uHistogramType & 8) == 8) && (histogramY > countsL.b) && (histogramY < counts.b)) {
						histogram.r = 1.0;
						histogram.g = 1.0;
						histogram.b = 1.0;
					}
					o_FragColor = vec4(mix(tex1.rgb, histogram, 0.5), tex1.a);
				} else {
					o_FragColor = tex1;
				}
			}
			"""

		/**
		 * RGBヒストグラムのみを全面に描画するフラグメントシェーダー
		 * これは今は使っていない
		 */
		private const val FRAGMENT_SHADER_HISTOGRAM_DRAW_ONLY_SSBO_ES31 =
			"""
			#version 310 es
			#define STEP (255.0)
			precision highp float;
			precision highp int;
	
			in vec2 vTextureCoord;
			uniform sampler2D sTexture;
			layout(std430, binding = 1) buffer Histogram {
				uint counts[256 * 6];
			};
			layout(location = 0) out vec4 o_FragColor;
			const float EPS = 0.01;
			const float SCALE = 0.75;
			void main() {
	//			vec4 color = texture(sTexture, vTextureCoord);
				uint index = uint(vTextureCoord.x * STEP);	// 0..STEP
				// ヒストグラムを取得
				float countsR = float(counts[index]);
				float countsG = float(counts[index + 256u]);
				float countsB = float(counts[index + 512u]);
	//			float countsI = float(counts[index + 768u]);
				// 最大値を取得して正規化
				float max = float(counts[1028u]) * SCALE;
				vec3 counts = vec3(countsR, countsG, countsB) / max;
				vec3 countsL = counts - EPS;
			
				vec4 histogram = vec4(0.0);
				float histogramY = 1.0 - vTextureCoord.y;
				if ((histogramY > countsL.r) && (histogramY < counts.r)) {
					histogram.r = 1.0;
					histogram.a = 1.0;
				}
				if ((histogramY > countsL.g) && (histogramY < counts.g)) {
					histogram.g = 1.0;
					histogram.a = 1.0;
				}
				if ((histogramY > countsL.b) && (histogramY < counts.b)) {
					histogram.b = 1.0;
					histogram.a = 1.0;
				}
				o_FragColor = histogram;
			}	
			"""

		/**
		 * 元映像のテクスチャと別途オフスクリーン描画したヒストグラムテクスチャを合成して表示するフラグメントシェーダー
		 * これは今は使っていない
		 */
		private const val FRAGMENT_SHADER_MIX_ES31 =
			"""
			#version 310 es
			precision highp float;
			const float bkColor = 0.2;
			in vec2 vTextureCoord;
			uniform sampler2D sTexture;
			uniform sampler2D sTexture2;
			// Format: vec4(minU, minV, maxU, maxV) in normalized UV space (0.0 to 1.0)
			uniform vec4 uEmbedRegion;
			layout(location = 0) out vec4 o_FragColor;
			void main() {
				bool isInsideEmbedRegion =
					vTextureCoord.x >= uEmbedRegion.x
					&& vTextureCoord.x <= uEmbedRegion.z
					&& vTextureCoord.y >= uEmbedRegion.y
					&& vTextureCoord.y <= uEmbedRegion.w;
				highp vec4 tex1 = texture(sTexture, vTextureCoord);
				if (isInsideEmbedRegion) {
					vec2 relativePosInEmbedRegion = vTextureCoord - uEmbedRegion.xy;
					vec2 embedRegionSize = uEmbedRegion.zw - uEmbedRegion.xy; // (maxU - minU, maxV - minV)
					vec2 uvForTexture2 = relativePosInEmbedRegion / embedRegionSize;
					highp vec4 tex2 = texture(sTexture2, uvForTexture2);
					tex2 = mix(tex2, vec4(bkColor), 1.0 - tex2.a);
					o_FragColor = vec4(mix(tex1.rgb, tex2.rgb, 0.5), tex1.a);
				} else {
					o_FragColor = tex1;
				}
			}	
			"""
	}
}
