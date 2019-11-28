package com.serenegiant.glutils;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2019 saki t_saki@serenegiant.com
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

import com.serenegiant.glutils.es2.GLHelper;

import androidx.annotation.NonNull;

/**
 * 描画領域全面にテクスチャを2D描画するためのヘルパークラス
 * 基本的に直接生成せずにGLDrawer2D#createメソッドを使うこと
 */
/*package*/class GLDrawer2DES2 extends GLDrawer2D {
	private static final boolean DEBUG = false; // FIXME set false on release
	private static final String TAG = GLDrawer2DES2.class.getSimpleName();

	/**
	 * コンストラクタ
	 * GLコンテキスト/EGLレンダリングコンテキストが有効な状態で呼ばないとダメ
	 * @param vertices 頂点座標, floatを8個 = (x,y) x 4ペア
	 * @param texcoord テクスチャ座標, floatを8個 = (s,t) x 4ペア
	 * @param isOES 外部テクスチャ(GL_TEXTURE_EXTERNAL_OES)を描画に使う場合はtrue。
	 * 				通常の2Dテキスチャを描画に使うならfalse
	 */
	/*package*/ GLDrawer2DES2(final float[] vertices,
		final float[] texcoord, final boolean isOES) {

		super(false, vertices, texcoord, isOES);
	}

	@Override
	protected void updateTexMatrix(final float[] texMatrix, final int offset) {
		GLES20.glUniformMatrix4fv(muTexMatrixLoc, 1, false, texMatrix, offset);
	}

	@Override
	protected void updateMvpMatrix(final float[] mvpMatrix) {
		GLES20.glUniformMatrix4fv(muMVPMatrixLoc, 1, false, mvpMatrix, 0);
	}

	@Override
	protected void bindTexture(final int texId) {
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(mTexTarget, texId);
	}

	@Override
	protected void updateVertices() {
		// 頂点座標をセット
		pVertex.clear();
		GLES20.glVertexAttribPointer(maPositionLoc,
			2, GLES20.GL_FLOAT, false, VERTEX_SZ, pVertex);
		GLES20.glEnableVertexAttribArray(maPositionLoc);
		// テクスチャ座標をセット
		pTexCoord.clear();
		GLES20.glVertexAttribPointer(maTextureCoordLoc,
			2, GLES20.GL_FLOAT, false, VERTEX_SZ, pTexCoord);
		GLES20.glEnableVertexAttribArray(maTextureCoordLoc);
	}

	@Override
	protected void drawVertices() {
		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, VERTEX_NUM);
	}

	@Override
	protected void finishDraw() {
		GLES20.glBindTexture(mTexTarget, 0);
        GLES20.glUseProgram(0);
	}

	/**
	 * テクスチャ名生成のヘルパーメソッド
	 * GLHelper#initTexを呼び出すだけ
	 * @return texture ID
	 */
	public int initTex() {
		return GLHelper.initTex(mTexTarget, GLES20.GL_TEXTURE0, GLES20.GL_NEAREST);
	}

	/**
	 * テクスチャ名破棄のヘルパーメソッド
	 * GLHelper.deleteTexを呼び出すだけ
	 * @param hTex
	 */
	public void deleteTex(final int hTex) {
		GLHelper.deleteTex(hTex);
	}

	@Override
	protected int loadShader(@NonNull final String vs, @NonNull final String fs) {
		return GLHelper.loadShader(vs, fs);
	}

	/**
	 * 破棄処理。GLコンテキスト/EGLレンダリングコンテキスト内で呼び出さないとダメ
	 * IDrawer2Dの実装
	 */
	@Override
	protected void internalReleaseShader(final int program) {
		GLES20.glDeleteProgram(program);
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
		GLES20.glUseProgram(hProgram);
		return GLES20.glGetAttribLocation(hProgram, name);
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
		GLES20.glUseProgram(hProgram);
		return GLES20.glGetUniformLocation(hProgram, name);
	}

	/**
	 * glUseProgramが呼ばれた状態で返る
	 * IShaderDrawer2dの実装
	 */
	@Override
	public void glUseProgram() {
		GLES20.glUseProgram(hProgram);
	}

	/**
	 * シェーダープログラム変更時の初期化処理
	 * glUseProgramが呼ばれた状態で返る
	 */
	protected void init() {
		GLES20.glUseProgram(hProgram);
		maPositionLoc = GLES20.glGetAttribLocation(hProgram, "aPosition");
		maTextureCoordLoc = GLES20.glGetAttribLocation(hProgram, "aTextureCoord");
		muMVPMatrixLoc = GLES20.glGetUniformLocation(hProgram, "uMVPMatrix");
		muTexMatrixLoc = GLES20.glGetUniformLocation(hProgram, "uTexMatrix");
		//
		GLES20.glUniformMatrix4fv(muMVPMatrixLoc,
			1, false, mMvpMatrix, 0);
		GLES20.glUniformMatrix4fv(muTexMatrixLoc,
			1, false, mMvpMatrix, 0);
		updateVertices();
	}

	private final int[] status = new int[1];
	@Override
	protected boolean validateProgram(final int program) {
		if (program >= 0) {
			GLES20.glValidateProgram(program);
			GLES20.glGetProgramiv(program, GLES20.GL_VALIDATE_STATUS, status, 0);
			return status[0] == GLES20.GL_TRUE;
		}
		return false;
	}
}
