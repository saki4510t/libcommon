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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import com.serenegiant.gl.GLDrawer2D;
import com.serenegiant.gl.GLSurface;

public class MediaSource implements ISource {
	private static final boolean DEBUG = false;
	private static final String TAG = "MediaSource";

	public final boolean isGLES3;
	private final int mTexUnit;
	protected GLSurface mSourceScreen;
	protected GLSurface mOutputScreen;
	protected int mWidth, mHeight;
	@NonNull
	protected final int[] mSrcTexIds = new int[1];
	/**
	 * リセット後最初の#apply呼び出しかどうか
	 */
	protected boolean firstApply;

	/**
	 * コンストラクタ
	 * GLコンテキスト内で生成すること
	 */
	public MediaSource(final boolean isGLES3) {
		this(isGLES3, GLES20.GL_TEXTURE4, 1, 1);
	}

	public MediaSource(final boolean isGLES3, final int width, final int height) {
		this(isGLES3, GLES20.GL_TEXTURE4, width, height);
	}

	public MediaSource(final boolean isGLES3, final int texUnit, final int width, final int height) {
		this.isGLES3 = isGLES3;
		mTexUnit = texUnit;
		resize(width, height);
	}

	/**
	 * オフスクリーンを初期状態に戻す
	 * GLコンテキスト内で呼び出すこと
	 * @return
	 */
	@Override
	public ISource reset() {
		firstApply = true;
		mSrcTexIds[0] = mSourceScreen.getTexId();
		Matrix.setIdentityM(mSourceScreen.getTexMatrix(), 0);
		Matrix.setIdentityM(mOutputScreen.getTexMatrix(), 0);
		return this;
	}

	/**
	 * 映像サイズを設定
	 * GLコンテキスト内で呼び出すこと
	 * @param width
	 * @param height
	 * @return
	 */
	@Override
	public ISource resize(final int width, final int height) {
		if (DEBUG) Log.v(TAG, String.format("resize(%d,%d):", width, height));
		if (mWidth != width || mHeight != height) {
			if (mSourceScreen != null) {
				mSourceScreen.release();
				mSourceScreen = null;
			}
			if (mOutputScreen != null) {
				mOutputScreen.release();
				mOutputScreen = null;
			}
			if ((width > 0) && (height > 0)) {
				// FIXME フィルタ処理自体は大丈夫そうなんだけどImageProcessorの処理がおかしくなるので今は2の乗数には丸めない
				// 代わりにImageProcessorの縦横のサイズ自体を2の乗数にする
				mSourceScreen = GLSurface.newInstance(isGLES3, mTexUnit, width, height, false, false);
				mOutputScreen = GLSurface.newInstance(isGLES3, mTexUnit, width, height, false, false);
				mWidth = width;
				mHeight = height;
			}
		}
		return reset();
	}

	/**
	 * IEffectを適用する。1回呼び出す毎に入力と出力のオフスクリーン(テクスチャ)が入れ替わる
	 * GLコンテキスト内で呼び出すこと
	 * @param effect
	 * @return
	 */
	@Override
	public ISource apply(@NonNull final IMediaEffect effect) {
		if (mSourceScreen != null) {
			if (!firstApply) {
				final GLSurface temp = mSourceScreen;
				mSourceScreen = mOutputScreen;
				mOutputScreen = temp;
				mSrcTexIds[0] = mSourceScreen.getTexId();
			}
//			if (DEBUG) Log.i(TAG, "apply:" + mSourceScreen.getTexId() + "->" + mOutputScreen.getTexId() + "," + effect);
			effect.apply(this);
			firstApply = false;
		}
		return this;
	}

	@Override
	public int getWidth() {
		return mWidth;
	}

	@Override
	public int getHeight() {
		return mHeight;
	}

	@NonNull
	@Override
	public int[] getSourceTexId() {
		return mSrcTexIds;
	}

	@Override
	public int getOutputTargetTexId() {
		return mOutputScreen.getTexId();
	}

	@Nullable
	@Override
	public GLSurface getOutputTargetTexture() {
		return mOutputScreen;
	}

	@Nullable
	@Override
	public float[] getTexMatrix() {
		return mSourceScreen.copyTexMatrix();
	}

	@Nullable
	@Override
	public GLSurface getResultTexture() {
		// 一度もフィルターが適用されていない場合はmmSourceScreenを返す
		// フィルターが1度でも適用されていればmOutputScreenを返す
		return firstApply ? mSourceScreen : mOutputScreen;
	}

	/**
	 * 関係するリソースを破棄
	 * GLコンテキスト内で呼び出すこと
	 */
	@Override
	public void release() {
		mSrcTexIds[0] = -1;
		if (mSourceScreen != null) {
			mSourceScreen.release();
			mSourceScreen = null;
		}
		if (mOutputScreen != null) {
			mOutputScreen.release();
			mOutputScreen = null;
		}
	}

	@Deprecated
	public MediaSource bind() {
		mSourceScreen.makeCurrent();
		return this;
	}

	@Deprecated
	public MediaSource unbind() {
		mSourceScreen.swap();
		reset();
		return this;
	}

	/**
	 * 入力用オフスクリーンに映像をセット
	 * @param drawer オフスクリーン描画用GLDrawer2D
	 * @param texId
	 * @param texMatrix
	 * @return
	 */
	public MediaSource setSource(
		@NonNull final GLDrawer2D drawer,
		final int texId,
		@Nullable @Size(min=16) final float[] texMatrix) {

//		if (DEBUG) Log.i(TAG, "setSource:" + mSourceScreen.getTexId());
		mSourceScreen.makeCurrent();
		try {
			drawer.draw(GLES20.GL_TEXTURE0, texId, texMatrix, 0);
		} catch (RuntimeException e) {
			Log.w(TAG, e);
		} finally {
			mSourceScreen.swap();
		}
		reset();
		return this;
	}
}
