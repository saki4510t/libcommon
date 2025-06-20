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

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.Matrix;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;

/**
 * テクスチャへOpenGL|ESで描画するためのオフスクリーン描画クラス
 * テクスチャをカラーバッファとしてFBOに割り当てる
 * テクスチャへのオフスクリーン描画しないのであればGLTextureを使う方が良い
 */
public abstract class GLSurface implements IGLSurface {
	private static final boolean DEBUG = false;
	private static final String TAG = GLSurface.class.getSimpleName();

	private static final boolean DEFAULT_ADJUST_POWER2 = false;

	/**
	 * インスタンス生成のヘルパーメソッド(GL_TEXTURE_2D), デプスバッファ無し
	 * @param isGLES3
	 * @param texUnit
	 * @param width
	 * @param height
	 */
	@SuppressLint("NewApi")
	public static GLSurface newInstance(final boolean isGLES3,
		@TexUnit final int texUnit,
		final int width, final int height) {

		if (isGLES3 && (GLUtils.getSupportedGLVersion() > 2)) {
			return new GLSurfaceES3(GLES30.GL_TEXTURE_2D, texUnit, GL_NO_TEXTURE,
				width, height,
				false, DEFAULT_ADJUST_POWER2);
		} else {
			return new GLSurfaceES2(GLES20.GL_TEXTURE_2D, texUnit, GL_NO_TEXTURE,
				width, height,
				false, DEFAULT_ADJUST_POWER2);
		}
	}

	/**
	 * インスタンス生成のヘルパーメソッド(GL_TEXTURE_2D)をwrapするためのコンストラクタ
	 * @param isGLES3
	 * @param texUnit
	 * @param width
	 * @param height
	 * @param useDepthBuffer
	 */
	@SuppressLint("NewApi")
	public static GLSurface newInstance(final boolean isGLES3,
		@TexUnit final int texUnit,
		final int width, final int height, final boolean useDepthBuffer) {

		if (isGLES3 && (GLUtils.getSupportedGLVersion() > 2)) {
			return new GLSurfaceES3(GLES30.GL_TEXTURE_2D, texUnit, GL_NO_TEXTURE,
				width, height,
				useDepthBuffer, DEFAULT_ADJUST_POWER2);
		} else {
			return new GLSurfaceES2(GLES20.GL_TEXTURE_2D, texUnit, GL_NO_TEXTURE,
				width, height,
				useDepthBuffer, DEFAULT_ADJUST_POWER2);
		}
	}

	/**
	 * インスタンス生成のヘルパーメソッド(GL_TEXTURE_2D)
	 * @param isGLES3
	 * @param texUnit
	 * @param width
	 * @param height
	 * @param useDepthBuffer
	 * @param adjustPower2
	 */
	@SuppressLint("NewApi")
	public static GLSurface newInstance(final boolean isGLES3,
		@TexUnit final int texUnit,
		final int width, final int height,
		final boolean useDepthBuffer, final boolean adjustPower2) {

		if (isGLES3 && (GLUtils.getSupportedGLVersion() > 2)) {
			return new GLSurfaceES3(GLES30.GL_TEXTURE_2D, texUnit, GL_NO_TEXTURE,
				width, height, useDepthBuffer, adjustPower2);
		} else {
			return new GLSurfaceES2(GLES20.GL_TEXTURE_2D, texUnit, GL_NO_TEXTURE,
				width, height, useDepthBuffer, adjustPower2);
		}
	}

	/**
	 * 既存のテクスチャ(GL_TEXTURE_2D)をwrapするためのインスタンス生成のヘルパーメソッド, デプスバッファなし
	 * @param isGLES3
	 * @param texId
	 * @param texUnit
	 * @param width
	 * @param height
	 */
	@SuppressLint("NewApi")
	public static GLSurface wrap(final boolean isGLES3,
		@TexUnit final int texUnit, final int texId,
		final int width, final int height) {

		if (isGLES3 && (GLUtils.getSupportedGLVersion() > 2)) {
			return new GLSurfaceES3(GLES30.GL_TEXTURE_2D, texUnit, texId,
				width, height,
				false, DEFAULT_ADJUST_POWER2);
		} else {
			return new GLSurfaceES2(GLES20.GL_TEXTURE_2D, texUnit, texId,
				width, height,
				false, DEFAULT_ADJUST_POWER2);
		}
	}

