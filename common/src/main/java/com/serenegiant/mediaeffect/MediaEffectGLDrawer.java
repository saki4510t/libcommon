package com.serenegiant.mediaeffect;
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

import android.opengl.GLES20;
import android.opengl.Matrix;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;

import com.serenegiant.gl.GLUtils;
import com.serenegiant.glutils.IMirror;
import com.serenegiant.graphics.MatrixUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Locale;

import static com.serenegiant.gl.ShaderConst.*;

public class MediaEffectGLDrawer implements IMirror {

	protected boolean mEnabled = true;

	private static final int FLOAT_SZ = Float.SIZE / 8;
	private static final int VERTEX_NUM = 4;
	private static final int VERTEX_SZ = VERTEX_NUM * 2;

	public static MediaEffectGLDrawer newInstance() {
		return new MediaEffectSingleDrawer(false, VERTEX_SHADER_ES2, FRAGMENT_SHADER_ES2);
	}

	public static MediaEffectGLDrawer newInstance(final int numTex) {
		if (numTex <= 1) {
			return new MediaEffectSingleDrawer(false, VERTEX_SHADER_ES2, FRAGMENT_SHADER_ES2);
		} else {
			return new MediaEffectGLDrawer(numTex, false, VERTEX_SHADER_ES2, FRAGMENT_SHADER_ES2);
		}
	}

	public static MediaEffectGLDrawer newInstance(final String fss) {
		return new MediaEffectSingleDrawer(false, VERTEX_SHADER_ES2, fss);
	}

	public static MediaEffectGLDrawer newInstance(final int numTex, final String fss) {
		if (numTex <= 1) {
			return new MediaEffectSingleDrawer(false, VERTEX_SHADER_ES2, fss);
		} else {
			return new MediaEffectGLDrawer(numTex, false, VERTEX_SHADER_ES2, fss);
		}
	}

	public static MediaEffectGLDrawer newInstance(
		final boolean isOES, final String fss) {
		return new MediaEffectSingleDrawer(isOES, VERTEX_SHADER_ES2, fss);
	}

	public static MediaEffectGLDrawer newInstance(
		final int numTex, final boolean isOES, final String fss) {

		if (numTex <= 1) {
			return new MediaEffectSingleDrawer(isOES, VERTEX_SHADER_ES2, fss);
		} else {
			return new MediaEffectGLDrawer(numTex, isOES, VERTEX_SHADER_ES2, fss);
		}
	}

	public static MediaEffectGLDrawer newInstance(
		final boolean isOES, final String vss, final String fss) {

		return new MediaEffectSingleDrawer(isOES, VERTEX_SHADER_ES2, fss);
	}
	
	public static MediaEffectGLDrawer newInstance(
		final int numTex, final boolean isOES, final String vss, final String fss) {

		if (numTex <= 1) {
			return new MediaEffectSingleDrawer(isOES, vss, fss);
		} else {
			return new MediaEffectGLDrawer(numTex, isOES, vss, fss);
		}
	}

	/**
	 * テクスチャを1枚しか使わない場合はこちらを使うこと
	 */
	protected static class MediaEffectSingleDrawer extends MediaEffectGLDrawer {
		protected MediaEffectSingleDrawer(
			final boolean isOES, final String vss, final String fss) {
			super(1, isOES, vss, fss);
		}

		/**
		 * テクスチャのバインド処理
		 * mSyncはロックされて呼び出される
		 * @param texIds texture ID
		 */
		protected void bindTexture(@NonNull final int[] texIds) {
			GLES20.glActiveTexture(TEX_NUMBERS[0]);
			if (texIds[0] != GL_NO_TEXTURE) {
				GLES20.glBindTexture(mTexTarget, texIds[0]);
				GLES20.glUniform1i(muTexLoc[0], 0);
			}
		}
	
		/**
		 * 描画後の後処理, テクスチャのunbind, プログラムをデフォルトに戻す
		 * mSyncはロックされて呼び出される
		 */
		protected void unbindTexture() {
			GLES20.glActiveTexture(TEX_NUMBERS[0]);
			GLES20.glBindTexture(mTexTarget, 0);
		}
	}

	@NonNull
	protected final Object mSync = new Object();
	@TexTarget
	protected final int mTexTarget;
	protected final int muMVPMatrixLoc;
	protected final int muTexMatrixLoc;
	protected final int[] muTexLoc;
	@Size(value=16)
	@NonNull
	protected final float[] mMvpMatrix = new float[16];
	protected int hProgram;
	@MirrorMode
	private int mMirror = IMirror.MIRROR_NORMAL;

