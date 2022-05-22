package com.serenegiant.gl;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2022 saki t_saki@serenegiant.com
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
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.Size;

/**
 * テクスチャへOpenGL|ESで描画するためのオフスクリーン描画クラス
 * テクスチャをカラーバッファとしてFBOに割り当てる
 * テクスチャへのオフスクリーン描画しないのであればGLTextureを使う方が良い
 */
public abstract class GLSurface implements IGLSurface {
	private static final boolean DEBUG = false;
	private static final String TAG = "GLSurface";

	private static final boolean DEFAULT_ADJUST_POWER2 = false;

	/**
	 * インスタンス生成のヘルパーメソッド(GL_TEXTURE_2D), デプスバッファ無し
	 * テクスチャユニットはGL_TEXTURE0
	 * @param isGLES3
	 * @param width
	 * @param height
	 */
	@SuppressLint("NewApi")
	public static GLSurface newInstance(final boolean isGLES3,
		final int width, final int height) {

		if (isGLES3 && (GLUtils.getSupportedGLVersion() > 2)) {
			return new GLSurfaceES3(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE0, GL_NO_TEXTURE,
				width, height, false, DEFAULT_ADJUST_POWER2);
		} else {
			return new GLSurfaceES2(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE0, GL_NO_TEXTURE,
				width, height, false, DEFAULT_ADJUST_POWER2);
		}
	}

	/**
	 * インスタンス生成のヘルパーメソッド(GL_TEXTURE_2D), デプスバッファ無し
	 * テクスチャユニットはGL_TEXTURE0
	 * @param isGLES3
	 * @param tex_unit
	 * @param width
	 * @param height
	 */
	@SuppressLint("NewApi")
	public static GLSurface newInstance(final boolean isGLES3,
		@TexUnit final int tex_unit,
		final int width, final int height) {

		if (isGLES3 && (GLUtils.getSupportedGLVersion() > 2)) {
			return new GLSurfaceES3(GLES30.GL_TEXTURE_2D, tex_unit, GL_NO_TEXTURE,
				width, height,
				false, DEFAULT_ADJUST_POWER2);
		} else {
			return new GLSurfaceES2(GLES20.GL_TEXTURE_2D, tex_unit, GL_NO_TEXTURE,
				width, height,
				false, DEFAULT_ADJUST_POWER2);
		}
	}

	/**
	 * インスタンス生成のヘルパーメソッド(GL_TEXTURE_2D)
	 * テクスチャユニットはGL_TEXTURE0
	 * @param isGLES3
	 * @param width dimension of offscreen(width)
	 * @param height dimension of offscreen(height)
	 * @param use_depth_buffer set true if you use depth buffer. the depth is fixed as 16bits
	 */
	@SuppressLint("NewApi")
	public static GLSurface newInstance(final boolean isGLES3,
		final int width, final int height,
		final boolean use_depth_buffer) {

		if (isGLES3 && (GLUtils.getSupportedGLVersion() > 2)) {
			return new GLSurfaceES3(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE0, GL_NO_TEXTURE,
				width, height, use_depth_buffer, DEFAULT_ADJUST_POWER2);
		} else {
			return new GLSurfaceES2(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE0, GL_NO_TEXTURE,
				width, height, use_depth_buffer, DEFAULT_ADJUST_POWER2);
		}
	}

	/**
	 * インスタンス生成のヘルパーメソッド(GL_TEXTURE_2D)をwrapするためのコンストラクタ
	 * テクスチャユニットはGL_TEXTURE0
	 * @param isGLES3
	 * @param tex_unit
	 * @param width
	 * @param height
	 * @param use_depth_buffer
	 */
	@SuppressLint("NewApi")
	public static GLSurface newInstance(final boolean isGLES3,
		@TexUnit final int tex_unit,
		final int width, final int height, final boolean use_depth_buffer) {

		if (isGLES3 && (GLUtils.getSupportedGLVersion() > 2)) {
			return new GLSurfaceES3(GLES30.GL_TEXTURE_2D, tex_unit, GL_NO_TEXTURE,
				width, height,
				use_depth_buffer, DEFAULT_ADJUST_POWER2);
		} else {
			return new GLSurfaceES2(GLES20.GL_TEXTURE_2D, tex_unit, GL_NO_TEXTURE,
				width, height,
				use_depth_buffer, DEFAULT_ADJUST_POWER2);
		}
	}