	/**
	 * 既存のテクスチャ(GL_TEXTURE_2D)をwrapするためのインスタンス生成のヘルパーメソッド
	 * @param isGLES3
	 * @param texUnit
	 * @param texId
	 * @param width
	 * @param height
	 * @param useDepthBuffer
	 */
	@SuppressLint("NewApi")
	public static GLSurface wrap(final boolean isGLES3,
		@TexUnit final int texUnit, final int texId,
		final int width, final int height, final boolean useDepthBuffer) {

		if (isGLES3 && (GLUtils.getSupportedGLVersion() > 2)) {
			return new GLSurfaceES3(GLES30.GL_TEXTURE_2D, texUnit, texId,
				width, height,
				useDepthBuffer, DEFAULT_ADJUST_POWER2);
		} else {
			return new GLSurfaceES2(GLES20.GL_TEXTURE_2D, texUnit, texId,
				width, height,
				useDepthBuffer, DEFAULT_ADJUST_POWER2);
		}
	}

	/**
	 * 既存のテクスチャをwrapするためのインスタンス生成のヘルパーメソッド
	 * XXX SurfaceTextureへ割り当てたGL_TEXTURE_EXTERNAL_OESのテクスチャは
	 *     少なくとも1回はSurfaceTexture#updateTexImageを呼び出すまでは
	 *     メモリー割り当てされておらず実際のテクスチャとしてアクセスできない。
	 *     SurfaceTexture#updateTexImageを呼ぶ前にGLSurfaceでラップすると
	 *     assignTextureでフレームバッファーをセットするときにクラッシュするので注意
	 * @param isGLES3
	 * @param texTarget GL_TEXTURE_EXTERNAL_OESかGL_TEXTURE_2D
	 * @param texUnit
	 * @param texId
	 * @param width
	 * @param height
	 * @param useDepthBuffer
	 */
	@SuppressLint("NewApi")
	public static GLSurface wrap(final boolean isGLES3,
		@TexTarget final int texTarget, @TexUnit final int texUnit, final int texId,
		final int width, final int height, final boolean useDepthBuffer) {

		if (isGLES3 && (GLUtils.getSupportedGLVersion() > 2)) {
			return new GLSurfaceES3(texTarget, texUnit, texId,
				width, height,
				useDepthBuffer, DEFAULT_ADJUST_POWER2);
		} else {
			return new GLSurfaceES2(texTarget, texUnit, texId,
				width, height,
				useDepthBuffer, DEFAULT_ADJUST_POWER2);
		}
	}

//================================================================================
	public final boolean isGLES3;
	@TexTarget
	protected final int TEX_TARGET;
	@TexUnit
	protected final int TEX_UNIT;
	protected final boolean mHasDepthBuffer, mAdjustPower2;
	protected boolean mWrappedTexture;
	/** 描画領域サイズ */
	protected int mWidth, mHeight;
	/** テクスチャサイズ */
	protected int mTexWidth, mTexHeight;
	/** オフスクリーンのカラーバッファに使うテクスチャ名 */
	protected int mFBOTexId = GL_NO_TEXTURE;
	/** // オフスクリーン用のバッファオブジェクト */
	protected int mDepthBufferObj = GL_NO_BUFFER;
	protected int mFrameBufferObj = GL_NO_BUFFER;
	/** テクスチャ座標変換行列 */
	@Size(value=16)
	@NonNull
	protected final float[] mTexMatrix = new float[16];
	protected int viewPortX, viewPortY, viewPortWidth, viewPortHeight;

