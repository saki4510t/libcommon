package com.serenegiant.mediaeffect;
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

import com.serenegiant.glutils.GLSurface;

public class MediaImageSource extends MediaSource {
	private GLSurface mImageOffscreen;
	private boolean isReset;
	/**
	 * コンストラクタ
	 * GLコンテキスト内で生成すること
	 */
	public MediaImageSource(final Bitmap src) {
		super(src.getWidth(), src.getHeight());
		mImageOffscreen = GLSurface.newInstance(false, mWidth, mHeight, false);
		setSource(src);
	}

	/**
	 * 映像ソース用のBitmapをセット
	 * GLコンテキスト内で呼び出すこと
	 * @param bitmap
	 * @return
	 */
	public ISource setSource(final Bitmap bitmap) {
		mImageOffscreen.loadBitmap(bitmap);
		reset();
		return this;
	}

	/**
	 * オフスクリーンを初期状態に戻す
	 * GLコンテキスト内で呼び出すこと
	 * @return
	 */
	@Override
	public ISource reset() {
		super.reset();
		isReset = true;
		mSrcTexIds[0] = mImageOffscreen.getTexId();
		return this;
	}

	/**
	 * IEffectを適用する。1回呼び出す毎に入力と出力のオフスクリーン(テクスチャ)が入れ替わる
	 * GLコンテキスト内で呼び出すこと
	 * @param effect
	 * @return
	 */
	@Override
	public ISource apply(IEffect effect) {
		if (mSourceScreen != null) {
			if (isReset) {
				isReset = false;
				needSwap = true;
			} else {
				if (needSwap) {
					final GLSurface temp = mSourceScreen;
					mSourceScreen = mOutputScreen;
					mOutputScreen = temp;
					mSrcTexIds[0] = mSourceScreen.getTexId();
				}
				needSwap = !needSwap;
			}
			effect.apply(mSrcTexIds,
				mOutputScreen.getTexWidth(), mOutputScreen.getTexHeight(),
				mOutputScreen.getTexId());
		}
		return this;
	}

}
