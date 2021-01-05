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

import android.annotation.SuppressLint;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import com.serenegiant.system.BuildCheck;
import com.serenegiant.utils.BufferHelper;

import java.nio.FloatBuffer;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static com.serenegiant.glutils.ShaderConst.*;

/**
 * 描画領域全面にテクスチャを2D描画するためのヘルパークラス
 * 基本的に直接生成せずにGLDrawer2D#createメソッドを使うこと
 */
public abstract class GLDrawer2D {
	private static final boolean DEBUG = false; // FIXME set false on release
	private static final String TAG = GLDrawer2D.class.getSimpleName();

	/**
	 * バッファオブジェクトを使って描画するかどうか
	 */
	protected static final boolean USE_VBO = true;

	protected static final float[] DEFAULT_VERTICES = {
		1.0f, 1.0f,		// 右上
		-1.0f, 1.0f,	// 左上
		1.0f, -1.0f,	// 右下
		-1.0f, -1.0f	// 左下
	};
	protected static final float[] DEFAULT_TEXCOORD = {
		1.0f, 1.0f,		// 右上
		0.0f, 1.0f,		// 左上
		1.0f, 0.0f,		// 右下
		0.0f, 0.0f		// 左下
	};
	protected static final int FLOAT_SZ = Float.SIZE / 8;

	/**
	 * インスタンス生成のためのヘルパーメソッド
	 * 頂点シェーダーとフラグメントシェーダはデフォルトのものを使う
	 * @param isOES
	 * @return
	 */
	public static GLDrawer2D create(final boolean isGLES3, final boolean isOES) {
		return create(isGLES3, DEFAULT_VERTICES, DEFAULT_TEXCOORD, isOES);
	}

	/**
	 * インスタンス生成のためのヘルパーメソッド
	 * @param vertices
	 * @param texcoord
	 * @param isOES
	 * @return
	 */
	@SuppressLint("NewApi")
	public static GLDrawer2D create(final boolean isGLES3,
		@NonNull final float[] vertices,
		@NonNull final float[] texcoord, final boolean isOES) {

		if (isGLES3 && BuildCheck.isAndroid4_3()) {
			return new GLDrawer2DES3(vertices, texcoord, isOES);
		} else {
			return new GLDrawer2DES2(vertices, texcoord, isOES);
		}
	}

	protected static int gLTextureUnit2Index(final int glTextureUnit) {
		return (glTextureUnit >= GLES20.GL_TEXTURE0) && (glTextureUnit <= GLES20.GL_TEXTURE31)
			? glTextureUnit - GLES20.GL_TEXTURE0 : 0;
	}

//================================================================================
	/**
	 * GLES3を使うかどうか
	 */
	public final boolean isGLES3;
	/**
	 * 頂点の数
	 */
	protected final int VERTEX_NUM;
	/**
	 * 頂点配列のサイズ
	 */
	protected final int VERTEX_SZ;
	/**
	 * 頂点座標
	 */
	protected final FloatBuffer pVertex;
	/**
	 * テクスチャ座標
	 */
	protected final FloatBuffer pTexCoord;
	/**
	 * テクスチャターゲット
	 * GL_TEXTURE_EXTERNAL_OESかGL_TEXTURE_2D
	 */
	protected final int mTexTarget;

	protected int hProgram;
	/**
	 * 頂点座標のlocation
	 */
	protected int maPositionLoc;
	/**
	 * テクスチャ座標のlocation
	 */
	protected int maTextureCoordLoc;
	/**
	 * 使用するテクスチャユニットのlocation
	 */
	protected int muTextureLoc;
	/**
	 * モデルビュー変換行列のlocation
	 */
	protected int muMVPMatrixLoc;
	/**
	 * テクスチャ座標変換行列のlocation
	 */
	protected int muTexMatrixLoc;
	/**
	 * モデルビュー変換行列
	 */
    @NonNull
	protected final float[] mMvpMatrix = new float[16];
	/**
	 * エラーカウンタ
	 */
	private int errCnt;

	/**
	 * コンストラクタ
	 * GLコンテキスト/EGLレンダリングコンテキストが有効な状態で呼ばないとダメ
	 * @param vertices 頂点座標, floatを8個 = (x,y) x 4ペア
	 * @param texcoord テクスチャ座標, floatを8個 = (s,t) x 4ペア
	 * @param isOES 外部テクスチャ(GL_TEXTURE_EXTERNAL_OES)を描画に使う場合はtrue。
	 * 				通常の2Dテキスチャを描画に使うならfalse
	 */
	protected GLDrawer2D(final boolean isGLES3,
		final float[] vertices,
		final float[] texcoord, final boolean isOES) {

		if (DEBUG) Log.v(TAG, "コンストラクタ:isGLES3=" + isGLES3 + ",isOES=" + isOES);
		this.isGLES3 = isGLES3;
		VERTEX_NUM = Math.min(
			vertices != null ? vertices.length : 0,
			texcoord != null ? texcoord.length : 0) / 2;
		VERTEX_SZ = VERTEX_NUM * 2;

		mTexTarget = isOES ? GL_TEXTURE_EXTERNAL_OES : GL_TEXTURE_2D;
		pVertex = BufferHelper.createFloatBuffer(vertices);
		pTexCoord = BufferHelper.createFloatBuffer(texcoord);

		// モデルビュー変換行列を初期化
		Matrix.setIdentityM(mMvpMatrix, 0);

		resetShader();
	}

