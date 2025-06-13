package com.serenegiant.gl;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2025 saki t_saki@serenegiant.com
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

import android.opengl.GLES31;
import android.opengl.Matrix;
import android.os.Build;
import android.util.Log;

import com.serenegiant.glutils.IMirror;
import com.serenegiant.graphics.MatrixUtils;
import com.serenegiant.system.Time;
import com.serenegiant.utils.BufferHelper;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import androidx.annotation.AnyThread;
import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.Size;
import androidx.annotation.WorkerThread;

import static com.serenegiant.gl.GLConst.GL_NO_BUFFER;
import static com.serenegiant.gl.GLConst.GL_TEXTURE_EXTERNAL_OES;
import static com.serenegiant.gl.GLConst.TexTarget;
import static com.serenegiant.gl.GLConst.TexUnit;
import static com.serenegiant.gl.ShaderConst.DEFAULT_TEXCOORD_2D;
import static com.serenegiant.gl.ShaderConst.DEFAULT_VERTICES_2D;
import static com.serenegiant.gl.ShaderConst.VERTEX_SHADER_ES31;
import static com.serenegiant.utils.BufferHelper.SIZEOF_INT_BYTES;

/**
 * RGBヒストグラム作成のヘルパークラス
 * OpenGL|ES3.1以降が必要
 * XXX 公式にはAPI>=21でES3.1対応だけど一部端末でES3の機能が抜けている場合があるのでAPI>=24とする
 */
@RequiresApi(api = Build.VERSION_CODES.N)
public class GLHistogram implements IMirror {
	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = GLHistogram.class.getSimpleName();

	/**
	 * バッファオブジェクトを使って描画するかどうか
	 */
	private static final boolean USE_VBO = true;
	/**
	 * コンピュートシェーダーを使ってヒストグラムをカウントするかどうか
	 * XXX コンピュートシェーダーを使ってもフラグメントシェーダーを使ってもかなり処理が重いので
	 *     ヒストグラムの更新頻度を下げても結構カクツク
	 *     全テクセルをカウントせずに飛び飛びにカウントするように実装した方がいいかも
	 */
	private static final boolean USB_COMPUTE_SHADER = true;

	/**
	 * RGBヒストグラムのカウント用フラグメントシェーダー
	 * Geminiのレスポンスから作成
	 */
	private static final String FRAGMENT_SHADER_HISTOGRAM_CNT_SSBO_ES31 =
		"""
		#version 310 es
		#extension GL_ANDROID_extension_pack_es31a : require
		precision highp float;
		precision highp int;
		precision highp uimage2D;
		precision highp image2D;
		
		in vec2 vTextureCoord;
		uniform sampler2D sTexture;
		layout(std430, binding = 1) buffer Histogram {
			uint counts[256 * 5];
		};
		const highp vec3 conv = vec3(0.2125, 0.7154, 0.0721);
		
		void main() {
			vec4 color = texture(sTexture, vTextureCoord);
		
			// Assuming color values are in the range [0.0, 1.0]
			// Convert to integer intensity [0, 255]
			uint indexR = uint(color.r * 255.0);
			uint indexG = uint(color.g * 255.0);
			uint indexB = uint(color.b * 255.0);
			uint indexI = uint(dot(color.rgb, conv) * 255.0);
		
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
		""";