	/**
	 * 既存のテクスチャをwrapするためのコンストラクタ
	 * @param texTarget GL_TEXTURE_2D
	 * @param texId
	 * @param width
	 * @param height
	 * @param useDepthBuffer
	 * @param adjustPower2
	 */
	@SuppressLint("NewApi")
	private GLSurface(final boolean isGLES3,
		@TexTarget final int texTarget, @TexUnit final int texUnit, final int texId,
		final int width, final int height,
		final boolean useDepthBuffer, final boolean adjustPower2) {

		if (DEBUG) Log.v(TAG, "Constructor");
		this.isGLES3 = isGLES3;
		TEX_TARGET = texTarget;
		TEX_UNIT = texUnit;
		mHasDepthBuffer = useDepthBuffer;
		mAdjustPower2 = adjustPower2;

		createFrameBuffer(width, height);
		int tex = texId;
		if (tex <= GL_NO_TEXTURE) {
			tex = genTexture(texTarget, texUnit, mTexWidth, mTexHeight);
		}
		assignTexture(tex, width, height, null);
		// assignTexture内で強制的にmWrappedTexture=trueになるので正しい値に上書き
		mWrappedTexture = texId > GL_NO_TEXTURE;
//		setViewPort(0, 0, mWidth, mHeight);
	}

//--------------------------------------------------------------------------------
	/**
	 * ISurfaceの実装
	 * 関連するリソースを破棄する
	 */
	@Override
	public void release() {
		if (DEBUG) Log.v(TAG, "release");
		releaseFrameBuffer();
	}

	/**
	 * ISurfaceの実装
	 * @return
	 */
	@Override
	public boolean isValid() {
		return mFrameBufferObj >= 0;
	}

//--------------------------------------------------------------------------------
	@Override
	public boolean isOES() {
		return TEX_TARGET == GL_TEXTURE_EXTERNAL_OES;
	}

	/**
	 * IGLSurfaceの実装
	 * バックバッファとして使っているテクスチャのテクスチャターゲット(GL_TEXTURE_2D等)を取得
	 * @return
	 */
	@TexTarget
	@Override
	public int getTexTarget() {
		return TEX_TARGET;
	}

	/**
	 * IGLSurfaceの実装
	 * バックバッファとして使っているテクスチャのテクスチャユニット(GL_TEXTURE0-GL_TEXTURE31)を取得
	 * @return
	 */
	@Override
	public int getTexUnit() {
		return TEX_UNIT;
	}

	/**
	 * IGLSurfaceの実装
	 * オフスクリーンテクスチャ名を取得
	 * このオフスクリーンへ書き込んだ画像をテクスチャとして使って他の描画を行う場合に使用できる
	 * @return
	 */
	@Override
	public int getTexId() {
		return mFBOTexId;
	}

	/**
	 * IGLSurfaceの実装
	 * 描画領域の幅を取得
	 * @return
	 */
	@Override
	public int getWidth() {
		return mWidth;
	}

	/**
	 * IGLSurfaceの実装
	 * 描画領域の高さを取得
	 * @return
	 */
	@Override
	public int getHeight() {
		return mHeight;
	}

	/**
	 * IGLSurfaceの実装
	 * バックバッファとして使っているテクスチャの実際の幅を取得
	 * 外部からのテクスチャをラップしたときは正しくないかもしれない
	 * @return
	 */
	@Override
	public int getTexWidth() {
		return mTexWidth;
	}

