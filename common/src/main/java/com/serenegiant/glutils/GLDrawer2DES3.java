package com.serenegiant.glutils;
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

import android.opengl.GLES30;
import android.os.Build;
import android.util.Log;

import com.serenegiant.glutils.es3.GLHelper;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;


/**
 * 描画領域全面にテクスチャを2D描画するためのヘルパークラス
 * 基本的に直接生成せずにGLDrawer2D#createメソッドを使うこと
 */
@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
	/*package*/ class GLDrawer2DES3 extends GLDrawer2D {
	private static final boolean DEBUG = false; // FIXME set false on release
	private static final String TAG = GLDrawer2DES3.class.getSimpleName();

	/**
	 * 頂点座標用バッファオブジェクト名
	 */
	private int mBufVertex;
	/**
	 * テクスチャ座標用バッファオブジェクト名
	 */
	private int mBufTexCoord;

	/**
	 * コンストラクタ
	 * GLコンテキスト/EGLレンダリングコンテキストが有効な状態で呼ばないとダメ
	 * @param vertices 頂点座標, floatを8個 = (x,y) x 4ペア
	 * @param texcoord テクスチャ座標, floatを8個 = (s,t) x 4ペア
	 * @param isOES 外部テクスチャ(GL_TEXTURE_EXTERNAL_OES)を使う場合はtrue。
	 * 				通常の2Dテキスチャならfalse
	 */
	/*package*/ GLDrawer2DES3(final float[] vertices,
		final float[] texcoord, final boolean isOES) {

		super(true, vertices, texcoord, isOES);
	}

	@Override
	protected void updateTexMatrix(final float[] texMatrix, final int offset) {
		GLES30.glUniformMatrix4fv(muTexMatrixLoc, 1, false, texMatrix, offset);
	}

	@Override
	protected void updateMvpMatrix(final float[] mvpMatrix, final int offset) {
		GLES30.glUniformMatrix4fv(muMVPMatrixLoc, 1, false, mvpMatrix, offset);
	}

	@Override
	protected void bindTexture(final int texUnit, final int texId) {
		GLES30.glActiveTexture(texUnit);
		GLES30.glBindTexture(mTexTarget, texId);
		GLES30.glUniform1i(muTextureLoc, gLTextureUnit2Index(texUnit));
	}

	@Override
	protected void updateVertices() {
		if (USE_VBO) {
			if (mBufVertex <= 0) {
				pVertex.clear();
				mBufVertex = GLHelper.createBuffer(GLES30.GL_ARRAY_BUFFER, pVertex, GLES30.GL_STATIC_DRAW);
				if (DEBUG) Log.v(TAG, "updateVertices:create buffer object for vertex," + mBufVertex);
			}
			if (mBufTexCoord <= 0) {
				pTexCoord.clear();
				mBufTexCoord = GLHelper.createBuffer(GLES30.GL_ARRAY_BUFFER, pTexCoord, GLES30.GL_STATIC_DRAW);
				if (DEBUG) Log.v(TAG, "updateVertices:create buffer object for tex coord," + mBufTexCoord);
			}
			// 頂点座標をセット
			GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mBufVertex);
			GLES30.glVertexAttribPointer(maPositionLoc,
				2, GLES30.GL_FLOAT, false, 0, 0);
			GLES30.glEnableVertexAttribArray(maPositionLoc);
			// テクスチャ座標をセット
			GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mBufTexCoord);
			GLES30.glVertexAttribPointer(maTextureCoordLoc,
				2, GLES30.GL_FLOAT, false, 0, 0);
			GLES30.glEnableVertexAttribArray(maTextureCoordLoc);
		} else {
			// 頂点座標をセット
			pVertex.clear();
			GLES30.glVertexAttribPointer(maPositionLoc,
				2, GLES30.GL_FLOAT, false, VERTEX_SZ, pVertex);
			GLES30.glEnableVertexAttribArray(maPositionLoc);
			// テクスチャ座標をセット
			pTexCoord.clear();
			GLES30.glVertexAttribPointer(maTextureCoordLoc,
				2, GLES30.GL_FLOAT, false, VERTEX_SZ, pTexCoord);
			GLES30.glEnableVertexAttribArray(maTextureCoordLoc);
		}
	}

	@Override
	protected void drawVertices() {
		GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, VERTEX_NUM);
	}

	@Override
	protected void finishDraw() {
		GLES30.glBindTexture(mTexTarget, 0);
		GLES30.glUseProgram(0);
	}

	/**
	 * テクスチャ名生成のヘルパーメソッド
	 * GLHelper#initTexを呼び出すだけ
	 * IShaderDrawer2dの実装
	 * @return texture ID
	 */
	@Override
	public int initTex() {
		return GLHelper.initTex(mTexTarget, GLES30.GL_TEXTURE0, GLES30.GL_NEAREST);
	}

	/**
	 * テクスチャ名生成のヘルパーメソッド
	 * GLHelper#initTexを呼び出すだけ
	 * @param texUnit
	 * @param filterParam
	 * @return
	 */
	@Override
	public int initTex(final int texUnit, final int filterParam) {
		return GLHelper.initTex(mTexTarget, texUnit, filterParam);
	}

	/**
	 * テクスチャ名破棄のヘルパーメソッド
	 * GLHelper.deleteTexを呼び出すだけ
	 * IShaderDrawer2dの実装
	 * @param hTex
	 */
	@Override
	public void deleteTex(final int hTex) {
		GLHelper.deleteTex(hTex);
	}

	@Override
	protected int loadShader(@NonNull final String vs, @NonNull final String fs) {
		if (DEBUG) Log.v(TAG, "loadShader:");
		return GLHelper.loadShader(vs, fs);
	}

	/**
	 * 破棄処理。GLコンテキスト/EGLレンダリングコンテキスト内で呼び出さないとダメ
	 * IDrawer2Dの実装
	 */
	@Override
	protected void internalReleaseShader(final int program) {
		// バッファーオブジェクトを破棄
		if (mBufVertex > 0) {
			GLHelper.deleteBuffer(mBufVertex);
			mBufVertex = 0;
		}
		if (mBufTexCoord > 0) {
			GLHelper.deleteBuffer(mBufTexCoord);
			mBufTexCoord = 0;
		}
		// シェーダーを破棄
		GLES30.glDeleteProgram(program);
	}

	/**
	 * アトリビュート変数のロケーションを取得
	 * glUseProgramが呼ばれた状態で返る
	 * IShaderDrawer2dの実装
	 * @param name
	 * @return
	 */
	@Override
	public int glGetAttribLocation(@NonNull final String name) {
		GLES30.glUseProgram(hProgram);
		return GLES30.glGetAttribLocation(hProgram, name);
	}

	/**
	 * ユニフォーム変数のロケーションを取得
	 * glUseProgramが呼ばれた状態で返る
	 * IShaderDrawer2dの実装
	 * @param name
	 * @return
	 */
	@Override
	public int glGetUniformLocation(@NonNull final String name) {
		GLES30.glUseProgram(hProgram);
		return GLES30.glGetUniformLocation(hProgram, name);
	}

	/**
	 * glUseProgramが呼ばれた状態で返る
	 * IShaderDrawer2dの実装
	 */
	@Override
	public void glUseProgram() {
		GLES30.glUseProgram(hProgram);
	}

	/**
	 * シェーダープログラム変更時の初期化処理
	 * glUseProgramが呼ばれた状態で返る
	 */
	@Override
	protected void init() {
		if (DEBUG) Log.v(TAG, "init:");
		GLES30.glUseProgram(hProgram);
		maPositionLoc = GLES30.glGetAttribLocation(hProgram, "aPosition");
		maTextureCoordLoc = GLES30.glGetAttribLocation(hProgram, "aTextureCoord");
		muTextureLoc = GLES30.glGetAttribLocation(hProgram, "sTexture");
		muMVPMatrixLoc = GLES30.glGetUniformLocation(hProgram, "uMVPMatrix");
		muTexMatrixLoc = GLES30.glGetUniformLocation(hProgram, "uTexMatrix");
		//
		GLES30.glUniformMatrix4fv(muMVPMatrixLoc,
			1, false, mMvpMatrix, 0);
		GLES30.glUniformMatrix4fv(muTexMatrixLoc,
			1, false, mMvpMatrix, 0);
		updateVertices();
	}

	private final int[] status = new int[1];
	@Override
	protected boolean validateProgram(final int program) {
		if (program >= 0) {
			GLES30.glValidateProgram(program);
			GLES30.glGetProgramiv(program, GLES30.GL_VALIDATE_STATUS, status, 0);
			return status[0] == GLES30.GL_TRUE;
		}
		return false;
	}

}