	/**
	 * RGBヒストグラムをカウントするためのコンピュートシェーダー
	 */
	private static final String COMPUTE_SHADER_HISTOGRAM_COMPUTE_ES31 =
		"""
		#version 310 es
		#extension GL_ANDROID_extension_pack_es31a : require
		precision highp float;
		precision highp int;
		precision highp uimage2D;
		precision highp image2D;

		layout (local_size_x = 16, local_size_y = 16) in;
		
//		layout(binding = 0, rgba8) uniform readonly image2D srcImage;
		layout(binding = 0) uniform sampler2D srcImage;
		layout(std430, binding = 1) buffer Histogram {
			uint counts[256 * 5];
		};
		uniform vec2 uROI[2];
		uniform mat4 uTexMatrix;
		const highp vec3 conv = vec3(0.2125, 0.7154, 0.0721);
		
		void main() {
			vec4 pos = vec4(vec2(gl_GlobalInvocationID.xy), 0.0, 1.0);
			vec2 uv = (uTexMatrix * pos).xy;
			if ((uv.x < uROI[0].x) || (uv.x >= uROI[1].x) || (uv.y < uROI[0].y) || (uv.y >= uROI[1].y)) return;
			vec4 color = texture(srcImage, uv / uROI[1]);
		
			// Assuming color values are in the range [0.0, 1.0]
			// Convert to integer intensity [0, 255]
			uint indexR = uint(color.r * 255.0);
			uint indexG = uint(color.g * 255.0);
			uint indexB = uint(color.b * 255.0);
			uint indexI = uint(dot(color.rgb, conv) * 255.0);
		
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
		""";

	/**
	 * 元映像にRGBヒストグラムを合成して描画するフラグメントシェーダー
	 * これは今は使っていない
	 */
	private static final String FRAGMENT_SHADER_HISTOGRAM_DRAW_SSBO_ES31 =
		"""
		#version 310 es
		#extension GL_ANDROID_extension_pack_es31a : require
		precision highp float;
		precision highp int;
		precision highp uimage2D;
		precision highp image2D;

		in vec2 vTextureCoord;
		uniform sampler2D sTexture;
		layout(std430, binding = 1) buffer Histogram {
			uint counts[256 * 5];
		};
		layout(location = 0) out vec4 o_FragColor;
		const float EPS = 0.01;
		const float SCALE = 0.5;
		void main() {
			vec4 color = texture(sTexture, vTextureCoord);
			uint index = uint(vTextureCoord.x * 255.0);	// 0..255
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
		""";

	/**
	 * RGBヒストグラムのみを全面に描画するフラグメントシェーダー
	 */
	private static final String FRAGMENT_SHADER_HISTOGRAM_DRAW_ONLY_SSBO_ES31 =
		"""
		#version 310 es
		#extension GL_ANDROID_extension_pack_es31a : require
		precision highp float;
		precision highp int;
		precision highp uimage2D;
		precision highp image2D;

		in vec2 vTextureCoord;
		uniform sampler2D sTexture;
		layout(std430, binding = 1) buffer Histogram {
			uint counts[256 * 5];
		};
		layout(location = 0) out vec4 o_FragColor;
		const float EPS = 0.01;
		const float SCALE = 0.75;
		void main() {
//			vec4 color = texture(sTexture, vTextureCoord);
			uint index = uint(vTextureCoord.x * 255.0);	// 0..255
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
		""";