	/**
	 * IGLSurfaceの実装
	 * バックバッファとして使っているテクスチャの実際の高さを取得
	 * 外部からのテクスチャをラップしたときは正しくないかもしれない
	 * @return
	 */
	@Override
	public int getTexHeight() {
		return mTexHeight;
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
	@Size(min=16)
	@NonNull
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
	public void copyTexMatrix(@NonNull @Size(min=16) final float[] matrix, final int offset) {
		System.arraycopy(mTexMatrix, 0, matrix, offset, mTexMatrix.length);
	}

	/**
	 * テクスチャ座標変換行列を取得(内部配列を直接返すので変更時は要注意)
	 * @return
	 */
	@Size(value=16)
	@NonNull
	@Override
	public float[] getTexMatrix() {
		return mTexMatrix;
	}

//--------------------------------------------------------------------------------
	/**
	 * 内部で保持しているテクスチャをバインドする
	 */
	public abstract void bindTexture();

	/**
	 * 指定したテクスチャをこのオフスクリーンに割り当てる
	 * @param texId
	 * @param width
	 * @param height
	 * @param texMatrix
	 */
	public abstract void assignTexture(
		final int texId,
		final int width, final int height,
		@Nullable @Size(min=16) final float[] texMatrix);

	/**
	 * オフスクリーン描画用のフレームバッファオブジェクトを生成する
	 * @param width
	 * @param height
	 */
	protected abstract void createFrameBuffer(final int width, final int height);

	/**
	 * オフスクリーンフレームバッファを破棄
	 */
    protected abstract void releaseFrameBuffer();

	protected abstract int genTexture(
		@TexTarget final int texTarget, @TexUnit final int texUnit,
		final int texWidth, final int texHeight);

	/**
	 * OpenGL|ES2用のGLSurface実装
	 */
	private static class GLSurfaceES2 extends GLSurface {
		/**
		 * 既存のテクスチャをwrapするためのコンストラクタ
		 *
		 * @param texTarget       GL_TEXTURE_2D
		 * @param texUnit
		 * @param texId
		 * @param width
		 * @param height
		 * @param useDepthBuffer
		 * @param adjustPower2
		 */
		private GLSurfaceES2(
			@TexTarget final int texTarget, @TexUnit final int texUnit, final int texId,
			final int width, final int height,
			final boolean useDepthBuffer, final boolean adjustPower2) {

			super(false,
				texTarget, texUnit, texId,
				width, height,
				useDepthBuffer, adjustPower2);
		}

		@Override
		public void bindTexture() {
			GLES20.glActiveTexture(TEX_UNIT);
			GLES20.glBindTexture(TEX_TARGET, mFBOTexId);
		}

		/**
		 * ISurfaceの実装
		 * オフスクリーン描画用のレンダリングバッファに切り替える
		 * Viewportも変更になるので必要であればunbind後にViewportの設定をすること
		 */
		@Override
		public void makeCurrent() {
			if (DEBUG) Log.v(TAG, "makeCurrent:");
			GLES20.glActiveTexture(TEX_UNIT);
			GLES20.glBindTexture(TEX_TARGET, mFBOTexId);
			GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBufferObj);
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
		 * ISurfaceの実装
		 */
		@Override
		public void swap() {
			if (DEBUG) Log.v(TAG, "swap:");
			GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
			GLES20.glActiveTexture(TEX_UNIT);
			GLES20.glBindTexture(TEX_TARGET, 0);
		}

		/**
		 * 指定したテクスチャをこのオフスクリーンに割り当てる
		 * @param texId
		 * @param width
		 * @param height
		 * @param texMatrix
		 */
		@Override
		public void assignTexture(
			final int texId,
			final int width, final int height,
			@Nullable @Size(min=16) final float[] texMatrix) {

			if ((width > mTexWidth) || (height > mTexHeight)) {
				releaseFrameBuffer();
				createFrameBuffer(width, height);
			}
			if (!mWrappedTexture && (mFBOTexId > GL_NO_TEXTURE)) {
				GLUtils.deleteTex(mFBOTexId);
			}
			mWrappedTexture = true;
			mFBOTexId = texId;
			GLES20.glActiveTexture(TEX_UNIT);
			 // フレームバッファオブジェクトをbindする
			GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBufferObj);
			GLUtils.checkGlError("glBindFramebuffer " + mFrameBufferObj);
			// フレームバッファにカラーバッファ(テクスチャ)を接続する
			GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
				TEX_TARGET, mFBOTexId, 0);
			GLUtils.checkGlError("glFramebufferTexture2D");

			if (mHasDepthBuffer) {
				// フレームバッファにデプスバッファを接続する
				GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER,
					GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_RENDERBUFFER, mDepthBufferObj);
				GLUtils.checkGlError("glFramebufferRenderbuffer");
			}

