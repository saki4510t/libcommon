package com.serenegiant.mediaeffect;
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

import android.util.Log;

import com.serenegiant.gl.GLSurface;
import com.serenegiant.glutils.IMirror;

import androidx.annotation.NonNull;

import static com.serenegiant.gl.ShaderConst.*;

/**
 * OpenGL|ES2のフラグメントシェーダーで映像効果を与える時の基本クラス
 */
public class MediaEffectGLBase implements IMediaEffect, IMirror {
	private static final boolean DEBUG = false;
	private static final String TAG = "MediaEffectGLBase";

	protected volatile boolean mEnabled = true;
	@NonNull
	protected final MediaEffectGLDrawer mDrawer;

	/**
	 * フラグメントシェーダーを指定する場合のコンストラクタ(頂点シェーダーはデフォルトを使用)
	 * @param numTex
	 * @param shader
	 */
	public MediaEffectGLBase(final int numTex, final String shader) {
		this(MediaEffectGLDrawer.newInstance(numTex, false, VERTEX_SHADER_ES2, shader));
	}

	/**
	 * フラグメントシェーダーを指定する場合のコンストラクタ(頂点シェーダーはデフォルトを使用)
	 * @param numTex
	 * @param shader
	 */
	public MediaEffectGLBase(
		final int numTex,
		final boolean isOES, final String shader) {

		this(MediaEffectGLDrawer.newInstance(numTex, isOES, VERTEX_SHADER_ES2, shader));
	}

	/**
	 * 頂点シェーダーとフラグメントシェーダーを指定する場合のコンストラクタ
	 * @param numTex
	 * @param vss
	 * @param fss
	 */
	public MediaEffectGLBase(
		final int numTex,
		final boolean isOES, final String vss, final String fss) {

		this(MediaEffectGLDrawer.newInstance(numTex, isOES, vss, fss));
	}
	
	/**
	 * コンストラクタ
	 * @param drawer
	 */
	public MediaEffectGLBase(@NonNull final MediaEffectGLDrawer drawer) {
		mDrawer = drawer;
//		resize(256, 256);
	}

	@Override
	public void release() {
		if (DEBUG) Log.v(TAG, "release:");
		mDrawer.release();
	}

	/**
	 * モデルビュー変換行列を取得(内部配列を直接返すので変更時は要注意)
	 * @return
	 */
	public float[] getMvpMatrix() {
		return mDrawer.getMvpMatrix();
	}

	/**
	 * モデルビュー変換行列に行列を割り当てる
	 * @param matrix 領域チェックしていないのでoffsetから16個以上必須
	 * @param offset
	 * @return
	 */
	public MediaEffectGLBase setMvpMatrix(final float[] matrix, final int offset) {
		mDrawer.setMvpMatrix(matrix, offset);
		return this;
	}

	/**
	 * モデルビュー変換行列のコピーを取得
	 * @param matrix 領域チェックしていないのでoffsetから16個以上必須
	 * @param offset
	 */
	public void getMvpMatrix(final float[] matrix, final int offset) {
		mDrawer.getMvpMatrix(matrix, offset);
	}

	@Override
	public void setMirror(@MirrorMode final int mirror) {
		mDrawer.setMirror(mirror);
	}

	@MirrorMode
	@Override
	public int getMirror() {
		return mDrawer.getMirror();
	}

	@Override
	public MediaEffectGLBase resize(final int width, final int height) {
		if (mDrawer != null) {
			mDrawer.setTexSize(width, height);
		}
		return this;
	}

	@Override
	public boolean enabled() {
		return mEnabled;
	}

	@Override
	public IMediaEffect setEnable(final boolean enable) {
		mEnabled = enable;
		return this;
	}

	/**
	 * if your source texture comes from ISource,
	 * please use this method instead of #apply(final int [], int, int, int)
	 * @param src
	 */
	@Override
	public void apply(@NonNull final ISource src) {
		if (!mEnabled) return;
		final GLSurface output = src.getOutputTargetTexture();
		final int[] srcTexIds = src.getSourceTexId();
		output.makeCurrent();
		try {
			mDrawer.apply(srcTexIds, src.getTexMatrix(), 0);
		} finally {
			output.swap();
		}
	}

	protected int getProgram() {
		return mDrawer.getProgram();
	}

}
