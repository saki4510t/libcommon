package com.serenegiant.mediaeffect;
/*
 * Androusb
 * Copyright (c) 2014-2016 saki t_saki@serenegiant.com
 * Distributed under the terms of the GNU Lesser General Public License (LGPL v3.0) License.
 * License details are in the file license.txt, distributed as part of this software.
 */

import android.graphics.Bitmap;

import com.serenegiant.glutils.TextureOffscreen;

public class MediaImageSource extends MediaSource {
	private TextureOffscreen mImageOffscreen;
	private boolean isReset;
	/**
	 * コンストラクタ
	 * GLコンテキスト内で生成すること
	 */
	public MediaImageSource(final Bitmap src) {
		super(src.getWidth(), src.getHeight());
		mImageOffscreen = new TextureOffscreen(mWidth, mHeight, false);
		SetSource(src);
	}

	public ISource SetSource(final Bitmap bitmap) {
		mImageOffscreen.loadBitmap(bitmap);
		reset();
		return this;
	}

	@Override
	public ISource reset() {
		super.reset();
		isReset = true;
		mSrcTexIds[0] = mImageOffscreen.getTexture();
		return this;
	}

	@Override
	public ISource apply(IEffect effect) {
		if (mSourceScreen != null) {
			if (isReset) {
				isReset = false;
				needSwap = true;
			} else {
				if (needSwap) {
					final TextureOffscreen temp = mSourceScreen;
					mSourceScreen = mOutputScreen;
					mOutputScreen = temp;
					mSrcTexIds[0] = mSourceScreen.getTexture();
				}
				needSwap = !needSwap;
			}
			effect.apply(mSrcTexIds, mOutputScreen.getTexWidth(), mOutputScreen.getTexHeight(), mOutputScreen.getTexture());
		}
		return this;
	}

}