	protected MediaEffectGLDrawer() {
		this(1, false, VERTEX_SHADER_ES2, FRAGMENT_SHADER_ES2);
	}

	protected MediaEffectGLDrawer(final int numTex) {
		this(numTex, false, VERTEX_SHADER_ES2, FRAGMENT_SHADER_ES2);
	}

	protected MediaEffectGLDrawer(final String fss) {
		this(1, false, VERTEX_SHADER_ES2, fss);
	}

	protected MediaEffectGLDrawer(final int numTex, final String fss) {
		this(numTex, false, VERTEX_SHADER_ES2, fss);
	}

	protected MediaEffectGLDrawer(final boolean isOES, final String fss) {
		this(1, isOES, VERTEX_SHADER_ES2, fss);
	}

	protected MediaEffectGLDrawer(
		final int numTex, final boolean isOES, final String fss) {
		this(numTex, isOES, VERTEX_SHADER_ES2, fss);
	}

	protected MediaEffectGLDrawer(
		final boolean isOES, final String vss, final String fss) {
		this(1, isOES, VERTEX_SHADER_ES2, fss);
	}
	
	protected MediaEffectGLDrawer(
		final int numTex,
		final boolean isOES, final String vss, final String fss) {

		mTexTarget = isOES ? GL_TEXTURE_EXTERNAL_OES : GL_TEXTURE_2D;
		final FloatBuffer pVertex = ByteBuffer.allocateDirect(VERTEX_SZ * FLOAT_SZ)
			.order(ByteOrder.nativeOrder()).asFloatBuffer();
		pVertex.put(DEFAULT_VERTICES_2D);
		pVertex.flip();
		final FloatBuffer pTexCoord = ByteBuffer.allocateDirect(VERTEX_SZ * FLOAT_SZ)
			.order(ByteOrder.nativeOrder()).asFloatBuffer();
		pTexCoord.put(DEFAULT_TEXCOORD_2D);
		pTexCoord.flip();

		// テクスチャ用のロケーションは最低でも1つは確保する
		muTexLoc = new int[numTex > 0 ? numTex : 1];
		hProgram = GLUtils.loadShader(vss, fss);
		GLES20.glUseProgram(hProgram);
		final int maPositionLoc = GLES20.glGetAttribLocation(hProgram, "aPosition");
		final int maTextureCoordLoc = GLES20.glGetAttribLocation(hProgram, "aTextureCoord");
        muMVPMatrixLoc = GLES20.glGetUniformLocation(hProgram, "uMVPMatrix");
        muTexMatrixLoc = GLES20.glGetUniformLocation(hProgram, "uTexMatrix");
        muTexLoc[0] = GLES20.glGetUniformLocation(hProgram, "sTexture");
        for (int i = 1; i < numTex; i++) {
			muTexLoc[i] = GLES20.glGetUniformLocation(hProgram,
				String.format(Locale.US, "sTexture%d", i + 1));
		}
        // モデルビュー変換行列を初期化
		Matrix.setIdentityM(mMvpMatrix, 0);
		if (!isOES) {
			MatrixUtils.setMirror(mMvpMatrix, MIRROR_VERTICAL);
		}
		//
		if (muMVPMatrixLoc >= 0) {
        	GLES20.glUniformMatrix4fv(muMVPMatrixLoc, 1, false, mMvpMatrix, 0);
		}
		if (muTexMatrixLoc >= 0) {
			// ここは単位行列に初期化するだけなのでmMvpMatrixを流用
        	GLES20.glUniformMatrix4fv(muTexMatrixLoc, 1, false, mMvpMatrix, 0);
		}
		// 頂点座標配列を割り当てる
		GLES20.glVertexAttribPointer(maPositionLoc,
			2, GLES20.GL_FLOAT, false, VERTEX_SZ, pVertex);
		GLES20.glEnableVertexAttribArray(maPositionLoc);
		// テクスチャ座標配列を割り当てる
		GLES20.glVertexAttribPointer(maTextureCoordLoc,
			2, GLES20.GL_FLOAT, false, VERTEX_SZ, pTexCoord);
		GLES20.glEnableVertexAttribArray(maTextureCoordLoc);
	}

	public void release() {
		GLES20.glUseProgram(0);
		if (hProgram >= 0) {
			GLES20.glDeleteProgram(hProgram);
		}
		hProgram = -1;
	}

	@Override
	public void setMirror(@MirrorMode final int mirror) {
		if (mMirror != mirror) {
			mMirror = mirror;
			MatrixUtils.setMirror(mMvpMatrix, mirror);
		}
	}

