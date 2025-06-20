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

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Size;

/**
 * OpenGL|ESのテクスチャ操作用のヘルパークラス
 */
public class GLTexture implements GLConst {
	private static final boolean DEBUG = false;	// XXX 実働時はfalseにすること
	private static final String TAG = GLTexture.class.getSimpleName();

	private static final boolean DEFAULT_ADJUST_POWER2 = false;

	/**
	 * インスタンス生成のためのヘルパーメソッド
	 * テクスチャターゲットはGL_TEXTURE_2D
	 * @param texUnit
	 * @param width テクスチャサイズ
	 * @param height テクスチャサイズ
	 * @param filter_param	テクスチャの補間方法を指定 GL_LINEARとかGL_NEAREST
	 */
	public static GLTexture newInstance(
		@TexUnit final int texUnit,
		final int width, final int height, final int filter_param) {

		return new GLTexture(
			GLES20.GL_TEXTURE_2D, texUnit, GL_NO_TEXTURE,
			width, height, DEFAULT_ADJUST_POWER2,
			filter_param);
	}

	/**
	 * インスタンス生成のためのヘルパーメソッド
	 * テクスチャターゲットはGL_TEXTURE_2D
	 * @param texUnit
	 * @param width テクスチャサイズ
	 * @param height テクスチャサイズ
	 * @param adjust_power2 テクスチャサイズを2の乗数にするかどうか
	 * @param filter_param	テクスチャの補間方法を指定 GL_LINEARとかGL_NEAREST
	 */
	public static GLTexture newInstance(
		@TexUnit final int texUnit,
		final int width, final int height,
		final boolean adjust_power2,
		@MinMagFilter final int filter_param) {
		return new GLTexture(
			GLES20.GL_TEXTURE_2D, texUnit, GL_NO_TEXTURE,
			width, height, adjust_power2,
			filter_param);
	}

	/**
	 * 既存テクスチャのラップ用ヘルパーメソッド
	 * @param texTarget
	 * @param texUnit
	 * @param texId
	 * @param width
	 * @param height
	 * @return
	 */
	public static GLTexture wrap(
		@TexTarget final int texTarget, @TexUnit final int texUnit, final int texId,
		final int width, final int height) {

		return new GLTexture(
			texTarget, texUnit, texId,
			width, height, false,
			GLES20.GL_LINEAR);
	}

//--------------------------------------------------------------------------------
	@TexTarget
	private final int TEX_TARGET;
	@TexUnit
	private final int TEX_UNIT;
	@MinMagFilter
	private final int FILTER_PARAM;
	private final boolean ADJUST_POWER2;
	private final boolean mWrappedTexture;
	private int mTextureId;
	@Size(value=16)
	@NonNull
	private final float[] mTexMatrix = new float[16];	// テクスチャ変換行列
	private int mTexWidth, mTexHeight;
	private int mWidth, mHeight;
	private int viewPortX, viewPortY, viewPortWidth, viewPortHeight;

	/**
	 * コンストラクタ
	 * @param texTarget 既存のテクスチャをラップするとき以外GL_TEXTURE_EXTERNAL_OESはだめ
	 * @param texUnit
	 * @param texId
	 * @param width テクスチャサイズ
	 * @param height テクスチャサイズ
	 * @param adjust_power2 テクスチャサイズを2の乗数にするかどうか
	 * @param filter_param	テクスチャの補間方法を指定 GL_LINEARとかGL_NEAREST
	 */
	protected GLTexture(
		@TexTarget final int texTarget, @TexUnit final int texUnit, final int texId,
		final int width, final int height,
		final boolean adjust_power2,
		@MinMagFilter final int filter_param) {

		if (DEBUG) Log.v(TAG, String.format("コンストラクタ(%d,%d)", width, height));
		TEX_TARGET = texTarget;
		TEX_UNIT = texUnit;
		mWrappedTexture = texId > GL_NO_TEXTURE;
		mTextureId = texId;
		FILTER_PARAM = filter_param;
		ADJUST_POWER2 = adjust_power2 && (texId <= GL_NO_TEXTURE);
		createTexture(width, height);
		if (DEBUG) Log.v(TAG, "GLTexture:id=" + mTextureId);
	}