	/**
	 * 破棄処理。GLコンテキスト/EGLレンダリングコンテキスト内で呼び出さないとダメ
	 * IDrawer2Dの実装
	 */
	@CallSuper
	public void release() {
		releaseShader();
	}

	/**
	 * 外部テクスチャを使うかどうか
	 * IShaderDrawer2dの実装
	 * @return
	 */
	public boolean isOES() {
		return mTexTarget == GL_TEXTURE_EXTERNAL_OES;
	}

	/**
	 * モデルビュー変換行列を取得(内部配列を直接返すので変更時は要注意)
	 * IDrawer2Dの実装
	 * @return
	 */
	@NonNull
	public float[] getMvpMatrix() {
		return mMvpMatrix;
	}

	/**
	 * モデルビュー変換行列に行列を割り当てる
	 * IDrawer2Dの実装
	 * @param matrix 領域チェックしていないのでoffsetから16個以上必須
	 * @param offset
	 * @return
	 */
	public GLDrawer2D setMvpMatrix(@NonNull final float[] matrix, final int offset) {
		System.arraycopy(matrix, offset, mMvpMatrix, 0, 16);
		return this;
	}

	/**
	 * モデルビュー変換行列のコピーを取得
	 * IDrawer2Dの実装
	 * @param matrix 領域チェックしていないのでoffsetから16個以上必須
	 * @param offset
	 */
	public void copyMvpMatrix(@NonNull final float[] matrix, final int offset) {
		System.arraycopy(mMvpMatrix, 0, matrix, offset, 16);
	}

	/**
	 * モデルビュー変換行列に左右・上下反転をセット
	 * @param mirror
	 */
	public void setMirror(@IRendererCommon.MirrorMode final int mirror) {
		GLUtils.setMirror(mMvpMatrix, mirror);
	}

	/**
	 * 現在のモデルビュー変換行列をxy平面で指定した角度回転させる
	 * @param degrees
	 */
	public void rotate(final int degrees) {
		GLUtils.rotate(mMvpMatrix, degrees);
	}

	/**
	 * モデルビュー変換行列にxy平面で指定した角度回転させた回転行列をセットする
	 * @param degrees
	 */
	public void setRotation(final int degrees) {
		GLUtils.setRotation(mMvpMatrix, degrees);
	}

	/**
	 * IGLSurfaceオブジェクトを描画するためのヘルパーメソッド
	 * IGLSurfaceオブジェクトで管理しているテクスチャ名とテクスチャ座標変換行列を使って描画する
	 * IDrawer2Dの実装
	 * @param surface
	 */
	public void draw(@NonNull final IGLSurface surface) {
		draw(surface.getTexUnit(), surface.getTexId(), surface.getTexMatrix(), 0, mMvpMatrix, 0);
	}

	/**
	 * 描画処理
	 * @param texId
	 * @param tex_matrix
	 * @param offset
	 */
	public synchronized void draw(
		final int texId,
		@Nullable final float[] tex_matrix, final int offset) {

		draw(GLES20.GL_TEXTURE0, texId, tex_matrix, offset, mMvpMatrix, 0);
	}

	/**
	 * 描画処理
	 * @param texUnit
	 * @param texId
	 * @param tex_matrix
	 * @param offset
	 */
	public synchronized void draw(
		final int texUnit, final int texId,
		@Nullable final float[] tex_matrix, final int offset) {

		draw(texUnit, texId, tex_matrix, offset, mMvpMatrix, 0);
	}

	/**
	 * 描画処理
	 * @param texUnit
	 * @param texId
	 * @param tex_matrix
	 * @param tex_offset
	 * @param mvp_matrix
	 * @param mvp_offset
	 */
	public synchronized void draw(
		final int texUnit, final int texId,
		@Nullable final float[] tex_matrix, final int tex_offset,
		@Nullable final float[] mvp_matrix, final int mvp_offset) {

//		if (DEBUG) Log.v(TAG, "draw");
		if (hProgram < 0) return;
		glUseProgram();
		if (tex_matrix != null) {
			// テクスチャ変換行列が指定されている時
			updateTexMatrix(tex_matrix, tex_offset);
		}
		if (mvp_matrix != null) {
			// モデルビュー変換行列が指定されている時
			updateMvpMatrix(mvp_matrix, mvp_offset);
		}
		bindTexture(texUnit, texId);
		if (validateProgram(hProgram)) {
			drawVertices();
			errCnt = 0;
		} else {
			if (errCnt++ == 0) {
				Log.w(TAG, "draw:invalid program");
				// シェーダーを再初期化する
				resetShader();
			}
		}
		finishDraw();
	}

