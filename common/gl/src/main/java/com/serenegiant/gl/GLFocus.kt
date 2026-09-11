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
import androidx.annotation.RequiresApi
import androidx.annotation.Size
import androidx.annotation.WorkerThread
import com.serenegiant.compute.GLComputeFocus
import com.serenegiant.compute.GLFocusBase
import com.serenegiant.gl.GLConst.TexTarget
import com.serenegiant.gl.GLConst.TexUnit
import com.serenegiant.graphics.IMirror
import com.serenegiant.graphics.IMirror.MirrorMode
import com.serenegiant.graphics.MatrixUtils
import com.serenegiant.nio.BufferHelper
import com.serenegiant.system.Time
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.log
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * フォーカス強度の計算を行うヘルパークラス
 * XXX 公式にはAPI>=21でES3.1対応だけど一部端末でES3の機能が抜けている場合があるのでAPI>=24とする
 * @param isOES ソース映像がOESテクスチャかどうか
 * @param maxFps フォーカス強度の最大更新頻度 デフォルトは2.0fps
 */
@RequiresApi(api = Build.VERSION_CODES.N)
class GLFocus @WorkerThread constructor(
	@JvmField
	val isOES: Boolean,
	@FloatRange(from = 0.0) maxFps: Float = 2.0f,
) : IMirror {
	/**
	 * フォーカス強度の次回更新予定時間、ナノ秒
	 */
	private var mNextDrawNs: Long
	/**
	 * フォーカス強度の更新周期、ナノ秒
	 */
	private val mIntervalsNs: Long
	/**
	 * フォーカス強度の更新時刻の幅、ナノ秒
	 */
	private val mIntervalsDeltaNs: Long
	/**
	 * フォーカス強度計算用GLFocusBase
	 */
	private var mGLCompute: GLFocusBase? = null
	/**
	 * 排他制御用
	 */
	private val mLock = ReentrantLock()

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
	private val histogramRGBId: Int
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
	 * フォーカス強度分布表示領域のロケーション
	 */
	private val muEmbedRegionLoc: Int
	/**
	 * フォーカス強度分布の表示領域を保持するfloat配列
	 * [minU,minV]-[maxU,maxVにフォーカス強度分布を表示する]
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
		if (DEBUG) Log.v(TAG, "コンストラクタ:isOES=$isOES,maxFps=$maxFps")
		mIntervalsNs = Math.round(1000000000.0 / (if (maxFps > 0.0f) maxFps else 2.0f))
		mIntervalsDeltaNs = -Math.round(mIntervalsNs * 0.03) // 3%ならショートしても良いことにする
		mNextDrawNs = Time.nanoTime() + mIntervalsNs
		val compute = GLComputeFocus(isOES)
		mGLCompute = compute

		histogramRGBId = compute.focusBufferId
		if (DEBUG) Log.v(TAG, "コンストラクタ:create shader")
		hProgram = GLUtils.loadShader(
			ShaderConst.VERTEX_SHADER_ES31,
			FRAGMENT_SHADER_MIX_SSBO_ES31
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
		GLUtils.checkGlError("glGetUniformLocation(uHistogramRGB)", DEBUG)
		muEmbedRegionLoc = GLES31.glGetUniformLocation(hProgram, "uEmbedRegion")
		GLUtils.checkGlError("コンストラクタ:glGetUniformLocation(sTexture2)", DEBUG)
		if (DEBUG) Log.v(TAG, "コンストラクタ:"
			+ "aPosition=" + maPositionLoc
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
	 * フォーカス強度分布を読み込むのに使うIntArray
	 */
	private val mReadBuffer = IntArray(GLFocusBase.FOCUS_COUNT_NUM)
	/**
	 * フォーカス強度分布を計算
	 * @param width 映像幅、ピクセル
	 * @param height 映像高さ、ピクセル
	 * @param texUnit テクスチャユニット
	 * @param texId テクスチャID
	 * @param texMatrix テクスチャ変換行列
	 * @param texOffset テクスチャ変換行列のオフセット
	 * @return フォーカス強度の計算・更新を行ったかどうかs
	 */
	@OptIn(ExperimentalUnsignedTypes::class)
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
				// とりあえず映像全面で計算
				compute.setROI(0.0f, 0.0f, width.toFloat(), height.toFloat())
//				// こっちは画面中央の1/4領域
//				val w = width.toFloat() / 4.0f
//				val h = height.toFloat() / 4.0f
//				compute.setROI(w * 1.5f, h * 1.5f, w * 2.5f, h * 2.5f)
				compute.compute(width, height, GLES31.GL_TEXTURE0, texId, texMatrix, texOffset)

				// FIXME テスト用にフォーカス強度分布の分散を計算
				//       平でコントラストの少ない面や全面がピンボケだと見かけ上
				//       分散が高く計算されてしまうので何かしらからの足切りが必要
				// FIXME ROIで画素数を制限した時に分散の値が全体的に低くなってしまう
				//       全体画素数に対するROI内画素数の比とかで補正する？
				// XXX 一番最後は最大値なので除外する
				//     平滑面やコントラストが低いと80-100より下に値が集中するので除外して計算する
				val work = compute.readFocusBuffer(mReadBuffer).asUIntArray()
					.slice(0..< GLFocusBase.FOCUS_HISTOGRAM_NUM)
				if (DEBUG) Log.v(TAG, "compute:${work}")
				// 個数
				val n = work.size.toDouble()
				// 平均値を計算
				val ave = work.sum().toDouble() / n	// 平均
				// 分散を計算...各値と平均値との差の2乗和をデータ点数で割る
				val sd = work.map { it.toDouble() - ave }.sumOf { it * it } / n
				if (DEBUG) Log.v(TAG, "compute:sd=${sd.roundToLong()},${(log(sd, 10.0) * 20.0 - 20.0).roundToInt()}")
			}
		}

		return result
	}

	/**
	 * 元画像とフォーカス強度分布の描画を実行
	 * EGL|GLコンテキストの存在するスレッド上で実行すること
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
		}
		if (DEBUG) GLUtils.checkGlError("draw:glUniform4fv,loc=$muEmbedRegionLoc", DEBUG)
		if (texMatrix != null) {
			// テクスチャ変換行列が指定されている時
			GLES31.glUniformMatrix4fv(muTexMatrixLoc, 1, false, texMatrix, texOffset)
		}
		if (muMVPMatrixLoc >= 0) {
			GLES31.glUniformMatrix4fv(muMVPMatrixLoc, 1, false, mMvpMatrix, 0)
		}
		// フォーカス強度分布用バッファをバインド, コンピュートシェーダーのbindingで指定した値(=2)へバインドする
		GLES31.glBindBufferBase(GLES31.GL_SHADER_STORAGE_BUFFER, 2, histogramRGBId)
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

	override fun setMirror(mirror: Int) {
		mLock.withLock {
			if (mMirror != mirror) {
				mMirror = mirror
				MatrixUtils.setMirror(mMvpMatrix, 0, mirror)
			}
		}
	}

	override fun getMirror(): Int {
		return mMirror
	}

	/**
	 * フォーカス強度分布の表示領域を設定する
	 * [minU,minV]-[maxU,maxVにフォーカス強度分布を表示する]
	 * ぞれぞれの値は[0.0..1.0]
	 * @param minU フォーカス強度分布表示矩形頂点1(テクスチャ座標)u
	 * @param minV フォーカス強度分布表示矩形頂点1(テクスチャ座標)v
	 * @param maxU フォーカス強度分布表示矩形頂点2(テクスチャ座標)u
	 * @param maxV フォーカス強度分布表示矩形頂点2(テクスチャ座標)v
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
		private const val DEBUG = true	// set false on production
		private val TAG = GLFocus::class.java.simpleName

		/**
		 * バッファオブジェクトを使って描画するかどうか
		 */
		private const val USE_VBO = true

		/**
		 * 元映像のテクスチャとフォーカス強度分布を合成して表示するフラグメントシェーダー
		 */
		private const val FRAGMENT_SHADER_MIX_SSBO_ES31 =
			"""
			#version 310 es
			#define STEP (${GLFocusBase.FOCUS_HISTOGRAM_NUM-1}.0)
			precision highp float;
			const float bkColor = 0.2;
			in vec2 vTextureCoord;
			uniform sampler2D sTexture;
			layout(std430, binding = 2) buffer Focus {
				uint counts[${GLFocusBase.FOCUS_COUNT_NUM}];
			};
			// Format: vec4(minU, minV, maxU, maxV) in normalized UV space (0.0 to 1.0)
			uniform vec4 uEmbedRegion;
			layout(location = 0) out vec4 o_FragColor;
			const float EPS = 0.01;
			const float SCALE = 0.90;
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
					// フォーカス強度を取得
					float countsFocus = float(counts[index]);
					// 最大値を取得して正規化
					float max = float(counts[${GLFocusBase.FOCUS_MAX_INDEX}u]) * SCALE;
					float counts = countsFocus / max;
					float countsL = counts - EPS;
					vec3 histogram = vec3(0.0);
					float histogramY = 1.0 - uvForTexture2.y;
					if ((histogramY > countsL) && (histogramY < counts)) {
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
	}
}