	@Override
	protected void finalize() throws Throwable {
		try {
			release();	// GLコンテキスト内じゃない可能性があるのであまり良くないけど
		} finally {
			super.finalize();
		}
	}

	/**
	 * テクスチャを破棄
	 * GLコンテキスト/EGLレンダリングコンテキスト内で呼び出すこと
	 */
	public void release() {
		if (DEBUG) Log.v(TAG, "release:");
		releaseTexture();
	}

	/**
	 * このインスタンスで管理しているテクスチャを有効にする(バインドする)
	 * ビューポートの設定も行う
	 */
	public void bind() {
//		if (DEBUG) Log.v(TAG, "makeCurrent:");
		GLES20.glActiveTexture(TEX_UNIT);	// テクスチャユニットを選択
		GLES20.glBindTexture(TEX_TARGET, mTextureId);
		setViewPort(viewPortX, viewPortY, viewPortWidth, viewPortHeight);
	}

	/**
	 * テクスチャをバインドする
	 * (ビューポートの設定はしない)
	 */
	public void bindTexture() {
		GLES20.glActiveTexture(TEX_UNIT);
		GLES20.glBindTexture(TEX_TARGET, mTextureId);
	}

	/**
	 * Viewportを設定
	 * ここで設定した値は次回以降makeCurrentを呼んだときに復帰される
	 * @param x
	 * @param y
	 * @param width
	 * @param height
	 */
	public void setViewPort(final int x, final int y, final int width, final int height) {
		viewPortX = x;
		viewPortY = y;
		viewPortWidth = width;
		viewPortHeight = height;

		GLES20.glViewport(x, y, width, height);
	}

	/**
	 * このインスタンスで管理しているテクスチャを無効にする(アンバインドする)
	 */
	public void unbind() {
//		if (DEBUG) Log.v(TAG, "swap:");
		GLES20.glActiveTexture(TEX_UNIT);	// テクスチャユニットを選択
		GLES20.glBindTexture(TEX_TARGET, 0);
	}

	public boolean isValid() {
		return mTextureId > GL_NO_TEXTURE;
	}

	/**
	 * テクスチャが外部テクスチャかどうかを取得
	 * @return
	 */
	public boolean isOES() {
		return TEX_TARGET == GL_TEXTURE_EXTERNAL_OES;
	}

	/**
	 * テクスチャターゲットを取得(GL_TEXTURE_2D)
	 * @return
	 */
	@TexTarget
	public int getTexTarget() {
		return TEX_TARGET;
	}

	@TexUnit
	public int getTexUnit() {
		return TEX_UNIT;
	}

	/**
	 * テクスチャ名を取得
	 * @return
	 */
	public int getTexId() {
		return mTextureId;
	}

	public int getWidth() {
		return 0;
	}

	public int getHeight() {
		return 0;
	}

	/**
	 * #copyTexMatrix()の返り値用のfloat配列
	 */
	@Size(value=16)
	@NonNull
	private final float[] mResultMatrix = new float[16];
	/**
	 * IGLSurfaceの実装
	 * テクスチャ座標変換行列のコピーを取得
	 * @return
	 */
	@Size(value=16)
	@NonNull
	public float[] copyTexMatrix() {
		System.arraycopy(mTexMatrix, 0, mResultMatrix, 0, 16);
		return mResultMatrix;
	}

	/**
	 * IGLSurfaceの実装
	 * テクスチャ座標変換行列のコピーを取得
	 * 領域チェックしていないのでoffset位置から16個以上確保しておくこと
	 * @param matrix
	 * @param offset
	 */
	public void copyTexMatrix(@NonNull @Size(min=16) final float[] matrix, final int offset) {
		System.arraycopy(mTexMatrix, 0, matrix, offset, mTexMatrix.length);
	}

