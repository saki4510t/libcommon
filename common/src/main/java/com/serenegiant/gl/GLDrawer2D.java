package com.serenegiant.gl;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2023 saki t_saki@serenegiant.com
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
import android.text.TextUtils;
import android.util.Log;

import com.serenegiant.glutils.IMirror;
import com.serenegiant.utils.BufferHelper;

import java.nio.FloatBuffer;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;

import static com.serenegiant.gl.ShaderConst.*;

/**
 * 描画領域全面にテクスチャを2D描画するためのヘルパークラス
 * 基本的に直接生成せずにGLDrawer2D#createメソッドを使うこと
 */
public class GLDrawer2D implements GLConst {
	private static final boolean DEBUG = false; // FIXME set false on release
	private static final String TAG = GLDrawer2D.class.getSimpleName();

	/**
	 * GLDrawer2Dインスタンス生成用のファクトリーインターフェース
	 */
	public interface DrawerFactory {
		@NonNull
		public GLDrawer2D create(final boolean isGLES3, final boolean isOES);
	}

	/**
	 * デフォルトのDrawerFactory実装
	 */
	public static DrawerFactory DEFAULT_FACTORY = new DrawerFactory() {
		@NonNull
		@Override
		public GLDrawer2D create(final boolean isGLES3, final boolean isOES) {
			return GLDrawer2D.create(isGLES3, isOES);
		}
	};

	/**
	 * バッファオブジェクトを使って描画するかどうか
	 */
	protected static final boolean USE_VBO = true;

	protected static final float[] DEFAULT_VERTICES = {
		1.0f, 1.0f,		// 右上
		-1.0f, 1.0f,	// 左上
		1.0f, -1.0f,	// 右下
		-1.0f, -1.0f,	// 左下
	};
	protected static final float[] DEFAULT_TEXCOORD = {
		1.0f, 0.0f,		// 右上
		0.0f, 0.0f,		// 左上
		1.0f, 1.0f,		// 右下
		0.0f, 1.0f,		// 左下
	};
	// 元々のDEFAULT_TEXCOORDはテクスチャ座標を上下反転させたこっちだった
	protected static final float[] DEFAULT_TEXCOORD_FLIP_VERTICAL = {
		1.0f, 1.0f,		// 右上
		0.0f, 1.0f,		// 左上
		1.0f, 0.0f,		// 右下
		0.0f, 0.0f,		// 左下
	};
	protected static final int FLOAT_SZ = Float.SIZE / 8;

	/**
	 * 頂点座標用バッファオブジェクト名
	 */
	private int mBufVertex = GL_NO_BUFFER;
	/**
	 * テクスチャ座標用バッファオブジェクト名
	 */
	private int mBufTexCoord = GL_NO_BUFFER;

	/**
	 * インスタンス生成のためのヘルパーメソッド
	 * 頂点シェーダーとフラグメントシェーダはデフォルトのものを使う
	 * @param isGLES3
	 * @param isOES
	 * @return
	 */
	public static GLDrawer2D create(final boolean isGLES3, final boolean isOES) {
		return create(isGLES3, isOES, null, null, null, null);
	}

	/**
	 * インスタンス生成のためのヘルパーメソッド
	 * @param isGLES3
	 * @param isOES
	 * @param fs
	 * @return
	 */
	@SuppressLint("NewApi")
	public static GLDrawer2D create(
		final boolean isGLES3, final boolean isOES,
		@Nullable final String fs) {

		return create(isGLES3, isOES, null, null, null, fs);
	}

	/**
	 * インスタンス生成のためのヘルパーメソッド
	 * @param isGLES3
	 * @param isOES
	 * @param vs
	 * @param fs
	 * @return
	 */
	@SuppressLint("NewApi")
	public static GLDrawer2D create(
		final boolean isGLES3, final boolean isOES,
		@Nullable final String vs, @Nullable final String fs) {

		return create(isGLES3, isOES, null, null, vs, fs);
	}