	/**
	 * テクスチャ変換行列をセット
	 * @param tex_matrix
	 * @param offset
	 */
	protected abstract void updateTexMatrix(final float[] tex_matrix, final int offset);

	/**
	 * モデルビュー変換行列をセット
	 * @param mvpMatrix
	 */
	protected abstract void updateMvpMatrix(final float[] mvpMatrix, final int offset);

	/**
	 * テクスチャをバインド
	 * @param texUnit
	 * @param texId
	 */
	protected abstract void bindTexture(final int texUnit, final int texId);

	/**
	 * 頂点座標をセット
	 */
	protected abstract void updateVertices();

	/**
	 * 描画実行
	 */
	protected abstract void drawVertices();

	/**
	 * 描画の後処理
	 */
	protected abstract void finishDraw();

	/**
	 * テクスチャ名生成のヘルパーメソッド
	 * GLHelper#initTexを呼び出すだけ
	 * @return texture ID
	 */
	public abstract int initTex();

	/**
	 * テクスチャ名生成のヘルパーメソッド
	 * GLHelper#initTexを呼び出すだけ
	 * @param texUnit
	 * @param filterParam
	 * @return
	 */
	public abstract int initTex(final int texUnit, final int filterParam);

	/**
	 * テクスチャ名破棄のヘルパーメソッド
	 * GLHelper.deleteTexを呼び出すだけ
	 * @param hTex
	 */
	public abstract void deleteTex(final int hTex);

	/**
	 * 頂点シェーダー・フラグメントシェーダーを変更する
	 * GLコンテキスト/EGLレンダリングコンテキスト内で呼び出さないとダメ
	 * glUseProgramが呼ばれた状態で返る
	 * @param vs 頂点シェーダー文字列
	 * @param fs フラグメントシェーダー文字列
	 */
	public synchronized void updateShader(@NonNull final String vs, @NonNull final String fs) {
		releaseShader();
		hProgram = loadShader(vs, fs);
		init();
	}

	/**
	 * シェーダーを破棄
	 */
	protected void releaseShader() {
		if (hProgram >= 0) {
			internalReleaseShader(hProgram);
		}
		hProgram = -1;
	}

	protected abstract int loadShader(@NonNull final String vs, @NonNull final String fs);
	protected abstract void internalReleaseShader(final int program);

	/**
	 * フラグメントシェーダーを変更する
	 * GLコンテキスト/EGLレンダリングコンテキスト内で呼び出さないとダメ
	 * glUseProgramが呼ばれた状態で返る
	 * @param fs フラグメントシェーダー文字列
	 */
	public void updateShader(@NonNull final String fs) {
		updateShader(isGLES3 ? VERTEX_SHADER_ES3 : VERTEX_SHADER_ES2, fs);
	}

	/**
	 * 頂点シェーダー・フラグメントシェーダーをデフォルトに戻す
	 */
	public void resetShader() {
		releaseShader();
		if (isGLES3) {
			hProgram = loadShader(VERTEX_SHADER_ES3,
				isOES() ? FRAGMENT_SHADER_EXT_ES3 : FRAGMENT_SHADER_ES3);
		} else {
			hProgram = loadShader(VERTEX_SHADER_ES2,
				isOES() ? FRAGMENT_SHADER_EXT_ES2 : FRAGMENT_SHADER_ES2);
		}
		init();
	}

	/**
	 * アトリビュート変数のロケーションを取得
	 * glUseProgramが呼ばれた状態で返る
	 * @param name
	 * @return
	 */
	public abstract int glGetAttribLocation(@NonNull final String name);

	/**
	 * ユニフォーム変数のロケーションを取得
	 * glUseProgramが呼ばれた状態で返る
	 * @param name
	 * @return
	 */
	public abstract int glGetUniformLocation(@NonNull final String name);

	/**
	 * glUseProgramが呼ばれた状態で返る
	 */
	public abstract void glUseProgram();

	/**
	 * シェーダープログラム変更時の初期化処理
	 * glUseProgramが呼ばれた状態で返る
	 */
	protected abstract void init();

	/**
	 * シェーダープログラムが使用可能かどうかをチェック
	 * @param program
	 * @return
	 */
	protected abstract boolean validateProgram(final int program);
}