	/**
	 * テクスチャ座標変換行列を取得(内部配列をそのまま返すので変更時は要注意)
	 * @return
	 */
	@Size(value=16)
	@NonNull
	public float[] getTexMatrix() {
		return mTexMatrix;
	}

	/**
	 * テクスチャ幅を取得
	 * @return
	 */
	public int getTexWidth() {
		return mTexWidth;
	}

	/**
	 * テクスチャ高さを取得
	 * @return
	 */
	public int getTexHeight() {
		return mTexHeight;
	}


	/**
	 * 指定したビットマップをテクスチャに読み込む
 	 * @param bitmap
	 */
	public void loadBitmap(@NonNull final Bitmap bitmap) {
		final int width = bitmap.getWidth();
		final int height = bitmap.getHeight();
		if (!mWrappedTexture && (width > mTexWidth) || (height > mTexHeight)) {
			// 自前でテクスチャ生成＆テクスチャサイズが大きくなったとき
			releaseTexture();
			createTexture(width, height);
		}
		bindTexture();
		android.opengl.GLUtils.texImage2D(TEX_TARGET, 0, bitmap, 0);
		GLES20.glBindTexture(TEX_TARGET, 0);
		// initialize texture matrix
		Matrix.setIdentityM(mTexMatrix, 0);
		mTexMatrix[0] = width / (float)mTexWidth;
		mTexMatrix[5] = height / (float)mTexHeight;
		if (DEBUG) Log.v(TAG, String.format("image(%d,%d),scale(%f,%f)",
			mWidth, mHeight, mTexMatrix[0], mTexMatrix[5]));
	}

	private void createTexture(final int width, final int height) {
//		if (DEBUG) Log.v(TAG, String.format("texSize(%d,%d)", mTexWidth, mTexHeight));
		if (mTextureId <= GL_NO_TEXTURE) {
			if (ADJUST_POWER2) {
				// テクスチャのサイズは2の乗数にするとき
				int w = 1;
				for (; w < width; w <<= 1) ;
				int h = 1;
				for (; h < height; h <<= 1) ;
				if (mTexWidth != w || mTexHeight != h) {
					mTexWidth = w;
					mTexHeight = h;
				}
			} else {
				mTexWidth = width;
				mTexHeight = height;
			}
			mWidth = width;
			mHeight = height;
			mTextureId = GLUtils.initTex(TEX_TARGET, TEX_UNIT, FILTER_PARAM);
			// テクスチャのメモリ領域を確保する
			GLES20.glTexImage2D(TEX_TARGET,
				0,					// ミップマップレベル0(ミップマップしない)
				GLES20.GL_RGBA,				// 内部フォーマット
				mTexWidth, mTexHeight,		// サイズ
				0,					// 境界幅
				GLES20.GL_RGBA,				// 引き渡すデータのフォーマット
				GLES20.GL_UNSIGNED_BYTE,	// データの型
				null);				// ピクセルデータ無し
		} else {
			mWidth = mTexWidth = width;
			mHeight = mTexHeight = height;
		}
		// テクスチャ変換行列を初期化
		Matrix.setIdentityM(mTexMatrix, 0);
		mTexMatrix[0] = mWidth / (float)mTexWidth;
		mTexMatrix[5] = mHeight / (float)mTexHeight;
		if (DEBUG) Log.v(TAG, String.format("image(%d,%d),scale(%f,%f)",
			mWidth, mHeight, mTexMatrix[0], mTexMatrix[5]));
		setViewPort(0, 0, mWidth, mHeight);
	}

	private void releaseTexture() {
		if (!mWrappedTexture && (mTextureId > GL_NO_TEXTURE)) {
			GLUtils.deleteTex(mTextureId);
			mTextureId = GL_NO_TEXTURE;
		}
	}

}