	@MirrorMode
	@Override
	public int getMirror() {
		return mMirror;
	}

	protected int getProgram() {
		return hProgram;
	}

	@NonNull
	@Size(value=16)
	public float[] getMvpMatrix() {
		return mMvpMatrix;
	}

	/**
	 * このクラスでは何もしない, 必要なら下位クラスでオーバーライドすること
	 * @param width
	 * @param height
	 */
	public void setTexSize(final int width, final int height) {
	}

	/**
	 * モデルビュー変換行列に行列を割り当てる
	 * @param matrix 領域チェックしていないのでoffsetから16個以上必須
	 * @param offset
	 * @return
	 */
	public void setMvpMatrix(@NonNull @Size(min=16) final float[] matrix, final int offset) {
		synchronized (mSync) {
			System.arraycopy(matrix, offset, mMvpMatrix, 0, mMvpMatrix.length);
		}
	}

	/**
	 * モデルビュー変換行列のコピーを取得
	 * @param matrix 領域チェックしていないのでoffsetから16個以上必須
	 * @param offset
	 */
	public void getMvpMatrix(@NonNull @Size(min=16) final float[] matrix, final int offset) {
		System.arraycopy(mMvpMatrix, 0, matrix, offset, mMvpMatrix.length);
	}

	/**
	 * preDraw => draw => postDrawを順に呼び出す
	 * @param texIds texture ID
	 * @param texMatrix テクスチャ変換行列、nullならば以前に適用したものが再利用される.
	 * 			領域チェックしていないのでoffsetから16個以上確保しておくこと
	 * @param offset テクスチャ変換行列のオフセット
	 */
	public void apply(@NonNull final int[] texIds, @Nullable @Size(min=16) final float[] texMatrix, final int offset) {
		synchronized (mSync) {
			GLES20.glUseProgram(hProgram);
			preDraw(texIds, texMatrix, offset);
			draw(texIds, texMatrix, offset);
			postDraw();
		}
	}

	/**
	 * 描画の前処理
	 * テクスチャ変換行列/モデルビュー変換行列を代入, テクスチャをbindする
	 * mSyncはロックされて呼び出される
	 * @param texIds texture ID
	 * @param texMatrix テクスチャ変換行列、nullならば以前に適用したものが再利用される.
	 * 			領域チェックしていないのでoffsetから16個以上確保しておくこと
	 * @param offset テクスチャ変換行列のオフセット
	 */
	protected void preDraw(@NonNull final int[] texIds, @Nullable @Size(min=16) final float[] texMatrix, final int offset) {
		if ((muTexMatrixLoc >= 0) && (texMatrix != null)) {
			GLES20.glUniformMatrix4fv(muTexMatrixLoc, 1, false, texMatrix, offset);
		}
		if (muMVPMatrixLoc >= 0) {
			GLES20.glUniformMatrix4fv(muMVPMatrixLoc, 1, false, mMvpMatrix, 0);
		}
		bindTexture(texIds);
	}

	protected void bindTexture(@NonNull final int[] texIds) {
		final int n = Math.min(texIds.length, muTexLoc.length);
		for (int i = 0; i < n; i++) {
			if (texIds[i] != GL_NO_TEXTURE) {
				GLES20.glActiveTexture(TEX_NUMBERS[i]);
				GLES20.glBindTexture(mTexTarget, texIds[i]);
				GLES20.glUniform1i(muTexLoc[i], i);
			}
		}
	}
	
	/**
	 * 実際の描画実行, GLES20.glDrawArraysを呼び出すだけ
	 * mSyncはロックされて呼び出される
	 * @param texIds texture ID
	 * @param texMatrix テクスチャ変換行列、nullならば以前に適用したものが再利用される.
	 * 			領域チェックしていないのでoffsetから16個以上確保しておくこと
	 * @param offset テクスチャ変換行列のオフセット
	 */
	protected void draw(@NonNull final int[] texIds, @Nullable @Size(min=16) final float[] texMatrix, final int offset) {
//		if (DEBUG) Log.v(TAG, "draw");
		// これが実際の描画
		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, VERTEX_NUM);
	}

	/**
	 * 描画後の後処理, テクスチャのunbind, プログラムをデフォルトに戻す
	 * mSyncはロックされて呼び出される
	 */
	protected void postDraw() {
		unbindTexture();
        GLES20.glUseProgram(0);
	}

	protected void unbindTexture() {
		for (int i = 0; i < muTexLoc.length; i++) {
			GLES20.glActiveTexture(TEX_NUMBERS[i]);
			GLES20.glBindTexture(mTexTarget, 0);
		}
	}
}
