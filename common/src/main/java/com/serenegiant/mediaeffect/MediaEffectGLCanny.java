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

import static com.serenegiant.gl.ShaderConst.FRAGMENT_SHADER_CANNY_ES2;

/**
 * Cannyエッジ検出フィルタ
 * 1．ガウシアンフィルタでノイズ除去
 * 2．ソーベルフィルタで輪郭強調
 * 3．輝度の勾配の方向と大きさを計算
 * 4．細線化を行うために非極大値抑制処理
 * 5．誤検知したエッジを除去するためにヒステリシスしきい値処理
 */
public class MediaEffectGLCanny extends MediaEffectGLBase {
	private static final boolean DEBUG = false;
	private static final String TAG = "MediaEffectGLCanny";

	public MediaEffectGLCanny() {
		super(new MediaEffectGLKernel3x3Drawer(false, FRAGMENT_SHADER_CANNY_ES2));
		if (DEBUG) Log.v(TAG, "コンストラクタ:");
	}

	public MediaEffectGLCanny(final float threshold) {
		this();
		setParameter(threshold);
	}

	public MediaEffectGLCanny setParameter(final float threshold) {
		((MediaEffectGLKernel3x3Drawer)mDrawer).setColorAdjust(threshold);
		return this;
	}
}