	/**
	 * インスタンス生成のヘルパーメソッド(GL_TEXTURE_2D)
	 * テクスチャユニットはGL_TEXTURE0
	 * @param isGLES3
	 * @param width
	 * @param height
	 * @param use_depth_buffer
	 * @param adjust_power2
	 */
	@SuppressLint("NewApi")
	public static GLSurface newInstance(final boolean isGLES3,
		final int width, final int height,
		final boolean use_depth_buffer, final boolean adjust_power2) {

		if (isGLES3 && (GLUtils.getSupportedGLVersion() > 2)) {
			return new GLSurfaceES3(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE0, GL_NO_TEXTURE,
				width, height, use_depth_buffer, adjust_power2);
		} else {
			return new GLSurfaceES2(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE0, GL_NO_TEXTURE,
				width, height, use_depth_buffer, adjust_power2);
		}
	}

	/**
	 * インスタンス生成のヘルパーメソッド(GL_TEXTURE_2D)
	 * @param isGLES3
	 * @param tex_unit
	 * @param width
	 * @param height
	 * @param use_depth_buffer
	 * @param adjust_power2
	 */
	@SuppressLint("NewApi")
	public static GLSurface newInstance(final boolean isGLES3,
		@TexUnit final int tex_unit,
		final int width, final int height,
		final boolean use_depth_buffer, final boolean adjust_power2) {

		if (isGLES3 && (GLUtils.getSupportedGLVersion() > 2)) {
			return new GLSurfaceES3(GLES30.GL_TEXTURE_2D, tex_unit, GL_NO_TEXTURE,
				width, height, use_depth_buffer, adjust_power2);
		} else {
			return new GLSurfaceES2(GLES20.GL_TEXTURE_2D, tex_unit, GL_NO_TEXTURE,
				width, height, use_depth_buffer, adjust_power2);
		}
	}

	/**
	 * 既存のテクスチャ(GL_TEXTURE_2D)をwrapするためのインスタンス生成のヘルパーメソッド, デプスバッファなし
	 * @param isGLES3
	 * @param tex_id
	 * @param tex_unit
	 * @param width
	 * @param height
	 */
	@SuppressLint("NewApi")
	public static GLSurface wrap(final boolean isGLES3,
		@TexUnit final int tex_unit, final int tex_id,
		final int width, final int height) {

		if (isGLES3 && (GLUtils.getSupportedGLVersion() > 2)) {
			return new GLSurfaceES3(GLES30.GL_TEXTURE_2D, tex_unit, tex_id,
				width, height,
				false, DEFAULT_ADJUST_POWER2);
		} else {
			return new GLSurfaceES2(GLES20.GL_TEXTURE_2D, tex_unit, tex_id,
				width, height,
				false, DEFAULT_ADJUST_POWER2);
		}
	}

