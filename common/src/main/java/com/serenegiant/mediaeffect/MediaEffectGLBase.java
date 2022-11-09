package com.serenegiant.mediaeffect;
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

import androidx.annotation.NonNull;

import android.opengl.GLES20;
import android.util.Log;

import com.serenegiant.gl.GLSurface;

import static com.serenegiant.gl.ShaderConst.*;

/**
 * OpenGL|ES2のフラグメントシェーダーで映像効果を与える時の基本クラス
 */
public class MediaEffectGLBase implements IEffect {
	private static final boolean DEBUG = false;
	private static final String TAG = "MediaEffectGLBase";

	protected GLSurface mOutputOffscreen;
	protected volatile boolean mEnabled = true;

	protected final MediaEffectDrawer mDrawer;

	/**
	 * フラグメントシェーダーを指定する場合のコンストラクタ(頂点シェーダーはデフォルトを使用)
	 * @param numTex
	 * @param shader
	 */
	public MediaEffectGLBase(final int numTex, final String shader) {
		this(MediaEffectDrawer.newInstance(numTex, false, VERTEX_SHADER_ES2, shader));
	}

	/**
	 * フラグメントシェーダーを指定する場合のコンストラクタ(頂点シェーダーはデフォルトを使用)
	 * @param numTex
	 * @param shader
	 */
	public MediaEffectGLBase(final int numTex,
							 final boolean isOES, final String shader) {

		this(MediaEffectDrawer.newInstance(numTex, isOES, VERTEX_SHADER_ES2, shader));
	}

	/**
	 * 頂点シェーダーとフラグメントシェーダーを指定する場合のコンストラクタ
	 * @param numTex
	 * @param vss
	 * @param fss
	 */
	public MediaEffectGLBase(final int numTex,
							 final boolean isOES, final String vss, final String fss) {

		this(MediaEffectDrawer.newInstance(numTex, isOES, vss, fss));
	}
	
	/**
	 * コンストラクタ
	 * @param drawer
	 */
	public MediaEffectGLBase(final MediaEffectDrawer drawer) {
		mDrawer = drawer;
//		resize(256, 256);
	}

	@Override
	public void release() {
		if (DEBUG) Log.v(TAG, "release:");
		mDrawer.release();
		if (mOutputOffscreen != null) {
			mOutputOffscreen.release();
			mOutputOffscreen = null;
		}
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
	public MediaEffectGLBase resize(final int width, final int height) {
		// ISourceを使う時は出力用オフスクリーンは不要なのと
		// ISourceを使わない時は描画時にチェックして生成するのでresize時には生成しないように変更
/*		if ((mOutputOffscreen == null) || (width != mOutputOffscreen.getWidth())
			|| (height != mOutputOffscreen.getHeight())) {
			mOutputOffscreen.release();
			mOutputOffscreen = new GLSurface(width, height, false);
		} */
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
	public IEffect setEnable(final boolean enable) {
		mEnabled = enable;
		return this;
	}

	/**
	 * If you know the source texture came from MediaSource,
	 * using #apply(MediaSource) is much efficient instead of this
	 * @param src_tex_ids
	 * @param width
	 * @param height
	 * @param out_tex_id
	 */
	@Override
	public void apply(@NonNull final int [] src_tex_ids,
		final int width, final int height, final int out_tex_id) {

		if (!mEnabled) return;
		if (mOutputOffscreen == null) {
			mOutputOffscreen = GLSurface.newInstance(false, GLES20.GL_TEXTURE0, width, height, false);
		}
		if ((out_tex_id != mOutputOffscreen.getTexId())
			|| (width != mOutputOffscreen.getWidth())
			|| (height != mOutputOffscreen.getHeight())) {
			mOutputOffscreen.assignTexture(out_tex_id, width, height);
		}
		mOutputOffscreen.makeCurrent();
		try {
			mDrawer.apply(src_tex_ids, mOutputOffscreen.copyTexMatrix(), 0);
		} finally {
			mOutputOffscreen.swap();
		}
	}

	@Override
	public void apply(@NonNull final int [] src_tex_ids,
		@NonNull final GLSurface output) {

		if (!mEnabled) return;
		output.makeCurrent();
		try {
			mDrawer.apply(src_tex_ids, output.copyTexMatrix(), 0);
		} finally {
			output.swap();
		}
	}

	/**
	 * if your source texture comes from ISource,
	 * please use this method instead of #apply(final int [], int, int, int)
	 * @param src
	 */
	@Override
	public void apply(final ISource src) {
		if (!mEnabled) return;
		final GLSurface output_tex = src.getOutputTexture();
		final int[] src_tex_ids = src.getSourceTexId();
		output_tex.makeCurrent();
		try {
			mDrawer.apply(src_tex_ids, output_tex.copyTexMatrix(), 0);
		} finally {
			output_tex.swap();
		}
	}

	protected int getProgram() {
		return mDrawer.getProgram();
	}

}