			// 正常に終了したかどうかを確認する
			final int status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
			if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
				throw new RuntimeException("Framebuffer not complete, status=" + status);
			}

			 // デフォルトのフレームバッファに戻す
			GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

			if (texMatrix != null) {
				System.arraycopy(texMatrix, 0, mTexMatrix, 0, 16);
			} else {
				// テクスチャ座標変換行列を初期化
				Matrix.setIdentityM(mTexMatrix, 0);
				mTexMatrix[0] = width / (float)mTexWidth;
				mTexMatrix[5] = height / (float)mTexHeight;
			}
			setViewPort(0, 0, mWidth, mHeight);
		}

		/**
		 * Bitmapから画像をテクスチャに読み込む
		 * @param bitmap
		 */
		@Override
		public void loadBitmap(@NonNull final Bitmap bitmap) {
			final int width = bitmap.getWidth();
			final int height = bitmap.getHeight();
			if ((width > mTexWidth) || (height > mTexHeight)) {
				releaseFrameBuffer();
				createFrameBuffer(width, height);
			}
			GLES20.glActiveTexture(TEX_UNIT);
			GLES20.glBindTexture(TEX_TARGET, mFBOTexId);
			android.opengl.GLUtils.texImage2D(TEX_TARGET, 0, bitmap, 0);
			GLES20.glBindTexture(TEX_TARGET, 0);
			// initialize texture matrix
			Matrix.setIdentityM(mTexMatrix, 0);
			mTexMatrix[0] = width / (float)mTexWidth;
			mTexMatrix[5] = height / (float)mTexHeight;
		}

		/**
		 * オフスクリーン描画用のフレームバッファオブジェクトを生成する
		 * @param width
		 * @param height
		 */
		@Override
		protected void createFrameBuffer(final int width, final int height) {
			final int[] ids = new int[1];

			if (mAdjustPower2) {
				// テクスチャのサイズは2の乗数にする
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
			if (mHasDepthBuffer) {
				// デプスバッファが必要な場合は、レンダーバッファオブジェクトを生成・初期化する
				GLES20.glGenRenderbuffers(1, ids, 0);
				mDepthBufferObj = ids[0];
				GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, mDepthBufferObj);
				// デプスバッファは16ビット
				GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER,
					GLES20.GL_DEPTH_COMPONENT16, mTexWidth, mTexHeight);
			}
			// フレームバッファオブジェクトを生成してbindする
			GLES20.glGenFramebuffers(1, ids, 0);
			GLUtils.checkGlError("glGenFramebuffers");
			mFrameBufferObj = ids[0];
			GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBufferObj);
			GLUtils.checkGlError("glBindFramebuffer " + mFrameBufferObj);

			// デフォルトのフレームバッファに戻す
			GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

		}

		/**
		 * オフスクリーンフレームバッファを破棄
		 */
		@Override
		protected void releaseFrameBuffer() {
			final int[] names = new int[1];
			// デプスバッファがある時はデプスバッファを破棄
			if (mDepthBufferObj > GL_NO_BUFFER) {
				names[0] = mDepthBufferObj;
				GLES20.glDeleteRenderbuffers(1, names, 0);
				mDepthBufferObj = GL_NO_BUFFER;
			}
			// オフスクリーンのカラーバッファ用のテクスチャを破棄
			if (!mWrappedTexture && (mFBOTexId > GL_NO_TEXTURE)) {
				GLUtils.deleteTex(mFBOTexId);
				mFBOTexId = GL_NO_TEXTURE;
			}
			// オフスクリーンのフレームバッファーオブジェクトを破棄
			if (mFrameBufferObj > GL_NO_BUFFER) {
				names[0] = mFrameBufferObj;
				GLES20.glDeleteFramebuffers(1, names, 0);
				mFrameBufferObj = GL_NO_BUFFER;
			}
		}

		/**
		 * カラーバッファのためにテクスチャを生成する
		 * @param texTarget
		 * @param texUnit
		 * @param texWidth
		 * @param texHeight
		 * @return
		 */
		protected int genTexture(final int texTarget, final int texUnit,
			final int texWidth, final int texHeight) {
			// カラーバッファのためにテクスチャを生成する
			final int texId = GLUtils.initTex(texTarget, texUnit,
				GLES20.GL_LINEAR, GLES20.GL_LINEAR, GLES20.GL_CLAMP_TO_EDGE);
			// テクスチャのメモリ領域を確保する
			GLES20.glTexImage2D(texTarget, 0, GLES20.GL_RGBA, texWidth, texHeight, 0,
				GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
			GLUtils.checkGlError("glTexImage2D");
			mWrappedTexture = false;
			return texId;
		}
	}	// GLSurfaceGLES2

	/**
	 * OpenGL|ES3用のGLSurface実装
	 */
	private static class GLSurfaceES3 extends GLSurface {
		/**
		 * 既存のテクスチャをwrapするためのコンストラクタ
		 *
		 * @param texTarget       GL_TEXTURE_2D
		 * @param texUnit
		 * @param texId
		 * @param width
		 * @param height
		 * @param useDepthBuffer
		 * @param adjustPower2
		 */
		private GLSurfaceES3(
			@TexTarget final int texTarget, @TexUnit final int texUnit, final int texId,
			final int width, final int height,
			final boolean useDepthBuffer, final boolean adjustPower2) {

			super(true,
				texTarget, texUnit, texId,
				width, height,
				useDepthBuffer, adjustPower2);
		}

		@Override
		public void bindTexture() {
			GLES30.glActiveTexture(TEX_UNIT);
			GLES30.glBindTexture(TEX_TARGET, mFBOTexId);
		}

		/**
		 * ISurfaceの実装
		 * オフスクリーン描画用のレンダリングバッファに切り替える
		 * Viewportも変更になるので必要であればunbind後にViewportの設定をすること
		 */
		@Override
		public void makeCurrent() {
			if (DEBUG) Log.v(TAG, "makeCurrent:");
			GLES30.glActiveTexture(TEX_UNIT);
			GLES30.glBindTexture(TEX_TARGET, mFBOTexId);
			GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, mFrameBufferObj);
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

			GLES30.glViewport(x, y, width, height);
		}

		/**
		 * ISurfaceの実装
		 */
		@Override
		public void swap() {
			if (DEBUG) Log.v(TAG, "swap:");
			GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0);
			GLES30.glActiveTexture(TEX_UNIT);
			GLES30.glBindTexture(TEX_TARGET, 0);
		}

		/**
		 * 指定したテクスチャをこのオフスクリーンに割り当てる
		 * @param texId
		 * @param width
		 * @param height
		 * @param texMatrix
		 */
		@Override
		public void assignTexture(
			final int texId,
			final int width, final int height,
			@Nullable @Size(min=16) final float[] texMatrix) {

			if ((width > mTexWidth) || (height > mTexHeight)) {
				releaseFrameBuffer();
				createFrameBuffer(width, height);
			}
			if (!mWrappedTexture && (mFBOTexId > GL_NO_TEXTURE)) {
				GLUtils.deleteTex(mFBOTexId);
			}
			mWrappedTexture = true;
			mFBOTexId = texId;
			GLES30.glActiveTexture(TEX_UNIT);
			 // フレームバッファオブジェクトをbindする
			GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, mFrameBufferObj);
			GLUtils.checkGlError("glBindFramebuffer " + mFrameBufferObj);
			// フレームバッファにカラーバッファ(テクスチャ)を接続する
			GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0,
				TEX_TARGET, mFBOTexId, 0);
			GLUtils.checkGlError("glFramebufferTexture2D");

			if (mHasDepthBuffer) {
				// フレームバッファにデプスバッファを接続する
				GLES30.glFramebufferRenderbuffer(GLES30.GL_FRAMEBUFFER,
					GLES30.GL_DEPTH_ATTACHMENT, GLES30.GL_RENDERBUFFER, mDepthBufferObj);
				GLUtils.checkGlError("glFramebufferRenderbuffer");
			}

			// 正常に終了したかどうかを確認する
			final int status = GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER);
			if (status != GLES30.GL_FRAMEBUFFER_COMPLETE) {
				throw new RuntimeException("Framebuffer not complete, status=" + status);
			}

			 // デフォルトのフレームバッファに戻す
			GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0);

			if (texMatrix != null) {
				System.arraycopy(texMatrix, 0, mTexMatrix, 0, 16);
			} else {
				// テクスチャ座標変換行列を初期化
				Matrix.setIdentityM(mTexMatrix, 0);
				mTexMatrix[0] = width / (float)mTexWidth;
				mTexMatrix[5] = height / (float)mTexHeight;
			}
			setViewPort(0, 0, mWidth, mHeight);
		}

		/**
		 * Bitmapから画像をテクスチャに読み込む
		 * @param bitmap
		 */
		@Override
		public void loadBitmap(@NonNull final Bitmap bitmap) {
			final int width = bitmap.getWidth();
			final int height = bitmap.getHeight();
			if ((width > mTexWidth) || (height > mTexHeight)) {
				releaseFrameBuffer();
				createFrameBuffer(width, height);
			}
			GLES30.glActiveTexture(TEX_UNIT);
			GLES30.glBindTexture(TEX_TARGET, mFBOTexId);
			android.opengl.GLUtils.texImage2D(TEX_TARGET, 0, bitmap, 0);
			GLES30.glBindTexture(TEX_TARGET, 0);
			// initialize texture matrix
			Matrix.setIdentityM(mTexMatrix, 0);
			mTexMatrix[0] = width / (float)mTexWidth;
			mTexMatrix[5] = height / (float)mTexHeight;
		}

		/**
		 * オフスクリーン描画用のフレームバッファオブジェクトを生成する
		 * @param width
		 * @param height
		 */
		@Override
		protected void createFrameBuffer(final int width, final int height) {
			final int[] ids = new int[1];

			if (mAdjustPower2) {
				// テクスチャのサイズは2の乗数にする
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

			if (mHasDepthBuffer) {
				// デプスバッファが必要な場合は、レンダーバッファオブジェクトを生成・初期化する
				GLES30.glGenRenderbuffers(1, ids, 0);
				mDepthBufferObj = ids[0];
				GLES30.glBindRenderbuffer(GLES30.GL_RENDERBUFFER, mDepthBufferObj);
				// デプスバッファは16ビット
				GLES30.glRenderbufferStorage(GLES30.GL_RENDERBUFFER,
					GLES30.GL_DEPTH_COMPONENT16, mTexWidth, mTexHeight);
			}
			// フレームバッファオブジェクトを生成してbindする
			GLES30.glGenFramebuffers(1, ids, 0);
			GLUtils.checkGlError("glGenFramebuffers");
			mFrameBufferObj = ids[0];
			GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, mFrameBufferObj);
			GLUtils.checkGlError("glBindFramebuffer " + mFrameBufferObj);

			// デフォルトのフレームバッファに戻す
			GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0);

		}

		/**
		 * オフスクリーンフレームバッファを破棄
		 */
		@Override
		protected void releaseFrameBuffer() {
			final int[] names = new int[1];
			// デプスバッファがある時はデプスバッファを破棄
			if (mDepthBufferObj > GL_NO_BUFFER) {
				names[0] = mDepthBufferObj;
				GLES30.glDeleteRenderbuffers(1, names, 0);
				mDepthBufferObj = GL_NO_BUFFER;
			}
			// オフスクリーンのカラーバッファ用のテクスチャを破棄
			if (!mWrappedTexture && (mFBOTexId > GL_NO_TEXTURE)) {
				GLUtils.deleteTex(mFBOTexId);
				mFBOTexId = GL_NO_TEXTURE;
			}
			// オフスクリーンのフレームバッファーオブジェクトを破棄
			if (mFrameBufferObj > GL_NO_BUFFER) {
				names[0] = mFrameBufferObj;
				GLES30.glDeleteFramebuffers(1, names, 0);
				mFrameBufferObj = GL_NO_BUFFER;
			}
		}

		/**
		 * カラーバッファのためにテクスチャを生成する
		 * @param texTarget
		 * @param texUnit
		 * @param texWidth
		 * @param texHeight
		 * @return
		 */
		@Override
		protected int genTexture(
			@TexTarget final int texTarget, @TexUnit final int texUnit,
			final int texWidth, final int texHeight) {
			// カラーバッファのためにテクスチャを生成する
			final int texId = GLUtils.initTex(texTarget, texUnit,
				GLES30.GL_LINEAR, GLES30.GL_LINEAR, GLES30.GL_CLAMP_TO_EDGE);
			// テクスチャのメモリ領域を確保する
			GLES30.glTexImage2D(texTarget, 0, GLES30.GL_RGBA, texWidth, texHeight, 0,
				GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null);
			GLUtils.checkGlError("glTexImage2D");
			mWrappedTexture = false;
			return texId;
		}
	}	// GLSurfaceGLES3
}