	/**
	 * 既存のテクスチャ(GL_TEXTURE_2D)をwrapするためのインスタンス生成のヘルパーメソッド
	 * @param isGLES3
	 * @param tex_unit
	 * @param tex_id
	 * @param width
	 * @param height
	 * @param use_depth_buffer
	 */
	@SuppressLint("NewApi")
	public static GLSurface wrap(final boolean isGLES3,
		@TexUnit final int tex_unit, final int tex_id,
		final int width, final int height, final boolean use_depth_buffer) {

		if (isGLES3 && (GLUtils.getSupportedGLVersion() > 2)) {
			return new GLSurfaceES3(GLES30.GL_TEXTURE_2D, tex_unit, tex_id,
				width, height,
				use_depth_buffer, DEFAULT_ADJUST_POWER2);
		} else {
			return new GLSurfaceES2(GLES20.GL_TEXTURE_2D, tex_unit, tex_id,
				width, height,
				use_depth_buffer, DEFAULT_ADJUST_POWER2);
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
	 * @param tex_target GL_TEXTURE_EXTERNAL_OESかGL_TEXTURE_2D
	 * @param tex_unit
	 * @param tex_id
	 * @param width
	 * @param height
	 * @param use_depth_buffer
	 */
	@SuppressLint("NewApi")
	public static GLSurface wrap(final boolean isGLES3,
		@TexTarget final int tex_target, @TexUnit final int tex_unit, final int tex_id,
		final int width, final int height, final boolean use_depth_buffer) {

		if (isGLES3 && (GLUtils.getSupportedGLVersion() > 2)) {
			return new GLSurfaceES3(tex_target, tex_unit, tex_id,
				width, height,
				use_depth_buffer, DEFAULT_ADJUST_POWER2);
		} else {
			return new GLSurfaceES2(tex_target, tex_unit, tex_id,
				width, height,
				use_depth_buffer, DEFAULT_ADJUST_POWER2);
		}
	}

//================================================================================
	protected final boolean isGLES3;
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
	protected int mDepthBufferObj = GL_NO_BUFFER, mFrameBufferObj = GL_NO_BUFFER;
	/** テクスチャ座標変換行列 */
	@Size(min=16)
	@NonNull
	protected final float[] mTexMatrix = new float[16];
	protected int viewPortX, viewPortY, viewPortWidth, viewPortHeight;

	/**
	 * 既存のテクスチャをwrapするためのコンストラクタ
	 * @param tex_target GL_TEXTURE_2D
	 * @param tex_id
	 * @param width
	 * @param height
	 * @param use_depth_buffer
	 * @param adjust_power2
	 */
	@SuppressLint("NewApi")
	private GLSurface(final boolean isGLES3,
		@TexTarget final int tex_target, @TexUnit final int tex_unit, final int tex_id,
		final int width, final int height,
		final boolean use_depth_buffer, final boolean adjust_power2) {

		if (DEBUG) Log.v(TAG, "Constructor");
		this.isGLES3 = isGLES3;
		TEX_TARGET = tex_target;
		TEX_UNIT = tex_unit;
		mHasDepthBuffer = use_depth_buffer;
		mAdjustPower2 = adjust_power2;

		createFrameBuffer(width, height);
		int tex = tex_id;
		if (tex < 0) {
			tex = genTexture(tex_target, tex_unit, mTexWidth, mTexHeight);
		}
		assignTexture(tex, width, height);
		// assignTexture内で強制的にmWrappedTexture=trueになるので正しい値に上書き
		mWrappedTexture = tex_id > GL_NO_TEXTURE;
		setViewPort(0, 0, mWidth, mHeight);
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
	 * @return
	 */
	@Override
	public int getTexWidth() {
		return mTexWidth;
	}

	/**
	 * IGLSurfaceの実装
	 * バックバッファとして使っているテクスチャの実際の高さを取得
	 * @return
	 */
	@Override
	public int getTexHeight() {
		return mTexHeight;
	}

	/**
	 * #copyTexMatrix()の返り値用のfloat配列
	 */
	@Size(min=16)
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
	@Size(min=16)
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
	 * @param texture_name
	 * @param width
	 * @param height
	 */
	public abstract void assignTexture(final int texture_name,
		final int width, final int height);

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

	protected abstract int genTexture(final int tex_target, final int tex_unit,
		final int tex_width, final int tex_height);

	/**
	 * OpenGL|ES2用のGLSurface実装
	 */
	private static class GLSurfaceES2 extends GLSurface {
		/**
		 * 既存のテクスチャをwrapするためのコンストラクタ
		 *
		 * @param tex_target       GL_TEXTURE_2D
		 * @param tex_unit
		 * @param tex_id
		 * @param width
		 * @param height
		 * @param use_depth_buffer
		 * @param adjust_power2
		 */
		private GLSurfaceES2(
			@TexTarget final int tex_target, @TexUnit final int tex_unit, final int tex_id,
			final int width, final int height,
			final boolean use_depth_buffer, final boolean adjust_power2) {

			super(false,
				tex_target, tex_unit, tex_id,
				width, height,
				use_depth_buffer, adjust_power2);
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
		 * @param texture_name
		 * @param width
		 * @param height
		 */
		@Override
		public void assignTexture(final int texture_name,
			final int width, final int height) {

			if ((width > mTexWidth) || (height > mTexHeight)) {
				releaseFrameBuffer();
				createFrameBuffer(width, height);
			}
			if (!mWrappedTexture && (mFBOTexId > GL_NO_TEXTURE)) {
				GLUtils.deleteTex(mFBOTexId);
			}
			mWrappedTexture = true;
			mFBOTexId = texture_name;
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

			// テクスチャ座標変換行列を初期化
			Matrix.setIdentityM(mTexMatrix, 0);
			mTexMatrix[0] = width / (float)mTexWidth;
			mTexMatrix[5] = height / (float)mTexHeight;
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
		 * @param tex_target
		 * @param tex_unit
		 * @param tex_width
		 * @param tex_height
		 * @return
		 */
		protected int genTexture(final int tex_target, final int tex_unit,
			final int tex_width, final int tex_height) {
			// カラーバッファのためにテクスチャを生成する
			final int tex_name = GLUtils.initTex(tex_target, tex_unit,
				GLES20.GL_LINEAR, GLES20.GL_LINEAR, GLES20.GL_CLAMP_TO_EDGE);
			// テクスチャのメモリ領域を確保する
			GLES20.glTexImage2D(tex_target, 0, GLES20.GL_RGBA, tex_width, tex_height, 0,
				GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
			GLUtils.checkGlError("glTexImage2D");
			mWrappedTexture = false;
			return tex_name;
		}
	}	// GLSurfaceGLES2

	/**
	 * OpenGL|ES3用のGLSurface実装
	 */
	@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
	private static class GLSurfaceES3 extends GLSurface {
		/**
		 * 既存のテクスチャをwrapするためのコンストラクタ
		 *
		 * @param tex_target       GL_TEXTURE_2D
		 * @param tex_unit
		 * @param tex_id
		 * @param width
		 * @param height
		 * @param use_depth_buffer
		 * @param adjust_power2
		 */
		private GLSurfaceES3(
			@TexTarget final int tex_target, @TexUnit final int tex_unit, final int tex_id,
			final int width, final int height,
			final boolean use_depth_buffer, final boolean adjust_power2) {

			super(true,
				tex_target, tex_unit, tex_id,
				width, height,
				use_depth_buffer, adjust_power2);
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
		 * @param texture_name
		 * @param width
		 * @param height
		 */
		public void assignTexture(final int texture_name,
			final int width, final int height) {

			if ((width > mTexWidth) || (height > mTexHeight)) {
				releaseFrameBuffer();
				createFrameBuffer(width, height);
			}
			if (!mWrappedTexture && (mFBOTexId > GL_NO_TEXTURE)) {
				GLUtils.deleteTex(mFBOTexId);
			}
			mWrappedTexture = true;
			mFBOTexId = texture_name;
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

			// テクスチャ座標変換行列を初期化
			Matrix.setIdentityM(mTexMatrix, 0);
			mTexMatrix[0] = width / (float)mTexWidth;
			mTexMatrix[5] = height / (float)mTexHeight;
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
		 * @param tex_target
		 * @param tex_unit
		 * @param tex_width
		 * @param tex_height
		 * @return
		 */
		protected int genTexture(final int tex_target, final int tex_unit,
			final int tex_width, final int tex_height) {
			// カラーバッファのためにテクスチャを生成する
			final int tex_name = GLUtils.initTex(tex_target, tex_unit,
				GLES30.GL_LINEAR, GLES30.GL_LINEAR, GLES30.GL_CLAMP_TO_EDGE);
			// テクスチャのメモリ領域を確保する
			GLES30.glTexImage2D(tex_target, 0, GLES30.GL_RGBA, tex_width, tex_height, 0,
				GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null);
			GLUtils.checkGlError("glTexImage2D");
			mWrappedTexture = false;
			return tex_name;
		}
	}	// GLSurfaceGLES3
}