	/**
	 * 元映像のテクスチャと別途オフスクリーン描画したヒストグラムテクスチャを合成して表示するフラグメントシェーダー
	 */
	private static final String FRAGMENT_SHADER_MIX_ES31 =
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
		""";

	/**
	 * RGBヒストグラムのカウント・描画用
	 */
	private static class HistogramDrawer {
		/**
		 * テクスチャターゲット
		 * GL_TEXTURE_EXTERNAL_OESかGL_TEXTURE_2D
		 */
		@TexTarget
		public final int texTarget;
		private final int histogramRGBId;
		/**
		 * 頂点座標用バッファオブジェクト名
		 */
		private int mBufVertex = GL_NO_BUFFER;
		/**
		 * テクスチャ座標用バッファオブジェクト名
		 */
		private int mBufTexCoord = GL_NO_BUFFER;
		/**
		 * 頂点の数
		 */
		private final int VERTEX_NUM;
		/**
		 * 頂点配列のサイズ
		 */
		private final int VERTEX_SZ;
		/**
		 * 頂点座標
		 */
		private final FloatBuffer pVertex;
		/**
		 * テクスチャ座標
		 */
		private final FloatBuffer pTexCoord;
		private final int hProgram;
		@Size(value = 16)
		@NonNull
		private final float[] mMvpMatrix = new float[16];
		private boolean mRelease;
		/**
		 * 頂点座標のlocation
		 */
		private final int maPositionLoc;
		/**
		 * テクスチャ座標のlocation
		 */
		private final int maTextureCoordLoc;
		/**
		 * モデルビュー変換行列のlocation
		 */
		private final int muMVPMatrixLoc;
		/**
		 * テクスチャ座標変換行列のlocation
		 */
		private final int muTexMatrixLoc;
		/**
		 * 使用するテクスチャユニットのlocation
		 */
		protected final int muTextureLoc;
		/**
		 * ヒストグラムを受け取るテクスチャRGBのlocation
		 */
		protected final int muHistogramRGBLoc;

		/**
		 * コンストラクタ
		 * @param isOES
		 * @param fss
		 */
		public HistogramDrawer(final boolean isOES, final int histogramRGBId, @NonNull final String fss) {
			texTarget = isOES ? GL_TEXTURE_EXTERNAL_OES : GLES31.GL_TEXTURE_2D;
			this.histogramRGBId = histogramRGBId;
			VERTEX_NUM = Math.min(DEFAULT_VERTICES_2D.length, DEFAULT_TEXCOORD_2D.length) / 2;
			VERTEX_SZ = VERTEX_NUM * 2;
			pVertex = BufferHelper.createBuffer(DEFAULT_VERTICES_2D);
			pTexCoord = BufferHelper.createBuffer(DEFAULT_TEXCOORD_2D);
			if (DEBUG) Log.v(TAG, "コンストラクタ:create shader");
			hProgram = GLUtils.loadShader(VERTEX_SHADER_ES31, fss);
			GLES31.glUseProgram(hProgram);
			// locationの取得処理
			maPositionLoc = GLES31.glGetAttribLocation(hProgram, "aPosition");
			GLUtils.checkGlError("glGetAttribLocation(aPosition)");
			maTextureCoordLoc = GLES31.glGetAttribLocation(hProgram, "aTextureCoord");
			GLUtils.checkGlError("glGetAttribLocation(aTextureCoord)");
			muMVPMatrixLoc = GLES31.glGetUniformLocation(hProgram, "uMVPMatrix");
			GLUtils.checkGlError("glGetUniformLocation(uMVPMatrix)");
			muTexMatrixLoc = GLES31.glGetUniformLocation(hProgram, "uTexMatrix");
			GLUtils.checkGlError("glGetUniformLocation(uTexMatrix)");
			muTextureLoc = GLES31.glGetUniformLocation(hProgram, "sTexture");
			GLUtils.checkGlError("glGetAttribLocation(sTexture)");
			muHistogramRGBLoc = GLES31.glGetUniformLocation(hProgram, "uHistogramRGB");
			GLUtils.checkGlError("glGetUniformLocation(uHistogramRGB)");
			if (DEBUG) Log.v(TAG, "コンストラクタ:aPosition=" + maPositionLoc
				+ ",aTextureCoord=" + maTextureCoordLoc
				+ ",uMVPMatrix=" + muMVPMatrixLoc
				+ ",uTexMatrix=" + muTexMatrixLoc
				+ ",sTexture=" + muTextureLoc
				+ ",uHistogramRGB=" + muHistogramRGBLoc
			);
			// テクスチャ変換行列とモデルビュー変換行列の初期化処理
			Matrix.setIdentityM(mMvpMatrix, 0);
			GLES31.glUniformMatrix4fv(muMVPMatrixLoc,
				1, false, mMvpMatrix, 0);
			GLUtils.checkGlError("glUniformMatrix4fv(muMVPMatrixLoc)");
			GLES31.glUniformMatrix4fv(muTexMatrixLoc,
				1, false, mMvpMatrix, 0);
			GLUtils.checkGlError("glUniformMatrix4fv(muTexMatrixLoc)");
			// テクスチャ座標と頂点座標の初期化処理
			updateVertices();
		}

		/**
		 * ヒストグラムのカウント実行
		 * EGL|GLコンテキストの存在するスレッド上で実行すること
		 */
		public void draw(
			@TexUnit final int texUnit, final int texId,
			@Nullable @Size(min=16) final float[] texMatrix, final int texOffset) {

//			if (DEBUG) Log.v(TAG, "compute:");
			// 描画準備
			GLES31.glUseProgram(hProgram);
			if (texMatrix != null) {
				// テクスチャ変換行列が指定されている時
				GLES31.glUniformMatrix4fv(muTexMatrixLoc, 1, false, texMatrix, texOffset);
			}
			if (muMVPMatrixLoc >= 0) {
				GLES31.glUniformMatrix4fv(muMVPMatrixLoc, 1, false, mMvpMatrix, 0);
			}
			// ヒストグラムデータ用のテクスチャ/バッファをバインド
			GLES31.glBindBufferBase(GLES31.GL_SHADER_STORAGE_BUFFER, 1, histogramRGBId);
			// 映像ソースのテクスチャをバインド
			GLES31.glActiveTexture(texUnit);
			if (DEBUG) GLUtils.checkGlError("bindTexture:glActiveTexture,texUnit=" + texUnit + ",loc=" + muTextureLoc);
			GLES31.glBindTexture(texTarget, texId);
			if (DEBUG) GLUtils.checkGlError("bindTexture:glBindTexture,texUnit=" + texUnit + ",loc=" + muTextureLoc);
			GLES31.glUniform1i(muTextureLoc, GLUtils.gLTextureUnit2Index(texUnit));
			if (DEBUG) GLUtils.checkGlError("bindTexture:glUniform1i,texUnit=" + texUnit + ",loc=" + muTextureLoc);
			// 描画実行
			GLES31.glDrawArrays(GLES31.GL_TRIANGLE_STRIP, 0, VERTEX_NUM);
			// 描画終了処理
			GLES31.glBindTexture(texTarget, 0);
			GLES31.glUseProgram(0);
		}

		/**
		 * 頂点座標をセット
		 */
		private void updateVertices() {
			if (DEBUG) Log.v(TAG, "updateVertices:");
			if (USE_VBO) {
				if (mBufVertex <= GL_NO_BUFFER) {
					pVertex.clear();
					mBufVertex = GLUtils.createBuffer(GLES31.GL_ARRAY_BUFFER, pVertex, GLES31.GL_STATIC_DRAW);
					if (DEBUG) Log.v(TAG, "updateVertices:create buffer object for vertex," + mBufVertex);
				}
				if (mBufTexCoord <= GL_NO_BUFFER) {
					pTexCoord.clear();
					mBufTexCoord = GLUtils.createBuffer(GLES31.GL_ARRAY_BUFFER, pTexCoord, GLES31.GL_STATIC_DRAW);
					if (DEBUG) Log.v(TAG, "updateVertices:create buffer object for tex coord," + mBufTexCoord);
				}
				// 頂点座標をセット
				GLES31.glBindBuffer(GLES31.GL_ARRAY_BUFFER, mBufVertex);
				GLES31.glVertexAttribPointer(maPositionLoc,
					2, GLES31.GL_FLOAT, false, 0, 0);
				GLES31.glEnableVertexAttribArray(maPositionLoc);
				// テクスチャ座標をセット
				GLES31.glBindBuffer(GLES31.GL_ARRAY_BUFFER, mBufTexCoord);
				GLES31.glVertexAttribPointer(maTextureCoordLoc,
					2, GLES31.GL_FLOAT, false, 0, 0);
				GLES31.glEnableVertexAttribArray(maTextureCoordLoc);
			} else {
				// 頂点座標をセット
				pVertex.clear();
				GLES31.glVertexAttribPointer(maPositionLoc,
					2, GLES31.GL_FLOAT, false, VERTEX_SZ, pVertex);
				GLES31.glEnableVertexAttribArray(maPositionLoc);
				// テクスチャ座標をセット
				pTexCoord.clear();
				GLES31.glVertexAttribPointer(maTextureCoordLoc,
					2, GLES31.GL_FLOAT, false, VERTEX_SZ, pTexCoord);
				GLES31.glEnableVertexAttribArray(maTextureCoordLoc);
			}
		}

		/**
		 * 関係するリソースを破棄
		 * EGL|GLコンテキストの存在するスレッド上で実行すること
		 */
		public void release() {
			if (!mRelease) {
				mRelease = true;
				if (DEBUG) Log.v(TAG, "release:");
				if (mBufVertex > GL_NO_BUFFER) {
					GLUtils.deleteBuffer(mBufVertex);
					mBufVertex = GL_NO_BUFFER;
				}
				if (mBufTexCoord > GL_NO_BUFFER) {
					GLUtils.deleteBuffer(mBufTexCoord);
					mBufTexCoord = GL_NO_BUFFER;
				}
				GLES31.glDeleteProgram(hProgram);
			}
		}
	} // HistogramDrawer

	public final boolean isOES;
	private final int texTarget;
	private long mNextDraw;
	private final long mIntervalsNs;
	private final long mIntervalsDeltaNs;
	@Nullable
	private final HistogramDrawer mComputeDrawer;
	@NonNull
	private final HistogramDrawer mHistogramDrawer;
	@NonNull
	private final GLDrawer2D mRendererDrawer;
	private final int mComputeProgram;
	private final int muROILoc;
	private final int muTexMatrixLoc;
	private final int muHistogramTexLoc;
	private final int muEmbedRegionLoc;
	@Size(value=4)
	@NonNull
	private final float[] mROI = new float[4];
	@Size(value=4)
	@NonNull
	private final float[] mHistogramRegion = {
		0.1f, 0.75f,	// minU, minV,
		0.6f, 0.9f,	// maxU, maxV,
	};
	/**
	 * ヒストグラム受け取り用のテクスチャをゼロクリアするために使うIntBuffer
	 */
	@NonNull
	private final IntBuffer mClearBuffer = BufferHelper.createBuffer(new int[256 * 5]);
	/**
	 * ヒストグラムを受け取るシェーダーストレージバッファオブジェクトID
	 */
	private int mHistogramRGBId = GL_NO_BUFFER;
	@MirrorMode
	private int mMirror = IMirror.MIRROR_NORMAL;
	@NonNull
	private final GLSurface mHistogramOffscreen;

	/**
	 * コンストラクタ
	 * ヒストグラムの歳代更新頻度は2fps
	 * @param isOES
	 */
	@WorkerThread
	public GLHistogram(final boolean isOES) {
		this(isOES, 2.0f);
	}
	/**
	 * コンストラクタ
	 * EGL|GLコンテキストの存在するスレッド上で実行すること
	 * @param isOES
	 * @param maxFps ヒストグラムの最大更新頻度、0以下なら2fps
	 */
	@WorkerThread
	public GLHistogram(final boolean isOES, final @FloatRange(from=0.0) float maxFps) {
		if (DEBUG) Log.v(TAG, "コンストラクタ:isOES=" + isOES + ",useComputeShader=" + USB_COMPUTE_SHADER + ",maxFps=" + maxFps);
		this.isOES = isOES;
		texTarget = isOES ? GL_TEXTURE_EXTERNAL_OES : GLES31.GL_TEXTURE_2D;
		mIntervalsNs = Math.round(1000000000.0 / (maxFps > 0.0f ? maxFps : 2.0f));
		mIntervalsDeltaNs = -Math.round(mIntervalsNs * 0.03);	// 3%ならショートしても良いことにする
		mNextDraw = Time.nanoTime() + mIntervalsNs;
		if (DEBUG) Log.v(TAG, "コンストラクタ:ヒストグラム受け取り用のシェーダーストレージバッファ初期化処理");
		mHistogramRGBId = initHistogramBuffer();
		mHistogramOffscreen = GLSurface.newInstance(true, GLES31.GL_TEXTURE3, 512, 512);
		if (USB_COMPUTE_SHADER) {
			mComputeDrawer = null;
			if (DEBUG) Log.v(TAG, "コンストラクタ:create compute shader");
			mComputeProgram = ComputeUtils.loadShader(COMPUTE_SHADER_HISTOGRAM_COMPUTE_ES31);
			if (DEBUG) Log.v(TAG, "コンストラクタ:mComputeProgram=" + mComputeProgram);
			muROILoc = GLES31.glGetUniformLocation(mComputeProgram, "uROI");
			GLUtils.checkGlError("コンストラクタ:glGetUniformLocation(uROI)");
			muTexMatrixLoc = GLES31.glGetUniformLocation(mComputeProgram, "uTexMatrix");
			GLUtils.checkGlError("コンストラクタ:glGetUniformLocation(uTexMatrix)");
			if (DEBUG) Log.v(TAG, "コンストラクタ:muROILoc=" + muROILoc + ",muTexMatrixLoc=" + muTexMatrixLoc);
		} else {
			mComputeProgram = -1;
			muROILoc = -1;
			muTexMatrixLoc = -1;
			mComputeDrawer = new HistogramDrawer(isOES, mHistogramRGBId,
				FRAGMENT_SHADER_HISTOGRAM_CNT_SSBO_ES31);
		}
		if (DEBUG) Log.v(TAG, "コンストラクタ:create mHistogramDrawer,isOES=" + isOES);
		mHistogramDrawer = new HistogramDrawer(isOES, mHistogramRGBId,
			FRAGMENT_SHADER_HISTOGRAM_DRAW_ONLY_SSBO_ES31);
		if (DEBUG) Log.v(TAG, "コンストラクタ:create mRendererDrawer,isOES=" + isOES);
		mRendererDrawer = GLDrawer2D.create(true, isOES, FRAGMENT_SHADER_MIX_ES31);
		muHistogramTexLoc = mRendererDrawer.glGetUniformLocation("sTexture2");
		muEmbedRegionLoc = mRendererDrawer.glGetUniformLocation("uEmbedRegion");
		GLUtils.checkGlError("コンストラクタ:glGetUniformLocation(sTexture2)");
		if (!isOES) {
			setMirror(MIRROR_VERTICAL);
		}
	}

	/**
	 * ヒストグラムを計算
	 * @param width
	 * @param height
	 * @param texUnit
	 * @param texId
	 * @param texMatrix
	 * @param texOffset
	 */
	@WorkerThread
	public void compute(
		final int width, final int height,
		@TexUnit final int texUnit, final int texId,
		@Nullable @Size(min=16) final float[] texMatrix, final int texOffset) {

		final long startTimeNs = System.nanoTime();
		if (Time.nanoTime() - mNextDraw > mIntervalsDeltaNs) {
			mNextDraw = Time.nanoTime() + mIntervalsNs;
			clearHistogramBuffer(mHistogramRGBId);
			if (USB_COMPUTE_SHADER) {
				GLES31.glUseProgram(mComputeProgram);
				// ROIをセット、今はテクスチャ全面をカウント対象とする, (0,0)-(width,height)
				mROI[0] = 0; mROI[1] = 0;
				mROI[2] = width; mROI[3] = height;
				GLES31.glUniform2fv(muROILoc, 2, mROI, 0);
				// テクスチャ変換行列をバインド
				GLES31.glUniformMatrix4fv(muTexMatrixLoc, 1, false, texMatrix, texOffset);
				// ヒストグラム用バッファをバインド
				GLES31.glBindBufferBase(GLES31.GL_SHADER_STORAGE_BUFFER, 1, mHistogramRGBId);
				// ソース映像のテクスチャをバインド
				GLES31.glActiveTexture(GLES31.GL_TEXTURE0);
				if (DEBUG) GLUtils.checkGlError("draw:glActiveTexture,texUnit=" + texUnit);
				GLES31.glBindTexture(texTarget, texId);
				if (DEBUG) GLUtils.checkGlError("draw:glBindTexture,texUnit=" + texUnit);
//				GLES31.glBindImageTexture(0, texId, 0, false, 0, GLES31.GL_READ_ONLY, GLES31.GL_RGBA8);
//				if (DEBUG) GLUtils.checkGlError("draw:glBindImageTexture");
				// コンピュート実行
				GLES31.glDispatchCompute((width + 15) / 16, (height + 15) / 16, 1);
				if (DEBUG) GLUtils.checkGlError("draw:glDispatchCompute");
				GLES31.glMemoryBarrier(GLES31.GL_SHADER_STORAGE_BARRIER_BIT | GLES31.GL_BUFFER_UPDATE_BARRIER_BIT);
			} else {
				mComputeDrawer.draw(texUnit, texId, texMatrix, texOffset);
			}
			mHistogramOffscreen.makeCurrent();
			mHistogramDrawer.draw(texUnit, texId, texMatrix, texOffset);
			mHistogramOffscreen.swap();
		}
//		drawTimeNs += System.nanoTime() - startTimeNs;
//		if (DEBUG && (++cnt %100 == 0)) {
//			Log.i(TAG, "draw time=" + drawTimeNs / 1000000f / cnt + "ms,fps=" + (cnt / (drawTimeNs / 1000000000f)));
//		}
	}

//	private long cnt = 0;
//	private long drawTimeNs = 0;
	/**
	 * ヒストグラムのカウントと描画を実行
	 * EGL|GLコンテキストの存在するスレッド上で実行すること
	 * コンピュートシェーダー:
	 * 1280x720で0.4ms弱@SM-M26かかかる
	 * 1920x1080で1.1ms弱@SM-M26かかかる
	 * 通常のシェーダー:
	 * 1280x720で0.36ms弱@SM-M26かかかる
	 * 1920x1080で1.06ms弱@SM-M26かかかる
	 * @param width
	 * @param height
	 * @param texUnit
	 * @param texId
	 * @param texMatrix
	 * @param texOffset
	 */
	@WorkerThread
	public void draw(
		final int width, final int height,
		@TexUnit final int texUnit, final int texId,
		@Nullable @Size(min=16) final float[] texMatrix, final int texOffset) {

		mRendererDrawer.glUseProgram();
		// ヒストグラムテクスチャをバインド
		GLES31.glActiveTexture(GLES31.GL_TEXTURE3);
		if (DEBUG) GLUtils.checkGlError("draw:glActiveTexture,texUnit=" + texUnit + ",loc=" + muHistogramTexLoc);
		GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, mHistogramOffscreen.getTexId());
		if (DEBUG) GLUtils.checkGlError("draw:glBindTexture,texUnit=" + texUnit + ",loc=" + muHistogramTexLoc);
		GLES31.glUniform1i(muHistogramTexLoc, 3);
		if (DEBUG) GLUtils.checkGlError("draw:glUniform1i,texUnit=" + texUnit + ",loc=" + muHistogramTexLoc);
		synchronized (mHistogramRegion) {
			GLES31.glUniform4fv(muEmbedRegionLoc, 1, mHistogramRegion, 0);
		}
		if (DEBUG) GLUtils.checkGlError("draw:glUniform4fv,loc=" + muEmbedRegionLoc);
		mRendererDrawer.draw(texUnit, texId, texMatrix, texOffset);
	}

	/**
	 * 関係するリソースを破棄
	 * EGL|GLコンテキストの存在するスレッド上で実行すること
	 */
	@WorkerThread
	public void release() {
		if (DEBUG) Log.v(TAG, "release:");
		if (mComputeDrawer != null) {
			mComputeDrawer.release();
		}
		if (mComputeProgram >= 0) {
			GLES31.glDeleteProgram(mComputeProgram);
		}
		mRendererDrawer.release();
		mHistogramDrawer.release();
		mHistogramOffscreen.release();
		if (mHistogramRGBId > GL_NO_BUFFER) {
			GLUtils.deleteBuffer(mHistogramRGBId);
			mHistogramRGBId = GL_NO_BUFFER;
		}
	}

	@Override
	public void setMirror(final int mirror) {
		synchronized (this) {
			if (mMirror != mirror) {
				mMirror = mirror;
				MatrixUtils.setMirror(mHistogramDrawer.mMvpMatrix, mirror);
				mRendererDrawer.setMirror(mirror);
			}
		}
	}

	@Override
	public int getMirror() {
		return mMirror;
	}

	/**
	 * ヒストグラムの表示領域を設定する
	 * [minU,minV]-[maxU,maxVにヒストグラムを表示する]
	 * ぞれぞれの値は[0.0..1.0]
	 * @param minU
	 * @param minV
	 * @param maxU
	 * @param maxV
	 */
	@AnyThread
	public void setHistogram(
		final float minU, final float minV,
		final float maxU, final float maxV) {
		synchronized (mHistogramRegion) {
			mHistogramRegion[0] = minU;
			mHistogramRegion[1] = minV;
			mHistogramRegion[2] = maxU;
			mHistogramRegion[3] = maxV;
		}
	}

	private void resetClearBuffer() {
		mClearBuffer.clear();
		mClearBuffer.position(mClearBuffer.capacity());
		mClearBuffer.flip();
	}

	/**
	 * ヒストグラム受け取り用のシェーダーストレージバッファオブジェクトを生成
	 * @return
	 */
	private int initHistogramBuffer() {
		if (DEBUG) Log.v(TAG, "initHistogramBuffer:");
		// バッファオブジェクトを生成
		int[] histogramBuffer = new int[1];
		GLES31.glGenBuffers(1, histogramBuffer, 0);
		GLUtils.checkGlError("initHistogramBuffer:glGenBuffers");
		// 操作するバッファを指定
		GLES31.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, histogramBuffer[0]);
		GLUtils.checkGlError("initHistogramBuffer:glBindBuffer");
		resetClearBuffer();
		GLES31.glBufferData(GLES31.GL_SHADER_STORAGE_BUFFER,
			SIZEOF_INT_BYTES * mClearBuffer.capacity(),	// sizeはバイト数なので注意
			mClearBuffer,
			GLES31.GL_DYNAMIC_COPY);
		GLUtils.checkGlError("initHistogramBuffer:glBufferData");
		// バッファの指定をクリア
		GLES31.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, 0);
		GLUtils.checkGlError("initHistogramBuffer:glBindBuffer(0)");
		return histogramBuffer[0];
	}

	/**
	 * ヒストグラム受け取り用のシェーダーストレージバッファオブジェクトをクリアする
	 * @return
	 */
	private void clearHistogramBuffer(final int bufferId) {
		// 操作するバッファを指定
		// 以降バッファIDとして0を指定するまではGL_SHADER_STORAGE_BUFFERを
		// 指定したバッファの操作は全てこのbufferIDで示すバッファに対して行われる
		GLES31.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, bufferId);
		if (DEBUG) GLUtils.checkGlError("clearAndBindHistogramBuffer:glBindBuffer(" + bufferId + ")");
		resetClearBuffer();
		GLES31.glBufferSubData(GLES31.GL_SHADER_STORAGE_BUFFER,
			0, SIZEOF_INT_BYTES * mClearBuffer.capacity(),	// sizeはバイト数なので注意
			mClearBuffer);
		if (DEBUG) GLUtils.checkGlError("clearAndBindHistogramBuffer:glBufferData");
		// バッファの指定をクリア
		GLES31.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, 0);
		if (DEBUG) GLUtils.checkGlError("clearAndBindHistogramBuffer:glBindBuffer(0)");
	}

}
