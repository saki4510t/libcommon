package com.serenegiant.glutils.es2;
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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;

import com.serenegiant.glutils.IGLSurface;

import java.io.IOException;

import androidx.annotation.NonNull;

/**
 * OpenGL|ESのテクスチャ操作用のヘルパークラス
 */
public class GLTexture implements IGLSurface {
//	private static final boolean DEBUG = false;	// FIXME 実働時はfalseにすること
//	private static final String TAG = "GLTexture";

	/* package */final int mTextureTarget;
	/* package */final int mTextureUnit ;
	/* package */int mTextureId;
	/* package */final float[] mTexMatrix = new float[16];	// テクスチャ変換行列
	/* package */int mTexWidth, mTexHeight;
	/* package */int mImageWidth, mImageHeight;
	private int viewPortX, viewPortY, viewPortWidth, viewPortHeight;

	/**
	 * コンストラクタ
	 * テクスチャユニットが常時GL_TEXTURE0なので複数のテクスチャを同時に使えない
	 * @param width テクスチャサイズ
	 * @param height テクスチャサイズ
	 * @param filter_param	テクスチャの補間方法を指定 GL_LINEARとかGL_NEAREST
	 */
	public GLTexture(final int width, final int height, final int filter_param) {
		this(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE0, width, height, filter_param);
	}

