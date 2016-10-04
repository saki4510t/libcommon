package com.serenegiant.mediaeffect;
/*
 * Androusb
 * Copyright (c) 2014-2016 saki t_saki@serenegiant.com
 * Distributed under the terms of the GNU Lesser General Public License (LGPL v3.0) License.
 * License details are in the file license.txt, distributed as part of this software.
 */

import android.util.Log;

import com.serenegiant.glutils.GLDrawer2D;
import com.serenegiant.glutils.TextureOffscreen;

public class MediaSource implements ISource {
	private static final boolean DEBUG = false;
	private static final String TAG = "MediaSource";

	protected TextureOffscreen mSourceScreen;
	protected TextureOffscreen mOutputScreen;
	protected int mWidth, mHeight;
	protected int[] mSrcTexIds = new int[1];
	protected boolean needSwap;

	/**
	 * コンストラクタ
	 * GLコンテキスト内で生成すること
	 */
	public MediaSource() {
		resize(1, 1);
	}

	public MediaSource(final int width, final int height) {
		resize(width, height);
	}

	@Override
	public ISource reset() {
		needSwap = false;
		mSrcTexIds[0] = mSourceScreen.getTexture();
		return this;
	}

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
				mSourceScreen = new TextureOffscreen(width, height, false, false);
				mOutputScreen = new TextureOffscreen(width, height, false, false);
				mWidth = width;
				mHeight = height;
				mSrcTexIds[0] = mSourceScreen.getTexture();
			}
		}
		needSwap = false;
		return this;
	}

	@Override
	public ISource apply(final IEffect effect) {
		if (mSourceScreen != null) {
			if (needSwap) {
				final TextureOffscreen temp = mSourceScreen;
				mSourceScreen = mOutputScreen;
				mOutputScreen = temp;
				mSrcTexIds[0] = mSourceScreen.getTexture();
			}
			needSwap = !needSwap;
//			effect.apply(mSrcTexIds, mOutputScreen.getTexWidth(), mOutputScreen.getTexHeight(), mOutputScreen.getTexture());
			effect.apply(this); // このメソッド呼び出しは1つ上のコメントアウトしてある行と結果は等価だけど効率はいい。
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

	@Override
	public int[] getSourceTexId() {
		return mSrcTexIds;
	}

	@Override
	public int getOutputTexId() {
		return needSwap ? mOutputScreen.getTexture() : mSourceScreen.getTexture();
	}

	@Override
	public float[] getTexMatrix() {
		return needSwap ? mOutputScreen.getTexMatrix() : mSourceScreen.getTexMatrix();
	}

	@Override
	public TextureOffscreen getOutputTexture() {
		return needSwap ? mOutputScreen : mSourceScreen;
	}

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

	public MediaSource bind() {
		mSourceScreen.bind();
		return this;
	}

	public MediaSource unbind() {
		mSourceScreen.unbind();
		reset();
		return this;
	}

	/**
	 * 入力用オフスクリーンに映像をセット
	 * @param drawer オフスクリーン描画用FullFrameRect
	 * @param tex_id
	 * @param tex_matrix
	 * @return
	 */
	public MediaSource setSource(final GLDrawer2D drawer, final int tex_id, final float[] tex_matrix) {
		mSourceScreen.bind();
		try {
			drawer.draw(tex_id, tex_matrix, 0);
		} catch (RuntimeException e) {
			Log.w(TAG, e);
		} finally {
			mSourceScreen.unbind();
		}
		reset();
		return this;
	}
}
