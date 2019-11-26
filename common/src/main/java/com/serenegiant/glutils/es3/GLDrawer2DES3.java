package com.serenegiant.glutils.es3;
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

import android.opengl.GLES30;
import android.os.Build;

import com.serenegiant.glutils.GLDrawer2D;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;


/**
 * 描画領域全面にテクスチャを2D描画するためのヘルパークラス
 */
@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class GLDrawer2DES3 extends GLDrawer2D {
//	private static final boolean DEBUG = false; // FIXME set false on release
//	private static final String TAG = "GLDrawer2DES3";

	/**
	 * コンストラクタ
	 * GLコンテキスト/EGLレンダリングコンテキストが有効な状態で呼ばないとダメ
	 * @param isOES 外部テクスチャ(GL_TEXTURE_EXTERNAL_OES)を使う場合はtrue。
	 * 				通常の2Dテキスチャならfalse
	 */
	public GLDrawer2DES3(final boolean isOES) {
		this(DEFAULT_VERTICES, DEFAULT_TEXCOORD, isOES);
	}

	/**
	 * コンストラクタ
	 * GLコンテキスト/EGLレンダリングコンテキストが有効な状態で呼ばないとダメ
	 * @param vertices 頂点座標, floatを8個 = (x,y) x 4ペア
	 * @param texcoord テクスチャ座標, floatを8個 = (s,t) x 4ペア
	 * @param isOES 外部テクスチャ(GL_TEXTURE_EXTERNAL_OES)を使う場合はtrue。
	 * 				通常の2Dテキスチャならfalse
	 */
	public GLDrawer2DES3(final float[] vertices,
		final float[] texcoord, final boolean isOES) {

		super(vertices, texcoord, isOES);
	}

	/**
	 * 破棄処理。GLコンテキスト/EGLレンダリングコンテキスト内で呼び出さないとダメ
	 * IDrawer2Dの実装
	 */
	@CallSuper
	@Override
	public void release() {
		if (hProgram >= 0) {
			GLES30.glDeleteProgram(hProgram);
		}
		hProgram = -1;
		super.release();
	}


	/**
	 * 指定したテクスチャを指定したテクスチャ変換行列を使って描画領域全面に描画するためのヘルパーメソッド
	 * このクラスインスタンスのモデルビュー変換行列が設定されていればそれも適用された状態で描画する
	 * IDrawer2Dの実装
	 * @param texId texture ID
	 * @param tex_matrix テクスチャ変換行列、nullならば以前に適用したものが再利用される。
	 * 					領域チェックしていないのでoffsetから16個以上確保しておくこと
	 */
	@Override
	public synchronized void draw(final int texId,
		final float[] tex_matrix, final int offset) {

//		if (DEBUG) Log.v(TAG, "draw");
		if (hProgram < 0) return;
		GLES30.glUseProgram(hProgram);
		if (tex_matrix != null) {
			// テクスチャ変換行列が指定されている時
			GLES30.glUniformMatrix4fv(muTexMatrixLoc, 1, false, tex_matrix, offset);
		}
		// モデルビュー変換行列をセット
		GLES30.glUniformMatrix4fv(muMVPMatrixLoc, 1, false, mMvpMatrix, 0);
		GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
		GLES30.glBindTexture(mTexTarget, texId);
		if (true) {
			// XXX 共有コンテキストを使っていると頂点配列が壊れてしまうときがあるようなので都度読み込む
			GLES30.glVertexAttribPointer(maPositionLoc,
				2, GLES30.GL_FLOAT, false, VERTEX_SZ, pVertex);
			GLES30.glVertexAttribPointer(maTextureCoordLoc,
				2, GLES30.GL_FLOAT, false, VERTEX_SZ, pTexCoord);
			GLES30.glEnableVertexAttribArray(maPositionLoc);
			GLES30.glEnableVertexAttribArray(maTextureCoordLoc);
		}
		GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, VERTEX_NUM);
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
		return GLHelper.loadShader(vs, fs);
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
		GLES30.glUseProgram(hProgram);
		maPositionLoc = GLES30.glGetAttribLocation(hProgram, "aPosition");
		maTextureCoordLoc = GLES30.glGetAttribLocation(hProgram, "aTextureCoord");
		muMVPMatrixLoc = GLES30.glGetUniformLocation(hProgram, "uMVPMatrix");
		muTexMatrixLoc = GLES30.glGetUniformLocation(hProgram, "uTexMatrix");
		//
		GLES30.glUniformMatrix4fv(muMVPMatrixLoc,
			1, false, mMvpMatrix, 0);
		GLES30.glUniformMatrix4fv(muTexMatrixLoc,
			1, false, mMvpMatrix, 0);
		GLES30.glVertexAttribPointer(maPositionLoc,
			2, GLES30.GL_FLOAT, false, VERTEX_SZ, pVertex);
		GLES30.glVertexAttribPointer(maTextureCoordLoc,
			2, GLES30.GL_FLOAT, false, VERTEX_SZ, pTexCoord);
		GLES30.glEnableVertexAttribArray(maPositionLoc);
		GLES30.glEnableVertexAttribArray(maTextureCoordLoc);
	}
}