	/**
	 * コンストラクタ
	 * @param texTarget GL_TEXTURE_EXTERNAL_OESはだめ
	 * @param texUnit
	 * @param width テクスチャサイズ
	 * @param height テクスチャサイズ
	 * @param filter_param	テクスチャの補間方法を指定 GL_LINEARとかGL_NEAREST
	 */
	public GLTexture(final int texTarget, final int texUnit,
					 final int width, final int height, final int filter_param) {
//		if (DEBUG) Log.v(TAG, String.format("コンストラクタ(%d,%d)", width, height));
		mTextureTarget = texTarget;
		mTextureUnit = texUnit;
		// テクスチャに使うビットマップは縦横サイズが2の乗数でないとダメ。
		// 更に、ミップマップするなら正方形でないとダメ
		// 指定したwidth/heightと同じか大きい2の乗数にする
		int w = 32;
		for (; w < width; w <<= 1);
		int h = 32;
		for (; h < height; h <<= 1);
		if (mTexWidth != w || mTexHeight != h) {
			mTexWidth = w;
			mTexHeight = h;
		}
		mImageWidth = mTexWidth;
		mImageHeight = mTexHeight;
//		if (DEBUG) Log.v(TAG, String.format("texSize(%d,%d)", mTexWidth, mTexHeight));
		mTextureId = GLHelper.initTex(mTextureTarget, texUnit, filter_param);
		// テクスチャのメモリ領域を確保する
		GLES20.glTexImage2D(mTextureTarget,
			0,					// ミップマップレベル0(ミップマップしない)
			GLES20.GL_RGBA,				// 内部フォーマット
			mTexWidth, mTexHeight,		// サイズ
			0,					// 境界幅
			GLES20.GL_RGBA,				// 引き渡すデータのフォーマット
			GLES20.GL_UNSIGNED_BYTE,	// データの型
			null);				// ピクセルデータ無し
		// テクスチャ変換行列を初期化
		Matrix.setIdentityM(mTexMatrix, 0);
		mTexMatrix[0] = width / (float)mTexWidth;
		mTexMatrix[5] = height / (float)mTexHeight;
		setViewPort(0, 0, mImageWidth, mImageHeight);
//		if (DEBUG) Log.v(TAG, "GLTexture:id=" + mTextureId);
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
	@Override
	public void release() {
//		if (DEBUG) Log.v(TAG, "release:");
		if (mTextureId >= 0) {
			GLHelper.deleteTex(mTextureId);
			mTextureId = -1;
		}
	}

	/**
	 * このインスタンスで管理しているテクスチャを有効にする(バインドする)
	 */
	@Override
	public void makeCurrent() {
//		if (DEBUG) Log.v(TAG, "makeCurrent:");
		GLES20.glActiveTexture(mTextureUnit);	// テクスチャユニットを選択
		GLES20.glBindTexture(mTextureTarget, mTextureId);
		setViewPort(viewPortX, viewPortY, viewPortWidth, viewPortHeight);
	}

	/**
	 * Viewportを設定
	 * ここで設定した値は次回以降makeCurrentを呼んだときに復帰される
	 * @param x
	 * @param y
	 * @param width
	 * @param height
	 */
	@Override
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
	@Override
	public void swap() {
//		if (DEBUG) Log.v(TAG, "swap:");
		GLES20.glActiveTexture(mTextureUnit);	// テクスチャユニットを選択
		GLES20.glBindTexture(mTextureTarget, 0);
	}

	@Override
	public boolean isValid() {
		return mTextureId >= 0;
	}

	/**
	 * テクスチャターゲットを取得(GL_TEXTURE_2D)
	 * @return
	 */
	@Override
	public int getTexTarget() {
		return mTextureTarget;
	}

	@Override
	public int getTexUnit() {
		return mTextureUnit;
	}

	/**
	 * テクスチャ名を取得
	 * @return
	 */
	@Override
	public int getTexId() {
		return mTextureId;
	}

	@Override
	public int getWidth() {
		return 0;
	}

	@Override
	public int getHeight() {
		return 0;
	}

	/**
	 * #copyTexMatrix()の返り値用のfloat配列
	 */
	private final float[] mResultMatrix = new float[16];
	/**
	 * IGLSurfaceの実装
	 * テクスチャ座標変換行列のコピーを取得
	 * @return
	 */
	@Override
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
	@Override
	public void copyTexMatrix(final float[] matrix, final int offset) {
		System.arraycopy(mTexMatrix, 0, matrix, offset, mTexMatrix.length);
	}

	/**
	 * テクスチャ座標変換行列を取得(内部配列をそのまま返すので変更時は要注意)
	 * @return
	 */
	@Override
	public float[] getTexMatrix() {
		return mTexMatrix;
	}

	/**
	 * テクスチャ幅を取得
	 * @return
	 */
	@Override
	public int getTexWidth() {
		return mTexWidth;
	}

	/**
	 * テクスチャ高さを取得
	 * @return
	 */
	@Override
	public int getTexHeight() {
		return mTexHeight;
	}

	/**
	 * 指定したファイルから画像をテクスチャに読み込む
	 * ファイルが存在しないか読み込めなければIOException/NullPointerExceptionを生成
	 * @param filePath
	 */
	@Override
	public void loadBitmap(@NonNull final String filePath) throws IOException {
//		if (DEBUG) Log.v(TAG, "loadBitmap:path=" + filePath);
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;	// Bitmapを生成せずにサイズ等の情報だけを取得する
		BitmapFactory.decodeFile(filePath, options);
		// テキスチャサイズ内に指定したイメージが収まるためのサブサンプリングを値を求める
		final int imageWidth = options.outWidth;
		final int imageHeight = options.outHeight;
		int inSampleSize = 1;	// サブサンプリングサイズ
		if ((imageHeight > mTexHeight) || (imageWidth > mTexWidth)) {
			if (imageWidth > imageHeight) {
				inSampleSize = (int)Math.ceil(imageHeight / (float)mTexHeight);
			} else {
				inSampleSize = (int)Math.ceil(imageWidth / (float)mTexWidth);
			}
		}
//		if (DEBUG) Log.v(TAG, String.format("image(%d,%d),tex(%d,%d),inSampleSize=%d",
// 			imageWidth, imageHeight, mTexWidth, mTexHeight, inSampleSize));
		// 実際の読み込み処理
		options.inSampleSize = inSampleSize;
		options.inJustDecodeBounds = false;
		loadBitmap(BitmapFactory.decodeFile(filePath, options));
	}
	
	/**
	 * 指定したビットマップをテクスチャに読み込む
 	 * @param bitmap
	 */
	@Override
	public void loadBitmap(@NonNull final Bitmap bitmap) {
		mImageWidth = bitmap.getWidth();	// 読み込んだイメージのサイズを取得
		mImageHeight = bitmap.getHeight();
		Bitmap texture = Bitmap.createBitmap(mTexWidth, mTexHeight, Bitmap.Config.ARGB_8888);
		final Canvas canvas = new Canvas(texture);
		canvas.drawBitmap(bitmap, 0, 0, null);
		bitmap.recycle();
		// テクスチャ座標変換行列を設定(読み込んだイメージサイズがテクスチャサイズにフィットするようにスケール変換)
		Matrix.setIdentityM(mTexMatrix, 0);
		mTexMatrix[0] = mImageWidth / (float)mTexWidth;
		mTexMatrix[5] = mImageHeight / (float)mTexHeight;
//		if (DEBUG) Log.v(TAG, String.format("image(%d,%d),scale(%f,%f)",
// 			mImageWidth, mImageHeight, mMvpMatrix[0], mMvpMatrix[5]));
		makeCurrent();
		GLUtils.texImage2D(mTextureTarget, 0, texture, 0);
		swap();
		texture.recycle();
	}
}
