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

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.text.TextUtils;
import android.util.Log;

import com.serenegiant.glutils.IMirror;
import com.serenegiant.graphics.MatrixUtils;
import com.serenegiant.utils.BufferHelper;

import java.nio.FloatBuffer;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;
import androidx.annotation.WorkerThread;

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
		@WorkerThread
		@NonNull
		public default GLDrawer2D create(final boolean isGLES3, final boolean isOES) {
			return GLDrawer2D.create(isGLES3, isOES);
		}
	}

	/**
	 * デフォルトのDrawerFactory実装
	 */
	public static DrawerFactory DEFAULT_FACTORY = new DrawerFactory() {};

	/**
	 * バッファオブジェクトを使って描画するかどうか
	 */
	protected static final boolean USE_VBO = true;

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
	public static GLDrawer2D create(
		final boolean isGLES3, final boolean isOES,
		@Nullable final float[] vertices,
		@Nullable final float[] texcoord,
		@Nullable final String vs, @Nullable final String fs) {

		return new GLDrawer2D(
			isGLES3 && (GLUtils.getSupportedGLVersion() > 2),
			isOES, vertices, texcoord, vs, fs);
	}

	/**
	 * 頂点座標用バッファオブジェクト名
	 */
	private int mBufVertex = GL_NO_BUFFER;
	/**
	 * テクスチャ座標用バッファオブジェクト名
	 */
	private int mBufTexCoord = GL_NO_BUFFER;
	/**
	 * GLES3を使うかどうか
	 */
	public final boolean isGLES3;
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
	/**
	 * テクスチャターゲット
	 * GL_TEXTURE_EXTERNAL_OESかGL_TEXTURE_2D
	 */
	@TexTarget
	public final int texTarget;

	private int hProgram;
	/**
	 * 頂点座標のlocation
	 */
	private int maPositionLoc;
	/**
	 * テクスチャ座標のlocation
	 */
	private int maTextureCoordLoc;
	/**
	 * 使用するテクスチャユニットのlocation
	 */
	private int muTextureLoc;
	/**
	 * モデルビュー変換行列のlocation
	 */
	private int muMVPMatrixLoc;
	/**
	 * テクスチャ座標変換行列のlocation
	 */
	private int muTexMatrixLoc;
	/**
	 * モデルビュー変換行列
	 */
	@Size(min=16)
    @NonNull
	private final float[] mMvpMatrix = new float[16];
	/**
	 * エラーカウンタ
	 */
	private int errCnt;
	@IMirror.MirrorMode
	private int mMirror = IMirror.MIRROR_NORMAL;

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
		final float[] _vertices = (vertices != null && vertices.length >= 2) ? vertices : DEFAULT_VERTICES_2D;
		@NonNull
		final float[] _texcoord = (texcoord != null && texcoord.length >= 2) ? texcoord : DEFAULT_TEXCOORD_2D;
		VERTEX_NUM = Math.min(_vertices.length, _texcoord.length) / 2;
		VERTEX_SZ = VERTEX_NUM * 2;

		final String _vs = !TextUtils.isEmpty(vs) ? vs
			: isGLES3 ? VERTEX_SHADER_ES3 : VERTEX_SHADER_ES2;
		final String _fs = !TextUtils.isEmpty(fs) ? fs
			: isGLES3 ? (isOES ? FRAGMENT_SHADER_EXT_ES3 : FRAGMENT_SHADER_ES3)
					  : (isOES ? FRAGMENT_SHADER_EXT_ES2 : FRAGMENT_SHADER_ES2);

		texTarget = isOES ? GL_TEXTURE_EXTERNAL_OES : GL_TEXTURE_2D;
		pVertex = BufferHelper.createBuffer(_vertices);
		pTexCoord = BufferHelper.createBuffer(_texcoord);

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
	 * @return
	 */
	public boolean isOES() {
		return texTarget == GL_TEXTURE_EXTERNAL_OES;
	}

	/**
	 * モデルビュー変換行列を取得(内部配列を直接返すので変更時は要注意)
	 * @return
	 */
	@Size(min=16)
	@NonNull
	public float[] getMvpMatrix() {
		return mMvpMatrix;
	}

	/**
	 * モデルビュー変換行列に行列を割り当てる
	 * #setMvpMatrix, #setMirror, #rotate, #setRotationは同じモデルビュー変換行列を変更するので注意!
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
	 * @param matrix 領域チェックしていないのでoffsetから16個以上必須
	 * @param offset
	 */
	public void copyMvpMatrix(@NonNull @Size(min=16) final float[] matrix, final int offset) {
		System.arraycopy(mMvpMatrix, 0, matrix, offset, 16);
	}

	/**
	 * モデルビュー変換行列に左右・上下反転をセット
	 * #setMvpMatrix, #setMirror, #rotate, #setRotationは同じモデルビュー変換行列を変更するので注意!
	 * @param mirror
	 */
	public void setMirror(@IMirror.MirrorMode final int mirror) {
		mMirror = mirror;
		MatrixUtils.setMirror(mMvpMatrix, mirror);
	}

	/**
	 * #setMirrorで適用したミラーモードを取得
	 * 外部でモデルビュー変換行列を変更した場合は一致しない可能性がある
	 * @return
	 */
	public int getMirror() {
		return mMirror;
	}

	/**
	 * 現在のモデルビュー変換行列をxy平面で指定した角度回転させる
	 * #setMvpMatrix, #setMirror, #rotate, #setRotationは同じモデルビュー変換行列を変更するので注意!
	 * @param degrees
	 */
	public void rotate(final int degrees) {
		MatrixUtils.rotate(mMvpMatrix, degrees);
	}

	/**
	 * モデルビュー変換行列にxy平面で指定した角度回転させた回転行列をセットする
	 * #setMvpMatrix, #setMirror, #rotate, #setRotationは同じモデルビュー変換行列を変更するので注意!
	 * @param degrees
	 */
	public void setRotation(final int degrees) {
		MatrixUtils.setRotation(mMvpMatrix, degrees);
	}

	/**
	 * IGLSurfaceオブジェクトを描画するためのヘルパーメソッド
	 * IGLSurfaceオブジェクトで管理しているテクスチャ名とテクスチャ座標変換行列を使って描画する
	 * IDrawer2Dの実装
	 * @param surface
	 */
	public final void draw(@NonNull final IGLSurface surface) {
		draw(surface.getTexUnit(), surface.getTexId(), surface.getTexMatrix(), 0, mMvpMatrix, 0);
	}

	/**
	 * GLTextureオブジェクトを描画するためのヘルパーメソッド
	 * GLTextureオブジェクトで管理しているテクスチャ名とテクスチャ座標変換行列を使って描画する
	 * @param texture
	 */
	public final void draw(@NonNull final GLTexture texture) {
		draw(texture.getTexUnit(), texture.getTexId(), texture.getTexMatrix(), 0, mMvpMatrix, 0);
	}

	/**
	 * 描画処理
	 * @param texUnit
	 * @param texId
	 * @param texMatrix
	 * @param offset
	 */
	public final synchronized void draw(
		@TexUnit final int texUnit, final int texId,
		@Nullable @Size(min=16) final float[] texMatrix, final int offset) {

		draw(texUnit, texId, texMatrix, offset, mMvpMatrix, 0);
	}

	/**
	 * 描画処理
	 * @param texUnit
	 * @param texId
	 * @param texMatrix
	 * @param texOffset
	 * @param mvpMatrix
	 * @param mvpOffset
	 */
	public final synchronized void draw(
		@TexUnit final int texUnit, final int texId,
		@Nullable @Size(min=16) final float[] texMatrix, final int texOffset,
		@Nullable @Size(min=16) final float[] mvpMatrix, final int mvpOffset) {

//		if (DEBUG) Log.v(TAG, "draw");
		if (hProgram < 0) return;
		prepareDraw(texUnit, texId, texMatrix, texOffset, mvpMatrix, mvpOffset);
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
	 * 描画の準備
	 * @param texUnit
	 * @param texId
	 * @param texMatrix
	 * @param texOffset
	 * @param mvpMatrix
	 * @param mvpOffset
	 */
	@CallSuper
	protected void prepareDraw(
		@TexUnit final int texUnit, final int texId,
		@Nullable @Size(min=16) final float[] texMatrix, final int texOffset,
		@Nullable @Size(min=16) final float[] mvpMatrix, final int mvpOffset) {

		glUseProgram();
		if (texMatrix != null) {
			// テクスチャ変換行列が指定されている時
			updateTexMatrix(texMatrix, texOffset);
		}
		if (mvpMatrix != null) {
			// モデルビュー変換行列が指定されている時
			updateMvpMatrix(mvpMatrix, mvpOffset);
		}
		bindTexture(texUnit, texId);
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
	protected void updateMvpMatrix(@NonNull @Size(min=16) final float[] mvpMatrix, final int offset) {
		GLES20.glUniformMatrix4fv(muMVPMatrixLoc, 1, false, mvpMatrix, offset);
	}

	/**
	 * テクスチャをバインド
	 * @param texUnit
	 * @param texId
	 */
	@CallSuper
	protected void bindTexture(@TexUnit final int texUnit, final int texId) {
		GLES20.glActiveTexture(texUnit);
		GLES20.glBindTexture(texTarget, texId);
		GLES20.glUniform1i(muTextureLoc, GLUtils.gLTextureUnit2Index(texUnit));
	}

	/**
	 * 頂点座標をセット
	 */
	@CallSuper
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
	@CallSuper
	protected void finishDraw() {
		GLES20.glBindTexture(texTarget, 0);
        GLES20.glUseProgram(0);
	}

	/**
	 * テクスチャ名生成のヘルパーメソッド
	 * GLHelper#initTexを呼び出すだけ
	 * @param texUnit
	 * @return texture ID
	 */
	@Deprecated
	public int initTex(@TexUnit final int texUnit) {
		return GLUtils.initTex(texTarget, texUnit, GLES20.GL_NEAREST);
	}

	/**
	 * テクスチャ名生成のヘルパーメソッド
	 * GLHelper#initTexを呼び出すだけ
	 * @param texUnit
	 * @param filterParam
	 * @return
	 */
	@Deprecated
	public int initTex(@TexUnit final int texUnit, final int filterParam) {
		return GLUtils.initTex(texTarget, texUnit, filterParam);
	}

	/**
	 * テクスチャ名破棄のヘルパーメソッド
	 * GLHelper.deleteTexを呼び出すだけ
	 * @param hTex
	 */
	@Deprecated
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
	@CallSuper
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
	@CallSuper
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
	@CallSuper
	protected void releaseShader() {
		if (hProgram > GL_NO_PROGRAM) {
			internalReleaseShader(hProgram);
		}
		hProgram = GL_NO_PROGRAM;
	}

	/**
	 * シェーダーをコンパイル
	 * @param vs 頂点シェーダーのソース文字列
	 * @param fs フラグメントシェーダーのソース文字列
	 * @return
	 */
	protected final int loadShader(@NonNull final String vs, @NonNull final String fs) {
		if (DEBUG) Log.v(TAG, "loadShader:");
		return GLUtils.loadShader(vs, fs);
	}

	/**
	 * シェーダー破棄処理の実体
	 * @param program
	 */
	@CallSuper
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
	 * プログラムハンドル取得
	 * @return
	 */
	public final int getProgram() {
		return hProgram;
	}

	/**
	 * アトリビュート変数のロケーションを取得
	 * glUseProgramが呼ばれた状態で返る
	 * @param name
	 * @return
	 */
	public final int glGetAttribLocation(@NonNull final String name) {
		GLES20.glUseProgram(hProgram);
		return GLES20.glGetAttribLocation(hProgram, name);
	}

	/**
	 * ユニフォーム変数のロケーションを取得
	 * glUseProgramが呼ばれた状態で返る
	 * @param name
	 * @return
	 */
	public final int glGetUniformLocation(@NonNull final String name) {
		GLES20.glUseProgram(hProgram);
		return GLES20.glGetUniformLocation(hProgram, name);
	}

	/**
	 * glUseProgramが呼ばれた状態で返る
	 */
	public final void glUseProgram() {
		GLES20.glUseProgram(hProgram);
	}

	/**
	 * シェーダープログラム変更時の初期化処理
	 * glUseProgramが呼ばれた状態で返る
	 */
	@CallSuper
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
	protected final boolean validateProgram(final int program) {
		if (program >= 0) {
			GLES20.glValidateProgram(program);
			GLES20.glGetProgramiv(program, GLES20.GL_VALIDATE_STATUS, status, 0);
			return status[0] == GLES20.GL_TRUE;
		}
		return false;
	}
}