	/**
	 * インスタンス生成のためのヘルパーメソッド
	 * @param isGLES3
	 * @param isOES
	 * @param vertices
	 * @param texcoord
	 * @return
	 */
	@SuppressLint("NewApi")
	public static GLDrawer2D create(
		final boolean isGLES3, final boolean isOES,
		@NonNull final float[] vertices,
		@NonNull final float[] texcoord) {

		return create(isGLES3, isOES, vertices, texcoord, null, null);
	}

	/**
	 * インスタンス生成のためのヘルパーメソッド
	 * @param isGLES3
	 * @param isOES
	 * @param vertices
	 * @param texcoord
	 * @return
	 */
	@SuppressLint("NewApi")
	public static GLDrawer2D create(
		final boolean isGLES3, final boolean isOES,
		@NonNull final float[] vertices,
		@NonNull final float[] texcoord,
		@Nullable final String fs) {

		return create(isGLES3, isOES, vertices, texcoord, null, fs);
	}

	/**
	 * インスタンス生成のためのヘルパーメソッド
	 * @param vertices
	 * @param texcoord
	 * @param isOES
	 * @return
	 */
	@SuppressLint("NewApi")
	public static GLDrawer2D create(
		final boolean isGLES3, final boolean isOES,
		@Nullable final float[] vertices,
		@Nullable final float[] texcoord,
		@Nullable final String vs, @Nullable final String fs) {

		if (isGLES3 && (GLUtils.getSupportedGLVersion() > 2)) {
			return new GLDrawer2D(true, isOES, vertices, texcoord, vs, fs);
		} else {
			return new GLDrawer2D(false, isOES, vertices, texcoord, vs, fs);
		}
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
	@TexTarget
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
	@Size(min=16)
    @NonNull
	protected final float[] mMvpMatrix = new float[16];
	/**
	 * エラーカウンタ
	 */
	private int errCnt;

	/**
	 * コンストラクタ
	 * GLコンテキスト/EGLレンダリングコンテキストが有効な状態で呼ばないとダメ
	 * @param isGLES3 GL|ES3かどうか
	 * @param isOES 外部テクスチャ(GL_TEXTURE_EXTERNAL_OES)を描画に使う場合はtrue。
	 * 				通常の2Dテキスチャを描画に使うならfalse
	 * @param vertices 頂点座標, floatを8個 = (x,y) x 4ペア
	 * @param texcoord テクスチャ座標, floatを8個 = (s,t) x 4ペア
	 */
	protected GLDrawer2D(
		final boolean isGLES3, final boolean isOES,
		@Nullable @Size(min=8) final float[] vertices,
		@Nullable @Size(min=8) final float[] texcoord,
		@Nullable final String vs, @Nullable final String fs) {

		if (DEBUG) Log.v(TAG, "コンストラクタ:isGLES3=" + isGLES3 + ",isOES=" + isOES);
		this.isGLES3 = isGLES3;
		@NonNull
		final float[] _vertices = (vertices != null && vertices.length >= 2) ? vertices : DEFAULT_VERTICES;
		@NonNull
		final float[] _texcoord = (texcoord != null && texcoord.length >= 2) ? texcoord : DEFAULT_TEXCOORD;
		VERTEX_NUM = Math.min(_vertices.length, _texcoord.length) / 2;
		VERTEX_SZ = VERTEX_NUM * 2;

		final String _vs = !TextUtils.isEmpty(vs) ? vs
			: isGLES3 ? VERTEX_SHADER_ES3 : VERTEX_SHADER_ES2;
		final String _fs = !TextUtils.isEmpty(fs) ? fs
			: isGLES3 ? (isOES ? FRAGMENT_SHADER_EXT_ES3 : FRAGMENT_SHADER_ES3)
					  : (isOES ? FRAGMENT_SHADER_EXT_ES2 : FRAGMENT_SHADER_ES2);

		mTexTarget = isOES ? GL_TEXTURE_EXTERNAL_OES : GL_TEXTURE_2D;
		pVertex = BufferHelper.createFloatBuffer(_vertices);
		pTexCoord = BufferHelper.createFloatBuffer(_texcoord);

		// モデルビュー変換行列を初期化
		Matrix.setIdentityM(mMvpMatrix, 0);

		updateShader(_vs, _fs);
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
	@Size(min=16)
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
	public GLDrawer2D setMvpMatrix(@NonNull @Size(min=16) final float[] matrix, final int offset) {
		System.arraycopy(matrix, offset, mMvpMatrix, 0, 16);
		return this;
	}

	/**
	 * モデルビュー変換行列のコピーを取得
	 * IDrawer2Dの実装
	 * @param matrix 領域チェックしていないのでoffsetから16個以上必須
	 * @param offset
	 */
	public void copyMvpMatrix(@NonNull @Size(min=16) final float[] matrix, final int offset) {
		System.arraycopy(mMvpMatrix, 0, matrix, offset, 16);
	}

	/**
	 * モデルビュー変換行列に左右・上下反転をセット
	 * @param mirror
	 */
	public void setMirror(@IMirror.MirrorMode final int mirror) {
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
	 * GLTextureオブジェクトを描画するためのヘルパーメソッド
	 * GLTextureオブジェクトで管理しているテクスチャ名とテクスチャ座標変換行列を使って描画する
	 * @param texture
	 */
	public void draw(@NonNull final GLTexture texture) {
		draw(texture.getTexUnit(), texture.getTexId(), texture.getTexMatrix(), 0, mMvpMatrix, 0);
	}

	/**
	 * 描画処理
	 * @param texUnit
	 * @param texId
	 * @param tex_matrix
	 * @param offset
	 */
	public synchronized void draw(
		@TexUnit final int texUnit, final int texId,
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
		@TexUnit final int texUnit, final int texId,
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
	 * @param texMatrix
	 * @param offset
	 */
	protected void updateTexMatrix(@NonNull @Size(min=16) final float[] texMatrix, final int offset) {
		GLES20.glUniformMatrix4fv(muTexMatrixLoc, 1, false, texMatrix, offset);
	}

	/**
	 * モデルビュー変換行列をセット
	 * @param mvpMatrix
	 */
	protected void updateMvpMatrix(@NonNull @Size(min=16)  final float[] mvpMatrix, final int offset) {
		GLES20.glUniformMatrix4fv(muMVPMatrixLoc, 1, false, mvpMatrix, offset);
	}

	/**
	 * テクスチャをバインド
	 * @param texUnit
	 * @param texId
	 */
	protected void bindTexture(@TexUnit final int texUnit, final int texId) {
		GLES20.glActiveTexture(texUnit);
		GLES20.glBindTexture(mTexTarget, texId);
		GLES20.glUniform1i(muTextureLoc, GLUtils.gLTextureUnit2Index(texUnit));
	}

	/**
	 * 頂点座標をセット
	 */
	protected void updateVertices() {
		if (USE_VBO) {
			if (mBufVertex <= GL_NO_BUFFER) {
				pVertex.clear();
				mBufVertex = GLUtils.createBuffer(GLES20.GL_ARRAY_BUFFER, pVertex, GLES20.GL_STATIC_DRAW);
				if (DEBUG) Log.v(TAG, "updateVertices:create buffer object for vertex," + mBufVertex);
			}
			if (mBufTexCoord <= GL_NO_BUFFER) {
				pTexCoord.clear();
				mBufTexCoord = GLUtils.createBuffer(GLES20.GL_ARRAY_BUFFER, pTexCoord, GLES20.GL_STATIC_DRAW);
				if (DEBUG) Log.v(TAG, "updateVertices:create buffer object for tex coord," + mBufTexCoord);
			}
			// 頂点座標をセット
			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mBufVertex);
			GLES20.glVertexAttribPointer(maPositionLoc,
				2, GLES20.GL_FLOAT, false, 0, 0);
			GLES20.glEnableVertexAttribArray(maPositionLoc);
			// テクスチャ座標をセット
			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mBufTexCoord);
			GLES20.glVertexAttribPointer(maTextureCoordLoc,
				2, GLES20.GL_FLOAT, false, 0, 0);
			GLES20.glEnableVertexAttribArray(maTextureCoordLoc);
		} else {
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
	}

	/**
	 * 描画実行
	 */
	protected void drawVertices() {
		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, VERTEX_NUM);
	}

	/**
	 * 描画の後処理
	 */
	protected void finishDraw() {
		GLES20.glBindTexture(mTexTarget, 0);
        GLES20.glUseProgram(0);
	}

	/**
	 * テクスチャ名生成のヘルパーメソッド
	 * GLHelper#initTexを呼び出すだけ
	 * @param texUnit
	 * @return texture ID
	 */
	public int initTex(@TexUnit final int texUnit) {
		return GLUtils.initTex(mTexTarget, texUnit, GLES20.GL_NEAREST);
	}

	/**
	 * テクスチャ名生成のヘルパーメソッド
	 * GLHelper#initTexを呼び出すだけ
	 * @param texUnit
	 * @param filterParam
	 * @return
	 */
	public int initTex(@TexUnit final int texUnit, final int filterParam) {
		return GLUtils.initTex(mTexTarget, texUnit, filterParam);
	}

	/**
	 * テクスチャ名破棄のヘルパーメソッド
	 * GLHelper.deleteTexを呼び出すだけ
	 * @param hTex
	 */
	public void deleteTex(final int hTex) {
		GLUtils.deleteTex(hTex);
	}

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
	 * シェーダーを破棄
	 */
	protected void releaseShader() {
		if (hProgram > GL_NO_PROGRAM) {
			internalReleaseShader(hProgram);
		}
		hProgram = GL_NO_PROGRAM;
	}

	protected int loadShader(@NonNull final String vs, @NonNull final String fs) {
		if (DEBUG) Log.v(TAG, "loadShader:");
		return GLUtils.loadShader(vs, fs);
	}

	protected void internalReleaseShader(final int program) {
		// バッファーオブジェクトを破棄
		if (mBufVertex > GL_NO_BUFFER) {
			GLUtils.deleteBuffer(mBufVertex);
			mBufVertex = GL_NO_BUFFER;
		}
		if (mBufTexCoord > GL_NO_BUFFER) {
			GLUtils.deleteBuffer(mBufTexCoord);
			mBufTexCoord = GL_NO_BUFFER;
		}
		// シェーダーを破棄
		GLES20.glDeleteProgram(program);
	}

	/**
	 * アトリビュート変数のロケーションを取得
	 * glUseProgramが呼ばれた状態で返る
	 * @param name
	 * @return
	 */
	public int glGetAttribLocation(@NonNull final String name) {
		GLES20.glUseProgram(hProgram);
		return GLES20.glGetAttribLocation(hProgram, name);
	}

	/**
	 * ユニフォーム変数のロケーションを取得
	 * glUseProgramが呼ばれた状態で返る
	 * @param name
	 * @return
	 */
	public int glGetUniformLocation(@NonNull final String name) {
		GLES20.glUseProgram(hProgram);
		return GLES20.glGetUniformLocation(hProgram, name);
	}

	/**
	 * glUseProgramが呼ばれた状態で返る
	 */
	public void glUseProgram() {
		GLES20.glUseProgram(hProgram);
	}

	/**
	 * シェーダープログラム変更時の初期化処理
	 * glUseProgramが呼ばれた状態で返る
	 */
	protected void init() {
		if (DEBUG) Log.v(TAG, "init:");
		GLES20.glUseProgram(hProgram);
		maPositionLoc = GLES20.glGetAttribLocation(hProgram, "aPosition");
		maTextureCoordLoc = GLES20.glGetAttribLocation(hProgram, "aTextureCoord");
		muTextureLoc = GLES20.glGetAttribLocation(hProgram, "sTexture");
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
	/**
	 * シェーダープログラムが使用可能かどうかをチェック
	 * @param program
	 * @return
	 */
	protected boolean validateProgram(final int program) {
		if (program >= 0) {
			GLES20.glValidateProgram(program);
			GLES20.glGetProgramiv(program, GLES20.GL_VALIDATE_STATUS, status, 0);
			return status[0] == GLES20.GL_TRUE;
		}
		return false;
	}
}
